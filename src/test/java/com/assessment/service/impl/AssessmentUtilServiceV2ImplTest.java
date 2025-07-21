package com.assessment.service.impl;

import com.assessment.cache.RedisCacheMgr;
import com.assessment.cassandra.utils.CassandraOperation;
import com.assessment.service.OutboundRequestHandlerServiceImpl;
import com.assessment.util.Constants;
import com.assessment.util.ServerProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections.MapUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AssessmentUtilServiceV2ImplTest {

    @InjectMocks
    private AssessmentUtilServiceV2Impl service;

    @Mock
    private ServerProperties serverProperties;
    @Mock
    private OutboundRequestHandlerServiceImpl outboundRequestHandlerService;
    @Mock
    private ObjectMapper mapper;
    @Mock
    private CassandraOperation cassandraOperation;
    @Mock
    private RedisCacheMgr redisCacheMgr;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void validateQumlAssessment_AllCorrect() {
        List<String> originalQuestionList = List.of("q1");
        Map<String, Object> questionMap = new HashMap<>();
        Map<String, Object> editorState = new HashMap<>();
        Map<String, Object> option = new HashMap<>();
        option.put(Constants.SELECTED_ANSWER, true);
        option.put(Constants.INDEX, "A");
        option.put(Constants.ANSWER, true);
        option.put(Constants.VALUE, Map.of(Constants.VALUE, "A"));
        editorState.put(Constants.OPTIONS, List.of(option));
        Map<String, Object> question = new HashMap<>();
        question.put(Constants.IDENTIFIER, "q1");
        question.put(Constants.QUESTION_TYPE, Constants.MCQ_SCA);
        question.put(Constants.EDITOR_STATE, editorState);
        questionMap.put("q1", question);

        List<Map<String, Object>> userQuestionList = List.of(new HashMap<>(question));
        userQuestionList.get(0).put(Constants.EDITOR_STATE, editorState);

        Map<String, Object> result = service.validateQumlAssessment(originalQuestionList, userQuestionList, questionMap);

        assertEquals(1, result.get(Constants.CORRECT));
        assertEquals(0, result.get(Constants.INCORRECT));
        assertEquals(0, result.get(Constants.BLANK));
        assertEquals(1, result.get(Constants.TOTAL));
    }

    @Test
    void validateQumlAssessment_BlankAnswer() {
        List<String> originalQuestionList = List.of("q1");
        Map<String, Object> questionMap = new HashMap<>();
        Map<String, Object> editorState = new HashMap<>();
        Map<String, Object> option = new HashMap<>();
        option.put(Constants.SELECTED_ANSWER, false);
        option.put(Constants.INDEX, "A");
        option.put(Constants.ANSWER, true);
        option.put(Constants.VALUE, Map.of(Constants.VALUE, "A"));
        editorState.put(Constants.OPTIONS, List.of(option));
        Map<String, Object> question = new HashMap<>();
        question.put(Constants.IDENTIFIER, "q1");
        question.put(Constants.QUESTION_TYPE, Constants.MCQ_SCA);
        question.put(Constants.EDITOR_STATE, editorState);
        questionMap.put("q1", question);

        List<Map<String, Object>> userQuestionList = List.of(new HashMap<>(question));
        userQuestionList.get(0).put(Constants.EDITOR_STATE, editorState);

        Map<String, Object> result = service.validateQumlAssessment(originalQuestionList, userQuestionList, questionMap);

        assertEquals(0, result.get(Constants.CORRECT));
        assertEquals(0, result.get(Constants.INCORRECT));
        assertEquals(1, result.get(Constants.BLANK));
        assertEquals(1, result.get(Constants.TOTAL));
    }

    @Test
    void validateQumlAssessment_Exception() {
        Map<String, Object> result = service.validateQumlAssessment(null, null, null);
        assertTrue(result.isEmpty());
    }

    @Test
    void shuffleOptions_ReturnsShuffledList() {
        Map<String, Object> option1 = Map.of("id", 1);
        Map<String, Object> option2 = Map.of("id", 2);
        List<Map<String, Object>> options = List.of(option1, option2);

        List<Map<String, Object>> shuffled = AssessmentUtilServiceV2Impl.shuffleOptions(options);

        assertEquals(2, shuffled.size());
        assertTrue(shuffled.containsAll(options));
    }

    @Test
    void filterQuestionMapDetail_PracticeQuestionSet() {
        Map<String, Object> questionMapResponse = new HashMap<>();
        questionMapResponse.put(Constants.EDITOR_STATE, Map.of("foo", "bar"));
        questionMapResponse.put(Constants.PRIMARY_CATEGORY, Constants.PRACTICE_QUESTION_SET);
        when(serverProperties.getAssessmentQuestionParams()).thenReturn(List.of(Constants.PRIMARY_CATEGORY));

        Map<String, Object> result = service.filterQuestionMapDetail(questionMapResponse, Constants.PRACTICE_QUESTION_SET);

        assertTrue(result.containsKey(Constants.EDITOR_STATE));
        assertEquals(Constants.PRACTICE_QUESTION_SET, result.get(Constants.PRIMARY_CATEGORY));
    }

    @Test
    void filterQuestionMapDetail_WithChoices() {
        Map<String, Object> questionMapResponse = new HashMap<>();
        questionMapResponse.put(Constants.CHOICES, Map.of(Constants.OPTIONS, List.of(Map.of("id", 1))));
        questionMapResponse.put(Constants.PRIMARY_CATEGORY, "MCQ");
        when(serverProperties.getAssessmentQuestionParams()).thenReturn(List.of(Constants.PRIMARY_CATEGORY));

        Map<String, Object> result = service.filterQuestionMapDetail(questionMapResponse, "MCQ");

        assertTrue(result.containsKey(Constants.CHOICES));
    }

    @Test
    void filterQuestionMapDetail_WithRhsChoices() {
        Map<String, Object> questionMapResponse = new HashMap<>();
        questionMapResponse.put(Constants.RHS_CHOICES, new ArrayList<>(List.of("A", "B")));
        questionMapResponse.put(Constants.PRIMARY_CATEGORY, Constants.MTF_QUESTION);
        when(serverProperties.getAssessmentQuestionParams()).thenReturn(List.of(Constants.PRIMARY_CATEGORY));

        Map<String, Object> result = service.filterQuestionMapDetail(questionMapResponse, Constants.MTF_QUESTION);

        assertTrue(result.containsKey(Constants.RHS_CHOICES));
    }

    @Test
    void readQuestionDetails_ExceptionReturnsEmptyList() {
        when(serverProperties.getAssessmentHost()).thenThrow(new RuntimeException("fail"));
        List<Map<String, Object>> result = service.readQuestionDetails(List.of("q1"));
        assertTrue(result.isEmpty());
    }

    @Test
    void getReadHierarchyApiResponse_ExceptionReturnsEmptyMap() {
        when(serverProperties.getAssessmentHost()).thenThrow(new RuntimeException("fail"));
        Map<String, Object> result = service.getReadHierarchyApiResponse("id");
        assertTrue(result.isEmpty());
    }

    @Test
    void fetchWheebox_EmptyCacheReturnsEmptyMap() {
        when(serverProperties.getRedisWheeboxKey()).thenReturn("wheebox");
        when(redisCacheMgr.getContentFromCache(anyString())).thenReturn("");
        Map<String, Object> result = service.fetchWheebox("user1");
        assertTrue(result.isEmpty());
    }

    @Test
    void fetchWheebox_CachePresentReturnsMap() throws Exception {
        when(serverProperties.getRedisWheeboxKey()).thenReturn("wheebox");
        when(redisCacheMgr.getContentFromCache(anyString())).thenReturn("{\"foo\":\"bar\"}");
        when(mapper.readValue(anyString(), ArgumentMatchers.<TypeReference<Map<String, Object>>>any()))
                .thenReturn(Map.of("foo", "bar"));
        Map<String, Object> result = service.fetchWheebox("user1");
        assertEquals("bar", result.get("foo"));
    }

    @Test
    void testFetchQuestionIdentifierValue_success() throws Exception {
        List<String> identifiers = List.of("q1");
        List<Object> questionList = new ArrayList<>();

        Map<String, Object> question = new HashMap<>();
        question.put(Constants.IDENTIFIER, "q1");
        question.put(Constants.RESULT, "OK");

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put(Constants.QUESTIONS, List.of(new HashMap<>()));

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put(Constants.RESPONSE_CODE, Constants.OK);
        responseMap.put(Constants.RESULT, resultMap);

        List<Map<String, Object>> apiResponse = List.of(responseMap);

        AssessmentUtilServiceV2Impl spyUtil = spy(service);
        doReturn(apiResponse).when(spyUtil).readQuestionDetails(any());

        doReturn(Map.of()).when(spyUtil).filterQuestionMapDetail(any(), any());

        String result = spyUtil.fetchQuestionIdentifierValue(identifiers, questionList, "category");

        assertEquals("", result);
        assertFalse(questionList.isEmpty());
    }

    @Test
    void testFetchQuestionIdentifierValue_failureOnAPI() throws Exception {
        List<String> identifiers = List.of("q1");
        List<Object> questionList = new ArrayList<>();

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put(Constants.RESPONSE_CODE, "ERROR");

        List<Map<String, Object>> apiResponse = List.of(responseMap);

        AssessmentUtilServiceV2Impl spyUtil = spy(service);
        doReturn(apiResponse).when(spyUtil).readQuestionDetails(any());

        String result = spyUtil.fetchQuestionIdentifierValue(identifiers, questionList, "category");

        assertTrue(result.contains("Failed"));
    }

    @Test
    void testReadQuestionDetails_success() {
        when(serverProperties.getAssessmentHost()).thenReturn("http://host");
        when(serverProperties.getAssessmentQuestionListPath()).thenReturn("/path");
        when(serverProperties.getSbApiKey()).thenReturn("api-key");

        when(outboundRequestHandlerService.fetchResultUsingPost(any(), any(), any()))
                .thenReturn(Map.of("data", "value"));

        List<String> identifiers = Arrays.asList("q1", "q2", "q3");
        List<Map<String, Object>> result = service.readQuestionDetails(identifiers);

        assertFalse(result.isEmpty());
    }

    @Test
    void testReadQuestionDetails_withException() {
        when(serverProperties.getAssessmentHost()).thenReturn(null);

        List<Map<String, Object>> result = service.readQuestionDetails(List.of("q1"));

        assertTrue(result.isEmpty());
    }

    @Test
    void testFilterQuestionMapDetail_withEditorStateAndChoices() {
        Map<String, Object> questionMap = new HashMap<>();
        questionMap.put("param1", "value1");
        questionMap.put("param2", "value2");
        questionMap.put(Constants.PRIMARY_CATEGORY, Constants.PRACTICE_QUESTION_SET);
        questionMap.put(Constants.EDITOR_STATE, Map.of("state", "something"));
        questionMap.put(Constants.CHOICES, Map.of(Constants.OPTIONS, List.of(
                Map.of("opt", "a"), Map.of("opt", "b"))));

        Map<String, Object> result = service.filterQuestionMapDetail(questionMap, Constants.PRACTICE_QUESTION_SET);

        assertNotNull(result);
        assertTrue(result.containsKey(Constants.EDITOR_STATE));
    }

    @Test
    void testFilterQuestionMapDetail_withRHSChoices() {
        Map<String, Object> questionMap = new HashMap<>();
        questionMap.put(Constants.PRIMARY_CATEGORY, Constants.MTF_QUESTION);
        questionMap.put(Constants.RHS_CHOICES, new ArrayList<>(List.of("a", "b")));

        Map<String, Object> result = service.filterQuestionMapDetail(questionMap, "any");

        assertNotNull(result);
    }


    @Test
    void testGetReadHierarchyApiResponse_success() {
        String assessmentId = "assessment123";
        String expectedUrl = "http://host/hierarchy/assessment123";
        Map<String, Object> mockResponse = Map.of("key", "value");

        // Mock serverProperties
        when(serverProperties.getAssessmentHost()).thenReturn("http://host");
        when(serverProperties.getAssessmentHierarchyReadPath()).thenReturn("/hierarchy/{id}");
        when(serverProperties.getSbApiKey()).thenReturn("dummy-api-key");

        // Mock outbound request
        when(outboundRequestHandlerService.fetchUsingGetWithHeaders(eq(expectedUrl), anyMap()))
                .thenReturn(mockResponse);

        Map<String, Object> result = service.getReadHierarchyApiResponse(assessmentId);

        assertNull(result);
    }

    @Test
    void testFetchHierarchyFromAssessServc_success() {
        String qSetId = "qset123";

        Map<String, Object> questionSetMap = new HashMap<>();
        questionSetMap.put("id", "qset123");

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put(Constants.QUESTION_SET, questionSetMap);

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put(Constants.RESPONSE_CODE, Constants.OK);
        responseMap.put(Constants.RESULT, resultMap);

        // Mock the internal call
        AssessmentUtilServiceV2Impl spy = Mockito.spy(service);
        doReturn(responseMap).when(spy).getReadHierarchyApiResponse(qSetId);

        Map<String, Object> output = spy.fetchHierarchyFromAssessServc(qSetId);

        assertNotNull(output);
        assertEquals("qset123", output.get("id"));
    }

    @Test
    void testFetchHierarchyFromAssessServc_failure_notOkResponseCode() {
        String qSetId = "qset123";

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put(Constants.RESPONSE_CODE, "ERROR");

        AssessmentUtilServiceV2Impl spy = Mockito.spy(service);
        doReturn(responseMap).when(spy).getReadHierarchyApiResponse(qSetId);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            spy.fetchHierarchyFromAssessServc(qSetId);
        });

        assertEquals("Internal Server Error", ex.getMessage());
    }

    @Test
    void testEditModeTrue_callsFetchHierarchy() {
        String assessmentIdentifier = "id123";
        Map<String, Object> expected = Map.of("key", "value");

        AssessmentUtilServiceV2Impl spy = spy(service);
        doReturn(expected).when(spy).fetchHierarchyFromAssessServc(assessmentIdentifier);

        Map<String, Object> result = spy.readAssessmentHierarchyFromCache(assessmentIdentifier, true);

        assertEquals(expected, result);
        verify(spy).fetchHierarchyFromAssessServc(assessmentIdentifier);
    }

    @Test
    void testRedisReturnsNonEmpty() throws Exception {
        String assessmentIdentifier = "id123";
        String json = "{\"foo\":\"bar\"}";
        Map<String, Object> parsedMap = Map.of("foo", "bar");

        when(serverProperties.qListFromCacheEnabled()).thenReturn(true);
        when(redisCacheMgr.getCache(anyString())).thenReturn(json);
        when(mapper.readValue(eq(json), ArgumentMatchers.<TypeReference<Map<String, Object>>>any()))
                .thenReturn(parsedMap);

        Map<String, Object> result = service.readAssessmentHierarchyFromCache(assessmentIdentifier, false);

        assertEquals(parsedMap, result);
        verify(mapper).readValue(eq(json), any(TypeReference.class));
    }

    @Test
    void testRedisReturnsEmpty_hierarchyFoundInCassandra() throws Exception {
        String assessmentIdentifier = "id123";

        when(serverProperties.qListFromCacheEnabled()).thenReturn(true);
        when(redisCacheMgr.getCache(anyString())).thenReturn("");

        Map<String, Object> cassandraRecord = new HashMap<>();
        cassandraRecord.put(Constants.HIERARCHY, "{\"bar\":\"baz\"}");

        List<Map<String, Object>> cassandraList = List.of(cassandraRecord);

        when(cassandraOperation.getRecordsByPropertiesWithoutFiltering(
                any(), any(), any(), any())).thenReturn(cassandraList);

        Map<String, Object> parsedMap = Map.of("bar", "baz");

        when(mapper.readValue(eq("{\"bar\":\"baz\"}"), ArgumentMatchers.<TypeReference<Map<String, Object>>>any()))
                .thenReturn(parsedMap);

        when(serverProperties.getRedisQuestionsReadTimeOut()).thenReturn(10);

        Map<String, Object> result = service.readAssessmentHierarchyFromCache(assessmentIdentifier, false);

        assertEquals(parsedMap, result);
        verify(redisCacheMgr).putCache(anyString(), eq(parsedMap), eq(10));
    }

    @Test
    void testRedisReturnsEmpty_cassandraReturnsEmpty() {
        String assessmentIdentifier = "id123";

        when(serverProperties.qListFromCacheEnabled()).thenReturn(true);
        when(redisCacheMgr.getCache(anyString())).thenReturn("");
        when(cassandraOperation.getRecordsByPropertiesWithoutFiltering(any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        Map<String, Object> result = service.readAssessmentHierarchyFromCache(assessmentIdentifier, false);

        assertEquals(MapUtils.EMPTY_MAP, result);
    }

    @Test
    void testRedisReturnsEmpty_hierarchyBlankInCassandra() {
        String assessmentIdentifier = "id123";

        when(serverProperties.qListFromCacheEnabled()).thenReturn(true);
        when(redisCacheMgr.getCache(anyString())).thenReturn("");

        Map<String, Object> cassandraRecord = new HashMap<>();
        cassandraRecord.put(Constants.HIERARCHY, "");

        List<Map<String, Object>> cassandraList = List.of(cassandraRecord);

        when(cassandraOperation.getRecordsByPropertiesWithoutFiltering(any(), any(), any(), any()))
                .thenReturn(cassandraList);

        Map<String, Object> result = service.readAssessmentHierarchyFromCache(assessmentIdentifier, false);

        assertEquals(MapUtils.EMPTY_MAP, result);
    }

    @Test
    void testRedisReturnsEmpty_jsonParsingFails() throws Exception {
        String assessmentIdentifier = "id123";

        when(serverProperties.qListFromCacheEnabled()).thenReturn(true);
        when(redisCacheMgr.getCache(anyString())).thenReturn("");

        Map<String, Object> cassandraRecord = new HashMap<>();
        cassandraRecord.put(Constants.HIERARCHY, "{\"invalid\":}");

        List<Map<String, Object>> cassandraList = List.of(cassandraRecord);

        when(cassandraOperation.getRecordsByPropertiesWithoutFiltering(any(), any(), any(), any()))
                .thenReturn(cassandraList);

        when(mapper.readValue(eq("{\"invalid\":}"), ArgumentMatchers.<TypeReference<Map<String, Object>>>any()))
                .thenThrow(new IOException("bad json"));

        Map<String, Object> result = service.readAssessmentHierarchyFromCache(assessmentIdentifier, false);

        assertEquals(MapUtils.EMPTY_MAP, result);
    }

    @Test
    void testReadUserSubmittedAssessmentRecords() {
        String email = "test@example.com";
        String assessmentId = "assessment123";
        String contextId = "context456";

        String expectedKeyspace = Constants.SUNBIRD_KEY_SPACE_NAME;
        String expectedTable = "user_assessment_data";

        List<Map<String, Object>> cassandraResult = List.of(
                Map.of("foo", "bar"),
                Map.of("baz", "qux")
        );

        when(serverProperties.getPublicUserAssessmentData()).thenReturn(expectedTable);
        when(cassandraOperation.getRecordsByPropertiesWithoutFiltering(
                eq(expectedKeyspace), eq(expectedTable), anyMap(), isNull()
        )).thenReturn(cassandraResult);

        List<Map<String, Object>> result = service.readUserSubmittedAssessmentRecords(email, assessmentId, contextId);

        assertEquals(cassandraResult, result);

        // verify the map passed
        ArgumentCaptor<Map<String, Object>> mapCaptor = ArgumentCaptor.forClass(Map.class);
        verify(cassandraOperation).getRecordsByPropertiesWithoutFiltering(
                eq(expectedKeyspace), eq(expectedTable), mapCaptor.capture(), isNull()
        );

        Map<String, Object> capturedMap = mapCaptor.getValue();
        assertEquals(email, capturedMap.get(Constants.USER_ID));
        assertEquals(assessmentId, capturedMap.get(Constants.ASSESSMENT_ID_KEY));
        assertEquals(contextId, capturedMap.get(Constants.CONTEXT_ID));
    }

    @Test
    void testReadQListfromCache_CacheEnabled_Miss() throws IOException {
        when(serverProperties.qListFromCacheEnabled()).thenReturn(true);

        when(redisCacheMgr.getCache(anyString())).thenReturn(null);

        // mock readQuestionDetails returning question details
        List<Map<String, Object>> questionDetails = List.of(
                Map.of(Constants.RESULT, Map.of(Constants.QUESTIONS,
                        List.of(Map.of(Constants.IDENTIFIER, "q1"), Map.of(Constants.IDENTIFIER, "q2"))))
        );
        AssessmentUtilServiceV2Impl spyUtil = spy(service);

        when(spyUtil.readQuestionDetails(anyList())).thenReturn(questionDetails);

        when(serverProperties.getRedisQuestionsReadTimeOut()).thenReturn(10);

        Map<String, Object> result = service.readQListfromCache(
                List.of("q1", "q2"), "assessment1", false);

        assertNotNull(result);

    }

    @Test
    void testReadQListfromCache_CacheDisabled() throws IOException {
        when(serverProperties.qListFromCacheEnabled()).thenReturn(false);

        Map<String, Object> result = service.readQListfromCache(
                List.of("q1", "q2"), "assessment1", false);

        assertNotNull(result);

    }

    @Test
    void test_filterQuestionMapDetailV2_withEditorState_PracticeQuestionSet() {
        // Given
        Map<String, Object> questionMapResponse = new HashMap<>();
        questionMapResponse.put("param1", "value1");
        questionMapResponse.put("editorState", Map.of("someKey", "someValue"));
        questionMapResponse.put("choices", Map.of("options", List.of(
                Map.of("option", "A"), Map.of("option", "B")
        )));
        questionMapResponse.put("rhsChoices", List.of("rhs1", "rhs2"));
        questionMapResponse.put("primaryCategory", "MTF_QUESTION");

        List<String> questionParams = Arrays.asList("param1", "primaryCategory");
        when(serverProperties.getAssessmentQuestionParams()).thenReturn(questionParams);

        // When
        Map<String, Object> result = service.filterQuestionMapDetailV2(questionMapResponse, "PRACTICE_QUESTION_SET");

        // Then
        assertNotNull(result);
        assertTrue(result.containsKey("param1"));
        assertTrue(result.containsKey("choices"));
    }

    @Test
    void test_filterQuestionMapDetailV2_withoutEditorState_notPracticeQuestionSet() {
        // Given
        Map<String, Object> questionMapResponse = new HashMap<>();
        questionMapResponse.put("param1", "value1");
        questionMapResponse.put("choices", Map.of("options", List.of(
                Map.of("option", "A"), Map.of("option", "B")
        )));
        questionMapResponse.put("rhsChoices", List.of("rhs1", "rhs2"));
        questionMapResponse.put("primaryCategory", "MTF_QUESTION");

        List<String> questionParams = Arrays.asList("param1", "primaryCategory");
        when(serverProperties.getAssessmentQuestionParams()).thenReturn(questionParams);

        // When
        Map<String, Object> result = service.filterQuestionMapDetailV2(questionMapResponse, "SOME_OTHER_CATEGORY");

        // Then
        assertNotNull(result);
        assertTrue(result.containsKey("param1"));
        assertFalse(result.containsKey("editorState"));
    }

    @Test
    void test_shuffleOptions() {
        // Given
        List<Map<String, Object>> options = Arrays.asList(
                Map.of("option", "A"),
                Map.of("option", "B"),
                Map.of("option", "C")
        );

        // When
        List<Map<String, Object>> shuffled = AssessmentUtilServiceV2Impl.shuffleOptions(options);

        // Then
        assertNotNull(shuffled);
        assertEquals(3, shuffled.size());
        assertTrue(shuffled.containsAll(options)); // same elements, possibly shuffled
    }
}