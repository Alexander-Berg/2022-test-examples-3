package ru.yandex.market.sc.core.domain.outbound;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.delivery_service.model.DeliveryServiceType;
import ru.yandex.market.sc.core.domain.inbound.repository.Inbound;
import ru.yandex.market.sc.core.domain.inbound.repository.InboundRegistryOrderStatus;
import ru.yandex.market.sc.core.domain.lot.repository.LotStatus;
import ru.yandex.market.sc.core.domain.outbound.model.OutboundIdentifier;
import ru.yandex.market.sc.core.domain.outbound.model.OutboundIdentifierType;
import ru.yandex.market.sc.core.domain.outbound.model.OutboundInternalStatusDto;
import ru.yandex.market.sc.core.domain.outbound.model.OutboundType;
import ru.yandex.market.sc.core.domain.outbound.model.partner.OutboundPartnerDto;
import ru.yandex.market.sc.core.domain.outbound.repository.Outbound;
import ru.yandex.market.sc.core.domain.outbound.repository.OutboundStatus;
import ru.yandex.market.sc.core.domain.sortable.SortableQueryService;
import ru.yandex.market.sc.core.domain.sortable.repository.Sortable;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.resolver.dto.ScContext;
import ru.yandex.market.sc.core.test.DefaultScUserWarehouseExtension;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.core.util.flow.xdoc.XDocFlow;
import ru.yandex.market.tpl.common.util.exception.TplIllegalArgumentException;
import ru.yandex.market.tpl.common.web.exception.TplInvalidActionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.market.sc.core.test.TestFactory.order;

@Transactional
@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@ExtendWith(DefaultScUserWarehouseExtension.class)
class OutboundFacadeTest {

    private final TestFactory testFactory;
    private final TransactionTemplate transactionTemplate;
    private final OutboundFacade outboundFacade;
    private final SortableQueryService sortableQueryService;
    private final XDocFlow flow;

    private SortingCenter sortingCenter;

    @MockBean
    Clock clock;

    @BeforeEach
    void init() {
        testFactory.setupMockClock(clock);
        sortingCenter = testFactory.storedSortingCenter();
    }

    @Test
    void shouldReturnNumberOfPlannedSortableBoxes() {
        Outbound outbound = flow.createInbound("IN1")
                .linkBoxes(3)
                .fixInbound()
                .createOutbound("OUT1")
                .addRegistryBoxes(3)
                .buildRegistry()
                .and()
                .getOutbound("OUT1");
        assertThat(outboundFacade.getPlannedForOutbound(outbound)).hasSize(3);
    }

    @Test
    void shouldReturnWarehouseNameByOutbound() {
        Outbound outbound = flow.createInbound("IN1")
                .linkBoxes(3)
                .fixInbound()
                .createOutbound("OUT1")
                .addRegistryBoxes(3)
                .buildRegistry()
                .and()
                .getOutbound("OUT1");
        assertThat(outboundFacade.getWarehouseNameForOutbound(outbound))
                .isEqualTo(TestFactory.warehouse().getIncorporation());
    }

    @Test
    void shouldReturnSetOfInboundsForOutbound() {
        Outbound outbound = flow.createInbound("IN1")
                .linkPallets(3)
                .fixInbound()
                .createOutbound("OUT1")
                .addRegistryPallets(3)
                .buildRegistry()
                .and()
                .getOutbound("OUT1");
        Inbound inbound = flow.getInbound("IN1");
        var inbounds = outboundFacade.getInboundsForOutbound(outbound);
        assertThat(inbounds).contains(inbound).hasSize(1);
    }

    @Test
    void shouldReturnEmptySetOfInbounds() {
        Outbound outbound = flow.createInbound("IN1")
                .linkPallets("XDOC-SOME-PALLET")
                .fixInbound()
                .createInbound("IN2")
                .linkPallets(3)
                .fixInbound()
                .createOutbound("OUT1")
                .addRegistryPallets(3)
                .buildRegistry()
                .and()
                .getOutbound("OUT1");
        Inbound secondInbound = flow.getInbound("IN1");

        var inbounds = outboundFacade.getInboundsForOutbound(outbound);
        assertThat(inbounds).doesNotContain(secondInbound).hasSize(1);
    }

