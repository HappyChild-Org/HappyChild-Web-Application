package com.c0324.casestudym5.service.impl;

import com.c0324.casestudym5.model.Message;
import com.c0324.casestudym5.model.User;
import com.c0324.casestudym5.repository.MessageRepository;
import com.c0324.casestudym5.repository.UserRepository;
import com.c0324.casestudym5.service.MessageService;
import com.c0324.casestudym5.dto.MessageDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MessageServiceImpl implements MessageService {
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public MessageServiceImpl(MessageRepository messageRepository, UserRepository userRepository, 
            SimpMessagingTemplate messagingTemplate) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public Message saveMessage(Long senderId, Long receiverId, String content) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("Sender not found"));
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new IllegalArgumentException("Receiver not found"));
        
        Message message = Message.builder()
                .sender(sender)
                .receiver(receiver)
                .content(content)
                .isRead(false)
                .build();
                
        return messageRepository.save(message);
    }

    @Override
    public List<MessageDTO> getMessagesBetweenUsers(Long userId1, Long userId2) {
        List<Message> messages = messageRepository.findBySenderIdAndReceiverIdOrReceiverIdAndSenderIdOrderByCreatedAtAsc(
                userId1, userId2);
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
        dto.setTimestamp(message.getCreatedAt());
        dto.setSenderName(message.getSender().getName());
        dto.setSenderAvatar(message.getSender().getAvatar() != null ? message.getSender().getAvatar().getUrl() : "/img/default-avatar.png");
        dto.setRead(message.getIsRead());
        return dto;
    }

    @Override
    public List<Message> getConversations(Long userId) {
        return messageRepository.findDistinctBySenderIdOrReceiverId(userId);
    }

    @Override
    public List<User> getConversationUsers(Long userId) {
        Set<Long> conversationUserIds = messageRepository.findDistinctConversationUserIds(userId);
        return userRepository.findAllById(conversationUserIds);
    }

    @Override
    public void markAsRead(Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalStateException("Message not found"));
        message.setIsRead(true);
        messageRepository.save(message);
    }

    @Override
    public long countUnreadMessages(Long receiverId) {
        return messageRepository.countByReceiverIdAndIsReadFalse(receiverId);
    }

    @Override
    public long countUnreadMessagesFromUser(Long receiverId, Long senderId) {
        return messageRepository.countByReceiverIdAndSenderIdAndIsReadFalse(receiverId, senderId);
    }

    @Override
    @Transactional
    public void markMessagesAsReadBetweenUsers(Long receiverId, Long senderId) {
        List<Message> unreadMessages = messageRepository.findByReceiverIdAndSenderIdAndIsReadFalse(receiverId, senderId);
        for (Message message : unreadMessages) {
            message.setIsRead(true);
        }
        messageRepository.saveAll(unreadMessages);
    }
}
