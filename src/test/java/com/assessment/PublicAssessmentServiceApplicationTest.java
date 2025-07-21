package com.assessment;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PublicAssessmentServiceApplicationTest {

    @Test
    void restTemplateBeanCreated() throws Exception {
        PublicAssessmentServiceApplication app = new PublicAssessmentServiceApplication();
        RestTemplate restTemplate = app.restTemplate();
        assertNotNull(restTemplate);

        ClientHttpRequestFactory factory = restTemplate.getRequestFactory();
        assertNotNull(factory);
    }
}
