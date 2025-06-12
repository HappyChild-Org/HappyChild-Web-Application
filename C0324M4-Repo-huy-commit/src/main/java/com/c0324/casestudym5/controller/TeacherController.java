package com.c0324.casestudym5.controller;

import com.c0324.casestudym5.dto.DocumentDTO;
import com.c0324.casestudym5.dto.StudentSearchDTO;
import com.c0324.casestudym5.dto.TeamDTO;
import com.c0324.casestudym5.model.*;
import com.c0324.casestudym5.service.*;
import com.c0324.casestudym5.service.impl.ClazzService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/teacher")
public class TeacherController {

    private final TeacherService teacherService;
    private final TeamService teamService;
    private final UserService userService;
    private final TopicService topicService;
    private final StudentService studentService;
    private final ClazzService classService;
    private final DocumentService documentService;
    private final FirebaseService firebaseService;
    private final SimpMessagingTemplate messagingTemplate;
    private final StudentTeacherService studentTeacherService;
    private final MessageService messageService;
    private final TestResultService testResultService;
    private final NotificationService notificationService;


    @Autowired
    public TeacherController(TeacherService teacherService, TeamService teamService, UserService userService, TopicService topicService, StudentService studentService, ClazzService classService, DocumentService documentService, FirebaseService firebaseService, SimpMessagingTemplate messagingTemplate, StudentTeacherService studentTeacherService, MessageService messageService, TestResultService testResultService, NotificationService notificationService) {
        this.teacherService = teacherService;
        this.teamService = teamService;
        this.userService = userService;
        this.topicService = topicService;
        this.studentService = studentService;
        this.classService = classService;
        this.documentService = documentService;
        this.firebaseService = firebaseService;
        this.messagingTemplate = messagingTemplate;
        this.studentTeacherService = studentTeacherService;
        this.messageService = messageService;
        this.testResultService = testResultService;
        this.notificationService = notificationService;
    }
    
