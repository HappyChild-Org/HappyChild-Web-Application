package com.c0324.casestudym5.controller;

import java.security.Principal;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.c0324.casestudym5.dto.AutismTestForm;
import com.c0324.casestudym5.dto.ChildRegistrationDTO;
import com.c0324.casestudym5.dto.MessageDTO;
import com.c0324.casestudym5.model.AutismQuestion;
import com.c0324.casestudym5.model.AutismTest;
import com.c0324.casestudym5.model.AutismTestAnswer;
import com.c0324.casestudym5.model.AutismTestResult;
import com.c0324.casestudym5.model.Student;
import com.c0324.casestudym5.model.User;
import com.c0324.casestudym5.service.AutismTestService;
import com.c0324.casestudym5.service.MessageService;
import com.c0324.casestudym5.service.StudentService;
import com.c0324.casestudym5.service.StudentTestService;
import com.c0324.casestudym5.service.UserService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/autism-test")
public class AutismTestController {
    private final AutismTestService autismTestService;
    private final UserService userService;
    private final MessageService messageService;
    private final StudentService studentService;
    private final StudentTestService studentTestService;

    @Autowired
    public AutismTestController(AutismTestService autismTestService, UserService userService, MessageService messageService, StudentService studentService, StudentTestService studentTestService) {
        this.autismTestService = autismTestService;
        this.userService = userService;
        this.messageService = messageService;
        this.studentService = studentService;
        this.studentTestService = studentTestService;
    }

    @GetMapping("/tests")
    public String showAvailableTests(Model model, Principal principal) {
        try {
            String email = principal.getName();
            User user = userService.findByEmail(email);
            Student student = studentService.findStudentByUserId(user.getId());
            
            if (student == null) {
                return "redirect:/error";
            }
            
            // Calculate age from date of birth
            int age = calculateAge(student.getUser().getDob());
            
            List<AutismTest> tests = autismTestService.getTestsForChildAge(age);
            model.addAttribute("tests", tests);
            return "student/autism-tests";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading tests: " + e.getMessage());
            return "error";
        }
    }

    private int calculateAge(Date dob) {
        if (dob == null) {
            return 0;
        }
        Calendar dobCal = Calendar.getInstance();
        dobCal.setTime(dob);
        Calendar today = Calendar.getInstance();
        int age = today.get(Calendar.YEAR) - dobCal.get(Calendar.YEAR);
        if (today.get(Calendar.DAY_OF_YEAR) < dobCal.get(Calendar.DAY_OF_YEAR)) {
            age--;
        }
        return age;
    }

    @GetMapping("/test/{id}")
    public String showTest(@PathVariable Long id, Model model, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            String email = principal.getName();
            User user = userService.findByEmail(email);
            if (user == null) {
                redirectAttributes.addFlashAttribute("error", "User not found");
                return "redirect:/error";
            }

            Student student = studentService.findStudentByUserId(user.getId());
            if (student == null) {
                redirectAttributes.addFlashAttribute("error", "Student profile not found");
                return "redirect:/error";
            }
            
            // Check if student has already taken this test
            List<AutismTestResult> existingResults = autismTestService.getResultsForStudentAndTest(user.getId(), id);
            if (!existingResults.isEmpty()) {
                redirectAttributes.addFlashAttribute("message", "You have already taken this test");
                return "redirect:/autism-test/results";
            }
            
            AutismTest test = autismTestService.getTestById(id);
            if (test == null) {
                redirectAttributes.addFlashAttribute("error", "Test not found");
                return "redirect:/error";
            }

            if (test.getStatus() != AutismTest.TestStatus.ACTIVE) {
                redirectAttributes.addFlashAttribute("error", "This test is not available");
                return "redirect:/autism-test/tests";
            }
            
            List<AutismQuestion> questions = autismTestService.getQuestionsForTest(id);
            if (questions.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "No questions found for this test");
                return "redirect:/autism-test/tests";
            }
            
            Map<String, List<AutismQuestion>> questionsByCategory = autismTestService.groupQuestionsByCategory(questions);
            
            model.addAttribute("test", test);
            model.addAttribute("questions", questions);
            model.addAttribute("questionsByCategory", questionsByCategory);
            model.addAttribute("testForm", new AutismTestForm());
            
