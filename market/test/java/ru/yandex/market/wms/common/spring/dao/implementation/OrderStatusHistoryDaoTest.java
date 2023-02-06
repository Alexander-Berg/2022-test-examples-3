package ru.yandex.market.wms.common.spring.dao.implementation;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.model.dto.InforOrderStatusDto;
import ru.yandex.market.wms.common.model.enums.InforOrderStatusTypeToFF;
import ru.yandex.market.wms.common.model.enums.OrderStatus;
import ru.yandex.market.wms.common.model.enums.OrderType;
import ru.yandex.market.wms.common.provider.OrderProvider;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.entity.Order;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static java.util.Arrays.asList;

class OrderStatusHistoryDaoTest extends IntegrationTest {

    @Autowired
    private OrderStatusHistoryDao orderStatusHistoryDao;

    @Test
    @DatabaseSetup("/db/dao/order-status/get-order-statuses.xml")
    public void shouldFindLatestOrderStatusesByExternalOrderKey() {
        List<InforOrderStatusDto> result =
                orderStatusHistoryDao.findLatestByExternalOrderKeys(Set.of("WMSB100003001", "WMSB100003002"));

        List<Object> expected = List.of(
                new InforOrderStatusDto("B100003001", "WMSB100003001",
                        InforOrderStatusTypeToFF.PICKED_COMPLETE,
                        LocalDateTime.of(2020, 12, 1, 10, 0, 0), OrderType.STANDARD),
                new InforOrderStatusDto("B100003002", "WMSB100003002",
                        InforOrderStatusTypeToFF.PICKED_COMPLETE,
                        LocalDateTime.of(2020, 12, 1, 10, 0, 0), OrderType.STANDARD));

        Assertions.assertEquals(expected, result);
    }

    @Test
    @DatabaseSetup("/db/dao/order-status/get-order-statuses-with-68-status.xml")
    public void shouldReturnPickedCompleteIfOrderIn68StatusAndLocatedOnSortStation() {
        List<InforOrderStatusDto> result =
                orderStatusHistoryDao.findLatestByExternalOrderKeys(Collections.singleton("WMSB100003002"));

        List<Object> expected = List.of(
                new InforOrderStatusDto("B100003002", "WMSB100003002",
                        InforOrderStatusTypeToFF.PICKED_COMPLETE,
                        LocalDateTime.of(2020, 12, 1, 10, 0, 0), OrderType.STANDARD));

        Assertions.assertEquals(expected, result);
    }

    @Test
    public void shouldReturnEmptyListIfNotFound() {
        List<InforOrderStatusDto> result =
                orderStatusHistoryDao.findLatestByExternalOrderKeys(Collections.singleton("WMSB100003002"));

        Assertions.assertEquals(Collections.emptyList(), result);
    }

    @Test
    @DatabaseSetup("/db/dao/order-status/get-order-statuses-with-originorderkey.xml")
    public void shouldReturnOrderStatusWithOriginOrderKey() {
        List<InforOrderStatusDto> result =
                orderStatusHistoryDao.findLatestByExternalOrderKeys(Collections.singleton("WMSB100003001"));

        List<Object> expected = List.of(
                new InforOrderStatusDto("0100003001", "WMSB100003001",
                        InforOrderStatusTypeToFF.SORTED_BY_DELIVERY_SERVICE,
                        LocalDateTime.of(2020, 12, 1, 10, 0, 0), OrderType.STANDARD));

        Assertions.assertEquals(expected, result);
    }

    @Test
    @DatabaseSetup("/db/dao/order-status/get-order-statuses-with-different-statuses.xml")
    public void shouldReturnSingleOrderStatusWithOriginOrderKeyAndLeastStatusCode() {
        List<InforOrderStatusDto> result =
                orderStatusHistoryDao.findLatestByExternalOrderKeys(Collections.singleton("WMSB100003001"));

        List<Object> expected = List.of(
                new InforOrderStatusDto("0100003001", "WMSB100003001",
                        InforOrderStatusTypeToFF.CREATED_EXTERNALLY,
                        LocalDateTime.of(2020, 12, 1, 10, 0, 0), OrderType.STANDARD));

        Assertions.assertEquals(expected, result);
    }

    @Test
    @DatabaseSetup("/db/dao/order-status/get-order-statuses-with-with-negative.xml")
    public void shouldReturnOrderStatusWithNegativeStatus() {
        List<InforOrderStatusDto> result =
                orderStatusHistoryDao.findLatestByExternalOrderKeys(Collections.singleton("WMSB100003001"));

        List<Object> expected = List.of(
                new InforOrderStatusDto("0100003001", "WMSB100003001",
                        InforOrderStatusTypeToFF.BACKORDER,
                        LocalDateTime.of(2020, 12, 1, 10, 0, 0), OrderType.STANDARD));

        Assertions.assertEquals(expected, result);
    }


    @ParameterizedTest
    @MethodSource("createOrderHistoryArgs")
    @DatabaseSetup(value = "/db/dao/order/common.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/db/dao/order/before-with-storer-courier-all-sku.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/db/dao/order/before-cancel-order.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/db/dao/order/after-add-statuses.xml", connection = "wmwhseConnection", assertionMode
            = NON_STRICT_UNORDERED)
    public void createOrderHistoryTest(String orderKey,
                                       String orderLineNumber,
                                       String orderType) {
        orderStatusHistoryDao.createOrderHistory(orderKey, orderLineNumber, orderType);
    }

    @ParameterizedTest
    @MethodSource("createOrderHistoryWithSetArgs")
    @DatabaseSetup(value = "/db/dao/order/common.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/db/dao/order/before-with-storer-courier-all-sku.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/db/dao/order/before-cancel-order.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/db/dao/order/after-add-statuses.xml", connection = "wmwhseConnection", assertionMode
            = NON_STRICT_UNORDERED)
    public void createOrderHistoryTest(List<String> orderKeys,
                                       String orderLineNumber,
                                       String orderType) {
        orderStatusHistoryDao.createOrderHistory(orderKeys, orderLineNumber, orderType);
    }

    @ParameterizedTest
    @MethodSource("createOrderStatusHistoriesTestArgs")
    @DatabaseSetup(value = "/db/dao/order-status-history/before-create-order-histories.xml", connection =
            "wmwhseConnection")
    @ExpectedDatabase(
            value = "/db/dao/order-status-history/after-create-order-histories.xml",
            connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void createOrderStatusHistoriesTest(List<Order> orders) {
        orderStatusHistoryDao.createOrderStatusHistories(orders);
    }

    private static Stream<Arguments> createOrderHistoryArgs() {
        return Stream.of(
                Arguments.of(
                        "0000039466",
                        "00001",
                        "02"
                )
        );
    }

    private static Stream<Arguments> createOrderHistoryWithSetArgs() {
        return Stream.of(
                Arguments.of(
                        Collections.singletonList("0000039466"),
                        "00001",
                        "02"
                )
        );
    }

    private static Stream<Arguments> createOrderStatusHistoriesTestArgs() {
        return Stream.of(
                Arguments.of(
                        asList(
                                OrderProvider.getOrder(
                                        "0000039466",
                                        null,
                                        OrderStatus.UNKNOWN.getValue(),
                                        null,
                                        0
                                ),
                                OrderProvider.getOrder(
                                        "0000039467",
                                        null,
                                        OrderStatus.UNKNOWN.getValue(),
                                        null,
                                        0
                                )
                        )
                )
        );
    }
}
