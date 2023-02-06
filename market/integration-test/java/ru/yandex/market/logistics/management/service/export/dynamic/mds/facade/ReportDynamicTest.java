package ru.yandex.market.logistics.management.service.export.dynamic.mds.facade;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import ru.yandex.market.common.mds.s3.client.content.ContentProvider;
import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.repository.export.VersionedMdsRepository;
import ru.yandex.market.logistics.management.repository.export.dynamic.s3.MdsS3BucketClient;
import ru.yandex.market.logistics.management.service.export.dynamic.DynamicFileFacade;
import ru.yandex.market.logistics.management.util.CleanDatabase;
import ru.yandex.market.logistics.management.util.TestableClock;

import static org.mockito.Mockito.atLeastOnce;

@SuppressWarnings("checkstyle:magicnumber")
@CleanDatabase
class ReportDynamicTest extends AbstractContextualTest {

    @Autowired
    private MdsS3BucketClient mdsClient;

    @Autowired
    private DynamicFileFacade dynamicFileFacade;

    @Autowired
    private TestableClock clock;

    @BeforeEach
    void setup() {
        clock.setFixed(Instant.parse("2020-02-20T20:20:20Z"), ZoneId.systemDefault());
    }

    @DisplayName("Проверяем корректность формирования файлов выгрузки dynamic (два platform_client_id)")
    @Test
    @Sql("/data/service/export/dynamic/prepare_data.sql")
    void testCreateReportDynamicFilesSuccess() {
        dynamicFileFacade.instantUpdate();

        ArgumentCaptor<String> captorPath = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ContentProvider> captorContent = ArgumentCaptor.forClass(ContentProvider.class);
        Mockito.verify(mdsClient, atLeastOnce()).upload(captorPath.capture(), captorContent.capture());

        String version = VersionedMdsRepository.FORMATTER.format(clock.instant());
        List<String> path = captorPath.getAllValues();
        var expectedPathes = List.of(
            "report_dynamic/lms.pbuf.sn.gz",
            "report_dynamic/archive/lms.pbuf.sn-" + version,
            "report_dynamic/lms.pbuf.sn",
            "report_dynamic/lms_1.pbuf.sn.gz",
            "report_dynamic/archive/lms_1.pbuf.sn-" + version,
            "report_dynamic/lms_1.pbuf.sn",
            "report_dynamic/lms_2.pbuf.sn.gz",
            "report_dynamic/archive/lms_2.pbuf.sn-" + version,
            "report_dynamic/lms_2.pbuf.sn"
        );
        softly.assertThat(path).as("Path parameter times captured").hasSize(expectedPathes.size());
        softly.assertThat(path).containsExactlyElementsOf(expectedPathes);
    }

    @DisplayName("Проверяем корректность формирования файлов выгрузки dynamic (частично валидные партнеры)")
    @Test
    @Sql("/data/service/export/dynamic/prepare_data_partial_correct.sql")
    void testCreateReportDynamicPartFilesSuccess() {
        dynamicFileFacade.instantUpdate();

        ArgumentCaptor<String> captorPath = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ContentProvider> captorContent = ArgumentCaptor.forClass(ContentProvider.class);
        Mockito.verify(mdsClient, atLeastOnce()).upload(captorPath.capture(), captorContent.capture());

        String version = VersionedMdsRepository.FORMATTER.format(clock.instant());
        var expectedPathes = List.of(
            "report_dynamic/lms.pbuf.sn.gz",
            "report_dynamic/archive/lms.pbuf.sn-" + version,
            "report_dynamic/lms.pbuf.sn",
            "report_dynamic/lms_1.pbuf.sn.gz",
            "report_dynamic/archive/lms_1.pbuf.sn-" + version,
            "report_dynamic/lms_1.pbuf.sn"
        );
        List<String> path = captorPath.getAllValues();
        softly.assertThat(path).as("Path parameter times captured").hasSize(expectedPathes.size());
        softly.assertThat(path).containsExactlyElementsOf(expectedPathes);
    }
}
