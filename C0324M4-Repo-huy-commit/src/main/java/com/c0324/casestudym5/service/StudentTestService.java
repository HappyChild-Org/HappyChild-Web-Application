package com.c0324.casestudym5.service;

import com.c0324.casestudym5.dto.StudentTestDTO;
import com.c0324.casestudym5.model.Student;
import com.c0324.casestudym5.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;

@Service
public class StudentTestService {
    
    private final StudentService studentService;
    private final UserService userService;

    @Autowired
    public StudentTestService(StudentService studentService, UserService userService) {
        this.studentService = studentService;
        this.userService = userService;
    }

    public StudentTestDTO getStudentTestInfo(Long studentId) {
        Student student = studentService.findById(studentId);
        if (student == null) {
            return null;
        }

        User user = student.getUser();
        StudentTestDTO dto = new StudentTestDTO();
        dto.setId(student.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setAddress(user.getAddress());
        dto.setDob(user.getDob());
        dto.setGender(user.getGender());
        
        // Calculate age
        int age = calculateAge(user.getDob());
        dto.setAge(age);
        
        // Calculate test level
        dto.calculateTestLevel();
        
        return dto;
    }

    private int calculateAge(Date dob) {
        Calendar dobCal = Calendar.getInstance();
        dobCal.setTime(dob);
        
        Calendar today = Calendar.getInstance();
        
        int age = today.get(Calendar.YEAR) - dobCal.get(Calendar.YEAR);
        
        // Check if birthday has occurred this year
        if (today.get(Calendar.DAY_OF_YEAR) < dobCal.get(Calendar.DAY_OF_YEAR)) {
            age--;
        }
        
        return age;
    }
} 