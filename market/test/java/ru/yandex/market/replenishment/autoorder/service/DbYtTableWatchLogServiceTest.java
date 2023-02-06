package ru.yandex.market.replenishment.autoorder.service;

import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.YtTableWatchLog;
import ru.yandex.market.replenishment.autoorder.service.yt.watchable.YtDailyStocksWatchable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
public class DbYtTableWatchLogServiceTest extends FunctionalTest {

    @Autowired
    private DbYtTableWatchLogService watchLogService;

    @Autowired
    private YtDailyStocksWatchable watchable;

    @Test
    public void notFindLatestWatchLog() {
        YtTableWatchLog latestWatchLog = watchLogService.findLatest(watchable);
        assertNull(latestWatchLog);
    }

    @Test
    @DbUnitDataSet(before = "DbYtTableWatchLogServiceTest.before.csv")
    public void findLatestWatchLog() {
        YtTableWatchLog latestWatchLog = watchLogService.findLatest(watchable);
        assertEquals(3, latestWatchLog.getId());
    }

    @Test
    @DbUnitDataSet(before = "DbYtTableWatchLogServiceTest.before.csv")
    public void findNotFilteredLogs() {
        List<YtTableWatchLog> logs = watchLogService.getNotImportedLogs(watchable);
        assertEquals(2, logs.size());
        assertEquals(2, logs.get(0).getId());
        assertEquals(3, logs.get(1).getId());
    }
}
