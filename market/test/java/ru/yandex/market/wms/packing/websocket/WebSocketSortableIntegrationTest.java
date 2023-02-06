package ru.yandex.market.wms.packing.websocket;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseTearDown;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.wms.common.model.enums.OrderType;
import ru.yandex.market.wms.common.spring.config.BaseTestConfig;
import ru.yandex.market.wms.common.spring.config.IntegrationTestConfig;
import ru.yandex.market.wms.common.spring.dao.entity.Order;
import ru.yandex.market.wms.common.spring.dao.entity.SkuId;
import ru.yandex.market.wms.common.spring.pojo.BoxImpl;
import ru.yandex.market.wms.common.spring.pojo.Carton;
import ru.yandex.market.wms.common.spring.servicebus.model.dto.ItemDto;
import ru.yandex.market.wms.common.spring.servicebus.model.dto.RecommendedBoxDto;
import ru.yandex.market.wms.common.spring.servicebus.model.response.RecommendCartonResponse;
import ru.yandex.market.wms.packing.LocationsRov;
import ru.yandex.market.wms.packing.dto.CheckParcelResponse;
import ru.yandex.market.wms.packing.dto.CloseParcelRequest;
import ru.yandex.market.wms.packing.dto.CloseParcelResponse;
import ru.yandex.market.wms.packing.dto.CloseParcelResponse.CreateSorterOrderState;
import ru.yandex.market.wms.packing.dto.PackingHintsDTO;
import ru.yandex.market.wms.packing.integration.PackingIntegrationTest;
import ru.yandex.market.wms.packing.pojo.IdleTablesDto;
import ru.yandex.market.wms.packing.pojo.PackingTask;
import ru.yandex.market.wms.packing.pojo.TaskItemDimensions;
import ru.yandex.market.wms.packing.utils.PackingAssertion;
import ru.yandex.market.wms.pickbylight.client.mock.PickByLightMockClient;
import ru.yandex.market.wms.pickbylight.model.StationOperation;
import ru.yandex.market.wms.pickbylight.model.StationSide;

import static com.github.springtestdbunit.annotation.DatabaseOperation.DELETE_ALL;
import static com.github.springtestdbunit.annotation.DatabaseOperation.INSERT;
import static com.github.springtestdbunit.annotation.DatabaseOperation.REFRESH;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.wms.packing.BaseDbObjects.CARTON_YMB;
import static ru.yandex.market.wms.packing.service.PickByLightService.COLORS;
import static ru.yandex.market.wms.packing.utils.TestCollectionUtils.head;
import static ru.yandex.market.wms.packing.utils.TestCollectionUtils.tail;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {BaseTestConfig.class, IntegrationTestConfig.class},
        properties = {"check.authentication=mock"})
public class WebSocketSortableIntegrationTest extends PackingIntegrationTest {

    private static final String USER = "TEST";

    @Autowired
    private PackingAssertion assertion;

    @SpyBean
    @Autowired
    private PickByLightMockClient pickByLightClient;

