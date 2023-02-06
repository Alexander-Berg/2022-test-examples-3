package ru.yandex.market.logistic.api.client.delivery;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.client.DeliveryServiceClient;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.exceptions.ValidationException;
import ru.yandex.market.logistic.api.model.delivery.Register;
import ru.yandex.market.logistic.api.model.delivery.Sender;
import ru.yandex.market.logistic.api.model.delivery.request.entities.ShipmentType;
import ru.yandex.market.logistic.api.utils.DateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.logistic.api.utils.delivery.DtoFactory.createResourceId;
import static ru.yandex.market.logistic.api.utils.delivery.DtoFactory.createSender;

class CreateRegisterTest extends CommonServiceClientTest {

    @Autowired
    private DeliveryServiceClient deliveryServiceClient;

    @Test
    void testCreateRegisterSucceeded() throws Exception {
        prepareMockServiceNormalized("ds_create_register", PARTNER_URL);
        deliveryServiceClient.createRegister(createRegister(), getPartnerProperties());
    }

    @Test
    void testCreateRegisterWithErrors() throws Exception {
        prepareMockServiceNormalized("ds_create_register", "ds_create_register_with_errors",
            PARTNER_URL);

        assertThrows(
            RequestStateErrorException.class,
            () -> deliveryServiceClient.createRegister(createRegister(), getPartnerProperties())
        );

    }

    @Test
    void testCreateRegisterValidationFailed() {
        Register register = createRegister(null);
        assertThrows(
            ValidationException.class,
            () -> deliveryServiceClient.createRegister(register, getPartnerProperties()));
    }

    @Test
    void testCreateRegisterWithEmptyPartnerId() throws Exception {
        prepareMockServiceNormalized(
            "ds_create_register",
            "ds_create_register_with_empty_partner_id",
            PARTNER_URL);

        assertThrows(
            ValidationException.class,
            () -> deliveryServiceClient.createRegister(createRegister(), getPartnerProperties()));
    }

    private static Register createRegister() {
        return createRegister(createSender());
    }

    private static Register createRegister(Sender sender) {
        return new Register.RegisterBuilder(createResourceId("1", "ext1"),
            Arrays.asList(createResourceId("2", "ext2"), createResourceId("3", "ext3")),
            new DateTime("2019-02-10T11:00:00+02:30"),
            sender,
            ShipmentType.ACCEPTANCE)
            .setShipmentId(createResourceId("4", "ext4"))
            .build();
    }
}
