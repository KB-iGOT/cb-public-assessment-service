package com.assessment.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class ServerProperties {


	@Value("${assessment.host}")
	private String assessmentHost;

	@Value("${assessment.hierarchy.read.path}")
	private String assessmentHierarchyReadPath;

	@Value("${sb.api.key}")
	private String sbApiKey;

	@Value("${assessment.question.list.path}")
	private String assessmentQuestionListPath;

	@Value("${read.assess.questions.from.redis}")
	private boolean qListFromCacheEnabled;

	@Value("${assessment.hierarchy.namespace}")
	private String assessmentHierarchyNameSpace;

	@Value("${assessment.hierarchy.table}")
	private String assessmentHierarchyTable;

	@Value("${assessment.read.assessmentLevel.params}")
	private String assessmentLevelParams;

	@Value("${assessment.read.sectionLevel.params}")
	private String assessmentSectionParams;

	@Value("${assessment.read.questionLevel.params}")
	private String assessmentQuestionParams;

	@Value("${assessment.user.submit.data.table}")
	private String assessmentUserSubmitDataTable;

	@Value("${assessment.read.min.question.params}")
	private String assessmentMinQuestionParams;

	@Value("${user.assessment.submission.duration}")
	private String userAssessmentSubmissionDuration;

	@Value("${redis.questions.read.timeout}")
	private Integer redisQuestionsReadTimeOut;

	@Value("${redis.data.host.name}")
	private String redisDataHostName;

	@Value("${redis.data.port}")
	private String redisDataPort;

	@Value("${redis.timeout}")
	private String redisTimeout;

	@Value("${redis.wheebox.key}")
	private String redisWheeboxKey;

	@Value("${redis.host.name}")
	private String redisHostName;

	@Value("${redis.port}")
	private String redisPort;

	@Value("${encryption.key}")
	private String encryptionKey;

	@Value("${svgTemplate}")
	private String svgTemplate;

	@Value("${content.hierarchy.namespace}")
	private String contentHierarchyNamespace;

	@Value("${content.hierarchy.table}")
	private String contentHierarchyTable;

	@Value("${kafka.topics.public.assessment.certificate}")
	private String kafkaTopicsPublicAssessmentCertificate;

	@Value("${public.user.assessment.data}")
	private String publicUserAssessmentData;

	@Value("${assessment.primarykey.notallowed.certificate}")
	private String assessmentPrimaryKeyNotAllowedCertificate;

	@Value("${notify.service.host}")
	private String notifyServiceHost;

	@Value("${notify.service.path.async}")
	private String notifyServicePathAsync;

	@Value("${notification.support.mail}")
	private String supportEmail;

	@Value("${public.assessment.certificate.template}")
	private String publicAssessmentCertificateTemplate;

	@Value("${kafka.topic.certificate.assessment.group}")
	private String kafkaTopicCertificateAssessmentGroup;

	@Value("${kafka.topic.certificate.request}")
	private String kafkaTopicCertificateRequest;

	@Value("${public.assessment.completion.template}")
	private String publicAssessmentCompletionTemplate;

	@Value("${public.access.url}")
	private String publicAccessUrl;

	@Value("${spring.kafka.public.assessment.notification.topic.name}")
	private String springKafkaPublicAssessmentNotificationTopicName;

	@Value("${public.assessment.cert.generation.post.process.topic}")
	private String publicAssessmentCertGenerationPostProcessTopic;
	@Value("${storage.cloud.url}")
	private String cloudStorageUrl;

	public String getAssessmentQuestionListPath() {
		return assessmentQuestionListPath;
	}

	public void setAssessmentQuestionListPath(String assessmentQuestionListPath) {
		this.assessmentQuestionListPath = assessmentQuestionListPath;
	}

	public Integer getRedisQuestionsReadTimeOut() {
		return redisQuestionsReadTimeOut;
	}

	public void setRedisQuestionsReadTimeOut(Integer redisQuestionsReadTimeOut) {
		this.redisQuestionsReadTimeOut = redisQuestionsReadTimeOut;
	}

	public String getRedisDataHostName() {
		return redisDataHostName;
	}

	public void setRedisDataHostName(String redisDataHostName) {
		this.redisDataHostName = redisDataHostName;
	}

	public String getRedisDataPort() {
		return redisDataPort;
	}

	public void setRedisDataPort(String redisDataPort) {
		this.redisDataPort = redisDataPort;
	}

	public String getRedisTimeout() {
		return redisTimeout;
	}

	public void setRedisTimeout(String redisTimeout) {
		this.redisTimeout = redisTimeout;
	}

	public String getUserAssessmentSubmissionDuration() {
		return userAssessmentSubmissionDuration;
	}

	public void setUserAssessmentSubmissionDuration(String userAssessmentSubmissionDuration) {
		this.userAssessmentSubmissionDuration = userAssessmentSubmissionDuration;
	}

	public String getAssessmentHost() {
		return assessmentHost;
	}

	public void setAssessmentHost(String assessmentHost) {
		this.assessmentHost = assessmentHost;
	}

	public String getAssessmentHierarchyReadPath() {
		return assessmentHierarchyReadPath;
	}

	public void setAssessmentHierarchyReadPath(String assessmentHierarchyReadPath) {
		this.assessmentHierarchyReadPath = assessmentHierarchyReadPath;
	}

	public String getSbApiKey() {
		return sbApiKey;
	}

	public void setSbApiKey(String sbApiKey) {
		this.sbApiKey = sbApiKey;
	}

	public boolean qListFromCacheEnabled() {
		return qListFromCacheEnabled;
	}

	public void setqListFromCacheEnabled(boolean qListFromCacheEnabled) {
		this.qListFromCacheEnabled = qListFromCacheEnabled;
	}

	public String getAssessmentHierarchyNameSpace() {
		return assessmentHierarchyNameSpace;
	}

	public void setAssessmentHierarchyNameSpace(String assessmentHierarchyNameSpace) {
		this.assessmentHierarchyNameSpace = assessmentHierarchyNameSpace;
	}

	public String getAssessmentHierarchyTable() {
		return assessmentHierarchyTable;
	}

	public void setAssessmentHierarchyTable(String assessmentHierarchyTable) {
		this.assessmentHierarchyTable = assessmentHierarchyTable;
	}

	public List<String> getAssessmentLevelParams() {
		return Arrays.asList(assessmentLevelParams.split(",", -1));
	}

	public void setAssessmentLevelParams(String assessmentLevelParams) {
		this.assessmentLevelParams = assessmentLevelParams;
	}

	public List<String> getAssessmentSectionParams() {
		return Arrays.asList(assessmentSectionParams.split(",", -1));
	}

	public void setAssessmentSectionParams(String assessmentSectionParams) {
		this.assessmentSectionParams = assessmentSectionParams;
	}

	public List<String> getAssessmentQuestionParams() {
		return Arrays.asList(assessmentQuestionParams.split(",", -1));
	}

	public void setAssessmentQuestionParams(String assessmentQuestionParams) {
		this.assessmentQuestionParams = assessmentQuestionParams;
	}

	public String getAssessmentMinQuestionParams() {
		return assessmentMinQuestionParams;
	}

	public void setAssessmentMinQuestionParams(String assessmentMinQuestionParams) {
		this.assessmentMinQuestionParams = assessmentMinQuestionParams;
	}

	public String getAssessmentUserSubmitDataTable() {
		return assessmentUserSubmitDataTable;
	}

	public void setAssessmentUserSubmitDataTable(String assessmentUserSubmitDataTable) {
		this.assessmentUserSubmitDataTable = assessmentUserSubmitDataTable;
	}

	public String getRedisWheeboxKey() {
		return redisWheeboxKey;
	}

	public void setRedisWheeboxKey(String redisWheeboxKey) {
		this.redisWheeboxKey = redisWheeboxKey;
	}

	public String getRedisHostName() {
		return redisHostName;
	}

	public void setRedisHostName(String redisHostName) {
		this.redisHostName = redisHostName;
	}

	public String getRedisPort() {
		return redisPort;
	}

	public void setRedisPort(String redisPort) {
		this.redisPort = redisPort;
	}

	public String getEncryptionKey() {
		return encryptionKey;
	}

	public void setEncryptionKey(String encryptionKey) {
		this.encryptionKey = encryptionKey;
	}

	public String getSvgTemplate() {
		return svgTemplate;
	}

	public void setSvgTemplate(String svgTemplate) {
		this.svgTemplate = svgTemplate;
	}

	public String getContentHierarchyNamespace() {
		return contentHierarchyNamespace;
	}

	public void setContentHierarchyNamespace(String contentHierarchyNamespace) {
		this.contentHierarchyNamespace = contentHierarchyNamespace;
	}

	public String getContentHierarchyTable() {
		return contentHierarchyTable;
	}

	public void setContentHierarchyTable(String contentHierarchyTable) {
		this.contentHierarchyTable = contentHierarchyTable;
	}

	public String getKafkaTopicsPublicAssessmentCertificate() {
		return kafkaTopicsPublicAssessmentCertificate;
	}

	public void setKafkaTopicsPublicAssessmentCertificate(String kafkaTopicsPublicAssessmentCertificate) {
		this.kafkaTopicsPublicAssessmentCertificate = kafkaTopicsPublicAssessmentCertificate;
	}

	public String getPublicUserAssessmentData() {
		return publicUserAssessmentData;
	}

	public void setPublicUserAssessmentData(String publicUserAssessmentData) {
		this.publicUserAssessmentData = publicUserAssessmentData;
	}

	public List<String> getAssessmentPrimaryKeyNotAllowedCertificate() {
		return Arrays.asList(assessmentPrimaryKeyNotAllowedCertificate.split(",", -1));
	}

	public void setAssessmentPrimaryKeyNotAllowedCertificate(String assessmentPrimaryKeyNotAllowedCertificate) {
		this.assessmentPrimaryKeyNotAllowedCertificate = assessmentPrimaryKeyNotAllowedCertificate;
	}

	public String getNotifyServiceHost() { return notifyServiceHost; }

	public void setNotifyServiceHost(String notifyServiceHost) { this.notifyServiceHost = notifyServiceHost; }

	public String getNotifyServicePathAsync() { return notifyServicePathAsync; }

	public void setNotifyServicePathAsync(String notifyServicePathAsync) { this.notifyServicePathAsync = notifyServicePathAsync;}

	public String getSupportEmail() { return supportEmail; }

	public void setSupportEmail(String supportEmail) { this.supportEmail = supportEmail; }

	public String getPublicAssessmentCertificateTemplate() { return publicAssessmentCertificateTemplate; }

	public void setPublicAssessmentCertificateTemplate(String publicAssessmentCertificateTemplate) {
		this.publicAssessmentCertificateTemplate = publicAssessmentCertificateTemplate;
	}

	public String getKafkaTopicCertificateAssessmentGroup() {
		return kafkaTopicCertificateAssessmentGroup;
	}

	public void setKafkaTopicCertificateAssessmentGroup(String kafkaTopicCertificateAssessmentGroup) {
		this.kafkaTopicCertificateAssessmentGroup = kafkaTopicCertificateAssessmentGroup;
	}

	public String getKafkaTopicCertificateRequest() {
		return kafkaTopicCertificateRequest;
	}

	public void setKafkaTopicCertificateRequest(String kafkaTopicCertificateRequest) {
		this.kafkaTopicCertificateRequest = kafkaTopicCertificateRequest;
	}

	public String getPublicAssessmentCompletionTemplate() {
		return publicAssessmentCompletionTemplate;
	}

	public void setPublicAssessmentCompletionTemplate(String publicAssessmentCompletionTemplate) {
		this.publicAssessmentCompletionTemplate = publicAssessmentCompletionTemplate;
	}

	public String getPublicAccessUrl() {
		return publicAccessUrl;
	}

	public void setPublicAccessUrl(String publicAccessUrl) {
		this.publicAccessUrl = publicAccessUrl;
	}


	public String getSpringKafkaPublicAssessmentNotificationTopicName() {
		return springKafkaPublicAssessmentNotificationTopicName;
	}

	public void setSpringKafkaPublicAssessmentNotificationTopicName(String springKafkaPublicAssessmentNotificationTopicName) {
		this.springKafkaPublicAssessmentNotificationTopicName = springKafkaPublicAssessmentNotificationTopicName;
	}

	public String getPublicAssessmentCertGenerationPostProcessTopic() {
		return publicAssessmentCertGenerationPostProcessTopic;
	}

	public void setPublicAssessmentCertGenerationPostProcessTopic(String publicAssessmentCertGenerationPostProcessTopic) {
		this.publicAssessmentCertGenerationPostProcessTopic = publicAssessmentCertGenerationPostProcessTopic;
	}

	public String getCloudStorageUrl() {
		return cloudStorageUrl;
	}

	public void setCloudStorageUrl(String cloudStorageUrl) {
		this.cloudStorageUrl = cloudStorageUrl;
	}
}