    @Test
    @DatabaseSetup(value = "/db/locations_setup_rov.xml", type = INSERT)
    @DatabaseSetup(value = "/db/base_setup.xml", type = INSERT)
    @DatabaseSetup(value = "/db/integration/websocket/sortable/normal/setup.xml", type = INSERT)
    @ExpectedDatabase(value = "/db/integration/websocket/sortable/normal/expected.xml",
            assertionMode = NON_STRICT_UNORDERED)
    @DatabaseTearDown(value = "/db/tear-down.xml", type = DELETE_ALL)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void sortableFlow() throws Exception {
        String orderKey = "ORD0777";
        List<String> uits = Stream.iterate(1, i -> i + 1)
                .limit(20)
                .map(i -> String.format("UID%04d", i))
                .collect(Collectors.toList());

        var socket = createSocket();
        socket.connect(USER, LocationsRov.TABLE_1);
        PackingTask task = socket.getTask();
        assertThat(task.getHints()).containsExactlyInAnyOrder(
                PackingHintsDTO.builder().code("CARRIER_HINT_1").data(Collections.emptyMap()).build(),
                PackingHintsDTO.builder().code("CARRIER_HINT_3").data(Collections.emptyMap()).build());
        assertion.assertTaskHasUits(task, Map.of(orderKey, new HashSet<>(uits)));
        assertion.assertUserHasSortableTask(USER, "SS1", orderKey);
        long ticketId = task.getTicket().getTicketId();

        socket.scanFirstItemIntoParcel(
                ticketId,
                head(uits),
                "YMA",
                "P000000501",
                List.of(PackingHintsDTO.builder().code("CARGOTYPE_HINT_1").data(Collections.emptyMap()).build(),
                        PackingHintsDTO.builder().code("CARGOTYPE_HINT_3").data(Collections.emptyMap()).build()
                ),
                false
        );
        socket.scanWrongItem(ticketId, "721234567800");
        socket.scanItemsIntoOpenParcel(ticketId, tail(uits));

        CloseParcelRequest parcelRequest = CloseParcelRequest.builder()
                .ticketId(ticketId)
                .orderKey(orderKey)
                .parcelId("P000000501")
                .recommendedCartonId("YMA")
                .selectedCartonId(CARTON_YMB.getType())
                .printer("P01")
                .uids(uits)
                .build();

        socket.checkParcel(parcelRequest, new CheckParcelResponse(0));
        socket.closeParcel(parcelRequest, new CloseParcelResponse(CreateSorterOrderState.PACK));
        Order order = Order.builder()
                .orderKey(orderKey)
                .externalOrderKey("EXT0777")
                .type(OrderType.STANDARD.getCode())
                .build();
        assertion.assertParcelLabel(order, "P000000501", 1, true);
        assertion.assertUserIsIdle(USER, LocationsRov.TABLE_1);

        socket.getTaskWhenNoTasks(IdleTablesDto.builder().tables(List.of("PACKTBL3")).build());
        socket.disconnect();
    }

    @Test
    @DatabaseSetup(value = "/db/locations_setup_rov.xml", type = INSERT)
    @DatabaseSetup(value = "/db/base_setup.xml", type = INSERT)
    @DatabaseSetup(value = "/db/integration/websocket/sortable/normal/setup.xml", type = INSERT)
    @DatabaseSetup(value = "/db/integration/websocket/sortable/normal/without_scan_settings.xml", type = REFRESH)
    @ExpectedDatabase(value = "/db/integration/websocket/sortable/normal//without_scan_expected.xml",
            assertionMode = NON_STRICT_UNORDERED)
    @DatabaseTearDown(value = "/db/tear-down.xml", type = DELETE_ALL)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void sortableFlowWithoutItemsScan() throws Exception {
        String orderKey = "ORD0777";
        List<String> uits = Stream.iterate(1, i -> i + 1)
                .limit(20)
                .map(i -> String.format("UID%04d", i))
                .collect(Collectors.toList());

        TaskItemDimensions item1 = newItem(10, 11, 12, 2, 1);
        TaskItemDimensions item2 = newItem(13, 14, 15, 3, 2);

        when(servicebusClient.recommendCarton(any())).thenReturn(RecommendCartonResponse.builder()
                .recommendations(List.of(getRecommendedBoxDto(CARTON_YMB, item1, item2)))
                .build());

        var socket = createSocket();
        socket.connect(USER, LocationsRov.TABLE_1);
        PackingTask task = socket.getTask();

        assertion.assertTaskHasUits(task, Map.of(orderKey, new HashSet<>(uits)));
        assertion.assertUserHasSortableTask(USER, "SS1", orderKey);
        assertThat(task.getParcels().size()).isEqualTo(1);
        assertThat(task.getParcels().get(0).getRecommendedCartonId()).isEqualTo("YMB");
        assertThat(task.getParcels().get(0).getParcelId()).isEqualTo("P000000501");
        long ticketId = task.getTicket().getTicketId();

        CloseParcelRequest parcelRequest = CloseParcelRequest.builder()
                .ticketId(ticketId)
                .orderKey(orderKey)
                .parcelId("P000000501")
                .recommendedCartonId("YMA")
                .selectedCartonId(CARTON_YMB.getType())
                .printer("P01")
                .uids(uits)
                .itemsNotScanned(true)
                .build();

        socket.checkParcel(parcelRequest, new CheckParcelResponse(0));
        socket.closeParcel(parcelRequest, new CloseParcelResponse(CreateSorterOrderState.PACK));
        Order order = Order.builder()
                .orderKey(orderKey)
                .externalOrderKey("EXT0777")
                .type(OrderType.STANDARD.getCode())
                .build();
        assertion.assertParcelLabel(order, "P000000501", 1, true);
        assertion.assertUserIsIdle(USER, LocationsRov.TABLE_1);

        socket.getTaskWhenNoTasks(IdleTablesDto.builder().tables(List.of("PACKTBL3")).build());
        socket.disconnect();
    }

