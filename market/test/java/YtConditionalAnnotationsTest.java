package ru.yandex.market.starter.yt;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;

import ru.yandex.market.starter.yt.annotation.ConditionalOnAnyProperty;
import ru.yandex.market.starter.yt.annotation.ConditionalOnCustomYtRpcClient;
import ru.yandex.market.starter.yt.annotation.ConditionalOnYtClientsCount;
import ru.yandex.market.starter.yt.annotation.ConditionalOnYtConnection;
import ru.yandex.market.starter.properties.yt.YtProtocol;
import ru.yandex.market.starter.yt.annotation.YtClientsMode;

import static org.assertj.core.api.Assertions.*;

public class YtConditionalAnnotationsTest {

    @Test
    public void conditionalOnAnyPropertyTest() {
        final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(ConditionalOnAnyPropertyTestConfiguration.class);

        contextRunner.run(ctx -> assertThat(ctx).doesNotHaveBean(TestBean.class));

        contextRunner
            .withPropertyValues("test.prefix.ewrewr=true")
            .run(ctx -> assertThat(ctx).hasSingleBean(TestBean.class));

        contextRunner
            .withPropertyValues(
                "test.prefix.ewrewr=true",
                "test.prefix.pkc=wewc",
                "mj.yt.protocol=http"
            )
            .run(ctx -> assertThat(ctx).hasSingleBean(TestBean.class));
    }

    @Test
    public void conditionalOnYtConnection_Default_Test() {
        final ApplicationContextRunner contextRunnerDefault = new ApplicationContextRunner()
            .withUserConfiguration(ConditionalOnYtConnection_Default_TestConfiguration.class);

        contextRunnerDefault.run(ctx -> assertThat(ctx).hasSingleBean(TestBean.class));

        contextRunnerDefault
            .withPropertyValues(
                "mj.yt.async=false",
                "mj.yt.protocol=http"
            )
            .run(ctx -> assertThat(ctx).hasSingleBean(TestBean.class));

        contextRunnerDefault
            .withPropertyValues(
                "mj.yt.async=true",
                "mj.yt.protocol=http"
            )
            .run(ctx -> assertThat(ctx).doesNotHaveBean(TestBean.class));

        contextRunnerDefault
            .withPropertyValues(
                "mj.yt.async=false",
                "mj.yt.protocol=rpc"
            )
            .run(ctx -> assertThat(ctx).doesNotHaveBean(TestBean.class));
    }

    @Test
    public void conditionalOnYtConnection_Async_Rpc_Test() {
        final ApplicationContextRunner contextRunnerDefault = new ApplicationContextRunner()
            .withUserConfiguration(ConditionalOnYtConnection_Async_Rpc_TestConfiguration.class);

        contextRunnerDefault.run(ctx -> assertThat(ctx).doesNotHaveBean(TestBean.class));

        contextRunnerDefault
            .withPropertyValues(
                "mj.yt.async=false",
                "mj.yt.protocol=http"
            )
            .run(ctx -> assertThat(ctx).doesNotHaveBean(TestBean.class));

        contextRunnerDefault
            .withPropertyValues(
                "mj.yt.async=true",
                "mj.yt.protocol=http"
            )
            .run(ctx -> assertThat(ctx).doesNotHaveBean(TestBean.class));

        contextRunnerDefault
            .withPropertyValues(
                "mj.yt.async=false",
                "mj.yt.protocol=rpc"
            )
            .run(ctx -> assertThat(ctx).doesNotHaveBean(TestBean.class));

        contextRunnerDefault
            .withPropertyValues(
                "mj.yt.async=true",
                "mj.yt.protocol=rpc"
            )
            .run(ctx -> assertThat(ctx).hasSingleBean(TestBean.class));
    }

    @Test
    public void conditionalOnYtConnection_AnySync_Test() {
        final ApplicationContextRunner contextRunnerDefault = new ApplicationContextRunner()
            .withUserConfiguration(ConditionalOnYtConnection_AnySync_Rpc_TestConfiguration.class);

        contextRunnerDefault
            .withPropertyValues(
                "mj.yt.protocol=rpc"
            )
            .run(ctx -> assertThat(ctx).hasSingleBean(TestBean.class));

        contextRunnerDefault
            .withPropertyValues(
                "mj.yt.async=false",
                "mj.yt.protocol=rpc"
            )
            .run(ctx -> assertThat(ctx).hasSingleBean(TestBean.class));

        contextRunnerDefault
            .withPropertyValues(
                "mj.yt.async=true",
                "mj.yt.protocol=rpc"
            )
            .run(ctx -> assertThat(ctx).hasSingleBean(TestBean.class));
    }

