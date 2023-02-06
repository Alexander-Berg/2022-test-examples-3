package ru.yandex.chemodan.app.telemost.translator;

import java.io.IOException;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.chemodan.app.telemost.services.model.BroadcastAndConferenceUris;
import ru.yandex.chemodan.util.test.AbstractTest;
import ru.yandex.misc.ip.HostPort;
import ru.yandex.misc.test.Assert;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class TranslatorClientTest extends AbstractTest {

    private static final BroadcastAndConferenceUris URIS = new BroadcastAndConferenceUris(
            "https://mnogonas.ru/j/127000",
            "https://telemost.com/j/640023"
    );

    private static final String ACTION_PATH = "/v2/conferences/https%3A%2F%2Ftelemost.com%2Fj%2F640023" +
            "/broadcast/https%3A%2F%2Fmnogonas.ru%2Fj%2F127000";

    @Mock
    public HttpClient httpClientMock;
    @InjectMocks
    public TranslatorClientImpl translatorClient;
    @Captor
    public ArgumentCaptor<HttpUriRequest> uriRequestCaptor;

    @Test
    public void testStartRequest() throws IOException {
        translatorClient.start(
                new HostPort("translator.ru", 80),
                URIS, "5treamK", "TT"
        );
        verify(httpClientMock).execute(uriRequestCaptor.capture(), any(), any());

        Assert.isInstance(uriRequestCaptor.getValue(), HttpPut.class);
        Assert.isNull(((HttpPut) uriRequestCaptor.getValue()).getEntity());
        Assert.equals(
                "http://translator.ru" + ACTION_PATH + "/start?stream_key=5treamK&translator_token=TT",
                uriRequestCaptor.getValue().getURI().toASCIIString());
    }

    @Test
    public void testStopRequest() throws IOException {
        translatorClient.stop(
                new HostPort("somewhere.com", 443),
                URIS, "5treamK"
        );
        verify(httpClientMock).execute(uriRequestCaptor.capture(), any(), any());

        Assert.isInstance(uriRequestCaptor.getValue(), HttpPost.class);
        Assert.isNull(((HttpPost) uriRequestCaptor.getValue()).getEntity());
        Assert.equals(
                "https://somewhere.com" + ACTION_PATH + "/stop?stream_key=5treamK",
                uriRequestCaptor.getValue().getURI().toASCIIString());
    }
}
