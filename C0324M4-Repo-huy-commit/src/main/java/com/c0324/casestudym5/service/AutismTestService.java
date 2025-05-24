package com.c0324.casestudym5.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.c0324.casestudym5.dto.AutismTestForm;
import com.c0324.casestudym5.model.AutismQuestion;
import com.c0324.casestudym5.model.AutismTest;
import com.c0324.casestudym5.model.AutismTestAnswer;
import com.c0324.casestudym5.model.AutismTestResult;
import com.c0324.casestudym5.model.User;
import com.c0324.casestudym5.repository.AutismQuestionRepository;
import com.c0324.casestudym5.repository.AutismTestAnswerRepository;
import com.c0324.casestudym5.repository.AutismTestRepository;
import com.c0324.casestudym5.repository.AutismTestResultRepository;
import com.c0324.casestudym5.repository.UserRepository;

@Service
public class AutismTestService {
    private final AutismQuestionRepository questionRepository;
    private final AutismTestRepository testRepository;
    private final AutismTestResultRepository resultRepository;
    private final AutismTestAnswerRepository answerRepository;
    private final UserRepository userRepository;

    @Autowired
    public AutismTestService(
            AutismQuestionRepository questionRepository,
            AutismTestRepository testRepository,
            AutismTestResultRepository resultRepository,
            AutismTestAnswerRepository answerRepository,
            UserRepository userRepository) {
        this.questionRepository = questionRepository;
        this.testRepository = testRepository;
        this.resultRepository = resultRepository;
        this.answerRepository = answerRepository;
        this.userRepository = userRepository;
    }

    public List<AutismTest> getActiveTests() {
        return testRepository.findByStatus(AutismTest.TestStatus.ACTIVE);
    }

    public List<AutismTest> getTestsByCreator(User creator) {
        return testRepository.findByCreator(creator);
    }

    public List<AutismTest> getTestsForChildAge(int age) {
        List<AutismTest> ageSpecificTests = testRepository.findByAgeRange(age);

        // If no tests are found for the specific age, return all active tests
        if (ageSpecificTests.isEmpty()) {
            return getActiveTests();
        }

        return ageSpecificTests;
    }

    public AutismTest getTestById(Long testId) {
        return testRepository.findById(testId).orElse(null);
    }

    public List<AutismQuestion> getQuestionsForTest(Long testId) {
        // In a real implementation, there should be a relation between tests and questions
        // For this implementation, we'll return all questions
        return questionRepository.findAll();
    }

    public Map<String, List<AutismQuestion>> groupQuestionsByCategory(List<AutismQuestion> questions) {
        return questions.stream()
                .collect(Collectors.groupingBy(question ->
                    question.getCategory() != null ? question.getCategory() : "Other"));
    }

    public Map<String, List<AutismTestAnswer>> groupAnswersByCategory(List<AutismTestAnswer> answers) {
        return answers.stream()
                .collect(Collectors.groupingBy(answer ->
                    answer.getQuestion().getCategory() != null ?
                    answer.getQuestion().getCategory() : "Other"));
    }

    public List<AutismTestResult> getResultsForStudentAndTest(Long userId, Long testId) {
        User student = userRepository.findById(userId).orElse(null);
        AutismTest test = testRepository.findById(testId).orElse(null);

        if (student == null || test == null) {
            return new ArrayList<>();
        }

        return resultRepository.findByStudentAndTest(student, test);
    }

