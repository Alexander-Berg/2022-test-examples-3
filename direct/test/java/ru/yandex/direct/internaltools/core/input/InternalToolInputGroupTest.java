package ru.yandex.direct.internaltools.core.input;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

import ru.yandex.direct.internaltools.core.annotations.input.Group;
import ru.yandex.direct.internaltools.core.annotations.input.Input;
import ru.yandex.direct.internaltools.core.container.InternalToolParameter;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.internaltools.core.bootstrap.InternalToolInputBootstrap.DEFAULT_GROUP_NAME;
import static ru.yandex.direct.internaltools.core.bootstrap.InternalToolInputBootstrap.DEFAULT_GROUP_PRIORITY;
import static ru.yandex.direct.internaltools.core.input.InternalToolInputTestUtil.getGroup;

/**
 * Тесты на парсинг описаний групп полей
 */
public class InternalToolInputGroupTest {
    private static final String TEST_LABEL = "Some date";
    private static final String TEST_NAME = "group name";
    private static final int TEST_PRIORITY = 10;

    public static class TestClass extends InternalToolParameter {
        @Group(name = TEST_NAME)
        @Input(label = TEST_LABEL)
        public Integer one;

        @Group(name = TEST_NAME, priority = TEST_PRIORITY)
        @Input(label = TEST_LABEL)
        public Integer two;

        @Input(label = TEST_LABEL)
        public Integer three;
    }

    /**
     * Проверяем, что умеем работать c аннотацией Group
     */
    @Test
    public void testAnnotationWithName() throws NoSuchFieldException {
        InternalToolInputGroup<TestClass> group = getGroup(TestClass.class, "one");

        assertThat(group).isNotNull()
                .as("Получен не null");

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(group.getName())
                .isEqualTo(TEST_NAME)
                .as("Имя корректно");
        soft.assertThat(group.getPriority())
                .isEqualTo(0)
                .as("Приоритет по умолчанию");
        soft.assertAll();
    }

    /**
     * Проверяем, что умеем работать c аннотацией Group с заданным приоритетом
     */
    @Test
    public void testAnnotationWithPriority() throws NoSuchFieldException {
        InternalToolInputGroup<TestClass> group = getGroup(TestClass.class, "two");

        assertThat(group).isNotNull()
                .as("Получен не null");

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(group.getName())
                .isEqualTo(TEST_NAME)
                .as("Имя корректно");
        soft.assertThat(group.getPriority())
                .isEqualTo(TEST_PRIORITY)
                .as("Приоритет из аннотации");
        soft.assertAll();
    }

    /**
     * Проверяем, что умеем работать c незаданной аннотацией Group
     */
    @Test
    public void testNoAnnotation() throws NoSuchFieldException {
        InternalToolInputGroup<TestClass> group = getGroup(TestClass.class, "three");

        assertThat(group).isNotNull()
                .as("Получен не null");

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(group.getName())
                .isEqualTo(DEFAULT_GROUP_NAME)
                .as("Имя по умолчанию");
        soft.assertThat(group.getPriority())
                .isEqualTo(DEFAULT_GROUP_PRIORITY)
                .as("Приоритет по умолчанию");
        soft.assertAll();
    }
}
