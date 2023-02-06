package ru.yandex.direct.api.v5.converter;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.model.ModelProperty;
import ru.yandex.direct.model.ModelWithId;

import static org.assertj.core.api.Assertions.assertThat;

public class ModelChangesHelperTest {
    private static final String NEW_VALUE = "aaa";

    private ModelChanges<ModelForTest> changes;
    private JAXBElement<String> jaxbElement;

    @Before
    public void before() {
        changes = new ModelChanges<>(1L, ModelForTest.class);
        jaxbElement = new JAXBElement<>(new QName("", "SomeField"), String.class, NEW_VALUE);
    }

    @Test
    public void processJaxbElement_NullJaxbElement_NoChanges() {
        ModelChangesHelper.processJaxbElement(changes, null, ModelForTest.SOME_FIELD);
        assertThat(changes.isPropChanged(ModelForTest.SOME_FIELD)).isFalse();
    }

    @Test
    public void processJaxbElement_NilJaxbElement_NewValueIsNull() {
        jaxbElement.setNil(true);
        ModelChangesHelper.processJaxbElement(changes, jaxbElement, ModelForTest.SOME_FIELD);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(changes.isPropChanged(ModelForTest.SOME_FIELD)).isTrue();
            softly.assertThat(changes.getChangedProp(ModelForTest.SOME_FIELD)).isNull();
        });
    }

    @Test
    public void processJaxbElement_NullValueAtJaxbElement_NewValueIsNull() {
        jaxbElement.setValue(null);
        ModelChangesHelper.processJaxbElement(changes, jaxbElement, ModelForTest.SOME_FIELD);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(changes.isPropChanged(ModelForTest.SOME_FIELD)).isTrue();
            softly.assertThat(changes.getChangedProp(ModelForTest.SOME_FIELD)).isNull();
        });
    }

    @Test
    public void processJaxbElement_JaxbElement_CheckNewValue() {
        ModelChangesHelper.processJaxbElement(changes, jaxbElement, ModelForTest.SOME_FIELD);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(changes.isPropChanged(ModelForTest.SOME_FIELD)).isTrue();
            softly.assertThat(changes.getChangedProp(ModelForTest.SOME_FIELD)).isEqualTo(NEW_VALUE);
        });
    }

    @SuppressWarnings("WeakerAccess")
    private static class ModelForTest implements ModelWithId {
        private String someField;
        private Long id;

        public static final ModelProperty<ModelForTest, String> SOME_FIELD = ModelProperty.create(
                ModelForTest.class, "someField", ModelForTest::getSomeField, ModelForTest::setSomeField);

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public void setId(Long id) {
            this.id = id;
        }

        public String getSomeField() {
            return someField;
        }

        public void setSomeField(String someField) {
            this.someField = someField;
        }
    }
}
