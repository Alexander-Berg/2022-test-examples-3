package ru.yandex.market.rg.asyncreport.orders.cis;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.util.DateTimes;
import ru.yandex.market.orderservice.client.model.CisDto;
import ru.yandex.market.orderservice.client.model.OrderItemsCisDto;
import ru.yandex.market.orderservice.client.model.OrderStatus;
import ru.yandex.market.rg.asyncreport.orders.cis.service.OrderCISService;
import ru.yandex.market.rg.asyncreport.orders.cis.service.OrderCISServiceImpl;
import ru.yandex.market.rg.client.orderservice.RgOrderServiceClient;
import ru.yandex.market.rg.client.orderservice.filter.CisOrderListingFilter;
import ru.yandex.market.rg.config.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Тест для {@link OrderCISServiceImpl}
 *
 * @author Zvorygin Andrey don-dron@yandex-team.ru
 */
@DbUnitDataSet(before = "OrderCISServiceTest.before.csv")
class OrderCISServiceTest extends FunctionalTest {

    private static final String ORDERS_IN_STATUS_DELIVERY =
            "000000000001,RETAIL,OTHER,ORDER,12,2021-03-09,41,952.9\n" +
                    "000000000001,RETAIL,OTHER,ORDER,12,2021-03-09,42,952.9\n" +
                    "000000000001,RETAIL,OTHER,ORDER,12,2021-03-09,43,952.9\n" +
                    "000000000001,RETAIL,OTHER,ORDER,12,2021-03-09,44,952.9\n" +
                    "000000000001,RETAIL,OTHER,ORDER,12,2021-03-09,45,952.9\n" +
                    "000000000001,RETAIL,OTHER,ORDER,13,2021-03-09,51,1062.9\n" +
                    "000000000001,RETAIL,OTHER,ORDER,13,2021-03-09,52,1062.9\n" +
                    "000000000001,RETAIL,OTHER,ORDER,13,2021-03-09,53,1062.9\n" +
                    "000000000001,RETAIL,OTHER,ORDER,13,2021-03-09,54,1062.9\n" +
                    "000000000001,RETAIL,OTHER,ORDER,13,2021-03-09,55,1062.9";

    private static final String ORDERS_IN_STATUS_DELIVERED =
            "000000000001,RETAIL,OTHER,ORDER,12,2021-03-09,01,512.9\n" +
                    "000000000001,RETAIL,OTHER,ORDER,12,2021-03-09,02,512.9\n" +
                    "000000000001,RETAIL,OTHER,ORDER,12,2021-03-09,03,512.9\n" +
                    "000000000001,RETAIL,OTHER,ORDER,12,2021-03-09,04,512.9\n" +
                    "000000000001,RETAIL,OTHER,ORDER,12,2021-03-09,05,512.9\n" +
                    "000000000001,RETAIL,OTHER,ORDER,13,2021-03-09,21,732.9\n" +
                    "000000000001,RETAIL,OTHER,ORDER,13,2021-03-09,22,732.9\n" +
                    "000000000001,RETAIL,OTHER,ORDER,13,2021-03-09,23,732.9\n" +
                    "000000000001,RETAIL,OTHER,ORDER,13,2021-03-09,24,732.9\n" +
                    "000000000001,RETAIL,OTHER,ORDER,13,2021-03-09,25,732.9";

    @Autowired
    private OrderCISService orderCISService;

    @Autowired
    private RgOrderServiceClient rgOrderServiceClient;

    private final Instant date = Instant.ofEpochSecond(LocalDate.of(2021, Month.MARCH, 8)
            .toEpochSecond(LocalTime.MIDNIGHT, ZoneOffset.UTC)).truncatedTo(ChronoUnit.DAYS);

    private final Instant nextDate = date.plus(1L, ChronoUnit.DAYS);

    @Test
    void testGetOrderItemCISBySupplierInDeliveryFromOS() {
        when(rgOrderServiceClient.streamOrderItemCIS(
                eq(CisOrderListingFilter.builder()
                        .withBusinessId(100L)
                        .withPartnerIds(List.of(774L))
                        .withFromTime(date)
                        .withToTime(nextDate)
                        .withStatuses(List.of(OrderStatus.DELIVERY))
                        .build())
        )).thenReturn(Stream.of(
                        new OrderItemsCisDto()
                                .createdAt(LocalDate.ofInstant(nextDate, DateTimes.MOSCOW_TIME_ZONE))
                                .orderId(12L)
                                .lines(List.of(
                                        new CisDto()
                                                .cis(List.of("41", "42", "43", "44", "45"))
                                                .cost(new BigDecimal("952.9")))),
                        new OrderItemsCisDto()
                                .createdAt(LocalDate.ofInstant(nextDate, DateTimes.MOSCOW_TIME_ZONE))
                                .orderId(13L)
                                .lines(List.of(
                                        new CisDto()
                                                .cis(List.of("51", "52", "53", "54", "55"))
                                                .cost(new BigDecimal("1062.9"))))
                )
        );

        testGetOrderItemCISBySupplier(false, ORDERS_IN_STATUS_DELIVERY);
    }

    @Test
    void testGetOrderItemCISBySupplierInDeliveredFromOS() {
        when(rgOrderServiceClient.streamOrderItemCIS(
                eq(CisOrderListingFilter.builder()
                        .withBusinessId(100L)
                        .withPartnerIds(List.of(774L))
                        .withFromTime(date)
                        .withToTime(nextDate)
                        .withStatuses(List.of(OrderStatus.DELIVERED))
                        .build())
        )).thenReturn(Stream.of(
                        new OrderItemsCisDto()
                                .createdAt(LocalDate.ofInstant(nextDate, DateTimes.MOSCOW_TIME_ZONE))
                                .orderId(12L)
                                .lines(List.of(
                                        new CisDto()
                                                .cis(List.of("01", "02", "03", "04", "05"))
                                                .cost(new BigDecimal("512.9")))),
                        new OrderItemsCisDto()
                                .createdAt(LocalDate.ofInstant(nextDate, DateTimes.MOSCOW_TIME_ZONE))
                                .orderId(13L)
                                .lines(List.of(
                                        new CisDto()
                                                .cis(List.of("21", "22", "23", "24", "25"))
                                                .cost(new BigDecimal("732.9"))))
                )
        );

        testGetOrderItemCISBySupplier(true, ORDERS_IN_STATUS_DELIVERED);
    }

    private void testGetOrderItemCISBySupplier(boolean delivered, String expectedData) {
        final List<OrderItemCIS> orderItemCISList =
                orderCISService.getOrderItemCISBySupplier(774, date, nextDate, delivered);

        assertEquals(expectedData, orderItemCISList
                .stream()
                .map(Object::toString)
                .collect(Collectors.joining("\n")));
    }

}
