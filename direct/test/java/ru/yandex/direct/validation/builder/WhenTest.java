package ru.yandex.direct.validation.builder;

import org.junit.Test;

import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class WhenTest {

    @Test
    public void isValid_ValidationResultHasNoErrorsAndWarnings_True() {
        Object value = new Object();
        boolean whenResult = When.isValid().apply(new ValidationResult<>(value));
        assertThat(whenResult, is(true));
    }

    @Test
    public void isValid_ValidationResultHasErrors_False() {
        Object value = new Object();
        ValidationResult<Object, Object> vr = new ValidationResult<>(value);
        vr.addError(new Object());
        boolean whenResult = When.isValid().apply(vr);
        assertThat(whenResult, is(false));
    }

    @Test
    public void isValid_ValidationResultHasWarnings_True() {
        Object value = new Object();
        ValidationResult<Object, Object> vr = new ValidationResult<>(value);
        vr.addWarning(new Object());
        boolean whenResult = When.isValid().apply(vr);
        assertThat(whenResult, is(true));
    }

    @Test
    public void notNull_ValueIsNotNull() {
        Object value = new Object();
        ValidationResult<Object, Object> vr = new ValidationResult<>(value);
        boolean whenResult = When.notNull().apply(vr);
        assertThat(whenResult, is(true));
    }

    @Test
    public void isValidBoth_NoErrors_HasErrors_False() {
        ValidationResult<Object, Object> vr1 = new ValidationResult<>(new Object());
        ValidationResult<Object, Object> vr2 = new ValidationResult<>(new Object())
                .addError(new Object());
        ItemValidationBuilder<Object, Object> builder2 = new ItemValidationBuilder<>(vr2);
        boolean whenResult = When.isValidBoth(builder2).apply(vr1);
        assertThat(whenResult, is(false));
    }

    @Test
    public void isValidBoth_HasErrors_NoErrors_False() {
        ValidationResult<Object, Object> vr1 = new ValidationResult<>(new Object())
                .addError(new Object());
        ValidationResult<Object, Object> vr2 = new ValidationResult<>(new Object());
        ItemValidationBuilder<Object, Object> builder2 = new ItemValidationBuilder<>(vr2);
        boolean whenResult = When.isValidBoth(builder2).apply(vr1);
        assertThat(whenResult, is(false));
    }

    @Test
    public void isValidBoth_NoErrors_NoErrors_True() {
        ValidationResult<Object, Object> vr1 = new ValidationResult<>(new Object());
        ValidationResult<Object, Object> vr2 = new ValidationResult<>(new Object());
        ItemValidationBuilder<Object, Object> builder2 = new ItemValidationBuilder<>(vr2);
        boolean whenResult = When.isValidBoth(builder2).apply(vr1);
        assertThat(whenResult, is(true));
    }

    @Test
    public void notNull_ValueIsNull() {
        ValidationResult<Object, Object> vr = ValidationResult.success(null);
        boolean whenResult = When.notNull().apply(vr);
        assertThat(whenResult, is(false));
    }
}
