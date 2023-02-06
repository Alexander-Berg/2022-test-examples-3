package ru.yandex.market.api.partner.controllers.feed;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.api.partner.context.Format;
import ru.yandex.market.api.partner.context.FunctionalTest;
import ru.yandex.market.api.partner.context.FunctionalTestHelper;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.util.MbiAsserts;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.market.core.matchers.HttpClientErrorMatcher.hasErrorCode;
import static ru.yandex.market.core.matchers.HttpClientErrorMatcher.hasErrorMessage;


public class FeedParameterControllerTest extends FunctionalTest {

    private static final long CAMPAIGN_ID = 10774;

    @DisplayName("Удачный сценарий сохранения значения периода обхода в базу, JSON.")
    @Test
    @DbUnitDataSet(
            before = "FeedParameterControllerTest.before.csv",
            after = "FeedParameterControllerTest.after.csv"
    )
    public void setFeedParameterRIMTestJson() {
        String body = "{\"parameters\":[{\"name\":\"reparseIntervalMinutes\", \"values\":[\"62\"]}]}";
        ResponseEntity<String> response = makeRequest(123, Format.JSON, body);
        checkJsonOKResponse(response);
    }

    @DisplayName("Удачный сценарий сохранения значения периода обхода в базу, XML.")
    @Test
    @DbUnitDataSet(
            before = "FeedParameterControllerTest.before.csv",
            after = "FeedParameterControllerTest.after.csv"
    )
    public void setFeedParameterRIMTestXml() {
        String body = "<feed-parameters><parameters><parameter><name>reparseIntervalMinutes</name>" +
                "<values><value>62</value></values></parameter></parameters></feed-parameters>";
        ResponseEntity<String> response = makeRequest(123, Format.XML, body);
        checkXmlOKResponse(response);
    }

    @DisplayName("Удачный сценарий сохранения ПУСТОГО значения периода обхода в базу, JSON.")
    @Test
    @DbUnitDataSet(
            before = "FeedParameterControllerTestFillInterval.after.csv",
            after = "FeedParameterControllerTestFillInterval.after.csv"
    )
    public void setFeedParameterEmptyRIMTestJson() {
        String body = "{\"parameters\":[{\"name\":\"reparseIntervalMinutes\", \"deleted\":\"true\"}]}";
        ResponseEntity<String> response = makeRequest(123, Format.JSON, body);
        checkJsonOKResponse(response);
    }

    @DisplayName("Удачный сценарий сохранения ПУСТОГО значения периода обхода в базу, XML.")
    @Test
    @DbUnitDataSet(
            before = "FeedParameterControllerTestFillInterval.after.csv",
            after = "FeedParameterControllerTestFillInterval.after.csv"
    )
    public void setFeedParameterEmptyRIMTestXml() {
        String body = "<feed-parameters><parameters><parameter>" +
                "<name>reparseIntervalMinutes</name>" +
                "<values><value>62</value></values>" +
                "<deleted>true</deleted>" +
                "</parameter></parameters></feed-parameters>";
        ResponseEntity<String> response = makeRequest(123, Format.XML, body);
        checkXmlOKResponse(response);
    }