    @Test
    void shouldVerifyCorrectnessOfOutbound() {

        Outbound outbound = flow.createInbound("IN1")
                .linkPallets("XDOC-1")
                .fixInbound()
                .createOutbound("OUT1")
                .addRegistryPallets("XDOC-1")
                .buildRegistry()
                .sortToAvailableCell("XDOC-1")
                .prepareToShip("XDOC-1")
                .and()
                .getOutbound("OUT1");
        Inbound inbound = flow.getInbound("IN1");

        assertThat(outboundFacade.isOutboundValid(outbound, Set.of(inbound))).isTrue();

    }

    @Test
    void shouldVerifyIncorrectnessOfOutbound() {

        transactionTemplate.executeWithoutResult(status -> {
            Outbound outbound = flow.createInbound("IN1")
                    .linkPallets("XDOC-1", "XDOC-2")
                    .fixInbound()
                    .createOutbound("OUT1")
                    .addRegistryPallets("XDOC-1", "XDOC-2")
                    .buildRegistry()
                    .sortToAvailableCell("XDOC-1", "XDOC-2")
                    .prepareToShip("XDOC-1")
                    .and()
                    .getOutbound("OUT1");
            Inbound inbound = flow.getInbound("IN1");
            assertThat(outboundFacade.isOutboundValid(outbound, Set.of(inbound))).isFalse();
        });
    }

    @Test
    void shouldReturnAmountOfPalletsForOutbound() {
        Outbound outbound = flow.createInbound("IN1")
                .linkPallets(3)
                .fixInbound()
                .createOutbound("OUT1")
                .addRegistryPallets(3)
                .buildRegistry()
                .and()
                .getOutbound("OUT1");
        assertThat(outboundFacade.getPlannedForOutbound(outbound)).hasSize(3);
    }

    @Test
    public void shouldReturnOutboundPartnerStatusDtoShipped() {
        Outbound outbound = flow.createInbound("IN1")
                .linkPallets("XDOC-1")
                .fixInbound()
                .createOutbound("OUT1")
                .addRegistryPallets("XDOC-1")
                .buildRegistry()
                .sortToAvailableCell("XDOC-1")
                .prepareToShip("XDOC-1")
                .shipAndGet("OUT1");

        OutboundInternalStatusDto outboundInternalStatusDto = OutboundFacade
                .getPartnerStatus(outbound, true, true);

        assertThat(outboundInternalStatusDto).isEqualTo(OutboundInternalStatusDto.SHIPPED);
    }

    @Test
    public void shouldReturnOutboundPartnerStatusDtoNotFullyPrepared() {
        Outbound outbound = flow.createInbound("IN1")
                .linkPallets("XDOC-1", "XDOC-2")
                .fixInbound()
                .createOutbound("OUT1")
                .addRegistryPallets("XDOC-1", "XDOC-2")
                .buildRegistry()
                .sortToAvailableCell("XDOC-1", "XDOC-2")
                .prepareToShip("XDOC-1")
                .and()
                .getOutbound("OUT1");

        OutboundInternalStatusDto outboundInternalStatusDto = OutboundFacade
                .getPartnerStatus(outbound, false, true);

        assertThat(outboundInternalStatusDto).isEqualTo(OutboundInternalStatusDto.NOT_FULLY_PREPARED);
    }

    @Test
    public void shouldReturnOutboundPartnerStatusDtoNotStarted() {
        Outbound outbound = flow.createInbound("IN1")
                .linkPallets("XDOC-1", "XDOC-2")
                .fixInbound()
                .createOutbound("OUT1")
                .addRegistryPallets("XDOC-1", "XDOC-2")
                .buildRegistry()
                .sortToAvailableCell("XDOC-1", "XDOC-2")
                .and()
                .getOutbound("OUT1");

        OutboundInternalStatusDto outboundInternalStatusDto = OutboundFacade
                .getPartnerStatus(outbound, true, false);

        AssertionsForClassTypes.assertThat(outboundInternalStatusDto).isEqualTo(OutboundInternalStatusDto.NOT_STARTED);
    }

