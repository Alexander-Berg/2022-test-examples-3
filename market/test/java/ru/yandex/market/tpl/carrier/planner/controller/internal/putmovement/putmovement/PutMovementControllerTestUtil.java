package ru.yandex.market.tpl.carrier.planner.controller.internal.putmovement.putmovement;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import ru.yandex.market.logistic.api.model.common.Address;
import ru.yandex.market.logistic.api.model.common.LegalEntity;
import ru.yandex.market.logistic.api.model.common.LegalForm;
import ru.yandex.market.logistic.api.model.common.Location;
import ru.yandex.market.logistic.api.model.common.LogisticPoint;
import ru.yandex.market.logistic.api.model.common.Movement;
import ru.yandex.market.logistic.api.model.common.Party;
import ru.yandex.market.logistic.api.model.common.Person;
import ru.yandex.market.logistic.api.model.common.Phone;
import ru.yandex.market.logistic.api.model.common.ResourceId;
import ru.yandex.market.logistic.api.model.common.TripInfo;
import ru.yandex.market.logistic.api.model.common.TripType;
import ru.yandex.market.logistic.api.model.common.request.RequestWrapper;
import ru.yandex.market.logistic.api.model.common.request.Token;
import ru.yandex.market.logistic.api.model.delivery.Trip;
import ru.yandex.market.logistic.api.model.delivery.request.PutMovementRequest;
import ru.yandex.market.logistic.api.model.delivery.request.PutTripRequest;
import ru.yandex.market.logistic.api.model.delivery.request.entities.restricted.PutMovementRestrictedData;
import ru.yandex.market.logistic.api.utils.DateTimeInterval;

public class PutMovementControllerTestUtil {

    public static final DateTimeInterval DEFAULT_INTERVAL = DateTimeInterval.fromFormattedValue(
            "2021-03-03T18:00:00+03:00/2021-03-03T21:00:00+03:00"
    );

    public static final DateTimeInterval INBOUND_DEFAULT_INTERVAL = DateTimeInterval.fromFormattedValue(
            "2021-03-03T20:00:00+03:00/2021-03-03T21:00:00+03:00"
    );

    public static final DateTimeInterval OUTBOUND_DEFAULT_INTERVAL = DateTimeInterval.fromFormattedValue(
            "2021-03-03T18:00:00+03:00/2021-03-03T19:00:00+03:00"
    );


    private PutMovementControllerTestUtil() {
        throw new UnsupportedOperationException();
    }

    public static RequestWrapper<PutMovementRequest> wrap(PutMovementRequest request) {
        return new RequestWrapper<>(new Token("ds_token"), "aa", "bb", request);
    }

    public static RequestWrapper<PutTripRequest> wrap(PutTripRequest request) {
        return new RequestWrapper<>(new Token("ds_token"), "aa", "bb", request);
    }

    public static PutMovementRequest prepareMovement(ResourceId movementResourceId) {
        return new PutMovementRequest(prepareMovementInternal(movementResourceId), null);
    }

    public static Movement prepareMovementInternal(ResourceId movementResourceId) {
        return prepareMovementInternal(movementResourceId, null);
    }

    public static PutMovementRequest prepareMovement(ResourceId movementResourceId, TripInfo tripInfo) {
        return prepareMovement(
                movementResourceId,
                new ResourceId("20", "1234"),
                new ResourceId("200", "5678"),
                tripInfo
        );
    }

    public static Movement prepareMovementInternal(ResourceId movementResourceId, TripInfo tripInfo) {
        return prepareMovementInternal(
                movementResourceId,
                new ResourceId("20", "1234"),
                new ResourceId("200", "5678"),
                tripInfo
        );
    }

    public static PutMovementRequest prepareMovement(ResourceId movementResourceId,
                                                     ResourceId shipperLocationId,
                                                     ResourceId receiverLocationId) {
        return prepareMovement(movementResourceId, shipperLocationId, receiverLocationId, null);
    }

