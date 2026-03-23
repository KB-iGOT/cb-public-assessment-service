package com.assessment.controller;

import com.assessment.model.SBApiResponse;
import com.assessment.service.HealthCheckService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for HealthController
 * Tests both health check and liveness endpoints
 */
@RunWith(MockitoJUnitRunner.class)
public class HealthControllerTest {

    @Mock
    private HealthCheckService healthCheckService;

    private HealthController healthController;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        healthController = new HealthController(healthCheckService);
        mockMvc = MockMvcBuilders.standaloneSetup(healthController).build();
    }

    // ==================== Health Endpoint Tests ====================

    @Test
    public void testHealth_AllServicesHealthy_ReturnsOK() {
        // Arrange
        SBApiResponse mockResponse = new SBApiResponse("health.check");
        mockResponse.put("status", "UP");
        mockResponse.put("healthy", true);
        mockResponse.setResponseCode(HttpStatus.OK);

        when(healthCheckService.checkHealth()).thenReturn(mockResponse);

        // Act
        ResponseEntity<SBApiResponse> response = healthController.health();

        // Assert
        assertNotNull("Response should not be null", response);
        assertEquals("Response code should be 200 OK", HttpStatus.OK, response.getStatusCode());
        assertNotNull("Response body should not be null", response.getBody());
        assertEquals("Status should be UP", "UP", response.getBody().get("status"));
        assertEquals("Healthy should be true", true, response.getBody().get("healthy"));

        // Verify service was called
        verify(healthCheckService, times(1)).checkHealth();
    }

    @Test
    public void testHealth_ServicesUnhealthy_ReturnsServiceUnavailable() {
        // Arrange
        SBApiResponse mockResponse = new SBApiResponse("health.check");
        mockResponse.put("status", "DOWN");
        mockResponse.put("healthy", false);
        mockResponse.setResponseCode(HttpStatus.SERVICE_UNAVAILABLE);

        when(healthCheckService.checkHealth()).thenReturn(mockResponse);

        // Act
        ResponseEntity<SBApiResponse> response = healthController.health();

        // Assert
        assertNotNull("Response should not be null", response);
        assertEquals("Response code should be 503 SERVICE_UNAVAILABLE",
                     HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull("Response body should not be null", response.getBody());
        assertEquals("Status should be DOWN", "DOWN", response.getBody().get("status"));
        assertEquals("Healthy should be false", false, response.getBody().get("healthy"));

        // Verify service was called
        verify(healthCheckService, times(1)).checkHealth();
    }

    @Test
    public void testHealth_WithMockMvc_ReturnsOK() throws Exception {
        // Arrange
        SBApiResponse mockResponse = new SBApiResponse("health.check");
        mockResponse.put("status", "UP");
        mockResponse.put("healthy", true);
        mockResponse.setResponseCode(HttpStatus.OK);

        when(healthCheckService.checkHealth()).thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("UP"))
                .andExpect(jsonPath("$.result.healthy").value(true));

        // Verify service was called
        verify(healthCheckService, times(1)).checkHealth();
    }

    @Test
    public void testHealth_WithMockMvc_ReturnsServiceUnavailable() throws Exception {
        // Arrange
        SBApiResponse mockResponse = new SBApiResponse("health.check");
        mockResponse.put("status", "DOWN");
        mockResponse.put("healthy", false);
        mockResponse.setResponseCode(HttpStatus.SERVICE_UNAVAILABLE);

        when(healthCheckService.checkHealth()).thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(get("/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.result.status").value("DOWN"))
                .andExpect(jsonPath("$.result.healthy").value(false));

        // Verify service was called
        verify(healthCheckService, times(1)).checkHealth();
    }

    @Test
    public void testHealth_ResponseContainsSBApiResponseStructure() {
        // Arrange
        SBApiResponse mockResponse = new SBApiResponse("health.check");
        mockResponse.put("status", "UP");
        mockResponse.put("healthy", true);
        mockResponse.setResponseCode(HttpStatus.OK);

        when(healthCheckService.checkHealth()).thenReturn(mockResponse);

        // Act
        ResponseEntity<SBApiResponse> response = healthController.health();

        // Assert
        SBApiResponse body = response.getBody();
        assertNotNull("Response body should not be null", body);
        assertNotNull("Response ID should not be null", body.getId());
        assertNotNull("Response version should not be null", body.getVer());
        assertNotNull("Response timestamp should not be null", body.getTs());
        assertNotNull("Response params should not be null", body.getParams());
        assertNotNull("Response result should not be null", body.getResult());
    }

    @Test
    public void testHealth_ServiceReturnsNullResponseCode_HandlesGracefully() {
        // Arrange
        SBApiResponse mockResponse = new SBApiResponse("health.check");
        mockResponse.put("status", "UP");
        mockResponse.put("healthy", true);
        mockResponse.setResponseCode(null); // Null response code

        when(healthCheckService.checkHealth()).thenReturn(mockResponse);

        // Act
        ResponseEntity<SBApiResponse> response = healthController.health();

        // Assert
        assertNotNull("Response should not be null", response);
        // Should handle null gracefully, might default to OK or return null
        assertNotNull("Response body should not be null", response.getBody());
    }

    // ==================== Liveness Endpoint Tests ====================

    @Test
    public void testLiveness_ReturnsOK() {
        // Act
        ResponseEntity<?> response = healthController.liveness();

        // Assert
        assertNotNull("Response should not be null", response);
        assertEquals("Response code should be 200 OK", HttpStatus.OK, response.getStatusCode());
        assertNotNull("Response body should not be null", response.getBody());
    }

    @Test
    public void testLiveness_ReturnsCorrectStatus() {
        // Act
        ResponseEntity<?> response = healthController.liveness();

        // Assert
        Object body = response.getBody();
        assertTrue("Response body should be a Map", body instanceof Map);

        @SuppressWarnings("unchecked")
        Map<String, String> responseMap = (Map<String, String>) body;

        assertEquals("Status should be UP", "UP", responseMap.get("status"));
        assertEquals("Message should be 'Service is alive'",
                     "Service is alive", responseMap.get("message"));
    }

    @Test
    public void testLiveness_WithMockMvc_ReturnsOK() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/health/live"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.message").value("Service is alive"));
    }

    @Test
    public void testLiveness_DoesNotCallHealthCheckService() {
        // Act
        healthController.liveness();

        // Assert - verify that health check service is never called for liveness
        verify(healthCheckService, never()).checkHealth();
    }

    @Test
    public void testLiveness_AlwaysReturnsSuccess() {
        // Act - Call multiple times
        ResponseEntity<?> response1 = healthController.liveness();
        ResponseEntity<?> response2 = healthController.liveness();
        ResponseEntity<?> response3 = healthController.liveness();

        // Assert - All should return OK
        assertEquals(HttpStatus.OK, response1.getStatusCode());
        assertEquals(HttpStatus.OK, response2.getStatusCode());
        assertEquals(HttpStatus.OK, response3.getStatusCode());
    }

    @Test
    public void testLiveness_ResponseBodyIsNotNull() {
        // Act
        ResponseEntity<?> response = healthController.liveness();

        // Assert
        assertNotNull("Response body should not be null", response.getBody());

        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();

        assertNotNull("Status should not be null", body.get("status"));
        assertNotNull("Message should not be null", body.get("message"));
    }

    // ==================== Integration Tests ====================

    @Test
    public void testHealth_MultipleCallsWithDifferentResults() {
        // Arrange
        SBApiResponse healthyResponse = new SBApiResponse("health.check");
        healthyResponse.put("status", "UP");
        healthyResponse.put("healthy", true);
        healthyResponse.setResponseCode(HttpStatus.OK);

        SBApiResponse unhealthyResponse = new SBApiResponse("health.check");
        unhealthyResponse.put("status", "DOWN");
        unhealthyResponse.put("healthy", false);
        unhealthyResponse.setResponseCode(HttpStatus.SERVICE_UNAVAILABLE);

        when(healthCheckService.checkHealth())
                .thenReturn(healthyResponse)
                .thenReturn(unhealthyResponse);

        // Act
        ResponseEntity<SBApiResponse> response1 = healthController.health();
        ResponseEntity<SBApiResponse> response2 = healthController.health();

        // Assert
        assertEquals(HttpStatus.OK, response1.getStatusCode());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response2.getStatusCode());

        verify(healthCheckService, times(2)).checkHealth();
    }

    @Test
    public void testHealth_VerifyEndpointMapping() throws Exception {
        // Arrange
        SBApiResponse mockResponse = new SBApiResponse("health.check");
        mockResponse.put("status", "UP");
        mockResponse.setResponseCode(HttpStatus.OK);

        when(healthCheckService.checkHealth()).thenReturn(mockResponse);

        // Act & Assert - Test base endpoint
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk());
    }

    @Test
    public void testLiveness_VerifyEndpointMapping() throws Exception {
        // Act & Assert - Test liveness endpoint
        mockMvc.perform(get("/health/live"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    // ==================== Constructor and Dependency Tests ====================

    @Test
    public void testConstructor_WithValidService() {
        // Act
        HealthController controller = new HealthController(healthCheckService);

        // Assert
        assertNotNull("Controller should be created", controller);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructor_WithNullService_ThrowsException() {
        // Act
        HealthController controller = new HealthController(null);

        // This should throw NullPointerException when trying to use the service
        controller.health();
    }

    // ==================== Response Headers and Content Type Tests ====================

    @Test
    public void testHealth_ContentTypeIsJson() throws Exception {
        // Arrange
        SBApiResponse mockResponse = new SBApiResponse("health.check");
        mockResponse.put("status", "UP");
        mockResponse.setResponseCode(HttpStatus.OK);

        when(healthCheckService.checkHealth()).thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"));
    }

    @Test
    public void testLiveness_ContentTypeIsJson() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/health/live"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"));
    }

    // ==================== Error Handling Tests ====================

    @Test(expected = RuntimeException.class)
    public void testHealth_ServiceThrowsException_PropagatesException() {
        // Arrange
        when(healthCheckService.checkHealth()).thenThrow(new RuntimeException("Service failure"));

        // Act
        healthController.health();

        // Assert - Exception should be propagated
    }

    @Test
    public void testHealth_ServiceThrowsException_WithMockMvc() throws Exception {
        // Arrange
        when(healthCheckService.checkHealth()).thenThrow(new RuntimeException("Service failure"));

        // Act & Assert
        mockMvc.perform(get("/health"))
                .andExpect(status().is5xxServerError());
    }
}