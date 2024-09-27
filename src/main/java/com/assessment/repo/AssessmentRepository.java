package com.assessment.repo;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;

public interface AssessmentRepository {

	List<Map<String, Object>> fetchUserAssessmentDataFromDB(String userId, String assessmentIdentifier);

	boolean addUserAssesmentDataToDB(String email, String assessmentId, Timestamp startTime, Timestamp endTime,
									 Map<String, Object> questionSet, String status, String name);

	Boolean updateUserAssesmentDataToDB(String userId, String assessmentIdentifier,
										Map<String, Object> submitAssessmentRequest, Map<String, Object> submitAssessmentResponse, String status,
										Date startTime,Map<String, Object> saveSubmitAssessmentRequest);
}