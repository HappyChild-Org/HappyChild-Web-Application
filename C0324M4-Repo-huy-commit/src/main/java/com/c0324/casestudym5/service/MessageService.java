package com.c0324.casestudym5.service;

import com.c0324.casestudym5.model.Message;
import com.c0324.casestudym5.model.User;
import com.c0324.casestudym5.dto.MessageDTO;
import java.util.List;

public interface MessageService {
    Message saveMessage(Long senderId, Long receiverId, String content);
    
    List<MessageDTO> getMessagesBetweenUsers(Long userId1, Long userId2);
    
    List<Message> getConversations(Long userId);

    List<User> getConversationUsers(Long userId);
    
    void markAsRead(Long messageId);
    
    long countUnreadMessages(Long receiverId);

    long countUnreadMessagesFromUser(Long receiverId, Long senderId);

    void markMessagesAsReadBetweenUsers(Long receiverId, Long senderId);
} 