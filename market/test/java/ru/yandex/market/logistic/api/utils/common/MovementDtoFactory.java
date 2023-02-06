package ru.yandex.market.logistic.api.utils.common;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.Nullable;

import ru.yandex.market.logistic.api.model.common.Korobyte;
import ru.yandex.market.logistic.api.model.common.Movement;
import ru.yandex.market.logistic.api.model.common.MovementStatus;
import ru.yandex.market.logistic.api.model.common.MovementStatusHistory;
import ru.yandex.market.logistic.api.model.common.MovementType;
import ru.yandex.market.logistic.api.model.common.ResourceId;
import ru.yandex.market.logistic.api.model.common.Status;
import ru.yandex.market.logistic.api.model.common.TripInfo;
import ru.yandex.market.logistic.api.model.common.TripType;
import ru.yandex.market.logistic.api.model.delivery.request.entities.restricted.PutMovementRestrictedData;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetMovementStatusHistoryResponse;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetMovementStatusResponse;
import ru.yandex.market.logistic.api.model.fulfillment.response.PutMovementResponse;
import ru.yandex.market.logistic.api.utils.DateTime;
import ru.yandex.market.logistic.api.utils.DateTimeInterval;

import static ru.yandex.market.logistic.api.model.common.StatusCode.CANCELLED_BY_PARTNER;
import static ru.yandex.market.logistic.api.model.common.StatusCode.COURIER_FOUND;
import static ru.yandex.market.logistic.api.model.common.StatusCode.CREATED;
import static ru.yandex.market.logistic.api.model.common.StatusCode.DELIVERED;
import static ru.yandex.market.logistic.api.model.common.StatusCode.DELIVERING;
import static ru.yandex.market.logistic.api.model.common.StatusCode.ERROR;
import static ru.yandex.market.logistic.api.model.common.StatusCode.HANDED_OVER;
import static ru.yandex.market.logistic.api.model.common.StatusCode.PENDING;

public final class MovementDtoFactory {

    private MovementDtoFactory() {
        throw new UnsupportedOperationException();
    }

    // putMovement and getMovement

    /**
     * Создает объект идентификации перемещения.
     */
    public static ResourceId createMovementId() {
        return new ResourceId("5927638", "39292337");
    }

    /**
     * Создаёт объект перемещения.
     */
    public static Movement createMovement() {
        return createMovement(new DateTimeInterval(getFrom(), getTo()), new BigDecimal("1234"));
    }

    /**
     * Создаёт объект перемещения с отсутствующим временным интервалом.
     */
    public static Movement createMovementWithoutInterval() {
        return createMovement(null, new BigDecimal("1234"));
    }

    /**
     * Создаёт объект перемещения с инвалидным объектом {@link Korobyte}.
     */
    public static Movement createMovementWithoutVolume() {
        return createMovement(new DateTimeInterval(getFrom(), getTo()), null);
    }

    /**
     * Создаёт успешный ответ для метода DS.putMovement.
     */
    public static ru.yandex.market.logistic.api.model.delivery.response.PutMovementResponse
    createPutMovementResponseDs() {
        return ru.yandex.market.logistic.api.model.delivery.response.PutMovementResponse.builder(createMovementId())
            .build();
    }

    /**
     * Создаёт успешный ответ для метода FF.putMovement.
     */
    public static PutMovementResponse createPutMovementResponseFF() {
        return PutMovementResponse.builder(createMovementId())
            .build();
    }

    public static PutMovementRestrictedData createPutMovementRestrictedData(Long courierId, Long transportId) {
        return new PutMovementRestrictedData(courierId, transportId);
    }

    private static Movement createMovement(@Nullable DateTimeInterval interval, @Nullable BigDecimal volume) {
        return Movement.builder(createMovementId(), interval, volume)
            .setWeight(BigDecimal.valueOf(1.1))
            .setShipper(DtoFactory.createParty("111325", "111326"))
            .setReceiver(DtoFactory.createParty("111425", "111426"))
            .setComment("Тут что-то комментируют")
            .setMaxPalletCapacity(15)
            .setTrip(
                new TripInfo.TripInfoBuilder().setTripId(
                    ResourceId.builder()
                        .setYandexId("TMT1")
                        .setPartnerId("2000")
                        .build())
                    .setRouteName("Маршрут №1")
                    .setFromIndex(0)
                    .setToIndex(2)
                    .setTotalCount(60)
                    .setPrice(10_000L)
                    .setType(TripType.MAIN)
                    .build()
            )
            .setInboundInterval(
                DateTimeInterval.fromFormattedValue("2020-03-20T12:00:00+01:00/2020-04-20T12:00:00+01:00")
            )
            .setOutboundInterval(
                DateTimeInterval.fromFormattedValue("2020-03-19T12:00:00+01:00/2020-04-19T12:00:00+01:00")
            )
            .setType(MovementType.LINEHAUL)
            .setSubtype(null)
            .build();
    }

    private static OffsetDateTime getFrom() {
        LocalDateTime from = LocalDateTime.of(2020, 3, 20, 12, 0, 0, 0);
        ZoneOffset offset = ZoneId.of("+01:00").getRules().getOffset(from);
        return OffsetDateTime.of(from, offset);
    }

