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
    List<Message> findBySenderAndReceiver(User sender, User receiver);
    
    @Query("SELECT m FROM Message m WHERE (m.sender = :user1 AND m.receiver = :user2) OR (m.sender = :user2 AND m.receiver = :user1) ORDER BY m.timestamp ASC")
    List<Message> findMessagesBetweenUsers(@Param("user1") User user1, @Param("user2") User user2);
    
    List<Message> findByReceiverAndReadFalse(User receiver);
    
    @Query("SELECT COUNT(m) FROM Message m WHERE m.receiver = :user AND m.read = false")
    long countUnreadMessages(@Param("user") User user);
    
    @Query("SELECT DISTINCT m.sender FROM Message m WHERE m.receiver = :user AND m.read = false")
    List<User> findUsersWithUnreadMessages(@Param("user") User user);
} 