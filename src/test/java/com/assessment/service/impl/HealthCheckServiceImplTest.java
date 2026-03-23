package com.assessment.service.impl;

import com.assessment.model.SBApiResponse;
import com.assessment.service.HealthCheckService;
import com.assessment.util.Constants;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.cassandra.core.CassandraAdminTemplate;
import org.springframework.data.cassandra.core.cql.CqlOperations;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for HealthCheckServiceImpl
 * Tests health check functionality for Redis, Cassandra, and Kafka
 */
@RunWith(MockitoJUnitRunner.class)
public class HealthCheckServiceImplTest {

    @Mock
    private JedisPool jedisPool;

    @Mock
    private Jedis jedis;

    @Mock
    private CassandraAdminTemplate cassandraTemplate;

    @Mock
    private CqlOperations cqlOperations;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;


    private HealthCheckService healthCheckService;

    @Before
    public void setUp() {
        healthCheckService = new HealthCheckServiceImpl(jedisPool, cassandraTemplate, kafkaTemplate);
    }

    // ==================== All Services Healthy Tests ====================

    @Test
    public void testCheckHealth_AllServicesHealthy() {
        // Arrange
        when(jedisPool.getResource()).thenReturn(jedis);
        when(jedis.ping()).thenReturn("PONG");

        when(cassandraTemplate.getCqlOperations()).thenReturn(cqlOperations);
        when(cqlOperations.execute("SELECT release_version FROM system.local")).thenReturn(true);

        when(kafkaTemplate.execute(any())).thenReturn(null);

        // Act
        SBApiResponse response = healthCheckService.checkHealth();

        // Assert
        assertNotNull("Response should not be null", response);
        assertEquals("Response ID should be health check API", Constants.HEALTH_CHECK_API, response.getId());
        assertEquals("Response code should be OK", HttpStatus.OK, response.getResponseCode());
        assertEquals("Params status should be SUCCESS", Constants.SUCCESS, response.getParams().getStatus());
        assertNull("Params err should be null", response.getParams().getErr());
        assertNull("Params errmsg should be null", response.getParams().getErrmsg());

        // Verify response structure
        Map<String, Object> result = response.getResult();
        assertNotNull("Result should not be null", result);

        Object responseObj = result.get(Constants.RESPONSE);
        assertNotNull("Response object should not be null", responseObj);
        assertTrue("Response object should be a Map", responseObj instanceof Map);

        @SuppressWarnings("unchecked")
        Map<String, Object> responseMap = (Map<String, Object>) responseObj;

        assertEquals("Name should be health check name", Constants.HEALTH_CHECK_NAME, responseMap.get(Constants.NAME));
        assertEquals("Healthy should be true", true, responseMap.get(Constants.HEALTHY));

        // Verify checks list
        Object checksObj = responseMap.get(Constants.CHECKS);
        assertNotNull("Checks should not be null", checksObj);
        assertTrue("Checks should be a List", checksObj instanceof List);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> checks = (List<Map<String, Object>>) checksObj;
        assertEquals("Should have 3 checks", 3, checks.size());

        // Verify Cassandra check (first in list)
        Map<String, Object> cassandraCheck = checks.get(0);
        assertEquals("Cassandra name should match", Constants.CASSANDRA_NAME, cassandraCheck.get(Constants.NAME));
        assertEquals("Cassandra should be healthy", true, cassandraCheck.get(Constants.HEALTHY));
        assertEquals("Cassandra err should be empty", "", cassandraCheck.get(Constants.ERR));
        assertEquals("Cassandra errmsg should be empty", "", cassandraCheck.get(Constants.ERRMSG));

        // Verify Redis check (second in list)
        Map<String, Object> redisCheck = checks.get(1);
        assertEquals("Redis name should match", Constants.REDIS_NAME, redisCheck.get(Constants.NAME));
        assertEquals("Redis should be healthy", true, redisCheck.get(Constants.HEALTHY));
        assertEquals("Redis err should be empty", "", redisCheck.get(Constants.ERR));
        assertEquals("Redis errmsg should be empty", "", redisCheck.get(Constants.ERRMSG));

        // Verify Kafka check (third in list)
        Map<String, Object> kafkaCheck = checks.get(2);
        assertEquals("Kafka name should match", Constants.KAFKA_NAME, kafkaCheck.get(Constants.NAME));
        assertEquals("Kafka should be healthy", true, kafkaCheck.get(Constants.HEALTHY));
        assertEquals("Kafka err should be empty", "", kafkaCheck.get(Constants.ERR));
        assertEquals("Kafka errmsg should be empty", "", kafkaCheck.get(Constants.ERRMSG));

        // Verify mocks were called
        verify(jedisPool, times(1)).getResource();
        verify(jedis, times(1)).ping();
        verify(jedis, times(1)).close();
        verify(cassandraTemplate, times(1)).getCqlOperations();
        verify(cqlOperations, times(1)).execute("SELECT release_version FROM system.local");
        verify(kafkaTemplate, times(1)).execute(any());
    }

