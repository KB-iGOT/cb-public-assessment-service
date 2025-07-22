package com.assessment.service.impl;

import com.assessment.cassandra.utils.CassandraOperation;
import com.assessment.datasecurity.DecryptionService;
import com.assessment.kafka.Producer;
import com.assessment.kafka.service.KafkaCertificateProducerService;
import com.assessment.repo.AssessmentRepository;
import com.assessment.util.Constants;
import com.assessment.util.ServerProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ReflectionUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AssessmentServiceV4ImplPrivateTest {

    @InjectMocks
    private AssessmentServiceV4Impl service;

    @Mock
    private AssessmentRepository repo;

    @Mock
    private ServerProperties props;

    @Mock
    private CassandraOperation cassandra;

    @Mock
    private ObjectMapper mapper;

    @Mock
    private ResourceLoader resourceLoader;

    @Mock
    private DecryptionService decryptor;

    @Mock
    private KafkaCertificateProducerService kafkaService;

    @Mock
    private Producer producer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testWriteDataToDBAndKafka_withStartTime_andKafkaTriggered() throws Exception {
        Map<String, Object> submitRequest = new HashMap<>();
        submitRequest.put(Constants.IDENTIFIER, "assessId");
        submitRequest.put(Constants.COURSE_ID, "courseId");

        String email = "test@email.com";
        String primaryCategory = "category";
        String contextId = "ctx";

        Map<String, Object> questionSet = new HashMap<>();
        questionSet.put(Constants.START_TIME, System.currentTimeMillis());

        Map<String, Object> result = new HashMap<>();
        result.put(Constants.PASS, true);

        when(repo.updateUserAssesmentDataToDB(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(true);
        when(props.getAssessmentPrimaryKeyNotAllowedCertificate()).thenReturn(Collections.emptyList());

        List<Map<String, Object>> submittedAssessment = List.of(Map.of(Constants.NAME, "recipientName"));
        when(repo.fetchUserAssessmentDataFromDB(any(), any())).thenReturn(submittedAssessment);

        List<Map<String, Object>> contentHierarchy = List.of(Map.of(Constants.HIERARCHY, "{\"source\":\"provider\",\"name\":\"courseName\",\"posterImage\":\"poster\"}"));
        when(cassandra.getRecordsByProperties(any(), any(), any(), any())).thenReturn(contentHierarchy);

        Resource resource = mock(Resource.class);
        when(resourceLoader.getResource(anyString())).thenReturn(resource);

        InputStream jsonStream = new ByteArrayInputStream("{\"key\":\"value\"}".getBytes());
        when(resource.getInputStream()).thenReturn(jsonStream);

        JsonNode jsonNode = mock(JsonNode.class);
        when(mapper.readTree(any(InputStream.class))).thenReturn(jsonNode);
        when(mapper.readValue(anyString(), eq(HashMap.class))).thenReturn(new HashMap<>());

        when(decryptor.decryptData(any())).thenReturn("decryptedUser");

        Method method = ReflectionUtils.findMethod(AssessmentServiceV4Impl.class,
                "writeDataToDatabaseAndTriggerKafkaEvent",
                Map.class, String.class, Map.class, Map.class, String.class, String.class);

        method.setAccessible(true);

        method.invoke(service, submitRequest, email, questionSet, result, primaryCategory, contextId);

        verify(repo).updateUserAssesmentDataToDB(any(), any(), any(), any(), any(), any(), any(), any());
        verify(producer).push(any(), eq(jsonNode));
    }

    @Test
    void testWriteDataToDBAndKafka_withoutStartTime() throws Exception {
        Map<String, Object> submitRequest = Map.of(Constants.IDENTIFIER, "assessId", Constants.COURSE_ID, "courseId");
        String email = "test@email.com";
        String primaryCategory = "category";
        String contextId = "ctx";

        Map<String, Object> questionSet = new HashMap<>(); // no START_TIME
        Map<String, Object> result = new HashMap<>();

        Method method = ReflectionUtils.findMethod(AssessmentServiceV4Impl.class,
                "writeDataToDatabaseAndTriggerKafkaEvent",
                Map.class, String.class, Map.class, Map.class, String.class, String.class);

        method.setAccessible(true);

        method.invoke(service, submitRequest, email, questionSet, result, primaryCategory, contextId);

        verifyNoInteractions(repo, producer, kafkaService);
    }

    @Test
    void testWriteDataToDBAndKafka_ExceptionHandling() throws Exception {
        Map<String, Object> submitRequest = new HashMap<>();
        submitRequest.put(Constants.IDENTIFIER, "assessId");
        submitRequest.put(Constants.COURSE_ID, "courseId");

        String email = "test@email.com";
        String primaryCategory = "category";
        String contextId = "ctx";

        Map<String, Object> questionSet = new HashMap<>();
        questionSet.put(Constants.START_TIME, System.currentTimeMillis());

        Map<String, Object> result = new HashMap<>();
        result.put(Constants.PASS, true);

        when(repo.updateUserAssesmentDataToDB(any(), any(), any(), any(), any(), any(), any(), any())).thenThrow(new RuntimeException("DB error"));

        Method method = ReflectionUtils.findMethod(AssessmentServiceV4Impl.class,
                "writeDataToDatabaseAndTriggerKafkaEvent",
                Map.class, String.class, Map.class, Map.class, String.class, String.class);

        method.setAccessible(true);

        method.invoke(service, submitRequest, email, questionSet, result, primaryCategory, contextId);

        verify(repo).updateUserAssesmentDataToDB(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void testValidateIfQuestionIdsAreSame_questionSetBlank() throws Exception {
        Map<String, Object> existingAssessmentData = Map.of(
                Constants.ASSESSMENT_READ_RESPONSE_KEY, "");

        Method method = getMethod();

        String result = (String) method.invoke(service,
                new HashMap<>(), new ArrayList<>(), List.of("identifier"), "user",
                existingAssessmentData);

        assertEquals(Constants.ASSESSMENT_SUBMIT_QUESTION_READ_FAILED, result);
    }

    @Test
    void testValidateIfQuestionIdsAreSame_invalidQuestionIds() throws Exception {
        String questionSetJson = "{\"children\": [{\"childNodes\": [\"q1\", \"q2\"]}]}";
        Map<String, Object> existingAssessmentData = Map.of(
                Constants.ASSESSMENT_READ_RESPONSE_KEY, questionSetJson);

        Map<String, Object> questionSetFromAssessment = new HashMap<>();
        questionSetFromAssessment.put(Constants.CHILDREN, List.of(
                Map.of(Constants.CHILD_NODES, List.of("q1", "q2"))
        ));

        when(mapper.readValue(eq(questionSetJson), ArgumentMatchers.<TypeReference<Map<String, Object>>>any()))
                .thenReturn(questionSetFromAssessment);

        List<Map<String, Object>> sectionListFromSubmitRequest = List.of(
                Map.of(Constants.CHILDREN, List.of(
                        Map.of("identifier", "q3")  // q3 is not in hierarchy
                ))
        );

        Method method = getMethod();

        String result = (String) method.invoke(service,
                new HashMap<>(), sectionListFromSubmitRequest, List.of("identifier"), "user",
                existingAssessmentData);

        assertEquals(Constants.ASSESSMENT_SUBMIT_INVALID_QUESTION, result);
    }

    @Test
    void testValidateIfQuestionIdsAreSame_allValid() throws Exception {
        String questionSetJson = "{\"children\": [{\"childNodes\": [\"q1\", \"q2\"]}]}";
        Map<String, Object> existingAssessmentData = Map.of(
                Constants.ASSESSMENT_READ_RESPONSE_KEY, questionSetJson);

        Map<String, Object> questionSetFromAssessment = new HashMap<>();
        questionSetFromAssessment.put(Constants.CHILDREN, List.of(
                Map.of(Constants.CHILD_NODES, List.of("q1", "q2"))
        ));

        when(mapper.readValue(eq(questionSetJson), ArgumentMatchers.<TypeReference<Map<String, Object>>>any()))
                .thenReturn(questionSetFromAssessment);

        List<Map<String, Object>> sectionListFromSubmitRequest = List.of(
                Map.of(Constants.CHILDREN, List.of(
                        Map.of("identifier", "q1"), Map.of("identifier", "q2")
                ))
        );

        Method method = getMethod();

        String result = (String) method.invoke(service,
                new HashMap<>(), sectionListFromSubmitRequest, List.of("identifier"), "user",
                existingAssessmentData);

        assertEquals("", result);
    }

    private Method getMethod() throws Exception {
        Method method = AssessmentServiceV4Impl.class.getDeclaredMethod(
                "validateIfQuestionIdsAreSame",
                Map.class, List.class, List.class, String.class, Map.class);
        method.setAccessible(true);
        return method;
    }

}
