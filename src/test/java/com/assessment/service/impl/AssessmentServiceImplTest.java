package com.assessment.service.impl;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.assessment.datasecurity.EncryptionService;
import com.assessment.model.SBApiResponse;
import com.assessment.repo.AssessmentRepository;
import com.assessment.service.AssessmentUtilServiceV2;
import com.assessment.util.Constants;
import com.assessment.util.ServerProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.HttpStatus;

import java.text.SimpleDateFormat;
import java.util.*;

class AssessmentServiceImplTest {

    @InjectMocks
    private AssessmentServiceImpl service;

    @Mock
    private AssessmentUtilServiceV2 utilService;

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private ServerProperties serverProperties;

    @Mock
    private AssessmentRepository repository;

    @Mock
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private Map<String, Object> createRequest() {
        Map<String, Object> req = new HashMap<>();
        req.put(Constants.EMAIL, "test@example.com");
        req.put(Constants.NAME, "Test User");
        req.put(Constants.ASSESSMENT_IDENTIFIER, "assess123");
        req.put(Constants.CONTEXT_ID, "ctx123");
        return req;
    }

    private Map<String, Object> createAssessmentDetail(boolean includeExpectedDuration) {
        Map<String, Object> assessment = new HashMap<>();
        assessment.put(Constants.PRIMARY_CATEGORY, Constants.PRACTICE_QUESTION_SET);
        assessment.put(Constants.EXPECTED_DURATION, includeExpectedDuration ? 30 : null);

        Map<String, Object> section = new HashMap<>();
        section.put(Constants.IDENTIFIER, "sec1");
        section.put(Constants.MAX_QUESTIONS, 1);
        List<Map<String, Object>> questions = new ArrayList<>();
        Map<String, Object> q1 = new HashMap<>();
        q1.put(Constants.IDENTIFIER, "q1");
        questions.add(q1);
        section.put(Constants.CHILDREN, questions);

        List<Map<String, Object>> sections = new ArrayList<>();
        sections.add(section);

        assessment.put(Constants.CHILDREN, sections);

        return assessment;
    }

    @Test
    void testReadAssessment_InvalidEmail() {
        Map<String, Object> req = createRequest();
        req.put(Constants.EMAIL, "invalid-email");
        when(encryptionService.encryptData(any())).thenReturn("encrypted");

        SBApiResponse res = service.readAssessment(false, req);

        assertEquals(HttpStatus.BAD_REQUEST, res.getResponseCode());
    }

    @Test
    void testReadAssessment_EditMode_PracticeQuestionSet() {
        Map<String, Object> req = createRequest();
        when(encryptionService.encryptData(any())).thenReturn("encrypted");
        when(utilService.fetchHierarchyFromAssessServc(any()))
                .thenReturn(createAssessmentDetail(true));

        SBApiResponse res = service.readAssessment(true, req);

        assertEquals(HttpStatus.OK, res.getResponseCode());
        assertNotNull(res.getResult().get(Constants.QUESTION_SET));
    }

