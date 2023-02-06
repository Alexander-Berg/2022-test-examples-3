package ru.yandex.direct.model;

import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.beanutils.BeanUtils;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.model.TestClass.NAME;

public class ModelPropertyTest {

    @Test
    public void create_CreatesOnlyOneInstanceOfEachProperty() {
        ModelProperty<TestClass, String> property =
                ModelProperty.create(TestClass.class, "name", TestClass::getName, TestClass::setName);
        assertThat(property, sameInstance(NAME));
    }

    @Test(expected = IllegalArgumentException.class)
    public void create_IncorrectNameCausesException() {
        ModelProperty.create(TestClass.class, "namee", TestClass::getName, TestClass::setName);
    }

    @Test
    public void set_SetsModelProperty() throws Exception {
        TestClass testObj = new TestClass(null, "value-1");
        String expectedValue = "new-value";
        NAME.set(testObj, expectedValue);
        String actualValue = NAME.get(testObj);
        assertThat(actualValue, equalTo(expectedValue));
    }

    @Test
    public void set_SetsBeanProperty() throws Exception {
        TestClass testObj = new TestClass(null, "value-1");
        String expectedValue = "new-value";
        NAME.set(testObj, expectedValue);
        String actualValue = BeanUtils.getProperty(testObj, NAME.name());
        assertThat(actualValue, equalTo(expectedValue));
    }

    @Test
    public void get_GetsBeanProperty() throws Exception {
        TestClass testObj = new TestClass(null, "value-1");
        String expectedValue = "new-value";
        BeanUtils.setProperty(testObj, NAME.name(), expectedValue);
        String actualValue = NAME.get(testObj);
        assertThat(actualValue, equalTo(expectedValue));
    }

    @Test
    public void getRaw_worksLikeGet() throws Exception {
        TestClass testObj = new TestClass(null, "value-" + ThreadLocalRandom.current().nextDouble());
        assertThat(NAME.getRaw(testObj), is(NAME.get(testObj)));
    }

    @Test
    public void copyRaw_works() throws Exception {
        var src = new TestClass(null, "value-" + ThreadLocalRandom.current().nextDouble());
        var dst = new TestClass();

        NAME.copyRaw(src, dst);

        assertThat(NAME.getRaw(dst), is(NAME.get(src)));
    }
}
