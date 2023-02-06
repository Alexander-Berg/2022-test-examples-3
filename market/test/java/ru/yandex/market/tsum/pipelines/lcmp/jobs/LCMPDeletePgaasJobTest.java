package ru.yandex.market.tsum.pipelines.lcmp.jobs;

import java.util.List;

import io.grpc.Channel;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import yandex.cloud.api.mdb.postgresql.v1.ClusterOuterClass;
import yandex.cloud.api.mdb.postgresql.v1.ClusterServiceGrpc;
import yandex.cloud.api.mdb.postgresql.v1.ClusterServiceOuterClass;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.TestJobContext;
import ru.yandex.market.tsum.pipelines.lcmp.resources.ComponentChangeRequest;
import ru.yandex.market.tsum.pipelines.lcmp.resources.ComponentSpecResource;

public class LCMPDeletePgaasJobTest {
    private static final JobContext CONTEXT = new TestJobContext();

    @Spy
    private ComponentChangeRequest componentChangeRequest = new ComponentChangeRequest();

    @Spy
    private Channel mdbChannel;

    @InjectMocks
    private LCMPDeletePgaasJob sut;

    private ClusterServiceGrpc.ClusterServiceBlockingStub clusterService;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        clusterService = Mockito.mock(ClusterServiceGrpc.ClusterServiceBlockingStub.class);
        Mockito.doReturn(null).when(clusterService).delete(Mockito.any());
        sut = Mockito.spy(sut);
        Mockito.when(sut.getClusterService()).thenReturn(clusterService);
    }

    @Test
    public void noRemovalCauseAllInTarget() throws Exception {
        ComponentSpecResource previous = TestComponentSpecGenerator.generate(List.of(), List.of());
        componentChangeRequest.setPreviousComponentSpecResource(previous);
        ComponentSpecResource target = TestComponentSpecGenerator.generate(List.of(), List.of());
        componentChangeRequest.setTargetComponentSpecResource(target);
        sut.execute(CONTEXT);
        Mockito.verify(clusterService, Mockito.never()).delete(Mockito.any());
    }

    @Test
    public void removeStoppedAndNoTargetClusters() throws Exception {
        ComponentSpecResource previous = TestComponentSpecGenerator.generate(List.of(), List.of());
        componentChangeRequest.setPreviousComponentSpecResource(previous);
        ComponentSpecResource target = TestComponentSpecGenerator.generate(List.of());
        componentChangeRequest.setTargetComponentSpecResource(target);
        Mockito.doReturn(ClusterOuterClass.Cluster.newBuilder()
                .setStatus(ClusterOuterClass.Cluster.Status.STOPPED)
                .build())
            .when(clusterService).get(getClusterRequest("cluster_1"));
        sut.execute(CONTEXT);
        Mockito.verify(clusterService).delete(Mockito.any());
    }

    private ClusterServiceOuterClass.GetClusterRequest getClusterRequest(String cluster) {
        return ClusterServiceOuterClass.GetClusterRequest
            .newBuilder()
            .setClusterId(cluster)
            .build();
    }
}
