package com.c0324.casestudym5.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.c0324.casestudym5.model.AutismTest;
import com.c0324.casestudym5.model.User;

@Repository
public interface AutismTestRepository extends JpaRepository<AutismTest, Long> {
    List<AutismTest> findByCreator(User creator);
    List<AutismTest> findByStatus(AutismTest.TestStatus status);
    @Query("SELECT t FROM AutismTest t WHERE t.ageRangeMin <= :age AND t.ageRangeMax >= :age AND t.status = 'ACTIVE'")
    List<AutismTest> findByAgeRange(@Param("age") int age);}
