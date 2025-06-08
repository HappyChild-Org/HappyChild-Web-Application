package com.c0324.casestudym5.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.c0324.casestudym5.dto.MessageDTO;
import com.c0324.casestudym5.model.Message;
import com.c0324.casestudym5.model.User;
import com.c0324.casestudym5.repository.MessageRepository;
import com.c0324.casestudym5.repository.UserRepository;

@Service
public class MessageService {
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public MessageService(MessageRepository messageRepository, UserRepository userRepository, 
            SimpMessagingTemplate messagingTemplate) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }
    
    @Transactional
    public Message saveMessage(MessageDTO messageDTO) {
        User sender = userRepository.findById(messageDTO.getSenderId()).orElse(null);
        User receiver = userRepository.findById(messageDTO.getReceiverId()).orElse(null);
        
        if (sender == null || receiver == null) {
            throw new IllegalArgumentException("Sender or receiver not found");
        }
        
        Message message = new Message();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent(messageDTO.getContent());
        message.setTimestamp(messageDTO.getTimestamp());
        
        return messageRepository.save(message);
    }
    
    public List<MessageDTO> getMessagesBetweenUsers(Long user1Id, Long user2Id) {
        User user1 = userRepository.findById(user1Id).orElse(null);
        User user2 = userRepository.findById(user2Id).orElse(null);
        
        if (user1 == null || user2 == null) {
            return new ArrayList<>();
        }
        
        List<Message> messages = messageRepository.findMessagesBetweenUsers(user1, user2);
        
        // Mark messages as read
        messages.stream()
                .filter(m -> m.getReceiver().getId().equals(user1Id) && !m.isRead())
                .forEach(m -> {
                    m.setRead(true);
                    messageRepository.save(m);
                });
        
        return messages.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    private MessageDTO convertToDTO(Message message) {
        MessageDTO dto = new MessageDTO();
        dto.setId(message.getId());
        dto.setSenderId(message.getSender().getId());
        dto.setReceiverId(message.getReceiver().getId());
        dto.setContent(message.getContent());
        dto.setTimestamp(message.getTimestamp());
        dto.setSenderName(message.getSender().getName());
        
        // Get avatar URL if available
        if (message.getSender().getAvatar() != null) {
            dto.setSenderAvatar(message.getSender().getAvatar().getUrl());
        } else {
            dto.setSenderAvatar("/img/default-avatar.png");
        }
        
        return dto;
    }
    
    public long countUnreadMessages(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return 0;
        }
        
        return messageRepository.countUnreadMessages(user);
    }
    
    public List<User> getUsersWithUnreadMessages(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return new ArrayList<>();
        }
        
        return messageRepository.findUsersWithUnreadMessages(user);
    }
    
    public void sendMessageViaWebSocket(MessageDTO message) {
        // Send message to the recipient's private channel
        messagingTemplate.convertAndSendToUser(
                message.getReceiverId().toString(),
                "/queue/messages",
                message
        );
    }
} 