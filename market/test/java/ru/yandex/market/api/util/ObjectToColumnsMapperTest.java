package ru.yandex.market.api.util;

import org.junit.Test;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.test.ExceptionMatcher;

import static org.junit.Assert.*;
import static ru.yandex.market.api.util.functional.Functionals.convertAndSet;

/**
 * Created by apershukov on 05.01.17.
 */
public class ObjectToColumnsMapperTest extends UnitTestBase {

    private static class Class1 {

        private String field1;
        private int field2;

        public String getField1() {
            return field1;
        }

        public void setField1(String field1) {
            this.field1 = field1;
        }

        public int getField2() {
            return field2;
        }

        public void setField2(int field2) {
            this.field2 = field2;
        }
    }

    private static final ObjectToColumnsMapper<Class1> CLASS1_MAPPER = ObjectToColumnsMapper.<Class1>builder()
        .supplier(Class1::new)
        .column(Class1::setField1, Class1::getField1)
        .column(convertAndSet(NumericUtil::getSafeInt, Class1::setField2), o -> String.valueOf(o.getField2()))
        .build();

    @Test
    public void testMapSimpleObjectToString() {
        Class1 object = new Class1();
        object.setField1("qwer");
        object.setField2(45);

        String[] array = CLASS1_MAPPER.map(object);
        assertArrayEquals(new String[]{"qwer", "45"}, array);
    }

    @Test
    public void testMapStringsToObject() {
        Class1 object = CLASS1_MAPPER.map(new String[]{"qwer", "45"});

        assertNotNull(object);
        assertEquals("qwer", object.getField1());
        assertEquals(45, object.getField2());
    }

    @Test
    public void testThrowExceptionIfRowLengthDiffers() {
        exception.expect(new ExceptionMatcher<IllegalArgumentException>() {

            @Override
            protected boolean match(IllegalArgumentException e) {
                return "Unexpected row length. Expected: 2, Found: 3".equals(e.getMessage());
            }
        });

        CLASS1_MAPPER.map(new String[]{"qwer", "45", "row-mapper"});
    }

    @Test
    public void testIgnoredColumnOnToObject() {
        ObjectToColumnsMapper<Class1> mapper = ObjectToColumnsMapper.<Class1>builder()
            .supplier(Class1::new)
            .column(Class1::setField1, Class1::getField1)
            .ignoredColumn()
            .column(convertAndSet(NumericUtil::getSafeInt, Class1::setField2), o -> String.valueOf(o.getField2()))
            .build();

        Class1 object = mapper.map(new String[]{"qwer", "to-ignore", "45"});

        assertNotNull(object);
        assertEquals("qwer", object.getField1());
        assertEquals(45, object.getField2());
    }

    @Test
    public void testIgnoredColumnOnToColumn() {
        ObjectToColumnsMapper<Class1> mapper = ObjectToColumnsMapper.<Class1>builder()
            .supplier(Class1::new)
            .column(Class1::setField1, Class1::getField1)
            .ignoredColumn()
            .column(convertAndSet(NumericUtil::getSafeInt, Class1::setField2), o -> String.valueOf(o.getField2()))
            .build();

        Class1 object = new Class1();
        object.setField1("qwer");
        object.setField2(45);

        String[] array = mapper.map(object);
        assertArrayEquals(new String[]{"qwer", null, "45"}, array);
    }

    @Test(expected = NullPointerException.class)
    public void testNPEOnBuildMapperWithoutSupplier() {
        ObjectToColumnsMapper.<Class1>builder()
            .column(Class1::setField1, Class1::getField1)
            .ignoredColumn()
            .column(convertAndSet(NumericUtil::getSafeInt, Class1::setField2), o -> String.valueOf(o.getField2()))
            .build();
    }
}
