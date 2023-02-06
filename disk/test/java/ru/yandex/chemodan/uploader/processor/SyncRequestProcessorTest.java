package ru.yandex.chemodan.uploader.processor;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.function.Function0;
import ru.yandex.chemodan.uploader.UidOrSpecial;
import ru.yandex.chemodan.uploader.av.AntivirusResult;
import ru.yandex.chemodan.uploader.mulca.MulcaUploadInfo;
import ru.yandex.chemodan.uploader.processor.SyncRequestProcessor.ExtractedExifData;
import ru.yandex.chemodan.uploader.registry.Stages;
import ru.yandex.chemodan.uploader.registry.record.status.UploadedMulcaFile;
import ru.yandex.chemodan.uploader.stage.SimpleStageProcessor;
import ru.yandex.commune.uploader.registry.StageResult;
import ru.yandex.inside.mulca.MulcaId;
import ru.yandex.misc.io.InputStreamSource;
import ru.yandex.misc.io.file.File2;
import ru.yandex.misc.test.Assert;

/**
 * @author akirakozov
 */
public class SyncRequestProcessorTest {

    private SyncRequestProcessor sut;

    @Mock
    private Stages stages;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        sut = new SyncRequestProcessor(stages, new SimpleStageProcessor());
    }

    @Test
    public void extractExifAndUploadToMulca() {
        final MulcaId sourceMulcaId = MulcaId.fromSerializedString("mulca-id");
        final File2 destinationFile = new File2("result");
        final String fullExif = "fullExif";

        Mockito.when(stages.downloadFromMulcaF(Mockito.eq(sourceMulcaId), Mockito.any(File2.class))).thenReturn(
                Function0.constF(new UploadedMulcaFile(destinationFile, sourceMulcaId)));

        Mockito.when(stages.extractFullExifInJsonF(destinationFile)).thenReturn(Function0.constF(fullExif));

        MulcaId exifMulcaId = MulcaId.fromSerializedString("exif-mulca-id");
        Mockito.when(stages.uploadStringToMulcaF(fullExif, UidOrSpecial.special("exif"))).thenReturn(
                Function0.constF(new MulcaUploadInfo(exifMulcaId, Option.empty())));

        ExtractedExifData exifData = sut.extractExifAndUploadToMulca(sourceMulcaId);
        Assert.equals(exifMulcaId, exifData.mulcaId);
        Assert.equals(fullExif, exifData.exifInJson);
    }

    @Test
    public void extractExifAndUploadToMulcaFromFileWithoutExif() {
        final MulcaId sourceMulcaId = MulcaId.fromSerializedString("mulca-id");
        final File2 destinationFile = new File2("result");

        Mockito.when(stages.downloadFromMulcaF(Mockito.eq(sourceMulcaId), Mockito.any(File2.class))).thenReturn(
                Function0.constF(new UploadedMulcaFile(destinationFile, sourceMulcaId)));

        Mockito.when(stages.extractFullExifInJsonF(destinationFile)).thenReturn(Function0.constF(""));

        ExtractedExifData exifData = sut.extractExifAndUploadToMulca(sourceMulcaId);

        Mockito.verify(stages, Mockito.never()).uploadToMulca(
                Mockito.any(UidOrSpecial.class), Mockito.any(InputStreamSource.class));

        Assert.equals(ExtractedExifData.EMPTY.mulcaId, exifData.mulcaId);
        Assert.equals(ExtractedExifData.EMPTY.exifInJson, exifData.exifInJson);
    }

    @Test
    public void checkWithAntivirus() {
        final MulcaId sourceMulcaId = MulcaId.fromSerializedString("mulca-id");
        final File2 destinationFile = new File2("result");

        Mockito.when(stages.downloadFromMulcaF(Mockito.eq(sourceMulcaId), Mockito.any(File2.class))).thenReturn(
                Function0.constF(new UploadedMulcaFile(destinationFile, sourceMulcaId)));

        Mockito.when(stages.checkWithAntivirusF(Mockito.any(Option.class), Mockito.eq(destinationFile))).thenReturn(
                Function0.<StageResult<AntivirusResult>>constF(StageResult.success(AntivirusResult.HEALTHY)));

        AntivirusResult result = sut.checkWithAntivirus(sourceMulcaId);

        Assert.equals(AntivirusResult.HEALTHY, result);
    }

}
