package com.assessment.controller;

import com.assessment.model.SBApiResponse;
import com.assessment.service.AssessmentService;
import com.assessment.service.AssessmentServiceV4;
import com.assessment.service.AssessmentServiceV5;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AssessmentControllerTest {

    @Mock
    private AssessmentService assessmentService;

    @Mock
    private AssessmentServiceV4 assessmentServiceV4;

    @Mock
    private AssessmentServiceV5 assessmentServiceV5;

    @InjectMocks
    private AssessmentController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private SBApiResponse mockResponse() {
        SBApiResponse response = new SBApiResponse();
        response.setResponseCode(HttpStatus.OK);
        return response;
    }

    @Test
    void testReadAssessmentV4() {
        Map<String, Object> request = Collections.singletonMap("key", "value");
        SBApiResponse expected = mockResponse();
        when(assessmentService.readAssessment(anyBoolean(), eq(request))).thenReturn(expected);

        ResponseEntity<SBApiResponse> response = controller.readAssessmentV4(request, "true");

        assertEquals(expected, response.getBody());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(assessmentService).readAssessment(true, request);
    }

    @Test
    void testReadQuestionListV4() {
        Map<String, Object> request = Collections.singletonMap("key", "value");
        SBApiResponse expected = mockResponse();
        when(assessmentService.readQuestionList(eq(request), anyBoolean())).thenReturn(expected);

        ResponseEntity<SBApiResponse> response = controller.readQuestionListV4(request, null);

        assertEquals(expected, response.getBody());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(assessmentService).readQuestionList(request, false);
    }

    @Test
    void testSubmitUserAssessmentV4() {
        Map<String, Object> request = Collections.emptyMap();
        SBApiResponse expected = mockResponse();
        when(assessmentServiceV4.submitAssessmentAsync(eq(request), anyBoolean())).thenReturn(expected);

        ResponseEntity<SBApiResponse> response = controller.submitUserAssessmentV4(request, "false");

        assertEquals(expected, response.getBody());
        verify(assessmentServiceV4).submitAssessmentAsync(request, false);
    }

    @Test
    void testSubmitUserAssessmentV5() {
        Map<String, Object> request = Collections.emptyMap();
        SBApiResponse expected = mockResponse();
        when(assessmentServiceV5.submitAssessmentAsync(eq(request), anyBoolean())).thenReturn(expected);

        ResponseEntity<SBApiResponse> response = controller.submitUserAssessmentV5(request, null);

        assertEquals(expected, response.getBody());
        verify(assessmentServiceV5).submitAssessmentAsync(request, false);
    }

    @Test
    void testReadAssessmentV5() {
        Map<String, Object> request = Collections.emptyMap();
        SBApiResponse expected = mockResponse();
        when(assessmentServiceV5.readAssessment(anyBoolean(), eq(request))).thenReturn(expected);

        ResponseEntity<SBApiResponse> response = controller.readAssessmentV5(request, "true");

        assertEquals(expected, response.getBody());
        verify(assessmentServiceV5).readAssessment(true, request);
    }

    @Test
    void testReadQuestionListV5() {
        Map<String, Object> request = Collections.emptyMap();
        SBApiResponse expected = mockResponse();
        when(assessmentServiceV5.readQuestionList(eq(request), anyBoolean())).thenReturn(expected);

        ResponseEntity<SBApiResponse> response = controller.readQuestionListV5(request, "");

        assertEquals(expected, response.getBody());
        verify(assessmentServiceV5).readQuestionList(request, false);
    }

    @Test
    void testReadAssessmentResultV5() {
        Map<String, Object> request = Collections.emptyMap();
        SBApiResponse expected = mockResponse();
        when(assessmentServiceV5.readAssessmentResultV5(request)).thenReturn(expected);

        ResponseEntity<SBApiResponse> response = controller.readAssessmentResultV5(request);

        assertEquals(expected, response.getBody());
        verify(assessmentServiceV5).readAssessmentResultV5(request);
    }

    @Test
    void testAssessmentCertificateReissue() {
        Map<String, Object> request = Collections.emptyMap();
        SBApiResponse expected = mockResponse();
        when(assessmentServiceV5.assessmentCertificateReissue(request)).thenReturn(expected);

        ResponseEntity<SBApiResponse> response = controller.assessmentCertificateReissue(request);

        assertEquals(expected, response.getBody());
        verify(assessmentServiceV5).assessmentCertificateReissue(request);
    }
}