            return "student/take-test";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error loading test: " + e.getMessage());
            return "redirect:/error";
        }
    }

    @PostMapping("/test/{id}/submit")
    public String submitTest(@PathVariable Long id,
                           @Valid AutismTestForm testForm,
                           BindingResult bindingResult,
                           Principal principal,
                           Model model) {
        try {
            String email = principal.getName();
            User user = userService.findByEmail(email);
            Student student = studentService.findStudentByUserId(user.getId());
            
            if (student == null) {
                return "redirect:/error";
            }
            
            if (bindingResult.hasErrors()) {
                return "student/take-test";
            }
            
            testForm.setTestId(id);
            Long resultId = autismTestService.submitTest(user.getId(), testForm);
            return "redirect:/autism-test/results";
        } catch (Exception e) {
            model.addAttribute("error", "Error submitting test: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/results")
    public String showTestResults(Model model, Principal principal) {
        try {
            String email = principal.getName();
            User user = userService.findByEmail(email);
            Student student = studentService.findStudentByUserId(user.getId());
            
            if (student == null) {
                return "redirect:/error";
            }
            
            List<AutismTestResult> results = autismTestService.getResultsForStudent(user.getId());
            model.addAttribute("results", results);
            return "student/test-results";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading results: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/result/{id}")
    public String showTestResultDetail(@PathVariable Long id, Model model, Principal principal) {
        try {
            String email = principal.getName();
            User user = userService.findByEmail(email);
            Student student = studentService.findStudentByUserId(user.getId());
            
            if (student == null) {
                return "redirect:/error";
            }
            
            AutismTestResult result = autismTestService.getResultById(id);
            if (result == null || !result.getStudent().getId().equals(user.getId())) {
                return "redirect:/error";
            }
            
            List<AutismTestAnswer> answers = autismTestService.getAnswersForResult(id);
            Map<String, List<AutismTestAnswer>> answersByCategory = autismTestService.groupAnswersByCategory(answers);
            
            model.addAttribute("result", result);
            model.addAttribute("answersByCategory", answersByCategory);
            return "student/test-result-detail";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading result details: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/teacher/pending-reviews")
    public String showPendingReviews(Model model, Principal principal) {
        try {
            String email = principal.getName();
            User user = userService.findByEmail(email);
            
            List<User> studentsWithPendingResults = autismTestService.getStudentsWithPendingResults();
            model.addAttribute("students", studentsWithPendingResults);
            return "teacher/pending-reviews";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading pending reviews: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/teacher/student/{id}/pending-results")
    public String showStudentPendingResults(@PathVariable Long id, Model model, Principal principal) {
        try {
            String email = principal.getName();
            User user = userService.findByEmail(email);
            
            User student = userService.findById(id);
            if (student == null) {
                return "redirect:/error";
            }
            
            List<AutismTestResult> pendingResults = autismTestService.getResultsForStudent(id).stream()
                    .filter(result -> result.getStatus() == AutismTestResult.TestResultStatus.PENDING_REVIEW)
                    .collect(Collectors.toList());
            
            model.addAttribute("student", student);
            model.addAttribute("results", pendingResults);
            return "teacher/student-pending-results";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading student's pending results: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/teacher/manage-tests/{testId}/questions")
    public String showManageQuestions(@PathVariable Long testId, Model model) {
        AutismTest test = autismTestService.getTestById(testId);
        if (test == null) {
            return "redirect:/error"; // Or an appropriate error page
        }
        List<AutismQuestion> questions = autismTestService.getQuestionsForTest(testId);
        model.addAttribute("test", test);
        model.addAttribute("questions", questions);
        return "teacher/manage-questions";
    }

    @GetMapping("/teacher/manage-tests/{testId}/questions/create")
    public String showCreateQuestionForm(@PathVariable Long testId, Model model) {
        AutismTest test = autismTestService.getTestById(testId);
        if (test == null) {
            return "redirect:/error";
        }
        model.addAttribute("test", test);
        model.addAttribute("autismQuestion", new AutismQuestion());
        return "teacher/create-question";
    }

    @PostMapping("/teacher/manage-tests/{testId}/questions/create")
    public String createQuestion(@PathVariable Long testId,
                               @RequestParam String questionText,
                               @RequestParam String category,
                               @RequestParam Integer weightScore,
                               @RequestParam String answerType,
                               @RequestParam(required = false) String imageUrl,
                               @RequestParam(required = false) String options,
                               Model model) {
        try {
            autismTestService.addQuestionToTest(testId, questionText, category, weightScore, answerType, imageUrl, options);
            return "redirect:/autism-test/teacher/manage-tests/" + testId + "/questions";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            AutismTest test = autismTestService.getTestById(testId);
            model.addAttribute("test", test);
            model.addAttribute("autismQuestion", new AutismQuestion());
            return "teacher/create-question";
        } catch (Exception e) {
            model.addAttribute("error", "Error creating question: " + e.getMessage());
            AutismTest test = autismTestService.getTestById(testId);
            model.addAttribute("test", test);
            model.addAttribute("autismQuestion", new AutismQuestion());
            return "teacher/create-question";
        }
    }

    @GetMapping("/teacher/review-result/{id}")
    public String showReviewResult(@PathVariable Long id, Model model, Principal principal) {
        try {
            String email = principal.getName();
            User user = userService.findByEmail(email);
            
            AutismTestResult result = autismTestService.getResultById(id);
            if (result == null) {
                return "redirect:/error";
            }
            
            List<AutismTestAnswer> answers = autismTestService.getAnswersForResult(id);
            Map<String, List<AutismTestAnswer>> answersByCategory = autismTestService.groupAnswersByCategory(answers);
            
            model.addAttribute("result", result);
            model.addAttribute("answersByCategory", answersByCategory);
            return "teacher/review-result";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading result for review: " + e.getMessage());
            return "error";
        }
    }

    @PostMapping("/teacher/review-result/{id}")
    public String submitReview(@PathVariable Long id,
                             @RequestParam String notes,
                             Principal principal,
                             Model model) {
        try {
            String email = principal.getName();
            User user = userService.findByEmail(email);
            
            autismTestService.saveReview(id, user.getId(), notes);
            return "redirect:/autism-test/teacher/pending-reviews";
        } catch (Exception e) {
            model.addAttribute("error", "Error submitting review: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/teacher/manage-tests")
    public String showManageTests(Model model, Principal principal) {
        try {
            String email = principal.getName();
            User user = userService.findByEmail(email);
            
            List<AutismTest> tests = autismTestService.getTestsByCreator(user);
            model.addAttribute("tests", tests);
            return "teacher/manage-tests";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading tests: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/teacher/create-test")
    public String showCreateTestForm(Model model) {
        model.addAttribute("test", new AutismTest());
        return "teacher/create-test";
    }

    @PostMapping("/teacher/create-test")
    public String createTest(@RequestParam String name,
                             @RequestParam String description,
                             @RequestParam Integer minAge,
                             @RequestParam Integer maxAge,
                             Principal principal,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        try {
            String email = principal.getName();
            if (email == null) {
                throw new IllegalArgumentException("User not authenticated");
            }
            User user = userService.findByEmail(email);
            if (user == null) {
                throw new IllegalArgumentException("User not found");
            }
            AutismTest test = autismTestService.createTest(name, description, minAge, maxAge, user);
            autismTestService.addPredefinedQuestionsToTest(test.getId());
            redirectAttributes.addFlashAttribute("toastMessage", "Tạo bài kiểm tra thành công!");
            redirectAttributes.addFlashAttribute("toastType", "success");
            return "redirect:/autism-test/teacher/manage-tests";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("test", new AutismTest());
            return "teacher/create-test";
        } catch (Exception e) {
            model.addAttribute("error", "Error creating test: " + e.getMessage());
            return "error";
        }
    }

    @PostMapping("/teacher/test/{id}/activate")
    public String activateTest(@PathVariable Long id) {
        autismTestService.updateTestStatus(id, AutismTest.TestStatus.ACTIVE);
        return "redirect:/autism-test/teacher/manage-tests";
    }

    @PostMapping("/teacher/test/{id}/deactivate")
    public String deactivateTest(@PathVariable Long id) {
        autismTestService.updateTestStatus(id, AutismTest.TestStatus.INACTIVE);
        return "redirect:/autism-test/teacher/manage-tests";
    }

    @PostMapping("/teacher/test/{id}/delete")
    public String deleteTest(@PathVariable Long id) {
        autismTestService.deleteTest(id);
        return "redirect:/autism-test/teacher/manage-tests";
    }

    @GetMapping("/teacher/student-list")
    public String showStudentList(Model model, Principal principal) {
        try {
            String email = principal.getName();
            User teacher = userService.findByEmail(email);
            
            List<Student> students = studentService.findAll();
            model.addAttribute("students", students);
            return "teacher/student-list";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading student list: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/profile")
    public String showStudentProfile(Model model, Principal principal) {
        try {
            String email = principal.getName();
            User user = userService.findByEmail(email);
            Student student = studentService.findStudentByUserId(user.getId());
            
            if (student == null) {
                return "redirect:/error";
            }
            
            model.addAttribute("student", student);
            return "student/profile";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading profile: " + e.getMessage());
            return "error";
        }
    }

//    // Messaging API
//    @GetMapping("/chat/{userId}")
//    public String showChatPage(@PathVariable Long userId, Model model, Principal principal) {
//        String email = principal.getName();
//        User currentUser = userService.findByEmail(email);
//        User chatPartner = userService.findById(userId);
//
//        if (chatPartner == null) {
//            return "redirect:/";
//        }
//
//        List<MessageDTO> messages = messageService.getMessagesBetweenUsers(currentUser.getId(), userId);
//
//        model.addAttribute("currentUser", currentUser);
//        model.addAttribute("chatPartner", chatPartner);
//        model.addAttribute("messages", messages);
//
//        return "chat/chat-page";
//    }

//    @PostMapping("/api/send-message")
//    @ResponseBody
//    public ResponseEntity<?> sendMessage(@RequestBody MessageDTO message, Principal principal) {
//        String email = principal.getName();
//        User sender = userService.findByEmail(email);
//
//        if (!sender.getId().equals(message.getSenderId())) {
//            return ResponseEntity.badRequest().body("Unauthorized sender");
//        }
//
//        messageService.saveMessage(message);
//
//        // Send via WebSocket
//        messageService.sendMessageViaWebSocket(message);
//
//        return ResponseEntity.ok().build();
//    }

//    @GetMapping("/api/messages/{userId}")
//    @ResponseBody
//    public List<MessageDTO> getMessages(@PathVariable Long userId, Principal principal) {
//        String email = principal.getName();
//        User currentUser = userService.findByEmail(email);
//
//        return messageService.getMessagesBetweenUsers(currentUser.getId(), userId);
//    }
//
//    @GetMapping("/student/register-child")
//    public String showRegistrationForm(Model model) {
//        model.addAttribute("childRegistrationDTO", new ChildRegistrationDTO());
//        return "student/register-child";
//    }

    @PostMapping("/student/register-child")
    public String registerChild(@Valid ChildRegistrationDTO registrationDTO, 
                              BindingResult bindingResult,
                              Principal principal,
                              Model model) {
        if (bindingResult.hasErrors()) {
            return "student/register-child";
        }

        try {
            String email = principal.getName();
            User user = userService.findByEmail(email);
            
            // Create new user for the child
            User childUser = new User();
            childUser.setName(registrationDTO.getName());
            childUser.setDob(registrationDTO.getDob());
            childUser.setGender(User.Gender.valueOf(registrationDTO.getGender()));
            childUser.setPhoneNumber(registrationDTO.getPhoneNumber());
            childUser.setAddress(registrationDTO.getAddress());
            
            // Set a temporary password (can be changed later)
            childUser.setPassword("temp123");
            
            // Save the child user
            userService.save(childUser);
            
            // Create student record
            Student student = new Student();
            student.setUser(childUser);
            student.setCode(generateStudentCode());
            student.setLeader(false);
            
            // Save the student
            studentService.save(student);
            
            return "redirect:/autism-test/tests";
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi khi đăng ký: " + e.getMessage());
            return "student/register-child";
        }
    }

    private String generateStudentCode() {
        // Generate a unique student code (you can implement your own logic)
        return "STU" + System.currentTimeMillis();
    }

    // Hiển thị danh sách câu hỏi chung để chọn
    @GetMapping("/teacher/manage-tests/{testId}/select-common-questions")
    public String showSelectCommonQuestions(@PathVariable Long testId, Model model) {
        List<AutismQuestion> commonQuestions = autismTestService.getCommonQuestions();
        model.addAttribute("commonQuestions", commonQuestions);
        model.addAttribute("testId", testId);
        return "teacher/select-common-questions";
    }

    // Xử lý thêm các câu hỏi đã chọn vào test
    @PostMapping("/teacher/manage-tests/{testId}/add-questions")
    public String addQuestionsToTest(@PathVariable Long testId, @RequestParam(name = "questionIds", required = false) List<Long> questionIds, RedirectAttributes redirectAttributes) {
        if (questionIds != null && !questionIds.isEmpty()) {
            autismTestService.addQuestionsToTest(testId, questionIds);
            redirectAttributes.addFlashAttribute("toastMessage", "Đã thêm câu hỏi vào bài test!");
            redirectAttributes.addFlashAttribute("toastType", "success");
        } else {
            redirectAttributes.addFlashAttribute("toastMessage", "Bạn chưa chọn câu hỏi nào!");
            redirectAttributes.addFlashAttribute("toastType", "warning");
        }
        return "redirect:/autism-test/teacher/manage-tests/" + testId + "/questions";
    }
}
