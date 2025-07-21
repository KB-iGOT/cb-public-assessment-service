package com.assessment.datasecurity.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

class CharacterEncoderTest {

    private DummyEncoder encoder;

    @BeforeEach
    void setUp() {
        encoder = new DummyEncoder();
    }

    @Test
    void testEncodeInputStreamOutputStream() throws IOException {
        byte[] input = {1, 2, 3, 4};
        ByteArrayInputStream in = new ByteArrayInputStream(input);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        encoder.encode(in, out);

        assertArrayEquals(input, out.toByteArray());
    }

    @Test
    void testEncodeByteArrayOutputStream() throws IOException {
        byte[] input = {1, 2, 3, 4};
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        encoder.encode(input, out);

        assertArrayEquals(input, out.toByteArray());
    }

    @Test
    void testEncodeByteArrayToString() {
        byte[] input = {1, 2, 3, 4};

        String result = encoder.encode(input);

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void testEncodeByteBufferOutputStream() throws IOException {
        byte[] input = {1, 2, 3, 4};
        ByteBuffer buffer = ByteBuffer.wrap(input);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        encoder.encode(buffer, out);

        assertArrayEquals(input, out.toByteArray());
    }

    @Test
    void testEncodeByteBufferToString() {
        byte[] input = {1, 2, 3, 4};
        ByteBuffer buffer = ByteBuffer.wrap(input);

        String result = encoder.encode(buffer);

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void testEncodeBufferInputStreamOutputStream() throws IOException {
        byte[] input = {1, 2, 3, 4};
        ByteArrayInputStream in = new ByteArrayInputStream(input);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        encoder.encodeBuffer(in, out);

        assertArrayEquals(input, out.toByteArray());
    }

    @Test
    void testEncodeBufferByteArrayOutputStream() throws IOException {
        byte[] input = {1, 2, 3, 4};
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        encoder.encodeBuffer(input, out);

        assertArrayEquals(input, out.toByteArray());
    }

    @Test
    void testEncodeBufferByteArrayToString() {
        byte[] input = {1, 2, 3, 4};

        String result = encoder.encodeBuffer(input);

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void testEncodeBufferByteBufferOutputStream() throws IOException {
        byte[] input = {1, 2, 3, 4};
        ByteBuffer buffer = ByteBuffer.wrap(input);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        encoder.encodeBuffer(buffer, out);

        assertArrayEquals(input, out.toByteArray());
    }

    @Test
    void testEncodeBufferByteBufferToString() {
        byte[] input = {1, 2, 3, 4};
        ByteBuffer buffer = ByteBuffer.wrap(input);

        String result = encoder.encodeBuffer(buffer);

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    /**
     * DummyEncoder disables line suffixes and buffer suffixes to avoid newlines in the output.
     */
    static class DummyEncoder extends CharacterEncoder {

        @Override
        protected int bytesPerAtom() {
            return 2;
        }

        @Override
        protected int bytesPerLine() {
            return 4;
        }

        @Override
        protected void encodeAtom(OutputStream out, byte[] buf, int off, int len) throws IOException {
            out.write(buf, off, len);
        }

        @Override
        protected void encodeLineSuffix(OutputStream aStream) {
            // No-op to suppress adding '\n'
        }

        @Override
        protected void encodeBufferSuffix(OutputStream aStream) {
            // No-op
        }
    }
}