    @DisplayName("Тест, что не сохранится отрицательное значение RIM, JSON.")
    @Test
    @DbUnitDataSet(
            before = "FeedParameterControllerTest.after.csv",
            after = "FeedParameterControllerTest.after.csv"
    )
    public void setFeedParameterNegativeRIMTestJson() {
        HttpClientErrorException httpClientErrorException = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> {
                    String body = "{\"parameters\":[{\"name\":\"reparseIntervalMinutes\", \"values\":[\"-62\"]}]}";
                    makeRequest(123, Format.JSON, body);
                }
        );
        MatcherAssert.assertThat(
                httpClientErrorException,
                allOf(
                        hasErrorCode(HttpStatus.BAD_REQUEST),
                        hasErrorMessage("Feed reparse interval minutes should be positive")
                )
        );
    }

    @DisplayName("Тест, что не сохранится не числовое значение RIM, JSON.")
    @Test
    @DbUnitDataSet(before = "FeedParameterControllerTest.after.csv", after = "FeedParameterControllerTest.after.csv")
    public void setFeedParameterStrRIMTestJson() {
        HttpClientErrorException httpClientErrorException = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> {
                    String body = "{\"parameters\":[{\"name\":\"reparseIntervalMinutes\", \"values\":[\"str\"]}]}";
                    makeRequest(123, Format.JSON, body);
                }
        );
        MatcherAssert.assertThat(
                httpClientErrorException,
                allOf(
                        hasErrorCode(HttpStatus.BAD_REQUEST),
                        hasErrorMessage("Could not parse feed reparse interval")
                )
        );
    }

    @DisplayName("Тест, что не сохранится значение больше 24 часов в минутах (1440), JSON.")
    @Test
    @DbUnitDataSet(before = "FeedParameterControllerTest.after.csv", after = "FeedParameterControllerTest.after.csv")
    public void setFeedParameterBigRIMTestJson() {
        HttpClientErrorException httpClientErrorException = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> {
                    String body = "{\"parameters\":[{\"name\":\"reparseIntervalMinutes\", \"values\":[\"2018\"]}]}";
                    makeRequest(123, Format.JSON, body);
                }
        );
        MatcherAssert.assertThat(
                httpClientErrorException,
                allOf(
                        hasErrorCode(HttpStatus.BAD_REQUEST),
                        hasErrorMessage("Feed reparse interval minutes should be less than 24 hours")
                )
        );
    }

    @DisplayName("Проверка сохранения значения для неизвестного фида, JSON.")
    @Test
    @DbUnitDataSet
    public void unknownFeedParameterRIMTestJson() {
        HttpClientErrorException httpClientErrorException = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> {
                    String body = "{\"parameters\":[{\"name\":\"reparseIntervalMinutes\", \"deleted\":\"true\"}]}";
                    makeRequest(1234, Format.JSON, body);
                }
        );
        MatcherAssert.assertThat(
                httpClientErrorException,
                allOf(
                        hasErrorCode(HttpStatus.NOT_FOUND),
                        hasErrorMessage("Feed not found: 1234")
                )
        );
    }

    @DisplayName("Проверка сохранения значения для фида от другой кампании, JSON")
    @Test
    @DbUnitDataSet
    public void wrongFeedParameterRIMTestJson() {
        HttpClientErrorException httpClientErrorException = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> {
                    String body = "{\"parameters\":[{\"name\":\"reparseIntervalMinutes\", \"value\":[\"74\"]}]}";
                    makeRequest(1023, Format.XML, body);
                }
        );
        MatcherAssert.assertThat(
                httpClientErrorException,
                allOf(
                        hasErrorCode(HttpStatus.NOT_FOUND),
                        hasErrorMessage("Feed not found: 1023")
                )
        );

    }

    @DisplayName("Проверка сохранения значения для фида от другой кампании, XML.")
    @Test
    @DbUnitDataSet
    public void wrongFeedParameterRIMTestXml() {
        HttpClientErrorException httpClientErrorException = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> {
                    String body = "<feed-parameters><parameters><parameter>" +
                            "<name>reparseIntervalMinutes</name>" +
                            "<values><value>84</value></values>" +
                            "<deleted>true</deleted>" +
                            "</parameter></parameters></feed-parameters>";
                    makeRequest(1023, Format.XML, body);
                }
        );
        MatcherAssert.assertThat(
                httpClientErrorException,
                allOf(
                        hasErrorCode(HttpStatus.NOT_FOUND)
                )
        );

    }

    private ResponseEntity<String> makeRequest(long feedId, Format format, String body) {
        return FunctionalTestHelper.makeRequest(url(CAMPAIGN_ID, feedId), HttpMethod.POST, format, body);
    }

    private String url(long campaignId, long feedId) {
        return String.format("%s/campaigns/%d/feeds/%d/params",
                urlBasePrefix, campaignId, feedId);
    }

    private void checkJsonOKResponse(ResponseEntity<String> response) {
        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
        String expected = "{\n" +
                "    \"status\": \"OK\"\n" +
                "}";
        MbiAsserts.assertJsonEquals(expected, response.getBody());
    }

    private void checkXmlOKResponse(ResponseEntity<String> response) {
        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
        String expected = "" +
                "<response>" +
                "    <status>OK</status>" +
                "</response>\n";
        MbiAsserts.assertXmlEquals(expected, response.getBody());
    }

}
