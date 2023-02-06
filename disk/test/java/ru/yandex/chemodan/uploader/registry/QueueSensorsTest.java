package ru.yandex.chemodan.uploader.registry;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.test.ReflectionUtils;
import ru.yandex.chemodan.uploader.ChemodanService;
import ru.yandex.chemodan.uploader.registry.record.MpfsRequestRecord;
import ru.yandex.chemodan.uploader.test.TestMpfsRequestRecordCreator;
import ru.yandex.commune.uploader.registry.UploadRegistry;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.test.TestBase;

/**
 * @author akirakozov
 */
public class QueueSensorsTest extends TestBase {

    private final TestMpfsRequestRecordCreator recordCreator = new TestMpfsRequestRecordCreator();
    private QueueSensors sut;

    @Mock
    private UploadRegistry<MpfsRequestRecord> uploadRegistry;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        sut = new QueueSensors(uploadRegistry);
    }

    @Test
    public void uploadFromServiceCount() {
        ListF<MpfsRequestRecord> records = Cf.list(
                createUploadFromServiceRecord(ChemodanService.VKONTAKTE),
                createUploadFromServiceRecord(ChemodanService.VKONTAKTE),
                createUploadFromServiceRecord(ChemodanService.FACEBOOK));

        Mockito.when(uploadRegistry.findRecordsInProgress(Mockito.eq(Option.empty())))
                .thenReturn(records);

        Assert.equals(2, sut.uploadFromServiceIncompleteCount(ChemodanService.VKONTAKTE.name()));
        Assert.equals(1, sut.uploadFromServiceIncompleteCount(ChemodanService.FACEBOOK.name()));
        Assert.equals(0, sut.uploadFromServiceIncompleteCount(ChemodanService.MAILRU.name()));
        Assert.equals(3, sut.uploadFromServiceIncompleteCount());
    }

    @Test
    public void exportPhotosCount() {
        ListF<MpfsRequestRecord> records = Cf.list(
                createExportPhotosRecord(ChemodanService.VKONTAKTE),
                createExportPhotosRecord(ChemodanService.FACEBOOK),
                createExportPhotosRecord(ChemodanService.FACEBOOK));

        Mockito.when(uploadRegistry.findRecordsInProgress(Mockito.eq(Option.empty())))
                .thenReturn(records);

        Assert.equals(1, sut.exportPhotosCountIncompleteCount(ChemodanService.VKONTAKTE.name()));
        Assert.equals(2, sut.exportPhotosCountIncompleteCount(ChemodanService.FACEBOOK.name()));
        Assert.equals(0, sut.exportPhotosCountIncompleteCount(ChemodanService.MAILRU.name()));
        Assert.equals(3, sut.exportPhotosCountIncompleteCount());
    }

    private MpfsRequestRecord createUploadFromServiceRecord(ChemodanService service) {
        MpfsRequestRecord.UploadFromService record = recordCreator.generateRandomUploadFromServiceRecord();
        ReflectionUtils.setFieldNewValue("sourceService", record.getRequest(), service);
        return record;
    }

    private MpfsRequestRecord createExportPhotosRecord(ChemodanService service) {
        MpfsRequestRecord.ExportPhotos record = recordCreator.generateRandomExportPhotosRecord();
        ReflectionUtils.setFieldNewValue("targetService", record.getRequest(), service);
        return record;
    }
}
