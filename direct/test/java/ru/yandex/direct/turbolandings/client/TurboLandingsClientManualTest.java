package ru.yandex.direct.turbolandings.client;

import java.time.Duration;
import java.util.List;

import com.google.common.primitives.Ints;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.util.HashedWheelTimer;
import org.assertj.core.api.Assertions;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.direct.asynchttp.FetcherSettings;
import ru.yandex.direct.asynchttp.ParallelFetcherFactory;
import ru.yandex.direct.turbolandings.client.model.DcTurboLanding;
import ru.yandex.direct.turbolandings.client.model.GetIdByUrlResponseItem;

import static java.util.Arrays.asList;


public class TurboLandingsClientManualTest {

    TurboLandingsClient turboLandingsClient;

    @Before
    public void setUp() throws Exception {

//        https://ad-constructor-integration.common.yandex.ru
        String url = "https://ad-constructor-test.common.yandex.ru";
        String token = "s3cr3t";
        String fileWithToken = "/etc/direct-tokens/banner_storage_canvas_auth.txt";
        TurboLandingsClientConfiguration turboLandingsClientConfiguration = new TurboLandingsClientConfiguration(url,
                token, fileWithToken);


        //AsyncHttpClient
        DefaultAsyncHttpClientConfig.Builder builder = new DefaultAsyncHttpClientConfig.Builder();
        builder.setRequestTimeout(Ints.saturatedCast(Duration.ofSeconds(30).toMillis()));
        builder.setReadTimeout(Ints.saturatedCast(Duration.ofSeconds(30).toMillis()));
        builder.setConnectTimeout(Ints.saturatedCast(Duration.ofSeconds(10).toMillis()));
        builder.setConnectionTtl(Ints.saturatedCast(Duration.ofMinutes(1).toMillis()));
        builder.setPooledConnectionIdleTimeout(
                Ints.saturatedCast(Duration.ofSeconds(20).toMillis()));
        builder.setIoThreadsCount(2);
        builder.setNettyTimer(new HashedWheelTimer(
                new ThreadFactoryBuilder().setNameFormat("ahc-timer-%02d").setDaemon(true).build()));
        DefaultAsyncHttpClient asyncHttpClient = new DefaultAsyncHttpClient(builder.build());

        //ParallelFetcherFactory
        FetcherSettings fetcherSettings = new FetcherSettings();
        ParallelFetcherFactory parallelFetcherFactory = new ParallelFetcherFactory(asyncHttpClient, fetcherSettings);

        turboLandingsClient = new TurboLandingsClient(turboLandingsClientConfiguration, parallelFetcherFactory);
    }

    @Test
    @Ignore
    public void getTurboLandings() {
        List<DcTurboLanding> turboLandings = turboLandingsClient.getTurboLandings(123L, List.of(123L));
        System.out.println("count = " + turboLandings.size());
        turboLandings.forEach(
                l -> System.out.printf("id=%s; name=%s, url=%s\n", l.getId(), l.getName(), l.getUrl())
        );
        Assertions.assertThat(turboLandings).isNotNull();
    }

    @Test
    @Ignore
    public void getTurboLandingsIdsByUrl() {
        List<GetIdByUrlResponseItem> tlInfo = turboLandingsClient.getTurbolandingIdsByUrl(asList(
                "https://project183126.turbo-site-test.common.yandex.net/pageNonExisting",
                "https://project183126.turbo-site-test.common.yandex.net/page183127?double",
                "https://project183126.turbo-site-test.common.yandex.net/page183127?double",
                "https://project183126.turbo-site-test.common.yandex.net/page183128")
        );
        System.out.println("count = " + tlInfo.size());
        tlInfo.forEach(
                l -> System.out.printf("id=%s; name=%s, url=%s\n", l.getClientId(), l.getLandingId(), l.getUrl())
        );
        Assertions.assertThat(tlInfo).isNotNull();
    }
}
