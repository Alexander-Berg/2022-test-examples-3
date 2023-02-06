package ru.yandex.market.core.fulfillment.report.excel;

import java.net.URL;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import ru.yandex.market.common.mds.s3.client.content.provider.StreamContentProvider;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.factory.ResourceLocationFactory;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link UploadFFReportsToMdsService}
 */
@ExtendWith(MockitoExtension.class)
public class UploadFFReportsToMdsServiceTest {
    private static final String PATH = "a/b";
    private static final String BUCKET_NAME = "test-bucket";
    private static final ResourceLocationFactory LOCATION_FACTORY = ResourceLocationFactory.create(BUCKET_NAME, PATH);

    private UploadFFReportsToMdsService mdsService;

    @Mock
    private MdsS3Client mdsS3Client;

    @BeforeEach
    void beforeEach() throws Exception {
        URL url = new URL("http", "test.test", "a/b/");
        when(mdsS3Client.getUrl(any())).thenReturn(url);

        mdsService = new UploadFFReportsToMdsService(mdsS3Client, LOCATION_FACTORY);
    }

    @Test
    void test_reportUploaded() {
        String fileName = "fileName";
        ResourceLocation location = LOCATION_FACTORY.createLocation(fileName);

        mdsService.uploadToMds(new byte[10], fileName);

        verify(mdsS3Client).upload(eq(location), any(StreamContentProvider.class));
        verify(mdsS3Client).getUrl(eq(location));
        verifyNoMoreInteractions(mdsS3Client);
    }
}
