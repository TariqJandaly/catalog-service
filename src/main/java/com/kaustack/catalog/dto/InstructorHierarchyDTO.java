package com.kaustack.catalog.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class InstructorHierarchyDTO {
    private String name;
    private String email;
    private List<CourseGroup> courses = new ArrayList<>();

    @Data
    public static class CourseGroup {
        private String courseLabel; // "CPCS-203"
        private String courseTitle; // "Programming II"
        private List<String> sections = new ArrayList<>();
    }
}