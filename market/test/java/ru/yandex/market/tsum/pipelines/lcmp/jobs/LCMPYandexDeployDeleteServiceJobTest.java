package ru.yandex.market.tsum.pipelines.lcmp.jobs;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import ru.yandex.market.tsum.clients.yp.YandexDeployClient;
import ru.yandex.market.tsum.clients.yp.model.DeployProgress;
import ru.yandex.market.tsum.clients.yp.model.DeployUnitStatus;
import ru.yandex.market.tsum.clients.yp.model.StageStatus;
import ru.yandex.market.tsum.clients.yp.transport.TransportType;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.runtime.exceptions.JobManualFailException;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.TestJobContext;
import ru.yandex.market.tsum.pipelines.lcmp.resources.ComponentChangeRequest;
import ru.yandex.market.tsum.pipelines.lcmp.resources.ComponentSpecResource;

public class LCMPYandexDeployDeleteServiceJobTest {
    @Spy
    private ComponentChangeRequest componentChangeRequest = new ComponentChangeRequest();

    @Mock
    private YandexDeployClient yandexDeployClient;

    @InjectMocks
    private LCMPYandexDeployDeleteServiceJob sut;

    private static final JobContext CONTEXT = new TestJobContext();

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        for (String service : List.of("1", "2", "3")) {
            Mockito.doReturn(true).when(yandexDeployClient).isStageExists(service);
        }
    }

    @Test(expected = JobManualFailException.class)
    public void noRemovalCauseServicesWithPods() throws Exception {
        ComponentSpecResource previous = TestComponentSpecGenerator.generate(
            List.of("1", "2")
        );
        ComponentSpecResource target = TestComponentSpecGenerator.generate(
            List.of("1", "3")
        );
        componentChangeRequest.setPreviousComponentSpecResource(previous);
        componentChangeRequest.setTargetComponentSpecResource(target);
        StageStatus stageStatus = StageStatus.newBuilder(TransportType.YSON)
            .putDeployUnits("d1", buildDeployUnit(0))
            .putDeployUnits("d2", buildDeployUnit(3))
            .build();
        Mockito.doReturn(stageStatus).when(yandexDeployClient).getStageStatus(TransportType.YSON, "2");
        sut.execute(CONTEXT);
    }

    @Test
    public void noRemovalCauseAllInTarget() throws Exception {
        ComponentSpecResource previous = TestComponentSpecGenerator.generate(
          List.of("1", "2")
        );
        ComponentSpecResource target = TestComponentSpecGenerator.generate(
            List.of("1", "2", "3")
        );
        componentChangeRequest.setPreviousComponentSpecResource(previous);
        componentChangeRequest.setTargetComponentSpecResource(target);
        sut.execute(CONTEXT);
        Mockito.verify(yandexDeployClient, Mockito.never()).removeStage(Mockito.anyString());
    }

    @Test
    public void removeServices() throws Exception {
        ComponentSpecResource previous = TestComponentSpecGenerator.generate(
            List.of("1", "2")
        );
        ComponentSpecResource target = TestComponentSpecGenerator.generate(
            List.of("1", "3")
        );
        componentChangeRequest.setPreviousComponentSpecResource(previous);
        componentChangeRequest.setTargetComponentSpecResource(target);
        StageStatus stageStatus = StageStatus.newBuilder(TransportType.YSON)
            .putDeployUnits("d1", buildDeployUnit(0))
            .putDeployUnits("d2", buildDeployUnit(0))
            .build();
        Mockito.doReturn(stageStatus).when(yandexDeployClient).getStageStatus(TransportType.YSON, "2");
        sut.execute(CONTEXT);
        Mockito.verify(yandexDeployClient).removeStage("2");
    }

    private DeployUnitStatus buildDeployUnit(int pods) {
        return DeployUnitStatus.newBuilder(TransportType.YSON)
            .setProgress(DeployProgress.newBuilder(TransportType.YSON)
                .setPodsTotal(pods)
                .build())
            .build();
    }
}
