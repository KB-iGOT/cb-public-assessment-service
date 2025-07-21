package com.assessment.cache;

import com.assessment.util.Constants;
import com.assessment.util.ServerProperties;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.slf4j.Logger;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RedisCacheMgrTest {

    @Mock JedisPool jedisPool;
    @Mock JedisPool jedisDataPopulationPool;
    @Mock
    ServerProperties cbExtServerProperties;
    @Mock Jedis jedis;

    @InjectMocks RedisCacheMgr redisCacheMgr;

    @Captor
    ArgumentCaptor<String> keyCaptor;

    @Captor
    ArgumentCaptor<String> valueCaptor;

    @Mock
    private Logger logger;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(jedisPool.getResource()).thenReturn(jedis);
    }

    @Test
    void testPostConstruct_withRedisTimeout() {
        when(cbExtServerProperties.getRedisQuestionsReadTimeOut()).thenReturn(123);
        when(cbExtServerProperties.getRedisTimeout()).thenReturn("3600");

        redisCacheMgr.postConstruct();

        assertEquals(3600, getStaticFieldValue("cache_ttl"));
    }

    @Test
    void testPostConstruct_withoutRedisTimeout() {
        when(cbExtServerProperties.getRedisQuestionsReadTimeOut()).thenReturn(123);
        when(cbExtServerProperties.getRedisTimeout()).thenReturn(null);

        redisCacheMgr.postConstruct();

        assertEquals(3600, getStaticFieldValue("cache_ttl"));
    }

    @Test
    void testPutCache_withException() {
        when(jedisPool.getResource()).thenThrow(new RuntimeException("fail"));

        assertDoesNotThrow(() ->
                redisCacheMgr.putCache("key", new DummyObject("error"), 100)
        );
    }

    @Test
    void testPutInQuestionCache_withException() {
        when(jedisPool.getResource()).thenThrow(new RuntimeException("fail"));

        assertDoesNotThrow(() ->
                redisCacheMgr.putInQuestionCache("key", new DummyObject("error"))
        );
    }

    @Test
    void testPutCache_withTTL() {
        Object obj = new DummyObject("foo");

        redisCacheMgr.putCache("key", obj, 100);

        verify(jedis).set(startsWith("CB_EXT_"), anyString());
        verify(jedis).expire(startsWith("CB_EXT_"), eq(100));
    }

    @Test
    void testPutCache_withoutTTL() {
        Object obj = new DummyObject("bar");

        redisCacheMgr.putCache("key", obj);

        verify(jedis).set(startsWith("CB_EXT_"), anyString());
        verify(jedis).expire(startsWith("CB_EXT_"), anyInt());
    }

    @Test
    void testPutInQuestionCache() {
        Object obj = new DummyObject("baz");

        redisCacheMgr.putInQuestionCache("key", obj);

        verify(jedis).set(startsWith("CB_EXT_"), anyString());
        verify(jedis).expire(startsWith("CB_EXT_"), anyInt());
    }

    @Test
    void testPutStringInCache_withTTL_success() {
        redisCacheMgr.putStringInCache("myKey", "myValue", 123);

        verify(jedis).set(keyCaptor.capture(), valueCaptor.capture());
        verify(jedis).expire(keyCaptor.capture(), eq(123));

        String fullKey = keyCaptor.getAllValues().get(0);
        String fullKeyForExpire = keyCaptor.getAllValues().get(1);
        String value = valueCaptor.getValue();

        assertTrue(fullKey.startsWith(Constants.REDIS_COMMON_KEY));
        assertEquals(fullKey, fullKeyForExpire);
        assertEquals("myValue", value);
    }

    @Test
    void testPutStringInCache_withTTL_exception() {
        when(jedisPool.getResource()).thenThrow(new RuntimeException("boom"));

        // Should not throw
        assertDoesNotThrow(() -> redisCacheMgr.putStringInCache("myKey", "myValue", 123));
    }

    @Test
    void testPutStringInCache_withoutTTL() {
        redisCacheMgr.putStringInCache("myKey", "myValue");

        verify(jedis).set(startsWith(Constants.REDIS_COMMON_KEY), eq("myValue"));
        verify(jedis).expire(startsWith(Constants.REDIS_COMMON_KEY), anyInt());
    }

    @Test
    void testDeleteKeyByName_success() {
        boolean result = redisCacheMgr.deleteKeyByName("myKey");

        verify(jedis).del(startsWith(Constants.REDIS_COMMON_KEY));
        assertTrue(result);
    }

    @Test
    void testDeleteKeyByName_exception() {
        when(jedisPool.getResource()).thenThrow(new RuntimeException("fail"));

        boolean result = redisCacheMgr.deleteKeyByName("myKey");

        assertFalse(result);
    }

    @Test
    void testDeleteAllCBExtKey_success() {
        Set<String> keys = Set.of(
                Constants.REDIS_COMMON_KEY + "k1",
                Constants.REDIS_COMMON_KEY + "k2"
        );
        when(jedis.keys(Constants.REDIS_COMMON_KEY + "*")).thenReturn(keys);

        boolean result = redisCacheMgr.deleteAllCBExtKey();

        assertTrue(result);
        verify(jedis).keys(Constants.REDIS_COMMON_KEY + "*");
        verify(jedis, times(1)).del(Constants.REDIS_COMMON_KEY + "k1");
        verify(jedis, times(1)).del(Constants.REDIS_COMMON_KEY + "k2");
    }

    @Test
    void testDeleteAllCBExtKey_exception() {
        when(jedisPool.getResource()).thenThrow(new RuntimeException("fail"));

        boolean result = redisCacheMgr.deleteAllCBExtKey();

        assertFalse(result);
    }

    @Test
    void testGetCache_success() {
        when(jedis.get(Constants.REDIS_COMMON_KEY + "myKey")).thenReturn("myValue");

        String value = redisCacheMgr.getCache("myKey");

        assertEquals("myValue", value);
        verify(jedis).get(Constants.REDIS_COMMON_KEY + "myKey");
    }

    @Test
    void testGetCache_exception() {
        when(jedisPool.getResource()).thenThrow(new RuntimeException("fail"));

        String value = redisCacheMgr.getCache("myKey");

        assertNull(value);
    }

    @Test
    void testMget_success() {
        List<String> fields = List.of("f1", "f2");
        String[] expectedKeys = {
                Constants.REDIS_COMMON_KEY + Constants.QUESTION_ID + "f1",
                Constants.REDIS_COMMON_KEY + Constants.QUESTION_ID + "f2"
        };

        List<String> expectedValues = List.of("val1", "val2");

        when(jedis.mget(expectedKeys)).thenReturn(expectedValues);

        List<String> result = redisCacheMgr.mget(fields);

        assertEquals(expectedValues, result);
        verify(jedis).mget(expectedKeys);
    }

    @Test
    void testMget_exception() {
        when(jedisPool.getResource()).thenThrow(new RuntimeException("fail"));

        List<String> result = redisCacheMgr.mget(List.of("f1", "f2"));

        assertNull(result);
    }

    @Test
    void testGetAllKeyNames_success() {
        Set<String> keys = Set.of(
                Constants.REDIS_COMMON_KEY + "k1",
                Constants.REDIS_COMMON_KEY + "k2"
        );

        when(jedis.keys(Constants.REDIS_COMMON_KEY + "*")).thenReturn(keys);

        Set<String> result = redisCacheMgr.getAllKeyNames();

        assertEquals(keys, result);
        verify(jedis).keys(Constants.REDIS_COMMON_KEY + "*");
    }

    @Test
    void testGetAllKeyNames_exception() {
        when(jedisPool.getResource()).thenThrow(new RuntimeException("fail"));

        Set<String> result = redisCacheMgr.getAllKeyNames();

        assertEquals(Collections.emptySet(), result);
    }

    @Test
    void testGetAllKeysAndValues_success() {
        when(jedisPool.getResource()).thenReturn(jedis);
        Set<String> keys = Set.of(Constants.REDIS_COMMON_KEY + "k1", Constants.REDIS_COMMON_KEY + "k2");
        when(jedis.keys(Constants.REDIS_COMMON_KEY + "*")).thenReturn(keys);
        when(jedis.get(Constants.REDIS_COMMON_KEY + "k1")).thenReturn("v1");
        when(jedis.get(Constants.REDIS_COMMON_KEY + "k2")).thenReturn("v2");

        List<Map<String, Object>> result = redisCacheMgr.getAllKeysAndValues();

        assertEquals(1, result.size());
        Map<String, Object> map = result.get(0);
        assertEquals("v1", map.get(Constants.REDIS_COMMON_KEY + "k1"));
        assertEquals("v2", map.get(Constants.REDIS_COMMON_KEY + "k2"));

        verify(jedis).keys(Constants.REDIS_COMMON_KEY + "*");
        verify(jedis).get(Constants.REDIS_COMMON_KEY + "k1");
        verify(jedis).get(Constants.REDIS_COMMON_KEY + "k2");
    }

    @Test
    void testGetAllKeysAndValues_noKeys() {
        when(jedisPool.getResource()).thenReturn(jedis);
        when(jedis.keys(Constants.REDIS_COMMON_KEY + "*")).thenReturn(Collections.emptySet());

        List<Map<String, Object>> result = redisCacheMgr.getAllKeysAndValues();

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetAllKeysAndValues_exception() {
        when(jedisPool.getResource()).thenThrow(new RuntimeException("fail"));

        List<Map<String, Object>> result = redisCacheMgr.getAllKeysAndValues();

        assertEquals(Collections.emptyList(), result);
    }

    @Test
    void testHget_success() {
        when(jedisDataPopulationPool.getResource()).thenReturn(jedis);
        String key = "someKey";
        int index = 1;
        String[] fields = {"field1", "field2"};
        List<String> expected = List.of("val1", "val2");

        when(jedis.hmget(key, fields)).thenReturn(expected);

        List<String> result = redisCacheMgr.hget(key, index, fields);

        assertEquals(expected, result);
        verify(jedis).select(index);
        verify(jedis).hmget(key, fields);
    }

    @Test
    void testHget_exception() {
        when(jedisDataPopulationPool.getResource()).thenThrow(new RuntimeException("fail"));

        List<String> result = redisCacheMgr.hget("key", 0, "f1", "f2");

        assertNull(result);
    }

    @Test
    void testGetCache_withIndex() {
        when(jedisPool.getResource()).thenReturn(jedis);
        when(jedis.get("myKey")).thenReturn("myValue");

        String result = redisCacheMgr.getCache("myKey", 1);

        assertEquals("myValue", result);
        verify(jedis).select(1);
        verify(jedis).get("myKey");
    }

    @Test
    void testGetCache_withoutIndex() {
        when(jedisPool.getResource()).thenReturn(jedis);
        when(jedis.get("myKey")).thenReturn("myValue");

        String result = redisCacheMgr.getCache("myKey", null);

        assertEquals("myValue", result);
        verify(jedis, never()).select(anyInt());
        verify(jedis).get("myKey");
    }

    @Test
    void testGetCache_exception_1() {
        when(jedisPool.getResource()).thenThrow(new RuntimeException("fail"));

        String result = redisCacheMgr.getCache("myKey", 1);

        assertNull(result);
    }

    @Test
    void testGetCacheFromDataRedish_withIndex() {
        when(jedisDataPopulationPool.getResource()).thenReturn(jedis);
        when(jedis.get("myKey")).thenReturn("myValue");

        String result = redisCacheMgr.getCacheFromDataRedish("myKey", 2);

        assertEquals("myValue", result);
        verify(jedis).select(2);
        verify(jedis).get("myKey");
    }

    @Test
    void testGetCacheFromDataRedish_withoutIndex() {
        when(jedisDataPopulationPool.getResource()).thenReturn(jedis);
        when(jedis.get("myKey")).thenReturn("myValue");

        String result = redisCacheMgr.getCacheFromDataRedish("myKey", null);

        assertEquals("myValue", result);
        verify(jedis, never()).select(anyInt());
        verify(jedis).get("myKey");
    }

    @Test
    void testGetCacheFromDataRedish_exception() {
        when(jedisDataPopulationPool.getResource()).thenThrow(new RuntimeException("fail"));

        String result = redisCacheMgr.getCacheFromDataRedish("myKey", 0);

        assertNull(result);
    }

    @Test
    void testGetContentFromCache_success() {
        when(jedisPool.getResource()).thenReturn(jedis);
        when(jedis.get("myKey")).thenReturn("myValue");

        String result = redisCacheMgr.getContentFromCache("myKey");

        assertEquals("myValue", result);
        verify(jedis).get("myKey");
    }

    @Test
    void testGetContentFromCache_exception() {
        when(jedisPool.getResource()).thenThrow(new RuntimeException("fail"));

        String result = redisCacheMgr.getContentFromCache("myKey");

        assertNull(result);
    }

    @Test
    void testKeyExists_false() {
        when(jedisPool.getResource()).thenReturn(jedis);
        when(jedis.exists("common:myKey")).thenReturn(false);

        boolean result = redisCacheMgr.keyExists("myKey");

        assertFalse(result);
    }

    @Test
    void testKeyExists_exception() {
        when(jedisPool.getResource()).thenThrow(new RuntimeException("fail"));

        boolean result = redisCacheMgr.keyExists("myKey");

        assertFalse(result);
    }

    @Test
    void testValueExists_false() {
        when(jedisPool.getResource()).thenReturn(jedis);
        when(jedis.sismember("common:myKey", "val")).thenReturn(false);

        boolean result = redisCacheMgr.valueExists("myKey", "val");

        assertFalse(result);
    }

    @Test
    void testValueExists_exception() {
        when(jedisPool.getResource()).thenThrow(new RuntimeException("fail"));

        boolean result = redisCacheMgr.valueExists("myKey", "val");

        assertFalse(result);
    }

    @Test
    void testPutCacheAsStringArrayWithNullTTL() {
        String key = "myKey";
        String[] values = {"a", "b"};

        when(jedisPool.getResource()).thenReturn(jedis);

        redisCacheMgr.putCacheAsStringArray(key, values, null);

        verify(jedis).sadd(Constants.REDIS_COMMON_KEY + key, values);
        verify(jedis).close();
    }

    @Test
    void testPutCacheAsStringArray_exception() {
        when(jedisPool.getResource()).thenThrow(new RuntimeException("fail"));

        assertDoesNotThrow(() ->
                redisCacheMgr.putCacheAsStringArray("myKey", new String[]{"z"}, 10)
        );
    }

    // helper to access static fields
    private int getStaticFieldValue(String field) {
        try {
            var f = RedisCacheMgr.class.getDeclaredField(field);
            f.setAccessible(true);
            return f.getInt(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static class DummyObject {
        public String name;

        DummyObject(String name) {
            this.name = name;
        }
    }
}
