package com.c0324.casestudym5.service;

import com.c0324.casestudym5.model.TestResult;
import com.c0324.casestudym5.repository.TestResultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TestResultService {
    @Autowired
    private TestResultRepository testResultRepository;

    public List<TestResult> findByStudentId(Long studentId) {
        return testResultRepository.findByStudentId(studentId);
    }
} 