package ru.yandex.market.logistics.tarifficator.base.tpl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.factory.ResourceLocationFactory;
import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.util.TvmClientApiTestUtil;
import ru.yandex.market.logistics.util.client.tvm.client.TvmClientApi;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

public abstract class AbstractCourierTariffUploadTest extends AbstractContextualTest {

    private static final String XLSX_MIME_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String XML_MIME_TYPE = "application/xml";

    private static final String MDS_FILE_URL = "http://localhost:8080/tpl_courier_tariff_document_1.xlsx";

    @Autowired
    private MdsS3Client mdsS3Client;

    @Autowired
    private ResourceLocationFactory resourceLocationFactory;

    @Autowired
    private TvmClientApi tvmClientApi;

    @Test
    @DisplayName("Загрузка файла курьерского тарифа")
    @ExpectedDatabase(
        value = "/controller/tpl/tariffs/db/after/success_upload.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    void uploadCourierTariff() throws Exception {
        TvmClientApiTestUtil.mockTvmClientApiUserTicket(tvmClientApi, 111L);
        when(mdsS3Client.getUrl(any())).thenReturn(new URL(MDS_FILE_URL));
        when(resourceLocationFactory.createLocation(anyString())).thenAnswer(invocation ->
            ResourceLocation.create("tarifficator", invocation.getArgument(0, String.class))
        );
        ResourceLocation resourceLocation = newLocation("tpl_courier_tariff_document_1.xlsx");

        MockMultipartFile file = newMockFile(
            XLSX_MIME_TYPE,
            getSystemResourceAsStream("controller/price-list-files/excel/minimal-price-list.xlsx")
        );

        mockMvc.perform(requestBuilder(file).headers(TvmClientApiTestUtil.USER_HEADERS))
            .andExpect(status().isOk())
            .andExpect(successResult());

        verify(mdsS3Client).upload(eq(resourceLocation), any());
        verify(mdsS3Client).getUrl(resourceLocation);
    }

    @Test
    @DisplayName("Загрузка файла курьерского тарифа без authorUid")
    @ExpectedDatabase(
        value = "/controller/tpl/tariffs/db/after/success_upload_without_author_uid.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    void uploadCourierTariffWithoutAuthorUid() throws Exception {
        when(mdsS3Client.getUrl(any())).thenReturn(new URL(MDS_FILE_URL));
        when(resourceLocationFactory.createLocation(anyString())).thenAnswer(invocation ->
            ResourceLocation.create("tarifficator", invocation.getArgument(0, String.class))
        );
        ResourceLocation resourceLocation = newLocation("tpl_courier_tariff_document_1.xlsx");

        MockMultipartFile file = newMockFile(
            XLSX_MIME_TYPE,
            getSystemResourceAsStream("controller/price-list-files/excel/minimal-price-list.xlsx")
        );

        mockMvc.perform(requestBuilder(file))
            .andExpect(status().isOk())
            .andExpect(successResult());

        verify(mdsS3Client).upload(eq(resourceLocation), any());
        verify(mdsS3Client).getUrl(resourceLocation);
    }

    @Test
    @DisplayName("Загрузка файла курьерского тарифа с неверным mime-type")
    @ExpectedDatabase(
        value = "/controller/tpl/tariffs/db/after/wrong_mime_type.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    void uploadCourierTariffWrongMimeType() throws Exception {
        when(mdsS3Client.getUrl(any())).thenReturn(new URL(MDS_FILE_URL));
        when(resourceLocationFactory.createLocation(anyString())).thenAnswer(invocation ->
            ResourceLocation.create("tarifficator", invocation.getArgument(0, String.class))
        );
        ResourceLocation resourceLocation = newLocation("tpl_courier_tariff_document_1.xlsx");

        MockMultipartFile file = newMockFile(
            XML_MIME_TYPE,
            getSystemResourceAsStream("controller/price-list-files/excel/minimal-price-list.xlsx")
        );

        mockMvc.perform(requestBuilder(file))
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage(
                "Unknown content type 'application/xml'. " +
                    "Use one of those: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'"
            ));

        verify(mdsS3Client, never()).upload(eq(resourceLocation), any());
        verify(mdsS3Client, never()).getUrl(resourceLocation);
    }

    @Nonnull
    protected abstract MockMultipartHttpServletRequestBuilder requestBuilder(MockMultipartFile file);

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
