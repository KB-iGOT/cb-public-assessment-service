package com.assessment.repo;

import com.assessment.cassandra.utils.CassandraOperation;
import com.assessment.model.SBApiResponse;
import com.assessment.util.Constants;
import com.assessment.util.ServerProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import java.sql.Timestamp;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AssessmentRepositoryImplTest {

    @InjectMocks
    private AssessmentRepositoryImpl repository;

    @Mock
    private CassandraOperation cassandraOperation;

    @Mock
    private ServerProperties serverProperties;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(serverProperties.getPublicUserAssessmentData()).thenReturn("assessment_table");
    }

    @Test
    void testAddUserAssesmentDataToDB_success() {
        String email = "user@example.com";
        String assessmentId = "assess123";
        Timestamp start = new Timestamp(System.currentTimeMillis());
        Timestamp end = new Timestamp(System.currentTimeMillis());
        Map<String, Object> questionSet = Map.of("q1", "a1");
        String status = "COMPLETED";
        String name = "Test User";
        String contextId = "context1";

        SBApiResponse response = new SBApiResponse();
        response.put(Constants.RESPONSE, Constants.SUCCESS);

        when(cassandraOperation.insertRecord(
                eq(Constants.KEYSPACE_SUNBIRD), eq("assessment_table"), anyMap()))
                .thenReturn(response);

        boolean result = repository.addUserAssesmentDataToDB(
                email, assessmentId, start, end, questionSet, status, name, contextId);

        assertTrue(result);
        verify(cassandraOperation).insertRecord(eq(Constants.KEYSPACE_SUNBIRD), eq("assessment_table"), anyMap());
    }

    @Test
    void testFetchUserAssessmentDataFromDB() {
        String email = "user@example.com";
        String assessmentId = "assess123";
        List<Map<String, Object>> expectedList = List.of(Map.of("key", "val"));

        when(cassandraOperation.getRecordsByProperties(
                eq(Constants.KEYSPACE_SUNBIRD), eq("assessment_table"), anyMap(), isNull()))
                .thenReturn(expectedList);

        List<Map<String, Object>> result = repository.fetchUserAssessmentDataFromDB(email, assessmentId);

        assertEquals(expectedList, result);
        verify(cassandraOperation).getRecordsByProperties(eq(Constants.KEYSPACE_SUNBIRD), eq("assessment_table"), anyMap(), isNull());
    }

    @Test
    void testUpdateUserAssesmentDataToDB_withAllFields() {
        String email = "user@example.com";
        String assessmentId = "assess123";
        Map<String, Object> submitRequest = Map.of("q", "r");
        Map<String, Object> submitResponse = Map.of(Constants.PASS, true);
        String status = "SUBMITTED";
        Date startTime = new Date();
        Map<String, Object> saveSubmitRequest = Map.of("save", "point");
        String contextId = "context1";

        repository.updateUserAssesmentDataToDB(email, assessmentId,
                submitRequest, submitResponse, status, startTime, saveSubmitRequest, contextId);

        verify(cassandraOperation).updateRecord(eq(Constants.KEYSPACE_SUNBIRD),
                eq("assessment_table"), anyMap(), anyMap());
    }

    @Test
    void testUpdateUserAssesmentDataToDB_withEmptyMaps() {
        String email = "user@example.com";
        String assessmentId = "assess123";
        Map<String, Object> submitRequest = Collections.emptyMap();
        Map<String, Object> submitResponse = Collections.emptyMap();
        String status = "";
        Date startTime = new Date();
        Map<String, Object> saveSubmitRequest = Collections.emptyMap();
        String contextId = "context1";

        repository.updateUserAssesmentDataToDB(email, assessmentId,
                submitRequest, submitResponse, status, startTime, saveSubmitRequest, contextId);

        verify(cassandraOperation).updateRecord(eq(Constants.KEYSPACE_SUNBIRD),
                eq("assessment_table"), anyMap(), anyMap());
    }
}
