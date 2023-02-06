package ru.yandex.market.wms.common.spring.dao.implementation;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.provider.OrderDetailProvider;
import ru.yandex.market.wms.common.provider.OrderProvider;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.OrderDaoTestData;
import ru.yandex.market.wms.common.spring.dao.entity.Order;
import ru.yandex.market.wms.common.spring.dao.entity.OrderDetail;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

class OrderDaoTest extends IntegrationTest {

    @Autowired
    protected OrderDao orderDao;

    @Test
    @DatabaseSetup(value = "/db/dao/order/data.xml", connection = "wmwhseConnection")
    public void selectOrderByExternalKey() {
        assertThat(
                orderDao.selectOrderByExternalKey("WMSB000001001"),
                is(equalTo(Optional.of(OrderDaoTestData.orderB000001001())))
        );
    }

    @Test
    @Disabled
    @DatabaseSetup(value = "/db/dao/order/data.xml", connection = "wmwhseConnection")
    public void findUnpackedOrdersAssignedToSortingStations() {
        assertThat(
                orderDao.findUnpackedOrdersAssignedToSortingStations(),
                is(equalTo(asList(OrderDaoTestData.orderB000001001(), OrderDaoTestData.orderB000001002(),
                        OrderDaoTestData.orderB000002001(), OrderDaoTestData.orderB000005001())))
        );
    }