    @Test
    public void shouldReturnOutboundPartnerStatusDtoReadyToShip() {
        Outbound outbound = flow.createInbound("IN1")
                .linkPallets("XDOC-1")
                .fixInbound()
                .createOutbound("OUT1")
                .addRegistryPallets("XDOC-1")
                .buildRegistry()
                .sortToAvailableCell("XDOC-1")
                .prepareToShip("XDOC-1")
                .and()
                .getOutbound("OUT1");

        OutboundInternalStatusDto outboundInternalStatusDto = OutboundFacade
                .getPartnerStatus(outbound, true, true);

        assertThat(outboundInternalStatusDto).isEqualTo(OutboundInternalStatusDto.READY_TO_SHIP);
    }

    @Test
    public void shouldReturnCancelledBySc() {
        Outbound outbound = getOutboundTest(OutboundStatus.CANCELLED_BY_SC);

        OutboundInternalStatusDto outboundInternalStatusDto = OutboundFacade
                .getPartnerStatus(outbound, true, true);

        assertThat(outboundInternalStatusDto).isEqualTo(OutboundInternalStatusDto.CANCELLED_BY_SC);

    }

    @Test
    public void shouldReturnError() {
        Outbound outbound = getOutboundTest(OutboundStatus.ERROR);

        OutboundInternalStatusDto outboundInternalStatusDto = OutboundFacade
                .getPartnerStatus(outbound, true, true);

        assertThat(outboundInternalStatusDto).isEqualTo(OutboundInternalStatusDto.ERROR);
    }

    @Test
    public void shouldReturnUnknown() {
        Outbound outbound = getOutboundTest(OutboundStatus.UNKNOWN);

        OutboundInternalStatusDto outboundInternalStatusDto = OutboundFacade
                .getPartnerStatus(outbound, true, true);

        assertThat(outboundInternalStatusDto).isEqualTo(OutboundInternalStatusDto.UNKNOWN);
    }

    @Test
    public void shouldReturnInbounds() {
        Sortable basket = flow.createBasketAndGet();

        var outbound = flow.createInbound("IN1")
                .linkBoxes("XDOC-1")
                .fixInbound()
                .createOutbound("OUT1")
                .addRegistryBoxes("XDOC-1")
                .buildRegistry(basket.getRequiredBarcodeOrThrow())
                .sortToAvailableLot("XDOC-1")
                .sortToAvailableCell(basket.getRequiredBarcodeOrThrow())
                .prepareToShip().and().getOutbound("OUT1");
        Inbound inbound = flow.getInbound("IN1");

        assertThat(outboundFacade.getInboundsForOutbound(outbound))
                .hasSize(1)
                .contains(inbound);

    }

    @Test
    void addInboundToOutbound() {
        Cell cell = flow.createShipCellAndGet("SHIP-1");
        flow.createInbound("in-1")
                .linkPallets("XDOC-1")
                .fixInbound()
                .createOutbound("out-1")
                .buildRegistry("XDOC-1")
                .sortToAvailableCell("XDOC-1");

        flow.createInbound("in-2")
                .linkPallets("XDOC-2")
                .fixInbound();

        outboundFacade.addInboundsToOutbound(flow.getSortingCenter().getId(), "out-1", List.of("in-2"));

        flow.sortToAvailableCell("XDOC-2");
        Sortable sortable = sortableQueryService.find(flow.getSortingCenter(), "XDOC-2").orElseThrow();
        assertThat(sortable.getCellIdOrNull()).isEqualTo(cell.getId());
    }

