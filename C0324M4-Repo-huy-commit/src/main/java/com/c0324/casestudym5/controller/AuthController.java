package com.c0324.casestudym5.controller;

import com.c0324.casestudym5.dto.ChildRegistrationDTO;
import com.c0324.casestudym5.model.Role;
import com.c0324.casestudym5.model.User;
import com.c0324.casestudym5.service.RoleService;
import com.c0324.casestudym5.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserService userService;
    private final RoleService roleService;
    private final BCryptPasswordEncoder passwordEncoder;

    @Autowired
    public AuthController(UserService userService, RoleService roleService, BCryptPasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.roleService = roleService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                          @RequestParam(value = "logout", required = false) String logout,
                          HttpServletRequest request,
                          Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            return "redirect:/";
        }

        HttpSession session = request.getSession();
        if (error != null) {
            model.addAttribute("error", true);
            String email = (String) session.getAttribute("email");
            if (email != null) {
                model.addAttribute("email", email);
            }
        }

        if (logout != null) {
            model.addAttribute("success", "You have been logged out successfully.");
        }

        model.addAttribute("registrationDTO", new ChildRegistrationDTO());
        return "common/auth-form";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registrationDTO") ChildRegistrationDTO registrationDTO,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("registrationDTO", registrationDTO);
            return "common/auth-form";
        }

        try {
            // Check if email already exists
            if (userService.findByEmail(registrationDTO.getEmail()) != null) {
                model.addAttribute("error", "Email already exists!");
                model.addAttribute("registrationDTO", registrationDTO);
                return "common/auth-form";
            }

            // Create new user
            User user = new User();
            user.setName(registrationDTO.getName());
            user.setEmail(registrationDTO.getEmail());
            user.setPassword(passwordEncoder.encode(registrationDTO.getPassword()));
            user.setDob(registrationDTO.getDob());
            user.setGender(User.Gender.valueOf(registrationDTO.getGender()));
            user.setPhoneNumber(registrationDTO.getPhoneNumber());
            user.setAddress(registrationDTO.getAddress());

            // Set role as STUDENT
            Role studentRole = roleService.findByName("ROLE_STUDENT");
            user.getRoles().add(studentRole);

            // Save user
            userService.save(user);

            redirectAttributes.addFlashAttribute("success", "Registration successful! Please login.");
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("error", "Registration failed: " + e.getMessage());
            model.addAttribute("registrationDTO", registrationDTO);
            return "common/auth-form";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication, RedirectAttributes redirectAttributes) {
        if (authentication != null) {
            new SecurityContextLogoutHandler().logout(request, response, authentication);
            redirectAttributes.addFlashAttribute("success", "You have been logged out successfully.");
        }
        return "redirect:/login";
    }
}
