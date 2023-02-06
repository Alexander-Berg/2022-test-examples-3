package ru.yandex.market.sc.core.domain.inbound;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.sc.core.configuration.ConfigurationProperties;
import ru.yandex.market.sc.core.domain.inbound.model.CreateInboundRegistryOrderRequest;
import ru.yandex.market.sc.core.domain.inbound.model.InboundCreateRequest;
import ru.yandex.market.sc.core.domain.inbound.model.InboundType;
import ru.yandex.market.sc.core.domain.inbound.repository.BoundRegistryRepository;
import ru.yandex.market.sc.core.domain.inbound.repository.Inbound;
import ru.yandex.market.sc.core.domain.inbound.repository.InboundInfo;
import ru.yandex.market.sc.core.domain.inbound.repository.InboundRepository;
import ru.yandex.market.sc.core.domain.inbound.repository.InboundStatus;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistryRepository;
import ru.yandex.market.sc.core.domain.movement_courier.model.MovementCourierRequest;
import ru.yandex.market.sc.core.domain.order.repository.ScOrder;
import ru.yandex.market.sc.core.domain.place.repository.PlaceRepository;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.transfer_act.OnlineTransferActPutTransferService;
import ru.yandex.market.sc.core.domain.transfer_act.TransferActManager;
import ru.yandex.market.sc.core.domain.transfer_act.event.NewTransferActAvailableEvent;
import ru.yandex.market.sc.core.domain.warehouse.model.WarehouseType;
import ru.yandex.market.sc.core.test.DefaultScUserWarehouseExtension;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.core.util.flow.xdoc.XDocFlow;
import ru.yandex.market.tpl.common.db.configuration.ConfigurationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.sc.core.test.TestFactory.order;

/**
 * @author valter
 */
@EmbeddedDbTest
@ExtendWith(DefaultScUserWarehouseExtension.class)
class InboundCommandServiceTest {

    @Autowired
    ConfigurationService configurationService;
    @Autowired
    InboundCommandService inboundCommandService;
    @Autowired
    TestFactory testFactory;
    @Autowired
    Clock clock;
    @Autowired
    XDocFlow flow;
    @Autowired
    InboundRepository inboundRepository;

    @MockBean
    TransferActManager transferActManager;

    SortingCenter sortingCenter;

    @Autowired
    BoundRegistryRepository boundRegistryRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    PlaceRepository placeRepository;

    @Autowired
    RegistryRepository registryRepository;

