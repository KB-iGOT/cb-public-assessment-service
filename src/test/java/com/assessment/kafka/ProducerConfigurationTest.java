package com.assessment.kafka;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class ProducerConfigurationTest {

    private ProducerConfiguration config;

    @BeforeEach
    void setUp() throws Exception {
        config = new ProducerConfiguration();

        // set private field kafkabootstrapAddress via reflection
        Field field = ProducerConfiguration.class.getDeclaredField("kafkabootstrapAddress");
        field.setAccessible(true);
        field.set(config, "localhost:9092");
    }

    @Test
    void testKafkaTemplate() {
        KafkaTemplate<String, String> template = config.kafkaTemplate();
        assertNotNull(template);
    }
}
