package ru.yandex.direct.common.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

public class PropertyFilterTest {

    public static final String STRING_VALUE = "some string";
    public static final int INT_VALUE = 12345;
    public static final float FLOAT_VALUE = 8948.5f;

    public static class A {

        private String someString;
        private Integer someInteger;
        private Float someFloat;

        public String getSomeString() {
            return someString;
        }

        public void setSomeString(String someString) {
            this.someString = someString;
        }

        public Integer getSomeInteger() {
            return someInteger;
        }

        public void setSomeInteger(Integer someInteger) {
            this.someInteger = someInteger;
        }

        public Float getSomeFloat() {
            return someFloat;
        }

        public void setSomeFloat(Float someFloat) {
            this.someFloat = someFloat;
        }
    }

    private PropertyFilter testedPropertyFilter;
    private A beanA;

    @Before
    public void before() throws Exception {
        testedPropertyFilter = new PropertyFilter();
        beanA = getFilledABean();
    }

    @Test
    public void filterProperties_NoPropertiesToLeave_ClearsAll() {
        A expected = new A();

        testedPropertyFilter.filterProperties(beanA, new ArrayList<>());
        assertThat(beanA, beanDiffer(expected));
    }

    @Test
    public void filterProperties_SomePropertiesToLeave_ClearsSome() {
        A expected = new A();
        expected.setSomeString(STRING_VALUE);
        expected.setSomeFloat(FLOAT_VALUE);

        List<String> properties = Arrays.asList("someString", "someFloat");

        testedPropertyFilter.filterProperties(beanA, properties);
        assertThat(beanA, beanDiffer(expected));
    }

    @Test
    public void filterProperties_AllPropertiesToLeave_ClearsNothing() {
        A expected = new A();
        expected.setSomeString(STRING_VALUE);
        expected.setSomeInteger(INT_VALUE);
        expected.setSomeFloat(FLOAT_VALUE);

        List<String> properties = Arrays.asList("someString", "someFloat", "someInteger");

        testedPropertyFilter.filterProperties(beanA, properties);
        assertThat(beanA, beanDiffer(expected));
    }

    @Test
    public void filterProperties_SecondCall_FilteringValid() {
        A expected = new A();
        expected.setSomeInteger(INT_VALUE);

        List<String> properties1 = Collections.singletonList("someString");
        testedPropertyFilter.filterProperties(beanA, properties1);

        // recover bean for new filtering
        beanA = getFilledABean();

        List<String> properties2 = Collections.singletonList("someInteger");
        testedPropertyFilter.filterProperties(beanA, properties2);
        assertThat(beanA, beanDiffer(expected));
    }

    private A getFilledABean() {
        beanA = new A();
        beanA.setSomeString(STRING_VALUE);
        beanA.setSomeInteger(INT_VALUE);
        beanA.setSomeFloat(FLOAT_VALUE);
        return beanA;
    }
}