    // ==================== Redis Failure Tests ====================

    @Test
    public void testCheckHealth_RedisDown() {
        // Arrange
        when(jedisPool.getResource()).thenThrow(new RuntimeException("Redis connection failed"));

        when(cassandraTemplate.getCqlOperations()).thenReturn(cqlOperations);
        when(cqlOperations.execute(anyString())).thenReturn(true);

        when(kafkaTemplate.execute(any())).thenReturn(null);

        // Act
        SBApiResponse response = healthCheckService.checkHealth();

        // Assert
        assertNotNull("Response should not be null", response);
        assertEquals("Response code should be OK", HttpStatus.OK, response.getResponseCode());

        @SuppressWarnings("unchecked")
        Map<String, Object> responseMap = (Map<String, Object>) response.getResult().get(Constants.RESPONSE);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> checks = (List<Map<String, Object>>) responseMap.get(Constants.CHECKS);

        // Find Redis check
        Map<String, Object> redisCheck = checks.stream()
                .filter(check -> Constants.REDIS_NAME.equals(check.get(Constants.NAME)))
                .findFirst()
                .orElse(null);

        assertNotNull("Redis check should exist", redisCheck);
        assertEquals("Redis should be unhealthy", false, redisCheck.get(Constants.HEALTHY));
        assertEquals("Redis err should be error code", Constants.ERR_CODE_SERVER_ERROR, redisCheck.get(Constants.ERR));
        assertNotNull("Redis errmsg should not be null", redisCheck.get(Constants.ERRMSG));
        assertTrue("Redis errmsg should contain error",
                   redisCheck.get(Constants.ERRMSG).toString().contains("Redis connection failed"));
    }

    @Test
    public void testCheckHealth_RedisReturnsNonPong() {
        // Arrange
        when(jedisPool.getResource()).thenReturn(jedis);
        when(jedis.ping()).thenReturn("INVALID");

        when(cassandraTemplate.getCqlOperations()).thenReturn(cqlOperations);
        when(cqlOperations.execute(anyString())).thenReturn(true);

        when(kafkaTemplate.execute(any())).thenReturn(null);

        // Act
        SBApiResponse response = healthCheckService.checkHealth();

        // Assert
        @SuppressWarnings("unchecked")
        Map<String, Object> responseMap = (Map<String, Object>) response.getResult().get(Constants.RESPONSE);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> checks = (List<Map<String, Object>>) responseMap.get(Constants.CHECKS);

        // Find Redis check
        Map<String, Object> redisCheck = checks.stream()
                .filter(check -> Constants.REDIS_NAME.equals(check.get(Constants.NAME)))
                .findFirst()
                .orElse(null);

        assertNotNull("Redis check should exist", redisCheck);
        assertEquals("Redis should be unhealthy", false, redisCheck.get(Constants.HEALTHY));
    }

    // ==================== Cassandra Failure Tests ====================

    @Test
    public void testCheckHealth_CassandraDown() {
        // Arrange
        when(jedisPool.getResource()).thenReturn(jedis);
        when(jedis.ping()).thenReturn("PONG");

        when(cassandraTemplate.getCqlOperations()).thenReturn(cqlOperations);
        when(cqlOperations.execute(anyString())).thenThrow(new RuntimeException("Cassandra connection failed"));

        when(kafkaTemplate.execute(any())).thenReturn(null);

        // Act
        SBApiResponse response = healthCheckService.checkHealth();

        // Assert
        assertNotNull("Response should not be null", response);

        @SuppressWarnings("unchecked")
        Map<String, Object> responseMap = (Map<String, Object>) response.getResult().get(Constants.RESPONSE);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> checks = (List<Map<String, Object>>) responseMap.get(Constants.CHECKS);

        // Find Cassandra check
        Map<String, Object> cassandraCheck = checks.stream()
                .filter(check -> Constants.CASSANDRA_NAME.equals(check.get(Constants.NAME)))
                .findFirst()
                .orElse(null);

        assertNotNull("Cassandra check should exist", cassandraCheck);
        assertEquals("Cassandra should be unhealthy", false, cassandraCheck.get(Constants.HEALTHY));
        assertEquals("Cassandra err should be error code", Constants.ERR_CODE_SERVER_ERROR, cassandraCheck.get(Constants.ERR));
        assertTrue("Cassandra errmsg should contain error",
                   cassandraCheck.get(Constants.ERRMSG).toString().contains("Cassandra connection failed"));
    }

