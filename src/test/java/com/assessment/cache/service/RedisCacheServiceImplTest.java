package com.assessment.cache.service;

import com.assessment.cache.RedisCacheMgr;
import com.assessment.model.SBApiResponse;
import com.assessment.util.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class RedisCacheServiceImplTest {

    @Mock
    private RedisCacheMgr redisCache;

    @InjectMocks
    private RedisCacheServiceImpl redisCacheService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testDeleteCache_success() throws Exception {
        when(redisCache.deleteAllCBExtKey()).thenReturn(true);

        SBApiResponse response = redisCacheService.deleteCache();

        assertThat(response.getParams().getStatus()).isEqualTo(Constants.SUCCESSFUL);
        assertThat(response.getResponseCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void testDeleteCache_empty() throws Exception {
        when(redisCache.deleteAllCBExtKey()).thenReturn(false);

        SBApiResponse response = redisCacheService.deleteCache();

        assertThat(response.getParams().getErrmsg()).contains("No Keys found");
        assertThat(response.getResponseCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void testGetKeys_found() throws Exception {
        Set<String> keys = new HashSet<>(Arrays.asList("key1", "key2"));
        when(redisCache.getAllKeyNames()).thenReturn(keys);

        SBApiResponse response = redisCacheService.getKeys();

        assertThat(response.getParams().getStatus()).isEqualTo(Constants.SUCCESSFUL);
        assertThat(response.getResponseCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void testGetKeys_empty() throws Exception {
        when(redisCache.getAllKeyNames()).thenReturn(Collections.emptySet());

        SBApiResponse response = redisCacheService.getKeys();

        assertThat(response.getParams().getErrmsg()).contains("No Keys found");
        assertThat(response.getResponseCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void testGetKeysAndValues_found() throws Exception {
        List<Map<String, Object>> kvList = Arrays.asList(
                Map.of("key", "k1", "value", "v1"),
                Map.of("key", "k2", "value", "v2")
        );
        when(redisCache.getAllKeysAndValues()).thenReturn(kvList);

        SBApiResponse response = redisCacheService.getKeysAndValues();

        assertThat(response.getParams().getStatus()).isEqualTo(Constants.SUCCESSFUL);
        assertThat(response.getResponseCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void testGetKeysAndValues_empty() throws Exception {
        when(redisCache.getAllKeysAndValues()).thenReturn(Collections.emptyList());

        SBApiResponse response = redisCacheService.getKeysAndValues();

        assertThat(response.getParams().getErrmsg()).contains("No Keys found");
        assertThat(response.getResponseCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
