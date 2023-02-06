package ru.yandex.market.common.mds.s3.spring.configuration;

import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.api.PureHistoryMdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.data.KeyGenerator;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Unit-тесты для {@link MdsS3HistoryConfiguration}.
 *
 * @author Vladislav Bauer
 */
public class MdsS3HistoryConfigurationTest {

    @Test
    public void testKeyGenerator() {
        final MdsS3HistoryConfiguration configuration = createConfiguration();
        final KeyGenerator keyGenerator = configuration.keyGenerator();

        assertThat(keyGenerator, notNullValue());
    }

    @Test
    public void testPureHistoryMdsS3Client() {
        final KeyGenerator keyGenerator = Mockito.mock(KeyGenerator.class);
        final MdsS3Client mdsS3Client = Mockito.mock(MdsS3Client.class);

        final MdsS3HistoryConfiguration configuration = createConfiguration();
        final PureHistoryMdsS3Client historyMdsS3Client =
                configuration.pureHistoryMdsS3Client(mdsS3Client, keyGenerator);

        assertThat(historyMdsS3Client, notNullValue());
    }


    private MdsS3HistoryConfiguration createConfiguration() {
        return new MdsS3HistoryConfiguration();
    }

}
