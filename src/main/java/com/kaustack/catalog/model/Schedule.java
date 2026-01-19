package com.kaustack.catalog.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "\"Schedule\"")
public class Schedule {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String type;

    @Column(name = "\"startTime\"")
    private Integer startTime;

    @Column(name = "\"endTime\"")
    private Integer endTime;

    @Column(name = "\"rawTime\"")
    private String rawTime;

    private String days;
    private String location;

    @Column(name = "\"dateRange\"")
    private String dateRange;

    @ManyToOne
    @JoinColumn(name = "\"sectionId\"")
    @JsonIgnoreProperties("schedules")
    private Section section;

    @ManyToOne
    @JoinColumn(name = "\"instructorId\"")
    @JsonIgnoreProperties("schedules")
    private Instructor instructor;
}