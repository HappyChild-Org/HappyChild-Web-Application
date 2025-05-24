//package com.c0324.casestudym5.controller;
//
//import com.c0324.casestudym5.dto.QuizForm;
//import com.c0324.casestudym5.model.Question;
//import com.c0324.casestudym5.model.QuizResult;
//import com.c0324.casestudym5.model.User;
//import com.c0324.casestudym5.service.QuizService;
//import com.c0324.casestudym5.service.UserService;
//import jakarta.validation.Valid;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.validation.BindingResult;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//
//import java.security.Principal;
//import java.util.List;
//import java.util.Map;
//
//@Controller
//public class QuizController {
//    private final QuizService quizService;
//    private final UserService userService;
//
//    @Autowired
//    public QuizController(QuizService quizService, UserService userService) {
//        this.quizService = quizService;
//        this.userService = userService;
//    }
//
//    @GetMapping("/quiz")
//    public String showQuizForm(Model model, Principal principal) {
//        String email = principal.getName();
//        User user = userService.findByEmail(email);
//        List<Question> questions = quizService.getQuestions();
//        model.addAttribute("questions", questions);
//        model.addAttribute("quizForm", new QuizForm());
//        model.addAttribute("user", user);
//        return "index";
//    }
//
//    @PostMapping("/quiz")
//    public String submitQuiz(@Valid QuizForm quizForm, BindingResult result, Model model, Principal principal) {
//        if (result.hasErrors()) {
//            List<Question> questions = quizService.getQuestions();
//            model.addAttribute("questions", questions);
//            return "index";
//        }
//        String email = principal.getName();
//        User user = userService.findByEmail(email);
//        int score = quizService.calculateScore(quizForm);
//        Long quizResultId = quizService.saveResult(user.getId(), score, quizForm.getAnswers());
//        return "redirect:/quiz/results?score=" + score;
//    }
//
//    @GetMapping("/quiz/results")
//    public String showResults(@RequestParam(required = false) Integer score, Model model, Principal principal) {
//        String email = principal.getName();
//        User user = userService.findByEmail(email);
//        List<QuizResult> results = quizService.getResults(user.getId());
//        model.addAttribute("results", results);
//        model.addAttribute("score", score);
//        return "results";
//    }
//
//    @GetMapping("/teacher/quiz-results")
//    public String showTeacherResults(@RequestParam(required = false) Long studentId, Model model) {
//        List<User> students = userService.findAllByRole("STUDENT");
//        model.addAttribute("students", students);
//        if (studentId != null) {
//            List<QuizResult> results = quizService.getResults(studentId);
//            Map<Long, List<QuizService.QuizAnswerDetail>> answerDetails = quizService.getAnswerDetails(studentId);
//            model.addAttribute("results", results);
//            model.addAttribute("answerDetails", answerDetails);
//            model.addAttribute("selectedStudentId", studentId);
//        }
//        return "teacher-results";
//    }
//}