    @Test
    public void conditionalOnYtConnection_AnyProto_Test() {
        final ApplicationContextRunner contextRunnerDefault = new ApplicationContextRunner()
            .withUserConfiguration(ConditionalOnYtConnection_Async_AnyProto_TestConfiguration.class);

        contextRunnerDefault
            .withPropertyValues(
                "mj.yt.async=true"
            )
            .run(ctx -> assertThat(ctx).hasSingleBean(TestBean.class));

        contextRunnerDefault
            .withPropertyValues(
                "mj.yt.async=true",
                "mj.yt.protocol=rpc"
            )
            .run(ctx -> assertThat(ctx).hasSingleBean(TestBean.class));

        contextRunnerDefault
            .withPropertyValues(
                "mj.yt.async=true",
                "mj.yt.protocol=http"
            )
            .run(ctx -> assertThat(ctx).hasSingleBean(TestBean.class));
    }

    @Test
    public void conditionalOnCustomYtRpcClientTest() {
        final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(ConditionalOnCustomYtRpcClientTestConfiguration.class);

        contextRunner.run(ctx -> assertThat(ctx).doesNotHaveBean(TestBean.class));

        contextRunner
            .withPropertyValues("mj.yt.rpc.custom=true")
            .run(ctx -> assertThat(ctx).hasSingleBean(TestBean.class));

        contextRunner
            .withPropertyValues(
                "mj.yt.rpc.preferredClusterName=ytr",
                "mj.yt.protocol=http"
            )
            .run(ctx -> assertThat(ctx).doesNotHaveBean(TestBean.class));

        contextRunner
            .withPropertyValues(
                "mj.yt.protocol=rpc"
            )
            .run(ctx -> assertThat(ctx).doesNotHaveBean(TestBean.class));

        contextRunner
            .withPropertyValues(
                "mj.yt.rpc.preferredClusterName=ytr",
                "mj.yt.protocol=rpc"
            )
            .run(ctx -> assertThat(ctx).hasSingleBean(TestBean.class));
    }

    @Test
    public void conditionalOnYtClientsCountTest() {
        final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(ConditionalOnYtClientsCountTestConfiguration.class);

        contextRunner.run(ctx -> {
            assertThat(ctx).hasSingleBean(TestBean.class);
            final TestBean testBean = ctx.getBean(TestBean.class);
            assertThat(testBean.getName()).isEqualTo(ConditionalOnYtClientsCountTestConfiguration.SINGLE_NAME);
        });

        contextRunner
            .withPropertyValues("mj.yt.apiHost=fsdfd")
            .run(ctx -> {
                assertThat(ctx).hasSingleBean(TestBean.class);
                final TestBean testBean = ctx.getBean(TestBean.class);
                assertThat(testBean.getName()).isEqualTo(ConditionalOnYtClientsCountTestConfiguration.SINGLE_NAME);
            });

        contextRunner
            .withPropertyValues(
                "mj.yt.clients.first.apiHost=wewedwe"
            )
            .run(ctx -> {
                assertThat(ctx).hasSingleBean(TestBean.class);
                final TestBean testBean = ctx.getBean(TestBean.class);
                assertThat(testBean.getName()).isEqualTo(ConditionalOnYtClientsCountTestConfiguration.MULTI_NAME);
            });
    }

    private static class ConditionalOnCustomYtRpcClientTestConfiguration {

        @Bean
        @ConditionalOnCustomYtRpcClient
        public TestBean testBean() {
            return new TestBean();
        }
    }

    private static class ConditionalOnAnyPropertyTestConfiguration {

        @Bean
        @ConditionalOnAnyProperty(prefix = "test.prefix")
        public TestBean testBean() {
            return new TestBean();
        }
    }

    @ConditionalOnYtConnection
    private static class ConditionalOnYtConnection_Default_TestConfiguration {

        @Bean
        public TestBean testBean() {
            return new TestBean();
        }
    }

    @ConditionalOnYtConnection(async = true, proto = YtProtocol.RPC)
    private static class ConditionalOnYtConnection_Async_Rpc_TestConfiguration {

        @Bean
        public TestBean testBean() {
            return new TestBean();
        }
    }

    @ConditionalOnYtConnection(anySync = true, proto = YtProtocol.RPC)
    private static class ConditionalOnYtConnection_AnySync_Rpc_TestConfiguration {

        @Bean
        public TestBean testBean() {
            return new TestBean();
        }
    }

    @ConditionalOnYtConnection(async = true, anyProto = true)
    private static class ConditionalOnYtConnection_Async_AnyProto_TestConfiguration {

        @Bean
        public TestBean testBean() {
            return new TestBean();
        }
    }

    private static class ConditionalOnYtClientsCountTestConfiguration {
        private static final String SINGLE_NAME = "single";
        private static final String MULTI_NAME = "multi";

        @Bean
        @ConditionalOnYtClientsCount(YtClientsMode.SINGLE)
        public TestBean singleTestBean() {
            return new TestBean(SINGLE_NAME);
        }

        @Bean
        @ConditionalOnYtClientsCount(YtClientsMode.MULTI)
        public TestBean multiTestBean() {
            return new TestBean(MULTI_NAME);
        }
    }

    private static class TestBean {
        private final String name;

        public TestBean() {
            name = "";
        }

        public TestBean(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
