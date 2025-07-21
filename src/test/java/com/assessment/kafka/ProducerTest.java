package com.assessment.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.util.concurrent.ListenableFuture;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProducerTest {

    @InjectMocks
    private Producer producer;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testPush_success() {
        when(kafkaTemplate.send(anyString(), anyString())).thenReturn(mock(ListenableFuture.class));

        producer.push("test-topic", new TestPayload("id", "name"));

        verify(kafkaTemplate).send(eq("test-topic"), anyString());
    }

    @Test
    void testPush_jsonException() throws JsonProcessingException {
        Object value = mock(Object.class);

        ObjectMapper mapper = mock(ObjectMapper.class);
        doThrow(new JsonProcessingException("error") {}).when(mapper).writeValueAsString(value);

        Producer producerSpy = spy(producer);

        producerSpy.push("topic", value);

        verify(producerSpy).push("topic", value);
    }

    @Test
    void testPushWithKey_success() {
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(mock(ListenableFuture.class));

        producer.pushWithKey("test-topic", new TestPayload("id", "name"), "key1");

        verify(kafkaTemplate).send(eq("test-topic"), eq("key1"), anyString());
    }

    @Test
    void testPushWithKey_jsonException() throws JsonProcessingException {
        Object value = mock(Object.class);

        ObjectMapper mapper = mock(ObjectMapper.class);
        doThrow(new JsonProcessingException("error") {}).when(mapper).writeValueAsString(value);

        Producer producerSpy = spy(producer);

        producerSpy.pushWithKey("topic", value, "key");

        verify(producerSpy).pushWithKey("topic", value, "key");
    }

    // Helper DTO
    static class TestPayload {
        public String id;
        public String name;
        public TestPayload(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
