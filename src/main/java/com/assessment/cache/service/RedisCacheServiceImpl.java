package com.assessment.cache.service;

import com.assessment.cache.RedisCacheMgr;
import com.assessment.model.SBApiResponse;
import com.assessment.util.AssessmentServiceLogger;
import com.assessment.util.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class RedisCacheServiceImpl implements RedisCacheService {

	@Autowired
	RedisCacheMgr redisCache;

	private AssessmentServiceLogger logger = new AssessmentServiceLogger(getClass().getName());

	@Override
	public SBApiResponse deleteCache() throws Exception {
		SBApiResponse response = new SBApiResponse(Constants.API_REDIS_DELETE);
		boolean res = redisCache.deleteAllCBExtKey();
		if (res) {
			response.getParams().setStatus(Constants.SUCCESSFUL);
			response.setResponseCode(HttpStatus.OK);
		} else {
			String errMsg = "No Keys found, Redis cache is empty";
			logger.info(errMsg);
			response.getParams().setErrmsg(errMsg);
			response.setResponseCode(HttpStatus.NOT_FOUND);
		}
		return response;
	}

	@Override
	public SBApiResponse getKeys() throws Exception {
		SBApiResponse response = new SBApiResponse(Constants.API_REDIS_GET_KEYS);
		Set<String> res = redisCache.getAllKeyNames();
		if (!res.isEmpty()) {
			response.getParams().setStatus(Constants.SUCCESSFUL);
			response.put(Constants.RESPONSE, res);
			response.setResponseCode(HttpStatus.OK);

		} else {
			String errMsg = "No Keys found, Redis cache is empty";
			logger.info(errMsg);
			response.getParams().setErrmsg(errMsg);
			response.setResponseCode(HttpStatus.NOT_FOUND);
		}
		return response;
	}

	@Override
	public SBApiResponse getKeysAndValues() throws Exception {
		SBApiResponse response = new SBApiResponse(Constants.API_REDIS_GET_KEYS_VALUE_SET);
		List<Map<String, Object>> result = redisCache.getAllKeysAndValues();

		if (!result.isEmpty()) {
			logger.info("All Keys and Values in Redis Cache is Fetched");
			response.getParams().setStatus(Constants.SUCCESSFUL);
			response.put(Constants.RESPONSE, result);
			response.setResponseCode(HttpStatus.OK);
		} else {
			String errMsg = "No Keys found, Redis cache is empty";
			logger.info(errMsg);
			response.getParams().setErrmsg(errMsg);
			response.setResponseCode(HttpStatus.NOT_FOUND);
		}
		return response;
	}
}
