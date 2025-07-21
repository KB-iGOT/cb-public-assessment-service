package com.assessment.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NotificationAsyncRequestTest {

    @Test
    void testGettersAndSetters() {
        NotificationAsyncRequest request = new NotificationAsyncRequest();

        String type = "EMAIL";
        int priority = 5;
        Map<String, Object> action = Map.of("key", "value");
        List<String> ids = List.of("id1", "id2");
        List<String> copyEmail = List.of("copy1@example.com", "copy2@example.com");

        request.setType(type);
        request.setPriority(priority);
        request.setAction(action);
        request.setIds(ids);
        request.setCopyEmail(copyEmail);

        assertEquals(type, request.getType());
        assertEquals(priority, request.getPriority());
        assertEquals(action, request.getAction());
        assertEquals(ids, request.getIds());
        assertEquals(copyEmail, request.getCopyEmail());
    }
}
