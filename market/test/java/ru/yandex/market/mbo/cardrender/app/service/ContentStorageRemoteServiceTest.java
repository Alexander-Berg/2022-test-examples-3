package ru.yandex.market.mbo.cardrender.app.service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.cardrender.app.BaseTest;
import ru.yandex.market.mbo.cs.CsGumofulRenderGrpc;
import ru.yandex.market.mbo.export.ExportReportModels;
import ru.yandex.market.mbo.storage.StorageKeyValueService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

/**
 * @author apluhin
 * @created 3/16/22
 */
public class ContentStorageRemoteServiceTest extends BaseTest {

    @Rule
    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();
    protected CsGumofulRenderGrpc.CsGumofulRenderImplBase csGumofulRenderImplBase;
    private ContentStorageRemoteService contentStorageRemoteService;
    private CsGumofulRenderGrpc.CsGumofulRenderBlockingStub csGumofulRenderBlockingStub;
    @Autowired
    private StorageKeyValueService storageKeyValueService;

    @Before
    public void setUp() throws Exception {
        var grpcSrvName = InProcessServerBuilder.generateName();
        csGumofulRenderImplBase = mock(CsGumofulRenderGrpc.CsGumofulRenderImplBase.class);
        grpcCleanup.register(InProcessServerBuilder.forName(grpcSrvName)
                .directExecutor().addService(csGumofulRenderImplBase).build().start());
        var channel = grpcCleanup.register(InProcessChannelBuilder.forName(grpcSrvName).directExecutor().build());
        csGumofulRenderBlockingStub = CsGumofulRenderGrpc.newBlockingStub(channel);

        contentStorageRemoteService = new ContentStorageRemoteService(
                csGumofulRenderBlockingStub,
                storageKeyValueService
        );
        storageKeyValueService.putValue("cs_enrich_enabled", true);
    }

    @Test
    public void testStaticBatch() {
        var m1 = generateModels(100);
        doThrow(RuntimeException.class).when(csGumofulRenderImplBase).getGumofulTemplates(any(), any());
        try {
            contentStorageRemoteService.enrichTemplate(m1);
        } catch (Exception e) {
            //
        }
        Mockito.verify(csGumofulRenderImplBase, times(7)).getGumofulTemplates(any(), any());
    }

    private List<ExportReportModels.ExportReportModel.Builder> generateModels(int size) {
        return IntStream.range(0, size)
                .boxed()
                .map(id -> ExportReportModels.ExportReportModel.newBuilder().setId(id).setCategoryId(id))
                .collect(Collectors.toList());
    }

}
