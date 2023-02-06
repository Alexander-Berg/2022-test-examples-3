package ru.yandex.market.sc.internal.controller.partner;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import one.util.streamex.StreamEx;
import org.apache.commons.collections4.CollectionUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.delivery_service.model.DeliveryServiceType;
import ru.yandex.market.sc.core.domain.delivery_service.repository.DeliveryServiceProperty;
import ru.yandex.market.sc.core.domain.inbound.repository.Inbound;
import ru.yandex.market.sc.core.domain.inbound.repository.Registry;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistryRepository;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistrySortable;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistrySortableRepository;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistryType;
import ru.yandex.market.sc.core.domain.lot.repository.LotStatus;
import ru.yandex.market.sc.core.domain.outbound.OutboundDocsService;
import ru.yandex.market.sc.core.domain.outbound.OutboundFacade;
import ru.yandex.market.sc.core.domain.outbound.OutboundTrnDocsService;
import ru.yandex.market.sc.core.domain.outbound.model.OutboundIdentifier;
import ru.yandex.market.sc.core.domain.outbound.model.OutboundIdentifierType;
import ru.yandex.market.sc.core.domain.outbound.model.OutboundInternalStatusDto;
import ru.yandex.market.sc.core.domain.outbound.model.OutboundType;
import ru.yandex.market.sc.core.domain.outbound.model.partner.InboundWithPalletsPartnerDtoList;
import ru.yandex.market.sc.core.domain.outbound.model.partner.InboundsInformation;
import ru.yandex.market.sc.core.domain.outbound.model.partner.OutboundAvailableAction;
import ru.yandex.market.sc.core.domain.outbound.model.partner.OutboundDiffDto;
import ru.yandex.market.sc.core.domain.outbound.model.partner.OutboundDocsDto;
import ru.yandex.market.sc.core.domain.outbound.model.partner.OutboundPartnerDto;
import ru.yandex.market.sc.core.domain.outbound.model.partner.OutboundTrnDocsDto;
import ru.yandex.market.sc.core.domain.outbound.model.partner.PalletPartnerDto;
import ru.yandex.market.sc.core.domain.outbound.model.partner.SortableDiffDto;
import ru.yandex.market.sc.core.domain.outbound.model.partner.SortableDiffStatusDto;
import ru.yandex.market.sc.core.domain.outbound.repository.Outbound;
import ru.yandex.market.sc.core.domain.outbound.repository.OutboundDocsStatus;
import ru.yandex.market.sc.core.domain.outbound.repository.OutboundRepository;
import ru.yandex.market.sc.core.domain.outbound.repository.OutboundStatus;
import ru.yandex.market.sc.core.domain.sortable.SortableQueryService;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLotService;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sortable.repository.Sortable;
import ru.yandex.market.sc.core.domain.sortable.repository.SortableRepository;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.resolver.dto.ScContext;
import ru.yandex.market.sc.core.test.DefaultScUserWarehouseExtension;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.core.util.flow.xdoc.XDocFlow;
import ru.yandex.market.sc.internal.controller.dto.PartnerShipOutboundDto;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;
import ru.yandex.market.sc.internal.util.ScIntControllerCaller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.sc.core.test.TestFactory.order;

@ScIntControllerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@ExtendWith(DefaultScUserWarehouseExtension.class)
class PartnerOutboundControllerTest {

    private final TestFactory testFactory;
    private final SortableQueryService sortableQueryService;
    private final SortableRepository sortableRepository;
    private final RegistrySortableRepository registrySortableRepository;
    private final TransactionTemplate transactionTemplate;
    private final RegistryRepository registryRepository;
    private final OutboundDocsService outboundDocsService;
    private final OutboundRepository outboundRepository;
    private final OutboundFacade outboundFacade;
    private final OutboundTrnDocsService outboundTrnDocsService;
    private final SortableLotService sortableLotService;
    private final ObjectMapper objectMapper;

    private final XDocFlow flow;
    private final ScIntControllerCaller caller;

    @MockBean
    Clock clock;
    private SortingCenter sortingCenter;
    private Cell cell;

    private final String EXPECTED_LIST_RESPONSE_FORMAT = """
            {
                "content": [%1$s],
              "pageable": {
                  "sort": {
                        "sorted": false,
                        "unsorted": true,
                        "empty": true
                      },
                      "offset": %10$s,
                      "pageNumber": %9$s,
                      "pageSize":  %5$s,
                      "paged": true,
                      "unpaged": false
                  },
              "totalPages": %2$s,
              "totalElements": %3$s,
              "last": %7$s,
              "first": %8$s,
              "size": %5$s,
              "number": %11$s,
              "sort": {
                "sorted": false,
                "unsorted": true,
                "empty": true
              },
              "numberOfElements": %6$s,
              "empty": %4$s}
            """;

    private final String EXPECTED_DTO_RESPONSE_FORMAT =
            """
                            {
                                "outboundExternalId": "%1$s",
                                "fromTime": %2$s,
                                "toTime": %3$s,
                                "plannedBoxesAmount": %4$s,
                                "plannedPalletAmount": %5$s,
                                "factualBoxesAmount": %6$s,
                                "factualPalletAmount": %7$s,
                                "sortedBoxesAmount": %8$s,
                                "sortedPalletAmount": %9$s,
                                "destination": "ООО Ромашка-Склад",
                                "status": "%10$s",
                                "docs" : {
                                    "status": "%11$s",
                                    "links": %12$s
                                },
                                "actions": []
                            }
                    """;

    private Outbound outbound;

    @BeforeEach
    void init() {
        testFactory.setupMockClock(clock);
        sortingCenter = testFactory.storedSortingCenter();
        cell = testFactory.storedCell(sortingCenter, "cell", CellType.COURIER, CellSubType.SHIP_XDOC);
    }

    @Test
    @DisplayName("Получение отгрузок (по дате) при отсутствии фактически отгруженных sortable. Статус - NOT_STARTED")
    void getOutbounds_byDate_statusNotStarted() {
        getOutbounds_statusNotStarted(LocalDate.now(clock), LocalDate.now(clock), null);
    }

    @Test
    @DisplayName("Получение отгрузок (по externalId) при отсутствии фактически отгруженных sortable. Статус - " +
            "NOT_STARTED")
    void getOutbounds_byExternalId_statusNotStarted() {
        getOutbounds_statusNotStarted(null, null, "out");
    }

    private void getOutbounds_statusNotStarted(
            @Nullable LocalDate dateFrom,
            @Nullable LocalDate dateTo,
            @Nullable String query) {
        outbound = flow.createInbound("in1")
                .linkPallets("XDOC-1", "XDOC-2")
                .fixInbound()
                .createOutbound("out")
                .buildRegistry("XDOC-1", "XDOC-2")
                .and()
                .getOutbound("out");
        Inbound inbound = flow.getInbound("in1");

        OutboundPartnerDto outboundPartnerDto = getOutboundPartnerDto(sortableQueryService.find(outbound),
                OutboundInternalStatusDto.NOT_STARTED, outbound, Set.of(inbound.getExternalId()),
                Set.of(inbound.getExternalId()),
                Set.of(),
                Set.of(Objects.requireNonNull(cell.getScNumber())));

        var expectedResponse = getExpectedResponse(outboundPartnerDto);

        caller.getOutbounds(dateFrom, dateTo, query, null)
                .andExpect(status().isOk())
                .andExpect(content().json(expectedResponse));
    }

