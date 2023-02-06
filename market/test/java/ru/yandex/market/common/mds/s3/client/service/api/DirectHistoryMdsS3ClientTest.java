package ru.yandex.market.common.mds.s3.client.service.api;

import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.common.mds.s3.client.content.consumer.TextContentConsumer;
import ru.yandex.market.common.mds.s3.client.content.factory.ContentConsumerFactory;
import ru.yandex.market.common.mds.s3.client.exception.MdsS3Exception;
import ru.yandex.market.common.mds.s3.client.model.ResourceFileDescriptor;
import ru.yandex.market.common.mds.s3.client.service.api.impl.DirectHistoryMdsS3ClientImpl;
import ru.yandex.market.common.mds.s3.client.service.data.KeyGenerator;
import ru.yandex.market.common.mds.s3.client.service.data.impl.DefaultKeyGenerator;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit-тесты для {@link DirectHistoryMdsS3Client}.
 *
 * @author Vladislav Bauer
 */
public class DirectHistoryMdsS3ClientTest {

    private static final String BUCKET = "test-bucket";
    private static final String TEST_DATA = "test data";
    private static final String FILE_NAME = "test.txt";


    @Test
    public void testDownloadLastPositive() {
        final DirectMdsS3Client directMdsS3Client = Mockito.mock(DirectMdsS3Client.class);
        when(directMdsS3Client.download(any(), any())).thenReturn(TEST_DATA);

        final DirectHistoryMdsS3Client directHistoryMdsS3Client = createDirectHistoryMdsS3Client(directMdsS3Client);
        final String data = downloadFile(directHistoryMdsS3Client);
        assertThat(data, equalTo(data));
    }

    @Test(expected = MdsS3Exception.class)
    public void testDownloadLastNegative() {
        final DirectMdsS3Client directMdsS3Client = Mockito.mock(DirectMdsS3Client.class);
        when(directMdsS3Client.download(any(), any())).thenThrow(MdsS3Exception.class);

        final DirectHistoryMdsS3Client directHistoryMdsS3Client = createDirectHistoryMdsS3Client(directMdsS3Client);
        final String data = downloadFile(directHistoryMdsS3Client);
        fail(data);
    }


    private DirectHistoryMdsS3Client createDirectHistoryMdsS3Client(final DirectMdsS3Client directMdsS3Client) {
        final KeyGenerator keyGenerator = new DefaultKeyGenerator();
        return new DirectHistoryMdsS3ClientImpl(directMdsS3Client, keyGenerator, BUCKET);
    }

    private String downloadFile(final DirectHistoryMdsS3Client directHistoryMdsS3Client) {
        final TextContentConsumer contentConsumer = ContentConsumerFactory.text();
        final ResourceFileDescriptor fileDescriptor = ResourceFileDescriptor.parse(FILE_NAME);
        return directHistoryMdsS3Client.downloadLast(fileDescriptor, contentConsumer);
    }

}