    @Test
    @DatabaseSetup(value = "/db/locations_setup_rov.xml", type = INSERT)
    @DatabaseSetup(value = "/db/base_setup.xml", type = INSERT)
    @DatabaseSetup(value = "/db/integration/websocket/sortable/normal/setup.xml", type = INSERT)
    @DatabaseSetup(value = "/db/integration/websocket/sortable/normal/use_qr.xml", type = INSERT)
    @ExpectedDatabase(value = "/db/integration/websocket/sortable/normal/expected.xml",
            assertionMode = NON_STRICT_UNORDERED)
    @DatabaseTearDown(value = "/db/tear-down.xml", type = DELETE_ALL)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void sortableFlowQRLabel() throws Exception {
        String orderKey = "ORD0777";
        List<String> uits = Stream.iterate(1, i -> i + 1)
                .limit(20)
                .map(i -> String.format("UID%04d", i))
                .collect(Collectors.toList());

        var socket = createSocket();
        socket.connect(USER, LocationsRov.TABLE_1);
        PackingTask task = socket.getTask();
        assertThat(task.getHints()).containsExactlyInAnyOrder(
                PackingHintsDTO.builder().code("CARRIER_HINT_1").data(Collections.emptyMap()).build(),
                PackingHintsDTO.builder().code("CARRIER_HINT_3").data(Collections.emptyMap()).build());
        assertion.assertTaskHasUits(task, Map.of(orderKey, new HashSet<>(uits)));
        assertion.assertUserHasSortableTask(USER, "SS1", orderKey);
        long ticketId = task.getTicket().getTicketId();

        socket.scanFirstItemIntoParcel(
                ticketId,
                head(uits),
                "YMA",
                "P000000501",
                List.of(PackingHintsDTO.builder().code("CARGOTYPE_HINT_1").data(Collections.emptyMap()).build(),
                        PackingHintsDTO.builder().code("CARGOTYPE_HINT_3").data(Collections.emptyMap()).build()
                ),
                false
        );
        socket.scanWrongItem(ticketId, "721234567800");
        socket.scanItemsIntoOpenParcel(ticketId, tail(uits));

        CloseParcelRequest parcelRequest = CloseParcelRequest.builder()
                .ticketId(ticketId)
                .orderKey(orderKey)
                .parcelId("P000000501")
                .recommendedCartonId("YMA")
                .selectedCartonId(CARTON_YMB.getType())
                .printer("P01")
                .uids(uits)
                .build();

        socket.checkParcel(parcelRequest, new CheckParcelResponse(0));
        socket.closeParcel(parcelRequest, new CloseParcelResponse(CreateSorterOrderState.PACK));
        Order order = Order.builder()
                .orderKey(orderKey)
                .externalOrderKey("EXT0777")
                .type(OrderType.STANDARD.getCode())
                .build();
        assertion.assertParcelQRLabel(order, "P000000501", 1, true);
        assertion.assertUserIsIdle(USER, LocationsRov.TABLE_1);

        socket.getTaskWhenNoTasks(IdleTablesDto.builder().tables(List.of("PACKTBL3")).build());
        socket.disconnect();
    }

