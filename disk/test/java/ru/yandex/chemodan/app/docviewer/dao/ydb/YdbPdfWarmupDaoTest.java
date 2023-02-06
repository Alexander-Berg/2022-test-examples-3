package ru.yandex.chemodan.app.docviewer.dao.ydb;

import org.joda.time.Duration;
import org.junit.Test;

import ru.yandex.bolts.function.Function1V;
import ru.yandex.chemodan.app.docviewer.dao.pdfWarmup.PdfWarmupTarget;
import ru.yandex.chemodan.ydb.dao.pojo.YdbTestUtils;
import ru.yandex.misc.db.q.SqlCondition;
import ru.yandex.misc.test.Assert;

/**
 * @author yashunsky
 */
public class YdbPdfWarmupDaoTest {
    @Test
    public void writeReadTest() {
        doWithEmptyTable(dao -> {
            PdfWarmupTarget a = new PdfWarmupTarget("id", 320, 240, false);
            PdfWarmupTarget b = new PdfWarmupTarget("id", 240, 320, false);
            Assert.assertHasSize(1, dao.createTasks(a, 0, 0));
            Assert.assertHasSize(0, dao.createTasks(a, 0, 0));
            Assert.assertHasSize(0, dao.createTasks(a, 1, 1));
            Assert.assertHasSize(1, dao.createTasks(b, 0, 0));
            dao.cleanup();
            Assert.assertHasSize(0, dao.createTasks(b, 0, 0));
            Assert.assertHasSize(0, dao.createTasks(b, 0, 8));
            Assert.assertHasSize(1, dao.createTasks(b, 8, 10));
            Assert.assertHasSize(2, dao.createTasks(b, 1, 31));
            Assert.assertHasSize(1, dao.createTasks(b, 40, 40));
            dao.forceCleanup(Duration.ZERO);
            Assert.hasSize(0, dao.find(SqlCondition.trueCondition()));
        });
    }

    private void doWithEmptyTable(Function1V<YdbPdfWarmupDao> action) {
        int blockSize = 9;
        Duration ttl = Duration.standardDays(2);
        YdbTestUtils.doWithTable(tm -> new YdbPdfWarmupDao(tm, blockSize, ttl, () -> false),
                dao -> action.apply((YdbPdfWarmupDao) dao));
    }
}
