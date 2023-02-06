package ru.yandex.market.mbo.mdm.common.service;

import java.time.Instant;
import java.util.List;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.infrastructure.MdmMonitoringResult;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ServiceOfferMigrationInfo;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.BusinessLockStatus;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.BusinessLockStatusRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.ServiceOfferMigrationRepository;
import ru.yandex.market.mbo.mdm.common.service.monitoring.MdmBusinessMigrationMonitoringService;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.utils.MdmProperties;
import ru.yandex.market.tms.quartz2.model.MonitoringStatus;

public class MdmBusinessMigrationMonitoringServiceImplTest extends MdmBaseDbTestClass {

    @Autowired
    MdmBusinessMigrationMonitoringService monitoringService;
    @Autowired
    StorageKeyValueService keyValueService;
    @Autowired
    BusinessLockStatusRepository lockStatusRepository;
    @Autowired
    ServiceOfferMigrationRepository offerMigrationRepository;

    private EnhancedRandom random;

    @Before
    public void setup() {
        random = TestDataUtils.defaultRandom(12312);
        keyValueService.putValue(MdmProperties.LOCKED_OLD_SUPPLIERS_WARN_NOTIFICATION_THRESHOLD, 0);
        keyValueService.putValue(MdmProperties.LOCKED_OLD_SUPPLIERS_ERROR_NOTIFICATION_THRESHOLD, 0);
        keyValueService.putValue(MdmProperties.OLD_MIGRATION_OFFERS_WARN_NOTIFICATION_THRESHOLD, 0);
        keyValueService.putValue(MdmProperties.OLD_MIGRATION_OFFERS_ERROR_NOTIFICATION_THRESHOLD, 0);
    }

    @Test
    public void whenAddLockedBusinessesShouldInsertIntoKeyValue() {
        monitoringService.addLockedBusinessesWithSaveRequest(List.of(777, 666, 888));
        var containingSuppliersString =
            keyValueService.getList(MdmProperties.LOCKED_SUPPLIERS_REQUESTED_SAVE_NEW_OFFERS, Integer.class, List.of());
        Assertions.assertThat(containingSuppliersString).containsExactlyInAnyOrder(777, 666, 888);
    }

    @Test
    public void whenRemoveLockedBusinessesShouldRemoveFromKeyValue() {
        monitoringService.addLockedBusinessesWithSaveRequest(List.of(777, 666, 888, 999));
        monitoringService.removeLockedBusinessesWithSaveRequest(List.of(777, 888, 9999));
        var containingSuppliersString =
            keyValueService.getList(MdmProperties.LOCKED_SUPPLIERS_REQUESTED_SAVE_NEW_OFFERS, Integer.class, List.of());
        Assertions.assertThat(containingSuppliersString).containsExactlyInAnyOrder(666, 999);
    }

    @Test
    public void whenRequestOldLockedSuppliersShouldReturnCorrectAnswer() {
        var row1 = random.nextObject(BusinessLockStatus.class);
        keyValueService.invalidateCache();
        int minutesThreshold =
            keyValueService.getCachedInt(MdmProperties.LOCKED_OLD_SUPPLIERS_MINUTES_THRESHOLD, 4);
        row1.setStatus(BusinessLockStatus.Status.LOCKED)
            .setUpdatedTs(Instant.now().minusSeconds((minutesThreshold + 1) * 60));
        lockStatusRepository.insertOrUpdate(row1);
        MdmMonitoringResult monitoringResult = monitoringService.getOldLockedSuppliersIsOverThresholdMonitoringResult();
        Assertions.assertThat(monitoringResult.getStatus()).isEqualTo(MonitoringStatus.CRIT);
        row1.setUpdatedTs(Instant.now());
        lockStatusRepository.insertOrUpdate(row1);
        monitoringResult = monitoringService.getOldLockedSuppliersIsOverThresholdMonitoringResult();
        Assertions.assertThat(monitoringResult.getStatus()).isEqualTo(MonitoringStatus.OK);
    }

    @Test
    public void whenRequestOldMigrationRequestsShouldReturnCorrectAnswer() {
        var row1 = random.nextObject(ServiceOfferMigrationInfo.class);
        int minutesThreshold =
            keyValueService.getCachedInt(MdmProperties.OLD_MIGRATION_OFFERS_MINUTES_THRESHOLD, 4);
        row1.setProcessed(false).setAddedTimestamp(Instant.now().minusSeconds((minutesThreshold + 1) * 60));
        offerMigrationRepository.insertOrUpdate(row1);
        MdmMonitoringResult monitoringResult =
            monitoringService.getOldRequestedMigrationOffersIsOverThresholdMonitoringResult();
        Assertions.assertThat(monitoringResult.getStatus()).isEqualTo(MonitoringStatus.CRIT);
        row1.setAddedTimestamp(Instant.now());
        offerMigrationRepository.insertOrUpdate(row1);
        monitoringResult = monitoringService.getOldRequestedMigrationOffersIsOverThresholdMonitoringResult();
        Assertions.assertThat(monitoringResult.getStatus()).isEqualTo(MonitoringStatus.OK);
    }

}
