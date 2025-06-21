package com.c0324.casestudym5.service;

import com.c0324.casestudym5.model.StudentTeacher;
import com.c0324.casestudym5.model.Teacher;

import java.util.List;
import java.util.Optional;

public interface StudentTeacherService {
    List<StudentTeacher> findByTeacher(Teacher teacher);
    Optional<StudentTeacher> findById(Long id);
    StudentTeacher save(StudentTeacher studentTeacher);
} 