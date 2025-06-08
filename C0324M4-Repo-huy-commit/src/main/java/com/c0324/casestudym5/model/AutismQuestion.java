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
@Table(name = "autism_questions")
public class AutismQuestion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(length = 1000)
    private String question;
    
    private String category;
    
    private Integer weightScore;
    
    @ManyToOne
    @JoinColumn(name = "test_id")
    private AutismTest autismTest;
    
    // For visual questions
    private String imageUrl;
    
    private String answerType; // "YES_NO", "MULTIPLE_CHOICE", "SCALE"
    
    private String options; // JSON string for multiple choice options
    
    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public Integer getWeightScore() {
        return weightScore;
    }
    
    public void setWeightScore(Integer weightScore) {
        this.weightScore = weightScore;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public String getAnswerType() {
        return answerType;
    }
    
    public void setAnswerType(String answerType) {
        this.answerType = answerType;
    }
    
    public String getOptions() {
        return options;
    }
    
    public void setOptions(String options) {
        this.options = options;
    }

    public AutismTest getAutismTest() {
        return autismTest;
    }

    public void setAutismTest(AutismTest autismTest) {
        this.autismTest = autismTest;
    }
} 