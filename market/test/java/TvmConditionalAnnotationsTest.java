package ru.yandex.market.starter.tvm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import ru.yandex.market.application.properties.utils.Environments;
import ru.yandex.market.javaframework.internal.environment.test.EnvironmentExtension;
import ru.yandex.market.javaframework.internal.environment.test.TestEnvironment;
import ru.yandex.market.starter.tvm.annotation.ConditionalOnEnvironment;
import ru.yandex.market.starter.tvm.annotation.ConditionalOnMissingTvmSecret;
import ru.yandex.market.starter.tvm.annotation.ConditionalOnTvmEnabled;
import ru.yandex.market.starter.tvm.config.TvmClientAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(EnvironmentExtension.class)
public class TvmConditionalAnnotationsTest {

    private static final String BEAN_ENV = Environments.PRESTABLE;
    private static final String ALSO_BEAN_ENV = Environments.INTEGRATION_TEST;
    private static final String NOT_BEAN_ENV = Environments.PRODUCTION;

    @Test
    @TestEnvironment(BEAN_ENV)
    public void conditionalOnEnvironment_BeanEnv_Test() {
        new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConditionalOnEnvironmentAutoConfiguration.class))
            .run(context -> assertThat(context).hasSingleBean(DummyBean.class));
    }

    @Test
    @TestEnvironment(ALSO_BEAN_ENV)
    public void conditionalOnEnvironment_AlsoBeanEnv_Test() {
        new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConditionalOnEnvironmentAutoConfiguration.class))
            .run(context -> assertThat(context).hasSingleBean(DummyBean.class));
    }

    @Test
    @TestEnvironment(NOT_BEAN_ENV)
    public void conditionalOnEnvironment_NotBeanEnv_Test() {
        new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConditionalOnEnvironmentAutoConfiguration.class))
            .run(context -> assertThat(context).doesNotHaveBean(DummyBean.class));
    }

    @Test
    public void conditionalOnServerOrClientsTvmEnabledTest() {
        new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConditionalOnServerOrClientsTvmEnabledAutoConfiguration.class))
            .run(context -> assertThat(context).hasSingleBean(DummyBean.class));

        new ApplicationContextRunner()
            .withPropertyValues("mj.tvm.serverTvmDisabled=true")
            .withConfiguration(AutoConfigurations.of(ConditionalOnServerOrClientsTvmEnabledAutoConfiguration.class))
            .run(context -> assertThat(context).hasSingleBean(DummyBean.class));

        new ApplicationContextRunner()
            .withPropertyValues("mj.tvm.clientsTvmDisabled=true")
            .withConfiguration(AutoConfigurations.of(ConditionalOnServerOrClientsTvmEnabledAutoConfiguration.class))
            .run(context -> assertThat(context).hasSingleBean(DummyBean.class));

        new ApplicationContextRunner()
            .withPropertyValues(
                "mj.tvm.serverTvmDisabled=true",
                "mj.tvm.clientsTvmDisabled=true"
            )
            .withConfiguration(AutoConfigurations.of(ConditionalOnServerOrClientsTvmEnabledAutoConfiguration.class))
            .run(context -> assertThat(context).doesNotHaveBean(DummyBean.class));
    }

    @Test
    public void conditionalOnMissingTvmSecretTest() {
        new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConditionalOnMissingTvmSecretAutoConfiguration.class))
            .run(context -> assertThat(context).hasSingleBean(DummyBean.class));

        new ApplicationContextRunner()
            .withPropertyValues(TvmClientAutoConfiguration.TVM_SECRET_PROP_NAME + "=sdfsdfs")
            .withConfiguration(AutoConfigurations.of(ConditionalOnMissingTvmSecretAutoConfiguration.class))
            .run(context -> assertThat(context).doesNotHaveBean(DummyBean.class));

        new ApplicationContextRunner()
            .withPropertyValues(TvmClientAutoConfiguration.FALLBACK_TVM_SECRET_PROP_NAME_1 + "=sdfsdfs")
            .withConfiguration(AutoConfigurations.of(ConditionalOnMissingTvmSecretAutoConfiguration.class))
            .run(context -> assertThat(context).doesNotHaveBean(DummyBean.class));

        new ApplicationContextRunner()
            .withPropertyValues(TvmClientAutoConfiguration.FALLBACK_TVM_SECRET_PROP_NAME_2 + "=sdfsdfs")
            .withConfiguration(AutoConfigurations.of(ConditionalOnMissingTvmSecretAutoConfiguration.class))
            .run(context -> assertThat(context).doesNotHaveBean(DummyBean.class));

        new ApplicationContextRunner()
            .withPropertyValues(
                TvmClientAutoConfiguration.TVM_SECRET_PROP_NAME + "=sdfsdfs",
                TvmClientAutoConfiguration.FALLBACK_TVM_SECRET_PROP_NAME_1 + "=sdfsdfs",
                TvmClientAutoConfiguration.FALLBACK_TVM_SECRET_PROP_NAME_2 + "=sdfsdfs"
            )
            .withConfiguration(AutoConfigurations.of(ConditionalOnMissingTvmSecretAutoConfiguration.class))
            .run(context -> assertThat(context).doesNotHaveBean(DummyBean.class));
    }

    static class DummyBean {

    }

    static class ConditionalOnEnvironmentAutoConfiguration {

        @Bean
        @ConditionalOnEnvironment({BEAN_ENV, ALSO_BEAN_ENV})
        public DummyBean dummyBean() {
            return new DummyBean();
        }
    }

    static class ConditionalOnServerOrClientsTvmEnabledAutoConfiguration {

        @Bean
        @ConditionalOnTvmEnabled
        public DummyBean dummyBean() {
            return new DummyBean();
        }
    }

    static class ConditionalOnMissingTvmSecretAutoConfiguration {

        @Bean
        @ConditionalOnMissingTvmSecret
        public DummyBean dummyBean() {
            return new DummyBean();
        }
    }
}
