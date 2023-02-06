package ru.yandex.chemodan.uploader.registry;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.function.Function;
import ru.yandex.chemodan.test.ReflectionUtils;
import ru.yandex.chemodan.uploader.ChemodanService;
import ru.yandex.chemodan.uploader.registry.record.MpfsRequestRecord;
import ru.yandex.chemodan.uploader.registry.record.MpfsRequestRecord.UploadToDefault;
import ru.yandex.chemodan.uploader.test.TestMpfsRequestRecordCreator;
import ru.yandex.commune.uploader.registry.CallbackResponseOption;
import ru.yandex.commune.uploader.util.http.IncomingFile;
import ru.yandex.misc.io.file.File2;
import ru.yandex.misc.test.Assert;


/**
 * @author akirakozov
 */
public class RequestRecordFilterTest {
    private final TestMpfsRequestRecordCreator creator = new TestMpfsRequestRecordCreator();
    private final RequestRecordFilter filter = new RequestRecordFilter();

    private final IncomingFile DUMMY_INCOMING_FILE = new IncomingFile(
            Option.empty(), Option.empty(), new File2("test"));

    @Test
    public void filterIncompleteRequestsWaitingForUser() {
        ListF<MpfsRequestRecord> records = createUploadToDefaultRecords(4);

        Assert.hasSize(4, filter.incompleteRequestsWaitingForUser(records));

        ((UploadToDefault) records.get(0)).getStatus().userFile.complete(DUMMY_INCOMING_FILE);
        ((UploadToDefault) records.get(3)).getStatus().userFile.complete(DUMMY_INCOMING_FILE);

        Assert.hasSize(2, filter.incompleteRequestsWaitingForUser(records));
    }

    @Test
    public void filterIncompleteAndUploadedToMulca() {
        ListF<MpfsRequestRecord> records = createUploadToDefaultRecords(5);

        ((UploadToDefault) records.get(0)).getStatus().userFile.complete(DUMMY_INCOMING_FILE);
        ((UploadToDefault) records.get(3)).getStatus().postProcess.commitFileUpload.complete(CallbackResponseOption.none());
        ((UploadToDefault) records.get(4)).getStatus().postProcess.commitFileUpload.complete(CallbackResponseOption.none());

        Assert.hasSize(2, filter.incompleteAndUploadedToMulca(records));
    }

    @Test
    public void filterIncompleteAndUploadedLocallyAndNotUploadedToMulca() {
        ListF<MpfsRequestRecord> records = createUploadToDefaultRecords(5);

        ((UploadToDefault) records.get(0)).getStatus().userFile.complete(DUMMY_INCOMING_FILE);
        ((UploadToDefault) records.get(1)).getStatus().userFile.complete(DUMMY_INCOMING_FILE);
        ((UploadToDefault) records.get(3)).getStatus().userFile.complete(DUMMY_INCOMING_FILE);
        ((UploadToDefault) records.get(4)).getStatus().userFile.complete(DUMMY_INCOMING_FILE);
        ((UploadToDefault) records.get(3)).getStatus().postProcess.commitFileUpload.complete(CallbackResponseOption.none());
        ((UploadToDefault) records.get(4)).getStatus().postProcess.commitFileUpload.complete(CallbackResponseOption.none());

        Assert.hasSize(2, filter.incompleteAndUploadedLocallyAndNotUploadedToMulca(records));
    }

    @Test
    public void uploadFromServiceCount() {
        ListF<MpfsRequestRecord> records = Cf.list(
                createUploadFromServiceRecord(ChemodanService.VKONTAKTE),
                createUploadFromServiceRecord(ChemodanService.VKONTAKTE),
                createUploadFromServiceRecord(ChemodanService.FACEBOOK));

        Assert.hasSize(2, filter.uploadFromServiceIncomplete(records, ChemodanService.VKONTAKTE));
        Assert.hasSize(1, filter.uploadFromServiceIncomplete(records, ChemodanService.FACEBOOK));
        Assert.hasSize(0, filter.uploadFromServiceIncomplete(records, ChemodanService.MAILRU));
    }

    @Test
    public void exportPhotosCount() {
        ListF<MpfsRequestRecord> records = Cf.list(
                createExportPhotosRecord(ChemodanService.VKONTAKTE),
                createExportPhotosRecord(ChemodanService.FACEBOOK),
                createExportPhotosRecord(ChemodanService.FACEBOOK));

        Assert.hasSize(1, filter.exportPhotosCountIncomplete(records, ChemodanService.VKONTAKTE));
        Assert.hasSize(2, filter.exportPhotosCountIncomplete(records, ChemodanService.FACEBOOK));
        Assert.hasSize(0, filter.exportPhotosCountIncomplete(records, ChemodanService.MAILRU));
    }

    @Test
    public void invokeFilter() {
        ListF<MpfsRequestRecord> records = createUploadToDefaultRecords(2);

        Assert.hasSize(2, filter.invokeFilter("incompleteRequestsWaitingForUser", records, Cf.<String>list()));
        Assert.hasSize(0, filter.invokeFilter("exportPhotosCountIncomplete",
                records, Cf.<String>list(ChemodanService.VKONTAKTE.name())));
    }

    private MpfsRequestRecord createUploadFromServiceRecord(ChemodanService service) {
        MpfsRequestRecord.UploadFromService record = creator.generateRandomUploadFromServiceRecord();
        ReflectionUtils.setFieldNewValue("sourceService", record.getRequest(), service);
        return record;
    }

    private MpfsRequestRecord createExportPhotosRecord(ChemodanService service) {
        MpfsRequestRecord.ExportPhotos record = creator.generateRandomExportPhotosRecord();
        ReflectionUtils.setFieldNewValue("targetService", record.getRequest(), service);
        return record;
    }

    private ListF<MpfsRequestRecord> createUploadToDefaultRecords(int num) {
        return Cf.range(0, num).map(
                new Function<Integer, MpfsRequestRecord>() {
                    public MpfsRequestRecord apply(Integer a) {
                        return creator.generateRandomUploadToDefaultRecord();
                    }
                });
    }
}
