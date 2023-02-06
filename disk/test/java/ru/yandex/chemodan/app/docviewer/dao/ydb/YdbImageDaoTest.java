package ru.yandex.chemodan.app.docviewer.dao.ydb;

import org.junit.Test;

import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.function.Function1V;
import ru.yandex.chemodan.app.docviewer.utils.DimensionO;
import ru.yandex.chemodan.ydb.dao.pojo.YdbTestUtils;
import ru.yandex.misc.db.q.SqlCondition;
import ru.yandex.misc.test.Assert;

/**
 * @author yashunsky
 */
public class YdbImageDaoTest {
    @Test
    public void writeReadTest() {
        doWithEmptyTable(dao -> {
            dao.saveImageIfNotExists("some file id", "some sub id", "some file link");
            Assert.notEmpty(dao.findOne("some file id", "some sub id"));

            dao.savePdfImageIfNotExists(
                    "another file id", 1, new DimensionO(Option.of(100), Option.of(200)), "another file link");
            dao.updateLastAccessTime("some file id", "some sub id");
            Assert.assertEquals(2, dao.find(SqlCondition.trueCondition()).length());

            dao.deleteByFileIdBatch("some file id", image -> dao.delete(image.fileId, image.subId));
            Assert.hasSize(1, dao.find(SqlCondition.trueCondition()));
        });
    }

    private void doWithEmptyTable(Function1V<YdbImageDao> action) {
        YdbTestUtils.doWithTable(YdbImageDao::new, dao -> action.apply((YdbImageDao) dao));
    }
}
