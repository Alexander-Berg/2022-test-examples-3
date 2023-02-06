package ru.yandex.market.order;

import java.time.LocalDate;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.order.matchers.OrderFraudInfoMatchers;
import ru.yandex.market.order.model.OrderFraudInfo;

import static org.junit.Assert.assertThat;

class OrderFraudDaoTest extends FunctionalTest {

    private static final LocalDate UPDATED_DATE_2020_08_04 = LocalDate.of(2020, 8, 4);
    private static final long BUYER_UID = 123;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private OrderFraudDao orderFraudDao;

    @BeforeEach
    void setUp() {
        orderFraudDao = new OrderFraudDao(jdbcTemplate);
    }

    @Test
    @DbUnitDataSet(before = "db/OrderFraudDaoTest.testUpdatedDate.before.csv")
    @DisplayName("Проверка импорта данных до перехода в Approval")
    void testUpdatedDate() {
        List<OrderFraudInfo> actual = orderFraudDao.getNotApprovedNewStatusOrders(
                List.of(
                        makeSimpleOrderFraudInfo(1L),
                        makeSimpleOrderFraudInfo(2L),
                        makeSimpleOrderFraudInfo(3L)
                ),
                UPDATED_DATE_2020_08_04
        );

        assertThat(actual, Matchers.hasSize(2));
        assertThat(
                actual,
                Matchers.contains(
                        OrderFraudInfoMatchers.hasOrderId(2L),
                        OrderFraudInfoMatchers.hasOrderId(3L)
                )
        );
    }

    @Test
    @DbUnitDataSet(before = "db/OrderFraudDaoTest.testAnyFlagChanged.before.csv")
    @DisplayName("Проверка импорта только измененных статусов")
    void testAnyFlagChanged() {
        List<OrderFraudInfo> actual = orderFraudDao.getNotApprovedNewStatusOrders(
                List.of(
                        makeSimpleOrderFraudInfo(1L),
                        makeSimpleOrderFraudInfo(2L),
                        makeSimpleOrderFraudInfo(3L),
                        makeSimpleOrderFraudInfo(4L),
                        makeSimpleOrderFraudInfo(5L),
                        makeSimpleOrderFraudInfo(6L),
                        makeSimpleOrderFraudInfo(7L)
                ),
                UPDATED_DATE_2020_08_04
        );

        assertThat(actual, Matchers.hasSize(6));
        assertThat(
                actual,
                Matchers.contains(
                        OrderFraudInfoMatchers.hasOrderId(1L),
                        OrderFraudInfoMatchers.hasOrderId(3L),
                        OrderFraudInfoMatchers.hasOrderId(4L),
                        OrderFraudInfoMatchers.hasOrderId(5L),
                        OrderFraudInfoMatchers.hasOrderId(6L),
                        OrderFraudInfoMatchers.hasOrderId(7L)
                )
        );
    }

    @Test
    @DbUnitDataSet(before = "db/OrderFraudDaoTest.testNotBilledOrders.before.csv")
    @DisplayName("Проверка импорта не обилленых заказов")
    void testNotBilledOrders() {
        List<OrderFraudInfo> actual = orderFraudDao.getNotApprovedNewStatusOrders(
                List.of(
                        makeSimpleOrderFraudInfo(1L),
                        makeSimpleOrderFraudInfo(2L),
                        makeSimpleOrderFraudInfo(3L)
                ),
                UPDATED_DATE_2020_08_04
        );

        assertThat(actual, Matchers.hasSize(2));
        assertThat(
                actual,
                Matchers.contains(
                        OrderFraudInfoMatchers.hasOrderId(1L),
                        OrderFraudInfoMatchers.hasOrderId(3L)
                )
        );
    }

    private static OrderFraudInfo makeSimpleOrderFraudInfo(long orderId) {
        return OrderFraudInfo.builder()
                .setOrderId(orderId)
                .setBuyerUid(BUYER_UID)
                .setUpdatedAt(UPDATED_DATE_2020_08_04)
                .setFirstOrder(false)
                .setOverLimit(false)
                .setOrderFraud(false)
                .build();
    }
}
