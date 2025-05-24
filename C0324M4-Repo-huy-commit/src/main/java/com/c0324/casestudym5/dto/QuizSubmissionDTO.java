package com.c0324.casestudym5.dto;

import lombok.Data;

import java.util.List;

@Data
public class QuizSubmissionDTO {
    private String childId;
    private List<AnswerDTO> answers;
}
