package com.kaustack.catalog.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Schedule {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String type;
    private Integer startTime;
    private Integer endTime;
    private String rawTime;
    private String days;
    private String location;
    private String dateRange;

    @ManyToOne
    @JoinColumn(name = "sectionId")
    @JsonIgnoreProperties("schedules")
    private Section section;

    @ManyToOne
    @JoinColumn(name = "instructorId")
    @JsonIgnoreProperties("schedules")
    private Instructor instructor;
}