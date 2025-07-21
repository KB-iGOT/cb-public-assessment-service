package com.assessment.cache.controller;

import com.assessment.cache.service.RedisCacheService;
import com.assessment.model.SBApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class RedisCacheControllerTest {

    @Mock
    private RedisCacheService redisCacheService;

    @InjectMocks
    private RedisCacheController redisCacheController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testDeleteCache() throws Exception {
        SBApiResponse mockResponse = new SBApiResponse();
        mockResponse.setResponseCode(org.springframework.http.HttpStatus.OK);
        when(redisCacheService.deleteCache()).thenReturn(mockResponse);

        ResponseEntity<?> response = redisCacheController.deleteCache();

        assertThat(response.getBody()).isEqualTo(mockResponse);
        assertThat(response.getStatusCode()).isEqualTo(mockResponse.getResponseCode());
    }

    @Test
    void testGetKeys() throws Exception {
        SBApiResponse mockResponse = new SBApiResponse();
        mockResponse.setResponseCode(org.springframework.http.HttpStatus.OK);
        when(redisCacheService.getKeys()).thenReturn(mockResponse);

        ResponseEntity<?> response = redisCacheController.getKeys();

        assertThat(response.getBody()).isEqualTo(mockResponse);
        assertThat(response.getStatusCode()).isEqualTo(mockResponse.getResponseCode());
    }

    @Test
    void testGetKeysAndValues() throws Exception {
        SBApiResponse mockResponse = new SBApiResponse();
        mockResponse.setResponseCode(org.springframework.http.HttpStatus.OK);
        when(redisCacheService.getKeysAndValues()).thenReturn(mockResponse);

        ResponseEntity<?> response = redisCacheController.getKeysAndValues();

        assertThat(response.getBody()).isEqualTo(mockResponse);
        assertThat(response.getStatusCode()).isEqualTo(mockResponse.getResponseCode());
    }
}
