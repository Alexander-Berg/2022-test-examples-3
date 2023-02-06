package ru.yandex.market.olap2.load;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.UUID;

import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.olap2.dao.ClickhouseService;
import ru.yandex.market.olap2.dao.MetadataDao;
import ru.yandex.market.olap2.graphite.Graphite;
import ru.yandex.market.olap2.leader.Shutdowner;
import ru.yandex.market.olap2.load.exceptions.TMFailedStatusException;
import ru.yandex.market.olap2.load.tasks.ClickhouseLoadTask;
import ru.yandex.market.olap2.model.ChUnionsHolder;
import ru.yandex.market.olap2.model.YtCluster;
import ru.yandex.market.olap2.step.StepSender;
import ru.yandex.market.olap2.yt.YtTableService;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.http.HttpVersion.HTTP_1_0;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.olap2.load.tasks.TaskPriority.DEFAULT;

public class TMRequestSenderTest {

    private TMRequestsSender sender;
    private ClickhouseLoadTask task;

    private ClickhouseService chService = mock(ClickhouseService.class);
    private YtTableService ytTableService = mock(YtTableService.class);
    private ChCacheRefresher chCacheRefresher = mock(ChCacheRefresher.class);
    private CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
    private MetadataDao metadataDao = mock(MetadataDao.class);
    private String taskId = UUID.randomUUID().toString();
    private ChUnionsHolder chUnionsHolder = mock(ChUnionsHolder.class);

    @Before
    public void prepare() {
        Mockito.reset(
                chService,
                ytTableService,
                chCacheRefresher,
                httpClient,
                metadataDao
        );
        sender = new TMRequestsSender(
                ytTableService,
                metadataDao,
                mock(Shutdowner.class),
                httpClient
        );
        task = clickhouseLoadTask(taskId, 2);
    }

    /**
     * Проверим, что при запуске loadTable() и правильной работе мы единожды попытаемся отправить сообщение.
     * todo - spy и проверку на параметры httpClient.
     */
    @Test
    public void testHappyPathLoadFirstTry() throws Exception {
        sender.tmUrl = "testurl.com";
        sender.waitTimeMillis = 0;
        sender.maxRetries = 2;

        CloseableHttpResponse postTaskResponse = buildResponse(200, taskId);
        when(httpClient.execute(any(HttpPost.class))).thenReturn(postTaskResponse);
        CloseableHttpResponse getStatusResponse = buildResponse(200,
                "{\"state\": \"completed\"}"
        );

        when(httpClient.execute(any(HttpGet.class))).thenReturn(getStatusResponse);
        sender.loadTable(task);
        verify(httpClient, times(1)).execute(any(HttpGet.class)); // запросим статус единожды
    }

    /**
     * Проверим, что, если нам ТМ в статусе таски указывает проблему с декодированием типа,
     * то мы не будем ретраить получение статуса.
     *
     * @throws Exception - ожидаем специального исключения по статусу (TMFailedStatusException)
     */
    @Test(expected = TMFailedStatusException.class)
    public void testStopRetryingIfChTypeNotDecodable() throws Exception {
        sender.tmUrl = "testurl.com";
        sender.waitTimeMillis = 0;
        sender.maxRetries = 2;

        CloseableHttpResponse postTaskResponse = buildResponse(200, taskId);
        when(httpClient.execute(any(HttpPost.class))).thenReturn(postTaskResponse);
        CloseableHttpResponse getStatusResponse = buildResponse(200,
                "{\"state\": \"failed\"," +
                        "\"error\": {"
                        + "\"message\":\"cannot decode value of ClickHouse type Nullable(Decimal(15,8)) from Yt value\""
                        + "}"
                        + "}"
        );

        when(httpClient.execute(any(HttpGet.class))).thenReturn(getStatusResponse);
        try {
            sender.loadTable(task);
        } catch (Exception e) {
            verify(httpClient, times(1)).execute(any(HttpGet.class)); // запросим статус единожды
            throw e;
        }
    }


    private ClickhouseLoadTask clickhouseLoadTask(String id, int retryCount) {
        return ClickhouseLoadTask.builder()
                .stepEventId(id)
                .path("//tmp/tests/path/")
                .partition(null)
                .metadataDaoImpl(metadataDao)
                .graphite(mock(Graphite.class))
                .service(chService)
                .sender(sender)
                .ytTableService(ytTableService)
                .chCacheRefresher(chCacheRefresher)
                .retryCount(retryCount)
                .priority(DEFAULT.name())
                .stepSender(mock(StepSender.class))
                .ytCluster(new YtCluster("hahn"))
                .chUnionsHolder(chUnionsHolder)
                .build();
    }

    private static MockResponse buildResponse(int statusCode) {
        return new MockResponse(
                new BasicStatusLine(
                        HTTP_1_0,
                        statusCode,
                        "reason"
                )
        );
    }

    private static MockResponse buildResponse(int statusCode, String content) {
        MockResponse response = buildResponse(statusCode);
        BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(new ByteArrayInputStream(content.getBytes(UTF_8)));
        response.setEntity(entity);
        return response;
    }

    private static class MockResponse extends BasicHttpResponse implements CloseableHttpResponse {

        public MockResponse(StatusLine statusline) {
            super(statusline);
        }

        @Override
        public void close() throws IOException {

        }
    }

}