    @DisplayName("Статус отгрузки по схеме ОПКО СЦ - СЦ")
    @Test
    void outboundStatus() {
        long sortingCenterToId = 111746283111L; //random number
        testFactory.setSortingCenterProperty(
                flow.getSortingCenter().getId(), SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        testFactory.setSortingCenterProperty(
                flow.getSortingCenter().getId(),
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS,
                true
        );
        testFactory.increaseScOrderId();
        var deliveryService = testFactory.storedDeliveryService(String.valueOf(sortingCenterToId));
        testFactory.setDeliveryServiceProperty(deliveryService,
                DeliveryServiceProperty.DS_SUPPORT_SC_TO_SC_TRANSPORTATIONS,
                String.valueOf(flow.getSortingCenter().getId()));
        var sortingCenterTo = testFactory.storedSortingCenter(
                TestFactory.SortingCenterParams.builder()
                        .id(sortingCenterToId)
                        .yandexId("5378264623")
                        .token("sc_to_token")
                        .partnerName("sc_to_partner_name")
                        .build()
        );
        var scToScOutbound = testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("outbound1")
                .type(OutboundType.DS_SC)
                .fromTime(clock.instant().minus(1, ChronoUnit.HOURS))
                .toTime(clock.instant().plus(1, ChronoUnit.HOURS))
                .sortingCenter(flow.getSortingCenter())
                .logisticPointToExternalId(sortingCenterTo.getYandexId())
                .build()
        );
        var outbound = outboundRepository.findByExternalId("outbound1").orElseThrow();
        var order = testFactory.createForToday(
                order(flow.getSortingCenter(), "1").places("11", "12")
                        .deliveryService(deliveryService).dsType(DeliveryServiceType.TRANSIT).build()
        ).acceptPlaces("11", "12").sortPlaces("11", "12").get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var cell = testFactory.determineRouteCell(route, order);
        var lot = testFactory.storedLot(flow.getSortingCenter(), cell, LotStatus.CREATED);
        testFactory.sortToLot(order, "11", lot, flow.getUser());
        testFactory.sortToLot(order, "12", lot, flow.getUser());
        testFactory.prepareToShipLot(lot);
        var outboundExternalId = outboundFacade.bindLotToOutbound(new OutboundIdentifier
                        (OutboundIdentifierType.EXTERNAL_ID, outbound.getExternalId()),
                lot.getBarcode(), testFactory.getRouteIdForSortableFlow(route), new ScContext(flow.getUser(), testFactory.storedSortingCenter()));
        OutboundPartnerDto expected = OutboundPartnerDto.builder()
                .id(outbound.getId())
                .outboundExternalId(outboundExternalId)
                .destination("")
                .status(OutboundInternalStatusDto.READY_TO_SHIP)
                .factualPalletAmount(1L)
                .plannedPalletAmount(1L)
                .sortedPalletAmount(1L)
                .carNumber(null)
                .courierInfo("name")
                .actions(List.of(OutboundAvailableAction.PRINT_QR_CODE))
                .toTime(LocalDateTime.ofInstant(outbound.getToTime(), clock.getZone()))
                .fromTime(LocalDateTime.ofInstant(outbound.getFromTime(), clock.getZone()))
                .destination(sortingCenterTo.getScName())
                .shipByLots(true)
                .docs(new OutboundDocsDto(OutboundDocsStatus.NOT_REQUESTED, Collections.emptyList()))
                .build();

        var actualPage = outboundFacade.getPartnerOutbounds(LocalDate.now(clock), LocalDate.now(clock), "",
                outbound.getSortingCenter(), testFactory.getRouteIdForSortableFlow(route), PageRequest.of(0, 20));
        assertThat(actualPage.stream().count()).isEqualTo(1);
        var actual = actualPage.stream().findFirst().orElseThrow();
        assertThat(actual).isEqualTo(expected);
        outboundFacade.shipOutboundByPIAdmin(outboundExternalId, null);
        expected = OutboundPartnerDto.builder()
                .id(outbound.getId())
                .outboundExternalId(outboundExternalId)
                .destination("")
                .status(OutboundInternalStatusDto.SHIPPED)
                .factualPalletAmount(1L)
                .plannedPalletAmount(1L)
                .sortedPalletAmount(1L)
                .carNumber(null)
                .courierInfo("name")
                .actions(List.of(OutboundAvailableAction.PRINT_QR_CODE, OutboundAvailableAction.PRINT_TRANSFER_ACT))
                .toTime(LocalDateTime.ofInstant(outbound.getToTime(), clock.getZone()))
                .fromTime(LocalDateTime.ofInstant(outbound.getFromTime(), clock.getZone()))
                .destination(sortingCenterTo.getScName())
                .shipByLots(true)
                .docs(new OutboundDocsDto(OutboundDocsStatus.NOT_REQUESTED, Collections.emptyList()))
                .build();
        actualPage = outboundFacade.getPartnerOutbounds(LocalDate.now(clock), LocalDate.now(clock), "",
                outbound.getSortingCenter(), testFactory.getRouteIdForSortableFlow(route), PageRequest.of(0, 20));
        assertThat(actualPage.stream().count()).isEqualTo(1);
        actual = actualPage.stream().findFirst().orElseThrow();
        actual.setActions(actual.getActions().stream().sorted().toList());
        expected.setActions(expected.getActions().stream().sorted().toList());
        assertThat(actual).isEqualTo(expected);
    }

