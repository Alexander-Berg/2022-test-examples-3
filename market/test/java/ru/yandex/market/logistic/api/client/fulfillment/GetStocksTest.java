package ru.yandex.market.logistic.api.client.fulfillment;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.exceptions.ValidationException;
import ru.yandex.market.logistic.api.model.fulfillment.UnitId;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetStocksResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.logistic.api.utils.fulfillment.DtoFactory.generateUnitIds;

class GetStocksTest extends CommonServiceClientTest {

    private static final int UNIT_IDS_COUNT = 3;
    private static final int LIMIT = 10;
    private static final int OFFSET = 100;

    @Test
    void testGetStockSucceeded() throws Exception {
        prepareMockServiceNormalized("ff_get_stocks", PARTNER_URL);

        GetStocksResponse response =
            fulfillmentClient.getStocks(LIMIT, OFFSET, generateUnitIds(UNIT_IDS_COUNT), getPartnerProperties());
        assertEquals(
            2,
            response.getItemStocksList().size(),
            "Должно вернуться корректное число itemStocks"
        );
    }

    @Test
    void testGetStocksWithErrors() throws Exception {
        prepareMockServiceNormalized("ff_get_stocks_with_errors", PARTNER_URL);

        assertThrows(
            RequestStateErrorException.class,
            () -> fulfillmentClient.getStocks(LIMIT, OFFSET, generateUnitIds(UNIT_IDS_COUNT), getPartnerProperties())
        );
    }

    @Test
    void testGetStocksValidationFailed() {
        UnitId unitId = new UnitId.UnitIdBuilder(null, null).build();
        assertThrows(
            ValidationException.class,
            () -> fulfillmentClient.getStocks(LIMIT, OFFSET, Collections.singletonList(unitId), getPartnerProperties())
        );
    }

    @Test
    void testGetStocksEmptyResponseSucceeded() throws Exception {
        prepareMockServiceNormalized(
            "ff_get_stocks",
            "ff_get_stocks_empty",
            PARTNER_URL
        );

        fulfillmentClient.getStocks(LIMIT, OFFSET, generateUnitIds(UNIT_IDS_COUNT), getPartnerProperties());
    }
}
