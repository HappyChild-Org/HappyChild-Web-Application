package com.c0324.casestudym5.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.c0324.casestudym5.model.Blogs;
import com.c0324.casestudym5.model.Teacher;
import com.c0324.casestudym5.model.Topic;
import com.c0324.casestudym5.model.User;
import com.c0324.casestudym5.service.BlogsService;
import com.c0324.casestudym5.service.NotificationService;
import com.c0324.casestudym5.service.TeacherService;
import com.c0324.casestudym5.service.TopicService;
import com.c0324.casestudym5.service.UserService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class HomeController {
    private final UserService userService;
    private final NotificationService notificationService;
    private final TopicService topicService;
    private final BlogsService blogsService;
    private final TeacherService teacherService;

    @Autowired
    public HomeController(UserService userService, NotificationService notificationService,
                          TopicService topicService, BlogsService blogsService, TeacherService teacherService) {
        this.userService = userService;
        this.notificationService = notificationService;
        this.topicService = topicService;
        this.blogsService = blogsService;
        this.teacherService = teacherService;
    }

    @GetMapping(value = {"/", "/home"})
    public String homePage(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            System.out.println("Authenticated user: " + authentication.getName());
            System.out.println("Authorities: " + authentication.getAuthorities());
        }
        Page<Teacher> list = teacherService.getTeachersPage(1,4);
        List<Topic> latestTopics = topicService.getLatestTopics(3);
        List<Blogs> latestBlogs = blogsService.getLatestBlogs(5);

        model.addAttribute("topics", latestTopics);
        model.addAttribute("blogs", latestBlogs);
        model.addAttribute("list", list);

        return "common/home-page";
    }

    @GetMapping("/mark-read")
    public ResponseEntity<?> markNotificationsAsRead(HttpServletRequest request) {
        String email = request.getUserPrincipal().getName();
        User currentUser = userService.findByEmail(email);
        notificationService.markAllAsRead(currentUser.getId());
        return ResponseEntity.ok().build();
    }
}
