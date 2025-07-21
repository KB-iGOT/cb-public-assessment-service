package com.assessment.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConsumerConfigurationTest {

    private ConsumerConfiguration config;

    @BeforeEach
    void setUp() {
        config = new ConsumerConfiguration();
    }

    @Test
    void testConsumerConfigs() {
        Map<String, Object> props = config.consumerConfigs();

        assertEquals(true, props.get(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG));
        assertEquals("1000", props.get(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG));
        assertEquals("15000", props.get(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG));
        assertEquals(StringDeserializer.class, props.get(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG));
        assertEquals(StringDeserializer.class, props.get(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG));
    }

    @Test
    void testConsumerFactory() {
        ConsumerFactory<String, String> factory = config.consumerFactory();

        assertNotNull(factory);
    }

    @Test
    void testKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                (ConcurrentKafkaListenerContainerFactory<String, String>) config.kafkaListenerContainerFactory();

        assertNotNull(factory);
        assertNotNull(factory.getConsumerFactory());
        assertEquals(3000L, factory.getContainerProperties().getPollTimeout());
    }
}
