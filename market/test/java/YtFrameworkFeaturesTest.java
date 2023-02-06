package ru.yandex.market.javaframework.yt;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.impl.YtConfiguration;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeStringNodeImpl;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.javaframework.internal.beanfactory.mapbyqualifier.MapByQualifierBeanFactory;
import ru.yandex.market.javaframework.yt.config.YtConfigurersAutoConfiguration;
import ru.yandex.market.javaframework.yt.config.YtProviderAutoConfiguration;
import ru.yandex.market.javaframework.yt.provider.IYtClientId;
import ru.yandex.market.javaframework.yt.provider.YtProvider;
import ru.yandex.market.starter.yt.config.YtAsyncAutoConfiguration;
import ru.yandex.market.starter.yt.config.YtConfigAutoConfiguration;
import ru.yandex.market.starter.yt.config.YtRpcClientAutoConfiguration;
import ru.yandex.market.starter.yt.config.YtSyncAutoConfiguration;
import ru.yandex.market.starter.yt.config.multiclient.YtAsyncProviderAutoConfiguration;
import ru.yandex.market.starter.yt.config.multiclient.YtMultiClientAutoConfiguration;
import ru.yandex.market.starter.yt.config.multiclient.YtMultiClientContext;
import ru.yandex.market.starter.yt.config.multiclient.YtSyncProviderAutoConfiguration;
import ru.yandex.market.starter.yt.configurer.YtConfigurersHolder;
import ru.yandex.market.starter.yt.configurer.sync.YtSyncConfigurer;
import ru.yandex.market.starter.yt.configurer.sync.YtSyncConfigurerAdapter;
import ru.yandex.market.starter.yt.provider.async.YtRpcClientProvider;
import ru.yandex.yt.ytclient.proxy.YtClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class YtFrameworkFeaturesTest {
    private static final String TOKEN = "token";
    private static final String API_HOST = "apiHost";

    private final ApplicationContextRunner contextRunner =
        new ApplicationContextRunner(() -> new AnnotationConfigApplicationContext(new MapByQualifierBeanFactory()))
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
            YtSyncProviderAutoConfiguration.class,
            // from framework
            YtConfigurersAutoConfiguration.class,
            YtProviderAutoConfiguration.class
        ));

    @Test
    void ytProviderTest() {
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
                assertThat(context).hasSingleBean(YtProvider.class);
                final YtProvider ytProvider = context.getBean(YtProvider.class);

                // test client 1
                assertThat(
                    ytProvider.get(new TestYtClientId<>(testClientName1, Yt.class))
                ).isNotNull();

                // test client 2
                assertThat(
                    ytProvider.get(new TestYtClientId<>(testClientName2, ru.yandex.inside.yt.kosher.async.Yt.class))
                ).isNotNull();

                // test client 3
                assertThat(
                    ytProvider.get(new TestYtClientId<>(testClientName3, ru.yandex.inside.yt.kosher.async.Yt.class))
                ).isNotNull();
            }
        );
    }

    @Test
    void ytConfigurersMappingTest() {
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

    private static class TestYtClientId<T> implements IYtClientId<T> {
        private final String id;
        private final Class<T> clientClass;

        public TestYtClientId(String id, Class<T> clientClass) {
            this.id = id;
            this.clientClass = clientClass;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public Class<T> getClientClass() {
            return clientClass;
        }
    }

    protected static class YtConfigurersConfiguration {

        private static final String YT_CLIENT_ID = "configurerTestClient";
        private static final YTreeNode TEST_JOB_SPEC_PATCH = new YTreeStringNodeImpl(YT_CLIENT_ID, null);

        @Qualifier(YT_CLIENT_ID)
        @Bean
        public YtSyncConfigurer testClientJobSpecPatch() {
            return new YtSyncConfigurerAdapter() {
                @Override
                public YTreeNode getJobSpecPatch() {
                    return TEST_JOB_SPEC_PATCH;
                }
            };
        }
    }
}
