package com.assessment.service.impl;

import com.assessment.cassandra.utils.CassandraOperation;
import com.assessment.datasecurity.EncryptionService;
import com.assessment.model.SBApiResponse;
import com.assessment.repo.AssessmentRepository;
import com.assessment.service.AssessmentServiceV5;
import com.assessment.service.AssessmentUtilServiceV2;
import com.assessment.service.OutboundRequestHandlerServiceImpl;
import com.assessment.util.Constants;
import com.assessment.util.ProjectUtil;
import com.assessment.util.ServerProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.mortbay.util.ajax.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.sql.Timestamp;
import java.util.*;

import static java.util.stream.Collectors.toList;

@Service
@SuppressWarnings("unchecked")
public class AssessmentServiceV5Impl implements AssessmentServiceV5 {

    private final Logger logger = LoggerFactory.getLogger(AssessmentServiceV5Impl.class);
    @Autowired
    ServerProperties serverProperties;

    @Autowired
    OutboundRequestHandlerServiceImpl outboundRequestHandlerService;

    @Autowired
    AssessmentUtilServiceV2 assessUtilServ;

    @Autowired
    ObjectMapper mapper;

    @Autowired
    private AssessmentRepository assessmentRepository;

    @Autowired
    CassandraOperation cassandraOperation;

    @Autowired
    private EncryptionService encryptionService;


