package com.kaustack.catalog.repository;

import com.kaustack.catalog.model.Section;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SectionRepository extends JpaRepository<Section, String>, JpaSpecificationExecutor<Section> {
    @EntityGraph(attributePaths = {"course", "instructor"})
    List<Section> findByTermId(String termId);
}