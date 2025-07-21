package com.assessment.util;

import com.assessment.model.SBApiResponse;
import com.assessment.util.exceptions.ProjectCommonException;
import com.assessment.util.exceptions.ResponseCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProjectUtilTest {

    @Test
    void testIsStringNullOREmpty() {
        assertTrue(ProjectUtil.isStringNullOREmpty(null));
        assertTrue(ProjectUtil.isStringNullOREmpty(""));
        assertTrue(ProjectUtil.isStringNullOREmpty("   "));
        assertFalse(ProjectUtil.isStringNullOREmpty("hello"));
    }

    @Test
    void testCreateServerError() {
        ProjectCommonException ex = ProjectUtil.createServerError(ResponseCode.SERVER_ERROR);
        assertNotNull(ex);
        assertEquals(ResponseCode.SERVER_ERROR.getResponseCode(), ex.getResponseCode());
    }

    @Test
    void testCreateClientError() {
        ProjectCommonException ex = ProjectUtil.createClientException(ResponseCode.CLIENT_ERROR);
        assertNotNull(ex);
        assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), ex.getResponseCode());
    }

    @Test
    void testCreateDefaultResponse() {
        SBApiResponse response = ProjectUtil.createDefaultResponse("testAPI");
        assertEquals("testAPI", response.getId());
        assertEquals(Constants.API_VERSION_1, response.getVer());
        assertEquals(Constants.SUCCESS, response.getParams().getStatus());
        assertEquals(HttpStatus.OK, response.getResponseCode());
        assertNotNull(response.getTs());
    }

    @Test
    void testGetDefaultHeaders() {
        Map<String, String> headers = ProjectUtil.getDefaultHeaders();
        assertEquals(1, headers.size());
        assertEquals(Constants.APPLICATION_JSON, headers.get(Constants.CONTENT_TYPE));
    }

    @Test
    void testConvertSecondsToHrsAndMinutes() {
        assertEquals("", ProjectUtil.convertSecondsToHrsAndMinutes(30)); // < 60
        assertEquals("59m", ProjectUtil.convertSecondsToHrsAndMinutes(3540));
        assertEquals("01h", ProjectUtil.convertSecondsToHrsAndMinutes(3600));
        assertEquals("01h 01m", ProjectUtil.convertSecondsToHrsAndMinutes(3660));
    }

    @Test
    void testFirstLetterCapitalWithSingleSpace() {
        String result = ProjectUtil.firstLetterCapitalWithSingleSpace("   hello   world   ");
        assertEquals("Hello World", result);

        result = ProjectUtil.firstLetterCapitalWithSingleSpace("java");
        assertEquals("Java", result);
    }

    @Test
    void testValidateEmailPattern() {
        assertTrue(ProjectUtil.validateEmailPattern("test@example.com"));
        assertFalse(ProjectUtil.validateEmailPattern("invalid-email"));
    }

    @Test
    void testValidateFullName() {
        assertTrue(ProjectUtil.validateFullName("John Doe"));
        assertTrue(ProjectUtil.validateFullName("O'Connor"));
        assertFalse(ProjectUtil.validateFullName("Invalid\nName"));
        assertFalse(ProjectUtil.validateFullName("Mr. "));
    }

    @Test
    void testUpdateErrorDetails() {
        SBApiResponse response = ProjectUtil.createDefaultResponse("testAPI");
        ProjectUtil.updateErrorDetails(response, "Something went wrong", HttpStatus.BAD_REQUEST);
        assertEquals(Constants.FAILED, response.getParams().getStatus());
        assertEquals("Something went wrong", response.getParams().getErrmsg());
        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
    }
}
