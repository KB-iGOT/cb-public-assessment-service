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

    @KafkaListener(groupId="${kafka.topic.certificate.assessment.group}", topics = "${kafka.topic.certificate.request}")
    public void notificationConsumer(ConsumerRecord<String, String> request) {
        logger.info("kafka notification received");
        try{
            logger.info("Received notification request: " + request.value());
            Map<String, Object> map = mapper.readValue(request.value(), HashMap.class);
            CompletableFuture.runAsync(() -> {
                assessmentServiceV5.processNotification(map);
            });
        } catch (Exception e) {
            logger.error(String.format("Error in notification request:: %s " + e.getMessage()),e);
        }
    }

    @KafkaListener(groupId="${kafka.topic.certificate.assessment.group}", topics = "${kafka.topic.download.certificate}")
    public void notifyDownloadCertificate(ConsumerRecord<String, String> request) {
        logger.info("kafka notification received");
        try{
            logger.info("Received notification request: " + request.value());
            Map<String, Object> map = mapper.readValue(request.value(), HashMap.class);
            CompletableFuture.runAsync(() -> {
                assessmentServiceV5.processDownloadNotification(map);
            });
        } catch (Exception e) {
            logger.error(String.format("Error in notification request:: %s " + e.getMessage()),e);
        }
    }
}
