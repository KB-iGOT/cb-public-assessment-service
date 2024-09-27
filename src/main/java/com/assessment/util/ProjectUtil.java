package com.assessment.util;

import com.assessment.model.SBApiResponse;
import com.assessment.model.SunbirdApiRespParam;
import com.assessment.util.exceptions.ProjectCommonException;
import com.assessment.util.exceptions.ResponseCode;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.springframework.http.HttpStatus;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class will contains all the common utility methods.
 *
 * @author Manzarul
 */
public class ProjectUtil {

    public static AssessmentServiceLogger logger = new AssessmentServiceLogger(ProjectUtil.class.getName());

    /**
     * This method will check incoming value is null or empty it will do empty check
     * by doing trim method. in case of null or empty it will return true else
     * false.
     *
     * @param value
     * @return
     */
    public static boolean isStringNullOREmpty(String value) {
        return (value == null || "".equals(value.trim()));
    }

    /**
     * This method will create and return server exception to caller.
     *
     * @param responseCode ResponseCode
     * @return ProjectCommonException
     */
    public static ProjectCommonException createServerError(ResponseCode responseCode) {
        return new ProjectCommonException(responseCode.getErrorCode(), responseCode.getErrorMessage(),
                ResponseCode.SERVER_ERROR.getResponseCode());
    }

    public static ProjectCommonException createClientException(ResponseCode responseCode) {
        return new ProjectCommonException(responseCode.getErrorCode(), responseCode.getErrorMessage(),
                ResponseCode.CLIENT_ERROR.getResponseCode());
    }

    public static SBApiResponse createDefaultResponse(String api) {
        SBApiResponse response = new SBApiResponse();
        response.setId(api);
        response.setVer(Constants.API_VERSION_1);
        response.setParams(new SunbirdApiRespParam(UUID.randomUUID().toString()));
        response.getParams().setStatus(Constants.SUCCESS);
        response.setResponseCode(HttpStatus.OK);
        response.setTs(DateTime.now().toString());
        return response;
    }

    public static Map<String, String> getDefaultHeaders() {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON);
        return headers;
    }

    public enum Method {
        GET, POST, PUT, DELETE, PATCH
    }

    public static String convertSecondsToHrsAndMinutes(int seconds) {
        String time = "";
        if (seconds > 60) {
            int min = (seconds / 60) % 60;
            int hours = (seconds / 60) / 60;
            String minutes = (min < 10) ? "0" + min : Integer.toString(min);
            String strHours = (hours < 10) ? "0" + hours : Integer.toString(hours);
            if (min > 0 && hours > 0)
                time = strHours + "h " + minutes + "m";
            else if (min == 0 && hours > 0)
                time = strHours + "h";
            else if (min > 0) {
                time = minutes + "m";
            }
        }
        return time;
    }

    public static String firstLetterCapitalWithSingleSpace(final String words) {
        return Stream.of(words.trim().split("\\s")).filter(word -> word.length() > 0)
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1)).collect(Collectors.joining(" "));
    }

    /**
     * Check the email id is valid or not
     *
     * @param email String
     * @return Boolean
     */

    public static Boolean validateEmailPattern(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\." + "[a-zA-Z0-9_+&*-]+)*@" + "(?:[a-zA-Z0-9-]+\\.)+[a-z"
                + "A-Z]{2,7}$";
        Pattern pat = Pattern.compile(emailRegex);
        if (pat.matcher(email).matches()) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    public static Boolean validateFullName(String firstName) {
        return firstName.matches("^(?!.*\\n)[a-zA-Z]+(?:['\\s][a-zA-Z]+)*(?<!\\.|\\s)$");
    }


    public static void updateErrorDetails(SBApiResponse response, String errMsg, HttpStatus responseCode) {
        response.getParams().setStatus(Constants.FAILED);
        response.getParams().setErrmsg(errMsg);
        response.setResponseCode(responseCode);

    }

}