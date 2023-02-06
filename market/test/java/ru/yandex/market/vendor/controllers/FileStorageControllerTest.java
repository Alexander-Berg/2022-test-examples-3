package ru.yandex.market.vendor.controllers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectResult;
import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendor.AbstractVendorPartnerFunctionalTest;
import ru.yandex.market.vendor.util.FunctionalTestHelper;
import ru.yandex.vendor.documents.FileStoragePath;
import ru.yandex.vendor.documents.S3Connection;
import ru.yandex.vendor.documents.S3FileStorage;
import ru.yandex.vendor.util.FileUploadService;

import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;
import static ru.yandex.vendor.documents.IFileStorage.UploadResponse;

@DbUnitDataSet(before = "/ru/yandex/market/vendor/controllers/FileStorageControllerTest/before.csv")
class FileStorageControllerTest extends AbstractVendorPartnerFunctionalTest {

    private static final Logger LOG = LoggerFactory.getLogger(FileStorageControllerTest.class);

    private final S3Connection s3Connection;
    private final S3FileStorage s3PublicFileStorage;
    private final FileUploadService fileUploadService;

    @Autowired
    public FileStorageControllerTest(S3Connection s3Connection, S3FileStorage s3PublicFileStorage, FileUploadService fileUploadService) {
        this.s3Connection = s3Connection;
        this.s3PublicFileStorage = s3PublicFileStorage;
        this.fileUploadService = fileUploadService;
    }

    /**
     * Тест проверяет, что при отсутствии имени файла в передаваемом url будет брошено исплючение
     * {@link ru.yandex.vendor.exception.BadParamException}
     */
    @Test
    void testUploadFileUrlWithoutFileName() {
        HttpClientErrorException exception = Assertions.assertThrows(HttpClientErrorException.class, () -> {
            String request = getStringResource("/testUploadFileUrlWithoutFileName/request.json");
            FunctionalTestHelper.post(baseUrl + "/files/model-editor-feed/upload/url?uid=67282295", request);
        });
        String resource = getStringResource("/testUploadFileUrlWithoutFileName/response.json");
        JsonAssert.assertJsonEquals(resource, exception.getResponseBodyAsString());
    }

    /**
     * Тест проверяет загрузку документа
     */
    @Test
    void testUploadDocument() throws IOException {
        final String documentName = "upload_document.png";
        final String documentPath = String.format("/testUploadDocument/%s", documentName);

        final AmazonS3 amazonS3 = s3Connection.getS3();
        PutObjectResult putObjectResult = Mockito.mock(PutObjectResult.class);
        Mockito.when(amazonS3.putObject(Mockito.anyString(), Mockito.anyString(), Mockito.any(File.class)))
                .thenReturn(putObjectResult);
        Mockito.doReturn(getInputStreamResource(documentPath).getInputStream())
                .when(fileUploadService)
                .readURL(Mockito.any());
        Mockito.doReturn(new UploadResponse(FileStoragePath.fromFileName(documentName), 1657L))
                .when(s3PublicFileStorage).uploadFile(Mockito.anyString(), Mockito.any(InputStream.class));

        final Map<String, String> request = Collections.singletonMap(
                "url",
                String.format("http://dummy.com/%s", documentName)
        );
        final String response = FunctionalTestHelper.post(baseUrl + "/files/documents/upload/url?uid=1", request);
        final String expected = getStringResource("/testUploadDocument/response.json");

        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проверяет получение документа
     */
    @Test
    void testGetDocument() {
        String response = FunctionalTestHelper.get(baseUrl + "/files/documents/meta/6e7e9dfe-2c79-4ca6-9ce5-340d0cdf4ed4");
        String expected = getStringResource("/testGetDocument/response.json");
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }
}
