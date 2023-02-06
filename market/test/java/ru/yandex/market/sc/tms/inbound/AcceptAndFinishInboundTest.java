package ru.yandex.market.sc.tms.inbound;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.sc.core.domain.inbound.FinishInboundJob;
import ru.yandex.market.sc.core.domain.inbound.model.InboundType;
import ru.yandex.market.sc.core.domain.inbound.repository.InboundRegistryOrderStatus;
import ru.yandex.market.sc.core.domain.inbound.repository.InboundStatus;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.tms.test.EmbeddedDbTmsTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.market.sc.core.configuration.ConfigurationProperties.SUPPLIER_DROPOFF_TRANSFER_ACT_ENABLED;
import static ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey.IS_DROPOFF;
import static ru.yandex.market.sc.core.test.TestFactory.order;

@EmbeddedDbTmsTest
public class AcceptAndFinishInboundTest {

    @Autowired
    TestFactory testFactory;

    @Autowired
    FinishInboundJob finishInboundJob;

    @MockBean
    Clock clock;

    @Autowired
    JdbcTemplate jdbcTemplate;

    SortingCenter sortingCenter;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        testFactory.setSortingCenterProperty(sortingCenter, IS_DROPOFF, true);
        doReturn(Instant.ofEpochMilli(0L)).when(clock).instant();
        doReturn(ZoneId.systemDefault()).when(clock).getZone();
    }

    /**
     * Принять поставку, в которой нет ни одного заказа который был бы принят на сц
     */
    @Test
    public void acceptInboundWithNonExistingOrdersTest() {
        var params = TestFactory.CreateInboundParams
                .builder()
                .fromDate(OffsetDateTime.now(clock))
                .toDate(OffsetDateTime.now(clock))
                .sortingCenter(sortingCenter)
                .build();
        var inbound = testFactory.createInbound(params);
        finishInboundJob.acceptInbounds();
        inbound = testFactory.getInbound(inbound.getExternalId());
        assertThat(inbound.getInboundStatus()).isEqualTo(InboundStatus.CREATED);
        var orders = testFactory.getAllOrdersByInboundId(inbound.getId());
        orders.forEach(order -> assertThat(order.getStatus()).isEqualTo(InboundRegistryOrderStatus.CREATED));
    }

    /**
     * Принять поставку, в которой есть заказы которые уже приняты на сц
     * заказы оставляем в статусе созданы; саму поставку переводим в статус принята
     */
    @Test
    public void acceptInboundWhenSomeOrdersAreAcceptedTest() {
        var order = testFactory.create(order(sortingCenter).build()).get();
        var params = TestFactory.CreateInboundParams
                .builder()
                .fromDate(OffsetDateTime.now(clock))
                .toDate(OffsetDateTime.now(clock))
                .warehouseFromExternalId(order.getWarehouseFrom().getYandexId())
                .sortingCenter(sortingCenter)
                .plainOrders(List.of(new Pair<>(order.getExternalId(), order.getExternalId())))
                .build();
        var inbound = testFactory.createInbound(params);
        finishInboundJob.acceptInbounds();
        var orders = testFactory.getAllOrdersByInboundId(inbound.getId());
        assertThat(testFactory.getInbound(inbound.getExternalId()).getInboundStatus()).isEqualTo(InboundStatus.CREATED);
        orders.forEach(o -> assertThat(o.getStatus()).isEqualTo(InboundRegistryOrderStatus.CREATED));
        testFactory.accept(order);
        finishInboundJob.acceptInbounds();
        orders = testFactory.getAllOrdersByInboundId(inbound.getId());
        assertThat(testFactory.getInbound(inbound.getExternalId()).getInboundStatus()).isEqualTo(InboundStatus.ARRIVED);
        orders.forEach(o -> assertThat(o.getStatus()).isEqualTo(InboundRegistryOrderStatus.CREATED));
    }

    /**
     * Принять поставку, когда на сц есть принятые заказы, но они с другого склада (т.е. не в этой поставке)
     */
    @Test
    public void acceptInboundWhenSomeOrdersAreAcceptedButItsFromAnotherWarehouseTest() {
        var order = testFactory.create(order(sortingCenter).build()).get();
        var anotherWarehouse = testFactory.storedWarehouse("another_wh_id");
        var params = TestFactory.CreateInboundParams
                .builder()
                .fromDate(OffsetDateTime.now(clock))
                .toDate(OffsetDateTime.now(clock))
                .warehouseFromExternalId(anotherWarehouse.getYandexId())
                .sortingCenter(sortingCenter)
                .plainOrders(List.of(new Pair<>(order.getExternalId(), order.getExternalId())))
                .build();
        testFactory.accept(order);
        var inbound = testFactory.createInbound(params);
        finishInboundJob.acceptInbounds();
        inbound = testFactory.getInbound(inbound.getExternalId());
        var orders = testFactory.getAllOrdersByInboundId(inbound.getId());
        assertThat(testFactory.getInbound(inbound.getExternalId()).getInboundStatus())
                .isEqualTo(InboundStatus.CREATED);
        orders.forEach(o -> assertThat(o.getStatus()).isEqualTo(InboundRegistryOrderStatus.CREATED));
    }

    /**
     * Завершить приемку, когда на сц нет принятых заказов из данной поставки
     */
    @Test
    public void finishInboundWhenThereNoOrdersTest() {
        var params = TestFactory.CreateInboundParams
                .builder()
                .fromDate(OffsetDateTime.now(clock))
                .toDate(OffsetDateTime.now(clock))
                .sortingCenter(sortingCenter)
                .build();
        var inbound = testFactory.createInbound(params);
        finishInboundJob.acceptInbounds();
        finishInboundJob.finishInboundsByTimeout();
        inbound = testFactory.getInbound(inbound.getExternalId());
        assertThat(inbound.getInboundStatus()).isEqualTo(InboundStatus.CREATED);
        var orders = testFactory.getAllOrdersByInboundId(inbound.getId());
        orders.forEach(order -> assertThat(order.getStatus()).isEqualTo(InboundRegistryOrderStatus.CREATED));
    }

    /**
     * Завершить приемку заказов, которые уже приняты на сц, но когда
     * количество времени с момента приемки поставки прошло еще не достаточно
     */
    @Test
    public void finishInboundWhenTimeHasNotExpiredTest() {
        var order = testFactory.create(order(sortingCenter).build()).get();
        var params = TestFactory.CreateInboundParams
                .builder()
                .fromDate(OffsetDateTime.now(clock))
                .toDate(OffsetDateTime.now(clock))
                .warehouseFromExternalId(order.getWarehouseFrom().getYandexId())
                .sortingCenter(sortingCenter)
                .plainOrders(List.of(new Pair<>(order.getExternalId(), order.getExternalId())))
                .build();
        var inbound = testFactory.createInbound(params);
        testFactory.accept(order);
        finishInboundJob.acceptInbounds();
        finishInboundJob.finishInboundsByTimeout();
        inbound = testFactory.getInbound(inbound.getExternalId());
        assertThat(inbound.getInboundStatus()).isEqualTo(InboundStatus.ARRIVED);
        var orders = testFactory.getAllOrdersByInboundId(inbound.getId());
        orders.forEach(o -> assertThat(o.getStatus()).isEqualTo(InboundRegistryOrderStatus.CREATED));
    }

    /**
     * Завершить приемку заказов, которые уже приняты на сц
     *
     */
    @Test
    public void finishInboundTest() {
        doReturn(Instant.now().minus(60 * 22 + 1, ChronoUnit.MINUTES)).when(clock).instant();
        TestFactory.setupMockClock(clock, Instant.now(clock));
        testFactory.setConfiguration(SUPPLIER_DROPOFF_TRANSFER_ACT_ENABLED, true);
        var order = testFactory.create(order(sortingCenter).build()).get();
        var params = TestFactory.CreateInboundParams
                .builder()
                .fromDate(OffsetDateTime.now(clock))
                .toDate(OffsetDateTime.now(clock))
                .inboundType(InboundType.DS_SC)
                .warehouseFromExternalId(order.getWarehouseFrom().getYandexId())
                .sortingCenter(sortingCenter)
                .plainOrders(List.of(new Pair<>(order.getExternalId(), order.getExternalId())))
                .build();
        var inbound = testFactory.createInbound(params);
        testFactory.accept(order);
        finishInboundJob.acceptInbounds();
        doReturn(Instant.now()).when(clock).instant();
        finishInboundJob.finishInboundsByTimeout();
        inbound = testFactory.getInbound(inbound.getExternalId());
        assertThat(inbound.getInboundStatus()).isEqualTo(InboundStatus.FIXED);
        var orders = testFactory.getAllOrdersByInboundId(inbound.getId());
        assertThat(orders).hasSize(3);
        assertThat(StreamEx.of(orders)
                .filter(o -> o.getExternalId().equals(order.getExternalId()))
                .filter(o -> o.getStatus() == InboundRegistryOrderStatus.CREATED)
                .count()).isEqualTo(1);
        assertThat(StreamEx.of(orders)
                .filter(o -> o.getExternalId().equals(order.getExternalId()))
                .filter(o -> o.getStatus() == InboundRegistryOrderStatus.FIXED)
                .count()).isEqualTo(1);
    }

    /**
     * Завершить приемку заказов, которые уже приняты на сц
     *
     */
    @Test
    public void doAcceptAndFixForNonDropoffInboundTest() {
        testFactory.setSortingCenterProperty(sortingCenter, IS_DROPOFF, false);
        var order = testFactory.create(order(sortingCenter).build()).get();
        var params = TestFactory.CreateInboundParams
                .builder()
                .fromDate(OffsetDateTime.now(clock))
                .toDate(OffsetDateTime.now(clock))
                .warehouseFromExternalId(order.getWarehouseFrom().getYandexId())
                .sortingCenter(sortingCenter)
                .plainOrders(List.of(new Pair<>(order.getExternalId(), order.getExternalId())))
                .build();
        var inbound = testFactory.createInbound(params);
        testFactory.accept(order);
        finishInboundJob.acceptInbounds();
        doReturn(clock.instant().plus(60 * 22 + 1, ChronoUnit.MINUTES)).when(clock).instant();
        finishInboundJob.finishInboundsByTimeout();
        inbound = testFactory.getInbound(inbound.getExternalId());
        assertThat(inbound.getInboundStatus()).isEqualTo(InboundStatus.ARRIVED);
        var orders = testFactory.getAllOrdersByInboundId(inbound.getId());
        assertThat(orders).hasSize(2);
        orders.forEach(o -> assertThat(o.getStatus()).isEqualTo(InboundRegistryOrderStatus.CREATED));
    }

    /**
     * Завершить приемку заказов, которые уже приняты на сц, но эти заказы из другого склада
     *
     */
    @Test
    public void finishInboundWhenOrdersFromAnotherWarehouseTest() {
        var order = testFactory.create(order(sortingCenter).build()).get();
        var anotherWarehouse = testFactory.storedWarehouse("another_wh_id");
        var params = TestFactory.CreateInboundParams
                .builder()
                .fromDate(OffsetDateTime.now(clock))
                .toDate(OffsetDateTime.now(clock))
                .warehouseFromExternalId(anotherWarehouse.getYandexId())
                .sortingCenter(sortingCenter)
                .plainOrders(List.of(new Pair<>(order.getExternalId(), order.getExternalId())))
                .build();
        var inbound = testFactory.createInbound(params);
        testFactory.accept(order);
        finishInboundJob.acceptInbounds();
        doReturn(clock.instant().plus(60 * 6 + 1, ChronoUnit.MINUTES)).when(clock).instant();
        finishInboundJob.finishInboundsByTimeout();
        inbound = testFactory.getInbound(inbound.getExternalId());
        assertThat(inbound.getInboundStatus()).isEqualTo(InboundStatus.CREATED);
        var orders = testFactory.getAllOrdersByInboundId(inbound.getId());
        orders.forEach(o -> assertThat(o.getStatus()).isEqualTo(InboundRegistryOrderStatus.CREATED));
    }

}
