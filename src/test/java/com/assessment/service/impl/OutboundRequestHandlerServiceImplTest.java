package com.assessment.service.impl;

import com.assessment.service.OutboundRequestHandlerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OutboundRequestHandlerServiceImplTest {

    @InjectMocks
    private OutboundRequestHandlerServiceImpl service;

    @Mock
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void test_fetchResultUsingPost_withoutHeaders() {
        Map<String, Object> expectedResponse = new HashMap<>();
        expectedResponse.put("key", "value");

        when(restTemplate.postForObject(anyString(), any(), eq(Map.class)))
                .thenReturn(expectedResponse);

        Object response = service.fetchResultUsingPost("http://test", Collections.singletonMap("req", "val"));
        assertNotNull(response);
        assertEquals(expectedResponse, response);
    }

    @Test
    void test_fetchResult() {
        Map<String, Object> expectedResponse = new HashMap<>();
        expectedResponse.put("key", "value");

        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(expectedResponse);

        Object response = service.fetchResult("http://test");
        assertNotNull(response);
        assertEquals(expectedResponse, response);
    }

    @Test
    void test_fetchUsingGetWithHeaders() {
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(Collections.singletonMap("key", "value"), HttpStatus.OK);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(responseEntity);

        Object response = service.fetchUsingGetWithHeaders("http://test", Collections.singletonMap("h", "v"));
        assertNotNull(response);
        assertEquals(Collections.singletonMap("key", "value"), response);
    }

    @Test
    void test_fetchUsingGetWithHeadersProfile() {
        Map<String, Object> expectedResponse = Collections.singletonMap("profile", "data");

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(expectedResponse, HttpStatus.OK));

        Object response = service.fetchUsingGetWithHeadersProfile("http://test", Collections.singletonMap("h", "v"));
        assertNotNull(response);
        assertEquals(expectedResponse, response);
    }

    @Test
    void test_fetchResultUsingPost_withHeaders() {
        Map<String, Object> expectedResponse = Collections.singletonMap("key", "value");

        when(restTemplate.postForObject(anyString(), any(), eq(Map.class)))
                .thenReturn(expectedResponse);

        Map<String, Object> response = service.fetchResultUsingPost(
                "http://test", Collections.singletonMap("req", "val"), Collections.singletonMap("h", "v"));
        assertNotNull(response);
        assertEquals(expectedResponse, response);
    }

    @Test
    void test_fetchResultUsingPatch() {
        Map<String, Object> expectedResponse = Collections.singletonMap("patch", "done");

        when(restTemplate.patchForObject(anyString(), any(), eq(Map.class)))
                .thenReturn(expectedResponse);

        Map<String, Object> response = service.fetchResultUsingPatch(
                "http://test", Collections.singletonMap("req", "val"), Collections.singletonMap("h", "v"));

        assertNotNull(response);
        assertEquals(expectedResponse, response);
    }

    @Test
    void test_fetchResultUsingPatch_returnsEmptyMap() {
        when(restTemplate.patchForObject(anyString(), any(), eq(Map.class)))
                .thenReturn(null);

        Map<String, Object> response = service.fetchResultUsingPatch(
                "http://test", Collections.singletonMap("req", "val"), Collections.singletonMap("h", "v"));

        assertNotNull(response);
        assertTrue(response.isEmpty());
    }

    @Test
    void test_fetchResultUsingPost_httpClientError() {
        HttpClientErrorException ex = mock(HttpClientErrorException.class);
        when(ex.getResponseBodyAsString()).thenReturn("{\"error\":\"bad request\"}");
        when(restTemplate.postForObject(anyString(), any(), eq(Map.class)))
                .thenThrow(ex);

        Object response = service.fetchResultUsingPost("http://test", Collections.singletonMap("req", "val"));
        assertNotNull(response);
        assertTrue(response instanceof Map);
    }

    @Test
    void test_fetchResult_httpClientError() {
        HttpClientErrorException ex = mock(HttpClientErrorException.class);
        when(ex.getResponseBodyAsString()).thenReturn("{\"error\":\"bad request\"}");
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenThrow(ex);

        Object response = service.fetchResult("http://test");
        assertNotNull(response);
        assertTrue(response instanceof Map);
    }

    @Test
    void test_fetchUsingGetWithHeadersProfile_httpClientError() {
        HttpClientErrorException ex = mock(HttpClientErrorException.class);
        when(ex.getResponseBodyAsString()).thenReturn("{\"error\":\"bad request\"}");
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(ex);

        Object response = service.fetchUsingGetWithHeadersProfile("http://test", Collections.singletonMap("h", "v"));
        assertNotNull(response);
        assertTrue(response instanceof Map);
    }

    @Test
    void test_fetchUsingGetWithHeaders_httpClientError() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(HttpClientErrorException.class);

        Object response = service.fetchUsingGetWithHeaders("http://test", Collections.singletonMap("h", "v"));
        assertNull(response);
    }

    @Test
    void test_fetchResultUsingPatch_httpClientError() {
        HttpClientErrorException ex = mock(HttpClientErrorException.class);
        when(ex.getResponseBodyAsString()).thenReturn("{\"error\":\"bad request\"}");
        when(restTemplate.patchForObject(anyString(), any(), eq(Map.class)))
                .thenThrow(ex);

        Map<String, Object> response = service.fetchResultUsingPatch("http://test", Collections.singletonMap("req", "val"), Collections.singletonMap("h", "v"));
        assertNotNull(response);
        assertTrue(response instanceof Map);
    }
}
