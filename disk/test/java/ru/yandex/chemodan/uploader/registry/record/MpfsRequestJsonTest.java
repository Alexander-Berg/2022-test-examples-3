package ru.yandex.chemodan.uploader.registry.record;

import java.net.URI;

import org.joda.time.Instant;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.uploader.ChemodanFile;
import ru.yandex.chemodan.uploader.ChemodanService;
import ru.yandex.chemodan.uploader.ExtractedFile;
import ru.yandex.chemodan.uploader.UidOrSpecial;
import ru.yandex.chemodan.uploader.registry.ApiVersion;
import ru.yandex.chemodan.uploader.registry.record.status.ExifInfo.GeoCoords;
import ru.yandex.chemodan.uploader.registry.record.status.UploadedMulcaFile;
import ru.yandex.chemodan.uploader.services.ServiceFileId;
import ru.yandex.chemodan.uploader.services.ServiceImageInfo;
import ru.yandex.chemodan.uploader.services.ServiceIncomingFile;
import ru.yandex.commune.archive.ArchiveEntry;
import ru.yandex.commune.archive.ArchiveListing;
import ru.yandex.commune.uploader.registry.RequestMeta;
import ru.yandex.commune.uploader.registry.RequestRevision;
import ru.yandex.commune.uploader.registry.UploadRequestId;
import ru.yandex.commune.uploader.util.HostInstant;
import ru.yandex.commune.uploader.util.UploaderJson;
import ru.yandex.commune.uploader.util.http.ContentInfo;
import ru.yandex.commune.uploader.util.http.IncomingFile;
import ru.yandex.inside.mulca.MulcaId;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.dataSize.DataSize;
import ru.yandex.misc.io.file.File2;
import ru.yandex.misc.io.http.UrlUtils;
import ru.yandex.misc.xml.dom4j.Dom4jUtils;

/**
 * @author vavinov
 * @author alexm
 */
public class MpfsRequestJsonTest {
    private static final Option<DataSize> maxSize = Option.of(DataSize.MEGABYTE);
    private static final Option<URI> callback = Option.of(UrlUtils.uri("http://localhost"));
    private static final Option<Boolean> disableRetries = Option.of(false);
    private static final Option<Boolean> disableRedirects = Option.of(false);
    private static final RequestMeta requestMeta = new RequestMeta(UploadRequestId.valueOf("sadf"), new Instant());
    private static final ChemodanFile chemodanFile =
            ChemodanFile.cons(UidOrSpecial.uid(PassportUid.cons(1)), "qwer", "/zxcv");
    private static final MulcaId mulcaId = MulcaId.fromSerializedString("fadsfads");
    private static final UploadedMulcaFile mulcaFile = new UploadedMulcaFile(new File2("/dev/null"), mulcaId);
    private static final IncomingFile incomingFile = new IncomingFile(Option.empty(), Option.empty(),
            new File2("/dev/null"));
    private static final ServiceIncomingFile serviceIncomingFile = new ServiceIncomingFile(incomingFile,
            Option.empty(), ChemodanService.DISK, Option.empty());
    private static final Option<String> yandexCloudRequestId = Option.of("123456");

    @Test
    public void serializeDeserialize() throws Exception {
        ListF<MpfsRequest> testData = Cf.arrayList();

        testData.add(new MpfsRequest.UploadToDefault(ApiVersion.V_0_2,
                chemodanFile, Option.empty(), maxSize, yandexCloudRequestId));

        testData.add(new MpfsRequest.PatchAtDefault(ApiVersion.V_0_2,
                mulcaId, "f3f2f23",
                chemodanFile, callback, maxSize, yandexCloudRequestId));

        testData.add(new MpfsRequest.PublishToService(ApiVersion.V_0_2,
                ChemodanService.FOTKI,
                mulcaId, chemodanFile,
                Dom4jUtils.readRootElement("<hello world='1' />".getBytes()),
                callback, maxSize, yandexCloudRequestId));

        testData.add(new MpfsRequest.UploadFromService(ApiVersion.V_0_2,
                ChemodanService.FOTKI,
                Option.of(ServiceFileId.valueOf("1341234:1234")),
                chemodanFile,
                callback,
                disableRetries,
                disableRedirects,
                maxSize,
                Option.empty(),
                Option.empty(),
                yandexCloudRequestId,
                Option.empty()));

        testData.add(new MpfsRequest.UploadFromService(ApiVersion.V_0_2,
                ChemodanService.FOTKI,
                Option.empty(),
                chemodanFile,
                callback,
                disableRetries,
                disableRedirects,
                maxSize,
                Option.of(new ServiceImageInfo(
                        Option.of(new GeoCoords(13.03, 54.02)),
                        "url",
                        Option.of(Instant.now())
                )),
                Option.empty(),
                yandexCloudRequestId,
                Option.empty())
        );

        testData.add(new MpfsRequest.UploadFromService(ApiVersion.V_0_2,
                ChemodanService.FOTKI,
                Option.empty(),
                chemodanFile,
                callback,
                disableRetries,
                disableRedirects,
                maxSize,
                Option.of(new ServiceImageInfo(
                        Option.of(new GeoCoords(13.03, 54.02)),
                        "url",
                        Option.of(Instant.now())
                )),
                Option.empty(),
                yandexCloudRequestId,
                Option.of(true))
        );

        testData.add(new MpfsRequest.ListArchive(ApiVersion.V_0_2,
                mulcaId,
                chemodanFile, callback, maxSize, yandexCloudRequestId));

        testData.add(new MpfsRequest.ExtractArchive(ApiVersion.V_0_2,
                ChemodanService.DISK,
                Option.empty(),
                Option.empty(),
                Option.of("bebe"),
                chemodanFile, callback, maxSize, yandexCloudRequestId));

        for (MpfsRequest r : testData) {
            String s = UploaderJson.write(r);
            UploaderJson.read(MpfsRequest.class, s);
        }
    }