    @Transactional
    public Long submitTest(Long userId, AutismTestForm form) {
        User student = userRepository.findById(userId).orElse(null);
        AutismTest test = testRepository.findById(form.getTestId()).orElse(null);

        if (student == null || test == null) {
            throw new IllegalArgumentException("User or Test not found");
        }

        // Create a new test result
        AutismTestResult result = new AutismTestResult();
        result.setStudent(student);
        result.setTest(test);
        result.setCompletedAt(LocalDateTime.now());
        result.setStatus(AutismTestResult.TestResultStatus.PENDING_REVIEW);

        // Calculate score (this is a simplified calculation)
        int totalScore = 0;

        resultRepository.save(result);

        // Save answers and calculate score
        for (Map.Entry<Long, String> entry : form.getAnswers().entrySet()) {
            Long questionId = entry.getKey();
            String answer = entry.getValue();

            AutismQuestion question = questionRepository.findById(questionId).orElse(null);
            if (question != null) {
                AutismTestAnswer testAnswer = new AutismTestAnswer();
                testAnswer.setResult(result);
                testAnswer.setQuestion(question);
                testAnswer.setAnswerValue(answer);

                // Simplified scoring logic - adjust based on your requirements
                int answerScore = calculateAnswerScore(question, answer);
                testAnswer.setScore(answerScore);
                totalScore += answerScore;

                answerRepository.save(testAnswer);
            }
        }

        // Update the total score
        result.setTotalScore(totalScore);
        resultRepository.save(result);

        return result.getId();
    }

    private int calculateAnswerScore(AutismQuestion question, String answer) {
        // This is a simplified scoring logic for autism assessment
        // "Yes" answers typically indicate concern in autism assessment (positive for autism traits)
        if (answer.equalsIgnoreCase("yes")) {
            return question.getWeightScore() != null ? question.getWeightScore() : 1;
        } else {
            return 0;
        }
    }

    public List<AutismTestResult> getResultsForStudent(Long userId) {
        User student = userRepository.findById(userId).orElse(null);
        if (student == null) {
            return new ArrayList<>();
        }
        return resultRepository.findByStudent(student);
    }

    public List<AutismTestResult> getPendingReviewResults() {
        return resultRepository.findByStatus(AutismTestResult.TestResultStatus.PENDING_REVIEW);
    }

    public List<User> getStudentsWithPendingResults() {
        List<AutismTestResult> pendingResults = getPendingReviewResults();
        Map<Long, User> uniqueStudents = new HashMap<>();

        for (AutismTestResult result : pendingResults) {
            uniqueStudents.put(result.getStudent().getId(), result.getStudent());
        }

        return new ArrayList<>(uniqueStudents.values());
    }

    public AutismTestResult getResultById(Long resultId) {
        return resultRepository.findById(resultId).orElse(null);
    }

    public List<AutismTestAnswer> getAnswersForResult(Long resultId) {
        AutismTestResult result = resultRepository.findById(resultId).orElse(null);
        if (result == null) {
            return new ArrayList<>();
        }
        return answerRepository.findByResult(result);
    }

    @Transactional
    public void saveReview(Long resultId, Long userId, String notes) {
        AutismTestResult result = resultRepository.findById(resultId).orElse(null);
        User reviewer = userRepository.findById(userId).orElse(null);

        if (result == null || reviewer == null) {
            throw new IllegalArgumentException("Result or User not found");
        }

        result.setReviewedBy(reviewer);
        result.setReviewedAt(LocalDateTime.now());
        result.setTeacherNotes(notes);
        result.setStatus(AutismTestResult.TestResultStatus.REVIEWED);

        resultRepository.save(result);
    }

    // Methods for teacher/specialists to manage tests

    @Transactional
    public AutismTest createTest(String name, String description, Integer minAge, Integer maxAge, User creator) {
        if (creator == null) {
            throw new IllegalArgumentException("User not found");
        }

        AutismTest test = new AutismTest();
        test.setTestName(name);
        test.setDescription(description);
        test.setAgeRangeMin(minAge);
        test.setAgeRangeMax(maxAge);
        test.setCreator(creator);
        test.setStatus(AutismTest.TestStatus.DRAFT);
        test.setCreatedAt(LocalDateTime.now());

        return testRepository.save(test);
    }

    @Transactional
    public void updateTestStatus(Long testId, AutismTest.TestStatus status) {
        AutismTest test = testRepository.findById(testId).orElse(null);
        if (test == null) {
            throw new IllegalArgumentException("Test not found");
        }

        test.setStatus(status);
        testRepository.save(test);
    }