    @ParameterizedTest
    @MethodSource("findOrdersByOrderKeysTestArgs")
    @DatabaseSetup(value = "/db/dao/order/common.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/db/dao/order/before-with-storer-courier-all-sku.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/db/dao/order/get-order-data.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/db/dao/order/get-order-data.xml",
            connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void findOrdersByOrderKeysTest(Collection<String> orderKeys,
                                          List<Order> expectedOrders) {
        List<Order> orders = orderDao.findOrdersByOrderKeys(orderKeys);

        Assertions.assertEquals(expectedOrders.size(), orders.size());

        for (int i = 0; i < expectedOrders.size(); i++) {
            Order order = orders.get(i);
            Order expectedOrder = expectedOrders.get(i);
            Assertions.assertAll(
                    () -> Assertions.assertEquals(expectedOrder.getOrderKey(), order.getOrderKey()),
                    () -> Assertions.assertEquals(expectedOrder.getExternalOrderKey(), order.getExternalOrderKey()),
                    () -> Assertions.assertEquals(expectedOrder.getStatus(), order.getStatus()),
                    () -> Assertions.assertEquals(expectedOrder.getSumQtyallocated(), order.getSumQtyallocated()),
                    () -> Assertions.assertEquals(expectedOrder.getSumQtyPicked(), order.getSumQtyPicked()),
                    () -> Assertions.assertEquals(expectedOrder.getSumQtyOpen(), order.getSumQtyOpen()),
                    () -> Assertions.assertEquals(expectedOrder.getTotalqty(), order.getTotalqty()),
                    () -> Assertions.assertEquals(expectedOrder.getDoor(), order.getDoor()),
                    () -> Assertions.assertEquals(expectedOrder.getCarrierCode(), order.getCarrierCode()),
                    () -> Assertions.assertEquals(expectedOrder.getScheduledShipDate(), order.getScheduledShipDate()),
                    () -> Assertions.assertEquals(expectedOrder.getType(), order.getType()),
                    () -> Assertions.assertEquals(expectedOrder.getTrailerKey(), order.getTrailerKey()),
                    () -> Assertions.assertEquals(
                            expectedOrder.getMaxAbsentItemsPricePercent(),
                            order.getMaxAbsentItemsPricePercent()
                    ),
                    () -> Assertions.assertEquals(expectedOrder.getRowVersion(), order.getRowVersion())
            );
        }
    }

    @ParameterizedTest
    @MethodSource("findAllOrdersForStartWithShipmentDateTimeTestArgs")
    @DatabaseSetup(value = "/db/dao/order/common.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/db/dao/order/before-with-storer-courier-all-sku.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/db/dao/order/get-order-data.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/db/dao/order/get-order-data.xml",
            connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void findAllOrdersForStartTest(Instant currentDate,
                                          Instant lastDate,
                                          List<Order> expectedOrders) {
        List<Order> orders = orderDao.findAllOrdersForStart(currentDate, lastDate);

        Assertions.assertEquals(expectedOrders.size(), orders.size());

        for (int i = 0; i < expectedOrders.size(); i++) {
            Order order = orders.get(i);
            Order expectedOrder = expectedOrders.get(i);
            Assertions.assertAll(
                    () -> Assertions.assertEquals(expectedOrder.getOrderKey(), order.getOrderKey()),
                    () -> Assertions.assertEquals(expectedOrder.getExternalOrderKey(), order.getExternalOrderKey()),
                    () -> Assertions.assertEquals(expectedOrder.getStatus(), order.getStatus()),
                    () -> Assertions.assertEquals(expectedOrder.getSumQtyallocated(), order.getSumQtyallocated()),
                    () -> Assertions.assertEquals(expectedOrder.getTotalqty(), order.getTotalqty()),
                    () -> Assertions.assertEquals(expectedOrder.getDoor(), order.getDoor()),
                    () -> Assertions.assertEquals(expectedOrder.getCarrierCode(), order.getCarrierCode()),
                    () -> Assertions.assertEquals(expectedOrder.getCarrierName(), order.getCarrierName()),
                    () -> Assertions.assertEquals(expectedOrder.getScheduledShipDate(), order.getScheduledShipDate()),
                    () -> Assertions.assertEquals(expectedOrder.getScheduledShipDateInDB(),
                            order.getScheduledShipDateInDB()),
                    () -> Assertions.assertEquals(expectedOrder.getRowVersion(), order.getRowVersion()),
                    () -> Assertions.assertEquals(expectedOrder.getStatus(), order.getStatus())
            );
        }
    }

    @ParameterizedTest
    @MethodSource("findAllOrdersForStartWithShipmentDateTimeTestArgs")
    @DatabaseSetup(value = "/db/dao/order/common.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/db/dao/order/before-with-storer-courier-all-sku.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/db/dao/order/get-order-data.xml", connection = "wmwhseConnection")
    public void findAllOrdersForStartWithShipmentDateTimeTest(Instant currentDate,
                                                              Instant lastDate,
                                                              List<Order> expectedOrders) {
        List<Order> orders = orderDao.findAllOrdersForStart(currentDate, lastDate);

        Assertions.assertEquals(expectedOrders.size(), orders.size());

        for (int i = 0; i < expectedOrders.size(); i++) {
            Order order = orders.get(i);
            Order expectedOrder = expectedOrders.get(i);
            Assertions.assertAll(
                    () -> Assertions.assertEquals(expectedOrder.getOrderKey(), order.getOrderKey()),
                    () -> Assertions.assertEquals(expectedOrder.getExternalOrderKey(), order.getExternalOrderKey()),
                    () -> Assertions.assertEquals(expectedOrder.getStatus(), order.getStatus()),
                    () -> Assertions.assertEquals(expectedOrder.getSumQtyallocated(), order.getSumQtyallocated()),
                    () -> Assertions.assertEquals(expectedOrder.getTotalqty(), order.getTotalqty()),
                    () -> Assertions.assertEquals(expectedOrder.getDoor(), order.getDoor()),
                    () -> Assertions.assertEquals(expectedOrder.getCarrierCode(), order.getCarrierCode()),
                    () -> Assertions.assertEquals(expectedOrder.getCarrierName(), order.getCarrierName()),
                    () -> Assertions.assertEquals(expectedOrder.getScheduledShipDate(), order.getScheduledShipDate()),
                    () -> Assertions.assertEquals(expectedOrder.getScheduledShipDateInDB(),
                            order.getScheduledShipDateInDB()),
                    () -> Assertions.assertEquals(expectedOrder.getShipmentDateTime(), order.getShipmentDateTime()),
                    () -> Assertions.assertEquals(expectedOrder.getRowVersion(), order.getRowVersion()),
                    () -> Assertions.assertEquals(expectedOrder.getStatus(), order.getStatus())
            );
        }
    }

    @ParameterizedTest
    @MethodSource("findAllOrdersForStartWithShipmentDateTimeTestArgs")
    @DatabaseSetup(value = "/db/dao/order/common.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/db/dao/order/before-with-storer-courier-all-sku.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/db/dao/order/get-order-data.xml", connection = "wmwhseConnection")
    public void findAllOrdersForStartWithBuildingWithShipmentDateTimeTest(Instant currentDate,
                                                                          Instant lastDate,
                                                                          List<Order> expectedOrders) {
        List<Order> orders = orderDao.findAllOrdersForStart(currentDate, lastDate, null);

        Assertions.assertEquals(expectedOrders.size(), orders.size());

        for (int i = 0; i < expectedOrders.size(); i++) {
            Order order = orders.get(i);
            Order expectedOrder = expectedOrders.get(i);
            Assertions.assertAll(
                    () -> Assertions.assertEquals(expectedOrder.getOrderKey(), order.getOrderKey()),
                    () -> Assertions.assertEquals(expectedOrder.getExternalOrderKey(), order.getExternalOrderKey()),
                    () -> Assertions.assertEquals(expectedOrder.getStatus(), order.getStatus()),
                    () -> Assertions.assertEquals(expectedOrder.getSumQtyallocated(), order.getSumQtyallocated()),
                    () -> Assertions.assertEquals(expectedOrder.getTotalqty(), order.getTotalqty()),
                    () -> Assertions.assertEquals(expectedOrder.getDoor(), order.getDoor()),
                    () -> Assertions.assertEquals(expectedOrder.getCarrierCode(), order.getCarrierCode()),
                    () -> Assertions.assertEquals(expectedOrder.getCarrierName(), order.getCarrierName()),
                    () -> Assertions.assertEquals(expectedOrder.getScheduledShipDate(), order.getScheduledShipDate()),
                    () -> Assertions.assertEquals(expectedOrder.getScheduledShipDateInDB(),
                            order.getScheduledShipDateInDB()),
                    () -> Assertions.assertEquals(expectedOrder.getShipmentDateTime(), order.getShipmentDateTime()),
                    () -> Assertions.assertEquals(expectedOrder.getRowVersion(), order.getRowVersion()),
                    () -> Assertions.assertEquals(expectedOrder.getStatus(), order.getStatus())
            );
        }
    }

    @ParameterizedTest
    @MethodSource("setStatusForNotSuitableOrdersForAosTestArgs")
    @DatabaseSetup(value = "/db/dao/order/common.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/db/dao/order/before-with-storer-courier-all-sku.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/db/dao/order/get-order-data.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/db/dao/order/after-update-statuses-for-aos.xml",
            connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void setStatusForNotSuitableOrdersForAosTest(List<Order> orders,
                                                        String editor,
                                                        int expectedUpdates) {
        int totalUpdates = orderDao.setStatusForNotSuitableOrdersForAos(orders, editor);
        Assertions.assertEquals(expectedUpdates, totalUpdates);
    }

    @ParameterizedTest
    @MethodSource("selectOrderDetailsByExternalOrderKeyArgs")
    @DatabaseSetup(value = "/db/dao/order/common.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/db/dao/order/before-with-storer-courier-all-sku.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/db/dao/order/before-cancel-order.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/db/dao/order/before-cancel-order.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED
    )
    public void selectOrderDetailsByExternalOrderKeyTest(String externOrderKey,
                                                         List<OrderDetail> expectedOrderDetails) {
        List<OrderDetail> orderDetails = orderDao.selectOrderDetailsByExternalOrderKey(externOrderKey);

        Assertions.assertEquals(expectedOrderDetails, orderDetails);
    }

    @ParameterizedTest
    @MethodSource("getCountFromPickDetailByKeyTestArgs")
    @DatabaseSetup(value = "/db/dao/order/common.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/db/dao/order/before-with-storer-courier-all-sku.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/db/dao/order/before-cancel-order.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/db/dao/order/before-cancel-order.xml",
            connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void getCountFromPickDetailByKeyTest(List<String> orderKeys,
                                                int expectedCount) {
        int count = orderDao.getCountFromPickDetailByKey(orderKeys);

        Assertions.assertEquals(expectedCount, count);
    }

    @ParameterizedTest
    @MethodSource("getCancelableStatusesTestArgs")
    @DatabaseSetup(value = "/db/dao/order/common.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/db/dao/order/before-with-storer-courier-all-sku.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/db/dao/order/before-cancel-order.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/db/dao/order/before-cancel-order.xml",
            connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void getCancelableStatusesTest(List<String> orderKeys,
                                          List<String> expectedCancelableStatuses) {
        List<String> cancelableStatuses = orderDao.getCancelableStatuses(orderKeys);

        Assertions.assertEquals(expectedCancelableStatuses, cancelableStatuses);
    }

    @ParameterizedTest
    @MethodSource("getOrderTypeByOrderKeyTestArgs")
    @DatabaseSetup(value = "/db/dao/order/common.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/db/dao/order/before-with-storer-courier-all-sku.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/db/dao/order/before-cancel-order.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/db/dao/order/before-cancel-order.xml",
            connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void getOrderTypeByOrderKeyTest(String orderKey,
                                           String expectedOrderType) {
        String orderType = orderDao.getOrderTypeByOrderKey(orderKey);

        Assertions.assertEquals(expectedOrderType, orderType);
    }

    @Test
    @DatabaseSetup(value = "/db/dao/order/common.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/db/dao/order/before-with-storer-courier-all-sku.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/db/dao/order/before-cancel-order.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/db/dao/order/after-cancel-order-1.xml",
            connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void cancelOrdersByKeyTest() {
        List<String> orderKeys = Collections.singletonList("0000039466");

        orderDao.cancelOrdersByKey(orderKeys);
    }

    @Test
    @DatabaseSetup(value = "/db/dao/order/common.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/db/dao/order/before-with-storer-courier-all-sku.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/db/dao/order/before-cancel-order.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/db/dao/order/after-cancel-order-2.xml",
            connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void cancelOrdersByKeyTestWhenTwoOrderKeys() {
        List<String> orderKeys = asList("A000039455", "B000039455");

        orderDao.cancelOrdersByKey(orderKeys);
    }

    @Test
    @DatabaseSetup(value = "/db/dao/order/common.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/db/dao/order/before-with-storer-courier-all-sku.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/db/dao/order/get-order-data.xml", connection = "wmwhseConnection")
    public void findOrderByKeyWithAlmostEmptyFields() {

        assertThat(
                orderDao.findOrderByOrderKey("B100003001"),
                is(equalTo(Optional.of(OrderDaoTestData.orderB100003001())))
        );
    }

    @Test
    @DatabaseSetup(value = "/db/dao/order/common.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/db/dao/order/before-with-storer-courier-all-sku.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/db/dao/order/get-order-data.xml", connection = "wmwhseConnection")
    public void findOrderByKeyWithPartlyEmptyFields() {
        assertThat(
                orderDao.findOrderByOrderKey("B100003002"),
                is(equalTo(Optional.of(OrderDaoTestData.orderB100003002())))
        );
    }

    @Test
    @DatabaseSetup(value = "/db/dao/order/before-clear-batchordernumber.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/db/dao/order/after-clear-batchordernumber.xml",
            connection = "wmwhseConnection",
            assertionMode = NON_STRICT
    )
    public void clearBatchOrderNumberByOrderKeys() {

        orderDao.clearBatchOrderNumberByOrderKeys(Arrays.asList("0001", "0002"));
    }

    private static Stream<Arguments> selectOrderDetailsByExternalOrderKeyArgs() {
        return Stream.of(
                Arguments.of(
                        "22899370",
                        OrderDetailProvider.getOrderDetailsForOrdinalWarehouse()
                ),
                Arguments.of(
                        "22899375",
                        OrderDetailProvider.getOrderDetailsForSuperWarehouse()
                )
        );
    }

    private static Stream<Arguments> getCountFromPickDetailByKeyTestArgs() {
        return Stream.of(
                Arguments.of(
                        Collections.singletonList("0000039466"),
                        0
                ), Arguments.of(
                        Collections.singletonList("0000039500"),
                        1
                )
        );
    }

    private static Stream<Arguments> getCancelableStatusesTestArgs() {
        return Stream.of(
                Arguments.of(
                        Collections.singletonList("0000039466"),
                        Collections.singletonList("1")
                ), Arguments.of(
                        Collections.singletonList("0000039500"),
                        Collections.singletonList("0")
                ), Arguments.of(
                        asList("A000039455", "B000039455"),
                        Collections.singletonList("1")
                ), Arguments.of(
                        asList("A000039456", "B000039456"),
                        asList("1", "0")
                ), Arguments.of(
                        asList("A000039457", "B000039457"),
                        Collections.singletonList("0")
                )
        );
    }

    private static Stream<Arguments> getOrderTypeByOrderKeyTestArgs() {
        return Stream.of(
                Arguments.of(
                        "0000039466",
                        "0"
                ), Arguments.of(
                        "0000039500",
                        "100"
                )
        );
    }

    private static Stream<Arguments> findOrdersByOrderKeysTestArgs() {
        return Stream.of(
                Arguments.of(
                        asList("B100003001", "B100003002"),
                        asList(
                                OrderProvider.getOrder(
                                        "B100003001",
                                        "WMSB100003001",
                                        "55",
                                        new BigDecimal("99.00"),
                                        0
                                ),
                                OrderProvider.getOrder(
                                        "B100003002",
                                        "WMSB100003002",
                                        "55",
                                        new BigDecimal("1.00"),
                                        0
                                )
                        )
                )
        );
    }

    private static Stream<Arguments> findAllOrdersForStartWithShipmentDateTimeTestArgs() {
        Instant current = Instant.parse("2021-09-21T10:00:00Z");
        Instant last = Instant.parse("2021-09-23T10:00:00Z");
        return Stream.of(
                Arguments.of(
                        current,
                        last,
                        asList(
                                OrderProvider.getOrder(
                                        "B100003010",
                                        "WMSB100003011",
                                        "02",
                                        0,
                                        null,
                                        OffsetDateTime.of(2021, 9, 22,
                                                16, // так как WAREHOUSE_TIMEZONE = 'Europe/Moscow'
                                                0, 0, 0, ZoneOffset.ofHours(3)),
                                        OffsetDateTime.of(2020, 12, 1, 10, 0, 0, 0, ZoneOffset.ofHours(3)),
                                        OffsetDateTime.of(2021, 9, 22, 13, 0, 0, 0, ZoneOffset.UTC)
                                ), OrderProvider.getOrder(
                                        "B100003111",
                                        "WMSB100003121",
                                        "02",
                                        4,
                                        null,
                                        OffsetDateTime.of(2021, 9, 22,
                                                16, // так как WAREHOUSE_TIMEZONE = 'Europe/Moscow'
                                                0, 0, 0, ZoneOffset.ofHours(3)),
                                        OffsetDateTime.of(2020, 12, 1, 10, 0, 0, 0, ZoneOffset.ofHours(3)),
                                        OffsetDateTime.of(2021, 9, 22, 13, 0, 0, 0, ZoneOffset.UTC)
                                )
                        )
                )
        );
    }

    private static Stream<Arguments> setStatusForNotSuitableOrdersForAosTestArgs() {
        return Stream.of(
                Arguments.of(
                        asList(
                                OrderProvider.getOrder(
                                        "B100003010",
                                        "WMSB100003011",
                                        "02",
                                        new BigDecimal("99.00"),
                                        0
                                ),
                                OrderProvider.getOrder(
                                        "B100003111",
                                        "WMSB100003121",
                                        "02",
                                        new BigDecimal("99.00"),
                                        4
                                )
                        ),
                        "test",
                        2
                )
        );
    }
}
