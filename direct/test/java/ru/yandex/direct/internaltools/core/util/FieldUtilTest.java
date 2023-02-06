package ru.yandex.direct.internaltools.core.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

import ru.yandex.direct.internaltools.core.exception.InternalToolProcessingException;

import static org.assertj.core.api.Assertions.assertThat;

public class FieldUtilTest {
    public static class TestClass {
        private String justFieldWOGetter;
        private boolean check;
        public boolean checkPublic;
        private String justField;
        @JsonProperty("name_for_field")
        private String jsonPropertyField;
        private String jsonPropertyGetterField;

        public boolean isCheck() {
            return check;
        }

        public String getJustField() {
            return justField;
        }

        public String getJsonPropertyField() {
            return jsonPropertyField;
        }

        @JsonProperty("name_for_getter")
        public String getJsonPropertyGetterField() {
            return jsonPropertyGetterField;
        }
    }

    @Test
    public void testGetName() throws NoSuchFieldException {
        String name = FieldUtil.getFieldName(TestClass.class, TestClass.class.getDeclaredField("justField"));
        assertThat(name).isEqualTo("justField");
    }

    @Test
    public void testGetNameJsonProperty() throws NoSuchFieldException {
        String name = FieldUtil.getFieldName(TestClass.class, TestClass.class.getDeclaredField("jsonPropertyField"));
        assertThat(name).isEqualTo("name_for_field");
    }

    @Test
    public void testGetNameJsonPropertyGetter() throws NoSuchFieldException {
        String name =
                FieldUtil.getFieldName(TestClass.class, TestClass.class.getDeclaredField("jsonPropertyGetterField"));
        assertThat(name).isEqualTo("name_for_getter");
    }

    @Test
    public void testGetAccessor() throws NoSuchFieldException, InvocationTargetException, IllegalAccessException {
        Method method = FieldUtil.getAccessor(TestClass.class, TestClass.class.getDeclaredField("justField"));

        assertThat(method).isNotNull();

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(method.getName()).isEqualTo("getJustField");
        TestClass tc = new TestClass();
        tc.justField = "data";
        soft.assertThat(method.invoke(tc)).isEqualTo("data");
        soft.assertAll();
    }

    @Test
    public void testGetAccessorBoolean()
            throws NoSuchFieldException, InvocationTargetException, IllegalAccessException {
        Method method = FieldUtil.getAccessor(TestClass.class, TestClass.class.getDeclaredField("check"));

        assertThat(method).isNotNull();

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(method.getName()).isEqualTo("isCheck");
        TestClass tc = new TestClass();
        tc.check = true;
        soft.assertThat(method.invoke(tc)).isEqualTo(true);
        soft.assertAll();
    }

    @Test
    public void testGetAccessorNull() throws NoSuchFieldException, InvocationTargetException, IllegalAccessException {
        Method method = FieldUtil.getAccessor(TestClass.class, TestClass.class.getDeclaredField("justFieldWOGetter"));

        assertThat(method).isNull();
    }

    @Test
    public void testGetExtractorGetter()
            throws NoSuchFieldException, InvocationTargetException, IllegalAccessException {
        InternalToolFieldExtractor extractor =
                FieldUtil.getExtractor(TestClass.class, TestClass.class.getDeclaredField("justField"), String.class);

        TestClass tc = new TestClass();
        tc.justField = "data";
        assertThat(extractor.getValue(tc)).isEqualTo("data");
    }

    @Test
    public void testGetExtractorProp() throws NoSuchFieldException, InvocationTargetException, IllegalAccessException {
        InternalToolFieldExtractor extractor =
                FieldUtil.getExtractor(TestClass.class, TestClass.class.getDeclaredField("checkPublic"), boolean.class);

        TestClass tc = new TestClass();
        tc.checkPublic = true;
        assertThat(extractor.getValue(tc)).isEqualTo(true);
    }

    @Test(expected = InternalToolProcessingException.class)
    public void testGetExtractorException()
            throws NoSuchFieldException, InvocationTargetException, IllegalAccessException {
        FieldUtil.getExtractor(TestClass.class, TestClass.class.getDeclaredField("justFieldWOGetter"), String.class);
    }
}