    @DisplayName("Статус отгрузки по схеме ОПКО дропофф - СЦ когда нет лотов.")
    @Test
    void outboundStatusFromDropoffToSc() {
        long sortingCenterToId = 111746283111L; //random number
        testFactory.setSortingCenterProperty(
                flow.getSortingCenter().getId(), SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        testFactory.setSortingCenterProperty(
                flow.getSortingCenter().getId(),
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS,
                true
        );
        testFactory.setSortingCenterProperty(
                flow.getSortingCenter().getId(),
                SortingCenterPropertiesKey.IS_DROPOFF,
                true
        );
        testFactory.increaseScOrderId();
        var deliveryService = testFactory.storedDeliveryService(String.valueOf(sortingCenterToId));
        testFactory.setDeliveryServiceProperty(deliveryService,
                DeliveryServiceProperty.DS_SUPPORT_SC_TO_SC_TRANSPORTATIONS,
                String.valueOf(flow.getSortingCenter().getId()));
        var sortingCenterTo = testFactory.storedSortingCenter(
                TestFactory.SortingCenterParams.builder()
                        .id(sortingCenterToId)
                        .yandexId("5378264623")
                        .token("sc_to_token")
                        .partnerName("sc_to_partner_name")
                        .build()
        );
        var scToScOutbound = testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("outbound1")
                .type(OutboundType.DS_SC)
                .fromTime(clock.instant())
                .toTime(clock.instant())
                .sortingCenter(flow.getSortingCenter())
                .logisticPointToExternalId(sortingCenterTo.getYandexId())
                .partnerToExternalId(deliveryService.getYandexId())
                .build()
        );
        var outbound = outboundRepository.findByExternalId("outbound1").orElseThrow();
        var order = testFactory.createForToday(
                order(flow.getSortingCenter(), "1").places("11", "12")
                        .deliveryService(deliveryService).dsType(DeliveryServiceType.TRANSIT).build()
        ).acceptPlaces("11", "12").sortPlaces("11", "12").get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var cell = testFactory.determineRouteCell(route, order);
        var outboundExternalId = outbound.getExternalId();
        OutboundPartnerDto expected = OutboundPartnerDto.builder()
                .id(outbound.getId())
                .outboundExternalId(outboundExternalId)
                .status(OutboundInternalStatusDto.NOT_STARTED)
                .courierInfo("name")
                .actions(List.of(OutboundAvailableAction.PRINT_QR_CODE))
                .toTime(LocalDateTime.ofInstant(outbound.getToTime(), clock.getZone()))
                .fromTime(LocalDateTime.ofInstant(outbound.getFromTime(), clock.getZone()))
                .destination(sortingCenterTo.getScName())
                .shipByLots(true)
                .docs(new OutboundDocsDto(OutboundDocsStatus.NOT_REQUESTED, Collections.emptyList()))
                .build();

        var actualPage = outboundFacade.getPartnerOutbounds(LocalDate.now(clock), LocalDate.now(clock), "",
                outbound.getSortingCenter(), testFactory.getRouteIdForSortableFlow(route), PageRequest.of(0, 20));
        assertThat(actualPage.stream().count()).isEqualTo(1);
        var actual = actualPage.stream().findFirst().orElseThrow();
        assertThat(actual).isEqualTo(expected);

        // start ship
        testFactory.shipPlace(order, "11");
        testFactory.shipPlace(order, "12");
        expected = OutboundPartnerDto.builder()
                .id(outbound.getId())
                .outboundExternalId(outboundExternalId)
                .status(OutboundInternalStatusDto.READY_TO_SHIP)
                .courierInfo("name")
                .actions(List.of(OutboundAvailableAction.PRINT_QR_CODE))
                .toTime(LocalDateTime.ofInstant(outbound.getToTime(), clock.getZone()))
                .fromTime(LocalDateTime.ofInstant(outbound.getFromTime(), clock.getZone()))
                .plannedBoxesAmount(2L)
                .sortedBoxesAmount(2L)
                .factualBoxesAmount(2L)
                .destination(sortingCenterTo.getScName())
                .shipByLots(true)
                .docs(new OutboundDocsDto(OutboundDocsStatus.NOT_REQUESTED, Collections.emptyList()))
                .build();

        Long routeId = testFactory.getRouteIdForSortableFlow(route.getId());
        actualPage = outboundFacade.getPartnerOutbounds(LocalDate.now(clock), LocalDate.now(clock), "",
                outbound.getSortingCenter(), routeId, PageRequest.of(0, 20));
        assertThat(actualPage.stream().count()).isEqualTo(1);
        actual = actualPage.stream().findFirst().orElseThrow();
        assertThat(actual).isEqualTo(expected);
        outboundFacade.shipOutboundByPIAdmin(outboundExternalId, null);
        expected = OutboundPartnerDto.builder()
                .id(outbound.getId())
                .outboundExternalId(outboundExternalId)
                .status(OutboundInternalStatusDto.SHIPPED)
                .courierInfo("name")
                .actions(List.of(OutboundAvailableAction.PRINT_QR_CODE, OutboundAvailableAction.PRINT_TRANSFER_ACT))
                .toTime(LocalDateTime.ofInstant(outbound.getToTime(), clock.getZone()))
                .fromTime(LocalDateTime.ofInstant(outbound.getFromTime(), clock.getZone()))
                .plannedBoxesAmount(2L)
                .sortedBoxesAmount(2L)
                .factualBoxesAmount(2L)
                .destination(sortingCenterTo.getScName())
                .shipByLots(true)
                .docs(new OutboundDocsDto(OutboundDocsStatus.NOT_REQUESTED, Collections.emptyList()))
                .build();
        actualPage = outboundFacade.getPartnerOutbounds(LocalDate.now(clock), LocalDate.now(clock), "",
                outbound.getSortingCenter(), testFactory.getRouteIdForSortableFlow(route), PageRequest.of(0, 20));
        assertThat(actualPage.stream().count()).isEqualTo(1);
        actual = actualPage.stream().findFirst().orElseThrow();
        actual.setActions(actual.getActions().stream().sorted().toList());
        expected.setActions(expected.getActions().stream().sorted().toList());
        assertThat(actual).isEqualTo(expected);
    }

    @DisplayName("Статус отгрузки по схеме ОПКО СЦ - СЦ с маппингом групп партнеров")
    @Test
    void outboundStatusWithMapping() {
        testFactory.setSortingCenterProperty(flow.getSortingCenter().getId(),
                SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, true);
        testFactory.setSortingCenterProperty(flow.getSortingCenter().getId(),
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS, true);
        testFactory.setSortingCenterProperty(flow.getSortingCenter().getId(),
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS_FOR_ALL_DELIVERY_SERVICES, true);
        testFactory.increaseScOrderId();

        var virtualPartnerId = 10001111111L;
        var existingYandexId = "10002222222";      // общая для нескольких виртуальных СЦ логистическая точка
        var existingPartnerId = 111746283111L;

        var sortingCenterTo = testFactory.storedSortingCenter(
                TestFactory.SortingCenterParams.builder()
                        .id(existingPartnerId)
                        .yandexId(existingYandexId) // правильная логистическая точка
                        .token("sc_to_token")
                        .partnerName("sc_to_partner_name")
                        .build()
        );

        testFactory.storedPartnerMappingGroup(virtualPartnerId, existingPartnerId);

        var deliveryService = testFactory.storedDeliveryService(String.valueOf(virtualPartnerId));

        var outbound = testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("outbound1")
                .type(OutboundType.DS_SC)
                .fromTime(clock.instant().minus(1, ChronoUnit.HOURS))
                .toTime(clock.instant().plus(1, ChronoUnit.HOURS))
                .sortingCenter(flow.getSortingCenter())
                .logisticPointToExternalId(existingYandexId)
                .build()
        );
        var order = testFactory.createForToday(
                order(flow.getSortingCenter(), "1")
                        .deliveryService(deliveryService).dsType(DeliveryServiceType.TRANSIT).build()
        ).accept().sort().get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var cell = testFactory.determineRouteCell(route, order);
        var lot = testFactory.storedLot(flow.getSortingCenter(), cell, LotStatus.CREATED);
        testFactory.sortOrderToLot(order, lot, flow.getUser());
        testFactory.prepareToShipLot(lot);

        var outboundExternalId = outboundFacade.bindLotToOutbound(new OutboundIdentifier
                        (OutboundIdentifierType.EXTERNAL_ID, outbound.getExternalId()),
                lot.getBarcode(), testFactory.getRouteIdForSortableFlow(route), new ScContext(flow.getUser(), testFactory.storedSortingCenter()));
        OutboundPartnerDto expected = OutboundPartnerDto.builder()
                .id(outbound.getId())
                .outboundExternalId(outboundExternalId)
                .status(OutboundInternalStatusDto.READY_TO_SHIP)
                .factualPalletAmount(1L)
                .plannedPalletAmount(1L)
                .sortedPalletAmount(1L)
                .carNumber(null)
                .courierInfo("name")
                .actions(List.of(OutboundAvailableAction.PRINT_QR_CODE))
                .toTime(LocalDateTime.ofInstant(outbound.getToTime(), clock.getZone()))
                .fromTime(LocalDateTime.ofInstant(outbound.getFromTime(), clock.getZone()))
                .destination(sortingCenterTo.getScName())
                .shipByLots(true)
                .docs(new OutboundDocsDto(OutboundDocsStatus.NOT_REQUESTED, Collections.emptyList()))
                .build();