    @Test
    public void testCheckHealth_CassandraQueryExecution() {
        // Arrange
        when(jedisPool.getResource()).thenReturn(jedis);
        when(jedis.ping()).thenReturn("PONG");

        when(cassandraTemplate.getCqlOperations()).thenReturn(cqlOperations);
        when(cqlOperations.execute("SELECT release_version FROM system.local")).thenReturn(true);

        when(kafkaTemplate.execute(any())).thenReturn(null);

        // Act
        healthCheckService.checkHealth();

        // Assert - Verify the exact Cassandra query
        verify(cqlOperations, times(1)).execute("SELECT release_version FROM system.local");
    }

    // ==================== Kafka Failure Tests ====================

    @Test
    public void testCheckHealth_KafkaDown() {
        // Arrange
        when(jedisPool.getResource()).thenReturn(jedis);
        when(jedis.ping()).thenReturn("PONG");

        when(cassandraTemplate.getCqlOperations()).thenReturn(cqlOperations);
        when(cqlOperations.execute(anyString())).thenReturn(true);

        when(kafkaTemplate.execute(any())).thenThrow(new RuntimeException("Kafka connection failed"));

        // Act
        SBApiResponse response = healthCheckService.checkHealth();

        // Assert
        assertNotNull("Response should not be null", response);

        @SuppressWarnings("unchecked")
        Map<String, Object> responseMap = (Map<String, Object>) response.getResult().get(Constants.RESPONSE);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> checks = (List<Map<String, Object>>) responseMap.get(Constants.CHECKS);

        // Find Kafka check
        Map<String, Object> kafkaCheck = checks.stream()
                .filter(check -> Constants.KAFKA_NAME.equals(check.get(Constants.NAME)))
                .findFirst()
                .orElse(null);

        assertNotNull("Kafka check should exist", kafkaCheck);
        assertEquals("Kafka should be unhealthy", false, kafkaCheck.get(Constants.HEALTHY));
        assertEquals("Kafka err should be error code", Constants.ERR_CODE_SERVER_ERROR, kafkaCheck.get(Constants.ERR));
        assertTrue("Kafka errmsg should contain error",
                   kafkaCheck.get(Constants.ERRMSG).toString().contains("Kafka connection failed"));
    }

    @Test
    public void testCheckHealth_KafkaExecuteCallback() {
        // Arrange
        when(jedisPool.getResource()).thenReturn(jedis);
        when(jedis.ping()).thenReturn("PONG");

        when(cassandraTemplate.getCqlOperations()).thenReturn(cqlOperations);
        when(cqlOperations.execute(anyString())).thenReturn(true);

        when(kafkaTemplate.execute(any())).thenReturn(null);

        // Act
        healthCheckService.checkHealth();

        // Assert - Verify Kafka execute is called
        verify(kafkaTemplate, times(1)).execute(any());
    }

    // ==================== Multiple Services Down Tests ====================

    @Test
    public void testCheckHealth_AllServicesDown() {
        // Arrange
        when(jedisPool.getResource()).thenThrow(new RuntimeException("Redis connection failed"));

        when(cassandraTemplate.getCqlOperations()).thenReturn(cqlOperations);
        when(cqlOperations.execute(anyString())).thenThrow(new RuntimeException("Cassandra connection failed"));

        when(kafkaTemplate.execute(any())).thenThrow(new RuntimeException("Kafka connection failed"));

        // Act
        SBApiResponse response = healthCheckService.checkHealth();

        // Assert
        assertNotNull("Response should not be null", response);
        assertEquals("Response code should be OK", HttpStatus.OK, response.getResponseCode());

        @SuppressWarnings("unchecked")
        Map<String, Object> responseMap = (Map<String, Object>) response.getResult().get(Constants.RESPONSE);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> checks = (List<Map<String, Object>>) responseMap.get(Constants.CHECKS);

        assertEquals("Should have 3 checks", 3, checks.size());

        // Verify all checks are unhealthy
        for (Map<String, Object> check : checks) {
            assertEquals("Check should be unhealthy", false, check.get(Constants.HEALTHY));
            assertEquals("Check err should be error code", Constants.ERR_CODE_SERVER_ERROR, check.get(Constants.ERR));
            assertNotNull("Check errmsg should not be null", check.get(Constants.ERRMSG));
        }
    }

