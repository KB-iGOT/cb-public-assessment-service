package com.assessment.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigTest {

    @Test
    void testGettersAndSetters() {
        Config config = new Config();

        String sender = "test-sender";
        Object topic = "test-topic";
        Object otp = 123456;
        String subject = "test-subject";

        config.setSender(sender);
        config.setTopic(topic);
        config.setOtp(otp);
        config.setSubject(subject);

        assertEquals(sender, config.getSender());
        assertEquals(topic, config.getTopic());
        assertEquals(otp, config.getOtp());
        assertEquals(subject, config.getSubject());
    }
}
