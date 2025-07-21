package com.assessment.util;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PropertiesCacheTest {

    private static PropertiesCache cache;

    @BeforeAll
    static void setUp() {
        cache = PropertiesCache.getInstance();
    }

    @Test
    void testSingletonInstance() {
        PropertiesCache instance1 = PropertiesCache.getInstance();
        PropertiesCache instance2 = PropertiesCache.getInstance();
        assertSame(instance1, instance2);
    }

    @Test
    void testSaveConfigPropertyAndRead() {
        cache.saveConfigProperty("test.key", "test.value");

        String value = cache.getProperty("test.key");
        assertEquals("test.value", value);

        String value2 = cache.readProperty("test.key");
        assertEquals("test.value", value2);
    }

    @Test
    void testGetPropertyReturnsKeyIfMissing() {
        String result = cache.getProperty("non.existing.key");
        assertEquals("non.existing.key", result);
    }

    @Test
    void testReadPropertyReturnsNullIfMissing() {
        String result = cache.readProperty("non.exist.read");
        assertNull(result);
    }

    @Test
    void testReadCustomErrorWithSpaces() {
        cache.saveConfigProperty("custom_error_key", "Custom Error");
        String result = cache.readCustomError("custom error key");
        assertEquals("Custom Error", result);
    }

    @Test
    void testAttributePercentageMap() {
        cache.attributePercentageMap.put("attr1", 0.75f);
        assertEquals(0.75f, cache.attributePercentageMap.get("attr1"));
    }
}
