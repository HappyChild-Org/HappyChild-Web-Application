package com.c0324.casestudym5.controller;

import com.c0324.casestudym5.dto.*;
import com.c0324.casestudym5.model.*;
import com.c0324.casestudym5.repository.StudentTeacherRepository;
import com.c0324.casestudym5.service.*;
import com.c0324.casestudym5.util.AppConstants;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.*;     

@Controller
@RequestMapping("/student")
public class StudentController {
    private final StudentService studentService;
    private final UserService userService;
    private final TeamService teamService;
    private final TopicService topicService;
    private final InvitationService invitationService;
    private final TeacherService teacherService;
    private final DocumentService documentService;
    private final StudentTeacherRepository studentTeacherRepository;
    private final NotificationService notificationService;

    @Autowired
    public StudentController(StudentService studentService, UserService userService, TeamService teamService, TopicService topicService, InvitationService invitationService, TeacherService teacherService, DocumentService documentService, StudentTeacherRepository studentTeacherRepository, NotificationService notificationService) {
        this.studentService = studentService;
        this.userService = userService;
        this.teamService = teamService;
        this.topicService = topicService;
        this.invitationService = invitationService;
        this.teacherService = teacherService;
        this.documentService = documentService;
        this.studentTeacherRepository = studentTeacherRepository;
        this.notificationService = notificationService;
    }

    @InitBinder
    public void initBinder(WebDataBinder dataBinder) {
        StringTrimmerEditor stringTrimmerEditor = new StringTrimmerEditor(true);
        dataBinder.registerCustomEditor(String.class, stringTrimmerEditor);
    }

    @GetMapping("/{id}")
    public String view(@PathVariable("id") Long id, Model model, HttpSession httpSession) {
        Student student = studentService.getStudent(id);
        model.addAttribute("student", student);
        model.addAttribute("pageTitle", student.getUser().getName());
        model.addAttribute("page", httpSession.getAttribute("page"));
        return "admin/student/student-details";
    }