    @Test
    @DatabaseSetup(value = "/db/locations_setup_rov.xml", type = INSERT)
    @DatabaseSetup(value = "/db/base_setup.xml", type = INSERT)
    @DatabaseSetup(value = "/db/integration/websocket/sortable/pbl/setup.xml", type = INSERT)
    @ExpectedDatabase(value = "/db/integration/websocket/sortable/pbl/expected.xml",
            assertionMode = NON_STRICT_UNORDERED)
    @DatabaseTearDown(value = "/db/tear-down.xml", type = DELETE_ALL)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void sortableFlowPickByLight() throws Exception {
        String station = "SS1";
        String orderKey = "ORD0777";

        pickByLightClient.setStationOperations(List.of(new StationOperation(station)));
        var socket = createSocket();
        socket.connect(USER, LocationsRov.TABLE_1);
        PackingTask task = socket.getTask();
        assertion.assertTaskHasUits(task, Map.of(orderKey, Set.of("UID0001", "UID0002")));
        assertion.assertUserHasSortableTask(USER, station, orderKey);
        verify(pickByLightClient).switchOn(station, StationSide.OUT, COLORS.get(0), List.of("CELL1", "CELL2"));
        reset(pickByLightClient);
        long ticketId = task.getTicket().getTicketId();

        socket.scanFirstItemIntoParcel(ticketId, "UID0001", "YMA", "P000000501",
                List.of(PackingHintsDTO.builder().code("CARGOTYPE_HINT_1").data(Collections.emptyMap()).build(),
                        PackingHintsDTO.builder().code("CARGOTYPE_HINT_3").data(Collections.emptyMap()).build()
                ),
                false
        );

        CloseParcelRequest parcelRequest = CloseParcelRequest.builder()
                .ticketId(ticketId)
                .orderKey(orderKey)
                .parcelId("P000000501")
                .recommendedCartonId("YMA")
                .selectedCartonId(CARTON_YMB.getType())
                .printer("P01")
                .uids(List.of("UID0001"))
                .build();

        socket.checkParcel(parcelRequest, new CheckParcelResponse(0));
        socket.closeParcel(parcelRequest, new CloseParcelResponse(CreateSorterOrderState.PACK));
        Order order = Order.builder()
                .orderKey(orderKey)
                .externalOrderKey("EXT0777")
                .type(OrderType.STANDARD.getCode())
                .build();
        assertion.assertParcelLabel(order, "P000000501", 1, false);

        socket.scanFirstItemIntoParcel(ticketId, "UID0002", "YMA", "P000000502",
                List.of(PackingHintsDTO.builder().code("CARGOTYPE_HINT_1").data(Collections.emptyMap()).build(),
                        PackingHintsDTO.builder().code("CARGOTYPE_HINT_3").data(Collections.emptyMap()).build()
                ),
                false
        );

        parcelRequest = CloseParcelRequest.builder()
                .ticketId(ticketId)
                .orderKey(orderKey)
                .parcelId("P000000502")
                .recommendedCartonId("YMA")
                .selectedCartonId("YMA")
                .printer("P01")
                .uids(List.of("UID0002"))
                .build();

        socket.checkParcel(parcelRequest, new CheckParcelResponse(0));
        verifyNoInteractions(pickByLightClient);
        socket.closeParcel(parcelRequest, new CloseParcelResponse(CreateSorterOrderState.PACK));
        assertion.assertParcelLabel(order, "P000000502", 2, true);
        verify(pickByLightClient).switchOff(station, StationSide.OUT, List.of("CELL1", "CELL2"));

        assertion.assertUserIsIdle(USER, LocationsRov.TABLE_1);
        socket.disconnect();
    }


