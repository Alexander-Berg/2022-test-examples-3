package ru.yandex.market.api.partner.controllers.campaign;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.api.partner.context.Format;
import ru.yandex.market.api.partner.context.FunctionalTest;
import ru.yandex.market.api.partner.context.FunctionalTestHelper;
import ru.yandex.market.mbi.util.MbiAsserts;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Проверяем хэндлер на не существующий метод.
 */
public class MethodNotFoundTest extends FunctionalTest {

    private final static long CAMPAIGN_ID = 10774;

    @DisplayName("Получение 404 на неизвестный ресурс, а не 500, JSON.")
    @Test
    public void getNotResourceCampaignSettingsTestJson() {

        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(url(CAMPAIGN_ID), HttpMethod.GET, Format.JSON)
        );
        assertThat(exception.getStatusCode(), equalTo(HttpStatus.NOT_FOUND));
        String response = exception.getResponseBodyAsString();
        //language=json
        String expected = "{\"error\":" +
                "{\"code\":404,\"message\":\"Resource not found\"}," +
                "\"errors\":[{\"code\":\"NOT_FOUND\"," +
                "\"message\":\"Resource not found\"}]," +
                "\"status\":\"ERROR\"}";
        MbiAsserts.assertJsonEquals(expected, response);
    }

    @DisplayName("Получение 404 на неизвестный ресурс, а не 500, XML.")
    @Test
    public void getNotResourceCampaignSettingsTestXml() {

        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(url(CAMPAIGN_ID), HttpMethod.GET, Format.XML)
        );
        assertThat(exception.getStatusCode(), equalTo(HttpStatus.NOT_FOUND));
        String response = exception.getResponseBodyAsString();
        //language=xml
        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<response><error code=\"404\"><message>Resource not found</message>" +
                "</error><errors><error code=\"NOT_FOUND\" message=\"Resource not found\"/>" +
                "</errors><status>ERROR</status></response>";
        MbiAsserts.assertXmlEquals(expected, response);
    }

    private String url(long campaignId) {
        return String.format("%s/v1/campaigns/%d/settings",
                urlBasePrefix, campaignId);
    }
}
