package ru.yandex.market.logistics.tarifficator.admin.tpl.couriertariffzonesortingcenter;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

import ru.yandex.market.common.mds.s3.client.content.ContentProvider;
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
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.noContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

class UploadFileTest extends AbstractContextualTest {
    private static final String XLSX_MIME_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String XML_MIME_TYPE = "application/xml";

    private static final String MDS_FILE_URL =
        "http://localhost:8080/tpl_courier_tariff_zone_sorting_center_document_1.xlsx";

    @Autowired
    private MdsS3Client mdsS3Client;

    @Autowired
    private ResourceLocationFactory resourceLocationFactory;

    @Autowired
    private TvmClientApi tvmClientApi;

    @AfterEach
    void after() {
        verifyNoMoreInteractions(mdsS3Client);
    }

    @Test
    @DisplayName("Загрузка файла со связками тарифных зон с сортировочными центрами")
    @ExpectedDatabase(
        value = "/controller/tpl/tariffs/tariff-zone-sorting-center/db/after/success_upload.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    void uploadCourierTariff() throws Exception {
        TvmClientApiTestUtil.mockTvmClientApiUserTicket(tvmClientApi, 111L);
        when(mdsS3Client.getUrl(any())).thenReturn(new URL(MDS_FILE_URL));
        when(resourceLocationFactory.createLocation(anyString())).thenAnswer(invocation ->
            ResourceLocation.create("tarifficator", invocation.getArgument(0, String.class))
        );
        ResourceLocation resourceLocation = newLocation("tpl_courier_tariff_zone_sorting_center_document_1.xlsx");

        MockMultipartFile file = newMockFile(
            XLSX_MIME_TYPE,
            getSystemResourceAsStream("controller/price-list-files/excel/minimal-price-list.xlsx")
        );

        mockMvc.perform(
            multipart("/admin/tpl-courier-tariffs/zone-sorting-center/files/upload")
                .file(file)
                .headers(TvmClientApiTestUtil.USER_HEADERS)
        )
            .andExpect(status().isOk())
            .andExpect(noContent());

        verify(mdsS3Client).upload(eq(resourceLocation), any(ContentProvider.class));
        verify(mdsS3Client).getUrl(resourceLocation);
    }

    @Test
    @DisplayName("Загрузка файла со связками тарифных зон с сортировочными центрами без authorUid")
    @ExpectedDatabase(
        value = "/controller/tpl/tariffs/tariff-zone-sorting-center/db/after/success_upload_without_author_uid.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    void uploadCourierTariffWithoutAuthorUid() throws Exception {
        when(mdsS3Client.getUrl(any())).thenReturn(new URL(MDS_FILE_URL));
        when(resourceLocationFactory.createLocation(anyString())).thenAnswer(invocation ->
            ResourceLocation.create("tarifficator", invocation.getArgument(0, String.class))
        );
        ResourceLocation resourceLocation = newLocation("tpl_courier_tariff_zone_sorting_center_document_1.xlsx");

        MockMultipartFile file = newMockFile(
            XLSX_MIME_TYPE,
            getSystemResourceAsStream("controller/price-list-files/excel/minimal-price-list.xlsx")
        );

        mockMvc.perform(multipart("/admin/tpl-courier-tariffs/zone-sorting-center/files/upload").file(file))
            .andExpect(status().isOk())
            .andExpect(noContent());

        verify(mdsS3Client).upload(eq(resourceLocation), any(ContentProvider.class));
        verify(mdsS3Client).getUrl(resourceLocation);
    }

    @Test
    @DisplayName("Загрузка файла со связками тарифных зон с сортировочными центрами с неверным mime-type")
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
        ResourceLocation resourceLocation = newLocation("tpl_courier_tariff_zone_sorting_center_document_1.xlsx");

        MockMultipartFile file = newMockFile(
            XML_MIME_TYPE,
            getSystemResourceAsStream("controller/price-list-files/excel/minimal-price-list.xlsx")
        );

        mockMvc.perform(multipart("/admin/tpl-courier-tariffs/zone-sorting-center/files/upload").file(file))
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage(
                "Unknown content type 'application/xml'. " +
                    "Use one of those: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'"
            ));

        verify(mdsS3Client, never()).upload(eq(resourceLocation), any(ContentProvider.class));
        verify(mdsS3Client, never()).getUrl(resourceLocation);
    }

    @Nonnull
    private ResourceLocation newLocation(String key) {
        return ResourceLocation.create("tarifficator", key);
    }

    @Nonnull
    private MockMultipartFile newMockFile(String extension, InputStream fileStream) throws IOException {
        return new MockMultipartFile(
            "request",
            "originalFileName",
            extension,
            fileStream
        );
    }
}
