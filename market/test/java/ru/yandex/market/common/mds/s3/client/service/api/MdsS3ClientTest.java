package ru.yandex.market.common.mds.s3.client.service.api;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.market.common.mds.s3.client.content.factory.ContentConsumerFactory;
import ru.yandex.market.common.mds.s3.client.content.factory.ContentProviderFactory;
import ru.yandex.market.common.mds.s3.client.model.ResourceListing;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.impl.MdsS3ClientImpl;
import ru.yandex.market.common.mds.s3.client.test.InOutUtils;

import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.common.mds.s3.client.test.TestUtils.TEST_DATA;

/**
 * Unit-тесты для {@link MdsS3Client}.
 *
 * @author Vladislav Bauer
 */
@RunWith(Parameterized.class)
public class MdsS3ClientTest {

    private static final String TEST_BUCKET = "bucket";
    private static final String TEST_KEY = "key";
    private static final ResourceLocation LOCATION = ResourceLocation.create(TEST_BUCKET, TEST_KEY);
    private static final String FAKE_URL = "http://fake.com";
    private static final int MAX_DELETE_SIZE = 1000;


    private final boolean flushBeforeUpload;

    public MdsS3ClientTest(final boolean flushBeforeUpload) {
        this.flushBeforeUpload = flushBeforeUpload;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { true }, { false }
        });
    }

    @Test
    public void testDownload() {
        final AmazonS3 amazonS3 = createAmazonS3();
        final MdsS3Client mdsS3Client = createMdsS3Client(amazonS3);

        final String result = mdsS3Client.download(LOCATION, ContentConsumerFactory.text());
        assertThat(result, equalTo(TEST_DATA));

        verify(amazonS3, atLeastOnce()).getObject(TEST_BUCKET, TEST_KEY);
        verifyNoMoreInteractions(amazonS3);
    }

    @Test
    public void testUpload() {
        final AmazonS3 amazonS3 = createAmazonS3();
        final MdsS3Client mdsS3Client = createMdsS3Client(amazonS3);

        mdsS3Client.upload(LOCATION, ContentProviderFactory.text(TEST_DATA));

        if (flushBeforeUpload) {
            verify(amazonS3, atLeastOnce()).putObject(eq(TEST_BUCKET), eq(TEST_KEY), any(File.class));
        } else {
            verify(amazonS3, atLeastOnce()).putObject(eq(TEST_BUCKET), eq(TEST_KEY), any(InputStream.class), any());
        }
        verifyNoMoreInteractions(amazonS3);
    }

    @Test
    public void testContains() {
        final AmazonS3 amazonS3 = createAmazonS3();
        final MdsS3Client mdsS3Client = createMdsS3Client(amazonS3);

        assertThat(mdsS3Client.contains(LOCATION), equalTo(true));
        assertThat(mdsS3Client.contains(ResourceLocation.create(TEST_BUCKET, EMPTY)), equalTo(false));

        verify(amazonS3, times(2)).doesObjectExist(anyString(), anyString());
        verifyNoMoreInteractions(amazonS3);
    }

    @Test
    public void testDelete() {
        final AmazonS3 amazonS3 = createAmazonS3();
        final MdsS3Client mdsS3Client = createMdsS3Client(amazonS3);

        mdsS3Client.delete();
        mdsS3Client.delete(LOCATION);

        verify(amazonS3, atLeastOnce()).deleteObjects(any(DeleteObjectsRequest.class));
        verifyNoMoreInteractions(amazonS3);
    }

    @Test
    public void testDeleteHugeNumberOfKeys() {
        final AmazonS3 amazonS3 = createAmazonS3();
        final MdsS3Client mdsS3Client = createMdsS3Client(amazonS3);

        final ResourceLocation[] locations = IntStream.range(0, (int) (1.5 * MAX_DELETE_SIZE))
                .mapToObj(i -> ResourceLocation.create(TEST_BUCKET, TEST_KEY + i))
                .toArray(ResourceLocation[]::new);

        mdsS3Client.delete(locations);

        verify(amazonS3, times(2)).deleteObjects(any(DeleteObjectsRequest.class));
        verifyNoMoreInteractions(amazonS3);
    }

    @Test
    public void testDeleteUsingPrefix() {
        final AmazonS3 amazonS3 = createAmazonS3();
        final MdsS3Client mdsS3Client = createMdsS3Client(amazonS3);

        mdsS3Client.deleteUsingPrefix(LOCATION);

        verify(amazonS3, atLeastOnce()).listObjects(any(ListObjectsRequest.class));
        verify(amazonS3, atLeastOnce()).deleteObjects(any(DeleteObjectsRequest.class));
        verifyNoMoreInteractions(amazonS3);
    }

    @Test
    public void testList() {
        checkList(true);
        checkList(false);
    }

    @Test
    public void testGetUrl() {
        final AmazonS3 amazonS3 = createAmazonS3();
        final MdsS3Client mdsS3Client = createMdsS3Client(amazonS3);

        final URL url = mdsS3Client.getUrl(LOCATION);
        assertThat(url, notNullValue());

        verify(amazonS3, atLeastOnce()).getUrl(eq(TEST_BUCKET), eq(TEST_KEY));
        verifyNoMoreInteractions(amazonS3);
    }


    private void checkList(final boolean recursive) {
        final AmazonS3 amazonS3 = createAmazonS3();
        final MdsS3Client mdsS3Client = createMdsS3Client(amazonS3);

        final ResourceListing listing = mdsS3Client.list(LOCATION, recursive);
        assertThat(listing.getKeys(), hasSize(1));
        assertThat(listing.getPrefixes(), empty());

        verify(amazonS3, atLeastOnce()).listObjects(any(ListObjectsRequest.class));
        verifyNoMoreInteractions(amazonS3);
    }

    private MdsS3Client createMdsS3Client(final AmazonS3 amazonS3) {
        final MdsS3ClientImpl mdsS3Client = new MdsS3ClientImpl(amazonS3);
        mdsS3Client.setFlushBeforeUpload(flushBeforeUpload);
        return mdsS3Client;
    }

    private AmazonS3 createAmazonS3() {
        final AtomicBoolean existed = new AtomicBoolean(true);
        final AmazonS3 amazonS3 = mock(AmazonS3.class);
        when(amazonS3.deleteObjects(any())).thenAnswer(invocation -> {
            existed.set(false);
            return null;
        });

        when(amazonS3.doesObjectExist(TEST_BUCKET, TEST_KEY)).thenAnswer(invocation -> existed.get());

        try {
            when(amazonS3.getUrl(TEST_BUCKET, TEST_KEY)).thenReturn(URI.create(FAKE_URL).toURL());
        } catch (final Exception ex) {
            throw new RuntimeException(ex);
        }

        final S3Object s3Object = createS3Object();
        when(amazonS3.getObject(TEST_BUCKET, TEST_KEY)).thenReturn(s3Object);

        when(amazonS3.listObjects(any(ListObjectsRequest.class))).thenAnswer(
            invocation -> {
                final S3ObjectSummary s3ObjectSummary = new S3ObjectSummary();
                s3ObjectSummary.setBucketName(TEST_BUCKET);
                s3ObjectSummary.setKey(TEST_KEY);

                final ObjectListing s3ObjectListing = new ObjectListing();
                s3ObjectListing.setBucketName(TEST_BUCKET);
                if (existed.get()) {
                    s3ObjectListing.getObjectSummaries().add(s3ObjectSummary);
                }
                return s3ObjectListing;
            }
        );

        return amazonS3;
    }

    private S3Object createS3Object() {
        final S3Object s3Object = mock(S3Object.class);
        final S3ObjectInputStream inputStream = new S3ObjectInputStream(InOutUtils.inputStream(), null);
        when(s3Object.getObjectContent()).thenReturn(inputStream);
        return s3Object;
    }

}
