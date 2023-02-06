package ru.yandex.market.ff.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;

import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.enums.DocumentType;
import ru.yandex.market.ff.service.exception.InconsistentRequestChangeException;
import ru.yandex.market.ff.service.exception.InvalidDocumentFileException;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Интеграционный тест для {@link RequestDocumentService}.
 *
 * @author avetokhin 23/01/18.
 */
class RequestDocumentServiceTest extends IntegrationTest {
    private static final String FILE_URL = "http://localhost:8080/file";

    @Autowired
    private RequestDocumentService documentService;

    @BeforeEach
    void init() throws MalformedURLException {
        doNothing().when(mdsS3Client).upload(any(), any());
        when(mdsS3Client.getUrl(any())).thenReturn(new URL(FILE_URL));
    }

    @Test
    @DatabaseSetup("classpath:service/request-doc/before-create.xml")
    @ExpectedDatabase(value = "classpath:service/request-doc/after-create.xml", assertionMode = NON_STRICT)
    void createSuccessfully() throws IOException {
        final MultipartFile file = file();
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[]{}));
        documentService.create(1, file, DocumentType.WITHDRAW);
        final ResourceLocation resourceLocation =
                ResourceLocation.create(MBI_TEST_BUCKET, "request_1_document_1_timestamp_1514801410.xls");
        verify(mdsS3Client).upload(eq(resourceLocation), any());
        verify(mdsS3Client).getUrl(resourceLocation);
        verifyNoMoreInteractions(mdsS3Client);
    }

    @Test
    @DatabaseSetup("classpath:service/request-doc/before-create.xml")
    void createIOError() {
        Assertions.assertThrows(InvalidDocumentFileException.class, () -> {
            final MultipartFile file = file();
            when(file.getInputStream()).thenThrow(new IOException());
            documentService.create(1, file, DocumentType.WITHDRAW);
        });
    }

    @Test
    @DatabaseSetup("classpath:service/request-doc/after-create.xml")
    @ExpectedDatabase(value = "classpath:service/request-doc/after-create.xml", assertionMode = NON_STRICT)
    void createMainSecondTime() {
        Assertions.assertThrows(InconsistentRequestChangeException.class, () -> {
            final MultipartFile file = file();
            when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[]{}));
            documentService.create(1, file, DocumentType.WITHDRAW);
        });
    }

    @Test
    @DatabaseSetup("classpath:service/request-doc/docs.xml")
    @ExpectedDatabase(value = "classpath:service/request-doc/after-delete.xml", assertionMode = NON_STRICT)
    void deleteSuccessfully() {
        documentService.delete(3L);
        verify(mdsFfwfS3Client).delete(
                ResourceLocation.create(TEST_BUCKET, "FILE_URL")
        );
        verifyNoMoreInteractions(mdsFfwfS3Client);
    }

    @Test
    @DatabaseSetup("classpath:service/request-doc/docs-mbi.xml")
    @ExpectedDatabase(value = "classpath:service/request-doc/after-delete-from-mbi.xml", assertionMode = NON_STRICT)
    void deleteFromMbiBucketSuccessfully() {
        documentService.delete(4L);
        verify(mdsS3Client).delete(
                ResourceLocation.create(MBI_TEST_BUCKET, "request_1_document_4_timestamp_1514801410.csv")
        );
        verifyNoMoreInteractions(mdsS3Client);
    }

    @Test
    @DatabaseSetup("classpath:service/request-doc/docs.xml")
    @ExpectedDatabase(value = "classpath:service/request-doc/docs.xml", assertionMode = NON_STRICT)
    void deleteMain() {
        Assertions.assertThrows(InconsistentRequestChangeException.class, () -> documentService.delete(1L));
    }

    private static MultipartFile file() {
        final MultipartFile file = mock(MultipartFile.class);
        when(file.getContentType()).thenReturn("application/vnd.ms-excel");
        return file;
    }
}
