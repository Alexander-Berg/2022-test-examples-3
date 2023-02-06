package ru.yandex.chemodan.uploader.status.strategy;

import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.uploader.ChemodanFile;
import ru.yandex.chemodan.uploader.UidOrSpecial;
import ru.yandex.chemodan.uploader.registry.ApiVersion;
import ru.yandex.chemodan.uploader.registry.record.MpfsRequest;
import ru.yandex.chemodan.uploader.registry.record.MpfsRequestRecord;
import ru.yandex.chemodan.uploader.registry.record.status.MpfsRequestStatus;
import ru.yandex.chemodan.uploader.registry.record.status.PostProcessStatus;
import ru.yandex.commune.uploader.registry.RequestMeta;
import ru.yandex.commune.uploader.registry.RequestRevision;
import ru.yandex.commune.uploader.registry.State;
import ru.yandex.commune.uploader.registry.UploadRegistry;
import ru.yandex.commune.uploader.registry.UploadRequestId;
import ru.yandex.commune.uploader.util.DataProgress;
import ru.yandex.commune.uploader.util.HostInstant;
import ru.yandex.commune.uploader.util.http.IncomingFile;
import ru.yandex.misc.dataSize.DataSize;
import ru.yandex.misc.io.file.File2;
import ru.yandex.misc.ip.Host;
import ru.yandex.misc.test.Assert;

import static org.mockito.Mockito.when;

/**
 * @author nshmakov
 */
public class ContentBasedStrategyTest {

    private ContentBasedStrategy sut = new ContentBasedStrategy();

    @Mock
    private UploadRegistry<MpfsRequestRecord> uploadRegistryMock;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        sut.setUploadRegistry(uploadRegistryMock);
        sut.setDefaultContentLength(1);
        sut.setDefaultCoef(2);
        sut.setRegularFileCoef(3);
        sut.setImageCoef(4);
        sut.setVideoCoef(5);
    }

    @Test
    public void shouldComputeLoadingStatusForVideoFile() {
        ListF<MpfsRequestRecord> records = Cf.list(mpfsRecord(Option.of("video/mpeg"), dataSize(1000)));
        when(uploadRegistryMock.findRecordsInProgress(Option.empty())).thenReturn(records);

        long actual = sut.compute();

        Assert.equals(5000L, actual);
    }

    @Test
    public void shouldComputeLoadingStatusForImageFile() {
        ListF<MpfsRequestRecord> records = Cf.list(mpfsRecord(Option.of("image/jpeg"), dataSize(1000)));
        when(uploadRegistryMock.findRecordsInProgress(Option.empty())).thenReturn(records);

        long actual = sut.compute();

        Assert.equals(4000L, actual);
    }

    @Test
    public void shouldComputeLoadingStatusForTwoFiles() {
        ListF<MpfsRequestRecord> records = Cf.list(
                mpfsRecord(Option.of("video/mpeg"), dataSize(1000)), mpfsRecord(Option.of("image/jpeg"), dataSize(500)));
        when(uploadRegistryMock.findRecordsInProgress(Option.empty())).thenReturn(records);

        long actual = sut.compute();

        Assert.equals(7000L, actual);
    }

    @Test
    public void shouldComputeZeroLoadingStatusWhenInactive() {
        when(uploadRegistryMock.findRecordsInProgress(Option.empty())).thenReturn(Cf.list());

        long actual = sut.compute();

        Assert.equals(0L, actual);
    }

    @Test
    public void shouldComputeZeroLoadingStatusWhenFileStateIsEmpty() {
        ListF<MpfsRequestRecord> records = Cf.list(mpfsRecord(new MpfsRequestStatus.UploadToDefault()));
        when(uploadRegistryMock.findRecordsInProgress(Option.empty())).thenReturn(records);

        long actual = sut.compute();

        Assert.equals(0L, actual);
    }

    @Test
    public void shouldUseEmptyFileCoefWhenFileSizeIsEmpty() {
        ListF<MpfsRequestRecord> records = Cf.list(mpfsRecord(Option.of("video/mpeg"), Option.empty()));
        when(uploadRegistryMock.findRecordsInProgress(Option.empty())).thenReturn(records);

        long actual = sut.compute();

        Assert.equals(5L, actual);
    }

    @Test
    public void shouldUseDefaultCoefWhenContentTypeIsEmpty() {
        ListF<MpfsRequestRecord> records = Cf.list(mpfsRecord(Option.empty(), dataSize(100)));
        when(uploadRegistryMock.findRecordsInProgress(Option.empty())).thenReturn(records);

        long actual = sut.compute();

        Assert.equals(200L, actual);
    }

    @Test
    public void shouldUseRegularFileCoefWhenContentTypeIsUnknown() {
        ListF<MpfsRequestRecord> records = Cf.list(mpfsRecord(Option.of("application/x-protobuf"), dataSize(400)));
        when(uploadRegistryMock.findRecordsInProgress(Option.empty())).thenReturn(records);

        long actual = sut.compute();

        Assert.equals(1200L, actual);
    }

    private MpfsRequestRecord mpfsRecord(Option<String> contentType, Option<DataSize> fileSize) {
        MpfsRequestStatus.UploadToDefault status = new MpfsRequestStatus.UploadToDefault(
                new State.InProgress<>(0, Instant.now(), new DataProgress(DataSize.fromBytes(0), fileSize),
                        Instant.now(), new IncomingFile(contentType, fileSize, new File2("/file"))),
                State.initial(), new PostProcessStatus(), State.initial(), State.initial(), State.initial());

        return mpfsRecord(status);
    }

    private MpfsRequestRecord mpfsRecord(MpfsRequestStatus.UploadToDefault status) {
        Instant now = Instant.now();
        RequestMeta meta = new RequestMeta(UploadRequestId.valueOf("reqId"), now);
        RequestRevision requestRevision = new RequestRevision(1, new HostInstant(Host.parse("host"), now));
        ChemodanFile chemodanFile = new ChemodanFile(UidOrSpecial.special("special"), "field", "/path");

        MpfsRequest.UploadToDefault req = new MpfsRequest.UploadToDefault(
                ApiVersion.V_0_2, chemodanFile, Option.empty(), Option.empty(), Option.empty());

        return new MpfsRequestRecord.UploadToDefault(meta, requestRevision, Option.empty(), req, status);
    }

    private Option<DataSize> dataSize(int size) {
        return Option.of(DataSize.fromBytes(size));
    }
}
