package com.c0324.casestudym5.repository;

import com.c0324.casestudym5.model.Student;
import com.c0324.casestudym5.model.StudentTeacher;
import com.c0324.casestudym5.model.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentTeacherRepository extends JpaRepository<StudentTeacher, Long> {
    boolean existsByStudentAndTeacher(Student student, Teacher teacher);
    List<StudentTeacher> findByTeacherAndStatus(Teacher teacher, StudentTeacher.Status status);
    int countByTeacherAndStatus(Teacher teacher, StudentTeacher.Status status);
    List<StudentTeacher> findByStudentAndStatus(Student student, StudentTeacher.Status status);

    @Query("SELECT st FROM StudentTeacher st WHERE st.student = :student AND st.status = 'APPROVED'")
    Optional<StudentTeacher> findApprovedTeacherForStudent(@Param("student") Student student);

    @Query("SELECT COUNT(st) FROM StudentTeacher st WHERE st.teacher = :teacher AND st.status = 'APPROVED'")
    long countApprovedStudentsForTeacher(@Param("teacher") Teacher teacher);

    @Query("SELECT st FROM StudentTeacher st WHERE st.student = :student AND st.status = 'PENDING'")
    List<StudentTeacher> findPendingRegistrationsForStudent(@Param("student") Student student);

    @Query("SELECT st FROM StudentTeacher st WHERE st.teacher = :teacher AND st.status = 'PENDING'")
    List<StudentTeacher> findPendingRegistrationsForTeacher(@Param("teacher") Teacher teacher);

    Optional<StudentTeacher> findByStudentAndTeacher(Student student, Teacher teacher);

    List<StudentTeacher> findByTeacher(Teacher teacher);
}