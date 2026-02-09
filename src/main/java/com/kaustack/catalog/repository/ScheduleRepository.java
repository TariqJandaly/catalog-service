package com.kaustack.catalog.repository;

import com.kaustack.catalog.model.Schedule;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, String> {

    @EntityGraph(attributePaths = {"section", "section.course", "instructor"})
    List<Schedule> findByInstructorIdAndSectionTermId(String instructorId, String termId);
}