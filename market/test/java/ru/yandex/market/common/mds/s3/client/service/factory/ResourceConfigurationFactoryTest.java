package ru.yandex.market.common.mds.s3.client.service.factory;

import javax.annotation.Nonnull;

import org.junit.Test;

import ru.yandex.market.common.mds.s3.client.model.ResourceConfiguration;
import ru.yandex.market.common.mds.s3.client.model.ResourceConfigurationTest;
import ru.yandex.market.common.mds.s3.client.model.ResourceFileDescriptor;
import ru.yandex.market.common.mds.s3.client.model.ResourceHistoryStrategy;
import ru.yandex.market.common.mds.s3.client.model.ResourceLifeTime;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.market.common.mds.s3.client.service.data.KeyGenerator.DELIMITER_EXTENSION;

/**
 * Unit-тесты для {@link ResourceConfigurationFactory}.
 *
 * @author Vladislav Bauer
 */
public class ResourceConfigurationFactoryTest extends AbstractBucketFactoryTest<ResourceConfigurationFactory> {

    private static final String NAME = "name";
    private static final String EXTENSION = "ext";
    private static final String FILE_NAME = NAME + DELIMITER_EXTENSION + EXTENSION;
    private static final int TTL = 5;


    @Test
    public void testCreateConfigurationWithHistoryOnly() {
        final ResourceConfigurationFactory factory = createFactory();
        final ResourceConfiguration configuration = factory.createConfigurationWithHistoryOnly(FILE_NAME, TTL);

        ResourceConfigurationTest.checkConfiguration(configuration);
        checkHistoryStrategy(configuration, ResourceHistoryStrategy.HISTORY_ONLY);
    }

    @Test
    public void testCreateConfigurationWithHistoryAndLast() {
        final ResourceConfigurationFactory factory = createFactory();
        final ResourceConfiguration configuration = factory.createConfigurationWithHistoryAndLast(FILE_NAME, TTL);

        ResourceConfigurationTest.checkConfiguration(configuration);
        checkHistoryStrategy(configuration, ResourceHistoryStrategy.HISTORY_WITH_LAST);
    }

    @Test
    public void testCreateResourceConfigurationWithoutHistory() {
        final ResourceConfigurationFactory factory = createFactory();
        final ResourceConfiguration configuration = factory.createConfigurationWithoutHistory(FILE_NAME);

        ResourceConfigurationTest.checkConfiguration(configuration);
        checkHistoryStrategy(configuration, ResourceHistoryStrategy.LAST_ONLY);
    }

    @Test
    public void testCreateConfiguration() {
        final ResourceConfigurationFactory factory = createFactory();
        final ResourceHistoryStrategy historyStrategy = ResourceHistoryStrategy.HISTORY_ONLY;
        final ResourceConfiguration configuration = factory.createConfiguration(
            historyStrategy,
            ResourceFileDescriptor.create(NAME),
            ResourceLifeTime.forever()
        );

        ResourceConfigurationTest.checkConfiguration(configuration);
        checkHistoryStrategy(configuration, historyStrategy);
    }


    /**
     * {@inheritDoc}
     */
    @Nonnull
    @Override
    protected ResourceConfigurationFactory createFactory(@Nonnull final String bucketName) {
        return ResourceConfigurationFactory.create(bucketName);
    }


    private ResourceConfigurationFactory createFactory() {
        return createFactory(BUCKET);
    }

    private void checkHistoryStrategy(
        final ResourceConfiguration configuration, final ResourceHistoryStrategy historyStrategy
    ) {
        assertThat(configuration.getHistoryStrategy(), equalTo(historyStrategy));
    }

}
