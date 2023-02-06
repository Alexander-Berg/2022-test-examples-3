package ru.yandex.market.tsum.pipelines.mstat.jobs;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.tsum.pipe.engine.runtime.helpers.TestJobContext;
import ru.yandex.market.tsum.pipelines.mstat.resources.HandleCheckConfig;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.http.HttpVersion.HTTP_1_0;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CheckHandleUntilFinishedJobTest {

    @InjectMocks
    private CheckHandleUntilFinishedJob job = new CheckHandleUntilFinishedJob();

    @Mock
    private HandleCheckConfig config;

    @Mock
    private CloseableHttpClient httpClient;

    private final TestJobContext context = new TestJobContext(false);

    @Before
    public void init() {
        Mockito.reset(config, httpClient);
    }

    @Test
    public void handleReturnsOK() throws Exception {
        when(config.getTimeToFailSeconds()).thenReturn(13L);
        when(config.getUrl()).thenReturn("http://testurl.com/");
        when(httpClient.execute(any(HttpGet.class)))
            .thenReturn(new MockResponse(200, "{\"state\": \"OK\", \"metaInfo\": \"id=this is our test id\"}"));

        job.execute(context);

        assertSoftly(softly -> {
            softly.assertThat(context.getLastProgress()).isNotNull();
            softly.assertThat(context.getLastProgress().getStatusText())
                .isEqualTo("Джоба выполнилась успешно, мета: id=this is our test id");
        });
    }

    @Test
    public void handleReturnsFailed() throws Exception {
        when(config.getTimeToFailSeconds()).thenReturn(9L);
        when(config.getCheckIntervalSeconds()).thenReturn(5L);
        when(config.getUrl()).thenReturn("http://testurl.com/");
        when(httpClient.execute(any(HttpGet.class)))
            .thenReturn(new MockResponse(200,
                "{\"state\": \"FAILED\", \"metaInfo\": \"id=this is our test id\"}"));
        Throwable throwable = catchThrowable(() -> job.execute(context));

        assertSoftly(softly -> {
            softly.assertThat(throwable).hasMessage("Проверяемая джоба завершилась фейлом, мета: id=this is our test " +
                "id");
        });
    }

    @Test
    public void handleReturnsInProgress() throws Exception {
        when(config.getTimeToFailSeconds()).thenReturn(9L).thenReturn(9L);
        when(config.getCheckIntervalSeconds()).thenReturn(5L).thenReturn(9L);
        when(config.getUrl()).thenReturn("http://testurl.com/").thenReturn("http://testurl.com/");
        when(httpClient.execute(any(HttpGet.class)))
            .thenReturn(new MockResponse(200,
                "{\"state\": \"IN_PROGRESS\", \"metaInfo\": \"id=this is our test id\"}"))
            .thenReturn(new MockResponse(200, "{\"state\": \"OK\", \"metaInfo\": \"id=this is our test id\"}"));

        job.execute(context);

        assertSoftly(softly -> {
            softly.assertThat(context.getLastProgress()).isNotNull();
            softly.assertThat(context.getLastProgress().getStatusText())
                .isEqualTo("Джоба выполнилась успешно, мета: id=this is our test id");
        });
        verify(httpClient, times(2)).execute(any(HttpGet.class));
    }

    @Test
    public void handleReturnsExceptionAndThenOk() throws Exception {
        when(config.getTimeToFailSeconds()).thenReturn(9L).thenReturn(9L);
        when(config.getCheckIntervalSeconds()).thenReturn(5L).thenReturn(9L);
        when(config.getUrl()).thenReturn("http://testurl.com/").thenReturn("http://testurl.com/");
        when(httpClient.execute(any(HttpGet.class)))
            .thenThrow(new IOException("Test exception"))
            .thenReturn(new MockResponse(200, "{\"state\": \"OK\", \"metaInfo\": \"id=this is our test id\"}"));

        job.execute(context);

        assertSoftly(softly -> {
            softly.assertThat(context.getLastProgress()).isNotNull();
            softly.assertThat(context.getLastProgress().getStatusText())
                .isEqualTo("Джоба выполнилась успешно, мета: id=this is our test id");
        });
        verify(httpClient, times(2)).execute(any(HttpGet.class));
    }

    protected static class MockResponse extends BasicHttpResponse implements CloseableHttpResponse {

        public MockResponse(StatusLine statusline) {
            super(statusline);
        }

        public MockResponse(int code, String content) {
            this(new BasicStatusLine(HTTP_1_0, code, "reason"));
            BasicHttpEntity entity = new BasicHttpEntity();
            entity.setContent(new ByteArrayInputStream(content.getBytes(UTF_8)));
            setEntity(entity);
        }

        @Override
        public void close() {

        }
    }
}
