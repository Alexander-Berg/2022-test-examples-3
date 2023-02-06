package ru.yandex.market.starter.tvm;

import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import ru.yandex.market.application.properties.utils.Environments;
import ru.yandex.market.javaframework.internal.environment.test.EnvironmentExtension;
import ru.yandex.market.javaframework.internal.environment.test.TestEnvironment;
import ru.yandex.market.starter.properties.tvm.TvmProperties;
import ru.yandex.market.starter.tvm.configurers.DefaultLocalTvmIdSupportConfigurer;
import ru.yandex.market.starter.tvm.factory.TvmClientSettings;

@ExtendWith(EnvironmentExtension.class)
public class DefaultLocalTvmIdSupportConfigurerTest {

    @Test
    @TestEnvironment(Environments.TESTING)
    public void testingEnvTest() {
        final TvmProperties tvmProperties = new TvmProperties();
        final DefaultLocalTvmIdSupportConfigurer configurer = new DefaultLocalTvmIdSupportConfigurer(tvmProperties);
        Assertions.assertEquals(Set.of(TvmClientSettings.LOCAL_TVM_ID), configurer.getSources());
    }

    @Test
    @TestEnvironment(Environments.PRODUCTION)
    public void anotherEnvTest() {
        final TvmProperties tvmProperties = new TvmProperties();
        final DefaultLocalTvmIdSupportConfigurer configurer = new DefaultLocalTvmIdSupportConfigurer(tvmProperties);
        Assertions.assertNull(configurer.getSources());
    }

    @Test
    @TestEnvironment(Environments.TESTING)
    public void disabledAccessFromLocalTest() {
        final TvmProperties tvmProperties = new TvmProperties();
        tvmProperties.setAccessForDefaultLocalTvmId(false);
        final DefaultLocalTvmIdSupportConfigurer configurer = new DefaultLocalTvmIdSupportConfigurer(tvmProperties);
        Assertions.assertNull(configurer.getSources());
    }
}
