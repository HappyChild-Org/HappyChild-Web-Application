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
        if (age <= 0) {
            return new ArrayList<>();
        }
        return testRepository.findByAgeRange(age);
    }

    public AutismTest getTestById(Long testId) {
        if (testId == null) {
            return null;
        }
        return testRepository.findById(testId).orElse(null);
    }

    public List<AutismQuestion> getQuestionsForTest(Long testId) {
        if (testId == null) return new ArrayList<>();
        AutismTest test = testRepository.findById(testId).orElse(null);
        if (test == null || test.getStatus() != AutismTest.TestStatus.ACTIVE) {
            return new ArrayList<>();
        }
        return questionRepository.findByAutismTestId(testId);
    }

    public Map<String, List<AutismQuestion>> groupQuestionsByCategory(List<AutismQuestion> questions) {
        if (questions == null || questions.isEmpty()) {
            return new HashMap<>();
        }
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
        if (userId == null || testId == null) {
            return new ArrayList<>();
        }
        User student = userRepository.findById(userId).orElse(null);
        AutismTest test = testRepository.findById(testId).orElse(null);

        if (student == null || test == null) {
            return new ArrayList<>();
        }

        return resultRepository.findByStudentAndTest(student, test);
    }

    @Transactional
    public Long submitTest(Long userId, AutismTestForm form) {
        if (userId == null || form == null || form.getTestId() == null) {
            throw new IllegalArgumentException("Invalid input parameters");
        }

        User student = userRepository.findById(userId).orElse(null);
        AutismTest test = testRepository.findById(form.getTestId()).orElse(null);

        if (student == null || test == null) {
            throw new IllegalArgumentException("User or Test not found");
        }

        if (test.getStatus() != AutismTest.TestStatus.ACTIVE) {
            throw new IllegalArgumentException("Test is not active");
        }

        // Check if student has already taken this test
        List<AutismTestResult> existingResults = getResultsForStudentAndTest(userId, form.getTestId());
        if (!existingResults.isEmpty()) {
            throw new IllegalArgumentException("Student has already taken this test");
        }

        // Create a new test result
        AutismTestResult result = new AutismTestResult();
        result.setStudent(student);
        result.setTest(test);
        result.setCompletedAt(LocalDateTime.now());
        result.setStatus(AutismTestResult.TestResultStatus.PENDING_REVIEW);

        // Calculate score
        int totalScore = 0;
        resultRepository.save(result);

        // Save answers and calculate score
        for (Map.Entry<Long, String> entry : form.getAnswers().entrySet()) {
            Long questionId = entry.getKey();
            String answer = entry.getValue();

            if (questionId == null || answer == null) {
                continue;
            }

            AutismQuestion question = questionRepository.findById(questionId).orElse(null);
            if (question != null) {
                AutismTestAnswer testAnswer = new AutismTestAnswer();
                testAnswer.setResult(result);
                testAnswer.setQuestion(question);
                testAnswer.setAnswerValue(answer);

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
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Test name is required");
        }
        if (minAge == null || maxAge == null || minAge < 0 || maxAge < 0 || minAge > maxAge) {
            throw new IllegalArgumentException("Invalid age range");
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
        testRepository.findById(testId).orElseThrow(() -> new IllegalArgumentException("Test not found"));
        
        // Thêm các câu hỏi mẫu
        addQuestionToTest(testId, "Does your baby show signs of \"never making any sounds\"?", "Communication - Language", 2, "YES_NO", null, null);
        addQuestionToTest(testId, "Does your baby show signs of \"not knowing how to express himself/herself when hungry, wet with urine, or passing stools\"?", "Communication - Language", 2, "YES_NO", null, null);
        addQuestionToTest(testId, "Does your baby show signs of \"not blinking/flinching when there is a loud noise\"?", "Communication - Language", 1, "YES_NO", null, null);
        addQuestionToTest(testId, "Does your baby have reduced movement in his/her body?", "Gross Motor", 2, "YES_NO", null, null);
        addQuestionToTest(testId, "Does your baby have any foot deformities? (Ex: flat foot, clubfoot)", "Gross Motor", 3, "YES_NO", null, null);
        addQuestionToTest(testId, "Does your baby have any hand deformities? (Ex: extra fingers)", "Fine Motor", 3, "YES_NO", null, null);
        addQuestionToTest(testId, "Does your baby have any restrictions in bending his/her fingers?", "Fine Motor", 2, "YES_NO", null, null);
        addQuestionToTest(testId, "Does your baby have an unusual facial expression?", "Imitate and Learn", 2, "YES_NO", null, null);
        addQuestionToTest(testId, "Does your baby have any abnormalities in the head?", "Imitate and Learn", 3, "YES_NO", null, null);
        addQuestionToTest(testId, "Does your baby have difficulty with bowel movements?", "Personal - Social", 2, "YES_NO", null, null);
        addQuestionToTest(testId, "Does your baby cry excessively with sucking, swallowing?", "Personal - Social", 2, "YES_NO", null, null);
        addQuestionToTest(testId, "Does your baby have seizures?", "Other extraordinary signs", 3, "YES_NO", null, null);
        addQuestionToTest(testId, "Does your baby have any abnormalities in the face?", "Other extraordinary signs", 2, "YES_NO", null, null);
    }

    @Transactional
    public AutismQuestion addQuestionToTest(Long testId, String questionText, String category, Integer weightScore, String answerType, String imageUrl, String options) {
        AutismTest test = testRepository.findById(testId).orElse(null);
        if (test == null) {
            throw new IllegalArgumentException("Test not found");
        }

        AutismQuestion question = new AutismQuestion();
        question.setQuestion(questionText);
        question.setCategory(category);
        question.setWeightScore(weightScore);
        question.setAnswerType(answerType);
        question.setImageUrl(imageUrl);
        question.setOptions(options);
        question.setAutismTest(test);

        return questionRepository.save(question);
    }

    // Lấy các câu hỏi chưa gán vào test nào
    public List<AutismQuestion> getCommonQuestions() {
        return questionRepository.findByAutismTestIsNull();
    }

    // Gán các câu hỏi vào test
    @org.springframework.transaction.annotation.Transactional
    public void addQuestionsToTest(Long testId, List<Long> questionIds) {
        AutismTest test = testRepository.findById(testId).orElseThrow();
        List<AutismQuestion> questions = questionRepository.findAllById(questionIds);
        for (AutismQuestion q : questions) {
            q.setAutismTest(test);
        }
        questionRepository.saveAll(questions);
    }
}
