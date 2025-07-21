package com.assessment.datasecurity.impl;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class BASE64EncoderTest {

    BASE64Encoder encoder = new BASE64Encoder();

    @Test
    void testBytesPerAtomAndLine() {
        assertEquals(3, encoder.bytesPerAtom());
        assertEquals(57, encoder.bytesPerLine());
    }

    @Test
    void testEncodeAtom_len1() throws IOException {
        byte[] data = { (byte) 'M' }; // ASCII 'M' → 77
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        encoder.encodeAtom(out, data, 0, 1);

        String result = out.toString(StandardCharsets.US_ASCII);
        assertEquals("TQ==", result);
    }

    @Test
    void testEncodeAtom_len2() throws IOException {
        byte[] data = { (byte) 'M', (byte) 'a' }; // ASCII 'M' 'a' → 77 97
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        encoder.encodeAtom(out, data, 0, 2);

        String result = out.toString(StandardCharsets.US_ASCII);
        assertEquals("TWE=", result);
    }

    @Test
    void testEncodeAtom_len3() throws IOException {
        byte[] data = { (byte) 'M', (byte) 'a', (byte) 'n' }; // ASCII 'M' 'a' 'n' → 77 97 110
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        encoder.encodeAtom(out, data, 0, 3);

        String result = out.toString(StandardCharsets.US_ASCII);
        assertEquals("TWFu", result);
    }

    @Test
    void testEncodeAtom_withOffset() throws IOException {
        byte[] data = { 0, (byte) 'M', (byte) 'a', (byte) 'n' }; // skip first dummy byte
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        encoder.encodeAtom(out, data, 1, 3);

        String result = out.toString(StandardCharsets.US_ASCII);
        assertEquals("TWFu", result);
    }
}
