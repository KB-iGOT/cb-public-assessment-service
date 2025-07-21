package com.assessment.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.assessment.cassandra.utils.CassandraOperation;
import com.assessment.datasecurity.DecryptionService;
import com.assessment.datasecurity.EncryptionService;
import com.assessment.kafka.Producer;
import com.assessment.kafka.service.KafkaCertificateProducerService;
import com.assessment.repo.AssessmentRepository;
import com.assessment.service.AssessmentUtilServiceV2;
import com.assessment.util.Constants;
import com.assessment.util.ServerProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import java.lang.reflect.Method;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class AssessmentServiceV4ImplMethodTest {

    private AssessmentServiceV4Impl service;

    @Mock
    AssessmentUtilServiceV2 assessmentUtilService;
    @Mock
    ServerProperties serverProperties;
    @Mock
    AssessmentRepository assessmentRepository;
    @Mock
    EncryptionService encryptionService;
    @Mock
    ObjectMapper mapper;
    @Mock
    CassandraOperation cassandraOperation;
    @Mock
    ResourceLoader resourceLoader;
    @Mock
    DecryptionService decryptionService;
    @Mock
    KafkaCertificateProducerService kafkaCertificateProducerService;
    @Mock
    Producer producer;

    @BeforeEach
    void setUp() {
        service = new AssessmentServiceV4Impl();

        // inject a mocked logger because the method catches exceptions & logs them
        Logger mockLogger = mock(Logger.class);
        var loggerField = getField("logger");
        loggerField.setAccessible(true);
        try {
            loggerField.set(service, mockLogger);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testCalculateAssessmentFinalResults_success() throws Exception {
        Map<String, Object> input = new HashMap<>();
        input.put("result", 85.0);
        input.put("total", 10);
        input.put("blank", 1);
        input.put("correct", 8);
        input.put("passPercentage", 50);
        input.put("incorrect", 1);

        Method method = getPrivateMethod();

        @SuppressWarnings("unchecked")
        Map<String, Object> output = (Map<String, Object>) method.invoke(service, input);

        assertNotNull(output);
        assertEquals(85.0, output.get("overallResult"));
        assertEquals(10, output.get("total"));
        assertEquals(1, output.get("blank"));
        assertEquals(8, output.get("correct"));
        assertEquals(50, output.get("passPercentage"));
        assertEquals(1, output.get("incorrect"));
        assertEquals(true, output.get("pass")); // 85 >= 50
    }

    @Test
    void testCalculateAssessmentFinalResults_exception() throws Exception {
        Map<String, Object> input = new HashMap<>() {
            @Override
            public Object get(Object key) {
                throw new RuntimeException("forced exception");
            }
        };

        Method method = getPrivateMethod();

        @SuppressWarnings("unchecked")
        Map<String, Object> output = (Map<String, Object>) method.invoke(service, input);

        assertNotNull(output);
        assertTrue(output.isEmpty() || output.containsKey("children")); // method still returns res
    }

    @Test
    void testWriteDataToDatabaseAndTriggerKafkaEvent_exception() throws Exception {
        Map<String, Object> submitRequest = new HashMap<>();
        Map<String, Object> questionSet = new HashMap<>();
        Map<String, Object> result = new HashMap<>();

        submitRequest.put(Constants.IDENTIFIER, "assessment1");
        submitRequest.put(Constants.COURSE_ID, "course1");
        questionSet.put(Constants.START_TIME, System.currentTimeMillis());
        result.put(Constants.PASS, true);

        Method method = AssessmentServiceV4Impl.class.getDeclaredMethod(
                "writeDataToDatabaseAndTriggerKafkaEvent",
                Map.class, String.class, Map.class, Map.class, String.class, String.class);
        method.setAccessible(true);

        Object response = method.invoke(service, submitRequest, "user@example.com", questionSet, result, "primaryCategory", "contextId");
        assertNull(response);
    }

    @Test
    void testCalculateSectionFinalResults_success() throws Exception {
        List<Map<String, Object>> sectionLevelResults = new ArrayList<>();

        sectionLevelResults.add(new HashMap<>() {{
            put(Constants.RESULT, 80.0);
            put(Constants.TOTAL, 10);
            put(Constants.BLANK, 1);
            put(Constants.CORRECT, 7);
            put(Constants.INCORRECT, 2);
            put(Constants.PASS_PERCENTAGE, 50);
        }});

        sectionLevelResults.add(new HashMap<>() {{
            put(Constants.RESULT, 90.0);
            put(Constants.TOTAL, 20);
            put(Constants.BLANK, 0);
            put(Constants.CORRECT, 18);
            put(Constants.INCORRECT, 2);
            put(Constants.PASS_PERCENTAGE, 50);
        }});

        Method method = AssessmentServiceV4Impl.class.getDeclaredMethod(
                "calculateSectionFinalResults",
                List.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        Map<String, Object> result =
                (Map<String, Object>) method.invoke(service, sectionLevelResults);

        assertNotNull(result);
        assertTrue(result.containsKey(Constants.OVERALL_RESULT));
        assertEquals(2, ((List<?>) result.get(Constants.CHILDREN)).size());
        assertEquals(30, result.get(Constants.TOTAL));
        assertEquals(1, result.get(Constants.BLANK));
        assertEquals(25, result.get(Constants.CORRECT));
        assertEquals(4, result.get(Constants.INCORRECT));
        assertEquals(true, result.get(Constants.PASS));

        Double overallResult = (Double) result.get(Constants.OVERALL_RESULT);
        assertEquals(85.0, overallResult);  // (80+90)/2
    }

    @Test
    void testCalculateSectionFinalResults_withException() throws Exception {
        List<Map<String, Object>> badData = new ArrayList<>();

        // put invalid data to trigger exception
        badData.add(new HashMap<>() {{
            put(Constants.RESULT, "invalid");  // should be Double
            put(Constants.TOTAL, 10);
            put(Constants.BLANK, 1);
            put(Constants.CORRECT, 7);
            put(Constants.INCORRECT, 2);
            put(Constants.PASS_PERCENTAGE, 50);
        }});

        Method method = AssessmentServiceV4Impl.class.getDeclaredMethod(
                "calculateSectionFinalResults",
                List.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        Map<String, Object> result =
                (Map<String, Object>) method.invoke(service, badData);

        assertNotNull(result);
        // should still return a map, possibly empty or partially filled
    }

    private Method getPrivateMethod() throws NoSuchMethodException {
        Method method = AssessmentServiceV4Impl.class.getDeclaredMethod(
                "calculateAssessmentFinalResults", Map.class);
        method.setAccessible(true);
        return method;
    }

    private java.lang.reflect.Field getField(String fieldName) {
        try {
            var field = AssessmentServiceV4Impl.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
