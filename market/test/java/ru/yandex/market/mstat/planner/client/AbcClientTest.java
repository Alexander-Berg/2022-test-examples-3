package ru.yandex.market.mstat.planner.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import junit.framework.TestCase;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.mockito.Mockito;

import java.net.HttpURLConnection;
import java.time.LocalDate;

import static java.util.Collections.singletonList;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.mstat.planner.util.RestUtil.parseLocalDateYMDFormat;

public class AbcClientTest extends TestCase {

    @SneakyThrows
    public void testGetDutyShifts() {
        LocalDate fromDate = parseLocalDateYMDFormat("2020-01-01");
        LocalDate toDate = parseLocalDateYMDFormat("2020-10-01");
        String login_correct = "orphie";

        String expectedData = "[{\"id\":752065,\"person\":{\"login\":\"katretyakova\",\"uid\":\"1120000000069662\"},\"schedule\":{\"id\":1847,\"name\":\"\\u0411\\u0435\\u043a\\u043b\\u043e\\u0433 \\u041c\\u0430\\u0440\\u043a\\u0435\\u0442\\u0430\"},\"is_approved\":true,\"start\":\"2020-09-28\",\"end\":\"2020-10-09\",\"start_datetime\":\"2020-09-28T00:00:00+03:00\",\"end_datetime\":\"2020-10-10T00:00:00+03:00\",\"replaces\":[]},{\"id\":484311,\"person\":{\"login\":\"moskovkin\",\"uid\":\"1120000000047770\"},\"schedule\":{\"id\":700,\"name\":\"\\u0414\\u0435\\u0436\\u0443\\u0440\\u0441\\u0442\\u0432\\u0430\"},\"is_approved\":true,\"start\":\"2020-10-12\",\"end\":\"2020-10-18\",\"start_datetime\":\"2020-10-12T00:00:00+03:00\",\"end_datetime\":\"2020-10-19T00:00:00+03:00\",\"replaces\":[]},{\"id\":752066,\"person\":{\"login\":\"avi2d\",\"uid\":\"1120000000112966\"},\"schedule\":{\"id\":1847,\"name\":\"\\u0411\\u0435\\u043a\\u043b\\u043e\\u0433 \\u041c\\u0430\\u0440\\u043a\\u0435\\u0442\\u0430\"},\"is_approved\":true,\"start\":\"2020-10-12\",\"end\":\"2020-10-23\",\"start_datetime\":\"2020-10-12T00:00:00+03:00\",\"end_datetime\":\"2020-10-24T00:00:00+03:00\",\"replaces\":[]},{\"id\":782997,\"person\":{\"login\":\"rkbeseda\",\"uid\":\"1120000000113922\"},\"schedule\":{\"id\":2292,\"name\":\"\\u0414\\u0435\\u0436\\u0443\\u0440\\u0441\\u0442\\u0432\\u043e b2b \\u0430\\u043d\\u0430\\u043b\\u0438\\u0442\\u0438\\u043a\\u0438\"},\"is_approved\":true,\"start\":\"2020-10-19\",\"end\":\"2020-10-25\",\"start_datetime\":\"2020-10-19T00:00:00+03:00\",\"end_datetime\":\"2020-10-26T00:00:00+03:00\",\"replaces\":[]},{\"id\":782998,\"person\":{\"login\":\"ritagolub\",\"uid\":\"1120000000182023\"},\"schedule\":{\"id\":2292,\"name\":\"\\u0414\\u0435\\u0436\\u0443\\u0440\\u0441\\u0442\\u0432\\u043e b2b \\u0430\\u043d\\u0430\\u043b\\u0438\\u0442\\u0438\\u043a\\u0438\"},\"is_approved\":true,\"start\":\"2020-10-26\",\"end\":\"2020-11-01\",\"start_datetime\":\"2020-10-26T00:00:00+03:00\",\"end_datetime\":\"2020-11-02T00:00:00+03:00\",\"replaces\":[]}]";
        String testData = "{\"results\" :" + expectedData + "}";

        HttpURLConnection mockHttpConnectionFirst = Mockito.mock(HttpURLConnection.class);
        Mockito.when(mockHttpConnectionFirst.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        Mockito.when(mockHttpConnectionFirst.getInputStream()).thenReturn(IOUtils.toInputStream(testData));

        final AbstractClient.UrlWrapper mockURLFirst = mock(AbstractClient.UrlWrapper.class);
        Mockito.when(mockURLFirst.openConnection()).thenReturn(mockHttpConnectionFirst);

        HttpURLConnection mockHttpConnectionSecond = Mockito.mock(HttpURLConnection.class);
        Mockito.when(mockHttpConnectionSecond.getResponseCode()).thenReturn(HttpURLConnection.HTTP_BAD_REQUEST);
        Mockito.when(mockHttpConnectionSecond.getErrorStream()).thenReturn(IOUtils.toInputStream("400 Bad Request"));

        final AbstractClient.UrlWrapper mockURLSecond = mock(AbstractClient.UrlWrapper.class);
        Mockito.when(mockURLSecond.openConnection()).thenReturn(mockHttpConnectionSecond);

        AbstractClient.URLFactory urlFactory = Mockito.mock(AbstractClient.URLFactory.class);
        String[] fields = {"id", "person.login", "person.uid", "schedule.id", "schedule.name", "is_approved", "start", "end", "start_datetime", "end_datetime"};
        String fielsParams = "&fields=" + String.join(",", fields) + ",replaces." + String.join(",replaces.", fields);
        Mockito.when(urlFactory.createUrl("https://abc-back.yandex-team.ru/api/v4/duty/shifts/?date_from=2020-01-01&date_to=2020-10-01&person=orphie" + fielsParams)).thenReturn(mockURLFirst);
        Mockito.when(urlFactory.createUrl("https://abc-back.yandex-team.ru/api/v4/duty/shifts/?date_from=null&date_to=null&person=orphie" + fielsParams)).thenReturn(mockURLSecond);

        AbcClient abcClient = new AbcClient(urlFactory, "");

        assertEquals(
                new JsonNodeFactory(false).nullNode(),
                abcClient.getDutyShifts(fromDate, toDate, null)
        );

        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(expectedData);
        assertEquals(
                jsonNode,
                abcClient.getDutyShifts(fromDate, toDate, singletonList(login_correct))
        );

        Assert.assertThrows(RuntimeException.class,
                () -> abcClient.getDutyShifts(null, null, singletonList(login_correct)));

    }
}

// dummy changes to MARKETDX-830
