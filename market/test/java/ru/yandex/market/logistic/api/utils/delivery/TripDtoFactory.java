package ru.yandex.market.logistic.api.utils.delivery;

import java.util.Collections;

import ru.yandex.market.logistic.api.model.common.ResourceId;
import ru.yandex.market.logistic.api.model.delivery.Trip;
import ru.yandex.market.logistic.api.model.delivery.response.PutTripResponse;
import ru.yandex.market.logistic.api.utils.common.MovementDtoFactory;

public class TripDtoFactory {

    private TripDtoFactory() {
        throw new UnsupportedOperationException();
    }

    public static ResourceId createTripId() {
        return ResourceId.builder().setYandexId("TMT1").setPartnerId("1").build();
    }

    public static Trip createTrip() {
        return new Trip(
            createTripId(),
            Collections.singletonList(
                MovementDtoFactory.createMovement()
            )
        );
    }

    public static PutTripResponse createPutTripResponseDs() {
        return new PutTripResponse(
            createTripId(), Collections.singletonList(MovementDtoFactory.createMovementId())
        );
    }
}
