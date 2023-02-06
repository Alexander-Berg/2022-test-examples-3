package ru.yandex.market.core.billing;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.order.OrderTrantimeFilter;
import ru.yandex.market.core.order.model.MbiBlueOrderType;
import ru.yandex.market.core.order.model.OrderTrantime;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.market.core.fulfillment.model.DeliveryEventType.FEE;
import static ru.yandex.market.core.fulfillment.model.DeliveryEventType.FF_PROCESSING;

public class OrderTrantimeDaoTest extends FunctionalTest {

    @Autowired
    private OrderTrantimeDao dao;

    private static Stream<Arguments> argumentsForFilterByBillingDate() {
        return Stream.of(
                Arguments.of(
                        MbiBlueOrderType.FULFILLMENT,
                        Set.of(equalTo(1L)),
                        "Поиск для FULFILLMENT"
                ),
                Arguments.of(
                        MbiBlueOrderType.DROP_SHIP,
                        Set.of(equalTo(2L)),
                        "Поиск для DROP_SHIP"
                ),
                Arguments.of(
                        MbiBlueOrderType.DROP_SHIP_BY_SELLER,
                        Set.of(equalTo(4L)),
                        "Поиск для DROP_SHIP_BY_SELLER"
                )
        );
    }

    @DbUnitDataSet(before = "OrderTrantimeDaoTest.testGet.before.csv")
    @ParameterizedTest(name = "{2}")
    @MethodSource("argumentsForFilterByBillingDate")
    public void testGetOrderTrantimesByDate(
            MbiBlueOrderType orderType,
            Set<Matcher<? super Long>> expectedOrderIds,
            String description
    ) {
        List<OrderTrantime> trantimes = dao.getOrderTrantimes(
                OrderTrantimeFilter.byBillingDate(
                        FF_PROCESSING,
                        LocalDate.of(2018, Month.FEBRUARY, 10),
                        orderType
                )
        );

        List<Long> actualOrderIds = trantimes.stream().map(OrderTrantime::getOrderId).collect(Collectors.toList());

        assertThat(actualOrderIds, containsInAnyOrder(expectedOrderIds));
    }

    @Test
    @DbUnitDataSet(before = "OrderTrantimeDaoTest.testGet.before.csv")
    public void testGetOrderTrantimesByOrderIds() {
        List<OrderTrantime> trantimes = dao.getOrderTrantimes(
                OrderTrantimeFilter.byOrderIds(FEE, Arrays.asList(1L, 3L))
        );

        assertThat(trantimes.size(), is(1));
        OrderTrantime orderTrantime = trantimes.get(0);
        assertThat(orderTrantime.getOrderId(), is(3L));
        assertThat(orderTrantime.getDeliveryEventType(), is(FEE));
        assertThat(orderTrantime.getTrantime(), is(LocalDateTime.of(2018, Month.FEBRUARY, 10, 5, 2, 1)));
    }

}
