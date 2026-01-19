package com.kaustack.catalog.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Table(name = "\"Section\"", indexes = {
        @Index(name = "idx_term_course", columnList = "\"termId\", \"courseId\"")
})
public class Section {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private Integer crn;

    @ManyToOne
    @JoinColumn(name = "\"termId\"")
    @JsonIgnoreProperties("sections")
    private Term term;

    @ManyToOne
    @JoinColumn(name = "\"courseId\"")
    @JsonIgnoreProperties("sections")
    private Course course;

    @ManyToOne
    @JoinColumn(name = "\"instructorId\"")
    @JsonIgnoreProperties({"sectionsTaught", "schedules"})
    private Instructor instructor;

    private String code;
    private String branch;

    @Column(name = "\"scheduleType\"")
    private String scheduleType;

    @Column(name = "\"instructionMethod\"")
    private String instructionMethod;

    private String level;
    private Integer credits;

    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL)
    @JsonIgnoreProperties("section")
    private List<Schedule> schedules;

    @Column(name = "\"createdAt\"")
    private LocalDateTime createdAt;

    @Column(name = "\"updatedAt\"")
    private LocalDateTime updatedAt;
}