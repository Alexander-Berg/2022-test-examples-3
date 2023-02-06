package ru.yandex.market.common.mds.s3.spring.configuration;

import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.common.mds.s3.client.exception.MdsS3Exception;
import ru.yandex.market.common.mds.s3.client.service.api.HistoryMdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.api.PureHistoryMdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.data.ResourceConfigurationProvider;
import ru.yandex.market.common.mds.s3.client.service.factory.ResourceConfigurationFactory;
import ru.yandex.market.common.mds.s3.spring.SpringTestConstants;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Unit-тесты для {@link MdsS3ResourceConfiguration}.
 *
 * @author Vladislav Bauer
 */
public class MdsS3ResourceConfigurationTest {

    @Test
    public void testGetResourceFactoryPositive() {
        final String bucketName = SpringTestConstants.BUCKET_NAME;
        final MdsS3ResourceConfiguration configuration = createConfiguration(bucketName);
        final ResourceConfigurationFactory factory = configuration.resourceConfigurationFactory();

        assertThat(factory, not(nullValue()));
        assertThat(factory.getBucketName(), equalTo(bucketName));
    }

    @Test(expected = MdsS3Exception.class)
    public void testGetResourceFactoryNegative() {
        final MdsS3ResourceConfiguration configuration = createConfiguration();
        final ResourceConfigurationFactory factory = configuration.resourceConfigurationFactory();

        fail(String.valueOf(factory));
    }

    @Test
    public void testNamedHistoryMdsS3Client() {
        final PureHistoryMdsS3Client pureHistoryMdsS3Client = Mockito.mock(PureHistoryMdsS3Client.class);
        final ResourceConfigurationProvider resourceConfigurationProvider =
                Mockito.mock(ResourceConfigurationProvider.class);

        final MdsS3ResourceConfiguration configuration = createConfiguration();
        final HistoryMdsS3Client<String> historyMdsS3Client =
                configuration.namedHistoryMdsS3Client(pureHistoryMdsS3Client, resourceConfigurationProvider);

        assertThat(historyMdsS3Client, notNullValue());
    }


    private MdsS3ResourceConfiguration createConfiguration(final String bucketName) {
        return new MdsS3ResourceConfiguration() {
            @Override
            protected String getDefaultBucketName() {
                return bucketName;
            }
        };
    }

    private MdsS3ResourceConfiguration createConfiguration() {
        return new MdsS3ResourceConfiguration();
    }

}
