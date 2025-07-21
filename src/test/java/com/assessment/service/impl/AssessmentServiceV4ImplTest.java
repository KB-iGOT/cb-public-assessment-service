package com.assessment.service.impl;

import com.assessment.cassandra.utils.CassandraOperation;
import com.assessment.datasecurity.DecryptionService;
import com.assessment.datasecurity.EncryptionService;
import com.assessment.kafka.Producer;
import com.assessment.kafka.service.KafkaCertificateProducerService;
import com.assessment.model.SBApiResponse;
import com.assessment.repo.AssessmentRepository;
import com.assessment.service.AssessmentUtilServiceV2;
import com.assessment.util.Constants;
import com.assessment.util.ServerProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssessmentServiceV4ImplTest {

    @InjectMocks
    private AssessmentServiceV4Impl service;

    @Mock
    private AssessmentUtilServiceV2 assessmentUtilService;
    @Mock
    private ServerProperties serverProperties;
    @Mock
    private AssessmentRepository assessmentRepository;
    @Mock
    private EncryptionService encryptionService;
    @Mock
    private ObjectMapper mapper;
    @Mock
    private CassandraOperation cassandraOperation;
    @Mock
    private ResourceLoader resourceLoader;
    @Mock
    private DecryptionService decryptionService;
    @Mock
    private KafkaCertificateProducerService kafkaCertificateProducerService;
    @Mock
    private Producer producer;

    @Test
    void testSubmitAssessmentAsync_valid_and_invalid() throws Exception {
        Map<String, Object> submitRequest = new HashMap<>();
        submitRequest.put(Constants.EMAIL, "test@email.com");
        submitRequest.put(Constants.CONTEXT_ID, "ctxId");
        submitRequest.put(Constants.IDENTIFIER, "assessId");
        submitRequest.put(Constants.CHILDREN, List.of(Map.of(Constants.IDENTIFIER, "sec1")));

        when(encryptionService.encryptData(anyString())).thenReturn("encrypted-email");
        when(assessmentUtilService.readAssessmentHierarchyFromCache(anyString(), anyBoolean()))
                .thenReturn(Map.of(
                        Constants.PRIMARY_CATEGORY, "category",
                        Constants.SCORE_CUTOFF_TYPE, Constants.ASSESSMENT_LEVEL_SCORE_CUTOFF,
                        Constants.EXPECTED_DURATION, 100,
                        Constants.CHILDREN, List.of(Map.of(Constants.IDENTIFIER, "sec1"))
                ));
        when(assessmentUtilService.readUserSubmittedAssessmentRecords(anyString(), anyString(), anyString()))
                .thenReturn(List.of(new HashMap<>() {{
                    put(Constants.START_TIME, new Date());
                    put(Constants.ASSESSMENT_READ_RESPONSE_KEY, "{\"children\": [{\"childNodes\": [\"q1\"]}]}");
                }}));

        when(serverProperties.getUserAssessmentSubmissionDuration()).thenReturn("10");
        when(mapper.readValue(anyString(), any(TypeReference.class)))
                .thenReturn(Map.of(Constants.ASSESSMENT_READ_RESPONSE_KEY, "value"));

        SBApiResponse response = service.submitAssessmentAsync(submitRequest, false);

        assertNotNull(response);
        assertEquals(Constants.API_SUBMIT_ASSESSMENT, response.getId());

        // Invalid email
        submitRequest.put(Constants.EMAIL, "invalid-email");
        SBApiResponse respInvalidEmail = service.submitAssessmentAsync(submitRequest, false);
        assertEquals(HttpStatus.BAD_REQUEST, respInvalidEmail.getResponseCode());

        // Missing identifier
        submitRequest.remove(Constants.IDENTIFIER);
        SBApiResponse respMissingId = service.submitAssessmentAsync(submitRequest, false);
        assertEquals(HttpStatus.BAD_REQUEST, respMissingId.getResponseCode());
    }

    @Test
    void testCreateResponseMapWithProperStructure_withResultMap() {
        Map<String, Object> hierarchySection = new HashMap<>();
        hierarchySection.put(Constants.IDENTIFIER, "sec1");
        hierarchySection.put(Constants.OBJECT_TYPE, "type");
        hierarchySection.put(Constants.PRIMARY_CATEGORY, "cat");
        hierarchySection.put(Constants.MINIMUM_PASS_PERCENTAGE, 50);

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put(Constants.RESULT, 75.0);
        resultMap.put(Constants.TOTAL, 10);
        resultMap.put(Constants.BLANK, 0);
        resultMap.put(Constants.CORRECT, 10);
        resultMap.put(Constants.INCORRECT, 0);
        resultMap.put(Constants.CHILDREN, Collections.emptyList());

        Map<String, Object> responseMap =
                service.createResponseMapWithProperStructure(hierarchySection, resultMap);

        assertEquals("sec1", responseMap.get(Constants.IDENTIFIER));
        assertEquals(75.0, responseMap.get(Constants.RESULT));
        assertEquals(true, responseMap.get(Constants.PASS));
    }

    @Test
    void testCreateResponseMapWithProperStructure_withNullResultMap() {
        Map<String, Object> hierarchySection = new HashMap<>();
        hierarchySection.put(Constants.IDENTIFIER, "sec1");
        hierarchySection.put(Constants.OBJECT_TYPE, "type");
        hierarchySection.put(Constants.PRIMARY_CATEGORY, "cat");
        hierarchySection.put(Constants.MINIMUM_PASS_PERCENTAGE, 50);
        hierarchySection.put(Constants.CHILDREN, List.of("q1", "q2"));

        Map<String, Object> responseMap =
                service.createResponseMapWithProperStructure(hierarchySection, null);

        assertEquals("sec1", responseMap.get(Constants.IDENTIFIER));
        assertEquals(0.0, responseMap.get(Constants.RESULT));
        assertEquals(2, responseMap.get(Constants.TOTAL));
        assertEquals(false, responseMap.get(Constants.PASS));
    }


}
