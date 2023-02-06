package ru.yandex.direct.binlogbroker.replicatetoyt;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.base.Preconditions;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import ru.yandex.direct.config.DirectConfigFactory;
import ru.yandex.direct.test.utils.TestUtils;
import ru.yandex.direct.utils.Transient;
import ru.yandex.direct.ytwrapper.client.YtClusterConfig;
import ru.yandex.direct.ytwrapper.client.YtClusterTypesafeConfigProvider;
import ru.yandex.direct.ytwrapper.model.YtCluster;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.impl.YtUtils;
import ru.yandex.yt.ytclient.bus.BusConnector;
import ru.yandex.yt.ytclient.bus.DefaultBusConnector;
import ru.yandex.yt.ytclient.proxy.YtClient;
import ru.yandex.yt.ytclient.proxy.request.CreateNode;
import ru.yandex.yt.ytclient.proxy.request.ObjectType;
import ru.yandex.yt.ytclient.rpc.RpcCredentials;
import ru.yandex.yt.ytclient.rpc.RpcOptions;

/**
 * Устанавливает соединение с настоящим YT и создаёт в нём временный каталог для тестов.
 */
@ParametersAreNonnullByDefault
public class JunitRealYt extends ExternalResource {
    private final String randomString;
    private final YtClusterConfig ytConfig;
    private final YtCluster ytCluster;
    private String testClassName;
    private BusConnector busConnector;
    private YtClient ytClient;
    private Yt yt;
    private boolean initialized;

    public JunitRealYt(YtCluster ytCluster) {
        this.ytCluster = ytCluster;
        this.randomString = TestUtils.randomName("", 8);
        this.ytConfig = new YtClusterTypesafeConfigProvider(DirectConfigFactory.getConfig().getBranch("yt").getConfig())
                .get(ytCluster);
    }

    private String getBasePathInternal() {
        return String.format("//home/direct/tmp/%s/%s-%s",
                ytConfig.getUser(),
                testClassName,
                randomString);
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                JunitRealYt.this.testClassName = description.getClassName();
                JunitRealYt.super.apply(base, description).evaluate();
            }
        };
    }

    @Override
    protected void before() throws Throwable {
        deinitializeStuff();
    }

    @Override
    protected void after() {
        deinitializeStuff();
    }

    /**
     * @return Путь ко временному каталогу для тестов
     */
    public String getBasePath() {
        initializeStuff();
        return getBasePathInternal();
    }

    /**
     * @return HTTP клиент для работы с YT
     */
    public Yt getYt() {
        initializeStuff();
        return yt;
    }

    /**
     * @return RPC клиент для работы с YT
     */
    public YtClient getYtClient() {
        initializeStuff();
        return ytClient;
    }

    private void initializeStuff() {
        if (initialized) {
            return;
        }
        Preconditions.checkState(busConnector == null);
        Preconditions.checkState(ytClient == null);
        Preconditions.checkState(yt == null);
        try (Transient<BusConnector> tBusConnector = new Transient<>(new DefaultBusConnector());
             Transient<YtClient> tYtClient = new Transient<>(new YtClient(tBusConnector.item,
                     new ru.yandex.yt.ytclient.proxy.YtCluster(
                             ytCluster.getName(), ytConfig.getProxyHost(), ytConfig.getProxyPort()),
                     new RpcCredentials(ytConfig.getUser(), ytConfig.getToken()),
                     new RpcOptions()))) {
            tYtClient.item.waitProxies().join();

            String basePath = getBasePathInternal();
            System.out.println("Test configuration:\n"
                    + "\tYT cluster: " + ytCluster.getName() + "\n"
                    + "\tUser: " + ytConfig.getUser() + "\n"
                    + "\tProxy: " + ytConfig.getProxy() + "\n"
                    + "\tBase node: " + basePath);
            tYtClient.item.createNode(new CreateNode(basePath, ObjectType.MapNode).setRecursive(true)).join();
            busConnector = tBusConnector.pop();
            ytClient = tYtClient.pop();
            yt = YtUtils.http(ytConfig.getProxy(), ytConfig.getToken());
            initialized = true;
        }
    }

    private void deinitializeStuff() {
        if (!initialized) {
            return;
        }
        Preconditions.checkState(busConnector != null);
        Preconditions.checkState(ytClient != null);
        Preconditions.checkState(yt != null);
        try (BusConnector ignored1 = busConnector;
             YtClient ignored2 = ytClient) {
            ytClient.removeNode(getBasePathInternal()).join();
        } finally {
            busConnector = null;
            ytClient = null;
            yt = null;
            initialized = false;
        }
    }
}
