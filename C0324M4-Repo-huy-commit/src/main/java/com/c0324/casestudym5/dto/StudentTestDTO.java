package com.c0324.casestudym5.dto;

import com.c0324.casestudym5.model.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class StudentTestDTO {
    private Long id;
    private String name;
    private String email;
    private String phoneNumber;
    private String address;
    private Date dob;
    private User.Gender gender;
    private int age;
    private String testLevel; // Level 1 (1-3 years), Level 2 (4-5 years), Level 3 (6 years)

    public void calculateTestLevel() {
        if (age >= 1 && age <= 3) {
            this.testLevel = "Level 1";
        } else if (age >= 4 && age <= 5) {
            this.testLevel = "Level 2";
        } else if (age == 6) {
            this.testLevel = "Level 3";
        } else {
            this.testLevel = "Not eligible for test";
        }
    }
} 