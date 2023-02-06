package ru.yandex.chemodan.uploader.log;


import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.junit.Test;

import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.test.TestHelper;
import ru.yandex.chemodan.uploader.ChemodanFile;
import ru.yandex.chemodan.uploader.UidOrSpecial;
import ru.yandex.chemodan.uploader.registry.ApiVersion;
import ru.yandex.chemodan.uploader.registry.record.MpfsRequest;
import ru.yandex.chemodan.uploader.registry.record.MpfsRequestRecord;
import ru.yandex.chemodan.uploader.registry.record.status.MpfsRequestStatus;
import ru.yandex.chemodan.util.tskv.TskvUtils;
import ru.yandex.commune.uploader.registry.RequestMeta;
import ru.yandex.commune.uploader.registry.RequestRevision;
import ru.yandex.commune.uploader.registry.UploadRequestId;
import ru.yandex.commune.uploader.util.HostInstant;
import ru.yandex.commune.uploader.util.http.IncomingFile;
import ru.yandex.commune.uploader.util.http.PutResult;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.dataSize.DataSize;
import ru.yandex.misc.io.file.File2;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.test.TestBase;

/**
 * @author akirakozov
 */
public class UploadStatisticLoggerTest extends TestBase {

    @Test
    public void logUpload() {
        TestHelper.initialize();
        ChemodanFile chemodanFile = new ChemodanFile(UidOrSpecial.uid(new PassportUid(123L)), "uniqF", "/a/b/c");
        MpfsRequest.UploadToDefault req = new MpfsRequest.UploadToDefault(
                ApiVersion.V_0_2, chemodanFile,
                Option.empty(),
                Option.empty(),
                Option.empty());

        MpfsRequestStatus.UploadToDefault status = new MpfsRequestStatus.UploadToDefault();
        status.userFile.complete(
                new IncomingFile(Option.empty(), Option.of(DataSize.fromBytes(1023)), new File2("file")));

        MpfsRequestRecord record = new MpfsRequestRecord.UploadToDefault(
                new RequestMeta(UploadRequestId.valueOf("12345"), new Instant()),
                RequestRevision.initial(HostInstant.hereAndNow()),
                Option.empty(),
                req, status);
        DateTime dt = new DateTime(2014, 5, 4, 14, 20, 0, DateTimeZone.getDefault());

        System.out.println(UploadStatisticLogger.formatLogMessage(record, PutResult.COMPLETED, Option.of("curl 1.0"), dt));
        MapF<String, String> tskv
                = TskvUtils.extractTskv(UploadStatisticLogger.formatLogMessage(record, PutResult.COMPLETED, Option.of("curl 1.0"), dt));

        Assert.some("12345", tskv.getO("id"));
        Assert.some("ydisk-uploader-stat-log", tskv.getO("tskv_format"));
        Assert.some("UploadToDefault", tskv.getO("type"));
        Assert.some("2014-05-04 14:20:00", tskv.getO("timestamp"));
        Assert.some("123", tskv.getO("puid"));
        Assert.some("/a/b/c", tskv.getO("path"));
        Assert.some("1023", tskv.getO("content_length"));
        Assert.some("curl 1.0", tskv.getO("user-agent"));
        Assert.some("201", tskv.getO("status"));
    }
}
