package com.kaustack.catalog.dto;

import lombok.Data;
import java.util.List;

@Data
public class SectionDTO {
    private String id;
    private Integer crn;
    private String sectionCode;

    private String courseTitle;
    private String courseCode;
    private String courseNumber;

    private String termName;

    // Primary Section Instructor
    private String instructorName;
    private String instructorEmail;

    private String branch;
    private String scheduleType;
    private Integer credits;

    private List<ScheduleDTO> schedules;

    @Data
    public static class ScheduleDTO {
        private String type;
        private String days;
        private String time;
        private String room;
        private String instructor;
    }
}