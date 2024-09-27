package com.assessment.service.impl;

import com.assessment.datasecurity.EncryptionService;
import com.assessment.model.SBApiResponse;
import com.assessment.repo.AssessmentRepository;
import com.assessment.service.AssessmentService;
import com.assessment.service.AssessmentUtilServiceV2;
import com.assessment.util.Constants;
import com.assessment.util.ProjectUtil;
import com.assessment.util.ServerProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;


import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

import static com.assessment.util.ProjectUtil.updateErrorDetails;
import static java.util.stream.Collectors.toList;

@Service
public class AssessmentServiceImpl implements AssessmentService {

    private final Logger logger = LoggerFactory.getLogger(AssessmentServiceImpl.class);

    @Autowired
    private AssessmentUtilServiceV2 assessmentUtilService;

    @Autowired
    private EncryptionService encryptionService;

    @Autowired
    ServerProperties serverProperties;

    @Autowired
    AssessmentRepository assessmentRepository;

    @Autowired
    ObjectMapper mapper;


    @Override
    public SBApiResponse readAssessment(String assessmentIdentifier, Boolean editMode, String email, String name) {
        logger.info("AssessmentServiceImpl::readAssessment... Started");
        SBApiResponse response = ProjectUtil.createDefaultResponse(Constants.API_READ_ASSESSMENT);
        String errMsg = "";
        try {

            if(!ProjectUtil.validateEmailPattern(email)){
                updateErrorDetails(response, Constants.INVALID_EMAIL, HttpStatus.BAD_REQUEST);
                return response;
            }
            String encryptedEmail = encryptionService.encryptData(email);

            logger.info(String.format("ReadAssessment... UserId: %s, AssessmentIdentifier: %s", email, assessmentIdentifier));

            Map<String, Object> assessmentAllDetail = null;

            if (editMode) {
                assessmentAllDetail = assessmentUtilService.fetchHierarchyFromAssessServc(assessmentIdentifier);
            } else {
                assessmentAllDetail = assessmentUtilService
                        .readAssessmentHierarchyFromCache(assessmentIdentifier, editMode);
            }

            if (MapUtils.isEmpty(assessmentAllDetail)) {
                updateErrorDetails(response, Constants.ASSESSMENT_HIERARCHY_READ_FAILED,
                        HttpStatus.INTERNAL_SERVER_ERROR);
                return response;
            }

            if (Constants.PRACTICE_QUESTION_SET
                    .equalsIgnoreCase((String) assessmentAllDetail.get(Constants.PRIMARY_CATEGORY)) || editMode) {
                response.getResult().put(Constants.QUESTION_SET, readAssessmentLevelData(assessmentAllDetail));
                return response;
            }

            List<Map<String, Object>> existingDataList = assessmentUtilService.readUserSubmittedAssessmentRecords(encryptedEmail, assessmentIdentifier);
            Timestamp assessmentStartTime = new Timestamp(new java.util.Date().getTime());
            if (existingDataList.isEmpty()) {
                logger.info("Assessment read first time for user.");
                // Add Null check for expectedDuration.throw bad questionSet Assessment Exam
                if (null == assessmentAllDetail.get(Constants.EXPECTED_DURATION)) {
                    errMsg = Constants.ASSESSMENT_INVALID;
                } else {
                    int expectedDuration = (Integer) assessmentAllDetail.get(Constants.EXPECTED_DURATION);
                    Timestamp assessmentEndTime = calculateAssessmentSubmitTime(expectedDuration,
                            assessmentStartTime, 0);
                    Map<String, Object> assessmentData = readAssessmentLevelData(assessmentAllDetail);
                    assessmentData.put(Constants.START_TIME, assessmentStartTime.getTime());
                    assessmentData.put(Constants.END_TIME, assessmentEndTime.getTime());
                    response.getResult().put(Constants.QUESTION_SET, assessmentData);
                    Boolean isAssessmentUpdatedToDB = assessmentRepository.addUserAssesmentDataToDB(encryptedEmail,
                            assessmentIdentifier, assessmentStartTime, assessmentEndTime,
                            (Map<String, Object>) (response.getResult().get(Constants.QUESTION_SET)),
                            Constants.NOT_SUBMITTED, name);
                    if (Boolean.FALSE.equals(isAssessmentUpdatedToDB)) {
                        errMsg = Constants.ASSESSMENT_DATA_START_TIME_NOT_UPDATED;
                    }
                }
            } else {
                logger.info("Assessment read... user has details... ");
                java.util.Date existingAssessmentEndTime = (java.util.Date) (existingDataList.get(0)
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
                    response.getResult().put(Constants.QUESTION_SET, questionSetFromAssessment);
                } else if ((assessmentStartTime.compareTo(existingAssessmentEndTime) < 0
                        && ((String) existingDataList.get(0).get(Constants.STATUS))
                        .equalsIgnoreCase(Constants.SUBMITTED))
                        || assessmentStartTime.compareTo(existingAssessmentEndTime) > 0) {
                    logger.info(
                            "Incase the assessment is submitted before the end time, or the endtime has exceeded, read assessment freshly ");
                    Map<String, Object> assessmentData = readAssessmentLevelData(assessmentAllDetail);
                    int expectedDuration = (Integer) assessmentAllDetail.get(Constants.EXPECTED_DURATION);
                    assessmentStartTime = new Timestamp(new java.util.Date().getTime());
                    Timestamp assessmentEndTime = calculateAssessmentSubmitTime(expectedDuration,
                            assessmentStartTime, 0);
                    assessmentData.put(Constants.START_TIME, assessmentStartTime.getTime());
                    assessmentData.put(Constants.END_TIME, assessmentEndTime.getTime());
                    response.getResult().put(Constants.QUESTION_SET, assessmentData);

                    Boolean isAssessmentUpdatedToDB = assessmentRepository.addUserAssesmentDataToDB(encryptedEmail,
                            assessmentIdentifier, assessmentStartTime, assessmentEndTime,
                            assessmentData, Constants.NOT_SUBMITTED, name);
                    if (Boolean.FALSE.equals(isAssessmentUpdatedToDB)) {
                        errMsg = Constants.ASSESSMENT_DATA_START_TIME_NOT_UPDATED;
                    }
                }
            }
        } catch (Exception e) {
            errMsg = String.format("Error while reading assessment. Exception: %s", e.getMessage());
            logger.error(errMsg, e);
        }
        if (StringUtils.isNotBlank(errMsg)) {
            updateErrorDetails(response, errMsg, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return response;
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

    private void readSectionLevelParams(Map<String, Object> assessmentAllDetail,
                                        Map<String, Object> assessmentFilteredDetail) {
        List<Map<String, Object>> sectionResponse = new ArrayList<>();
        List<String> sectionIdList = new ArrayList<>();
        List<String> sectionParams = serverProperties.getAssessmentSectionParams();
        List<Map<String, Object>> sections = (List<Map<String, Object>>) assessmentAllDetail.get(Constants.CHILDREN);
        for (Map<String, Object> section : sections) {
            sectionIdList.add((String) section.get(Constants.IDENTIFIER));
            Map<String, Object> newSection = new HashMap<>();
            for (String sectionParam : sectionParams) {
                if (section.containsKey(sectionParam)) {
                    newSection.put(sectionParam, section.get(sectionParam));
                }
            }
            List<Map<String, Object>> questions = (List<Map<String, Object>>) section.get(Constants.CHILDREN);
            // Shuffle the list of questions
            Collections.shuffle(questions);
            int maxQuestions = (int) section.getOrDefault(Constants.MAX_QUESTIONS, questions.size());
            List<String> childNodeList = questions.stream()
                    .map(question -> (String) question.get(Constants.IDENTIFIER))
                    .limit(maxQuestions)
                    .collect(Collectors.toList());
            Collections.shuffle(childNodeList);
            newSection.put(Constants.CHILD_NODES, childNodeList);
            sectionResponse.add(newSection);
        }
        assessmentFilteredDetail.put(Constants.CHILDREN, sectionResponse);
        assessmentFilteredDetail.put(Constants.CHILD_NODES, sectionIdList);
    }

    @Override
    public SBApiResponse readQuestionList(Map<String, Object> requestBody, String email, boolean editMode) {
        SBApiResponse response = ProjectUtil.createDefaultResponse(Constants.API_QUESTIONS_LIST);
        String errMsg;
        Map<String, String> result = new HashMap<>();
        try {
            List<String> identifierList = new ArrayList<>();
            List<Object> questionList = new ArrayList<>();
            result = validateQuestionListAPI(requestBody, email, identifierList, editMode);
            errMsg = result.get(Constants.ERROR_MESSAGE);
            if (StringUtils.isNotBlank(errMsg)) {
                updateErrorDetails(response, errMsg, HttpStatus.BAD_REQUEST);
                return response;
            }

            String assessmentIdFromRequest = (String) requestBody.get(Constants.ASSESSMENT_ID_KEY);
            Map<String, Object> questionsMap = assessmentUtilService.readQListfromCache(identifierList, assessmentIdFromRequest, editMode);
            for (String questionId : identifierList) {
                questionList.add(assessmentUtilService.filterQuestionMapDetail((Map<String, Object>) questionsMap.get(questionId),
                        result.get(Constants.PRIMARY_CATEGORY)));
            }
            if (errMsg.isEmpty() && identifierList.size() == questionList.size()) {
                response.getResult().put(Constants.QUESTIONS, questionList);
            }
        } catch (Exception e) {
            errMsg = String.format("Failed to fetch the question list. Exception: %s", e.getMessage());
            logger.error(errMsg, e);
        }
        if (StringUtils.isNotBlank(errMsg)) {
            updateErrorDetails(response, errMsg, HttpStatus.BAD_REQUEST);
        }
        return response;
    }

    private Map<String, String> validateQuestionListAPI(Map<String, Object> requestBody, String email,
                                                        List<String> identifierList, boolean editMode) throws IOException {
        Map<String, String> result = new HashMap<>();

        email = encryptionService.encryptData(email);
        String assessmentIdFromRequest = (String) requestBody.get(Constants.ASSESSMENT_ID_KEY);
        if (StringUtils.isBlank(assessmentIdFromRequest)) {
            result.put(Constants.ERROR_MESSAGE, Constants.ASSESSMENT_ID_KEY_IS_NOT_PRESENT_IS_EMPTY);
            return result;
        }
        identifierList.addAll(getQuestionIdList(requestBody));
        if (identifierList.isEmpty()) {
            result.put(Constants.ERROR_MESSAGE, Constants.IDENTIFIER_LIST_IS_EMPTY);
            return result;
        }

        Map<String, Object> assessmentAllDetail = assessmentUtilService
                .readAssessmentHierarchyFromCache(assessmentIdFromRequest, editMode);

        if (MapUtils.isEmpty(assessmentAllDetail)) {
            result.put(Constants.ERROR_MESSAGE, Constants.ASSESSMENT_HIERARCHY_READ_FAILED);
            return result;
        }
        String primaryCategory = (String) assessmentAllDetail.get(Constants.PRIMARY_CATEGORY);
        if (Constants.PRACTICE_QUESTION_SET
                .equalsIgnoreCase(primaryCategory) || editMode) {
            result.put(Constants.PRIMARY_CATEGORY, primaryCategory);
            result.put(Constants.ERROR_MESSAGE, StringUtils.EMPTY);
            return result;
        }

        Map<String, Object> userAssessmentAllDetail = new HashMap<String, Object>();

        List<Map<String, Object>> existingDataList = assessmentUtilService.readUserSubmittedAssessmentRecords(
                email, assessmentIdFromRequest);
        String questionSetFromAssessmentString = (!existingDataList.isEmpty())
                ? (String) existingDataList.get(0).get(Constants.ASSESSMENT_READ_RESPONSE_KEY)
                : "";
        if (StringUtils.isNotBlank(questionSetFromAssessmentString)) {
            userAssessmentAllDetail.putAll(mapper.readValue(questionSetFromAssessmentString,
                    new TypeReference<Map<String, Object>>() {
                    }));
        } else {
            result.put(Constants.ERROR_MESSAGE, Constants.USER_ASSESSMENT_DATA_NOT_PRESENT);
            return result;
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
            if (validateQuestionListRequest(identifierList, questionsFromAssessment)) {
                result.put(Constants.ERROR_MESSAGE, StringUtils.EMPTY);
            } else {
                result.put(Constants.ERROR_MESSAGE, Constants.THE_QUESTIONS_IDS_PROVIDED_DONT_MATCH);
            }
            return result;
        } else {
            result.put(Constants.ERROR_MESSAGE, Constants.ASSESSMENT_ID_INVALID);
            return result;
        }
    }

    private Boolean validateQuestionListRequest(List<String> identifierList, List<String> questionsFromAssessment) {
        return (new HashSet<>(questionsFromAssessment).containsAll(identifierList)) ? Boolean.TRUE : Boolean.FALSE;
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
}
