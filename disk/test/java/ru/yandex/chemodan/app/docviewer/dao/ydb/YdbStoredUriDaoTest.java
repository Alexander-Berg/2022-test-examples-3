package ru.yandex.chemodan.app.docviewer.dao.ydb;

import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Test;

import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Tuple2List;
import ru.yandex.bolts.function.Function1V;
import ru.yandex.chemodan.app.docviewer.convert.TargetType;
import ru.yandex.chemodan.app.docviewer.copy.ActualUri;
import ru.yandex.chemodan.app.docviewer.states.ErrorCode;
import ru.yandex.chemodan.ydb.dao.pojo.YdbTestUtils;
import ru.yandex.misc.dataSize.DataSize;
import ru.yandex.misc.test.Assert;

/**
 * @author yashunsky
 */
public class YdbStoredUriDaoTest {

    @Test
    public void writeReadTest() {
        doWithEmptyTable(dao -> {
            ActualUri uri = new ActualUri("http://localhost");
            ActualUri anotherUri = new ActualUri("http://localhost/2");
            dao.saveOrUpdateUri(uri, Option.of("some content type"), TargetType.HTML_WITH_IMAGES, 1.0f);
            dao.saveOrUpdateUri(uri, Option.of("some content type"), TargetType.PLAIN_TEXT, 1.0f);
            dao.updatePasswords(uri, Tuple2List.fromPairs("v1", "v2"));
            dao.updateUri(uri,
                    Option.of("some other content type"), "some file id", DataSize.MEGABYTE, Option.of(Instant.now()));
            dao.updateUri(uri, ErrorCode.UNSUPPORTED_SOURCE_TYPE, "everything went wrong",
                    Option.of("some error path"), Option.of(100L), Option.of(200L));

            Assert.notEmpty(dao.findAllByFileId("some file id"));
            Assert.notEmpty(dao.find(uri));
            dao.updateUriClean(uri);
            Assert.notEmpty(dao.find(uri));

            dao.saveOrUpdateUri(anotherUri, Option.of("some content type"), TargetType.HTML_WITH_IMAGES, 1.0f);
            dao.updateUri(anotherUri, ErrorCode.UNSUPPORTED_SOURCE_TYPE, "everything went wrong",
                    Option.of("some error path"), Option.of(100L), Option.of(200L));

/*      temporary disabled
            Assert.hasSize(2, dao.find(SqlCondition.trueCondition()));
            dao.deleteErrorsByTimestampLessBatch(Instant.now().plus(Duration.standardDays(1)));
            Assert.hasSize(1, dao.find(SqlCondition.trueCondition()));
            dao.deleteByTimestampLessBatch(Instant.now().plus(Duration.standardDays(1)));
            Assert.hasSize(0, dao.find(SqlCondition.trueCondition()));
*/
        });
    }

    private void doWithEmptyTable(Function1V<YdbStoredUriDao> action) {
        YdbTestUtils.doWithTable(transactionManager -> new YdbStoredUriDao(transactionManager, Duration.ZERO),
                dao -> action.apply((YdbStoredUriDao) dao));
    }
}