    public static Movement prepareMovementInternal(ResourceId movementResourceId,
                                                     ResourceId shipperLocationId,
                                                     ResourceId receiverLocationId) {
        return prepareMovementInternal(movementResourceId, shipperLocationId, receiverLocationId, null);
    }

    public static PutMovementRequest prepareMovement(ResourceId movementResourceId,
                                                     ResourceId shipperLocationId,
                                                     ResourceId receiverLocationId,
                                                     TripInfo tripInfo) {
        return prepareMovement(movementResourceId, BigDecimal.ONE, shipperLocationId, receiverLocationId, tripInfo);
    }

    public static Movement prepareMovementInternal(ResourceId movementResourceId,
                                                     ResourceId shipperLocationId,
                                                     ResourceId receiverLocationId,
                                                     TripInfo tripInfo) {
        return prepareMovementInternal(movementResourceId, BigDecimal.ONE, shipperLocationId, receiverLocationId, tripInfo);
    }

    public static PutMovementRequest prepareMovement(ResourceId movementResourceId,
                                                     BigDecimal volume,
                                                     ResourceId shipperLocationId,
                                                     ResourceId receiverLocationId) {
        return prepareMovement(movementResourceId, volume, shipperLocationId, receiverLocationId, null);
    }

    public static Movement prepareMovementInternal(ResourceId movementResourceId,
                                                     BigDecimal volume,
                                                     ResourceId shipperLocationId,
                                                     ResourceId receiverLocationId) {
        return prepareMovementInternal(movementResourceId, volume, shipperLocationId, receiverLocationId, null);
    }

    public static PutMovementRequest prepareMovement(ResourceId movementResourceId,
                                                     BigDecimal volume,
                                                     ResourceId shipperLocationId,
                                                     ResourceId receiverLocationId,
                                                     TripInfo tripInfo) {
        return prepareMovement(movementResourceId, DEFAULT_INTERVAL, volume, shipperLocationId, receiverLocationId, tripInfo);
    }

    public static Movement prepareMovementInternal(ResourceId movementResourceId,
                                                     BigDecimal volume,
                                                     ResourceId shipperLocationId,
                                                     ResourceId receiverLocationId,
                                                     TripInfo tripInfo) {
        return prepareMovementInternal(movementResourceId, DEFAULT_INTERVAL, volume, shipperLocationId, receiverLocationId, tripInfo);
    }

    public static PutMovementRequest prepareMovement(ResourceId movementResourceId,
                                                     DateTimeInterval interval,
                                                     BigDecimal volume,
                                                     ResourceId shipperLocationId,
                                                     ResourceId receiverLocationId) {
        return prepareMovement(movementResourceId, interval, volume, shipperLocationId, receiverLocationId, null);
    }

    public static Movement prepareMovementInternal(ResourceId movementResourceId,
                                                     DateTimeInterval interval,
                                                     BigDecimal volume,
                                                     ResourceId shipperLocationId,
                                                     ResourceId receiverLocationId) {
        return prepareMovementInternal(movementResourceId, interval, volume, shipperLocationId, receiverLocationId, null);
    }

    public static PutMovementRequest prepareMovement(ResourceId movementResourceId,
                                                     DateTimeInterval interval,
                                                     BigDecimal volume,
                                                     ResourceId shipperLocationId,
                                                     ResourceId receiverLocationId,
                                                     TripInfo tripInfo) {
        return prepareMovement(movementResourceId, interval, volume, shipperLocationId, receiverLocationId,
                INBOUND_DEFAULT_INTERVAL, OUTBOUND_DEFAULT_INTERVAL, Function.identity(), null, tripInfo);
    }

    public static Movement prepareMovementInternal(ResourceId movementResourceId,
                                                     DateTimeInterval interval,
                                                     BigDecimal volume,
                                                     ResourceId shipperLocationId,
                                                     ResourceId receiverLocationId,
                                                     TripInfo tripInfo) {
        return prepareMovementInternal(movementResourceId, interval, volume, shipperLocationId, receiverLocationId,
                INBOUND_DEFAULT_INTERVAL, OUTBOUND_DEFAULT_INTERVAL, Function.identity(), null, tripInfo);
    }

