package ru.yandex.market.ff4shops.api.json.openapi.outbound;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.annotation.Nonnull;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.IOUtils;
import ru.yandex.market.common.mds.s3.client.content.provider.StreamContentProvider;
import ru.yandex.market.common.mds.s3.client.exception.MdsS3Exception;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.ff4shops.api.json.openapi.AbstractOpenApiTest;
import ru.yandex.market.ff4shops.client.model.SaveOutboundFileRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.ff4shops.client.ResponseSpecBuilders.shouldBeCode;
import static ru.yandex.market.ff4shops.client.ResponseSpecBuilders.validatedWith;

@DisplayName("Сохранение файла отправки")
@DbUnitDataSet(before = "saveOutboundFile.before.csv")
public class SaveOutboundFileTest extends AbstractOpenApiTest {

    private static final byte[] BYTES = {1, 2, 3, 4};
    private static final ResourceLocation LOCATION = ResourceLocation.create("ff4shops", "1");

    @Autowired
    private MdsS3Client mdsS3Client;

    private final ArgumentCaptor<StreamContentProvider> streamContentProviderCaptor
            = ArgumentCaptor.forClass(StreamContentProvider.class);

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(mdsS3Client);
    }

    @Test
    @DisplayName("Отправка не найдена")
    void outboundNotFound() {
        assertResponseBody(
                saveOutboundFile("200", "another-file-name", 404),
                "ru/yandex/market/ff4shops/api/json/openapi/outbound/saveOutboundFile.notFound.json"
        );
    }

    @Test
    @DisplayName("Ошибка при сохранении файла в MDS")
    void uploadException() {
        doThrow(new MdsS3Exception("Uploading to S3 failed")).when(mdsS3Client).upload(eq(LOCATION), any());

        assertResponseBody(
                saveOutboundFile("100", "another-file-name", 500),
                "ru/yandex/market/ff4shops/api/json/openapi/outbound/saveOutboundFile.uploadException.json"
        );

        verify(mdsS3Client).upload(eq(LOCATION), any());
    }

    @Test
    @DisplayName("Успех")
    @DbUnitDataSet(after = "saveOutboundFile.after.csv")
    void success() throws IOException {
        when(mdsS3Client.getUrl(LOCATION)).thenReturn(new URL("http://file"));
        saveOutboundFile("100", "file-name", 200);

        verify(mdsS3Client).upload(eq(LOCATION), streamContentProviderCaptor.capture());
        InputStream inputStream = streamContentProviderCaptor.getValue().getInputStream();
        Assertions.assertThat(IOUtils.readInputStreamToBytes(inputStream)).isEqualTo(BYTES);

        verify(mdsS3Client).getUrl(LOCATION);
    }

    @Nonnull
    private String saveOutboundFile(String outboundId, String fileName, int expectedHttpCode) {
        SaveOutboundFileRequest request = new SaveOutboundFileRequest();
        request.setFileName(fileName);
        request.setFileContent(BYTES);

        return apiClient.outboundFiles().saveOutboundFile()
                .outboundYandexIdPath(outboundId)
                .body(request)
                .execute(validatedWith(shouldBeCode(expectedHttpCode)))
                .jsonPath()
                .prettify();
    }
}
