package ru.yandex.market.sc.internal.controller.partner;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import one.util.streamex.StreamEx;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.sc.core.configuration.ConfigurationProperties;
import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.delivery_service.model.DeliveryServiceType;
import ru.yandex.market.sc.core.domain.delivery_service.repository.DeliveryServiceProperty;
import ru.yandex.market.sc.core.domain.inbound.InboundCommandService;
import ru.yandex.market.sc.core.domain.inbound.InboundFacade;
import ru.yandex.market.sc.core.domain.inbound.model.ComplexInboundAction;
import ru.yandex.market.sc.core.domain.inbound.model.DiscrepancyActStatus;
import ru.yandex.market.sc.core.domain.inbound.model.GroupPutCarInfoRequest;
import ru.yandex.market.sc.core.domain.inbound.model.InboundAvailableAction;
import ru.yandex.market.sc.core.domain.inbound.model.InboundDiscrepancyActDto;
import ru.yandex.market.sc.core.domain.inbound.model.InboundPageGroupAction;
import ru.yandex.market.sc.core.domain.inbound.model.InboundPageSimpleAction;
import ru.yandex.market.sc.core.domain.inbound.model.InboundPartnerDto;
import ru.yandex.market.sc.core.domain.inbound.model.InboundPartnerParamsDto;
import ru.yandex.market.sc.core.domain.inbound.model.InboundPartnerStatusDto;
import ru.yandex.market.sc.core.domain.inbound.model.InboundType;
import ru.yandex.market.sc.core.domain.inbound.model.PutCarInfoRequest;
import ru.yandex.market.sc.core.domain.inbound.repository.BoundRegistryRepository;
import ru.yandex.market.sc.core.domain.inbound.repository.CarInfo;
import ru.yandex.market.sc.core.domain.inbound.repository.Inbound;
import ru.yandex.market.sc.core.domain.inbound.repository.InboundInfo;
import ru.yandex.market.sc.core.domain.inbound.repository.InboundInfoRepository;
import ru.yandex.market.sc.core.domain.inbound.repository.InboundRegistryOrderStatus;
import ru.yandex.market.sc.core.domain.inbound.repository.InboundRepository;
import ru.yandex.market.sc.core.domain.inbound.repository.InboundStatus;
import ru.yandex.market.sc.core.domain.inbound.repository.InboundStatusHistory;
import ru.yandex.market.sc.core.domain.inbound.repository.InboundStatusHistoryRepository;
import ru.yandex.market.sc.core.domain.inbound.repository.Registry;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistryOrder;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistryRepository;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistryType;
import ru.yandex.market.sc.core.domain.lot.model.PartnerLotRequestDto;
import ru.yandex.market.sc.core.domain.measurements_so.model.UnitMeasurementsDto;
import ru.yandex.market.sc.core.domain.movement_courier.repository.MovementCourier;
import ru.yandex.market.sc.core.domain.outbound.repository.Outbound;
import ru.yandex.market.sc.core.domain.scan.model.SaveVGHRequestDto;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLot;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLotService;
import ru.yandex.market.sc.core.domain.sortable.model.enums.DirectFlowType;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sortable.repository.Sortable;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.domain.warehouse.model.WarehouseType;
import ru.yandex.market.sc.core.domain.warehouse.repository.Warehouse;
import ru.yandex.market.sc.core.test.SortableTestFactory;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.core.util.flow.xdoc.XDocFlow;
import ru.yandex.market.sc.internal.controller.dto.PartnerLotDtoWrapper;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;
import ru.yandex.market.sc.internal.test.ScTestUtils;
import ru.yandex.market.sc.internal.util.ScIntControllerCaller;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.sc.core.domain.inbound.repository.RegistryType.FACTUAL;
import static ru.yandex.market.sc.core.domain.inbound.repository.RegistryType.PLANNED;
import static ru.yandex.market.sc.core.exception.ScErrorCode.INBOUND_NOT_CONFIRMED;
import static ru.yandex.market.sc.core.test.TestFactory.order;
import static ru.yandex.market.sc.internal.util.ScIntControllerCaller.PUT_CAR_INFO_REQUEST;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ScIntControllerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@SuppressWarnings("checkstyle:ClassLength")
public class PartnerInboundControllerTest {

    private final JdbcTemplate jdbcTemplate;
    private final MockMvc mockMvc;
    private final TestFactory testFactory;
    private final SortableTestFactory sortableTestFactory;
    private final InboundRepository inboundRepository;
    private final InboundInfoRepository inboundInfoRepository;
    private final InboundStatusHistoryRepository inboundStatusHistoryRepository;
    private final SortableLotService sortableLotService;
    private final XDocFlow flow;
    private final ScIntControllerCaller caller;
    private final InboundFacade inboundFacade;
    private final RegistryRepository registryRepository;
    private final BoundRegistryRepository boundRegistryRepository;
    private final InboundCommandService inboundCommandService;

    @MockBean
    Clock clock;

    SortingCenter sortingCenter;
    User user;
    Warehouse warehouse;
    ScIntControllerCaller foreignCaller;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        user = testFactory.storedUser(sortingCenter, TestFactory.USER_UID_LONG);
        warehouse = testFactory.storedWarehouse();
        testFactory.setupMockClock(clock);

        var foreignSortingCenter = testFactory.storedSortingCenter2();
        foreignCaller = ScIntControllerCaller.createCaller(mockMvc, foreignSortingCenter.getPartnerId());

