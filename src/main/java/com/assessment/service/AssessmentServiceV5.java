package com.assessment.service;

import com.assessment.model.SBApiResponse;

import javax.validation.Valid;
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

    /**
     * Reads an assessment.
     * @return The response from the assessment read.
     */
    SBApiResponse readAssessment(Boolean editMode, Map<String, Object> requestBody);

    /**
     * Reads a list of questions.
     *
     * @param requestBody The request body containing the question list parameters.
     * @param email       The email of the user.
     * @param edit        Whether the question list is being read in edit mode.
     * @return The response from the question list read.
     */
    SBApiResponse readQuestionList(@Valid Map<String, Object> requestBody, String email, Boolean edit);

    public SBApiResponse readAssessmentResultV5(Map<String, Object> request);
}
