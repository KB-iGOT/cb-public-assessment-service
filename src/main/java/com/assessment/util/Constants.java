package com.assessment.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Constants {
	public static final String INVALID_USER_FULL_NAME = "Invalid user fullName";
	public static final String INVALID_EMAIL ="Invalid email";
	public static final String EMAIL = "email";
	public static final String USER_ID = "userId";
	public static final String IDENTIFIER = "identifier";
	public static final String RESPONSE_CODE = "responseCode";
	public static final String SUCCESSFUL = "Successful";
	public static final String FAILED = "Failed";
	public static final String STATUS = "status";
	public static final String PASS_STATUS = "pass_status";
	public static final String RESULT = "result";
	public static final String OK = "OK";
	public static final String FETCH_RESULT_CONSTANT = ".fetchResult:";
	public static final String URI_CONSTANT = "URI: ";
	public static final String REQUEST_CONSTANT = "Request: ";
	public static final String RESPONSE_CONSTANT = "Response: ";
	// User assessment pass mark
	public static final String RESPONSE = "response";
	public static final String NAME = "name";
	// assessment
	public static final String QUESTION_SET = "questionSet";
	// Cassandra Constants
	public static final String INSERT_INTO = "INSERT INTO ";
	public static final String DOT = ".";
	public static final String OPEN_BRACE = "(";
	public static final String VALUES_WITH_BRACE = ") VALUES (";
	public static final String QUE_MARK = "?";
	public static final String COMMA = ",";
	public static final String CLOSING_BRACE = ");";
	public static final String SUCCESS = "success";
	public static final String EXCEPTION_MSG_FETCH = "Exception occurred while fetching record from ";
	public static final String EXCEPTION_MSG_DELETE = "Exception occurred while deleting record from ";
	// Database and Tables
	public static final String KEYSPACE_SUNBIRD = "sunbird";
	public static final String VALUE = "value";
	public static final String CONTENT_TYPE = "Content-Type";
	public static final String APPLICATION_JSON = "application/json";
	public static final String UNAUTHORIZED = "unauthorized";
	// Redis
	public static final String API_REDIS_DELETE = "api.redis.delete";
	public static final String API_REDIS_GET_KEYS = "api.redis.get.keys";
	public static final String API_REDIS_GET_KEYS_VALUE_SET = "api.redis.get.keys&values";
	public static final String REDIS_COMMON_KEY = "CB_EXT_";
	public static final String AUTHORIZATION = "authorization";
	public static final String PRIMARY_CATEGORY = "primaryCategory";
	public static final String REQUEST = "request";
	// telemetry audit constants
	public static final String IDENTIFIER_REPLACER = "{identifier}";
	public static final String CHILDREN = "children";
	public static final String MAX_QUESTIONS = "maxQuestions";
	public static final String CHILD_NODES = "childNodes";
	public static final String SEARCH = "search";
	public static final String QUESTION_ID = "qs_id_";
	public static final String ASSESSMENT_ID = "assess_id_";
	public static final String EDITOR_STATE = "editorState";
	public static final String CHOICES = "choices";
	public static final String ANSWER = "answer";
	public static final String OPTIONS = "options";
	public static final String HIERARCHY = "hierarchy";
	public static final String SUNBIRD_KEY_SPACE_NAME = "sunbird";
	public static final String CORE_CONNECTIONS_PER_HOST_FOR_LOCAL = "coreConnectionsPerHostForLocal";
	public static final String CORE_CONNECTIONS_PER_HOST_FOR_REMOTE = "coreConnectionsPerHostForRemote";
	public static final String MAX_CONNECTIONS_PER_HOST_FOR_LOCAl = "maxConnectionsPerHostForLocal";
	public static final String MAX_CONNECTIONS_PER_HOST_FOR_REMOTE = "maxConnectionsPerHostForRemote";
	public static final String MAX_REQUEST_PER_CONNECTION = "maxRequestsPerConnection";
	public static final String HEARTBEAT_INTERVAL = "heartbeatIntervalSeconds";
	public static final String POOL_TIMEOUT = "poolTimeoutMillis";
	public static final String SUNBIRD_CASSANDRA_CONSISTENCY_LEVEL = "sunbird_cassandra_consistency_level";

	public static final String CASSANDRA_CONFIG_HOST = "cassandra.config.host";
	public static final String OBJECT_TYPE = "objectType";
	public static final String QUESTIONS = "questions";
	public static final String RHS_CHOICES = "rhsChoices";
	public static final String MTF_QUESTION = "MTF Question";
	public static final String FTB_QUESTION = "FTB Question";
	public static final String API_QUESTIONS_LIST = "api.questions.list";
	public static final String MINIMUM_PASS_PERCENTAGE = "minimumPassPercentage";
	public static final String TOTAL = "total";
	public static final String BLANK = "blank";
	public static final String CORRECT = "correct";
	public static final String INCORRECT = "incorrect";
	public static final String PASS = "pass";

	public static final String API_VERSION_1 = "1.0";
	public static final String BODY = "body";
	public static final String ERROR_MESSAGE = "errmsg";

	public static final String PRACTICE_QUESTION_SET = "Practice Question Set";
	public static final String EXPECTED_DURATION = "expectedDuration";
	public static final String SUBMITTED = "SUBMITTED";
	public static final String NOT_SUBMITTED = "NOT SUBMITTED";
	public static final String END_TIME = "endtime";
	public static final String ASSESSMENT_ID_KEY = "assessmentId";
	public static final String START_TIME = "starttime";
	public static final String QUESTION_TYPE = "qType";
	public static final String SELECTED_ANSWER = "selectedAnswer";
	public static final String INDEX = "index";
	public static final String MCQ_SCA = "mcq-sca";
	public static final String MCQ_MCA = "mcq-mca";
	public static final String FTB = "ftb";
	public static final String MTF = "mtf";
	public static final String IS_CORRECT = "isCorrect";
	public static final String OPTION_ID = "optionId";

	public static final String TABLE_PUBLIC_USER_ASSESSMENT_DATA = "public_user_assessment_data";

	public static final String ASSESSMENT_DATA_START_TIME_NOT_UPDATED = "Assessment Data & Start Time not updated in the DB.";
	public static final String ASSESSMENT_HIERARCHY_READ_FAILED = "Assessment hierarchy read failed, failed to process request";
	public static final String ASSESSMENT_ID_KEY_IS_NOT_PRESENT_IS_EMPTY = "Assessment Id Key is not present/is empty";

	public static final String USER_ASSESSMENT_DATA_NOT_PRESENT = "User Assessment Data not present in Databases";
	public static final String ASSESSMENT_ID_INVALID = "The Assessment Id is Invalid/Doesn't match with our records";
	public static final String IDENTIFIER_LIST_IS_EMPTY = "Identifier List is Empty";
	public static final String THE_QUESTIONS_IDS_PROVIDED_DONT_MATCH = "The Questions Ids Provided don't match the active user assessment session";


	public static final String ASSESSMENT_READ_RESPONSE = "assessmentreadresponse";

	public static final String API_READ_ASSESSMENT = "api.assessment.read";
	public static final String ASSESSMENT_READ_RESPONSE_KEY = "assessmentReadResponse";
	public static final String BULK_UPLOAD_VERIFICATION_REGEX = "bulk.upload.tag.verification.regex";


	public static final String EMPTY="";
	public static final String UNDER_SCORE="_";

	public static final String ASSESSMENT_INVALID = "Assessment Data doesn't contain mandatory values of expected duration.";

	public static final String DB_COLUMN_CREDIT_DATE = "credit_date";

	public static final String NEGATIVE_MARKING_PERCENTAGE = "negativeMarkingPercentage";
	public static final String ASSESSMENT_TYPE = "assessmentType";
	public static final String TOTAL_MARKS = "totalMarks";
	public static final String QUESTION_SECTION_SCHEME = "questionSectionScheme";
	public static final String OPTION_WEIGHTAGE = "optionalWeightage";
	public static final String QUESTION_WEIGHTAGE = "questionWeightage";
	public static final String QUESTION_LEVEL = "questionLevel";
	public static final String SECTION_RESULT = "sectionResult";
	public static final String SECTION_MARKS= "sectionMarks";
	public static final String FAIL = "fail";

	public static final String PAGE_ID = "pageId";

	public static final String MCQ_MCA_W = "mcq-mca-w";
	public static final String MCQ_SCA_TF = "mcq-sca-tf";

	public static final String API_SUBMIT_ASSESSMENT = "api.submit.asssessment";

	public static final String ASSESSMENT_ID_INVALID_SESSION_EXPIRED = "Assessment Id Invalid/Session Expired/Redis Cache doesn't have this question list details";
	public static final String INVALID_ASSESSMENT_ID = "Invalid Assessment Id";
	public static final String READ_ASSESSMENT_FAILED = "Failed to read assessment hierarchy for the given AssessmentId.";
	public static final String READ_ASSESSMENT_START_TIME_FAILED = "Failed to read the assessment start time.";
	public static final String WRONG_SECTION_DETAILS = "Wrong section details.";
	public static final String ASSESSMENT_SUBMIT_EXPIRED = "The Assessment submission time-period is over! Assessment can't be submitted";
	public static final String ASSESSMENT_ALREADY_SUBMITTED = "This Assessment is already Submitted!";


	public static final String SCORE_CUTOFF_TYPE = "scoreCutoffType";
	public static final String PASS_PERCENTAGE = "passPercentage";
	public static final String OVERALL_RESULT = "overallResult";

	public static final String ASSESSMENT_LEVEL_SCORE_CUTOFF = "assessmentlevel";
	public static final String SECTION_LEVEL_SCORE_CUTOFF = "sectionlevel";

	public static final String ASSESSMENT_SUBMIT_INVALID_QUESTION = "The QuestionId provided don't match to the Assessment Read";
	public static final String ASSESSMENT_SUBMIT_QUESTION_READ_FAILED = "Failed to read Question Set from DB";

	public static final String TOTAL_SCORE = "totalScore";
	public static final String SUBMIT_ASSESSMENT_RESPONSE = "submitassessmentresponse";
	public static final String CONTENT_ID_KEY = "contentId";
	public static final String BATCH_ID = "batchId";
	public static final String COURSE_ID = "courseId";
	public static final String COMPETENCY = "competency";

	public static final String MAX_ASSESSMENT_RETAKE_ATTEMPTS = "maxAssessmentRetakeAttempts";
	public static final String SUBMIT_ASSESSMENT_RESPONSE_KEY = "submitAssessmentResponse";
	public static final String TIME_TAKEN_FOR_ASSESSMENT= "timeTakenForAssessment";
	public static final String RETAKE_ATTEMPT_CONSUMED = "retakeAttemptsConsumed";
	public static final String TOTAL_PERCENTAGE = "totalPercentage";
	public static final String TOTAL_SECTION_MARKS = "totalSectionMarks";
	public static final String SECTION_LEVEL_DEFINITION = "sectionLevelDefinition";
	public static final String ASSESSMENT_RETRY_ATTEMPTS_CROSSED = "Maximum retry attempts for assessment reached.";
	public static final String NO_OF_QUESTIONS = "noOfQuestions";
	public static final String ASSESSMENT_IDENTIFIER="assessmentIdentifier";
	public static final String CONTEXT_ID="contextId";
	public static final String API_READ_ASSESSMENT_RESULT = "api.assessment.read.result";
	public static final String INVALID_REQUEST = "Invalid Request";
	public static final String STATUS_IS_IN_PROGRESS = "isInProgress";
	public static final String SOURCE = "source";
	public static final String POSTER_IMAGE = "posterImage";
	public static final String COMPLETION_DATE = "completionDate";
	public static final String PROVIDER_NAME = "providerName";
	public static final String RECIPIENT_NAME = "recipientName";
	public static final String COURSE_NAME = "courseName";
	public static final String COURSE_POSTER_IMAGE ="coursePosterImage";
	public static final String COURSE_ASSESSMENT = "Course Assessment";
	public static final String PUBLIC_USER_ASSESSMENT_TABLE ="public_user_assessment_data_test";
	public static final String API_ASSESSMENT_NOTIFY = "api.assessment.notify";
	public static final String RECIPIENT_EMAILS = "recipientEmails";
	public static final String LINK = "link";
	public static final String COURSE_POSTER_IMAGE_URL= "coursePosterImageUrl";
	public static final String COURSE_PROVIDER = "courseProvider";
	public static final String ID = "id";
	public static final String TYPE = "type";
	public static final String TEMPLATE = "template";
	public static final String USER = "user";
	public static final String CATEGORY = "category";
	public static final String DATA = "data";
	public static final String PARAMS = "params";
	public static final String CONFIG = "config";
	public static final String NOTIFICATIONS = "notifications";
	public static final String SUBJECT = "subject";
	public static final String TABLE_EMAIL_TEMPLATE = "email_template";
	public static final String CREATED_BY = "createdBy";
	public static final String CERTIFICATE_LINK = "certificateLink";
	public static final String COURSE_POSTER_IMAGE_KEY = "coursePosterImage";
	public static final String E_DATA ="edata";
	public static final String RELATED = "related";
    public static final String COURSE_COMPLETION_SUBJECT = "Congratulations on your successful completion of a course at iGOT Karmayogi!";
	public static final String ISSUED_CERTIFICATE  = "issued_certificates";
	public static final String CERT_PUBLIC_URL ="cert_publicurl";
	public static final String COURSE_COMPLETE_SUBJECT = "Course Completion Certificate";

	private Constants() {
		throw new IllegalStateException("Utility class");
	}

}
