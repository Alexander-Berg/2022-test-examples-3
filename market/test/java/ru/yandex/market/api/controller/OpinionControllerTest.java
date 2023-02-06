package ru.yandex.market.api.controller;

import org.junit.Test;
import ru.yandex.market.api.error.ValidationErrors;
import ru.yandex.market.api.integration.UnitTestBase;

import static junit.framework.Assert.*;

public class OpinionControllerTest extends UnitTestBase {

    // TODO Split to small tests
    @Test
    public void shouldGradeParserWorksWell() {
        ValidationErrors errors = new ValidationErrors();
        Parameters.GRADE_WITH_ERROR_PARSER.get("", errors);
        assertTrue(errors.isEmpty());

        errors = new ValidationErrors();
        assertNull(Parameters.GRADE_WITH_ERROR_PARSER.get("all", errors));
        assertTrue(errors.isEmpty());

        errors = new ValidationErrors();
        assertNull(Parameters.GRADE_WITH_ERROR_PARSER.get("*", errors));
        assertFalse(errors.isEmpty());

        errors = new ValidationErrors();
        assertNull(Parameters.GRADE_WITH_ERROR_PARSER.get("-3", errors));
        assertFalse(errors.isEmpty());

        errors = new ValidationErrors();
        assertNull(Parameters.GRADE_WITH_ERROR_PARSER.get("3", errors));
        assertFalse(errors.isEmpty());

        errors = new ValidationErrors();
        assertEquals(2, (int) Parameters.GRADE_WITH_ERROR_PARSER.get("2", errors));
        assertTrue(errors.isEmpty());

        errors = new ValidationErrors();
        assertEquals(-2, (int) Parameters.GRADE_WITH_ERROR_PARSER.get("-2", errors));
        assertTrue(errors.isEmpty());

        errors = new ValidationErrors();
        assertEquals(0, (int) Parameters.GRADE_WITH_ERROR_PARSER.get("0", errors));
        assertTrue(errors.isEmpty());
    }

}