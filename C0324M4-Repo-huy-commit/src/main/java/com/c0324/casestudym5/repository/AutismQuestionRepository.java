package com.c0324.casestudym5.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.c0324.casestudym5.model.AutismQuestion;

@Repository
public interface AutismQuestionRepository extends JpaRepository<AutismQuestion, Long> {
    List<AutismQuestion> findByCategory(String category);
} 