package ru.yandex.direct.common.util;

import java.util.Collections;
import java.util.Set;

import org.apache.commons.beanutils.BeanUtils;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

public class PropertyFilterCachingTest {

    public static final String STRING_VALUE = "some string";
    public static final int INT_VALUE = 12345;
    public static final double DOUBLE_VALUE = 1029348d;

    public static class A {

        private String someString = STRING_VALUE;
        private Integer someInteger = INT_VALUE;

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
    }

    public static class B {
        private Long someLong = 1289L;
        private Double someDouble = DOUBLE_VALUE;

        public Long getSomeLong() {
            return someLong;
        }

        public void setSomeLong(Long someLong) {
            this.someLong = someLong;
        }

        public Double getSomeDouble() {
            return someDouble;
        }

        public void setSomeDouble(Double someDouble) {
            this.someDouble = someDouble;
        }
    }

    private A beanA = new A();
    private B beanB = new B();

    private PropertyFilter.PropertiesDescritor propertiesDescritor;
    private PropertyFilter testedPropertyFilter;

    @Before
    public void before() throws Exception {
        Set<String> propsA = BeanUtils.describe(beanA).keySet();
        Set<String> propsB = BeanUtils.describe(beanB).keySet();

        propertiesDescritor = mock(PropertyFilter.PropertiesDescritor.class);
        when(propertiesDescritor.describe(beanA)).thenReturn(propsA);
        when(propertiesDescritor.describe(beanB)).thenReturn(propsB);

        testedPropertyFilter = new PropertyFilter();
        testedPropertyFilter.setPropertiesDescritor(propertiesDescritor);
    }

    @Test
    public void filterProperties_FirstCall_CallsPropertiesDescriptor() {
        testedPropertyFilter.filterProperties(beanA, Collections.singletonList("someString"));
        verify(propertiesDescritor, times(1)).describe(any());
    }

    @Test
    public void filterProperties_SecondCallSameBean_DoesNotCallPropertiesDescriptor() {
        testedPropertyFilter.filterProperties(beanA, Collections.singletonList("someString"));
        testedPropertyFilter.filterProperties(beanA, Collections.singletonList("someString"));
        verify(propertiesDescritor, times(1)).describe(any());
    }

    @Test
    public void filterProperties_SecondCallSameBean_FiltersValid() {
        A expected = new A();
        expected.setSomeString(STRING_VALUE);
        expected.setSomeInteger(null);

        testedPropertyFilter.filterProperties(beanA, Collections.singletonList("someString"));
        beanA.setSomeInteger(INT_VALUE);
        testedPropertyFilter.filterProperties(beanA, Collections.singletonList("someString"));
        assertThat(beanA, beanDiffer(expected));
    }

    @Test
    public void filterProperties_SecondCallAnotherBean_CallsPropertiesDescriptor() {
        testedPropertyFilter.filterProperties(beanA, Collections.singletonList("someString"));
        testedPropertyFilter.filterProperties(beanB, Collections.singletonList("someDouble"));
        verify(propertiesDescritor, times(2)).describe(any());
    }

    @Test
    public void filterProperties_SecondCallAnotherBean_FiltersValid() {
        B expected = new B();
        expected.setSomeLong(null);
        expected.setSomeDouble(DOUBLE_VALUE);

        testedPropertyFilter.filterProperties(beanA, Collections.singletonList("someString"));
        testedPropertyFilter.filterProperties(beanB, Collections.singletonList("someDouble"));
        assertThat(beanB, beanDiffer(expected));
    }
}
