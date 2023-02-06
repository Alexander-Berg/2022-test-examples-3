package ru.yandex.market.partner.mvc.controller.application;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.stream.Stream;

import com.amazonaws.services.s3.AmazonS3;
import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.api.cpa.yam.entity.PartnerApplicationDocumentType;
import ru.yandex.market.common.mds.s3.client.content.ContentProvider;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.factory.ResourceLocationFactory;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DbUnitDataSet(
        before = "data/PartnerApplicationDocumentsControllerFunctionalTest.before.csv"
)
public class PartnerApplicationDocumentsControllerFunctionalTest extends FunctionalTest {
    @Autowired
    private ResourceLocationFactory resourceLocationFactory;

    private static final long TEST_REQUEST_ID = 1101L;

    private static final String REQUEST_NOT_FOUND_MESSAGE =
            "[{\n" +
                    "  \"code\": \"REQUEST_NOT_FOUND\",\n" +
                    "  \"details\":{}" +
                    "}]";

    @Autowired
    private MdsS3Client mdsS3Client;

    private File tempDir;

    private static Stream<Arguments> testUploadDocumentParameters() {
        return Stream.of(
                Arguments.of(
                        "upload_pdf.pdf",
                        "test pdf\n",
                        "{\n" +
                                "  \"id\": 1,\n" +
                                "  \"requestId\": 1101,\n" +
                                "  \"type\": \"0\",\n" +
                                "  \"name\": \"upload_pdf.pdf\",\n" +
                                "  \"size\": 9\n" +
                                "}"
                ),
                Arguments.of(
                        "upload_jpg.jpg",
                        "test jpg\n",
                        "{\n" +
                                "  \"id\": 1,\n" +
                                "  \"requestId\": 1101,\n" +
                                "  \"type\": \"0\",\n" +
                                "  \"name\": \"upload_jpg.jpg\",\n" +
                                "  \"size\": 9\n" +
                                "}"
                ),
                Arguments.of(
                        "upload_png.png",
                        "test png\n",
                        "{\n" +
                                "  \"id\": 1,\n" +
                                "  \"requestId\": 1101,\n" +
                                "  \"type\": \"0\",\n" +
                                "  \"name\": \"upload_png.png\",\n" +
                                "  \"size\": 9\n" +
                                "}"
                ),
                Arguments.of(
                        "upload_jpeg.jpeg",
                        "test jpeg\n",
                        "{\n" +
                                "  \"id\": 1,\n" +
                                "  \"requestId\": 1101,\n" +
                                "  \"type\": \"0\",\n" +
                                "  \"name\": \"upload_jpeg.jpeg\",\n" +
                                "  \"size\": 10\n" +
                                "}"
                )
        );
    }

    @BeforeEach
    public void setUp() {
        tempDir = Files.createTempDir();
    }

    @AfterEach
    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(tempDir);

