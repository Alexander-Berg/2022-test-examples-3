package ru.yandex.market.tpl.core.domain.sc.batch.update.task;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.outbound.dto.BatchRegistryDto;
import ru.yandex.market.tpl.api.model.shift.UserShiftStatus;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterCachedService;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterRepository;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.routing.events.ScRoutingResultsUpdatedEvent;
import ru.yandex.market.tpl.core.domain.sc.ScManager;
import ru.yandex.market.tpl.core.domain.sc.batch_update.model.BatchUpdateInternalCourierDto;
import ru.yandex.market.tpl.core.domain.sc.batch_update.model.BatchUpdateOrdersRequest;
import ru.yandex.market.tpl.core.domain.sc.batch_update.model.BatchUpdateResponse;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserProperties;
import ru.yandex.market.tpl.core.domain.user.UserPropertyService;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.external.delivery.sc.SortCenterDirectClient;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.IS_ORDER_BATCH_REGISTRY_CREATE_ENABLED;

@RequiredArgsConstructor
class BatchUpdateOrdersServiceTest extends TplAbstractTest {

    private final TestUserHelper testUserHelper;
    private final OrderGenerateService orderGenerateService;
    private final SortingCenterCachedService sortingCenterCachedService;
    private final TestDataFactory testDataFactory;
    private final BatchUpdateOrderRequestService batchUpdateOrderRequestService;
    private final SortingCenterRepository sortingCenterRepository;
    private final UserPropertyService userPropertyService;
    private final ConfigurationProviderAdapter configurationProviderAdapter;
    @SpyBean
    private SortCenterDirectClient sortCenterDirectClient;
    @SpyBean
    private ScManager scManager;
    @MockBean
    private ApplicationEventPublisher eventPublisher;

    private final String routingProcessingId = "processingId";

    private BatchUpdateOrdersService batchUpdateOrdersService;

    private User user;
    private SortingCenter sortingCenter;
    private Order order;
    private Order pvzOrder;
    private UserShift userShift;

