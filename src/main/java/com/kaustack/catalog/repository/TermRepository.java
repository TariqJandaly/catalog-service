package com.kaustack.catalog.repository;

import com.kaustack.catalog.model.Term;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TermRepository extends JpaRepository<Term, String> {
    Optional<Term> findByTermCode(String termCode);

    // Helper to find the latest updated term
    Optional<Term> findTopByOrderByUpdatedAtDesc();
}