package com.assessment.consumer;

import com.assessment.service.AssessmentServiceV5;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.slf4j.Logger;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

class NotificationConsumerTest {

    @InjectMocks
    private NotificationConsumer consumer;

    @Mock
    private AssessmentServiceV5 assessmentServiceV5;

    @Mock
    private Logger logger;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Replace private logger with our mock
        TestUtils.setField(consumer, "logger", logger);
    }

    @Test
    void testNotificationConsumer_validRequest() throws Exception {
        Map<String, Object> message = Map.of("key", "value");
        String json = objectMapper.writeValueAsString(message);

        ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<>("topic", 0, 0, null, json);

        consumer.notificationConsumer(consumerRecord);

        verify(logger).info("kafka notification received");
        verify(logger).info(contains("Received notification request"));
        verify(assessmentServiceV5, atLeastOnce()).processNotification(anyMap());
    }

    @Test
    void testNotifyDownloadCertificate_validRequest() throws Exception {
        Map<String, Object> message = Map.of("key", "value");
        String json = objectMapper.writeValueAsString(message);

        ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<>("topic", 0, 0, null, json);

        consumer.notifyDownloadCertificate(consumerRecord);

        verify(logger).info("kafka notification received");
        verify(logger).info(contains("Received notification request"));
    }

}
