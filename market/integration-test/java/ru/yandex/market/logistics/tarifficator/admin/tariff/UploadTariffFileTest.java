package ru.yandex.market.logistics.tarifficator.admin.tariff;

import java.io.IOException;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.logistics.tarifficator.base.AbstractUploadFileTest;
import ru.yandex.market.logistics.tarifficator.model.enums.PlatformClient;
import ru.yandex.market.logistics.tarifficator.util.TestUtils;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static ru.yandex.market.logistics.tarifficator.util.ValidationUtil.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Загрузка файла для тарифа через админку")
class UploadTariffFileTest extends AbstractUploadFileTest {

    @Nonnull
    @Override
    protected ResultActions mockMvcPerform(MockMultipartFile file) throws Exception {
        return mockMvc.perform(
            multipart("/admin/tariffs/upload/1")
                .file(file)
        );
    }

    @Nonnull
    @Override
    protected ResultMatcher successResult() {
        return TestUtils.noContent();
    }

    @Nonnull
    @Override
    protected String getRequestParamName() {
        return "request";
    }

    @Test
    @DisplayName("Успешная загрузка файла прайс-листа тарифа, не принадлежащего платформе")
    @DatabaseSetup("/controller/price-list-files/db/before/price-list-upload.xml")
    @ExpectedDatabase(
        value = "/controller/price-list-files/db/after/price-list-upload-success.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/history-events/upload-price-list-history-events.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void uploadPriceListWithoutPlatform() throws Exception {
        doNothing().when(processUploadedPriceListProducer).produceTasks(eq(1L));
        ResourceLocation resourceLocation = newLocation("price_list_document_1.xlsx");

        uploadTariffFile(defaultMockFile(), null)
            .andExpect(status().isOk())
            .andExpect(successResult());

        verify(mdsS3Client).upload(eq(resourceLocation), any());
        verify(mdsS3Client).getUrl(resourceLocation);
        verify(processUploadedPriceListProducer).produceTasks(eq(1L));
    }

    @Test
    @DisplayName("Успешная загрузка файла прайс-листа тарифа платформы")
    @DatabaseSetup("/controller/price-list-files/db/before/price-list-upload.xml")
    @DatabaseSetup("/controller/price-list-files/db/before/tags.xml")
    @ExpectedDatabase(
        value = "/controller/price-list-files/db/after/price-list-upload-success.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/history-events/upload-price-list-history-events.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void uploadPriceListWithPlatform() throws Exception {
        doNothing().when(processUploadedPriceListProducer).produceTasks(eq(1L));
        ResourceLocation resourceLocation = newLocation("price_list_document_1.xlsx");

        uploadTariffFile(defaultMockFile(), PlatformClient.YANDEX_DELIVERY)
            .andExpect(status().isOk())
            .andExpect(successResult());

        verify(mdsS3Client).upload(eq(resourceLocation), any());
        verify(mdsS3Client).getUrl(resourceLocation);
        verify(processUploadedPriceListProducer).produceTasks(eq(1L));
    }

    @Test
    @DisplayName("Отказано в доступе при попытке загрузки файла прайс-листа тарифа, не принадлежащего платформе")
    @DatabaseSetup("/controller/price-list-files/db/before/price-list-upload.xml")
    @ExpectedDatabase(
        value = "/controller/price-list-files/db/before/price-list-upload.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void uploadPriceListWithoutPlatformForbiddenError() throws Exception {
        uploadTariffFile(defaultMockFile(), PlatformClient.YANDEX_DELIVERY)
            .andExpect(status().isForbidden())
            .andExpect(errorMessage("Unable to access [TARIFF] with ids [1]"));
    }

    @Test
    @DisplayName("Отказано в доступе при попытке загрузки файла прайс-листа тарифа платформы")
    @DatabaseSetup("/controller/price-list-files/db/before/price-list-upload.xml")
    @DatabaseSetup("/controller/price-list-files/db/before/tags.xml")
    @ExpectedDatabase(
        value = "/controller/price-list-files/db/before/price-list-upload.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void uploadPriceListWithPlatformForbiddenError() throws Exception {
        uploadTariffFile(defaultMockFile(), null)
            .andExpect(status().isForbidden())
            .andExpect(errorMessage("Unable to access [TARIFF] with ids [1]"));
    }

    @Nonnull
    private ResultActions uploadTariffFile(MockMultipartFile file, @Nullable PlatformClient platform) throws Exception {
        return mockMvc.perform(
            multipart("/admin/platform/tariffs/upload/1", platform)
                .file(file)
                .param("platformClients", Optional.ofNullable(platform).map(Enum::toString).orElse(""))
        );
    }

    @Nonnull
    private MockMultipartFile defaultMockFile() throws IOException {
        return newMockFile(
            XLSX_MIME_TYPE,
            getSystemResourceAsStream("controller/price-list-files/excel/minimal-price-list.xlsx")
        );
    }
}
