package ru.yandex.market.crm.core.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Named;

import com.google.common.collect.ImmutableSet;
import io.netty.channel.nio.NioEventLoopGroup;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.operations.jars.JavaYtRunner;
import ru.yandex.inside.yt.kosher.operations.Operation;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeStringNode;
import ru.yandex.market.crm.yt.YtAclConfiguration;
import ru.yandex.market.crm.yt.operations.OperationHandler;
import ru.yandex.market.crm.core.yt.CrmJarProcessor;
import ru.yandex.market.crm.core.yt.KeyValueStorage;
import ru.yandex.market.crm.core.yt.YtReplicationConfig;
import ru.yandex.market.crm.yt.client.YtClient;
import ru.yandex.market.crm.yt.operations.YtOperationStrategy;
import ru.yandex.market.crm.core.yt.paths.YtFolders;
import ru.yandex.market.crm.yt.tx.CallbackExecutor;
import ru.yandex.market.crm.yt.tx.TxRunner;
import ru.yandex.market.crm.core.yt.tx.TxRunnerImpl;
import ru.yandex.market.crm.core.yt.tx.YtTransactionManager;
import ru.yandex.market.crm.util.Futures;
import ru.yandex.market.crm.yt.YtClientBuilder;
import ru.yandex.misc.thread.ThreadUtils;
import ru.yandex.yt.ytclient.bus.BusConnector;
import ru.yandex.yt.ytclient.bus.DefaultBusConnector;
import ru.yandex.yt.ytclient.proxy.YtCluster;
import ru.yandex.yt.ytclient.rpc.RpcCredentials;
import ru.yandex.yt.ytclient.rpc.RpcOptions;

import static java.nio.file.attribute.PosixFilePermission.GROUP_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.GROUP_READ;
import static java.nio.file.attribute.PosixFilePermission.GROUP_WRITE;
import static java.nio.file.attribute.PosixFilePermission.OTHERS_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OTHERS_READ;
import static java.nio.file.attribute.PosixFilePermission.OTHERS_WRITE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;
import static ru.yandex.market.crm.core.yt.CoreYtConfig.MAIN_CLUSTER_CLIENT;

/**
 * Использует для локальный yt, который перед прогоном тестов должен быть поднят рецептом
 * см. ya.make модуля
 *
 * @author apershukov
 */
@Configuration
@EnableTransactionManagement(proxyTargetClass = true)
@ComponentScan("ru.yandex.market.crm.core.yt.paths")
@Import(YtAclConfiguration.class)
public class LocalYtConfig {

    private static String getProxy() {
        return EnvProvider.getEnv("YT_PROXY");
    }

    @Bean
    public YtClient ytClient(Yt yt,
                             @Named(MAIN_CLUSTER_CLIENT) ru.yandex.yt.ytclient.proxy.YtClient ytRpcClient) {
        OperationHandler<Operation, Void> operationHandler = new OperationHandler<>(
                "yt",
                new CallbackExecutor(),
                new YtOperationStrategy()
        );

        YtClient ytClient = new YtClient(
                "market-lilucrm",
                "default",
                yt,
                ytRpcClient,
                operationHandler
        );

        waitHealthyTabletCell(ytClient);

        return ytClient;
    }


    @Bean("ytTxManager")
    public YtTransactionManager ytTransactionManager(
            @Named(MAIN_CLUSTER_CLIENT) ru.yandex.yt.ytclient.proxy.YtClient rpcYtClient,
            BusConnector busConnector) {
        return new YtTransactionManager(rpcYtClient, busConnector);
    }

    @Bean
    public TxRunner ytTransactionTemplate(YtTransactionManager txManager) {
        return new TxRunnerImpl(txManager);
    }

    @Bean
    public CrmJarProcessor jarProcessor(@Value("${yt.jars.dir}") String jarsDir) {
        return new CrmJarProcessor(jarsDir);
    }

