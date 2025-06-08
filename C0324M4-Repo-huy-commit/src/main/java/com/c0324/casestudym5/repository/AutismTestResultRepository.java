package com.c0324.casestudym5.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.c0324.casestudym5.model.AutismTest;
import com.c0324.casestudym5.model.AutismTestResult;
import com.c0324.casestudym5.model.User;

@Repository
public interface AutismTestResultRepository extends JpaRepository<AutismTestResult, Long> {
    List<AutismTestResult> findByStudent(User student);
    List<AutismTestResult> findByTest(AutismTest test);
    List<AutismTestResult> findByStatus(AutismTestResult.TestResultStatus status);
    List<AutismTestResult> findByStudentAndStatus(User student, AutismTestResult.TestResultStatus status);
    List<AutismTestResult> findByReviewedBy(User teacher);
    List<AutismTestResult> findByStudentAndTest(User student, AutismTest test);
} 