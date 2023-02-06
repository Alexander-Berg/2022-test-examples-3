package ru.yandex.market.bidding.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import javax.validation.ConstraintViolation;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

class PageTest extends ModelValidation {

    @Test
    void invalidFrom() {
        Page page = new Page(-1, 0, 0, Collections.EMPTY_LIST);
        Set<ConstraintViolation<Page>> constraintViolations = validator.validate(page);
        assertEquals(1, constraintViolations.size());
        ge(0, constraintViolations.iterator().next());
    }

    @Test
    void invalidTo() {
        Page page = new Page(0, -1, 0, Collections.EMPTY_LIST);
        Set<ConstraintViolation<Page>> constraintViolations = validator.validate(page);
        assertEquals(1, constraintViolations.size());
        ge(0, constraintViolations.iterator().next());
    }

    @Test
    void invalidCount() {
        Page page = new Page(0, 0, -1, Collections.EMPTY_LIST);
        Set<ConstraintViolation<Page>> constraintViolations = validator.validate(page);
        assertEquals(1, constraintViolations.size());
        ge(0, constraintViolations.iterator().next());
    }

    @Test
    void invalidItems() throws Exception {
        Page page = new Page(0, 0, 1, Arrays.asList(new Group(1, 1, "ABC", 10, 0), new Group(-1, 1, "ABC", 10, 0)));
        Set<ConstraintViolation<Page>> constraintViolations = validator.validate(page);
        assertEquals(1, constraintViolations.size());
        ge(0, constraintViolations.iterator().next());
    }

    @Test
    void empty() {
        Page p = Page.of(1, 10, 0, Collections.emptyList());
        assertEquals(0, p.from);
        assertEquals(0, p.to);
    }

    @Test
    void fromIsBiggerThanTo() {
        Page p = Page.of(10, 1, 100, Collections.emptyList());
        assertEquals(0, p.from);
        assertEquals(0, p.to);
    }

    @Test
    void resultIsLessThanRequested() {
        Page p = Page.of(1, 10, 1, Arrays.asList(new Group(1, 1, "ABC", 10, 0), new Group(-1, 1, "ABC", 10, 0)));
        assertEquals(1, p.from);
        assertEquals(1, p.to);
    }
}