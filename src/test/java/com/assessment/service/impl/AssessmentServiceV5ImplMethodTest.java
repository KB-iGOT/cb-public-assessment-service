package com.assessment.service.impl;

import com.assessment.cassandra.utils.CassandraOperation;
import com.assessment.datasecurity.DecryptionService;
import com.assessment.kafka.Producer;
import com.assessment.kafka.service.KafkaCertificateProducerService;
import com.assessment.repo.AssessmentRepository;
import com.assessment.util.ServerProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AssessmentServiceV5ImplMethodTest {

    @InjectMocks
    private AssessmentServiceV5Impl service;

    @Mock
    private ServerProperties serverProperties;

    @Mock
    private CassandraOperation cassandraOperation;

    @Mock
    private ObjectMapper mapper;

    @Mock
    private ResourceLoader resourceLoader;

    @Mock
    private DecryptionService decryptionService;

    @Mock
    private KafkaCertificateProducerService kafkaCertificateProducerService;

    @Mock
    private Producer producer;

    @Mock
    private AssessmentRepository assessmentRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testResendMessageToKafkaForCertificate() throws Exception {
        String recipientName = "John Doe";
        String email = "test@email.com";
        String contextId = "context123";
        String assessmentId = "assessment456";

        // Mock Cassandra response
        Map<String, Object> contentHierarchy = new HashMap<>();
        contentHierarchy.put("hierarchy", "{\"source\":\"Test Source\",\"name\":\"Test Course\",\"posterImage\":\"poster.jpg\"}");

        when(cassandraOperation.getRecordsByProperties(any(), any(), anyMap(), isNull()))
                .thenReturn(Collections.singletonList(contentHierarchy));

        // Mock mapper.readValue for hierarchy
        Map<String, Object> hierarchyMap = new HashMap<>();
        hierarchyMap.put("source", "Test Source");
        hierarchyMap.put("name", "Test Course");
        hierarchyMap.put("posterImage", "poster.jpg");

        when(mapper.readValue(anyString(), eq(HashMap.class)))
                .thenReturn((HashMap) hierarchyMap);

        // Mock resource and its InputStream
        Resource resourceMock = mock(Resource.class);
        when(resourceLoader.getResource("classpath:certificate-kafka-json.json")).thenReturn(resourceMock);

        InputStream jsonInputStream = new ByteArrayInputStream("{\"dummy\":\"json\"}".getBytes());
        when(resourceMock.getInputStream()).thenReturn(jsonInputStream);

        // Mock mapper.readTree
        JsonNode jsonNode = mock(JsonNode.class);
        when(mapper.readTree(any(InputStream.class))).thenReturn(jsonNode);

        // Mock decryption
        when(decryptionService.decryptData(email)).thenReturn("decrypted@email.com");

        // Use reflection to invoke private method
        Method method = AssessmentServiceV5Impl.class.getDeclaredMethod(
                "resendMessageToKafkaForCertificate",
                String.class, String.class, String.class, String.class);
        method.setAccessible(true);

        method.invoke(service, recipientName, email, contextId, assessmentId);

        // Verify interactions
        verify(cassandraOperation).getRecordsByProperties(any(), any(), anyMap(), isNull());
        verify(mapper).readValue(anyString(), eq(HashMap.class));
        verify(resourceLoader).getResource("classpath:certificate-kafka-json.json");
        verify(resourceMock).getInputStream();
        verify(mapper).readTree(any(InputStream.class));
        verify(decryptionService).decryptData(email);
        verify(kafkaCertificateProducerService).replacePlaceholders(eq(jsonNode), anyMap());
    }

    @Test
    void testResendMessageToKafkaForCertificate_exceptionFlow() throws Exception {
        String recipientName = "John Doe";
        String email = "test@email.com";
        String contextId = "context123";
        String assessmentId = "assessment456";

        // Mock Cassandra to throw an exception
        when(cassandraOperation.getRecordsByProperties(any(), any(), anyMap(), isNull()))
                .thenThrow(new RuntimeException("Simulated exception"));

        // Use reflection to invoke private method
        Method method = AssessmentServiceV5Impl.class.getDeclaredMethod(
                "resendMessageToKafkaForCertificate",
                String.class, String.class, String.class, String.class);
        method.setAccessible(true);

        method.invoke(service, recipientName, email, contextId, assessmentId);

        // Verify Cassandra was called before exception
        verify(cassandraOperation).getRecordsByProperties(any(), any(), anyMap(), isNull());

        // You can also verify that none of the later calls happen
        verifyNoInteractions(resourceLoader, producer, kafkaCertificateProducerService);
    }

    @Test
    void testValidateUserAssessmentData_emptyList() throws Exception {
        Method method = AssessmentServiceV5Impl.class.getDeclaredMethod(
                "validateUserAssementData", List.class);
        method.setAccessible(true);

        String result = (String) method.invoke(service, Collections.emptyList());

        assertEquals("User assessment data not found", result);
    }

    @Test
    void testValidateUserAssessmentData_notPassed() throws Exception {
        Method method = AssessmentServiceV5Impl.class.getDeclaredMethod(
                "validateUserAssementData", List.class);
        method.setAccessible(true);

        Map<String, Object> map = new HashMap<>();
        map.put("passStatus", false);
        List<Map<String, Object>> input = Collections.singletonList(map);

        String result = (String) method.invoke(service, input);

        assertEquals("", result);
    }

    @Test
    void testValidateUserAssessmentData_success() throws Exception {
        Method method = AssessmentServiceV5Impl.class.getDeclaredMethod(
                "validateUserAssementData", List.class);
        method.setAccessible(true);

        Map<String, Object> map = new HashMap<>();
        map.put("passStatus", true);
        List<Map<String, Object>> input = Collections.singletonList(map);

        String result = (String) method.invoke(service, input);

        assertEquals("", result);
    }

    @Test
    void testProcessRandomization_resultFalse_returnsOriginal() throws Exception {
        Method method = AssessmentServiceV5Impl.class.getDeclaredMethod(
                "processRandomizationForQuestions",
                Map.class, List.class);
        method.setAccessible(true);

        Map<String, Map<String, Object>> sectionMap = new HashMap<>();
        sectionMap.put("level1", Map.of("noOfQuestions", 0)); // <= NO_OF_QUESTIONS <= 0

        List<Map<String, Object>> questions = List.of(
                Map.of("questionLevel", "level1", "question", "Q1"),
                Map.of("questionLevel", "level1", "question", "Q2")
        );

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result = (List<Map<String, Object>>) method.invoke(
                service, sectionMap, questions);

        assertEquals(questions, result);
    }

    @Test
    void testProcessRandomization_resultTrue_returnsSubset() throws Exception {
        Method method = AssessmentServiceV5Impl.class.getDeclaredMethod(
                "processRandomizationForQuestions",
                Map.class, List.class);
        method.setAccessible(true);

        Map<String, Map<String, Object>> sectionMap = new HashMap<>();
        sectionMap.put("level1", Map.of("noOfQuestions", 1));
        sectionMap.put("level2", Map.of("noOfQuestions", 2));

        List<Map<String, Object>> questions = new ArrayList<>();
        questions.add(new HashMap<>(Map.of("questionLevel", "level1", "question", "Q1")));
        questions.add(new HashMap<>(Map.of("questionLevel", "level1", "question", "Q2")));
        questions.add(new HashMap<>(Map.of("questionLevel", "level2", "question", "Q3")));
        questions.add(new HashMap<>(Map.of("questionLevel", "level2", "question", "Q4")));
        questions.add(new HashMap<>(Map.of("questionLevel", "level2", "question", "Q5")));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result = (List<Map<String, Object>>) method.invoke(
                service, sectionMap, questions);

        // should not return more than the configured counts: 1 for level1, 2 for level2
        long level1Count = result.stream().filter(q -> q.get("questionLevel").equals("level1")).count();
        long level2Count = result.stream().filter(q -> q.get("questionLevel").equals("level2")).count();

        assertTrue(result.size() <= 3);
        assertTrue(level1Count <= 1);
        assertTrue(level2Count <= 2);
    }

    @Test
    void testWriteDataToDatabaseAndTriggerKafkaEvent_successAndException() throws Exception {
        service.serverProperties = mock(ServerProperties.class);

        Map<String, Object> submitRequest = new HashMap<>();
        submitRequest.put("identifier", "assessment1");

        Map<String, Object> questionSetFromAssessment = new HashMap<>();
        questionSetFromAssessment.put("startTime", System.currentTimeMillis());

        Map<String, Object> result = new HashMap<>();
        result.put("pass", true);

        List<String> notAllowed = List.of("somethingElse");
        when(service.serverProperties.getAssessmentPrimaryKeyNotAllowedCertificate()).thenReturn(notAllowed);

        when(assessmentRepository.updateUserAssesmentDataToDB(any(), any(), any(), any(), any(), any(), isNull(), any()))
                .thenReturn(true);

        Method method = AssessmentServiceV5Impl.class.getDeclaredMethod(
                "writeDataToDatabaseAndTriggerKafkaEvent",
                Map.class, String.class, Map.class, Map.class, String.class, String.class);
        method.setAccessible(true);

        // happy path
        try {
            method.invoke(service, submitRequest, "email", questionSetFromAssessment, result, "allowedCategory", "contextId");
        } catch (Exception e) {
            fail("Method threw an exception: " + e.getCause());
        }

        // exception path
        when(assessmentRepository.updateUserAssesmentDataToDB(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("db error"));

        try {
            method.invoke(service, submitRequest, "email", questionSetFromAssessment, result, "allowedCategory", "contextId");
        } catch (Exception e) {
        fail("Method threw an exception: " + e.getCause());
         }
    }

    @Test
    void testSendMessageToKafkaForCertificate_successAndException() throws Exception {

        service.serverProperties = mock(ServerProperties.class);
        service.cassandraOperation = mock(CassandraOperation.class);
        service.mapper = mock(ObjectMapper.class);
        service.decryptionService = mock(DecryptionService.class);
        service.resourceLoader = mock(ResourceLoader.class);
        service.kafkaCertificateProducerService = mock(KafkaCertificateProducerService.class);
        service.producer = mock(Producer.class);

        Method method = AssessmentServiceV5Impl.class.getDeclaredMethod(
                "sendMessageToKafkaForCertificate",
                Map.class, String.class, String.class, String.class);
        method.setAccessible(true);

        Map<String, Object> submitRequest = new HashMap<>();
        submitRequest.put("identifier", "assessment1");

        List<Map<String, Object>> userData = List.of(Map.of("name", "John"));
        when(assessmentRepository.fetchUserAssessmentDataFromDB(any(), any())).thenReturn(userData);

        List<Map<String, Object>> hierarchyData = List.of(
                Map.of("hierarchy", "{\"source\":\"provider\",\"name\":\"coursename\",\"posterImage\":\"poster.jpg\"}")
        );
        when(service.cassandraOperation.getRecordsByProperties(any(), any(), any(), isNull()))
                .thenReturn(hierarchyData);

        Map<String, Object> hierarchyMap = new HashMap<>();
        hierarchyMap.put("source", "provider");
        hierarchyMap.put("name", "coursename");
        hierarchyMap.put("posterImage", "poster.jpg");

        when(mapper.readValue(anyString(), eq(HashMap.class))).thenReturn((HashMap) hierarchyMap);

        Resource resource = mock(Resource.class);
        when(resourceLoader.getResource(anyString())).thenReturn(resource);
        InputStream inputStream = new ByteArrayInputStream("{\"key\":\"value\"}".getBytes());
        when(resource.getInputStream()).thenReturn(inputStream);

        JsonNode jsonNode = mock(JsonNode.class);
        when(mapper.readTree(any(InputStream.class))).thenReturn(jsonNode);

        when(decryptionService.decryptData(anyString())).thenReturn("decryptedEmail");

        doNothing().when(kafkaCertificateProducerService).replacePlaceholders(any(), any());
        doNothing().when(producer).push(any(), any());

        // happy path
        try{
            method.invoke(service, submitRequest, "email", "contextId", "assessmentId");
        } catch (Exception e) {
        fail("Method threw an exception: " + e.getCause());
        }


        // exception path
        when(assessmentRepository.fetchUserAssessmentDataFromDB(any(), any()))
                .thenThrow(new RuntimeException("db error"));

        try{
            method.invoke(service, submitRequest, "email", "contextId", "assessmentId");
        } catch (Exception e) {
            fail("Method threw an exception: " + e.getCause());
        }
    }
}

