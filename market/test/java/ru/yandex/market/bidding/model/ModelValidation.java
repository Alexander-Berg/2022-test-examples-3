package ru.yandex.market.bidding.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.junit.jupiter.api.BeforeEach;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created with IntelliJ IDEA.
 * User: snoop
 * Date: 9/4/14
 * Time: 3:31 PM
 */
public abstract class ModelValidation {

    protected Validator validator;

    @BeforeEach
    public void before() {
        validator = createValidator();
    }

    protected Validator createValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        return factory.getValidator();
    }

    protected <T> void eqSet(List<String> expectedMessages, Set<ConstraintViolation<T>> constraintViolations) {
        expectedMessages = new ArrayList<>(expectedMessages);
        List<String> actualMessages = constraintViolations.stream().
                map(ConstraintViolation::getMessage).collect(Collectors.toList());
        expectedMessages.removeAll(actualMessages);
        assertTrue(expectedMessages.toString(), expectedMessages.isEmpty());
    }

    protected <T> void ge(long bound, ConstraintViolation<T> violation) {
        assertEquals("{javax.validation.constraints.Min.message}", violation.getMessageTemplate());
        assertEquals(bound, violation.getConstraintDescriptor().getAttributes().get("value"));
    }

    protected <T> void le(long bound, ConstraintViolation<T> violation) {
        assertEquals("{javax.validation.constraints.Max.message}", violation.getMessageTemplate());
        assertEquals(bound, violation.getConstraintDescriptor().getAttributes().get("value"));
    }

    protected <T> void notNull(ConstraintViolation<T> violation) {
        assertEquals("{javax.validation.constraints.NotNull.message}", violation.getMessageTemplate());
    }

    protected <T> void badSize(ConstraintViolation<T> violation, int min, int max) {
        assertEquals("{javax.validation.constraints.Size.message}", violation.getMessageTemplate());
        assertEquals(min, violation.getConstraintDescriptor().getAttributes().get("min"));
        assertEquals(max, violation.getConstraintDescriptor().getAttributes().get("max"));
    }
}
