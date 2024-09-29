package com.assessment.service;

import com.assessment.model.SBApiResponse;

import javax.validation.Valid;
import java.util.Map;

public interface AssessmentService {

    public SBApiResponse readAssessment(Boolean editMode, Map<String, Object> requestBody);

    public SBApiResponse readQuestionList(Map<String, Object> requestBody, boolean editMode);
}
