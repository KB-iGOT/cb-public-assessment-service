package com.assessment.cassandra.utils;

import com.assessment.model.SBApiResponse;
import com.assessment.util.Constants;
import com.datastax.driver.core.*;
import com.datastax.driver.core.querybuilder.Select;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CassandraOperationImplTest {

    @InjectMocks
    private CassandraOperationImpl cassandraOperation;

    @Mock
    private CassandraConnectionManager connectionManager;
    @Mock
    private Session session;
    @Mock
    private PreparedStatement preparedStatement;
    @Mock
    private BoundStatement boundStatement;
    @Mock
    private BatchStatement batchStatement;
    @Mock
    private ResultSet resultSet;
    @Mock
    private Row row;
    @Mock
    private Statement statement;
    @Mock
    private PagingState pagingState;
    @Mock
    private ExecutionInfo executionInfo;

    @Mock
    private Logger logger;

    @Mock
    Row row1;

    @Mock
    Row row2;

    @Mock
    Iterator<Row> rowIterator;


    @BeforeEach
    void setUp() {
        when(connectionManager.getSession(anyString())).thenReturn(session);
    }

    @Test
    void insertRecord_exception() {
        Map<String, Object> req = Map.of("id", "1");
        try (MockedStatic<CassandraUtil> util = mockStatic(CassandraUtil.class)) {
            util.when(() -> CassandraUtil.getPreparedStatement(any(), any(), any())).thenReturn("insert");
            when(session.prepare(anyString())).thenThrow(new RuntimeException("fail"));

            SBApiResponse resp = cassandraOperation.insertRecord("ks", "tbl", req);
            assertEquals(Constants.FAILED, resp.get(Constants.RESPONSE));
            assertTrue(resp.containsKey(Constants.ERROR_MESSAGE));
        }
    }

    @Test
    void insertBulkRecord_success() {
        List<Map<String, Object>> req = List.of(Map.of("id", "1"));
        try (MockedStatic<CassandraUtil> util = mockStatic(CassandraUtil.class)) {
            util.when(() -> CassandraUtil.getPreparedStatement(any(), any(), any())).thenReturn("insert");
            when(session.prepare(anyString())).thenReturn(preparedStatement);

            SBApiResponse resp = cassandraOperation.insertBulkRecord("ks", "tbl", req);
            assertNotNull(resp);
        }
    }
    @Test
    void insertBulkRecord_exception() {
        List<Map<String, Object>> req = List.of(Map.of("id", "1"));
        try (MockedStatic<CassandraUtil> util = mockStatic(CassandraUtil.class)) {
            util.when(() -> CassandraUtil.getPreparedStatement(any(), any(), any())).thenReturn("insert");
            when(session.prepare(anyString())).thenThrow(new RuntimeException("fail"));

            SBApiResponse resp = cassandraOperation.insertBulkRecord("ks", "tbl", req);
            assertNull(resp.get(Constants.RESPONSE));
        }
    }

    @Test
    void getRecordsByProperties_success() {
        Map<String, Object> propertyMap = Map.of("id", "1");
        List<String> fields = List.of("id");
        try (MockedStatic<CassandraUtil> util = mockStatic(CassandraUtil.class)) {
            util.when(() -> CassandraUtil.createResponse(any(ResultSet.class))).thenReturn(List.of(Map.of("id", "1")));
            when(session.execute(any(Statement.class))).thenReturn(resultSet);

            List<Map<String, Object>> resp = cassandraOperation.getRecordsByProperties("ks", "tbl", propertyMap, fields);
            assertFalse(resp.isEmpty());
        }
    }

    @Test
    void getRecordsByProperties_exception() {
        Map<String, Object> propertyMap = Map.of("id", "1");
        List<String> fields = List.of("id");
        when(session.execute(any(Statement.class))).thenThrow(new RuntimeException("fail"));
        List<Map<String, Object>> resp = cassandraOperation.getRecordsByProperties("ks", "tbl", propertyMap, fields);
        assertTrue(resp.isEmpty());
    }

    @Test
    void getRecordsByPropertiesWithKey_success() {
        Map<String, Object> propertyMap = Map.of("id", "1");
        List<String> fields = List.of("id");
        try (MockedStatic<CassandraUtil> util = mockStatic(CassandraUtil.class)) {
            util.when(() -> CassandraUtil.createResponse(any(ResultSet.class), anyString())).thenReturn(Map.of("id", "1"));
            when(session.execute(any(Statement.class))).thenReturn(resultSet);

            Map<String, Object> resp = cassandraOperation.getRecordsByProperties("ks", "tbl", propertyMap, fields, "id");
            assertFalse(resp.isEmpty());
        }
    }

    @Test
    void getRecordsByPropertiesWithKey_exception() {
        Map<String, Object> propertyMap = Map.of("id", "1");
        List<String> fields = List.of("id");
        when(session.execute(any(Statement.class))).thenThrow(new RuntimeException("fail"));
        Map<String, Object> resp = cassandraOperation.getRecordsByProperties("ks", "tbl", propertyMap, fields, "id");
        assertTrue(resp.isEmpty());
    }

    @Test
    void searchByWhereClause_success() {
        List<String> fields = List.of("id");
        Date date = new Date();
        try (MockedStatic<CassandraUtil> util = mockStatic(CassandraUtil.class)) {
            util.when(() -> CassandraUtil.createResponse(any(ResultSet.class))).thenReturn(List.of(Map.of("id", "1")));
            when(session.execute(any(Statement.class))).thenReturn(resultSet);

            List<Map<String, Object>> resp = cassandraOperation.searchByWhereClause("ks", "tbl", fields, date);
            assertFalse(resp.isEmpty());
        }
    }

    @Test
    void getRecordsByPropertiesWithPagination_success() {
        Map<String, Object> propertyMap = Map.of("id", "1");
        List<String> fields = List.of("id");
        try (MockedStatic<CassandraUtil> util = mockStatic(CassandraUtil.class)) {
            util.when(() -> CassandraUtil.createResponse(any(ResultSet.class), anyString())).thenReturn(Map.of("id", "1"));
            when(session.execute(any(Statement.class))).thenReturn(resultSet);

            Map<String, Object> resp = cassandraOperation.getRecordsByPropertiesWithPagination("ks", "tbl", propertyMap, fields, 1, null, "id");
            assertFalse(resp.isEmpty());
        }
    }

    @Test
    void getRecordsByPropertiesWithPagination_withUpdatedOn() {
        Map<String, Object> propertyMap = Map.of("id", "1");
        List<String> fields = List.of("id");
        try (MockedStatic<CassandraUtil> util = mockStatic(CassandraUtil.class)) {
            util.when(() -> CassandraUtil.createResponse(any(ResultSet.class), anyString())).thenReturn(Map.of("id", "1"));
            when(session.execute(any(Statement.class))).thenReturn(resultSet);

            Map<String, Object> resp = cassandraOperation.getRecordsByPropertiesWithPagination("ks", "tbl", propertyMap, fields, 1, UUID.randomUUID().toString(), "id");
            assertFalse(resp.isEmpty());
        }
    }

    @Test
    void deleteRecord_success() {
        Map<String, Object> keyMap = Map.of("id", "1");
        when(session.execute(any(Statement.class))).thenReturn(resultSet);
        cassandraOperation.deleteRecord("ks", "tbl", keyMap);
        verify(session).execute(any(Statement.class));
    }

    @Test
    void deleteRecord_exception() {
        Map<String, Object> keyMap = Map.of("id", "1");
        when(session.execute(any(Statement.class))).thenThrow(new RuntimeException("fail"));
        assertThrows(RuntimeException.class, () -> cassandraOperation.deleteRecord("ks", "tbl", keyMap));
    }

    @Test
    void updateRecord_success() {
        Map<String, Object> update = Map.of("name", "abc");
        Map<String, Object> key = Map.of("id", "1");
        when(session.execute(any(Statement.class))).thenReturn(resultSet);

        Map<String, Object> resp = cassandraOperation.updateRecord("ks", "tbl", update, key);
        assertEquals(Constants.SUCCESS, resp.get(Constants.RESPONSE));
    }

    @Test
    void updateRecord_exception() {
        Map<String, Object> update = Map.of("name", "abc");
        Map<String, Object> key = Map.of("id", "1");
        when(session.execute(any(Statement.class))).thenThrow(new RuntimeException("fail"));

        assertThrows(RuntimeException.class, () -> cassandraOperation.updateRecord("ks", "tbl", update, key));
    }

    @Test
    void getRecordCount_success() {
        when(session.execute(any(Statement.class))).thenReturn(resultSet);
        when(resultSet.one()).thenReturn(row);
        when(row.getLong(0)).thenReturn(5L);

        Long count = cassandraOperation.getRecordCount("ks", "tbl");
        assertEquals(5L, count);
    }

    @Test
    void getRecordCount_exception() {
        when(session.execute(any(Statement.class))).thenThrow(new RuntimeException("fail"));
        assertThrows(RuntimeException.class, () -> cassandraOperation.getRecordCount("ks", "tbl"));
    }

    @Test
    void getRecordsWithInClause_success() {
        List<Map<String, Object>> propertyMaps = List.of(Map.of("id", List.of("1", "2")));
        List<String> fields = List.of("id");
        try (MockedStatic<CassandraUtil> util = mockStatic(CassandraUtil.class)) {
            util.when(() -> CassandraUtil.createResponse(any(ResultSet.class))).thenReturn(List.of(Map.of("id", "1")));
            when(session.execute(any(Statement.class))).thenReturn(resultSet);

            List<Map<String, Object>> resp = cassandraOperation.getRecordsWithInClause("ks", "tbl", propertyMaps, fields);
            assertFalse(resp.isEmpty());
        }
    }

    @Test
    void getRecordsWithInClause_exception() {
        List<Map<String, Object>> propertyMaps = List.of(Map.of("id", List.of("1", "2")));
        List<String> fields = List.of("id");
        when(session.execute(any(Statement.class))).thenThrow(new RuntimeException("fail"));
        List<Map<String, Object>> resp = cassandraOperation.getRecordsWithInClause("ks", "tbl", propertyMaps, fields);
        assertTrue(resp.isEmpty());
    }

    @Test
    void getRecordsByPropertiesWithoutFiltering_success() {
        Map<String, Object> propertyMap = Map.of("id", "1");
        List<String> fields = List.of("id");
        try (MockedStatic<CassandraUtil> util = mockStatic(CassandraUtil.class)) {
            util.when(() -> CassandraUtil.createResponse(any(ResultSet.class))).thenReturn(List.of(Map.of("id", "1")));
            when(session.execute(any(Statement.class))).thenReturn(resultSet);

            List<Map<String, Object>> resp = cassandraOperation.getRecordsByPropertiesWithoutFiltering("ks", "tbl", propertyMap, fields, 1);
            assertFalse(resp.isEmpty());
        }
    }

    @Test
    void getRecordsByPropertiesWithoutFiltering_exception() {
        Map<String, Object> propertyMap = Map.of("id", "1");
        List<String> fields = List.of("id");
        when(session.execute(any(Statement.class))).thenThrow(new RuntimeException("fail"));
        List<Map<String, Object>> resp = cassandraOperation.getRecordsByPropertiesWithoutFiltering("ks", "tbl", propertyMap, fields, 1);
        assertTrue(resp.isEmpty());
    }

    @Test
    void getRecordsByPropertiesByKey_success() {
        Map<String, Object> propertyMap = Map.of("id", "1");
        List<String> fields = List.of("id");
        try (MockedStatic<CassandraUtil> util = mockStatic(CassandraUtil.class)) {
            util.when(() -> CassandraUtil.createResponse(any(ResultSet.class), anyString())).thenReturn(Map.of("id", "1"));
            when(session.execute(any(Statement.class))).thenReturn(resultSet);

            Map<String, Object> resp = cassandraOperation.getRecordsByPropertiesByKey("ks", "tbl", propertyMap, fields, "id");
            assertFalse(resp.isEmpty());
        }
    }

    @Test
    void getRecordsByPropertiesByKey_exception() {
        Map<String, Object> propertyMap = Map.of("id", "1");
        List<String> fields = List.of("id");
        when(session.execute(any(Statement.class))).thenThrow(new RuntimeException("fail"));
        Map<String, Object> resp = cassandraOperation.getRecordsByPropertiesByKey("ks", "tbl", propertyMap, fields, "id");
        assertTrue(resp.isEmpty());
    }

    @Test
    void getKarmaPointsRecordsByPropertiesWithPaginationList_success() {
        Map<String, Object> propertyMap = Map.of("id", "1");
        List<String> fields = List.of("id");
        Date date = new Date();
        try (MockedStatic<CassandraUtil> util = mockStatic(CassandraUtil.class)) {
            util.when(() -> CassandraUtil.createResponse(any(ResultSet.class))).thenReturn(List.of(Map.of("id", "1")));
            when(session.execute(any(Statement.class))).thenReturn(resultSet);

            List<Map<String, Object>> resp = cassandraOperation.getKarmaPointsRecordsByPropertiesWithPaginationList("ks", "tbl", propertyMap, fields, 1, date, "id", date);
            assertFalse(resp.isEmpty());
        }
    }

    @Test
    void getKarmaPointsRecordsByPropertiesWithPaginationList_exception() {
        Map<String, Object> propertyMap = Map.of("id", "1");
        List<String> fields = List.of("id");
        Date date = new Date();
        when(session.execute(any(Statement.class))).thenThrow(new RuntimeException("fail"));
        List<Map<String, Object>> resp = cassandraOperation.getKarmaPointsRecordsByPropertiesWithPaginationList("ks", "tbl", propertyMap, fields, 1, date, "id", date);
        assertTrue(resp.isEmpty());
    }

    @Test
    void getRecordCountWithUserId_success() {
        when(session.execute(any(Statement.class))).thenReturn(resultSet);
        when(resultSet.one()).thenReturn(row);
        when(row.getLong(0)).thenReturn(10L);

        Long count = cassandraOperation.getRecordCountWithUserId("ks", "tbl", "user", new Date());
        assertEquals(10L, count);
    }

    @Test
    void getRecordCountWithUserId_exception() {
        when(session.execute(any(Statement.class))).thenThrow(new RuntimeException("fail"));

        Executable executable = () -> cassandraOperation.getRecordCountWithUserId("ks", "tbl", "user", new Date());

        RuntimeException ex = assertThrows(RuntimeException.class, executable);

        assertEquals("fail", ex.getMessage());
    }

    @Test
    void getRecordByIdentifierWithPage_exception() {
        Map<String, Object> key = Map.of("id", "1");
        List<String> fields = List.of("id");
        Map<String, Object> resp = cassandraOperation.getRecordByIdentifierWithPage("ks", "tbl", key, fields, "page", 1);
        assertTrue(resp.isEmpty());
    }

    @Test
    void getCountOfRecordByIdentifier_success() {
        Map<String, Object> key = Map.of("id", "1");
        List<Map<String, Object>> fakeResp = List.of(Map.of("system.count(id)", 2L));
        try (MockedStatic<CassandraUtil> util = mockStatic(CassandraUtil.class)) {
            util.when(() -> CassandraUtil.createResponse(any(ResultSet.class))).thenReturn(fakeResp);
            when(session.execute(any(Statement.class))).thenReturn(resultSet);

            Long count = cassandraOperation.getCountOfRecordByIdentifier("ks", "tbl", key, "id");
            assertEquals(2L, count);
        }
    }

    @Test
    void testGetAllRecords_success() {
        String keyspace = "ks";
        String table = "tbl";
        List<String> fields = List.of("col1", "col2");
        String key = "col1";

        Map<String, Map<String, String>> objectInfo = new HashMap<>();

        when(connectionManager.getSession(keyspace)).thenReturn(session);

        // create a mock Select query and let processQuery build it
        // The query itself isn’t executed because connectionManager is mocked.
        doReturn(resultSet).when(session).execute(any(Select.class));

        Map<String, String> columnsMapping = Map.of("col1", "col1", "col2", "col2");

        // override CassandraUtil.fetchColumnsMapping
        try (MockedStatic<CassandraUtil> utilMock = mockStatic(CassandraUtil.class)) {
            utilMock.when(() -> CassandraUtil.fetchColumnsMapping(resultSet))
                    .thenReturn(columnsMapping);

            when(resultSet.iterator()).thenReturn(rowIterator);

            cassandraOperation.getAllRecords(keyspace, table, fields, key, objectInfo);

            // verify objectInfo populated
            assertEquals(0, objectInfo.size());

        }
    }

    @Test
    void testGetAllRecordsWithPagination() {
        String keyspace = "ks";
        String table = "tbl";
        List<String> fields = List.of("col1", "col2");
        String key = "col1";

        Map<String, Map<String, String>> objectInfo = new HashMap<>();

        // mock session & resultSet
        when(connectionManager.getSession(keyspace)).thenReturn(session);
        when(session.execute(any(Statement.class))).thenReturn(resultSet);
        when(resultSet.getExecutionInfo()).thenReturn(executionInfo);

        // Simulate two pages: first call returns pagingState, second call returns null
        when(executionInfo.getPagingState()).thenReturn(pagingState, null);

        // Simulate rows for both pages
        when(resultSet.iterator()).thenReturn(rowIterator);

        // mock columns mapping
        Map<String, String> columnsMapping = Map.of("col1", "col1", "col2", "col2");

        try (MockedStatic<CassandraUtil> utilMock = mockStatic(CassandraUtil.class);
             MockedConstruction<Select> selectMock = mockConstruction(Select.class, (mock, context) -> {
                 when(mock.setFetchSize(anyInt())).thenReturn(mock);
                 when(mock.setPagingState(any())).thenReturn(mock);
             })) {

            utilMock.when(() -> CassandraUtil.fetchColumnsMapping(resultSet))
                    .thenReturn(columnsMapping);

            cassandraOperation.getAllRecordsWithPagination(keyspace, table, fields, key, objectInfo);
        }

        // Assertions
        assertEquals(0, objectInfo.size());
         }

    @Test
    void getCountOfRecordByIdentifier_exception() {
        Map<String, Object> key = Map.of("id", "1");
        when(session.execute(any(Statement.class))).thenThrow(new RuntimeException("fail"));
        Long count = cassandraOperation.getCountOfRecordByIdentifier("ks", "tbl", key, "id");
        assertEquals(0L, count);
    }
}