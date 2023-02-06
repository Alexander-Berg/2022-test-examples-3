package ru.yandex.market.common.mds.s3.client.service.builder;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import ru.yandex.market.common.mds.s3.client.exception.MdsS3Exception;
import ru.yandex.market.common.mds.s3.client.model.ResourceConfiguration;
import ru.yandex.market.common.mds.s3.client.model.ResourceFileDescriptor;
import ru.yandex.market.common.mds.s3.client.model.ResourceHistoryStrategy;
import ru.yandex.market.common.mds.s3.client.model.ResourceLifeTime;
import ru.yandex.market.common.mds.s3.client.service.data.ResourceConfigurationProvider;
import ru.yandex.market.common.mds.s3.client.service.factory.ResourceConfigurationFactory;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Unit-тесты для {@link ResourceConfigurationProviderBuilder}.
 *
 * @author Vladislav Bauer
 */
public class ResourceConfigurationProviderBuilderTest {

    private static final String BUCKET = "BUCKET";
    private static final int CONFIGURATIONS_COUNT = 4;

    private static final String ALL_NAME = "config-with-history-and-last";
    private static final String ALL_EXT = "extension";
    private static final int ALL_TTL = 5;

    private static final String HISTORY_NAME = "config-with-history-only";
    private static final String HISTORY_EXT = "ext";
    private static final int HISTORY_TTL = 7;

    private static final String CURRENT_EXT = null;
    private static final String CURRENT_NAME = "config-without-history";

    private static final String FOREVER_NAME = "forever-young-i-want-to-be-forever";


    @Test
    public void testBuilderWithBucketName() {
        checkBuilder(ResourceConfigurationProviderBuilder.create(BUCKET));
    }

    @Test(expected = MdsS3Exception.class)
    public void testBuilderWithBucketNameNegative() {
        checkBuilder(ResourceConfigurationProviderBuilder.create(StringUtils.EMPTY));
    }

    @Test
    public void testBuilderWithResourceFactory() {
        final ResourceConfigurationFactory factory = ResourceConfigurationFactory.create(BUCKET);
        checkBuilder(new ResourceConfigurationProviderBuilder(factory));
    }


    private void checkBuilder(final ResourceConfigurationProviderBuilder builder) {
        final ResourceConfigurationProvider provider = builder
            .addConfigurationWithHistoryAndLast(ALL_NAME + "." + ALL_EXT, ALL_TTL)
            .addConfigurationWithHistoryOnly(HISTORY_NAME + "." + HISTORY_EXT, HISTORY_TTL)
            .addConfigurationWithoutHistory(CURRENT_NAME)
            .addConfiguration(
                ResourceConfiguration.create(
                    BUCKET, ResourceHistoryStrategy.HISTORY_ONLY,
                    ResourceFileDescriptor.create(FOREVER_NAME),
                    ResourceLifeTime.forever()
                )
            )
            .build();

        assertThat(provider, notNullValue());
        assertThat(provider.getConfigurations(), hasSize(CONFIGURATIONS_COUNT));

        checkConfig(provider, ALL_NAME, ALL_EXT, ResourceLifeTime.create(ALL_TTL));
        checkConfig(provider, HISTORY_NAME, HISTORY_EXT, ResourceLifeTime.create(HISTORY_TTL));
        checkConfig(provider, CURRENT_NAME, CURRENT_EXT, null);
    }

    private void checkConfig(
        final ResourceConfigurationProvider provider,
        final String name, final String ext, final ResourceLifeTime lifeTime
    ) {
        final ResourceConfiguration config = provider.getByName(name);
        final ResourceFileDescriptor fileDescriptor = config.getFileDescriptor();

        final String confExt = fileDescriptor.getExtension().orElse(null);
        final ResourceLifeTime confLifeTime = config.getLifeTime().orElse(null);

        assertThat(fileDescriptor.getName(), equalTo(name));
        assertThat(confExt, equalTo(ext));
        assertThat(confLifeTime, equalTo(lifeTime));
    }

}
