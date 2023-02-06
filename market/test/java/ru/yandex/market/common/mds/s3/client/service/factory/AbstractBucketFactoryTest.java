package ru.yandex.market.common.mds.s3.client.service.factory;

import javax.annotation.Nonnull;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import ru.yandex.market.common.mds.s3.client.exception.MdsS3Exception;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Базовый класс тестов для {@link AbstractBucketFactory} и наследников.
 *
 * @author Vladislav Bauer
 */
public abstract class AbstractBucketFactoryTest<T extends AbstractBucketFactory> {

    static final String BUCKET = "bucket";


    @Test
    public void testGetBucketName() {
        final T factory = createFactory(BUCKET);
        assertThat(factory.getBucketName(), equalTo(BUCKET));
    }

    @Test(expected = MdsS3Exception.class)
    public void testCreateNegative() {
        final T factory = createFactory(StringUtils.EMPTY);
        fail(String.valueOf(factory));
    }


    @Nonnull
    protected abstract T createFactory(@Nonnull final String bucketName);

}
