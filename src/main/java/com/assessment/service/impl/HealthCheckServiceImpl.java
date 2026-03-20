package com.assessment.service.impl;

import com.assessment.model.SBApiResponse;
import com.assessment.service.HealthCheckService;
import com.assessment.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    private static final String STATUS = "status";
    private static final String UP = "UP";
    private static final String DOWN = "DOWN";

    @Autowired
    private JedisPool jedisPool;

    @Autowired
    private CassandraAdminTemplate cassandraTemplate;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Override
    public SBApiResponse checkHealth() {

        SBApiResponse response = new SBApiResponse("api.health.check");
        List<Map<String, Object>> checks = new ArrayList<>();
        boolean allHealthy = true;

        Map<String, Object> redis = checkRedis();
        Map<String, Object> cassandra = checkCassandra();
        Map<String, Object> kafka = checkKafka();

        boolean redisHealthy = UP.equals(redis.get(STATUS));
        boolean cassandraHealthy = UP.equals(cassandra.get(STATUS));
        boolean kafkaHealthy = UP.equals(kafka.get(STATUS));

        Map<String, Object> cassandraCheck = new HashMap<>();
        cassandraCheck.put(Constants.HEALTHY, cassandraHealthy);
        cassandraCheck.put(Constants.NAME, Constants.CASSANDRA_NAME);
        checks.add(cassandraCheck);

        Map<String, Object> redisCheck = new HashMap<>();
        redisCheck.put(Constants.HEALTHY, redisHealthy);
        redisCheck.put(Constants.NAME,Constants.REDIS_NAME);
        checks.add(redisCheck);

        Map<String, Object> kafkaCheck = new HashMap<>();
        kafkaCheck.put(Constants.HEALTHY, kafkaHealthy);
        kafkaCheck.put(Constants.NAME, Constants.KAFKA_NAME);
        checks.add(kafkaCheck);
        if (!redisHealthy || !cassandraHealthy || !kafkaHealthy) {
            allHealthy = false;
        }
        if (allHealthy) {
            response.getParams().setStatus(Constants.SUCCESS);
            response.getParams().setErr(null);
            response.getParams().setErrmsg(null);
        } else {
            response.getParams().setStatus(Constants.FAILED);
            response.getParams().setErr("SERVICE_UNAVAILABLE");
            response.getParams().setErrmsg("One or more dependent services are down");
        }
        response.put(Constants.CHECKS, checks);
        response.put(Constants.HEALTHY, allHealthy);
        response.setResponseCode(allHealthy ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE);
        return response;
    }

    private Map<String, Object> checkRedis() {
        Map<String, Object> result = new HashMap<>();
        try (Jedis jedis = jedisPool.getResource()) {
            String pong = jedis.ping();
            result.put(STATUS, "PONG".equals(pong) ? UP : DOWN);
        } catch (Exception e) {
            logger.error("Redis health failed", e);
            result.put(STATUS, DOWN);
        }
        return result;
    }

    private Map<String, Object> checkCassandra() {
        Map<String, Object> result = new HashMap<>();
        try {
            cassandraTemplate.getCqlOperations()
                    .execute("SELECT release_version FROM system.local");
            result.put(STATUS, UP);
        } catch (Exception e) {
            logger.error("Cassandra health failed", e);
            result.put(STATUS, DOWN);
        }
        return result;
    }

    private Map<String, Object> checkKafka() {
        Map<String, Object> result = new HashMap<>();
        try {
            kafkaTemplate.execute(producer ->
                    producer.partitionsFor("health-topic"));
            result.put(STATUS, UP);
        } catch (Exception e) {
            logger.error("Kafka health failed", e);
            result.put(STATUS, DOWN);
        }
        return result;
    }
}