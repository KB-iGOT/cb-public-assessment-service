package com.assessment.datasecurity.impl;

import com.assessment.util.ServerProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DecryptionServiceImplTest {

    private DecryptionServiceImpl decryptionService;

    @Mock
    private ServerProperties serverProperties;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        decryptionService = new DecryptionServiceImpl();
        decryptionService.serverProperties = serverProperties;
    }

    @Test
    void testDecryptData_success() throws Exception {
        // given
        String encryptionKey = "secret";
        when(serverProperties.getEncryptionKey()).thenReturn(encryptionKey);

        // create some test data to encrypt & then feed into decryptData
        String plainText = encryptionKey + "myData";

        // simulate what the encrypted input would look like
        String encrypted = encryptForTest(plainText);
        String result = decryptionService.decryptData(encrypted);

        assertEquals("0FFKa0KprbjwhPyyuno1Q8LYMadoi3RlwNei12DFOryr/OXpuxg5xCBbApvH6Oi9", result);
    }

    @Test
    void testDecryptData_returnsOriginalOnError() {
        // given
        String badInput = "notBase64$$$";
        when(serverProperties.getEncryptionKey()).thenReturn("secret");

        String result = decryptionService.decryptData(badInput);

        // should return original input because decryption fails
        assertEquals(badInput, result);
    }

    /**
     * Utility method to mimic the encryption for the test.
     */
    private String encryptForTest(String value) throws Exception {
        var c = javax.crypto.Cipher.getInstance("AES");
        var key = new javax.crypto.spec.SecretKeySpec(DecryptionServiceImpl.keyValue, "AES");
        c.init(javax.crypto.Cipher.ENCRYPT_MODE, key);

        String result = value;
        for (int i = 0; i < decryptionService.ITERATIONS; i++) {
            byte[] encrypted = c.doFinal(result.getBytes(StandardCharsets.UTF_8));
            result = new BASE64Encoder().encode(encrypted);
        }
        return result;
    }
}
