package ru.yandex.market.logistic.api.client.fulfillment;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.exceptions.ValidationException;
import ru.yandex.market.logistic.api.model.fulfillment.UnitId;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetExpirationItemsResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.logistic.api.utils.fulfillment.DtoFactory.generateUnitIds;

/**
 * @author avetokhin 24.12.18.
 */
class GetExpirationItemsTest extends CommonServiceClientTest {

    private static final int UNIT_IDS_COUNT = 3;
    private static final int LIMIT = 10;
    private static final int OFFSET = 100;

    @Test
    void testGetExpirationItemsSucceeded() throws Exception {
        prepareMockServiceNormalized("ff_get_expiration_items", PARTNER_URL);

        GetExpirationItemsResponse response =
            fulfillmentClient.getExpirationItems(LIMIT, OFFSET, generateUnitIds(UNIT_IDS_COUNT),
                getPartnerProperties());
        assertEquals(
            1,
            response.getItemExpirationList().size(),
            "Должно вернуться корректное число itemExpiration"
        );
    }

    @Test
    void testGetExpirationItemsWithErrors() throws Exception {
        prepareMockServiceNormalized("ff_get_expiration_items_with_errors", PARTNER_URL);

        assertThrows(
            RequestStateErrorException.class,
            () -> fulfillmentClient.getExpirationItems(
                LIMIT,
                OFFSET,
                generateUnitIds(UNIT_IDS_COUNT),
                getPartnerProperties()
            )
        );
    }

    @Test
    void testGetExpirationItemsValidationFailed() {
        UnitId unitId = new UnitId.UnitIdBuilder(null, null).build();
        assertThrows(
            ValidationException.class,
            () -> fulfillmentClient.getExpirationItems(
                LIMIT,
                OFFSET,
                Collections.singletonList(unitId),
                getPartnerProperties()
            )
        );
    }

    @Test
    void testGetExpirationItemsEmptyResponseSucceeded() throws Exception {
        prepareMockServiceNormalized(
            "ff_get_expiration_items",
            "ff_get_expiration_items_empty",
            PARTNER_URL
        );

        fulfillmentClient.getExpirationItems(LIMIT, OFFSET, generateUnitIds(UNIT_IDS_COUNT), getPartnerProperties());
    }

}
