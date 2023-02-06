package ru.yandex.market.tsum.tms.tasks.training;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.tsum.clients.staff.StaffApiClient;
import ru.yandex.market.tsum.core.config.MongoConfig;
import ru.yandex.market.tsum.tms.TsumTmsDebugRuntimeConfig;

import java.text.SimpleDateFormat;

/**
 * @author Strakhov Artem <a href="mailto:dukeartem@yandex-team.ru"></a>
 * @date 29.08.17
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MongoConfig.class, TsumTmsDebugRuntimeConfig.class})
public class TrainingAlertsCronTaskTest {

    @Autowired
    StaffApiClient staffApiClient;

    @Test
    public void checkAlertTime() throws Exception {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Assert.assertTrue(TrainingAlertsCronTask.checkAlertTime(formatter.parse("2017-08-10 00:00:00"),
            formatter.parse("2017-08-10 01:00:00"),
            3700));
        Assert.assertFalse(TrainingAlertsCronTask.checkAlertTime(formatter.parse("2017-08-10 00:00:00"),
            formatter.parse("2017-08-10 01:00:00"),
            3500));
    }

    @Ignore
    @Test
    public void doesStaffLoginExist() throws Exception {
        Assert.assertTrue(TrainingAlertsCronTask.doesStaffLoginExist("dukeartem", staffApiClient));
        Assert.assertFalse(TrainingAlertsCronTask.doesStaffLoginExist("kukajambo", staffApiClient));
    }

}