package ru.yandex.chemodan.uploader.registry;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.uploader.config.UploaderCoreContextConfigurationForTests;
import ru.yandex.chemodan.uploader.registry.processors.MarkMidForRemoveProcessor;
import ru.yandex.chemodan.uploader.registry.record.MpfsRequest;
import ru.yandex.chemodan.uploader.registry.record.MpfsRequestRecord;
import ru.yandex.chemodan.uploader.registry.record.MpfsRequestRecordUtils;
import ru.yandex.chemodan.uploader.registry.record.Record;
import ru.yandex.chemodan.util.test.AbstractTest;
import ru.yandex.commune.uploader.registry.CallbackResponse;
import ru.yandex.commune.uploader.registry.CallbackResponseOption;
import ru.yandex.commune.uploader.registry.RequestRevision;
import ru.yandex.commune.uploader.registry.StageListenerPool;
import ru.yandex.commune.uploader.registry.StageProperties;
import ru.yandex.commune.uploader.registry.StageSemaphores;
import ru.yandex.commune.uploader.registry.UploadRegistry;
import ru.yandex.commune.uploader.registry.UploadRequestStatus;
import ru.yandex.commune.uploader.util.HostInstant;
import ru.yandex.inside.mulca.MulcaId;
import ru.yandex.misc.test.Assert;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Vsevolod Tolstopyatov (qwwdfsad)
 */

@ContextConfiguration(classes = UploaderCoreContextConfigurationForTests.class)
public class MarkMulcaIdForRemoveTest extends AbstractTest {
    @Mock
    private Stages stagesMock;

    @Autowired
    private UploadRegistry<MpfsRequestRecord> uploadRegistry;
    @Autowired
    private StageListenerPool<Record<?>> stageListenerPool;
    @Autowired
    private StageProperties stageProperties;
    @Autowired
    private StageSemaphores stageSemaphores;

    private RequestStatesHandler handler;
    private MarkMidForRemoveProcessor processor;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        handler = new RequestStatesHandler(
                stageListenerPool, stageProperties, stageSemaphores, uploadRegistry, stagesMock);
        processor = new MarkMidForRemoveProcessor(handler, stagesMock);
    }

    @Test
    public void markMulcaIdsForRemove() {
        ListF<MulcaId> mulcaIds = Cf.list(MulcaId.valueOf("1", ""), MulcaId.valueOf("2", ""), MulcaId.valueOf("3", ""));

        when(stagesMock.markMulcaIdToRemoveF(any(MulcaId.class))).thenReturn(
                () -> new CallbackResponseOption(Option.of(
                        new CallbackResponse(200, "", "", Cf.map(), Option.empty()))));

        MpfsRequestRecord.MarkMulcaIdsForRemove record = createRecord(mulcaIds);
        processor.process(record);

        MpfsRequestRecord.MarkMulcaIdsForRemove actual =
                (MpfsRequestRecord.MarkMulcaIdsForRemove) uploadRegistry.findRecord(record.meta.id).get();
        Assert.equals(UploadRequestStatus.Result.COMPLETED, actual.getStatus().resultWithFinalStages());
        verify(stagesMock, times(mulcaIds.size())).markMulcaIdToRemoveF(any(MulcaId.class));
    }

    private MpfsRequestRecord.MarkMulcaIdsForRemove createRecord(ListF<MulcaId> mulcaIds) {
        return (MpfsRequestRecord.MarkMulcaIdsForRemove) uploadRegistry.saveRecord(MpfsRequestRecordUtils.consF(
                new MpfsRequest.MarkMulcaIdsForRemove(ApiVersion.V_0_2, mulcaIds, Option.empty()),
                RequestRevision.initial(HostInstant.hereAndNow())
        ));
    }

}
