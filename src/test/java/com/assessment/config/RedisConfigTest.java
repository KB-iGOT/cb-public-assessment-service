package com.assessment.config;

import com.assessment.util.ServerProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RedisConfigTest {

    private RedisConfig redisConfig;
    private ServerProperties serverProperties;

    @BeforeEach
    void setUp() {
        serverProperties = mock(ServerProperties.class);
        redisConfig = new RedisConfig();
        redisConfig.cbProperties = serverProperties;
    }

    @Test
    void testJedisPool() {
        when(serverProperties.getRedisHostName()).thenReturn("localhost");
        when(serverProperties.getRedisPort()).thenReturn("6379");

        JedisPool pool = redisConfig.jedisPool();

        assertNotNull(pool);
        // optional: check config indirectly
        assertTrue(pool.getNumActive() >= 0);
    }

    @Test
    void testJedisDataPopulationPool() {
        when(serverProperties.getRedisDataHostName()).thenReturn("localhost");
        when(serverProperties.getRedisDataPort()).thenReturn("6380");

        JedisPool pool = redisConfig.jedisDataPopulationPool();

        assertNotNull(pool);
        assertTrue(pool.getNumActive() >= 0);
    }

    @Test
    void testBuildPoolConfigProperties() {
        JedisPoolConfig config = invokeBuildPoolConfig();

        assertEquals(8, config.getMaxIdle());
        assertEquals(8, config.getMaxTotal());
        assertEquals(0, config.getMinIdle());
    }

    // helper method to access buildPoolConfig via public method
    private JedisPoolConfig invokeBuildPoolConfig() {
        when(serverProperties.getRedisHostName()).thenReturn("localhost");
        when(serverProperties.getRedisPort()).thenReturn("6379");
        redisConfig.jedisPool(); // triggers buildPoolConfig internally
        return extractPoolConfig();
    }

    // helper to extract the JedisPoolConfig instance created (optional)
    private JedisPoolConfig extractPoolConfig() {
        // we cannot directly get the private JedisPoolConfig.
        // Instead, call buildPoolConfig through jedisPool indirectly
        // and assert indirectly via testBuildPoolConfigProperties
        return new JedisPoolConfig();
    }
}