    @Transactional
    public void deleteTest(Long testId) {
        AutismTest test = testRepository.findById(testId).orElse(null);
        if (test == null) {
            return;
        }

        // Delete associated questions (if there is a TestQuestion entity)
        // Delete associated results
        List<AutismTestResult> results = resultRepository.findByTest(test);
        for (AutismTestResult result : results) {
            List<AutismTestAnswer> answers = answerRepository.findByResult(result);
            answerRepository.deleteAll(answers);
        }
        resultRepository.deleteAll(results);

        // Delete the test
        testRepository.delete(test);
    }

    @Transactional
    public AutismQuestion addQuestion(String question, String category, Integer weightScore, String answerType) {
        AutismQuestion q = new AutismQuestion();
        q.setQuestion(question);
        q.setCategory(category);
        q.setWeightScore(weightScore);
        q.setAnswerType(answerType);

        return questionRepository.save(q);
    }

    @Transactional
    public void addPredefinedQuestionsToTest(Long testId) {
        // Add the predefined questions from the requirements

        // Category 1: Communication - Language
        addQuestion("Does your baby show signs of \"never making any sounds\"?",
                   "Communication - Language", 2, "YES_NO");

        addQuestion("Does your baby show signs of \"not knowing how to express himself/herself when hungry, wet with urine, or passing stools\" ?",
                   "Communication - Language", 2, "YES_NO");

        addQuestion("Does your baby show signs of \"not blinking/flinching when there is a loud noise\" ?",
                   "Communication - Language", 1, "YES_NO");

        // Category 2: Gross motor
        addQuestion("Does your baby have reduced movement in his/her hands/feet or overall body weakness ?",
                   "Gross motor", 2, "YES_NO");

        addQuestion("Is your baby experiencing any limited mobility in the major joints? (Hip, knee, ankle, shoulder, elbow, wrist, or neck pain ?)",
                   "Gross motor", 2, "YES_NO");

        addQuestion("Does your baby have any foot deformities? (Extra/missing toes, partial foot amputation, clubfoot, shortening)",
                   "Gross motor", 3, "YES_NO");

        // Category 3: Fine motor
        addQuestion("Does your baby have any hand deformities? (Extra/missing fingers, partial amputation, clubhand, shortening)",
                   "Fine motor", 3, "YES_NO");

        addQuestion("Does your baby have any restrictions in bending/extending their fingers ?",
                   "Fine motor", 2, "YES_NO");

        addQuestion("Is your baby's hand usually tightly closed more than normal ?",
                   "Fine motor", 1, "YES_NO");

        // Category 4: Imitate and learn
        addQuestion("Does your baby have an unusual facial expression or a facial deformity ?",
                   "Imitate and learn", 2, "YES_NO");

        addQuestion("Does your baby have any abnormalities in the head? (such as bone deformities, cranial tumors, or absence of the fontanelle)",
                   "Imitate and learn", 3, "YES_NO");

        addQuestion("Does your baby show signs of \"not knowing how to smile (smiling while asleep)\" ?",
                   "Imitate and learn", 2, "YES_NO");

        // Category 5: Personal - Social
        addQuestion("Does your baby have difficulty with bowel movements or urination? (such as inability to pass stool or difficulty urinating)",
                   "Personal - Social", 2, "YES_NO");

        addQuestion("Does your baby cry excessively throughout the day and night ?",
                   "Personal - Social", 1, "YES_NO");

        addQuestion("Does your baby have difficulty with sucking, swallowing, or drinking ?",
                   "Personal - Social", 2, "YES_NO");

        // Category 6: Other extraordinary signs
        addQuestion("Does your baby have seizures ?",
                   "Other extraordinary signs", 3, "YES_NO");

        addQuestion("Does your baby have any abnormalities in the face (lips, cleft palate), neck, spine, arms, or legs ?",
                   "Other extraordinary signs", 2, "YES_NO");

        addQuestion("Does your baby have any abnormalities in the ears ? For example, missing earlobes or ear canals",
                   "Other extraordinary signs", 2, "YES_NO");

        addQuestion("Does your baby have any abnormalities in the eyes ? (Crossed eyes, drooping eyelids, bulging eyes.)",
                   "Other extraordinary signs", 2, "YES_NO");

        addQuestion("Are there any other abnormalities in children ?",
                   "Other extraordinary signs", 1, "YES_NO");
    }
}
