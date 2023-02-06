package ru.yandex.market.sc.core.domain.scan;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import ru.yandex.market.sc.core.domain.client_return.ClientReturnService;
import ru.yandex.market.sc.core.domain.client_return.repository.ClientReturnBarcodePrefix;
import ru.yandex.market.sc.core.domain.order.OrderCommandService;
import ru.yandex.market.sc.core.domain.order.OrderFlowService;
import ru.yandex.market.sc.core.domain.order.model.CreateReturnRequest;
import ru.yandex.market.sc.core.domain.order.model.DeletedSegmentRequest;
import ru.yandex.market.sc.core.domain.order.model.OrderReturnType;
import ru.yandex.market.sc.core.domain.scan.event.UnknownBoxReceivedEvent;
import ru.yandex.market.sc.core.domain.scan.model.AcceptReturnedOrderRequestDto;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.resolver.dto.ScContext;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.model.WarehouseDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static ru.yandex.market.sc.core.test.TestFactory.order;

@EmbeddedDbTest
@RecordApplicationEvents
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class UnknownBoxReceivedEventTest {

    @Autowired
    private ApplicationEvents applicationEvents;
    @Autowired
    private OrderCommandService orderCommandService;
    private final ScanService scanService;
    private final TestFactory testFactory;
    @MockBean
    private OrderFlowService orderFlowService;

    @MockBean
    private Clock clock;
    private SortingCenter sortingCenter;
    private User user;

    @BeforeEach
    void init() {
        testFactory.setupMockClock(clock);
        testFactory.increaseScOrderId();
        sortingCenter = testFactory.storedSortingCenter();
        testFactory.setSortingCenterProperty(
            sortingCenter.getId(), SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        user = testFactory.storedUser(sortingCenter, 456L);
    }

    @Test
    @DisplayName("Cкан уже созданного заказа: ивент не отправляется")
    void noEventWhenAlreadyCreated() {
        String externalId = "ALREADY_CREATED";
        setupReturnAlreadyCreated(externalId);
        AcceptReturnedOrderRequestDto requestDto = createRequestDto(externalId);

        scanService.acceptReturnedOrder(requestDto, new ScContext(user));

        assertThat(applicationEvents.stream(UnknownBoxReceivedEvent.class))
            .isEmpty();
    }

    private void setupReturnAlreadyCreated(String externalId) {
        testFactory.createForToday(order(sortingCenter, externalId).build())
            .accept().sort().ship().makeReturn().get();
    }

    @Test
    @DisplayName("Скан возврата с другого СЦ: ивент не отправляется")
    void noEventWhenAnotherSc() {
        String externalId = "ANOTHER_SC";
        setupReturnAnotherSc(externalId);
        AcceptReturnedOrderRequestDto requestDto = createRequestDto(externalId);

        scanService.acceptReturnedOrder(requestDto, new ScContext(user));

        assertThat(applicationEvents.stream(UnknownBoxReceivedEvent.class))
            .isEmpty();
    }

    private void setupReturnAnotherSc(String externalId) {
        var anotherSortingCenter = testFactory.storedSortingCenter(9L);
        testFactory.createForToday(order(anotherSortingCenter, externalId).build())
            .accept().sort().ship().makeReturn().get();

        var warehouseReturn = testFactory.storedWarehouse("1");
        when(orderFlowService.lookForMisdeliveryReturnWarehouse(sortingCenter, externalId))
            .thenReturn(Optional.of(warehouseReturn));
    }

    @Test
    @DisplayName("Скан клиентского возврата: ивент не отправляется")
    void noEventWhenClientReturn() {
        String externalId = ClientReturnBarcodePrefix.CLIENT_RETURN_PVZ.getAnyPrefix() + "-1";
        setupReturnClient();
        AcceptReturnedOrderRequestDto requestDto = createRequestDto(externalId);

        scanService.acceptReturnedOrder(requestDto, new ScContext(user));

        assertThat(applicationEvents.stream(UnknownBoxReceivedEvent.class))
            .isEmpty();
    }

    private void setupReturnClient() {
        testFactory.storedWarehouse(ClientReturnBarcodePrefix.CLIENT_RETURN_PVZ.getWarehouseReturnId());
        testFactory.storedDeliveryService(ClientReturnService.DELIVERY_SERVICE_YA_ID);
    }

    @Test
    @DisplayName("Скан случайного ШК: ивент отправляется")
    void sendEventWhenRandomExternalId() {
        String externalId = "RANDOM_EXTERNAL_ID";
        AcceptReturnedOrderRequestDto requestDto = createRequestDto(externalId);

        scanService.acceptReturnedOrder(requestDto, new ScContext(user));

        assertThat(applicationEvents.stream(UnknownBoxReceivedEvent.class))
            .hasSize(1);
    }

    @Test
    @DisplayName("Евент отправляется если коробка в статусе DELETED")
    void succesSendEventIfDeleted() {
        String externalId = "DELETED";
        setupDeleted(externalId);

        scanService.acceptReturnedOrder(new AcceptReturnedOrderRequestDto(externalId, externalId, null),
                new ScContext(user));
        assertThat(applicationEvents.stream(UnknownBoxReceivedEvent.class))
                .hasSize(1);
    }

    private AcceptReturnedOrderRequestDto createRequestDto(String externalId) {
        return new AcceptReturnedOrderRequestDto(externalId, null, null);
    }

    private void setupDeleted(String barcode) {
        testFactory.storedFakeReturnDeliveryService();
        testFactory.storedCourier(-1, ClientReturnService.CLIENT_RETURN_COURIER);
        var whFrom = testFactory.storedWarehouse("wh0");
        var whTo = testFactory.storedWarehouse("wh1");
        var cargoUnitId = "1aaaa123";
        var segmentUuid = "1123-12asdf-sadf-3213";
        var fromWarehouse = WarehouseDto.builder()
                .yandexId(whFrom.getYandexId())
                .build();
        var returnWarehouse = WarehouseDto.builder()
                .yandexId(whTo.getYandexId())
                .build();

        orderCommandService.createReturn(CreateReturnRequest.builder()
                        .sortingCenter(sortingCenter)
                        .orderBarcode(barcode)
                        .returnDate(LocalDate.now(clock))
                        .returnWarehouse(returnWarehouse)
                        .fromWarehouse(fromWarehouse)
                        .segmentUuid(segmentUuid)
                        .cargoUnitId(cargoUnitId)
                        .timeIn(Instant.now(clock))
                        .timeOut(Instant.now(clock))
                        .orderReturnType(OrderReturnType.CLIENT_RETURN)
                        .assessedCost(new BigDecimal(10_000))
                        .build()
                , user);
        orderCommandService.deleteSegmentUUI(DeletedSegmentRequest.builder()
                .segmentUuid(segmentUuid)
                .cargoUnitId(cargoUnitId)
                .build());
    }
}
