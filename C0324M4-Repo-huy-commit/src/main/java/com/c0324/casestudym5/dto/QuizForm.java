package com.c0324.casestudym5.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.HashMap;
import java.util.Map;

public class QuizForm {
    @NotEmpty
    private Map<Long, String> answers = new HashMap<>();

    public Map<Long, String> getAnswers() {
        return answers;
    }

    public void setAnswers(Map<Long, String> answers) {
        this.answers = answers;
    }
}
