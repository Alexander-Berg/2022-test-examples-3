package ru.yandex.market.tsum.pipelines.lcmp.jobs;

import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import ru.yandex.market.tsum.clients.nanny.NannyCleanupService;
import ru.yandex.market.tsum.clients.nanny.NannyClient;
import ru.yandex.market.tsum.clients.nanny.service.NannyService;
import ru.yandex.market.tsum.clients.nanny.service.State;
import ru.yandex.market.tsum.context.TsumJobContext;
import ru.yandex.market.tsum.multitesting.YpAllocationParams;
import ru.yandex.market.tsum.pipe.engine.definition.context.impl.SupportType;
import ru.yandex.market.tsum.pipe.engine.runtime.exceptions.JobManualFailException;
import ru.yandex.market.tsum.pipelines.lcmp.resources.ComponentChangeRequest;
import ru.yandex.market.tsum.pipelines.lcmp.resources.ComponentSpecResource;

public class LCMPNannyDeleteServiceJobTest {
    @Spy
    private ComponentChangeRequest componentChangeRequest = new ComponentChangeRequest();

    @Mock
    private NannyClient nannyClient;

    @Mock
    private NannyCleanupService nannyCleanupService;

    @Mock
    private  TsumJobContext context;

    @InjectMocks
    private LCMPNannyDeleteServiceJob sut;

    @Before
    public void setup() {
        ComponentSpecResource previousComponent = TestComponentSpecGenerator.generate(
            List.of("n1", "n2"), List.of("n3", "n4"));
        componentChangeRequest.setPreviousComponentSpecResource(previousComponent);

        ComponentSpecResource targetComponent = TestComponentSpecGenerator.generate(
            List.of("n1", "n5"), List.of("n4"));
        componentChangeRequest.setTargetComponentSpecResource(targetComponent);

        MockitoAnnotations.initMocks(this);
        Mockito.doThrow(
            new JobManualFailException("Выключите все сервисы перед их удалением", List.of(SupportType.NANNY))
            ).when(context).actions();
    }

    @Test(expected = JobManualFailException.class)
    public void noRemovalCauseActiveServices() throws Exception {
        Mockito.when(nannyClient.getOptionalService("n2")).thenReturn(Optional.of(
            new NannyService("n2", getOffline(), null, null, null, null)
        ));
        Mockito.when(nannyClient.getOptionalService("n3")).thenReturn(Optional.of(
            new NannyService("n3", getActive(), null, null, null, null)
        ));
        sut.execute(context);
    }

    @Test
    public void noRemovalCauseAllInTarget() throws Exception {
        ComponentSpecResource previousComponent = TestComponentSpecGenerator.generate(
            List.of("n1", "n2"), List.of("n3", "n4"));
        componentChangeRequest.setPreviousComponentSpecResource(previousComponent);

        ComponentSpecResource targetComponent = TestComponentSpecGenerator.generate(
            List.of("n1", "n2", "n5"), List.of("n3", "n4")
        );
        componentChangeRequest.setTargetComponentSpecResource(targetComponent);

        sut.execute(context);
        Mockito.verify(nannyCleanupService).cleanup(
            List.of(), YpAllocationParams.Location.getNamesOfConcreteLocations()
        );
    }

    @Test
    public void removeOfflineAndNoTargetServices() throws Exception {
        Mockito.when(nannyClient.getOptionalService("n2")).thenReturn(Optional.of(
            new NannyService("n2", getOffline(), null, null, null, null)
        ));
        Mockito.when(nannyClient.getOptionalService("n3")).thenReturn(Optional.of(
            new NannyService("n3", getOffline(), null, null, null, null)
        ));
        sut.execute(context);
        Mockito.verify(nannyCleanupService).cleanup(
            List.of("n2", "n3"), YpAllocationParams.Location.getNamesOfConcreteLocations()
        );
    }

    private State getOffline() {
        return new State(new State.Content(null, new State.Content.Summary("", "OFFLINE")));
    }

    public State getActive() {
        return new State(new State.Content(null, new State.Content.Summary("", "ACTIVE")));
    }
}
