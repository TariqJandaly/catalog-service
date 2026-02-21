package com.kaustack.catalog.repository;

import com.kaustack.catalog.model.Schedule;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, String> {

    @EntityGraph(attributePaths = {"section", "section.course", "instructor"})
    List<Schedule> findByInstructorIdAndSectionTermId(String instructorId, String termId);

    @Modifying
    @Query("DELETE FROM Schedule s WHERE s.section.id = :sectionId")
    void deleteBySectionId(@Param("sectionId") String sectionId);

    @Modifying
    @Query("DELETE FROM Schedule s WHERE s.section.term.id = :termId")
    void deleteByTermId(@Param("termId") String termId);
}