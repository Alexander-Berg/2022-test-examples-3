package ru.yandex.market.bidding.model;

import java.util.Set;

import javax.validation.ConstraintViolation;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

class GroupTest extends ModelValidation {

    @Test
    void nameIsNull() {
        Group group = new Group(0, 1, null, 0, 0);
        Set<ConstraintViolation<Group>> constraintViolations = validator.validate(group);
        assertEquals(1, constraintViolations.size());
        notNull(constraintViolations.iterator().next());
    }

    @Test
    void invalidId() {
        Group group = new Group(-1, 1, "ABC", 10, 0);
        Set<ConstraintViolation<Group>> constraintViolations = validator.validate(group);
        assertEquals(1, constraintViolations.size());
        ge(0, constraintViolations.iterator().next());
    }

    @Test
    void invalidShopId() {
        Group group = new Group(0, 0, "", 10, 0);
        Set<ConstraintViolation<Group>> constraintViolations = validator.validate(group);
        assertEquals(1, constraintViolations.size());
        ge(1, constraintViolations.iterator().next());
    }

    @Test
    void invalidBidsCount() {
        Group group = new Group(1, 1, "ABC", -1, 0);
        Set<ConstraintViolation<Group>> constraintViolations = validator.validate(group);
        assertEquals(1, constraintViolations.size());
        ge(0, constraintViolations.iterator().next());
    }

    @Test
    void invalidEmptyBidsCount() {
        Group group = new Group(1, 1, "ABC", 1, -1);
        Set<ConstraintViolation<Group>> constraintViolations = validator.validate(group);
        assertEquals(1, constraintViolations.size());
        ge(0, constraintViolations.iterator().next());
    }
}