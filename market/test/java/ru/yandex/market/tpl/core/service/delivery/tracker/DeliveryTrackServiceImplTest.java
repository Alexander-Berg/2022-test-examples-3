package ru.yandex.market.tpl.core.service.delivery.tracker;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.delivery.tracker.api.client.TrackerApiClient;
import ru.yandex.market.delivery.tracker.domain.entity.DeliveryTrack;
import ru.yandex.market.delivery.tracker.domain.entity.DeliveryTrackCheckpoint;
import ru.yandex.market.delivery.tracker.domain.entity.DeliveryTrackMeta;
import ru.yandex.market.delivery.tracker.domain.enums.EntityType;
import ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus;
import ru.yandex.market.delivery.tracker.domain.enums.SurveyType;
import ru.yandex.market.logistic.api.model.common.OrderStatusType;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.sc.OrderStatusUpdate;
import ru.yandex.market.tpl.core.domain.sc.OrderStatusUpdateResult;
import ru.yandex.market.tpl.core.domain.sc.ScManager;
import ru.yandex.market.tpl.core.domain.yago.YandexGoOrderNotifier;
import ru.yandex.market.tpl.core.service.delivery.tracker.dto.NotificationResult;
import ru.yandex.market.tpl.core.service.delivery.tracker.dto.TrackerNotificationResultStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author kukabara
 */
