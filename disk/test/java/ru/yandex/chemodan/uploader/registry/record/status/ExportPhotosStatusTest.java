package ru.yandex.chemodan.uploader.registry.record.status;

import org.junit.Test;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.commune.uploader.registry.RequestStatusUtils;
import ru.yandex.commune.uploader.registry.StatusFieldInfo;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.test.TestBase;

/**
 * @author akirakozov
 */
public class ExportPhotosStatusTest extends TestBase {

    @Test
    public void checkStatusFields() {
        MpfsRequestStatus.ExportPhotos status = new MpfsRequestStatus.ExportPhotos(1);

        ListF<StatusFieldInfo> fieldsInfo = RequestStatusUtils.statusFields(status);
        Assert.sizeIs(7, fieldsInfo);
        Assert.some(fieldsInfo.find(StatusFieldInfo.getNameF().andThenEquals("mep.p[0].uploadInfo")));
    }
}