    @Test
    void addInboundsToOutbound() {
        Cell cell = flow.createShipCellAndGet("SHIP-1");
        flow.createInbound("in-1")
                .linkPallets("XDOC-1")
                .fixInbound()
                .createOutbound("out-1")
                .buildRegistry("XDOC-1")
                .sortToAvailableCell("XDOC-1");

        flow.createInbound("in-2")
                .linkPallets("XDOC-2")
                .fixInbound()
                .createInbound("in-3")
                .linkPallets("XDOC-3")
                .fixInbound();

        outboundFacade.addInboundsToOutbound(flow.getSortingCenter().getId(), "out-1", List.of("in-2", "in-3"));

        flow.sortToAvailableCell("XDOC-2");
        flow.sortToAvailableCell("XDOC-3");
        Sortable sortable = sortableQueryService.find(flow.getSortingCenter(), "XDOC-2").orElseThrow();
        Sortable sortable2 = sortableQueryService.find(flow.getSortingCenter(), "XDOC-3").orElseThrow();
        assertThat(sortable.getCellIdOrNull()).isEqualTo(cell.getId());
        assertThat(sortable2.getCellIdOrNull()).isEqualTo(cell.getId());
    }

    @Test
    void addInboundUnfixedToOutbound() {
        Cell cell = flow.createShipCellAndGet("SHIP-1");
        flow.createInbound("in-1")
                .linkPallets("XDOC-1")
                .fixInbound()
                .createOutbound("out-1")
                .buildRegistry("XDOC-1")
                .sortToAvailableCell("XDOC-1");

        flow.createInbound("in-2")
                .linkPallets("XDOC-2")
                .fixInbound()
                .createInbound("in-3")
                .linkPallets("XDOC-3");

        assertThatThrownBy(
                () -> outboundFacade.addInboundsToOutbound(
                        flow.getSortingCenter().getId(),
                        "out-1",
                        List.of("in-2", "in-3")
                )
        )
                .isInstanceOf(TplInvalidActionException.class)
                .hasMessage("Some inbounds are not fixed");

        flow.sortToAvailableCell("XDOC-2");
        flow.sortToAvailableCell("XDOC-3");
        Sortable sortable = sortableQueryService.find(flow.getSortingCenter(), "XDOC-2").orElseThrow();
        Sortable sortable2 = sortableQueryService.find(flow.getSortingCenter(), "XDOC-3").orElseThrow();
        assertThat(sortable.getCellIdOrNull()).isNotEqualTo(cell.getId());
        assertThat(sortable2.getCellIdOrNull()).isNotEqualTo(cell.getId());
    }

    @Test
    void addBasketToOutbound() {
        testFactory.storedWarehouse();
        var cell = flow.createShipCellAndGet("SHIP-1");
        var bufferCell = flow.createBufferCellAndGet("cell-1", TestFactory.WAREHOUSE_YANDEX_ID);
        var lot = flow.createBasket(bufferCell);
        flow.createInbound("in-1")
                .linkPallets("XDOC-1")
                .fixInbound()
                .createOutbound("out-1")
                .buildRegistry("XDOC-1")
                .sortToAvailableCell("XDOC-1");

        flow.createInbound("in-2")
                .linkBoxes("XDOC-2")
                .fixInbound()
                .createInbound("in-3")
                .linkBoxes("XDOC-3")
                .fixInbound();

        Sortable box1 = sortableQueryService.find(flow.getSortingCenter(), "XDOC-2").orElseThrow();
        Sortable box2 = sortableQueryService.find(flow.getSortingCenter(), "XDOC-3").orElseThrow();

        flow.sortBoxToLot(box1, lot);
        flow.sortBoxToLot(box2, lot);

        Sortable basket = sortableQueryService.find(lot.getSortableId()).orElseThrow();

        flow.packLot(basket.getRequiredBarcodeOrThrow());

        outboundFacade.addBasketToOutbound(flow.getSortingCenter().getId(), "out-1",
                basket.getRequiredBarcodeOrThrow());

        flow.sortToAvailableCell(basket.getRequiredBarcodeOrThrow());
        box1 = sortableQueryService.find(flow.getSortingCenter(), "XDOC-2").orElseThrow();
        box2 = sortableQueryService.find(flow.getSortingCenter(), "XDOC-3").orElseThrow();
        basket = sortableQueryService.find(lot.getSortableId()).orElseThrow();
        assertThat(basket.getCellIdOrNull()).isEqualTo(cell.getId());
        assertThat(box1.getOutbound().getExternalId()).isEqualTo("out-1");
        assertThat(box2.getOutbound().getExternalId()).isEqualTo("out-1");
    }

