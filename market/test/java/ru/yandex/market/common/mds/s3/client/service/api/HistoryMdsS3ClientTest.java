package ru.yandex.market.common.mds.s3.client.service.api;

import java.net.URI;
import java.net.URL;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

import com.amazonaws.services.s3.AmazonS3;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.market.common.mds.s3.client.content.consumer.TextContentConsumer;
import ru.yandex.market.common.mds.s3.client.content.factory.ContentConsumerFactory;
import ru.yandex.market.common.mds.s3.client.exception.MdsS3Exception;
import ru.yandex.market.common.mds.s3.client.model.FileMeta;
import ru.yandex.market.common.mds.s3.client.model.ResourceConfiguration;
import ru.yandex.market.common.mds.s3.client.model.ResourceFileDescriptor;
import ru.yandex.market.common.mds.s3.client.model.ResourceHistoryStrategy;
import ru.yandex.market.common.mds.s3.client.model.ResourceListing;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.impl.MdsS3ClientImpl;
import ru.yandex.market.common.mds.s3.client.service.api.impl.NamedHistoryMdsS3ClientImpl;
import ru.yandex.market.common.mds.s3.client.service.api.impl.PureHistoryMdsS3ClientImpl;
import ru.yandex.market.common.mds.s3.client.service.data.KeyGenerator;
import ru.yandex.market.common.mds.s3.client.service.data.ResourceConfigurationProvider;
import ru.yandex.market.common.mds.s3.client.service.data.impl.DefaultKeyGenerator;
import ru.yandex.market.common.mds.s3.client.test.RandUtils;
import ru.yandex.market.common.mds.s3.client.test.ResourceConfigurationProviderFactory;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.common.mds.s3.client.service.api.impl.PureHistoryMdsS3ClientImpl.DEFAULT_ZONE_ID;

/**
 * Unit-тесты для {@link HistoryMdsS3Client}.
 *
 * @author Vladislav Bauer
 */
@RunWith(Parameterized.class)
public class HistoryMdsS3ClientTest {

    private static final String BUCKET = "bucket";
    private static final String KEY = "key";
    private static final Date DATE =
        Date.from(LocalDate.of(2020, 7, 9).atStartOfDay(DEFAULT_ZONE_ID).toInstant());
    private static final String FAKE_URL = "http://fake.url";

    private final ResourceConfigurationProvider configurationProvider;
    private final boolean withFolder;

    public HistoryMdsS3ClientTest(final boolean withFolder) {
        this.withFolder = withFolder;
        configurationProvider = ResourceConfigurationProviderFactory.create(BUCKET, this.withFolder);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
            {true}, {false}
        });
    }


    @Test
    public void testUploadPositive() {
        final NamedHistoryMdsS3Client historyMdsS3Client = createHistoryMdsS3Client();
        final Collection<ResourceConfiguration> configurations = configurationProvider.getConfigurations();

        for (final ResourceConfiguration configuration : configurations) {
            final ResourceFileDescriptor fileDescriptor = configuration.getFileDescriptor();
            final String name = fileDescriptor.getName();
            final ResourceLocation location = historyMdsS3Client.upload(name, RandUtils.createText());

            assertThat(location, notNullValue());
            assertThat(location.getBucketName(), equalTo(BUCKET));
            assertThat(location.getKey(), not(isEmptyOrNullString()));
        }
    }

    @Test(expected = MdsS3Exception.class)
    public void testUploadNegative() {
        final NamedHistoryMdsS3Client historyMdsS3Client = createHistoryMdsS3Client();
        final String unknownName = RandUtils.randomText();
        final ResourceLocation location = historyMdsS3Client.upload(unknownName, RandUtils.createText());

        fail(String.valueOf(location));
    }

    @Test
    public void testDownloadLast() {
        final NamedHistoryMdsS3Client historyMdsS3Client = createHistoryMdsS3Client();
        final Collection<ResourceConfiguration> configurations = configurationProvider.getConfigurations();

        for (final ResourceConfiguration configuration : configurations) {
            final ResourceFileDescriptor fileDescriptor = configuration.getFileDescriptor();
            final String name = fileDescriptor.getName();
            final String result = historyMdsS3Client.downloadLast(name, ContentConsumerFactory.text());

            assertThat(result, not(isEmptyOrNullString()));
        }
    }

    @Test
    public void testDeleteOld() {
        final NamedHistoryMdsS3Client historyMdsS3Client = createHistoryMdsS3Client();
        final Collection<ResourceConfiguration> configurations = configurationProvider.getConfigurations();

        for (final ResourceConfiguration configuration : configurations) {
            try {
                final ResourceFileDescriptor fileDescriptor = configuration.getFileDescriptor();
                final String configurationName = fileDescriptor.getName();

                historyMdsS3Client.deleteOld(configurationName);
            } catch (final MdsS3Exception ex) {
                final ResourceHistoryStrategy historyStrategy = configuration.getHistoryStrategy();
                if (historyStrategy.needHistory()) {
                    throw ex;
                }
            }
        }
    }

    @Test
    public void testGetUrl() throws Exception {
        final AmazonS3 amazonS3 = createAmazonS3();
        final MdsS3Client mdsS3Client = new MdsS3ClientImpl(amazonS3);
        final KeyGenerator keyGenerator = new DefaultKeyGenerator();
        final PureHistoryMdsS3Client pureHistoryMdsS3Client = new PureHistoryMdsS3ClientImpl(mdsS3Client, keyGenerator);
        final NamedHistoryMdsS3Client namedHistoryMdsS3Client = new NamedHistoryMdsS3ClientImpl(pureHistoryMdsS3Client, configurationProvider);

        final URL url = pureHistoryMdsS3Client.getUrl(ResourceLocation.create(BUCKET, KEY));
        assertThat(url, notNullValue());
        final URL url1 = namedHistoryMdsS3Client.getUrl(ResourceLocation.create(BUCKET, KEY));
        assertThat(url1, notNullValue());

        assertEquals(url, url1);

        verify(amazonS3, atLeastOnce()).getUrl(eq(BUCKET), eq(KEY));
        verifyNoMoreInteractions(amazonS3);
    }


    private NamedHistoryMdsS3Client createHistoryMdsS3Client() {
        final KeyGenerator keyGenerator = new DefaultKeyGenerator();
        final MdsS3Client mdsS3Client = createMdsS3Client();

        final PureHistoryMdsS3ClientImpl historyMdsS3Client = new PureHistoryMdsS3ClientImpl(mdsS3Client, keyGenerator);
        historyMdsS3Client.setZoneId(DEFAULT_ZONE_ID);

        return new NamedHistoryMdsS3ClientImpl(historyMdsS3Client, configurationProvider);
    }

    private MdsS3Client createMdsS3Client() {
        final MdsS3Client mdsS3Client = mock(MdsS3Client.class);

        when(mdsS3Client.list(any(ResourceLocation.class), any(Boolean.class)))
            .thenReturn(ResourceListing.createListingWithMeta(BUCKET, Collections.singletonList(new FileMeta(KEY, DATE)), Collections.singletonList(KEY)));

        when(mdsS3Client.download(any(ResourceLocation.class), any(TextContentConsumer.class)))
            .thenReturn(RandUtils.randomText());

        return mdsS3Client;
    }

    private AmazonS3 createAmazonS3() throws Exception{
        final AmazonS3 amazonS3 = mock(AmazonS3.class);

        when(amazonS3.getUrl(BUCKET, KEY)).thenReturn(URI.create(FAKE_URL).toURL());

        return amazonS3;
    }

}