    @Bean
    public Yt yt(CrmJarProcessor crmJarProcessor, YtFolders ytFolders) throws IOException {
        Yt yt = YtClientBuilder.forCluster(getProxy(), "")
                .setJavaPath(extractJdk())
                .setStatePingTimeoutSec(10L)
                .setJarsProcessor(crmJarProcessor)
                .setTmpDirectory(ytFolders.getTmp())
                .build();

        JavaYtRunner.uploadJars(yt, null);

        return yt;
    }

    @Bean(YtReplicationConfig.META_CLUSTER_CLIENT)
    public ru.yandex.yt.ytclient.proxy.YtClient metaClusterYtClient(@Named(MAIN_CLUSTER_CLIENT) ru.yandex.yt.ytclient.proxy.YtClient ytClient) {
        return ytClient;
    }

    @Bean(YtReplicationConfig.REPLICA_CLUSTERS_CLIENT)
    public ru.yandex.yt.ytclient.proxy.YtClient replicaClusterYtClient(@Named(MAIN_CLUSTER_CLIENT) ru.yandex.yt.ytclient.proxy.YtClient ytClient) {
        return ytClient;
    }

    @Bean
    public KeyValueStorage keyValueStorage(@Named(MAIN_CLUSTER_CLIENT) ru.yandex.yt.ytclient.proxy.YtClient ytClient) {
        return new KeyValueStorage(ytClient, ytClient);
    }

    private String extractJdk() throws IOException {
        Path archivePath = Paths.get("jdk1.8.0_60.tar");

        if (!Files.exists(archivePath)) {
            // Jdk 11
            archivePath = Paths.get("jdk.tar");
        }

        if (!Files.exists(archivePath)) {
            // Если архивов с jdk нет, значит, скорее всего, тест был запущен из idea
            String javaPath = System.getenv("JAVA_PATH");
            return javaPath == null ? null : javaPath + "/bin/java";
        }

        InputStream fileIn = Files.newInputStream(archivePath);
        Path jdkDir = Files.createTempDirectory("java_");

        try (TarArchiveInputStream in = new TarArchiveInputStream(fileIn)) {
            TarArchiveEntry entry;
            while ((entry = in.getNextTarEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                Path curFile = Paths.get(jdkDir.toString(), entry.getName());
                Files.createDirectories(curFile.getParent());

                try (OutputStream out = Files.newOutputStream(curFile)) {
                    IOUtils.copy(in, out);
                }
            }
        }

        Path binPath = jdkDir.resolve("bin").resolve("java").toAbsolutePath();

        Files.setPosixFilePermissions(
                binPath,
                ImmutableSet.of(
                        OWNER_READ,
                        GROUP_READ,
                        OTHERS_READ,
                        OWNER_WRITE,
                        GROUP_WRITE,
                        OTHERS_WRITE,
                        OWNER_EXECUTE,
                        GROUP_EXECUTE,
                        OTHERS_EXECUTE
                )
        );

        return binPath.toString();
    }

    @Bean(name = MAIN_CLUSTER_CLIENT, destroyMethod = "close")
    public ru.yandex.yt.ytclient.proxy.YtClient ytRpcClient(BusConnector busConnector) {
        String proxy = getProxy();

        String[] parts = proxy.split(":");
        String fqdn = parts[0];
        int port = Integer.parseInt(parts[1]);

        YtCluster cluster = new YtCluster(
                "unknown",
                fqdn,
                port
        );

        ru.yandex.yt.ytclient.proxy.YtClient ytRpcClient = new ru.yandex.yt.ytclient.proxy.YtClient(
                busConnector,
                cluster,
                new RpcCredentials(),
                new RpcOptions()
        );

        Futures.joinWait1M(ytRpcClient.waitProxies());

        return ytRpcClient;
    }

    @Bean(destroyMethod = "close")
    public BusConnector busConnector() {
        return new DefaultBusConnector(new NioEventLoopGroup(), true);
    }

    private void waitHealthyTabletCell(YtClient ytClient) {
        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < 60_000) {
            List<YTreeStringNode> cells = ytClient.getDirectoryEntries(
                    YPath.simple("//sys/tablet_cells"),
                    Collections.singleton("health")
            );

            if (cells.isEmpty()) {
                continue;
            }

            boolean isHealthy = cells.stream()
                    .allMatch(entry -> entry.getAttribute("health")
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
