package ru.yandex.chemodan.app.orchestrator.manager;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.orchestrator.cloud.ContainerHostPortPojo;
import ru.yandex.chemodan.app.orchestrator.cloud.ControlAgentClient;
import ru.yandex.chemodan.app.orchestrator.cloud.DiscoveryClient;
import ru.yandex.chemodan.app.orchestrator.dao.ContainersDao;
import ru.yandex.commune.alive2.location.Location;
import ru.yandex.commune.alive2.location.LocationResolver;
import ru.yandex.commune.alive2.location.LocationType;
import ru.yandex.commune.bazinga.BazingaTaskManager;
import ru.yandex.misc.ip.HostPort;

public class FromCloudActualizationManagerTest {


    private final HostPort testHostPort = new HostPort("host", 8080);
    @Mock
    private DiscoveryClient discoveryClientMock;
    @Mock
    private ContainersDao daoMock;
    @Mock
    private ControlAgentClient controlAgentMock;
    @Mock
    private OrchestratorControl controlMock;
    @Mock
    private BazingaTaskManager taskManagerMock;
    @Mock
    private LocationResolver locationResolver;
    private FromCloudActualizationManager manager;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
        manager = new FromCloudActualizationManager(daoMock, controlAgentMock, controlMock, taskManagerMock,
                discoveryClientMock, locationResolver, 1);
        Mockito.when(discoveryClientMock.resolveClusterEndpoints(Mockito.anyString()))
                .thenReturn(Cf.list(testHostPort));
    }

    @Test
    public void doNotCreateMoreContainersThanBatchSize() {
        actualizeTestImpl(10, 5, 5);
    }

    @Test
    public void createOnlyRequiredContainersAmount() {
        actualizeTestImpl(1, 5, 1);
    }

    private void actualizeTestImpl(int required, int batchSize, int expectedCreation) {
        Mockito.when(controlAgentMock.listEndpoints(Mockito.anyString())).thenReturn(Cf.list());
        ContainerHostPortPojo containerMock = Mockito.mock(ContainerHostPortPojo.class);
        Mockito.when(containerMock.getContainerId()).thenReturn("id");
        Mockito.when(containerMock.getHostPort()).thenReturn(testHostPort);
        Mockito.when(controlAgentMock.createContainer(Mockito.anyString())).thenReturn(Option.of(containerMock))
                .getMock();
        Mockito.when(controlMock.getContainersPerPod()).thenReturn(required);
        Mockito.when(controlMock.getCreateContainerBatchSize()).thenReturn(batchSize);
        Mockito.when(locationResolver.resolveLocationFor(Mockito.any()))
                .thenReturn(new Location("host", Option.of("sas"), Option.empty(), Cf.list(), LocationType.YP));
        manager.actualizeItem("testItem");
        Mockito.verify(controlAgentMock, Mockito.times(expectedCreation)).createContainer(Mockito.anyString());
    }
}
