package com.kaustack.catalog.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Entity
@Data
public class Instructor {
    @Id
    private String id;

    @Column(unique = true)
    private String name;
    private String email;

    @OneToMany(mappedBy = "instructor")
    @JsonIgnoreProperties("instructor")
    private List<Section> sectionsTaught;

    @OneToMany(mappedBy = "instructor")
    @JsonIgnoreProperties("instructor")
    private List<Schedule> schedules;
}