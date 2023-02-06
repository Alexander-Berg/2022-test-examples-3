package ru.yandex.market.common.mds.s3.spring.configuration;

import com.amazonaws.services.s3.AmazonS3;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.common.mds.s3.client.exception.MdsS3Exception;
import ru.yandex.market.common.mds.s3.client.service.api.DirectMdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.mds.s3.client.test.RandUtils;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Unit-тесты для {@link MdsS3BasicConfiguration}.
 *
 * @author Vladislav Bauer
 */
public class MdsS3BasicConfigurationTest {

    @Test
    public void testNotConfiguredParameters() {
        final MdsS3BasicConfiguration configuration = createEmptyConfiguration();

        assertThat(configuration.getAccessKey(), nullValue());
        assertThat(configuration.getSecretKey(), nullValue());
        assertThat(configuration.getEndpoint(), nullValue());
    }

    @Test
    public void testMdsS3Client() {
        final AmazonS3 amazonS3 = Mockito.mock(AmazonS3.class);
        final MdsS3BasicConfiguration configuration = createEmptyConfiguration();
        final MdsS3Client mdsS3Client = configuration.mdsS3Client(amazonS3);

        assertThat(mdsS3Client, notNullValue());
    }

    @Test(expected = MdsS3Exception.class)
    public void testAmazonS3Negative() {
        final MdsS3BasicConfiguration configuration = createEmptyConfiguration();
        final AmazonS3 amazonS3 = configuration.amazonS3();

        fail(String.valueOf(amazonS3));
    }

    @Test
    public void testAmazonS3Positive() {
        final MdsS3BasicConfiguration configuration = createSetupConfiguration();
        final AmazonS3 amazonS3 = configuration.amazonS3();

        assertThat(amazonS3, notNullValue());
    }

    @Test
    public void testDirectMdsS3Client() {
        final MdsS3BasicConfiguration configuration = createSetupConfiguration();
        final DirectMdsS3Client directMdsS3Client = configuration.directMdsS3Client();

        assertThat(directMdsS3Client, notNullValue());
    }


    private MdsS3BasicConfiguration createEmptyConfiguration() {
        return new MdsS3BasicConfiguration();
    }

    private MdsS3BasicConfiguration createSetupConfiguration() {
        return new MdsS3BasicConfiguration() {
            @Override
            protected String getAccessKey() {
                return RandUtils.randomText();
            }

            @Override
            protected String getSecretKey() {
                return RandUtils.randomText();
            }

            @Override
            protected String getEndpoint() {
                return RandUtils.randomText();
            }

            @Override
            protected boolean isFlushBeforeUpload() {
                return false;
            }
        };
    }

}
