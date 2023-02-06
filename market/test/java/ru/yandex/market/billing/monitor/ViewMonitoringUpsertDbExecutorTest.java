package ru.yandex.market.billing.monitor;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

class ViewMonitoringUpsertDbExecutorTest extends FunctionalTest {

    @Autowired
    private JugglerMonitoringDao jugglerMonitoringDao;

    @Test
    @DisplayName("Синхронизация мониторингов с базой.")
    @DbUnitDataSet(
            before = "csv/ViewMonitoringUpsertDbExecutorTest.jobWorkingTest.before.csv",
            after = "csv/ViewMonitoringUpsertDbExecutorTest.jobWorkingTest.after.csv"
    )
    public void jobWorkingTest() {
        Set<Long> monitorIdsFromDb = jugglerMonitoringDao.getExistingIds();
        Set<Long> monitorIdsInEnum = Set.of(2L, 3L);
        jugglerMonitoringDao.upsertMonitoringsInfo(monitorIdsInEnum);
        monitorIdsFromDb.removeAll(monitorIdsInEnum);
        if (monitorIdsFromDb.size() > 0) {
            jugglerMonitoringDao.deleteMonitorings(monitorIdsFromDb);
        }
    }
}
