package ru.yandex.chemodan.app.docviewer.dao.ydb;

import org.junit.Test;

import ru.yandex.bolts.function.Function1V;
import ru.yandex.chemodan.ydb.dao.pojo.YdbTestUtils;
import ru.yandex.misc.test.Assert;

/**
 * @author yashunsky
 */
public class YdbScheduledTaskDaoTest {
    @Test
    public void writeReadTest() {
        doWithEmptyTable(dao -> {
            String taskId = "some task id";
            String host = "localhost";
            dao.saveOrUpdateScheduleItem(taskId, host);
            Assert.hasSize(1, dao.find(taskId));
            dao.delete(taskId);
            Assert.hasSize(0, dao.find(taskId));
        });
    }

    private void doWithEmptyTable(Function1V<YdbScheduledTaskDao> action) {
        YdbTestUtils.doWithTable(tm -> new YdbScheduledTaskDao(tm, "test"),
                dao -> action.apply((YdbScheduledTaskDao) dao));
    }
}
