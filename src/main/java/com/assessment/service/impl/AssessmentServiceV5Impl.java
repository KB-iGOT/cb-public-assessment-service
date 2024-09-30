package com.assessment.service.impl;

import com.assessment.cassandra.utils.CassandraOperation;
import com.assessment.datasecurity.DecryptionService;
import com.assessment.datasecurity.EncryptionService;
import com.assessment.kafka.Producer;
import com.assessment.kafka.service.KafkaCertificateProducerService;
import com.assessment.model.Config;
import com.assessment.model.NotificationAsyncRequest;
import com.assessment.model.SBApiResponse;
import com.assessment.model.Template;
import com.assessment.repo.AssessmentRepository;
import com.assessment.service.AssessmentServiceV5;
import com.assessment.service.AssessmentUtilServiceV2;
import com.assessment.service.OutboundRequestHandlerServiceImpl;
import com.assessment.util.Constants;
import com.assessment.util.ProjectUtil;
import com.assessment.util.ServerProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import javax.validation.Valid;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
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

    @Autowired
    ResourceLoader resourceLoader;

    @Autowired
    DecryptionService decryptionService;

    @Autowired
    KafkaCertificateProducerService kafkaCertificateProducerService;

    @Autowired
    Producer producer;

    /**
     * Submits an assessment asynchronously.
     *
     * @param submitRequest     The assessment data to be submitted.
     * @param editMode Whether the assessment is being submitted in edit mode.
     * @return The response from the assessment submission.
     */
    public SBApiResponse submitAssessmentAsync(Map<String, Object> submitRequest, boolean editMode) {
        logger.info("AssessmentServicev5Impl::submitAssessmentAsync.. started");
        SBApiResponse outgoingResponse = ProjectUtil.createDefaultResponse(Constants.API_SUBMIT_ASSESSMENT);
        long assessmentCompletionTime = Calendar.getInstance().getTime().getTime();
        try {
            String errMsg;
            List<Map<String, Object>> sectionListFromSubmitRequest = new ArrayList<>();
            List<Map<String, Object>> hierarchySectionList = new ArrayList<>();
            Map<String, Object> existingAssessmentData = new HashMap<>();

            // Step-1 fetch userid
            String email = (String) submitRequest.get(Constants.EMAIL);
            String contextId = (String) submitRequest.get(Constants.CONTEXT_ID);
            String assessmentIdFromRequest = (String) submitRequest.get(Constants.IDENTIFIER);

            SBApiResponse errResponse = validateSubmitAssessmentPayload(email, assessmentIdFromRequest, editMode);
            if(!ObjectUtils.isEmpty(errResponse)){
                return errResponse;
            }

            email = encryptionService.encryptData(email);
            Map<String, Object> assessmentHierarchy = readAssessment(assessmentIdFromRequest, editMode);

            hierarchySectionList.addAll((List<Map<String, Object>>) assessmentHierarchy.get(Constants.CHILDREN));
            sectionListFromSubmitRequest.addAll((List<Map<String, Object>>) submitRequest.get(Constants.CHILDREN));

            if (((String) (assessmentHierarchy.get(Constants.PRIMARY_CATEGORY)))
                    .equalsIgnoreCase(Constants.PRACTICE_QUESTION_SET) || editMode) {

            } else {

                errMsg = validateSubmitAssessmentRequest(submitRequest, email, contextId, hierarchySectionList,
                        sectionListFromSubmitRequest, assessmentHierarchy, existingAssessmentData, editMode);
                if (StringUtils.isNotBlank(errMsg)) {
                    updateErrorDetails(outgoingResponse, errMsg, HttpStatus.BAD_REQUEST);
                    return outgoingResponse;
                }
            }
            //Confirm whether the submitted request sections and questions match.

            int maxAssessmentRetakeAttempts = (Integer) assessmentHierarchy.get(Constants.MAX_ASSESSMENT_RETAKE_ATTEMPTS);
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
                                    (String) assessmentHierarchy.get(Constants.PRIMARY_CATEGORY),contextId);
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
                int minimumPassPercentage=(int) assessmentHierarchy.get(Constants.MINIMUM_PASS_PERCENTAGE);
                Map<String, Object> result = calculateSectionFinalResults(sectionLevelsResults, assessmentStartTime, assessmentCompletionTime, maxAssessmentRetakeAttempts,minimumPassPercentage);

                if (!Constants.PRACTICE_QUESTION_SET.equalsIgnoreCase(assessmentPrimaryCategory) && !editMode) {
                    int retakeAttemptsConsumed = calculateAssessmentRetakeCount(email, assessmentIdFromRequest,contextId);
                    result.put(Constants.RETAKE_ATTEMPT_CONSUMED, retakeAttemptsConsumed);
                    outgoingResponse.getResult().putAll(result);
                    outgoingResponse.getParams().setStatus(Constants.SUCCESS);
                    outgoingResponse.setResponseCode(HttpStatus.OK);
                    outgoingResponse.getResult().put(Constants.PRIMARY_CATEGORY, assessmentPrimaryCategory);
                    String questionSetFromAssessmentString = (String) existingAssessmentData
                            .get(Constants.ASSESSMENT_READ_RESPONSE_KEY);
                    Map<String, Object> questionSetFromAssessment = null;
                    if (StringUtils.isNotBlank(questionSetFromAssessmentString)) {
                        questionSetFromAssessment = mapper.readValue(questionSetFromAssessmentString,
                                new TypeReference<Map<String, Object>>() {
                                });
                    }
                    writeDataToDatabaseAndTriggerKafkaEvent(submitRequest, email, questionSetFromAssessment, result,
                            (String) assessmentHierarchy.get(Constants.PRIMARY_CATEGORY),contextId);
                }else {
                    result.put(Constants.RETAKE_ATTEMPT_CONSUMED, 0);
                    outgoingResponse.getResult().putAll(result);
                    outgoingResponse.getParams().setStatus(Constants.SUCCESS);
                    outgoingResponse.setResponseCode(HttpStatus.OK);
                    outgoingResponse.getResult().put(Constants.PRIMARY_CATEGORY, assessmentPrimaryCategory);
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

    private int calculateAssessmentRetakeCount(String email, String assessmentId, String contextId) {
        List<Map<String, Object>> userAssessmentDataList = assessUtilServ.readUserSubmittedAssessmentRecords(email,
                assessmentId, contextId);
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

    private String validateSubmitAssessmentRequest(Map<String, Object> submitRequest, String email, String contextId,
                                                   List<Map<String, Object>> hierarchySectionList, List<Map<String, Object>> sectionListFromSubmitRequest,
                                                 Map<String, Object> assessmentHierarchy, Map<String, Object> existingAssessmentData, boolean editMode) throws Exception {

        List<Map<String, Object>> existingDataList = assessUtilServ.readUserSubmittedAssessmentRecords(
                email, (String) submitRequest.get(Constants.IDENTIFIER), contextId);
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
                                                         Map<String, Object> questionSetFromAssessment, Map<String, Object> result, String primaryCategory,String contextId) {
        try {
            if (questionSetFromAssessment.get(Constants.START_TIME) != null) {
                Long existingAssessmentStartTime = (Long) questionSetFromAssessment.get(Constants.START_TIME);
                Timestamp startTime = new Timestamp(existingAssessmentStartTime);
                String assessmentId = (String) submitRequest.get(Constants.IDENTIFIER);
                Boolean isAssessmentUpdatedToDB = assessmentRepository.updateUserAssesmentDataToDB(email,
                        assessmentId, submitRequest, result, Constants.SUBMITTED,
                        startTime, null, contextId);

                List<String> notAllowedForKafkaEvent = serverProperties.getAssessmentPrimaryKeyNotAllowedCertificate();
                if (Boolean.TRUE.equals(isAssessmentUpdatedToDB) && Boolean.TRUE.equals(result.get(Constants.PASS)) && notAllowedForKafkaEvent.stream()
                        .noneMatch(key -> key.equals(primaryCategory))) {

                    sendMessageToKafkaForCertificate(submitRequest, email, contextId, assessmentId);

                }
            }
        } catch (Exception e) {
            logger.error("Failed to write data for assessment submit response. Exception: ", e);
        }
    }

    private void sendMessageToKafkaForCertificate(Map<String, Object> submitRequest, String email, String contextId, String assessmentId) throws IOException {

        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String completionDate = dateFormat.format(new Date());

            List<Map<String, Object>> submitedAssessmentDetails = assessmentRepository.fetchUserAssessmentDataFromDB(email, (String) submitRequest.get(Constants.IDENTIFIER));
            String recipientName = (String) submitedAssessmentDetails.get(0).get(Constants.NAME);

            Map<String, Object> propertyMap = new HashMap<>();
            propertyMap.put(Constants.IDENTIFIER, contextId);
            List<Map<String, Object>> contentHierarchyDetails = cassandraOperation.getRecordsByProperties(serverProperties.getContentHierarchyNamespace(), serverProperties.getContentHierarchyTable(), propertyMap, null);

            String contentHierarchyStr = (String) contentHierarchyDetails.get(0).get(Constants.HIERARCHY);
            Map<String, Object> contentHierarchyObj = mapper.readValue(contentHierarchyStr, HashMap.class);
            String courseProvider = (String) contentHierarchyObj.get(Constants.SOURCE);
            String courseName = (String) contentHierarchyObj.get(Constants.NAME);
            String coursePosterImage = (String) contentHierarchyObj.get(Constants.POSTER_IMAGE);


            Resource resource = resourceLoader.getResource("classpath:certificate-kafka-json.json");
            InputStream inputStream = resource.getInputStream();
            JsonNode jsonNode = mapper.readTree(inputStream);

            Map<String, Object> certificateRequest = new HashMap<>();
            certificateRequest.put(Constants.USER_ID, decryptionService.decryptData(email));
            certificateRequest.put(Constants.ASSESSMENT_ID_KEY, assessmentId);
            certificateRequest.put(Constants.COURSE_ID, contextId);
            certificateRequest.put(Constants.COMPLETION_DATE, completionDate);
            certificateRequest.put(Constants.PROVIDER_NAME, courseProvider);
            certificateRequest.put(Constants.COURSE_NAME, courseName);
            certificateRequest.put(Constants.COURSE_POSTER_IMAGE, coursePosterImage);
            certificateRequest.put(Constants.RECIPIENT_NAME, recipientName);
            kafkaCertificateProducerService.replacePlaceholders(jsonNode, certificateRequest);
            String jsonNodeStr = mapper.writeValueAsString(jsonNode);
            producer.push(serverProperties.getKafkaTopicsPublicAssessmentCertificate(), jsonNodeStr);
        }catch (Exception e){
            logger.error("Failed to send kafka message: ", e);
        }
    }

    private Map<String, Object> calculateSectionFinalResults(List<Map<String, Object>> sectionLevelResults, long assessmentStartTime, long assessmentCompletionTime, int maxAssessmentRetakeAttempts, int overallPassPercentage) {
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
            res.put(Constants.TIME_TAKEN_FOR_ASSESSMENT, assessmentCompletionTime - assessmentStartTime);
            res.put(Constants.MAX_ASSESSMENT_RETAKE_ATTEMPTS, maxAssessmentRetakeAttempts);
            double totalPercentage = (totalSectionMarks / (double) totalMarks) * 100;
            res.put(Constants.PASS, totalPercentage >= overallPassPercentage);
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


    /**
     * Reads an assessment.
     *
     * @param editMode Whether the assessment is being read in edit mode.
     * @return The response from the assessment read.
     */
    @Override
    public SBApiResponse readAssessment(Boolean editMode, Map<String, Object> requestBody){
        logger.info("AssessmentServicev5Impl::readAssessment... Started");
        SBApiResponse outgoingResponse = ProjectUtil.createDefaultResponse(Constants.API_READ_ASSESSMENT);
        String errMsg = "";
        try {
            String assessmentIdentifier = (String) requestBody.get(Constants.ASSESSMENT_IDENTIFIER);

            // Step-1 : Read assessment using assessment Id from the Assessment Service
            Map<String, Object> assessmentAllDetail = readAssessment(assessmentIdentifier, editMode);
            //Step-2 : If assessment is empty throw validation error
            if (MapUtils.isEmpty(assessmentAllDetail)) {
                updateErrorDetails(outgoingResponse, Constants.ASSESSMENT_HIERARCHY_READ_FAILED,
                        HttpStatus.INTERNAL_SERVER_ERROR);
                return outgoingResponse;
            }
            //Step-3 : If assessment is practice question set send back the response
            if (Constants.PRACTICE_QUESTION_SET
                    .equalsIgnoreCase((String) assessmentAllDetail.get(Constants.PRIMARY_CATEGORY)) || editMode) {
                outgoingResponse.getResult().put(Constants.QUESTION_SET, readAssessmentLevelData(assessmentAllDetail));
                return outgoingResponse;
            }

            //Step-4 : If primary category is course assessment then email validation is done
            if (assessmentAllDetail.get(Constants.PRIMARY_CATEGORY).equals(Constants.COURSE_ASSESSMENT)) {
                String email = (String) requestBody.get(Constants.EMAIL);
                String name = (String) requestBody.get(Constants.NAME);
                String contextId = (String) requestBody.get(Constants.CONTEXT_ID);
                logger.info(String.format("ReadAssessment... UserId: %s, AssessmentIdentifier: %s", email, assessmentIdentifier));

                SBApiResponse errResponse = validateCoursePublicAssessmentPayload(requestBody);
                if(!ObjectUtils.isEmpty(errResponse)){
                    return errResponse;
                }
                return handleAssessmentReadForPublicAssessments(email, name, contextId, assessmentIdentifier, assessmentAllDetail);
            }


        } catch (Exception e) {
            errMsg = String.format("Error while reading assessment. Exception: %s", e.getMessage());
            logger.error(errMsg, e);
        }
        if (StringUtils.isNotBlank(errMsg)) {
            updateErrorDetails(outgoingResponse, errMsg, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return outgoingResponse;
    }

    private Map<String, Object> readAssessmentLevelData(Map<String, Object> assessmentAllDetail) {
        List<String> assessmentParams = serverProperties.getAssessmentLevelParams();
        Map<String, Object> assessmentFilteredDetail = new HashMap<>();
        for (String assessmentParam : assessmentParams) {
            if ((assessmentAllDetail.containsKey(assessmentParam))) {
                assessmentFilteredDetail.put(assessmentParam, assessmentAllDetail.get(assessmentParam));
            }
        }
        readSectionLevelParams(assessmentAllDetail, assessmentFilteredDetail);
        return assessmentFilteredDetail;
    }

    private void readSectionLevelParams(Map<String, Object> assessmentAllDetail,
                                        Map<String, Object> assessmentFilteredDetail) {
        List<Map<String, Object>> sectionResponse = new ArrayList<>();
        List<String> sectionIdList = new ArrayList<>();
        List<String> sectionParams = serverProperties.getAssessmentSectionParams();
        List<Map<String, Object>> sections = (List<Map<String, Object>>) assessmentAllDetail.get(Constants.CHILDREN);
        String assessmentType = (String) assessmentAllDetail.get(Constants.ASSESSMENT_TYPE);
        for (Map<String, Object> section : sections) {
            sectionIdList.add((String) section.get(Constants.IDENTIFIER));
            Map<String, Object> newSection = new HashMap<>();
            for (String sectionParam : sectionParams) {
                if (section.containsKey(sectionParam)) {
                    newSection.put(sectionParam, section.get(sectionParam));
                }
            }
            List<Map<String, Object>> questions = (List<Map<String, Object>>) section.get(Constants.CHILDREN);
            List<String> childNodeList;
            if (assessmentType.equalsIgnoreCase(Constants.QUESTION_WEIGHTAGE)) {
                List<Map<String, Object>> selectedQuestionsList = processRandomizationForQuestions((Map<String, Map<String, Object>>) section.get(Constants.SECTION_LEVEL_DEFINITION), questions);
                childNodeList = selectedQuestionsList.stream()
                        .map(question -> (String) question.get(Constants.IDENTIFIER))
                        .collect(toList());
            } else {
                int maxQuestions = (int) section.getOrDefault(Constants.MAX_QUESTIONS, questions.size());
                List<Map<String, Object>> shuffledQuestionsList = shuffleQuestions(questions);
                childNodeList = shuffledQuestionsList.stream()
                        .map(question -> (String) question.get(Constants.IDENTIFIER))
                        .limit(maxQuestions)
                        .collect(toList());
            }
            Collections.shuffle(childNodeList);
            newSection.put(Constants.CHILD_NODES, childNodeList);
            sectionResponse.add(newSection);
        }
        assessmentFilteredDetail.put(Constants.CHILDREN, sectionResponse);
        assessmentFilteredDetail.put(Constants.CHILD_NODES, sectionIdList);
    }

    /**
     * Process randomization for selecting questions based on section level definitions and limits.
     *
     * @param sectionLevelDefinitionMap Map containing section level definitions with 'noOfQuestions' and 'noOfMaxQuestions'.
     * @param questions                 List of questions to be processed.
     * @return List of selected questions based on randomization and limits.
     */
    private List<Map<String, Object>> processRandomizationForQuestions(Map<String, Map<String, Object>> sectionLevelDefinitionMap, List<Map<String, Object>> questions) {
        List<Map<String, Object>> shuffledQuestionsList = shuffleQuestions(questions);
        List<Map<String, Object>> selectedQuestionsList = new ArrayList<>();
        Map<String, Integer> noOfQuestionsMap = new HashMap<>();
        Map<String, Integer> dupNoOfQuestionsMap = new HashMap<>();     // Duplicate map for tracking selected questions
        boolean result = sectionLevelDefinitionMap.values().stream()
                .anyMatch(proficiencyMap -> {
                    Object maxNoOfQuestionsValue = proficiencyMap.get(Constants.NO_OF_QUESTIONS);
                    if (maxNoOfQuestionsValue instanceof Integer) {
                        return (Integer) maxNoOfQuestionsValue > 0;
                    }
                    return false;
                });

        if (!result) {
            return questions;
        } else {
            // Populate noOfQuestionsMap and noOfMaxQuestionsMap from sectionLevelDefinitionMap
            sectionLevelDefinitionMap.forEach((sectionLevelDefinitionKey, proficiencyMap) -> proficiencyMap.forEach((key, value) -> {
                if (key.equalsIgnoreCase(Constants.NO_OF_QUESTIONS)) {
                    noOfQuestionsMap.put(sectionLevelDefinitionKey, (Integer) value);
                    dupNoOfQuestionsMap.put(sectionLevelDefinitionKey, 0);
                }
            }));

            // Process each question for randomization and limit checking
            for (Map<String, Object> question : shuffledQuestionsList) {
                String questionLevel = (String) question.get(Constants.QUESTION_LEVEL);
                // Check if adding one more question of this level is within limits
                if (dupNoOfQuestionsMap.getOrDefault(questionLevel, 0) < noOfQuestionsMap.getOrDefault(questionLevel, 0)) {
                    // Add the question to selected list
                    selectedQuestionsList.add(question);
                    // Update dupNoOfQuestionsMap to track the count of selected questions for this level
                    dupNoOfQuestionsMap.put(questionLevel, dupNoOfQuestionsMap.getOrDefault(questionLevel, 0) + 1);
                }
            }
            return selectedQuestionsList;
        }
    }

    /**
     * Shuffles the list of questions maps.
     *
     * @param questions The list of questions maps to be shuffled.
     * @return A new list containing the shuffled questions maps.
     */
    public static List<Map<String, Object>> shuffleQuestions(List<Map<String, Object>> questions) {
        // Create a copy of the original list to avoid modifying the input list
        List<Map<String, Object>> shuffledQnsList = new ArrayList<>(questions);
        // Shuffle the list using Collections.shuffle()
        Collections.shuffle(shuffledQnsList);
        return shuffledQnsList;
    }

    /**
     * Reads a list of questions.
     *
     * @param requestBody The request body containing the question list parameters.
     * @param editMode        Whether the question list is being read in edit mode.
     * @return The response from the question list read.
     */
    @Override
    public SBApiResponse readQuestionList(@Valid Map<String, Object> requestBody, Boolean editMode) {
        SBApiResponse response = ProjectUtil.createDefaultResponse(Constants.API_QUESTIONS_LIST);
        String errMsg;
        Map<String, String> result = new HashMap<>();
        try {

            List<String> identifierList = new ArrayList<>();
            List<Object> questionList = new ArrayList<>();
            String contextId = (String) requestBody.get(Constants.CONTEXT_ID);
            String email = (String) requestBody.get(Constants.EMAIL);
            String assessmentIdFromRequest = (String) requestBody.get(Constants.ASSESSMENT_IDENTIFIER);

            //Step-1 : Validation for QuestionList requestBody
            SBApiResponse errResponse = validateQuestionListAPI(requestBody, editMode);
            if(!ObjectUtils.isEmpty(errResponse)){
                return errResponse;
            }

            Map<String, Object> userAssessmentAllDetail = new HashMap<String, Object>();
            String encryptedEmail = encryptionService.encryptData(email);
            identifierList.addAll(getQuestionIdList(requestBody));

            String questionSetFromAssessmentString = fetchAssessmentMetaDataResponse(encryptedEmail, assessmentIdFromRequest, contextId);
            if (StringUtils.isNotBlank(questionSetFromAssessmentString)) {
                userAssessmentAllDetail.putAll(mapper.readValue(questionSetFromAssessmentString,
                        new TypeReference<Map<String, Object>>() {
                        }));
            } else {
                response.put(Constants.ERROR_MESSAGE, Constants.USER_ASSESSMENT_DATA_NOT_PRESENT);
                return response;
            }

            if (!MapUtils.isEmpty(userAssessmentAllDetail)) {
                result.put(Constants.PRIMARY_CATEGORY, (String) userAssessmentAllDetail.get(Constants.PRIMARY_CATEGORY));
                List<String> questionsFromAssessment = new ArrayList<>();
                List<Map<String, Object>> sections = (List<Map<String, Object>>) userAssessmentAllDetail
                        .get(Constants.CHILDREN);
                for (Map<String, Object> section : sections) {
                    // Out of the list of questions received in the payload, checking if the request
                    // has only those ids which are a part of the user's latest assessment
                    // Fetching all the remaining questions details from the Redis
                    questionsFromAssessment.addAll((List<String>) section.get(Constants.CHILD_NODES));
                }
                if (!validateQuestionListRequest(identifierList, questionsFromAssessment)) {
                    response.put(Constants.ERROR_MESSAGE, Constants.THE_QUESTIONS_IDS_PROVIDED_DONT_MATCH);
                    return response;
                }
            } else {
                response.put(Constants.ERROR_MESSAGE, Constants.ASSESSMENT_ID_INVALID);
                return response;
            }

            Map<String, Object> questionsMap = assessUtilServ.readQListfromCache(identifierList, assessmentIdFromRequest, editMode);
            for (String questionId : identifierList) {
                questionList.add(assessUtilServ.filterQuestionMapDetailV2((Map<String, Object>) questionsMap.get(questionId),
                        result.get(Constants.PRIMARY_CATEGORY)));
            }
            if (identifierList.size() == questionList.size()) {
                response.getResult().put(Constants.QUESTIONS, questionList);
            }
        } catch (Exception e) {
            errMsg = String.format("Failed to fetch the question list. Exception: %s", e.getMessage());
            logger.error(errMsg, e);
            response.put(Constants.ERROR_MESSAGE, errMsg);
            return response;
        }
        return response;
    }

    private SBApiResponse validateQuestionListAPI(Map<String, Object> requestBody, boolean editMode) throws IOException {
        return validateQuestionListPayload(requestBody,editMode);

    }

    private List<String> getQuestionIdList(Map<String, Object> questionListRequest) {
        try {
            if (questionListRequest.containsKey(Constants.REQUEST)) {
                Map<String, Object> request = (Map<String, Object>) questionListRequest.get(Constants.REQUEST);
                if ((!ObjectUtils.isEmpty(request)) && request.containsKey(Constants.SEARCH)) {
                    Map<String, Object> searchObj = (Map<String, Object>) request.get(Constants.SEARCH);
                    if (!ObjectUtils.isEmpty(searchObj) && searchObj.containsKey(Constants.IDENTIFIER)
                            && !CollectionUtils.isEmpty((List<String>) searchObj.get(Constants.IDENTIFIER))) {
                        return (List<String>) searchObj.get(Constants.IDENTIFIER);
                    }
                }
            }
        } catch (Exception e) {
            logger.error(String.format("Failed to process the questionList request body. %s", e.getMessage()));
        }
        return Collections.emptyList();
    }

    private Boolean validateQuestionListRequest(List<String> identifierList, List<String> questionsFromAssessment) {
        return (new HashSet<>(questionsFromAssessment).containsAll(identifierList)) ? Boolean.TRUE : Boolean.FALSE;
    }

    public SBApiResponse readAssessmentResultV5(Map<String, Object> request) {
        SBApiResponse response = ProjectUtil.createDefaultResponse(Constants.API_READ_ASSESSMENT_RESULT);
        try {
            Map<String, Object> requestMap = (Map<String, Object>) request.get(Constants.REQUEST);
            String email = (String) requestMap.get(Constants.EMAIL);

            if(!ProjectUtil.validateEmailPattern(email)){
                updateErrorDetails(response, Constants.INVALID_EMAIL, HttpStatus.BAD_REQUEST);
                return response;
            }
            email = encryptionService.encryptData(email);
            String errMsg = validateAssessmentReadResult(request);
            if (StringUtils.isNotBlank(errMsg)) {
                updateErrorDetails(response, errMsg, HttpStatus.BAD_REQUEST);
                return response;
            }

            Map<String, Object> requestBody = (Map<String, Object>) request.get(Constants.REQUEST);
            String assessmentIdentifier = (String) requestBody.get(Constants.ASSESSMENT_IDENTIFIER);
            String contextId = (String) requestBody.get(Constants.CONTEXT_ID);
            List<Map<String, Object>> existingDataList = assessUtilServ.readUserSubmittedAssessmentRecords(
                    email, assessmentIdentifier, contextId);

            if (existingDataList.isEmpty()) {
                updateErrorDetails(response, Constants.USER_ASSESSMENT_DATA_NOT_PRESENT, HttpStatus.BAD_REQUEST);
                return response;
            }

            String statusOfLatestObject = (String) existingDataList.get(0).get(Constants.STATUS);
            if (!Constants.SUBMITTED.equalsIgnoreCase(statusOfLatestObject)) {
                response.getResult().put(Constants.STATUS_IS_IN_PROGRESS, true);
                return response;
            }

            String latestResponse = (String) existingDataList.get(0).get(Constants.SUBMIT_ASSESSMENT_RESPONSE_KEY);
            if (StringUtils.isNotBlank(latestResponse)) {
                response.putAll(mapper.readValue(latestResponse, new TypeReference<Map<String, Object>>() {
                }));
            }
        } catch (Exception e) {
            String errMsg = String.format("Failed to process Assessment read response. Excption: %s", e.getMessage());
            updateErrorDetails(response, errMsg, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    private String validateAssessmentReadResult(Map<String, Object> request) {
        String errMsg = "";
        if (MapUtils.isEmpty(request) || !request.containsKey(Constants.REQUEST)) {
            return Constants.INVALID_REQUEST;
        }

        Map<String, Object> requestBody = (Map<String, Object>) request.get(Constants.REQUEST);
        if (MapUtils.isEmpty(requestBody)) {
            return Constants.INVALID_REQUEST;
        }
        List<String> missingAttribs = new ArrayList<String>();
        if (!requestBody.containsKey(Constants.ASSESSMENT_IDENTIFIER)
                || StringUtils.isBlank((String) requestBody.get(Constants.ASSESSMENT_IDENTIFIER))) {
            missingAttribs.add(Constants.ASSESSMENT_IDENTIFIER);
        }

        if (!requestBody.containsKey(Constants.CONTEXT_ID)
                || StringUtils.isBlank((String) requestBody.get(Constants.CONTEXT_ID))) {
            missingAttribs.add(Constants.CONTEXT_ID);
        }

        if (!missingAttribs.isEmpty()) {
            errMsg = "One or more mandatory fields are missing in Request. Mandatory fields are : "
                    + missingAttribs.toString();
        }
        Map<String, Object> assessmentHierarchy = readAssessment((String) request.get(Constants.ASSESSMENT_IDENTIFIER), false);
        // validate the hierachy

        return errMsg;
    }

    private SBApiResponse handleAssessmentReadForPublicAssessments(String email, String name, String contextId, String assessmentIdentifier, Map<String, Object> assessmentAllDetail){

        SBApiResponse outgoingResponse = ProjectUtil.createDefaultResponse(Constants.API_READ_ASSESSMENT);
        String errMsg = "";

        //step-4.2 : encryption of the email
        email = encryptionService.encryptData(email);
        // Step-4.3 : check for user assessment record from cassandra
        List<Map<String, Object>> existingDataList = assessUtilServ.readUserSubmittedAssessmentRecords(
                email, assessmentIdentifier, contextId);
        Timestamp assessmentStartTime = new Timestamp(new Date().getTime());

        //Step-4.4 : If user assessment does not exist, making a fresh entry in cassandra
        if (existingDataList.isEmpty()) {
            logger.info("Assessment read first time for user.");
            // Add Null check for expectedDuration.throw bad questionSet Assessment Exam
            if (null == assessmentAllDetail.get(Constants.EXPECTED_DURATION)) {
                errMsg = Constants.ASSESSMENT_INVALID;
                updateErrorDetails(outgoingResponse, errMsg,
                        HttpStatus.INTERNAL_SERVER_ERROR);
                return outgoingResponse;
            } else {
                Boolean isAssessmentUpdatedToDB = addAssessmentDataToDB(email, name, contextId, assessmentIdentifier, assessmentAllDetail, assessmentStartTime, outgoingResponse);
                if (Boolean.FALSE.equals(isAssessmentUpdatedToDB)) {
                    errMsg = Constants.ASSESSMENT_DATA_START_TIME_NOT_UPDATED;
                    updateErrorDetails(outgoingResponse, errMsg,
                            HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }
        } else {
            logger.info("Assessment read... user has details... ");
            Date existingAssessmentEndTime = (Date) (existingDataList.get(0)
                    .get(Constants.END_TIME));
            Timestamp existingAssessmentEndTimeTimestamp = new Timestamp(
                    existingAssessmentEndTime.getTime());

            if (assessmentStartTime.compareTo(existingAssessmentEndTimeTimestamp) < 0
                    && Constants.NOT_SUBMITTED.equalsIgnoreCase((String) existingDataList.get(0).get(Constants.STATUS))) {
                String questionSetFromAssessmentString = (String) existingDataList.get(0)
                        .get(Constants.ASSESSMENT_READ_RESPONSE_KEY);
                Map<String, Object> questionSetFromAssessment = new Gson().fromJson(
                        questionSetFromAssessmentString, new TypeToken<HashMap<String, Object>>() {
                        }.getType());
                questionSetFromAssessment.put(Constants.START_TIME, assessmentStartTime.getTime());
                questionSetFromAssessment.put(Constants.END_TIME,
                        existingAssessmentEndTimeTimestamp.getTime());
                outgoingResponse.getResult().put(Constants.QUESTION_SET, questionSetFromAssessment);
            } else if (((String) existingDataList.get(0).get(Constants.STATUS)).equalsIgnoreCase(Constants.SUBMITTED) && !(Boolean) existingDataList.get(0).get(Constants.PASS_STATUS)) {
                Boolean isAssessmentUpdatedToDB = addAssessmentDataToDB(email, name, contextId, assessmentIdentifier, assessmentAllDetail, assessmentStartTime, outgoingResponse);
                if (Boolean.FALSE.equals(isAssessmentUpdatedToDB)) {
                    errMsg = Constants.ASSESSMENT_DATA_START_TIME_NOT_UPDATED;
                    updateErrorDetails(outgoingResponse, errMsg,
                            HttpStatus.INTERNAL_SERVER_ERROR);
                }
            } else if (((String) existingDataList.get(0).get(Constants.STATUS)).equalsIgnoreCase(Constants.SUBMITTED)  && (Boolean) existingDataList.get(0).get(Constants.PASS_STATUS)) {
                outgoingResponse.getResult().put(Constants.RESPONSE, "User has already submitted the assessment");
                outgoingResponse.getParams().setStatus(Constants.SUCCESS);
                outgoingResponse.setResponseCode(HttpStatus.OK);
                return outgoingResponse;
            }
        }

        return outgoingResponse;
    }

    private Boolean addAssessmentDataToDB(String email, String name, String contextId, String assessmentIdentifier, Map<String, Object> assessmentAllDetail, Timestamp assessmentStartTime, SBApiResponse outgoingResponse) {
        int expectedDuration = (Integer) assessmentAllDetail.get(Constants.EXPECTED_DURATION);
        Timestamp assessmentEndTime = calculateAssessmentSubmitTime(expectedDuration,
                assessmentStartTime, 0);
        Map<String, Object> assessmentData = readAssessmentLevelData(assessmentAllDetail);
        assessmentData.put(Constants.START_TIME, assessmentStartTime.getTime());
        assessmentData.put(Constants.END_TIME, assessmentEndTime.getTime());
        outgoingResponse.getResult().put(Constants.QUESTION_SET, assessmentData);
        return assessmentRepository.addUserAssesmentDataToDB(email,
                assessmentIdentifier, assessmentStartTime, assessmentEndTime,
                (Map<String, Object>) (outgoingResponse.getResult().get(Constants.QUESTION_SET)),
                Constants.NOT_SUBMITTED, name, contextId);
    }

    private SBApiResponse validateCoursePublicAssessmentPayload(Map<String, Object> requestBody) {

        SBApiResponse outgoingResponse = ProjectUtil.createDefaultResponse(Constants.API_READ_ASSESSMENT);
        String email = (String) requestBody.get(Constants.EMAIL);
        String name = (String) requestBody.get(Constants.NAME);
        String assessmentIdentifier = (String) requestBody.get(Constants.ASSESSMENT_IDENTIFIER);
        String contextId = (String) requestBody.get(Constants.CONTEXT_ID);

        if (StringUtils.isEmpty(email) || StringUtils.isEmpty(name) || StringUtils.isEmpty(assessmentIdentifier) || StringUtils.isEmpty(contextId)) {
            updateErrorDetails(outgoingResponse, Constants.INVALID_REQUEST, HttpStatus.BAD_REQUEST);
            return outgoingResponse;
        }
        //Step-4.1 email validation step
        if (!ProjectUtil.validateEmailPattern(email)) {
            updateErrorDetails(outgoingResponse, Constants.INVALID_EMAIL, HttpStatus.BAD_REQUEST);
            return outgoingResponse;
        }
        return null;
    }

    private Map<String, Object> readAssessment(String assessmentIdentifier, boolean editMode) {
        if (editMode) {
            return assessUtilServ.fetchHierarchyFromAssessServc(assessmentIdentifier);
        }
        return assessUtilServ
                .readAssessmentHierarchyFromCache(assessmentIdentifier, editMode);

    }

    private SBApiResponse validateQuestionListPayload(Map<String, Object> requestBody, boolean editMode){

        SBApiResponse response = ProjectUtil.createDefaultResponse(Constants.API_QUESTIONS_LIST);

        String assessmentIdentifier = (String) requestBody.get(Constants.ASSESSMENT_IDENTIFIER);
        if (StringUtils.isBlank(assessmentIdentifier)) {
            response.put(Constants.ERROR_MESSAGE, Constants.ASSESSMENT_ID_KEY_IS_NOT_PRESENT_IS_EMPTY);
            return response;
        }
        // Step-1 : Read assessment using assessment Id from the Assessment Service
        Map<String, Object> assessmentAllDetail = readAssessment(assessmentIdentifier, editMode);
        //Step-2 : If assessment is empty throw validation error
        if (MapUtils.isEmpty(assessmentAllDetail)) {
            updateErrorDetails(response, Constants.ASSESSMENT_HIERARCHY_READ_FAILED,
                    HttpStatus.INTERNAL_SERVER_ERROR);
            return response;
        }
        String primaryCategory = (String) assessmentAllDetail.get(Constants.PRIMARY_CATEGORY);
        if (Constants.PRACTICE_QUESTION_SET
                .equalsIgnoreCase(primaryCategory) || editMode) {
            response.put(Constants.PRIMARY_CATEGORY, primaryCategory);
            response.put(Constants.ERROR_MESSAGE, StringUtils.EMPTY);
            return response;
        }

        List<String> identifierList = new ArrayList<>();
        identifierList.addAll(getQuestionIdList(requestBody));
        if (identifierList.isEmpty()) {
            response.put(Constants.ERROR_MESSAGE, Constants.IDENTIFIER_LIST_IS_EMPTY);
            return response;
        }
        return null;
    }

    private String fetchAssessmentMetaDataResponse(String encryptedEmail, String assessmentIdFromRequest, String contextId) {
        List<Map<String, Object>> existingDataList = assessUtilServ.readUserSubmittedAssessmentRecords(
                encryptedEmail, assessmentIdFromRequest, contextId);
        return (!existingDataList.isEmpty())
                ? (String) existingDataList.get(0).get(Constants.ASSESSMENT_READ_RESPONSE_KEY)
                : "";

    }

    private SBApiResponse validateSubmitAssessmentPayload(String email, String assessmentIdFromRequest, boolean editMode){

        SBApiResponse response = ProjectUtil.createDefaultResponse(Constants.API_SUBMIT_ASSESSMENT);

        if (StringUtils.isEmpty(assessmentIdFromRequest)) {
            updateErrorDetails(response, Constants.INVALID_ASSESSMENT_ID,
                    HttpStatus.INTERNAL_SERVER_ERROR);
            return response;
        }

        Map<String, Object> assessmentHierarchyRead = readAssessment(assessmentIdFromRequest, editMode);
        //Step-2 : If assessment is empty throw validation error
        if (MapUtils.isEmpty(assessmentHierarchyRead)) {
            updateErrorDetails(response, Constants.ASSESSMENT_HIERARCHY_READ_FAILED,
                    HttpStatus.INTERNAL_SERVER_ERROR);
            return response;
        }
        //Step-3 : If assessment is practice question set send back the response
        if (Constants.COURSE_ASSESSMENT
                .equalsIgnoreCase((String) assessmentHierarchyRead.get(Constants.PRIMARY_CATEGORY)) || editMode) {

            if (!ProjectUtil.validateEmailPattern(email)) {
                updateErrorDetails(response, Constants.INVALID_EMAIL, HttpStatus.BAD_REQUEST);
                return response;
            }
        }
        return null;
    }
  
    @Override
    public void processNotification (Map<String, Object> request) {
        logger.info("kafka notification processing started");
        try {
            logger.info("Processing notification request: " + mapper.writeValueAsString(request));
            String error = validateAssessmentRequest(request);
            if (StringUtils.isNotBlank(error)) {
                logger.info(error);
                return ;
            }
            Map<String, Object> edata = (Map<String, Object>) request.get(Constants.E_DATA);
            Map<String, Object> related = (Map<String, Object>)edata.get(Constants.RELATED);
            Map<String, Object> propertyMap = new HashMap<>();
            /*String email = encryptionService.encryptData((String) edata.get(Constants.USER_ID));
            propertyMap.put(Constants.USER_ID, email);
            propertyMap.put(Constants.ASSESSMENT_ID_KEY,request.get(Constants.ASSESSMENT_IDENTIFIER));
            propertyMap.put(Constants.CONTEXT_ID, related.get(Constants.COURSE_ID));
            List<Map<String, Object>> cassandraResponse = cassandraOperation.getRecordsByPropertiesWithoutFiltering(Constants.SUNBIRD_KEY_SPACE_NAME, serverProperties.getPublicUserAssessmentData(), propertyMap,null, null);
            String userValidationResponse = validateUserAssementData(cassandraResponse);
            if (StringUtils.isNotBlank(userValidationResponse)) {
                logger.info(userValidationResponse);
                return response;
            }*/

            Map<String, Object> mailNotificationDetails = new HashMap<>();
            mailNotificationDetails.put(Constants.RECIPIENT_EMAILS, Collections.singletonList((String)edata.get(Constants.USER_ID)));
            mailNotificationDetails.put(Constants.COURSE_NAME, edata.get(Constants.COURSE_NAME));
            mailNotificationDetails.put(Constants.COURSE_POSTER_IMAGE_URL, edata.get(Constants.COURSE_POSTER_IMAGE));
            mailNotificationDetails.put(Constants.SUBJECT,"Completion certificate");
            sendAssessmentNotification(mailNotificationDetails);
            logger.info("assessment notification sent successfully");
        }catch (Exception e){
            logger.error("failed to send the assessment notification :: " + e);
        }
    }

    private void sendAssessmentNotification(Map<String, Object> mailNotificationDetails) {
        Map<String, Object> params = new HashMap<>();
        NotificationAsyncRequest notificationRequest = new NotificationAsyncRequest();
        Map<String, Object> action = new HashMap<>();
        Map<String, Object> templ = new HashMap<>();
        Map<String, Object> usermap = new HashMap<>();
        params.put(Constants.COURSE_NAME, mailNotificationDetails.get(Constants.COURSE_NAME));
        params.put(Constants.COURSE_POSTER_IMAGE_KEY, mailNotificationDetails.get(Constants.COURSE_POSTER_IMAGE_URL));
        //params.put(Constants.CERTIFICATE_LINK, mailNotificationDetails.get(Constants.CERTIFICATE_LINK));
        Template template = new Template(constructEmailTemplate(serverProperties.getPublicAssessmentCertificateTemplate(), params), serverProperties.getPublicAssessmentCertificateTemplate(), params);
        usermap.put(Constants.ID, "");
        usermap.put(Constants.TYPE, Constants.USER);
        action.put(Constants.TEMPLATE, templ);
        action.put(Constants.TYPE, Constants.EMAIL);
        action.put(Constants.CATEGORY, Constants.EMAIL);
        action.put(Constants.CREATED_BY, usermap);
        Config config = new Config();
        config.setSubject((String) mailNotificationDetails.get(Constants.SUBJECT));
        config.setSender(serverProperties.getSupportEmail());
        templ.put(Constants.TYPE, Constants.EMAIL);
        templ.put(Constants.DATA, template.getData());
        templ.put(Constants.ID, template.getId());
        templ.put(Constants.PARAMS, params);
        templ.put(Constants.CONFIG, config);
        notificationRequest.setType(Constants.EMAIL);
        notificationRequest.setPriority(1);
        notificationRequest.setIds((List<String>) mailNotificationDetails.get(Constants.RECIPIENT_EMAILS));
        notificationRequest.setAction(action);

        Map<String, Object> req = new HashMap<>();
        Map<String, List<NotificationAsyncRequest>> notificationMap = new HashMap<>();
        notificationMap.put(Constants.NOTIFICATIONS, Collections.singletonList(notificationRequest));
        req.put(Constants.REQUEST, notificationMap);
        sendNotification(req);
    }

    private String constructEmailTemplate(String templateName, Map<String, Object> params) {
        String replacedHTML = new String();
        try {
            Map<String, Object> propertyMap = new HashMap<>();
            propertyMap.put(Constants.NAME, templateName);
            List<Map<String, Object>> templateMap = cassandraOperation.getRecordsByProperties(Constants.KEYSPACE_SUNBIRD, Constants.TABLE_EMAIL_TEMPLATE, propertyMap, Collections.singletonList(Constants.TEMPLATE));
            String htmlTemplate = templateMap.stream()
                    .findFirst()
                    .map(template -> (String) template.get(Constants.TEMPLATE))
                    .orElse(null);
            VelocityEngine velocityEngine = new VelocityEngine();
            velocityEngine.init();
            VelocityContext context = new VelocityContext();
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                context.put(entry.getKey(), entry.getValue());
            }
            StringWriter writer = new StringWriter();
            velocityEngine.evaluate(context, writer, "HTMLTemplate", htmlTemplate);
            replacedHTML = writer.toString();
        } catch (Exception e) {
            logger.error("Unable to create template ", e);
        }
        return replacedHTML;
    }

    public String validateAssessmentRequest(Map<String,Object> request){
        StringBuffer str = new StringBuffer();
        List<String> errList = new ArrayList<>();

        if (ObjectUtils.isEmpty(request)) {
            str.append("Request object is empty.");
            return str.toString();
        }
        if (StringUtils.isBlank((String) request.get(Constants.ASSESSMENT_ID_KEY))) {
            errList.add(Constants.ASSESSMENT_ID_KEY);
            return str.append("Failed to Due To Missing Params - ").append(errList).append(".").toString();
        }
        Map<String, Object> edata = (Map<String, Object>) request.get(Constants.E_DATA);

        if (StringUtils.isEmpty((String) edata.get(Constants.USER_ID))) {
            errList.add(Constants.USER_ID);
            return str.append("Failed to Due To Missing Params - ").append(errList).append(".").toString();
        }
        Map<String, Object> related = (Map<String, Object>)edata.get(Constants.RELATED);
        if (StringUtils.isEmpty((String) related.get(Constants.COURSE_ID))) {
            errList.add(Constants.COURSE_ID);
            return str.append("Failed to Due To Missing Params - ").append(errList).append(".").toString();
        }
        if (!errList.isEmpty()) {
            str.append("Failed to Due To Missing Params - ").append(errList).append(".");
        }
        return str.toString();
    }

    private void sendNotification(Map<String, Object> request) {
        StringBuilder builder = new StringBuilder();
        builder.append(serverProperties.getNotifyServiceHost()).append(serverProperties.getNotifyServicePathAsync());
        try {
            logger.info(mapper.writeValueAsString(request));
            Map<String, Object> response = outboundRequestHandlerService.fetchResultUsingPost(builder.toString(), request, null);
            logger.debug("The email notification is successfully sent, response is: " + response);
        } catch (Exception e) {
            logger.error("Exception while posting the data in notification service: ", e);
        }
    }

    private String validateUserAssementData(List<Map<String, Object>> userAssessmentData) {
        String error = "";

        if (CollectionUtils.isEmpty(userAssessmentData)) {
            error = "User assessment data not found";
            return error;
        }
        Map<String, Object> userAssessmentDataMap =userAssessmentData.get(0);
        if(Boolean.FALSE.equals(userAssessmentDataMap.get(Constants.PASS_STATUS))){
            error = "assessment is not passed";
            return error;
        }
        return error;
    }

}