        Mockito.reset(mdsS3Client);
    }

    @ParameterizedTest(name = "Test upload of {1}")
    @MethodSource("testUploadDocumentParameters")
    @DbUnitDataSet
    public void testUploadDocument(String documentName, String documentContent, String expectedResponse) throws IOException {
        String key = "partner-application-documents/101/" + TEST_REQUEST_ID + "/partner-application-doc-1";
        String bucket = "bucket";
        when(resourceLocationFactory.createLocation(anyString()))
                .thenReturn(ResourceLocation.create(bucket, key));
        File document = new File(tempDir, documentName);
        FileUtils.write(document, documentContent, StandardCharsets.UTF_8);
        doReturn(new URL("http://mds.yandex.net/test-doc")).when(mdsS3Client).getUrl(any());

        HttpEntity entity = FunctionalTestHelper.createMultipartHttpEntity(
                "document",
                new FileSystemResource(document),
                (params) -> params.add("type", String.valueOf(PartnerApplicationDocumentType.OTHER.getId()))
        );
        ResponseEntity<String> responseEntity = FunctionalTestHelper.post(getUploadDocumentUrl(TEST_REQUEST_ID), entity);

        JsonTestUtil.assertEquals(responseEntity, expectedResponse);

        ArgumentCaptor<ResourceLocation> locationCaptor = ArgumentCaptor.forClass(ResourceLocation.class);
        ArgumentCaptor<ContentProvider> contentCaptor = ArgumentCaptor.forClass(ContentProvider.class);
        verify(mdsS3Client).upload(locationCaptor.capture(), contentCaptor.capture());

        assertThat(
                locationCaptor.getValue().getKey(),
                equalTo(key)
        );
    }

    @Test
    @DbUnitDataSet
    public void testUploadDocumentInUnsupportedFormat() throws IOException {
        File document = File.createTempFile("document", ".txt", tempDir);

        HttpEntity entity = FunctionalTestHelper.createMultipartHttpEntity(
                "document",
                new FileSystemResource(document),
                (params) -> {
                    params.add("type", String.valueOf(PartnerApplicationDocumentType.OTHER.getId()));
                }
        );

        try {
            FunctionalTestHelper.post(getUploadDocumentUrl(TEST_REQUEST_ID), entity);
            fail("4xx error code expected");
        } catch (HttpClientErrorException e) {
            JsonTestUtil.assertResponseErrorMessage(
                    "[{\n" +
                            "  \"code\": \"UNSUPPORTED_FILE_EXTENSION\",\n" +
                            "  \"details\": {\n" +
                            "    \"SUPPORTED_FILE_EXTENSIONS\": [\n" +
                            "      \"jpeg\",\n" +
                            "      \"jpg\",\n" +
                            "      \"pdf\",\n" +
                            "      \"png\"\n" +
                            "    ]\n" +
                            "  }\n" +
                            "}]",
                    e.getResponseBodyAsString()
            );
        }

        verifyNoMoreInteractions(mdsS3Client);
    }

    @Test
    @DbUnitDataSet
    public void testUploadTooLargeDocument() throws IOException {
        File tooLargeFile = createMoreThan1MbInSizeFile("document.pdf");

        HttpEntity entity = FunctionalTestHelper.createMultipartHttpEntity(
                "document",
                new FileSystemResource(tooLargeFile),
                (params) -> {
                    params.add("type", String.valueOf(PartnerApplicationDocumentType.OTHER.getId()));
                }
        );

        try {
            FunctionalTestHelper.post(getUploadDocumentUrl(TEST_REQUEST_ID), entity);
            fail("4xx error code expected");
        } catch (HttpClientErrorException e) {
            JsonTestUtil.assertResponseErrorMessage(
                    "[{\n" +
                            "  \"code\": \"FILE_TOO_LARGE\",\n" +
                            "  \"details\": {\n" +
                            "    \"MAX_ALLOWED_FILE_SIZE_MB\": 1\n" +
                            "  }\n" +
                            "}]",
                    e.getResponseBodyAsString()
            );
        }

        verifyNoMoreInteractions(mdsS3Client);
    }

    @Test
    @DbUnitDataSet(
            before = "data/testUploadDocumentToNotOwnedRequest.before.csv"
    )
    public void testUploadDocumentToNotOwnedRequest() throws IOException {
        File document = File.createTempFile("document", ".pdf", tempDir);

        HttpEntity entity = FunctionalTestHelper.createMultipartHttpEntity(
                "document",
                new FileSystemResource(document),
                (params) -> {
                    params.add("type", String.valueOf(PartnerApplicationDocumentType.OTHER.getId()));
                }
        );

        try {
            FunctionalTestHelper.post(getUploadDocumentUrl(2102L), entity);
            fail("4xx error code expected");
        } catch (HttpClientErrorException e) {
            JsonTestUtil.assertResponseErrorMessage(
                    REQUEST_NOT_FOUND_MESSAGE,
                    e.getResponseBodyAsString()
            );
        }

        verifyNoMoreInteractions(mdsS3Client);
    }

    @Test
    @DbUnitDataSet(
            before = "data/getValidDocumentUrl.before.csv"
    )
    public void getValidDocumentUrl() throws MalformedURLException {
        String key = "partner-application-documents/101/" + TEST_REQUEST_ID + "/partner-application-doc-3001";
        String bucket = "bucket";
        when(resourceLocationFactory.createLocation(anyString()))
                .thenReturn(ResourceLocation.create(bucket, key));
        String expectedUrl = "http://mds.yandex.net/test-doc";
        ArgumentCaptor<ResourceLocation> resourceLocationCaptor = ArgumentCaptor.forClass(ResourceLocation.class);
        doReturn(new URL(expectedUrl)).when(mdsS3Client).getUrl(resourceLocationCaptor.capture());

        ResponseEntity<String> responseEntity = FunctionalTestHelper.get(getDocumentUrlUrl(TEST_REQUEST_ID, 3001L));

        JsonTestUtil.assertEquals(responseEntity, getClass(), "data/get-document-url-response.json");

        assertThat(
                resourceLocationCaptor.getValue().getKey(),
                equalTo("partner-application-documents/101/" + TEST_REQUEST_ID + "/partner-application-doc-3001")
        );
    }

    @Test
    @DbUnitDataSet(
            before = "data/getDocumentUrlForNotOwnedRequest.before.csv"
    )
    public void getDocumentUrlForNotOwnedRequest() throws MalformedURLException {
        try {
            FunctionalTestHelper.get(getDocumentUrlUrl(2102L, 3001L));
        } catch (HttpClientErrorException e) {
            JsonTestUtil.assertResponseErrorMessage(
                    REQUEST_NOT_FOUND_MESSAGE,
                    e.getResponseBodyAsString()
            );
        }

        verifyNoMoreInteractions(mdsS3Client);
    }

    private File createMoreThan1MbInSizeFile(String fileName) throws IOException {
        File result = new File(tempDir, fileName);
        try (FileOutputStream out = new FileOutputStream(result)) {
            byte[] bytes = new byte[1024 * 1024 + 1];
            new SecureRandom().nextBytes(bytes);
            out.write(bytes);
        }
        return result;
    }

    private String getUploadDocumentUrl(long requestId) {
        return baseUrl + "/partner/application/" + requestId + "/documents?id=11001&_user_id=123";
    }

    private String getDocumentUrlUrl(long requestId, long documentId) {
        return baseUrl + "/partner/application/" + requestId + "/documents/" + documentId + "/url" + "?id=11001&_user_id=123";
    }

}
