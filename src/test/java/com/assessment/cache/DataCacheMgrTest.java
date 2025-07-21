package com.assessment.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class DataCacheMgrTest {

    private DataCacheMgr dataCacheMgr;

    @BeforeEach
    void setUp() {
        dataCacheMgr = new DataCacheMgr();
    }

    @Test
    void testPutAndGetStringFromCache() {
        String key = "strKey";
        String value = "strValue";

        assertEquals("", dataCacheMgr.getStringFromCache(key), "Should return empty string when key is missing");

        dataCacheMgr.putStringInCache(key, value);
        assertEquals(value, dataCacheMgr.getStringFromCache(key));
    }

    @Test
    void testPutAndGetObjectFromCache() {
        String key = "objKey";
        Object value = 12345;

        assertNull(dataCacheMgr.getObjectFromCache(key), "Should return null when key is missing");

        dataCacheMgr.putObjectInCache(key, value);
        assertEquals(value, dataCacheMgr.getObjectFromCache(key));
    }

    @Test
    void testPutAndGetContentFromCache() {
        String key = "contentKey";
        Map<String, Object> content = new HashMap<>();
        content.put("field1", "value1");

        assertNull(dataCacheMgr.getContentFromCache(key), "Should return null when key is missing");

        dataCacheMgr.putContentInCache(key, content);
        assertEquals(content, dataCacheMgr.getContentFromCache(key));
        assertEquals("value1", dataCacheMgr.getContentFromCache(key).get("field1"));
    }

    @Test
    void testGetStringFromCacheWhenNotPresent() {
        assertEquals("", dataCacheMgr.getStringFromCache("nonExistingKey"));
    }

    @Test
    void testGetObjectFromCacheWhenNotPresent() {
        assertNull(dataCacheMgr.getObjectFromCache("nonExistingKey"));
    }

    @Test
    void testGetContentFromCacheWhenNotPresent() {
        assertNull(dataCacheMgr.getContentFromCache("nonExistingKey"));
    }
}
