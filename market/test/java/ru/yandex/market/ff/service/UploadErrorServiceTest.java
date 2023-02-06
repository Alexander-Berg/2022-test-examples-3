package ru.yandex.market.ff.service;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.enums.FileExtension;
import ru.yandex.market.ff.model.entity.UploadError;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Интеграционные тесты для {@link UploadErrorService}.
 *
 * @author avetokhin 15/02/18.
 */

class UploadErrorServiceTest extends IntegrationTest {

    private static final String URL = "http://localhost:6060";

    @Autowired
    private UploadErrorService uploadErrorService;

    @BeforeEach
    void init() throws MalformedURLException {
        when(mdsFfwfS3Client.getUrl(any())).thenReturn(new URL(URL));
        when(mdsS3Client.getUrl(any())).thenReturn(new URL(URL));
    }

    @Test
    @DatabaseSetup("classpath:empty.xml")
    @ExpectedDatabase(value = "classpath:service/upload-error/after-create-with-file.xml", assertionMode = NON_STRICT)
    void createWithFile() {
        final UploadError error = uploadErrorService.create(mock(InputStream.class), FileExtension.XLS,
                RequestType.SUPPLY);
        assertThat(error, notNullValue());
        assertThat(error.getTtl(), equalTo(UploadErrorService.ERROR_FILE_TTL));
        assertThat(error.getCreatedAt(), equalTo(LocalDateTime.of(2018, 1, 1, 10, 10, 10)));
        assertThat(error.getFileUrl(), equalTo(URL));
        assertThat(error.getFileExtension(), equalTo(FileExtension.XLS));

        ResourceLocation resourceLocation = ResourceLocation.create(MBI_TEST_BUCKET, "tmp_1.xls");
        verify(mdsS3Client).upload(eq(resourceLocation), any());
        verify(mdsS3Client).getUrl(resourceLocation);
        verifyNoMoreInteractions(mdsS3Client);
        verifyZeroInteractions(mdsFfwfS3Client);
    }

    @Test
    @DatabaseSetup("classpath:empty.xml")
    @ExpectedDatabase(value = "classpath:service/upload-error/after-create-without-file.xml",
            assertionMode = NON_STRICT)
    void createWithoutFile() {
        final UploadError error = uploadErrorService.create(RequestType.SUPPLY);
        assertThat(error, notNullValue());
        assertThat(error.getTtl(), equalTo(UploadErrorService.ERROR_FILE_TTL));
        assertThat(error.getCreatedAt(), equalTo(LocalDateTime.of(2018, 1, 1, 10, 10, 10)));
        assertThat(error.getFileUrl(), nullValue());
        assertThat(error.getFileExtension(), nullValue());

        verifyZeroInteractions(mdsFfwfS3Client);
        verifyZeroInteractions(mdsS3Client);
    }

    @Test
    @DatabaseSetup("classpath:service/upload-error/before-delete.xml")
    @ExpectedDatabase(value = "classpath:service/upload-error/after-delete-with-file.xml", assertionMode = NON_STRICT)
    void deleteWithFile() {
        uploadErrorService.delete(1L);
        ResourceLocation resourceLocation = ResourceLocation.create(TEST_BUCKET, "tmp_1.xls");

        verify(mdsFfwfS3Client).delete(resourceLocation);
        verifyNoMoreInteractions(mdsFfwfS3Client);
        verifyZeroInteractions(mdsS3Client);
    }

    @Test
    @DatabaseSetup("classpath:service/upload-error/before-delete.xml")
    @ExpectedDatabase(value = "classpath:service/upload-error/after-delete-without-file.xml",
            assertionMode = NON_STRICT)
    void deleteWithoutFile() {
        uploadErrorService.delete(3L);
        verifyZeroInteractions(mdsFfwfS3Client);
        verifyZeroInteractions(mdsS3Client);
    }

}
