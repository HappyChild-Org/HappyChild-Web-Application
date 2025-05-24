package com.c0324.casestudym5.controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.c0324.casestudym5.dto.AutismTestForm;
import com.c0324.casestudym5.dto.MessageDTO;
import com.c0324.casestudym5.model.AutismQuestion;
import com.c0324.casestudym5.model.AutismTest;
import com.c0324.casestudym5.model.AutismTestAnswer;
import com.c0324.casestudym5.model.AutismTestResult;
import com.c0324.casestudym5.model.User;
import com.c0324.casestudym5.service.AutismTestService;
import com.c0324.casestudym5.service.MessageService;
import com.c0324.casestudym5.service.UserService;

import jakarta.validation.Valid;

@Controller
public class AutismTestController {
    private final AutismTestService autismTestService;
    private final UserService userService;
    private final MessageService messageService;

    @Autowired
    public AutismTestController(AutismTestService autismTestService, UserService userService, MessageService messageService) {
        this.autismTestService = autismTestService;
        this.userService = userService;
        this.messageService = messageService;
    }


    @GetMapping("/student/autism-tests")
    public String showAvailableTests(Model model, Principal principal) {
        try {
            System.out.println("DEBUG: Accessing /student/autism-tests");

            String email = principal.getName();
            User user = userService.findByEmail(email);
            System.out.println("DEBUG: Found user with email " + email);


            int age = 5; // Default age if calculation fails
            try {
                age = userService.calculateAge(user);
                System.out.println("DEBUG: User age: " + age);
            } catch (Exception e) {
                System.err.println("Error calculating age: " + e.getMessage());
            }

            List<AutismTest> tests = autismTestService.getTestsForChildAge(age);
            System.out.println("DEBUG: Found " + tests.size() + " tests");

            model.addAttribute("tests", tests);
            model.addAttribute("student", user);

            return "student/autism-tests";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("errorMessage", e.getMessage());
            return "error";
        }
    }

    @GetMapping("/student/autism-test/{testId}")
    public String showTest(@PathVariable Long testId, Model model, Principal principal) {
        String email = principal.getName();
        User user = userService.findByEmail(email);

        List<AutismTestResult> existingResults = autismTestService.getResultsForStudentAndTest(user.getId(), testId);
        if (!existingResults.isEmpty()) {
            return "redirect:/student/autism-test-results";
        }

        AutismTest test = autismTestService.getTestById(testId);
        if (test == null) {
            return "redirect:/student/autism-tests";
        }

        List<AutismQuestion> questions = autismTestService.getQuestionsForTest(testId);

        Map<String, List<AutismQuestion>> questionsByCategory = autismTestService.groupQuestionsByCategory(questions);

        model.addAttribute("test", test);
        model.addAttribute("questions", questions);
        model.addAttribute("questionsByCategory", questionsByCategory);
        model.addAttribute("testForm", new AutismTestForm());

        return "student/take-test";
    }

    @PostMapping("/student/autism-test/{testId}")
    public String submitTest(
            @PathVariable Long testId,
            @Valid AutismTestForm testForm,
            Model model,
            Principal principal) {

        String email = principal.getName();
        User user = userService.findByEmail(email);

        testForm.setTestId(testId);
        Long resultId = autismTestService.submitTest(user.getId(), testForm);

        return "redirect:/student/autism-test-results";
    }

    @GetMapping("/student/autism-test-results")
    public String showTestResults(Model model, Principal principal) {
        String email = principal.getName();
        User user = userService.findByEmail(email);

        List<AutismTestResult> results = autismTestService.getResultsForStudent(user.getId());
        model.addAttribute("results", results);

        return "student/test-results";
    }

    @GetMapping("/student/autism-test-result/{resultId}")
    public String showTestResultDetail(@PathVariable Long resultId, Model model, Principal principal) {
        String email = principal.getName();
        User user = userService.findByEmail(email);

        AutismTestResult result = autismTestService.getResultById(resultId);

        if (result == null || !result.getStudent().getId().equals(user.getId())) {
            return "redirect:/student/autism-test-results";
        }

        List<AutismTestAnswer> answers = autismTestService.getAnswersForResult(resultId);

        model.addAttribute("result", result);
        model.addAttribute("answers", answers);

        return "student/test-result-detail";
    }

    @GetMapping("/teacher/pending-reviews")
    public String showPendingReviews(Model model, Principal principal) {
        String email = principal.getName();
        User teacher = userService.findByEmail(email);

        List<User> studentsWithPendingResults = autismTestService.getStudentsWithPendingResults();
        model.addAttribute("students", studentsWithPendingResults);

        return "teacher/pending-reviews";
    }

