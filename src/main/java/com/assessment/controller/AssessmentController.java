package com.assessment.controller;


import com.assessment.model.SBApiResponse;
import com.assessment.service.AssessmentService;
import com.assessment.util.Constants;
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


    @GetMapping("/v1/public/assessment/read/{assessmentIdentifier}")
    public ResponseEntity<SBApiResponse> readAssessmentV4(
            @PathVariable("assessmentIdentifier") String assessmentIdentifier, @RequestHeader(Constants.EMAIL) String email, @RequestHeader(Constants.NAME) String name, @RequestParam(name = "editMode", required = false) String editMode) {
        Boolean edit = StringUtils.isEmpty(editMode) ? false : Boolean.parseBoolean(editMode);
        SBApiResponse readResponse = assessmentService.readAssessment(assessmentIdentifier, edit, email, name);
        return new ResponseEntity<>(readResponse, readResponse.getResponseCode());
    }

    @PostMapping("/v1/public/assessment/question/list")
    public ResponseEntity<?> readQuestionListV4(@Valid @RequestBody Map<String, Object> requestBody,
                                                @RequestHeader(Constants.EMAIL) String email,@RequestParam(name = "editMode" ,required = false) String editMode) {
        Boolean edit = org.apache.commons.lang.StringUtils.isEmpty(editMode)  ? false : Boolean.parseBoolean(editMode);
        SBApiResponse response = assessmentService.readQuestionList(requestBody, email,edit);
        return new ResponseEntity<>(response, response.getResponseCode());
    }
}
