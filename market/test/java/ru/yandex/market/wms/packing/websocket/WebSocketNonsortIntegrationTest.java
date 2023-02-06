package ru.yandex.market.wms.packing.websocket;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseTearDown;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.wms.common.model.enums.OrderStatus;
import ru.yandex.market.wms.common.model.enums.OrderType;
import ru.yandex.market.wms.common.spring.config.BaseTestConfig;
import ru.yandex.market.wms.common.spring.config.IntegrationTestConfig;
import ru.yandex.market.wms.common.spring.dao.entity.Order;
import ru.yandex.market.wms.common.spring.pojo.Carton;
import ru.yandex.market.wms.packing.LocationsRov;
import ru.yandex.market.wms.packing.dto.CheckParcelResponse;
import ru.yandex.market.wms.packing.dto.CloseParcelRequest;
import ru.yandex.market.wms.packing.dto.CloseParcelResponse;
import ru.yandex.market.wms.packing.dto.CloseParcelResponse.CreateSorterOrderState;
import ru.yandex.market.wms.packing.dto.HotContainersResponse;
import ru.yandex.market.wms.packing.dto.PackingHintsDTO;
import ru.yandex.market.wms.packing.enums.ItemStatus;
import ru.yandex.market.wms.packing.enums.TicketType;
import ru.yandex.market.wms.packing.integration.PackingIntegrationTest;
import ru.yandex.market.wms.packing.pojo.HotContainer;
import ru.yandex.market.wms.packing.pojo.IdleTablesDto;
import ru.yandex.market.wms.packing.pojo.OrderPackingTask;
import ru.yandex.market.wms.packing.pojo.PackingTask;
import ru.yandex.market.wms.packing.service.PackingTaskService;
import ru.yandex.market.wms.packing.utils.PackingAssertion;

import static com.github.springtestdbunit.annotation.DatabaseOperation.DELETE_ALL;
import static com.github.springtestdbunit.annotation.DatabaseOperation.INSERT;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static ru.yandex.market.wms.packing.BaseDbObjects.CARTON_YMA;
import static ru.yandex.market.wms.packing.BaseDbObjects.CARTON_YMB;
import static ru.yandex.market.wms.packing.utils.TestCollectionUtils.head;
import static ru.yandex.market.wms.packing.utils.TestCollectionUtils.tail;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {BaseTestConfig.class, IntegrationTestConfig.class},
        properties = {"check.authentication=mock", "warehouse-timezone = Europe/Samara"})
public class WebSocketNonsortIntegrationTest extends PackingIntegrationTest {

    private static final String USER = "TEST";

