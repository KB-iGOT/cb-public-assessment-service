package com.assessment.kafka;

import com.assessment.util.AssessmentServiceLogger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * @author mahesh.vakkund
 */
@Service
public class Producer {
    private AssessmentServiceLogger log = new AssessmentServiceLogger(getClass().getName());


    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;

    public void push(String topic, Object value) {
        ObjectMapper mapper = new ObjectMapper();
        String message = null;
        try {
            message = mapper.writeValueAsString(value);
            kafkaTemplate.send(topic, message);
        } catch (JsonProcessingException e) {
            log.error(e);
        }
    }

    public void pushWithKey(String topic, Object value, String key) {
        ObjectMapper mapper = new ObjectMapper();
        String message = null;
        try {
            message = mapper.writeValueAsString(value);
            kafkaTemplate.send(topic, key, message);
        } catch (JsonProcessingException e) {
            log.error(e);
        }
    }
}
