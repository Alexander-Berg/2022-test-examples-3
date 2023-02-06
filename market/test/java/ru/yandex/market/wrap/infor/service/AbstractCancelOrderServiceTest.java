package ru.yandex.market.wrap.infor.service;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.model.common.ErrorCode;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.exception.FulfillmentApiException;
import ru.yandex.market.logistics.test.integration.SoftAssertionSupport;
import ru.yandex.market.wrap.infor.client.WrappedInforClient;
import ru.yandex.market.wrap.infor.configuration.WmsDataSourceTypeContextHolder;
import ru.yandex.market.wrap.infor.configuration.enums.DataSourceType;
import ru.yandex.market.wrap.infor.entity.InforOrderStatus;
import ru.yandex.market.wrap.infor.entity.InforOrderStatusType;
import ru.yandex.market.wrap.infor.model.OrderType;
import ru.yandex.market.wrap.infor.repository.OrderStatusRepository;
import ru.yandex.market.wrap.infor.service.common.AbstractCancelOrderService;
import ru.yandex.market.wrap.infor.service.order.CancelOrderService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AbstractCancelOrderServiceTest extends SoftAssertionSupport {

    private static final Set<InforOrderStatusType> CANCELLABLE_STATUS_TYPES = EnumSet.of(
        InforOrderStatusType.UNKNOWN,
        InforOrderStatusType.OUT_OF_SYNC,
        InforOrderStatusType.EMPTY_ORDER,
        InforOrderStatusType.CREATED_EXTERNALLY,
        InforOrderStatusType.CREATED_INTERNALLY,
        InforOrderStatusType.DID_NOT_ALLOCATE,

        InforOrderStatusType.CONVERTED,
        InforOrderStatusType.NOT_STARTED,
        InforOrderStatusType.BATCHED_ORDER,
        InforOrderStatusType.PART_PRE_ALLOCATED,
        InforOrderStatusType.PRE_ALLOCATED,
        InforOrderStatusType.RELEASED_TO_WAREHOUSE_PLANNER,
        InforOrderStatusType.PART_ALLOCATED,
        InforOrderStatusType.PART_ALLOCATED_PART_PICKED,
        InforOrderStatusType.PART_ALLOCATED_PART_SHIPPED,
        InforOrderStatusType.ALLOCATED,
        InforOrderStatusType.SUBSTITUTED,
        InforOrderStatusType.PART_RELEASED,
        InforOrderStatusType.PART_RELEASED_PART_PICKED,
        InforOrderStatusType.PART_RELEASED_PART_SHIPPED,
        InforOrderStatusType.RELEASED
    );

    private static final Set<InforOrderStatusType> ALREADY_CANCELLED_STATUS_TYPES = EnumSet.of(
        InforOrderStatusType.CANCELLED_EXTERNALLY,
        InforOrderStatusType.CANCELLED_INTERNALLY
    );

    private static final Set<InforOrderStatusType> NON_CANCELLABLE_STATUS_TYPES = EnumSet.of(
        InforOrderStatusType.IN_PICKING,
        InforOrderStatusType.PART_PICKED,
        InforOrderStatusType.PART_PICKED_PART_SHIPPED,
        InforOrderStatusType.PICKED_COMPLETE,
        InforOrderStatusType.PICKED_PART_SHIPPED,
        InforOrderStatusType.IN_PACKING,
        InforOrderStatusType.PACK_COMPLETE,

        InforOrderStatusType.STAGED,
        InforOrderStatusType.MANIFESTED,
        InforOrderStatusType.IN_LOADING,
        InforOrderStatusType.LOADED,
        InforOrderStatusType.PART_SHIPPED,
        InforOrderStatusType.CLOSE_PRODUCTION,

        InforOrderStatusType.SHIPPED_COMPLETE,
        InforOrderStatusType.DELIVERED_ACCEPTED,
        InforOrderStatusType.DELIVERED_REJECTED
    );

    private static final String EXTERNAL_ORDER_KEY_PREFIX = "externalOrderKey";
    private static final String ORDER_KEY_PREFIX = "orderKey";

    private AbstractCancelOrderService cancelOrderService;
    private OrderStatusRepository orderStatusRepository;
    private WrappedInforClient inforClient;

    @BeforeEach
    void setUp() {
        orderStatusRepository = mock(OrderStatusRepository.class);
        when(orderStatusRepository.findLatestByExternalOrderKey(anyString())).thenReturn(Optional.empty());
        when(orderStatusRepository.getStatusCancelFlag(any())).thenReturn(Optional.empty());
        for (InforOrderStatusType type : InforOrderStatusType.values()) {
            String externalId = EXTERNAL_ORDER_KEY_PREFIX + type.getCode();
            String orderId = ORDER_KEY_PREFIX + type.getCode();
            InforOrderStatus status = new InforOrderStatus(orderId, externalId, type, null, OrderType.STANDARD);
            when(orderStatusRepository.findLatestByExternalOrderKey(eq(externalId))).thenReturn(Optional.of(status));
        }

        inforClient = mock(WrappedInforClient.class);
        when(inforClient.cancelShipment(anyString())).thenReturn(null);
        WmsDataSourceTypeContextHolder wmsDataSourceTypeContextHolder = new WmsDataSourceTypeContextHolder();
        wmsDataSourceTypeContextHolder.setDataSourceType(DataSourceType.READ_WRITE);
        cancelOrderService = new AbstractCancelOrderService(
            inforClient,
            orderStatusRepository,
            EnumSet.of(OrderType.STANDARD),
            wmsDataSourceTypeContextHolder) {
        };
        cancelOrderService = new CancelOrderService(inforClient, orderStatusRepository, wmsDataSourceTypeContextHolder);
    }

    /**
     * Сценарий #1: Проверяем, что будет взаимодействие с инфором для каждого отменяемого статуса.
     */
    @Test
    void testOnCancelableStatusTypes() {
        for (InforOrderStatusType statusType : CANCELLABLE_STATUS_TYPES) {
            cancelOrderService.cancel(getResourceId(statusType));
        }

        verify(orderStatusRepository, times(CANCELLABLE_STATUS_TYPES.size())).findLatestByExternalOrderKey(anyString());
        verify(inforClient, times(CANCELLABLE_STATUS_TYPES.size())).cancelShipment(anyString());
    }

    /**
     * Сценарий #2: Проверяем, что при неотменяемых статусах будет взаимодействие с репозиторием,
     * но будет брошено исключение до взаимодействия с инфором.
     */
    @Test
    void testOnNonCancelableStatusTypes() {
        when(orderStatusRepository.getStatusCancelFlag(any())).thenReturn(Optional.empty());

        for (InforOrderStatusType statusType : NON_CANCELLABLE_STATUS_TYPES) {
            try {
                cancelOrderService.cancel(getResourceId(statusType));
            } catch (FulfillmentApiException exc) {
                softly.assertThat(exc.getErrorsArray()[0].getCode()).isEqualTo(ErrorCode.BAD_REQUEST);
            }
        }

        verify(orderStatusRepository, times(NON_CANCELLABLE_STATUS_TYPES.size()))
            .findLatestByExternalOrderKey(anyString());

        verify(inforClient, times(0)).cancelShipment(anyString());
    }

    /**
     * Сценарий #3: Проверяем, что для уже отмененных заказов будет взаимодействие с репозиторием,
     * но не будет взаимодействия с Инфором.
     */
    @Test
    void testOnAlreadyCancelledStatusTypes() {
        for (InforOrderStatusType statusType : ALREADY_CANCELLED_STATUS_TYPES) {
            cancelOrderService.cancel(getResourceId(statusType));

        }

        verify(orderStatusRepository, times(ALREADY_CANCELLED_STATUS_TYPES.size()))
            .findLatestByExternalOrderKey(anyString());

        verify(inforClient, times(0)).cancelShipment(anyString());
    }

    private ResourceId getResourceId(InforOrderStatusType statusType) {
        String externalOrderId = EXTERNAL_ORDER_KEY_PREFIX + statusType.getCode();
        String orderId = ORDER_KEY_PREFIX + statusType.getCode();
        return new ResourceId(externalOrderId, orderId);
    }
}
