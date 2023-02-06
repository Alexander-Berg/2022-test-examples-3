package ru.yandex.market.common.mds.s3.client.service.factory;

import javax.annotation.Nonnull;

import org.junit.Test;

import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.data.KeyGenerator;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Unit-тесты для {@link ResourceLocationFactory}.
 *
 * @author Vladislav Bauer
 */
public class ResourceLocationFactoryTest extends AbstractBucketFactoryTest<ResourceLocationFactory> {

    private static final String KEY = "key";
    private static final String PREFIX = "prefix";


    @Test
    public void testCreateLocation() {
        final ResourceLocationFactory factory = createFactory(BUCKET);
        final ResourceLocation location = factory.createLocation(KEY);

        assertThat(factory.getPathPrefix().isPresent(), equalTo(false));
        assertThat(location.getBucketName(), equalTo(BUCKET));
        assertThat(location.getKey(), equalTo(KEY));
    }

    @Test
    public void testCreateLocationWithPrefix() {
        final ResourceLocationFactory factory = ResourceLocationFactory.create(BUCKET, PREFIX);
        final ResourceLocation location = factory.createLocation(KEY);

        assertThat(factory.getPathPrefix().orElse(null), equalTo(PREFIX));
        assertThat(location.getBucketName(), equalTo(BUCKET));
        assertThat(location.getKey(), equalTo(PREFIX + KeyGenerator.DELIMITER_FOLDER + KEY));
    }


    /**
     * {@inheritDoc}
     */
    @Nonnull
    @Override
    protected ResourceLocationFactory createFactory(@Nonnull final String bucketName) {
        return ResourceLocationFactory.create(bucketName);
    }

}
