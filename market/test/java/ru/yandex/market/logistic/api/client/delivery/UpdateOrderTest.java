package ru.yandex.market.logistic.api.client.delivery;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.client.DeliveryServiceClient;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.exceptions.ValidationException;
import ru.yandex.market.logistic.api.model.delivery.Korobyte;
import ru.yandex.market.logistic.api.model.delivery.Location;
import ru.yandex.market.logistic.api.model.delivery.Order;
import ru.yandex.market.logistic.api.model.delivery.PartnerCode;
import ru.yandex.market.logistic.api.model.delivery.Place;
import ru.yandex.market.logistic.api.model.delivery.ResourceId;
import ru.yandex.market.logistic.api.model.delivery.Sender;
import ru.yandex.market.logistic.api.model.delivery.response.UpdateOrderResponse;
import ru.yandex.market.logistic.api.utils.delivery.DtoFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UpdateOrderTest extends CommonServiceClientTest {

    @Autowired
    private DeliveryServiceClient deliveryServiceClient;

    @Test
    void testUpdateOrderSucceeded() throws Exception {
        prepareMockServiceNormalized("ds_update_order", PARTNER_URL);

        UpdateOrderResponse response = deliveryServiceClient.updateOrder(DtoFactory.createOrder(),
            getPartnerProperties());
        assertEquals(
            getPlacesWithTracks(),
            response.getPlaces(),
            "Проверяем соответствие places"
        );
    }

    @Test
    void testUpdateOrderWithErrors() throws Exception {
        prepareMockServiceNormalized("ds_update_order_with_errors", PARTNER_URL);

        assertThrows(
            RequestStateErrorException.class,
            () -> deliveryServiceClient.updateOrder(DtoFactory.createOrder(), getPartnerProperties())
        );
    }

    @Test
    void testCreateOrderValidationFailed() {
        Location address = DtoFactory.createLocationFrom(null);
        Sender sender = DtoFactory.createSender("ИП «Тестовый виртуальный магазин проекта Фулфиллмент»", address, null);
        Order order = DtoFactory.createOrder(sender, null, null, null);

        assertThrows(
            ValidationException.class,
            () -> deliveryServiceClient.updateOrder(order, getPartnerProperties())
        );
    }

    private List<Place> getPlacesWithTracks() {
        return Collections.singletonList(
            new Place.PlaceBuilder(new ResourceId.ResourceIdBuilder().setYandexId("123").setDeliveryId("321").build())
                .setKorobyte(new Korobyte.KorobyteBuilder()
                    .setWidth(1)
                    .setHeight(2)
                    .setLength(3)
                    .setWeightGross(BigDecimal.valueOf(10))
                    .build()
                )
                .setPartnerCodes(Collections.singletonList(
                    new PartnerCode("555", "code555")))
                .setItemPlaces(DtoFactory.createItemPlaces())
                .build()
        );
    }
}