    @BeforeEach
    void init() {
        reset(configurationProviderAdapter);
        batchUpdateOrdersService = new BatchUpdateOrdersService(
                sortCenterDirectClient, scManager, sortingCenterCachedService, eventPublisher,
                batchUpdateOrderRequestService
        );
        user = testUserHelper.findOrCreateUser(123L);
        sortingCenter = testUserHelper.sortingCenter(666L);
        PickupPoint pvzPickupPoint = testDataFactory.createPickupPoint(PartnerSubType.PVZ, 2L, 1L);
        clearAfterTest(pvzPickupPoint);
        pvzOrder = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .pickupPoint(pvzPickupPoint)
                        .places(OrderGenerateService.OrderGenerateParam.generateOrderPlaceDto(2))
                        .build());
        order = orderGenerateService.createOrder();
        userShift = testUserHelper.createShiftWithDeliveryTask(user, UserShiftStatus.SHIFT_CREATED, order);
    }

    /**
     * Даже пустой {@link AfterEach} нужен
     * для срабатывания {@link ru.yandex.market.tpl.core.test.TplAbstractTest#clearAfterTest(Object)}
     */
    @AfterEach
    void afterEach() {
        sortingCenterRepository.delete(sortingCenter);
    }

    @Test
    void processPayloadSuccess() {
        //given
        doReturn(new BatchUpdateResponse(List.of(order.getExternalOrderId()), Collections.emptyList()))
                .when(sortCenterDirectClient).batchUpdateOrders(any());

        Map<Long, List<Long>> orderIdsByUserId = Map.of(user.getId(), List.of(order.getId()));
        var payload = new BatchUpdateOrdersPayload(
                "req",
                orderIdsByUserId,
                sortingCenter.getId(),
                userShift.getShift().getShiftDate(),
                routingProcessingId
        );

        //when
        batchUpdateOrdersService.processPayload(payload);

        //then
        ArgumentCaptor<BatchUpdateOrdersRequest> requestCaptor =
                ArgumentCaptor.forClass(BatchUpdateOrdersRequest.class);
        verify(sortCenterDirectClient, atLeastOnce()).batchUpdateOrders(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getCourierDtoList()).containsOnly(
                new BatchUpdateInternalCourierDto(user.getUid(),
                        user.getFullName(),
                        user.getVehicleNumber(),
                        null,
                        null,
                        user.getCompany().getName()));
        assertThat(requestCaptor.getValue().getNextCursorMark()).isNull();
        assertThat(requestCaptor.getValue().getTotalOrders()).isEqualTo(1);
        assertThat(requestCaptor.getValue().getSortingCenterId()).isEqualTo(sortingCenter.getId());
        assertThat(requestCaptor.getValue().getExternalIdListByCourier()).containsOnlyKeys(user.getUid());
        assertThat(requestCaptor.getValue().getExternalIdListByCourier())
                .containsAllEntriesOf(Map.of(user.getUid(), List.of(order.getExternalOrderId())));

        verify(scManager, times(1)).batchUpdateCourierAndDate(any(), any(), any(), any());

        ArgumentCaptor<ScRoutingResultsUpdatedEvent> eventCaptor =
                ArgumentCaptor.forClass(ScRoutingResultsUpdatedEvent.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
        assertEquals(eventCaptor.getValue().getShiftDate(), userShift.getShift().getShiftDate());
        assertEquals(eventCaptor.getValue().getShiftDate(), userShift.getShift().getShiftDate());
        assertEquals(eventCaptor.getValue().getSortingCenterId(), sortingCenter.getId());
        assertEquals(eventCaptor.getValue().getProcessingId(), routingProcessingId);
    }

    @Test
    @Transactional
    @Disabled("https://st.yandex-team.ru/MARKETTPL-7095")
    void processPayloadSuccessWithBatchRegistry() {
        testUserHelper.addLockerDeliveryTaskToShift(user, userShift, pvzOrder);
        doReturn(new BatchUpdateResponse(List.of(pvzOrder.getExternalOrderId()), Collections.emptyList()))
                .when(sortCenterDirectClient).batchUpdateOrders(any());
        when(configurationProviderAdapter.isBooleanEnabled(IS_ORDER_BATCH_REGISTRY_CREATE_ENABLED)).thenReturn(true);
        userPropertyService.addPropertyToUser(user, UserProperties.USER_ORDER_BATCH_SHIFTING_ENABLED, true);
        Map<Long, List<Long>> orderIdsByUserId = Map.of(user.getId(), List.of(pvzOrder.getId()));
        var payload = new BatchUpdateOrdersPayload(
                "req",
                orderIdsByUserId,
                sortingCenter.getId(),
                userShift.getShift().getShiftDate(),
                routingProcessingId
        );

        //when
        batchUpdateOrdersService.processPayload(payload);

        //then
        ArgumentCaptor<BatchUpdateOrdersRequest> requestCaptor =
                ArgumentCaptor.forClass(BatchUpdateOrdersRequest.class);
        verify(sortCenterDirectClient, atLeastOnce()).batchUpdateOrders(requestCaptor.capture());
        BatchUpdateOrdersRequest value = requestCaptor.getValue();
        assertThat(value.getCourierDtoList()).containsOnly(
                new BatchUpdateInternalCourierDto(user.getUid(),
                        user.getFullName(),
                        user.getVehicleNumber(),
                        null,
                        null,
                        user.getCompany().getName()));
        assertThat(value.getNextCursorMark()).isNull();
        assertThat(value.getTotalOrders()).isEqualTo(1);
        assertThat(value.getSortingCenterId()).isEqualTo(sortingCenter.getId());
        assertThat(value.getExternalIdListByCourier()).containsOnlyKeys(user.getUid());
        assertThat(value.getExternalIdListByCourier())
                .containsAllEntriesOf(Map.of(user.getUid(), List.of(pvzOrder.getExternalOrderId())));
        List<BatchRegistryDto> batchRegistries = value.getBatchRegistries();
        assertNotNull(batchRegistries);
        verify(scManager, times(1)).batchUpdateCourierAndDate(any(), any(), any(), any());
        ArgumentCaptor<ScRoutingResultsUpdatedEvent> eventCaptor =
                ArgumentCaptor.forClass(ScRoutingResultsUpdatedEvent.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
        assertEquals(eventCaptor.getValue().getShiftDate(), userShift.getShift().getShiftDate());
        assertEquals(eventCaptor.getValue().getShiftDate(), userShift.getShift().getShiftDate());
        assertEquals(eventCaptor.getValue().getSortingCenterId(), sortingCenter.getId());
        assertEquals(eventCaptor.getValue().getProcessingId(), routingProcessingId);
    }

    @Test
    void processPayloadWhenResponseWithoutConfirmation() {
        //given
        doReturn(new BatchUpdateResponse(Collections.emptyList(), List.of(order.getExternalOrderId())))
                .when(sortCenterDirectClient).batchUpdateOrders(any());

        Map<Long, List<Long>> orderIdsByUserId = Map.of(user.getId(), List.of(order.getId()));
        var payload = new BatchUpdateOrdersPayload(
                "req",
                orderIdsByUserId,
                sortingCenter.getId(),
                userShift.getShift().getShiftDate(),
                routingProcessingId
        );

        //when
        batchUpdateOrdersService.processPayload(payload);

        //then
        verify(scManager, times(0)).batchUpdateCourierAndDate(any(), any(), any(), any());

        ArgumentCaptor<ScRoutingResultsUpdatedEvent> eventCaptor =
                ArgumentCaptor.forClass(ScRoutingResultsUpdatedEvent.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
        assertEquals(eventCaptor.getValue().getShiftDate(), userShift.getShift().getShiftDate());
        assertEquals(eventCaptor.getValue().getSortingCenterId(), sortingCenter.getId());
        assertEquals(eventCaptor.getValue().getProcessingId(), routingProcessingId);
    }

    @Test
    void processPayloadWhenResponseFail() {
        //given
        doThrow(new RuntimeException())
                .when(sortCenterDirectClient).batchUpdateOrders(any());

        Map<Long, List<Long>> orderIdsByUserId = Map.of(user.getId(), List.of(order.getId()));
        var payload = new BatchUpdateOrdersPayload(
                "req",
                orderIdsByUserId,
                sortingCenter.getId(),
                userShift.getShift().getShiftDate(),
                routingProcessingId
        );

        //when
        assertThrows(RuntimeException.class,
                () -> batchUpdateOrdersService.processPayload(payload));

        //then
        verify(scManager, times(0)).batchUpdateCourierAndDate(any(), any(), any(), any());
        verify(eventPublisher, never()).publishEvent(any(ScRoutingResultsUpdatedEvent.class));
    }

}
