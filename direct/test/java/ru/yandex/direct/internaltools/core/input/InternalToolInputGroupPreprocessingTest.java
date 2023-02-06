package ru.yandex.direct.internaltools.core.input;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.internaltools.core.annotations.input.Group;
import ru.yandex.direct.internaltools.core.annotations.input.Input;
import ru.yandex.direct.internaltools.core.container.InternalToolParameter;
import ru.yandex.direct.validation.builder.ItemValidationBuilder;
import ru.yandex.direct.validation.result.Defect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * Тесты на парсинг описаний групп полей
 */
public class InternalToolInputGroupPreprocessingTest {
    private static final String TEST_LABEL = "Some date";
    private static final String TEST_NAME = "group name";
    private static final int TEST_PRIORITY = 10;

    public static class TestClass extends InternalToolParameter {
        @Group(name = TEST_NAME, priority = TEST_PRIORITY)
        @Input(label = TEST_LABEL)
        public Integer one;
    }

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private InternalToolInput<TestClass, Integer> input;
    private InternalToolInputGroup<TestClass> group;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        group = new InternalToolInputGroup<>(TEST_NAME, TEST_PRIORITY, Collections.singletonList(input));
    }

    @Test
    public void testInputPreparedForSending() {
        List<InternalToolInput<TestClass, ?>> inputList = group.getInputList();
        assertThat(inputList)
                .size().isEqualTo(1);

        verify(input).applyPreProcessors();
    }

    @Test
    public void testInputPreparedForValidating() {
        TestClass param = new TestClass();
        ItemValidationBuilder<TestClass, Defect> validationBuilder = ItemValidationBuilder.of(param);
        group.addValidation(validationBuilder, param);

        verify(input).addValidation(eq(validationBuilder), eq(param));
    }
}
