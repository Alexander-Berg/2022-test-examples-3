package ru.yandex.direct.grid.core.util.ordering;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jooq.Field;
import org.jooq.OrderField;
import org.jooq.impl.DSL;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.grid.model.Order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RunWith(Parameterized.class)
public class JooqOrderingProcessorTest {
    private static final Field<String> STRING_FIELD = DSL.field("string", String.class);
    private static final Field<LocalDate> DATE_FIELD = DSL.field("date", LocalDate.class);
    private static final Field<BigDecimal> NUM_FIELD = DSL.field("num", BigDecimal.class);

    private static final String STR_ORD = "str";
    private static final String INT_ORD = "int";
    private static final String DATE_ORD = "date";

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

    private static final JooqOrderingProcessor<String> PROCESSOR = JooqOrderingProcessor.<String>builder()
            .withField(STR_ORD, STRING_FIELD)
            .withField(INT_ORD, NUM_FIELD)
            .withField(DATE_ORD, DATE_FIELD)
            .build();

    private static OrderingItem<String> getOrderingItem(String field, Order order) {
        return new TestOrderingItem(field, order);
    }

    @Parameterized.Parameters(name = "comparators = {0}; result = {1}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                // Нет сортировки
                {null, Collections.emptyList(), null},
                // Пустой список сортировки
                {Collections.emptyList(), Collections.emptyList(), null},

                // Сортировка только по строке
                {Collections.singletonList(getOrderingItem(STR_ORD, Order.ASC)),
                        Collections.singletonList(STRING_FIELD), null},
                {Collections.singletonList(getOrderingItem(STR_ORD, Order.DESC)),
                        Collections.singletonList(STRING_FIELD.desc()), null},

                // Сортировка по всем
                {Arrays.asList(getOrderingItem(INT_ORD, Order.ASC), getOrderingItem(STR_ORD, Order.DESC),
                        getOrderingItem(DATE_ORD, Order.DESC)),
                        Arrays.asList(NUM_FIELD, STRING_FIELD.desc(), DATE_FIELD.desc()), null},

                // Ошибка из-за несуществующего значения
                {Arrays.asList(getOrderingItem(STR_ORD, Order.ASC), getOrderingItem("somevalue", Order.DESC)),
                        null, IllegalArgumentException.class},
        });
    }

    @Parameterized.Parameter(0)
    public List<OrderingItem<String>> orderingItems;

    @Parameterized.Parameter(1)
    public List<OrderField<?>> expectedOrder;

    @Parameterized.Parameter(2)
    public Class<?> exceptionClass;

    @Test
    public void testSortCorrect() {
        if (exceptionClass == null) {
            List<OrderField<?>> sorted = PROCESSOR.construct(orderingItems);

            assertThat(sorted)
                    .isEqualTo(expectedOrder);
        } else {
            assertThatThrownBy(() -> PROCESSOR.construct(orderingItems))
                    .isInstanceOf(exceptionClass);
        }
    }
}