        testFactory.setSortingCenterProperty(sortingCenter.getId(), SortingCenterPropertiesKey.XDOC_ENABLED, false);
    }

    @DisplayName("Передаем id перемещения в поставке")
    @Test
    @SneakyThrows
    void inboundDtoHasTransportationId() {
        var inbound = createdInbound("1", InboundType.DS_SC, warehouse.getYandexId(), emptyMap(), null, "Зп-1234");
        var localDate = OffsetDateTime.now(clock).toLocalDate();

        callGetInboundsAndGetResult(inbound.getSortingCenter(), getFilter(localDate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].transportationId").value(inbound.getTransportationId()));
    }

    @DisplayName("Поставка с пустым реестром не вызывает исключения")
    @Test
    @SneakyThrows
    void inboundWithEmptyRegistryReturned() {
        Outbound outbound = testFactory.createOutbound(sortingCenter);
        var inbound = createdInbound("1", InboundType.DS_SC, warehouse.getYandexId(), emptyMap(), null, "Зп-1234");

        Registry outboundRegistry = testFactory.bindRegistry(outbound.getExternalId(), RegistryType.FACTUAL);
        Registry inboundRegistry = testFactory.bindRegistry(inbound, outboundRegistry.getExternalId(),
                RegistryType.PLANNED);
        LocalDate localDate = OffsetDateTime.now(clock).toLocalDate();


        ResultActions resultActions = callGetInboundsAndGetResult(inbound.getSortingCenter(), getFilter(localDate));

        resultActions.andExpect(ResultMatcher.matchAll(
                status().isOk(),
                jsonPath("$.content[0].inboundExternalId").value(inbound.getExternalId())
        ));

    }

    @DisplayName("Создание и фиксация фактического реестра при фиксации Поставки")
    @Test
    @SneakyThrows
    void createFactualRegistryOnInboundFix() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.ENABLE_DISCREPANCY_ACT_GENERATING_BY_REGISTRY_MANAGER, true);

        testFactory.createForToday(
                order(sortingCenter).externalId("acceptedOrder").build()
        ).accept().get();
        testFactory.createForToday(
                order(sortingCenter).externalId("partiallyAcceptedOrder")
                        .places("partiallyAcceptedOrder-1", "partiallyAcceptedOrder-2")
                        .build()
        ).acceptPlace("partiallyAcceptedOrder-1").get();
        testFactory.createForToday(
                order(sortingCenter).externalId("createdOrder").build()
        ).get();
        testFactory.createForToday(
                order(sortingCenter).externalId("sortedOrder").build()
        ).accept().sort().get();
        testFactory.createForToday(
                        order(sortingCenter).externalId("partiallySortedOrder")
                                .places("partiallySortedOrder-1", "partiallySortedOrder-2")
                                .build()
                ).acceptPlace("partiallySortedOrder-1")
                .sortPlace("partiallySortedOrder-1").get();
        testFactory.createForToday(
                order(sortingCenter).externalId("shippedOrder").build()
        ).accept().sort().shipPlace("shippedOrder").get();
        testFactory.createForToday(
                        order(sortingCenter).externalId("partiallyShippedOrder")
                                .places("partiallyShippedOrder-1", "partiallyShippedOrder-2")
                                .dsType(DeliveryServiceType.TRANSIT)
                                .deliveryService(testFactory.storedDeliveryService("4564564"))
                                .build()
                ).acceptPlace("partiallyShippedOrder-1")
                .sortPlace("partiallyShippedOrder-1")
                .shipPlace("partiallyShippedOrder-1")
                .get();
        testFactory.createForToday(
                order(sortingCenter, "cancelledAcceptedOrder").build()
        ).cancel().accept().get();

        testFactory.createForToday(
                order(sortingCenter).externalId("cancelledPartiallyAcceptedOrder")
                        .places("cancelledPartiallyAcceptedOrder-1", "cancelledPartiallyAcceptedOrder-2")
                        .build()
        ).cancel().acceptPlace("cancelledPartiallyAcceptedOrder-1").get();

        var plannedRegistryMap = Map.of("registry_1",
                List.of(
                        Pair.of("acceptedOrder", "acceptedOrder"),
                        Pair.of("partiallyAcceptedOrder", "partiallyAcceptedOrder-1"),
                        Pair.of("partiallyAcceptedOrder", "partiallyAcceptedOrder-2"),
                        Pair.of("createdOrder", "createdOrder"),
                        Pair.of("sortedOrder", "sortedOrder"),
                        Pair.of("partiallySortedOrder", "partiallySortedOrder-1"),
                        Pair.of("partiallySortedOrder", "partiallySortedOrder-2"),
                        Pair.of("shippedOrder", "shippedOrder"),
                        Pair.of("partiallyShippedOrder", "partiallyShippedOrder-1"),
                        Pair.of("partiallyShippedOrder", "partiallyShippedOrder-2"),
                        Pair.of("cancelledAcceptedOrder", "cancelledAcceptedOrder"),
                        Pair.of("cancelledPartiallyAcceptedOrder", "cancelledPartiallyAcceptedOrder-1"),
                        Pair.of("cancelledPartiallyAcceptedOrder", "cancelledPartiallyAcceptedOrder-2")
                )
        );

        var sortingCenterFrom = testFactory.storedSortingCenter2();
        testFactory.setSortingCenterProperty(sortingCenterFrom,
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS, true);
        testFactory.setSortingCenterProperty(sortingCenterFrom,
                SortingCenterPropertiesKey.ENABLE_DISCREPANCY_ACT_GENERATING_BY_REGISTRY_MANAGER, true);
        var deliveryService = testFactory.storedDeliveryService(String.valueOf(sortingCenter.getId()));
        testFactory.setDeliveryServiceProperty(deliveryService,
                DeliveryServiceProperty.DS_SUPPORT_SC_TO_SC_TRANSPORTATIONS, String.valueOf(sortingCenterFrom.getId()));

        var inbound = testFactory.createInbound(TestFactory.CreateInboundParams.builder()
                .inboundExternalId("in-1")
                .inboundType(InboundType.DS_SC)
                .fromDate(OffsetDateTime.now(clock))
                .warehouseFromExternalId(sortingCenterFrom.getYandexId())
                .sortingCenter(sortingCenter)
                .toDate(OffsetDateTime.now(clock))
                .sortingCenter(sortingCenter)
                .registryMap(plannedRegistryMap).build());

        caller.performAction(inbound.getExternalId(), InboundAvailableAction.FIX_INBOUND)
                .andExpect(status().isOk());

        inbound = testFactory.getInbound(inbound.getId());
        assertThat(inbound.getInboundStatus()).isEqualTo(InboundStatus.FIXED);

        var inboundFactualRegistries = registryRepository.findAllByInboundId(inbound.getId())
                .stream()
                .filter(it -> it.getType() == FACTUAL)
                .toList();

        assertThat(inboundFactualRegistries.size()).isEqualTo(1);
        var factualRegistry = inboundFactualRegistries.get(0);
        var registryOrders = boundRegistryRepository.findAllByRegistryId(factualRegistry.getId());
        assertThat(registryOrders.size()).isEqualTo(8);
        var expectedRegistryOrders = Stream.of(
                Pair.of("acceptedOrder", "acceptedOrder"),
                Pair.of("partiallyAcceptedOrder", "partiallyAcceptedOrder-1"),
                Pair.of("sortedOrder", "sortedOrder"),
                Pair.of("partiallySortedOrder", "partiallySortedOrder-1"),
                Pair.of("shippedOrder", "shippedOrder"),
                Pair.of("partiallyShippedOrder", "partiallyShippedOrder-1"),
                Pair.of("cancelledAcceptedOrder", "cancelledAcceptedOrder"),
                Pair.of("cancelledPartiallyAcceptedOrder", "cancelledPartiallyAcceptedOrder-1")
        ).map(it -> new RegistryOrder(
                it.first,
                it.second,
                factualRegistry.getId(),
                null,
                InboundRegistryOrderStatus.FIXED
        )).toArray(RegistryOrder[]::new);

        assertThat(registryOrders)
                .usingElementComparatorIgnoringFields("id", "palletId", "updatedAt", "createdAt")
                .containsExactlyInAnyOrder(expectedRegistryOrders);
    }

    @DisplayName("Создание и фиксация фактического реестра при фиксации Поставки для дропоффа")
    @Test
    @SneakyThrows
    void createDropoffFactualRegistryOnInboundFixForToday() {
        testFactory.setConfiguration(ConfigurationProperties.SUPPLIER_DROPOFF_TRANSFER_ACT_ENABLED, true);
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.IS_DROPOFF, true);

        testFactory.createOrder(
                order(sortingCenter).externalId("acceptedOrder-another-wh")
                        .warehouseFromId("another-wh").build()
        ).updateShipmentDate(LocalDate.now(clock)).accept().get();

        var order = testFactory.createOrder(
                order(sortingCenter).externalId("acceptedOrder").build()
        ).updateShipmentDate(LocalDate.now(clock)).accept().get();

        testFactory.createOrder(
                order(sortingCenter).externalId("acceptedPastOrder").build()
        ).updateShipmentDate(LocalDate.now(clock)).accept().get();

        var warehouseFromYandexId = order.getWarehouseFrom().getYandexId();

        var plannedRegistryMapForYesterday = Map.of("registry_yesterday",
                List.of(
                        Pair.of("acceptedPastOrder", "acceptedPastOrder")
                )
        );
        var plannedRegistryMap = Map.of("registry_today",
                List.of(
                        Pair.of("acceptedOrder", "acceptedOrder")
                )
        );

        var plannedRegistryMapFromAnotherWh = Map.of("registry_today_another_wh",
                List.of(
                        Pair.of("acceptedOrder-another-wh", "acceptedOrder-another-wh")
                )
        );

        var yesterdayInbound = testFactory.createInbound(TestFactory.CreateInboundParams.builder()
                .inboundExternalId("in-yesterday")
                .inboundType(InboundType.DS_SC)
                .fromDate(OffsetDateTime.now(clock).minusDays(1))
                .warehouseFromExternalId(warehouseFromYandexId)
                .toDate(OffsetDateTime.now(clock).minusDays(1))
                .sortingCenter(sortingCenter)
                .registryMap(plannedRegistryMapForYesterday).build());

        testFactory.acceptInbound(yesterdayInbound.getId());

        var inbound = testFactory.createInbound(TestFactory.CreateInboundParams.builder()
                .inboundExternalId("in-today")
                .inboundType(InboundType.DS_SC)
                .fromDate(OffsetDateTime.now(clock))
                .warehouseFromExternalId(warehouseFromYandexId)
                .toDate(OffsetDateTime.now(clock))
                .sortingCenter(sortingCenter)
                .registryMap(plannedRegistryMap).build());

        testFactory.acceptInbound(inbound.getId());


        var inboundFromAnotherWh = testFactory.createInbound(TestFactory.CreateInboundParams.builder()
                .inboundExternalId("inbound-another-wh")
                .inboundType(InboundType.DS_SC)
                .fromDate(OffsetDateTime.now(clock).minusDays(1))
                .warehouseFromExternalId("another-wh")
                .toDate(OffsetDateTime.now(clock).minusDays(1))
                .sortingCenter(sortingCenter)
                .registryMap(plannedRegistryMapFromAnotherWh).build());

        caller.performAction(inbound.getExternalId(), InboundAvailableAction.FIX_INBOUND)
                .andExpect(status().isOk());

        inbound = testFactory.getInbound(inbound.getId());
        assertThat(inbound.getInboundStatus()).isEqualTo(InboundStatus.FIXED);

        var inboundFactualRegistries = registryRepository.findAllByInboundId(inbound.getId())
                .stream()
                .filter(it -> it.getType() == FACTUAL)
                .toList();

        assertThat(inboundFactualRegistries.size()).isEqualTo(1);
        var factualRegistry = inboundFactualRegistries.get(0);
        var registryOrders = boundRegistryRepository.findAllByRegistryId(factualRegistry.getId());
        assertThat(registryOrders.size()).isEqualTo(1);
        var expectedRegistryOrders = Stream.of(
                Pair.of("acceptedOrder", "acceptedOrder")
        ).map(it -> new RegistryOrder(
                it.first,
                it.second,
                factualRegistry.getId(),
                null,
                InboundRegistryOrderStatus.FIXED
        )).toArray(RegistryOrder[]::new);

        assertThat(registryOrders)
                .usingElementComparatorIgnoringFields("id", "palletId", "updatedAt", "createdAt")
                .containsExactlyInAnyOrder(expectedRegistryOrders);
    }

    @DisplayName("Создание и фиксация фактического реестра при фиксации Поставки для дропоффа на вчера")
    @Test
    @SneakyThrows
    void createDropoffFactualRegistryOnInboundFixForYesterday() {
        testFactory.setConfiguration(ConfigurationProperties.SUPPLIER_DROPOFF_TRANSFER_ACT_ENABLED, true);
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.IS_DROPOFF, true);

        var plannedRegistryMapForYesterday = Map.of("registry_yesterday",
                List.of(
                        Pair.of("acceptedPastOrder", "acceptedPastOrder")
                )
        );
        var plannedRegistryMap = Map.of("registry_today",
                List.of(
                        Pair.of("acceptedOrder", "acceptedOrder")
                )
        );

        var yesterdayInbound = testFactory.createInbound(TestFactory.CreateInboundParams.builder()
                .inboundExternalId("in-yesterday")
                .inboundType(InboundType.DS_SC)
                .fromDate(OffsetDateTime.now(clock).minusDays(1))
                .warehouseFromExternalId("1")
                .toDate(OffsetDateTime.now(clock).minusDays(1))
                .sortingCenter(sortingCenter)
                .registryMap(plannedRegistryMapForYesterday).build());

        testFactory.acceptInbound(yesterdayInbound.getId());

        var inbound = testFactory.createInbound(TestFactory.CreateInboundParams.builder()
                .inboundExternalId("in-today")
                .inboundType(InboundType.DS_SC)
                .fromDate(OffsetDateTime.now(clock))
                .warehouseFromExternalId("1")
                .toDate(OffsetDateTime.now(clock))
                .sortingCenter(sortingCenter)
                .registryMap(plannedRegistryMap).build());

        testFactory.acceptInbound(inbound.getId());

        testFactory.createOrder(
                order(sortingCenter).externalId("acceptedOrder").build()
        ).updateShipmentDate(LocalDate.now(clock)).accept().get();

        testFactory.createOrder(
                order(sortingCenter).externalId("acceptedPastOrder").build()
        ).updateShipmentDate(LocalDate.now(clock)).accept().get();

        caller.performAction(yesterdayInbound.getExternalId(), InboundAvailableAction.FIX_INBOUND)
                .andExpect(status().isOk());

        yesterdayInbound = testFactory.getInbound(yesterdayInbound.getId());
        assertThat(yesterdayInbound.getInboundStatus()).isEqualTo(InboundStatus.FIXED);

        var inboundFactualRegistries = registryRepository.findAllByInboundId(yesterdayInbound.getId())
                .stream()
                .filter(it -> it.getType() == FACTUAL)
                .toList();

        assertThat(inboundFactualRegistries.size()).isEqualTo(1);
        var factualRegistry = inboundFactualRegistries.get(0);
        var registryOrders = boundRegistryRepository.findAllByRegistryId(factualRegistry.getId());
        assertThat(registryOrders.size()).isEqualTo(1);
        var expectedRegistryOrders = Stream.of(
                Pair.of("acceptedPastOrder", "acceptedPastOrder")
        ).map(it -> new RegistryOrder(
                it.first,
                it.second,
                factualRegistry.getId(),
                null,
                InboundRegistryOrderStatus.FIXED
        )).toArray(RegistryOrder[]::new);

        assertThat(registryOrders)
                .usingElementComparatorIgnoringFields("id", "palletId", "updatedAt", "createdAt")
                .containsExactlyInAnyOrder(expectedRegistryOrders);
    }

    @DisplayName("Поставки на mock дату отсутствуют")
    @Test
    @SneakyThrows
    void emptyInboundList() {
        String expected = ScTestUtils.fileContent("internal/partner/inbounds/responses/empty.json");
        getInboundAndAssertResponse(expected);
    }

    @DisplayName("Поставка, в которой не указан курьер")
    @Test
    @SneakyThrows
    void inboundWithoutCourier() {
        var inbound = createdInbound("1", InboundType.XDOC_TRANSIT, warehouse.getYandexId(), emptyMap(), null,
                "Зп-1234");
        createSortable("XDOC-p1", SortableType.XDOC_PALLET, inbound);

        var fetched = inboundRepository.findById(inbound.getId()).orElseThrow();
        fetched.setMovementCourier(null);
        inboundRepository.save(fetched);

        mockStatusHistoryBy(inbound);
        String expected = ScTestUtils.fileContent("internal/partner/inbounds/responses/inboundWithoutCourier.json");
        getInboundAndAssertResponse(expected);
    }

    @DisplayName("Поставка, у которой нет склада (такое может быть в случае если поставщик магазин)")
    @Test
    @SneakyThrows
    void inboundWithoutWarehouse() {
        var inbound = createdInbound("1", InboundType.XDOC_TRANSIT);
        createSortable("XDOC-p1", SortableType.XDOC_PALLET, inbound);

        var fetched = inboundRepository.findById(inbound.getId()).orElseThrow();
        fetched.setWarehouseFromId(null);
        inboundRepository.save(fetched);

        mockStatusHistoryBy(inbound);
        String expected = ScTestUtils.fileContent("internal/partner/inbounds/responses/inboundWithoutWarehouse.json");
        getInboundAndAssertResponse(expected);
    }

    @DisplayName("Одна поставка xDoc на текущую дату - плановый реестр отсутствует")
    @Test
    @SneakyThrows
    void xDocWithoutRegistry() {
        var inbound = createdInbound("1", InboundType.XDOC_TRANSIT);
        createSortable("XDOC-p1", SortableType.XDOC_PALLET, inbound);
        createSortable("XDOC-p2", SortableType.XDOC_PALLET, inbound);
        createSortable("XDOC-b1", SortableType.XDOC_BOX, inbound);

        mockStatusHistoryBy(inbound);
        String expected = ScTestUtils.fileContent("internal/partner/inbounds/responses/xDocWithoutRegistry.json");
        getInboundAndAssertResponse(expected);
    }

    @DisplayName("Одна поставка xDoc на текущую дату - есть плановый реестр")
    @Test
    @SneakyThrows
    void xDocWithRegistry() {
        var inbound = createdInbound("1", InboundType.XDOC_TRANSIT);
        var sortable1 = createSortable("XDOC-p1", SortableType.XDOC_PALLET, inbound);
        var sortable2 = createSortable("XDOC-p2", SortableType.XDOC_PALLET, inbound);
        var sortable3 = createSortable("XDOC-b1", SortableType.XDOC_BOX, inbound);
        var registry = testFactory.bindRegistry(inbound, "1111", RegistryType.PLANNED);
        testFactory.bindRegistrySortable(registry, sortable1);
        testFactory.bindRegistrySortable(registry, sortable2);
        testFactory.bindRegistrySortable(registry, sortable3);

        mockStatusHistoryBy(inbound);
        String expected = ScTestUtils.fileContent("internal/partner/inbounds/responses/xDocWithRegistry.json");
        getInboundAndAssertResponse(expected);
    }

    @DisplayName("Одна поставка НЕ xDoc - есть плановый реестр, не один order еще не был принят")
    @Test
    @SneakyThrows
    void nonXDocWithRegistryNotAccepted() {
        var inbound = createdInbound("1", InboundType.DS_SC);
        var registry = testFactory.bindRegistry(inbound, "registryId-2", PLANNED);
        testFactory.bindInboundOrder(inbound, registry, "placeId-1", "palletId-1");
        testFactory.bindInboundOrder(inbound, registry, "placeId-2", "palletId-1");
        testFactory.bindInboundOrder(inbound, registry, "placeId-3", "palletId-2");
        testFactory.bindInboundOrder(inbound, registry, "placeId-4", "palletId-2");

        mockStatusHistoryBy(inbound);
        String expected = ScTestUtils
                .fileContent("internal/partner/inbounds/responses/nonXDocWithRegistryNotAccepted.json");
        getInboundAndAssertResponse(expected);
    }

    @DisplayName("Одна поставка НЕ xDoc - нет реестра")
    @Test
    @SneakyThrows
    void nonXDocWithoutRegistry() {
        createdInbound("1", InboundType.DS_SC);
        String expected = ScTestUtils.fileContent("internal/partner/inbounds/responses/nonXDocWithoutRegistry.json");
        getInboundAndAssertResponse(expected);
    }

    @DisplayName("Несколько поставок, есть xDoc и не xDoc")
    @Test
    @SneakyThrows
    void multipleInbounds() {
        var storedWarehouse = testFactory.storedWarehouse("yandex-id", WarehouseType.DROPOFF);

        var xDocInboundWithPlan = createdInbound("1", InboundType.XDOC_TRANSIT);
        var sortable1 = createSortable("XDOC-p1", SortableType.XDOC_PALLET, xDocInboundWithPlan);
        var sortable2 = createSortable("XDOC-p2", SortableType.XDOC_PALLET, xDocInboundWithPlan);
        var sortable3 = createSortable("XDOC-b1", SortableType.XDOC_BOX, xDocInboundWithPlan);
        var registry = testFactory.bindRegistry(xDocInboundWithPlan, "1111", RegistryType.PLANNED);
        testFactory.bindRegistrySortable(registry, sortable1);
        testFactory.bindRegistrySortable(registry, sortable2);
        testFactory.bindRegistrySortable(registry, sortable3);

        MovementCourier courier = testFactory.storedMovementCourier(
                new MovementCourier("111", "name", "OOO name", null, null)
        );

        var nonXDocInbound = createdInbound("2", InboundType.DS_SC, storedWarehouse, courier);
        var nonXDocReg = testFactory.bindRegistry(nonXDocInbound, "registryId-3", PLANNED);
        testFactory.bindInboundOrder(nonXDocInbound, nonXDocReg, "placeId-1", "palletId-1");
        testFactory.bindInboundOrder(nonXDocInbound, nonXDocReg, "placeId-2", "palletId-1");
        testFactory.bindInboundOrder(nonXDocInbound, nonXDocReg, "placeId-3", "palletId-2");
        testFactory.bindInboundOrder(nonXDocInbound, nonXDocReg, "placeId-4", "palletId-2");

        var xDocInbound = createdInbound("3", InboundType.XDOC_TRANSIT);
        createSortable("XDOC-p3", SortableType.XDOC_PALLET, xDocInbound);
        createSortable("XDOC-p4", SortableType.XDOC_PALLET, xDocInbound);
        createSortable("XDOC-b2", SortableType.XDOC_BOX, xDocInbound);

        var nonXDocInbound2 = createdInbound("4", InboundType.DS_SC, storedWarehouse, courier);
        var nonXDocReg2 = testFactory.bindRegistry(nonXDocInbound2, "registryId-4", PLANNED);
        testFactory.bindInboundOrder(nonXDocInbound2, nonXDocReg2, "placeId-5", "palletId-6");

        mockStatusHistoryBy(xDocInboundWithPlan);
        mockStatusHistoryBy(xDocInbound);
        mockStatusHistoryBy(nonXDocInbound);
        mockStatusHistoryBy(nonXDocInbound2);

        getInboundAndAssertResponse(
                ScTestUtils.fileContent("internal/partner/inbounds/responses/multipleInbounds.json")
        );
        getInboundAndAssertResponse(
                ScTestUtils.fileContent("internal/partner/inbounds/responses/multipleInbounds_page0.json"),
                PageRequest.of(0, 2)
        );
        getInboundAndAssertResponse(
                ScTestUtils.fileContent("internal/partner/inbounds/responses/multipleInbounds_page1.json"),
                PageRequest.of(1, 2)
        );
    }

    @DisplayName("Несколько поставок, с одинаковыми курьером и датой мержатся в одну, если он с дропоффа")
    @Test
    @SneakyThrows
    void multipleInbounds1() {
        MovementCourier courier = testFactory.storedMovementCourier(
                new MovementCourier("111", "name", "OOO name", null, null)
        );

        var specificWarehouse = testFactory.storedWarehouse("yandex-id", WarehouseType.DROPOFF);

        var nonXDocInbound = createdInbound("1", InboundType.DS_SC, specificWarehouse, courier);
        var nonXDocReg = testFactory.bindRegistry(nonXDocInbound, "registryId-3", PLANNED);
        testFactory.bindInboundOrder(nonXDocInbound, nonXDocReg, "placeId-1", "palletId-1");
        testFactory.bindInboundOrder(nonXDocInbound, nonXDocReg, "placeId-2", "palletId-1");
        testFactory.bindInboundOrder(nonXDocInbound, nonXDocReg, "placeId-3", "palletId-2");
        testFactory.bindInboundOrder(nonXDocInbound, nonXDocReg, "placeId-4", "palletId-2");

        var nonXDocInbound2 = createdInbound("22", InboundType.DS_SC, specificWarehouse, courier);
        var nonXDocInbound3 = createdInbound("333", InboundType.DS_SC, specificWarehouse, courier);
        var nonXDocInbound4 = createdInbound("4444", InboundType.DS_SC, specificWarehouse, courier);
        var nonXDocReg2 = testFactory.bindRegistry(nonXDocInbound2, "registryId-4", PLANNED);
        testFactory.bindInboundOrder(nonXDocInbound2, nonXDocReg2, "placeId-5", "palletId-6");


        mockStatusHistoryBy(nonXDocInbound);
        mockStatusHistoryBy(nonXDocInbound2);
        mockStatusHistoryBy(nonXDocInbound3);
        mockStatusHistoryBy(nonXDocInbound4);

        getInboundAndAssertResponse(
                ScTestUtils.fileContent("internal/partner/inbounds/responses/multipleInboundsMergeDropoff.json")
        );
    }
    @DisplayName("Несколько поставок, с одинаковыми курьером и датой НЕ мержатся в одну, если курьеры разные")
    @Test
    @SneakyThrows
    void multipleInbounds1A() {
        MovementCourier courier = testFactory.storedMovementCourier(
                new MovementCourier("111", "name", "OOO name", null, null)
        );

        var specificWarehouse = testFactory.storedWarehouse("yandex-id", WarehouseType.DROPOFF);

        var nonXDocInbound = createdInbound("1", InboundType.DS_SC, specificWarehouse, courier);
        var nonXDocReg = testFactory.bindRegistry(nonXDocInbound, "registryId-3", PLANNED);
        testFactory.bindInboundOrder(nonXDocInbound, nonXDocReg, "placeId-1", "palletId-1");
        testFactory.bindInboundOrder(nonXDocInbound, nonXDocReg, "placeId-2", "palletId-1");
        testFactory.bindInboundOrder(nonXDocInbound, nonXDocReg, "placeId-3", "palletId-2");
        testFactory.bindInboundOrder(nonXDocInbound, nonXDocReg, "placeId-4", "palletId-2");

        var nonXDocInbound2 = createdInbound(
                "22", InboundType.DS_SC, specificWarehouse, courier);
        var nonXDocInbound3 = createdInbound(
                "333", InboundType.DS_SC, specificWarehouse, testFactory.storedMovementCourier(200L));
        var nonXDocInbound4 = createdInbound(
                "4444", InboundType.DS_SC, specificWarehouse, testFactory.storedMovementCourier(300L));
        var nonXDocReg2 = testFactory.bindRegistry(nonXDocInbound2, "registryId-4", PLANNED);
        testFactory.bindInboundOrder(nonXDocInbound2, nonXDocReg2, "placeId-5", "palletId-6");


        mockStatusHistoryBy(nonXDocInbound);
        mockStatusHistoryBy(nonXDocInbound2);
        mockStatusHistoryBy(nonXDocInbound3);
        mockStatusHistoryBy(nonXDocInbound4);

        getInboundAndAssertResponse(
                ScTestUtils.fileContent(
                    "internal/partner/inbounds/responses/multipleInboundsDontMergeDropoffWithDifferentCouriers.json")
        );
    }

    @DisplayName("Несколько поставок, с одинаковым складом и курьером НЕ мержатся в одну, если это складская поставка")
    @Test
    @SneakyThrows
    void multipleInbounds2() {
        MovementCourier courier = testFactory.storedMovementCourier(
                new MovementCourier("111", "name", "OOO name", null, null)
        );

        var specificWarehouse = testFactory.storedWarehouse("yandex-id", WarehouseType.SORTING_CENTER);

        var nonXDocInbound = createdInbound("1", InboundType.DS_SC, specificWarehouse, courier);
        var nonXDocReg = testFactory.bindRegistry(nonXDocInbound, "registryId-3", PLANNED);
        testFactory.bindInboundOrder(nonXDocInbound, nonXDocReg, "placeId-1", "palletId-1");
        testFactory.bindInboundOrder(nonXDocInbound, nonXDocReg, "placeId-2", "palletId-1");
        testFactory.bindInboundOrder(nonXDocInbound, nonXDocReg, "placeId-3", "palletId-2");
        testFactory.bindInboundOrder(nonXDocInbound, nonXDocReg, "placeId-4", "palletId-2");

        var nonXDocInbound2 = createdInbound("22", InboundType.DS_SC, specificWarehouse, courier);
        var nonXDocInbound3 = createdInbound("333", InboundType.DS_SC, specificWarehouse, courier);
        var nonXDocInbound4 = createdInbound("4444", InboundType.DS_SC, specificWarehouse, courier);
        var nonXDocReg2 = testFactory.bindRegistry(nonXDocInbound2, "registryId-4", PLANNED);

        testFactory.bindInboundOrder(nonXDocInbound2, nonXDocReg2, "placeId-5", "palletId-6");


        mockStatusHistoryBy(nonXDocInbound);
        mockStatusHistoryBy(nonXDocInbound2);
        mockStatusHistoryBy(nonXDocInbound3);
        mockStatusHistoryBy(nonXDocInbound4);

        getInboundAndAssertResponse(
                ScTestUtils.fileContent("internal/partner/inbounds/responses/multipleInboundsDontMergeWarehouseShop" +
                        ".json")
        );
    }

    @DisplayName("Несколько поставок, с одинаковым складом и курьером НЕ мержатся в одну, если это магазинная поставка")
    @Test
    @SneakyThrows
    void multipleInbounds3() {
        MovementCourier courier = testFactory.storedMovementCourier(
                new MovementCourier("111", "name", "OOO name", null, null)
        );

        var specificWarehouse = testFactory.storedWarehouse("yandex-id", WarehouseType.SHOP);

        var nonXDocInbound = createdInbound("1", InboundType.DS_SC, specificWarehouse, courier);
        var nonXDocReg = testFactory.bindRegistry(nonXDocInbound, "registryId-3", PLANNED);
        testFactory.bindInboundOrder(nonXDocInbound, nonXDocReg, "placeId-1", "palletId-1");
        testFactory.bindInboundOrder(nonXDocInbound, nonXDocReg, "placeId-2", "palletId-1");
        testFactory.bindInboundOrder(nonXDocInbound, nonXDocReg, "placeId-3", "palletId-2");
        testFactory.bindInboundOrder(nonXDocInbound, nonXDocReg, "placeId-4", "palletId-2");

        var nonXDocInbound2 = createdInbound("22", InboundType.DS_SC, specificWarehouse, courier);
        var nonXDocInbound3 = createdInbound("333", InboundType.DS_SC, specificWarehouse, courier);
        var nonXDocInbound4 = createdInbound("4444", InboundType.DS_SC, specificWarehouse, courier);
        var nonXDocReg2 = testFactory.bindRegistry(nonXDocInbound2, "registryId-4", PLANNED);

        testFactory.bindInboundOrder(nonXDocInbound2, nonXDocReg2, "placeId-5", "palletId-6");


        mockStatusHistoryBy(nonXDocInbound);
        mockStatusHistoryBy(nonXDocInbound2);
        mockStatusHistoryBy(nonXDocInbound3);
        mockStatusHistoryBy(nonXDocInbound4);

        getInboundAndAssertResponse(
                ScTestUtils.fileContent(
                        "internal/partner/inbounds/responses/multipleInboundsDontMergeWarehouseShop.json")
        );
    }


    @Test
    @SneakyThrows
    void xdocStatusSorting() {
        flow.inboundBuilder("first")
                .nextLogisticPoint("123")
                .informationListBarcode("Зп-1")
                .build();
        flow.inboundBuilder("second")
                .nextLogisticPoint("123")
                .informationListBarcode("Зп-2")
                .build()
                .linkPallets("XDOC-1")
                .fixInbound();

        var result = inboundFacade.getPartnerInbounds(
                forToday(),
                sortingCenter,
                PageRequest.of(0, 10)
        );

        Assertions.assertEquals(result.get().findFirst()
                        .map(InboundPartnerDto::getInboundStatus)
                        .orElseThrow(() -> new IllegalStateException("Empty status")),
                InboundPartnerStatusDto.FIXED
        );
    }

    @DisplayName("Фильтр по id склада (поставщика)")
    @Test
    @SneakyThrows
    void warehouseFilter() {
        var firstWarehouse = testFactory.storedWarehouse("some-ya-id", WarehouseType.DROPOFF);
        Warehouse otherWarehouse = testFactory.storedWarehouse("77410094", WarehouseType.DROPOFF);
        MovementCourier courier = testFactory.storedMovementCourier(
                new MovementCourier("111", "name", "OOO name", null, null)
        );
        MovementCourier otherCourier = testFactory.storedMovementCourier(
                new MovementCourier("112", "name", "OOO name", null, null)
        );

        var nonXDocInbound = createdInbound("1", InboundType.DS_SC, firstWarehouse, courier);
        var nonXDocReg = testFactory.bindRegistry(nonXDocInbound, "registryId-3", PLANNED);
        testFactory.bindInboundOrder(nonXDocInbound, nonXDocReg, "placeId-1", "palletId-1");
        testFactory.bindInboundOrder(nonXDocInbound, nonXDocReg, "placeId-2", "palletId-1");
        testFactory.bindInboundOrder(nonXDocInbound, nonXDocReg, "placeId-3", "palletId-2");
        testFactory.bindInboundOrder(nonXDocInbound, nonXDocReg, "placeId-4", "palletId-2");

        var nonXDocInbound2 = createdInbound("2", InboundType.DS_SC, otherWarehouse, courier);
        var nonXDocReg2 = testFactory.bindRegistry(nonXDocInbound2, "registryId-4", PLANNED);
        testFactory.bindInboundOrder(nonXDocInbound2, nonXDocReg2, "placeId-5", "palletId-6");

        var nonXDocInbound3 = createdInbound("3", InboundType.DS_SC, otherWarehouse, otherCourier);
        var nonXDocReg3 = testFactory.bindRegistry(nonXDocInbound3, "registryId-5", PLANNED);
        testFactory.bindInboundOrder(nonXDocInbound3, nonXDocReg3, "placeId-7", "palletId-8");

        var xDocInbound = createdInbound("4", InboundType.XDOC_TRANSIT, firstWarehouse, null);
        createSortable("XDOC-p3", SortableType.XDOC_PALLET, xDocInbound);
        createSortable("XDOC-p4", SortableType.XDOC_PALLET, xDocInbound);
        createSortable("XDOC-b2", SortableType.XDOC_BOX, xDocInbound);

        var xDocInbound2 = createdInbound("5", InboundType.XDOC_TRANSIT, otherWarehouse, null);
        createSortable("XDOC-p6", SortableType.XDOC_PALLET, xDocInbound2);

        mockStatusHistoryBy(nonXDocInbound);
        mockStatusHistoryBy(nonXDocInbound2);
        mockStatusHistoryBy(nonXDocInbound3);
        mockStatusHistoryBy(xDocInbound);
        mockStatusHistoryBy(xDocInbound2);

        getInboundAndAssertResponse(
                ScTestUtils.fileContent("internal/partner/inbounds/responses/warehouseFilter.json"),
                firstWarehouse.getId()
        );
    }

    @Test
    @SneakyThrows
    @DisplayName("Количество заказов и палет по плану и принятых")
    void acceptedOrdersAmount() {
        var courier = testFactory.storedMovementCourier(
                new MovementCourier("111", "name", "OOO name", null, null)
        );
        var nonXDocInbound = createdInbound("1", InboundType.DS_SC, warehouse, courier);
        testFactory.createForToday(order(sortingCenter)
                .externalId(nonXDocInbound.getExternalId())
                .places("place-1", "place-2")
                .warehouseFromId(warehouse.getYandexId())
                .build()
        ).acceptPlaces("place-1").get();

        var nonXDocReg = testFactory.bindRegistry(nonXDocInbound, "registryId-1", PLANNED);
        testFactory.bindInboundOrder(nonXDocInbound, nonXDocReg, "place-1", "palletId-1");
        testFactory.bindInboundOrder(nonXDocInbound, nonXDocReg, "place-2", "palletId-1");
        testFactory.bindInboundOrder(nonXDocInbound, nonXDocReg, "place-3", "palletId-2");

        callGetInboundsAndGetResult(sortingCenter, getFilter(LocalDate.now(clock)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].externalId").value(nonXDocInbound.getExternalId()))
                .andExpect(jsonPath("$.content[0].totalAmount").value(3))
                .andExpect(jsonPath("$.content[0].acceptedAmount").value(1))
                .andExpect(jsonPath("$.content[0].plannedPalletAmount").value(2))
                .andExpect(jsonPath("$.content[0].palletAmount").value(1));
    }

    @DisplayName("Успешное завершение поставки по штрих-коду с информационного листа")
    @Test
    @SneakyThrows
    void successfulFixInbound() {
        var specificUser = testFactory.storedUser(sortingCenter, 1234);
        var inbound = testFactory.createInbound(TestFactory.CreateInboundParams.builder()
                .inboundExternalId("101")
                .inboundType(InboundType.XDOC_TRANSIT)
                .fromDate(OffsetDateTime.now(clock))
                .warehouseFromExternalId("warehouse-from-id")
                .toDate(OffsetDateTime.now(clock))
                .sortingCenter(sortingCenter)
                .registryMap(Map.of())
                .informationListBarcode("Зп-12345")
                .nextLogisticPointId("1234")
                .confirmed(true)
                .build());
        testFactory.linkSortableToInbound(inbound, "XDOC-11", SortableType.XDOC_PALLET, specificUser);

        caller.performAction("Зп-12345", InboundAvailableAction.FIX_INBOUND)
                .andExpect(status().isOk());
    }

    @DisplayName("Попытка завершить поставку, но есть дубли по штрих-коду с информационного листа")
    @Test
    @SneakyThrows
    void fixInboundButThereAreDuplicateInbondByInfoListCode() {
        var specificUser = testFactory.storedUser(sortingCenter, 1234);
        var oldInbound = testFactory.createInbound(TestFactory.CreateInboundParams.builder()
                .inboundExternalId("99")
                .inboundType(InboundType.XDOC_TRANSIT)
                .fromDate(OffsetDateTime.now(clock))
                .warehouseFromExternalId("warehouse-from-id")
                .toDate(OffsetDateTime.now(clock))
                .sortingCenter(sortingCenter)
                .registryMap(Map.of())
                .informationListBarcode("Зп-12345")
                .nextLogisticPointId("1234")
                .build());
        var inbound = testFactory.createInbound(TestFactory.CreateInboundParams.builder()
                .inboundExternalId("101")
                .inboundType(InboundType.XDOC_TRANSIT)
                .fromDate(OffsetDateTime.now(clock))
                .warehouseFromExternalId("warehouse-from-id")
                .toDate(OffsetDateTime.now(clock))
                .sortingCenter(sortingCenter)
                .registryMap(Map.of())
                .informationListBarcode("Зп-12345")
                .nextLogisticPointId("1234")
                .confirmed(true)
                .build());
        testFactory.linkSortableToInbound(inbound, "XDOC-11", SortableType.XDOC_PALLET, specificUser);

        caller.performAction("Зп-12345", InboundAvailableAction.FIX_INBOUND)
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath(
                        "$.message",
                        equalTo("Болле одной поставки с таким номером")
                ));
    }

    @DisplayName("Успешное завершение поставки по штрих-коду с информационного листа")
    @Test
    @SneakyThrows
    void attemptToFixEmptyInbound() {
        testFactory.createInbound(TestFactory.CreateInboundParams.builder()
                .inboundExternalId("101")
                .inboundType(InboundType.XDOC_TRANSIT)
                .fromDate(OffsetDateTime.now(clock))
                .warehouseFromExternalId("warehouse-from-id")
                .toDate(OffsetDateTime.now(clock))
                .sortingCenter(sortingCenter)
                .registryMap(Map.of())
                .informationListBarcode("Зп-12345")
                .build());

        caller.performAction("Зп-12345", InboundAvailableAction.FIX_INBOUND)
                .andExpect(status().is(400))
                .andExpect(jsonPath("$.error").value("CANT_FIX_EMPTY_INBOUND"));
    }

    @DisplayName("Попытка завершить не существующую поставку")
    @Test
    @SneakyThrows
    void attemptToFixInboundThatDoesNotExist() {
        var nonExistentId = "99999111";

        caller.performAction(nonExistentId, InboundAvailableAction.FIX_INBOUND)
                .andExpect(status().is(404))
                .andExpect(jsonPath("$.error").value("ENTITY_NOT_FOUND"));
    }

    @DisplayName("Попытка завершить поставку, которая принадлежит другому Сорт Центру")
    @Test
    @SneakyThrows
    void attemptToFixInboundWhichDoesNotBelongToSortingCenter() {
        var sortCenterOwner = testFactory.storedSortingCenter(100001);
        var specificUser = testFactory.storedUser(sortCenterOwner, 124);

        var inbound = testFactory.createInbound(TestFactory.CreateInboundParams.builder()
                .inboundExternalId("101")
                .inboundType(InboundType.XDOC_TRANSIT)
                .fromDate(OffsetDateTime.now(clock))
                .warehouseFromExternalId("warehouse-from-id")
                .toDate(OffsetDateTime.now(clock))
                .sortingCenter(sortCenterOwner)
                .registryMap(Map.of())
                .confirmed(true)
                .build());
        testFactory.linkSortableToInbound(inbound, "XDOC-11", SortableType.XDOC_PALLET, specificUser);

        foreignCaller.performAction(inbound.getExternalId(), InboundAvailableAction.FIX_INBOUND)
                .andExpect(status().is(404))
                .andExpect(jsonPath("$.error").value("ENTITY_NOT_FOUND"));
    }

    @DisplayName("Фильтрация по типу поставки")
    @Test
    @SneakyThrows
    void filterByInboundType() {
        var xDocInboundWithPlan = createdInbound("1", InboundType.XDOC_TRANSIT);
        var sortable1 = createSortable("XDOC-p1", SortableType.XDOC_PALLET, xDocInboundWithPlan);
        var sortable2 = createSortable("XDOC-p2", SortableType.XDOC_PALLET, xDocInboundWithPlan);
        var sortable3 = createSortable("XDOC-b1", SortableType.XDOC_BOX, xDocInboundWithPlan);
        var registry = testFactory.bindRegistry(xDocInboundWithPlan, "1111", RegistryType.PLANNED);
        testFactory.bindRegistrySortable(registry, sortable1);
        testFactory.bindRegistrySortable(registry, sortable2);
        testFactory.bindRegistrySortable(registry, sortable3);

        var xDocInbound = createdInbound("2", InboundType.XDOC_TRANSIT);
        createSortable("XDOC-p3", SortableType.XDOC_PALLET, xDocInbound);
        createSortable("XDOC-p4", SortableType.XDOC_PALLET, xDocInbound);

        var nonXDocInbound = createdInbound("3", InboundType.DS_SC);
        var nonXDocReg = testFactory.bindRegistry(nonXDocInbound, "registryId-3", PLANNED);
        testFactory.bindInboundOrder(nonXDocInbound, nonXDocReg, "placeId-1", "palletId-1");
        testFactory.bindInboundOrder(nonXDocInbound, nonXDocReg, "placeId-2", "palletId-1");
        testFactory.bindInboundOrder(nonXDocInbound, nonXDocReg, "placeId-3", "palletId-2");
        testFactory.bindInboundOrder(nonXDocInbound, nonXDocReg, "placeId-4", "palletId-2");

        mockStatusHistoryBy(xDocInboundWithPlan);
        mockStatusHistoryBy(xDocInbound);
        mockStatusHistoryBy(nonXDocInbound);
        String expected = ScTestUtils.fileContent("internal/partner/inbounds/responses/filterByInboundType.json");
        getInboundAndAssertResponse(expected, List.of(InboundType.XDOC_TRANSIT));
    }

    @DisplayName("Фильтрация по типу поставки с не валидным фильтром")
    @Test
    @SneakyThrows
    void filterByInboundTypeWithInvalidFilter() {
        String filter = "?date=" + LocalDate.now(clock) + "&types=INVALID2";
        callGetInboundsAndGetResult(sortingCenter, filter)
                .andExpect(status().is4xxClientError());
    }

    @DisplayName("Запрос на получение баркодов без фильтра")
    @Test
    @SneakyThrows
    void generateXDocBarcodesWithoutFilter() {
        var response = callBarcodes(null)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(response).hasSizeGreaterThan(0);  // сгенерированная jasper'ом pdf - корректный способ сравнениея
        // не работает
    }

    @DisplayName("Запрос на получение баркодов с фильтром на 5 штук")
    @Test
    @SneakyThrows
    void generateXDocBarcodes() {
        var response = callBarcodes(5)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(response).hasSizeGreaterThan(0);  // сгенерированная jasper'ом pdf - корректный способ сравнениея
        // не работает
    }

    @ParameterizedTest(name = "Ошибка при запросе на получение баркодов: меньше 1 и более 100 ({0}) штук")
    @ValueSource(ints = {-1, 0, 1001})
    @SneakyThrows
    void generateXDocBarcodesNegative(int amount) {
        callBarcodes(amount)
                .andExpect(status().is4xxClientError());
    }

    @ParameterizedTest(name = "Фильтр по номеру поставки: {2}")
    @MethodSource("getNameFilter")
    void filterByPartName(String nameFilter, String expectedExternalId, String comment) {
        flow.inboundBuilder("9999111122")
                .informationListBarcode("Зп-370123223")
                .build();

        // лишняя XDOC поставка, которая не должна появиться в response
        flow.inboundBuilder("111111111111")
                .informationListBarcode("Зп-37111111111")
                .build();

        var dropShipInbound = createdInbound("66666661111", InboundType.DS_SC);
        var registry = testFactory.bindRegistry(dropShipInbound, "registryId-2", PLANNED);
        testFactory.bindInboundOrder(dropShipInbound, registry, "placeId-2", "palletId-2");

        // лишная дропшип поставка, которая не должна появиться в response
        var dropShipInbound2 = createdInbound("77777772222", InboundType.DS_SC);
        var registry2 = testFactory.bindRegistry(dropShipInbound2, "registryId-3", PLANNED);
        testFactory.bindInboundOrder(dropShipInbound2, registry2, "placeId-3", "palletId-3");

        caller.getInbounds(
                InboundPartnerParamsDto.builder()
                        .date(LocalDate.now(clock))
                        .namePart(nameFilter)
                        .build()
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].inboundExternalId").value(expectedExternalId));
    }

    private static List<Arguments> getNameFilter() {
        return List.of(
                Arguments.of("66666661111", "66666661111", "поиск дропшип поставки"),
                Arguments.of("9999111122", "Зп-370123223", "поиск XDOC поставки по externalId"),
                Arguments.of("Зп-370123223", "Зп-370123223", "поиск XDOC поставки по informationListCode"),
                Arguments.of("666611", "66666661111", "поиск дропшип поставки по частичному имени"),
                Arguments.of("911112", "Зп-370123223", "поиск XDOC поставки по частичному externalId"),
                Arguments.of("0123223", "Зп-370123223", "поиск XDOC поставки по частичному informationListCode")
        );
    }

    @DisplayName("Фильтр по диапазону дат")
    @Test
    void filterByDateRange() {
        flow.inboundBuilder("in-1")
                .date(LocalDate.of(2021, 1, 1))
                .build();
        flow.inboundBuilder("in-2")
                .date(LocalDate.of(2021, 1, 2))
                .build();
        flow.inboundBuilder("in-3")
                .date(LocalDate.of(2021, 1, 3))
                .build();

        caller.getInbounds(
                InboundPartnerParamsDto.builder()
                        .date(LocalDate.of(2021, 1, 1))
                        .dateTo(LocalDate.of(2021, 1, 1))
                        .build()
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].inboundExternalId").value("in-1"));

        caller.getInbounds(
                InboundPartnerParamsDto.builder()
                        .date(LocalDate.of(2021, 1, 1))
                        .dateTo(LocalDate.of(2021, 1, 2))
                        .build()
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(2)))
                .andExpect(jsonPath("$.content[0].inboundExternalId").value("in-1"))
                .andExpect(jsonPath("$.content[1].inboundExternalId").value("in-2"));
    }

    @DisplayName("Фильтр по статусу поставки XDOC")
    @Nested
    class InboundPartnerStatusFilterXDOCTest {

        Warehouse samaraWH;
        Cell samaraCell;

        @BeforeEach
        void init() {
            samaraWH = testFactory.storedWarehouse("123123123");
            samaraCell = testFactory.storedCell(
                    sortingCenter,
                    "samara-keep-2",
                    CellType.BUFFER,
                    CellSubType.BUFFER_XDOC,
                    samaraWH.getYandexId());
        }

        @ParameterizedTest(name = "фильтр по статусу {1}")
        @MethodSource("getExpectedExternalIdAndStatusFilter")
        void filterByStatus(String externalId, InboundPartnerStatusDto statusFilter) {
            // создаем поставки с различными статусами
            inboundCreated("in-1");
            inboundConfirmed("in-2");
            inboundCarArrived("in-2-1");
            inboundReadyToReceive("in-3");
            palletLinkedToInbound("in-4", "XDOC-4");
            palletSortedToCell("in-5", "XDOC-5");
            inboundFixed("in-6", "XDOC-6");
            inboundLeftSortingCenterWithOutbound("in-7", "out-7", "XDOC-7");

            // проверяем что все поставки были созданы
            assertThat(
                    StreamEx.of(inboundRepository.findAll())
                            .map(Inbound::getExternalId)
                            .toList()
            ).containsExactlyInAnyOrder("in-1", "in-2", "in-2-1", "in-3", "in-4", "in-5", "in-6", "in-7");

            // выполняем поиск с фильтром по статусам
            caller.getInbounds(
                    InboundPartnerParamsDto.builder()
                            .date(LocalDate.now(clock))
                            .statuses(Set.of(statusFilter))
                            .build()
            ).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                    .andExpect(jsonPath("$.content[0].inboundExternalId").value(externalId));
        }

        private static List<Arguments> getExpectedExternalIdAndStatusFilter() {
            return List.of(
                    Arguments.of("in-1", InboundPartnerStatusDto.CREATED),
                    Arguments.of("in-2", InboundPartnerStatusDto.CONFIRMED),
                    Arguments.of("in-2-1", InboundPartnerStatusDto.CAR_ARRIVED),
                    Arguments.of("in-3", InboundPartnerStatusDto.READY_TO_RECEIVE),
                    Arguments.of("in-4", InboundPartnerStatusDto.NEED_SORTING),
                    Arguments.of("in-5", InboundPartnerStatusDto.READY_TO_BE_FIXED),
                    Arguments.of("in-6", InboundPartnerStatusDto.FIXED),
                    Arguments.of("in-7", InboundPartnerStatusDto.SHIPPED)
            );
        }

        @DisplayName("Фильтр по нескольким статусам")
        @Test
        void filterBySeveralStatuses() {
            inboundCreated("in-1");
            inboundConfirmed("in-2");
            inboundReadyToReceive("in-3");

            caller.getInbounds(
                    InboundPartnerParamsDto.builder()
                            .date(LocalDate.now(clock))
                            .statuses(Set.of(
                                    InboundPartnerStatusDto.CREATED,
                                    InboundPartnerStatusDto.CONFIRMED
                            ))
                            .build()
            ).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").value(Matchers.hasSize(2)))
                    .andExpect(jsonPath("$.content[0].inboundExternalId").value("in-2"))
                    .andExpect(jsonPath("$.content[1].inboundExternalId").value("in-1"));
        }

        @DisplayName("Фильтр по статусам пустой")
        @Test
        void filterByStatusesEmpty() {
            inboundCreated("in-1");
            inboundConfirmed("in-2");
            inboundReadyToReceive("in-3");

            caller.getInbounds(
                    InboundPartnerParamsDto.builder()
                            .date(LocalDate.now(clock))
                            .statuses(Set.of())
                            .build()
            ).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").value(Matchers.hasSize(3)))
                    .andExpect(jsonPath("$.content[0].inboundExternalId").value("in-3"))
                    .andExpect(jsonPath("$.content[1].inboundExternalId").value("in-2"))
                    .andExpect(jsonPath("$.content[2].inboundExternalId").value("in-1"));

            caller.getInbounds(
                    InboundPartnerParamsDto.builder()
                            .date(LocalDate.now(clock))
                            .statuses(null)
                            .build()
            ).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").value(Matchers.hasSize(3)))
                    .andExpect(jsonPath("$.content[0].inboundExternalId").value("in-3"))
                    .andExpect(jsonPath("$.content[1].inboundExternalId").value("in-2"))
                    .andExpect(jsonPath("$.content[2].inboundExternalId").value("in-1"));
        }

        private void inboundCreated(String externalId) {
            flow.inboundBuilder(externalId)
                    .confirm(false)
                    .nextLogisticPoint(samaraWH.getYandexId())
                    .build();
        }

        private void inboundConfirmed(String externalId) {
            inboundCreated(externalId);
            testFactory.confirmInbound(externalId);
        }

        private void inboundCarArrived(String externalId) {
            inboundConfirmed(externalId);
            caller.inboundCarArrived(externalId, PUT_CAR_INFO_REQUEST).andExpect(status().isOk());
        }

        private void inboundReadyToReceive(String externalId) {
            inboundCarArrived(externalId);

            caller.performAction(externalId, InboundAvailableAction.READY_TO_RECEIVE)
                    .andExpect(status().isOk());
            assertThat(inboundRepository.findByExternalId(externalId).orElseThrow())
                    .extracting(Inbound::getInboundStatus).isEqualTo(InboundStatus.READY_TO_RECEIVE);
        }

        private void palletLinkedToInbound(String externalId, String barcode) {
            inboundReadyToReceive(externalId);

            flow.toArrival(externalId)
                    .linkPallets(barcode);
        }

        private void palletSortedToCell(String externalId, String barcode) {
            palletLinkedToInbound(externalId, barcode);

            sortableTestFactory.sortByBarcode(barcode, samaraCell.getId());
        }

        private void inboundFixed(String externalId, String barcode) {
            palletSortedToCell(externalId, barcode);

            caller.performAction(externalId, InboundAvailableAction.FIX_INBOUND)
                    .andExpect(status().isOk());
        }

        private void inboundLeftSortingCenterWithOutbound(String inboundExtId, String outboundExtId, String barcode) {
            inboundFixed(inboundExtId, barcode);

            // Поставка была отгружена
            flow.createOutbound(outboundExtId)
                    .addRegistryPallets(barcode)
                    .buildRegistry()
                    .sortToAvailableCell(barcode)
                    .prepareToShip(barcode)
                    .shipAndGet(outboundExtId);
        }
    }

    @SuppressWarnings("checkstyle:MethodLength")
    @DisplayName("Смена статусов и доступных действий для XDOC поставки")
    @Test
    void actionAndStatusForXDocInbound() {
        Warehouse specificWarehouse = testFactory.storedWarehouse("101010987");
        Cell cell = testFactory.storedCell(
                sortingCenter,
                "samara-keep",
                CellType.BUFFER,
                CellSubType.BUFFER_XDOC,
                specificWarehouse.getYandexId());

        // поставка создана, но не подтверждена
        flow.inboundBuilder("in-1")
                .confirm(false)
                .nextLogisticPoint(specificWarehouse.getYandexId())
                .build();

        var inbound = testFactory.getInbound("in-1");

        caller.getInbounds(forToday())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].inboundExternalId").value("in-1"))
                .andExpect(jsonPath("$.content[0].actions").isArray())
                .andExpect(jsonPath("$.content[0].actions").isEmpty())
                .andExpect(jsonPath("$.content[0].complexActions").value(ComplexInboundAction.PUT_INBOUND_INFO.name()))
                .andExpect(jsonPath("$.content[0].inboundStatus").value(InboundPartnerStatusDto.CREATED.name()));

        testFactory.confirmInbound("in-1");
        assertThat(inboundRepository.findByExternalId("in-1").orElseThrow())
                .extracting(Inbound::isConfirmed).isEqualTo(true);

        caller.getInbounds(forToday())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].inboundExternalId").value("in-1"))
                .andExpect(jsonPath("$.content[0].actions").isArray())
                .andExpect(jsonPath("$.content[0].actions").isEmpty())
                .andExpect(jsonPath("$.content[0].complexActions").value(ComplexInboundAction.PUT_INBOUND_INFO.name()))
                .andExpect(jsonPath("$.content[0].inboundStatus").value(InboundPartnerStatusDto.CONFIRMED.name()));

        // Оператор на РЦ вносит данные водителя
        caller.inboundCarArrived("in-1", PUT_CAR_INFO_REQUEST).andExpect(status().isOk());

        assertThat(inboundRepository.findByExternalId("in-1").orElseThrow())
                .extracting(Inbound::getInboundStatus).isEqualTo(InboundStatus.CAR_ARRIVED);

        caller.getInbounds(forToday())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].inboundExternalId").value("in-1"))
                .andExpect(jsonPath("$.content[0].actions").isArray())
                .andExpect(jsonPath("$.content[0].actions").value(InboundAvailableAction.READY_TO_RECEIVE.name()))
                .andExpect(jsonPath("$.content[0].complexActions").value(ComplexInboundAction.GET_INBOUND_INFO.name()))
                .andExpect(jsonPath("$.content[0].inboundStatus").value(InboundPartnerStatusDto.CAR_ARRIVED.name()));


        // Оператор на РЦ (XDOC сорт центр) проверяет документы водителя и проставляет отметку, что поставку можно
        // принимать
        caller.performAction("in-1", InboundAvailableAction.READY_TO_RECEIVE)
                .andExpect(status().isOk())
                .andExpect(content().json(String.format("""
                        {
                          "id": %d,
                          "externalId": "in-1",
                          "inboundStatus": "READY_TO_RECEIVE",
                          "palletAmount": 0,
                          "boxAmount": 0,
                          "arrivalIntervalStart": "2020-04-16T13:36:34",
                          "arrivalIntervalEnd": "2020-04-16T13:36:34",
                          "supplierName": "ООО Сапплаер",
                          "courierName": "name",
                          "actions": [],
                          "complexActions": [
                            "GET_INBOUND_INFO"
                          ],
                          "carInfo": {
                            "fullName": "name",
                            "phoneNumber": "+79998882211",
                            "carNumber": "XXX",
                            "carBrand": "volvo",
                            "trailerNumber": "YYY",
                            "comment": "no comments"
                          },
                          "discrepancyActStatus": "NOT_AVAILABLE",
                          "inboundExternalId": "in-1"
                        }""", inbound.getId()), true
                ));

        assertThat(inboundRepository.findByExternalId("in-1").orElseThrow())
                .extracting(Inbound::getInboundStatus).isEqualTo(InboundStatus.READY_TO_RECEIVE);

        caller.getInbounds(forToday())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].inboundExternalId").value("in-1"))
                .andExpect(jsonPath("$.content[0].actions").isArray())
                .andExpect(jsonPath("$.content[0].actions").isEmpty())
                .andExpect(
                        jsonPath("$.content[0].complexActions").value(ComplexInboundAction.GET_INBOUND_INFO.name()))
                .andExpect(
                        jsonPath("$.content[0].inboundStatus").value(InboundPartnerStatusDto.READY_TO_RECEIVE.name()));

        // приняты первые палеты
        flow.toArrival("in-1")
                .linkPallets("XDOC-1");

        caller.getInbounds(forToday())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].inboundExternalId").value("in-1"))
                .andExpect(jsonPath("$.content[0].actions").isArray())
                .andExpect(jsonPath("$.content[0].actions").isEmpty())
                .andExpect(jsonPath("$.content[0].complexActions").value(ComplexInboundAction.GET_INBOUND_INFO.name()))
                .andExpect(jsonPath("$.content[0].inboundStatus").value(InboundPartnerStatusDto.NEED_SORTING.name()));

        // поставка была полностью отсортирована в ячейку
        sortableTestFactory.sortByBarcode("XDOC-1", cell.getId());

        caller.getInbounds(forToday())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].inboundExternalId").value("in-1"))
                .andExpect(jsonPath("$.content[0].actions").isArray())
                .andExpect(jsonPath("$.content[0].actions").value(InboundAvailableAction.FIX_INBOUND.name()))
                .andExpect(jsonPath("$.content[0].complexActions").value(ComplexInboundAction.GET_INBOUND_INFO.name()))
                .andExpect(
                        jsonPath("$.content[0].inboundStatus").value(InboundPartnerStatusDto.READY_TO_BE_FIXED.name()));

        // работник склада фиксировал поставку
        caller.performAction("in-1", InboundAvailableAction.FIX_INBOUND)
                .andExpect(status().isOk())
                .andExpect(content().json(String.format("""
                        {
                          "id": %d,
                          "externalId": "in-1",
                          "inboundStatus": "FIXED",
                          "palletAmount": 1,
                          "boxAmount": 0,
                          "startedAt": "2020-04-16T13:36:34",
                          "finishedAt": "2020-04-16T13:36:34",
                          "arrivalIntervalStart": "2020-04-16T13:36:34",
                          "arrivalIntervalEnd": "2020-04-16T13:36:34",
                          "supplierName": "ООО Сапплаер",
                          "courierName": "name",
                          "actions": [],
                          "complexActions": [
                            "GET_INBOUND_INFO"
                          ],
                          "carInfo": {
                            "fullName": "name",
                            "phoneNumber": "+79998882211",
                            "carNumber": "XXX",
                            "carBrand": "volvo",
                            "trailerNumber": "YYY",
                            "comment": "no comments"
                          },
                          "discrepancyActStatus": "NOT_AVAILABLE",
                          "inboundExternalId": "in-1"
                        }""", inbound.getId()), true
                ));

        caller.getInbounds(forToday())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].inboundExternalId").value("in-1"))
                .andExpect(jsonPath("$.content[0].actions").isArray())
                .andExpect(jsonPath("$.content[0].actions").isEmpty())
                .andExpect(jsonPath("$.content[0].complexActions").value(ComplexInboundAction.GET_INBOUND_INFO.name()))
                .andExpect(jsonPath("$.content[0].inboundStatus").value(InboundPartnerStatusDto.FIXED.name()));

        // Поставка была отгружена
        flow.createOutbound("out-1")
                .addRegistryPallets("XDOC-1")
                .buildRegistry()
                .sortToAvailableCell("XDOC-1")
                .prepareToShip("XDOC-1")
                .shipAndGet("out-1");

        caller.getInbounds(forToday())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].inboundExternalId").value("in-1"))
                .andExpect(jsonPath("$.content[0].actions").isArray())
                .andExpect(jsonPath("$.content[0].actions").isEmpty())
                .andExpect(jsonPath("$.content[0].complexActions").value(ComplexInboundAction.GET_INBOUND_INFO.name()))
                .andExpect(jsonPath("$.content[0].inboundStatus").value(InboundPartnerStatusDto.SHIPPED.name()));
    }

    @ParameterizedTest(name = "Получение кнопки action для дропшип поставки")
    @MethodSource("getDropShipInboundActions")
    void getInboundActionForDropShipInbound(InboundStatus status, InboundAvailableAction expectedAction) {
        var inbound = createdInbound("in-1", InboundType.DS_SC);
        var registry = testFactory.bindRegistry(inbound, "registryId-2", PLANNED);
        testFactory.bindInboundOrder(inbound, registry, "placeId-1", "palletId-1");

        inbound.setInboundStatus(status);
        inboundRepository.save(inbound);

        var resultAction = caller.getInbounds(forToday())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].inboundExternalId").value("in-1"));

        if (expectedAction == null) {
            resultAction
                    .andExpect(jsonPath("$.content[0].actions").isArray())
                    .andExpect(jsonPath("$.content[0].actions").isEmpty());
        } else {
            resultAction.andExpect(jsonPath("$.content[0].actions").value(expectedAction.name()));
        }
    }

    private static List<Arguments> getDropShipInboundActions() {
        var actionByStatus = new ArrayList<Arguments>();
        for (InboundStatus status : InboundStatus.values()) {
            InboundAvailableAction action = switch (status) {
                case CREATED, CONFIRMED, CAR_ARRIVED, READY_TO_RECEIVE, ARRIVED, IN_PROGRESS,
                        INITIAL_ACCEPTANCE_COMPLETED -> InboundAvailableAction.FIX_INBOUND;
                case FIXED ->  InboundAvailableAction.PRINT_TRANSFER_ACT;
                case CANCELLED, CANCELLED_BY_OPERATOR, UNKNOWN -> null;
            };
            actionByStatus.add(Arguments.of(status, action));
        }
        return actionByStatus;
    }


    @Test
    @DisplayName("""
            Нажатие кнопки READY_TO_RECEIVE в ПИ на поставке XDOC в статусе CREATE
            переведет её в статус READY_TO_RECEIVE
             c использованием ручки {0}
            """)
    void xdocInboundReadyToReceive() {
        var inbound = flow.createInboundAndGet("in-1");

        caller.inboundCarArrived("in-1", PUT_CAR_INFO_REQUEST).andExpect(status().isOk());

        caller.performAction(inbound.getExternalId(), InboundAvailableAction.READY_TO_RECEIVE)
                .andExpect(status().isOk());

        var inboundAfter = inboundRepository.findByExternalId(inbound.getExternalId()).orElseThrow();
        assertThat(inboundAfter.getInboundStatus())
                .isEqualTo(InboundStatus.READY_TO_RECEIVE);
    }

    @Test
    @DisplayName("""
            Попытка перевести поставку в READY_TO_RECEIVE статус,
            когда она уже READY_TO_RECEIVE, ничего не произойдет
             c использованием ручки {0}
            """)
    void attemptToReadyToReceiveInboundThatIsAlreadyReadyToReceive() {
        var inbound = flow.createInboundAndGet("in-1");

        caller.inboundCarArrived("in-1", PUT_CAR_INFO_REQUEST).andExpect(status().isOk());

        caller.performAction(inbound.getExternalId(), InboundAvailableAction.READY_TO_RECEIVE)
                .andExpect(status().isOk());

        caller.performAction(inbound.getExternalId(), InboundAvailableAction.READY_TO_RECEIVE)
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("После обновления информации о водителе, будет возвращаем обновленный InboundPartnerDto")
    void putCarInfoReturnsUpdatedPartnerInboundDto() {
        var inbound = flow.createInboundAndGet("in-1");

        caller.inboundCarArrived("in-1", PUT_CAR_INFO_REQUEST)
                .andExpect(status().isOk())
                .andExpect(content().json(String.format("""
                        {
                          "id": %d,
                          "externalId": "in-1",
                          "inboundStatus": "CAR_ARRIVED",
                          "palletAmount": 0,
                          "boxAmount": 0,
                          "arrivalIntervalStart": "2020-04-16T13:36:34",
                          "arrivalIntervalEnd": "2020-04-16T13:36:34",
                          "supplierName": "ООО Сапплаер",
                          "courierName": "name",
                          "actions": [
                            "READY_TO_RECEIVE"
                          ],
                          "complexActions": [
                            "GET_INBOUND_INFO"
                          ],
                          "carInfo": {
                            "fullName": "name",
                            "phoneNumber": "+79998882211",
                            "carNumber": "XXX",
                            "carBrand": "volvo",
                            "trailerNumber": "YYY",
                            "comment": "no comments"
                          },
                          "inboundExternalId": "in-1",
                          "discrepancyActStatus": "NOT_AVAILABLE"
                        }""", inbound.getId()), true));
    }

    @Test
    @DisplayName("Проверка статуса скачивания Акта о расхождениях для поставок СЦ-СЦ")
    void scToScDiscrepancyActStatusTest() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.ENABLE_DISCREPANCY_ACT_GENERATING_BY_REGISTRY_MANAGER, true);
        var sortingCenterFrom = testFactory.storedSortingCenter(
                TestFactory.SortingCenterParams.builder()
                        .id(12413491)
                        .yandexId("5378264623")
                        .token("sc_from_token")
                        .partnerName("sc_from_partner_name")
                        .build()
        );
        testFactory.setSortingCenterProperty(sortingCenterFrom,
                SortingCenterPropertiesKey.ENABLE_DISCREPANCY_ACT_GENERATING_BY_REGISTRY_MANAGER, true);
        var scToScInbound = createdInbound("inbound-1", InboundType.DS_SC, sortingCenterFrom.getYandexId(),
                Map.of("reg", List.of(Pair.of("o1", "p-1"))), null, null);

        caller.getInbounds(forToday())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].discrepancyActStatus")
                        .value(DiscrepancyActStatus.NOT_AVAILABLE.name()));

        caller.performAction(scToScInbound.getExternalId(), InboundAvailableAction.FIX_INBOUND)
                .andExpect(status().isOk());

        caller.getInbounds(forToday())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].discrepancyActStatus")
                        .value(DiscrepancyActStatus.GENERATING.name()));

        inboundCommandService.setDiscrepancyAct(
                new InboundDiscrepancyActDto(
                        scToScInbound.getTransportationId(),
                        "bucket",
                        "filename"
                )
        );

        caller.getInbounds(forToday())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].discrepancyActStatus")
                        .value(DiscrepancyActStatus.READY_TO_DOWNLOAD.name()));
    }

    @Test
    @DisplayName("Групповая операция - записи об изменении истории статусов будут записаны для всех поставок")
    void groupOperationHistoryRecords() {
        var inbound1 = flow.inboundBuilder("666661").realSupplierName("ООО Березка").build().getInbound();
        var inbound2 = flow.inboundBuilder("666662").realSupplierName("ООО Березка").build().getInbound();

        var externalIds = List.of(inbound1.getExternalId(), inbound2.getExternalId());

        var historyRecordsBefore = inboundStatusHistoryRepository.findAllByInboundExternalIdIn(externalIds);

        var request = new GroupPutCarInfoRequest();
        request.setExternalIds(externalIds);
        request.setCarInfo(PUT_CAR_INFO_REQUEST);

        caller.inboundsCarArrived(request).andExpect(status().isOk());

        var historyRecordsAfter = inboundStatusHistoryRepository.findAllByInboundExternalIdIn(externalIds);
        historyRecordsAfter.removeAll(historyRecordsBefore);

        assertThat(historyRecordsAfter)
                .hasSize(2)
                .map(InboundStatusHistory::getInboundExternalId)
                .containsExactlyInAnyOrderElementsOf(externalIds);
    }

    @Test
    @DisplayName("Групповая операция заполнения данных о водителе")
    void groupOperationInboundCarArrivedSuccess() {
        var inbound1 = flow.inboundBuilder("666661").realSupplierName("ООО Березка").build().getInbound();
        var inbound2 = flow.inboundBuilder("666662").realSupplierName("ООО Березка").build().getInbound();

        var request = new GroupPutCarInfoRequest();
        request.setExternalIds(List.of(inbound1.getExternalId(), inbound2.getExternalId()));
        request.setCarInfo(PUT_CAR_INFO_REQUEST);

        caller.inboundsCarArrived(request).andExpect(status().isOk());

        var inboundInfo1 = inboundInfoRepository.findById(inbound1.getId()).orElseThrow();
        var inboundInfo2 = inboundInfoRepository.findById(inbound2.getId()).orElseThrow();

        assertThat(inboundInfo1.getGroupId())
                .isNotNull()
                .isEqualTo(inboundInfo2.getGroupId());

        assertThat(inboundInfo1.getCarInfo())
                .isNotNull()
                .isEqualTo(inboundInfo2.getCarInfo());
    }

    @Test
    @DisplayName("Групповая операция возвращает обновленный список dto")
    void groupOperationReturnsUpdatedPartnerDtos() {
        var inbound1 = flow.inboundBuilder("666661").realSupplierName("ООО Березка").build().getInbound();
        var inbound2 = flow.inboundBuilder("666662").realSupplierName("ООО Березка").build().getInbound();

        var request = new GroupPutCarInfoRequest();
        request.setExternalIds(List.of(inbound1.getExternalId(), inbound2.getExternalId()));
        request.setCarInfo(PUT_CAR_INFO_REQUEST);

        caller.inboundsCarArrived(request)
            .andExpect(status().isOk())
            .andExpect(content().json("""
                {
                  "inbounds": [
                    {
                      "externalId": "666661",
                      "inboundStatus": "CAR_ARRIVED",
                      "palletAmount": 0,
                      "boxAmount": 0,
                      "arrivalIntervalStart": "2020-04-16T13:36:34",
                      "arrivalIntervalEnd": "2020-04-16T13:36:34",
                      "supplierName": "ООО Березка",
                      "courierName": "name",
                      "actions": [
                        "READY_TO_RECEIVE"
                      ],
                      "complexActions": [
                        "GET_INBOUND_INFO"
                      ],
                      "carInfo": {
                        "fullName": "name",
                        "phoneNumber": "+79998882211",
                        "carNumber": "XXX",
                        "carBrand": "volvo",
                        "trailerNumber": "YYY",
                        "comment": "no comments"
                      },
                      "inboundExternalId": "666661"
                    },
                    {
                      "externalId": "666662",
                      "inboundStatus": "CAR_ARRIVED",
                      "palletAmount": 0,
                      "boxAmount": 0,
                      "arrivalIntervalStart": "2020-04-16T13:36:34",
                      "arrivalIntervalEnd": "2020-04-16T13:36:34",
                      "supplierName": "ООО Березка",
                      "courierName": "name",
                      "actions": [
                        "READY_TO_RECEIVE"
                      ],
                      "complexActions": [
                        "GET_INBOUND_INFO"
                      ],
                      "carInfo": {
                        "fullName": "name",
                        "phoneNumber": "+79998882211",
                        "carNumber": "XXX",
                        "carBrand": "volvo",
                        "trailerNumber": "YYY",
                        "comment": "no comments"
                      },
                      "inboundExternalId": "666662"
                    }
                  ]
                }
                """));
    }

    @Test
    @DisplayName("Групповая операция заполнения данных о водителе - попытка заполнить группу для разных поставщиков")
    void groupOperationInboundCarArrivedFailDifferentInboundSuppliers() {
        flow.inboundBuilder("666661").realSupplierName("ООО Березка").build();
        flow.inboundBuilder("666662").realSupplierName("ООО Березка").build();
        flow.inboundBuilder("666663").realSupplierName("ООО Сто пудов").build();
        flow.inboundBuilder("666664").realSupplierName(null).build();

        var request = new GroupPutCarInfoRequest();
        request.setExternalIds(List.of("666661", "666662", "666663", "666664"));
        request.setCarInfo(PUT_CAR_INFO_REQUEST);

        caller.inboundsCarArrived(request)
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"message\": \"Inbounds belong to different suppliers\"}"));
    }

    @Test
    @DisplayName("Групповая операция заполнения данных о водителе - есть поставка в невалидном статусе")
    void groupOperationInboundCarArrivedFailInavlidStatus() {
        flow.inboundBuilder("666661").realSupplierName("ООО Березка").build();

        flow.inboundBuilder("666662")
                .realSupplierName("ООО Березка")
                .build()
                .linkPallets("XDOC-1")
                .fixInbound();

        var request = new GroupPutCarInfoRequest();
        request.setExternalIds(List.of("666661", "666662"));
        request.setCarInfo(PUT_CAR_INFO_REQUEST);

        caller.inboundsCarArrived(request)
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"message\": \"Inbounds 666662 XDOC_TRANSIT FIXED can not accept car\"}"));
    }

    @Test
    @DisplayName("Обновление информации о водителе со страницы подробной информации о поставке")
    void updateCarInfoAfterCarArrived() {
        var inbound = flow.createInboundAndGet("in-1");

        caller.inboundCarArrived("in-1", PUT_CAR_INFO_REQUEST).andExpect(status().isOk());

        caller.putCarInfo("in-1", PutCarInfoRequest.builder()
                        .fullName("name-2")
                        .phoneNumber("+79998882299")
                        .carNumber("XXX2")
                        .carBrand("volvo2")
                        .trailerNumber("YYY2")
                        .comment("yes comments")
                        .build())
                .andExpect(status().isOk())
                .andExpect(content().json(String.format("""
                        {
                          "id": %d,
                          "externalId": "in-1",
                          "inboundStatus": "CAR_ARRIVED",
                          "palletAmount": 0,
                          "boxAmount": 0,
                          "arrivalIntervalStart": "2020-04-16T13:36:34",
                          "arrivalIntervalEnd": "2020-04-16T13:36:34",
                          "supplierName": "ООО Сапплаер",
                          "courierName": "name",
                          "actions": [
                            "READY_TO_RECEIVE"
                          ],
                          "complexActions": [
                            "GET_INBOUND_INFO"
                          ],
                          "carInfo": {
                            "fullName": "name-2",
                            "phoneNumber": "+79998882299",
                            "carNumber": "XXX2",
                            "carBrand": "volvo2",
                            "trailerNumber": "YYY2",
                            "comment": "yes comments"
                          },
                          "inboundExternalId": "in-1",
                          "discrepancyActStatus": "NOT_AVAILABLE"
                        }""", inbound.getId()), true));
    }

    @Test
    @DisplayName("""
            Попытка перевести НЕ XDOC поставку в статус READY_TO_RECEIVE завершится ошибкой
             c использованием ручки {0}
            """)
    void attemptToReadyToReceiveNonXDocInbound() {
        var inbound = createdInbound("in-1", InboundType.DS_SC);

        caller.performAction(inbound.getExternalId(), InboundAvailableAction.READY_TO_RECEIVE)
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"message\": \"Inbound of type DS_SC can not be changed to " +
                        "READY_TO_RECEIVE status\"}"));
    }

    @Test
    @DisplayName("""
            При попытке перевести XDOC поставку в статус READY_TO_RECEIVE если она не подтверждена мерчом и
             складом, произойдет ошибка
             c использованием ручки {0}
            """)
    void attemptToReadyToReceiveXDocInboundThatIsNotConfirmedByWarehouseAndMerch() {
        var inbound = flow.inboundBuilder("in-1")
                .confirm(false)
                .createAndGet();

        caller.performAction(inbound.getExternalId(), InboundAvailableAction.READY_TO_RECEIVE)
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"error\": \"" + INBOUND_NOT_CONFIRMED.name() + "\"}"));
    }

    @DisplayName("InboundInfo для xdoc поставки создается одновременно с созданием поставки")
    @Test
    void inboundInfoCreation() {
        var inbound = flow.createInbound("in-1")
                .getInbound();

        var inboundInfo = Optional.ofNullable(inbound.getInboundInfo());

        assertThat(inboundInfo)
                .isNotEmpty()
                .get()
                .extracting(InboundInfo::getInbound)
                .isEqualTo(inbound);
    }

    @DisplayName("InboundInfo для xdoc поставки создание и получение")
    @Test
    void inboundInfo() {
        Warehouse specificWarehouse = testFactory.storedWarehouse("samara-wh");

        var inbound = flow.inboundBuilder("in-1")
                .nextLogisticPoint(specificWarehouse.getYandexId())
                .informationListBarcode("000001317")
                .build()
                .getInbound();

        var request = PutCarInfoRequest.builder()
                .fullName("fio")
                .phoneNumber("+79998882233")
                .carNumber("XXXX")
                .carBrand("volvo")
                .trailerNumber("YYYY")
                .comment("no comments")
                .build();
        caller.inboundCarArrived(inbound.getInformationListCode(), request)
                .andExpect(status().isOk());

        caller.putDocInfo(
                inbound.getExternalId(),
                """
                        {
                            "docNumber": "number-1",
                            "docDate": "2000-10-31",
                            "inboundDate": "2000-11-01",
                            "price": 100.0,
                            "tax": 20.0,
                            "untaxedPrice": 80.0
                          }
                        """
        ).andExpect(status().isOk());

        flow.toArrival(inbound.getExternalId())
                .linkPallets("XDOC-1", "XDOC-2");

        mockStatusHistoryBy(inbound, Instant.ofEpochSecond(1500000000), InboundStatus.CONFIRMED);
        mockStatusHistoryBy(inbound, Instant.ofEpochSecond(1500001000), InboundStatus.CAR_ARRIVED);
        mockStatusHistoryBy(inbound, Instant.ofEpochSecond(1500002000), InboundStatus.ARRIVED);

        caller.getInboundInfo(inbound.getInformationListCode())
                .andExpect(status().isOk())
                .andExpect(content().json(String.format("""
                        {
                          "id": %s,
                          "externalId": "in-1",
                          "informationListCode": "000001317",
                          "nextWarehouse": "ООО Ромашка-Склад",
                          "inboundOwner": "ООО Сапплаер",
                          "status":"NEED_SORTING",
                          "carInfo": {
                            "fullName": "fio",
                            "phoneNumber": "+79998882233",
                            "carNumber": "XXXX",
                            "carBrand": "volvo",
                            "trailerNumber": "YYYY",
                            "comment": "no comments"
                          },
                          "docInfo":{
                            "docNumber": "number-1",
                            "docDate": "2000-10-31",
                            "inboundDate": "2000-11-01",
                            "price": 100.0,
                            "tax": 20.0,
                            "untaxedPrice": 80.0
                          },
                          "units": [
                            {
                              "barcode": "XDOC-1",
                              "type": "XDOC_PALLET",
                              "measurements": {}
                            },
                            {
                              "barcode": "XDOC-2",
                              "type": "XDOC_PALLET",
                              "measurements": {}
                            }
                          ],
                          "history": [
                            {
                              "status": "CONFIRMED",
                              "dateTime": "2017-07-14T05:40:00"
                            },
                            {
                              "status": "CAR_ARRIVED",
                              "dateTime": "2017-07-14T05:56:40"
                            },
                            {
                              "status": "ARRIVED",
                              "dateTime": "2017-07-14T06:13:20"
                            }
                          ]
                        }""", inbound.getId()), true));
    }

    @SneakyThrows
    @Test
    void getInboundInfoWithMeasurements() {
        UnitMeasurementsDto unitMeasurementsDto1 = new UnitMeasurementsDto(
                new BigDecimal(120),
                new BigDecimal(150),
                new BigDecimal(80),
                new BigDecimal(200)
        );

        UnitMeasurementsDto unitMeasurementsDto2 = new UnitMeasurementsDto(
                new BigDecimal(120),
                new BigDecimal(60),
                new BigDecimal(80),
                new BigDecimal(100)
        );

        flow.createInbound("in-1")
                .linkPallets("XDOC-1")
                .saveVgh(SaveVGHRequestDto.builder()
                        .sortableType(SortableType.XDOC_PALLET)
                        .vgh(unitMeasurementsDto1)
                        .build(),
                        "XDOC-1")
                .linkPallets("XDOC-2")
                .saveVgh(SaveVGHRequestDto.builder()
                                .sortableType(SortableType.XDOC_PALLET)
                                .vgh(unitMeasurementsDto2)
                                .build(),
                        "XDOC-2")
                .fixInbound();

        caller.getInboundInfo("in-1")
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                            "units":[
                                {
                                    "barcode":"XDOC-1",
                                    "type":"XDOC_PALLET",
                                    "measurements":{
                                        "width":120,
                                        "height":150,
                                        "length":80,
                                        "weight":200
                                    }
                                },
                                {
                                    "barcode":"XDOC-2",
                                    "type":"XDOC_PALLET",
                                    "measurements":{
                                        "width":120,
                                        "height":60,
                                        "length":80,
                                        "weight":100
                                    }
                                }
                            ]
                        }
                        """));
    }

    @ParameterizedTest(name = "Валидный ввод документов о поставке - {0}")
    @MethodSource("validDocumentInput")
    void validDocumentInfo(String desc, String request) {
        Warehouse specificWarehouse = testFactory.storedWarehouse("samara-wh");

        var inbound = flow.inboundBuilder("in-1")
                .nextLogisticPoint(specificWarehouse.getYandexId())
                .informationListBarcode("000001317")
                .build()
                .getInbound();

        caller.putDocInfo(inbound.getExternalId(), request)
                .andExpect(status().isOk());
    }

    @ParameterizedTest(name = "Не валидный ввод документов о поставке - {0}")
    @MethodSource("invalidDocumentInput")
    void invalidDocumentInfo(String errMsg, String request) {
        Warehouse specificWarehouse = testFactory.storedWarehouse("samara-wh");

        var inbound = flow.inboundBuilder("in-1")
                .nextLogisticPoint(specificWarehouse.getYandexId())
                .informationListBarcode("000001317")
                .build()
                .getInbound();

        caller.putDocInfo(inbound.getExternalId(), request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(errMsg));
    }

    private static List<Arguments> validDocumentInput() {
        return List.of(
                Arguments.of("Все поля заполнены", """
                        {
                            "docNumber": "number-1",
                            "docDate": "2000-10-31",
                            "inboundDate": "2000-11-01",
                            "price": 100.0,
                            "tax": 20.0,
                            "untaxedPrice": 80.0
                        }
                        """),
                Arguments.of("Все поля заполнены", """
                        {
                            "docNumber": "number-1",
                            "docDate": "2000-10-31",
                            "inboundDate": "2000-10-31",
                            "price": 100.0,
                            "tax": 20.0,
                            "untaxedPrice": 80.0
                        }
                        """),
                Arguments.of("Заполнена дата прибытия на РЦ", """
                        {
                            "inboundDate": "2000-11-01"
                        }
                        """)
        );
    }

    private static List<Arguments> invalidDocumentInput() {
        return List.of(
                Arguments.of(
                        "Сумма с НДС должна быть больше либо равно нулю",
                        """
                                {
                                    "docNumber": "number-1",
                                    "docDate": "2000-10-30",
                                    "inboundDate": "2000-10-31",
                                    "price": -1,
                                    "tax": 20.0,
                                    "untaxedPrice": 80.0
                                }
                                """),
                Arguments.of(
                        "Фактическая дата поставки не может быть меньше даты по документа о поставке",
                        """
                                {
                                    "docNumber": "number-1",
                                    "docDate": "2000-10-31",
                                    "inboundDate": "2000-10-30",
                                    "price": 100.0,
                                    "tax": 20.0,
                                    "untaxedPrice": 80.0
                                }
                                """),
                Arguments.of(
                        "Должны быть заполнены либо все поля, либо ровно одно поле c датой фактического прибытия на РЦ",
                        """
                                {
                                    "docNumber": "number-1",
                                    "inboundDate": "2000-11-01"
                                }
                                """),
                Arguments.of("Должны быть заполнены либо все поля, либо ровно одно поле c датой фактического прибытия" +
                             " на РЦ", "{}")
        );
    }

    @Test
    @DisplayName("для xdoc есть поле externalId(заполняется externalId) и inboundExternalId(заполняется заполняется " +
            "informationListCode) поставки")
    void xDocInboundInformationListCodeAndExternalId() {
        flow.inboundBuilder("in-2")
                .informationListBarcode("000001318")
                .build();

        caller.getInbounds(forToday())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].externalId").value("in-2"))
                .andExpect(jsonPath("$.content[0].inboundExternalId").value("000001318"));
    }

    @Test
    @SneakyThrows
    @DisplayName("Информация о водителе и автомобиле выводится в в списке поставок")
    void carInfoInInboundList() {
        Warehouse specificWarehouse = testFactory.storedWarehouse("samara-wh");

        var inbound = flow.inboundBuilder("in-1")
                .nextLogisticPoint(specificWarehouse.getYandexId())
                .informationListBarcode("000001317")
                .build()
                .getInbound();

        var request = PutCarInfoRequest.builder()
                .fullName("fio")
                .phoneNumber("+79998882233")
                .carNumber("XXXX")
                .carBrand("volvo")
                .trailerNumber("YYYY")
                .comment("no comments")
                .build();
        caller.inboundCarArrived(inbound.getInformationListCode(), request)
                .andExpect(status().isOk());

        mockStatusHistoryBy(inbound);
        String expected = ScTestUtils.fileContent("internal/partner/inbounds/responses/xDocWithCarInfo.json");
        getInboundAndAssertResponse(expected);
    }

    @Test
    @DisplayName("создание inboundInfo из кнопки carArrived")
    void getInboundInfoAfterPutInboundInfo() {
        var inbound = flow.inboundBuilder("in-1")
                .confirm(false)
                .createAndGet();

        caller.getInboundInfo(
                inbound.getExternalId()
        )
                .andExpect(status().isOk());

        caller.inboundCarArrived(inbound.getExternalId(), PUT_CAR_INFO_REQUEST).andExpect(status().isOk());

        InboundInfo inboundInfo = inbound.getInboundInfo();

        caller.getInboundInfo(
                inbound.getExternalId()
        )
                .andExpect(status().isOk())
                .andExpect(content().json(expectedInboundInfo(Objects.requireNonNull(inboundInfo))));
    }

    private static String expectedInboundInfo(InboundInfo inboundInfo) {

        return String.format("""
                {
                   "carInfo": %s
                }
                """, expectedCarInfo(inboundInfo.getCarInfo()));
    }

    private static String expectedCarInfo(@Nullable CarInfo carInfo) {
        if (carInfo == null) {
            return "{}";
        }
        return String.format("""
                        {
                          "fullName": "%s",
                          "phoneNumber": "%s",
                          "carNumber": "%s",
                          "carBrand": "%s",
                          "trailerNumber": "%s",
                          "comment": "%s"
                        }
                        """, carInfo.getFullName(), carInfo.getPhoneNumber(),
                carInfo.getCarNumber(), carInfo.getCarBrand(), carInfo.getTrailerNumber(),
                carInfo.getComment());
    }

    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @Nested
    class FixOnlyFullySortedInbounds {

        Warehouse samaraWH;
        Cell samaraCell;

        @BeforeEach
        void init() {
            testFactory.setSortingCenterProperty(
                    sortingCenter,
                    SortingCenterPropertiesKey.ENABLE_FIX_ONLY_FULLY_SORTED_XDOC_INBOUNDS,
                    true
            );
            samaraWH = testFactory.storedWarehouse("101010987");
            samaraCell = testFactory.storedCell(
                    sortingCenter,
                    "samara-keep",
                    CellType.BUFFER,
                    CellSubType.BUFFER_XDOC,
                    samaraWH.getYandexId());
        }

        @Test
        @DisplayName("Все палеты в поставке отсортированы в ячейку с использованием ручки {0}")
        void allPalletsSortedToCell() {
            flow.inboundBuilder("in-1")
                    .nextLogisticPoint(samaraWH.getYandexId())
                    .build()
                    .linkPallets("XDOC-1", "XDOC-2");

            sortableTestFactory.sortByBarcode("XDOC-1", samaraCell.getId());
            sortableTestFactory.sortByBarcode("XDOC-2", samaraCell.getId());

            caller.performAction("in-1", InboundAvailableAction.FIX_INBOUND)
                    .andExpect(status().isOk());
        }

        @DisplayName("Поставка содержит не отсортированные в ячейку палеты")
        @Test
        void hasNotSortedPallets() {
            flow.inboundBuilder("in-1")
                    .nextLogisticPoint(samaraWH.getYandexId())
                    .build()
                    .linkPallets("XDOC-1", "XDOC-2")
                    .apiFixInbound(res -> res.andExpect(status().isBadRequest())
                            .andExpect(content().json("{\"error\": \"CANT_FIX_NOT_FULLY_SORTED_INBOUND\"}")));
        }

        @Test
        @DisplayName("Все коробки в поставке отсортированы в лот с использованием ручки {0}")
        void allBoxesSortedToLot() {
            flow.inboundBuilder("in-1")
                    .nextLogisticPoint(samaraWH.getYandexId())
                    .build()
                    .linkBoxes("XDOC-1", "XDOC-2");

            var lotDto = caller.createLot(
                    new PartnerLotRequestDto(samaraCell.getId(), 1)
            ).andExpect(status().isOk())
                    .getResponseAsClass(PartnerLotDtoWrapper.class);

            SortableLot basket = sortableLotService.findByLotIdOrThrow(lotDto.getLots().get(0).getId());


            sortableTestFactory.lotSort("XDOC-1", basket.getBarcode(), user);
            sortableTestFactory.lotSort("XDOC-2", basket.getBarcode(), user);

            caller.performAction("in-1", InboundAvailableAction.FIX_INBOUND)
                    .andExpect(status().isOk());
        }

        @DisplayName("Поставка содержит не отсортированные в лот коробки")
        @Test
        void hasNotSortedBoxes() {
            flow.inboundBuilder("in-1")
                    .nextLogisticPoint(samaraWH.getYandexId())
                    .build()
                    .linkPallets("XDOC-1", "XDOC-2")
                    .apiFixInbound(res -> res.andExpect(status().isBadRequest())
                            .andExpect(content().json("{\"error\": \"CANT_FIX_NOT_FULLY_SORTED_INBOUND\"}")));
        }
    }

    @DisplayName("Список статусов inbound сорт центра для отображения на фронте")
    @Test
    void inboundStatusesListForSortingCenter() {
        testFactory.setSortingCenterProperty(TestFactory.SC_ID, SortingCenterPropertiesKey.XDOC_ENABLED, false);
        String expected = StreamEx.of(
                InboundPartnerStatusDto.CREATED,
                InboundPartnerStatusDto.ARRIVED,
                InboundPartnerStatusDto.IN_PROGRESS,
                InboundPartnerStatusDto.FIXED,
                InboundPartnerStatusDto.CANCELLED
        ).map(value -> "\"" + value.name() + "\"")
                .collect(Collectors.joining(","));

        caller.getStatuses()
                .andExpect(status().isOk())
                .andExpect(content().json("[" + expected + "]", true));
    }

    @DisplayName("Список статусов inbound XDOC сорт центра для отображения на фронте")
    @Test
    void inboundStatusesListForXDocSortingCenter() {
        testFactory.setSortingCenterProperty(TestFactory.SC_ID, SortingCenterPropertiesKey.XDOC_ENABLED, true);
        String expected = StreamEx.of(
                InboundPartnerStatusDto.CREATED,
                InboundPartnerStatusDto.CONFIRMED,
                InboundPartnerStatusDto.CAR_ARRIVED,
                InboundPartnerStatusDto.READY_TO_RECEIVE,
                InboundPartnerStatusDto.NEED_SORTING,
                InboundPartnerStatusDto.READY_TO_BE_FIXED,
                InboundPartnerStatusDto.INITIAL_ACCEPTANCE_COMPLETED,
                InboundPartnerStatusDto.FIXED,
                InboundPartnerStatusDto.SHIPPED,
                InboundPartnerStatusDto.CANCELLED,
                InboundPartnerStatusDto.CANCELLED_BY_OPERATOR
        ).map(value -> "\"" + value.name() + "\"")
                .collect(Collectors.joining(","));

        caller.getStatuses()
                .andExpect(status().isOk())
                .andExpect(content().json("[" + expected + "]", true));
    }


    @DisplayName("Список статусов inbound сорт центра для отображения на фронте")
    @Test
    void inboundStatusesV2ForSortingCenter() {
        testFactory.setSortingCenterProperty(TestFactory.SC_ID, SortingCenterPropertiesKey.XDOC_ENABLED, false);
        List<InboundPartnerStatusDto> expectedStatuses = StreamEx.of(
                        InboundPartnerStatusDto.CREATED,
                        InboundPartnerStatusDto.ARRIVED,
                        InboundPartnerStatusDto.IN_PROGRESS,
                        InboundPartnerStatusDto.FIXED,
                        InboundPartnerStatusDto.CANCELLED
                ).toList();
        List<InboundPartnerStatusDto> expectedDefault = StreamEx.of(
                        InboundPartnerStatusDto.ARRIVED,
                        InboundPartnerStatusDto.IN_PROGRESS,
                        InboundPartnerStatusDto.FIXED
                ).toList();

        caller.getStatusesWrapper()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statuses[*]", hasSize(expectedStatuses.size())))
                .andExpect(jsonPath("$.defaultStatuses[*]", hasSize(expectedDefault.size())));
    }

    @DisplayName("Список статусов inbound XDOC сорт центра для отображения на фронте")
    @Test
    void inboundStatusesV2ForXDocSortingCenter() {
        testFactory.setSortingCenterProperty(TestFactory.SC_ID, SortingCenterPropertiesKey.XDOC_ENABLED, true);
        List<InboundPartnerStatusDto> expectedStatuses = List.of(
                InboundPartnerStatusDto.CREATED,
                InboundPartnerStatusDto.CONFIRMED,
                InboundPartnerStatusDto.CAR_ARRIVED,
                InboundPartnerStatusDto.READY_TO_RECEIVE,
                InboundPartnerStatusDto.NEED_SORTING,
                InboundPartnerStatusDto.READY_TO_BE_FIXED,
                InboundPartnerStatusDto.INITIAL_ACCEPTANCE_COMPLETED,
                InboundPartnerStatusDto.FIXED,
                InboundPartnerStatusDto.SHIPPED,
                InboundPartnerStatusDto.CANCELLED,
                InboundPartnerStatusDto.CANCELLED_BY_OPERATOR
        );
        List<InboundPartnerStatusDto> expectedDefault = List.of(
                InboundPartnerStatusDto.CONFIRMED,
                InboundPartnerStatusDto.CAR_ARRIVED,
                InboundPartnerStatusDto.READY_TO_RECEIVE,
                InboundPartnerStatusDto.NEED_SORTING,
                InboundPartnerStatusDto.READY_TO_BE_FIXED,
                InboundPartnerStatusDto.INITIAL_ACCEPTANCE_COMPLETED,
                InboundPartnerStatusDto.FIXED,
                InboundPartnerStatusDto.SHIPPED
        );

        caller.getStatusesWrapper()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statuses[*]", hasSize(expectedStatuses.size())))
                .andExpect(jsonPath("$.defaultStatuses[*]", hasSize(expectedDefault.size())));
    }

    @DisplayName("Список статусов inbound XDOC сорт центра для отображения на фронте")
    @Test
    void inboundTypesV2() {
        List<InboundType> expectedTypes = List.of(
                InboundType.DS_SC,
                InboundType.XDOC_TRANSIT,
                InboundType.XDOC_ANOMALY
        );

        caller.getAllowedTypesWrapper()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.types[*]", hasSize(expectedTypes.size())))
                .andExpect(jsonPath("$.defaultTypes[*]", hasSize(0)));
    }

    @DisplayName("MARKETTPLSC-2214 агрегация по имени не должна происходить, вместо этого агрегация по id курьера, " +
            "фиксация поставок работает по отдельности (только для дропоффов)")
    @Test
    void aggregateByIdAndFix() {
        Warehouse w1 = testFactory.storedWarehouse();

        var courier1 = testFactory.storedMovementCourier(
                new MovementCourier("111", "IVAN", "OOO IVAN", null, null)
        );
        var courier2 = testFactory.storedMovementCourier(
                new MovementCourier("112", "IVAN", "OOO IVAN", null, null)
        );

        var inbound1 = createdInbound("5862281", InboundType.DS_SC, w1.getYandexId(),
                Map.of("reg1", List.of(Pair.of("ext1", "ext-1"))), courier1, null);
        var inbound2 = createdInbound("5862282", InboundType.DS_SC, w1.getYandexId(),
                Map.of("reg2", List.of(Pair.of("ext2", "ext-2"))), courier2, null);

        mockStatusHistoryBy(inbound1);
        mockStatusHistoryBy(inbound2);

        String expected = ScTestUtils.fileContent("internal/partner/inbounds/responses/dropShipAggregateById.json");
        caller.getInbounds(forToday())
                .andExpect(status().isOk())
                .andExpect(content().json(expected, false));

        // фиксация первой поставки, при этом вторая поставка не затронута
        caller.performAction(inbound1.getExternalId(), InboundAvailableAction.FIX_INBOUND)
                .andExpect(status().isOk());

        mockStatusHistoryBy(inbound1);

        expected = ScTestUtils.fileContent(
                "internal/partner/inbounds/responses/dropShipAggregateByIdAndFirstFixed.json");
        caller.getInbounds(forToday())
                .andExpect(status().isOk())
                .andExpect(content().json(expected, false));

        // фиксация второй поставки
        caller.performAction(inbound2.getExternalId(), InboundAvailableAction.FIX_INBOUND)
                .andExpect(status().isOk());

        caller.getInbounds(forToday())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(2)))
                .andExpect(jsonPath("$.content[0].action").doesNotExist())
                .andExpect(jsonPath("$.content[0].actions").isArray())
                .andExpect(jsonPath("$.content[0].actions")
                        .value(InboundAvailableAction.PRINT_TRANSFER_ACT.name()))
                .andExpect(jsonPath("$.content[0].inboundStatus").value(InboundPartnerStatusDto.FIXED.name()))
                .andExpect(jsonPath("$.content[1].action").doesNotExist())
                .andExpect(jsonPath("$.content[1].inboundStatus").value(InboundPartnerStatusDto.FIXED.name()));
    }

    @Test
    public void filterBySupplierName() {
        flow.inboundBuilder("in-1")
                .realSupplierName("test-supplier")
                .informationListBarcode("Зп-1")
                .build()
                .linkPallets("XDOC-1")
                .fixInbound();

        caller.getInbounds(InboundPartnerParamsDto.builder()
                        .supplierNamePart("test")
                        .dateTo(LocalDate.now(clock))
                        .date(LocalDate.now(clock))
                        .build())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].supplierName").value("test-supplier"))
                .andExpect(jsonPath("$.content[0].inboundExternalId").value("Зп-1"));

    }

    @Test
    @DisplayName("Отправляем информацию о кнопках на странице Поставки для XDoc СЦ")
    void getInboundsPageContainsXDocActions() {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.XDOC_ENABLED, true);

        caller.getInbounds(forToday())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.simpleActions").isArray())
                .andExpect(jsonPath("$.simpleActions").value(InboundPageSimpleAction.XDOC_GET_BARCODES.name()))
                .andExpect(jsonPath("$.groupActions").isArray())
                .andExpect(jsonPath("$.groupActions").value(InboundPageGroupAction.XDOC_PUT_CAR_ARRIVED.name()));
    }

    private void getInboundAndAssertResponse(String expected) throws Exception {
        String actual = callAndGetResponse(LocalDate.now(clock), null, null, null);
        System.out.println("Ответ от getInbound:\n" + actual);
        JSONAssert.assertEquals(expected, actual, false);
    }

    private void getInboundAndAssertResponse(String expected, List<InboundType> types) throws Exception {
        String actual = callAndGetResponse(LocalDate.now(clock), types, null, null);
        JSONAssert.assertEquals(expected, actual, false);
    }

    private void getInboundAndAssertResponse(String expected, Pageable pageable) throws Exception {
        String actual = callAndGetResponse(LocalDate.now(clock), null, pageable, null);
        JSONAssert.assertEquals(expected, actual, false);
    }

    private void getInboundAndAssertResponse(String expected, long warehouseId) throws Exception {
        String actual = callAndGetResponse(LocalDate.now(clock), null, null, warehouseId);
        JSONAssert.assertEquals(expected, actual, false);
    }

    private String callAndGetResponse(
            LocalDate date,
            @Nullable List<InboundType> types,
            @Nullable Pageable pageable,
            @Nullable Long warehouseId
    ) throws Exception {
        String filter = getFilter(date, types, pageable, warehouseId);
        return callGetInboundsAndGetResult(sortingCenter, filter)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @NotNull
    private String getFilter(LocalDate date) {
        return getFilter(date, null, null, null);
    }

    @NotNull
    private String getFilter(LocalDate date, @org.jetbrains.annotations.Nullable List<InboundType> types,
                             @org.jetbrains.annotations.Nullable Pageable pageable,
                             @org.jetbrains.annotations.Nullable Long warehouseId) {
        String filter = "?date=" + date;
        if (types != null && !types.isEmpty()) {
            filter += "&types=" + StreamEx.of(types).map(InboundType::toString).joining(",");
        }
        if (pageable != null) {
            filter += "&page=" + pageable.getPageNumber() + "&size=" + pageable.getPageSize();
        }
        if (warehouseId != null) {
            filter += "&warehouseId=" + warehouseId;
        }
        return filter;
    }

    private ResultActions callGetInboundsAndGetResult(SortingCenter sortingCenter, String filter) throws Exception {
        String url = "/internal/partners/" + sortingCenter.getPartnerId()
                + "/inbounds" + filter;
        return mockMvc.perform(
                MockMvcRequestBuilders.get(url));
    }

    private Inbound createdInbound(String externalId, InboundType inboundType) {
        return createdInbound(externalId, inboundType, warehouse, null);
    }

    private Inbound createdInbound(
            String externalId,
            InboundType inboundType,
            Warehouse warehouse,
            @Nullable MovementCourier courier
    ) {
        return createdInbound(externalId, inboundType, warehouse.getYandexId(), emptyMap(), courier, null);
    }

    private Inbound createdInbound(
            String externalId,
            InboundType inboundType,
            String warehouseFromExternalId,
            Map<String, List<Pair<String, String>>> registryMap,
            @Nullable MovementCourier courier,
            @Nullable String informationListBarcode
    ) {
        var params = TestFactory.CreateInboundParams.builder()
                .inboundExternalId(externalId)
                .inboundType(inboundType)
                .fromDate(OffsetDateTime.now(clock))
                .warehouseFromExternalId(warehouseFromExternalId)
                .toDate(OffsetDateTime.now(clock))
                .sortingCenter(sortingCenter)
                .registryMap(registryMap)
                .movementCourier(courier)
                .informationListBarcode(informationListBarcode)
                .confirmed(true)
                .transportationId("TRN" + externalId)
                .build();
        return testFactory.createInbound(params);
    }

    private Sortable createSortable(String barcode,
                                    SortableType sortableType,
                                    @Nullable Inbound inbound) {
        return sortableTestFactory
                .storeSortable(sortingCenter, sortableType, DirectFlowType.TRANSIT, barcode,
                        inbound, null)
                .dummyPrepareDirect()
                .get();
    }

    private void mockStatusHistoryBy(Inbound inbound, Instant instant, @Nullable InboundStatus inboundStatus) {
        var statusFilter = inboundStatus == null ? "" : " and inbound_status = '" + inboundStatus.name() + "'";

        jdbcTemplate.update("UPDATE inbound_status_history " +
                "SET status_updated_at = '" + instant + "' WHERE inbound_id = " + inbound.getId() + statusFilter);
    }

    private void mockStatusHistoryBy(Inbound inbound) {
        var instant = clock.instant();
        mockStatusHistoryBy(inbound, instant, null);
    }

    @SneakyThrows
    private ResultActions callBarcodes(Integer amount) {
        var filter = amount == null ? "" : "?amount=" + amount;
        String url = "/internal/partners/" + sortingCenter.getPartnerId()
                + "/inbounds/barcodes" + filter;
        return mockMvc.perform(
                MockMvcRequestBuilders.get(url));
    }

    private InboundPartnerParamsDto forToday() {
        return InboundPartnerParamsDto.builder()
                .date(LocalDate.now(clock))
                .build();
    }

}
