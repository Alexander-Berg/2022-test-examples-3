package ru.yandex.market.logistics.tarifficator.service.tpl;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.mds.s3.client.content.consumer.StreamCopyContentConsumer;
import ru.yandex.market.common.mds.s3.client.exception.MdsS3Exception;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.factory.ResourceLocationFactory;
import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DbUnitConfiguration(
    dataSetLoader = ReplacementDataSetLoader.class,
    databaseConnection = "dbUnitQualifiedDatabaseConnection"
)
@DisplayName("Тесты сервиса для работы с тарифами курьерских 3pl компаний")
public class CourierTariffServiceTest extends AbstractContextualTest {
    private static final String MDS_FILE_URL = "http://localhost:8080/tpl_courier_tariff_document_1.xlsx";

    @Autowired
    private MdsS3Client mdsS3Client;
    @Autowired
    private ResourceLocationFactory resourceLocationFactory;
    @Autowired
    private CourierTariffService courierTariffService;

    @BeforeEach
    void init() throws MalformedURLException {
        clock.setFixed(Instant.parse("2021-04-15T10:15:30Z"), ZoneId.systemDefault());
        when(mdsS3Client.getUrl(any())).thenReturn(new URL(MDS_FILE_URL));
        when(resourceLocationFactory.createLocation(anyString())).thenAnswer(invocation ->
            ResourceLocation.create("tarifficator", invocation.getArgument(0, String.class))
        );
    }

    @Test
    @DisplayName("Успешно обработать файл тарифа курьерских 3pl компаний")
    @DatabaseSetup("/controller/tpl/tariffs/db/after/success_upload.xml")
    @ExpectedDatabase(
        value = "/service/tpl/courier/db/after/process_success.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void processFileSuccess() {
        mockMdsS3Client("service/tpl/courier/xlsx/tpl_courier_tariff.xlsx");
        courierTariffService.processFile(1L);
        verifyMdsS3Client();
    }

    @Test
    @DisplayName("Успешно обработать файл тарифа курьерских 3pl компаний с указанием тарифной зоны")
    @DatabaseSetup("/controller/tpl/tariffs/db/after/success_upload.xml")
    @DatabaseSetup("/controller/tpl/tariffs/db/after/tariff_zone.xml")
    @ExpectedDatabase(
        value = "/service/tpl/courier/db/after/process_with_tariff_zone_success.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void processFileWithTariffZoneSuccess() {
        mockMdsS3Client("service/tpl/courier/xlsx/tpl_courier_tariff_with_tariff_zone.xlsx");
        courierTariffService.processFile(1L);
        verifyMdsS3Client();
    }

    @Test
    @DisplayName("Обработать файл тарифа курьерских 3pl компаний без обязательных колонок")
    @DatabaseSetup("/controller/tpl/tariffs/db/after/success_upload.xml")
    @ExpectedDatabase(
        value = "/service/tpl/courier/db/after/process_without_required_columns.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void processFileWithoutRequiredColumns() {
        mockMdsS3Client("service/tpl/courier/xlsx/tpl_courier_tariff_without_required_columns.xlsx");
        courierTariffService.processFile(1L);
        verifyMdsS3Client();
    }

    @Test
    @DisplayName("Обработать файл тарифа курьерских 3pl компаний без обязательных полей")
    @DatabaseSetup("/controller/tpl/tariffs/db/after/success_upload.xml")
    @ExpectedDatabase(
        value = "/service/tpl/courier/db/after/process_without_required_fields.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void processFileWithoutRequiredFields() {
        mockMdsS3Client("service/tpl/courier/xlsx/tpl_courier_tariff_without_required_fields.xlsx");
        courierTariffService.processFile(1L);
        verifyMdsS3Client();
    }

    @Test
    @DisplayName("Обработать файл тарифа курьерских 3pl компаний с отрицательными полями")
    @DatabaseSetup("/controller/tpl/tariffs/db/after/success_upload.xml")
    @ExpectedDatabase(
        value = "/service/tpl/courier/db/after/process_with_negative_fields.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void processFileWithNegativeFields() {
        mockMdsS3Client("service/tpl/courier/xlsx/tpl_courier_tariff_with_negative_fields.xlsx");
        courierTariffService.processFile(1L);
        verifyMdsS3Client();
    }

    @Test
    @DisplayName("Обработать файл тарифа курьерских 3pl компаний с указанием несуществующей тарифной зоны")
    @DatabaseSetup("/controller/tpl/tariffs/db/after/success_upload.xml")
    @DatabaseSetup("/controller/tpl/tariffs/db/after/tariff_zone.xml")
    @ExpectedDatabase(
        value = "/service/tpl/courier/db/after/process_with_non_exist_tariff_zone.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void processFileWithNonExistTariffZone() {
        mockMdsS3Client("service/tpl/courier/xlsx/tpl_courier_tariff_with_non_exist_tariff_zone.xlsx");
        courierTariffService.processFile(1L);
        verifyMdsS3Client();
    }

    @Test
    @DisplayName("Ошибка при скачивании файла из хранилища прокидывается выше")
    @DatabaseSetup("/controller/tpl/tariffs/db/after/success_upload.xml")
    @ExpectedDatabase(
        value = "/service/tpl/courier/db/after/process_mds_error.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void unableToDownloadCourierTariff() {
        doThrow(new MdsS3Exception("Could not process operation")).when(mdsS3Client).download(any(), any());
        softly.assertThatThrownBy(() -> courierTariffService.processFile(1L))
            .isInstanceOf(MdsS3Exception.class)
            .hasMessage("Could not process operation");
        verifyMdsS3Client();
    }

    private void mockMdsS3Client(String responseFilePath) {
        doAnswer(invocation -> {
            StreamCopyContentConsumer consumer = invocation.getArgument(1);
            InputStream is = Objects.requireNonNull(getSystemResourceAsStream(responseFilePath));
            consumer.consume(is);
            return null;
        }).when(mdsS3Client).download(any(), any());
    }

    private void verifyMdsS3Client() {
        verify(mdsS3Client).download(
            eq(resourceLocationFactory.createLocation("tpl_courier_tariff_document_1.xlsx")),
            any(StreamCopyContentConsumer.class)
        );
    }
}
