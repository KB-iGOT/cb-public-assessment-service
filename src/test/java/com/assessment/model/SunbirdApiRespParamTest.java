package com.assessment.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SunbirdApiRespParamTest {

    @Test
    void testGettersAndSetters() {
        SunbirdApiRespParam param = new SunbirdApiRespParam();

        String resmsgid = "resmsgid";
        String msgid = "msgid";
        String err = "error";
        String status = "success";
        String errmsg = "error message";

        param.setResmsgid(resmsgid);
        param.setMsgid(msgid);
        param.setErr(err);
        param.setStatus(status);
        param.setErrmsg(errmsg);

        assertEquals(resmsgid, param.getResmsgid());
        assertEquals(msgid, param.getMsgid());
        assertEquals(err, param.getErr());
        assertEquals(status, param.getStatus());
        assertEquals(errmsg, param.getErrmsg());
    }

    @Test
    void testParameterizedConstructor() {
        String id = "testId";
        SunbirdApiRespParam param = new SunbirdApiRespParam(id);

        assertEquals(id, param.getResmsgid());
        assertEquals(id, param.getMsgid());
    }

    @Test
    void testNoArgsConstructor() {
        SunbirdApiRespParam param = new SunbirdApiRespParam();
        assertNotNull(param);
    }
}
