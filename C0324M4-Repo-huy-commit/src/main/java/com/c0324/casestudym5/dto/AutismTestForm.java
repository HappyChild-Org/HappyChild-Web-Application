package com.c0324.casestudym5.dto;

import java.util.Map;
//Form DTO để submit bài test
public class AutismTestForm {
    private Long testId;
    private Map<Long, String> answers; // questionId -> answer

    public Long getTestId() {
        return testId;
    }

    public void setTestId(Long testId) {
        this.testId = testId;
    }

    public Map<Long, String> getAnswers() {
        return answers;
    }

    public void setAnswers(Map<Long, String> answers) {
        this.answers = answers;
    }
} 