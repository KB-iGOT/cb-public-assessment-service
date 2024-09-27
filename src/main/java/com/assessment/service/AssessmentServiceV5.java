package com.assessment.service;

import com.assessment.model.SBApiResponse;

import java.util.Map;

public interface AssessmentServiceV5 {


    /**
     * Submits an assessment asynchronously.
     *
     * @param requestBody     The assessment data to be submitted.
     * @param email    The email of the user submitting the assessment.
     * @param editMode Whether the assessment is being submitted in edit mode.
     * @return The response from the assessment submission.
     */
    public SBApiResponse submitAssessmentAsync(Map<String, Object> requestBody, String email, boolean editMode);
}