    private Teacher getCurrentTeacher() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return teacherService.getTeacherByEmail(email);
    }

    @GetMapping("/detail/{id}")
    public String getTeacher(@PathVariable Long id, Model model) {
        Optional<Teacher> teacher = teacherService.getTeacherById(id);
        if (teacher.isPresent()) {
            model.addAttribute("teacher", teacher.get());
            return "admin/teacher/teacher-details";
        } else {
            return "common/404";
        }
    }

    @PostMapping("/change-avatar")
    public String showChangeAvatarForm(@RequestParam("avatar") MultipartFile avatar, Model model) {
        String fileName = avatar.getOriginalFilename();
        long fileSize = avatar.getSize();
        long maxFileSize = 5 * 1024 * 1024; // 5MB

        if (fileName != null && (fileName.endsWith(".jpg") || fileName.endsWith(".png") || fileName.endsWith(".jpeg"))) {
            if (fileSize <= maxFileSize) {
                userService.changeAvatar(avatar);
            } else {
                model.addAttribute("imageError", "Kích thước ảnh không được vượt quá 5MB");
            }
        } else {
            model.addAttribute("imageError", "Chỉ hỗ trợ ảnh có định dạng jpg, jpeg, png");
        }

        return "admin/teacher/teacher-create";
    }

    @GetMapping("/team")
    public String showTeamPage(@RequestParam(name = "name", defaultValue = "", required = false) String keyword,
                               @RequestParam(name = "page", defaultValue = "0") int page,
                               Model model, Principal principal) {
        User currentUser = userService.findByEmail(principal.getName());
        Page<TeamDTO> teamPage = teamService.getPageTeams(page, keyword, currentUser);
        model.addAttribute("teams", teamPage.getContent());
        model.addAttribute("totalPages", teamPage.getTotalPages());
        model.addAttribute("currentPage", page);
        model.addAttribute("keyword", keyword);
        return "/teacher/team-list";
    }

    @MessageMapping("/delete-team")
    public String handleNotification(@Payload Map<String, Object> payload, Principal principal) {
        Long teamId = Long.parseLong(payload.get("teamId").toString());
        Team team = teamService.findById(teamId);
        User sender = userService.findByEmail(principal.getName());
        teamService.deleteTeam(teamId, sender);

        messagingTemplate.convertAndSend("/socket/notification", "Đã xóa nhóm " + team.getName() + " thành công");
        return "redirect:/teacher/team";
    }


    @MessageMapping("/set-deadline")
    public String setDeadline(@Payload Map<String, Object> payload, Principal principal) throws ParseException {
        Long teamId = Long.parseLong(payload.get("teamId").toString());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date newDeadline = sdf.parse(payload.get("deadline").toString());
        User setBy = userService.findByEmail(principal.getName());
        topicService.setNewDeadline(teamId, newDeadline, setBy);

        messagingTemplate.convertAndSend("/socket/notification", "Hạn chót nộp dự án đã được cập nhật");
        return "redirect:/teacher/team";
    }


    @GetMapping("/topics")
    public String getPendingTopics(Model model,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "6") int size, Principal principal) {
        String email = principal.getName();
        Teacher currentTeacher = teacherService.getTeacherByEmail(email);
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Topic> topicPage = topicService.getTopicByTeacher(currentTeacher.getId(),pageRequest);

        model.addAttribute("topics", topicPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", topicPage.getTotalPages());
        return "teacher/topic-approval";
    }

    @PostMapping("/topics/{id}/approve")
    public String approveTopic(@PathVariable Long id) {
        topicService.approveTopic(id);
        return "redirect:/teacher/topics";
    }

    @PostMapping("/topics/{id}/reject")
    public String rejectTopic(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String reason = body.get("reason");
        topicService.rejectTopic(id, reason);
        return "redirect:/teacher/topics";
    }

    @GetMapping("/student-list")
    public String viewStudentList(Model model) {
        Teacher currentTeacher = getCurrentTeacher();
        List<StudentTeacher> registrations = studentTeacherService.findByTeacher(currentTeacher);

        // Lấy tin nhắn chưa đọc
        Map<Long, Long> unreadCounts = new HashMap<>();
        for (StudentTeacher registration : registrations) {
            long count = messageService.countUnreadMessages(registration.getStudent().getUser().getId());
            unreadCounts.put(registration.getStudent().getUser().getId(), count);
        }

        // Giả định có TestResultService để lấy kết quả kiểm tra
        Map<Long, List<TestResult>> testResults = registrations.stream()
                .collect(Collectors.toMap(
                        reg -> reg.getStudent().getId(),
                        reg -> testResultService.findByStudentId(reg.getStudent().getId())
                ));

        model.addAttribute("registrations", registrations);
        model.addAttribute("unreadCounts", unreadCounts);
        model.addAttribute("testResults", testResults);
        model.addAttribute("currentUser", currentTeacher.getUser());
        return "teacher/student-list";
    }
    
    @GetMapping("/student/{id}")
    public String viewStudentDetail(@PathVariable Long id, Model model) {
        Student student = studentService.getStudent(id);
        model.addAttribute("student", student);
        return "teacher/student-detail";
    }

    @GetMapping("/test-results/{id}")
    public String viewTestResults(@PathVariable Long id, Model model) {
        Student student = studentService.getStudent(id);
        List<TestResult> results = testResultService.findByStudentId(id);
        model.addAttribute("student", student);
        model.addAttribute("results", results);
        return "teacher/test-results";
    }

    @GetMapping("/documents/upload")
    public String showDocumentsPage(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "5") int size,
                                    Model model) {
        User currentUser = userService.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName());
        Teacher teacher = teacherService.getTeacherByEmail(currentUser.getEmail());
        Page<Document> documentPage = documentService.getDocumentsPage(page, size, teacher);
        model.addAttribute("documentDTO", new DocumentDTO());
        model.addAttribute("teachers", teacherService.getAllTeachers());
        model.addAttribute("documents", documentPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", documentPage.getTotalPages());
        model.addAttribute("totalItems", documentPage.getTotalElements());
        model.addAttribute("pageSize", size);
        return "teacher/documents";
    }


    @PostMapping("/documents/upload")
    public String uploadDocument(@Valid @ModelAttribute("documentDTO") DocumentDTO documentDTO,
                                 BindingResult bindingResult,
                                 Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("teachers", teacherService.getAllTeachers());
            model.addAttribute("documentDTO", documentDTO);
            model.addAttribute("error", "Vui lòng kiểm tra thông tin đã nhập!");
            return "teacher/documents";
        }


        User currentUser = userService.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName());
        Teacher teacher = teacherService.getTeacherByEmail(currentUser.getEmail());
        if(teacher == null){
            model.addAttribute("teachers", teacherService.getAllTeachers());
            model.addAttribute("documentDTO", documentDTO);
            model.addAttribute("notFoundError", "Không tìm thấy giáo viên");
            return "teacher/documents";
        }

        // Upload file lên Firebase
        String fileUrl = firebaseService.uploadFileToFireBase(documentDTO.getFileUrl(), "documents");
        if (fileUrl == null) {
            model.addAttribute("teachers", teacherService.getAllTeachers());
            model.addAttribute("documentDTO", documentDTO);
            model.addAttribute("error", "Lỗi khi tải lên tài liệu.");
            return "teacher/documents";
        }

        try {
            Document document = new Document();
            document.setName(documentDTO.getName());
            document.setDescription(documentDTO.getDescription());
            document.setTeacher(teacher);
            document.setStatus(documentDTO.isStatus());

            documentService.saveDocument(document, fileUrl);
            redirectAttributes.addFlashAttribute("toastMessage", "Tải tài liệu thành công!");
            redirectAttributes.addFlashAttribute("toastType", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("toastMessage", "Đã có lỗi trong quá trình.");
            redirectAttributes.addFlashAttribute("toastType", "danger");
            System.out.println(e.getMessage());
        }

        return "redirect:/teacher/documents/upload";
    }

    @PostMapping("/approve-registration/{id}")
    public String approveRegistration(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Optional<StudentTeacher> optionalStudentTeacher = studentTeacherService.findById(id);
            if (optionalStudentTeacher.isPresent()) {
                StudentTeacher studentTeacher = optionalStudentTeacher.get();
                studentTeacher.setStatus(StudentTeacher.Status.APPROVED);
                studentTeacherService.save(studentTeacher);

                // Send notification to student
                Notification notification = Notification.builder()
                        .sender(studentTeacher.getTeacher().getUser())
                        .receiver(studentTeacher.getStudent().getUser())
                        .content(String.format("Giáo viên %s đã phê duyệt yêu cầu tư vấn của bạn.", studentTeacher.getTeacher().getUser().getName()))
                        .url("/messages/chat/" + studentTeacher.getTeacher().getUser().getId())
                        .build();
                notificationService.sendNotification(notification);

                redirectAttributes.addFlashAttribute("successMessage", "Yêu cầu đã được phê duyệt và thông báo cho học sinh.");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy yêu cầu đăng ký.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi phê duyệt yêu cầu: " + e.getMessage());
        }
        return "redirect:/teacher/student-list";
    }
}
