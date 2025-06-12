package com.c0324.casestudym5.service.impl;

import com.c0324.casestudym5.dto.MessageDTO;
import com.c0324.casestudym5.model.User;
import java.util.List;

public interface MessageService {
    void saveMessage(MessageDTO messageDTO);
    List<MessageDTO> getMessagesBetweenUsers(Long userId1, Long userId2);
    void sendMessageViaWebSocket(MessageDTO message);
    List<User> getUsersWithUnreadMessages(Long userId);
    long countUnreadMessages(User user);
    void markMessagesAsRead(Long senderId, Long receiverId);
} 