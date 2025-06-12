package com.c0324.casestudym5.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "test_results")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false)
    private String testName;

    @Column(nullable = false)
    private Double score;

    @Column(columnDefinition = "TEXT")
    private String comments;
} 