package com.assessment.service;

import com.assessment.model.SBApiResponse;

import java.util.Map;

public interface AssessmentService {

    public SBApiResponse readAssessment(String assessmentIdentifier, Boolean editMode, String email, String name);

    public SBApiResponse readQuestionList(Map<String, Object> requestBody, String email, boolean editMode);
}
