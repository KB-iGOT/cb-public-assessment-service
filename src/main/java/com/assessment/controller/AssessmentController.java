package com.assessment.controller;


import com.assessment.model.SBApiResponse;
import com.assessment.service.AssessmentService;
import com.assessment.service.AssessmentServiceV4;
import com.assessment.service.AssessmentServiceV5;
import com.assessment.util.Constants;
import org.apache.hadoop.classification.InterfaceAudience;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

@RestController
public class AssessmentController {

    @Autowired
    private AssessmentService assessmentService;

    @Autowired
    private AssessmentServiceV4 assessmentServiceV4;

    @Autowired
    private AssessmentServiceV5 assessmentServiceV5;

    @PostMapping("/v1/public/assessment/read")
    public ResponseEntity<SBApiResponse> readAssessmentV4( @Valid @RequestBody Map<String, Object> requestBody, @RequestParam(name = "editMode", required = false) String editMode) {
        Boolean edit = StringUtils.isEmpty(editMode) ? false : Boolean.parseBoolean(editMode);
        SBApiResponse readResponse = assessmentService.readAssessment(edit, requestBody);
        return new ResponseEntity<>(readResponse, readResponse.getResponseCode());
    }

    @PostMapping("/v1/public/assessment/question/list")
    public ResponseEntity<SBApiResponse> readQuestionListV4(@Valid @RequestBody Map<String, Object> requestBody, @RequestParam(name = "editMode", required = false) String editMode) {
        Boolean edit = org.apache.commons.lang.StringUtils.isEmpty(editMode) ? false : Boolean.parseBoolean(editMode);
        SBApiResponse response = assessmentService.readQuestionList(requestBody, edit);
        return new ResponseEntity<>(response, response.getResponseCode());
    }

    // =======================
    // V4 Enhancements
    // Async capability and not using Redis
    // =======================
    @PostMapping("/v4/user/assessment/submit")
    public ResponseEntity<SBApiResponse> submitUserAssessmentV4(@Valid @RequestBody Map<String, Object> requestBody, @RequestParam(name = "editMode", required = false) String editMode) {
        Boolean edit = org.apache.commons.lang.StringUtils.isEmpty(editMode) ? false : Boolean.parseBoolean(editMode);
        SBApiResponse submitResponse = assessmentServiceV4.submitAssessmentAsync(requestBody, edit);
        return new ResponseEntity<>(submitResponse, submitResponse.getResponseCode());
    }

    @PostMapping("/v5/user/assessment/submit")
    public ResponseEntity<SBApiResponse> submitUserAssessmentV5(@Valid @RequestBody Map<String, Object> requestBody, @RequestParam(name = "editMode", required = false) String editMode) {
        Boolean edit = org.apache.commons.lang.StringUtils.isEmpty(editMode) ? false : Boolean.parseBoolean(editMode);
        SBApiResponse submitResponse = assessmentServiceV5.submitAssessmentAsync(requestBody, edit);
        return new ResponseEntity<>(submitResponse, submitResponse.getResponseCode());
    }

    @PostMapping("/v5/public/user/assessment/read")
    public ResponseEntity<SBApiResponse> readAssessmentV5( @Valid @RequestBody Map<String, Object> requestBody, @RequestParam(name = "editMode", required = false) String editMode) {
        boolean edit = !org.apache.commons.lang.StringUtils.isEmpty(editMode) && Boolean.parseBoolean(editMode);
        SBApiResponse readResponse = assessmentServiceV5.readAssessment(edit, requestBody);
        return new ResponseEntity<>(readResponse, readResponse.getResponseCode());
    }

    @PostMapping("/v5/public/assessment/question/list")
    public ResponseEntity<SBApiResponse> readQuestionListV5(@Valid @RequestBody Map<String, Object> requestBody, @RequestParam(name = "editMode", required = false) String editMode) {
        Boolean edit = org.apache.commons.lang.StringUtils.isEmpty(editMode) ? false : Boolean.parseBoolean(editMode);
        SBApiResponse response = assessmentServiceV5.readQuestionList(requestBody, edit);
        return new ResponseEntity<>(response, response.getResponseCode());
    }

    @PostMapping("/v5/quml/assessment/result")
    public ResponseEntity<SBApiResponse> readAssessmentResultV5(@Valid @RequestBody Map<String, Object> requestBody) {
        SBApiResponse response = assessmentServiceV5.readAssessmentResultV5(requestBody);
        return new ResponseEntity<>(response, response.getResponseCode());
    }
}
