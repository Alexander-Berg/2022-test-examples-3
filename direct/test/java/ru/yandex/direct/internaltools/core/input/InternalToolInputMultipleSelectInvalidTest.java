package ru.yandex.direct.internaltools.core.input;

import java.util.List;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import ru.yandex.direct.internaltools.core.annotations.input.Input;
import ru.yandex.direct.internaltools.core.annotations.input.MultipleSelect;
import ru.yandex.direct.internaltools.core.container.InternalToolParameter;
import ru.yandex.direct.internaltools.core.exception.InternalToolInitialisationException;

import static ru.yandex.direct.internaltools.core.input.InternalToolInputTestUtil.getInput;

/**
 * Тесты на ошибки парсинга описаний полей с выбором нескольких элементов
 */
public class InternalToolInputMultipleSelectInvalidTest {

    private static final String TEST_LABEL = "Test label";

    private static final String ENUM_CHOICE_ONE = "ONE"; //TestEnum.ONE
    private static final String ENUM_CHOICE_TWO = "TWO"; //TestEnum.TWO
    private static final String CHOICE_ONE = "test1";
    private static final String CHOICE_TWO = "test2";

    public enum EmptyTestEnum {
    }

    public enum TestEnum {
        ONE,
        TWO
    }

    public static class TestClass extends InternalToolParameter {

        @MultipleSelect(choices = ENUM_CHOICE_ONE)
        @Input(label = TEST_LABEL)
        public Set<EmptyTestEnum> emptyEnumMultipleSelect;

        @MultipleSelect(choices = {ENUM_CHOICE_ONE, ENUM_CHOICE_TWO})
        @Input(label = TEST_LABEL)
        public Set<TestEnum> enumMultipleSelectWithChoices;

        @MultipleSelect
        @Input(label = TEST_LABEL)
        public Set<String> stringMultipleSelectWithoutChoices;

        @MultipleSelect(choices = {CHOICE_ONE, CHOICE_TWO})
        @Input(label = TEST_LABEL)
        public List<String> invalidType;

        @MultipleSelect(choices = {CHOICE_ONE, CHOICE_TWO})
        @Input(label = TEST_LABEL)
        public Set<Double> invalidTypeOfSet;

        @MultipleSelect(choices = {CHOICE_ONE, CHOICE_TWO})
        @Input(label = TEST_LABEL)
        public Set<Long> invalidChoicesTypeForLongSet;
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();


    @Test
    public void getException_WhenEmptyEnumMultipleSelect() throws NoSuchFieldException {
        thrown.expect(InternalToolInitialisationException.class);
        thrown.expectMessage("Empty enums not supported");

        getInput(TestClass.class, "emptyEnumMultipleSelect");
    }

    @Test
    public void getException_WhenEnumMultipleSelectWithChoices() throws NoSuchFieldException {
        thrown.expect(InternalToolInitialisationException.class);
        thrown.expectMessage("Choices not supported for enum MultipleSelect");

        getInput(TestClass.class, "enumMultipleSelectWithChoices");
    }

    @Test
    public void getException_WhenStringMultipleSelectWithoutChoices() throws NoSuchFieldException {
        thrown.expect(InternalToolInitialisationException.class);
        thrown.expectMessage("Empty selects not supported");

        getInput(TestClass.class, "stringMultipleSelectWithoutChoices");
    }

    @Test
    public void getException_WhenInvalidType() throws NoSuchFieldException {
        thrown.expect(InternalToolInitialisationException.class);
        thrown.expectMessage("invalidType field has invalid type or annotations");

        getInput(TestClass.class, "invalidType");
    }

    @Test
    public void getException_WhenInvalidTypeOfSet() throws NoSuchFieldException {
        thrown.expect(InternalToolInitialisationException.class);
        thrown.expectMessage("invalidTypeOfSet field has invalid type or annotations");

        getInput(TestClass.class, "invalidTypeOfSet");
    }

    @Test
    public void getException_WhenInvalidChoicesTypeForLongSet() throws NoSuchFieldException {
        thrown.expect(InternalToolInitialisationException.class);
        thrown.expectMessage("Invalid type of choices for long multiSelect");

        getInput(TestClass.class, "invalidChoicesTypeForLongSet");
    }
}
