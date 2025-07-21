package com.assessment.model;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TemplateTest {

    @Test
    void testConstructorAndGetters() {
        Map<String, Object> params = new HashMap<>();
        params.put("key", "value");

        Template template = new Template("data", "id", params);

        assertEquals("data", template.getData());
        assertEquals("id", template.getId());
        assertEquals(params, template.getParams());
    }

    @Test
    void testSetters() {
        Template template = new Template(null, null, null);

        template.setData("newData");
        template.setId("newId");

        Map<String, Object> newParams = new HashMap<>();
        newParams.put("paramKey", "paramValue");
        template.setParams(newParams);

        assertEquals("newData", template.getData());
        assertEquals("newId", template.getId());
        assertEquals(newParams, template.getParams());
    }

    @Test
    void testToString() {
        Template template = new Template("data", "id", null);

        String toString = template.toString();

        assertTrue(toString.contains("data='data'"));
        assertTrue(toString.contains("id='id'"));
        assertTrue(toString.contains("params=null"));
    }
}
