package com.assessment.service;

import com.assessment.model.SBApiResponse;

import java.util.Map;

public interface AssessmentServiceV4 {

    public SBApiResponse submitAssessmentAsync(Map<String, Object> data, boolean editMode);
}
