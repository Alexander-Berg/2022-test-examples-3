package ru.yandex.direct.telephony.client;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.util.HashedWheelTimer;
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
import ru.yandex.direct.tvm.TvmIntegration;
import ru.yandex.direct.tvm.TvmIntegrationImpl;
import ru.yandex.telephony.backend.lib.proto.telephony_platform.ServiceNumber;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.tvm.TvmService.DIRECT_SCRIPTS_TEST;
import static ru.yandex.direct.tvm.TvmService.TELEPHONY_TEST;

@Ignore("For manual run")
public class TelephonyClientManualTest {

    private static final String TESTING_URL = "https://platform-preprod.telephony.yandex.net";

    private static final Duration COMMON_TIMEOUT = Duration.ofSeconds(5);

    private TelephonyClient telephonyClient;

    @Before
    public void setUp() {
        DirectConfig directConfig = getDirectConfig();
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.initialize();
        TvmIntegration tvmIntegration = TvmIntegrationImpl.create(directConfig, scheduler);
        ParallelFetcherFactory parallelFetcherFactory =
                new ParallelFetcherFactory(getAsyncHttpClient(), new FetcherSettings());
        String testPlaybackId = "896f68c1-fc13-4209-8d6e-fafdde08e46f";

        telephonyClient = new TelephonyClient(TESTING_URL, tvmIntegration, TELEPHONY_TEST, parallelFetcherFactory,
                () -> testPlaybackId);
    }

    private DirectConfig getDirectConfig() {
        Map<String, Object> conf = new HashMap<>();
        conf.put("tvm.enabled", true);
        conf.put("tvm.app_id", DIRECT_SCRIPTS_TEST.getId());
        conf.put("tvm.api.url", "https://tvm-api.yandex.net");
        conf.put("tvm.api.error_delay", "5s");
        conf.put("tvm.secret", "file:////etc/direct-tokens/tvm2_direct-scripts-test");
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
    public void getServiceNumbers_manualTest() {
        ServiceNumber serviceNumber = telephonyClient.getServiceNumber("");
        assertThat(serviceNumber).isNotNull();

        //ba4659dc-30d7-479c-8084-89c49e8b9869 : +79120996314
        //5c8fc68d-6955-4241-87ed-db3549bbe829 : +79120996346
        System.out.println(serviceNumber.getServiceNumberID() + " : " + serviceNumber.getNum());
    }

    @Test
    public void getClientServiceNumbers_manualTest() {
        long clientId = 1;
        List<ServiceNumber> clientServiceNumbers = telephonyClient.getClientServiceNumbers(clientId);
        assertThat(clientServiceNumbers).isNotNull();

        System.out.println(
                clientServiceNumbers.get(0).getServiceNumberID() +
                        " : " + clientServiceNumbers.get(0).getNum());
    }

}
