package ru.yandex.market.config;

import java.util.Collection;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Test;

import ru.yandex.market.common.mds.s3.client.model.ResourceConfiguration;
import ru.yandex.market.common.mds.s3.client.service.data.ResourceConfigurationProvider;
import ru.yandex.market.common.mds.s3.client.service.factory.ResourceConfigurationFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;

class ShopTmsMdsConfigTest {
    private static final String TEST_BUCKET = "test-bucket";

    @Test
    void testResourceConfigurationProvider() {
        final ResourceConfigurationProvider resourceConfigurationProvider = createResourceConfigurationProvider();
        final Collection<ResourceConfiguration> configurations = resourceConfigurationProvider.getConfigurations();
        assertThat(configurations, not(empty()));
    }

    private ResourceConfigurationProvider createResourceConfigurationProvider() {
        final ShopTmsMdsConfig config = createConfig();
        final ResourceConfigurationFactory factory = ResourceConfigurationFactory.create(TEST_BUCKET);
        return config.resourceConfigurationProvider(factory);
    }

    @Nonnull
    private ShopTmsMdsConfig createConfig() {
        return new ShopTmsMdsConfig();
    }
}