    @Test
    void testReadAssessment_NonEditMode_HierarchyEmpty() {
        Map<String, Object> req = createRequest();
        when(encryptionService.encryptData(any())).thenReturn("encrypted");
        when(utilService.readAssessmentHierarchyFromCache(any(), anyBoolean()))
                .thenReturn(Collections.emptyMap());

        SBApiResponse res = service.readAssessment(false, req);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, res.getResponseCode());
    }

    @Test
    void testReadAssessment_FirstTime_NoExistingData() {
        Map<String, Object> req = createRequest();
        Map<String, Object> assessment = createAssessmentDetail(true);

        when(encryptionService.encryptData(any())).thenReturn("encrypted");
        when(utilService.readAssessmentHierarchyFromCache(any(), anyBoolean()))
                .thenReturn(assessment);
        when(utilService.readUserSubmittedAssessmentRecords(any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(repository.addUserAssesmentDataToDB(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(true);
        when(serverProperties.getAssessmentLevelParams()).thenReturn(List.of(Constants.PRIMARY_CATEGORY));
        when(serverProperties.getAssessmentSectionParams()).thenReturn(List.of(Constants.IDENTIFIER, Constants.MAX_QUESTIONS));

        SBApiResponse res = service.readAssessment(false, req);

        assertEquals(HttpStatus.OK, res.getResponseCode());
        assertNotNull(res.getResult().get(Constants.QUESTION_SET));
    }

    @Test
    void testReadAssessment_Existing_NotSubmitted() {
        Map<String, Object> req = createRequest();
        Map<String, Object> assessment = createAssessmentDetail(true);

        when(encryptionService.encryptData(any())).thenReturn("encrypted");
        when(utilService.readAssessmentHierarchyFromCache(any(), anyBoolean()))
                .thenReturn(assessment);

        Map<String, Object> existing = new HashMap<>();
        existing.put(Constants.END_TIME, new Date(System.currentTimeMillis() + 100000));
        existing.put(Constants.STATUS, Constants.NOT_SUBMITTED);
        existing.put(Constants.ASSESSMENT_READ_RESPONSE_KEY, "{\"dummy\":\"value\"}");
        when(utilService.readUserSubmittedAssessmentRecords(any(), any(), any()))
                .thenReturn(List.of(existing));

        when(serverProperties.getAssessmentLevelParams()).thenReturn(List.of(Constants.PRIMARY_CATEGORY));
        when(serverProperties.getAssessmentSectionParams()).thenReturn(List.of(Constants.IDENTIFIER, Constants.MAX_QUESTIONS));

        SBApiResponse res = service.readAssessment(false, req);

        assertEquals(HttpStatus.OK, res.getResponseCode());
        assertNotNull(res.getResult().get(Constants.QUESTION_SET));
    }

    @Test
    void testReadAssessment_Existing_Submitted_Fail() {
        Map<String, Object> req = createRequest();
        Map<String, Object> assessment = createAssessmentDetail(true);

        when(encryptionService.encryptData(any())).thenReturn("encrypted");
        when(utilService.readAssessmentHierarchyFromCache(any(), anyBoolean()))
                .thenReturn(assessment);

        Map<String, Object> existing = new HashMap<>();
        existing.put(Constants.END_TIME, new Date());
        existing.put(Constants.STATUS, Constants.SUBMITTED);
        existing.put(Constants.PASS_STATUS, false);
        when(utilService.readUserSubmittedAssessmentRecords(any(), any(), any()))
                .thenReturn(List.of(existing));

        when(repository.addUserAssesmentDataToDB(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(true);
        when(serverProperties.getAssessmentLevelParams()).thenReturn(List.of(Constants.PRIMARY_CATEGORY));
        when(serverProperties.getAssessmentSectionParams()).thenReturn(List.of(Constants.IDENTIFIER, Constants.MAX_QUESTIONS));

        SBApiResponse res = service.readAssessment(false, req);

        assertEquals(HttpStatus.OK, res.getResponseCode());
        assertNotNull(res.getResult().get(Constants.QUESTION_SET));
    }

    @Test
    void testReadAssessment_Existing_Submitted_Pass() {
        Map<String, Object> req = createRequest();
        Map<String, Object> assessment = createAssessmentDetail(true);

        when(encryptionService.encryptData(any())).thenReturn("encrypted");
        when(utilService.readAssessmentHierarchyFromCache(any(), anyBoolean()))
                .thenReturn(assessment);

        Map<String, Object> existing = new HashMap<>();
        existing.put(Constants.END_TIME, new Date());
        existing.put(Constants.STATUS, Constants.SUBMITTED);
        existing.put(Constants.PASS_STATUS, true);
        when(utilService.readUserSubmittedAssessmentRecords(any(), any(), any()))
                .thenReturn(List.of(existing));

        SBApiResponse res = service.readAssessment(false, req);

        assertEquals(HttpStatus.OK, res.getResponseCode());
    }

    @Test
    void testReadAssessment_Existing_Submitted_Pass_1() {
        Map<String, Object> req = createRequest();
        Map<String, Object> assessment = createAssessmentDetail(true);
        assessment.put(Constants.PRIMARY_CATEGORY, "Question Set");


        when(encryptionService.encryptData(any())).thenReturn("encrypted");
        when(utilService.readAssessmentHierarchyFromCache(any(), anyBoolean()))
                .thenReturn(assessment);

        Map<String, Object> existing = new HashMap<>();
        existing.put(Constants.END_TIME, new Date());
        existing.put(Constants.STATUS, Constants.SUBMITTED);
        existing.put(Constants.PASS_STATUS, true);
        when(utilService.readUserSubmittedAssessmentRecords(any(), any(), any()))
                .thenReturn(List.of(existing));

        SBApiResponse res = service.readAssessment(false, req);

        assertEquals(HttpStatus.OK, res.getResponseCode());
        assertEquals("User has already submitted the assessment",res.getResult().get("response"));
    }

    @Test
    void testReadAssessment_InternalServerError() {
        Map<String, Object> req = createRequest();
        Map<String, Object> assessment = createAssessmentDetail(true);
        assessment.put(Constants.PRIMARY_CATEGORY, "Question Set");


        when(encryptionService.encryptData(any())).thenReturn("encrypted");
        when(utilService.readAssessmentHierarchyFromCache(any(), anyBoolean()))
                .thenReturn(assessment);

        Map<String, Object> existing = new HashMap<>();
        existing.put(Constants.END_TIME, new Date());
        existing.put(Constants.STATUS, Constants.SUBMITTED);
        existing.put(Constants.PASS_STATUS, true);
        when(utilService.readUserSubmittedAssessmentRecords(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        SBApiResponse res = service.readAssessment(false, req);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, res.getResponseCode());
        assertEquals("Assessment Data & Start Time not updated in the DB.",res.getParams().getErrmsg());
    }

    @Test
    void testReadAssessment_Existing_NotSubmitted_AndWithinTime() {
        Map<String, Object> req = createRequest();
        Map<String, Object> assessment = createAssessmentDetail(true);
        assessment.put(Constants.PRIMARY_CATEGORY, "Question Set");

        when(encryptionService.encryptData(any())).thenReturn("encrypted");
        when(utilService.readAssessmentHierarchyFromCache(any(), anyBoolean()))
                .thenReturn(assessment);

        // existing record with END_TIME > now, STATUS == NOT_SUBMITTED
        Map<String, Object> existing = new HashMap<>();
        existing.put(Constants.END_TIME, new Date(System.currentTimeMillis() + 30_000));  // 30 sec in future
        existing.put(Constants.STATUS, Constants.NOT_SUBMITTED);

        // prepare a JSON string representing saved question set
        Map<String, Object> savedQSet = new HashMap<>();
        savedQSet.put("dummyKey", "dummyValue");
        String savedQSetJson = new Gson().toJson(savedQSet);
        existing.put(Constants.ASSESSMENT_READ_RESPONSE_KEY, savedQSetJson);

        when(utilService.readUserSubmittedAssessmentRecords(any(), any(), any()))
                .thenReturn(List.of(existing));

        SBApiResponse res = service.readAssessment(false, req);

        assertEquals(HttpStatus.OK, res.getResponseCode());

        // Assert the response contains the updated QUESTION_SET
        Map<String, Object> questionSet =
                (Map<String, Object>) res.getResult().get(Constants.QUESTION_SET);

        assertNotNull(questionSet);
        assertEquals("dummyValue", questionSet.get("dummyKey"));
        assertTrue(questionSet.containsKey(Constants.START_TIME));
        assertTrue(questionSet.containsKey(Constants.END_TIME));
    }

    @Test
    void testReadAssessment_Existing_Submitted_Pass_2() {
        Map<String, Object> req = createRequest();
        Map<String, Object> assessment = createAssessmentDetail(true);
        assessment.put(Constants.PRIMARY_CATEGORY, "Question Set");

        when(encryptionService.encryptData(any())).thenReturn("encrypted");
        when(utilService.readAssessmentHierarchyFromCache(any(), anyBoolean()))
                .thenReturn(assessment);

        Map<String, Object> existing = new HashMap<>();
        existing.put(Constants.END_TIME, parseDate("2025-07-16 09:00:00"));
        existing.put(Constants.STATUS, Constants.SUBMITTED);   // ✅ FIXED
        existing.put(Constants.PASS_STATUS, false);             // ✅ keep
        when(utilService.readUserSubmittedAssessmentRecords(any(), any(), any()))
                .thenReturn(List.of(existing));

        SBApiResponse res = service.readAssessment(false, req);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, res.getResponseCode());
        assertEquals("Assessment Data & Start Time not updated in the DB.", res.getParams().getErrmsg());
    }


    @Test
    void testReadAssessment_Exception() {
        Map<String, Object> req = createRequest();

        when(encryptionService.encryptData(any())).thenThrow(new RuntimeException("Test Exception"));

        SBApiResponse res = service.readAssessment(false, req);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, res.getResponseCode());
    }
    @Test
    void testMissingAssessmentId() {
        Map<String, Object> req = requestWithIdentifiers();
        req.remove(Constants.ASSESSMENT_IDENTIFIER);

        SBApiResponse response = service.readQuestionList(req, false);

        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
    }

    @Test
    void testEmptyIdentifierList() {
        Map<String, Object> req = requestWithIdentifiers();
        setIdentifiers(req, Collections.emptyList());

        SBApiResponse response = service.readQuestionList(req, false);

        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
    }

    @Test
    void testAssessmentHierarchyEmpty() {
        Map<String, Object> req = requestWithIdentifiers();

        when(encryptionService.encryptData(any())).thenReturn("encrypted");
        when(utilService.readAssessmentHierarchyFromCache(any(), anyBoolean()))
                .thenReturn(Collections.emptyMap());

        SBApiResponse response = service.readQuestionList(req, false);

        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
    }

    @Test
    void testPracticeQuestionSet() {
        Map<String, Object> req = requestWithIdentifiers();
        Map<String, Object> assessment = Map.of(Constants.PRIMARY_CATEGORY, Constants.PRACTICE_QUESTION_SET);

        when(encryptionService.encryptData(any())).thenReturn("encrypted");
        when(utilService.readAssessmentHierarchyFromCache(any(), anyBoolean()))
                .thenReturn(assessment);

        SBApiResponse response = service.readQuestionList(req, false);

        assertEquals(HttpStatus.OK, response.getResponseCode());
    }

    @Test
    void testUserAssessmentDataNotPresent() {
        Map<String, Object> req = requestWithIdentifiers();
        Map<String, Object> assessment = Map.of(Constants.PRIMARY_CATEGORY, "SomeOtherCategory");

        when(encryptionService.encryptData(any())).thenReturn("encrypted");
        when(utilService.readAssessmentHierarchyFromCache(any(), anyBoolean()))
                .thenReturn(assessment);
        when(utilService.readUserSubmittedAssessmentRecords(any(), any(), any()))
                .thenReturn(List.of());

        SBApiResponse response = service.readQuestionList(req, false);

        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
    }

    @Test
    void testUserAssessmentInvalidQuestionIds() throws Exception {
        Map<String, Object> req = requestWithIdentifiers();

        Map<String, Object> assessment = Map.of(Constants.PRIMARY_CATEGORY, "OtherCategory");
        Map<String, Object> userAssessment = new HashMap<>();
        userAssessment.put(Constants.PRIMARY_CATEGORY, "OtherCategory");

        Map<String, Object> section = new HashMap<>();
        section.put(Constants.CHILD_NODES, List.of("q1", "q2"));

        userAssessment.put(Constants.CHILDREN, List.of(section));

        when(encryptionService.encryptData(any())).thenReturn("encrypted");
        when(utilService.readAssessmentHierarchyFromCache(any(), anyBoolean()))
                .thenReturn(assessment);
        when(utilService.readUserSubmittedAssessmentRecords(any(), any(), any()))
                .thenReturn(List.of(Map.of(Constants.ASSESSMENT_READ_RESPONSE_KEY, "{\"primaryCategory\":\"OtherCategory\",\"children\":[{\"childNodes\":[\"q1\",\"q2\"]}]}")));

        when(mapper.readValue(anyString(), ArgumentMatchers.<TypeReference<Map<String, Object>>>any()))
                .thenReturn(userAssessment);

        // purposefully request ids that don't match
        setIdentifiers(req, List.of("q3"));

        SBApiResponse response = service.readQuestionList(req, false);

        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
    }

    @Test
    void testUserAssessmentValid() throws Exception {
        Map<String, Object> req = requestWithIdentifiers();

        Map<String, Object> assessment = Map.of(Constants.PRIMARY_CATEGORY, "OtherCategory");
        Map<String, Object> userAssessment = new HashMap<>();
        userAssessment.put(Constants.PRIMARY_CATEGORY, "OtherCategory");

        Map<String, Object> section = new HashMap<>();
        section.put(Constants.CHILD_NODES, List.of("q1"));

        userAssessment.put(Constants.CHILDREN, List.of(section));

        when(encryptionService.encryptData(any())).thenReturn("encrypted");
        when(utilService.readAssessmentHierarchyFromCache(any(), anyBoolean()))
                .thenReturn(assessment);
        when(utilService.readUserSubmittedAssessmentRecords(any(), any(), any()))
                .thenReturn(List.of(Map.of(Constants.ASSESSMENT_READ_RESPONSE_KEY, "{\"primaryCategory\":\"OtherCategory\",\"children\":[{\"childNodes\":[\"q1\"]}]}")));

        when(mapper.readValue(anyString(), ArgumentMatchers.<TypeReference<Map<String, Object>>>any()))
                .thenReturn(userAssessment);

        when(utilService.readQListfromCache(any(), any(), anyBoolean()))
                .thenReturn(Map.of("q1", Map.of("id", "q1")));
        when(utilService.filterQuestionMapDetail(any(), any()))
                .thenReturn(Map.of("id", "q1"));

        SBApiResponse response = service.readQuestionList(req, false);

        assertEquals(HttpStatus.OK, response.getResponseCode());
        assertTrue(response.getResult().containsKey(Constants.QUESTIONS));
    }

    @Test
    void testExceptionCase() {
        Map<String, Object> req = requestWithIdentifiers();

        when(encryptionService.encryptData(any())).thenThrow(new RuntimeException("boom"));

        SBApiResponse response = service.readQuestionList(req, false);

        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
    }

    private Map<String, Object> requestWithIdentifiers() {
        Map<String, Object> req = new HashMap<>();
        req.put(Constants.ASSESSMENT_IDENTIFIER, "assess1");
        req.put(Constants.EMAIL, "user@example.com");
        req.put(Constants.CONTEXT_ID, "ctx1");

        setIdentifiers(req, List.of("q1"));

        return req;
    }

    private void setIdentifiers(Map<String, Object> req, List<String> ids) {
        Map<String, Object> search = new HashMap<>();
        search.put(Constants.IDENTIFIER, ids);

        Map<String, Object> request = new HashMap<>();
        request.put(Constants.SEARCH, search);

        req.put(Constants.REQUEST, request);
    }

    private Date parseDate(String dateStr) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(dateStr);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
