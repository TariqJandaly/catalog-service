package com.kaustack.catalog.misc;

import com.kaustack.catalog.model.*;
import com.kaustack.catalog.repository.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {

    private final CourseRepository courseRepository;
    private final TermRepository termRepository;
    private final SectionRepository sectionRepository;
    private final ScheduleRepository scheduleRepository;
    private final InstructorRepository instructorRepository;
    private final RestTemplate restTemplate;

    @Value("${app.data.load:false}")
    private boolean load;

    @Value("${app.data.courses-url}")
    private String coursesUrl;

    @Value("${app.data.instructors-url}")
    private String instructorsUrl;

    @Override
    @Transactional
    public void run(String... args) {
        if (!load) return;
        loadCourses();
        loadInstructors();
    }

    private void loadCourses() {
        log.info("Fetching courses...");
        CoursesApiResponse response = restTemplate.getForObject(coursesUrl, CoursesApiResponse.class);

        if (response == null || !"success".equals(response.getStatus()) || response.getData() == null) {
            log.warn("No valid course data received.");
            return;
        }

        Term term = termRepository.findByTermCode(response.getTermId()).orElse(new Term());
        term.setName(response.getTermName());
        term.setTermCode(response.getTermId());
        term.setUpdatedAt(LocalDateTime.now());
        if (term.getCreatedAt() == null) term.setCreatedAt(LocalDateTime.now());
        term = termRepository.save(term);

        // Preload existing entities into Maps — one query per type
        Map<String, Course> courseMap = courseRepository.findAll().stream()
                .collect(Collectors.toMap(c -> c.getCode() + "-" + c.getNumber(), Function.identity()));
        Map<String, Instructor> instructorMap = instructorRepository.findAll().stream()
                .collect(Collectors.toMap(Instructor::getId, Function.identity()));
        Map<String, Section> sectionMap = sectionRepository.findByTermId(term.getId()).stream()
                .collect(Collectors.toMap(Section::getId, Function.identity()));

        // Delete all schedules for this term in one shot
        scheduleRepository.deleteByTermId(term.getId());

        List<Course> courses = new ArrayList<>();
        List<Instructor> newInstructors = new ArrayList<>();
        List<Section> sections = new ArrayList<>();
        Map<String, List<ScheduleData>> pendingSchedules = new HashMap<>();

        for (CourseData cd : response.getData()) {
            Course course = courseMap.getOrDefault(cd.getCourseCode() + "-" + cd.getCourseNumber(), new Course());
            course.setCode(cd.getCourseCode());
            course.setNumber(cd.getCourseNumber());
            course.setTitle(cd.getTitle());
            courses.add(course);

            if (cd.getSections() == null) continue;

            for (SectionData sd : cd.getSections()) {

                Instructor instructor = null;
                if (sd.getInstructorId() != null && !sd.getInstructorId().isBlank()) {
                    instructor = instructorMap.computeIfAbsent(sd.getInstructorId(), iId -> {
                        Instructor newInst = new Instructor();
                        newInst.setId(iId);
                        newInst.setName(iId);
                        newInstructors.add(newInst);
                        return newInst;
                    });
                }

                Section section = sectionMap.getOrDefault(sd.getId(), new Section());
                section.setId(sd.getId());
                section.setCrn(sd.getCrn());
                section.setTerm(term);
                section.setCourse(course);
                section.setInstructor(instructor);
                section.setCode(sd.getCode());
                section.setBranch(sd.getBranch());
                section.setScheduleType(sd.getScheduleType());
                section.setInstructionMethod(sd.getInstructionMethod());
                section.setLevel(sd.getLevel());
                section.setCredits(sd.getCredits());
                if (sd.getCreatedAt() != null) section.setCreatedAt(Instant.parse(sd.getCreatedAt()).atZone(ZoneOffset.UTC).toLocalDateTime());
                if (sd.getUpdatedAt() != null) section.setUpdatedAt(Instant.parse(sd.getUpdatedAt()).atZone(ZoneOffset.UTC).toLocalDateTime());
                sections.add(section);

                if (sd.getSchedules() != null) pendingSchedules.put(sd.getId(), sd.getSchedules());
            }
        }

        // Save courses, instructors, and sections first
        courseRepository.saveAll(courses);
        instructorRepository.saveAll(newInstructors);
        List<Section> savedSections = sectionRepository.saveAll(sections);

        // Build schedules using the managed section entities returned by saveAll
        List<Schedule> schedules = new ArrayList<>();
        for (Section saved : savedSections) {
            List<ScheduleData> sds = pendingSchedules.get(saved.getId());
            if (sds == null) continue;
            for (ScheduleData schd : sds) {
                Schedule schedule = new Schedule();
                schedule.setType(schd.getType());
                schedule.setStartTime(schd.getStartTime());
                schedule.setEndTime(schd.getEndTime());
                schedule.setRawTime(schd.getRawTime());
                schedule.setDays(schd.getDays());
                schedule.setLocation(schd.getLocation());
                schedule.setDateRange(schd.getDateRange());
                schedule.setSection(saved);
                schedules.add(schedule);
            }
        }
        scheduleRepository.saveAll(schedules);

        log.info("Done. Loaded {} courses, {} sections, {} schedules.", courses.size(), sections.size(), schedules.size());
    }

    private void loadInstructors() {
        log.info("Fetching instructors...");
        InstructorsApiResponse response = restTemplate.getForObject(instructorsUrl, InstructorsApiResponse.class);

        if (response == null || !"success".equals(response.getStatus()) || response.getData() == null) {
            log.warn("No valid instructor data received.");
            return;
        }

        // Preload existing entities into Maps — one query per type
        Map<String, Instructor> instructorMap = instructorRepository.findAll().stream()
                .collect(Collectors.toMap(Instructor::getId, Function.identity()));
        Map<String, Section> sectionMap = sectionRepository.findByTermId(response.getTermId()).stream()
                .collect(Collectors.toMap(Section::getId, Function.identity()));

        List<Instructor> instructors = new ArrayList<>();

        for (InstructorData id : response.getData()) {
            Instructor instructor = instructorMap.getOrDefault(id.getId(), new Instructor());
            instructor.setId(id.getId());
            instructor.setName(id.getName());
            instructor.setEmail(id.getEmail());
            instructors.add(instructor);

            if (id.getSections() == null) continue;

            for (SectionRef sr : id.getSections()) {
                Section section = sectionMap.get(sr.getId());
                if (section != null) section.setInstructor(instructor);
            }
        }

        instructorRepository.saveAll(instructors);
        sectionRepository.saveAll(new ArrayList<>(sectionMap.values()));

        log.info("Done. Loaded {} instructors.", instructors.size());
    }

    // --- Courses API DTOs ---

    @Data
    private static class CoursesApiResponse {
        private String status;
        private String termName;
        private String termId;
        private List<CourseData> data;
    }

    @Data
    private static class CourseData {
        private String id;
        private String courseCode;
        private String courseNumber;
        private String title;
        private List<SectionData> sections;
    }

    @Data
    private static class SectionData {
        private String id;
        private Integer crn;
        private String instructorId;
        private String code;
        private String branch;
        private String scheduleType;
        private String instructionMethod;
        private String level;
        private Integer credits;
        private String createdAt;
        private String updatedAt;
        private List<ScheduleData> schedules;
    }

    @Data
    private static class ScheduleData {
        private String type;
        private Integer startTime;
        private Integer endTime;
        private String rawTime;
        private String days;
        private String location;
        private String dateRange;
    }

    // --- Instructors API DTOs ---

    @Data
    private static class InstructorsApiResponse {
        private String status;
        private String termName;
        private String termId;
        private List<InstructorData> data;
    }

    @Data
    private static class InstructorData {
        private String id;
        private String name;
        private String email;
        private List<SectionRef> sections;
    }

    @Data
    private static class SectionRef {
        private String id;
    }
}
