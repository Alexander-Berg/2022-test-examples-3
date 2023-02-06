package ru.yandex.market.tpl.core.domain.yago;

import java.sql.Date;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.Value;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.delivery.tracker.domain.entity.DeliveryService;
import ru.yandex.market.delivery.tracker.domain.entity.DeliveryTrack;
import ru.yandex.market.delivery.tracker.domain.entity.DeliveryTrackCheckpoint;
import ru.yandex.market.delivery.tracker.domain.entity.DeliveryTrackMeta;
import ru.yandex.market.delivery.tracker.domain.enums.DeliveryServiceType;
import ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus;
import ru.yandex.market.logistic.gateway.common.model.delivery.OrderStatusType;
import ru.yandex.market.tpl.core.domain.yago.history.YandexGoOrderHistoryRecord;
import ru.yandex.market.tpl.core.domain.yago.history.YandexGoOrderHistoryRepository;
import ru.yandex.market.tpl.core.service.delivery.tracker.dto.NotificationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class YandexGoOrderNotifierTest {

    @InjectMocks
    YandexGoOrderNotifierImpl notifier;

    @Mock
    YandexGoOrderProperties yandexGoOrderProperties;

    @Mock
    YandexGoOrderCommandService yandexGoOrderCommandService;

    @Mock
    YandexGoOrderRepository repository;

    @Mock
    YandexGoOrderHistoryRepository historyRepository;

    @Value
    class OrderUpdate {
        OrderDeliveryCheckpointStatus status;
        Instant time;
    }

    private static final long DELIVERY_SERVICE_ID = 123;
    private static final long TRACKER_ID = 456;
    private static final long YANDEXGO_ORDER_ID = 345L;
    private static final Instant NOW = OffsetDateTime.of(2000, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC).toInstant();

    @Test
    void shouldSupportTrack_whenSupports_ifDeliveryServiceIdMatchPropertyValue() {
        // given
        DeliveryTrack track = createDeliveryTrack();
        when(yandexGoOrderProperties.getDeliveryServiceId()).thenReturn(Optional.of(DELIVERY_SERVICE_ID));

        // when
        boolean supports = notifier.supports(track);

        // then
        assertThat(supports).isTrue();
    }

    @Test
    void shouldNoSupportTrack_whenSupports_ifDeliveryServiceIdDoesNotMatchPropertyValue() {
        // given
        DeliveryTrack track = createDeliveryTrack();
        when(yandexGoOrderProperties.getDeliveryServiceId()).thenReturn(Optional.of(DELIVERY_SERVICE_ID + 1));

        // when
        boolean supports = notifier.supports(track);

        // then
        assertThat(supports).isFalse();
    }

    private DeliveryTrack createDeliveryTrack() {
        DeliveryTrack track = new DeliveryTrack();
        DeliveryTrackMeta meta = new DeliveryTrackMeta();
        meta.setDeliveryServiceId(DELIVERY_SERVICE_ID);
        track.setDeliveryTrackMeta(meta);
        return track;
    }

    @Test
    void shouldNoSupportTrack_whenSupports_ifPropertyDoesNotExist() {
        // given
        DeliveryTrack track = createDeliveryTrack();
        when(yandexGoOrderProperties.getDeliveryServiceId()).thenReturn(Optional.empty());

        // when
        boolean supports = notifier.supports(track);

        // then
        assertThat(supports).isFalse();
    }

    @Test
    void shouldReturnNotFound_whenNotifyTracks_ifYandexGoOrderNotFound() {
        // given
        List<OrderUpdate> updates = List.of();
        DeliveryTrack track = getDeliveryTrack(TRACKER_ID, updates);

        // when
        NotificationResult result = notifier.notifyTrack(track);

        // then
        assertThat(result).isEqualTo(NotificationResult.notFound(TRACKER_ID));
    }

    private DeliveryTrack getDeliveryTrack(long trackerId, List<OrderUpdate> updates) {
        DeliveryTrackMeta meta =
                new DeliveryTrackMeta(
                        String.valueOf(YANDEXGO_ORDER_ID),
                        new DeliveryService(1, null, DeliveryServiceType.UNKNOWN),
                        null,
                        null,
                        null);
        meta.setId(trackerId);
        return new DeliveryTrack(
                meta,
                updates.stream()
                        .map(u ->
                                new DeliveryTrackCheckpoint()
                                        .setDeliveryCheckpointStatus(u.status)
                                        .setCheckpointDate(Date.from(u.time)))
                        .collect(Collectors.toList()));
    }

    @Test
    void shouldReturnOkAndDoNotRunAnyUpdate_whenNotifyTracks_ifOldUpdateListIsEmpty() {
        // given
        mockFindOrder();
        List<OrderUpdate> updates = List.of();
        DeliveryTrack track = getDeliveryTrack(TRACKER_ID, updates);

        // when
        NotificationResult result = notifier.notifyTrack(track);

        // then
        assertThat(result).isEqualTo(NotificationResult.ok(TRACKER_ID));
        verifyNoInteractions(yandexGoOrderCommandService);
    }

    @Test
    void shouldUpdateAllInputUpdatesInCorrectOrder_whenNotifyTracks_ifOldUpdateListIsEmpty() {
        // given
        mockFindOrder();
        mockOldUpdates(List.of());
        List<OrderUpdate> updates =
                List.of(
                        createStatusUpdate(OrderDeliveryCheckpointStatus.DELIVERY_LOADED, 1),
                        createStatusUpdate(OrderDeliveryCheckpointStatus.SENDER_SENT, 0));
        DeliveryTrack track = getDeliveryTrack(TRACKER_ID, updates);

        // when
        NotificationResult result = notifier.notifyTrack(track);

        // then
        assertThat(result).isEqualTo(NotificationResult.ok(TRACKER_ID));
        InOrder inOrder = inOrder(yandexGoOrderCommandService);
        inOrder.verify(yandexGoOrderCommandService)
                .updateOrderStatus(createCommand(OrderStatusType.ORDER_CREATED_BUT_NOT_APPROVED));
        inOrder.verify(yandexGoOrderCommandService).updateOrderStatus(createCommand(OrderStatusType.ORDER_CREATED));
    }


    @Test
    void shouldCollapseEqualsStatusUpdates_whenNotifyTracks_ifOldUpdateListIsEmpty() {
        // given
        mockFindOrder();
        mockOldUpdates(List.of());
        List<OrderUpdate> updates =
                List.of(
                        createStatusUpdate(OrderDeliveryCheckpointStatus.CANCELED, 4),
                        createStatusUpdate(OrderDeliveryCheckpointStatus.CANCELED, 5),
                        createStatusUpdate(OrderDeliveryCheckpointStatus.DELIVERY_LOADED, 2),
                        createStatusUpdate(OrderDeliveryCheckpointStatus.DELIVERY_LOADED, 3),
                        createStatusUpdate(OrderDeliveryCheckpointStatus.SENDER_SENT, 0),
                        createStatusUpdate(OrderDeliveryCheckpointStatus.SENDER_SENT, 1));
        DeliveryTrack track = getDeliveryTrack(TRACKER_ID, updates);

        // when
        NotificationResult result = notifier.notifyTrack(track);

        // then
        assertThat(result).isEqualTo(NotificationResult.ok(TRACKER_ID));
        verify(yandexGoOrderCommandService).updateOrderStatus(createCommand(OrderStatusType.ORDER_CREATED_BUT_NOT_APPROVED));
        verify(yandexGoOrderCommandService).updateOrderStatus(createCommand(OrderStatusType.ORDER_CREATED));
        verify(yandexGoOrderCommandService).updateOrderStatus(createCommand(OrderStatusType.ORDER_CANCELLED_BY_CUSTOMER));
        verifyNoMoreInteractions(yandexGoOrderCommandService);
    }

    @Test
    void shouldAddAllUpdates_whenNotifyTracks_ifOldUpdatesListIsNotEmpty() {
        // given
        mockFindOrder();
        mockOldUpdates(List.of(createHistoryRecord(OrderStatusType.ORDER_CREATED_BUT_NOT_APPROVED)));
        List<OrderUpdate> updates =
                List.of(
                        createStatusUpdate(OrderDeliveryCheckpointStatus.CANCELED, 5),
                        createStatusUpdate(OrderDeliveryCheckpointStatus.DELIVERY_LOADED, 1));
        DeliveryTrack track = getDeliveryTrack(TRACKER_ID, updates);

        // when
        NotificationResult result = notifier.notifyTrack(track);

        // then
        assertThat(result).isEqualTo(NotificationResult.ok(TRACKER_ID));
        InOrder inOrder = inOrder(yandexGoOrderCommandService);
        inOrder.verify(yandexGoOrderCommandService).updateOrderStatus(createCommand(OrderStatusType.ORDER_CREATED));
        inOrder.verify(yandexGoOrderCommandService)
                .updateOrderStatus(createCommand(OrderStatusType.ORDER_CANCELLED_BY_CUSTOMER));
        verifyNoMoreInteractions(yandexGoOrderCommandService);
    }

    @Test
    void shouldSkipFirstUpdate_whenNotifyTracks_ifOldUpdateContainsFirstNewUpdate() {
        // given
        mockFindOrder();
        mockOldUpdates(List.of(createHistoryRecord(OrderStatusType.ORDER_CREATED_BUT_NOT_APPROVED)));
        List<OrderUpdate> updates =
                List.of(
                        createStatusUpdate(OrderDeliveryCheckpointStatus.CANCELED, 5),
                        createStatusUpdate(OrderDeliveryCheckpointStatus.SENDER_SENT, 1));
        DeliveryTrack track = getDeliveryTrack(TRACKER_ID, updates);

        // when
        NotificationResult result = notifier.notifyTrack(track);

        // then
        assertThat(result).isEqualTo(NotificationResult.ok(TRACKER_ID));
        InOrder inOrder = inOrder(yandexGoOrderCommandService);
        inOrder.verify(yandexGoOrderCommandService)
                .updateOrderStatus(createCommand(OrderStatusType.ORDER_CANCELLED_BY_CUSTOMER));
        verifyNoMoreInteractions(yandexGoOrderCommandService);
    }

    @Test
    void shouldSkipAllUpdates_whenNotifyTracks_ifOldUpdatesContainAllNewUpdates() {
        // given
        mockFindOrder();
        mockOldUpdates(List.of(
                createHistoryRecord(OrderStatusType.ORDER_CREATED_BUT_NOT_APPROVED),
                createHistoryRecord(OrderStatusType.ORDER_CREATED)));
        List<OrderUpdate> updates =
                List.of(
                        createStatusUpdate(OrderDeliveryCheckpointStatus.DELIVERY_LOADED, 5),
                        createStatusUpdate(OrderDeliveryCheckpointStatus.SENDER_SENT, 1));
        DeliveryTrack track = getDeliveryTrack(TRACKER_ID, updates);

        // when
        NotificationResult result = notifier.notifyTrack(track);

        // then
        assertThat(result).isEqualTo(NotificationResult.ok(TRACKER_ID));
        verifyNoMoreInteractions(yandexGoOrderCommandService);
    }

    @Test
    void shouldProcessAllStatuses_whenNotifyTracks_ifNoUpdatesExistInRepository() {
        // given
        mockFindOrder();
        mockOldUpdates(List.of());
        List<OrderUpdate> updates =
                List.of(createStatusUpdate(OrderDeliveryCheckpointStatus.SENDER_SENT, 0));
        DeliveryTrack track = getDeliveryTrack(TRACKER_ID, updates);

        // when
        NotificationResult result = notifier.notifyTrack(track);

        // then
        assertThat(result).isEqualTo(NotificationResult.ok(TRACKER_ID));
        verify(yandexGoOrderCommandService).updateOrderStatus(
                createCommand(OrderStatusType.ORDER_CREATED_BUT_NOT_APPROVED));
    }

    private void mockOldUpdates(List<YandexGoOrderHistoryRecord> updates) {
        when(historyRepository.getOrderHistory(YANDEXGO_ORDER_ID)).thenReturn(updates);
    }

    private YandexGoOrderCommand.UpdateStatus createCommand(OrderStatusType orderCreated) {
        return new YandexGoOrderCommand.UpdateStatus(YANDEXGO_ORDER_ID, orderCreated);
    }

    private YandexGoOrderHistoryRecord createHistoryRecord(OrderStatusType status) {
        return new YandexGoOrderHistoryRecord().setStatus(status);
    }

    private OrderUpdate createStatusUpdate(OrderDeliveryCheckpointStatus status, long seconds) {
        return new OrderUpdate(status, NOW.plusSeconds(seconds));
    }

    private void mockFindOrder() {
        YandexGoOrder yandexGoOrder = new YandexGoOrder();
        yandexGoOrder.setId(YANDEXGO_ORDER_ID);
        when(repository.findByTrackId(String.valueOf(YANDEXGO_ORDER_ID))).thenReturn(Optional.of(yandexGoOrder));
    }
}
