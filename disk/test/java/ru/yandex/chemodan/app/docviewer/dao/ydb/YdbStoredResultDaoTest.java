package ru.yandex.chemodan.app.docviewer.dao.ydb;

import org.joda.time.Duration;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.function.Function1V;
import ru.yandex.chemodan.app.docviewer.convert.DocumentProperties;
import ru.yandex.chemodan.app.docviewer.convert.TargetType;
import ru.yandex.chemodan.app.docviewer.convert.result.ConvertResultType;
import ru.yandex.chemodan.app.docviewer.convert.result.PageInfo;
import ru.yandex.chemodan.app.docviewer.convert.result.PagesInfo;
import ru.yandex.chemodan.app.docviewer.dao.results.ConvertErrorArgs;
import ru.yandex.chemodan.app.docviewer.dao.results.ConvertSuccessArgs;
import ru.yandex.chemodan.app.docviewer.states.ErrorCode;
import ru.yandex.chemodan.ydb.dao.pojo.YdbTestUtils;
import ru.yandex.commune.alive2.AliveAppInfo;
import ru.yandex.misc.test.Assert;


/**
 * @author yashunsky
 */
public class YdbStoredResultDaoTest {
    @Test
    public void writeReadTest() {
        String fileId = "file1234567890";
        TargetType targetType = TargetType.HTML_WITH_IMAGES;

        doWithEmptyTable(dao -> {
            ConvertSuccessArgs args = ConvertSuccessArgs.builder()
                    .fileId(fileId)
                    .targetType(targetType)
                    .type(ConvertResultType.ZIPPED_HTML)
                    .resultFileLink("some link")
                    .weight(100)
                    .length(200)
                    .pages(1)
                    .contentType(Option.of("text/html"))
                    .remoteFileId(Option.of("remote file id"))
                    .pagesInfo(Option.of(new PagesInfo(Cf.list(new PageInfo(1, 2.3f, 4.5f)))))
                    .rawPassword(Option.of("password"))
                    .properties(new DocumentProperties(Cf.map("someProperty", "and it's value")))
                    .build();
            dao.saveOrUpdateResult(args);

            dao.updateLastAccessTime(fileId, targetType);

            dao.addExtractedText(fileId, targetType, "link to text");

            Assert.notEmpty(dao.find(fileId, targetType));
            Assert.notEmpty(dao.findByRemoteId("remote file id"));

            String otherFileId = "file2";

            ConvertErrorArgs errorArgs = ConvertErrorArgs.builder()
                    .fileId(otherFileId)
                    .targetType(targetType)
                    .errorCode(ErrorCode.UNKNOWN_CONVERT_ERROR)
                    .error("some error")
                    .failedAttemptsCount(1)
                    .packageVersion("some version")
                    .build();

            dao.saveOrUpdateResult(errorArgs);

            Assert.notEmpty(dao.find(otherFileId, targetType));
        });
    }

    private void doWithEmptyTable(Function1V<YdbStoredResultDao> action) {
        AliveAppInfo aliveAppInfo = Mockito.mock(AliveAppInfo.class);
        Mockito.when(aliveAppInfo.getVersion()).thenReturn("test version");

        YdbTestUtils.doWithTable(tm -> new YdbStoredResultDao(tm, aliveAppInfo, Duration.standardHours(1)),
                dao -> action.apply((YdbStoredResultDao) dao));
    }
}
