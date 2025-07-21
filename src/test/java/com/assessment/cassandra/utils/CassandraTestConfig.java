package com.assessment.cassandra.utils;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class CassandraTestConfig {

    @Bean
    public Cluster mockCluster() {
        Cluster cluster = Mockito.mock(Cluster.class);
        Session session = Mockito.mock(Session.class);

        // When cluster.connect() is called, return mock session
        Mockito.when(cluster.connect()).thenReturn(session);

        return cluster;
    }

    @Bean
    public CassandraConnectionManagerImpl cassandraConnectionManager(Cluster mockCluster) {
        return new CassandraConnectionManagerImpl();
    }
}
