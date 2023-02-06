package ru.yandex.market.api.partner.controllers.delivery;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.api.partner.context.Format;
import ru.yandex.market.api.partner.context.FunctionalTest;
import ru.yandex.market.api.partner.context.FunctionalTestHelper;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.mbi.util.MbiAsserts;

@DbUnitDataSet(before = "deliveryServices.csv")
public class DeliveryServicesControllerTest extends FunctionalTest {

    @Test
    void testReturningOnlyFavouritesServices() {
        ResponseEntity<String> response = makeRequest(Format.JSON);
        MbiAsserts.assertJsonEquals(getFileContent("favouritesDS.json"), response.getBody());
    }

    private String getFileContent(String fileName) {
        return StringTestUtil.getString(getClass(), fileName);
    }

    private ResponseEntity<String> makeRequest(Format format) {
        return FunctionalTestHelper.makeRequest(url(), HttpMethod.GET, format);
    }

    private String url() {
        return String.format("%s/delivery/services", urlBasePrefix);
    }
}
