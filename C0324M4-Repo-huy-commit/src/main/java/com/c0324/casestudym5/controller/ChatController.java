package com.c0324.casestudym5.controller;

import com.c0324.casestudym5.dto.MessageDTO;
import com.c0324.casestudym5.model.*;
import com.c0324.casestudym5.model.Message;
import com.c0324.casestudym5.service.*;
import com.c0324.casestudym5.repository.StudentTeacherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;

@Controller
@RequestMapping("/messages/chat")
public class ChatController {

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserService userService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private TeacherService teacherService;

    @Autowired
    private StudentTeacherRepository studentTeacherRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();
        return userService.findByEmail(userEmail);
    }

    @GetMapping("/{userId}")
    public String showChatPage(@PathVariable Long userId, Model model) {
        User currentUser = getCurrentUser();
        User chatPartnerUser = userService.findById(userId);

        System.out.println("DEBUG: showChatPage called. Current user ID: " + (currentUser != null ? currentUser.getId() : "null") + ", Chat partner ID: " + userId);
        System.out.println("DEBUG: Current user roles: " + (currentUser != null ? currentUser.getRoles() : "null"));
        System.out.println("DEBUG: Chat partner user roles: " + (chatPartnerUser != null ? chatPartnerUser.getRoles() : "null"));

        if (chatPartnerUser == null) {
            model.addAttribute("errorMessage", "Người dùng đối tác không tồn tại!");
            return "redirect:/";
        }

        Student currentStudent = null;
        Teacher currentTeacher = null;
        Student chatPartnerStudent = null;
        Teacher chatPartnerTeacher = null;

        // Determine current user's entity (Student or Teacher)
        if (currentUser.getRoles().stream().anyMatch(role -> role.getName().equals(Role.RoleName.ROLE_STUDENT))) {
            currentStudent = studentService.findStudentByUserId(currentUser.getId());
            System.out.println("DEBUG: Current user is a STUDENT. Student entity: " + (currentStudent != null ? currentStudent.getId() : "null"));
        } else if (currentUser.getRoles().stream().anyMatch(role -> role.getName().equals(Role.RoleName.ROLE_TEACHER))) {
            currentTeacher = teacherService.findByUserId(currentUser.getId());
            System.out.println("DEBUG: Current user is a TEACHER. Teacher entity: " + (currentTeacher != null ? currentTeacher.getId() : "null"));
        }

        // Determine chat partner's entity (Student or Teacher)
        if (chatPartnerUser.getRoles().stream().anyMatch(role -> role.getName().equals(Role.RoleName.ROLE_STUDENT))) {
            chatPartnerStudent = studentService.findStudentByUserId(chatPartnerUser.getId());
            System.out.println("DEBUG: Chat partner is a STUDENT. Student entity: " + (chatPartnerStudent != null ? chatPartnerStudent.getId() : "null"));
        } else if (chatPartnerUser.getRoles().stream().anyMatch(role -> role.getName().equals(Role.RoleName.ROLE_TEACHER))) {
            chatPartnerTeacher = teacherService.findByUserId(chatPartnerUser.getId());
            System.out.println("DEBUG: Chat partner is a TEACHER. Teacher entity: " + (chatPartnerTeacher != null ? chatPartnerTeacher.getId() : "null"));
        }

        // Now, find the StudentTeacher relationship based on who is chatting with whom
        Optional<StudentTeacher> studentTeacherOptional = Optional.empty();

        if (currentStudent != null && chatPartnerTeacher != null) {
            // Student chatting with Teacher
            studentTeacherOptional = studentTeacherRepository.findByStudentAndTeacher(currentStudent, chatPartnerTeacher);
            System.out.println("DEBUG: Attempting to find StudentTeacher for Student ID " + currentStudent.getId() + " and Teacher ID " + chatPartnerTeacher.getId());
        } else if (currentTeacher != null && chatPartnerStudent != null) {
            // Teacher chatting with Student
            studentTeacherOptional = studentTeacherRepository.findByStudentAndTeacher(chatPartnerStudent, currentTeacher);
            System.out.println("DEBUG: Attempting to find StudentTeacher for Student ID " + chatPartnerStudent.getId() + " and Teacher ID " + currentTeacher.getId());
        } else {
            // Invalid combination for chat (e.g., student-student, teacher-teacher, or entities not found)
            System.out.println("DEBUG: Invalid chat combination (student-student, teacher-teacher, or entities not found).");
            model.addAttribute("errorMessage", "Không thể thiết lập cuộc trò chuyện. Vui lòng đảm bảo bạn đang chat với giáo viên hoặc học sinh phù hợp.");
            return "common/403";
        }

        if (!studentTeacherOptional.isPresent()) {
            System.out.println("DEBUG: StudentTeacher relationship NOT FOUND or invalid for current user and chat partner.");
            model.addAttribute("errorMessage", "Bạn không có quyền chat với người dùng này! (Mối quan hệ không tồn tại hoặc không hợp lệ)");
            return "common/403";
        }

        StudentTeacher studentTeacher = studentTeacherOptional.get();
        System.out.println("DEBUG: StudentTeacher relationship FOUND. ID: " + studentTeacher.getId() + ", Status: " + studentTeacher.getStatus());

        boolean isPendingStudentRelationship = false;
        if (studentTeacher.getStatus().equals(StudentTeacher.Status.PENDING) &&
            currentUser.getRoles().stream().anyMatch(role -> role.getName().equals(Role.RoleName.ROLE_STUDENT))) {
            isPendingStudentRelationship = true;
            System.out.println("DEBUG: Relationship is PENDING for current student.");
        }

        List<MessageDTO> messages = messageService.getMessagesBetweenUsers(currentUser.getId(), userId);
        System.out.println("DEBUG: Loaded " + messages.size() + " messages for user " + currentUser.getId() + " and partner " + userId);
        List<User> conversationUsers = messageService.getConversationUsers(currentUser.getId());

        // Tính toán số tin nhắn chưa đọc cho mỗi user
        Map<Long, Long> unreadCounts = new HashMap<>();
        for (User user : conversationUsers) {
            long unreadCount = messageService.countUnreadMessagesFromUser(currentUser.getId(), user.getId());
            if (unreadCount > 0) {
                unreadCounts.put(user.getId(), unreadCount);
            }
        }

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("chatPartner", chatPartnerUser);
        model.addAttribute("messages", messages);
        model.addAttribute("conversationUsers", conversationUsers);
        model.addAttribute("unreadCounts", unreadCounts);
        model.addAttribute("studentTeacherRelationship", studentTeacher);
        model.addAttribute("isPendingStudentRelationship", isPendingStudentRelationship);

        return "chat/chat-page";
    }

    @MessageMapping("/message.send")
    public void sendMessage(@Payload MessageDTO message) {
        System.out.println("DEBUG: Received message from " + message.getSenderId() + " to " + message.getReceiverId() + ": " + message.getContent());

        Message savedMessage = messageService.saveMessage(
                message.getSenderId(),
                message.getReceiverId(),
                message.getContent()
        );
        System.out.println("DEBUG: Saved message with ID: " + savedMessage.getId());

        MessageDTO messageToSend = new MessageDTO();
        messageToSend.setId(savedMessage.getId());
        messageToSend.setSenderId(savedMessage.getSender().getId());
        messageToSend.setReceiverId(savedMessage.getReceiver().getId());
        messageToSend.setContent(savedMessage.getContent());
        messageToSend.setTimestamp(savedMessage.getCreatedAt());
        messageToSend.setSenderName(savedMessage.getSender().getName());
        messageToSend.setSenderAvatar(savedMessage.getSender().getAvatar() != null ? savedMessage.getSender().getAvatar().getUrl() : "/img/default-avatar.png");
        messageToSend.setRead(savedMessage.getIsRead());

        // Gửi tới người nhận qua hàng đợi cá nhân
        messagingTemplate.convertAndSendToUser(
                message.getReceiverId().toString(),
                "/queue/messages",
                messageToSend
        );
        System.out.println("DEBUG: Sent to receiver queue: /user/" + message.getReceiverId() + "/queue/messages");

        // Gửi lại cho người gửi qua hàng đợi cá nhân
        messagingTemplate.convertAndSendToUser(
                message.getSenderId().toString(),
                "/queue/messages",
                messageToSend
        );
        System.out.println("DEBUG: Sent to sender queue: /user/" + message.getSenderId() + "/queue/messages");

        // Gửi tới topic chung của cuộc trò chuyện
        String chatRoom = "/topic/chat/" + Math.min(message.getSenderId(), message.getReceiverId()) + "/" + Math.max(message.getSenderId(), message.getReceiverId());
        messagingTemplate.convertAndSend(chatRoom, messageToSend);
        System.out.println("DEBUG: Sent to chat room: " + chatRoom);
    }
    @MessageMapping("/chat.read")
    public void markAsRead(@Payload Long messageId) {
        messageService.markAsRead(messageId);
    }

    @GetMapping("/conversations")
    @ResponseBody
    public List<Message> getConversations() {
        User currentUser = getCurrentUser();
        return messageService.getConversations(currentUser.getId());
    }

    @GetMapping("/unread/count")
    @ResponseBody
    public long getUnreadCount() {
        User currentUser = getCurrentUser();
        return messageService.countUnreadMessages(currentUser.getId());
    }

    @GetMapping("/unread/count/{senderId}")
    @ResponseBody
    public long getUnreadCountFromUser(@PathVariable Long senderId) {
        User currentUser = getCurrentUser();
        return messageService.countUnreadMessagesFromUser(currentUser.getId(), senderId);
    }

    @PostMapping("/mark-read/{senderId}")
    @ResponseBody
    public ResponseEntity<String> markMessagesAsRead(@PathVariable Long senderId) {
        try {
            User currentUser = getCurrentUser();
            messageService.markMessagesAsReadBetweenUsers(currentUser.getId(), senderId);
            return ResponseEntity.ok("Success");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}
