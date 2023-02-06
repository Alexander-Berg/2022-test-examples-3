package ru.yandex.chemodan.uploader.registry.record.status;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.SetF;
import ru.yandex.inside.mulca.MulcaId;
import ru.yandex.misc.test.Assert;

import static ru.yandex.chemodan.uploader.test.DomainUtils.mid;
import static ru.yandex.chemodan.uploader.test.DomainUtils.uploadInfo;

/**
 * @author nshmakov
 */
public class PostProcessStatusTest {

    @Test
    public void getUploadedFilesMids() {
        MulcaId fileMid = mid(1);
        MulcaId digestMid = mid(2);
        MulcaId docPreviewMid = mid(3);
        MulcaId imgPreviewMid = mid(4);
        MulcaId videoPreviewMid = mid(5);
        SetF<MulcaId> expected = Cf.set(fileMid, digestMid, docPreviewMid, imgPreviewMid, videoPreviewMid);

        PostProcessStatus status = new PostProcessStatus();
        status.fileMulcaUploadInfo.complete(uploadInfo(fileMid));
        status.digestMulcaUploadInfo.complete(uploadInfo(digestMid));
        status.previewDocumentStatus.previewMulcaUploadInfo.complete(uploadInfo(docPreviewMid));
        status.previewImageStatus.previewMulcaUploadInfo.complete(uploadInfo(imgPreviewMid));
        status.previewVideoStatus.previewMulcaUploadInfo.complete(uploadInfo(videoPreviewMid));

        SetF<MulcaId> actual = Cf.set(status.getUploadedFilesMids());

        Assert.equals(expected, actual);
    }
}
