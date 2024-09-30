package com.assessment.consumer;

import com.assessment.service.AssessmentServiceV5;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.core.type.TypeReference;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
public class NotificationConsumer {
    @Autowired
    private AssessmentServiceV5 assessmentServiceV5;
    private ObjectMapper mapper = new ObjectMapper();
    private final Logger logger = LoggerFactory.getLogger(NotificationConsumer.class);

    @KafkaListener(groupId="notification-group-id", topics = "notification-topic")
    public void notificationConsumer(ConsumerRecord<String, String> request) {
        try{
            Map<String, Object> map = mapper.readValue(request.value(), new TypeReference<Map<String, Object>>() {});
            CompletableFuture.runAsync(() -> {
                assessmentServiceV5.processNotification(map);
            });
        } catch (Exception e) {
            logger.error(String.format("Error in notification request:: %s " + e.getMessage()),e);
        }
    }
}
