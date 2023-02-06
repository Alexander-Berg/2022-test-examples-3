package ru.yandex.chemodan.uploader.registry;

import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.message.BasicHttpResponse;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.function.Function1V;
import ru.yandex.chemodan.uploader.registry.record.status.ExifInfo;
import ru.yandex.chemodan.uploader.registry.record.status.ExifInfo.GeoCoords;
import ru.yandex.chemodan.uploader.services.ServiceImageInfo;
import ru.yandex.commune.uploader.local.file.LocalFileManager;
import ru.yandex.commune.uploader.registry.UploadRequestId;
import ru.yandex.misc.io.InputStreamSource;
import ru.yandex.misc.io.cl.ClassLoaderUtils;
import ru.yandex.misc.io.file.File2;
import ru.yandex.misc.test.Assert;

/**
 * @author nshmakov
 */
public class StagesTest {

    private final Stages sut = new Stages();

    private final InputStreamSource image = ClassLoaderUtils.streamSourceForResource(
            "ru/yandex/chemodan/uploader/preview/cat.jpg");

    @Before
    public void init() {
        sut.setLocalFileManager(new LocalFileManager(File2.createNewTmpDir(), false, false));
        sut.setExtractExifEnabled();
    }

    @Test
    public void shouldUseSpecifiedParametersInsteadOfExif() {
        GeoCoords location = new GeoCoords(10, 20);
        Option<Instant> creationTime = Option.of(new Instant(123123123));
        String serviceFileUrl = "http://url.jpg";
        ServiceImageInfo info = new ServiceImageInfo(Option.of(location), serviceFileUrl, creationTime);

        ExifInfo result = sut.extractExifInfoF(UploadRequestId.valueOf("1"), image, Option.of(info), "image/jpg",
                Function1V.nop())
                .apply()
                .getResult();

        Assert.equals(location, result.getGeoCoords().get());
        Assert.equals(creationTime, result.getCreationDate());
    }

    @Test(expected = MediaTypeNotSupportedException.class)
    public void throwMediaTypeNotSupportedExceptionIfAvatarResponse415() {
        HttpResponse response = new BasicHttpResponse(new ProtocolVersion("",0,0), 415, "reason");
        sut.handleAvatarsResponse(response, null);
    }
}

