package ru.yandex.direct.internaltools.core.input;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.assertj.core.api.SoftAssertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import ru.yandex.direct.internaltools.core.annotations.input.Input;
import ru.yandex.direct.internaltools.core.container.InternalToolParameter;
import ru.yandex.direct.internaltools.core.exception.InternalToolInitialisationException;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.internaltools.core.input.InternalToolInputTestUtil.getInput;

/**
 * Тесты на базовую функциональность генерации описания поля ввода
 */
public class InternalToolInputGeneralTest {
    private static final String TEST_DESCRIPTION = "Some info about field";

    public static class TestClass extends InternalToolParameter {
        private Long fieldNoInput;
        @Input(label = "no getter")
        private Long fieldNoGetter;
        @Input(label = "unsupported type")
        private byte unknownField;
        @Input(label = "public field")
        public String publicField;
        @Input(label = "private field with getter", description = TEST_DESCRIPTION)
        private String fieldWithGetter;
        @Input(label = "private boolean field with getter")
        private boolean booleanFieldWithGetter;
        @Input(label = "json property field", required = false)
        @JsonProperty("json_property_field")
        public String jsonPropertyField;
        @JsonProperty("error name")
        @Input(label = "some label", required = false)
        public String errorInName;
        @Input(label = "json property field with getter")
        private String jsonPropertyFieldWithGetter;

        public byte getUnknownField() {
            return unknownField;
        }

        public String getFieldWithGetter() {
            return fieldWithGetter;
        }

        public boolean isBooleanFieldWithGetter() {
            return booleanFieldWithGetter;
        }

        @JsonProperty("json_property_field_with_getter")
        public String getJsonPropertyFieldWithGetter() {
            return jsonPropertyFieldWithGetter;
        }
    }

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    /**
     * Проверяем, что публичное проаннотированное поле успешно парсится
     */
    @Test
    public void testPublicFieldSuccess() throws NoSuchFieldException {
        InternalToolInput<TestClass, ?> input = getInput(TestClass.class, "publicField");

        assertThat(input).isNotNull();
        SoftAssertions soft = new SoftAssertions();

        soft.assertThat(input.getName())
                .isEqualTo("publicField")
                .as("Имя поля взяли из класса");
        soft.assertThat(input.isRequired())
                .isTrue()
                .as("Параметр задан по-умолчанию");
        soft.assertThat(input.getDescription())
                .isEmpty();

        soft.assertAll();
    }

    /**
     * Проверяем, что поле с JsonProperty успешно парсится
     */
    @Test
    public void testJsonPropertySuccess() throws NoSuchFieldException {
        InternalToolInput<TestClass, ?> input = getInput(TestClass.class, "jsonPropertyField");

        assertThat(input).isNotNull();
        SoftAssertions soft = new SoftAssertions();

        soft.assertThat(input.getName())
                .isEqualTo("json_property_field")
                .as("Имя поля взяли из аннотации");
        soft.assertThat(input.isRequired())
                .isFalse()
                .as("Параметр задан в аннотации");
        soft.assertThat(input.getDescription())
                .isEmpty();

        soft.assertAll();
    }

    /**
     * Проверяем, что поле с JsonProperty успешно парсится
     */
    @Test(expected = InternalToolInitialisationException.class)
    public void testJsonPropertyInvalidNameError() throws NoSuchFieldException {
        getInput(TestClass.class, "errorInName");
    }

    /**
     * Проверяем, что поле с JsonProperty на геттере успешно парсится
     */
    @Test
    public void testJsonPropertyOnGetterSuccess() throws NoSuchFieldException {
        InternalToolInput<TestClass, ?> input = getInput(TestClass.class, "jsonPropertyFieldWithGetter");

        assertThat(input).isNotNull();
        SoftAssertions soft = new SoftAssertions();

        soft.assertThat(input.getName())
                .isEqualTo("json_property_field_with_getter")
                .as("Имя поля взяли из аннотации");
        soft.assertThat(input.isRequired())
                .isTrue()
                .as("Параметр задан по-умолчанию");
        soft.assertThat(input.getDescription())
                .isEmpty();

        soft.assertAll();
    }

    /**
     * Проверяем что приватное поле с геттером успешно парсится
     */
    @Test
    public void testFieldWithGetterSuccess() throws NoSuchFieldException {
        InternalToolInput<TestClass, ?> input = getInput(TestClass.class, "fieldWithGetter");

        assertThat(input).isNotNull();
        SoftAssertions soft = new SoftAssertions();

        soft.assertThat(input.getName())
                .isEqualTo("fieldWithGetter")
                .as("Имя поля взяли из класса");
        soft.assertThat(input.isRequired())
                .isTrue()
                .as("Параметр задан по-умолчанию");
        soft.assertThat(input.getDescription())
                .isEqualTo(TEST_DESCRIPTION)
                .as("Параметр задан в аннотации");

        soft.assertAll();
    }

    /**
     * Проверяем что boolean-поле с геттером, начинающимся "is" успешно парсится.
     */
    @Test
    public void testBooleanFieldWithGetterSuccess() throws NoSuchFieldException {
        InternalToolInput<TestClass, ?> input = getInput(TestClass.class, "booleanFieldWithGetter");

        assertThat(input).isNotNull();
    }

    /**
     * Проверяем что при парсинге поля без аннотации @Input возвращаем null
     */
    @Test
    public void testFieldNoInputIsNull() throws NoSuchFieldException {
        InternalToolInput<TestClass, ?> input = getInput(TestClass.class, "fieldNoInput");

        assertThat(input).isNull();
    }

    /**
     * Проверям, что падаем на проанннотированном приватном поле без геттера
     */
    @Test(expected = InternalToolInitialisationException.class)
    public void testFieldNoGetterError() throws NoSuchFieldException {
        getInput(TestClass.class, "fieldNoGetter");
    }

    /**
     * Проверяем, что падаем на проанннотированном поле, с типом которого не умеем работать
     */
    @Test(expected = InternalToolInitialisationException.class)
    public void testUnknownTypeFieldError() throws NoSuchFieldException {
        getInput(TestClass.class, "unknownField");
    }
}
