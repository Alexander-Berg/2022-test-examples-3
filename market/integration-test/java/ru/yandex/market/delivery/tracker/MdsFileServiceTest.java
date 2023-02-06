package ru.yandex.market.delivery.tracker;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

import ru.yandex.market.common.mds.s3.client.content.consumer.StreamCopyContentConsumer;
import ru.yandex.market.common.mds.s3.client.content.provider.StreamContentProvider;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.factory.ResourceLocationFactory;
import ru.yandex.market.delivery.tracker.service.mds.MdsFileService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class MdsFileServiceTest extends AbstractContextualTest {

    private static final byte[] TEST_FILE_CONTENT = "Some test".getBytes(StandardCharsets.UTF_8);
    private static final String BUCKET_NAME = "delivery-tracker-doc-test";
    private static final Long TEST_FILE_KEY = 1L;

    @Autowired
    private MdsS3Client mdsS3Client;

    @Autowired
    private MdsFileService mdsFileService;

    @Autowired
    private ResourceLocationFactory resourceLocationFactory;

    @BeforeEach
    void init() throws MalformedURLException {
        String testUrl = "http://localhost:8080/delivery-tracker-doc-testing-1.xml";
        when(mdsS3Client.getUrl(any())).thenReturn(new URL(testUrl));
        when(resourceLocationFactory.createLocation(eq("1"))).thenReturn(
            ResourceLocation.create("delivery-tracker-doc-test", "1")
        );
    }

    @Test
    @DatabaseSetup("/database/states/empty_DB.xml")
    @ExpectedDatabase(
        value = "/database/expected/single_mds_file.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testFileUpload() throws Exception {
        ResourceLocation resourceLocation = ResourceLocation.create(
            BUCKET_NAME,
            TEST_FILE_KEY.toString()
        );
        when(resourceLocationFactory.createLocation(eq(TEST_FILE_KEY.toString()))).thenReturn(resourceLocation);

        assertions.assertThat(mdsFileService.uploadFile(createMultipartFile())).isEqualTo(TEST_FILE_KEY);

        ArgumentCaptor<StreamContentProvider> captor = ArgumentCaptor.forClass(StreamContentProvider.class);
        verify(mdsS3Client).upload(eq(resourceLocation), captor.capture());
        verify(mdsS3Client).getUrl(resourceLocation);
        verifyNoMoreInteractions(mdsS3Client);

        byte[] content = IOUtils.toByteArray(captor.getValue().getInputStream());
        assertions().assertThat(content).isEqualTo(TEST_FILE_CONTENT);
    }

    @Test
    void testUploadInvalidFile() throws Exception {
        MockMultipartFile file = spy(createMultipartFile());
        when(file.getInputStream()).thenThrow(IOException.class);

        assertions().assertThatThrownBy(() -> mdsFileService.uploadFile(file))
            .isEqualToComparingFieldByFieldRecursively(new IllegalArgumentException(
                "Cannot get input stream from file [original-test.txt]",
                new IOException()
        ));
    }

    @Test
    @DatabaseSetup("/database/states/single_mds_file.xml")
    void testDownloadFile() {
        when(mdsS3Client.download(eq(ResourceLocation.create(BUCKET_NAME, TEST_FILE_KEY.toString())), any()))
            .thenAnswer(
                invocation -> {
                    StreamCopyContentConsumer<OutputStream> argument = invocation.getArgument(1);
                    argument.consume(new ByteArrayInputStream(TEST_FILE_CONTENT));
                    return null;
                }
            );

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        mdsFileService.downloadFile(1L, os);
        assertions().assertThat(os.toByteArray()).isEqualTo(TEST_FILE_CONTENT);
    }

    private MockMultipartFile createMultipartFile() {
        return new MockMultipartFile(
            "test.txt",
            "original-test.txt",
            "text/plain",
            TEST_FILE_CONTENT
        );
    }
}
