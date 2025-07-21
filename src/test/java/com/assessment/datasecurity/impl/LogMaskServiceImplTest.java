package com.assessment.datasecurity.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LogMaskServiceImplTest {

    private LogMaskServiceImpl logMaskService;

    @BeforeEach
    void setUp() {
        logMaskService = new LogMaskServiceImpl();
    }

    @Test
    void testMaskEmail_withNormalEmail() {
        String email = "example@gmail.com";
        String masked = logMaskService.maskEmail(email);

        assertNotNull(masked);
        assertTrue(masked.startsWith("ex"));
        assertTrue(masked.contains("@gmail.com"));
        assertNotEquals(email, masked);
    }

    @Test
    void testMaskEmail_withShortEmail() {
        String email = "ab@x.com";
        String masked = logMaskService.maskEmail(email);

        assertNotNull(masked);
        assertEquals("ab@x.com", masked);
    }

    @Test
    void testMaskPhone_withNormalPhone() {
        String phone = "9876543210";
        String masked = logMaskService.maskPhone(phone);

        assertNotNull(masked);
        assertTrue(masked.startsWith("98765"));
        assertNotEquals(phone, masked);

    }

    @Test
    void testMaskPhone_withShortPhone() {
        String phone = "12345";
        String masked = logMaskService.maskPhone(phone);

        assertNotNull(masked);
        assertEquals("12345", masked); // since no digits beyond 5 to mask
    }

    @Test
    void testMaskEmail_withEmptyString() {
        String email = "";
        String masked = logMaskService.maskEmail(email);

        assertEquals("", masked);
    }

    @Test
    void testMaskPhone_withEmptyString() {
        String phone = "";
        String masked = logMaskService.maskPhone(phone);

        assertEquals("", masked);
    }
}
