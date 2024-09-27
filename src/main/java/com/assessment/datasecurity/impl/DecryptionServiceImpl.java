package com.assessment.datasecurity.impl;

import com.assessment.datasecurity.DecryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;

@Service
public class DecryptionServiceImpl implements DecryptionService {

    private static Logger log = LoggerFactory.getLogger(DecryptionServiceImpl.class);


    private static Cipher c;

    static String ALGORITHM = "AES";
    int ITERATIONS = 3;
    static byte[] keyValue =
            new byte[]{'T', 'h', 'i', 's', 'A', 's', 'I', 'S', 'e', 'r', 'c', 'e', 'K', 't', 'e', 'y'};


    static {
        try {
            Key key = generateKey();
            c = Cipher.getInstance(ALGORITHM);
            c.init(Cipher.DECRYPT_MODE, key);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private static Key generateKey() {
        return new SecretKeySpec(keyValue, ALGORITHM);
    }

    public String decryptData(String value) {
        try {
            String dValue = null;
            String valueToDecrypt = value.trim();
            String sunbird_encryption = "PassWord";
            for (int i = 0; i < ITERATIONS; i++) {
                byte[] decordedValue = new BASE64Decoder().decodeBuffer(valueToDecrypt);
                byte[] decValue = c.doFinal(decordedValue);
                dValue =
                        new String(decValue, StandardCharsets.UTF_8).substring(sunbird_encryption.length());
                valueToDecrypt = dValue;
            }
            return dValue;
        } catch (Exception ex) {
            // This could happen with masked email and phone number. Not others.
            log.error("DefaultDecryptionServiceImpl:decrypt: ignorable errorMsg = ", ex);
        }
        return value;
    }
}
