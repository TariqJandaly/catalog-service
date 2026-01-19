package com.kaustack.catalog.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Table(indexes = {
        @Index(name = "idx_term_course", columnList = "termId, subjectId")
})
public class Section {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private Integer crn;

    // Prisma: termId String
    @ManyToOne
    @JoinColumn(name = "termId")
    @JsonIgnoreProperties("sections")
    private Term term;

    // Prisma: subjectId String (Mapped to Course entity)
    @ManyToOne
    @JoinColumn(name = "subjectId")
    @JsonIgnoreProperties("sections")
    private Course course;

    // Prisma: instructorId String
    @ManyToOne
    @JoinColumn(name = "instructorId")
    @JsonIgnoreProperties({"sectionsTaught", "schedules"})
    private Instructor instructor;

    private String code;
    private String branch;
    private String scheduleType;
    private String instructionMethod;
    private String level;
    private Integer credits;

    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL)
    @JsonIgnoreProperties("section")
    private List<Schedule> schedules;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
}