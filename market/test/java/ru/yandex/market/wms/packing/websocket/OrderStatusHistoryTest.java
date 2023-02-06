package ru.yandex.market.wms.packing.websocket;

import java.time.Instant;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseTearDown;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.wms.common.model.enums.OrderStatus;
import ru.yandex.market.wms.common.spring.config.BaseTestConfig;
import ru.yandex.market.wms.common.spring.config.IntegrationTestConfig;
import ru.yandex.market.wms.common.spring.dao.entity.OrderHistory;
import ru.yandex.market.wms.common.spring.dao.implementation.OrderDao;
import ru.yandex.market.wms.common.spring.dao.implementation.OrderStatusHistoryDao;
import ru.yandex.market.wms.packing.LocationsRov;
import ru.yandex.market.wms.packing.enums.PackingSourceType;
import ru.yandex.market.wms.packing.integration.PackingIntegrationTest;
import ru.yandex.market.wms.packing.utils.PackingTaskDataset;
import ru.yandex.market.wms.packing.utils.Parcel;

import static com.github.springtestdbunit.annotation.DatabaseOperation.DELETE_ALL;
import static com.github.springtestdbunit.annotation.DatabaseOperation.INSERT;
import static org.assertj.core.api.Assertions.assertThat;

@Disabled
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {BaseTestConfig.class, IntegrationTestConfig.class},
        properties = {"check.authentication=mock"})
public class OrderStatusHistoryTest extends PackingIntegrationTest {

    private static final String USER = "TEST";
    private static final String ORDERKEY = "ORD0001";

    private static final OrderHistory SETUP_HISTORY_58 = setupHistory(OrderStatus.IN_SORTING);
    private static final OrderHistory SETUP_HISTORY_59 = setupHistory(OrderStatus.SORTING_COMPLETE);
    private static final OrderHistory SETUP_HISTORY_61 = setupHistory(OrderStatus.IN_PACKING);
    private static final OrderHistory SETUP_HISTORY_92 = setupHistory(OrderStatus.PART_SHIPPED);

    @Autowired
    private OrderDao orderDao;

    @Autowired
    private OrderStatusHistoryDao orderStatusHistoryDao;

    @Test
    @DatabaseSetup(value = "/db/locations_setup_rov.xml", type = INSERT)
    @DatabaseSetup(value = "/db/base_setup.xml", type = INSERT)
    @DatabaseSetup(value = "/db/integration/websocket/history/nothing-shipped.xml", type = INSERT)
    @DatabaseTearDown(value = "/db/tear-down.xml", type = DELETE_ALL)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void nothingShipped() throws Exception {
        createPackingFlow()
                .connect(USER, LocationsRov.TABLE_1)
                .packSortable(PackingTaskDataset.of(
                        Parcel.builder().orderKey(ORDERKEY).uits(List.of("UID0001"))
                                .parcelId("P000000501").parcelNumber(1).carton("YMA")
                                .validationAfterParcelClosed(() -> {
                                    assertThat(orderDao.getStatus(ORDERKEY)).isEqualTo(OrderStatus.IN_PACKING);
                                    assertThat(orderStatusHistoryDao.getForOrderWithDetails(ORDERKEY)).containsExactly(
                                            SETUP_HISTORY_58, SETUP_HISTORY_59, history61(USER));
                                }),
                        Parcel.builder().orderKey(ORDERKEY).uits(List.of("UID0002"))
                                .parcelId("P000000502").parcelNumber(2).carton("YMA")
                                .validationAfterParcelClosed(() -> {
                                    assertThat(orderDao.getStatus(ORDERKEY)).isEqualTo(OrderStatus.IN_PACKING);
                                    assertThat(orderStatusHistoryDao.getForOrderWithDetails(ORDERKEY)).containsExactly(
                                            SETUP_HISTORY_58, SETUP_HISTORY_59, history61(USER), history61(USER));
                                }),
                        Parcel.builder().orderKey(ORDERKEY).uits(List.of("UID0003"))
                                .parcelId("P000000503").parcelNumber(3).carton("YMA").isLast(true)
                                .validationAfterParcelClosed(() -> {
                                    assertThat(orderDao.getStatus(ORDERKEY)).isEqualTo(OrderStatus.PACKED);
                                    assertThat(orderDao.getStatus(ORDERKEY)).isEqualTo(OrderStatus.PACKED);
                                    assertThat(orderStatusHistoryDao.getForOrderWithDetails(ORDERKEY)).containsExactly(
                                            SETUP_HISTORY_58, SETUP_HISTORY_59, history61(USER), history61(USER),
                                            history65line1(USER), history65(USER));
                                })
                ))
                .disconnect();
    }

