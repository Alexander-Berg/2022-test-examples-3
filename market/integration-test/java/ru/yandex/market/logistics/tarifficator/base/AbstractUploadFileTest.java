package ru.yandex.market.logistics.tarifficator.base;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.factory.ResourceLocationFactory;
import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.jobs.producer.ProcessUploadedPriceListProducer;
import ru.yandex.market.logistics.tarifficator.util.TestUtils;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

public abstract class AbstractUploadFileTest extends AbstractContextualTest {
    protected static final String XLSX_MIME_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String MDS_FILE_URL = "http://localhost:8080/price_list_document_1.xlsx";

    @Autowired
    protected MdsS3Client mdsS3Client;
    @Autowired
    protected ProcessUploadedPriceListProducer processUploadedPriceListProducer;
    @Autowired
    private ResourceLocationFactory resourceLocationFactory;

    @BeforeEach
    void init() throws MalformedURLException {
        when(mdsS3Client.getUrl(any())).thenReturn(new URL(MDS_FILE_URL));
        when(resourceLocationFactory.createLocation(anyString())).thenAnswer(invocation ->
            ResourceLocation.create("tarifficator", invocation.getArgument(0, String.class))
        );
    }

    @AfterEach
    void after() {
        verifyNoMoreInteractions(mdsS3Client);
    }

    @Test
    @DisplayName("Успешная загрузка файла прайс-листа-тарифа")
    @DatabaseSetup("/controller/price-list-files/db/before/price-list-upload.xml")
    @ExpectedDatabase(
        value = "/controller/price-list-files/db/after/price-list-upload-success.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/history-events/upload-price-list-history-events.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void uploadPriceList() throws Exception {
        doNothing().when(processUploadedPriceListProducer).produceTasks(eq(1L));
        ResourceLocation resourceLocation = newLocation("price_list_document_1.xlsx");

        MockMultipartFile file = newMockFile(
            XLSX_MIME_TYPE,
            getSystemResourceAsStream("controller/price-list-files/excel/minimal-price-list.xlsx")
        );

        mockMvcPerform(file)
            .andExpect(status().isOk())
            .andExpect(successResult());

        verify(mdsS3Client).upload(eq(resourceLocation), any());
        verify(mdsS3Client).getUrl(resourceLocation);
        verify(processUploadedPriceListProducer).produceTasks(eq(1L));
    }

    @Test
    @DisplayName("Ошибка при получении файла прайс-листа-тарифа")
    @DatabaseSetup("/controller/price-list-files/db/before/price-list-upload.xml")
    void uploadPriceListIOError() throws Exception {
        MockMultipartFile file = spy(newMockFile(
            XLSX_MIME_TYPE,
            getSystemResourceAsStream("controller/price-list-files/excel/minimal-price-list.xlsx")
        ));
        when(file.getInputStream()).thenThrow(IOException.class);

        mockMvcPerform(file)
            .andExpect(status().isBadRequest())
            .andExpect(TestUtils.jsonContent("controller/price-list-files/response/upload_io_error_response.json"));
    }

    @Test
    @DisplayName("Неправильно указан Content-Type")
    @DatabaseSetup("/controller/price-list-files/db/before/price-list-upload.xml")
    void uploadPriceListInvalidExtension() throws Exception {
        MockMultipartFile file = newMockFile(
            "image/jpeg",
            getSystemResourceAsStream("controller/price-list-files/excel/minimal-price-list.xlsx")
        );
        mockMvcPerform(file)
            .andExpect(status().isBadRequest())
            .andExpect(TestUtils.jsonContent(
                "controller/price-list-files/response/upload_wrong_content_type_response.json"
            ));
    }

    @Nonnull
    protected ResourceLocation newLocation(String key) {
        return ResourceLocation.create("tarifficator", key);
    }

    @Nonnull
    protected MockMultipartFile newMockFile(String extension, InputStream fileStream) throws IOException {
        return new MockMultipartFile(
            getRequestParamName(),
            "originalFileName",
            extension,
            fileStream
        );
    }

    @Nonnull
    protected abstract ResultActions mockMvcPerform(MockMultipartFile file) throws Exception;

    @Nonnull
    protected abstract ResultMatcher successResult();

    @Nonnull
    protected abstract String getRequestParamName();
}