    @SpyBean
    @Autowired
    private PackingTaskService packingTaskService;
    @Autowired
    private PackingAssertion assertion;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DatabaseSetup(value = "/db/locations_setup_rov.xml", type = INSERT)
    @DatabaseSetup(value = "/db/base_setup.xml", type = INSERT)
    @DatabaseSetup(value = "/db/integration/websocket/nonsort/normal/setup.xml", type = INSERT)
    @ExpectedDatabase(value = "/db/integration/websocket/nonsort/normal/expected.xml",
            assertionMode = NON_STRICT_UNORDERED)
    @DatabaseTearDown(value = "/db/tear-down.xml", type = DELETE_ALL)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void pickAndPackFlow() throws Exception {
        String containerId = "CART101";
        String orderKey1 = "ORD0777";
        String orderKey2 = "ORD0778";
        Map<String, Set<String>> expectedUitsByOrderKey = Map.of(
                orderKey1, new TreeSet<>(Set.of("UID0001", "UID0002", "UID0003", "UID0004", "UID0005")),
                orderKey2, new TreeSet<>(Set.of("UID0007"))
        );

        var socket = createSocket();
        socket.connect(USER, LocationsRov.NONSORT_TABLE_2);
        assertion.assertUserIsIdle(USER, LocationsRov.NONSORT_TABLE_2);

        socket.hotContainers(new HotContainersResponse(List.of(
                HotContainer.builder()
                        .containerId("CART103")
                        .loc("NSCONS2-01")
                        .shippingDeadline(LocalDateTime.parse("2020-04-01T20:00:00"))
                        .build()
        )));

        Mockito.clearInvocations(packingTaskService);
        PackingTask task = socket.getTask(containerId);
        assertThat(task.getTicket().getTicketKey().getId()).isEqualTo(containerId);
        assertion.assertTaskHasUits(task, expectedUitsByOrderKey);
        assertion.assertUserHasNonsortTask(USER, "NSCONS2-01", containerId, TicketType.OVERSIZE);
        assertion.assertTaskContainerMayBeUsedAsParcelState(task, false);
        Mockito.verify(packingTaskService, never()).findUnassignedTicketForContainer(anyString());
        long ticketId = task.getTicket().getTicketId();

        // ORDER 1
        Collection<String> uits = expectedUitsByOrderKey.get(orderKey1);
        socket.scanFirstItemIntoParcel(
                ticketId,
                head(uits),
                CARTON_YMA.getType(),
                "P000000501",
                List.of(PackingHintsDTO.of("OVERSIZE_VOLUME_HINT_2"), PackingHintsDTO.of("OVERSIZE_WEIGHT_HINT_3")),
                false
        );
        socket.scanWrongItem(ticketId, "721234567800");
        socket.scanItemsIntoOpenParcel(ticketId, tail(uits));

        CloseParcelRequest parcelRequest = CloseParcelRequest.builder()
                .ticketId(ticketId)
                .orderKey(orderKey1)
                .parcelId("P000000501")
                .recommendedCartonId(Carton.NONPACK_TYPE)
                .selectedCartonId(CARTON_YMB.getType())
                .printer("P01")
                .uids(uits)
                .build();
        socket.checkParcel(parcelRequest, new CheckParcelResponse(0));
        socket.closeParcel(parcelRequest, new CloseParcelResponse(CreateSorterOrderState.PACK));
        Order order1 = Order.builder()
                .orderKey(orderKey1)
                .externalOrderKey("EXT0777")
                .type(OrderType.STANDARD.getCode())
                .build();
        assertion.assertParcelLabel(order1, "P000000501", 1, false);

        // ORDER 2
        uits = expectedUitsByOrderKey.get(orderKey2);
        socket.scanFirstItemIntoParcel(
                ticketId,
                head(uits),
                CARTON_YMA.getType(),
                "P000000502",
                List.of(PackingHintsDTO.of("OVERSIZE_WEIGHT_HINT_1"), PackingHintsDTO.of("OVERSIZE_VOLUME_HINT_1")),
                false
        );

        parcelRequest = CloseParcelRequest.builder()
                .ticketId(ticketId)
                .orderKey(orderKey2)
                .parcelId("P000000502")
                .recommendedCartonId(Carton.NONPACK_TYPE)
                .selectedCartonId(Carton.NONPACK_TYPE)
                .printer("P01")
                .uids(uits)
                .build();
        socket.checkParcel(parcelRequest, new CheckParcelResponse(0));
        socket.closeParcel(parcelRequest, new CloseParcelResponse(CreateSorterOrderState.PACK));
        Order order2 = Order.builder()
                .orderKey(orderKey2)
                .externalOrderKey("EXT0778")
                .type(OrderType.STANDARD.getCode())
                .build();
        assertion.assertParcelLabel(order2, "P000000502", 1, true);
        assertion.assertUserIsIdle(USER, LocationsRov.NONSORT_TABLE_2);

        // TRY TO GET THE SAME TASK AGAIN
        socket.getTaskThatNotExists(containerId);
        Mockito.verify(packingTaskService).findUnassignedTicketForContainer(containerId);

        // GET NEXT TASK
        containerId = "CART103";
        Mockito.clearInvocations(packingTaskService);
        task = socket.getTask(containerId);
        assertThat(task.getTicket().getTicketKey().getId()).isEqualTo(containerId);
        assertion.assertTaskContainerMayBeUsedAsParcelState(task, false);
        Mockito.verify(packingTaskService, never()).findUnassignedTicketForContainer(anyString());

        socket.disconnect();
    }

