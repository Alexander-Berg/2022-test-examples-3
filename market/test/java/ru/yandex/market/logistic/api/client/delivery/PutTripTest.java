package ru.yandex.market.logistic.api.client.delivery;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.client.DeliveryServiceClient;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.model.delivery.response.PutTripResponse;
import ru.yandex.market.logistic.api.model.properties.PartnerProperties;
import ru.yandex.market.logistic.api.utils.delivery.TripDtoFactory;

public class PutTripTest extends CommonServiceClientTest {

    @Autowired
    private DeliveryServiceClient client;

    @Test
    void testSuccessfulResponse() throws Exception {
        PartnerProperties props = getPartnerProperties();
        prepareMockServiceNormalized("put_trip", props.getUrl());
        PutTripResponse response = client.putTrip(TripDtoFactory.createTrip(), props);
        assertions.assertThat(response).isEqualTo(TripDtoFactory.createPutTripResponseDs());
    }

    @Test
    void testErrorResponse() throws Exception {
        PartnerProperties props = getPartnerProperties();
        prepareMockServiceNormalized("put_trip", "put_trip_with_errors", props.getUrl());
        assertions.assertThatThrownBy(() -> client.putTrip(TripDtoFactory.createTrip(), props))
            .hasMessage("Omg something terrible happened")
            .isInstanceOf(RequestStateErrorException.class);
    }
}
