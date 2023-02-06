package ru.yandex.market.api.partner.request;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.api.partner.context.Format;
import ru.yandex.market.api.partner.context.FunctionalTest;
import ru.yandex.market.api.partner.context.FunctionalTestHelper;
import ru.yandex.market.mbi.util.MbiAsserts;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Проверяем, что мы не вываливаем кишки, если нет тела запроса.
 */
public class RequestBodyIsMissingTest extends FunctionalTest {

    @Test
    public void testEmptyBodyXml() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> makeRequest(Format.XML, "")
        );
        assertThat(exception.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
        //language=xml
        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<response><error code=\"400\"><message>Required request body is missing</message>" +
                "</error><errors><error code=\"BAD_REQUEST\" message=\"Required request body is missing\"/>" +
                "</errors><status>ERROR</status></response>";
        MbiAsserts.assertXmlEquals(expected, exception.getResponseBodyAsString());
    }

    @Test
    public void testEmptyBodyJson() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> makeRequest(Format.JSON, "")
        );
        assertThat(exception.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
        //language=json
        String expected = "{\"error\":" +
                "{\"code\":400,\"message\":\"Required request body is missing\"}," +
                "\"errors\":[{\"code\":\"BAD_REQUEST\"," +
                "\"message\":\"Required request body is missing\"}]," +
                "\"status\":\"ERROR\"}";
        MbiAsserts.assertJsonEquals(expected, exception.getResponseBodyAsString());
    }

    private ResponseEntity<String> makeRequest(Format format, String body) {
        return FunctionalTestHelper.makeRequest(url(10774), HttpMethod.POST, format, body);
    }

    private String url(long campaignId) {
        return String.format("%s/campaigns/%d/outlets",
                urlBasePrefix, campaignId);
    }
}