    /**
     * Есть два одинаковых заказа. Единственное, что их отличает - это время отгрузки (SHIPMENTDATETIME)
     * Один заказ отгружается 2021-09-04 13:00:00.000, другой заказ отгружается на день позже - 2021-09-05 13:00:00.000
     * Заказы для packing'а должны приоритизироваться по времени отгрузки, поэтому для упаковки должен
     * быть выбран заказ с более ранним временем отгрузки
     */
    @Test
    @DatabaseSetup(value = "/db/locations_setup_rov.xml", type = INSERT)
    @DatabaseSetup(value = "/db/base_setup.xml", type = INSERT)
    @DatabaseSetup(value = "/db/integration/websocket/sortable/shipment_date_time/setup.xml", type = INSERT)
    @DatabaseTearDown(value = "/db/tear-down.xml", type = DELETE_ALL)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void getTasksSortedByShipmentDateTime() throws Exception {
        var socket = createSocket();
        socket.connect(USER, LocationsRov.TABLE_1);

        PackingTask task = socket.getTask();

        String orderWithEarliestShipmentDateTime = "ORD0777";
        assertThat(task.getTicket().getOrderKey()).isEqualTo(orderWithEarliestShipmentDateTime);

        socket.disconnect();
    }

    @Test
    @DatabaseSetup(value = "/db/locations_setup_rov.xml", type = INSERT)
    @DatabaseSetup(value = "/db/base_setup.xml", type = INSERT)
    @DatabaseSetup(value = "/db/integration/websocket/sortable/normal/setup.xml", type = INSERT)
    @DatabaseSetup(value = "/db/integration/websocket/sortable/normal/max_parcel_dimensions_biggest.xml",
            type = INSERT)
    @ExpectedDatabase(value = "/db/integration/websocket/sortable/normal/expected.xml",
            assertionMode = NON_STRICT_UNORDERED)
    @DatabaseTearDown(value = "/db/tear-down.xml", type = DELETE_ALL)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void sortableFlowWithOrderParcelBiggestDimensions() throws Exception {
        String orderKey = "ORD0777";
        List<String> uits = Stream.iterate(1, i -> i + 1)
                .limit(20)
                .map(i -> String.format("UID%04d", i))
                .collect(Collectors.toList());

        var socket = createSocket();
        socket.connect(USER, LocationsRov.TABLE_1);
        PackingTask task = socket.getTask();
        assertThat(task.getHints()).containsExactlyInAnyOrder(
                PackingHintsDTO.builder().code("CARRIER_HINT_1").data(Collections.emptyMap()).build(),
                PackingHintsDTO.builder().code("CARRIER_HINT_3").data(Collections.emptyMap()).build());
        assertion.assertTaskHasUits(task, Map.of(orderKey, new HashSet<>(uits)));
        assertion.assertUserHasSortableTask(USER, "SS1", orderKey);
        long ticketId = task.getTicket().getTicketId();

        socket.scanFirstItemIntoParcel(
                ticketId,
                head(uits),
                "YMA",
                "P000000501",
                List.of(PackingHintsDTO.builder().code("CARGOTYPE_HINT_1").data(Collections.emptyMap()).build(),
                        PackingHintsDTO.builder().code("CARGOTYPE_HINT_3").data(Collections.emptyMap()).build()
                ),
                false
        );
        socket.scanWrongItem(ticketId, "721234567800");
        socket.scanItemsIntoOpenParcel(ticketId, tail(uits));

        CloseParcelRequest parcelRequest = CloseParcelRequest.builder()
                .ticketId(ticketId)
                .orderKey(orderKey)
                .parcelId("P000000501")
                .recommendedCartonId("YMA")
                .selectedCartonId(CARTON_YMB.getType())
                .printer("P01")
                .uids(uits)
                .build();

        socket.checkParcel(parcelRequest, new CheckParcelResponse(0));
        socket.closeParcel(parcelRequest, new CloseParcelResponse(CreateSorterOrderState.PACK));
        Order order = Order.builder()
                .orderKey(orderKey)
                .externalOrderKey("EXT0777")
                .type(OrderType.STANDARD.getCode())
                .build();
        assertion.assertParcelLabel(order, "P000000501", 1, true);
        assertion.assertUserIsIdle(USER, LocationsRov.TABLE_1);

        socket.getTaskWhenNoTasks(IdleTablesDto.builder().tables(List.of("PACKTBL3")).build());
        socket.disconnect();
    }

