package com.assessment.cassandra.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.AfterEach;

@ExtendWith(MockitoExtension.class)
class CassandraConnectionManagerImplTest {

    private AutoCloseable mocks;

    @BeforeEach
    void setup() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mocks != null) {
            mocks.close();
        }
    }

    @Test
    void testShutdownHook() throws InterruptedException {
        Thread thread = new CassandraConnectionManagerImpl.ResourceCleanUp();
        thread.start();
        thread.join(2000); // Wait for the thread to finish (max 2 seconds)
        assertFalse(thread.isAlive(), "Shutdown hook thread should have finished execution");
    }
}
