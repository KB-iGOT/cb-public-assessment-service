package com.assessment.datasecurity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OneWayHashingTest {

    @Test
    void testEncryptVal_returnsConsistentHash() {
        String input = "password123";
        String hash1 = OneWayHashing.encryptVal(input);
        String hash2 = OneWayHashing.encryptVal(input);

        assertNotNull(hash1);
        assertNotNull(hash2);
        assertEquals(hash1, hash2, "Hash should be consistent for same input");
    }

    @Test
    void testEncryptVal_differentInputsGiveDifferentHashes() {
        String input1 = "password123";
        String input2 = "Password123";

        String hash1 = OneWayHashing.encryptVal(input1);
        String hash2 = OneWayHashing.encryptVal(input2);

        assertNotNull(hash1);
        assertNotNull(hash2);
        assertNotEquals(hash1, hash2, "Different inputs should produce different hashes");
    }

    @Test
    void testEncryptVal_emptyString() {
        String hash = OneWayHashing.encryptVal("");

        assertNotNull(hash);
        assertFalse(hash.isEmpty(), "Hash of empty string should not be empty");
    }

    @Test
    void testEncryptVal_nullInput() {
        String hash = OneWayHashing.encryptVal(null);

        assertNotNull(hash);
        assertEquals("", hash, "Hash should return empty string on null input or exception");
    }

    @Test
    void testEncryptVal_hashIsHex() {
        String input = "test";
        String hash = OneWayHashing.encryptVal(input);

        assertNotNull(hash);
        assertTrue(hash.matches("[0-9a-f]+"), "Hash should be hexadecimal");
        assertEquals(64, hash.length(), "SHA-256 hash should have length 64 in hex");
    }
}
