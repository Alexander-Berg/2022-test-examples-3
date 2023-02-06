package ru.yandex.market.logistic.api.client.delivery;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.client.DeliveryServiceClient;
import ru.yandex.market.logistic.api.model.common.Car;
import ru.yandex.market.logistic.api.model.common.Courier;
import ru.yandex.market.logistic.api.model.common.OrderTransferCode;
import ru.yandex.market.logistic.api.model.common.OrderTransferCodes;
import ru.yandex.market.logistic.api.model.common.Person;
import ru.yandex.market.logistic.api.model.common.Phone;
import ru.yandex.market.logistic.api.model.delivery.response.GetCourierResponse;

import static ru.yandex.market.logistic.api.utils.delivery.DtoFactory.createResourceId;

class GetCourierTest extends CommonServiceClientTest {

    @Autowired
    private DeliveryServiceClient deliveryServiceClient;

    @Test
    void getCourierSucceeded() throws Exception {
        prepareMockServiceNormalized("ds_get_courier", PARTNER_URL);
        GetCourierResponse getCourierResponse = deliveryServiceClient.getCourier(
            createResourceId("yandexId-123", "partnerId-123"),
            getPartnerProperties()
        );
        assertions.assertThat(getCourierResponse)
            .usingRecursiveComparison()
            .isEqualTo(new GetCourierResponse(
                new Courier.CourierBuilder()
                    .setPersons(Collections.singletonList(new Person("Иван", "Иванов", "Иванович")))
                    .setCar(Car.builder("А001AA001").setDescription("Вишневая девятка").build())
                    .setPhone(new Phone("+79991234567", "123"))
                    .setUrl("https://go.yandex/route/6ea161f870ba6574d3bd9bdd19e1e9d8?lang=ru")
                    .build(),
                new OrderTransferCodes.OrderTransferCodesBuilder()
                    .setInbound(
                        new OrderTransferCode.OrderTransferCodeBuilder()
                            .setElectronicAcceptanceCertificate("202020")
                            .build()
                    )
                    .build()
            ));
    }

}