    @Test
    @DatabaseSetup(value = "/db/locations_setup_rov.xml", type = INSERT)
    @DatabaseSetup(value = "/db/base_setup.xml", type = INSERT)
    @DatabaseSetup(value = "/db/integration/websocket/history/part-shipped.xml", type = INSERT)
    @DatabaseTearDown(value = "/db/tear-down.xml", type = DELETE_ALL)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void partShipped() throws Exception {
        createPackingFlow()
                .connect(USER, LocationsRov.TABLE_1)
                .packSortable(PackingTaskDataset.of(
                        Parcel.builder().orderKey(ORDERKEY).uits(List.of("UID0001"))
                                .parcelId("P000000501").parcelNumber(2).carton("YMA")
                                .validationAfterParcelClosed(() -> {
                                    assertThat(orderDao.getStatus(ORDERKEY)).isEqualTo(OrderStatus.PART_SHIPPED);
                                    assertThat(orderStatusHistoryDao.getForOrderWithDetails(ORDERKEY)).containsExactly(
                                            SETUP_HISTORY_61, SETUP_HISTORY_92, history61(USER), history92(USER));
                                }),
                        Parcel.builder().orderKey(ORDERKEY).uits(List.of("UID0002"))
                                .parcelId("P000000502").parcelNumber(3).carton("YMA").isLast(true)
                                .validationAfterParcelClosed(() -> {
                                    assertThat(orderDao.getStatus(ORDERKEY)).isEqualTo(OrderStatus.PART_SHIPPED);
                                    assertThat(orderStatusHistoryDao.getForOrderWithDetails(ORDERKEY)).containsExactly(
                                            SETUP_HISTORY_61, SETUP_HISTORY_92, history61(USER), history92(USER),
                                            history65(USER), history92(USER));
                                })
                ))
                .disconnect();
    }


    private static OrderHistory history61(String user) {
        return history(user, OrderStatus.IN_PACKING, "");
    }

    private static OrderHistory history65(String user) {
        return history(user, OrderStatus.PACKED, "");
    }

    private static OrderHistory history65line1(String user) {
        return history(user, OrderStatus.PACKED, "00001");
    }

    private static OrderHistory history92(String user) {
        OrderHistory history = history(user, OrderStatus.PART_SHIPPED, "");
        return history.toBuilder()
                .addDate(history.getAddDate().plusMillis(10))
                .build();
    }

    private static OrderHistory history(String user, OrderStatus status, String lineNumber) {
        return OrderHistory.builder()
                .orderKey(ORDERKEY)
                .orderLineNumber(lineNumber)
                .status(status)
                .comments(PackingSourceType.NEW_PACKING.getCode())
                .addWho(user)
                .addDate(Instant.parse("2020-04-01T12:34:56.789Z"))
                .build();
    }

    private static OrderHistory setupHistory(OrderStatus status) {
        return OrderHistory.builder()
                .orderKey(ORDERKEY)
                .status(status)
                .comments("...")
                .addWho("SETUP")
                .addDate(Instant.parse("2020-01-01T00:00:00Z"))
                .build();
    }

}
