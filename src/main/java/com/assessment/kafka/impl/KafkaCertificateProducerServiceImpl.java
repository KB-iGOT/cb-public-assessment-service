package com.assessment.kafka.impl;

import com.assessment.kafka.Producer;
import com.assessment.kafka.service.KafkaCertificateProducerService;
import com.assessment.util.Constants;
import com.assessment.util.ServerProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;


/**
 * @author mahesh.vakkund
 */
@Service
public class KafkaCertificateProducerServiceImpl implements KafkaCertificateProducerService {
    private ObjectMapper mapper = new ObjectMapper();
    private final Logger log = LoggerFactory.getLogger(KafkaCertificateProducerServiceImpl.class);

    @Autowired
    RestTemplate restTemplate;


    @Autowired
    private Producer producer;

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private ServerProperties serverProperties;


    public static Timestamp convertToTimestamp(String dateString) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            Date parsedDate = dateFormat.parse(dateString);
            return new Timestamp(parsedDate.getTime());
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }


    public void replacePlaceholders(JsonNode jsonNode, Map<String, Object> certificateRequest) {
        log.debug("KafkaConsumer :: replacePlaceholders");
        if (jsonNode.isObject()) {
            ObjectNode objectNode = (ObjectNode) jsonNode;
            objectNode.fields().forEachRemaining(entry -> {
                JsonNode value = entry.getValue();
                if (value.isTextual()) {
                    String textValue = value.asText();
                    if (textValue.startsWith("${") && textValue.endsWith("}")) {
                        String placeholder = textValue.substring(2, textValue.length() - 1);
                        String replacement = getReplacementValue(placeholder, certificateRequest);
                        objectNode.put(entry.getKey(), replacement);
                    }
                } else if (value.isArray()) {
                    value.elements().forEachRemaining(element -> {
                        if (element.isObject()) {
                            replacePlaceholders(element, certificateRequest);
                        }
                    });
                } else {
                    replacePlaceholders(value, certificateRequest);
                }
            });
        }
    }

    private String getReplacementValue(String placeholder, Map<String, Object> certificateRequest) {
        log.debug("KafkaConsumer :: getReplacementValue");
        switch (placeholder) {
            case "user.id":
                return (String) certificateRequest.get(Constants.USER_ID);
            case "course.id":
                return (String) certificateRequest.get(Constants.COURSE_ID);
            case "today.date":
                return (String) certificateRequest.get(Constants.COMPLETION_DATE);
            case "time.ms":
                return String.valueOf(System.currentTimeMillis());
            case "unique.id":
                return UUID.randomUUID().toString();
            case "course.name":
                return (String) certificateRequest.get(Constants.COURSE_NAME);
            case "provider.name":
                return (String) certificateRequest.get(Constants.PROVIDER_NAME);
            case "user.name":
                return (String) certificateRequest.get(Constants.RECIPIENT_NAME);
            case "course.poster.image":
                return (String) certificateRequest.get(Constants.COURSE_POSTER_IMAGE);
            case "svgTemplate":
                return serverProperties.getSvgTemplate();
            case "assessment.id":
                return (String) certificateRequest.get(Constants.ASSESSMENT_ID_KEY);
            default:
                return "";
        }
    }

    private static String convertDateFormat(String originalDate) {
        DateTimeFormatter originalFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        LocalDate date = LocalDate.parse(originalDate, originalFormatter);
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return date.format(outputFormatter);
    }
}
