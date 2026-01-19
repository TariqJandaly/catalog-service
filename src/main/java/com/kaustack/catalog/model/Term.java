package com.kaustack.catalog.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Table(name = "\"Term\"")
public class Term {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String name;

    @Column(name = "\"termCode\"", unique = true)
    private String termCode;

    @Column(name = "\"startDate\"")
    private LocalDateTime startDate;

    @Column(name = "\"endDate\"")
    private LocalDateTime endDate;

    @OneToMany(mappedBy = "term")
    @JsonIgnoreProperties("term")
    private List<Section> sections;

    @Column(name = "\"createdAt\"")
    private LocalDateTime createdAt;

    @Column(name = "\"updatedAt\"")
    private LocalDateTime updatedAt;
}