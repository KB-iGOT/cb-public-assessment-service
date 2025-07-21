package com.assessment.service.impl;

import static org.junit.jupiter.api.Assertions.*;

import com.assessment.cassandra.utils.CassandraOperation;
import com.assessment.service.OutboundRequestHandlerServiceImpl;
import com.assessment.util.ServerProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

@ExtendWith(MockitoExtension.class)
class AssessmentUtilServiceV2ImplMethodTest {

    @InjectMocks
    AssessmentUtilServiceV2Impl util;

    @Mock
    ServerProperties serverProperties;

    @Mock
    OutboundRequestHandlerServiceImpl outboundRequestHandlerService;

    @Mock
    CassandraOperation cassandraOperation;

    @Spy
    ObjectMapper mapper = new ObjectMapper();

    Map<String, Object> questionSetDetailsMap;
    List<String> originalQuestionList;
    List<Map<String, Object>> userQuestionList;
    Map<String, Object> questionMap;

    @BeforeEach
    void setup() {
        questionSetDetailsMap = new HashMap<>();
        originalQuestionList = List.of("q1");
        userQuestionList = new ArrayList<>();
        questionMap = new HashMap<>();

        // Setup questionMap
        Map<String, Object> question = new HashMap<>();
        question.put("identifier", "q1");
        question.put("questionType", "mcq_sca");
        question.put("questionLevel", "level1");

        Map<String, Object> editorState = new HashMap<>();
        List<Map<String, Object>> options = new ArrayList<>();
        Map<String, Object> option = new HashMap<>();
        option.put("index", "1");
        option.put("answer", true);
        option.put("value", Map.of("value", "option1"));
        options.add(option);

        editorState.put("options", options);
        question.put("editorState", editorState);

        questionMap.put("q1", question);

        // Setup userQuestionList
        Map<String, Object> userAnswer = new HashMap<>();
        userAnswer.put("identifier", "q1");
        userAnswer.put("questionType", "mcq_sca");
        userAnswer.put("editorState", Map.of(
                "options", List.of(
                        Map.of(
                                "index", "1",
                                "selectedAnswer", true,
                                "value", Map.of("value", "option1"),
                                "answer", true
                        )
                )
        ));

        userQuestionList.add(userAnswer);

        questionSetDetailsMap.put("assessmentType", "questionWeightage");
        questionSetDetailsMap.put("totalMarks", 10);
        questionSetDetailsMap.put("minimumPassPercentage", 50);
        questionSetDetailsMap.put("questionSectionScheme", Map.of("level1", 5));
        questionSetDetailsMap.put("negativeMarkingPercentage", "0%");
    }


    @Test
    void test_validateQumlAssessmentV2_withQuestionWeightage_correct() {
        questionSetDetailsMap.put("assessmentType", "QUESTION_WEIGHTAGE");
        questionSetDetailsMap.put("minimumPassPercentage", 50);
        questionSetDetailsMap.put("totalMarks", 10);
        questionSetDetailsMap.put("questionSectionScheme", Map.of("level1", 5));
        questionSetDetailsMap.put("negativeMarkingPercentage", "20%");

        Map<String, Object> question = new HashMap<>();
        question.put("identifier", "q1");
        question.put("questionType", "mcq_sca");
        question.put("editorState", Map.of("options", List.of(
                Map.of("index", "1", "selectedAnswer", true, "value", Map.of("value", "opt1"), "answer", true)
        )));
        userQuestionList.add(question);

        questionMap.put("q1", Map.of(
                "identifier", "q1",
                "questionType", "mcq_sca",
                "editorState", Map.of("options", List.of(
                        Map.of("index", "1", "answer", true, "value", Map.of("value", "opt1"))
                )),
                "questionLevel", "level1"
        ));

        Map<String, Object> result = util.validateQumlAssessmentV2(
                questionSetDetailsMap, originalQuestionList, userQuestionList, questionMap);

        assertNotNull(result);
    }

    @Test
    void test_validateQumlAssessmentV2_questionWeightage() {
        Map<String, Object> result = util.validateQumlAssessmentV2(
                questionSetDetailsMap,
                originalQuestionList,
                userQuestionList,
                questionMap
        );

        assertNotNull(result);
    }


    @Test
    void test_validateQumlAssessmentV2_blankQuestion() {
        questionSetDetailsMap.put("assessmentType", "QUESTION_WEIGHTAGE");
        questionSetDetailsMap.put("minimumPassPercentage", 50);
        questionSetDetailsMap.put("totalMarks", 10);
        questionSetDetailsMap.put("questionSectionScheme", Map.of("level1", 5));
        questionSetDetailsMap.put("negativeMarkingPercentage", "20%");

        Map<String, Object> question = new HashMap<>();
        question.put("identifier", "q1");
        question.put("questionType", "mcq_sca");
        question.put("editorState", Map.of("options", List.of())); // no options marked
        userQuestionList.add(question);

        questionMap.put("q1", Map.of(
                "identifier", "q1",
                "questionType", "mcq_sca",
                "editorState", Map.of("options", List.of())
        ));

        Map<String, Object> result = util.validateQumlAssessmentV2(
                questionSetDetailsMap, originalQuestionList, userQuestionList, questionMap);

        assertNotNull(result);
    }

    @Test
    void test_validateQumlAssessmentV2_handlesException() {
        questionSetDetailsMap.put("assessmentType", null);

        Map<String, Object> result = util.validateQumlAssessmentV2(
                questionSetDetailsMap, originalQuestionList, userQuestionList, questionMap);

        assertNotNull(result);
        assertTrue(result.isEmpty()); // because exception path returns empty map
    }

}

