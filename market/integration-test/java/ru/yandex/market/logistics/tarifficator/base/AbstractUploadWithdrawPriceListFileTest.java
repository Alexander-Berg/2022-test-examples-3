package ru.yandex.market.logistics.tarifficator.base;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
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
import ru.yandex.market.logistics.tarifficator.jobs.producer.ProcessUploadedWithdrawPriceListFileProducer;
import ru.yandex.market.logistics.tarifficator.util.TestUtils;
import ru.yandex.market.logistics.tarifficator.util.ValidationUtil;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

public abstract class AbstractUploadWithdrawPriceListFileTest extends AbstractContextualTest {
    private static final String MDS_FILE_URL = "http://localhost:8080/withdraw_price_list_document_1.xlsx";
    private static final String XLSX_MIME_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Autowired
    private MdsS3Client mdsS3Client;
    @Autowired
    private ResourceLocationFactory resourceLocationFactory;
    @Autowired
    private ProcessUploadedWithdrawPriceListFileProducer processUploadedWithdrawPriceListFileProducer;

    @BeforeEach
    void init() throws MalformedURLException {
        when(mdsS3Client.getUrl(any())).thenReturn(new URL(MDS_FILE_URL));
        when(resourceLocationFactory.createLocation(anyString())).thenAnswer(invocation ->
            ResourceLocation.create("tarifficator", invocation.getArgument(0, String.class))
        );
    }

    @Test
    @DisplayName("Успешная загрузка файла прайс-листа заборного тарифа")
    @DatabaseSetup("/controller/withdraw/price-list-files/db/before/withdraw-price-list-file-upload.xml")
    @ExpectedDatabase(
        value = "/controller/withdraw/price-list-files/db/after/withdraw-price-list-file-uploaded-success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void uploadWithdrawPriceList() throws Exception {
        doNothing().when(processUploadedWithdrawPriceListFileProducer).produceTask(eq(1L));
        ResourceLocation resourceLocation = newLocation("withdraw_price_list_document_1.xlsx");

        MockMultipartFile file = newMockFile(
            XLSX_MIME_TYPE,
            getSystemResourceAsStream("controller/price-list-files/excel/minimal-price-list.xlsx")
        );

        mockMvcPerform(file)
            .andExpect(status().isOk())
            .andExpect(successResult());

        verify(mdsS3Client).upload(eq(resourceLocation), any());
        verify(mdsS3Client).getUrl(resourceLocation);
        verify(processUploadedWithdrawPriceListFileProducer).produceTask(eq(1L));
    }

    @Test
    @DisplayName("Ошибка при получении файла прайс-листа заборного тарифа")
    @DatabaseSetup("/controller/withdraw/price-list-files/db/before/withdraw-price-list-file-upload.xml")
    void uploadWithdrawPriceListIOError() throws Exception {
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
    @DatabaseSetup("/controller/withdraw/price-list-files/db/before/withdraw-price-list-file-upload.xml")
    void uploadWithdrawPriceListInvalidExtension() throws Exception {
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

    @Test
    @DisplayName("Попытка загрузить файл для несуществующего заборного тарифа")
    @DatabaseSetup("/controller/price-list-files/db/before/price-list-upload.xml")
    void uploadWithdrawPriceListForTariff() throws Exception {
        MockMultipartFile file = spy(newMockFile(
            XLSX_MIME_TYPE,
            getSystemResourceAsStream("controller/price-list-files/excel/minimal-price-list.xlsx")
        ));
        mockMvcPerform(file)
            .andExpect(status().isNotFound())
            .andExpect(ValidationUtil.errorMessage("Failed to find [WITHDRAW_TARIFF] with ids [[1]]"));
    }

    @Nonnull
    protected abstract ResultActions mockMvcPerform(MockMultipartFile file) throws Exception;

    @Nonnull
    protected abstract ResultMatcher successResult();

    @Nonnull
    protected abstract String getRequestParamName();

    @Nonnull
    private ResourceLocation newLocation(String key) {
        return ResourceLocation.create("tarifficator", key);
    }

    @Nonnull
    private MockMultipartFile newMockFile(String extension, InputStream fileStream) throws IOException {
        return new MockMultipartFile(
            getRequestParamName(),
            "originalFileName",
            extension,
            fileStream
        );
    }
}
