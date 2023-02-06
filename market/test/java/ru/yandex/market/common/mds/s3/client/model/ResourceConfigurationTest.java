package ru.yandex.market.common.mds.s3.client.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.market.common.mds.s3.client.exception.MdsS3Exception;
import ru.yandex.market.common.mds.s3.client.service.data.KeyGenerator;
import ru.yandex.market.common.mds.s3.client.service.data.ResourceConfigurationProvider;
import ru.yandex.market.common.mds.s3.client.test.ResourceConfigurationProviderFactory;
import ru.yandex.market.common.mds.s3.client.test.TestUtils;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static ru.yandex.market.common.mds.s3.client.model.ResourceConfiguration.create;
import static ru.yandex.market.common.mds.s3.client.model.ResourceHistoryStrategy.HISTORY_ONLY;
import static ru.yandex.market.common.mds.s3.client.model.ResourceHistoryStrategy.HISTORY_WITH_LAST;
import static ru.yandex.market.common.mds.s3.client.model.ResourceHistoryStrategy.LAST_ONLY;

/**
 * Unit-тесты для {@link ResourceConfiguration}.
 *
 * @author Vladislav Bauer
 */
@RunWith(Parameterized.class)
public class ResourceConfigurationTest {

    private static final String BUCKET = "bucket";
    private static final String NAME = "name";
    private static final String EXTENSION = "ext";

    private static final int TTL = 5;
    private static final ResourceLifeTime LIFE_TIME = ResourceLifeTime.create(TTL);

    private final ResourceFileDescriptor fileDescriptor;
    private final boolean withFolder;

    public ResourceConfigurationTest(final boolean withFolder) {
        this.withFolder = withFolder;
        fileDescriptor = ResourceFileDescriptor.create(NAME, EXTENSION,
            ResourceConfigurationProviderFactory.folder(this.withFolder));
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            { true }, { false }
        });
    }

    @Test
    public void testBasicMethods() {
        TestUtils.checkEqualsAndHashCodeContract(ResourceConfiguration.class);
    }

    @Test
    public void testCreateConfigurationWithHistoryOnly() {
        checkConfiguration(create(BUCKET, HISTORY_ONLY, fileDescriptor, LIFE_TIME));
    }

    @Test
    public void testCreateConfigurationWithHistoryAndLast() {
        checkConfiguration(create(BUCKET, HISTORY_WITH_LAST, fileDescriptor, LIFE_TIME));
    }

    @Test
    public void testCreateResourceConfigurationWithoutHistory() {
        final ResourceLifeTime lifeTime = null;
        checkConfiguration(create(BUCKET, LAST_ONLY, fileDescriptor, lifeTime));
    }

    @Test(expected = MdsS3Exception.class)
    public void testCreateWithEmptyBucket() {
        fail(String.valueOf(create(StringUtils.EMPTY, LAST_ONLY, fileDescriptor, LIFE_TIME)));
    }

    @Test(expected = MdsS3Exception.class)
    public void testCreateResourceConfigurationUnnecessaryTtl() {
        fail(String.valueOf(create(BUCKET, LAST_ONLY, fileDescriptor, LIFE_TIME)));
    }

    @Test(expected = MdsS3Exception.class)
    public void testCreateResourceConfigurationMissedTtl() {
        fail(String.valueOf(create(BUCKET, HISTORY_ONLY, fileDescriptor, null)));
    }

    @Test
    public void testNeedHistory() {
        final ResourceConfigurationProvider provider =
            ResourceConfigurationProviderFactory.create(BUCKET, withFolder);
        final Collection<ResourceConfiguration> configurations = provider.getConfigurations();

        for (final ResourceConfiguration configuration : configurations) {
            final ResourceHistoryStrategy historyStrategy = configuration.getHistoryStrategy();

            assertThat(historyStrategy.needHistory(), equalTo(ResourceConfiguration.needHistory(configuration)));
        }
    }

    @Test
    public void testToLocation() {
        final ResourceConfigurationProvider provider =
            ResourceConfigurationProviderFactory.create(BUCKET, withFolder);
        final Collection<ResourceConfiguration> configurations = provider.getConfigurations();

        for (final ResourceConfiguration configuration : configurations) {
            final ResourceLocation location = configuration.toLocation();
            final ResourceFileDescriptor fileDescriptor = configuration.getFileDescriptor();

            assertThat(location, notNullValue());
            assertThat(location.getBucketName(), equalTo(BUCKET));
            final String goodKey = withFolder ?
                fileDescriptor.getFolder().orElse(null) + KeyGenerator.DELIMITER_FOLDER + fileDescriptor.getName() :
                fileDescriptor.getName();
            assertThat(location.getKey(), equalTo(goodKey));
        }
    }

    @Test
    public void testFindDuplicatesDetected() {
        final ResourceConfiguration configuration = create(BUCKET, HISTORY_ONLY, fileDescriptor, LIFE_TIME);
        final Collection<ResourceConfiguration> duplicates =
            ResourceConfiguration.findDuplicates(Arrays.asList(configuration, configuration));

        assertThat(duplicates, hasSize(1));
        assertThat(duplicates.iterator().next(), equalTo(configuration));
    }

    @Test
    public void testFindDuplicatesNotDetected() {
        final ResourceConfiguration configuration = create(BUCKET, HISTORY_ONLY, fileDescriptor, LIFE_TIME);
        final Collection<ResourceConfiguration> duplicates =
            ResourceConfiguration.findDuplicates(Collections.singletonList(configuration));

        assertThat(duplicates, empty());
    }


    public static void checkConfiguration(final ResourceConfiguration configuration) {
        final ResourceFileDescriptor fileDescriptor = configuration.getFileDescriptor();

        assertThat(configuration.getBucketName(), equalTo(BUCKET));
        assertThat(fileDescriptor.getName(), equalTo(NAME));
    }

}
