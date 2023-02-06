package ru.yandex.direct.expert.client;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.SoftAssertions;
import org.asynchttpclient.AsyncHttpClient;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.asynchttp.FetcherSettings;
import ru.yandex.direct.asynchttp.ParallelFetcherFactory;
import ru.yandex.direct.config.DirectConfig;
import ru.yandex.direct.config.DirectConfigFactory;
import ru.yandex.direct.env.EnvironmentType;
import ru.yandex.direct.expert.client.model.Certificate;
import ru.yandex.direct.tvm.TvmIntegrationImpl;

import static ru.yandex.direct.config.EssentialConfiguration.CONFIG_SCHEDULER_BEAN_NAME;
import static ru.yandex.direct.tvm.TvmService.DIRECT_SCRIPTS_TEST;
import static ru.yandex.direct.tvm.TvmService.EXPERT_API_TESTING;

@Ignore("Ходит в тестовую сертификатницу")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestingConfiguration.class})
public class ExpertClientManualTest {

    private static final long UID1 = 722092392L;
    private static final long UID2 = 324597933L;
    private static final long FAKE_UID = (long) Math.pow(2, 30);

    @Autowired
    public AsyncHttpClient asyncHttpClient;
    @Autowired()
    @Qualifier(CONFIG_SCHEDULER_BEAN_NAME)
    public TaskScheduler liveConfigChangeTaskScheduler;

    private ExpertClient expertClient;

    @Before
    public void setUp() throws Exception {
        DirectConfig directConfig = getDirectConfig();
        TvmIntegrationImpl tvmIntegration = TvmIntegrationImpl.create(directConfig, liveConfigChangeTaskScheduler);
        ParallelFetcherFactory fetcherFactory = new ParallelFetcherFactory(asyncHttpClient, new FetcherSettings());
        expertClient = new ExpertClient("http://expert-api-testing.commerce-int.yandex.ru/v1", EXPERT_API_TESTING,
                fetcherFactory, tvmIntegration);
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

    @Test
    public void getCertificates_sucessForExistUid() {
        List<Long> uids = Arrays.asList(UID1, UID2, FAKE_UID);
        Map<Long, List<Certificate>> certByUid = expertClient.getCertificates(uids);
        Long[] uidsAsArray = uids.toArray(new Long[0]);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(certByUid.keySet()).containsExactlyInAnyOrder(uidsAsArray);
            soft.assertThat(certByUid.get(UID1)).isNotEmpty();
            soft.assertThat(certByUid.get(UID2)).isNotEmpty();
            soft.assertThat(certByUid.get(FAKE_UID)).isEmpty();
        });
    }
}
