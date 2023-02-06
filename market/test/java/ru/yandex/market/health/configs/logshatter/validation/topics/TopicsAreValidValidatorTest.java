package ru.yandex.market.health.configs.logshatter.validation.topics;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ParametersAreNonnullByDefault
class TopicsAreValidValidatorTest {

    public static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    public void testNull() {
        BeanClass bean = new BeanClass(null);
        Set<ConstraintViolation<BeanClass>> result = VALIDATOR.validate(bean);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testEmpty() {
        BeanClass bean = new BeanClass(Collections.emptyList());
        Set<ConstraintViolation<BeanClass>> result = VALIDATOR.validate(bean);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testValidSingleton() {
        BeanClass bean = new BeanClass(Collections.singletonList("a--b"));
        Set<ConstraintViolation<BeanClass>> result = VALIDATOR.validate(bean);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testInvalidSingleton() {
        BeanClass bean = new BeanClass(Collections.singletonList("a"));
        Set<ConstraintViolation<BeanClass>> result = VALIDATOR.validate(bean);
        assertFalse(result.isEmpty());
    }

    public static class BeanClass {
        @TopicsAreValid
        @Nullable
        private final List<String> topics;

        BeanClass(@Nullable List<String> topics) {
            this.topics = topics;
        }

        public List<String> getTopics() {
            return topics;
        }
    }
}