        var actualPage = outboundFacade.getPartnerOutbounds(LocalDate.now(clock), LocalDate.now(clock), "",
                outbound.getSortingCenter(), testFactory.getRouteIdForSortableFlow(route), PageRequest.of(0, 20));
        assertThat(actualPage.stream().count()).isEqualTo(1);
        var actual = actualPage.stream().findFirst().orElseThrow();
        assertThat(actual).isEqualTo(expected);
        outboundFacade.shipOutboundByPIAdmin(outboundExternalId, null);
        expected = OutboundPartnerDto.builder()
                .id(outbound.getId())
                .outboundExternalId(outboundExternalId)
                .destination("")
                .status(OutboundInternalStatusDto.SHIPPED)
                .factualPalletAmount(1L)
                .plannedPalletAmount(1L)
                .sortedPalletAmount(1L)
                .carNumber(null)
                .courierInfo("name")
                .actions(List.of(OutboundAvailableAction.PRINT_QR_CODE, OutboundAvailableAction.PRINT_TRANSFER_ACT))
                .toTime(LocalDateTime.ofInstant(outbound.getToTime(), clock.getZone()))
                .fromTime(LocalDateTime.ofInstant(outbound.getFromTime(), clock.getZone()))
                .destination(sortingCenterTo.getScName())
                .shipByLots(true)
                .docs(new OutboundDocsDto(OutboundDocsStatus.NOT_REQUESTED, Collections.emptyList()))
                .build();
        actualPage = outboundFacade.getPartnerOutbounds(LocalDate.now(clock), LocalDate.now(clock), "",
                outbound.getSortingCenter(), testFactory.getRouteIdForSortableFlow(route), PageRequest.of(0, 20));
        assertThat(actualPage.stream().count()).isEqualTo(1);
        actual = actualPage.stream().findFirst().orElseThrow();
        actual.setActions(actual.getActions().stream().sorted().toList());
        expected.setActions(expected.getActions().stream().sorted().toList());
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName("Получение отгрузок при всех фактически отгруженных sortable. Статус - ReadyToShip")
    void getOutbounds_statusReadyToShip() {
        outbound = flow.createInbound("in1")
                .linkPallets("XDOC-1", "XDOC-2")
                .fixInbound()
                .createOutbound("out")
                .buildRegistry("XDOC-1", "XDOC-2")
                .sortToAvailableCell("XDOC-1", "XDOC-2")
                .prepareToShip("XDOC-1", "XDOC-2")
                .and()
                .getOutbound("out");
        Inbound inbound = flow.getInbound("in1");

        OutboundPartnerDto outboundPartnerDto = getOutboundPartnerDto(sortableQueryService.find(outbound),
                OutboundInternalStatusDto.READY_TO_SHIP, outbound, Set.of(inbound.getExternalId()),
                Set.of(),
                Set.of(inbound.getExternalId()),
                Set.of(Objects.requireNonNull(cell.getScNumber())));

        var expectedResponse = getExpectedResponse(outboundPartnerDto);

        caller.getOutbounds(LocalDate.now(clock), LocalDate.now(clock), null, null)
                .andExpect(status().isOk())
                .andExpect(content().json(expectedResponse));
    }

    @Test
    @DisplayName("Получение отгрузок при всех фактически отгруженных sortable при времени до и после полуночи. " +
            "Статус - READY_TO_SHIP")
    void getOutbounds_statusReadyToShip_timeBeforeAndAfterMidnight() {
        testFactory.setupMockClock(clock, Instant.parse("2019-12-31T23:00:01Z"));
        outbound = flow.createInbound("in1")
                .linkPallets("XDOC-1", "XDOC-2")
                .fixInbound()
                .createOutbound("out")
                .buildRegistry("XDOC-1", "XDOC-2")
                .sortToAvailableCell("XDOC-1", "XDOC-2")
                .prepareToShip("XDOC-1", "XDOC-2")
                .and()
                .getOutbound("out");
        Inbound inbound = flow.getInbound("in1");

        OutboundPartnerDto outboundPartnerDto = getOutboundPartnerDto(sortableQueryService.find(outbound),
                OutboundInternalStatusDto.READY_TO_SHIP, outbound, Set.of(inbound.getExternalId()),
                Set.of(),
                Set.of(inbound.getExternalId()),
                Set.of(Objects.requireNonNull(cell.getScNumber())));

        var expectedResponse = getExpectedResponse(outboundPartnerDto);

        caller.getOutbounds(LocalDate.now(clock), LocalDate.now(clock), null, null)
                .andExpect(status().isOk())
                .andExpect(content().json(expectedResponse));
    }

    @Test
    @DisplayName("Получение отгрузок при неполностью отгруженной одной поставки. Статус - NOT_FULLY_PREPARED")
    void getOutbounds_statusNotFullyPrepared() {
        var outboundPartnerDto = transactionTemplate.execute(status -> {
            outbound = flow.createInbound("in1")
                    .linkPallets("XDOC-1", "XDOC-2")
                    .fixInbound()
                    .createOutbound("out")
                    .buildRegistry("XDOC-1", "XDOC-2")
                    .sortToAvailableCell("XDOC-1", "XDOC-2")
                    .prepareToShip("XDOC-1")
                    .and()
                    .getOutbound("out");
            Inbound inbound = flow.getInbound("in1");

            return getOutboundPartnerDto(sortableQueryService.find(outbound),
                    OutboundInternalStatusDto.NOT_FULLY_PREPARED, outbound, Set.of(inbound.getExternalId()),
                    Set.of(inbound.getExternalId()),
                    Set.of(),
                    Set.of(Objects.requireNonNull(cell.getScNumber())));
        });

        var expectedResponse = getExpectedResponse(outboundPartnerDto);

        caller.getOutbounds(LocalDate.now(clock), LocalDate.now(clock), null, null)
                .andExpect(status().isOk())
                .andExpect(content().json(expectedResponse));
    }

    @Test
    @DisplayName("Получение отгрузок при неполностью отгруженной одной из поставок и не начатой второй. Статус - " +
            "NOT_FULLY_PREPARED")
    void getOutbounds_statusNotFullyPrepared_differentInbounds() {
        OutboundPartnerDto outboundPartnerDto = transactionTemplate.execute(status -> {
            outbound = flow.createInbound("in1")
                    .linkPallets("XDOC-1", "XDOC-2")
                    .fixInbound()
                    .createInbound("in2")
                    .linkPallets("XDOC-3")
                    .fixInbound()
                    .createOutbound("out")
                    .buildRegistry("XDOC-1", "XDOC-2", "XDOC-3")
                    .sortToAvailableCell("XDOC-1", "XDOC-2", "XDOC-3")
                    .prepareToShip("XDOC-1")
                    .and()
                    .getOutbound("out");
            Inbound inbound1 = flow.getInbound("in1");
            Inbound inbound2 = flow.getInbound("in2");
            return getOutboundPartnerDto(sortableQueryService.find(outbound),
                    OutboundInternalStatusDto.NOT_FULLY_PREPARED, outbound, Set.of(inbound1.getExternalId(),
                            inbound2.getExternalId()),
                    Set.of(inbound2.getExternalId(), inbound1.getExternalId()),
                    Set.of(),
                    Set.of(Objects.requireNonNull(cell.getScNumber())));
        });

        var expectedResponse = getExpectedResponse(outboundPartnerDto);

        caller.getOutbounds(LocalDate.now(clock), LocalDate.now(clock), null, null)
                .andExpect(status().isOk())
                .andExpect(content().json(expectedResponse));
    }

    @Test
    @DisplayName("Получение отгрузок при полностью отгруженной одной из поставок и не начатой второй." +
            " Статус - READY_TO_SHIP")
    void getOutbounds_statusReadyToShip_differentInbounds() {
        OutboundPartnerDto outboundPartnerDto = transactionTemplate.execute(status -> {
            outbound = flow.createInbound("in1")
                    .linkPallets("XDOC-1", "XDOC-2")
                    .fixInbound()
                    .createInbound("in2")
                    .linkPallets("XDOC-3")
                    .fixInbound()
                    .createOutbound("out")
                    .buildRegistry("XDOC-1", "XDOC-2", "XDOC-3")
                    .sortToAvailableCell("XDOC-1", "XDOC-2", "XDOC-3")
                    .prepareToShip("XDOC-1", "XDOC-2")
                    .and()
                    .getOutbound("out");
            Inbound inbound1 = flow.getInbound("in1");
            Inbound inbound2 = flow.getInbound("in2");

            return getOutboundPartnerDto(sortableQueryService.find(outbound),
                    OutboundInternalStatusDto.READY_TO_SHIP, outbound, Set.of(inbound1.getExternalId(),
                            inbound2.getExternalId()),
                    Set.of(inbound2.getExternalId()),
                    Set.of(inbound1.getExternalId()),
                    Set.of(Objects.requireNonNull(cell.getScNumber())));
        });

        var expectedResponse = getExpectedResponse(outboundPartnerDto);

        caller.getOutbounds(LocalDate.now(clock), LocalDate.now(clock), null, null)
                .andExpect(status().isOk())
                .andExpect(content().json(expectedResponse));
    }

