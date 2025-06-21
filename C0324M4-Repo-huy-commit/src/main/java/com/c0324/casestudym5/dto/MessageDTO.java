package com.c0324.casestudym5.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageDTO {
    private Long id;
    private Long senderId;
    private Long receiverId;
    private String content;
    private LocalDateTime timestamp;
    private String senderName;
    private String senderAvatar;
    private boolean read;
}