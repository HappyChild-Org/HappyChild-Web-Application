package com.c0324.casestudym5.repository;

import com.c0324.casestudym5.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findAllByTopicId(Long topicId);

    List<Comment> findTop3ByTopicIdOrderByCreatedAtDesc(Long topicId);
}