    private static OffsetDateTime getTo() {
        LocalDateTime to = LocalDateTime.of(2020, 4, 20, 12, 0, 0, 0);
        ZoneOffset offset = ZoneId.of("+01:00").getRules().getOffset(to);
        return OffsetDateTime.of(to, offset);
    }

    // getMovementStatus and getMovementStatusHistory

    /**
     * Создаёт список идентификаторов перемещений.
     */
    public static List<ResourceId> createMovementIds() {
        return Arrays.asList(
            ResourceId.builder()
                .setYandexId("111424")
                .setPartnerId("111525")
                .build(),
            ResourceId.builder()
                .setYandexId("222424")
                .setPartnerId("222525")
                .build(),
            ResourceId.builder()
                .setYandexId("333424")
                .setPartnerId("333525")
                .build(),
            ResourceId.builder()
                .setPartnerId("444424")
                .build()
        );
    }

    /**
     * Создаёт список идентификаторов перемещений без идентификатора партнера.
     */
    public static List<ResourceId> createMovementIdsWithoutPartnerId() {
        return Arrays.asList(
            ResourceId.builder()
                .setYandexId("111424")
                .build(),
            ResourceId.builder()
                .setYandexId("222424")
                .setPartnerId("222525")
                .build(),
            ResourceId.builder()
                .setYandexId("333424")
                .setPartnerId("333525")
                .build()
        );
    }

    /**
     * Создаёт успешный ответ для метода DS.getMovementStatus.
     */
    public static ru.yandex.market.logistic.api.model.delivery.response.GetMovementStatusResponse
    createGetMovementStatusResponseDS() {
        return ru.yandex.market.logistic.api.model.delivery.response.GetMovementStatusResponse.builder(
            createMovementStatuses(createMovementIds(), createStatuses())
        ).build();
    }

    /**
     * Создаёт успешный ответ для метода FF.getMovementStatus.
     */
    public static GetMovementStatusResponse createGetMovementStatusResponseFF() {
        return GetMovementStatusResponse.builder(
            createMovementStatuses(createMovementIds(), createStatuses())
        ).build();
    }

    /**
     * Создаёт успешный ответ для метода DS.getMovementStatusHistory.
     */
    public static ru.yandex.market.logistic.api.model.delivery.response.GetMovementStatusHistoryResponse
    createGetMovementStatusHistoryResponseDS() {
        return ru.yandex.market.logistic.api.model.delivery.response.GetMovementStatusHistoryResponse.builder(
            createMovementStatusHistories(createMovementIds(), createStatusHistories())
        ).build();
    }

    /**
     * Создаёт успешный ответ для метода FF.getMovementStatusHistory.
     */
    public static GetMovementStatusHistoryResponse createGetMovementStatusHistoryResponseFF() {
        return GetMovementStatusHistoryResponse.builder(
            createMovementStatusHistories(createMovementIds(), createStatusHistories())
        ).build();
    }

    private static List<MovementStatus> createMovementStatuses(List<ResourceId> movementIds, List<Status> statuses) {
        return IntStream.range(0, Math.min(statuses.size(), movementIds.size()))
            .mapToObj(i -> MovementStatus.builder(movementIds.get(i), statuses.get(i)).build())
            .collect(Collectors.toList());
    }

    private static List<Status> createStatuses() {
        return Arrays.asList(
            Status.builder(CREATED, new DateTime("2020-03-20T12:00:00+01:00")).setMessage("Some message").build(),
            Status.builder(ERROR, new DateTime("2020-05-10T09:00:00")).setMessage("Another message").build(),
            Status.builder(DELIVERED, new DateTime("2021-04-20T16:45:00+04:00")).build(),
            Status.builder(CANCELLED_BY_PARTNER, new DateTime("2021-02-03T21:21:21")).build()
        );
    }

    private static List<MovementStatusHistory> createMovementStatusHistories(
        List<ResourceId> movementIds,
        List<List<Status>> statusHistories
    ) {
        return IntStream.range(0, Math.min(movementIds.size(), statusHistories.size()))
            .mapToObj(i -> MovementStatusHistory.builder(movementIds.get(i), statusHistories.get(i)).build())
            .collect(Collectors.toList());
    }

    private static List<List<Status>> createStatusHistories() {
        return Arrays.asList(
            Arrays.asList(
                Status.builder(PENDING, new DateTime("2020-03-20T12:00:00+01:00")).build(),
                Status.builder(CREATED, new DateTime("2020-03-20T14:00:00+01:00")).build()
            ),
            Collections.singletonList(
                Status.builder(ERROR, new DateTime("2020-03-20T12:00:00")).build()
            ),
            Arrays.asList(
                Status.builder(PENDING, new DateTime("2020-04-20T09:00:00+03:00")).build(),
                Status.builder(CREATED, new DateTime("2020-04-20T10:00:00+03:00")).build(),
                Status.builder(COURIER_FOUND, new DateTime("2020-04-20T14:00:00+03:00")).build(),
                Status.builder(HANDED_OVER, new DateTime("2020-04-20T14:17:00+03:00")).build(),
                Status.builder(DELIVERING, new DateTime("2020-04-21T21:05:00+03:00")).build(),
                Status.builder(DELIVERED, new DateTime("2020-04-20T12:00:00+03:00")).build()
            )
        );
    }

}