    /**
     * Submits an assessment asynchronously.
     *
     * @param submitRequest     The assessment data to be submitted.
     * @param email    The email of the user submitting the assessment.
     * @param editMode Whether the assessment is being submitted in edit mode.
     * @return The response from the assessment submission.
     */
    public SBApiResponse submitAssessmentAsync(Map<String, Object> submitRequest, String email, boolean editMode) {
        logger.info("AssessmentServicev5Impl::submitAssessmentAsync.. started");
        SBApiResponse outgoingResponse = ProjectUtil.createDefaultResponse(Constants.API_SUBMIT_ASSESSMENT);
        long assessmentCompletionTime = Calendar.getInstance().getTime().getTime();
        try {
            // Step-1 fetch userid
            if (!ProjectUtil.validateEmailPattern(email)) {
                updateErrorDetails(outgoingResponse, Constants.INVALID_EMAIL, HttpStatus.BAD_REQUEST);
                return outgoingResponse;
            }

            email = encryptionService.encryptData(email);
            String assessmentIdFromRequest = (String) submitRequest.get(Constants.IDENTIFIER);
            String errMsg;
            List<Map<String, Object>> sectionListFromSubmitRequest = new ArrayList<>();
            List<Map<String, Object>> hierarchySectionList = new ArrayList<>();
            Map<String, Object> assessmentHierarchy = new HashMap<>();
            Map<String, Object> existingAssessmentData = new HashMap<>();
            //Confirm whether the submitted request sections and questions match.
            errMsg = validateSubmitAssessmentRequest(submitRequest, email, hierarchySectionList,
                    sectionListFromSubmitRequest, assessmentHierarchy, existingAssessmentData, editMode);
            if (StringUtils.isNotBlank(errMsg)) {
                updateErrorDetails(outgoingResponse, errMsg, HttpStatus.BAD_REQUEST);
                return outgoingResponse;
            }
            int maxAssessmentRetakeAttempts = (Integer) assessmentHierarchy.get(Constants.MAX_ASSESSMENT_RETAKE_ATTEMPTS);
            int retakeAttemptsConsumed = calculateAssessmentRetakeCount(email, assessmentIdFromRequest);
            String assessmentPrimaryCategory = (String) assessmentHierarchy.get(Constants.PRIMARY_CATEGORY);
            String assessmentType = ((String) assessmentHierarchy.get(Constants.ASSESSMENT_TYPE)).toLowerCase();
            String scoreCutOffType;
            if (assessmentType.equalsIgnoreCase(Constants.QUESTION_WEIGHTAGE)) {
                scoreCutOffType = Constants.SECTION_LEVEL_SCORE_CUTOFF;
            } else {
                scoreCutOffType = Constants.ASSESSMENT_LEVEL_SCORE_CUTOFF;
            }
            List<Map<String, Object>> sectionLevelsResults = new ArrayList<>();
            for (Map<String, Object> hierarchySection : hierarchySectionList) {
                String hierarchySectionId = (String) hierarchySection.get(Constants.IDENTIFIER);
                String userSectionId = "";
                Map<String, Object> userSectionData = new HashMap<>();
                for (Map<String, Object> sectionFromSubmitRequest : sectionListFromSubmitRequest) {
                    userSectionId = (String) sectionFromSubmitRequest.get(Constants.IDENTIFIER);
                    if (userSectionId.equalsIgnoreCase(hierarchySectionId)) {
                        userSectionData = sectionFromSubmitRequest;
                        break;
                    }
                }

                hierarchySection.put(Constants.SCORE_CUTOFF_TYPE, scoreCutOffType);
                List<Map<String, Object>> questionsListFromSubmitRequest = new ArrayList<>();
                if (userSectionData.containsKey(Constants.CHILDREN)
                        && !ObjectUtils.isEmpty(userSectionData.get(Constants.CHILDREN))) {
                    questionsListFromSubmitRequest = (List<Map<String, Object>>) userSectionData
                            .get(Constants.CHILDREN);
                }
                List<String> desiredKeys = Lists.newArrayList(Constants.IDENTIFIER);
                List<Object> questionsList = questionsListFromSubmitRequest.stream()
                        .flatMap(x -> desiredKeys.stream().filter(x::containsKey).map(x::get)).collect(toList());
                List<String> questionsListFromAssessmentHierarchy = questionsList.stream()
                        .map(object -> Objects.toString(object, null)).collect(toList());
                Map<String, Object> result = new HashMap<>();
                Map<String, Object> questionSetDetailsMap = getParamDetailsForQTypes(hierarchySection, assessmentHierarchy, hierarchySectionId);
                switch (scoreCutOffType) {
                    case Constants.ASSESSMENT_LEVEL_SCORE_CUTOFF: {
                        result.putAll(createResponseMapWithProperStructure(hierarchySection,
                                assessUtilServ.validateQumlAssessmentV2(questionSetDetailsMap, questionsListFromAssessmentHierarchy,
                                        questionsListFromSubmitRequest, assessUtilServ.readQListfromCache(questionsListFromAssessmentHierarchy, assessmentIdFromRequest, editMode))));
                        Map<String, Object> finalRes = calculateAssessmentFinalResults(result);
                        outgoingResponse.getResult().putAll(finalRes);
                        outgoingResponse.getResult().put(Constants.PRIMARY_CATEGORY, assessmentPrimaryCategory);
                        if (!Constants.PRACTICE_QUESTION_SET.equalsIgnoreCase(assessmentPrimaryCategory) && !editMode) {
                            String questionSetFromAssessmentString = (String) existingAssessmentData
                                    .get(Constants.ASSESSMENT_READ_RESPONSE_KEY);
                            Map<String, Object> questionSetFromAssessment = null;
                            if (StringUtils.isNotBlank(questionSetFromAssessmentString)) {
                                questionSetFromAssessment = mapper.readValue(questionSetFromAssessmentString,
                                        new TypeReference<Map<String, Object>>() {
                                        });
                            }
                            writeDataToDatabaseAndTriggerKafkaEvent(submitRequest, email, questionSetFromAssessment, finalRes,
                                    (String) assessmentHierarchy.get(Constants.PRIMARY_CATEGORY));
                        }
                        return outgoingResponse;
                    }
                    case Constants.SECTION_LEVEL_SCORE_CUTOFF: {
                        result.putAll(createResponseMapWithProperStructure(hierarchySection,
                                assessUtilServ.validateQumlAssessmentV2(questionSetDetailsMap, questionsListFromAssessmentHierarchy,
                                        questionsListFromSubmitRequest, assessUtilServ.readQListfromCache(questionsListFromAssessmentHierarchy, assessmentIdFromRequest, editMode))));
                        sectionLevelsResults.add(result);
                    }
                    break;
                    default:
                        break;
                }
            }
            if (Constants.SECTION_LEVEL_SCORE_CUTOFF.equalsIgnoreCase(scoreCutOffType)) {
                long assessmentStartTime = 0;
                if (existingAssessmentData.get(Constants.START_TIME) != null) {
                    Date assessmentStart = (Date) existingAssessmentData.get(Constants.START_TIME);
                    assessmentStartTime = assessmentStart.getTime();
                }
                Map<String, Object> result = calculateSectionFinalResults(sectionLevelsResults, assessmentStartTime, assessmentCompletionTime, maxAssessmentRetakeAttempts, retakeAttemptsConsumed);
                outgoingResponse.getResult().putAll(result);
                outgoingResponse.getParams().setStatus(Constants.SUCCESS);
                outgoingResponse.setResponseCode(HttpStatus.OK);
                outgoingResponse.getResult().put(Constants.PRIMARY_CATEGORY, assessmentPrimaryCategory);
                if (!Constants.PRACTICE_QUESTION_SET.equalsIgnoreCase(assessmentPrimaryCategory) && !editMode) {
                    String questionSetFromAssessmentString = (String) existingAssessmentData
                            .get(Constants.ASSESSMENT_READ_RESPONSE_KEY);
                    Map<String, Object> questionSetFromAssessment = null;
                    if (StringUtils.isNotBlank(questionSetFromAssessmentString)) {
                        questionSetFromAssessment = mapper.readValue(questionSetFromAssessmentString,
                                new TypeReference<Map<String, Object>>() {
                                });
                    }
                    writeDataToDatabaseAndTriggerKafkaEvent(submitRequest, email, questionSetFromAssessment, result,
                            (String) assessmentHierarchy.get(Constants.PRIMARY_CATEGORY));
                }
                return outgoingResponse;
            }

        } catch (Exception e) {
            String errMsg = String.format("Failed to process assessment submit request. Exception: ", e.getMessage());
            logger.error(errMsg, e);
            updateErrorDetails(outgoingResponse, errMsg, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return outgoingResponse;
    }

    private void updateErrorDetails(SBApiResponse response, String errMsg, HttpStatus responseCode) {
        response.getParams().setStatus(Constants.FAILED);
        response.getParams().setErrmsg(errMsg);
        response.setResponseCode(responseCode);
    }

    private int calculateAssessmentRetakeCount(String email, String assessmentId) {
        List<Map<String, Object>> userAssessmentDataList = assessUtilServ.readUserSubmittedAssessmentRecords(email,
                assessmentId);
        return (int) userAssessmentDataList.stream()
                .filter(userData -> userData.containsKey(Constants.SUBMIT_ASSESSMENT_RESPONSE_KEY)
                        && null != userData.get(Constants.SUBMIT_ASSESSMENT_RESPONSE_KEY))
                .count();
    }

    private Timestamp calculateAssessmentSubmitTime(int expectedDuration, Timestamp assessmentStartTime,
                                                    int bufferTime) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(assessmentStartTime.getTime());
        if (bufferTime > 0) {
            cal.add(Calendar.SECOND,
                    expectedDuration + Integer.parseInt(serverProperties.getUserAssessmentSubmissionDuration()));
        } else {
            cal.add(Calendar.SECOND, expectedDuration);
        }
        return new Timestamp(cal.getTime().getTime());
    }

    private String validateSubmitAssessmentRequest(Map<String, Object> submitRequest, String email,
                                                   List<Map<String, Object>> hierarchySectionList, List<Map<String, Object>> sectionListFromSubmitRequest,
                                                   Map<String, Object> assessmentHierarchy, Map<String, Object> existingAssessmentData, boolean editMode) throws Exception {
        if (StringUtils.isEmpty((String) submitRequest.get(Constants.IDENTIFIER))) {
            return Constants.INVALID_ASSESSMENT_ID;
        }
        String assessmentIdFromRequest = (String) submitRequest.get(Constants.IDENTIFIER);
        assessmentHierarchy.putAll(assessUtilServ.readAssessmentHierarchyFromCache(assessmentIdFromRequest, editMode));
        if (MapUtils.isEmpty(assessmentHierarchy)) {
            return Constants.READ_ASSESSMENT_FAILED;
        }

        hierarchySectionList.addAll((List<Map<String, Object>>) assessmentHierarchy.get(Constants.CHILDREN));
        sectionListFromSubmitRequest.addAll((List<Map<String, Object>>) submitRequest.get(Constants.CHILDREN));
        if (((String) (assessmentHierarchy.get(Constants.PRIMARY_CATEGORY)))
                .equalsIgnoreCase(Constants.PRACTICE_QUESTION_SET) || editMode)
            return "";

        List<Map<String, Object>> existingDataList = assessUtilServ.readUserSubmittedAssessmentRecords(
                email, (String) submitRequest.get(Constants.IDENTIFIER));
        if (existingDataList.isEmpty()) {
            return Constants.USER_ASSESSMENT_DATA_NOT_PRESENT;
        } else {
            existingAssessmentData.putAll(existingDataList.get(0));
        }

        Date assessmentStartTime = (Date) existingAssessmentData.get(Constants.START_TIME);
        if (assessmentStartTime == null) {
            return Constants.READ_ASSESSMENT_START_TIME_FAILED;
        }
        int expectedDuration = (Integer) assessmentHierarchy.get(Constants.EXPECTED_DURATION);
        Timestamp later = calculateAssessmentSubmitTime(expectedDuration,
                new Timestamp(assessmentStartTime.getTime()),
                Integer.parseInt(serverProperties.getUserAssessmentSubmissionDuration()));
        Timestamp submissionTime = new Timestamp(new Date().getTime());
        int time = submissionTime.compareTo(later);
        if (time <= 0) {
            List<String> desiredKeys = Lists.newArrayList(Constants.IDENTIFIER);
            List<Object> hierarchySectionIds = hierarchySectionList.stream()
                    .flatMap(x -> desiredKeys.stream().filter(x::containsKey).map(x::get)).collect(toList());
            List<Object> submitSectionIds = sectionListFromSubmitRequest.stream()
                    .flatMap(x -> desiredKeys.stream().filter(x::containsKey).map(x::get)).collect(toList());
            if (!new HashSet<>(hierarchySectionIds).containsAll(submitSectionIds)) {
                return Constants.WRONG_SECTION_DETAILS;
            } else {
                String areQuestionIdsSame = validateIfQuestionIdsAreSame(submitRequest,
                        sectionListFromSubmitRequest, desiredKeys, email, existingAssessmentData);
                if (!areQuestionIdsSame.isEmpty())
                    return areQuestionIdsSame;
            }
        } else {
            return Constants.ASSESSMENT_SUBMIT_EXPIRED;
        }

        return "";
    }

    private String validateIfQuestionIdsAreSame(Map<String, Object> submitRequest,
                                                List<Map<String, Object>> sectionListFromSubmitRequest, List<String> desiredKeys, String userId,
                                                Map<String, Object> existingAssessmentData) throws Exception {
        String questionSetFromAssessmentString = (String) existingAssessmentData
                .get(Constants.ASSESSMENT_READ_RESPONSE_KEY);
        if (StringUtils.isNotBlank(questionSetFromAssessmentString)) {
            Map<String, Object> questionSetFromAssessment = mapper.readValue(questionSetFromAssessmentString,
                    new TypeReference<Map<String, Object>>() {
                    });
            if (questionSetFromAssessment != null && questionSetFromAssessment.get(Constants.CHILDREN) != null) {
                List<Map<String, Object>> sections = (List<Map<String, Object>>) questionSetFromAssessment
                        .get(Constants.CHILDREN);
                List<String> desiredKey = Lists.newArrayList(Constants.CHILD_NODES);
                List<Object> questionList = sections.stream()
                        .flatMap(x -> desiredKey.stream().filter(x::containsKey).map(x::get)).collect(toList());
                List<Object> questionIdsFromAssessmentHierarchy = new ArrayList<>();
                List<Map<String, Object>> questionsListFromSubmitRequest = new ArrayList<>();
                for (Object question : questionList) {
                    questionIdsFromAssessmentHierarchy.addAll((List<String>) question);
                }
                for (Map<String, Object> userSectionData : sectionListFromSubmitRequest) {
                    if (userSectionData.containsKey(Constants.CHILDREN)
                            && !ObjectUtils.isEmpty(userSectionData.get(Constants.CHILDREN))) {
                        questionsListFromSubmitRequest
                                .addAll((List<Map<String, Object>>) userSectionData.get(Constants.CHILDREN));
                    }
                }
                List<Object> userQuestionIdsFromSubmitRequest = questionsListFromSubmitRequest.stream()
                        .flatMap(x -> desiredKeys.stream().filter(x::containsKey).map(x::get))
                        .collect(toList());
                if (!new HashSet<>(questionIdsFromAssessmentHierarchy).containsAll(userQuestionIdsFromSubmitRequest)) {
                    return Constants.ASSESSMENT_SUBMIT_INVALID_QUESTION;
                }
            }
        } else {
            return Constants.ASSESSMENT_SUBMIT_QUESTION_READ_FAILED;
        }
        return "";
    }

    public Map<String, Object> createResponseMapWithProperStructure(Map<String, Object> hierarchySection,
                                                                    Map<String, Object> resultMap) {
        Map<String, Object> sectionLevelResult = new HashMap<>();
        sectionLevelResult.put(Constants.IDENTIFIER, hierarchySection.get(Constants.IDENTIFIER));
        sectionLevelResult.put(Constants.OBJECT_TYPE, hierarchySection.get(Constants.OBJECT_TYPE));
        sectionLevelResult.put(Constants.PRIMARY_CATEGORY, hierarchySection.get(Constants.PRIMARY_CATEGORY));
        sectionLevelResult.put(Constants.PASS_PERCENTAGE, hierarchySection.get(Constants.MINIMUM_PASS_PERCENTAGE));
        sectionLevelResult.put(Constants.NAME, hierarchySection.get(Constants.NAME));
        Double result;
        if (!ObjectUtils.isEmpty(resultMap)) {
            result = (Double) resultMap.get(Constants.RESULT);
            sectionLevelResult.put(Constants.RESULT, result);
            sectionLevelResult.put(Constants.BLANK, resultMap.get(Constants.BLANK));
            sectionLevelResult.put(Constants.CORRECT, resultMap.get(Constants.CORRECT));
            sectionLevelResult.put(Constants.INCORRECT, resultMap.get(Constants.INCORRECT));
            sectionLevelResult.put(Constants.CHILDREN, resultMap.get(Constants.CHILDREN));
            sectionLevelResult.put(Constants.SECTION_RESULT, resultMap.get(Constants.SECTION_RESULT));
            sectionLevelResult.put(Constants.TOTAL_MARKS, resultMap.get(Constants.TOTAL_MARKS));
            sectionLevelResult.put(Constants.SECTION_MARKS, resultMap.get(Constants.SECTION_MARKS));


        } else {
            result = 0.0;
            sectionLevelResult.put(Constants.RESULT, result);
            List<String> childNodes = (List<String>) hierarchySection.get(Constants.CHILDREN);
            sectionLevelResult.put(Constants.TOTAL, childNodes.size());
            sectionLevelResult.put(Constants.BLANK, childNodes.size());
            sectionLevelResult.put(Constants.CORRECT, 0);
            sectionLevelResult.put(Constants.INCORRECT, 0);
        }
        sectionLevelResult.put(Constants.PASS,
                result >= ((Integer) hierarchySection.get(Constants.MINIMUM_PASS_PERCENTAGE)));
        sectionLevelResult.put(Constants.OVERALL_RESULT, result);
        return sectionLevelResult;
    }

    private Map<String, Object> calculateAssessmentFinalResults(Map<String, Object> assessmentLevelResult) {
        Map<String, Object> res = new HashMap<>();
        try {
            res.put(Constants.CHILDREN, Collections.singletonList(assessmentLevelResult));
            Double result = (Double) assessmentLevelResult.get(Constants.RESULT);
            res.put(Constants.OVERALL_RESULT, result);
            res.put(Constants.TOTAL, assessmentLevelResult.get(Constants.TOTAL));
            res.put(Constants.BLANK, assessmentLevelResult.get(Constants.BLANK));
            res.put(Constants.CORRECT, assessmentLevelResult.get(Constants.CORRECT));
            res.put(Constants.PASS_PERCENTAGE, assessmentLevelResult.get(Constants.PASS_PERCENTAGE));
            res.put(Constants.INCORRECT, assessmentLevelResult.get(Constants.INCORRECT));
            res.put(Constants.NAME, assessmentLevelResult.get(Constants.NAME));
            Integer minimumPassPercentage = (Integer) assessmentLevelResult.get(Constants.PASS_PERCENTAGE);
            res.put(Constants.PASS, result >= minimumPassPercentage);
        } catch (Exception e) {
            logger.error("Failed to calculate Assessment final results. Exception: ", e);
        }
        return res;
    }

    private void writeDataToDatabaseAndTriggerKafkaEvent(Map<String, Object> submitRequest, String email,
                                                         Map<String, Object> questionSetFromAssessment, Map<String, Object> result, String primaryCategory) {
        try {
            if (questionSetFromAssessment.get(Constants.START_TIME) != null) {
                Long existingAssessmentStartTime = (Long) questionSetFromAssessment.get(Constants.START_TIME);
                Timestamp startTime = new Timestamp(existingAssessmentStartTime);
                Boolean isAssessmentUpdatedToDB = assessmentRepository.updateUserAssesmentDataToDB(email,
                        (String) submitRequest.get(Constants.IDENTIFIER), submitRequest, result, Constants.SUBMITTED,
                        startTime, null);
                if (Boolean.TRUE.equals(isAssessmentUpdatedToDB)) {
                    Map<String, Object> kafkaResult = new HashMap<>();
                    kafkaResult.put(Constants.CONTENT_ID_KEY, submitRequest.get(Constants.IDENTIFIER));
                    kafkaResult.put(Constants.COURSE_ID,
                            submitRequest.get(Constants.COURSE_ID) != null ? submitRequest.get(Constants.COURSE_ID)
                                    : "");
                    kafkaResult.put(Constants.BATCH_ID,
                            submitRequest.get(Constants.BATCH_ID) != null ? submitRequest.get(Constants.BATCH_ID) : "");
                    kafkaResult.put(Constants.USER_ID, submitRequest.get(Constants.USER_ID));
                    kafkaResult.put(Constants.ASSESSMENT_ID_KEY, submitRequest.get(Constants.IDENTIFIER));
                    kafkaResult.put(Constants.PRIMARY_CATEGORY, primaryCategory);
                    kafkaResult.put(Constants.TOTAL_SCORE, result.get(Constants.OVERALL_RESULT));
                    if ((primaryCategory.equalsIgnoreCase("Competency Assessment")
                            && submitRequest.containsKey("competencies_v3")
                            && submitRequest.get("competencies_v3") != null)) {
                        Object[] obj = (Object[]) JSON.parse((String) submitRequest.get("competencies_v3"));
                        if (obj != null) {
                            Object map = obj[0];
                            ObjectMapper m = new ObjectMapper();
                            Map<String, Object> props = m.convertValue(map, Map.class);
                            kafkaResult.put(Constants.COMPETENCY, props.isEmpty() ? "" : props);
                            System.out.println(obj);

                        }
                        System.out.println(obj);
                    }
                    //kafka topic push code needs to be added
                }
            }
        } catch (Exception e) {
            logger.error("Failed to write data for assessment submit response. Exception: ", e);
        }
    }

    private Map<String, Object> calculateSectionFinalResults(List<Map<String, Object>> sectionLevelResults, long assessmentStartTime, long assessmentCompletionTime, int maxAssessmentRetakeAttempts, int retakeAttemptsConsumed) {
        Map<String, Object> res = new HashMap<>();
        Double result;
        Integer correct = 0;
        Integer blank = 0;
        Integer inCorrect = 0;
        Integer total = 0;
        Double totalSectionMarks = 0.0;
        Integer totalMarks = 0;
        int pass = 0;
        Double totalResult = 0.0;
        try {
            for (Map<String, Object> sectionChildren : sectionLevelResults) {
                res.put(Constants.CHILDREN, sectionLevelResults);
                result = (Double) sectionChildren.get(Constants.RESULT);
                totalResult += result;
                blank += (Integer) sectionChildren.get(Constants.BLANK);
                correct += (Integer) sectionChildren.get(Constants.CORRECT);
                inCorrect += (Integer) sectionChildren.get(Constants.INCORRECT);
                Integer minimumPassPercentage = (Integer) sectionChildren.get(Constants.PASS_PERCENTAGE);
                if (result >= minimumPassPercentage) {
                    pass++;
                }
                if (sectionChildren.get(Constants.SECTION_MARKS) != null) {
                    totalSectionMarks += (Double) sectionChildren.get(Constants.SECTION_MARKS);
                }
                if (sectionChildren.get(Constants.TOTAL_MARKS) != null) {
                    totalMarks += (Integer) sectionChildren.get(Constants.TOTAL_MARKS);
                }
            }
            if (correct > 0 && inCorrect > 0) {
                res.put(Constants.OVERALL_RESULT, ((double) correct / (double) (correct + inCorrect)) * 100);
            } else {
                res.put(Constants.OVERALL_RESULT, 0);
            }
            res.put(Constants.BLANK, blank);
            res.put(Constants.CORRECT, correct);
            res.put(Constants.INCORRECT, inCorrect);
            res.put(Constants.PASS, (pass == sectionLevelResults.size()));
            res.put(Constants.TIME_TAKEN_FOR_ASSESSMENT, assessmentCompletionTime - assessmentStartTime);
            res.put(Constants.MAX_ASSESSMENT_RETAKE_ATTEMPTS, maxAssessmentRetakeAttempts);
            res.put(Constants.RETAKE_ATTEMPT_CONSUMED, retakeAttemptsConsumed);
            double totalPercentage = (totalSectionMarks / (double) totalMarks) * 100;
            res.put(Constants.TOTAL_PERCENTAGE, totalPercentage);
            res.put(Constants.TOTAL_SECTION_MARKS, totalSectionMarks);
            res.put(Constants.TOTAL_MARKS, totalMarks);
        } catch (Exception e) {
            logger.error("Failed to calculate assessment score. Exception: ", e);
        }
        return res;
    }


    /**
     * Generates a map containing marks for each question.
     * The input is a map where each key is a section name, and the value is another map.
     * This inner map has proficiency keys, and each proficiency key maps to a map containing various attributes including "marksForQuestion".
     * The output map's keys are of the format "sectionKey|proficiencyKey" and values are the corresponding marks for that question.
     *
     * @param qSectionSchemeMap a map representing sections and their respective proficiency maps
     * @return a map where each key is a combination of section and proficiency, and each value is the marks for that question
     */
    public Map<String, Integer> generateMarkMap(Map<String, Map<String, Object>> qSectionSchemeMap) {
        Map<String, Integer> markMap = new HashMap<>();
        logger.info("Starting to generate mark map from qSectionSchemeMap");
        qSectionSchemeMap.keySet().forEach(sectionKey -> {
            Map<String, Object> proficiencyMap = qSectionSchemeMap.get(sectionKey);
            proficiencyMap.forEach((key, value) -> {
                if (key.equalsIgnoreCase("marksForQuestion")) {
                    markMap.put(sectionKey, (Integer) value);
                }
            });
        });
        logger.info("Completed generating mark map");
        return markMap;
    }


    /**
     * Retrieves the parameter details for question types based on the given assessment hierarchy.
     *
     * @param assessmentHierarchy a map containing the assessment hierarchy details.
     * @return a map containing the parameter details for the question types.
     */
    private Map<String, Object> getParamDetailsForQTypes(Map<String, Object> hierarchySection, Map<String, Object> assessmentHierarchy, String hierarchySectionId) {
        logger.info("Starting getParamDetailsForQTypes with assessmentHierarchy: {}", assessmentHierarchy);
        Map<String, Object> questionSetDetailsMap = new HashMap<>();
        String assessmentType = (String) assessmentHierarchy.get(Constants.ASSESSMENT_TYPE);
        questionSetDetailsMap.put(Constants.ASSESSMENT_TYPE, assessmentType);
        questionSetDetailsMap.put(Constants.MINIMUM_PASS_PERCENTAGE, assessmentHierarchy.get(Constants.MINIMUM_PASS_PERCENTAGE));
        questionSetDetailsMap.put(Constants.TOTAL_MARKS, hierarchySection.get(Constants.TOTAL_MARKS));
        if (assessmentType.equalsIgnoreCase(Constants.QUESTION_WEIGHTAGE)) {
            Map<String, Map<String, Object>> questionSectionSchema = (Map<String, Map<String, Object>>) hierarchySection.get(Constants.SECTION_LEVEL_DEFINITION);
            questionSetDetailsMap.put(Constants.QUESTION_SECTION_SCHEME, generateMarkMap(questionSectionSchema));
            questionSetDetailsMap.put(Constants.NEGATIVE_MARKING_PERCENTAGE, assessmentHierarchy.get(Constants.NEGATIVE_MARKING_PERCENTAGE));
            questionSetDetailsMap.put("hierarchySectionId", hierarchySectionId);
        }
        logger.info("Completed getParamDetailsForQTypes with result: {}", questionSetDetailsMap);
        return questionSetDetailsMap;
    }
}