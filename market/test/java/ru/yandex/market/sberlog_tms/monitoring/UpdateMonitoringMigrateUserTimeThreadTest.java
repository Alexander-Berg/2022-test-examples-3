package ru.yandex.market.sberlog_tms.monitoring;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import ru.yandex.market.sberlog_tms.SberlogtmsConfig;
import ru.yandex.market.sberlog_tms.dao.SberlogDbDao;

import java.util.Collections;
import java.util.regex.Pattern;

/**
 * @author Strakhov Artem <a href="mailto:dukeartem@yandex-team.ru"></a>
 * @date 19.11.19
 */
@Disabled
@SpringJUnitConfig(SberlogtmsConfig.class)
class UpdateMonitoringMigrateUserTimeThreadTest {
    @Value("${sberlogtms.scheduled.migratemarketidtopuid.crittime}")
    long migrateMarketidToPuidCritTime;

    @Test
    public void allOk() throws InterruptedException {
        //"0;OK"
        Thread.sleep(1000);
        UpdateMonitoringMigrateUserTimeThread.setRun(true);

        SberlogDbDao sberlogDbDao = Mockito.mock(SberlogDbDao.class);
        Mockito.when(sberlogDbDao.getAllUnlinkedUser()).thenReturn(Collections.singletonList(""));

        new UpdateMonitoringMigrateUserTimeThread(sberlogDbDao, migrateMarketidToPuidCritTime);
        Thread.sleep(1000);
        UpdateMonitoringMigrateUserTimeThread.setRun(false);

        final Pattern answer_pattern = Pattern.compile("0;OK");

        Assertions.assertTrue(answer_pattern.matcher(
                UpdateMonitoringMigrateUserTimeThread.getRefreshTimeStatus()).matches());
    }

    @Test
    public void userDidNotMigrate() throws InterruptedException {
        //"2;This users: " + status + " not migrate more than "
        Thread.sleep(1000);
        UpdateMonitoringMigrateUserTimeThread.setRun(true);

        SberlogDbDao sberlogDbDao = Mockito.mock(SberlogDbDao.class);
        Mockito.when(sberlogDbDao.getAllUnlinkedUser()).thenReturn(Collections.singletonList("123"));

        new UpdateMonitoringMigrateUserTimeThread(sberlogDbDao, migrateMarketidToPuidCritTime);
        Thread.sleep(10000);
        UpdateMonitoringMigrateUserTimeThread.setRun(false);

        final Pattern answer_pattern = Pattern.compile("2;This users: .*");

        Assertions.assertTrue(answer_pattern.matcher(
                UpdateMonitoringMigrateUserTimeThread.getRefreshTimeStatus()).matches());


    }

}
