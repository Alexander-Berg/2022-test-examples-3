package ru.yandex.market.core.mds;

import java.util.Collection;

import javax.annotation.Nonnull;

import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.market.common.mds.s3.client.model.ResourceConfiguration;
import ru.yandex.market.common.mds.s3.client.service.data.ResourceConfigurationProvider;
import ru.yandex.market.common.mds.s3.client.service.factory.ResourceConfigurationFactory;
import ru.yandex.market.common.mds.s3.spring.configuration.MdsS3ResourceConfiguration;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

/**
 * Базовый класс для тестирования Spring-конфигурация на основе {@link MdsS3ResourceConfiguration}.
 *
 * @author Vladislav Bauer
 */
@Ignore
public abstract class AbstractMdsS3ConfigTest<T extends MdsS3ResourceConfiguration> {

    private static final String TEST_BUCKET = "test-bucket";


    @Test
    public void testResourceConfigurationProvider() {
        final ResourceConfigurationProvider resourceConfigurationProvider = createResourceConfigurationProvider();
        final Collection<ResourceConfiguration> configurations = resourceConfigurationProvider.getConfigurations();

        assertThat(configurations, not(empty()));
    }


    @Nonnull
    protected abstract T createConfig();


    private ResourceConfigurationProvider createResourceConfigurationProvider() {
        final T config = createConfig();
        final ResourceConfigurationFactory factory = ResourceConfigurationFactory.create(TEST_BUCKET);

        return config.resourceConfigurationProvider(factory);
    }

}