    /**
     * Включены флаги: PKG_OVERSIZE_ALWAYS_NONPACK, PKG_NONSORT_1_ITEM_IN_PARCEL
     */
    @Test
    @DatabaseSetup(value = "/db/locations_setup_rov.xml", type = INSERT)
    @DatabaseSetup(value = "/db/base_setup.xml", type = INSERT)
    @DatabaseSetup(value = "/db/integration/websocket/nonsort/all-nonpack/setup.xml", type = INSERT)
    @ExpectedDatabase(value = "/db/integration/websocket/nonsort/all-nonpack/expected.xml",
            assertionMode = NON_STRICT_UNORDERED)
    @DatabaseTearDown(value = "/db/tear-down.xml", type = DELETE_ALL)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void pickAndPackFlowWithNonpackFlags() throws Exception {
        String containerId = "CART101";
        String orderKey = "ORD0777";
        Map<String, String> uitToParcel = new TreeMap<>(Map.of(
                "UID0001", "P000000501",
                "UID0002", "P000000502",
                "UID0003", "P000000503"
        ));

        var socket = createSocket();
        socket.connect(USER, LocationsRov.NONSORT_TABLE_2);
        assertion.assertUserIsIdle(USER, LocationsRov.NONSORT_TABLE_2);

        socket.hotContainers(new HotContainersResponse(List.of()));

        Mockito.clearInvocations(packingTaskService);
        PackingTask task = socket.getTask(containerId);
        assertThat(task.getTicket().getTicketKey().getId()).isEqualTo(containerId);
        assertion.assertTaskHasUits(task, Map.of(orderKey, uitToParcel.keySet()));
        assertion.assertUserHasNonsortTask(USER, "NSCONS2-01", containerId, TicketType.OVERSIZE);
        assertion.assertTaskContainerMayBeUsedAsParcelState(task, false);
        Mockito.verify(packingTaskService, never()).findUnassignedTicketForContainer(anyString());
        long ticketId = task.getTicket().getTicketId();
        int parcelNumber = 1;

        for (Map.Entry<String, String> entry : uitToParcel.entrySet()) {
            String uit = entry.getKey();
            String parcelId = entry.getValue();
            socket.scanFirstItemIntoParcel(
                    ticketId,
                    uit,
                    Carton.NONPACK_TYPE,
                    parcelId,
                    List.of(PackingHintsDTO.of("OVERSIZE_WEIGHT_HINT_1"), PackingHintsDTO.of("OVERSIZE_VOLUME_HINT_2")),
                    true
            );

            CloseParcelRequest parcelRequest = CloseParcelRequest.builder()
                    .ticketId(ticketId)
                    .orderKey(orderKey)
                    .parcelId(parcelId)
                    .recommendedCartonId(Carton.NONPACK_TYPE)
                    .selectedCartonId(Carton.NONPACK_TYPE)
                    .printer("P01")
                    .uids(List.of(uit))
                    .build();
            socket.checkParcel(parcelRequest, new CheckParcelResponse(0));
            socket.closeParcel(parcelRequest, new CloseParcelResponse(CreateSorterOrderState.PACK));
            Order order1 = Order.builder()
                    .orderKey(orderKey)
                    .externalOrderKey("EXT0777")
                    .type(OrderType.STANDARD.getCode())
                    .build();
            assertion.assertParcelLabel(order1, parcelId, parcelNumber, parcelNumber == uitToParcel.size());
            parcelNumber++;
        }
    }

    @Test
    @DatabaseSetup(value = "/db/locations_setup_rov.xml", type = INSERT)
    @DatabaseSetup(value = "/db/base_setup.xml", type = INSERT)
    @DatabaseSetup(value = "/db/integration/websocket/nonsort/normal/setup.xml", type = INSERT)
    @DatabaseTearDown(value = "/db/tear-down.xml", type = DELETE_ALL)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void pickAndPackFlowWhenNoTasks() throws Exception {
        var socket = createSocket();
        socket.connect(USER, LocationsRov.NONSORT_TABLE_1);
        socket.hotContainersWhenNoTasks(IdleTablesDto.builder()
                .tables(List.of(LocationsRov.NONSORT_TABLE_2.getLoc()))
                .build());
        socket.disconnect();
    }

