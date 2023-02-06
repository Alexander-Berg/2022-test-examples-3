package step;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import client.TrackerClient;
import dto.responses.tm.TmCheckpointStatus;
import dto.responses.tracker.TracksItem;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import toolkit.Retrier;

import ru.yandex.market.delivery.tracker.domain.entity.DeliveryTrack;
import ru.yandex.market.delivery.tracker.domain.entity.DeliveryTrackCheckpoint;
import ru.yandex.market.delivery.tracker.domain.entity.DeliveryTrackMeta;
import ru.yandex.market.delivery.tracker.domain.enums.DeliveryType;
import ru.yandex.market.delivery.tracker.domain.enums.EntityType;
import ru.yandex.market.delivery.tracker.domain.enums.HasIntId;
import ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus;

@Slf4j
public class TrackerSteps {
    private static final TrackerClient TRACKER_CLIENT = new TrackerClient();

    public void addOrderCheckpointToTracker(
        long trackerId,
        OrderDeliveryCheckpointStatus deliveryCheckpointStatus
    ) {
        addOrderCheckpointToTracker(trackerId, LocalDate.now(), deliveryCheckpointStatus);
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    @Step("Регистрируем трек в трекер")
    public DeliveryTrackMeta registerTrack(
        String trackCode,
        long deliveryServiceId,
        long consumerId,
        String entityId,
        LocalDate estimatedArrivalDateFrom,
        LocalDate estimatedArrivalDateTo,
        DeliveryType deliveryType,
        boolean isGlobalOrder,
        EntityType entityType
    ) {
        return TRACKER_CLIENT.registerTrack(
            trackCode,
            deliveryServiceId,
            consumerId,
            entityId,
            estimatedArrivalDateFrom,
            estimatedArrivalDateTo,
            deliveryType,
            isGlobalOrder,
            entityType);
    }

    @Step("Отправляем чекопоинт {deliveryCheckpointStatus} по {trackerId} напрямую в трекер")
    public void addOrderCheckpointToTracker(
        long trackerId,
        LocalDate localDate,
        OrderDeliveryCheckpointStatus deliveryCheckpointStatus
    ) {
        log.debug("Adding checkpoint {} to TrackerId {}", deliveryCheckpointStatus, trackerId);
        TRACKER_CLIENT.addCheckpoint(trackerId, deliveryCheckpointStatus.getId(), localDate, EntityType.ORDER);
    }

    @Step("Отправляем чекопоинт {deliveryCheckpointStatus} по {trackerId} напрямую в трекер")
    public void addTmCheckpointToTracker(
        long trackerId,
        TmCheckpointStatus deliveryCheckpointStatus,
        EntityType entityType
    ) {
        log.debug("Adding checkpoint {} to TrackerId {}", deliveryCheckpointStatus, trackerId);
        Retrier.clientRetry(() -> TRACKER_CLIENT.addCheckpoint(
            trackerId,
            deliveryCheckpointStatus.getId(),
            LocalDate.now(),
            entityType
        ));
    }

    @Step("Получаем DeliveryTrackMeta по entityId")
    public TracksItem getTrackerMetaByEntityId(String entityId, Integer entityType) {
        log.debug("Getting tracker id for entityId {}", entityId);
        return Retrier.retry(() -> {
            List<TracksItem> tracks = TRACKER_CLIENT.getTracksByEntityId(entityId, entityType).getTracks();
            Assertions.assertFalse(tracks.isEmpty(), "Пришли пустые треки entityId = " + entityId);
            Assertions.assertNotNull(tracks.get(0), "Трек пустой entityId = " + entityId);
            return tracks.get(0);
        });
    }

    // Оставляем возможность поиска trackerId без указания типа сущности в трекере
    @Step("Получаем DeliveryTrackMeta по entityId")
    public TracksItem getTrackerMetaByEntityId(String entityId) {
        log.debug("Getting tracker id for entityId {}", entityId);
        return Retrier.retry(() -> {
            List<TracksItem> tracks = TRACKER_CLIENT.getTracksByEntityId(entityId).getTracks();
            Assertions.assertFalse(tracks.isEmpty(), "Пришли пустые треки entityId = " + entityId);
            Assertions.assertNotNull(tracks.get(0), "Трек пустой entityId = " + entityId);
            return tracks.get(0);
        });
    }

    @Step("Получаем DeliveryTrackMeta по entityId")
    public DeliveryTrack getTrackById(long id) {
        log.debug("Getting tracker id for entityId {}", id);
        return Retrier.retry(() -> {
            DeliveryTrack track = TRACKER_CLIENT.getTrackById(id);
            Assertions.assertNotNull(track, "Трек пустой id = " + id);
            return track;
        });
    }

    @Step("Ждем получения чекпоинта {expectedCheckpoint}")
    public void verifyCheckpoint(long trackerId, int expectedCheckpoint) {
        Retrier.retry(() ->
                Assertions.assertTrue(
                    getTrackById(trackerId).getDeliveryTrackCheckpoints()
                        .stream()
                        .map(DeliveryTrackCheckpoint::getDeliveryCheckpointStatus)
                        .map(HasIntId::getId)
                        .collect(Collectors.toList())
                        .contains(expectedCheckpoint),
                    "Не пришел чекпоинт " + expectedCheckpoint + " по треку " + trackerId),
            Retrier.RETRIES_BIG
        );
    }

    @Step("Меняем время обработки трека на сейчас")
    public void instantRequest(Long orderId) {
        TRACKER_CLIENT.instantRequest(List.of(orderId.toString()));
    }
}