    @Test
    @DisplayName("Получение отгрузок в статусе SHIPPED. Статус - SHIPPED")
    void getOutbounds_statusShipped() {
        OutboundPartnerDto outboundPartnerDto = transactionTemplate.execute(status -> {
            outbound = flow.createInbound("in1")
                    .linkPallets("XDOC-1", "XDOC-2")
                    .fixInbound()
                    .createOutbound("out")
                    .buildRegistry("XDOC-1", "XDOC-2")
                    .sortToAvailableCell("XDOC-1", "XDOC-2")
                    .prepareToShip("XDOC-1", "XDOC-2")
                    .shipAndGet("out");

            Inbound inbound = flow.getInbound("in1");

            return getOutboundPartnerDto(sortableQueryService.find(outbound),
                    OutboundInternalStatusDto.SHIPPED, outbound, Set.of(inbound.getExternalId()),
                    new HashSet<>(),
                    Set.of(inbound.getExternalId()),
                    Set.of(Objects.requireNonNull(cell.getScNumber())));
        });

        var expectedResponse = getExpectedResponse(outboundPartnerDto);

        caller.getOutbounds(LocalDate.now(clock), LocalDate.now(clock), null, null)
                .andExpect(status().isOk())
                .andExpect(content().json(expectedResponse));
    }

    @Test
    @DisplayName("Получение нескольких отгрузок за разные дни. Статус - ReadyToShip")
    void getOutbounds_severalOutbounds_differentDays() {
        outbound = flow.createInbound("in1")
                .linkPallets("XDOC-1", "XDOC-2")
                .fixInbound()
                .createOutbound("out1")
                .externalId("reg1")
                .buildRegistry("XDOC-1", "XDOC-2")
                .sortToAvailableCell("XDOC-1", "XDOC-2")
                .prepareToShip("XDOC-1", "XDOC-2")
                .and()
                .getOutbound("out1");
        Inbound inbound = flow.getInbound("in1");

        Cell cell2 = testFactory.storedCell(sortingCenter, "cell2", CellType.COURIER, CellSubType.SHIP_XDOC);

        var outbound2 = flow.createInbound("in2")
                .linkPallets("XDOC-3", "XDOC-4")
                .fixInbound()
                .outboundBuilder("out2")
                .toRegistryBuilder(
                        clock.instant().plus(1, ChronoUnit.DAYS),
                        clock.instant().plus(2, ChronoUnit.DAYS)
                )
                .externalId("reg2")
                .buildRegistry("XDOC-3", "XDOC-4")
                .and()
                .getOutbound("out2");

        Inbound inbound2 = flow.getInbound("in2");

        OutboundPartnerDto outboundPartnerDto1 = getOutboundPartnerDto(sortableQueryService.find(outbound),
                OutboundInternalStatusDto.READY_TO_SHIP, outbound, Set.of(inbound.getExternalId()),
                new HashSet<>(),
                Set.of(inbound.getExternalId()),
                Set.of(Objects.requireNonNull(cell.getScNumber())));

        OutboundPartnerDto outboundPartnerDto2 = getOutboundPartnerDto(sortableQueryService.find(outbound2),
                OutboundInternalStatusDto.NOT_STARTED, outbound2, Set.of(inbound2.getExternalId()),
                new HashSet<>(),
                Set.of(inbound2.getExternalId()),
                Set.of(Objects.requireNonNull(cell2.getScNumber())));

        var expectedResponse = getExpectedResponse(outboundPartnerDto1, outboundPartnerDto2);

        caller.getOutbounds(LocalDate.now(clock), LocalDate.now(clock).plusDays(1), null, null)
                .andExpect(status().isOk())
                .andExpect(content().json(expectedResponse));
    }

    @Test
    void getOutboundsWithBasket() {
        Sortable basket = flow.createBasketAndGet();
        var outbound = flow.createInbound("IN1")
                .linkBoxes("XDOC-1")
                .fixInbound()
                .createOutbound("OUT1")
                .addRegistryBoxes("XDOC-1")
                .buildRegistry(basket.getRequiredBarcodeOrThrow())
                .sortToAvailableLot("XDOC-1")
                .sortToAvailableCell(basket.getRequiredBarcodeOrThrow())
                .prepareToShip()
                .and()
                .getOutbound("OUT1");

        Inbound inbound = flow.getInbound("IN1");

        OutboundPartnerDto outboundPartnerDto = getOutboundPartnerDto(
                sortableQueryService.find(outbound),
                OutboundInternalStatusDto.NOT_STARTED, outbound, Set.of(inbound.getExternalId()),
                new HashSet<>(),
                Set.of(inbound.getExternalId()),
                Set.of(Objects.requireNonNull(cell.getScNumber())));

        var expectedResponse = getExpectedResponse(outboundPartnerDto);

        caller.getOutbounds(LocalDate.now(clock), LocalDate.now(clock).plusDays(1), null, null)
                .andExpect(status().isOk())
                .andExpect(content().json(expectedResponse));
    }