    @DisplayName("Статус отгрузки по схеме ОПКО СЦ - СЦ с маппингом групп партнеров")
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void outboundStatusWithPartnerGroupMapping() {
        testFactory.setSortingCenterProperty(flow.getSortingCenter().getId(),
                SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, true);
        testFactory.setSortingCenterProperty(flow.getSortingCenter().getId(),
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS, true);
        testFactory.setSortingCenterProperty(flow.getSortingCenter().getId(),
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS_FOR_ALL_DELIVERY_SERVICES, true);
        testFactory.increaseScOrderId();

        var virtualPartnerId = 55555L;
        var existingPartnerId = 75008L;

        var existingPartnerYandexId = "10002222222";

        testFactory.storedSortingCenter(
                TestFactory.SortingCenterParams.builder()
                        .id(existingPartnerId)
                        .yandexId(existingPartnerYandexId)
                        .token("existing_sc_to_token")
                        .partnerName("existing_sc_to_partner_name")
                        .build()
        );

        testFactory.storedPartnerMappingGroup(virtualPartnerId, existingPartnerId);

        var outbound = testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("outbound1")
                .type(OutboundType.DS_SC)
                .fromTime(clock.instant().minus(1, ChronoUnit.HOURS))
                .toTime(clock.instant().plus(1, ChronoUnit.HOURS))
                .sortingCenter(flow.getSortingCenter())
                .logisticPointToExternalId(existingPartnerYandexId)
                .partnerToExternalId("55555")
                .build()
        );

        var virtualScRouteOutbound = createOrderAndBindToOutbound(virtualPartnerId, outbound);
        var existingScRouteOutbound = createOrderAndBindToOutbound(existingPartnerId, outbound);

        assertThat(virtualScRouteOutbound.getContent().size()).isEqualTo(1);
        assertThat(existingScRouteOutbound.getContent().size()).isEqualTo(1);

        assertThat(virtualScRouteOutbound.getContent().get(0).getOutboundExternalId())
                .isEqualTo(existingScRouteOutbound.getContent().get(0).getOutboundExternalId());

        outboundFacade.shipOutboundByPIAdmin(virtualScRouteOutbound.getContent().get(0).getOutboundExternalId(), null);

        var registryOrdersByRegistryExternalId = testFactory.getRegistryOrdersByRegistryExternalId(
                testFactory.getRegistryByOutboundId(outbound.getId()).get(0).getId()
        );
        assertThat(registryOrdersByRegistryExternalId).hasSize(2);
        assertThat(registryOrdersByRegistryExternalId).allMatch(a -> a.getStatus() == InboundRegistryOrderStatus.FIXED);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("Привязка лота к отгрузке другого маршрута")
    void bindLotsToWrongOutbound() {
        testFactory.setSortingCenterProperty(flow.getSortingCenter().getId(),
                SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, true);
        testFactory.setSortingCenterProperty(flow.getSortingCenter().getId(),
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS, true);
        testFactory.setSortingCenterProperty(flow.getSortingCenter().getId(),
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS_FOR_ALL_DELIVERY_SERVICES, true);

        testFactory.storedSortingCenter(TestFactory.SortingCenterParams.builder().id(777).yandexId("correctDestination")
                .token("correctDestination").partnerName("correctDestination").build());
        testFactory.storedSortingCenter(TestFactory.SortingCenterParams.builder().id(888).yandexId("wrongDestination")
                .token("wrongDestination").partnerName("wrongDestination").build());

        var correctOb = testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("correctDestinationOutbound")
                .type(OutboundType.DS_SC)
                .fromTime(clock.instant().minus(1, ChronoUnit.HOURS))
                .toTime(clock.instant().plus(1, ChronoUnit.HOURS))
                .sortingCenter(flow.getSortingCenter())
                .logisticPointToExternalId("correctDestination")
                .build()
        );