@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class DeliveryTrackServiceImplTest {

    private static final String ORDER_ID = "ORDER";
    private static final long SC_ID = 123L;
    private static final long SC_ID_2 = 234L;
    private static final long TRACKER_ID = 1L;
    private static final long TRACKER_ID_2 = 2L;
    private final SoftAssertions softly = new SoftAssertions();

    @MockBean
    private ScManager scManager;

    @SpyBean
    private TrackerApiClient trackerApiClient;

    @MockBean
    private YandexGoOrderNotifier yandexGoOrderNotifier;

    @Autowired
    private DeliveryTrackService deliveryTrackService;

    @AfterEach
    public void tearDown() {
        softly.assertAll();
    }

    @Test
    void notifyTracksNotFound() {
        when(scManager.updateOrderStatuses(eq(ORDER_ID), eq(SC_ID), anyList())).thenReturn(
                OrderStatusUpdateResult.NOT_FOUND
        );

        List<NotificationResult> results = deliveryTrackService.notifyTracks(
                List.of(createTrack(TRACKER_ID, ORDER_ID, SC_ID, System.currentTimeMillis(),
                        List.of(OrderDeliveryCheckpointStatus.SORTING_CENTER_LOADED)))
        );
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(TrackerNotificationResultStatus.NOT_FOUND);
    }

    @Test
    void notifyTracksIgnored() {
        when(scManager.updateOrderStatuses(eq(ORDER_ID), eq(SC_ID), anyList())).thenReturn(
                OrderStatusUpdateResult.NOT_SUPPORTED
        );

        List<NotificationResult> results = deliveryTrackService.notifyTracks(
                List.of(createTrack(TRACKER_ID, ORDER_ID, SC_ID, System.currentTimeMillis(),
                        List.of(OrderDeliveryCheckpointStatus.SORTING_CENTER_LOADED)))
        );
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(TrackerNotificationResultStatus.IGNORED);
    }

    @Test
    void notifyTracksUpdate() {
        when(scManager.updateOrderStatuses(eq(ORDER_ID), eq(SC_ID), anyList())).thenReturn(
                OrderStatusUpdateResult.SUCCESS
        );
        when(scManager.updateOrderStatuses(eq(ORDER_ID), eq(SC_ID_2), anyList())).thenReturn(
                OrderStatusUpdateResult.SUCCESS
        );

        long millis = System.currentTimeMillis();
        List<NotificationResult> results = deliveryTrackService.notifyTracks(List.of(
                createTrack(TRACKER_ID, ORDER_ID, SC_ID, millis + 10, List.of(
                        OrderDeliveryCheckpointStatus.SORTING_CENTER_RETURN_PREPARING,
                        OrderDeliveryCheckpointStatus.SORTING_CENTER_RETURN_ARRIVED)),
                createTrack(TRACKER_ID, ORDER_ID, SC_ID_2, millis + 10, List.of(
                        OrderDeliveryCheckpointStatus.SORTING_CENTER_RETURN_PREPARING,
                        OrderDeliveryCheckpointStatus.SORTING_CENTER_RETURN_ARRIVED)),
                createTrack(TRACKER_ID_2, ORDER_ID, SC_ID, millis, List.of(
                        OrderDeliveryCheckpointStatus.SORTING_CENTER_CREATED,
                        OrderDeliveryCheckpointStatus.SORTING_CENTER_LOADED))
        ));
        assertThat(results).hasSize(1);
        softly.assertThat(results.get(0).getStatus()).isEqualTo(TrackerNotificationResultStatus.OK);
        softly.assertThat(results.get(0).getTrackerId()).isEqualTo(TRACKER_ID);

        ArgumentCaptor<List<OrderStatusUpdate>> statusCaptor = ArgumentCaptor.forClass(List.class);
        verify(scManager).updateOrderStatuses(eq(ORDER_ID), eq(SC_ID), statusCaptor.capture());
        softly.assertThat(statusCaptor.getValue()).extracting(OrderStatusUpdate::getStatusCode)
                .containsExactly(OrderStatusType.SO_GOT_INFO_ABOUT_PLANNED_RETURN.getCode(),
                        OrderStatusType.RETURNED_ORDER_AT_SO_WAREHOUSE.getCode());
    }

    static DeliveryTrack createTrack(long trackerId,
                                     String orderId,
                                     Long scPartnerId,
                                     long millis,
                                     List<OrderDeliveryCheckpointStatus> statuses) {
        DeliveryTrackMeta deliveryTrackMeta = new DeliveryTrackMeta();
        deliveryTrackMeta.setId(trackerId);
        deliveryTrackMeta.setEntityId(orderId);
        deliveryTrackMeta.setEntityType(EntityType.ORDER);
        deliveryTrackMeta.setTrackCode(orderId);
        deliveryTrackMeta.setLastUpdatedDate(new Date(millis));
        deliveryTrackMeta.setDeliveryServiceId(scPartnerId);

        List<DeliveryTrackCheckpoint> deliveryTrackCheckpoints = StreamEx.of(statuses)
                .zipWith(IntStreamEx.ints())
                .mapKeyValue((status, index) -> new DeliveryTrackCheckpoint(
                        trackerId, new Date(millis + index), status, SurveyType.PULL))
                .collect(Collectors.toList());
        return new DeliveryTrack(deliveryTrackMeta, deliveryTrackCheckpoints);
    }


    @Test
    void shouldCallYandexGoOrderNotifier_whenNotifyTracks_ifYandexGoNotifierSupportsDeliveryTrack() {
        // given
        long trackerId = 123;
        String orderId = "order-id";
        Long scId = 456L;
        long millis = 10000;
        when(yandexGoOrderNotifier.supports(any())).thenReturn(true);
        when(yandexGoOrderNotifier.notifyTrack(any())).thenReturn(NotificationResult.ok(trackerId));
        OrderDeliveryCheckpointStatus sortingCenterReturnPreparing =
                OrderDeliveryCheckpointStatus.SORTING_CENTER_RETURN_PREPARING;
        List<OrderDeliveryCheckpointStatus> statuses =
                List.of(sortingCenterReturnPreparing);
        DeliveryTrack track = createTrack(trackerId, orderId, scId, millis, statuses);
        List<DeliveryTrack> tracks = List.of(track);

        // when
        deliveryTrackService.notifyTracks(tracks);

        // then
        verify(yandexGoOrderNotifier).notifyTrack(track);
    }

}