    @Test
    @DatabaseSetup(value = "/db/locations_setup_rov.xml", type = INSERT)
    @DatabaseSetup(value = "/db/base_setup.xml", type = INSERT)
    @DatabaseSetup(value = "/db/integration/websocket/nonsort/cancelled/setup.xml", type = INSERT)
    @ExpectedDatabase(value = "/db/integration/websocket/nonsort/cancelled/expected.xml",
            assertionMode = NON_STRICT_UNORDERED)
    @DatabaseTearDown(value = "/db/tear-down.xml", type = DELETE_ALL)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void pickAndPackFlowWithCancelledOrder() throws Exception {
        String containerId = "CART101";
        String containerForCancelledItems = "CART999";
        String cancelledOrderKey = "ORD0777";
        String normalOrderKey = "ORD0778";
        Set<String> expectedOrderlessUids = Set.of("UID0007");
        Map<String, Set<String>> expectedUitsByOrderKey = Map.of(
                cancelledOrderKey, Set.of("UID0001", "UID0002", "UID0003"),
                normalOrderKey, Set.of("UID0004", "UID0005")
        );

        var socket = createSocket();
        socket.connect(USER, LocationsRov.NONSORT_TABLE_2);
        assertion.assertUserIsIdle(USER, LocationsRov.NONSORT_TABLE_2);

        socket.hotContainers(new HotContainersResponse(List.of()));

        PackingTask task = socket.getTask(containerId);
        assertThat(task.getTicket().getTicketKey().getId()).isEqualTo(containerId);
        assertion.assertTaskHasUits(task, expectedUitsByOrderKey, expectedOrderlessUids);
        assertion.assertUserHasNonsortTask(USER, "NSCONS2-01", containerId, TicketType.OVERSIZE);
        assertion.assertTaskContainerMayBeUsedAsParcelState(task, false);
        OrderPackingTask cancelledOrderTask = task.getOrderTasks().stream()
                .filter(t -> t.getOrderKey().equals(cancelledOrderKey))
                .findFirst().get();
        assertThat(cancelledOrderTask.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED_INTERNALLY);
        assertThat(cancelledOrderTask.getItems()).allMatch(i -> i.getItemStatus() == ItemStatus.CANCELLED);
        long ticketId = task.getTicket().getTicketId();

        // сканируем поочередно товары из отмененного и нормального заказа
        // товары из отмененного кладем на тару, из нормального - сразу закрываем коробку
        socket.scanCancelledItem(ticketId, "UID0001", containerForCancelledItems);

        socket.scanFirstItemIntoParcel(ticketId, "UID0004", "YMA", "P000000501", true);
        CloseParcelRequest parcelRequest = CloseParcelRequest.builder()
                .ticketId(ticketId)
                .orderKey(normalOrderKey)
                .parcelId("P000000501")
                .recommendedCartonId("YMA")
                .selectedCartonId("YMA")
                .printer("P01")
                .uids(Set.of("UID0004"))
                .build();
        socket.checkParcel(parcelRequest, new CheckParcelResponse(0));
        socket.closeParcel(parcelRequest, new CloseParcelResponse(CreateSorterOrderState.PACK));
        Order normalOrder = Order.builder()
                .orderKey(normalOrderKey)
                .externalOrderKey("EXT0778")
                .type(OrderType.STANDARD.getCode())
                .build();
        assertion.assertParcelLabel(normalOrder, "P000000501", 1, false);

        socket.scanCancelledItem(ticketId, "UID0002", containerForCancelledItems);

        socket.scanFirstItemIntoParcel(ticketId, "UID0005", "YMA", "P000000502", true);
        parcelRequest = CloseParcelRequest.builder()
                .ticketId(ticketId)
                .orderKey(normalOrderKey)
                .parcelId("P000000502")
                .recommendedCartonId("YMA")
                .selectedCartonId("YMA")
                .printer("P01")
                .uids(Set.of("UID0005"))
                .build();
        socket.checkParcel(parcelRequest, new CheckParcelResponse(0));
        socket.closeParcel(parcelRequest, new CloseParcelResponse(CreateSorterOrderState.PACK));
        assertion.assertParcelLabel(normalOrder, "P000000502", 2, true);

        socket.scanCancelledItem(ticketId, "UID0003", containerForCancelledItems);

        socket.scanCancelledItem(ticketId, "UID0007", containerForCancelledItems);

        assertion.assertUserIsIdle(USER, LocationsRov.NONSORT_TABLE_2);
        socket.disconnect();
    }