    public static PutMovementRequest prepareMovement(ResourceId movementResourceId,
                                                     DateTimeInterval interval,
                                                     BigDecimal volume,
                                                     ResourceId shipperLocationId,
                                                     ResourceId receiverLocationId,
                                                     DateTimeInterval inboundInterval,
                                                     DateTimeInterval outboundInterval,
                                                     Function<Movement.MovementBuilder, Movement.MovementBuilder> customizer) {
        return prepareMovement(movementResourceId, interval, volume, shipperLocationId, receiverLocationId,
                inboundInterval, outboundInterval, customizer, null, null);
    }

    public static Movement prepareMovementInternal(ResourceId movementResourceId,
                                                     DateTimeInterval interval,
                                                     BigDecimal volume,
                                                     ResourceId shipperLocationId,
                                                     ResourceId receiverLocationId,
                                                     DateTimeInterval inboundInterval,
                                                     DateTimeInterval outboundInterval,
                                                     Function<Movement.MovementBuilder, Movement.MovementBuilder> customizer) {
        return prepareMovementInternal(movementResourceId, interval, volume, shipperLocationId, receiverLocationId,
                inboundInterval, outboundInterval, customizer, null, null);
    }

    public static PutMovementRequest prepareMovement(ResourceId movementResourceId,
                                                     DateTimeInterval interval,
                                                     BigDecimal volume,
                                                     ResourceId shipperLocationId,
                                                     ResourceId receiverLocationId,
                                                     DateTimeInterval inboundInterval,
                                                     DateTimeInterval outboundInterval,
                                                     Function<Movement.MovementBuilder, Movement.MovementBuilder> customizer,
                                                     PutMovementRestrictedData restrictedData) {
        return prepareMovement(movementResourceId, interval, volume, shipperLocationId, receiverLocationId, inboundInterval, outboundInterval, customizer, restrictedData, null);
    }

    public static Movement prepareMovementInternal(ResourceId movementResourceId,
                                                     DateTimeInterval interval,
                                                     BigDecimal volume,
                                                     ResourceId shipperLocationId,
                                                     ResourceId receiverLocationId,
                                                     DateTimeInterval inboundInterval,
                                                     DateTimeInterval outboundInterval,
                                                     Function<Movement.MovementBuilder, Movement.MovementBuilder> customizer,
                                                     PutMovementRestrictedData restrictedData) {

        return prepareMovementInternal(movementResourceId, interval, volume, shipperLocationId, receiverLocationId, inboundInterval, outboundInterval, customizer, restrictedData, null);
    }

    public static PutMovementRequest prepareMovement(ResourceId movementResourceId,
                                                     DateTimeInterval interval,
                                                     BigDecimal volume,
                                                     ResourceId shipperLocationId,
                                                     ResourceId receiverLocationId,
                                                     DateTimeInterval inboundInterval,
                                                     DateTimeInterval outboundInterval,
                                                     Function<Movement.MovementBuilder, Movement.MovementBuilder> customizer,
                                                     PutMovementRestrictedData restrictedData,
                                                     TripInfo tripInfo) {
        Movement movement = prepareMovementInternal(movementResourceId, interval, volume,
                shipperLocationId, receiverLocationId,
                inboundInterval, outboundInterval,
                customizer,
                restrictedData, tripInfo);

        return new PutMovementRequest(movement, restrictedData);
    }

    public static Movement prepareMovementInternal(ResourceId movementResourceId,
                                                     DateTimeInterval interval,
                                                     BigDecimal volume,
                                                     ResourceId shipperLocationId,
                                                     ResourceId receiverLocationId,
                                                     DateTimeInterval inboundInterval,
                                                     DateTimeInterval outboundInterval,
                                                     Function<Movement.MovementBuilder, Movement.MovementBuilder> customizer,
                                                     PutMovementRestrictedData restrictedData,
                                                     TripInfo tripInfo) {
        Movement.MovementBuilder movementBuilder = Movement.builder(
                        movementResourceId,
                        interval,
                        volume
                )
                .setTrip(tripInfo)
                .setWeight(null)
                .setInboundInterval(inboundInterval)
                .setOutboundInterval(outboundInterval)
                .setShipper(new Party(fromLocation(shipperLocationId), legalEntity(), null))
                .setReceiver(new Party(toLocation(receiverLocationId), legalEntity(), null))
                .setComment(null)
                .setMaxPalletCapacity(1);

        return customizer.apply(movementBuilder).build();
    }

