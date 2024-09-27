package com.assessment.cache.service;


import com.assessment.model.SBApiResponse;

public interface RedisCacheService {

    public SBApiResponse deleteCache() throws Exception;

    public SBApiResponse getKeys() throws Exception;

    public SBApiResponse getKeysAndValues() throws Exception;

}
