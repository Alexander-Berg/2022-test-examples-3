package ru.yandex.direct.turboapps.client;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.util.HashedWheelTimer;
import one.util.streamex.EntryStream;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import ru.yandex.direct.asynchttp.FetcherSettings;
import ru.yandex.direct.asynchttp.ParallelFetcherFactory;
import ru.yandex.direct.config.DirectConfig;
import ru.yandex.direct.config.DirectConfigFactory;
import ru.yandex.direct.env.EnvironmentType;
import ru.yandex.direct.turboapps.client.model.TurboAppInfoRequest;
import ru.yandex.direct.turboapps.client.model.TurboAppInfoResponse;
import ru.yandex.direct.turboapps.client.model.TurboAppMetaContentResponse;
import ru.yandex.direct.tvm.TvmIntegration;
import ru.yandex.direct.tvm.TvmIntegrationImpl;
import ru.yandex.direct.utils.JsonUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.tvm.TvmService.DIRECT_SCRIPTS_TEST;
import static ru.yandex.direct.tvm.TvmService.TURBO_APP_TEST;

@Ignore("For manual run")
public class TurboAppsClientManualTest {

    private static final String URL_WITH_TURBO_APP = "https://yandex.ru/turbo?text=https%3A//15-sotok.ru" +
            "/katalog-tekhniki/prokladka-golovki-bloka-150259700-mk-krot&utm_source=turbo_turbo";

    private static final String URL_WITHOUT_TURBO_APP = "some_strange_url";

    private static final String TESTING_URL = "http://superapp-http-direct.advmachine.yandex.net:80";

    private static final Duration COMMON_TIMEOUT = Duration.ofSeconds(5);

    private TurboAppsClient turboappsClient;

    @Before
    public void setUp() {
        DirectConfig directConfig = getDirectConfig();
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.initialize();
        TvmIntegration tvmIntegration = TvmIntegrationImpl.create(directConfig, scheduler);
        ParallelFetcherFactory parallelFetcherFactory =
                new ParallelFetcherFactory(getAsyncHttpClient(), new FetcherSettings()
                        .withRequestRetries(2)
                        .withRequestTimeout(Duration.ofSeconds(2))
                        .withParallel(16));

        turboappsClient = new TurboAppsClient(TESTING_URL, 5000, tvmIntegration, TURBO_APP_TEST,
                parallelFetcherFactory);
    }

    private DirectConfig getDirectConfig() {
        Map<String, Object> conf = new HashMap<>();
        conf.put("tvm.enabled", true);
        conf.put("tvm.app_id", DIRECT_SCRIPTS_TEST.getId());
        conf.put("tvm.api.url", "https://tvm-api.yandex.net");
        conf.put("tvm.api.error_delay", "5s");
        conf.put("tvm.secret", "file://~/.direct-tokens/tvm2_direct-scripts-test");
        return DirectConfigFactory.getConfig(EnvironmentType.DB_TESTING, conf);
    }

    private AsyncHttpClient getAsyncHttpClient() {
        DefaultAsyncHttpClientConfig.Builder builder = new DefaultAsyncHttpClientConfig.Builder();
        builder.setRequestTimeout((int) COMMON_TIMEOUT.toMillis());
        builder.setReadTimeout((int) COMMON_TIMEOUT.toMillis());
        builder.setConnectTimeout((int) COMMON_TIMEOUT.toMillis());
        builder.setConnectionTtl((int) COMMON_TIMEOUT.toMillis());
        builder.setPooledConnectionIdleTimeout((int) COMMON_TIMEOUT.toMillis());
        builder.setIoThreadsCount(2);
        builder.setNettyTimer(new HashedWheelTimer(
                new ThreadFactoryBuilder().setNameFormat("ahc-timer-%02d").setDaemon(true).build()));
        return new DefaultAsyncHttpClient(builder.build());
    }

    @Test
    public void getTurboAppsInfo_manualTest() throws TurboAppsClientException {
        long bannerId1 = 228;
        long bannerId2 = 1337;

        Map<Long, String> requests = Map.of(
                bannerId1, URL_WITH_TURBO_APP,
                bannerId2, URL_WITHOUT_TURBO_APP
        );

        var turboApps = turboappsClient.getTurboApps(EntryStream.of(requests)
                .mapKeyValue(TurboAppInfoRequest::new).toList());

        TurboAppInfoResponse expected = new TurboAppInfoResponse()
                .withBannerId(bannerId1)
                .withBannerUrl(URL_WITH_TURBO_APP)
                .withAppId(99327L)
                .withContent("{\"TurboAppUrlType\":\"AsIs\"}")
                .withMetaContent(JsonUtils.toJson(new TurboAppMetaContentResponse()
                        .withName("Яндекс.Метро")
                        .withDescription("Схемы метро городов мира, информация о станциях и маршруты в объезд перекрытий")
                        .withIconUrl("https://avatars.mds.yandex.net/get-games/1881371/2a00000170a155455bf5979079c9a07746e6/cover1")));

        System.err.println(JsonUtils.toJson(turboApps));

        assertThat(turboApps.keySet()).containsExactly(bannerId1);
        assertThat(turboApps.get(bannerId1)).is(matchedBy(beanDiffer(expected)));
    }
}
