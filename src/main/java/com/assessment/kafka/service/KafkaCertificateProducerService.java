package com.assessment.kafka.service;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

public interface KafkaCertificateProducerService {

    public void replacePlaceholders(JsonNode jsonNode, Map<String, Object> certificateRequest);
}