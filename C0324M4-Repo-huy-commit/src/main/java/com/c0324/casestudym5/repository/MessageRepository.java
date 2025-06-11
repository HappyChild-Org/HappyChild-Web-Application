package com.c0324.casestudym5.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.c0324.casestudym5.model.Message;
import com.c0324.casestudym5.model.User;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    @Query("SELECT m FROM Message m WHERE (m.sender.id = :userId1 AND m.receiver.id = :userId2) OR (m.sender.id = :userId2 AND m.receiver.id = :userId1) ORDER BY m.timestamp")
    List<Message> findMessagesBetweenUsers(@Param("userId1") Long userId1, @Param("userId2") Long userId2);
    
    @Query("SELECT DISTINCT m.sender FROM Message m WHERE m.receiver = :user AND m.read = false")
    List<User> findUsersWithUnreadMessages(@Param("user") User user);
    
    @Query("SELECT COUNT(m) FROM Message m WHERE m.receiver = :user AND m.read = false")
    long countUnreadMessages(@Param("user") User user);
} 