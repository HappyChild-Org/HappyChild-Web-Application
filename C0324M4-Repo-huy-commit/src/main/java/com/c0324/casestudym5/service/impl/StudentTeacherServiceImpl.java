package com.c0324.casestudym5.service.impl;

import com.c0324.casestudym5.model.StudentTeacher;
import com.c0324.casestudym5.model.Teacher;
import com.c0324.casestudym5.repository.StudentTeacherRepository;
import com.c0324.casestudym5.service.StudentTeacherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class StudentTeacherServiceImpl implements StudentTeacherService {

    private final StudentTeacherRepository studentTeacherRepository;

    @Autowired
    public StudentTeacherServiceImpl(StudentTeacherRepository studentTeacherRepository) {
        this.studentTeacherRepository = studentTeacherRepository;
    }

    @Override
    public List<StudentTeacher> findByTeacher(Teacher teacher) {
        return studentTeacherRepository.findByTeacher(teacher);
    }

    @Override
    public Optional<StudentTeacher> findById(Long id) {
        return studentTeacherRepository.findById(id);
    }

    @Override
    public StudentTeacher save(StudentTeacher studentTeacher) {
        return studentTeacherRepository.save(studentTeacher);
    }
} 