    public static LegalEntity legalEntity() {
        return new LegalEntity(
                "ООО Яндекс Маркет",
                "ООО Яндекс Маркет",
                LegalForm.OOO,
                "1167746491395",
                "7704357909",
                "770401001",
                Address.builder(
                        "121099, город Москва, Новинский бульвар, дом 8, помещение 9.03 этаж 9"
                ).build(),
                "",
                "",
                "",
                ""
        );
    }

    public static LogisticPoint fromLocation(ResourceId logisticPointId) {
        return LogisticPoint.builder(logisticPointId)
                .setLocation(new Location(
                        "Россия",
                        "ЦФО",
                        "Москва",
                        "Москва",
                        "Москва",
                        "",
                        "Новинский бульвар",
                        "8",
                        null,
                        null,
                        null,
                        "101000",
                        "-",
                        5,
                        "Смоленская",
                        BigDecimal.valueOf(55.751310),
                        BigDecimal.valueOf(37.584613),
                        213L,
                        ""
                ))
                .setPhones(List.of(
                        new Phone("88005553535", "123"),
                        new Phone("123456789", null)
                ))
                .setContact(new Person("Имя", "Фамилия", null))
                .build();
    }

    public static LogisticPoint toLocation(ResourceId logisticPointId) {
        return LogisticPoint.builder(logisticPointId)
                .setLocation(new Location(
                        "Россия",
                        "ЦФО",
                        "Москва",
                        "Москва",
                        "Москва",
                        "",
                        "ул. Логистическая",
                        "1",
                        null,
                        null,
                        null,
                        "101000",
                        "-",
                        5,
                        "Котельники",
                        BigDecimal.valueOf(55.751310),
                        BigDecimal.valueOf(37.584613),
                        213L,
                        ""
                ))
                .build();
    }


    public static PutTripRequest prepareTrip(ResourceId tripId, List<Movement> movements) {
        return new PutTripRequest(new Trip(tripId, movements));
    }

    public static List<Movement> prepareTripMovements(ResourceId tripId, List<ResourceId> movementExternalIds) {
        int count = movementExternalIds.size();
        AtomicInteger order = new AtomicInteger(0);
        return movementExternalIds.stream()
                .map(movementId -> prepareMovementInternal(
                        movementId,
                        DEFAULT_INTERVAL,
                        BigDecimal.ONE,
                        new ResourceId("20", "1234"),
                        new ResourceId("200", "5678"),
                        INBOUND_DEFAULT_INTERVAL,
                        OUTBOUND_DEFAULT_INTERVAL,
                        Function.identity(),
                        null,
                        new TripInfo(tripId, "Test route", order.get(), count * 2 - order.getAndIncrement() - 1, count * 2, 10L, TripType.MAIN)
                ))
                .collect(Collectors.toList());
    }

    public static List<Movement> prepareTripMovements(ResourceId tripId, int count) {
        return IntStream.range(0, count)
                .mapToObj(order -> prepareMovementInternal(
                        new ResourceId("TMM" + order, null),
                        DEFAULT_INTERVAL,
                        BigDecimal.ONE,
                        new ResourceId("20", "1234"),
                        new ResourceId("200", "5678"),
                        INBOUND_DEFAULT_INTERVAL,
                        OUTBOUND_DEFAULT_INTERVAL,
                        Function.identity(),
                        null,
                        new TripInfo(tripId, "Test route", order, count * 2 - order - 1, count * 2, 10L, TripType.MAIN)
                        ))
                .collect(Collectors.toList());
    }
}
