package com.c0324.casestudym5.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;
//Form DTO để submit bài test
@Setter
@Getter
public class AutismTestForm {
    private Long testId;
    private Map<Long, String> answers; // questionId -> answer

}