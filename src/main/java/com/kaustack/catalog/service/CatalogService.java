package com.kaustack.catalog.service;

import com.kaustack.catalog.dto.InstructorHierarchyDTO;
import com.kaustack.catalog.model.Schedule;
import com.kaustack.catalog.model.Section;
import com.kaustack.catalog.model.Term;
import com.kaustack.catalog.repository.SectionRepository;
import com.kaustack.catalog.repository.TermRepository;
import jakarta.persistence.criteria.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CatalogService {

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private TermRepository termRepository;

    // Helper: Convert "HH:MM" to minutes-since-midnight
    private Integer parseTimeBytes(String timeStr) {
        if (timeStr == null || !timeStr.matches("\\d{1,2}:\\d{2}")) return null;
        String[] parts = timeStr.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }

    public Page<Section> search(
            String termCode,
            String q,
            int page,
            int limit,
            String days,
            String instructor,
            String startTime,
            String endTime,
            String level,
            String crn,
            String sectionCode,
            String gender,
            String branch
    ) {
        // 1. Resolve Term
        Term term;
        if (termCode != null && !termCode.isEmpty()) {
            term = termRepository.findByTermCode(termCode)
                    .orElseThrow(() -> new IllegalArgumentException("Term not found: " + termCode));
        } else {
            term = termRepository.findTopByOrderByUpdatedAtDesc()
                    .orElseThrow(() -> new IllegalArgumentException("No terms found in database"));
        }

        // 2. Build Specification
        Specification<Section> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // A. Mandatory Term Filter
            predicates.add(cb.equal(root.get("term").get("id"), term.getId()));

            // B. Smart Search (q) - Tokenized
            if (q != null && !q.trim().isEmpty()) {
                String normalizedQ = q.replaceAll("[-_]", " ")
                        .replaceAll("([a-zA-Z])(\\d)", "$1 $2")
                        .replaceAll("(\\d)([a-zA-Z])", "$1 $2");

                String[] tokens = normalizedQ.trim().split("\\s+");

                for (String token : tokens) {
                    String pattern = "%" + token.toLowerCase() + "%";
                    List<Predicate> orPredicates = new ArrayList<>();

                    // Match Course Fields (ManyToOne - Safe)
                    orPredicates.add(cb.like(cb.lower(root.get("course").get("title")), pattern));
                    orPredicates.add(cb.like(cb.lower(root.get("course").get("code")), pattern));
                    orPredicates.add(cb.like(cb.lower(root.get("course").get("number")), pattern));

                    // Match Section Code
                    orPredicates.add(cb.like(cb.lower(root.get("code")), pattern));

                    // Match CRN
                    try {
                        int crnVal = Integer.parseInt(token);
                        orPredicates.add(cb.equal(root.get("crn"), crnVal));
                    } catch (NumberFormatException ignored) { }

                    predicates.add(cb.or(orPredicates.toArray(new Predicate[0])));
                }
            }

            // C. Instructor Filter (Using Subquery to avoid duplicates/DISTINCT)
            if (instructor != null && !instructor.trim().isEmpty()) {
                String pattern = "%" + instructor.toLowerCase() + "%";

                // 1. Check Primary Instructor
                Predicate primaryMatch = cb.like(cb.lower(root.get("instructor").get("name")), pattern);

                // 2. Check Schedule Instructors (Subquery)
                Subquery<Schedule> subquery = query.subquery(Schedule.class);
                Root<Schedule> subRoot = subquery.from(Schedule.class);
                subquery.select(subRoot);
                subquery.where(
                        cb.equal(subRoot.get("section"), root),
                        cb.like(cb.lower(subRoot.get("instructor").get("name")), pattern)
                );

                predicates.add(cb.or(primaryMatch, cb.exists(subquery)));
            }

            // D. Days Filter (Using Subquery)
            if (days != null && !days.isEmpty()) {
                String sortedDays = sortDays(days.toUpperCase());

                Subquery<Schedule> subquery = query.subquery(Schedule.class);
                Root<Schedule> subRoot = subquery.from(Schedule.class);
                subquery.select(subRoot);
                subquery.where(
                        cb.equal(subRoot.get("section"), root),
                        cb.like(cb.upper(subRoot.get("days")), "%" + sortedDays + "%")
                );

                predicates.add(cb.exists(subquery));
            }

            // E. Time Filters (Using Subquery)
            if (startTime != null || endTime != null) {
                Subquery<Schedule> subquery = query.subquery(Schedule.class);
                Root<Schedule> subRoot = subquery.from(Schedule.class);
                subquery.select(subRoot);

                List<Predicate> timePredicates = new ArrayList<>();
                timePredicates.add(cb.equal(subRoot.get("section"), root));

                if (startTime != null) {
                    Integer min = parseTimeBytes(startTime);
                    if (min != null) timePredicates.add(cb.greaterThanOrEqualTo(subRoot.get("startTime"), min));
                }
                if (endTime != null) {
                    Integer min = parseTimeBytes(endTime);
                    if (min != null) timePredicates.add(cb.lessThanOrEqualTo(subRoot.get("endTime"), min));
                }

                subquery.where(timePredicates.toArray(new Predicate[0]));
                predicates.add(cb.exists(subquery));
            }

            // F. Standard Filters
            if (crn != null && !crn.isEmpty()) {
                try {
                    predicates.add(cb.equal(root.get("crn"), Integer.parseInt(crn)));
                } catch (NumberFormatException ignored) {}
            }

            if (level != null && !level.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("level")), "%" + level.toLowerCase() + "%"));
            }

            if (sectionCode != null && !sectionCode.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("code")), "%" + sectionCode.toLowerCase() + "%"));
            }

            // G. Gender/Branch Filter
            if (gender != null && !gender.isEmpty()) {
                String mappedGender = mapGender(gender);
                if (mappedGender != null) {
                    predicates.add(cb.like(cb.lower(root.get("branch")), "%" + mappedGender + "%"));
                }
            }

            if (branch != null && !branch.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("branch")), "%" + branch.toLowerCase() + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // 3. Pagination & Sorting
        Pageable pageable = PageRequest.of(page - 1, limit,
                Sort.by("course.code").ascending()
                        .and(Sort.by("course.number").ascending())
                        .and(Sort.by("code").ascending())
        );

        return sectionRepository.findAll(spec, pageable);
    }

    public Map<String, List<String>> getGroupedSections(String termCode) {
        Term term = resolveTerm(termCode);

        // Fetch all sections for this term
        List<Section> sections = sectionRepository.findByTermId(term.getId());

        return sections.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getCourse().getCode() + "-" + s.getCourse().getNumber(),
                        TreeMap::new,
                        Collectors.mapping(Section::getCode, Collectors.toList())
                ));
    }

    public List<InstructorHierarchyDTO> getInstructorHierarchy(String termCode) {
        Term term = resolveTerm(termCode);

        // Fetch all sections in this term
        List<Section> sections = sectionRepository.findByTermId(term.getId());

        // Group by Instructor Name first
        Map<String, List<Section>> byInstructor = sections.stream()
                .filter(s -> s.getInstructor() != null)
                .collect(Collectors.groupingBy(s -> s.getInstructor().getName()));

        List<InstructorHierarchyDTO> result = new ArrayList<>();

        for (Map.Entry<String, List<Section>> entry : byInstructor.entrySet()) {
            String instructorName = entry.getKey();
            List<Section> instructorSections = entry.getValue();

            InstructorHierarchyDTO dto = new InstructorHierarchyDTO();
            dto.setName(instructorName);
            // Grab email from the first section found for this instructor
            dto.setEmail(instructorSections.getFirst().getInstructor().getEmail());

            // Subgroup by Course
            Map<String, List<Section>> byCourse = instructorSections.stream()
                    .collect(Collectors.groupingBy(s ->
                            s.getCourse().getCode() + "-" + s.getCourse().getNumber()
                    ));

            for (Map.Entry<String, List<Section>> courseEntry : byCourse.entrySet()) {
                InstructorHierarchyDTO.CourseGroup group = new InstructorHierarchyDTO.CourseGroup();
                group.setCourseLabel(courseEntry.getKey());
                group.setCourseTitle(courseEntry.getValue().getFirst().getCourse().getTitle());

                // Collect section codes
                List<String> codes = courseEntry.getValue().stream()
                        .map(Section::getCode)
                        .sorted()
                        .collect(Collectors.toList());

                group.setSections(codes);
                dto.getCourses().add(group);
            }

            // Sort courses alphabetically
            dto.getCourses().sort(Comparator.comparing(InstructorHierarchyDTO.CourseGroup::getCourseLabel));

            result.add(dto);
        }

        // Sort instructors alphabetically
        result.sort(Comparator.comparing(InstructorHierarchyDTO::getName));
        return result;
    }

    // Helper to reuse term resolution logic
    private Term resolveTerm(String termCode) {
        if (termCode != null && !termCode.isEmpty()) {
            return termRepository.findByTermCode(termCode)
                    .orElseThrow(() -> new IllegalArgumentException("Term not found: " + termCode));
        } else {
            return termRepository.findTopByOrderByUpdatedAtDesc()
                    .orElseThrow(() -> new IllegalArgumentException("No terms found in database"));
        }
    }

    private String sortDays(String input) {
        String dayOrder = "MTWRFSU";
        return Arrays.stream(input.split(""))
                .sorted((a, b) -> dayOrder.indexOf(a) - dayOrder.indexOf(b))
                .collect(Collectors.joining());
    }

    private String mapGender(String input) {
        if ("male".equalsIgnoreCase(input)) return "طلاب";
        if ("female".equalsIgnoreCase(input)) return "طالبات";
        return null;
    }
}