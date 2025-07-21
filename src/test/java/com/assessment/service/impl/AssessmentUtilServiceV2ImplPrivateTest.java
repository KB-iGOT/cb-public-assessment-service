package com.assessment.service.impl;

import static org.junit.jupiter.api.Assertions.*;

import com.assessment.util.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Stream;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AssessmentUtilServiceV2ImplPrivateTest {

    private AssessmentUtilServiceV2Impl service;

    @BeforeEach
    void setUp() {
        service = new AssessmentUtilServiceV2Impl();
    }

    @Test
    void test_fetchRecursiveQuestionIds_withNestedChildren() throws Exception {
        // prepare test data
        Map<String, Object> leaf1 = new HashMap<>();
        leaf1.put("objectType", "Question");
        leaf1.put("identifier", "q1");

        Map<String, Object> leaf2 = new HashMap<>();
        leaf2.put("objectType", "Question");
        leaf2.put("identifier", "q2");

        Map<String, Object> nestedSet = new HashMap<>();
        nestedSet.put("objectType", "QuestionSet");
        nestedSet.put("children", List.of(leaf2));

        Map<String, Object> root = new HashMap<>();
        root.put("objectType", "QuestionSet");
        root.put("children", List.of(leaf1, nestedSet));

        List<Map<String, Object>> children = List.of(root);

        // reflectively get the private method
        Method method = AssessmentUtilServiceV2Impl.class.getDeclaredMethod("fetchRecursiveQuestionIds", List.class, List.class);
        method.setAccessible(true);

        // invoke
        List<String> result = (List<String>) method.invoke(service, children, new ArrayList<>());

        // assertions
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains("q1"));
        assertTrue(result.contains("q2"));
    }

    @Test
    void test_fetchRecursiveQuestionIds_withEmptyChildren() throws Exception {
        Method method = AssessmentUtilServiceV2Impl.class.getDeclaredMethod("fetchRecursiveQuestionIds", List.class, List.class);
        method.setAccessible(true);

        List<String> result = (List<String>) method.invoke(service, Collections.emptyList(), new ArrayList<>());

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @ParameterizedTest
    @MethodSource("provideTestCases")
    void test_calculateScoreForOptionWeightage(
            String assessmentType,
            List<String> marked,
            Double expected
    ) throws Exception {
        Method method = getPrivateMethod();

        Map<String, Object> question = Map.of("identifier", "q1");

        Map<String, Object> optionWeightageForQ1 = Map.of("1", 2, "2", 4);
        Map<String, Object> optionWeightages = Map.of("q1", optionWeightageForQ1);

        Double sectionMarks = 5.0;

        Double result = (Double) method.invoke(
                service, question, assessmentType, optionWeightages, sectionMarks, marked
        );

        assertEquals(expected, result);
    }

    @Test
    void test_getMarkedIndexForOptionWeightAge() throws Exception {
        // Arrange


        List<Map<String, Object>> options = new ArrayList<>();
        Map<String, Object> option1 = new HashMap<>();
        option1.put("index", "1");
        options.add(option1);

        Map<String, Object> option2 = new HashMap<>();
        option2.put("index", "2");
        options.add(option2);

        List<String> marked = new ArrayList<>();

        // Act
        Method method = getPMethod();
        method.invoke(service, options, marked);

        // Assert
        assertEquals(2, marked.size());
        assertEquals("1", marked.get(0));
        assertEquals("2", marked.get(1));
    }

    @Test
    void test_getMarkedIndexForQuestionWeightAge() throws Exception {
        // Arrange


        List<Map<String, Object>> options = new ArrayList<>();

        Map<String, Object> option1 = new HashMap<>();
        option1.put("selectedAnswer", true);
        option1.put("index", "1");
        options.add(option1);

        Map<String, Object> option2 = new HashMap<>();
        option2.put("selectedAnswer", false);
        option2.put("index", "2");
        options.add(option2);

        Map<String, Object> option3 = new HashMap<>();
        option3.put("selectedAnswer", true);
        option3.put("index", "3");
        options.add(option3);

        List<String> marked = new ArrayList<>();

        // Act
        Method method = getMarkedIndexForQuestionWeightAgeMethod();
        method.invoke(service, options, marked);

        // Assert
        assertEquals(2, marked.size());
        assertTrue(marked.contains("1"));
        assertTrue(marked.contains("3"));
    }

    @Test
    void test_handleCorrectAnswer() throws Exception {
        // Arrange


        Double sectionMarks = 5.0;

        Map<String, Object> questionSetSectionScheme = new HashMap<>();
        questionSetSectionScheme.put("easy", 2);
        questionSetSectionScheme.put("medium", 4);
        questionSetSectionScheme.put("hard", 6);

        Map<String, Object> proficiencyMap = new HashMap<>();
        proficiencyMap.put("questionLevel", "medium");

        // Act
        Method method = getMethodCorrectAnswer();
        Double result = (Double) method.invoke(service, sectionMarks, questionSetSectionScheme, proficiencyMap);

        // Assert
        assertEquals(5.0 + 4.0, result);
    }

    @Test
    void test_handleIncorrectAnswer_withNegativeMarks() throws Exception {


        int negativeMarksValue = 25; // 25%
        Double sectionMarks = 10.0;

        Map<String, Object> questionSetSectionScheme = new HashMap<>();
        questionSetSectionScheme.put("easy", 4);

        Map<String, Object> proficiencyMap = new HashMap<>();
        proficiencyMap.put("questionLevel", "easy");

        Method method = getMethodhandleIncorrectAnswer();
        Double result = (Double) method.invoke(service, negativeMarksValue, sectionMarks, questionSetSectionScheme, proficiencyMap);

        // expected deduction: 10 - (25% * 4) = 10 - 1.0 = 9.0
        assertEquals(9.0, result);
    }

    @Test
    void test_handleIncorrectAnswer_withZeroNegativeMarks() throws Exception {


        int negativeMarksValue = 0;
        Double sectionMarks = 10.0;

        Map<String, Object> questionSetSectionScheme = new HashMap<>();
        questionSetSectionScheme.put("easy", 4);

        Map<String, Object> proficiencyMap = new HashMap<>();
        proficiencyMap.put("questionLevel", "easy");

        Method method = getMethodhandleIncorrectAnswer();
        Double result = (Double) method.invoke(service, negativeMarksValue, sectionMarks, questionSetSectionScheme, proficiencyMap);

        // No deduction because negativeMarksValue = 0
        assertEquals(10.0, result);
    }


    @ParameterizedTest
    @MethodSource("provideSectionResultCases")
    void test_computeSectionResults(
            Double sectionMarks,
            Integer totalMarks,
            int minimumPassValue,
            String expectedResult
    ) throws Exception {

        Map<String, Object> resultMap = new HashMap<>();

        Method method = getMethodComputeSectionResults();
        method.invoke(service, sectionMarks, totalMarks, minimumPassValue, resultMap);

        assertEquals(expectedResult, resultMap.get("sectionResult"));
    }

    @Test
    void test_updateResultMap() throws Exception {

        // input data
        List<Map<String, Object>> userQuestionList = new ArrayList<>();
        Map<String, Object> question = new HashMap<>();
        question.put("id", "q1");
        userQuestionList.add(question);

        Integer correct = 2;
        Integer blank = 1;
        Integer inCorrect = 3;
        Double sectionMarks = 8.0;
        Integer totalMarks = 10;

        Map<String, Object> resultMap = new HashMap<>();

        Method method = getMethodUpdateResultMap();
        method.invoke(service, userQuestionList, correct, blank, inCorrect, resultMap, sectionMarks, totalMarks);

        assertEquals(inCorrect, resultMap.get("incorrect"));
        assertEquals(blank, resultMap.get("blank"));
        assertEquals(correct, resultMap.get("correct"));
        assertEquals(userQuestionList, resultMap.get("children"));
        assertEquals(sectionMarks, resultMap.get("sectionMarks"));
        assertEquals(totalMarks, resultMap.get("totalMarks"));
    }

    private Method getMethodUpdateResultMap() throws NoSuchMethodException {
        Method method = AssessmentUtilServiceV2Impl.class.getDeclaredMethod("updateResultMap", List.class, Integer.class, Integer.class, Integer.class, Map.class, Double.class, Integer.class);
        method.setAccessible(true);
        return method;
    }

    @Test
    void test_getProficiencyMap() throws Exception {


        Map<String, Object> questionMap = new HashMap<>();
        Map<String, Object> question = new HashMap<>();
        question.put("identifier", "q1");

        Map<String, Object> expectedProficiency = Map.of("level", "easy");
        questionMap.put("q1", expectedProficiency);

        Method method = getMapMethod("getProficiencyMap", Map.class, Map.class);

        Object result = method.invoke(service, questionMap, question);

        assertEquals(expectedProficiency, result);
    }

    @Test
    void test_sortAnswers_withMoreThanOneElement() throws Exception {


        List<String> answers = new ArrayList<>(Arrays.asList("b", "a", "c"));

        Method method = getMapMethod("sortAnswers", List.class);

        method.invoke(service, answers);

        assertEquals(Arrays.asList("a", "b", "c"), answers);
    }

    @Test
    void test_sortAnswers_withSingleElement() throws Exception {

        List<String> answers = new ArrayList<>(Collections.singletonList("only"));

        Method method = getMapMethod("sortAnswers", List.class);

        method.invoke(service, answers);

        assertEquals(Collections.singletonList("only"), answers); // should remain unchanged
    }

    @Test
    void testCalculatePassPercentage_questionWeightage() throws Exception {
        Method method = AssessmentUtilServiceV2Impl.class.getDeclaredMethod("calculatePassPercentage", Double.class, Integer.class, Integer.class, Integer.class, Integer.class, String.class, Map.class);
        method.setAccessible(true);

        Map<String, Object> resultMap = new HashMap<>();
        Double sectionMarks = 80.0;
        Integer totalMarks = 100;
        Integer correct = 8;
        Integer blank = 1;
        Integer inCorrect = 1;

        method.invoke(null,  // static method
                sectionMarks, totalMarks, correct, blank, inCorrect, "questionWeightage", resultMap);

        assertEquals(80.0, resultMap.get(Constants.RESULT));
    }

    @Test
    void testCalculatePassPercentage_optionWeightage() throws Exception {
        Method method = AssessmentUtilServiceV2Impl.class.getDeclaredMethod("calculatePassPercentage", Double.class, Integer.class, Integer.class, Integer.class, Integer.class, String.class, Map.class);
        method.setAccessible(true);

        Map<String, Object> resultMap = new HashMap<>();
        Double sectionMarks = 80.0;  // ignored in this case
        Integer totalMarks = 100;   // ignored in this case
        Integer correct = 8;
        Integer blank = 1;
        Integer inCorrect = 1;

        method.invoke(null, sectionMarks, totalMarks, correct, blank, inCorrect, "optionalWeightage", resultMap);

        int expectedTotal = correct + blank + inCorrect;
        double expectedResult = (correct * 100d) / expectedTotal;

        assertEquals(expectedResult, resultMap.get(Constants.RESULT));
        assertEquals(expectedTotal, resultMap.get(Constants.TOTAL));
    }

    @Test
    void test_getMarkedIndexForEachQuestion_allBranches() throws Exception {
        Method method = AssessmentUtilServiceV2Impl.class.getDeclaredMethod("getMarkedIndexForEachQuestion", String.class, List.class, List.class, String.class);
        method.setAccessible(true);

        List<Map<String, Object>> options;
        List<String> marked;

        // MTF
        options = List.of(Map.of("index", "1", "selectedAnswer", true));
        marked = new ArrayList<>();
        method.invoke(service, "mtf", options, marked, "any");
        assertEquals(List.of("1-true"), marked);

        // FTB
        options = List.of(Map.of("selectedAnswer", "answer1"));
        marked = new ArrayList<>();
        method.invoke(service, "ftb", options, marked, "any");
        assertEquals(List.of("answer1"), marked);

        // MCQ_SCA - QUESTION_WEIGHTAGE
        options = List.of(Map.of("index", "2", "selectedAnswer", true));
        marked = new ArrayList<>();
        method.invoke(service, "mcq-sca", options, marked, "questionWeightage");
        assertEquals(List.of("2"), marked);

        // MCQ_SCA - OPTION_WEIGHTAGE
        options = List.of(Map.of("index", "3"));
        marked = new ArrayList<>();
        method.invoke(service, "mcq-sca", options, marked, "optionalWeightage");
        assertEquals(List.of("3"), marked);

        // MCQ_MCA_W - OPTION_WEIGHTAGE
        options = List.of(Map.of("index", "4"));
        marked = new ArrayList<>();
        method.invoke(service, "mcq-mca-w", options, marked, "optionalWeightage");
        assertEquals(List.of("4"), marked);

        // Default
        options = List.of(Map.of("index", "5", "selectedAnswer", false));
        marked = new ArrayList<>();
        method.invoke(service, "UNKNOWN_TYPE", options, marked, "any");
        assertTrue(marked.isEmpty()); // nothing added
    }

    @Test
    void test_getOptionWeightages() throws Exception {
        Method method = AssessmentUtilServiceV2Impl.class.getDeclaredMethod("getOptionWeightages", List.class, Map.class);
        method.setAccessible(true);

        List<String> questions = List.of("q1", "q2", "q3", "q4");

        Map<String, Object> questionMap = new HashMap<>();
        questionMap.put("q1", createQuestion("q1", "mcq-sca"));
        questionMap.put("q2", createQuestion("q2", "mcq-mca"));
        questionMap.put("q3", createQuestion("q3", "mcq-mca-w"));
        questionMap.put("q4", createQuestion("q4", "ftb")); // default case

        Map<String, Object> result = (Map<String, Object>) method.invoke(service, questions, questionMap);

        assertNotNull(result);
        assertEquals(4, result.size());

        // MCQ_SCA / MCQ_MCA / MCQ_MCA_W: optionWeightage populated
        Map<String, Object> q1Weightage = (Map<String, Object>) result.get("q1");
        assertEquals("answer1", q1Weightage.get("val1"));

        Map<String, Object> q2Weightage = (Map<String, Object>) result.get("q2");
        assertEquals("answer1", q2Weightage.get("val1"));

        Map<String, Object> q3Weightage = (Map<String, Object>) result.get("q3");
        assertEquals("answer1", q3Weightage.get("val1"));

        // default: empty map
        Map<String, Object> q4Weightage = (Map<String, Object>) result.get("q4");
        assertTrue(q4Weightage.isEmpty());
    }

    @Test
    void test_handleBlankAnswers() throws Exception {
        Method method = AssessmentUtilServiceV2Impl.class.getDeclaredMethod("handleBlankAnswers", List.class, Map.class, Integer.class);
        method.setAccessible(true);

        List<Map<String, Object>> userQuestionList = new ArrayList<>();
        Map<String, Object> answers = new HashMap<>();

        // Scenario: answers.size() > userQuestionList.size()
        userQuestionList.add(createQuestion("q1"));
        answers.put("q1", "a1");
        answers.put("q2", "a2");
        Integer blank = 0;

        Integer result = (Integer) method.invoke(service, userQuestionList, answers, blank);

        // answers.size() = 2, userQuestionList.size() = 1 → blank += 1
        assertEquals(1, result);

        // Scenario: answers.size() <= userQuestionList.size()
        answers.clear();
        answers.put("q1", "a1");
        userQuestionList.clear();
        userQuestionList.add(createQuestion("q1"));
        userQuestionList.add(createQuestion("q2"));

        blank = 0;

        result = (Integer) method.invoke(service, userQuestionList, answers, blank);

        assertEquals(0, result);
    }

    private Map<String, Object> createQuestion(String id) {
        Map<String, Object> question = new HashMap<>();
        question.put("identifier", id);
        return question;
    }

    private Map<String, Object> createQuestion(String id, String type) {
        Map<String, Object> question = new HashMap<>();
        question.put("identifier", id);
        question.put("qType", type);

        Map<String, Object> editorState = new HashMap<>();
        List<Map<String, Object>> options = new ArrayList<>();
        Map<String, Object> option = new HashMap<>();

        Map<String, Object> valueObj = new HashMap<>();
        valueObj.put("value", "val1");

        option.put("value", valueObj);
        option.put("answer", "answer1");
        options.add(option);

        editorState.put("options", options);
        question.put("editorState", editorState);
        return question;
    }

    private Method getMapMethod(String name, Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method = AssessmentUtilServiceV2Impl.class.getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        return method;
    }


    private Method getMethodComputeSectionResults() throws Exception {
        Method method = AssessmentUtilServiceV2Impl.class.getDeclaredMethod(
                "computeSectionResults",
                Double.class, Integer.class, int.class, Map.class
        );
        method.setAccessible(true);
        return method;
    }

    private Stream<Arguments> provideSectionResultCases() {
        return Stream.of(
                Arguments.of(8.0, 10, 50, "pass"),   // >50%
                Arguments.of(3.0, 10, 50, "fail"),  // <50%
                Arguments.of(0.0, 10, 50, "fail")   // 0 marks
        );
    }

    private Method getMethodhandleIncorrectAnswer() throws NoSuchMethodException {
        Method method = AssessmentUtilServiceV2Impl.class.getDeclaredMethod("handleIncorrectAnswer", int.class, Double.class, Map.class, Map.class);
        method.setAccessible(true);
        return method;
    }

    private Method getMethodCorrectAnswer() throws NoSuchMethodException {
        Method method = AssessmentUtilServiceV2Impl.class.getDeclaredMethod("handleCorrectAnswer", Double.class, Map.class, Map.class);
        method.setAccessible(true);
        return method;
    }

    private Method getMarkedIndexForQuestionWeightAgeMethod() throws NoSuchMethodException {
        Method method = AssessmentUtilServiceV2Impl.class.getDeclaredMethod("getMarkedIndexForQuestionWeightAge", List.class, List.class);
        method.setAccessible(true);
        return method;
    }

    private Method getPMethod() throws NoSuchMethodException {
        Method method = AssessmentUtilServiceV2Impl.class.getDeclaredMethod("getMarkedIndexForOptionWeightAge", List.class, List.class);
        method.setAccessible(true);
        return method;
    }

    private Method getPrivateMethod() throws Exception {
        Method method = AssessmentUtilServiceV2Impl.class.getDeclaredMethod(
                "calculateScoreForOptionWeightage",
                Map.class, String.class, Map.class, Double.class, List.class
        );
        method.setAccessible(true);
        return method;
    }

    private Stream<Arguments> provideTestCases() {
        return Stream.of(
                Arguments.of("OPTION_WEIGHTAGE", List.of("2"), 5.0),
                Arguments.of("OPTION_WEIGHTAGE", List.of("3"), 5.0),
                Arguments.of("SOME_OTHER_TYPE", List.of("1"), 5.0)
        );
    }
}

