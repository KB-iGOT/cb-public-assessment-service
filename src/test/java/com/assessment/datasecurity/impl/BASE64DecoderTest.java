package com.assessment.datasecurity.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.*;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class BASE64DecoderTest {

    BASE64Decoder decoder = new BASE64Decoder();

    @Test
    void testBytesPerAtomAndLine() {
        assertEquals(4, decoder.bytesPerAtom());
        assertEquals(72, decoder.bytesPerLine());
    }


    @ParameterizedTest(name = "decodeAtom: base64=\"{0}\" -> \"{1}\"")
    @CsvSource({
            "'TQ==', M",
            "'TWE=', Ma",
            "'TWFu', Man",
            "'\n\rTWFu', Man"
    })
    void testDecodeAtom(String base64, String expected) throws Exception {
        PushbackInputStream in = new PushbackInputStream(
                new ByteArrayInputStream(base64.getBytes(StandardCharsets.US_ASCII))
        );
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        decoder.decodeAtom(in, out, 4);

        byte[] result = out.toByteArray();
        assertEquals(expected, new String(result, StandardCharsets.US_ASCII));
    }

    @Test
    void testDecodeAtom_notEnoughBytes() {
        PushbackInputStream in = new PushbackInputStream(new ByteArrayInputStream("".getBytes(StandardCharsets.US_ASCII)));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        IOException exception = assertThrows(IOException.class, () -> {
            decoder.decodeAtom(in, out, 1);
        });
        assertEquals("BASE64Decoder: Not enough bytes for an atom.", exception.getMessage());
    }

    @Test
    void testDecodeAtom_inputEndsPrematurely() {
        PushbackInputStream in = new PushbackInputStream(new ByteArrayInputStream("".getBytes(StandardCharsets.US_ASCII)));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        IOException exception = assertThrows(IOException.class, () -> {
            decoder.decodeAtom(in, out, 4);
        });
        assertNull(exception.getMessage());
    }
}
