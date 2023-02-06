package ru.yandex.direct.staff.client;


import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import com.google.common.primitives.Ints;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.util.HashedWheelTimer;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import ru.yandex.direct.asynchttp.FetcherSettings;
import ru.yandex.direct.asynchttp.ParallelFetcherFactory;
import ru.yandex.direct.config.DirectConfig;
import ru.yandex.direct.config.DirectConfigFactory;
import ru.yandex.direct.env.EnvironmentType;
import ru.yandex.direct.staff.client.model.StaffConfiguration;
import ru.yandex.direct.staff.client.model.json.PersonInfo;
import ru.yandex.direct.tvm.TvmIntegration;
import ru.yandex.direct.tvm.TvmIntegrationImpl;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.tvm.TvmService.DIRECT_SCRIPTS_TEST;
import static ru.yandex.direct.tvm.TvmService.STAFF_TEST;

@Ignore("Ходит в тестовый контур стафа")
public class StaffClientManualTest {

    private static final String[] LOGINS = new String[]{"ajkon", "maxlog", "kuhtich"};

    private StaffClient client;

    @Before
    public void setUp() {
        //TVM
        Map<String, Object> conf = new HashMap<>();
        conf.put("tvm.enabled", true);
        conf.put("tvm.app_id", DIRECT_SCRIPTS_TEST.getId());
        conf.put("tvm.api.url", "https://tvm-api.yandex.net");
        conf.put("tvm.api.error_delay", "5s");
        conf.put("tvm.secret", "file:////etc/direct-tokens/tvm2_direct-scripts-test");
        DirectConfig directConfig = DirectConfigFactory.getConfig(EnvironmentType.TESTING, conf);
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.initialize();
        TvmIntegration tvmIntegration = TvmIntegrationImpl.create(directConfig, scheduler);
        assumeThat(tvmIntegration.getTicket(STAFF_TEST), CoreMatchers.notNullValue());

        //Configuration
        StaffConfiguration staffConfiguration = new StaffConfiguration("https://staff-api.test.yandex-team.ru",
                "https://staff.test.yandex-team.ru", null);

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

        client = new StaffClient(staffConfiguration, parallelFetcherFactory,
                tvmIntegration, false);
    }

    @Test
    public void getStaffUserInfos() {
        Map<String, PersonInfo> staffUserInfos = client.getStaffUserInfos(asList(LOGINS));
        assertThat(staffUserInfos.keySet()).containsExactlyInAnyOrder(LOGINS);
    }
}