    @Test
    public void testCheckHealth_RedisAndCassandraDown_KafkaUp() {
        // Arrange
        when(jedisPool.getResource()).thenThrow(new RuntimeException("Redis connection failed"));

        when(cassandraTemplate.getCqlOperations()).thenReturn(cqlOperations);
        when(cqlOperations.execute(anyString())).thenThrow(new RuntimeException("Cassandra connection failed"));

        when(kafkaTemplate.execute(any())).thenReturn(null);

        // Act
        SBApiResponse response = healthCheckService.checkHealth();

        // Assert
        @SuppressWarnings("unchecked")
        Map<String, Object> responseMap = (Map<String, Object>) response.getResult().get(Constants.RESPONSE);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> checks = (List<Map<String, Object>>) responseMap.get(Constants.CHECKS);

        // Verify Redis is down
        Map<String, Object> redisCheck = checks.stream()
                .filter(check -> Constants.REDIS_NAME.equals(check.get(Constants.NAME)))
                .findFirst()
                .orElse(null);
        assertNotNull(redisCheck);
        assertEquals(false, redisCheck.get(Constants.HEALTHY));

        // Verify Cassandra is down
        Map<String, Object> cassandraCheck = checks.stream()
                .filter(check -> Constants.CASSANDRA_NAME.equals(check.get(Constants.NAME)))
                .findFirst()
                .orElse(null);
        assertNotNull(cassandraCheck);
        assertEquals(false, cassandraCheck.get(Constants.HEALTHY));

        // Verify Kafka is up
        Map<String, Object> kafkaCheck = checks.stream()
                .filter(check -> Constants.KAFKA_NAME.equals(check.get(Constants.NAME)))
                .findFirst()
                .orElse(null);
        assertNotNull(kafkaCheck);
        assertEquals(true, kafkaCheck.get(Constants.HEALTHY));
    }

    // ==================== Resource Management Tests ====================

    @Test
    public void testCheckHealth_JedisAutoCloseable() {
        // Arrange
        when(jedisPool.getResource()).thenReturn(jedis);
        when(jedis.ping()).thenReturn("PONG");
        doNothing().when(jedis).close();

        when(cassandraTemplate.getCqlOperations()).thenReturn(cqlOperations);
        when(cqlOperations.execute(anyString())).thenReturn(true);

        when(kafkaTemplate.execute(any())).thenReturn(null);

        // Act
        healthCheckService.checkHealth();

        // Assert - Verify Jedis is closed properly (try-with-resources)
        verify(jedis, times(1)).close();
    }

    @Test
    public void testCheckHealth_JedisCloseException_DoesNotAffectOtherChecks() {
        // Arrange
        when(jedisPool.getResource()).thenReturn(jedis);
        when(jedis.ping()).thenReturn("PONG");
        doThrow(new RuntimeException("Close failed")).when(jedis).close();

        when(cassandraTemplate.getCqlOperations()).thenReturn(cqlOperations);
        when(cqlOperations.execute(anyString())).thenReturn(true);

        when(kafkaTemplate.execute(any())).thenReturn(null);

        // Act & Assert - Should not throw exception
        try {
            healthCheckService.checkHealth();
            fail("Should have thrown exception due to Jedis close failure");
        } catch (RuntimeException e) {
            // Expected - exception from try-with-resources close
            assertTrue(e.getMessage().contains("Close failed"));
        }
    }

    // ==================== Response Structure Tests ====================

    @Test
    public void testCheckHealth_ResponseStructure() {
        // Arrange
        when(jedisPool.getResource()).thenReturn(jedis);
        when(jedis.ping()).thenReturn("PONG");

        when(cassandraTemplate.getCqlOperations()).thenReturn(cqlOperations);
        when(cqlOperations.execute(anyString())).thenReturn(true);

        when(kafkaTemplate.execute(any())).thenReturn(null);

        // Act
        SBApiResponse response = healthCheckService.checkHealth();

        // Assert - Verify complete response structure
        assertNotNull("Response should not be null", response);
        assertNotNull("Response ID should not be null", response.getId());
        assertEquals("Response ID should be health check API", Constants.HEALTH_CHECK_API, response.getId());
        assertNotNull("Response version should not be null", response.getVer());
        assertNotNull("Response timestamp should not be null", response.getTs());
        assertNotNull("Response params should not be null", response.getParams());
        assertNotNull("Response code should not be null", response.getResponseCode());
        assertNotNull("Response result should not be null", response.getResult());

        // Verify params structure
        assertNotNull("Params status should not be null", response.getParams().getStatus());
        assertEquals("Params status should be SUCCESS", Constants.SUCCESS, response.getParams().getStatus());
        assertNull("Params err should be null", response.getParams().getErr());
        assertNull("Params errmsg should be null", response.getParams().getErrmsg());

        // Verify result contains required keys
        Map<String, Object> result = response.getResult();
        assertTrue("Result should contain 'response' key", result.containsKey(Constants.RESPONSE));
    }

