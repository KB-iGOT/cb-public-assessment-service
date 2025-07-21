package com.assessment.datasecurity.impl;

import org.junit.jupiter.api.Test;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

class CharacterDecoderTest {

    static class DummyDecoder extends CharacterDecoder {
        @Override
        protected int bytesPerAtom() {
            return 2;
        }

        @Override
        protected int bytesPerLine() {
            return 4;
        }

        @Override
        protected void decodeAtom(PushbackInputStream aStream, OutputStream bStream, int l) throws IOException {
            byte[] buf = new byte[l];
            int read = readFully(aStream, buf, 0, l);
            if (read != -1) {
                bStream.write(buf, 0, read);
            }
        }
    }

    CharacterDecoder decoder = new DummyDecoder();

    @Test
    void testReadFully() throws IOException {
        byte[] input = {1, 2, 3, 4};
        ByteArrayInputStream in = new ByteArrayInputStream(input);
        byte[] buffer = new byte[4];
        int read = decoder.readFully(in, buffer, 0, 4);

        assertEquals(4, read);
        assertArrayEquals(input, buffer);
    }

    @Test
    void testDefaultNoOps() throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(new byte[0]);
        PushbackInputStream ps = new PushbackInputStream(in);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // just call them — they do nothing and should not throw
        decoder.decodeBufferPrefix(ps, out);
        decoder.decodeBufferSuffix(ps, out);
        decoder.decodeLineSuffix(ps, out);

        int lineLength = decoder.decodeLinePrefix(ps, out);
        assertEquals(4, lineLength); // our DummyDecoder bytesPerLine
    }

    @Test
    void testDecodeAtomThrowsIfNotOverridden() {
        CharacterDecoder baseDecoder = new CharacterDecoder() {
            @Override
            protected int bytesPerAtom() {
                return 1;
            }

            @Override
            protected int bytesPerLine() {
                return 1;
            }
        };
        PushbackInputStream ps = new PushbackInputStream(new ByteArrayInputStream(new byte[0]));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        IOException ex = assertThrows(IOException.class, () -> {
            baseDecoder.decodeAtom(ps, out, 1);
        });

        assertNotNull(ex);
    }
}
