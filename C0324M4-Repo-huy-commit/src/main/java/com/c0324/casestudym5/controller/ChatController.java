package com.c0324.casestudym5.controller;

import com.c0324.casestudym5.dto.MessageDTO;
import com.c0324.casestudym5.model.User;
import com.c0324.casestudym5.service.MessageService;
import com.c0324.casestudym5.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/chat")
public class ChatController {

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserService userService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @GetMapping("/{userId}")
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

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload MessageDTO message) {
        messageService.saveMessage(message);
        messagingTemplate.convertAndSendToUser(
            message.getReceiverId().toString(),
            "/queue/messages",
            message
        );
    }
} 