    @Test
    public void testCheckHealth_ChecksOrderAndCount() {
        // Arrange
        when(jedisPool.getResource()).thenReturn(jedis);
        when(jedis.ping()).thenReturn("PONG");

        when(cassandraTemplate.getCqlOperations()).thenReturn(cqlOperations);
        when(cqlOperations.execute(anyString())).thenReturn(true);

        when(kafkaTemplate.execute(any())).thenReturn(null);

        // Act
        SBApiResponse response = healthCheckService.checkHealth();

        // Assert
        @SuppressWarnings("unchecked")
        Map<String, Object> responseMap = (Map<String, Object>) response.getResult().get(Constants.RESPONSE);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> checks = (List<Map<String, Object>>) responseMap.get(Constants.CHECKS);

        assertEquals("Should have exactly 3 checks", 3, checks.size());

        // Verify order: Cassandra, Redis, Kafka
        assertEquals("First check should be Cassandra", Constants.CASSANDRA_NAME, checks.get(0).get(Constants.NAME));
        assertEquals("Second check should be Redis", Constants.REDIS_NAME, checks.get(1).get(Constants.NAME));
        assertEquals("Third check should be Kafka", Constants.KAFKA_NAME, checks.get(2).get(Constants.NAME));
    }

    // ==================== Constructor Tests ====================

    @Test
    public void testConstructor_WithValidDependencies() {
        // Act
        HealthCheckServiceImpl service = new HealthCheckServiceImpl(jedisPool, cassandraTemplate, kafkaTemplate);

        // Assert
        assertNotNull("Service should be created", service);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructor_WithNullJedisPool_ThrowsException() {
        // Act
        HealthCheckServiceImpl service = new HealthCheckServiceImpl(null, cassandraTemplate, kafkaTemplate);

        // This should throw NullPointerException when trying to use jedisPool
        service.checkHealth();
    }

    @Test(expected = NullPointerException.class)
    public void testConstructor_WithNullCassandraTemplate_ThrowsException() {
        // Arrange
        when(jedisPool.getResource()).thenReturn(jedis);
        when(jedis.ping()).thenReturn("PONG");

        // Act
        HealthCheckServiceImpl service = new HealthCheckServiceImpl(jedisPool, null, kafkaTemplate);

        // This should throw NullPointerException when trying to use cassandraTemplate
        service.checkHealth();
    }

    @Test(expected = NullPointerException.class)
    public void testConstructor_WithNullKafkaTemplate_ThrowsException() {
        // Arrange
        when(jedisPool.getResource()).thenReturn(jedis);
        when(jedis.ping()).thenReturn("PONG");

        when(cassandraTemplate.getCqlOperations()).thenReturn(cqlOperations);
        when(cqlOperations.execute(anyString())).thenReturn(true);

        // Act
        HealthCheckServiceImpl service = new HealthCheckServiceImpl(jedisPool, cassandraTemplate, null);

        // This should throw NullPointerException when trying to use kafkaTemplate
        service.checkHealth();
    }

    // ==================== Exception Message Propagation Tests ====================

    @Test
    public void testCheckHealth_ExceptionMessagePropagation() {
        // Arrange
        String errorMessage = "Connection timeout to Redis server";
        when(jedisPool.getResource()).thenThrow(new RuntimeException(errorMessage));

        when(cassandraTemplate.getCqlOperations()).thenReturn(cqlOperations);
        when(cqlOperations.execute(anyString())).thenReturn(true);

        when(kafkaTemplate.execute(any())).thenReturn(null);

        // Act
        SBApiResponse response = healthCheckService.checkHealth();

        // Assert - Verify exception message is propagated
        @SuppressWarnings("unchecked")
        Map<String, Object> responseMap = (Map<String, Object>) response.getResult().get(Constants.RESPONSE);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> checks = (List<Map<String, Object>>) responseMap.get(Constants.CHECKS);

        Map<String, Object> redisCheck = checks.stream()
                .filter(check -> Constants.REDIS_NAME.equals(check.get(Constants.NAME)))
                .findFirst()
                .orElse(null);

        assertNotNull(redisCheck);
        String errmsg = (String) redisCheck.get(Constants.ERRMSG);
        assertTrue("Error message should contain exception details", errmsg.contains(errorMessage));
    }
}