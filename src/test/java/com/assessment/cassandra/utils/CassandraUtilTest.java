package com.assessment.cassandra.utils;

import com.datastax.driver.core.*;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CassandraUtilTest {

    @Test
    void testGetPreparedStatement() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", 1);
        map.put("name", "test");

        String stmt = CassandraUtil.getPreparedStatement("ks", "table", map);

        assertEquals("INSERT INTO ks.table(id,name) VALUES (?,?);", stmt);
    }

    @Test
    void testCreateResponseList() {
        ResultSet resultSet = mock(ResultSet.class);
        Row row = mock(Row.class);

        ColumnDefinitions defs = mock(ColumnDefinitions.class);
        ColumnDefinitions.Definition def = mock(ColumnDefinitions.Definition.class);

        when(resultSet.getColumnDefinitions()).thenReturn(defs);
        when(defs.asList()).thenReturn(Collections.singletonList(def));
        when(def.getName()).thenReturn("id");

        // Mock property reader to just return the same key
        CassandraPropertyReader reader = CassandraPropertyReader.getInstance();
        reader.readProperty("id"); // triggers init

        when(resultSet.iterator()).thenReturn(Collections.singletonList(row).iterator());
        when(row.getObject("id")).thenReturn(42);

        List<Map<String, Object>> response = CassandraUtil.createResponse(resultSet);
        assertEquals(1, response.size());
        assertEquals(42, response.get(0).get("id"));
    }

    @Test
    void testCreateResponseMap() {
        ResultSet resultSet = mock(ResultSet.class);
        Row row = mock(Row.class);

        ColumnDefinitions defs = mock(ColumnDefinitions.class);
        ColumnDefinitions.Definition def = mock(ColumnDefinitions.Definition.class);

        when(resultSet.getColumnDefinitions()).thenReturn(defs);
        when(defs.asList()).thenReturn(Collections.singletonList(def));
        when(def.getName()).thenReturn("id");

        when(resultSet.iterator()).thenReturn(Collections.singletonList(row).iterator());
        when(row.getObject("id")).thenReturn("key");

        Map<String, Object> response = CassandraUtil.createResponse(resultSet, "id");
        assertEquals(1, response.size());
        assertTrue(response.containsKey("key"));
    }

    @Test
    void testFetchColumnsMapping() {
        ResultSet resultSet = mock(ResultSet.class);

        ColumnDefinitions defs = mock(ColumnDefinitions.class);
        ColumnDefinitions.Definition def = mock(ColumnDefinitions.Definition.class);

        when(resultSet.getColumnDefinitions()).thenReturn(defs);
        when(defs.asList()).thenReturn(Collections.singletonList(def));

        when(def.getName()).thenReturn("id");

        Map<String, String> map = CassandraUtil.fetchColumnsMapping(resultSet);
        assertEquals(1, map.size());
        assertEquals("id", map.get("id"));
    }
}
