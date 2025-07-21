package com.assessment.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ServerPropertiesTest {

    private ServerProperties props;

    @BeforeEach
    void setUp() {
        props = new ServerProperties();
    }

    @Test
    void test_GettersAndSetters(){
        props.setNotifyServicePathAsync("notifyPath");
        assertEquals("notifyPath", props.getNotifyServicePathAsync());

        props.setSupportEmail("support@email.com");
        assertEquals("support@email.com", props.getSupportEmail());

        props.setPublicAssessmentCertificateTemplate("template");
        assertEquals("template", props.getPublicAssessmentCertificateTemplate());

        props.setKafkaTopicCertificateAssessmentGroup("group");
        assertEquals("group", props.getKafkaTopicCertificateAssessmentGroup());

        props.setKafkaTopicCertificateRequest("request");
        assertEquals("request", props.getKafkaTopicCertificateRequest());

        props.setPublicAssessmentCompletionTemplate("completion");
        assertEquals("completion", props.getPublicAssessmentCompletionTemplate());

        props.setPublicAccessUrl("url");
        assertEquals("url", props.getPublicAccessUrl());

        props.setSpringKafkaPublicAssessmentNotificationTopicName("topicName");
        assertEquals("topicName", props.getSpringKafkaPublicAssessmentNotificationTopicName());

        props.setPublicAssessmentCertGenerationPostProcessTopic("postTopic");
        assertEquals("postTopic", props.getPublicAssessmentCertGenerationPostProcessTopic());

        props.setCloudStorageUrl("cloudUrl");
        assertEquals("cloudUrl", props.getCloudStorageUrl());

        props.setUserAssessmentSubmissionDuration("dur");
        assertEquals("dur", props.getUserAssessmentSubmissionDuration());

        props.setRedisQuestionsReadTimeOut(100);
        assertEquals(100, props.getRedisQuestionsReadTimeOut());

    }

    @Test
    void testGettersAndSetters() {
        props.setAssessmentHost("host");
        assertEquals("host", props.getAssessmentHost());

        props.setAssessmentHierarchyReadPath("path");
        assertEquals("path", props.getAssessmentHierarchyReadPath());

        props.setSbApiKey("key");
        assertEquals("key", props.getSbApiKey());

        props.setAssessmentQuestionListPath("qpath");
        assertEquals("qpath", props.getAssessmentQuestionListPath());

        props.setqListFromCacheEnabled(true);
        assertTrue(props.qListFromCacheEnabled());

        props.setAssessmentHierarchyNameSpace("ns");
        assertEquals("ns", props.getAssessmentHierarchyNameSpace());

        props.setAssessmentHierarchyTable("table");
        assertEquals("table", props.getAssessmentHierarchyTable());

        props.setAssessmentMinQuestionParams("minParams");
        assertEquals("minParams", props.getAssessmentMinQuestionParams());

        props.setAssessmentUserSubmitDataTable("submitTable");
        assertEquals("submitTable", props.getAssessmentUserSubmitDataTable());

        props.setRedisDataHostName("rhost");
        assertEquals("rhost", props.getRedisDataHostName());

        props.setRedisDataPort("6379");
        assertEquals("6379", props.getRedisDataPort());

        props.setRedisTimeout("timeout");
        assertEquals("timeout", props.getRedisTimeout());

        props.setRedisWheeboxKey("wkey");
        assertEquals("wkey", props.getRedisWheeboxKey());

        props.setRedisHostName("rh");
        assertEquals("rh", props.getRedisHostName());

        props.setRedisPort("rp");
        assertEquals("rp", props.getRedisPort());

        props.setEncryptionKey("ekey");
        assertEquals("ekey", props.getEncryptionKey());

        props.setSvgTemplate("svg");
        assertEquals("svg", props.getSvgTemplate());

        props.setContentHierarchyNamespace("chns");
        assertEquals("chns", props.getContentHierarchyNamespace());

        props.setContentHierarchyTable("cht");
        assertEquals("cht", props.getContentHierarchyTable());

        props.setKafkaTopicsPublicAssessmentCertificate("ktpac");
        assertEquals("ktpac", props.getKafkaTopicsPublicAssessmentCertificate());

        props.setPublicUserAssessmentData("puad");
        assertEquals("puad", props.getPublicUserAssessmentData());

        props.setNotifyServiceHost("notifyHost");
        assertEquals("notifyHost", props.getNotifyServiceHost());

        }

    @Test
    void testListProperties() {
        props.setAssessmentLevelParams("param1,param2,param3");
        List<String> levelParams = props.getAssessmentLevelParams();
        assertEquals(3, levelParams.size());
        assertEquals("param1", levelParams.get(0));

        props.setAssessmentSectionParams("s1,s2");
        List<String> sectionParams = props.getAssessmentSectionParams();
        assertEquals(2, sectionParams.size());
        assertEquals("s1", sectionParams.get(0));

        props.setAssessmentQuestionParams("q1,q2,q3,q4");
        List<String> questionParams = props.getAssessmentQuestionParams();
        assertEquals(4, questionParams.size());
        assertEquals("q1", questionParams.get(0));

        props.setAssessmentPrimaryKeyNotAllowedCertificate("pk1,pk2");
        List<String> pkList = props.getAssessmentPrimaryKeyNotAllowedCertificate();
        assertEquals(2, pkList.size());
        assertEquals("pk1", pkList.get(0));
    }
}
