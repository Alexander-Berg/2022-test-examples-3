package ru.yandex.chemodan.app.telemost.ugcLive;

import java.net.URI;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.chemodan.app.telemost.TelemostBaseContextTest;
import ru.yandex.chemodan.app.telemost.ugcLive.model.LineInfo;
import ru.yandex.chemodan.app.telemost.ugcLive.model.StreamAction;
import ru.yandex.chemodan.app.telemost.ugcLive.model.StreamState;
import ru.yandex.chemodan.util.http.HttpClientConfigurator;
import ru.yandex.devtools.test.annotations.YaExternal;
import ru.yandex.devtools.test.annotations.YaIgnore;
import ru.yandex.misc.io.http.HttpException;
import ru.yandex.misc.test.Assert;

@YaExternal
@YaIgnore
@Ignore
@ContextConfiguration(classes = UgcLiveClientTest.Context.class)
public class UgcLiveClientTest extends TelemostBaseContextTest {

    private static final String userTicket = "<ya tool tvmknife get_user_ticket>";

    @Autowired
    private UgcLiveClient ugcLiveClient;

    @Test
    public void testCreateOfflineStream() {
        long lineId = ugcLiveClient.createLine("Test line");
        try {
            LineInfo info = ugcLiveClient.getLineInfo(lineId);
            Assert.none(info.getStreamState());
            Assert.none(info.getEpisodeSlug());

            String slug = ugcLiveClient.createEpisode(lineId, "Test episode");
            try {
                LineInfo updated = ugcLiveClient.getLineInfo(lineId);
                Assert.some(StreamState.OFFLINE, updated.getStreamState());
                Assert.some(slug, updated.getEpisodeSlug());

                Assert.equals(info.getRtmpKey(), updated.getRtmpKey());
                Assert.some(StreamState.OFFLINE, ugcLiveClient.getStreamState(slug));

                Assert.assertThrows(
                        () -> ugcLiveClient.performStreamAction(slug, StreamAction.PUBLISH),
                        HttpException.class,
                        (e) -> e.getCause().getMessage().contains("actual offline")
                                && ((HttpResponseException) e.getCause()).getStatusCode() == HttpStatus.SC_CONFLICT
                );
            } finally {
                ugcLiveClient.deleteEpisode(slug);
            }
        } finally {
            ugcLiveClient.deleteLine(lineId);
        }
    }

    @Configuration
    public static class Context {
        @Bean
        @Primary
        public UgcLiveClient ugcLiveClient(
                @Value("${ugc-live.base-url}") URI baseUrl,
                @Value("${ugc-live.channel-id}") String channelId,
                HttpClientConfigurator ugcLiveHttpClientConfigurator
        ) {
            return new UgcLiveClientImpl(
                    ugcLiveHttpClientConfigurator.configure(),
                    baseUrl, channelId, () -> userTicket);
        }
    }
}
