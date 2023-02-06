package ru.yandex.market.logistic.api.client.fulfillment;

import java.util.Arrays;
import java.util.List;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.exceptions.ValidationException;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.ReturnRegister;
import ru.yandex.market.logistic.api.model.fulfillment.Sender;
import ru.yandex.market.logistic.api.model.fulfillment.response.CreateReturnRegisterResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.logistic.api.utils.RequestValidationUtils.getNotEmptyErrorMessage;
import static ru.yandex.market.logistic.api.utils.RequestValidationUtils.getNotNullErrorMessage;
import static ru.yandex.market.logistic.api.utils.fulfillment.DtoFactory.createResourceId;
import static ru.yandex.market.logistic.api.utils.fulfillment.DtoFactory.createSender;

class CreateReturnRegisterTest extends CommonServiceClientTest {

    @Test
    void testCreateRegisterSucceeded() throws Exception {
        prepareMockServiceNormalized("ff_create_return_register", PARTNER_URL);
        CreateReturnRegisterResponse actualResponse =
            fulfillmentClient.createReturnRegister(createReturnRegister(), getPartnerProperties());

        assertEquals(getExpectedResponse(), actualResponse, "Asserting the response is correct");
    }

    @Test
    void testCreateRegisterWithErrors() throws Exception {
        prepareMockServiceNormalized(
            "ff_create_return_register",
            "ff_create_return_register_with_errors",
            PARTNER_URL
        );

        assertions.assertThatThrownBy(
            () -> fulfillmentClient.createReturnRegister(createReturnRegister(), getPartnerProperties())
        )
            .isInstanceOf(RequestStateErrorException.class)
            .hasMessage("Request processing failure");
    }

    @Test
    void validateNotNullOrders() {
        assertions.assertThatThrownBy(
            () -> fulfillmentClient.createReturnRegister(
                createReturnRegister(createSender(), null),
                getPartnerProperties()
            )
        )
            .isInstanceOf(ValidationException.class)
            .hasMessage(getNotEmptyErrorMessage("ordersId"));
    }

    @Test
    void validateNotEmptyOrders() {
        assertions.assertThatThrownBy(
            () -> fulfillmentClient.createReturnRegister(
                createReturnRegister(createSender(), ImmutableList.of()),
                getPartnerProperties()
            )
        )
            .isInstanceOf(ValidationException.class)
            .hasMessage(getNotEmptyErrorMessage("ordersId"));
    }

    @Test
    void validateNotNullSender() {
        assertions.assertThatThrownBy(
            () -> fulfillmentClient.createReturnRegister(
                createReturnRegister(null),
                getPartnerProperties()
            )
        )
            .isInstanceOf(ValidationException.class)
            .hasMessage(getNotNullErrorMessage("sender"));
    }

    private ReturnRegister createReturnRegister() {
        return createReturnRegister(createSender(),
            Arrays.asList(
                createResourceId("1", "ext1"),
                createResourceId("2", "ext2")));
    }

    private ReturnRegister createReturnRegister(Sender sender) {
        return createReturnRegister(sender,
            Arrays.asList(
                createResourceId("1", "ext1"),
                createResourceId("2", "ext2")));
    }

    private ReturnRegister createReturnRegister(Sender sender, List<ResourceId> orderIds) {
        return new ReturnRegister(orderIds, sender);
    }

    private CreateReturnRegisterResponse getExpectedResponse() {
        return new CreateReturnRegisterResponse();
    }
}
