package ru.yandex.market.netting;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.notification.service.NotificationService;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.shop.FunctionalTest;

public class NettingMigrationServiceTest extends FunctionalTest {


    @Autowired
    private NettingMigrationDao nettingMigrationDao;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private NotificationService notificationService;

    @Test
    @DbUnitDataSet(before = "csv/NettingMigrationServiceTest.testUpdate.before.csv",
            after = "csv/NettingMigrationServiceTest.testUpdate.after.csv")
    void testUpdate() {
        NettingMigrationYtDao nettingMigrationYtDao = Mockito.mock(NettingMigrationYtDao.class);
        Mockito.when(nettingMigrationYtDao.getWhitePartnersWithActiveStrategies()).thenReturn(Set.of(4444L));
        Mockito.when(nettingMigrationYtDao.getBluePartnersWithActiveStrategies()).thenReturn(Set.of(5555L));

        NettingMigrationService nettingMigrationService = new NettingMigrationService(
                nettingMigrationDao,
                nettingMigrationYtDao,
                notificationService,
                environmentService
        );

        nettingMigrationService.doMigrate();
    }
}
