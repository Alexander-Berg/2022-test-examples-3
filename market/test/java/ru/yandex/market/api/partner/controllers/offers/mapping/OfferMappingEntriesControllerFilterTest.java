package ru.yandex.market.api.partner.controllers.offers.mapping;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.api.partner.context.Format;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class OfferMappingEntriesControllerFilterTest extends OfferMappingEntriesTest {

    private static final String ERROR_JSON = "OfferMappingEntriesControllerFilterTest.error.json";
    private static final String ERROR_STATUS_JSON = "OfferMappingEntriesControllerFilterTest.errorStatus.json";

    @Test
    @DbUnitDataSet(before = "OfferMappingEntriesControllerTest.before.csv")
    void simultaneouslyFiltersAndShopSku() {
        HttpClientErrorException exception = assertThrows(HttpClientErrorException.class, () ->
                getEntries(Format.JSON,
                        "?shop_sku=no_here&status=READY&availability=ACTIVE&category_id=1&vendor=Tesla"));
        JsonTestUtil.assertEquals(
                exception.getResponseBodyAsString(), JsonTestUtil.parseJson(this.getClass(), ERROR_JSON).toString());
    }

    @Test
    @DbUnitDataSet(before = "OfferMappingEntriesControllerTest.before.csv")
    void usingUnknownStatus() {
        HttpClientErrorException exception = assertThrows(HttpClientErrorException.class, () ->
                getEntries(Format.JSON,
                        "?status=READY,UNKNOWN&vendor=Tesla"));
        JsonTestUtil.assertEquals(
                exception.getResponseBodyAsString(),
                JsonTestUtil.parseJson(this.getClass(), ERROR_STATUS_JSON).toString());
    }
}
