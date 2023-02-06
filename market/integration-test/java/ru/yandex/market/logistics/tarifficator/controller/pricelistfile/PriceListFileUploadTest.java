package ru.yandex.market.logistics.tarifficator.controller.pricelistfile;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.logistics.tarifficator.base.AbstractUploadFileTest;
import ru.yandex.market.logistics.tarifficator.util.TestUtils;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

class PriceListFileUploadTest extends AbstractUploadFileTest {

    @Test
    @DisplayName("Успешная загрузка файла прайс-листа-тарифа с ограничением на количество направлений")
    @DatabaseSetup("/controller/price-list-files/db/before/price-list-upload.xml")
    @ExpectedDatabase(
        value = "/controller/price-list-files/db/after/price-list-with-restrictions-upload-success.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/history-events/upload-price-list-history-events.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void uploadPriceListWithDirectionCountRestriction() throws Exception {
        doNothing().when(processUploadedPriceListProducer).produceTasks(1L);
        ResourceLocation resourceLocation = newLocation("price_list_document_1.xlsx");

        MockMultipartFile file = newMockFile(
            XLSX_MIME_TYPE,
            getSystemResourceAsStream("controller/price-list-files/excel/minimal-price-list.xlsx")
        );

        mockMvcPerformWithRestrictions(file)
            .andExpect(status().isOk())
            .andExpect(successResult());

        verify(mdsS3Client).upload(eq(resourceLocation), any());
        verify(mdsS3Client).getUrl(resourceLocation);
        verify(processUploadedPriceListProducer).produceTasks(1L);
    }

    @Nonnull
    @Override
    protected ResultActions mockMvcPerform(MockMultipartFile file) throws Exception {
        return mockMvc.perform(
            multipart("/price-list/files/tariff/1")
                .file(file)
        );
    }

    @Nonnull
    @Override
    protected ResultMatcher successResult() {
        return TestUtils.jsonContent(
            "controller/price-list-files/response/upload_success_response.json",
            "createdAt",
            "updatedAt"
        );
    }

    @Nonnull
    @Override
    protected String getRequestParamName() {
        return "file";
    }

    @Nonnull
    private ResultActions mockMvcPerformWithRestrictions(MockMultipartFile file) throws Exception {
        return mockMvc.perform(
            multipart("/price-list/files/tariff/1")
                .file(file)
                .param("directionCountRestriction", "1")
                .param("weightBreaksCountRestriction", "10")
        );
    }
}
