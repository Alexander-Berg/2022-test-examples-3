package ru.yandex.market.logistic.api.client.fulfillment;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.exceptions.ValidationException;
import ru.yandex.market.logistic.api.model.fulfillment.ErrorCode;
import ru.yandex.market.logistic.api.model.fulfillment.ErrorItem;
import ru.yandex.market.logistic.api.model.fulfillment.ErrorPair;
import ru.yandex.market.logistic.api.model.fulfillment.UnitId;
import ru.yandex.market.logistic.api.model.fulfillment.response.PutReferenceItemsResponse;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.logistic.api.utils.fulfillment.DtoFactory.createItemWithEmptyOutboundRemainingLifetimes;
import static ru.yandex.market.logistic.api.utils.fulfillment.DtoFactory.createItemWithInvalidLifetimePercentage;
import static ru.yandex.market.logistic.api.utils.fulfillment.DtoFactory.createItemsWithRemainingLifetimes;

class PutReferenceItemsTest extends CommonServiceClientTest {

    @Test
    void testPutReferenceItemsSucceeded() throws Exception {
        prepareMockServiceNormalized("ff_put_reference_items", PARTNER_URL);

        PutReferenceItemsResponse response = fulfillmentClient.putReferenceItems(
            createItemsWithRemainingLifetimes(),
            getPartnerProperties()
        );

        assertEquals(getExpectedResponse(), response);
    }

    @Test
    void testPutReferenceItemsWithErrors() throws Exception {
        prepareMockServiceNormalized(
            "ff_put_reference_items",
            "ff_put_reference_items_with_errors",
            PARTNER_URL
        );
        assertThrows(
            RequestStateErrorException.class,
            () -> fulfillmentClient.putReferenceItems(createItemsWithRemainingLifetimes(), getPartnerProperties())
        );
    }

    @Test
    void testPutReferenceItemsValidationFailed() {
        assertThrows(
            ValidationException.class,
            () -> fulfillmentClient.putReferenceItems(null, getPartnerProperties())
        );
    }

    @Test
    void testPutReferenceItemsResponseValidationFailed() throws Exception {
        prepareMockServiceNormalized(
            "ff_put_reference_items",
            "ff_put_reference_items_not_valid",
            PARTNER_URL
        );
        assertThrows(
            ValidationException.class,
            () -> fulfillmentClient.putReferenceItems(createItemsWithRemainingLifetimes(), getPartnerProperties())
        );
    }

    @Test
    void testPutReferenceItemsWithShelfLivePercentageEqualsOneHundred() {
        ValidationException validationException = assertThrows(
            ValidationException.class,
            () -> fulfillmentClient.putReferenceItems(createItemWithInvalidLifetimePercentage(), getPartnerProperties())
        );

        assertTrue(validationException.getMessage().contains("must be less than or equal to 99"));
    }

    @Test
    void testPutReferenceItemsWithoutEmptyShelfLivesOutbound() {
        ValidationException validationException = assertThrows(
            ValidationException.class,
            () -> fulfillmentClient.putReferenceItems(
                createItemWithEmptyOutboundRemainingLifetimes(),
                getPartnerProperties()
            )
        );

        assertTrue(validationException.getMessage()
            .contains(
                "At least one of these fields must be passed [days, percentage]', "
                    + "propertyPath=request.items[0].remainingLifetimes.outbound"
            )
        );
    }

    private PutReferenceItemsResponse getExpectedResponse() {
        ErrorItem errorItem = new ErrorItem.ErrorItemBuilder(
            new UnitId.UnitIdBuilder(549309L, "4607086562215").setId("100561421890").build())
            .setErrorCode(new ErrorPair.ErrorPairBuilder(ErrorCode.UNKNOWN_ERROR, "Не прошел валидацию").build())
            .build();
        return new PutReferenceItemsResponse(Collections.singletonList(errorItem));
    }
}
