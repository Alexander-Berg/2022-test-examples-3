package ru.yandex.canvas.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

import org.junit.Test;

import ru.yandex.canvas.model.validation.ValidHtml5Size;
import ru.yandex.canvas.model.validation.ValidHtml5SizesList;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ValidHtml5SizeTest {
    private static Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    public void checkOk() {
        ListValidationContainer validationContainer = new ListValidationContainer(Arrays.asList("240x400"));
        Set<ConstraintViolation<ListValidationContainer>> validationResult = validator.validate(validationContainer);
        assertTrue("Null as items is invalid", validationResult.isEmpty());
    }

    @Test
    public void checkEmptyListOk() {
        ListValidationContainer validationContainer = new ListValidationContainer(Collections.emptyList());
        Set<ConstraintViolation<ListValidationContainer>> validationResult = validator.validate(validationContainer);
        assertTrue("Null as items is invalid", validationResult.isEmpty());
    }

    @Test
    public void checkNullListOk() {
        ListValidationContainer validationContainer = new ListValidationContainer(null);
        Set<ConstraintViolation<ListValidationContainer>> validationResult = validator.validate(validationContainer);
        assertTrue("Null as items is invalid", validationResult.isEmpty());
    }

    @Test
    public void checkFail() {
        ListValidationContainer validationContainer = new ListValidationContainer(Arrays.asList("2x400"));
        Set<ConstraintViolation<ListValidationContainer>> validationResult = validator.validate(validationContainer);
        assertFalse("Null as items is invalid", validationResult.isEmpty());
    }

    @Test
    public void checkStringOk() {
        StringValidationContainer validationContainer = new StringValidationContainer("240x400");
        Set<ConstraintViolation<StringValidationContainer>> validationResult = validator.validate(validationContainer);
        assertTrue("Null as items is invalid", validationResult.isEmpty());
    }

    @Test
    public void checkStringFail() {
        StringValidationContainer validationContainer = new StringValidationContainer("200x400");
        Set<ConstraintViolation<StringValidationContainer>> validationResult = validator.validate(validationContainer);
        assertFalse("Null as items is invalid", validationResult.isEmpty());
    }

    private static class ListValidationContainer {
        @ValidHtml5SizesList
        List<String> sizes;

        public ListValidationContainer(List<String> sizes) {
            this.sizes = sizes;
        }
    }

    private static class StringValidationContainer {
        @ValidHtml5Size
        String size;

        public StringValidationContainer(String size) {
            this.size = size;
        }
    }
}
