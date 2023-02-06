package ru.yandex.market.logistic.api.client.fulfillment;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.exceptions.ValidationException;
import ru.yandex.market.logistic.api.model.fulfillment.Register;
import ru.yandex.market.logistic.api.model.fulfillment.request.entities.ShipmentType;
import ru.yandex.market.logistic.api.model.fulfillment.response.CreateRegisterResponse;
import ru.yandex.market.logistic.api.utils.DateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.logistic.api.utils.RequestValidationUtils.getNotEmptyErrorMessage;
import static ru.yandex.market.logistic.api.utils.RequestValidationUtils.getNotNullErrorMessage;
import static ru.yandex.market.logistic.api.utils.fulfillment.DtoFactory.createResourceId;
import static ru.yandex.market.logistic.api.utils.fulfillment.DtoFactory.createSender;

class CreateRegisterTest extends CommonServiceClientTest {
    @Test
    void testCreateRegisterSucceeded() throws Exception {
        prepareMockServiceNormalized("ff_create_register", PARTNER_URL);
        CreateRegisterResponse actualResponse =
            fulfillmentClient.createRegister(createRegisterBuilder().build(), getPartnerProperties());

        assertEquals(getExpectedResponse(), actualResponse, "Asserting the response is correct");
    }

    @Test
    void testCreateRegisterWithErrors() throws Exception {
        prepareMockServiceNormalized(
            "ff_create_register",
            "ff_create_register_with_errors",
            PARTNER_URL
        );
        assertThrows(
            RequestStateErrorException.class,
            () -> fulfillmentClient.createRegister(createRegisterBuilder().build(), getPartnerProperties())
        );
    }

    @Test
    void validateNullSender() {
        assertions.assertThatThrownBy(
            () -> fulfillmentClient.createRegister(
                createRegisterBuilder().setSender(null).build(),
                getPartnerProperties()
            )
        )
            .isInstanceOf(ValidationException.class)
            .hasMessage(getNotNullErrorMessage("sender"));
    }

    @Test
    void validateNullRegisterId() {
        assertions.assertThatThrownBy(
            () -> fulfillmentClient.createRegister(
                createRegisterBuilder().setRegisterId(null).build(),
                getPartnerProperties()
            )
        )
            .isInstanceOf(ValidationException.class)
            .hasMessage(getNotNullErrorMessage("registerId"));
    }

    @Test
    void validateNullShipmentDate() {
        assertions.assertThatThrownBy(
            () -> fulfillmentClient.createRegister(createRegisterBuilder().setShipmentDate(null).build(),
                getPartnerProperties())
        )
            .isInstanceOf(ValidationException.class)
            .hasMessage(getNotNullErrorMessage("shipmentDate"));
    }

    @Test
    void validateNullOrdersId() {
        assertions.assertThatThrownBy(
            () -> fulfillmentClient.createRegister(createRegisterBuilder().setOrdersId(null).build(),
                getPartnerProperties())
        )
            .isInstanceOf(ValidationException.class)
            .hasMessage(getNotEmptyErrorMessage("ordersId"));
    }

    @Test
    void validateEmptyOrdersId() {
        assertions.assertThatThrownBy(
            () -> fulfillmentClient.createRegister(
                createRegisterBuilder().setOrdersId(Collections.emptyList()).build(),
                getPartnerProperties())
        )
            .isInstanceOf(ValidationException.class)
            .hasMessage(getNotEmptyErrorMessage("ordersId"));
    }

    @Test
    void validateNullOrderId() {
        Register.RegisterBuilder registerBuilder = createRegisterBuilder();
        registerBuilder.setOrdersId(Arrays.asList(
            createResourceId("2", "ext2"),
            null,
            createResourceId("3", "ext3")
        ));

        assertions.assertThatThrownBy(
            () -> fulfillmentClient.createRegister(
                registerBuilder.build(),
                getPartnerProperties())
        )
            .isInstanceOf(ValidationException.class)
            .hasMessage(getNotNullErrorMessage("ordersId[1].<list element>"));
    }

    @Test
    void validateNullableValues() throws Exception {
        prepareMockServiceNormalized(
            "ff_create_register_with_only_required_fields",
            "ff_create_register",
            PARTNER_URL);
        fulfillmentClient.createRegister(
            createRegisterBuilder()
                .setShipmentType(null)
                .setShipmentId(null)
                .build(),
            getPartnerProperties());
    }

    private CreateRegisterResponse getExpectedResponse() {
        return new CreateRegisterResponse(createResourceId("1", "ext1"));
    }

    private static Register.RegisterBuilder createRegisterBuilder() {
        return new Register.RegisterBuilder(createResourceId("1", "ext1"),
            Arrays.asList(
                createResourceId("2", "ext2"),
                createResourceId("3", "ext3")
            ),
            new DateTime("2019-02-10T11:00:00+02:30"),
            createSender())
            .setShipmentDate(new DateTime("2019-02-10T11:00:00+02:30"))
            .setShipmentId(createResourceId("4", "ext4"))
            .setShipmentType(ShipmentType.ACCEPTANCE);
    }
}
