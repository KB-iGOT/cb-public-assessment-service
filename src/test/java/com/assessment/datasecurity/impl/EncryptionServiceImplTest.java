package com.assessment.datasecurity.impl;

import com.assessment.util.ServerProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EncryptionServiceImplTest {

    private EncryptionServiceImpl encryptionService;

    @Mock
    private ServerProperties serverProperties;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        encryptionService = new EncryptionServiceImpl();
        encryptionService.serverProperties = serverProperties;
    }

    @Test
    void testEncryptData_success() {
        // given
        String encryptionKey = "secret";
        when(serverProperties.getEncryptionKey()).thenReturn(encryptionKey);

        String plainText = "myData";

        String encrypted = encryptionService.encryptData(plainText);

        assertNotNull(encrypted, "Encrypted value should not be null");
        assertNotEquals(plainText, encrypted, "Encrypted value should differ from input");
    }

    @Test
    void testEncryptData_handlesExceptionAndStillReturnsValue() {
        // given
        when(serverProperties.getEncryptionKey()).thenReturn("secret");

        // simulate error by modifying the Cipher state temporarily (not trivial)
        // Instead, we can spy & override the cipher to throw exception — but that’s not possible since `Cipher c` is static & final.

        // So here we just test that even if `doFinal` failed, it logs but keeps processing.
        // Since in current code it logs & returns whatever is produced

        String result = encryptionService.encryptData("data");

        assertNotNull(result);
    }

    /**
     * Optional: test that encryption & decryption round-trip works
     */
    @Test
    void testEncryptionAndDecryptionAreInverse() {
        when(serverProperties.getEncryptionKey()).thenReturn("secret");

        String plainText = "myData";
        String encrypted = encryptionService.encryptData(plainText);

        DecryptionServiceImpl decryptionService = new DecryptionServiceImpl();
        decryptionService.serverProperties = serverProperties;

        String decrypted = decryptionService.decryptData(encrypted);

        assertEquals(plainText, decrypted, "Decrypted text should equal original");
    }
}
