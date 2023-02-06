package ru.yandex.chemodan.uploader.registry.record;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.commune.uploader.util.UploaderJson;
import ru.yandex.misc.io.cl.ClassLoaderUtils;

/**
 * This test just checks that records parsing doesn't fail
 * @author akirakozov
 */
public class MpfsRecordParseTest {

    @Test
    public void parseUploadFromService1() {
        String data = ClassLoaderUtils.streamSourceForResource(
                MpfsRecordParseTest.class, "uploadFromService.js").readText();
        UploaderJson.read(MpfsRequestRecord.UploadFromService.class, data);
    }

    @Test
    public void parsePatchAtDefault1() {
        String data = ClassLoaderUtils.streamSourceForResource(
                MpfsRecordParseTest.class, "patchAtDefault.js").readText();
        UploaderJson.read(MpfsRequestRecord.PatchAtDefault.class, data);
    }

    @Test
    public void parseUploadToDefault() {
        String data1 = ClassLoaderUtils.streamSourceForResource(
                MpfsRecordParseTest.class, "uploadToDefault.js").readText();
        MpfsRequestRecord.UploadToDefault record1 = UploaderJson.read(MpfsRequestRecord.UploadToDefault.class, data1);
        Assert.assertEquals(Long.valueOf(1278), record1.getStatus().postProcess.digestCalculationStatus.get().getResultO().get().getSize().get());

        String data2 = ClassLoaderUtils.streamSourceForResource(
                MpfsRecordParseTest.class, "uploadToDefaultWithTempFailedDigest.js").readText();
        MpfsRequestRecord.UploadToDefault record2 = UploaderJson.read(MpfsRequestRecord.UploadToDefault.class, data2);

        Assert.assertFalse(record2.getStatus().postProcess.digestCalculationStatus.get().isFinished());
        Assert.assertFalse(record2.getStatus().postProcess.digestMulcaUploadInfo.get().isFinished());
    }

    @Test
    public void parseExtractArchive() {
        String data = ClassLoaderUtils.streamSourceForResource(
                MpfsRecordParseTest.class, "extractArchive.js").readText();
        UploaderJson.read(MpfsRequestRecord.ExtractArchive.class, data);
    }

    @Test
    public void parseGeneratePreview() {
        String data = ClassLoaderUtils.streamSourceForResource(
                MpfsRecordParseTest.class, "generatePreview.js").readText();
        UploaderJson.read(MpfsRequestRecord.GeneratePreview.class, data);
    }
}
