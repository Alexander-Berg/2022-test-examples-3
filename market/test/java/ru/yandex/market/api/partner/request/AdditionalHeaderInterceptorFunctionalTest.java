package ru.yandex.market.api.partner.request;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.api.partner.context.Format;
import ru.yandex.market.api.partner.context.FunctionalTest;
import ru.yandex.market.api.partner.context.FunctionalTestHelper;
import ru.yandex.market.common.test.db.DbUnitDataSet;

@DbUnitDataSet(before = "AdditionalHeaderInterceptorFunctionalTest.before.csv")
class AdditionalHeaderInterceptorFunctionalTest extends FunctionalTest {

    private static final String X_REQUEST_TAGS_HEADER = "X-Request-Tags";

    @DisplayName("Получение заголовка X-Request-Tags белой кампании")
    @Test
    void getWhiteCampaignByIdTestJson() {
        ResponseEntity<String> response = FunctionalTestHelper.makeRequest(String.format("%s/campaigns/%d",
                urlBasePrefix, 10774L), HttpMethod.GET, Format.JSON);
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertEquals(1, response.getHeaders().get(X_REQUEST_TAGS_HEADER).size());
        Assertions.assertEquals("SHOP", response.getHeaders().getFirst(X_REQUEST_TAGS_HEADER));
    }

    @DisplayName("Получение заголовка X-Request-Tags синей кампании")
    @Test
    void getBlueCampaignByIdTestJson() {
        ResponseEntity<String> response = FunctionalTestHelper.makeRequest(String.format("%s/campaigns/%d",
                urlBasePrefix, 1000571241L), HttpMethod.GET, Format.JSON);
        Assertions.assertEquals(1, response.getHeaders().get(X_REQUEST_TAGS_HEADER).size());
        Assertions.assertEquals("SUPPLIER", response.getHeaders().getFirst(X_REQUEST_TAGS_HEADER));
    }

    @DisplayName("Не получение заголовка X-Request-Tags списка кампаний")
    @Test
    void getCampaignsByUidTestJson() {
        ResponseEntity<String> response = FunctionalTestHelper.makeRequest(urlBasePrefix + "/campaigns",
                HttpMethod.GET, Format.JSON);
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertFalse(response.getHeaders().containsKey(X_REQUEST_TAGS_HEADER));

    }
}