    /**
     * Задание только что стало актуальным в базе и не успело прорасти в  кеш.
     * Для этого нужно чтобы кеш был не пустым, иначе при запросе задания кеш будет обновляться
     */
    @Test
    @DatabaseSetup(value = "/db/locations_setup_rov.xml", type = INSERT)
    @DatabaseSetup(value = "/db/base_setup.xml", type = INSERT)
    @DatabaseSetup(value = "/db/integration/websocket/nonsort/cache-miss/setup.xml", type = INSERT)
    @DatabaseTearDown(value = "/db/tear-down.xml", type = DELETE_ALL)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void getTaskByContainerIdWhenIsNotCached() throws Exception {
        String containerId = "CART102";
        var socket = createSocket();
        socket.connect(USER, LocationsRov.NONSORT_TABLE_2);
        assertion.assertUserIsIdle(USER, LocationsRov.NONSORT_TABLE_2);

        // изначально этого задания в базе нет (в пикдеталях стоит статус 0)
        Mockito.clearInvocations(packingTaskService);
        socket.getTaskThatNotExists(containerId);
        Mockito.verify(packingTaskService).findUnassignedTicketForContainer(containerId);

        // имитация появления задания в базе
        jdbc.update("update wmwhse1.pickdetail set status = '5' where id = ?", containerId);

        Mockito.clearInvocations(packingTaskService);
        PackingTask task = socket.getTask(containerId);
        assertThat(task.getTicket().getTicketKey().getId()).isEqualTo(containerId);
        assertion.assertTaskHasUits(task, Map.of("ORD0778", Set.of("UID0002")));
        assertion.assertUserHasNonsortTask(USER, "NSCONS2-01", containerId, TicketType.OVERSIZE);
        assertion.assertTaskContainerMayBeUsedAsParcelState(task, false);
        Mockito.verify(packingTaskService).findUnassignedTicketForContainer(containerId);
    }

    @Test
    @DatabaseSetup(value = "/db/locations_setup_rov.xml", type = INSERT)
    @DatabaseSetup(value = "/db/base_setup.xml", type = INSERT)
    @DatabaseSetup(value = "/db/integration/websocket/nonsort/no-task/setup.xml", type = INSERT)
    @ExpectedDatabase(value = "/db/integration/websocket/nonsort/no-task/expected.xml",
            assertionMode = NON_STRICT_UNORDERED)
    @DatabaseTearDown(value = "/db/tear-down.xml", type = DELETE_ALL)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void pickAndPackFlowCartWithNoTask() throws Exception {
        String containerId = "CART101";
        String containerIdEmpty = "CART105";

        var socket = createSocket();
        socket.connect(USER, LocationsRov.NONSORT_TABLE_2);
        assertion.assertUserIsIdle(USER, LocationsRov.NONSORT_TABLE_2);
        socket.hotContainers(new HotContainersResponse(List.of()));
        socket.getTaskAndExpectError(containerId,
                "Весь контейнер " + containerId + " можно переставить в отмену");
        assertion.assertUserIsIdle(USER, LocationsRov.NONSORT_TABLE_2);
        socket.getTaskAndExpectError(containerIdEmpty,
                "Контейнер " + containerIdEmpty + " пустой");
        assertion.assertUserIsIdle(USER, LocationsRov.NONSORT_TABLE_2);
        socket.disconnect();
    }
}
