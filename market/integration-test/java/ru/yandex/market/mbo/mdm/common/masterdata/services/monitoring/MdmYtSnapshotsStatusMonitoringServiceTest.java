package ru.yandex.market.mbo.mdm.common.masterdata.services.monitoring;

import java.time.LocalDateTime;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.market.mbo.mdm.common.infrastructure.MdmMonitoringResult;
import ru.yandex.market.mbo.mdm.common.service.monitoring.MdmYtSnapshotsStatusMonitoringService;
import ru.yandex.market.mbo.mdm.common.service.monitoring.MdmYtSnapshotsStatusMonitoringServiceImpl;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.MdmBaseIntegrationTestClass;
import ru.yandex.market.mboc.common.infrastructure.util.UnstableInit;
import ru.yandex.market.mboc.common.utils.MdmProperties;
import ru.yandex.market.tms.quartz2.model.MonitoringStatus;

/**
 * В тесте проверяется корректность коннекта к Yt, считывания таблиц и их обработки (парсинг колонок и тд).
 * Корректность/консистентость данных не проверяется.
 */
public class MdmYtSnapshotsStatusMonitoringServiceTest extends MdmBaseIntegrationTestClass {
    @Autowired
    @Qualifier("hahnYtHttpApi")
    private UnstableInit<Yt> hahnYtHttpApi;

    @Autowired
    @Qualifier("arnoldYtHttpApi")
    private UnstableInit<Yt> arnoldYtHttpApi;

    @Autowired
    private StorageKeyValueService skv;

    @Value("${mdm.yt-snapshots-status-table:}")
    private String ytSnapshotsStatusTable;

    @Value("${mdm.yt-snapshots.master-data:}")
    private String masterDataYtSnapshotTable;

    @Value("${mdm.yt-snapshots.reference-item:}")
    private String referenceItemYtSnapshotTable;

    private MdmYtSnapshotsStatusMonitoringService monitoringService;

    @Before
    public void setUp() throws Exception {
        monitoringService = new MdmYtSnapshotsStatusMonitoringServiceImpl(
            hahnYtHttpApi,
            arnoldYtHttpApi,
            skv,
            ytSnapshotsStatusTable,
            masterDataYtSnapshotTable,
            referenceItemYtSnapshotTable
        );
    }

    @Test
    public void testNoCheckNeededYet() {
        // given
        skv.putValue(MdmProperties.YT_SNAPSHOTS_STATUS_CHECK_START_HOUR_UTC, LocalDateTime.now().getHour() + 1);

        // when
        MdmMonitoringResult result = monitoringService.checkYtSnapshotsStatus();

        // then
        Assertions.assertThat(result.getStatus()).isEqualTo(MonitoringStatus.WARN);
    }
}
