package ru.yandex.direct.model;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class ModelChangesTest {
    private static final long MODEL_ID = 1L;
    private static final String NEW_DESC = "new desc";
    private static final String NEW_NAME = "new name";

    ModelChanges<TestClass> changes = new ModelChanges<>(MODEL_ID, TestClass.class);

    @Test
    public void process_NullValue_MarksPropertyAsChanged() {
        changes.process(null, TestClass.NAME);
        assertThat(changes.isPropChanged(TestClass.NAME), is(true));
    }

    @Test
    public void process_NotNullValue_MarksPropertyAsChanged() {
        changes.process(NEW_NAME, TestClass.NAME);
        assertThat(changes.isPropChanged(TestClass.NAME), is(true));
    }

    @Test
    public void process_NullValue_SavesNewValue() {
        changes.process(null, TestClass.NAME);
        assertThat(changes.getChangedProp(TestClass.NAME), nullValue());
    }

    @Test
    public void process_NotNullValue_SavesNewValue() {
        changes.process(NEW_NAME, TestClass.NAME);
        assertThat(changes.getChangedProp(TestClass.NAME), is(NEW_NAME));
    }


    @Test
    public void processNotNull_NullValue_DoesNotMarkPropertyAsChanged() {
        changes.processNotNull(null, TestClass.NAME);
        assertThat(changes.isPropChanged(TestClass.NAME), is(false));
    }

    @Test
    public void processNotNull_NullValueWithTransformer_DoesNotMarkPropertyAsChanged() {
        changes.processNotNull((String) null, TestClass.NAME, String::toUpperCase);
        assertThat(changes.isPropChanged(TestClass.NAME), is(false));
    }

    @Test
    public void processNotNull_NotNullValue_MarksPropertyAsChanged() {
        changes.processNotNull(NEW_DESC, TestClass.DESCRIPTION);
        assertThat(changes.isPropChanged(TestClass.DESCRIPTION), is(true));
    }

    @Test
    public void processNotNull_NotNullValue_SavesNewValue() {
        changes.processNotNull(NEW_NAME, TestClass.NAME);
        assertThat(changes.getChangedProp(TestClass.NAME), is(NEW_NAME));
    }

    @Test
    public void processNotNull_NotNullValueWithTransformer_AppliesTransformer() {
        changes.processNotNull(NEW_NAME, TestClass.NAME, String::toUpperCase);
        assertThat(changes.getChangedProp(TestClass.NAME), is(NEW_NAME.toUpperCase()));
    }


    @Test(expected = IllegalArgumentException.class)
    public void getChangedProp_UnchangedProperty_ThrowsException() {
        changes.processNotNull(null, TestClass.NAME);
        changes.getChangedProp(TestClass.NAME);
    }


    @Test
    public void replaceWithoutId_ChangedProperty_AppliesModifier() {
        changes.processNotNull(NEW_NAME, TestClass.NAME);
        changes.replace(TestClass.NAME, str -> str.toUpperCase());
        assertThat(changes.getChangedProp(TestClass.NAME), is(NEW_NAME.toUpperCase()));
    }

    @Test
    public void replaceWithoutId_ChangedProperty_PassExistingValueToModifier() {
        changes.processNotNull(NEW_NAME, TestClass.NAME);
        changes.replace(TestClass.NAME, str -> {
            assertThat(str, is(NEW_NAME));
            return str;
        });
    }

    @Test
    public void replaceWithoutId_ChangedProperty_ReturnsModifiedValue() {
        changes.processNotNull(NEW_NAME, TestClass.NAME);
        String newValue = changes.replace(TestClass.NAME, str -> str.toUpperCase());
        assertThat(newValue, is(NEW_NAME.toUpperCase()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void replaceWithoutId_UnchangedProperty_ThrowsException() {
        changes.processNotNull(null, TestClass.NAME);
        changes.replace(TestClass.NAME, str -> str.toUpperCase());
    }


    @Test
    public void replaceWithId_ChangedProperty_AppliesModifier() {
        changes.processNotNull(NEW_NAME, TestClass.NAME);
        changes.replace(TestClass.NAME, (id, str) -> str.toUpperCase());
        assertThat(changes.getChangedProp(TestClass.NAME), is(NEW_NAME.toUpperCase()));
    }

    @Test
    public void replaceWithId_ChangedProperty_PassExistingValueToModifier() {
        changes.processNotNull(NEW_NAME, TestClass.NAME);
        changes.replace(TestClass.NAME, (id, str) -> {
            assertThat(str, is(NEW_NAME));
            return str;
        });
    }

    @Test
    public void replaceWithId_ChangedProperty_PassValidIdToModifier() {
        changes.processNotNull(NEW_NAME, TestClass.NAME);
        changes.replace(TestClass.NAME, (id, str) -> {
            assertThat(id, is(MODEL_ID));
            return str;
        });
    }

    @Test
    public void replaceWithId_ChangedProperty_ReturnsModifiedValue() {
        changes.processNotNull(NEW_NAME, TestClass.NAME);
        String newValue = changes.replace(TestClass.NAME, (id, str) -> str.toUpperCase());
        assertThat(newValue, is(NEW_NAME.toUpperCase()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void replaceWithId_UnchangedProperty_ThrowsException() {
        changes.processNotNull(null, TestClass.NAME);
        changes.replace(TestClass.NAME, (id, str) -> str.toUpperCase());
    }

    @Test
    public void toModelsWorks() {
        changes.processNotNull(NEW_NAME, TestClass.NAME);
        Assertions.assertThat(changes.toModel())
                .isEqualToComparingFieldByFieldRecursively(new TestClass(MODEL_ID, NEW_NAME));
    }
}
