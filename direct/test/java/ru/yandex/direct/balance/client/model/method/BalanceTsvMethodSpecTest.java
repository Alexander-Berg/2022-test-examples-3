package ru.yandex.direct.balance.client.model.method;

import java.util.Arrays;
import java.util.List;

import com.univocity.parsers.annotations.Parsed;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

class BalanceTsvMethodSpecTest {
    public static class TestData {
        @Parsed(field = "FIELD1")
        private Long field1;

        @Parsed(field = "FIELD2")
        private Long field2;

        public Long getField1() {
            return field1;
        }

        public void setField1(Long field1) {
            this.field1 = field1;
        }

        public Long getField2() {
            return field2;
        }

        public void setField2(Long field2) {
            this.field2 = field2;
        }
    }

    @Test
    void testDeserialization() {
        BalanceTsvMethodSpec<TestData> spec = new BalanceTsvMethodSpec<>("SomeMethod", TestData.class);
        List<TestData> dataList = spec.convertResponse("FIELD2\tFIELD1\n12\t34\n56\t78\n");

        assertThat("Получили ожидаемые значения", dataList, beanDiffer(Arrays.asList(td(34, 12), td(78, 56))));
    }

    private TestData td(long f1, long f2) {
        TestData col = new TestData();
        col.setField1(f1);
        col.setField2(f2);
        return col;
    }
}
