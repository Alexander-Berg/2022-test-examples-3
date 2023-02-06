package ru.yandex.direct.grid.core.util.ordering;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.grid.model.Order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RunWith(Parameterized.class)
public class OrderingProcessorTest {
    private static final OrderingTestClass TEST_ONE = new OrderingTestClass(2, "Five");
    private static final OrderingTestClass TEST_TWO = new OrderingTestClass(3, "one");
    private static final OrderingTestClass TEST_THREE = new OrderingTestClass(1, "two");
    private static final OrderingTestClass TEST_FOUR = new OrderingTestClass(2, "Three");
    private static final OrderingTestClass TEST_FIVE = new OrderingTestClass(null, "two");
    private static final OrderingTestClass TEST_SIX = null;
    private static final String STR_FIELD = "str";
    private static final String INT_FIELD = "int";
    private static final List<OrderingTestClass> TEST_ITEMS = Arrays.asList(
            TEST_ONE, TEST_TWO, TEST_THREE, TEST_FOUR, TEST_FIVE, TEST_SIX
    );

    public static class OrderingTestClass {
        private final Integer intField;
        private final String strField;

        public OrderingTestClass(Integer intField, String strField) {
            this.intField = intField;
            this.strField = strField;
        }

        public String getStrField() {
            return strField;
        }

        public Integer getIntField() {
            return intField;
        }

        @Override
        public String toString() {
            return String.format("%s -> %s", intField, strField);
        }
    }

    public static class TestOrderingItem implements OrderingItem<String> {
        private final String field;
        private final Order order;

        public TestOrderingItem(String field, Order order) {
            this.field = field;
            this.order = order;
        }

        @Override
        public String getField() {
            return field;
        }

        @Override
        public Order getOrder() {
            return order;
        }

        @Override
        public String toString() {
            return String.format("%s -> %s", field, order);
        }
    }

    private static OrderingProcessor<String, OrderingTestClass> getProcessor(boolean nullFirst) {
        return OrderingProcessor.<String, OrderingTestClass>builder(nullFirst)
                .withFieldComparator(STR_FIELD, OrderingTestClass::getStrField)
                .withFieldComparator(INT_FIELD, OrderingTestClass::getIntField)
                .build();
    }

    private static OrderingItem<String> getOrderingItem(String field, Order order) {
        return new TestOrderingItem(field, order);
    }

    @Parameterized.Parameters(name = "nullFirst = {0}; comparators = {1}; exception = {3}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                // Нет сортировки
                {true, null,
                        Arrays.asList(TEST_ONE, TEST_TWO, TEST_THREE, TEST_FOUR, TEST_FIVE, TEST_SIX), null},
                {true, Collections.emptyList(),
                        Arrays.asList(TEST_ONE, TEST_TWO, TEST_THREE, TEST_FOUR, TEST_FIVE, TEST_SIX), null},

                // Сортировка только по строке
                {true, Collections.singletonList(getOrderingItem(STR_FIELD, Order.ASC)),
                        Arrays.asList(TEST_SIX, TEST_ONE, TEST_FOUR, TEST_TWO, TEST_THREE, TEST_FIVE), null},
                {true, Collections.singletonList(getOrderingItem(STR_FIELD, Order.DESC)),
                        Arrays.asList(TEST_THREE, TEST_FIVE, TEST_TWO, TEST_FOUR, TEST_ONE, TEST_SIX), null},
                {false, Collections.singletonList(getOrderingItem(STR_FIELD, Order.ASC)),
                        Arrays.asList(TEST_ONE, TEST_FOUR, TEST_TWO, TEST_THREE, TEST_FIVE, TEST_SIX), null},
                {true, Arrays.asList(getOrderingItem(STR_FIELD, Order.ASC), getOrderingItem("somevalue", Order.DESC)),
                        null, IllegalArgumentException.class},

                // Сортировка по строке и числу
                {true, Arrays.asList(getOrderingItem(STR_FIELD, Order.ASC), getOrderingItem(INT_FIELD, Order.ASC)),
                        Arrays.asList(TEST_SIX, TEST_ONE, TEST_FOUR, TEST_TWO, TEST_FIVE, TEST_THREE), null},
                {true, Arrays.asList(getOrderingItem(STR_FIELD, Order.ASC), getOrderingItem(INT_FIELD, Order.DESC)),
                        Arrays.asList(TEST_SIX, TEST_ONE, TEST_FOUR, TEST_TWO, TEST_THREE, TEST_FIVE), null},
                {false, Arrays.asList(getOrderingItem(STR_FIELD, Order.ASC), getOrderingItem(INT_FIELD, Order.DESC)),
                        Arrays.asList(TEST_ONE, TEST_FOUR, TEST_TWO, TEST_FIVE, TEST_THREE, TEST_SIX), null},

                // Сортировка по числу и строке
                {true, Arrays.asList(getOrderingItem(INT_FIELD, Order.ASC), getOrderingItem(STR_FIELD, Order.ASC)),
                        Arrays.asList(TEST_SIX, TEST_FIVE, TEST_THREE, TEST_ONE, TEST_FOUR, TEST_TWO), null},
                {false, Arrays.asList(getOrderingItem(INT_FIELD, Order.DESC), getOrderingItem(STR_FIELD, Order.ASC)),
                        Arrays.asList(TEST_SIX, TEST_FIVE, TEST_TWO, TEST_ONE, TEST_FOUR, TEST_THREE), null},

                // Ошибка из-за несуществующего значения
                {true, Arrays.asList(getOrderingItem(STR_FIELD, Order.ASC), getOrderingItem("somevalue", Order.DESC)),
                        null, IllegalArgumentException.class},
        });
    }

    @Parameterized.Parameter(0)
    public Boolean nullFirst;

    @Parameterized.Parameter(1)
    public List<OrderingItem<String>> orderingItems;

    @Parameterized.Parameter(2)
    public List<OrderingTestClass> expectedOrder;

    @Parameterized.Parameter(3)
    public Class<?> exceptionClass;

    @Test
    public void testSortCorrect() {
        OrderingProcessor<String, OrderingTestClass> processor = getProcessor(nullFirst);

        if (exceptionClass == null) {
            List<OrderingTestClass> sorted = processor.sort(TEST_ITEMS, orderingItems);

            assertThat(sorted)
                    .containsExactlyElementsOf(expectedOrder);
        } else {
            assertThatThrownBy(() -> processor.sort(TEST_ITEMS, orderingItems))
                    .isInstanceOf(exceptionClass);
        }
    }
}
