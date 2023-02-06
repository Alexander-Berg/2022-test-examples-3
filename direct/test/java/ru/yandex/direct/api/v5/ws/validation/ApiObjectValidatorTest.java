package ru.yandex.direct.api.v5.ws.validation;

import java.util.Collections;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import ru.yandex.direct.api.v5.ws.TranslatableExceptionMatcher;

public class ApiObjectValidatorTest {

    private ApiObjectValidator apiObjectValidatorUnderTest;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        apiObjectValidatorUnderTest = new ApiObjectValidator();
    }

    private enum SimpleFieldNamesEnum {
        ID
    }

    private static class ClassWithRequiredField {
        @NotNull
        String field;
    }

    private static class SimpleGetRequest {
        @NotNull
        @Size(min = 1)
        List<SimpleFieldNamesEnum> fieldNames;


        @Valid
        List<ClassWithRequiredField> objects;

        @Valid
        ClassWithRequiredField child;

        @Max(10)
        int someInt;

        SimpleGetRequest withFieldNames(List<SimpleFieldNamesEnum> fieldNames) {
            this.fieldNames = fieldNames;
            return this;
        }

        SimpleGetRequest withSomeInt(final int someInt) {
            this.someInt = someInt;
            return this;
        }

        SimpleGetRequest withObjects(List<ClassWithRequiredField> objects) {
            this.objects = objects;
            return this;
        }

        SimpleGetRequest withChild(ClassWithRequiredField child) {
            this.child = child;
            return this;
        }
    }

    @Test
    public void validateValid() {
        SimpleGetRequest request =
                new SimpleGetRequest().withFieldNames(Collections.singletonList(SimpleFieldNamesEnum.ID));
        apiObjectValidatorUnderTest.validate(request);
    }

    @Test
    public void validateNotNull() {
        thrown.expect(new TranslatableExceptionMatcher(NullValueApiException.missedParameter("FieldNames")));
        apiObjectValidatorUnderTest.validate(new SimpleGetRequest());
    }

    @Test
    public void validateNotNullDeeply() {
        thrown.expect(new TranslatableExceptionMatcher(NullValueApiException.missedField("Child.Field")));
        apiObjectValidatorUnderTest.validate(
                new SimpleGetRequest()
                        .withFieldNames(Collections.singletonList(SimpleFieldNamesEnum.ID))
                        .withChild(new ClassWithRequiredField()));
    }

    @Test
    public void validateNotNullDeeplyInArray() {
        thrown.expect(new TranslatableExceptionMatcher(NullValueApiException.missedFieldInArray("Objects", "Field")));
        apiObjectValidatorUnderTest.validate(
                new SimpleGetRequest()
                        .withFieldNames(Collections.singletonList(SimpleFieldNamesEnum.ID))
                        .withObjects(Collections.singletonList(new ClassWithRequiredField())));
    }

    @Test
    public void validateInvalidSize() {
        thrown.expect(InvalidSizeApiException.class);
        thrown.expect(new TranslatableExceptionMatcher(
                new InvalidSizeApiException("FieldNames", 1, Integer.MAX_VALUE)));
        apiObjectValidatorUnderTest.validate(new SimpleGetRequest().withFieldNames(Collections.emptyList()));
    }

    @Test
    public void validateInvalidValue() {
        thrown.expect(InvalidValueApiException.class);
        thrown.expect(new TranslatableExceptionMatcher(
                new InvalidValueApiException("SomeInt")));
        apiObjectValidatorUnderTest.validate(
                new SimpleGetRequest()
                        .withFieldNames(Collections.singletonList(SimpleFieldNamesEnum.ID))
                        .withSomeInt(100));
    }

    @Test
    public void validateStabilityWhenSeveralViolationsOccurred() {
        // Тест проверяет, что при наличии нескольких проблем, отображается одна и та же
        // Если сломается, тест должен начать мигать
        thrown.expect(NullValueApiException.class);
        thrown.expect(new TranslatableExceptionMatcher(NullValueApiException.missedParameter("FieldNames")));
        apiObjectValidatorUnderTest.validate(
                new SimpleGetRequest().withSomeInt(100));
    }

}
