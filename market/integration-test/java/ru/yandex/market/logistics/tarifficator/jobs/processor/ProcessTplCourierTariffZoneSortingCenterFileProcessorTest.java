package ru.yandex.market.logistics.tarifficator.jobs.processor;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.mds.s3.client.content.consumer.StreamCopyContentConsumer;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.factory.ResourceLocationFactory;
import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.jobs.model.TplCourierTariffZoneSortingCenterFilePayload;
import ru.yandex.market.logistics.tarifficator.jobs.processor.admin.ProcessTplCourierTariffZoneSortingCenterFileProcessor;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@DatabaseSetup(
    value = "/service/tpl/courier/tariff-zone-sorting-center/db/before/courier_tariff_zones.xml",
    connection = "dbUnitQualifiedDatabaseConnection"
)
public class ProcessTplCourierTariffZoneSortingCenterFileProcessorTest extends AbstractContextualTest {
    private static final TplCourierTariffZoneSortingCenterFilePayload PAYLOAD =
        new TplCourierTariffZoneSortingCenterFilePayload(REQUEST_ID, 1L);
    private static final String MDS_FILE_URL =
        "http://localhost:8080/tpl_courier_tariff_zone_sorting_center_document_1.xlsx";
    @Autowired
    private ProcessTplCourierTariffZoneSortingCenterFileProcessor processor;
    @Autowired
    private MdsS3Client mdsS3Client;
    @Autowired
    private ResourceLocationFactory resourceLocationFactory;

    @BeforeEach
    void init() throws MalformedURLException {
        clock.setFixed(Instant.parse("2021-04-15T10:15:30Z"), ZoneId.systemDefault());
        when(mdsS3Client.getUrl(any())).thenReturn(new URL(MDS_FILE_URL));
        when(resourceLocationFactory.createLocation(anyString())).thenAnswer(invocation ->
            ResourceLocation.create("tarifficator", invocation.getArgument(0, String.class))
        );
    }

    @Test
    @DisplayName("Успешная обработка файла")
    @DatabaseSetup(
        value = "/controller/tpl/tariffs/tariff-zone-sorting-center/db/after/success_upload.xml",
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    @ExpectedDatabase(
        value = "/service/tpl/courier/tariff-zone-sorting-center/db/after/success_processing.xml",
        connection = "dbUnitQualifiedDatabaseConnection",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successProcessing() {
        mockMdsS3Client("service/tpl/courier/tariff-zone-sorting-center/xlsx/tariff_zone_sorting_center.xlsx");
        processor.processPayload(PAYLOAD);
    }

    @Test
    @DisplayName("Обработка пустого файла")
    @DatabaseSetup(
        value = "/controller/tpl/tariffs/tariff-zone-sorting-center/db/after/success_upload.xml",
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    @ExpectedDatabase(
        value = "/service/tpl/courier/tariff-zone-sorting-center/db/after/empty_file.xml",
        connection = "dbUnitQualifiedDatabaseConnection",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void emptyFileProcessing() {
        mockMdsS3Client("service/tpl/courier/tariff-zone-sorting-center/xlsx/empty_file.xlsx");
        processor.processPayload(PAYLOAD);
    }

    private void mockMdsS3Client(String responseFilePath) {
        doAnswer(invocation -> {
            StreamCopyContentConsumer consumer = invocation.getArgument(1);
            InputStream is = Objects.requireNonNull(getSystemResourceAsStream(responseFilePath));
            consumer.consume(is);
            return null;
        }).when(mdsS3Client).download(any(), any());
    }
}
