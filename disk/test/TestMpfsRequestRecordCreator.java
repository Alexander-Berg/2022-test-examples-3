package ru.yandex.chemodan.uploader.test;

import org.joda.time.Instant;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.function.Function0;
import ru.yandex.chemodan.test.CustomRandomValueGenerator;
import ru.yandex.chemodan.uploader.UidOrSpecial;
import ru.yandex.chemodan.uploader.registry.record.MpfsRequest;
import ru.yandex.chemodan.uploader.registry.record.MpfsRequestRecord;
import ru.yandex.chemodan.uploader.registry.record.MpfsRequestRecordUtils;
import ru.yandex.commune.random.RandomValueGenerator;
import ru.yandex.commune.uploader.registry.RequestMeta;
import ru.yandex.commune.uploader.registry.RequestRevision;
import ru.yandex.commune.uploader.registry.UploadRequestId;
import ru.yandex.commune.uploader.util.HostInstant;
import ru.yandex.misc.random.Random2;
import ru.yandex.misc.reflection.ClassX;

/**
 * @author akirakozov
 */
public class TestMpfsRequestRecordCreator {
    private static final RandomValueGenerator generator = new CustomRandomValueGenerator(true,
            Cf.map(ClassX.wrap(UidOrSpecial.class).uncheckedCast(), uidOrSpecialGeneratorF()));

    public MpfsRequestRecord.UploadToDefault generateRandomUploadToDefaultRecord() {
        MpfsRequest.UploadToDefault record = generator.randomValue(MpfsRequest.UploadToDefault.class);
        return MpfsRequestRecordUtils.consF(record, initialRevision()).apply(createMeta(), HostInstant.hereAndNow());
    }

    public MpfsRequestRecord.UploadFromService generateRandomUploadFromServiceRecord() {
        MpfsRequest.UploadFromService record = generator.randomValue(MpfsRequest.UploadFromService.class);
        return MpfsRequestRecordUtils.consF(record, initialRevision()).apply(createMeta(), HostInstant.hereAndNow());
    }

    public MpfsRequestRecord.ExportPhotos generateRandomExportPhotosRecord() {
        MpfsRequest.ExportPhotos record = generator.randomValue(MpfsRequest.ExportPhotos.class);
        return MpfsRequestRecordUtils.consF(record, initialRevision()).apply(createMeta(), HostInstant.hereAndNow());
    }

    private RequestRevision initialRevision() {
        return RequestRevision.initial(HostInstant.hereAndNow());
    }

    private RequestMeta createMeta() {
        return new RequestMeta(UploadRequestId.valueOf(Random2.R.nextAlnum(6)), new Instant());
    }

    private static Function0<Object> uidOrSpecialGeneratorF() {
        return new Function0<Object>() {
            public Object apply() {
                return UidOrSpecial.special(Random2.R.nextAlnum(8));
            }
        };
    }
}