    @Test
    public void uploadToDefault() throws Exception {
        MpfsRequestRecord.UploadToDefault r = new MpfsRequestRecord.UploadToDefault(
                requestMeta,
                HostInstant.hereAndNow(),
                RequestRevision.initial(HostInstant.hereAndNow()),
                new MpfsRequest.UploadToDefault(
                        ApiVersion.V_0_2,
                        chemodanFile,
                        callback,
                        maxSize,
                        yandexCloudRequestId));
        r.getStatus().payloadInfo.set(r.getStatus().payloadInfo.get().asInitial().get()
                .start(new Instant(), maxSize, new ContentInfo(
                        Option.of("string-inside-option"), 13L, "asdf", "asdf")));
        String s = UploaderJson.write(r);
        UploaderJson.read(MpfsRequestRecord.class, s);
    }

    @Test
    public void listArchive() throws Exception {
        MpfsRequestRecord.ListArchive r = new MpfsRequestRecord.ListArchive(
                requestMeta,
                HostInstant.hereAndNow(),
                RequestRevision.initial(HostInstant.hereAndNow()),
                new MpfsRequest.ListArchive(
                        ApiVersion.V_0_2,
                        mulcaId,
                        chemodanFile,
                        callback,
                        maxSize,
                        yandexCloudRequestId));
        r.getStatus().originalFile.set(r.getStatus().originalFile.get().asInitial().get()
                .start(new Instant(), maxSize, mulcaFile));
        r.getStatus().listing.set(r.getStatus().listing.get().asInitial().get()
                .start(new Instant(), maxSize, new ArchiveListing("zip", Cf.list(
                        new ArchiveEntry("foo/bar", Option.of(666L), false, false)))));
        String s = UploaderJson.write(r);
        UploaderJson.read(MpfsRequestRecord.class, s);
    }

    @Test
    public void extractArchive() throws Exception {
        MpfsRequestRecord.ExtractArchive r = new MpfsRequestRecord.ExtractArchive(
                requestMeta,
                HostInstant.hereAndNow(),
                RequestRevision.initial(HostInstant.hereAndNow()),
                new MpfsRequest.ExtractArchive(
                        ApiVersion.V_0_2,
                        ChemodanService.DISK,
                        Option.empty(),
                        Option.empty(),
                        Option.of("foo/bar"),
                        chemodanFile,
                        callback,
                        maxSize,
                        yandexCloudRequestId));
        r.getStatus().downloadedFileFromService2.set(r.getStatus().downloadedFileFromService2.get().asInitial().get()
                .start(new Instant(), maxSize, serviceIncomingFile));
        ListF<ExtractedFile> extractedFiles = Cf.list(new ExtractedFile(
                        new ArchiveEntry("foo/bar", Option.of(1L), false, false), new File2("/dev/null")));
        r.getStatus().extractedFiles.set(r.getStatus().extractedFiles.get().asInitial().get()
                .start(new Instant(), maxSize, extractedFiles));
        r.getStatus().multipleUpload.fill(chemodanFile.getUidOrSpecial(), chemodanFile.getPath(), extractedFiles);
        String s = UploaderJson.write(r);
        UploaderJson.read(MpfsRequestRecord.class, s);
    }
}
