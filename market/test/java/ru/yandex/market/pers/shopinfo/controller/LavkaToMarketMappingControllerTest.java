package ru.yandex.market.pers.shopinfo.controller;

import org.json.JSONException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.pers.shopinfo.test.context.FunctionalTest;

@DbUnitDataSet(before = "lavkaToMarket.csv")
class LavkaToMarketMappingControllerTest extends FunctionalTest {

    private static ResponseEntity<String> getLavkaToMarketMapping(String urlBasePrefix, String eatsAndLavkaId) {
        return FunctionalTestHelper.get(urlBasePrefix + "/lavkaToMarketId?eats-and-lavka-id" + "=" + eatsAndLavkaId);
    }

    @Test
    @DisplayName("GET /lavkaToMarketId")
    void testSuccessMarketId() throws JSONException {
        ResponseEntity<String> response = getLavkaToMarketMapping(urlBasePrefix, "SomeShop1");
        JSONAssert.assertEquals("" +
                "{\n" +
                "    \"partnerId\": 456,\n" +
                "}", response.getBody(), JSONCompareMode.NON_EXTENSIBLE);

        response = getLavkaToMarketMapping(urlBasePrefix, "SomeShop2");
        JSONAssert.assertEquals("" +
                "{\n" +
                "    \"partnerId\": 654,\n" +
                "}", response.getBody(), JSONCompareMode.NON_EXTENSIBLE);

        response = getLavkaToMarketMapping(urlBasePrefix, "SomeOtherShop");
        JSONAssert.assertEquals("" +
                "{\n" +
                "    \"partnerId\": 655,\n" +
                "}", response.getBody(), JSONCompareMode.NON_EXTENSIBLE);

    }

    @Test
    @DisplayName("GET /lavkaToMarketId. Не существует партнёра с таким eatsAndLavkaId")
    void testSupplierInfoNotFound() throws JSONException {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> getLavkaToMarketMapping(urlBasePrefix, "someUnknownLavkaId")
        );
        Assertions.assertEquals(exception.getRawStatusCode(), 404);
    }

    @Test
    @DisplayName("GET /lavkaToMarketId. В кэш попадают только партнёры от лавки.")
    void testAliveDbs() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> getLavkaToMarketMapping(urlBasePrefix, "anothershop")
        );
        Assertions.assertEquals(exception.getRawStatusCode(), 404);
    }

}

