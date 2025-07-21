package com.assessment.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.assessment.cassandra.utils.CassandraOperation;
import com.assessment.datasecurity.DecryptionService;
import com.assessment.datasecurity.EncryptionService;
import com.assessment.kafka.Producer;
import com.assessment.kafka.service.KafkaCertificateProducerService;
import com.assessment.model.SBApiResponse;
import com.assessment.repo.AssessmentRepository;
import com.assessment.service.AssessmentUtilServiceV2;
import com.assessment.service.OutboundRequestHandlerServiceImpl;
import com.assessment.util.Constants;
import com.assessment.util.ServerProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;

class AssessmentServiceV5ImplTest {

    @InjectMocks
    private AssessmentServiceV5Impl service;

    @Mock
    private ServerProperties serverProperties;
    @Mock
    private OutboundRequestHandlerServiceImpl outboundRequestHandlerService;
    @Mock
    private AssessmentUtilServiceV2 assessUtilServ;
    @Mock
    private ObjectMapper mapper;
    @Mock
    private AssessmentRepository assessmentRepository;
    @Mock
    private CassandraOperation cassandraOperation;
    @Mock
    private EncryptionService encryptionService;
    @Mock
    private DecryptionService decryptionService;
    @Mock
    private KafkaCertificateProducerService kafkaCertificateProducerService;
    @Mock
    private Producer producer;
    @Mock
    private ResourceLoader resourceLoader;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testSubmitAssessmentAsync_happyPath() throws Exception {
        Map<String, Object> submitRequest = new HashMap<>();
        submitRequest.put("email", "test@example.com");
        submitRequest.put("contextId", "ctx123");
        submitRequest.put("identifier", "assess123");

        Map<String, Object> submitSection = new HashMap<>();
        submitSection.put("identifier", "section1");

        Map<String, Object> submitQuestion = new HashMap<>();
        submitQuestion.put("identifier", "q1");

        submitSection.put("children", List.of(submitQuestion));
        submitRequest.put("children", List.of(submitSection));

        Map<String, Object> hierarchySection = new HashMap<>();
        hierarchySection.put("identifier", "section1");
        hierarchySection.put("objectType", "Section");
        hierarchySection.put("primaryCategory", "COURSE_ASSESSMENT");
        hierarchySection.put("minimumPassPercentage", 50);
        hierarchySection.put("name", "Section 1");
        hierarchySection.put("totalMarks", 10);
        hierarchySection.put("sectionLevelDefinition", Map.of());

        Map<String, Object> hierarchy = new HashMap<>();
        hierarchy.put("primaryCategory", "COURSE_ASSESSMENT");
        hierarchy.put("children", List.of(hierarchySection));
        hierarchy.put("maxAssessmentRetakeAttempts", 3);
        hierarchy.put("assessmentType", "QUESTION_WEIGHTAGE");
        hierarchy.put("minimumPassPercentage", 50);
        hierarchy.put("expectedDuration", 100);

        when(assessUtilServ.readAssessmentHierarchyFromCache(anyString(), anyBoolean()))
                .thenReturn(hierarchy);

        when(encryptionService.encryptData(anyString()))
                .thenReturn("encryptedEmail");

        when(assessUtilServ.readUserSubmittedAssessmentRecords(any(), any(), any()))
                .thenReturn(Collections.singletonList(
                        Map.of("starttime", new Date(),
                                "assessmentReadResponse", "{\"children\":[]}")));

        when(serverProperties.getUserAssessmentSubmissionDuration())
                .thenReturn("30");

        when(assessUtilServ.readQListfromCache(any(), any(), anyBoolean()))
                .thenReturn(Collections.emptyMap());

        when(assessmentRepository.updateUserAssesmentDataToDB(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(true);

        when(decryptionService.decryptData(anyString()))
                .thenReturn("user123");

        when(assessmentRepository.fetchUserAssessmentDataFromDB(any(), any()))
                .thenReturn(Collections.singletonList(Map.of("name", "Test User")));

        when(cassandraOperation.getRecordsByProperties(any(), any(), any(), any()))
                .thenReturn(Collections.singletonList(Map.of("hierarchy", "{\"name\":\"CourseName\",\"source\":\"Provider\",\"posterImage\":\"image.png\"}")));

        Resource resource = mock(Resource.class);
        when(resourceLoader.getResource(anyString())).thenReturn(resource);
        when(resource.getInputStream()).thenReturn(new ByteArrayInputStream("{}".getBytes()));

        when(mapper.readTree(any(InputStream.class))).thenReturn(mock(com.fasterxml.jackson.databind.JsonNode.class));
        when(mapper.readValue(anyString(), ArgumentMatchers.<TypeReference<Map<String, Object>>>any()))
                .thenReturn(Map.of("children", Collections.emptyList()));

        when(serverProperties.getAssessmentPrimaryKeyNotAllowedCertificate()).thenReturn(Collections.emptyList());
        when(serverProperties.getKafkaTopicsPublicAssessmentCertificate()).thenReturn("topic");

        SBApiResponse response = service.submitAssessmentAsync(submitRequest, false);

        assertNotNull(response);
    }

    @Test
    void testSubmitAssessmentAsync_validationFails() {
        Map<String, Object> submitRequest = new HashMap<>();
        submitRequest.put("email", "test@example.com");
        submitRequest.put("identifier", "");
        submitRequest.put("children", Collections.emptyList());

        SBApiResponse response = service.submitAssessmentAsync(submitRequest, false);

        assertNotNull(response);
        assertNotEquals(HttpStatus.OK, response.getResponseCode());
    }

    @Test
    void testSubmitAssessmentAsync_editMode() {
        Map<String, Object> submitRequest = new HashMap<>();
        submitRequest.put("email", "test@example.com");
        submitRequest.put("identifier", "assess123");
        submitRequest.put("children", Collections.emptyList());

        Map<String, Object> hierarchy = new HashMap<>();
        hierarchy.put("primaryCategory", "PRACTICE_QUESTION_SET");
        hierarchy.put("children", Collections.emptyList());
        hierarchy.put("assessmentType", "SIMPLE");
        hierarchy.put("maxAssessmentRetakeAttempts", 1);
        hierarchy.put("minimumPassPercentage", 50);
        hierarchy.put("expectedDuration", 100);

        when(assessUtilServ.fetchHierarchyFromAssessServc(anyString())).thenReturn(hierarchy);
        when(encryptionService.encryptData(anyString())).thenReturn("encEmail");

        when(assessUtilServ.readUserSubmittedAssessmentRecords(any(), any(), any()))
                .thenReturn(Collections.singletonList(
                        Map.of("starttime", new Date(),
                                "assessmentReadResponseKey", "{\"children\":[]}")));

        when(serverProperties.getUserAssessmentSubmissionDuration()).thenReturn("30");

        SBApiResponse response = service.submitAssessmentAsync(submitRequest, true);

        assertNotNull(response);
    }

    @Test
    void testSubmitAssessmentAsync_exceptionFlow() {
        Map<String, Object> submitRequest = new HashMap<>();
        submitRequest.put("email", "test@example.com");
        submitRequest.put("identifier", "assess123");
        submitRequest.put("children", Collections.emptyList());

        Map<String, Object> hierarchy = new HashMap<>();
        hierarchy.put("primaryCategory", "COURSE_ASSESSMENT");
        hierarchy.put("children", Collections.emptyList());
        hierarchy.put("assessmentType", "SIMPLE");
        hierarchy.put("maxAssessmentRetakeAttempts", 1);
        hierarchy.put("minimumPassPercentage", 50);
        hierarchy.put("expectedDuration", 100);

        when(assessUtilServ.readAssessmentHierarchyFromCache(anyString(), anyBoolean()))
                .thenReturn(hierarchy);

        when(encryptionService.encryptData(anyString())).thenReturn("encEmail");

        when(assessUtilServ.readUserSubmittedAssessmentRecords(any(), any(), any()))
                .thenThrow(new RuntimeException("DB Error"));

        SBApiResponse response = service.submitAssessmentAsync(submitRequest, false);

        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getResponseCode());
    }

    @Test
    void testSubmitAssessmentAsync_happyPath_withChildren() throws Exception {
        Map<String, Object> submitRequest = new HashMap<>();
        submitRequest.put("email", "test@example.com");
        submitRequest.put("contextId", "ctx123");
        submitRequest.put("identifier", "assess123");

        Map<String, Object> sectionFromRequest = new HashMap<>();
        sectionFromRequest.put("identifier", "sec1");
        sectionFromRequest.put("children", List.of(Map.of("identifier", "q1")));
        submitRequest.put("children", List.of(sectionFromRequest));

        Map<String, Object> hierarchySection = new HashMap<>();
        hierarchySection.put("identifier", "sec1");
        hierarchySection.put("objectType", "Section");
        hierarchySection.put("primaryCategory", "SectionCategory");
        hierarchySection.put("minimumPassPercentage", 50);
        hierarchySection.put("name", "Section 1");
        hierarchySection.put("children", List.of("q1"));
        hierarchySection.put("totalMarks", 10);
        hierarchySection.put("sectionLevelDefinition", Collections.emptyMap());

        Map<String, Object> hierarchy = new HashMap<>();
        hierarchy.put("primaryCategory", "COURSE_ASSESSMENT");
        hierarchy.put("children", List.of(hierarchySection));
        hierarchy.put("maxAssessmentRetakeAttempts", 3);
        hierarchy.put("assessmentType", "questionWeightage");
        hierarchy.put("minimumPassPercentage", 50);
        hierarchy.put("expectedDuration", 100);

        when(assessUtilServ.readAssessmentHierarchyFromCache(anyString(), anyBoolean()))
                .thenReturn(hierarchy);

        when(encryptionService.encryptData(anyString()))
                .thenReturn("encryptedEmail");

        when(assessUtilServ.readUserSubmittedAssessmentRecords(any(), any(), any()))
                .thenReturn(List.of(
                        Map.of("starttime", new Date(),
                                "assessmentReadResponse", "{\"children\":[{\"identifier\":\"sec1\",\"childNodes\":[\"q1\"]}]}")));

        when(serverProperties.getUserAssessmentSubmissionDuration())
                .thenReturn("30");

        when(assessUtilServ.readQListfromCache(any(), any(), anyBoolean()))
                .thenReturn(Collections.emptyMap());

        when(assessUtilServ.validateQumlAssessmentV2(any(), any(), any(), any()))
                .thenReturn(Map.of("result", 1.0, "blank", 0, "correct", 1, "incorrect", 0,
                        "children", Collections.emptyList(), "sectionResult", Collections.emptyList(), "totalMarks", 10, "sectionMarks", 10));

        when(assessmentRepository.updateUserAssesmentDataToDB(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(true);

        when(decryptionService.decryptData(anyString()))
                .thenReturn("user123");

        when(assessmentRepository.fetchUserAssessmentDataFromDB(any(), any()))
                .thenReturn(List.of(Map.of("name", "Test User")));

        when(cassandraOperation.getRecordsByProperties(any(), any(), any(), any()))
                .thenReturn(List.of(Map.of("hierarchy", "{\"name\":\"CourseName\",\"source\":\"Provider\",\"posterImage\":\"image.png\"}")));

        Resource resource = mock(Resource.class);
        when(resourceLoader.getResource(anyString())).thenReturn(resource);
        when(resource.getInputStream()).thenReturn(new ByteArrayInputStream("{}".getBytes()));

        when(mapper.readTree(any(InputStream.class))).thenReturn(mock(com.fasterxml.jackson.databind.JsonNode.class));
        when(mapper.readValue(anyString(), ArgumentMatchers.<TypeReference<Map<String, Object>>>any()))
                .thenReturn(Map.of("children", List.of(Map.of("identifier", "sec1", "childNodes", List.of("q1")))));

        when(serverProperties.getAssessmentPrimaryKeyNotAllowedCertificate()).thenReturn(Collections.emptyList());
        when(serverProperties.getKafkaTopicsPublicAssessmentCertificate()).thenReturn("topic");

        SBApiResponse response = service.submitAssessmentAsync(submitRequest, false);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getResponseCode());
    }

    @Test
    void testSubmitAssessmentAsync_happyPath_withChildren_1() throws Exception {
        Map<String, Object> submitRequest = new HashMap<>();
        submitRequest.put("email", "test@example.com");
        submitRequest.put("contextId", "ctx123");
        submitRequest.put("identifier", "assess123");

        Map<String, Object> sectionFromRequest = new HashMap<>();
        sectionFromRequest.put("identifier", "sec1");
        sectionFromRequest.put("children", List.of(Map.of("identifier", "q1")));
        submitRequest.put("children", List.of(sectionFromRequest));
        submitRequest.put("starttime", new Date());

        Map<String, Object> hierarchySection = new HashMap<>();
        hierarchySection.put("identifier", "sec1");
        hierarchySection.put("objectType", "Section");
        hierarchySection.put("primaryCategory", "SectionCategory");
        hierarchySection.put("minimumPassPercentage", 50);
        hierarchySection.put("name", "Section 1");
        hierarchySection.put("children", List.of("q1"));
        hierarchySection.put("totalMarks", 10);
        hierarchySection.put("sectionLevelDefinition", Collections.emptyMap());

        Map<String, Object> hierarchy = new HashMap<>();
        hierarchy.put("primaryCategory", "COURSE_ASSESSMENT");
        hierarchy.put("children", List.of(hierarchySection));
        hierarchy.put("maxAssessmentRetakeAttempts", 3);
        hierarchy.put("assessmentType", "question_weightage");
        hierarchy.put("minimumPassPercentage", 50);
        hierarchy.put("expectedDuration", 100);

        when(assessUtilServ.readAssessmentHierarchyFromCache(anyString(), anyBoolean()))
                .thenReturn(hierarchy);

        when(encryptionService.encryptData(anyString()))
                .thenReturn("encryptedEmail");

        Date startTime = new Date();
        when(assessUtilServ.readUserSubmittedAssessmentRecords(any(), any(), any()))
                .thenReturn(List.of(
                        Map.of(
                                "starttime", startTime,
                                "assessmentReadResponse", String.format(
                                        "{\"starttime\":\"%s\", \"children\":[{\"identifier\":\"sec1\",\"childNodes\":[\"q1\"]}]}",
                                        startTime.toString()
                                )
                        )
                ));
        when(serverProperties.getUserAssessmentSubmissionDuration())
                .thenReturn("30");

        when(assessUtilServ.readQListfromCache(any(), any(), anyBoolean()))
                .thenReturn(Collections.emptyMap());

        when(assessUtilServ.validateQumlAssessmentV2(any(), any(), any(), any()))
                .thenReturn(Map.of("result", 1.0, "blank", 0, "correct", 1, "incorrect", 0,
                        "children", Collections.emptyList(), "sectionResult", Collections.emptyList(), "totalMarks", 10, "sectionMarks", 10));

        when(assessmentRepository.updateUserAssesmentDataToDB(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(true);

        when(decryptionService.decryptData(anyString()))
                .thenReturn("user123");

        when(assessmentRepository.fetchUserAssessmentDataFromDB(any(), any()))
                .thenReturn(List.of(Map.of("name", "Test User")));

        when(cassandraOperation.getRecordsByProperties(any(), any(), any(), any()))
                .thenReturn(List.of(Map.of("hierarchy", "{\"name\":\"CourseName\",\"source\":\"Provider\",\"posterImage\":\"image.png\"}")));

        Resource resource = mock(Resource.class);
        when(resourceLoader.getResource(anyString())).thenReturn(resource);
        when(resource.getInputStream()).thenReturn(new ByteArrayInputStream("{}".getBytes()));

        when(mapper.readTree(any(InputStream.class))).thenReturn(mock(com.fasterxml.jackson.databind.JsonNode.class));
        when(mapper.readValue(anyString(), ArgumentMatchers.<TypeReference<Map<String, Object>>>any()))
                .thenReturn(Map.of("children", List.of(Map.of("identifier", "sec1", "childNodes", List.of("q1")))));

        when(serverProperties.getAssessmentPrimaryKeyNotAllowedCertificate()).thenReturn(Collections.emptyList());
        when(serverProperties.getKafkaTopicsPublicAssessmentCertificate()).thenReturn("topic");

        SBApiResponse response = service.submitAssessmentAsync(submitRequest, false);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getResponseCode());
    }


    @Test
    void testReadAssessment_editMode() {
        Map<String, Object> requestBody = Map.of(Constants.ASSESSMENT_IDENTIFIER, "a1");
        Map<String, Object> assessmentDetail = Map.of(Constants.PRIMARY_CATEGORY, Constants.PRACTICE_QUESTION_SET);

        when(assessUtilServ.fetchHierarchyFromAssessServc("a1")).thenReturn(assessmentDetail);

        SBApiResponse response = service.readAssessment(true, requestBody);

        assertNotNull(response);
    }

    @Test
    void testReadAssessment_practiceQuestionSet() {
        Map<String, Object> requestBody = Map.of(Constants.ASSESSMENT_IDENTIFIER, "a1");
        Map<String, Object> assessmentDetail = new HashMap<>();
        assessmentDetail.put(Constants.PRIMARY_CATEGORY, Constants.PRACTICE_QUESTION_SET);
        assessmentDetail.put("param1", "val1");
        assessmentDetail.put("param2", "val2");
        assessmentDetail.put(Constants.CHILDREN, List.of(
                Map.of(Constants.IDENTIFIER, "sec1", Constants.CHILDREN, List.of(
                        Map.of(Constants.IDENTIFIER, "q1")
                ))
        ));

        when(assessUtilServ.readAssessmentHierarchyFromCache("a1", false)).thenReturn(assessmentDetail);

        SBApiResponse response = service.readAssessment(false, requestBody);

        assertNotNull(response);
    }

    @Test
    void testReadAssessment_courseAssessment_firstTime() {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put(Constants.ASSESSMENT_IDENTIFIER, "a1");
        requestBody.put(Constants.EMAIL, "user@example.com");
        requestBody.put(Constants.NAME, "User");
        requestBody.put(Constants.CONTEXT_ID, "ctx1");

        Map<String, Object> assessmentDetail = new HashMap<>();
        assessmentDetail.put(Constants.PRIMARY_CATEGORY, Constants.COURSE_ASSESSMENT);
        assessmentDetail.put(Constants.EXPECTED_DURATION, 100);
        assessmentDetail.put(Constants.ASSESSMENT_TYPE, "type");
        assessmentDetail.put(Constants.CHILDREN, List.of(
                Map.of(Constants.IDENTIFIER, "sec1", Constants.CHILDREN, List.of(
                        Map.of(Constants.IDENTIFIER, "q1")
                ))
        ));

        when(assessUtilServ.readAssessmentHierarchyFromCache("a1", false)).thenReturn(assessmentDetail);
        when(encryptionService.encryptData(any())).thenReturn("encrypted");
        when(assessUtilServ.readUserSubmittedAssessmentRecords(any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(assessmentRepository.addUserAssesmentDataToDB(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(true);

        SBApiResponse response = service.readAssessment(false, requestBody);

        assertNotNull(response);
        assertEquals(Constants.SUCCESS, response.getParams().getStatus());
    }

    @Test
    void testReadAssessment_existingRecord_notSubmitted() {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put(Constants.ASSESSMENT_IDENTIFIER, "a1");
        requestBody.put(Constants.EMAIL, "user@example.com");
        requestBody.put(Constants.NAME, "User");
        requestBody.put(Constants.CONTEXT_ID, "ctx1");

        Map<String, Object> assessmentDetail = new HashMap<>();
        assessmentDetail.put(Constants.PRIMARY_CATEGORY, Constants.COURSE_ASSESSMENT);
        assessmentDetail.put(Constants.EXPECTED_DURATION, 100);
        assessmentDetail.put(Constants.ASSESSMENT_TYPE, "type");
        assessmentDetail.put(Constants.CHILDREN, List.of(
                Map.of(Constants.IDENTIFIER, "sec1", Constants.CHILDREN, List.of(
                        Map.of(Constants.IDENTIFIER, "q1")
                ))
        ));

        when(assessUtilServ.readAssessmentHierarchyFromCache("a1", false)).thenReturn(assessmentDetail);
        when(encryptionService.encryptData(any())).thenReturn("encrypted");

        Date endTime = new Date(System.currentTimeMillis() + 100000);
        Map<String, Object> existingRecord = new HashMap<>();
        existingRecord.put(Constants.END_TIME, endTime);
        existingRecord.put(Constants.STATUS, Constants.NOT_SUBMITTED);
        existingRecord.put(Constants.ASSESSMENT_READ_RESPONSE_KEY, "{\"children\":[],\"starttime\":0,\"endtime\":0}");

        when(assessUtilServ.readUserSubmittedAssessmentRecords(any(), any(), any()))
                .thenReturn(List.of(existingRecord));

        SBApiResponse response = service.readAssessment(false, requestBody);

        assertNotNull(response);
        assertEquals(Constants.SUCCESS, response.getParams().getStatus());
    }

    @Test
    void testReadAssessment_existingRecord_submitted_failed() {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put(Constants.ASSESSMENT_IDENTIFIER, "a1");
        requestBody.put(Constants.EMAIL, "user@example.com");
        requestBody.put(Constants.NAME, "User");
        requestBody.put(Constants.CONTEXT_ID, "ctx1");

        Map<String, Object> assessmentDetail = new HashMap<>();
        assessmentDetail.put(Constants.PRIMARY_CATEGORY, Constants.COURSE_ASSESSMENT);
        assessmentDetail.put(Constants.EXPECTED_DURATION, 100);
        assessmentDetail.put(Constants.ASSESSMENT_TYPE, "type");
        assessmentDetail.put(Constants.CHILDREN, List.of(
                Map.of(Constants.IDENTIFIER, "sec1", Constants.CHILDREN, List.of(
                        Map.of(Constants.IDENTIFIER, "q1")
                ))
        ));

        when(assessUtilServ.readAssessmentHierarchyFromCache("a1", false)).thenReturn(assessmentDetail);
        when(encryptionService.encryptData(any())).thenReturn("encrypted");

        Map<String, Object> existingRecord = new HashMap<>();
        existingRecord.put(Constants.END_TIME, new Date());
        existingRecord.put(Constants.STATUS, Constants.SUBMITTED);
        existingRecord.put(Constants.PASS_STATUS, false);

        when(assessUtilServ.readUserSubmittedAssessmentRecords(any(), any(), any()))
                .thenReturn(List.of(existingRecord));

        when(assessmentRepository.addUserAssesmentDataToDB(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(true);

        SBApiResponse response = service.readAssessment(false, requestBody);

        assertNotNull(response);
        assertEquals(Constants.SUCCESS, response.getParams().getStatus());
    }

    @Test
    void testReadAssessment_existingRecord_submitted_passed() {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put(Constants.ASSESSMENT_IDENTIFIER, "a1");
        requestBody.put(Constants.EMAIL, "user@example.com");
        requestBody.put(Constants.NAME, "User");
        requestBody.put(Constants.CONTEXT_ID, "ctx1");

        Map<String, Object> assessmentDetail = new HashMap<>();
        assessmentDetail.put(Constants.PRIMARY_CATEGORY, Constants.COURSE_ASSESSMENT);
        assessmentDetail.put(Constants.EXPECTED_DURATION, 100);
        assessmentDetail.put(Constants.ASSESSMENT_TYPE, "type");
        assessmentDetail.put(Constants.CHILDREN, List.of(
                Map.of(Constants.IDENTIFIER, "sec1", Constants.CHILDREN, List.of(
                        Map.of(Constants.IDENTIFIER, "q1")
                ))
        ));

        when(assessUtilServ.readAssessmentHierarchyFromCache("a1", false)).thenReturn(assessmentDetail);
        when(encryptionService.encryptData(any())).thenReturn("encrypted");

        Map<String, Object> existingRecord = new HashMap<>();
        existingRecord.put(Constants.END_TIME, new Date());
        existingRecord.put(Constants.STATUS, Constants.SUBMITTED);
        existingRecord.put(Constants.PASS_STATUS, true);

        when(assessUtilServ.readUserSubmittedAssessmentRecords(any(), any(), any()))
                .thenReturn(List.of(existingRecord));

        SBApiResponse response = service.readAssessment(false, requestBody);

        assertNotNull(response);
        assertEquals(Constants.SUCCESS, response.getParams().getStatus());
    }

    @Test
    void testReadAssessment_invalidRequest() {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put(Constants.ASSESSMENT_IDENTIFIER, "a1");
        requestBody.put(Constants.PRIMARY_CATEGORY, Constants.COURSE_ASSESSMENT);

        Map<String, Object> assessmentDetail = Map.of(Constants.PRIMARY_CATEGORY, Constants.COURSE_ASSESSMENT);

        when(assessUtilServ.readAssessmentHierarchyFromCache("a1", false)).thenReturn(assessmentDetail);

        SBApiResponse response = service.readAssessment(false, requestBody);

        assertNotNull(response);
        assertEquals("Failed", response.getParams().getStatus());
    }

    @Test
    void testReadAssessment_exception() {
        Map<String, Object> requestBody = Map.of(Constants.ASSESSMENT_IDENTIFIER, "a1");

        when(assessUtilServ.readAssessmentHierarchyFromCache(any(), anyBoolean()))
                .thenThrow(new RuntimeException("error"));

        SBApiResponse response = service.readAssessment(false, requestBody);

        assertNotNull(response);
        assertEquals("Failed", response.getParams().getStatus());
    }


    @Test
    void testReadQuestionList_success() throws Exception {
        Map<String, Object> requestBody = getValidRequest();
        Map<String, Object> assessmentDetail = Map.of(Constants.PRIMARY_CATEGORY, "category", Constants.CHILDREN, List.of(
                Map.of(Constants.CHILD_NODES, List.of("q1", "q2"))
        ));

        when(assessUtilServ.readAssessmentHierarchyFromCache(any(), anyBoolean()))
                .thenReturn(assessmentDetail);
        when(encryptionService.encryptData(any())).thenReturn("encrypted");
        when(assessUtilServ.readUserSubmittedAssessmentRecords(any(), any(), any()))
                .thenReturn(List.of(Map.of(Constants.ASSESSMENT_READ_RESPONSE_KEY, "{\"primaryCategory\":\"category\",\"children\":[{\"childNodes\":[\"q1\"]}]}")));
        when(mapper.readValue(anyString(), any(TypeReference.class))).thenReturn(
                Map.of(Constants.PRIMARY_CATEGORY, "category", Constants.CHILDREN, List.of(Map.of(Constants.CHILD_NODES, List.of("q1"))))
        );
        when(assessUtilServ.readQListfromCache(any(), any(), anyBoolean()))
                .thenReturn(Map.of("q1", Map.of("id", "q1")));
        when(assessUtilServ.filterQuestionMapDetailV2(any(), any())).thenReturn(Map.of("id", "q1"));

        var response = service.readQuestionList(requestBody, false);

        assertNotNull(response);
        assertTrue(response.getResult().containsKey(Constants.QUESTIONS));
    }

    @Test
    void testReadQuestionList_blankAssessmentId() {
        Map<String, Object> requestBody = new HashMap<>();
        var response = service.readQuestionList(requestBody, false);

        assertEquals(Constants.ASSESSMENT_ID_KEY_IS_NOT_PRESENT_IS_EMPTY, response.get(Constants.ERROR_MESSAGE));
    }

    @Test
    void testReadQuestionList_invalidAssessment() {
        Map<String, Object> requestBody = getValidRequest();

        when(assessUtilServ.readAssessmentHierarchyFromCache(any(), anyBoolean()))
                .thenReturn(Collections.emptyMap());

        var response = service.readQuestionList(requestBody, false);

        assertEquals(Constants.ASSESSMENT_HIERARCHY_READ_FAILED, response.getParams().getErrmsg());
    }

    @Test
    void testReadQuestionList_practiceQuestionSet() {
        Map<String, Object> requestBody = getValidRequest();

        Map<String, Object> assessmentDetail = Map.of(Constants.PRIMARY_CATEGORY, Constants.PRACTICE_QUESTION_SET);

        when(assessUtilServ.readAssessmentHierarchyFromCache(any(), anyBoolean()))
                .thenReturn(assessmentDetail);

        var response = service.readQuestionList(requestBody, false);

        assertTrue(response.containsKey(Constants.ERROR_MESSAGE));
        assertEquals("", response.get(Constants.ERROR_MESSAGE));  // comes from PRACTICE_QUESTION_SET branch
    }

    @Test
    void testReadQuestionList_emptyQuestionIdList() {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put(Constants.ASSESSMENT_IDENTIFIER, "a1");

        Map<String, Object> assessmentDetail = Map.of(Constants.PRIMARY_CATEGORY, "category", Constants.CHILDREN, List.of());

        when(assessUtilServ.readAssessmentHierarchyFromCache(any(), anyBoolean()))
                .thenReturn(assessmentDetail);

        var response = service.readQuestionList(requestBody, false);

        assertEquals(Constants.IDENTIFIER_LIST_IS_EMPTY, response.get(Constants.ERROR_MESSAGE));
    }

    @Test
    void testReadQuestionList_noUserAssessmentData(){
        Map<String, Object> requestBody = getValidRequest();

        Map<String, Object> assessmentDetail = Map.of(Constants.PRIMARY_CATEGORY, "category", Constants.CHILDREN, List.of(
                Map.of(Constants.CHILD_NODES, List.of("q1", "q2"))
        ));

        when(assessUtilServ.readAssessmentHierarchyFromCache(any(), anyBoolean()))
                .thenReturn(assessmentDetail);
        when(encryptionService.encryptData(any())).thenReturn("encrypted");
        when(assessUtilServ.readUserSubmittedAssessmentRecords(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        var response = service.readQuestionList(requestBody, false);

        assertEquals(Constants.USER_ASSESSMENT_DATA_NOT_PRESENT, response.get(Constants.ERROR_MESSAGE));
    }

    @Test
    void testReadQuestionList_invalidQuestionIds() throws Exception {
        Map<String, Object> requestBody = getValidRequest();

        Map<String, Object> assessmentDetail = Map.of(Constants.PRIMARY_CATEGORY, "category", Constants.CHILDREN, List.of(
                Map.of(Constants.CHILD_NODES, List.of("q1"))
        ));

        when(assessUtilServ.readAssessmentHierarchyFromCache(any(), anyBoolean()))
                .thenReturn(assessmentDetail);
        when(encryptionService.encryptData(any())).thenReturn("encrypted");
        when(assessUtilServ.readUserSubmittedAssessmentRecords(any(), any(), any()))
                .thenReturn(List.of(Map.of(Constants.ASSESSMENT_READ_RESPONSE_KEY, "{\"primaryCategory\":\"category\",\"children\":[{\"childNodes\":[\"q2\"]}]}")));
        when(mapper.readValue(anyString(), any(TypeReference.class)))
                .thenReturn(Map.of(Constants.PRIMARY_CATEGORY, "category", Constants.CHILDREN, List.of(Map.of(Constants.CHILD_NODES, List.of("q2")))));

        var response = service.readQuestionList(requestBody, false);

        assertEquals(Constants.THE_QUESTIONS_IDS_PROVIDED_DONT_MATCH, response.get(Constants.ERROR_MESSAGE));
    }

    @Test
    void testReadQuestionList_exception() {
        Map<String, Object> requestBody = getValidRequest();

        Map<String, Object> assessmentDetail = Map.of(Constants.PRIMARY_CATEGORY, "category", Constants.CHILDREN, List.of(
                Map.of(Constants.CHILD_NODES, List.of("q1", "q2"))
        ));

        when(assessUtilServ.readAssessmentHierarchyFromCache(any(), anyBoolean()))
                .thenReturn(assessmentDetail);
        when(encryptionService.encryptData(any())).thenReturn("encrypted");
        when(assessUtilServ.readUserSubmittedAssessmentRecords(any(), any(), any()))
                .thenThrow(new RuntimeException("boom"));

        var response = service.readQuestionList(requestBody, false);

        assertTrue(response.get(Constants.ERROR_MESSAGE).toString().contains("Failed to fetch the question list"));
    }

    @Test
    void testReadAssessmentResultV5_success() throws Exception {
        Map<String, Object> request = validRequest();
        String encryptedEmail = "encrypted-email";
        Map<String, Object> dbRecord = Map.of(
                Constants.STATUS, Constants.SUBMITTED,
                Constants.SUBMIT_ASSESSMENT_RESPONSE_KEY, "{\"result\":true}"
        );

        when(encryptionService.encryptData(anyString())).thenReturn(encryptedEmail);
        when(assessUtilServ.readUserSubmittedAssessmentRecords(eq(encryptedEmail), any(), any()))
                .thenReturn(List.of(dbRecord));
        when(mapper.readValue(anyString(), any(TypeReference.class)))
                .thenReturn(Map.of("result", true));

        SBApiResponse response = service.readAssessmentResultV5(request);

        assertNotNull(response);
        assertTrue(response.containsKey("result"));
    }

    @Test
    void testReadAssessmentResultV5_invalidEmail() {
        Map<String, Object> innerRequest = new HashMap<>();
        innerRequest.put(Constants.EMAIL, "bad-email");
        innerRequest.put(Constants.ASSESSMENT_IDENTIFIER, "assess123");
        innerRequest.put(Constants.CONTEXT_ID, "ctx123");

        Map<String, Object> request = Map.of(Constants.REQUEST, innerRequest);

        SBApiResponse response = service.readAssessmentResultV5(request);

        assertEquals(Constants.INVALID_EMAIL, response.getParams().getErrmsg());
    }

    @Test
    void testReadAssessmentResultV5_missingFields_emptyAssessment() {
        Map<String, Object> request = validRequest();
        ((Map<String, Object>) request.get(Constants.REQUEST)).put(Constants.ASSESSMENT_IDENTIFIER,"");

        when(encryptionService.encryptData(anyString())).thenReturn("enc");

        SBApiResponse response = service.readAssessmentResultV5(request);

        assertTrue(response.getParams().getErrmsg().contains(Constants.ASSESSMENT_IDENTIFIER));
    }

    @Test
    void testReadAssessmentResultV5_missingFields() {
        Map<String, Object> request = validRequest();
        ((Map<String, Object>) request.get(Constants.REQUEST)).remove(Constants.ASSESSMENT_IDENTIFIER);

        when(encryptionService.encryptData(anyString())).thenReturn("enc");

        SBApiResponse response = service.readAssessmentResultV5(request);

        assertTrue(response.getParams().getErrmsg().contains(Constants.ASSESSMENT_IDENTIFIER));
    }

    @Test
    void testReadAssessmentResultV5_missingFields_ContextField() {
        Map<String, Object> request = validRequest();
        ((Map<String, Object>) request.get(Constants.REQUEST)).put(Constants.CONTEXT_ID,"");

        when(encryptionService.encryptData(anyString())).thenReturn("enc");

        SBApiResponse response = service.readAssessmentResultV5(request);

        assertTrue(response.getParams().getErrmsg().contains(Constants.CONTEXT_ID));
    }

    @Test
    void testReadAssessmentResultV5_missingFields_ContextID() {
        Map<String, Object> request = validRequest();
        ((Map<String, Object>) request.get(Constants.REQUEST)).remove(Constants.CONTEXT_ID);

        when(encryptionService.encryptData(anyString())).thenReturn("enc");

        SBApiResponse response = service.readAssessmentResultV5(request);

        assertTrue(response.getParams().getErrmsg().contains(Constants.CONTEXT_ID));
    }
    @Test
    void testReadAssessmentResultV5_noUserData() {
        Map<String, Object> request = validRequest();

        when(encryptionService.encryptData(anyString())).thenReturn("enc");
        when(assessUtilServ.readUserSubmittedAssessmentRecords(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        SBApiResponse response = service.readAssessmentResultV5(request);

        assertEquals(Constants.USER_ASSESSMENT_DATA_NOT_PRESENT, response.getParams().getErrmsg());
    }

    @Test
    void testReadAssessmentResultV5_inProgressStatus() {
        Map<String, Object> request = validRequest();

        when(encryptionService.encryptData(anyString())).thenReturn("enc");
        when(assessUtilServ.readUserSubmittedAssessmentRecords(any(), any(), any()))
                .thenReturn(List.of(Map.of(Constants.STATUS, "NOT_SUBMITTED")));

        SBApiResponse response = service.readAssessmentResultV5(request);

        assertTrue((Boolean) response.getResult().get(Constants.STATUS_IS_IN_PROGRESS));
    }

    @Test
    void testReadAssessmentResultV5_exception() {
        Map<String, Object> request = validRequest();

        when(encryptionService.encryptData(anyString())).thenReturn("enc");
        when(assessUtilServ.readUserSubmittedAssessmentRecords(any(), any(), any()))
                .thenThrow(new RuntimeException("boom"));

        SBApiResponse response = service.readAssessmentResultV5(request);

        assertTrue(response.getParams().getErrmsg()
                .contains("Failed to process Assessment read response"));
    }

    @Test
    void testProcessNotification_notificationValidRequest() throws Exception {
        Map<String, Object> request = notificationValidRequest();

        when(mapper.writeValueAsString(any())).thenReturn("{}");
        when(cassandraOperation.getRecordsByProperties(any(), any(), any(), any()))
                .thenReturn(List.of(Map.of(Constants.TEMPLATE, "<html>${COURSE_NAME}</html>")));
        when(outboundRequestHandlerService.fetchResultUsingPost(any(), any(), any()))
                .thenReturn(Map.of());

        service.processNotification(request);

        verify(outboundRequestHandlerService).fetchResultUsingPost(any(), any(), any());
    }

    @Test
    void testProcessNotification_invalidRequest_missingAssessmentId() throws Exception {
        Map<String, Object> request = notificationValidRequest();
        request.remove(Constants.ASSESSMENT_ID_KEY);

        when(mapper.writeValueAsString(any())).thenReturn("{}");

        service.processNotification(request);
        // no exception thrown, logs the error
    }

    @Test
    void testProcessNotification_invalidRequest_missingUserId() throws Exception {
        Map<String, Object> request = notificationValidRequest();
        ((Map<String, Object>) request.get(Constants.E_DATA)).remove(Constants.USER_ID);

        when(mapper.writeValueAsString(any())).thenReturn("{}");

        service.processNotification(request);
    }

    @Test
    void testProcessNotification_invalidRequest_missingCourseId() throws Exception {
        Map<String, Object> request = notificationValidRequest();
        Map<String, Object> related = (Map<String, Object>) ((Map<String, Object>) request.get(Constants.E_DATA)).get(Constants.RELATED);
        related.remove(Constants.COURSE_ID);

        when(mapper.writeValueAsString(any())).thenReturn("{}");

        service.processNotification(request);
    }

    @Test
    void testProcessNotification_templateException() throws Exception {
        Map<String, Object> request = notificationValidRequest();

        when(mapper.writeValueAsString(any())).thenReturn("{}");
        when(cassandraOperation.getRecordsByProperties(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Template DB error"));

        service.processNotification(request);
    }

    @Test
    void testProcessNotification_notificationException() throws Exception {
        Map<String, Object> request = notificationValidRequest();

        when(mapper.writeValueAsString(any())).thenReturn("{}");
        when(outboundRequestHandlerService.fetchResultUsingPost(any(), any(), any()))
                .thenThrow(new RuntimeException("Notification error"));

        when(cassandraOperation.getRecordsByProperties(any(), any(), any(), any()))
                .thenReturn(List.of(Map.of(Constants.TEMPLATE, "<html>${COURSE_NAME}</html>")));

        service.processNotification(request);
    }

    @Test
    void testValidateAssessmentRequest_nullRequest() {
        String result = service.validateAssessmentRequest(null);
        assertTrue(result.contains("Request object is empty"));
    }

    @Test
    void testValidateAssessmentRequest_missingAssessmentId() {
        Map<String, Object> request = notificationValidRequest();
        request.remove(Constants.ASSESSMENT_ID_KEY);

        String result = service.validateAssessmentRequest(request);
        assertTrue(result.contains(Constants.ASSESSMENT_ID_KEY));
    }

    @Test
    void testValidateAssessmentRequest_missingUserId() {
        Map<String, Object> request = notificationValidRequest();
        ((Map<String, Object>) request.get(Constants.E_DATA)).remove(Constants.USER_ID);

        String result = service.validateAssessmentRequest(request);
        assertTrue(result.contains(Constants.USER_ID));
    }

    @Test
    void testValidateAssessmentRequest_missingCourseId() {
        Map<String, Object> request = notificationValidRequest();
        Map<String, Object> related = (Map<String, Object>) ((Map<String, Object>) request.get(Constants.E_DATA)).get(Constants.RELATED);
        related.remove(Constants.COURSE_ID);

        String result = service.validateAssessmentRequest(request);
        assertTrue(result.contains(Constants.COURSE_ID));
    }

    @Test
    void testValidateAssessmentRequest_notificationValidRequest() {
        Map<String, Object> request = notificationValidRequest();
        String result = service.validateAssessmentRequest(request);
        assertTrue(result.isEmpty() || result.equals(""));
    }

    @Test
    void testAssessmentCertificateReissue_invalidEmail() {
        Map<String, Object> request = assessmentCertificateReissueValidRequest();
        request.put(Constants.EMAIL, "bad-email");

        SBApiResponse response = service.assessmentCertificateReissue(request);

        assertTrue(response.getParams().getErrmsg().contains(Constants.INVALID_EMAIL));
    }

    @Test
    void testAssessmentCertificateReissue_missingParams() {
        Map<String, Object> request = new HashMap<>();

        SBApiResponse response = service.assessmentCertificateReissue(request);

        assertTrue(response.getParams().getErrmsg().contains(Constants.INVALID_REQUEST));
    }

    @Test
    void testAssessmentCertificateReissue_exception() {
        Map<String, Object> request = assessmentCertificateReissueValidRequest();

        when(encryptionService.encryptData(any())).thenThrow(new RuntimeException("boom"));

        SBApiResponse response = service.assessmentCertificateReissue(request);

        assertTrue(response.getParams().getErrmsg().contains("Failed to read user assessment"));
    }

    @Test
    void testProcessDownloadNotification_validFlow() throws Exception {
        Map<String, Object> request = downloadNotificationValidRequest();

        when(serverProperties.getCloudStorageUrl()).thenReturn("https://cloud/");
        when(serverProperties.getPublicUserAssessmentData()).thenReturn("user_assessment_table");
        when(serverProperties.getPublicAccessUrl()).thenReturn("https://public/");
        when(serverProperties.getContentHierarchyNamespace()).thenReturn("content_namespace");
        when(serverProperties.getContentHierarchyTable()).thenReturn("content_table");
        when(serverProperties.getPublicAssessmentCertificateTemplate()).thenReturn("templateName");
        when(serverProperties.getSupportEmail()).thenReturn("support@example.com");
        when(serverProperties.getNotifyServiceHost()).thenReturn("http://notify-service");
        when(serverProperties.getNotifyServicePathAsync()).thenReturn("/notify/async");

        when(encryptionService.encryptData(anyString())).thenReturn("encrypted@email");

        // mock first cassandra call (user assessment data)
        when(cassandraOperation.getRecordsByPropertiesWithoutFiltering(
                eq(Constants.SUNBIRD_KEY_SPACE_NAME),
                eq("user_assessment_table"),
                anyMap(),
                isNull(),
                isNull()
        )).thenReturn(
                List.of(
                        Map.of("cert_publicurl", "https://cloud/some/path/certificate.pdf")
                )
        );

        // mock second cassandra call (content hierarchy)
        when(cassandraOperation.getRecordsByProperties(
                eq("content_namespace"),
                eq("content_table"),
                anyMap(),
                isNull()
        )).thenReturn(
                List.of(
                        Map.of(Constants.HIERARCHY, "{\"name\":\"Sample Course\",\"posterImage\":\"poster.jpg\"}")
                )
        );

        // mock ObjectMapper to parse hierarchy JSON
        Map<String, Object> hierarchyMap = new HashMap<>();
        hierarchyMap.put(Constants.NAME, "Sample Course");
        hierarchyMap.put(Constants.POSTER_IMAGE, "poster.jpg");

        when(mapper.readValue(anyString(), eq(HashMap.class)))
                .thenReturn((HashMap) hierarchyMap);


        // mock outbound call
        when(outboundRequestHandlerService.fetchResultUsingPost(anyString(), any(), isNull()))
                .thenReturn(Map.of("status", "OK"));

        service.processDownloadNotification(request);

        // verify outbound call was made
        verify(outboundRequestHandlerService).fetchResultUsingPost(anyString(), any(), isNull());
    }


    @Test
    void testProcessDownloadNotification_emptyCassandraResponse() {
        Map<String, Object> request = downloadNotificationValidRequest();

        when(encryptionService.encryptData(anyString())).thenReturn("encrypted@email");
        when(cassandraOperation.getRecordsByPropertiesWithoutFiltering(any(), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        service.processDownloadNotification(request);

        // Should log but not throw
        verifyNoInteractions(outboundRequestHandlerService);
    }

    @Test
    void testProcessDownloadNotification_exceptionInFlow() {
        Map<String, Object> request = downloadNotificationValidRequest();

        when(encryptionService.encryptData(anyString())).thenThrow(new RuntimeException("boom"));

        service.processDownloadNotification(request);

        // Should catch and log exception
        verifyNoInteractions(outboundRequestHandlerService);
    }

    private Map<String, Object> downloadNotificationValidRequest() {
        Map<String, Object> map = new HashMap<>();
        map.put("userid", "user@email.com");
        map.put("courseid", "course123");
        map.put("assessmentid", "assess123");
        return map;
    }

    private Map<String, Object> assessmentCertificateReissueValidRequest() {
        Map<String, Object> map = new HashMap<>();
        map.put(Constants.EMAIL, "test@email.com");
        map.put(Constants.ASSESSMENT_IDENTIFIER, "assessment123");
        map.put(Constants.CONTEXT_ID, "context456");
        return map;
    }

    private Map<String, Object> notificationValidRequest() {
        Map<String, Object> related = new HashMap<>();
        related.put(Constants.COURSE_ID, "course123");

        Map<String, Object> edata = new HashMap<>();
        edata.put(Constants.USER_ID, "user1@example.com");
        edata.put(Constants.COURSE_NAME, "Course Name");
        edata.put(Constants.COURSE_POSTER_IMAGE, "image.png");
        edata.put(Constants.RELATED, related);

        Map<String, Object> request = new HashMap<>();
        request.put(Constants.ASSESSMENT_ID_KEY, "assessment123");
        request.put(Constants.E_DATA, edata);

        return request;
    }

    private Map<String, Object> validRequest() {
        Map<String, Object> innerRequest = new HashMap<>();
        innerRequest.put(Constants.EMAIL, "user@example.com");
        innerRequest.put(Constants.ASSESSMENT_IDENTIFIER, "assess123");
        innerRequest.put(Constants.CONTEXT_ID, "ctx123");

        Map<String, Object> outerRequest = new HashMap<>();
        outerRequest.put(Constants.REQUEST, innerRequest);

        return outerRequest;
    }


    private Map<String, Object> getValidRequest() {
        return Map.of(
                Constants.ASSESSMENT_IDENTIFIER, "a1",
                Constants.CONTEXT_ID, "ctx",
                Constants.EMAIL, "user@example.com",
                Constants.REQUEST, Map.of(
                        Constants.SEARCH, Map.of(
                                Constants.IDENTIFIER, List.of("q1")
                        )
                )
        );
    }

}
