package ru.yandex.market.common.mds.s3.client.service.api;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.common.mds.s3.client.content.factory.ContentConsumerFactory;
import ru.yandex.market.common.mds.s3.client.exception.MdsS3Exception;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.impl.DirectMdsS3ClientImpl;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit-тесты для {@link DirectMdsS3Client}.
 *
 * @author Mikhail Khorkov (atroxaper@yandex-team.ru)
 */
@RunWith(MockitoJUnitRunner.class)
public class DirectMdsS3ClientTest {

    private static final String ENDPOINT = "endpoint";
    private static final String DATA = "data";
    private static final String BUCKET = "bucket";
    private static final String KEY = "key";
    private static final ResourceLocation LOCATION = ResourceLocation.create(BUCKET, KEY);


    @Mock
    private CloseableHttpClient httpClient;

    private DirectMdsS3Client directMdsS3Client;


    @Before
    public void onBefore() {
        directMdsS3Client = new DirectMdsS3ClientImpl(httpClient, ENDPOINT);
    }

    @Test
    public void downloadPositive() throws Exception {
        final CloseableHttpResponse response = response(HttpStatus.SC_OK, DATA);
        when(httpClient.execute(any())).thenReturn(response);

        final String downloaded = directMdsS3Client.download(LOCATION, ContentConsumerFactory.text());
        assertThat(downloaded, equalTo(DATA));
    }

    @Test(expected = MdsS3Exception.class)
    public void downloadNegative() throws Exception {
        final CloseableHttpResponse response = response(HttpStatus.SC_NOT_FOUND, DATA);
        when(httpClient.execute(any())).thenReturn(response);

        fail(String.valueOf(directMdsS3Client.download(LOCATION, ContentConsumerFactory.text())));
    }


    private CloseableHttpResponse response(final int code, final String content) throws Exception {
        final StatusLine status = Mockito.mock(StatusLine.class);
        when(status.getStatusCode()).thenReturn(code);

        final HttpEntity entity = Mockito.mock(HttpEntity.class);
        final byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        when(entity.getContent()).thenReturn(new ByteArrayInputStream(bytes));

        final CloseableHttpResponse response = Mockito.mock(CloseableHttpResponse.class);
        when(response.getStatusLine()).thenReturn(status);
        when(response.getEntity()).thenReturn(entity);

        return response;
    }

}
