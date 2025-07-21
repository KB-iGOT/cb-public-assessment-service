package com.assessment.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CassandraConfigTest {

    private TestCassandraConfig config;

    @BeforeEach
    void setUp() {
        config = new TestCassandraConfig();
        config.setContactPoints("127.0.0.1");
        config.setPort(9042);
        config.setKeyspaceName("test_keyspace");
    }

    @Test
    void testContactPoints() {
        assertThat(config.getContactPoints()).isEqualTo("127.0.0.1");
    }

    @Test
    void testPort() {
        assertThat(config.getPort()).isEqualTo(9042);
    }

    @Test
    void testKeyspaceName() {
        assertThat(config.getKeyspaceName()).isEqualTo("test_keyspace");
    }

    @Test
    void testMetricsDisabled() {
        assertThat(config.getMetricsEnabled()).isFalse();
    }

    /**
     * Dummy concrete implementation for testing purposes.
     */
    static class TestCassandraConfig extends CassandraConfig {
        // nothing extra — just satisfies the abstract class
    }
}
