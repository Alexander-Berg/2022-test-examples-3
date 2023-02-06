package ru.yandex.market.mstat.planner.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import junit.framework.TestCase;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.mockito.Mockito;

import java.io.OutputStream;
import java.net.HttpURLConnection;

import static org.mockito.Mockito.mock;

public class TrackerClientTest extends TestCase {
    AbstractClient.URLFactory urlFactory = Mockito.mock(AbstractClient.URLFactory.class);
    TrackerClient client = new TrackerClient(urlFactory, "");

    @SneakyThrows
    public void testCreateIssue() {
        String testData = "{\"self\" : \"https://st-api.test.yandex-team.ru/v2/issues/STARTREK-6090\", \"id\" : \"539ed8d6e4b0c91505c586d8\", \"key\" : \"STARTREK-6090\", \"version\" : 1403078509771, \"summary\" : \"New issue\", \"queue\" : {\"self\" : \"https://st-api.test.yandex-team.ru/v2/queues/STARTREK\", \"id\" : \"683\", \"key\" : \"STARTREK\", \"display\" : \"Стартрек\"}}";
        HttpURLConnection mockHttpConnection = Mockito.mock(HttpURLConnection.class);
        Mockito.when(mockHttpConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_CREATED);
        OutputStream outputStream = Mockito.mock(OutputStream.class);
        Mockito.when(mockHttpConnection.getOutputStream()).thenReturn(outputStream);
        Mockito.when(mockHttpConnection.getInputStream()).thenReturn(IOUtils.toInputStream(testData));
        final AbstractClient.UrlWrapper mockURLFirst = mock(AbstractClient.UrlWrapper.class);
        Mockito.when(mockURLFirst.openConnection()).thenReturn(mockHttpConnection);

        Mockito.when(urlFactory.createUrl("https://st-api.yandex-team.ru/v2/issues")).thenReturn(mockURLFirst);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(testData);
        String data = "{\"queue\": \"TEST\", \"summary\": \"Test Issue\", \"parent\":\"TEST-1234\", \"links\":[{\"relationship\": \"relates\", \"issue\": \"ABC-123\"},{\"relationship\": \"relates\", \"origin\": \"ru.yandex.jira\", \"key\": \"SSB-123\"}, {\"relationship\": \"depends on\", \"issue\": \"TEST-42\"}],\"attachmentIds\": [ 1, 2, 3 ]}";
        assertEquals(jsonNode, client.createIssue(mapper.readTree(data), ""));

        Mockito.when(mockHttpConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_NOT_FOUND);
        Assert.assertThrows(RuntimeException.class, () -> client.createIssue(mapper.readTree(data), ""));
    }

    @SneakyThrows
    public void testUpdateIssue() {
        String testData = "{\"self\" : \"https://st-api.test.yandex-team.ru/v2/issues/STARTREK-6090\", \"id\" : \"539ed8d6e4b0c91505c586d8\", \"key\" : \"STARTREK-6090\", \"version\" : 1403078509771, \"summary\" : \"New issue\", \"queue\" : {\"self\" : \"https://st-api.test.yandex-team.ru/v2/queues/STARTREK\", \"id\" : \"683\", \"key\" : \"STARTREK\", \"display\" : \"Стартрек\"}}";
        HttpURLConnection mockHttpConnection = Mockito.mock(HttpURLConnection.class);
        Mockito.when(mockHttpConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_CREATED);
        OutputStream outputStream = Mockito.mock(OutputStream.class);
        Mockito.when(mockHttpConnection.getOutputStream()).thenReturn(outputStream);
        Mockito.when(mockHttpConnection.getInputStream()).thenReturn(IOUtils.toInputStream(testData));
        final AbstractClient.UrlWrapper mockURLFirst = mock(AbstractClient.UrlWrapper.class);
        Mockito.when(mockURLFirst.openConnection()).thenReturn(mockHttpConnection);

        Mockito.when(urlFactory.createUrl("https://st-api.yandex-team.ru/v2/issues/STARTREK-6090")).thenReturn(mockURLFirst);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(testData);
        String data = "{\"queue\": \"TEST\", \"summary\": \"Test Issue\", \"parent\":\"TEST-1234\", \"links\":[{\"relationship\": \"relates\", \"issue\": \"ABC-123\"},{\"relationship\": \"relates\", \"origin\": \"ru.yandex.jira\", \"key\": \"SSB-123\"}, {\"relationship\": \"depends on\", \"issue\": \"TEST-42\"}],\"attachmentIds\": [ 1, 2, 3 ]}";
        assertEquals(jsonNode, client.updateIssue("STARTREK-6090", mapper.readTree(data), ""));
    }

    @SneakyThrows
    public void testGetIssues() {
        String testData = "[ {\"self\" : \"https://st-api.yandex-team.ru/v2/issues/STARTREK-4126\", \"id\" : \"539ee230e4b08d60c73ee99d\", \"key\" : \"STARTREK-4126\", \"version\" : 1402927540266, \"summary\" : \"Сломался стартрек\", \"description\" : \"Закончились лимиты из-за проксирования поиска в джиру\", \"queue\" : {\"self\" : \"https://st-api.yandex-team.ru/v2/queues/STARTREK\", \"id\" : \"111\", \"key\" : \"STARTREK\", \"display\" : \"Стартрек\"}, \"status\" : {\"self\" : \"https://st-api.yandex-team.ru/v2/statuses/3\", \"id\" : \"3\", \"key\" : \"closed\", \"display\" : \"Closed\"}, \"resolution\" : {\"self\" : \"https://st-api.yandex-team.ru/v2/resolutions/1\", \"id\" : \"1\", \"key\" : \"fixed\", \"display\" : \"Fixed\"}}]";

        HttpURLConnection mockHttpConnection = Mockito.mock(HttpURLConnection.class);
        Mockito.when(mockHttpConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_CREATED);
        OutputStream outputStream = Mockito.mock(OutputStream.class);
        Mockito.when(mockHttpConnection.getOutputStream()).thenReturn(outputStream);
        Mockito.when(mockHttpConnection.getInputStream()).thenReturn(IOUtils.toInputStream(testData));
        final AbstractClient.UrlWrapper mockURLFirst = mock(AbstractClient.UrlWrapper.class);
        Mockito.when(mockURLFirst.openConnection()).thenReturn(mockHttpConnection);

        Mockito.when(urlFactory.createUrl("https://st-api.yandex-team.ru/v2/issues/_search?perPage=1000")).thenReturn(mockURLFirst);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(testData);
        assertEquals(jsonNode, client.getIssues("2016-05-23T00:00:00", "2016-05-24T00:00:00", "orphie", "MARKETUNDEFINED"));
    }
}
