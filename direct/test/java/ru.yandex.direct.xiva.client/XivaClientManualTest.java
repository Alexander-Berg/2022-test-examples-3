package ru.yandex.direct.xiva.client;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
import ru.yandex.direct.dbschema.ppc.enums.XivaPushesQueuePushType;
import ru.yandex.direct.env.EnvironmentType;
import ru.yandex.direct.tvm.TvmIntegrationImpl;
import ru.yandex.direct.xiva.client.model.Push;
import ru.yandex.direct.xiva.client.model.Recipient;
import ru.yandex.direct.xiva.client.model.RecipientUser;
import ru.yandex.direct.xiva.client.model.Signature;
import ru.yandex.direct.xiva.client.model.Subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.tvm.TvmService.DIRECT_SCRIPTS_TEST;
import static ru.yandex.direct.tvm.TvmService.XIVA_API_TEST;

@Ignore("For manual run")
public class XivaClientManualTest {
    private static final Duration CONNECTION_TIMEOUT = Duration.ofSeconds(10);
    private static final String TEST_USER = "direct-test-user";
    private static final String TEST_CLIENT = "xiva-client-test";
    private static final String TEST_URL = "https://yandex.ru";
    private static final String SERVICE = "direct";
    private static final String XIVA_SERVER_HOST = "https://push-sandbox.yandex.ru";

    private XivaClient xivaClient;
    private XivaConfig xivaConfig;

    @Before
    public void setup() throws IOException {
        DirectConfig directConfig = getDirectConfig();
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.initialize();
        TvmIntegrationImpl tvmIntegration = TvmIntegrationImpl.create(directConfig, scheduler);

        xivaConfig = new XivaConfig(XIVA_SERVER_HOST, SERVICE);

        DefaultAsyncHttpClientConfig.Builder httpClientConfigBuilder = new DefaultAsyncHttpClientConfig.Builder();
        httpClientConfigBuilder.setConnectTimeout((int) CONNECTION_TIMEOUT.toMillis());
        DefaultAsyncHttpClient httpClient = new DefaultAsyncHttpClient(httpClientConfigBuilder.build());
        ParallelFetcherFactory fetcherFactory = new ParallelFetcherFactory(httpClient, new FetcherSettings());

        xivaClient = new XivaClient(xivaConfig, fetcherFactory, tvmIntegration, XIVA_API_TEST);
    }

    private DirectConfig getDirectConfig() {
        Map<String, Object> conf = new HashMap<>();
        conf.put("tvm.enabled", true);
        conf.put("tvm.app_id", DIRECT_SCRIPTS_TEST.getId());
        conf.put("tvm.secret", "file://~/.direct-tokens/tvm2_direct-scripts-test");
        return DirectConfigFactory.getConfig(EnvironmentType.DEVELOPMENT, conf);
    }

    @Test
    public void getSignature() {
        Signature signature = xivaClient.getSignature(TEST_USER);
        assertThat(signature).isNotNull();
    }

    @Test
    public void subscribe() {
        // subscribe
        String subscriptionId = xivaClient.subscribeServer(
                TEST_USER, TEST_CLIENT, "session-001", TEST_URL, null);
        assertThat(subscriptionId).isNotNull();

        // check subscriptions containing subscriptionId
        List<Subscription> subscriptions = xivaClient.subscriptions(TEST_USER);
        assertThat(subscriptions.size()).isGreaterThan(0);
        Boolean containsId = false;
        for (Subscription s : subscriptions) {
            containsId = containsId || (s.getId().equals(subscriptionId));
        }
        assertThat(containsId).isEqualTo(true);

        // unsubscribe
        xivaClient.unsubscribeServer(TEST_USER, subscriptionId);
    }

    @Test
    public void batchSend() {
        String subscriptionId = xivaClient.subscribeServer(
                TEST_USER, TEST_CLIENT, "session-001", TEST_URL, null);

        XivaPushesQueuePushType push = XivaPushesQueuePushType.FAKE_PUSH;
        RecipientUser recipientUser = new RecipientUser(TEST_USER);
        LinkedList<Recipient> recipients = new LinkedList<>();
        recipients.add(recipientUser);
        xivaClient.sendBatch(recipients, new Push(push.toString(), push.toString()), null);
    }

    @Test
    public void send() {
        xivaClient.subscribeServer(TEST_USER, TEST_CLIENT, "session-002", TEST_URL, null);
        xivaClient.send(TEST_USER, new Push("message", "test-message"), 1);
    }
}
