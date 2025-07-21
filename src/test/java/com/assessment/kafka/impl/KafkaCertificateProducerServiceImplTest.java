package com.assessment.kafka.impl;

import com.assessment.kafka.Producer;
import com.assessment.util.Constants;
import com.assessment.util.ServerProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.client.RestTemplate;

import java.sql.Timestamp;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class KafkaCertificateProducerServiceImplTest {

    @InjectMocks
    KafkaCertificateProducerServiceImpl service;

    @Mock
    RestTemplate restTemplate;

    @Mock
    Producer producer;

    @Mock
    ResourceLoader resourceLoader;

    @Mock
    ServerProperties serverProperties;

    ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testConvertToTimestamp_valid() {
        Timestamp ts = KafkaCertificateProducerServiceImpl.convertToTimestamp("12/31/2020");
        assertNotNull(ts);
    }

    @Test
    void testConvertToTimestamp_invalid() {
        Timestamp ts = KafkaCertificateProducerServiceImpl.convertToTimestamp("invalid-date");
        assertNull(ts);
    }

    @Test
    void testConvertDateFormat() {
        String result = invokeConvertDateFormat("12/31/2020");
        assertEquals("2020-12-31", result);
    }

    private String invokeConvertDateFormat(String date) {
        try {
            var method = KafkaCertificateProducerServiceImpl.class.getDeclaredMethod("convertDateFormat", String.class);
            method.setAccessible(true);
            return (String) method.invoke(null, date);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testReplacePlaceholders_withAllCases() {
        ObjectNode root = mapper.createObjectNode();
        root.put("user", "${user.id}");
        root.put("unknown", "${unknown.placeholder}");
        ArrayNode arr = root.putArray("array");
        ObjectNode child = mapper.createObjectNode();
        child.put("course", "${course.id}");
        arr.add(child);

        Map<String, Object> request = Map.of(
                Constants.USER_ID, "u1",
                Constants.COURSE_ID, "c1",
                Constants.COMPLETION_DATE, "2021-01-01",
                Constants.COURSE_NAME, "courseName",
                Constants.PROVIDER_NAME, "provider",
                Constants.RECIPIENT_NAME, "recipient",
                Constants.COURSE_POSTER_IMAGE, "image.png",
                Constants.ASSESSMENT_ID_KEY, "aid"
        );

        when(serverProperties.getSvgTemplate()).thenReturn("svgTemplateValue");

        service.replacePlaceholders(root, request);

        assertEquals("u1", root.get("user").asText());
        assertEquals("", root.get("unknown").asText());
        assertEquals("c1", root.get("array").get(0).get("course").asText());
    }

    @Test
    void testGetReplacementValue_allCases() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put(Constants.USER_ID, "u1");
        request.put(Constants.COURSE_ID, "c1");
        request.put(Constants.COMPLETION_DATE, "2021-01-01");
        request.put(Constants.COURSE_NAME, "courseName");
        request.put(Constants.PROVIDER_NAME, "provider");
        request.put(Constants.RECIPIENT_NAME, "recipient");
        request.put(Constants.COURSE_POSTER_IMAGE, "image.png");
        request.put(Constants.ASSESSMENT_ID_KEY, "aid");

        when(serverProperties.getSvgTemplate()).thenReturn("svgTemplateValue");

        // invoke private method using reflection
        var method = KafkaCertificateProducerServiceImpl.class
                .getDeclaredMethod("getReplacementValue", String.class, Map.class);
        method.setAccessible(true);

        assertEquals("u1", method.invoke(service, "user.id", request));
        assertEquals("c1", method.invoke(service, "course.id", request));
        assertEquals("2021-01-01", method.invoke(service, "today.date", request));
        assertNotNull(method.invoke(service, "time.ms", request));
        assertNotNull(method.invoke(service, "unique.id", request));
        assertEquals("courseName", method.invoke(service, "course.name", request));
        assertEquals("provider", method.invoke(service, "provider.name", request));
        assertEquals("recipient", method.invoke(service, "user.name", request));
        assertEquals("image.png", method.invoke(service, "course.poster.image", request));
        assertEquals("svgTemplateValue", method.invoke(service, "svgTemplate", request));
        assertEquals("aid", method.invoke(service, "assessment.id", request));
        assertEquals("", method.invoke(service, "unknown", request));
    }
}
