package ru.yandex.market.common.mds.s3.client.service.data;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang.math.RandomUtils;
import org.junit.Test;

import ru.yandex.market.common.mds.s3.client.model.ResourceConfiguration;
import ru.yandex.market.common.mds.s3.client.model.ResourceFileDescriptor;
import ru.yandex.market.common.mds.s3.client.model.ResourceHistoryStrategy;
import ru.yandex.market.common.mds.s3.client.model.ResourceLifeTime;
import ru.yandex.market.common.mds.s3.client.service.data.impl.ResourceConfigurationProviderImpl;
import ru.yandex.market.common.mds.s3.client.test.RandUtils;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Unit-тесты для {@link ResourceConfigurationProvider}.
 *
 * @author Vladislav Bauer
 */
public class ResourceConfigurationProviderTest {

    private static final int MAX_CONFIGURATION_GROUPS = 10;
    private static final ResourceConfigurationProvider PROVIDER = createResourceConfigurationProvider();


    @Test
    public void testConstruction() {
        final Collection<ResourceConfiguration> configurations = PROVIDER.getConfigurations();
        final int expectedSize = MAX_CONFIGURATION_GROUPS * ResourceHistoryStrategy.values().length;

        assertThat(configurations, hasSize(expectedSize));
    }

    @Test(expected = Exception.class)
    public void testImmutability() {
        final Collection<ResourceConfiguration> configurations = PROVIDER.getConfigurations();
        final Collection<ResourceConfiguration> newConfigurations = generateResourceConfigurations(0);

        configurations.addAll(newConfigurations);
    }

    @Test
    public void testFindByNamePositive() {
        checkAll(name -> {
            final Optional<ResourceConfiguration> configuration = PROVIDER.findByName(name);
            assertThat(configuration.isPresent(), equalTo(true));
        });
    }

    @Test
    public void testFindByNameNegative() {
        final String unknownName = RandUtils.randomText();
        final Optional<ResourceConfiguration> configuration = PROVIDER.findByName(unknownName);

        assertThat(configuration.isPresent(), equalTo(false));
    }

    @Test
    public void testGetByNamePositive() {
        checkAll(name -> {
            final ResourceConfiguration configuration = PROVIDER.getByName(name);
            assertThat(configuration, notNullValue());
        });
    }

    @Test
    public void testGetByNameNegative() {
        checkAll(name -> {
            final ResourceConfiguration configuration = PROVIDER.getByName(name);
            assertThat(configuration, notNullValue());
        });
    }


    private void checkAll(final Consumer<String> consumer) {
        IntStream.range(0, MAX_CONFIGURATION_GROUPS).forEach(index -> {
            for (final ResourceHistoryStrategy strategy : ResourceHistoryStrategy.values()) {
                final String name = generateName(index, strategy);
                consumer.accept(name);
            }
        });
    }

    private static ResourceConfigurationProvider createResourceConfigurationProvider() {
        final Collection<ResourceConfiguration> configurations = generateResourceConfigurations();
        return new ResourceConfigurationProviderImpl(configurations);
    }

    private static Collection<ResourceConfiguration> generateResourceConfigurations() {
        return IntStream.range(0, MAX_CONFIGURATION_GROUPS)
            .mapToObj(ResourceConfigurationProviderTest::generateResourceConfigurations)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

    private static Collection<ResourceConfiguration> generateResourceConfigurations(final int index) {
        return Arrays.asList(
            generateResourceConfiguration(index, ResourceHistoryStrategy.HISTORY_WITH_LAST, generateLifeTime()),
            generateResourceConfiguration(index, ResourceHistoryStrategy.HISTORY_ONLY, generateLifeTime()),
            generateResourceConfiguration(index, ResourceHistoryStrategy.LAST_ONLY, null)
        );
    }

    private static ResourceConfiguration generateResourceConfiguration(
        final int i, final ResourceHistoryStrategy strategy, final ResourceLifeTime lifeTime
    ) {
        final String name = generateName(i, strategy);
        final ResourceFileDescriptor fileDescriptor = ResourceFileDescriptor.create(name, name);

        return ResourceConfiguration.create(name, strategy, fileDescriptor, lifeTime);
    }

    private static String generateName(final int index, final ResourceHistoryStrategy strategy) {
        return strategy + String.valueOf(index);
    }

    private static ResourceLifeTime generateLifeTime() {
        final int lifeTime = RandomUtils.nextInt(MAX_CONFIGURATION_GROUPS) + 1;
        return ResourceLifeTime.create(lifeTime);
    }

}
