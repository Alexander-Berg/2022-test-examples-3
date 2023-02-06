package ru.yandex.chemodan.uploader.registry.record.status;

import org.junit.BeforeClass;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.commune.uploader.registry.RequestStatusRegistry;
import ru.yandex.commune.uploader.registry.RequestStatusUtils;
import ru.yandex.commune.uploader.registry.StatusFieldInfo;
import ru.yandex.commune.uploader.registry.StatusFieldsNames;
import ru.yandex.commune.uploader.registry.UploadRequestBaseStatus;
import ru.yandex.misc.io.cl.ClassLoaderUtils;
import ru.yandex.misc.lang.Check;
import ru.yandex.misc.reflection.ClassX;
import ru.yandex.misc.test.Assert;

/**
 * @author ssytnik
 * @author alexm
 */
public class MpfsRequestStatusTest {

    @BeforeClass
    public static void beforeClass() {
        MpfsRequestStatus.registerStatuses();
    }

    @Test
    public void validateFieldsConsistency() throws Exception {
        for (ClassX<? extends UploadRequestBaseStatus> c : RequestStatusRegistry.getStatusClasses()) {
            MpfsRequestStatus status = (MpfsRequestStatus)c.newInstance();
            if (status instanceof MpfsRequestStatus.ExtractArchive) {
                continue; // dynamic post-process
            }
            Check.C.equals(
                    status.isUploadingToDisk(),
                    status.getPostProcessStatusO().isPresent(),
                    "Validation has failed for " + c.getSimpleName());
        }
    }

    @Test
    public void statusList() {
        // status list from help servlet http://kladun2g.dev.mail.yandex.net:8080/help
        ListF<String> ethalon = ClassLoaderUtils.streamSourceForResource(getClass(), "statusList").readLines();
        ListF<String> sn = StatusFieldsNames.getSimpleStatusNames()
                .map(s -> s.split("\\.")[0]).unique().toList().sorted();
        Assert.assertListsEqual(ethalon, sn);
    }

    @Test
    public void stageNames() {
        MpfsRequestStatus status = new MpfsRequestStatus.GeneratePreview();
        Assert.equals(
                Cf.list("notCancelledByAdmin", "internalError", "originalFile", "generatePreview", "commitFinal"),
                RequestStatusUtils.statusFields(status).map(StatusFieldInfo.getNameF()));
    }

    @Test
    public void nestedStageNames() {
        MpfsRequestStatus status = new MpfsRequestStatus.UploadToDefault();
        ListF<String> names = RequestStatusUtils.statusFields(status).map(StatusFieldInfo.getNameF());
        Assert.in("payloadInfo", names);
        Assert.in("pp.pd.generatePreview", names);
    }

}
