package com.c0324.casestudym5.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.c0324.casestudym5.model.AutismQuestion;
import com.c0324.casestudym5.model.AutismTestAnswer;
import com.c0324.casestudym5.model.AutismTestResult;

@Repository
public interface AutismTestAnswerRepository extends JpaRepository<AutismTestAnswer, Long> {
    List<AutismTestAnswer> findByResult(AutismTestResult result);
    List<AutismTestAnswer> findByResultAndQuestion(AutismTestResult result, AutismQuestion question);
} 