    private Student getCurrentStudent() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();
        User currentUser = userService.findByEmail(userEmail);
        return studentService.findStudentByUserId(currentUser.getId());
    }

    @GetMapping("/menu")
    public String studentMenu() {
        Student currentStudent = getCurrentStudent();
        if (currentStudent.getTeam() != null) {
            return "redirect:/student/info-team";
        }
        return "redirect:/student/team";
    }

    @GetMapping("/student-details/{id}")
    @ResponseBody
    public ResponseEntity<InvitedStudentDTO> getStudentById(@PathVariable Long id) {
        InvitedStudentDTO studentDTO = studentService.getStudentDTOById(id);
        if (studentDTO != null) {
            return ResponseEntity.ok(studentDTO);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/team")
    public String formRegisterTeam(@RequestParam(name = "page", required = false, defaultValue = "1") int page,
                                   @RequestParam(name = "search", required = false, defaultValue = "") String search,
                                   Model model) {
        Student currentStudent = getCurrentStudent();
        Team currentTeam = currentStudent.getTeam();

        if (!model.containsAttribute("team")) {
            model.addAttribute("team", new TeamDTO());
        }

        if (currentTeam != null && !currentStudent.isLeader()) {
            return "common/404";
        }

        Page<Student> availableStudents = studentService.getAvailableStudents(page, search, currentStudent.getId());
        List<Invitation> invitation = invitationService.findByStudent(currentStudent);

        model.addAttribute("search", search);
        model.addAttribute("currentPage", page);
        model.addAttribute("invitation", invitation);
        model.addAttribute("list", availableStudents);
        model.addAttribute("currentTeam", currentTeam);
        model.addAttribute("currentStudent", currentStudent);
        model.addAttribute("invitationService", invitationService);
        model.addAttribute("totalPages", availableStudents.getTotalPages());

        return "team/team-register";
    }

    @PostMapping("/create-team")
    public String createTeam(@ModelAttribute("team") @Valid TeamDTO teamDTO,
                             BindingResult bindingResult,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.team", bindingResult);
            redirectAttributes.addFlashAttribute("team", teamDTO);
            return "redirect:/student/team";
        }
        if (teamService.existsByName(teamDTO.getName())) {
            bindingResult.rejectValue("name", "", "Tên nhóm đã tồn tại, vui lòng nhập tên khác!");
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.team", bindingResult);
            redirectAttributes.addFlashAttribute("team", teamDTO);
            return "redirect:/student/team";
        }
        Student currentStudent = getCurrentStudent();

        List<Invitation> pendingInvitations = invitationService.findByStudent(currentStudent);
        if (!pendingInvitations.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn còn lời mời tham gia nhóm chưa xử lý!");
            return "redirect:/student/team";
        }
        teamService.createNewTeam(teamDTO, currentStudent);
        redirectAttributes.addFlashAttribute("successMessage", "Nhóm đã được tạo thành công!");
        return "redirect:/student/info-team";
    }

    @PostMapping("/invite-team")
    public String inviteStudent(@RequestParam Long studentId, RedirectAttributes redirectAttributes) {
        Student currentStudent = getCurrentStudent();
        Team currentTeam = currentStudent.getTeam();

        // Kiểm tra quyền nhóm trưởng
        if (!currentStudent.isLeader()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Chỉ nhóm trưởng mới có thể mời thành viên!");
            return "redirect:/student/team";
        }

        // Kiểm tra nhóm tồn tại
        if (currentTeam == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn cần tạo nhóm trước khi mời thành viên!");
            return "redirect:/student/team";
        }

        // Kiểm tra giới hạn 3 thành viên
        if (currentTeam.getStudents().size() >= 3) {
            redirectAttributes.addFlashAttribute("errorMessage", "Nhóm đã đủ 3 thành viên!");
            return "redirect:/student/team";
        }

        try {
            invitationService.inviteStudent(studentId, currentStudent, currentTeam);
            redirectAttributes.addFlashAttribute("successMessage", "Lời mời đã được gửi thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Đã xảy ra lỗi khi gửi lời mời: " + e.getMessage());
        }
        return "redirect:/student/team";
    }

    @PostMapping("/invitation/handle")
    public String handleInvitation(@RequestParam Long invitationId, @RequestParam boolean accept, RedirectAttributes redirectAttributes) {
        String result = invitationService.handleInvitation(invitationId, accept);

        if ("success".equals(result)) {
            redirectAttributes.addFlashAttribute("successMessage", "Bạn đã tham gia nhóm thành công!");
            return "redirect:/student/info-team";
        } else if ("full".equals(result)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Nhóm đã đủ 3 thành viên!");
            redirectAttributes.addFlashAttribute("errorType", "full");
        } else if ("declined".equals(result)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn đã từ chối lời mời!");
            redirectAttributes.addFlashAttribute("errorType", "declined");
        }
        return "redirect:/student/team";
    }

    @GetMapping("/info-team")
    public String teamInfo(Model model, Pageable pageable) {
        Student currentStudent = getCurrentStudent();
        Team team = currentStudent.getTeam();
        if (team == null) {
            return "common/404";
        }
        Page<Student> availableStudents = studentService.findAllExceptCurrentStudent(currentStudent.getId(), pageable);

        model.addAttribute("team", team);
        model.addAttribute("student", currentStudent);
        model.addAttribute("list", availableStudents);
        return "team/team-info";
    }

    @GetMapping("/register-topic")
    public String showRegisterTopicForm(Model model,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "10") int size,
                                        RedirectAttributes redirectAttributes) {
        Student currentStudent = getCurrentStudent();
        Team currentTeam = currentStudent.getTeam();
        if (currentTeam == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn chưa có nhóm");
            return "redirect:/student/info-team";
        }
        if (currentTeam.getTopic() != null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Nhóm đã có đề tài hoặc đang đợi phê duyệt");
            return "redirect:/student/info-team";
        }
        if (currentTeam.getTeacher() == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng đăng ký giáo viên trước khi đăng ký đề tài!");
            return "redirect:/student/info-team";
        }
        if (!model.containsAttribute("registerTopic")) {
            model.addAttribute("registerTopic", new RegisterTopicDTO());
        }
        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Topic> topicPage = topicService.findByStatus(1, pageable);
        model.addAttribute("topics", topicPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", topicPage.getTotalPages());
        return "student/register-topic";
    }

    @PostMapping("/handle-register-topic")
    public String registerTopic(@Valid @ModelAttribute RegisterTopicDTO registerTopicDTO, BindingResult result, Principal principal, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.registerTopic", result);
            redirectAttributes.addFlashAttribute("registerTopic", registerTopicDTO);
            return "redirect:/student/register-topic";
        }

        MultipartFile image = registerTopicDTO.getImage();
        MultipartFile description = registerTopicDTO.getDescription();
        long maxFileSizeImage = 5 * 1024 * 1024; // 5MB
        long maxFileSizeDescription = 15 * 1024 * 1024; // 15MB

        if (image != null && !image.isEmpty()) {
            String imageName = image.getOriginalFilename();
            long imageSize = image.getSize();
            if (imageName != null && (imageName.endsWith(".jpg") || imageName.endsWith(".png") || imageName.endsWith(".jpeg"))) {
                if (imageSize > maxFileSizeImage) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Kích thước ảnh không được vượt quá 5MB");
                    redirectAttributes.addFlashAttribute("registerTopic", registerTopicDTO);
                    return "redirect:/student/register-topic";
                }
            } else {
                redirectAttributes.addFlashAttribute("imageError", "Chỉ hỗ trợ ảnh có định dạng jpg, jpeg, png");
                redirectAttributes.addFlashAttribute("registerTopic", registerTopicDTO);
                return "redirect:/student/register-topic";
            }
        }

        if (description != null && !description.isEmpty()) {
            String descriptionName = description.getOriginalFilename();
            long descriptionSize = description.getSize();
            if (descriptionName != null && (descriptionName.endsWith(".xls") || descriptionName.endsWith(".xlsx") || descriptionName.endsWith(".doc") || descriptionName.endsWith(".docx") || descriptionName.endsWith(".ppt") || descriptionName.endsWith(".pptx"))) {
                if (descriptionSize > maxFileSizeDescription) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Kích thước tệp mô tả không được vượt quá 15MB");
                    redirectAttributes.addFlashAttribute("registerTopic", registerTopicDTO);
                    return "redirect:/student/register-topic";
                }
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Chỉ hỗ trợ tệp có định dạng xls, xlsx, doc, docx, ppt, pptx");
                redirectAttributes.addFlashAttribute("registerTopic", registerTopicDTO);
                return "redirect:/student/register-topic";
            }
        }

        boolean isRegistered = topicService.registerTopic(registerTopicDTO, principal.getName());
        if (!isRegistered) {
            redirectAttributes.addFlashAttribute("errorMessage", "Đăng ký đề tài thất bại");
            redirectAttributes.addFlashAttribute("registerTopic", registerTopicDTO);
            return "redirect:/student/register-topic";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Đăng ký đề tài thành công");
        return "redirect:/student/info-team";
    }

    @GetMapping("/progress")
    public String redirectToProgress() {
        Student currentStudent = getCurrentStudent();
        Team currentTeam = currentStudent.getTeam();
        if (currentTeam == null) {
            return "common/404";
        }
        if (currentTeam.getTopic() != null && currentTeam.getTopic().getApproved() == AppConstants.TOPIC_APPROVED) {
            Long topicId = currentTeam.getTopic().getId();
            return "redirect:/progress/" + topicId;
        }
        return "redirect:/student/info-team";
    }

    @GetMapping("/list-teacher")
    public String getAllTeachers(@RequestParam(defaultValue = "0") int page, Model model) {
        int pageSize = 5;
        Pageable pageable = PageRequest.of(page, pageSize);

        Page<Teacher> teacherPage = teacherService.findAll(pageable);

        if (teacherPage == null || teacherPage.getContent().isEmpty()) {
            model.addAttribute("teachers", new ArrayList<>());
            model.addAttribute("teacherTeamCount", new HashMap<Long, Integer>());
            model.addAttribute("teacherIndividualStudentCount", new HashMap<Long, Long>());
            model.addAttribute("totalPages", 0);
            model.addAttribute("pageNumber", 0);
            model.addAttribute("errorMessage", "Không tìm thấy giáo viên.");
            return "student/register-teacher";
        }

        List<Teacher> teachers = teacherPage.getContent();
        Map<Long, Integer> teacherTeamCount = new HashMap<>();
        Map<Long, Long> teacherIndividualStudentCount = new HashMap<>();

        for (Teacher teacher : teachers) {
            int teamCount = teamService.countTeamsByTeacherId(teacher.getId());
            teacherTeamCount.put(teacher.getId(), teamCount);

            long individualStudentCount = studentTeacherRepository.countByTeacherAndStatus(teacher, StudentTeacher.Status.APPROVED);
            teacherIndividualStudentCount.put(teacher.getId(), individualStudentCount);
        }

        List<Long> registeredTeacherIds = new ArrayList<>();
        Student currentStudent = getCurrentStudent();
        
        Optional<StudentTeacher> approvedIndividualTeacher = studentTeacherRepository.findApprovedTeacherForStudent(currentStudent);
        if (approvedIndividualTeacher.isPresent()) {
            registeredTeacherIds.add(approvedIndividualTeacher.get().getTeacher().getId());
        }

        model.addAttribute("teachers", teachers);
        model.addAttribute("teacherTeamCount", teacherTeamCount);
        model.addAttribute("teacherIndividualStudentCount", teacherIndividualStudentCount);
        model.addAttribute("totalPages", teacherPage.getTotalPages());
        model.addAttribute("pageNumber", page);
        model.addAttribute("registeredTeacherIds", registeredTeacherIds);

        return "student/register-teacher";
    }

    @PostMapping("/register-team-teacher")
    public String registerTeamTeacher(@RequestParam Long teacherId, RedirectAttributes redirectAttributes) {
        try {
            Student currentStudent = getCurrentStudent();

            // Kiểm tra nhóm tồn tại
            if (currentStudent.getTeam() == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Bạn cần có một nhóm để đăng ký giáo viên hướng dẫn!");
                return "redirect:/student/list-teacher";
            }

            // Kiểm tra quyền nhóm trưởng
            if (!currentStudent.isLeader()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Chỉ nhóm trưởng mới có thể đăng ký giáo viên hướng dẫn!");
                return "redirect:/student/list-teacher";
            }

            // Kiểm tra nhóm đã có giáo viên chưa
            if (currentStudent.getTeam().getTeacher() != null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Nhóm của bạn đã có giáo viên hướng dẫn!");
                return "redirect:/student/list-teacher";
            }

            Teacher teacher = teacherService.getTeacherById(teacherId)
                    .orElseThrow(() -> new IllegalStateException("Giáo viên không tồn tại"));

            // Kiểm tra giới hạn 5 nhóm cho mỗi giáo viên
            if (teamService.countTeamsByTeacherId(teacherId) >= 5) {
                redirectAttributes.addFlashAttribute("errorMessage", "Giáo viên đã đủ số lượng nhóm hướng dẫn!");
                return "redirect:/student/list-teacher";
            }

            teamService.registerTeacher(currentStudent.getTeam().getId(), teacherId);

            // Gửi thông báo cho giáo viên
            Notification notification = Notification.builder()
                    .sender(currentStudent.getUser())
                    .receiver(teacher.getUser())
                    .content(String.format("Nhóm %s (trưởng nhóm: %s) đã đăng ký bạn làm giáo viên hướng dẫn.",
                            currentStudent.getTeam().getName(), currentStudent.getUser().getName()))
                    .url("/teacher/teams")
                    .build();
            notificationService.sendNotification(notification);

            redirectAttributes.addFlashAttribute("successMessage", "Đăng ký giáo viên hướng dẫn thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi đăng ký: " + e.getMessage());
        }
        return "redirect:/student/list-teacher";
    }

    @PostMapping("/register-individual-teacher")
    public String registerIndividualTeacher(@RequestParam Long teacherId, RedirectAttributes redirectAttributes) {
        try {
            Student currentStudent = getCurrentStudent();

            // Ngăn nhóm trưởng đăng ký cá nhân
            if (currentStudent.isLeader()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Nhóm trưởng phải sử dụng đăng ký giáo viên cho nhóm!");
                return "redirect:/student/list-teacher";
            }

            Teacher teacher = teacherService.getTeacherById(teacherId)
                    .orElseThrow(() -> new IllegalStateException("Giáo viên không tồn tại"));

            // Kiểm tra đăng ký trùng
            if (studentTeacherRepository.existsByStudentAndTeacher(currentStudent, teacher)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Bạn đã đăng ký hoặc đang chờ phê duyệt với giáo viên này!");
                return "redirect:/student/list-teacher";
            }

            // Kiểm tra giới hạn 10 sinh viên cá nhân cho mỗi giáo viên
            if (studentTeacherRepository.countByTeacherAndStatus(teacher, StudentTeacher.Status.APPROVED) >= 10) {
                redirectAttributes.addFlashAttribute("errorMessage", "Giáo viên đã đủ số lượng sinh viên cá nhân!");
                return "redirect:/student/list-teacher";
            }

            // Tạo đăng ký mới
            StudentTeacher registration = StudentTeacher.builder()
                    .student(currentStudent)
                    .teacher(teacher)
                    .status(StudentTeacher.Status.PENDING)
                    .build();
            studentTeacherRepository.save(registration);

            // Gửi thông báo cho giáo viên
            Notification notification = Notification.builder()
                    .sender(currentStudent.getUser())
                    .receiver(teacher.getUser())
                    .content(String.format("%s đã đăng ký tư vấn cá nhân với bạn.",
                            currentStudent.getUser().getName()))
                    .url("/teacher/student-list")
                    .build();
            notificationService.sendNotification(notification);

            // Chuyển hướng đến hộp chat
            redirectAttributes.addFlashAttribute("successMessage", "Đăng ký tư vấn cá nhân thành công! Vui lòng chờ giáo viên phê duyệt.");
            return "redirect:/messages/chat/" + teacherId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi đăng ký: " + e.getMessage());
            return "redirect:/student/list-teacher";
        }
    }

    @GetMapping("/teacher-details/{id}")
    @ResponseBody
    public ResponseEntity<RegisterTeacherDTO> getTeacherById(@PathVariable Long id) {
        Optional<Teacher> teacherOptional = teacherService.getTeacherById(id);
        if (teacherOptional.isPresent()) {
            Teacher teacher = teacherOptional.get();
            RegisterTeacherDTO teacherDTO = new RegisterTeacherDTO(
                    teacher.getId(),
                    teacher.getUser().getName(),
                    teacher.getUser().getEmail(),
                    teacher.getDegree(),
                    teacher.getUser().getPhoneNumber(),
                    teacher.getUser().getDob(),
                    teacher.getUser().getAddress(),
                    teacher.getUser().getGender(),
                    teacher.getUser().getAvatar(),
                    teacher.getFaculty() != null ? teacher.getFaculty().getName() : "Chưa có khoa"
            );
            return ResponseEntity.ok(teacherDTO);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/documents")
    public String getAllDocuments(@RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "5") int size,
                                  Model model) {
        User currentUser = userService.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName());
        Student currentStudent = studentService.findStudentByUserId(currentUser.getId());
        Team currentTeam = currentStudent.getTeam();
        Teacher teacher = currentTeam.getTeacher();
        Page<Document> documentPage = documentService.getDocumentsPage(page, size, teacher);

        model.addAttribute("documents", documentPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", documentPage.getTotalPages());
        model.addAttribute("totalItems", documentPage.getTotalElements());
        model.addAttribute("pageSize", size);

        return "student/document-view";
    }
}