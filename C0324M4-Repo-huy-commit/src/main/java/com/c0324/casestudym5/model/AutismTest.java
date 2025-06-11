package com.c0324.casestudym5.model;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;


// model chính tạo test
@Entity
@Table(name = "autism_tests")
public class AutismTest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String testName;
    
    private String description;
    
    private Integer ageRangeMin;
    
    private Integer ageRangeMax;
    
    @Enumerated(EnumType.STRING)
    private TestStatus status;
    
    private LocalDateTime createdAt;
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User creator;
    
    @OneToMany(mappedBy = "autismTest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AutismQuestion> questions;
    
    public enum TestStatus {
        ACTIVE, INACTIVE, DRAFT
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Integer getAgeRangeMin() {
        return ageRangeMin;
    }
    
    public void setAgeRangeMin(Integer ageRangeMin) {
        this.ageRangeMin = ageRangeMin;
    }
    
    public Integer getAgeRangeMax() {
        return ageRangeMax;
    }
    
    public void setAgeRangeMax(Integer ageRangeMax) {
        this.ageRangeMax = ageRangeMax;
    }
    
    public TestStatus getStatus() {
        return status;
    }
    
    public void setStatus(TestStatus status) {
        this.status = status;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public User getCreator() {
        return creator;
    }
    
    public void setCreator(User creator) {
        this.creator = creator;
    }

    public List<AutismQuestion> getQuestions() {
        return questions;
    }

    public void setQuestions(List<AutismQuestion> questions) {
        this.questions = questions;
    }
} 