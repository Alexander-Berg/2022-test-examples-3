package ru.yandex.market.starter.yt;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.YtConfiguration;
import ru.yandex.inside.yt.kosher.impl.async.YtImplHttp;
import ru.yandex.inside.yt.kosher.impl.async.YtImplRpc;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeStringNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.starter.yt.config.YtAsyncAutoConfiguration;
import ru.yandex.market.starter.yt.config.YtConfigAutoConfiguration;
import ru.yandex.market.starter.yt.config.YtRpcClientAutoConfiguration;
import ru.yandex.market.starter.yt.config.YtSyncAutoConfiguration;
import ru.yandex.market.starter.yt.config.multiclient.YtAsyncProviderAutoConfiguration;
import ru.yandex.market.starter.yt.config.multiclient.YtMultiClientAutoConfiguration;
import ru.yandex.market.starter.yt.config.multiclient.YtMultiClientContext;
import ru.yandex.market.starter.yt.config.multiclient.YtSyncProviderAutoConfiguration;
import ru.yandex.market.starter.yt.configurer.YtConfigurersHolder;
import ru.yandex.market.starter.yt.configurer.sync.YtSyncConfigurerAdapter;
import ru.yandex.market.starter.yt.provider.async.YtAsyncProvider;
import ru.yandex.market.starter.yt.provider.async.YtRpcClientProvider;
import ru.yandex.market.starter.yt.provider.sync.YtSyncProvider;
import ru.yandex.yt.ytclient.proxy.YtClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class YtAutoConfigurationTest {
    private static final String TOKEN = "token";
    private static final String API_HOST = "apiHost";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withPropertyValues(
            "mj.yt.apiHost=" + API_HOST,
            "mj.yt.token=" + TOKEN
        )
        .withConfiguration(AutoConfigurations.of(
            YtConfigAutoConfiguration.class,
            YtAsyncAutoConfiguration.class,
            YtRpcClientAutoConfiguration.class,
            YtSyncAutoConfiguration.class,
            YtMultiClientAutoConfiguration.class,
            YtAsyncProviderAutoConfiguration.class,
            YtSyncProviderAutoConfiguration.class
        ));

    private final ApplicationContextRunner contextRunnerWithoutDefLayers =
        contextRunner.withPropertyValues("mj.yt.defaultPortoLayers=false");

    @Test
    void ytConfig_SimpleParams_Test() {
        final String user = "user";
        final String pool = "pool";
        final String role = "role";
        final boolean https = true;
        final String javaBinary = "javaBinary";
        final String tmpDir = "//tmpDir";
        final String cacheDir = "//cacheDir";
        final int fileCacheReplicationFactor = 12324;
        final int simpleCommandsRetries = 97;
        final int operationStatusPingTimeoutMillis = 42323;
        final int heavyCommandsRetries = 435;
        final int heavyCommandsTimeoutMillis = 6576;
        final int heavyProxiesTimeoutMillis = 54263;
        final int writeChunkSize = 7822;
        final int snapshotTransactionTimeoutMillis = 87763;
        final int snapshotTransactionPingPeriodMillis = 234;
        final int httpClientMaxConnectionCount = 87427;
        final boolean suppressAccessTracking = true;
        final boolean suppressModificationTracking = true;
        final String proxyHost = "proxyHost";
        final int proxyPort = 7564;
        final boolean httpsForProxy = true;

        contextRunner
            .withPropertyValues(
                "mj.yt.user=" + user,
                "mj.yt.pool=" + pool,
                "mj.yt.role=" + role,
                "mj.yt.https=" + https,
                "mj.yt.javaBinary=" + javaBinary,
                "mj.yt.tmpDir=" + tmpDir,
                "mj.yt.cacheDir=" + cacheDir,
                "mj.yt.fileCacheReplicationFactor=" + fileCacheReplicationFactor,
                "mj.yt.simpleCommandsRetries=" + simpleCommandsRetries,
                "mj.yt.operationStatusPingTimeoutMillis=" + operationStatusPingTimeoutMillis,
                "mj.yt.heavyCommandsRetries=" + heavyCommandsRetries,
                "mj.yt.heavyCommandsTimeoutMillis=" + heavyCommandsTimeoutMillis,
                "mj.yt.heavyProxiesTimeoutMillis=" + heavyProxiesTimeoutMillis,
                "mj.yt.writeChunkSize=" + writeChunkSize,
                "mj.yt.snapshotTransactionTimeoutMillis=" + snapshotTransactionTimeoutMillis,
                "mj.yt.snapshotTransactionPingPeriodMillis=" + snapshotTransactionPingPeriodMillis,
                "mj.yt.httpClientMaxConnectionCount=" + httpClientMaxConnectionCount,
                "mj.yt.suppressAccessTracking=" + suppressAccessTracking,
                "mj.yt.suppressModificationTracking=" + suppressModificationTracking,
                "mj.yt.proxyHost=" + proxyHost,
                "mj.yt.proxyPort=" + proxyPort,
                "mj.yt.httpsForProxy=" + httpsForProxy
            )
            .run(context -> {
                    assertThat(context).hasSingleBean(YtConfiguration.class);
                    assertThat(context).doesNotHaveBean(YtMultiClientContext.class);
                    final YtConfiguration ytConfiguration = context.getBean(YtConfiguration.class);
                    assertThat(ytConfiguration.getApiHost()).isEqualTo(API_HOST);
                    assertThat(ytConfiguration.getToken()).isEqualTo(TOKEN);
                    assertThat(ytConfiguration.getUser()).isEqualTo(user);
                    assertThat(ytConfiguration.getPool().get()).isEqualTo(pool);
                    assertThat(ytConfiguration.getRole().get()).isEqualTo(role);
                    assertThat(ytConfiguration.getUseHttps()).isEqualTo(https);
                    assertThat(ytConfiguration.getJavaBinary()).isEqualTo(javaBinary);
                    assertThat(ytConfiguration.getTmpDir()).isEqualTo(YPath.simple(tmpDir));
                    assertThat(ytConfiguration.getCacheDir().get()).isEqualTo(YPath.simple(cacheDir));
                    assertThat(ytConfiguration.getFileCacheReplicationFactor()).isEqualTo(fileCacheReplicationFactor);
                    assertThat(ytConfiguration.getSimpleCommandsRetries()).isEqualTo(simpleCommandsRetries);
                    assertThat(ytConfiguration.getOperationStatusPingTimeout())
                        .isEqualTo(Duration.ofMillis(operationStatusPingTimeoutMillis));
                    assertThat(ytConfiguration.getHeavyCommandsRetries()).isEqualTo(heavyCommandsRetries);
                    assertThat(ytConfiguration.getHeavyCommandsTimeout())
                        .isEqualTo(Duration.ofMillis(heavyCommandsTimeoutMillis));
                    assertThat(ytConfiguration.getHeavyProxiesTimeout())
                        .isEqualTo(Duration.ofMillis(heavyProxiesTimeoutMillis));
                    assertThat(ytConfiguration.getWriteChunkSize()).isEqualTo(writeChunkSize);
                    assertThat(ytConfiguration.getSnapshotTransactionTimeout())
                        .isEqualTo(Duration.ofMillis(snapshotTransactionTimeoutMillis));
                    assertThat(ytConfiguration.getSnapshotTransactionPingPeriod())
                        .isEqualTo(Duration.ofMillis(snapshotTransactionPingPeriodMillis));
                    assertThat(ytConfiguration.getHttpClientMaxConnectionCount()).isEqualTo(httpClientMaxConnectionCount);
                }
            );
    }

    @Test
    void ytConfig_ApiUrl_Test() {
        final String apiUrl = "https://yandex.ru";
        contextRunner
            .withPropertyValues(
                "mj.yt.apiUrl=" + apiUrl
            )
            .run(context -> {
                    assertThat(context).doesNotHaveBean(YtMultiClientContext.class);
                    final YtConfiguration ytConfiguration = context.getBean(YtConfiguration.class);
                    assertThat(ytConfiguration.getApiHost()).isEqualTo("yandex.ru:443");
                }
            );
    }

    @Test
    void ytConfig_Cluster_Test() {
        final String cluster = "seneca-vla";
        contextRunner
            .withPropertyValues(
                "mj.yt.cluster=" + cluster
            )
            .run(context -> {
                    final YtConfiguration ytConfiguration = context.getBean(YtConfiguration.class);
                    assertThat(ytConfiguration.getApiHost()).isEqualTo(cluster + ".yt.yandex.net");
                }
            );
    }

    @Test
    void ytConfig_JavaOptions_Test() {
        final List<String> javaOptions = List.of("yjjysd", "xvsadj", "uoiuojs");
        contextRunnerWithoutDefLayers
            .withPropertyValues(
                "mj.yt.javaOptions[0]=" + javaOptions.get(0),
                "mj.yt.javaOptions[1]=" + javaOptions.get(1),
                "mj.yt.javaOptions[2]=" + javaOptions.get(2)
            )
            .run(context -> {
                    final YtConfiguration ytConfiguration = context.getBean(YtConfiguration.class);
                    assertThat(ytConfiguration.getJavaOptions()).isEqualTo(javaOptions);
                }
            );
    }

    @Test
    void ytConfig_Atomicity_Test() {
        contextRunner
            .withPropertyValues(
                "mj.yt.atomicity=true"
            )
            .run(context -> {
                    final YtConfiguration ytConfiguration = context.getBean(YtConfiguration.class);
                    assertThat(ytConfiguration.getAtomicity().get()).isEqualTo("full");
                }
            );

        contextRunner
            .withPropertyValues(
                "mj.yt.atomicity=false"
            )
            .run(context -> {
                    final YtConfiguration ytConfiguration = context.getBean(YtConfiguration.class);
                    assertThat(ytConfiguration.getAtomicity().get()).isEqualTo("none");
                }
            );
    }

    @Test
    void ytConfig_Acl_Test() {
        final List<String> permissions = List.of("werewr", "htvc", "gfgiz");
        final List<String> subjects = List.of("jgsc", "dsfcx", "bjdfs");
        contextRunner
            .withPropertyValues(
                "mj.yt.acl.permissions[0]=" + permissions.get(0),
                "mj.yt.acl.permissions[1]=" + permissions.get(1),
                "mj.yt.acl.permissions[2]=" + permissions.get(2),
                "mj.yt.acl.subjects[0]=" + subjects.get(0),
                "mj.yt.acl.subjects[1]=" + subjects.get(1),
                "mj.yt.acl.subjects[2]=" + subjects.get(2)
            )
            .run(context -> {
                    final YtConfiguration ytConfiguration = context.getBean(YtConfiguration.class);

                    final YTreeNode yTreeNode = ytConfiguration.getSpecPatch().get();
                    final YTreeNode aclNode = yTreeNode.asMap().get("acl").asList().get(0);

                    // Check permissions
                    final YTreeNode permissionsNode = aclNode.asMap().get("permissions");

                    final List<YTreeNode> permissionsNodesList = permissionsNode.asList();
                    assertThat(permissionsNodesList.size()).isEqualTo(permissions.size());

                    for (int i = 0; i < permissions.size(); i++) {
                        assertThat(permissionsNodesList.get(i).stringValue()).isEqualTo(permissions.get(i));
                    }

                    // Check subjects
                    final YTreeNode subjectsNode = aclNode.asMap().get("subjects");

                    final List<YTreeNode> subjectsNodesList = subjectsNode.asList();
                    assertThat(subjectsNodesList.size()).isEqualTo(subjects.size());

                    for (int i = 0; i < subjects.size(); i++) {
                        assertThat(subjectsNodesList.get(i).stringValue()).isEqualTo(subjects.get(i));
                    }
                }
            );
    }

    @Test
    void ytConfig_Owners_Test() {
        final List<String> owners = List.of("test1", "test2", "test3");
        contextRunner
            .withPropertyValues(
                "mj.yt.owners[0]=" + owners.get(0),
                "mj.yt.owners[1]=" + owners.get(1),
                "mj.yt.owners[2]=" + owners.get(2)
            )
            .run(context -> {
                    final YtConfiguration ytConfiguration = context.getBean(YtConfiguration.class);

                    final YTreeNode yTreeNode = ytConfiguration.getSpecPatch().get();
                    final YTreeNode ownersNode = yTreeNode.asMap().get("owners");

                    final List<YTreeNode> ownersNodesList = ownersNode.asList();
                    assertThat(ownersNodesList.size()).isEqualTo(owners.size());

                    for (int i = 0; i < owners.size(); i++) {
                        assertThat(ownersNodesList.get(i).stringValue()).isEqualTo(owners.get(i));
                    }
                }
            );
    }

    @Test
    void ytConfig_SpecPath_JobSpecPath_Test() {
        final YTreeNode node = YTree.builder()
            .beginMap()
            .key("test1").value("val1")
            .key("test2").value(Map.of("subtest1", "val2", "subtest2", "val3"))
            .endMap().build();

        // spec patch
        contextRunnerWithoutDefLayers
            .withPropertyValues(
                "mj.yt.specPatch.test1=val1",
                "mj.yt.specPatch.test2.subtest1=val2",
                "mj.yt.specPatch.test2.subtest2=val3"
            )
            .run(context -> {
                    final YtConfiguration ytConfiguration = context.getBean(YtConfiguration.class);
                    assertThat(ytConfiguration.getSpecPatch().get()).isEqualTo(node);
                }
            );

        // job spec patch
        contextRunnerWithoutDefLayers
            .withPropertyValues(
                "mj.yt.jobSpecPatch.test1=val1",
                "mj.yt.jobSpecPatch.test2.subtest1=val2",
                "mj.yt.jobSpecPatch.test2.subtest2=val3"
            )
            .run(context -> {
                    final YtConfiguration ytConfiguration = context.getBean(YtConfiguration.class);
                    assertThat(ytConfiguration.getJobSpecPatch().get()).isEqualTo(node);
                }
            );
    }

    @Test
    void ytConfig_DefaultLayers_Test() {
        // Without default layers
        contextRunnerWithoutDefLayers
            .run(context -> {
                    assertThat(context).hasSingleBean(YtConfiguration.class);
                    final YtConfiguration ytConfiguration = context.getBean(YtConfiguration.class);
                    assertThat(ytConfiguration.getSpecPatch().isPresent()).isFalse();
                }
            );

        // With default layers
        contextRunner
            .run(context -> {
                    assertThat(context).hasSingleBean(YtConfiguration.class);
                    final YtConfiguration ytConfiguration = context.getBean(YtConfiguration.class);
                    assertThat(ytConfiguration.getSpecPatch().isPresent()).isTrue();
                }
            );

        contextRunnerWithoutDefLayers
            .withPropertyValues(
                "mj.yt.porto=true"
            )
            .run(context -> {
                    assertThat(context).hasSingleBean(YtConfiguration.class);
                    final YtConfiguration ytConfiguration = context.getBean(YtConfiguration.class);
                    assertThat(ytConfiguration.getSpecPatch().isPresent()).isTrue();
                }
            );

        contextRunnerWithoutDefLayers
            .withPropertyValues(
                "mj.yt.portoJava8=true"
            )
            .run(context -> {
                    assertThat(context).hasSingleBean(YtConfiguration.class);
                    final YtConfiguration ytConfiguration = context.getBean(YtConfiguration.class);
                    assertThat(ytConfiguration.getSpecPatch().isPresent()).isTrue();
                }
            );

        contextRunnerWithoutDefLayers
            .withPropertyValues(
                "mj.yt.portoJava10=true"
            )
            .run(context -> {
                    assertThat(context).hasSingleBean(YtConfiguration.class);
                    final YtConfiguration ytConfiguration = context.getBean(YtConfiguration.class);
                    assertThat(ytConfiguration.getSpecPatch().isPresent()).isTrue();
                }
            );

        contextRunnerWithoutDefLayers
            .withPropertyValues(
                "mj.yt.portoJava11=true"
            )
            .run(context -> {
                    assertThat(context).hasSingleBean(YtConfiguration.class);
                    final YtConfiguration ytConfiguration = context.getBean(YtConfiguration.class);
                    assertThat(ytConfiguration.getSpecPatch().isPresent()).isTrue();
                }
            );

        contextRunnerWithoutDefLayers
            .withPropertyValues(
                "mj.yt.portoJava15=true"
            )
            .run(context -> {
                    assertThat(context).hasSingleBean(YtConfiguration.class);
                    final YtConfiguration ytConfiguration = context.getBean(YtConfiguration.class);
                    assertThat(ytConfiguration.getSpecPatch().isPresent()).isTrue();
                }
            );

        contextRunnerWithoutDefLayers
            .withPropertyValues(
                "mj.yt.portoJava17=true"
            )
            .run(context -> {
                    assertThat(context).hasSingleBean(YtConfiguration.class);
                    final YtConfiguration ytConfiguration = context.getBean(YtConfiguration.class);
                    assertThat(ytConfiguration.getSpecPatch().isPresent()).isTrue();
                }
            );
    }

    @Test
    void ytSyncTest() {
        // by default
        contextRunner.run(context -> {
                assertThat(context).doesNotHaveBean(YtMultiClientContext.class);
                assertThat(context).hasSingleBean(YtConfiguration.class);
                assertThat(context).hasSingleBean(Yt.class);
                assertThat(context).doesNotHaveBean(ru.yandex.inside.yt.kosher.async.Yt.class);
            }
        );

        // manual declaration
        contextRunner.withPropertyValues(
            "mj.yt.async=false"
        ).run(context -> {
                assertThat(context).doesNotHaveBean(YtMultiClientContext.class);
                assertThat(context).hasSingleBean(YtConfiguration.class);
                assertThat(context).hasSingleBean(Yt.class);
                assertThat(context).doesNotHaveBean(ru.yandex.inside.yt.kosher.async.Yt.class);
            }
        );
    }

    @Test
    void ytAsyncTest() {
        contextRunner.withPropertyValues(
            "mj.yt.async=true"
        ).run(context -> {
                assertThat(context).doesNotHaveBean(YtMultiClientContext.class);
                assertThat(context).hasSingleBean(YtConfiguration.class);
                assertThat(context).doesNotHaveBean(Yt.class);
                assertThat(context).hasSingleBean(ru.yandex.inside.yt.kosher.async.Yt.class);
                final ru.yandex.inside.yt.kosher.async.Yt asyncYt =
                    context.getBean(ru.yandex.inside.yt.kosher.async.Yt.class);
                assertThat(asyncYt).isInstanceOf(YtImplHttp.class);
            }
        );
    }

    @Test
    void ytAsyncRpcTest() {
        contextRunner
            .withUserConfiguration(TestYtClientConfiguration.class)
            .withPropertyValues(
                "mj.yt.async=true",
                "mj.yt.protocol=rpc"
            ).run(context -> {
                    assertThat(context).doesNotHaveBean(YtMultiClientContext.class);
                    assertThat(context).hasSingleBean(YtConfiguration.class);
                    assertThat(context).doesNotHaveBean(Yt.class);
                    assertThat(context).hasSingleBean(ru.yandex.inside.yt.kosher.async.Yt.class);

                    final ru.yandex.inside.yt.kosher.async.Yt asyncYt =
                        context.getBean(ru.yandex.inside.yt.kosher.async.Yt.class);
                    assertThat(asyncYt).isInstanceOf(YtImplRpc.class);
                }
            );
    }

    @Test
    void ytCustomRpcClientTest() {
        final ApplicationContextRunner customYtClientRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                YtConfigAutoConfiguration.class,
                YtRpcClientAutoConfiguration.class
            ))
            .withPropertyValues(
                "mj.yt.token=sdffsd",
                "mj.yt.apiHost=eryty"
            );

        customYtClientRunner
            .withPropertyValues(
                "mj.yt.rpc.custom=true"
            ).run(context -> {
                    assertThat(context).hasSingleBean(YtClient.class);
                }
            );
    }

    @Test
    void ytMultiClientTest() {
        final String testClientName1 = "testclient1";
        final String testClientName2 = "testclient2";
        final String testClientName3 = "testclient3";
        final String cluster1 = "hume";
        final String cluster2 = "arnold";
        final String cluster3 = "hahn";

        contextRunner
            .withUserConfiguration(TestMultiYtClientConfiguration.class)
            .withPropertyValues(
            "mj.yt.clients." + testClientName1 + ".cluster=" + cluster1,
            "mj.yt.clients." + testClientName2 + ".cluster=" + cluster2,
            "mj.yt.clients." + testClientName2 + ".async=true",
            "mj.yt.clients." + testClientName3 + ".cluster=" + cluster3,
            "mj.yt.clients." + testClientName3 + ".async=true",
            "mj.yt.clients." + testClientName3 + ".protocol=rpc",
            "mj.yt.clients." + testClientName3 + ".rpc.custom=true"

        ).run(context -> {
                assertThat(context).hasSingleBean(YtMultiClientContext.class);
                assertThat(context).doesNotHaveBean(YtConfiguration.class);
                assertThat(context).doesNotHaveBean(Yt.class);
                assertThat(context).doesNotHaveBean(ru.yandex.inside.yt.kosher.async.Yt.class);
                assertThat(context).hasSingleBean(YtSyncProvider.class);
                assertThat(context).hasSingleBean(YtAsyncProvider.class);

                final YtMultiClientContext ytMultiClientContext = context.getBean(YtMultiClientContext.class);

                // test client 1
                final YtSyncProvider ytSyncProvider = context.getBean(YtSyncProvider.class);
                assertThat(ytSyncProvider.get(testClientName1)).isNotNull().isInstanceOf(Yt.class);
                final YtConfiguration ytConfiguration1 =
                    ytMultiClientContext.getYtConfigurationMap().get(testClientName1);
                assertThat(ytConfiguration1).isNotNull();
                assertThat(ytConfiguration1.getApiHost()).startsWith(cluster1);


                // test client 2
                final YtAsyncProvider ytAsyncProvider = context.getBean(YtAsyncProvider.class);
                assertThat(ytAsyncProvider.get(testClientName2)).isNotNull().isInstanceOf(YtImplHttp.class);
                final YtConfiguration ytConfiguration2 =
                    ytMultiClientContext.getYtConfigurationMap().get(testClientName2);
                assertThat(ytConfiguration2).isNotNull();
                assertThat(ytConfiguration2.getApiHost()).startsWith(cluster2);

                // test client 3
                assertThat(ytAsyncProvider.get(testClientName3)).isNotNull().isInstanceOf(YtImplRpc.class);
                final YtConfiguration ytConfiguration3 =
                    ytMultiClientContext.getYtConfigurationMap().get(testClientName3);
                assertThat(ytConfiguration3).isNotNull();
                assertThat(ytConfiguration3.getApiHost()).startsWith(cluster3);
            }
        );
    }

    @Test
    void ytMultiRpcClientTest() {
        final String testClientName1 = "testclient1";
        final String testClientName2 = "testclient2";
        final String cluster1 = "arnold";
        final String cluster2 = "hahn";

        contextRunner
            .withUserConfiguration(TestMultiYtClientConfiguration.class)
            .withPropertyValues(
            "mj.yt.clients." + testClientName1 + ".cluster=" + cluster1,
            "mj.yt.clients." + testClientName1 + ".async=true",
            "mj.yt.clients." + testClientName1 + ".protocol=rpc",
            "mj.yt.clients." + testClientName1 + ".rpc.custom=true",
            "mj.yt.clients." + testClientName2 + ".cluster=" + cluster2,
            "mj.yt.clients." + testClientName2 + ".async=true",
            "mj.yt.clients." + testClientName2 + ".protocol=rpc",
            "mj.yt.clients." + testClientName2 + ".rpc.custom=true"
        ).run(context -> {
                assertThat(context).hasSingleBean(YtMultiClientContext.class);
                assertThat(context).doesNotHaveBean(YtConfiguration.class);
                assertThat(context).doesNotHaveBean(Yt.class);
                assertThat(context).doesNotHaveBean(ru.yandex.inside.yt.kosher.async.Yt.class);
                assertThat(context).hasSingleBean(YtSyncProvider.class);
                assertThat(context).hasSingleBean(YtAsyncProvider.class);
                assertThat(context).hasSingleBean(YtRpcClientProvider.class);

                final YtRpcClientProvider ytRpcClientProvider = context.getBean(YtRpcClientProvider.class);
                assertThat(ytRpcClientProvider.get(testClientName1)).isNotNull();
                assertThat(ytRpcClientProvider.get(testClientName2)).isNotNull();
            }
        );
    }

    @Test
    void ytConfigurersTest() {
        contextRunner
            .withUserConfiguration(YtConfigurersConfiguration.class)
            .withPropertyValues(
                "mj.yt.clients." + YtConfigurersConfiguration.YT_CLIENT_ID + ".cluster=hume"
            )
            .run(context -> {
                    assertThat(context).hasSingleBean(YtConfigurersHolder.class);
                    assertThat(context).hasSingleBean(YtMultiClientContext.class);

                    final YtMultiClientContext ytMultiClientContext = context.getBean(YtMultiClientContext.class);
                    final YtConfiguration ytConfiguration =
                        ytMultiClientContext.getYtConfigurationMap().get(YtConfigurersConfiguration.YT_CLIENT_ID);
                    assertThat(ytConfiguration).isNotNull();
                    assertThat(ytConfiguration.getJobSpecPatch().get())
                        .isEqualTo(YtConfigurersConfiguration.TEST_JOB_SPEC_PATCH);
                }
            );
    }

    protected static class TestYtClientConfiguration {

        @Bean
        public YtClient ytClient() {
            final YtClient ytClient = mock(YtClient.class);
            when(ytClient.waitProxies()).thenReturn(CompletableFuture.completedFuture(null));
            return ytClient;
        }
    }

    protected static class TestMultiYtClientConfiguration {

        @Bean
        public BeanPostProcessor ytRpcClientProviderProcessor() {
            return new BeanPostProcessor() {
                @Override
                public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
                    if (bean instanceof YtRpcClientProvider) {
                        final HashMap<String, YtClient> mockedYtClients = new HashMap<>();
                        final YtRpcClientProvider ytRpcClientProvider = (YtRpcClientProvider) bean;
                        for (Map.Entry<String, YtClient> entry :
                            ytRpcClientProvider.getYtClientMap().entrySet()) {

                            final YtClient ytClient = mock(YtClient.class);
                            when(ytClient.waitProxies()).thenReturn(CompletableFuture.completedFuture(null));
                            mockedYtClients.put(entry.getKey(), ytClient);
                        }
                        return new YtRpcClientProvider(mockedYtClients);
                    }
                    return BeanPostProcessor.super.postProcessBeforeInitialization(bean, beanName);
                }
            };
        }
    }

    protected static class YtConfigurersConfiguration {

        private static final String YT_CLIENT_ID = "configurerTestClient";
        private static final YTreeNode TEST_JOB_SPEC_PATCH = new YTreeStringNodeImpl(YT_CLIENT_ID, null);

        @Bean
        public YtConfigurersHolder ytConfigurersHolder() {
            final YtConfigurersHolder ytConfigurersHolder = new YtConfigurersHolder();
            ytConfigurersHolder.setYtSyncConfigurers(Map.of(YT_CLIENT_ID,
                new YtSyncConfigurerAdapter() {
                    @Override
                    public YTreeNode getJobSpecPatch() {
                        return TEST_JOB_SPEC_PATCH;
                    }
                }
            ));
            return ytConfigurersHolder;
        }
    }
}
