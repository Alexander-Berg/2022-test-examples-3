package ru.yandex.market.crm.platform.test.yt;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.netty.channel.nio.NioEventLoopGroup;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.crm.platform.config.ClusterGroup;
import ru.yandex.market.crm.platform.yt.KvStorageClient;
import ru.yandex.market.crm.platform.yt.KvStorageClientImpl;
import ru.yandex.market.crm.platform.yt.SingleBackendSelectorImpl;
import ru.yandex.market.crm.platform.yt.YtClusters;
import ru.yandex.market.crm.platform.yt.YtFolders;
import ru.yandex.market.crm.platform.yt.YtTables;
import ru.yandex.market.crm.util.Futures;
import ru.yandex.misc.thread.ThreadUtils;
import ru.yandex.yt.ytclient.bus.BusConnector;
import ru.yandex.yt.ytclient.bus.DefaultBusConnector;
import ru.yandex.yt.ytclient.proxy.YtClient;
import ru.yandex.yt.ytclient.proxy.YtCluster;
import ru.yandex.yt.ytclient.proxy.request.ColumnFilter;
import ru.yandex.yt.ytclient.proxy.request.ListNode;
import ru.yandex.yt.ytclient.rpc.RpcCredentials;
import ru.yandex.yt.ytclient.rpc.RpcOptions;

/**
 * @author apershukov
 */
@Configuration
@ComponentScan("ru.yandex.market.crm.platform.test.yt")
public class CoreTestYtConfig {

    public static String getProxy() {
        String proxy = System.getenv("YT_PROXY");
        if (proxy == null) {
            throw new IllegalStateException("'YT_PROXY' environment variable is not set");
        }

        return proxy;
    }

    public static final String YT_HOME = "//home/platform";
    private static final String HEALTH = "health";

    @Bean(destroyMethod = "close")
    public YtClient ytClient(BusConnector busConnector) {
        String[] parts = getProxy().split(":");
        String fqdn = parts[0];
        int port = Integer.parseInt(parts[1]);

        YtCluster cluster = new YtCluster("unknown", fqdn, port);

        ru.yandex.yt.ytclient.proxy.YtClient ytRpcClient = new ru.yandex.yt.ytclient.proxy.YtClient(
                busConnector,
                cluster,
                new RpcCredentials(),
                new RpcOptions()
        );

        Futures.joinWait1M(ytRpcClient.waitProxies());

        waitHealthyTabletCell(ytRpcClient);

        return ytRpcClient;
    }

    @Bean(destroyMethod = "close")
    public BusConnector busConnector() {
        return new DefaultBusConnector(new NioEventLoopGroup(), true);
    }

    @Bean
    public KvStorageClient kvStorageClient(YtClient ytClient) {
        return new KvStorageClientImpl(ytClient, new SingleBackendSelectorImpl(ytClient));
    }

    @Bean
    public YtFolders ytFolders() {
        return new YtFolders(YT_HOME);
    }

    @Bean
    public YtTables ytTables(YtFolders ytFolders) {
        return new YtTables(ytFolders, YT_HOME + "/triggers_history", YT_HOME + "/users", YT_HOME + "/user_ids");
    }

    @Bean
    public YtClusters ytClusters() {
        return new YtClusters(
                new YtClusters.YtCluster("hahn"),
                Set.of(
                        new YtClusters.YtCluster("sas", "hahn", ClusterGroup.OFFLINE)
                )
        );
    }

    private void waitHealthyTabletCell(YtClient ytClient) {
        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < 60_000) {
            List<YTreeNode> cells = ytClient.listNode(
                    new ListNode("//sys/tablet_cells")
                            .setAttributes(ColumnFilter.of(HEALTH))
            ).join().asList();

            if (cells.isEmpty()) {
                continue;
            }

            boolean isHealthy = cells.stream()
                    .allMatch(entry -> entry.getAttribute(HEALTH)
                            .map(YTreeNode::stringValue)
                            .map("good"::equalsIgnoreCase)
                            .orElse(false)
                    );

            if (isHealthy) {
                return;
            }

            ThreadUtils.sleep(500, TimeUnit.MILLISECONDS);
        }

        throw new IllegalStateException("No healthy tablet cell");
    }
}
