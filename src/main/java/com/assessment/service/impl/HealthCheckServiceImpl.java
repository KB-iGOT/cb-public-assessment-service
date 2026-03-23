package com.assessment.service.impl;

import com.assessment.model.SBApiResponse;
import com.assessment.service.HealthCheckService;
import com.assessment.util.Constants;
import com.assessment.util.ProjectUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.cassandra.core.CassandraAdminTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class HealthCheckServiceImpl implements HealthCheckService {

    private static final Logger logger = LoggerFactory.getLogger(HealthCheckServiceImpl.class);

    private final JedisPool jedisPool;
    private final CassandraAdminTemplate cassandraTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;

    // Constructor Injection
    public HealthCheckServiceImpl(JedisPool jedisPool,
                                  CassandraAdminTemplate cassandraTemplate,
                                  KafkaTemplate<String, String> kafkaTemplate) {
        this.jedisPool = jedisPool;
        this.cassandraTemplate = cassandraTemplate;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public SBApiResponse checkHealth() {

        SBApiResponse response = new SBApiResponse(Constants.HEALTH_CHECK_API);
        List<Map<String, Object>> checks = new ArrayList<>();

        Map<String, Object> redis = checkRedis();
        Map<String, Object> cassandra = checkCassandra();
        Map<String, Object> kafka = checkKafka();

        boolean redisHealthy = Constants.UP.equals(redis.get(Constants.STATUS));
        boolean cassandraHealthy = Constants.UP.equals(cassandra.get(Constants.STATUS));
        boolean kafkaHealthy = Constants.UP.equals(kafka.get(Constants.STATUS));

        checks.add(ProjectUtil.createDefaultMapResponse(
                Constants.CASSANDRA_NAME,
                cassandraHealthy,
                (Exception) cassandra.get(Constants.EXCEPTION)
        ));

        checks.add(ProjectUtil.createDefaultMapResponse(
                Constants.REDIS_NAME,
                redisHealthy,
                (Exception) redis.get(Constants.EXCEPTION)
        ));

        checks.add(ProjectUtil.createDefaultMapResponse(
                Constants.KAFKA_NAME,
                kafkaHealthy,
                (Exception) kafka.get(Constants.EXCEPTION)
        ));

        response.getParams().setStatus(Constants.SUCCESS);
        response.getParams().setErr(null);
        response.getParams().setErrmsg(null);
        response.setResponseCode(HttpStatus.OK);

        Map<String, Object> responseObj = new HashMap<>();
        responseObj.put(Constants.CHECKS, checks);
        responseObj.put(Constants.HEALTHY, true);
        responseObj.put(Constants.NAME, Constants.HEALTH_CHECK_NAME);
        response.put(Constants.RESPONSE, responseObj);

        return response;
    }

    private Map<String, Object> checkRedis() {
        Map<String, Object> result = new HashMap<>();
        try (Jedis jedis = jedisPool.getResource()) {
            String pong = jedis.ping();
            result.put(Constants.STATUS, "PONG".equals(pong) ? Constants.UP : Constants.DOWN);
            result.put(Constants.EXCEPTION, null);
        } catch (Exception e) {
            logger.error("Redis health failed: {}", e.getMessage(), e);
            result.put(Constants.STATUS, Constants.DOWN);
            result.put(Constants.EXCEPTION, e);
        }
        return result;
    }

    private Map<String, Object> checkCassandra() {
        Map<String, Object> result = new HashMap<>();
        try {
            cassandraTemplate.getCqlOperations().execute("SELECT release_version FROM system.local");
            result.put(Constants.STATUS, Constants.UP);
            result.put(Constants.EXCEPTION, null);
        } catch (Exception e) {
            logger.error("Cassandra health failed: {}", e.getMessage(), e);
            result.put(Constants.STATUS, Constants.DOWN);
            result.put(Constants.EXCEPTION, e);
        }
        return result;
    }

    private Map<String, Object> checkKafka() {
        Map<String, Object> result = new HashMap<>();
        try {
            kafkaTemplate.execute(producer ->
                    producer.partitionsFor("health-topic"));
            result.put(Constants.STATUS, Constants.UP);
            result.put(Constants.EXCEPTION, null);
        } catch (Exception e) {
            logger.error("Kafka health failed: {}", e.getMessage(), e);
            result.put(Constants.STATUS, Constants.DOWN);
            result.put(Constants.EXCEPTION, e);
        }
        return result;
    }
}