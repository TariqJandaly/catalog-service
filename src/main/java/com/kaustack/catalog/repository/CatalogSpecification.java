package com.kaustack.catalog.repository;

import jakarta.persistence.criteria.*;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.domain.Specification;

public class CatalogSpecification<T> implements Specification<T> {

    private final SearchCriteria criteria;

    public CatalogSpecification(SearchCriteria criteria) {
        this.criteria = criteria;
    }

    @Override
    public Predicate toPredicate(@NonNull Root<T> root, @NonNull CriteriaQuery<?> query, @NonNull CriteriaBuilder builder) {
        Path<Object> path;

        if (criteria.getKey().contains(".")) {
            String[] split = criteria.getKey().split("\\.");
            path = root.join(split[0]).get(split[1]);
        } else {
            path = root.get(criteria.getKey());
        }

        String op = criteria.getOperation();
        String val = criteria.getValue().toString();

        if (op.equalsIgnoreCase(">")) {
            return builder.greaterThanOrEqualTo(path.as(String.class), val);
        }
        else if (op.equalsIgnoreCase("<")) {
            return builder.lessThanOrEqualTo(path.as(String.class), val);
        }
        else if (op.equalsIgnoreCase(":")) {
            if (path.getJavaType() == String.class) {
                return builder.like(
                        builder.lower(path.as(String.class)),
                        "%" + val.toLowerCase() + "%"
                );
            } else {
                return builder.equal(path, val);
            }
        }
        return null;
    }
}