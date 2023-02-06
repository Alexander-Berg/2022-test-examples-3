package ru.yandex.chemodan.uploader.log;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import ru.yandex.bolts.collection.MapF;
import ru.yandex.chemodan.mpfs.MpfsHid;
import ru.yandex.chemodan.util.tskv.TskvUtils;
import ru.yandex.commune.uploader.registry.UploadRequestId;
import ru.yandex.misc.dataSize.DataSize;
import ru.yandex.misc.test.Assert;

/**
 * @author akirakozov
 */
public class ZipStreamStatLoggerTest {

    @Test
    public void formatLogMessage() throws Exception {
        DateTime dt = new DateTime(2014, 5, 4, 14, 20, 0, DateTimeZone.getDefault());
        String result = ZipStreamStatLogger.formatLogMessage(
                UploadRequestId.valueOf("123"), dt, new MpfsHid("aaaa"), "bbbb.mp3", "audio", true, DataSize.fromBytes(19));

        MapF<String, String> tskv = TskvUtils.extractTskv(result);

        Assert.some("123", tskv.getO("id"));
        Assert.some("ydisk-uploader-zip-traffic-log", tskv.getO("tskv_format"));
        Assert.some("2014-05-04 14:20:00", tskv.getO("timestamp"));
        Assert.some("aaaa", tskv.getO("hid"));
        Assert.some("bbbb.mp3", tskv.getO("name"));
        Assert.some("audio", tskv.getO("media-type"));
        Assert.some("true", tskv.getO("auth"));
        Assert.some("19", tskv.getO("bytes"));
    }
}
