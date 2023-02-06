package ru.yandex.market.logistics.tarifficator.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;

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

import ru.yandex.market.common.mds.s3.client.content.consumer.StreamCopyContentConsumer;
import ru.yandex.market.common.mds.s3.client.exception.MdsS3Exception;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.factory.ResourceLocationFactory;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.LocationZoneResponse;
import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.service.withdraw.pricelist.WithdrawPriceListFileService;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

class WithdrawPriceListFileServiceTest extends AbstractContextualTest {
    private static final Instant TIME_11_AM = Instant.parse("2020-02-02T11:00:00.00Z");
    private static final String MDS_FILE_URL = "http://localhost:8080/withdraw_price_list_document_1.xlsx";
    private static final Long PRICE_LIST_FILE_ID = 1L;

    @Autowired
    private MdsS3Client mdsS3Client;
    @Autowired
    private ResourceLocationFactory resourceLocationFactory;
    @Autowired
    private WithdrawPriceListFileService withdrawPriceListFileService;
    @Autowired
    private LMSClient lmsClient;

    @BeforeEach
    void init() throws MalformedURLException {
        clock.setFixed(TIME_11_AM, ZoneOffset.UTC);
        when(mdsS3Client.getUrl(any())).thenReturn(new URL(MDS_FILE_URL));
        when(resourceLocationFactory.createLocation(anyString())).thenAnswer(invocation ->
            ResourceLocation.create("tarifficator", invocation.getArgument(0, String.class))
        );
    }

    @AfterEach
    void after() {
        verifyNoMoreInteractions(mdsS3Client);
        clock.clearFixed();
    }

    @Test
    @DisplayName("Успешно обработать файл заборного прайс-листа")
    @DatabaseSetup("/controller/withdraw/price-list-files/db/after/withdraw-price-list-file-uploaded-success.xml")
    @ExpectedDatabase(
        value = "/controller/withdraw/price-list-files/db/after/withdraw-price-list-file-processed-success.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void processFileSuccess() {
        mockLocationZones();
        mockMdsS3Client("controller/withdraw/price-list-files/xlsx/price-list-file.xlsx");
        withdrawPriceListFileService.processFile(PRICE_LIST_FILE_ID);
        verifyMdsS3Client();
    }

    @Test
    @DisplayName("Обработать файл заборного прайс-листа с несуществующей локацией")
    @DatabaseSetup("/controller/withdraw/price-list-files/db/after/withdraw-price-list-file-uploaded-success.xml")
    @ExpectedDatabase(
        value = "/controller/withdraw/price-list-files/db/after/withdraw-price-list-file-with-non-exist-location.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void processFileWitNonExistLocationZones() {
        mockLocationZones();
        mockMdsS3Client("controller/withdraw/price-list-files/xlsx/non-exist-location-zones.xlsx");
        withdrawPriceListFileService.processFile(PRICE_LIST_FILE_ID);
        verifyMdsS3Client();
    }

    @Test
    @DisplayName("Обработать файл заборного прайс-листа с неуказанной локацией")
    @DatabaseSetup("/controller/withdraw/price-list-files/db/after/withdraw-price-list-file-uploaded-success.xml")
    @ExpectedDatabase(
        value = "/controller/withdraw/price-list-files/db/after/non-presented-location-zone.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void processFileWitNonPresentedLocationZones() {
        mockLocationZones();
        mockMdsS3Client("controller/withdraw/price-list-files/xlsx/non-presented-location-zones.xlsx");
        withdrawPriceListFileService.processFile(PRICE_LIST_FILE_ID);
        verifyMdsS3Client();
    }

    @Test
    @DisplayName("Ошибка при скачивании файла из хранилища прокидывается выше")
    @DatabaseSetup("/controller/withdraw/price-list-files/db/after/withdraw-price-list-file-uploaded-success.xml")
    void unableToDownloadFeed() {
        doThrow(new MdsS3Exception("Could not process operation")).when(mdsS3Client).download(any(), any());
        softly.assertThatThrownBy(() -> withdrawPriceListFileService.processFile(PRICE_LIST_FILE_ID))
            .isInstanceOf(MdsS3Exception.class);
        verifyMdsS3Client();
    }

    @Nonnull
    private ResourceLocation newLocation(String key) {
        return ResourceLocation.create("tarifficator", key);
    }

    @Nonnull
    private MockMultipartFile newMockFile(String extension, InputStream fileStream) throws IOException {
        return new MockMultipartFile(
            "file",
            "originalFileName",
            extension,
            fileStream
        );
    }

    private void verifyMdsS3Client() {
        verify(mdsS3Client).download(
            eq(createResourceLocation()),
            any(StreamCopyContentConsumer.class)
        );
    }

    @Nonnull
    private ResourceLocation createResourceLocation() {
        return resourceLocationFactory.createLocation("withdraw_price_list_document_1.xlsx");
    }

    @Nonnull
    private ResultActions mockMvcPerform(MockMultipartFile file) throws Exception {
        return mockMvc.perform(multipart("/withdraw-price-list/files/tariff/1").file(file));
    }

    private void mockMdsS3Client(String responseFilePath) {
        doAnswer(invocation -> {
            StreamCopyContentConsumer consumer = invocation.getArgument(1);
            InputStream is = Objects.requireNonNull(getSystemResourceAsStream(responseFilePath));
            consumer.consume(is);
            return null;
        }).when(mdsS3Client).download(any(), any());
    }

    private void mockLocationZones() {
        when(lmsClient.getLocationZonesByLocationId(213L)).thenReturn(List.of(
            new LocationZoneResponse.Builder()
                .locationZoneId(1L)
                .name("Внутри МКАД")
                .locationId(213L)
                .build(),
            new LocationZoneResponse.Builder()
                .locationZoneId(2L)
                .name("От 0 до 10 км от МКАД")
                .locationId(213L)
                .build(),
            new LocationZoneResponse.Builder()
                .locationZoneId(3L)
                .name("От 10 до 30 км от МКАД")
                .locationId(213L)
                .build(),
            new LocationZoneResponse.Builder()
                .locationZoneId(4L)
                .name("От 30 км от МКАД до границ МО")
                .locationId(213L)
                .build()
            )
        );
    }
}
