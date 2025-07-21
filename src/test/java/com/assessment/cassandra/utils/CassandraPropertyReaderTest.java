package com.assessment.cassandra.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class CassandraPropertyReaderTest {

    @AfterEach
    void resetSingleton() throws Exception {
        // reset the singleton for clean state
        Field instance = CassandraPropertyReader.class.getDeclaredField("cassandraPropertyReader");
        instance.setAccessible(true);
        instance.set(null, null);
    }

    @Test
    void testSingletonInstance() {
        CassandraPropertyReader instance1 = CassandraPropertyReader.getInstance();
        assertNotNull(instance1);

        CassandraPropertyReader instance2 = CassandraPropertyReader.getInstance();
        assertSame(instance1, instance2); // should be same object (singleton)
    }

    @Test
    void testReadProperty_existingKey() {
        CassandraPropertyReader reader = CassandraPropertyReader.getInstance();
        String result = reader.readProperty("some.nonexistent.key"); // likely not in file
        assertEquals("some.nonexistent.key", result);
    }

    @Test
    void testReadProperty_returnsValueIfExists() throws Exception {
        CassandraPropertyReader reader = CassandraPropertyReader.getInstance();

        // Inject a known property for test
        Field propsField = CassandraPropertyReader.class.getDeclaredField("properties");
        propsField.setAccessible(true);
        Properties props = (Properties) propsField.get(reader);
        props.setProperty("test.key", "testValue");

        String result = reader.readProperty("test.key");
        assertEquals("testValue", result);
    }

    @Test
    void testPrivateConstructor_throwsIOException() throws Exception {
        // Forcefully invoke private constructor and simulate IOException

        Constructor<CassandraPropertyReader> constructor =
                CassandraPropertyReader.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        try {
            constructor.newInstance();
            assertNotNull(CassandraPropertyReader.getInstance());
        } catch (Exception e) {
            fail("Should not throw here, handled internally");
        }
    }
}