    @ParameterizedTest(name = "{index} => page={0}, size={1}, numberOfOutbounds={2}")
    @CsvSource({
            "0, 10, 20",
            "1, 20, 30",
            "2, 10, 10",
            "1, 20, 21"
    })
    @DisplayName("Получение нескольких страниц отгрузок. Статус - ReadyToShip")
    void getOutbounds_severalOutbounds_firstPages(int page, int size, int numberOfOutbounds) {
        var dtos = createOutbounds(numberOfOutbounds);
        var expectedResponse =
                getExpectedResponse(size, page, dtos.toArray(OutboundPartnerDto[]::new));
        caller.getOutbounds(LocalDate.now(clock), LocalDate.now(clock), null, PageRequest.of(page, size))
                .andDo(r -> System.out.println("content: " + r))
                .andExpect(status().isOk())
                .andExpect(content().json(expectedResponse));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "10", "04", "SMTH"})
    @DisplayName("Получение нескольких отгрузок с фильтром. Статус - ReadyToShip")
    void getOutbounds_severalOutbounds_withFilter(String query) {
        var dtos = createOutbounds(5);
        var expectedResponse =
                getExpectedResponse(dtos.stream()
                        .filter(outboundPartnerDto -> outboundPartnerDto.getOutboundExternalId().contains(query))
                        .toArray(OutboundPartnerDto[]::new));
        caller.getOutbounds(LocalDate.now(clock), LocalDate.now(clock), query, null)
                .andExpect(status().isOk())
                .andExpect(content().json(expectedResponse));

    }

    @Test
    public void shouldReturnCancelledOutbound() {
        testFactory.createOutbound("ob1", OutboundStatus.CANCELLED_BY_SC, OutboundType.DS_SC,
                Instant.now(clock), Instant.now(clock), "lp_dest", sortingCenter, null);

        caller.getOutbounds(LocalDate.now(clock), LocalDate.now(clock), null, null)
                .andExpect(jsonPath("$.content[*]", hasSize(1)))
                .andExpect(jsonPath("$.content[0].status", is("CANCELLED_BY_SC")));
    }

    @Test
    @DisplayName("Завершение отгрузки. Перевод в статус assembled")
    void prepareOutbound() {
        outbound = flow.createInbound("in1")
                .linkPallets("XDOC-1", "XDOC-2")
                .fixInbound()
                .createOutbound("out1")
                .externalId("reg1")
                .buildRegistry("XDOC-1", "XDOC-2")
                .sortToAvailableCell("XDOC-1", "XDOC-2")
                .prepareToShip("XDOC-1", "XDOC-2")
                .and()
                .getOutbound("out1");
        // каждый вызов генерит реестр - у диспетчера должна быть возмжоность пересоздавать
        caller.prepareToShipOutbound(outbound.getExternalId())
                .andExpect(status().isOk());
        var registriesBeforeSecondCall = registryRepository.findAllByOutboundId(outbound.getId());
        caller.prepareToShipOutbound(outbound.getExternalId())
                .andExpect(status().isOk());

        var registriesAfterSecondCall = registryRepository.findAllByOutboundId(outbound.getId());
        var newRegistry = StreamEx.of(CollectionUtils.subtract(registriesAfterSecondCall, registriesBeforeSecondCall))
                .findFirst()
                .orElseThrow();

        var actualOutbound = flow.getOutbound(outbound.getExternalId());
        var actualDocs = outboundDocsService.tryToGetDocs(outbound).get();

        Assertions.assertThat(actualOutbound.getStatus()).isEqualTo(OutboundStatus.ASSEMBLED);
        Assertions.assertThat(actualDocs.getStatus()).isEqualTo(OutboundDocsStatus.REQUESTED);
        Assertions.assertThat(actualDocs.getRegistry()).isEqualTo(newRegistry);

        var registries = registryRepository.findAllByOutboundId(outbound.getId());
        assertThat(registries.stream().map(Registry::getType).toList())
                .containsExactlyInAnyOrder(RegistryType.PLANNED, RegistryType.PREPARED, RegistryType.PREPARED);
    }

    @Test
    @DisplayName("Получение состояния отгрузки по документам и разнице плана/факта")
    void preShipState() {
        flow.inboundBuilder("in-0")
                .informationListBarcode("Зп-3700")
                .build()
                .carArrivedAndReadyToReceive()
                .linkPallets("XDOC-001", "XDOC-002")
                .fixInbound();

        flow.inboundBuilder("in-1")
                .informationListBarcode("Зп-3701")
                .build()
                .carArrivedAndReadyToReceive()
                .linkPallets("XDOC-101", "XDOC-102")
                .fixInbound();

        flow.inboundBuilder("in-2")
                .informationListBarcode("Зп-3702")
                .build()
                .carArrivedAndReadyToReceive()
                .linkPallets("XDOC-201", "XDOC-202")
                .fixInbound();

        flow.inboundBuilder("in-3")
                .informationListBarcode("Зп-3703")
                .build()
                .carArrivedAndReadyToReceive()
                .linkBoxes("XDOC-301", "XDOC-302", "XDOC-303")
                .fixInbound();

        flow.inboundBuilder("in-4")
                .informationListBarcode("Зп-3704")
                .build()
                .carArrivedAndReadyToReceive()
                .linkBoxes("XDOC-401", "XDOC-402", "XDOC-403")
                .fixInbound();

        String unplannedInfoListCode = "Зп-3705";
        String unplannedBarcode = "XDOC-501";
        flow.inboundBuilder("in-5")
                .informationListBarcode(unplannedInfoListCode)
                .build()
                .carArrivedAndReadyToReceive()
                .linkPallets(unplannedBarcode)
                .fixInbound();

        testFactory.storedWarehouse();
        Cell cell = flow.createBufferCellAndGet("cell-1", TestFactory.WAREHOUSE_YANDEX_ID);

        // сортируем коробки в лоты
        Sortable basket3 = flow.createBasketAndGet(cell);
        var lot3 = sortableLotService.findBySortableId(basket3.getId()).orElseThrow();
        Sortable box31 = sortableQueryService.find(sortingCenter, "XDOC-301").orElseThrow();
        Sortable box32 = sortableQueryService.find(sortingCenter, "XDOC-302").orElseThrow();
        Sortable box33 = sortableQueryService.find(sortingCenter, "XDOC-303").orElseThrow();
        flow.sortBoxToLot(box31, lot3);
        flow.sortBoxToLot(box32, lot3);
        flow.sortBoxToLot(box33, lot3);
        flow.packLot(basket3.getRequiredBarcodeOrThrow());

        Sortable basket4 = flow.createBasketAndGet(cell);
        var lot1 = sortableLotService.findBySortableId(basket4.getId()).orElseThrow();
        Sortable box41 = sortableQueryService.find(sortingCenter, "XDOC-401").orElseThrow();
        Sortable box42 = sortableQueryService.find(sortingCenter, "XDOC-402").orElseThrow();
        Sortable box43 = sortableQueryService.find(sortingCenter, "XDOC-403").orElseThrow();
        flow.sortBoxToLot(box41, lot1);
        flow.sortBoxToLot(box42, lot1);
        flow.sortBoxToLot(box43, lot1);
        flow.packLot(basket4.getRequiredBarcodeOrThrow());

        // создали реестр отгрузки, подготовили не все грузоместа в отгрузке
        outbound = flow.createOutbound("out1")
                .externalId("reg1")
                .addRegistryPallets(
                        "XDOC-001", "XDOC-002",
                        "XDOC-101", "XDOC-102",
                        "XDOC-201", "XDOC-202",
                        basket3.getRequiredBarcodeOrThrow(), basket4.getRequiredBarcodeOrThrow()
                )
                .addRegistryBoxes(
                        "XDOC-301", "XDOC-302", "XDOC-303",
                        "XDOC-401", "XDOC-402", "XDOC-403"
                )
                .buildRegistry()
                .sortToAvailableCell(
                        "XDOC-001", "XDOC-002",
                        "XDOC-101", "XDOC-102",
                        "XDOC-201", "XDOC-202",
                        basket3.getRequiredBarcodeOrThrow(), basket4.getRequiredBarcodeOrThrow()
                )
                .prepareToShip(
                        "XDOC-101", "XDOC-102",
                        "XDOC-201", "XDOC-202",
                        basket3.getRequiredBarcodeOrThrow()
                )
                .and()
                .getOutbound("out1");

        // добавляем лишнюю палету в отгрузку
        Sortable plannedSortable = sortableQueryService.findOrThrow(sortingCenter, "XDOC-101");

        Sortable unplannedSortable = sortableQueryService.findOrThrow(sortingCenter, unplannedBarcode);
        unplannedSortable.setMutableState(unplannedSortable.getMutableState()
                .withOutbound(plannedSortable.getOutbound())
                .withOutRoute(plannedSortable.getOutRoute())
        );
        sortableRepository.save(unplannedSortable);

        flow.sortToAvailableCell(unplannedBarcode);
        flow.prepareLot(unplannedBarcode);

        // вызов стейта отгрузки
        OutboundDiffDto actual = caller.preShipState(outbound.getExternalId())
                .andExpect(status().isOk())
                .getResponseAsClass(OutboundDiffDto.class);

        assertThat(actual)
                .isEqualTo(new OutboundDiffDto(
                        outbound.getExternalId(),
                        List.of(
                                new SortableDiffDto(
                                        "XDOC-001",
                                        SortableType.XDOC_PALLET,
                                        "Зп-3700",
                                        SortableDiffStatusDto.ABSENCE
                                ),
                                new SortableDiffDto(
                                        "XDOC-002",
                                        SortableType.XDOC_PALLET,
                                        "Зп-3700",
                                        SortableDiffStatusDto.ABSENCE
                                ),
                                new SortableDiffDto(
                                        unplannedBarcode,
                                        SortableType.XDOC_PALLET,
                                        unplannedInfoListCode,
                                        SortableDiffStatusDto.EXTRA
                                ),
                                new SortableDiffDto(
                                        basket4.getRequiredBarcodeOrThrow(),
                                        SortableType.XDOC_BASKET,
                                        null,
                                        SortableDiffStatusDto.ABSENCE
                                )
                        ),
                        "Нужно запросить документы!!!"
                ));
    }

    @Test
    @DisplayName("Завершение отгрузки. Перевод в статус shipped")
    void shipXDocOutbounds() {
        outbound = flow.createInbound("in1")
                .linkPallets("XDOC-1", "XDOC-2")
                .fixInbound()
                .createOutbound("out1")
                .externalId("reg1")
                .buildRegistry("XDOC-1", "XDOC-2")
                .sortToAvailableCell("XDOC-1", "XDOC-2")
                .prepareToShip("XDOC-1", "XDOC-2")
                .and()
                .getOutbound("out1");
        caller.shipOutbound(outbound.getExternalId())
                .andExpect(status().isOk());

        Assertions.assertThat(flow.getOutbound(outbound.getExternalId()).getStatus()).isEqualTo(OutboundStatus.SHIPPED);
    }

    @Test
    @DisplayName("Завершение отгрузки с комментарием. Перевод в статус shipped")
    void shipXDocOutboundWithComment() {
        outbound = flow.createInbound("in1")
                .linkPallets("XDOC-1")
                .fixInbound()
                .createOutbound("out1")
                .externalId("reg1")
                .buildRegistry("XDOC-1")
                .sortToAvailableCell("XDOC-1")
                .prepareToShip("XDOC-1")
                .and()
                .getOutbound("out1");
        caller.shipOutboundV2(
                        outbound.getExternalId(), new PartnerShipOutboundDto("Все ок - эта палета не поедет"))
                .andExpect(status().isOk());

        Assertions.assertThat(flow.getOutbound(outbound.getExternalId()).getStatus()).isEqualTo(OutboundStatus.SHIPPED);
    }

    @Disabled
    @Test
    @DisplayName("Завершение отгрузки вида СЦ - СЦ")
    void shipScToScOutbound() {
        // ToDo добавить тест на отгрузку 1 коробки в нескольких лотов в рамках отдного outbound'а
        SortingCenter scTo = testFactory.storedSortingCenter(
                TestFactory.SortingCenterParams.builder()
                        .id(75001)
                        .yandexId("9999998881")
                        .build()
        );

        Outbound outbound = testFactory.createOutbound(
                "1001",
                OutboundStatus.CREATED,
                OutboundType.DS_SC,
                clock.instant(),
                clock.instant(),
                scTo.getYandexId(),
                sortingCenter,
                testFactory.storedMovementCourier(1111L)
        );

        User user = testFactory.storedUser(sortingCenter, 124L);
        testFactory.setSortingCenterProperty(sortingCenter.getId(),
                SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, true);
        testFactory.increaseScOrderId();

        var courier1 = testFactory.storedCourier(1L);
        var order = testFactory.createForToday(
                order(sortingCenter, "1").places("11", "12").build()
        ).updateCourier(courier1).acceptPlaces("11", "12").sortPlaces("11", "12").get();

        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var cell = testFactory.determineRouteCell(route, order);
        var lot = testFactory.storedLot(sortingCenter, cell, LotStatus.CREATED);
        testFactory.sortToLot(order, "11", lot, user);
        testFactory.sortToLot(order, "12", lot, user);
        testFactory.prepareToShipLot(lot);

        // TODO mock связи outbound-sortable-lot поменять на реальное создание связи
        Sortable lotSo = sortableRepository.findByIdOrThrow(lot.getSortableId());
        lotSo.setMutableState(
                lotSo.getMutableState()
                        .withStatus(SortableStatus.SHIPPED_DIRECT)
                        .withOutbound(outbound));
        sortableRepository.save(lotSo);

        caller.shipOutbound(outbound.getExternalId())
                .andExpect(status().isOk());

        caller.shipOutbound(outbound.getExternalId())
                .andExpect(status().is4xxClientError());

        List<Registry> registries = registryRepository.findAllByOutboundId(outbound.getId());
        assertThat(registries)
                .hasSize(1)
                .allMatch(reg -> reg.getType() == RegistryType.FACTUAL);

        List<RegistrySortable> regSortables = registrySortableRepository.findAllByRegistryIn(registries);
        assertThat(regSortables.stream().map(RegistrySortable::getSortableExternalId).toList())
                .containsExactlyInAnyOrder(lot.getBarcode());

        // TODO реестры для ордеров и плейсов
    }

    private List<OutboundPartnerDto> createOutbounds(int numberOfOutbounds) {
        List<OutboundPartnerDto> result = new ArrayList<>();
        for (int i = 0; i < numberOfOutbounds; i++) {
            Cell cell = testFactory.storedCell(sortingCenter, "cell_" + i, CellType.COURIER, CellSubType.SHIP_XDOC);
            Outbound outbound = flow.createInbound("in1" + i)
                    .linkPallets("XDOC-1" + i)
                    .fixInbound()
                    .createOutbound("1000000" + i)
                    .externalId("reg1" + i)
                    .buildRegistry("XDOC-1" + i)
                    .sortToAvailableCell("XDOC-1" + i)
                    .prepareToShip("XDOC-1" + i)
                    .and()
                    .getOutbound("1000000" + i);
            Inbound inbound = flow.getInbound("in1" + i);
            result.add(getOutboundPartnerDto(sortableQueryService.find(outbound),
                    OutboundInternalStatusDto.READY_TO_SHIP, outbound, Set.of(inbound.getExternalId()),
                    new HashSet<>(),
                    Set.of(inbound.getExternalId()),
                    Set.of(Objects.requireNonNull(cell.getScNumber())))
            );
        }
        return result;
    }

    private OutboundPartnerDto getOutboundPartnerDto(
            Set<Sortable> pallets, OutboundInternalStatusDto status, Outbound outbound, Set<String> plannedInbounds,
            Set<String> sortedInbounds, Set<String> preparedInbounds, Set<String> cells
    ) {
        long sortedBoxes = pallets.stream()
                .filter(sortable -> sortable.getType() == SortableType.XDOC_BOX)
                .filter(sortable -> sortable.getStatus() == SortableStatus.SORTED_DIRECT)
                .count();
        long preparedBoxes = pallets.stream()
                .filter(sortable -> sortable.getType() == SortableType.XDOC_BOX)
                .filter(sortable -> sortable.getStatus() == SortableStatus.PREPARED_DIRECT ||
                        sortable.getStatus() == SortableStatus.SHIPPED_DIRECT)
                .count();
        long plannedBoxes = pallets.stream()
                .filter(sortable -> sortable.getType() == SortableType.XDOC_BOX)
                .count();
        Set<Sortable> plannedPallets = pallets.stream()
                .filter(sortable -> sortable.getType() == SortableType.XDOC_PALLET ||
                        sortable.getType() == SortableType.XDOC_BASKET)
                .collect(Collectors.toSet());
        Set<Sortable> sortPallets = pallets.stream()
                .filter(sortable -> sortable.getType() == SortableType.XDOC_PALLET ||
                        sortable.getType() == SortableType.XDOC_BASKET)
                .filter(sortable -> sortable.getStatus() == SortableStatus.SORTED_DIRECT)
                .collect(Collectors.toSet());
        Set<Sortable> preparedPallets = pallets.stream()
                .filter(sortable -> sortable.getType() == SortableType.XDOC_PALLET ||
                        sortable.getType() == SortableType.XDOC_BASKET)
                .filter(sortable -> sortable.getStatus() == SortableStatus.PREPARED_DIRECT ||
                        sortable.getStatus() == SortableStatus.SHIPPED_DIRECT)
                .collect(Collectors.toSet());
        return OutboundPartnerDto.builder()
                .id(outbound.getId())
                .outboundExternalId(outbound.getExternalId())
                .factualBoxesAmount(preparedBoxes)
                .plannedBoxesAmount(plannedBoxes)
                .sortedBoxesAmount(sortedBoxes)
                .factualPalletAmount(preparedPallets.size())
                .plannedPalletAmount(plannedPallets.size())
                .sortedPalletAmount(sortPallets.size())
                .inboundsInformation(
                        InboundsInformation.builder()
                                .plannedPallet(pallets.stream()
                                        .map(Sortable::getRequiredBarcodeOrThrow)
                                        .collect(Collectors.toSet()))
                                .sortedPallet(sortPallets.stream()
                                        .map(Sortable::getRequiredBarcodeOrThrow)
                                        .collect(Collectors.toSet()))
                                .preparedPallet(preparedPallets.stream()
                                        .map(Sortable::getRequiredBarcodeOrThrow)
                                        .collect(Collectors.toSet()))
                                .cells(cells)
                                .inboundsPlanned(InboundWithPalletsPartnerDtoList.builder()
                                        .inboundsWithBaskets(pallets.stream()
                                                .filter(sortable -> sortable.getType() == SortableType.XDOC_BASKET)
                                                .map(sortable -> PalletPartnerDto.builder()
                                                        .barcode(sortable.getRequiredBarcodeOrThrow())
                                                        .inboundIds(plannedInbounds)
                                                        .build()
                                                )
                                                .toList()
                                        )
                                        .build())
                                .inboundsSorted(InboundWithPalletsPartnerDtoList.builder()
                                        .inboundsWithBaskets(sortPallets.stream()
                                                .filter(sortable -> sortable.getType() == SortableType.XDOC_BASKET)
                                                .map(sortable -> PalletPartnerDto.builder()
                                                        .barcode(sortable.getRequiredBarcodeOrThrow())
                                                        .inboundIds(sortedInbounds)
                                                        .build()
                                                )
                                                .toList()
                                        )
                                        .build())
                                .inboundsPrepared(InboundWithPalletsPartnerDtoList.builder()
                                        .inboundsWithBaskets(preparedPallets.stream()
                                                .filter(sortable -> sortable.getType() == SortableType.XDOC_BASKET)
                                                .map(sortable -> PalletPartnerDto.builder()
                                                        .barcode(sortable.getRequiredBarcodeOrThrow())
                                                        .inboundIds(preparedInbounds)
                                                        .build()
                                                )
                                                .toList()
                                        )
                                        .build())
                                .build()
                )

                .fromTime(LocalDateTime.ofInstant(outbound.getFromTime(), clock.getZone()))
                .toTime(LocalDateTime.ofInstant(outbound.getToTime(), clock.getZone()))
                .carNumber("A777MP77")
                .destination(testFactory.findWarehouseBy().getIncorporation())
                .status(status)
                .actions(List.of())
                .docs(new OutboundDocsDto(OutboundDocsStatus.NOT_REQUESTED, Collections.emptyList()))
                .build();
    }

    @Test
    void shipEmptyOutbound() {
        flow.createInbound("in-1")
                .linkPallets("XDOC-1")
                .fixInbound()
                .createOutbound("out-1")
                .buildRegistry("XDOC-1")
                .sortToAvailableCell("XDOC-1");

        caller.shipOutbound("out-1")
                .andExpect(status().is4xxClientError())
                .andExpect(content().json("""
                            {"message": "Невозможно отгрузить outbound out-1: для отгрузки не подготовлено ни одно грузоместо"}
                        """));

    }

    private String getExpectedResponse(OutboundPartnerDto... outboundPartnerDtos) {
        int defaultPageSize = 20;
        int defaultPageNumber = 0;
        return getExpectedResponse(defaultPageSize, defaultPageNumber, outboundPartnerDtos);
    }

    private String getExpectedResponse(int size, int page, OutboundPartnerDto... outboundPartnerDtos) {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (int i = size * page; i < size * (page + 1) && i < outboundPartnerDtos.length; i++) {
            sb.append(getExpectedOutboundPartnerDto(outboundPartnerDtos[i])).append(",");
            count++;
        }
        // Справка по индексам
//                "content": [%1$s],
//                "totalPages": %2$s,
//                "totalElements": %3$s,
//                "empty": %4$s,
//                "size": %5$s
//                "numberOfElements": %6$s,
//                "last": %7$s,
//                "first": %8$s,
//                "pageNumber": %9$s,
//                "offset": %10$s,
//                "number": %11$s,

        int totalPages = outboundPartnerDtos.length == 0 ? 0 : ((outboundPartnerDtos.length - 1) / size) + 1;
        return String.format(
                EXPECTED_LIST_RESPONSE_FORMAT,
                sb.length() > 0 ? sb.substring(0, sb.length() - 1) : sb.toString(),
                totalPages,
                outboundPartnerDtos.length,
                sb.length() == 0,
                size,
                count,
                page >= (totalPages - 1),
                page == 0,
                page,
                page * size,
                page
        );
    }

    @SneakyThrows
    private StringBuilder getExpectedOutboundPartnerDto(OutboundPartnerDto outboundPartnerDto) {
        StringBuilder sb = new StringBuilder();
        var a = outboundPartnerDto.getDocs().getLinks().toString();
        return sb.append(
                String.format(EXPECTED_DTO_RESPONSE_FORMAT,
                        outboundPartnerDto.getOutboundExternalId(),
                        objectMapper.writeValueAsString(outboundPartnerDto.getFromTime()),
                        objectMapper.writeValueAsString(outboundPartnerDto.getToTime()),
                        outboundPartnerDto.getPlannedBoxesAmount(),
                        outboundPartnerDto.getPlannedPalletAmount(),
                        outboundPartnerDto.getFactualBoxesAmount(),
                        outboundPartnerDto.getFactualPalletAmount(),
                        outboundPartnerDto.getSortedBoxesAmount(),
                        outboundPartnerDto.getSortedPalletAmount(),
                        outboundPartnerDto.getStatus().toString(),
                        outboundPartnerDto.getDocs().getStatus().toString(),
                        outboundPartnerDto.getDocs().getLinks().toString()
                ));
    }

    @Test
    @DisplayName("Получение ТрН outbound'а после отгрузки лотов")
    void getOutboundWaybillDocuments() {
        List<String> documents = List.of("https://s3.yandex.net/trn-outbound-1.xlsx");

        SortingCenter scFrom = testFactory.storedSortingCenter(100L);
        testFactory.setSortingCenterProperty(scFrom.getId(), SortingCenterPropertiesKey.RETURN_OUTBOUND_ENABLED, true);
        var userScFrom = testFactory.storedUser(scFrom, 1001L);
        SortingCenter scTo = testFactory.storedSortingCenter(
                TestFactory.SortingCenterParams.builder()
                        .id(200L)
                        .yandexId("9865")
                        .token("sc_to_token")
                        .partnerName("sc_to_partner_name")
                        .build()
        );

        var outbound = testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("outbound1")
                .type(OutboundType.ORDERS_RETURN)
                .fromTime(clock.instant())
                .toTime(clock.instant())
                .sortingCenter(scFrom)
                .logisticPointToExternalId(scTo.getYandexId())
                .build()
        );

        var deliveryService = testFactory.storedDeliveryService(String.valueOf(scTo.getId()));
        var order = testFactory.createForToday(
                        order(scFrom, "1").places("11", "12")
                                .deliveryService(deliveryService)
                                .dsType(DeliveryServiceType.TRANSIT)
                                .build())
                .acceptPlaces("11", "12").sortPlaces("11", "12").ship()
                .makeReturn().accept().sort().get();
        var route = testFactory.findOutgoingRoute(order).orElseThrow();
        var cell = testFactory.determineRouteCell(route, order);
        var lot = testFactory.storedLot(scFrom, cell, LotStatus.CREATED);
        testFactory.sortToLot(order, "11", lot, userScFrom);
        testFactory.sortToLot(order, "12", lot, userScFrom);
        testFactory.prepareToShipLot(lot);
        testFactory.shipLots(testFactory.getRouteIdForSortableFlow(route), sortingCenter);
        // TODO пришли ТрН документы из TM
        outboundTrnDocsService.saveDocuments(outbound, documents);

        var actualPage = outboundFacade.getPartnerOutbounds(LocalDate.now(clock), LocalDate.now(clock), "",
                outbound.getSortingCenter(), testFactory.getRouteIdForSortableFlow(route), PageRequest.of(0, 20));
        var actual = actualPage.stream().findFirst().orElseThrow();

        assertThat(actualPage.getTotalElements()).isEqualTo(1);
        assertThat(actual.getTrnDocs()).isEqualTo(new OutboundTrnDocsDto(documents));
    }
}
