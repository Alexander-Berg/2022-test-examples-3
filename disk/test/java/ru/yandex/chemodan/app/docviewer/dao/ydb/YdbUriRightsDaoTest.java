package ru.yandex.chemodan.app.docviewer.dao.ydb;

import org.joda.time.Duration;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.function.Function1V;
import ru.yandex.chemodan.app.docviewer.copy.ActualUri;
import ru.yandex.chemodan.ydb.dao.pojo.YdbTestUtils;
import ru.yandex.inside.passport.PassportUidOrZero;
import ru.yandex.misc.test.Assert;

/**
 * @author yashunsky
 */
public class YdbUriRightsDaoTest {
    @Test
    public void writeReadTest() {
        doWithEmptyTable(dao -> {
            ActualUri uri = new ActualUri("http://localhost");
            ActualUri anotherUri = new ActualUri("http://localhost/1");
            PassportUidOrZero uid = PassportUidOrZero.fromUid(12345L);
            String fileId = "now with file id";

            dao.saveOrUpdateUriRight(uri, uid);
            Assert.isTrue(dao.findExistsUriRight(uri, uid));

            dao.updateUriRights(uri, fileId);
            Assert.notEmpty(dao.findUriByFileIdAndUid(fileId, uid));

            Assert.notEmpty(dao.findUrisAccessedByUid(uid));

            Assert.isTrue(dao.validate(fileId, uid));
            Assert.isFalse(dao.validate("wrong file id", uid));

            dao.saveOrUpdateUriRight(anotherUri, uid);

            Assert.equals(Cf.set(uri, anotherUri), dao.findUrisAccessedByUid(uid).unique());
/*      temporary disabled
            dao.deleteByTimestampLessBatch(Instant.now().minus(Duration.standardDays(1)));

            Assert.hasSize(2, dao.find(SqlCondition.trueCondition()));
            dao.deleteByTimestampLessBatch(Instant.now().plus(Duration.standardDays(1)));
            Assert.hasSize(0, dao.find(SqlCondition.trueCondition()));
*/
        });
    }

    private void doWithEmptyTable(Function1V<YdbUriRightsDao> action) {
        YdbTestUtils.doWithTable(transactionManager -> new YdbUriRightsDao(transactionManager, Duration.ZERO),
                dao -> action.apply((YdbUriRightsDao) dao));
    }

}
