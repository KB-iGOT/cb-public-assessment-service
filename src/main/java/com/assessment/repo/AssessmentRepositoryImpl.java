package com.assessment.repo;

import com.assessment.cassandra.utils.CassandraOperation;
import com.assessment.model.SBApiResponse;
import com.assessment.util.AssessmentServiceLogger;
import com.assessment.util.Constants;
import com.assessment.util.ServerProperties;
import com.google.gson.Gson;
import org.apache.commons.collections4.MapUtils;
import org.codehaus.plexus.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class AssessmentRepositoryImpl implements AssessmentRepository {

    private AssessmentServiceLogger logger = new AssessmentServiceLogger(getClass().getName());

    @Autowired
    CassandraOperation cassandraOperation;

    @Autowired
    ServerProperties serverProperties;

    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");


    @Override
    public boolean addUserAssesmentDataToDB(String email, String assessmentIdentifier, Timestamp startTime,
                                            Timestamp endTime, Map<String, Object> questionSet, String status, String name,String contextId) {
        Map<String, Object> request = new HashMap<>();
        request.put(Constants.USER_ID, email);
        request.put(Constants.NAME, name);
        request.put(Constants.ASSESSMENT_ID_KEY, assessmentIdentifier);
        request.put(Constants.CONTEXT_ID,contextId);
        request.put(Constants.START_TIME, startTime);
        request.put(Constants.END_TIME, endTime);
        request.put(Constants.ASSESSMENT_READ_RESPONSE, new Gson().toJson(questionSet));
        request.put(Constants.STATUS, status);
        SBApiResponse resp = cassandraOperation.insertRecord(Constants.KEYSPACE_SUNBIRD,
                serverProperties.getPublicUserAssessmentData(), request);
        return resp.get(Constants.RESPONSE).equals(Constants.SUCCESS);
    }

    @Override
    public List<Map<String, Object>> fetchUserAssessmentDataFromDB(String email, String assessmentIdentifier) {
        Map<String, Object> request = new HashMap<>();
        request.put(Constants.USER_ID, email);
        request.put(Constants.ASSESSMENT_ID_KEY, assessmentIdentifier);
        return cassandraOperation.getRecordsByProperties(
                Constants.KEYSPACE_SUNBIRD,  serverProperties.getPublicUserAssessmentData(), request, null);
    }

    @Override
    public Boolean updateUserAssesmentDataToDB(String email, String assessmentIdentifier,
                                               Map<String, Object> submitAssessmentRequest, Map<String, Object> submitAssessmentResponse, String status,
                                               Date startTime, Map<String, Object> saveSubmitAssessmentRequest, String contextId) {
        Map<String, Object> compositeKeys = new HashMap<>();
        compositeKeys.put(Constants.USER_ID, email);
        compositeKeys.put(Constants.ASSESSMENT_ID_KEY, assessmentIdentifier);
        compositeKeys.put(Constants.CONTEXT_ID, contextId);
        compositeKeys.put(Constants.START_TIME, startTime);
        Map<String, Object> fieldsToBeUpdated = new HashMap<>();
        if (MapUtils.isNotEmpty(submitAssessmentRequest)) {
            fieldsToBeUpdated.put("submitassessmentrequest", new Gson().toJson(submitAssessmentRequest));
        }
        if (MapUtils.isNotEmpty(submitAssessmentResponse)) {
            fieldsToBeUpdated.put("submitassessmentresponse", new Gson().toJson(submitAssessmentResponse));
        }
        if (StringUtils.isNotBlank(status)) {
            fieldsToBeUpdated.put(Constants.STATUS, status);
        }
        if (MapUtils.isNotEmpty(saveSubmitAssessmentRequest)) {
            fieldsToBeUpdated.put("savepointsubmitreq", new Gson().toJson(saveSubmitAssessmentRequest));
        }
        cassandraOperation.updateRecord(Constants.KEYSPACE_SUNBIRD,  serverProperties.getPublicUserAssessmentData(),
                fieldsToBeUpdated, compositeKeys);
        return true;
    }

}