    @GetMapping("/teacher/student-results/{studentId}")
    public String showStudentResults(@PathVariable Long studentId, Model model, Principal principal) {
        String email = principal.getName();
        User teacher = userService.findByEmail(email);

        User student = userService.findById(studentId);
        if (student == null) {
            return "redirect:/teacher/pending-reviews";
        }

        List<AutismTestResult> results = autismTestService.getResultsForStudent(studentId);
        model.addAttribute("results", results);
        model.addAttribute("student", student);
        model.addAttribute("studentId", studentId);

        return "teacher/student-results";
    }

    @GetMapping("/teacher/review-test/{resultId}")
    public String showReviewForm(@PathVariable Long resultId, Model model, Principal principal) {
        String email = principal.getName();
        User teacher = userService.findByEmail(email);

        AutismTestResult result = autismTestService.getResultById(resultId);
        if (result == null) {
            return "redirect:/teacher/pending-reviews";
        }

        List<AutismTestAnswer> answers = autismTestService.getAnswersForResult(resultId);

        Map<String, List<AutismTestAnswer>> answersByCategory = autismTestService.groupAnswersByCategory(answers);

        model.addAttribute("result", result);
        model.addAttribute("answers", answers);
        model.addAttribute("answersByCategory", answersByCategory);
        model.addAttribute("student", result.getStudent());

        return "teacher/review-test";
    }

    @PostMapping("/teacher/review-test/{resultId}")
    public String submitReview(
            @PathVariable Long resultId,
            @RequestParam String notes,
            Principal principal) {

        String email = principal.getName();
        User teacher = userService.findByEmail(email);

        AutismTestResult result = autismTestService.getResultById(resultId);
        if (result == null) {
            return "redirect:/teacher/pending-reviews";
        }

        autismTestService.saveReview(resultId, teacher.getId(), notes);

        return "redirect:/teacher/student-results/" + result.getStudent().getId();
    }

    @GetMapping("/teacher/manage-tests")
    public String showManageTests(Model model, Principal principal) {
        String email = principal.getName();
        User teacher = userService.findByEmail(email);

        List<AutismTest> tests = autismTestService.getTestsByCreator(teacher);
        model.addAttribute("tests", tests);

        return "teacher/manage-tests";
    }

    @GetMapping("/teacher/create-test")
    public String showCreateTestForm(Model model, Principal principal) {
        String email = principal.getName();
        User teacher = userService.findByEmail(email);

        return "teacher/create-test";
    }

    @PostMapping("/teacher/create-test")
    public String createTest(
            @RequestParam String name,
            @RequestParam String description,
            @RequestParam Integer minAge,
            @RequestParam Integer maxAge,
            Principal principal) {

        String email = principal.getName();
        User teacher = userService.findByEmail(email);

        AutismTest test = autismTestService.createTest(name, description, minAge, maxAge, teacher);

        autismTestService.addPredefinedQuestionsToTest(test.getId());

        return "redirect:/teacher/manage-tests";
    }

    @PostMapping("/teacher/test/{id}/activate")
    public String activateTest(@PathVariable Long id) {
        autismTestService.updateTestStatus(id, AutismTest.TestStatus.ACTIVE);
        return "redirect:/teacher/manage-tests";
    }

    @PostMapping("/teacher/test/{id}/deactivate")
    public String deactivateTest(@PathVariable Long id) {
        autismTestService.updateTestStatus(id, AutismTest.TestStatus.INACTIVE);
        return "redirect:/teacher/manage-tests";
    }

    @PostMapping("/teacher/test/{id}/delete")
    public String deleteTest(@PathVariable Long id) {
        autismTestService.deleteTest(id);
        return "redirect:/teacher/manage-tests";
    }

    // Messaging API
    @GetMapping("/chat/{userId}")
    public String showChatPage(@PathVariable Long userId, Model model, Principal principal) {
        String email = principal.getName();
        User currentUser = userService.findByEmail(email);
        User chatPartner = userService.findById(userId);

        if (chatPartner == null) {
            return "redirect:/";
        }

        List<MessageDTO> messages = messageService.getMessagesBetweenUsers(currentUser.getId(), userId);

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("chatPartner", chatPartner);
        model.addAttribute("messages", messages);

        return "chat/chat-page";
    }

    @PostMapping("/api/send-message")
    @ResponseBody
    public ResponseEntity<?> sendMessage(@RequestBody MessageDTO message, Principal principal) {
        String email = principal.getName();
        User sender = userService.findByEmail(email);

        if (!sender.getId().equals(message.getSenderId())) {
            return ResponseEntity.badRequest().body("Unauthorized sender");
        }

        messageService.saveMessage(message);

        // Send via WebSocket
        messageService.sendMessageViaWebSocket(message);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/messages/{userId}")
    @ResponseBody
    public List<MessageDTO> getMessages(@PathVariable Long userId, Principal principal) {
        String email = principal.getName();
        User currentUser = userService.findByEmail(email);

        return messageService.getMessagesBetweenUsers(currentUser.getId(), userId);
    }
}