    @Test
    @DatabaseSetup(value = "/db/locations_setup_rov.xml", type = INSERT)
    @DatabaseSetup(value = "/db/base_setup.xml", type = INSERT)
    @DatabaseSetup(value = "/db/integration/websocket/sortable/normal/setup.xml", type = INSERT)
    @DatabaseSetup(value = "/db/integration/websocket/sortable/normal/max_parcel_middle_dimensions.xml",
            type = INSERT)
    @ExpectedDatabase(value = "/db/integration/websocket/sortable/normal/expected.xml",
            assertionMode = NON_STRICT_UNORDERED)
    @DatabaseTearDown(value = "/db/tear-down.xml", type = DELETE_ALL)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void sortableFlowWithOrderParcelMiddleDimensions() throws Exception {
        String orderKey = "ORD0777";
        List<String> uits = Stream.iterate(1, i -> i + 1)
                .limit(20)
                .map(i -> String.format("UID%04d", i))
                .collect(Collectors.toList());

        var socket = createSocket();
        socket.connect(USER, LocationsRov.TABLE_1);
        PackingTask task = socket.getTask();
        assertThat(task.getHints()).containsExactlyInAnyOrder(
                PackingHintsDTO.builder().code("CARRIER_HINT_1").data(Collections.emptyMap()).build(),
                PackingHintsDTO.builder().code("CARRIER_HINT_3").data(Collections.emptyMap()).build(),
                PackingHintsDTO.builder().code("ORDER_MAX_PARCEL_BOX").data(Map.of("box", "YMB")).build()
        );
        assertion.assertTaskHasUits(task, Map.of(orderKey, new HashSet<>(uits)));
        assertion.assertUserHasSortableTask(USER, "SS1", orderKey);
        long ticketId = task.getTicket().getTicketId();

        socket.scanFirstItemIntoParcel(
                ticketId,
                head(uits),
                "YMA",
                "P000000501",
                List.of(PackingHintsDTO.builder().code("CARGOTYPE_HINT_1").data(Collections.emptyMap()).build(),
                        PackingHintsDTO.builder().code("CARGOTYPE_HINT_3").data(Collections.emptyMap()).build()
                ),
                false
        );
        socket.scanWrongItem(ticketId, "721234567800");
        socket.scanItemsIntoOpenParcel(ticketId, tail(uits));

        CloseParcelRequest parcelRequest = CloseParcelRequest.builder()
                .ticketId(ticketId)
                .orderKey(orderKey)
                .parcelId("P000000501")
                .recommendedCartonId("YMA")
                .selectedCartonId(CARTON_YMB.getType())
                .printer("P01")
                .uids(uits)
                .build();

        socket.checkParcel(parcelRequest, new CheckParcelResponse(0));
        socket.closeParcel(parcelRequest, new CloseParcelResponse(CreateSorterOrderState.PACK));
        Order order = Order.builder()
                .orderKey(orderKey)
                .externalOrderKey("EXT0777")
                .type(OrderType.STANDARD.getCode())
                .build();
        assertion.assertParcelLabel(order, "P000000501", 1, true);
        assertion.assertUserIsIdle(USER, LocationsRov.TABLE_1);

        socket.getTaskWhenNoTasks(IdleTablesDto.builder().tables(List.of("PACKTBL3")).build());
        socket.disconnect();
    }

