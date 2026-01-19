package com.kaustack.catalog.repository;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SearchCriteria {
    private String key;       // e.g., "crn" or "course.code"
    private String operation; // e.g., ":", ">", "<"
    private Object value;     // e.g., "10293" or "CS"
}