    @SpyBean
    OnlineTransferActPutTransferService onlineTransferActPutTransferService;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
    }

    @Test
    void createInboundWithoutLocation() {
        String externalId = "Inbound-1";
        Long persistedInboundId = inboundCommandService.putInbound(inboundCreateRequestWithoutLocation(externalId));

        Inbound persistedInbound = testFactory.getInbound(persistedInboundId);

        assertThat(persistedInbound.getExternalId()).isEqualTo(externalId);
    }

    private InboundCreateRequest inboundCreateRequestWithoutLocation(String externalId) {
        return InboundCreateRequest.builder()
                .externalId(externalId)
                .warehouseFromId("wf-1")
                .inboundType(InboundType.CROSSDOCK)
                .fromDate(OffsetDateTime.now(clock))
                .toDate(OffsetDateTime.now(clock))
                .courierRequest(new MovementCourierRequest("movement-1", "name-1", "legal-1", null, 212_85_06L,
                        "phone2345"))
                .locationCreateRequest(null)
                .comment("my comment")
                .sortingCenter(sortingCenter)
                .build();
    }

    @DisplayName("дропшиповая поставка будет в статусе CREATED а не в статусе CONFIRMED")
    @Test
    void dropShipInboundAlwaysInStatusCreatedOnUpdateAndCreate() {
        InboundCreateRequest request = dropShipInboundCreateRequest("in-1");
        request.withConfirmed(false);

        // созданная поставка в статусе CREATED
        inboundCommandService.putInbound(request);
        var inbound = inboundRepository.findByExternalId("in-1").orElseThrow();
        assertThat(inbound)
                .extracting(Inbound::getInboundStatus)
                .isEqualTo(InboundStatus.CREATED);

        // после попытки обновить с confirmed = true остается в статусе CREATED
        inboundCommandService.putInbound(
                request
                        .withConfirmed(true)
        );
        inbound = inboundRepository.findByExternalId("in-1").orElseThrow();
        assertThat(inbound)
                .extracting(Inbound::getInboundStatus)
                .isEqualTo(InboundStatus.CREATED);

        // созданная поставка с confirmed = true при создании, будет в статусе CREATED
        request = dropShipInboundCreateRequest("in-2");

        inboundCommandService.putInbound(
                request
                        .withConfirmed(true)
        );
        inbound = inboundRepository.findByExternalId("in-2").orElseThrow();
        assertThat(inbound)
                .extracting(Inbound::getInboundStatus)
                .isEqualTo(InboundStatus.CREATED);
    }

    private InboundCreateRequest dropShipInboundCreateRequest(String externalId) {
        return InboundCreateRequest.builder()
                .externalId(externalId)
                .warehouseFromId("wf-1")
                .inboundType(InboundType.DS_SC)
                .fromDate(OffsetDateTime.now(clock))
                .toDate(OffsetDateTime.now(clock))
                .courierRequest(new MovementCourierRequest("movement-1", "name-1", "legal-1", null, 212_85_06L,
                        "phone2345"))
                .locationCreateRequest(null)
                .comment("my comment")
                .sortingCenter(sortingCenter)
                .build();
    }

    @ParameterizedTest(name = "Начальный статус XDOC поставки в зависимости от флага confirmed")
    @MethodSource("getConfirmedAndStatus")
    void inboundStatusOnCreation(boolean confirmed, InboundStatus expected) {
        var inbound = flow.inboundBuilder("in-1")
                .confirm(confirmed)
                .build()
                .getInbound();

        assertThat(inbound)
                .extracting(Inbound::getInboundStatus)
                .isEqualTo(expected);
    }

    @ParameterizedTest(name = "update XDOC поставки в зависимости от флага confirmed")
    @MethodSource("getConfirmedAndStatus")
    void inboundStatusOnUpdate(boolean confirmed, InboundStatus expected) {
        var inbound = flow.inboundBuilder("in-1")
                .confirm(false)
                .build()
                .getInbound();

        InboundCreateRequest request = testFactory.from(inbound);
        request = request.withConfirmed(confirmed);
        inboundCommandService.putInbound(request);

        inbound = inboundRepository.findByExternalId("in-1").orElseThrow();
        assertThat(inbound)
                .extracting(Inbound::getInboundStatus)
                .isEqualTo(expected);
    }

    @Test
    void updateRealSuppleirName() {
        var inbound = flow.inboundBuilder("in-1")
                .realSupplierName(null)
                .build()
                .getInbound();

        InboundCreateRequest request = testFactory.from(inbound);
        String realSupplierName = "test-supplier1";
        request = request.withRealSupplierName(realSupplierName);
        inboundCommandService.putInbound(request);

        inbound = inboundRepository.findByExternalId("in-1").orElseThrow();
        assertThat(inbound)
                .extracting(Inbound::getInboundInfo)
                .extracting(InboundInfo::getRealSupplierName)
                .isEqualTo(realSupplierName);

    }

    @Test
    @DisplayName("Отправка ивента о доступном АПП при создании погрузки")
    void createInboundAndSendTransferActAvailableEvent() {
        var inboundExternalId = "in-1";

        configurationService.mergeValue(ConfigurationProperties.SEND_TRANSFER_ACT_TO_YARD_ENABLED, true);

        var inbound = flow.inboundBuilder(inboundExternalId)
                .build().getInbound();

        var request = testFactory.from(inbound);
        inboundCommandService.putInbound(request);

        var argument = ArgumentCaptor.forClass(NewTransferActAvailableEvent.class);

        verify(transferActManager, Mockito.times(1))
                .transferActAvailable(argument.capture());

        var actualEvent = argument.getValue();

        assertThat(actualEvent.getTransferActId()).isEqualTo(InboundMapper.getTransferActNumber(inboundExternalId));
        assertThat(actualEvent.getLogisticPointId()).isEqualTo(inbound.getWarehouseFromId());
        assertThat(actualEvent.getSortingCenterId()).isEqualTo(inbound.getSortingCenter().getId());
    }

    @Test
    void createInboundRegistry_setInboundForPlace() {
        var order = testFactory.createOrderForToday(sortingCenter).get();

        var params = TestFactory.CreateInboundParams
                .builder()
                .fromDate(OffsetDateTime.now(clock))
                .toDate(OffsetDateTime.now(clock))
                .warehouseFromExternalId(order.getWarehouseFrom().getYandexId())
                .sortingCenter(sortingCenter)
                .plainOrders(List.of(new Pair<>(order.getExternalId(), order.getExternalId())))
                .build();

        var inbound = testFactory.createInbound(params);
        assertThat(testFactory.orderPlace(order).getInboundId()).isEqualTo(inbound.getId());
    }

    private static List<Arguments> getConfirmedAndStatus() {
        return List.of(
                Arguments.of(false, InboundStatus.CREATED),
                Arguments.of(true, InboundStatus.CONFIRMED)
        );
    }

    @Test
    @DisplayName("Отмена приёмки")
    void cancel() {
        var params = TestFactory.CreateInboundParams
                .builder()
                .fromDate(OffsetDateTime.now(clock))
                .toDate(OffsetDateTime.now(clock))
                .warehouseFromExternalId("TMU12345")
                .sortingCenter(sortingCenter)
                .build();

        var inbound = testFactory.createInbound(params);
        inboundCommandService.cancel(inbound.getId());

        Inbound actual = testFactory.getInbound(inbound.getId());
        assertThat(actual.getInboundStatus()).isEqualTo(InboundStatus.CANCELLED);
    }

    @Test
    @DisplayName("Отмена приёмки некорректный статус")
    void cancelIncorrectStatus() {
        var params = TestFactory.CreateInboundParams
                .builder()
                .fromDate(OffsetDateTime.now(clock))
                .toDate(OffsetDateTime.now(clock))
                .warehouseFromExternalId("TMU12345")
                .sortingCenter(sortingCenter)
                .build();
        var inbound = testFactory.createInbound(params);
        inbound.setInboundStatus(InboundStatus.ARRIVED);
        inboundRepository.save(inbound);

        assertThatThrownBy(() -> inboundCommandService.cancel(inbound.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Inbound %d can't be cancelled because of uncancelable status or not exists"
                        .formatted(inbound.getId()));

        Inbound actual = testFactory.getInbound(inbound.getId());
        assertThat(actual.getInboundStatus()).isEqualTo(InboundStatus.ARRIVED);
    }

    @Test
    @DisplayName("Отмена отменённой приёмки")
    void cancelTwice() {
        var params = TestFactory.CreateInboundParams
                .builder()
                .fromDate(OffsetDateTime.now(clock))
                .toDate(OffsetDateTime.now(clock))
                .warehouseFromExternalId("TMU12345")
                .sortingCenter(sortingCenter)
                .build();
        var inbound = testFactory.createInbound(params);
        inbound.setInboundStatus(InboundStatus.CANCELLED_BY_OPERATOR);
        inboundRepository.save(inbound);

        inboundCommandService.cancel(inbound.getId());

        Inbound actual = testFactory.getInbound(inbound.getId());
        assertThat(actual.getInboundStatus()).isEqualTo(InboundStatus.CANCELLED_BY_OPERATOR);
    }

    @Test
    void successPutInboundRegistryBatching() {
        configurationService.mergeValue(ConfigurationProperties.PUT_INBOUND_REGISTRY_BATCHING, true);

        var nonUpdateOrder = testFactory.createForToday(order(sortingCenter, "nonUpdateOrderExtId-0")
                .places("nonUpdateOrderExtId-0", "nonUpdateOrderExtId-1").build()).get();
        var nonUpdatePlaces = placeRepository.findAllByOrderIdOrderById(nonUpdateOrder.getId());
        assertThat(nonUpdatePlaces.size()).isEqualTo(2);

        var inboundExternalId = "ex_id_1";
        var inbound = flow.createInboundAndGet(inboundExternalId);
        var reqs = new ArrayList<CreateInboundRegistryOrderRequest>();
        var orders = new ArrayList<ScOrder>();
        for (int i = 0; i < 10; i++) {
            String orderExternalId = "order_ext_id-" + i;
            String placeMainPartnerCode = "place_ext_id" + i;
            orders.add(testFactory.createForToday(order(sortingCenter, orderExternalId)
                    .places(placeMainPartnerCode).build()).get());
            reqs.add(new CreateInboundRegistryOrderRequest(
                    inbound.getExternalId(),
                    orderExternalId,
                    placeMainPartnerCode,
                    null
            ));
        }

        inboundCommandService.createInboundRegistry(
                reqs, Collections.emptyList(),
                inbound.getExternalId(),
                "TEST_REGESTRY", testFactory.storedUser(123123123L)
        );

        var registries = registryRepository.findAllByInboundId(inbound.getId());

        assertThat(boundRegistryRepository.findAllByRegistryId(registries.get(0).getId()).size())
                .isEqualTo(reqs.size());

        var nonUpdatePlaces1 = placeRepository.findAllByOrderIdOrderById(nonUpdateOrder.getId());
        assertThat(nonUpdatePlaces1.size()).isEqualTo(2);
        assertThat(nonUpdatePlaces1.stream().filter(nup -> nup.getInboundId() != null).count()).isEqualTo(0);


        var modifiedPlaces =
                placeRepository.findAllByOrderIdIn(orders.stream().map(ScOrder::getId).collect(Collectors.toList()));

        modifiedPlaces.forEach(p -> {
                    assertThat(p.getInboundId()).isEqualTo(inbound.getId());
                }
        );


    }

    @Test
    void fixInboundById() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.ENABLE_DISCREPANCY_ACT_GENERATING_BY_REGISTRY_MANAGER, true);
        var warehouse = testFactory.storedWarehouse("shopWhYandexId", WarehouseType.SHOP);
        var inbound = testFactory.createInbound(
                TestFactory.CreateInboundParams.builder()
                        .fromDate(OffsetDateTime.now(clock))
                        .toDate(OffsetDateTime.now(clock))
                        .inboundType(InboundType.DS_SC)
                        .sortingCenter(sortingCenter)
                        .inboundExternalId("IN-123")
                        .warehouseFromExternalId(warehouse.getYandexId())
                        .nextLogisticPointId(warehouse.getYandexId())
                        .build()
        );
        testFactory.finishInbound(inbound, testFactory.storedUser(sortingCenter, 777));
        verify(onlineTransferActPutTransferService, times(1)).putInboundTransfer(any(), any(), any());
    }
}