    @Test
    @DatabaseSetup(value = "/db/locations_setup_rov.xml", type = INSERT)
    @DatabaseSetup(value = "/db/base_setup.xml", type = INSERT)
    @DatabaseSetup(value = "/db/integration/websocket/sortable/withdrawal/setup.xml", type = INSERT)
    @ExpectedDatabase(value = "/db/integration/websocket/sortable/withdrawal/expected.xml",
            assertionMode = NON_STRICT_UNORDERED)
    @DatabaseTearDown(value = "/db/tear-down.xml", type = DELETE_ALL)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void sortableFlowWithdrawal() throws Exception {
        String orderKey = "ORD0777";
        List<String> uits = Stream.iterate(1, i -> i + 1)
                .limit(20)
                .map(i -> String.format("UID%04d", i))
                .collect(Collectors.toList());

        var socket = createSocket();
        socket.connect(USER, LocationsRov.TABLE_1);
        PackingTask task = socket.getTask();
        assertThat(task.getHints()).containsExactlyInAnyOrder(
                PackingHintsDTO.builder().code("CARRIER_HINT_1").data(Collections.emptyMap()).build(),
                PackingHintsDTO.builder().code("CARRIER_HINT_3").data(Collections.emptyMap()).build());
        assertion.assertTaskHasUits(task, Map.of(orderKey, new HashSet<>(uits)));
        assertion.assertUserHasSortableTask(USER, "SS1", orderKey);
        long ticketId = task.getTicket().getTicketId();

        socket.scanFirstItemIntoParcel(
                ticketId,
                head(uits),
                "YMA",
                "P000000501",
                List.of(PackingHintsDTO.builder().code("CARGOTYPE_HINT_1").data(Collections.emptyMap()).build(),
                        PackingHintsDTO.builder().code("CARGOTYPE_HINT_3").data(Collections.emptyMap()).build()
                ),
                false
        );
        socket.scanWrongItem(ticketId, "721234567800");
        socket.scanItemsIntoOpenParcel(ticketId, tail(uits));

        CloseParcelRequest parcelRequest = CloseParcelRequest.builder()
                .ticketId(ticketId)
                .orderKey(orderKey)
                .parcelId("P000000501")
                .recommendedCartonId("YMA")
                .selectedCartonId(CARTON_YMB.getType())
                .printer("P01")
                .uids(uits)
                .build();

        socket.checkParcel(parcelRequest, new CheckParcelResponse(0));
        socket.closeParcel(parcelRequest, new CloseParcelResponse(CreateSorterOrderState.CELL));
        Order order = Order.builder()
                .orderKey(orderKey)
                .externalOrderKey("EXT0777")
                .type(OrderType.OUTBOUND_FIT.getCode())
                .build();
        assertion.assertParcelLabel(order, "P000000501", 1, true);
        assertion.assertUserIsIdle(USER, LocationsRov.TABLE_1);

        socket.getTaskWhenNoTasks(IdleTablesDto.builder().tables(List.of("PACKTBL3")).build());
        socket.disconnect();
    }


    static TaskItemDimensions newItem(double x, double y, double z, int quantity, int num) {
        return new TaskItemDimensions(new SkuId("storer" + num, "sku" + num), BigDecimal.valueOf(quantity),
                new BoxImpl(x, y, z, x * y * z), 0, "manufacturerSku" + num);
    }

    private RecommendedBoxDto getRecommendedBoxDto(Carton cartonA, TaskItemDimensions... items) {
        return RecommendedBoxDto.builder()
                .boxId(cartonA.getType())
                .items(Arrays.stream(items).map(currentItem -> ItemDto.builder()
                                .storerKey(currentItem.getSkuId().getStorerKey())
                                .manufacturerSku(currentItem.getManufacturerSku())
                                .qty(currentItem.getQty().intValue())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

}
