package com.c0324.casestudym5.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "autism_test_answers")
public class AutismTestAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "result_id")
    private AutismTestResult result;
    
    @ManyToOne
    @JoinColumn(name = "question_id")
    private AutismQuestion question;
    
    @Column(length = 1000)
    private String answerValue;
    
    private Integer score;
    
    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AutismTestResult getResult() {
        return result;
    }

    public void setResult(AutismTestResult result) {
        this.result = result;
    }
    
    public AutismQuestion getQuestion() {
        return question;
    }
    
    public void setQuestion(AutismQuestion question) {
        this.question = question;
    }
    
    public String getAnswerValue() {
        return answerValue;
    }
    
    public void setAnswerValue(String answerValue) {
        this.answerValue = answerValue;
    }
    
    public Integer getScore() {
        return score;
    }
    
    public void setScore(Integer score) {
        this.score = score;
    }
} 