        var wrongOb = testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("wrongDestinationOutbound")
                .type(OutboundType.DS_SC)
                .fromTime(clock.instant())
                .toTime(clock.instant())
                .sortingCenter(flow.getSortingCenter())
                .logisticPointToExternalId("wrongDestination")
                .build()
        );

        var correctDeliveryService = testFactory.storedDeliveryService("777");
        var order = testFactory.createForToday(order(flow.getSortingCenter(), "o777")
                .deliveryService(correctDeliveryService).dsType(DeliveryServiceType.TRANSIT).build()
        ).accept().sort().get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var cell = testFactory.determineRouteCell(route, order);
        var lot = testFactory.storedLot(flow.getSortingCenter(), cell, LotStatus.CREATED);
        testFactory.sortOrderToLot(order, lot, flow.getUser());
        testFactory.prepareToShipLot(lot);

        Assertions.assertThrows(TplIllegalArgumentException.class, () ->
                outboundFacade.bindLotToOutbound(new OutboundIdentifier(OutboundIdentifierType.EXTERNAL_ID,
                                wrongOb.getExternalId()),
                        lot.getBarcode(), testFactory.getRouteIdForSortableFlow(route),
                        new ScContext(flow.getUser(), testFactory.storedSortingCenter()))
        );
        Assertions.assertDoesNotThrow(() ->
                outboundFacade.bindLotToOutbound(new OutboundIdentifier(OutboundIdentifierType.EXTERNAL_ID,
                                correctOb.getExternalId()),
                        lot.getBarcode(), testFactory.getRouteIdForSortableFlow(route),
                        new ScContext(flow.getUser(), testFactory.storedSortingCenter()))
        );
    }

    private Page<OutboundPartnerDto> createOrderAndBindToOutbound(Long partnerId, Outbound outbound) {

        var deliveryService = testFactory.storedDeliveryService(String.valueOf(partnerId));

        var order = testFactory.createForToday(order(flow.getSortingCenter(), "o" + partnerId)
                .deliveryService(deliveryService).dsType(DeliveryServiceType.TRANSIT).build()
        ).accept().sort().get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var cell = testFactory.determineRouteCell(route, order);
        var lot = testFactory.storedLot(flow.getSortingCenter(), cell, LotStatus.CREATED);
        testFactory.sortOrderToLot(order, lot, flow.getUser());
        testFactory.prepareToShipLot(lot);

        outboundFacade.bindLotToOutbound(new OutboundIdentifier(
                        OutboundIdentifierType.EXTERNAL_ID, outbound.getExternalId()), lot.getBarcode(),
                testFactory.getRouteIdForSortableFlow(route), new ScContext(flow.getUser(), testFactory.storedSortingCenter()));
        return outboundFacade.getPartnerOutbounds(LocalDate.now(clock), LocalDate.now(clock), "",
                outbound.getSortingCenter(), testFactory.getRouteIdForSortableFlow(route), PageRequest.of(0, 20));
    }

    private Outbound getOutboundTest(OutboundStatus outboundStatus) {
        Outbound outbound = new Outbound();
        outbound.setId(1L)
                .setType(OutboundType.XDOC)
                .setStatus(outboundStatus)
                .setFromTime(Instant.parse("2020-01-01T00:00:00Z"))
                .setToTime(Instant.parse("2020-01-02T00:00:00Z"))
                .setExternalId("123")
                .setSortingCenter(sortingCenter);
        return outbound;
    }

}
