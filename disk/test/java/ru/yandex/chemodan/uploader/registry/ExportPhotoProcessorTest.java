package ru.yandex.chemodan.uploader.registry;

import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.boot.ChemodanInitContextConfiguration;
import ru.yandex.chemodan.uploader.ChemodanService;
import ru.yandex.chemodan.uploader.config.UploaderCoreContextConfigurationForTests;
import ru.yandex.chemodan.uploader.registry.processors.ExportPhotosProcessor;
import ru.yandex.chemodan.uploader.registry.record.MpfsRequest;
import ru.yandex.chemodan.uploader.registry.record.MpfsRequestRecord;
import ru.yandex.chemodan.uploader.registry.record.status.ExportPhotoStatus;
import ru.yandex.chemodan.uploader.social.SocialClientErrException;
import ru.yandex.chemodan.uploader.social.SocialProxyClient;
import ru.yandex.chemodan.uploader.social.SocialProxyRetriableException;
import ru.yandex.chemodan.uploader.social.SocialProxyStages;
import ru.yandex.chemodan.uploader.social.SocialTaskInfo;
import ru.yandex.chemodan.util.test.AbstractTest;
import ru.yandex.commune.uploader.registry.RequestMeta;
import ru.yandex.commune.uploader.registry.RequestRevision;
import ru.yandex.commune.uploader.registry.State;
import ru.yandex.commune.uploader.registry.UploadRequestId;
import ru.yandex.commune.uploader.registry.storage.RecordStorage;
import ru.yandex.commune.uploader.util.HostInstant;
import ru.yandex.inside.mulca.MulcaClient;
import ru.yandex.inside.mulca.MulcaId;
import ru.yandex.misc.io.ByteArrayInputStreamSource;
import ru.yandex.misc.io.http.Timeout;
import ru.yandex.misc.random.Random2;
import ru.yandex.misc.test.Assert;

/**
 * @author Vsevolod Tolstopyatov (qwwdfsad)
 */


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
        ChemodanInitContextConfiguration.class,
        UploaderCoreContextConfigurationForTests.class})
public class ExportPhotoProcessorTest extends AbstractTest {

    @Value("${uploader.export.vk.use.sleep:-true}")
    private boolean exportVkUseSleep;
    @Value("${uploader.export.vk.sleep.duration:-300ms}")
    private Duration exportVkSleepDuration;

    @Autowired
    @Qualifier("localDiskStorage")
    private RecordStorage<MpfsRequestRecord> storage;

    @Autowired
    private MulcaClient mulcaClient;

    @Autowired
    Stages stages;

    @Autowired
    private RequestStatesHandler requestStatesHandler;

    @Mock
    private SocialProxyClient socialProxyClient;

    private ExportPhotosProcessor exportPhotosProcessor;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        exportPhotosProcessor = new ExportPhotosProcessor(
                requestStatesHandler, stages,
                new SocialProxyStages(socialProxyClient, Timeout.seconds(3)),
                exportVkUseSleep, exportVkSleepDuration);
    }

    @Test
    public void exportPhotoNotFoundInMulca() {
        MpfsRequest.ExportPhotos request = createRequest("profile", Cf.list(MulcaId.fromSerializedString("mulca-id")));

        MpfsRequestRecord.ExportPhotos exportRecord = createRecord(request);
        storage.saveRecord(exportRecord);
        exportPhotosProcessor.process(exportRecord);

        ExportPhotoStatus status = exportRecord.getStatus().multiplePhotoExport.queue.get(0);
        Assert.equals(status.previewFile.get().getType(), State.StateType.SKIPPED_FAILURE);
        Assert.equals(status.uploadInfo.get().getType(), State.StateType.SKIPPED_SUCCESS);
        Assert.equals(status.uploadToSocialNet.get().getType(), State.StateType.SKIPPED_SUCCESS);
        Assert.equals(status.commitUploadMeta.get().getType(), State.StateType.SKIPPED_SUCCESS);
    }

    @Test
    public void exportPhotoSocialClientError() {
        MulcaId mulcaId = mulcaClient.upload(new ByteArrayInputStreamSource("hello".getBytes()), "tmp");
        MpfsRequest.ExportPhotos request = createRequest("profile", Cf.list(mulcaId));

        Mockito.when(socialProxyClient.getPhotoUploadInfo(Mockito.anyString(), Mockito.anyString()))
                .thenThrow(new SocialClientErrException(null));

        MpfsRequestRecord.ExportPhotos exportRecord = createRecord(request);
        storage.saveRecord(exportRecord);
        exportPhotosProcessor.process(exportRecord);

        ExportPhotoStatus status = exportRecord.getStatus().multiplePhotoExport.queue.get(0);
        Assert.equals(status.previewFile.get().getType(), State.StateType.SUCCESS);
        Assert.equals(status.uploadInfo.get().getType(), State.StateType.SKIPPED_FAILURE);
        Assert.equals(status.uploadToSocialNet.get().getType(), State.StateType.SKIPPED_SUCCESS);
        Assert.equals(status.commitUploadMeta.get().getType(), State.StateType.SKIPPED_SUCCESS);
    }

    @Test
    public void exportPhotoSocialClientRateLimitExceeded() {
        MulcaId mulcaId = mulcaClient.upload(new ByteArrayInputStreamSource("hello".getBytes()), "tmp");
        MpfsRequest.ExportPhotos request = createRequest("profile", Cf.list(mulcaId));

        Mockito.when(socialProxyClient.getPhotoUploadInfo(Mockito.anyString(), Mockito.anyString()))
                .thenThrow(new SocialProxyRetriableException(new SocialTaskInfo.FailReason("", "", "", "")));

        MpfsRequestRecord.ExportPhotos exportRecord = createRecord(request);
        storage.saveRecord(exportRecord);
        exportPhotosProcessor.process(exportRecord);

        ExportPhotoStatus status = exportRecord.getStatus().multiplePhotoExport.queue.get(0);
        Assert.equals(status.previewFile.get().getType(), State.StateType.SUCCESS);
        Assert.equals(status.uploadInfo.get().getType(), State.StateType.TEMPORARY_FAILURE);
        Assert.equals(status.uploadToSocialNet.get().getType(), State.StateType.INITIAL);
        Assert.equals(status.commitUploadMeta.get().getType(), State.StateType.INITIAL);
    }

    private MpfsRequestRecord.ExportPhotos createRecord(MpfsRequest.ExportPhotos request) {
        HostInstant hereAndNow = HostInstant.hereAndNow();
        return new MpfsRequestRecord.ExportPhotos(new RequestMeta(UploadRequestId.valueOf(Random2.R.nextAlnum(8)), new Instant()),
                hereAndNow, RequestRevision.initial(hereAndNow), request);
    }

    private MpfsRequest.ExportPhotos createRequest(String profile, ListF<MulcaId> previewStids) {
        return new MpfsRequest.ExportPhotos(ApiVersion.V_0_2, previewStids, profile,
                "1", ChemodanService.VKONTAKTE, Option.empty(), Option.empty());
    }
}
