package ru.yandex.market.tsum.pipelines.sre.jobs;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.gson.Gson;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.tsum.clients.l3manager.L3ApiRequestResult;
import ru.yandex.market.tsum.clients.l3manager.L3ManagerClient;
import ru.yandex.market.tsum.clients.l3manager.models.Balancer;
import ru.yandex.market.tsum.clients.l3manager.models.Service;
import ru.yandex.market.tsum.clients.l3manager.models.ServiceConfig;
import ru.yandex.market.tsum.clients.l3manager.models.VirtualServer;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobProgressContext;
import ru.yandex.market.tsum.pipelines.sre.resources.balancer.BalancerInfo;
import ru.yandex.startrek.client.Session;
import ru.yandex.startrek.client.model.Comment;
import ru.yandex.startrek.client.model.Issue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.validateMockitoUsage;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@RunWith(MockitoJUnitRunner.Silent.class)
public class CreateVirtualServiceJobTest {

    @InjectMocks
    private CreateVirtualServiceJob job = new CreateVirtualServiceJob();

    @Mock
    private JobContext jobContext;

    @Mock
    private JobProgressContext jobProgressContext;

    @Mock
    private L3ManagerClient client;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Session startrekSession;

    @Mock
    private Issue issue;

    @Spy
    private List<BalancerInfo> balancerInfoList = new ArrayList<>();

    private Service service;
    private Service newService;
    private final String fqdn = "test.vs.market.yandex.net";
    private L3ApiRequestResult resultService;
    private L3ApiRequestResult resultVirtualServer;
    private List<Balancer> balancers;
    private VirtualServer virtualServer;
    private ServiceConfig serviceConfig;
    private L3ApiRequestResult deployResult;
    private L3ApiRequestResult resultServiceConfig;
    private BalancerInfo.Builder balancerInfoBuilder;

    public CreateVirtualServiceJobTest() throws IOException {
        BalancerInfo balancerInfo = BalancerPipelineTestFactory.getBalancerInfo();
        balancerInfo.setFqdn(fqdn);
        balancerInfo.setHttpPort(80);
        balancerInfoList.add(balancerInfo);
    }

    @Before
    public void setUp() throws Exception {
        service = BalancerPipelineTestFactory.getL3managerService();
        newService = BalancerPipelineTestFactory.getL3managerNewService();
        resultService = BalancerPipelineTestFactory.getL3managerResultService();
        balancers = BalancerPipelineTestFactory.getL3managerBalancers();
        resultVirtualServer = BalancerPipelineTestFactory.getL3managerResultVirtualServer();
        resultServiceConfig = BalancerPipelineTestFactory.getL3ManagerResultServiceConfig();
        System.out.println(resultVirtualServer);
        virtualServer = BalancerPipelineTestFactory.getL3managerVirtualServer();
        serviceConfig = BalancerPipelineTestFactory.getL3managerServiceConfig();
        deployResult = BalancerPipelineTestFactory.getL3ManagerDeployResult();
        balancerInfoBuilder = BalancerPipelineTestFactory.getBalancerInfoBuilder();

        when(jobContext.progress()).thenReturn(jobProgressContext);
    }

    /**
     * Тест проверяет, что при несуществующем сервисе в l3-manager проходят все четыре этапа создания нового
     * балансера в l3-manager:
     * - заведение сервиса
     * - создание конфига виртуального сервера
     * - заведение виртуального сервера
     * - деплой настроек на
     *
     */
    @Test
    public void executeIfTestingBalancerNotExists() throws Exception {
        when(client.findService(fqdn)).thenReturn(null);
        when(client.createService(anyString(), anyString())).thenReturn(resultService);
        when(client.getService(anyInt())).thenReturn(newService);
        when(client.getBalancers(anyString())).thenReturn(balancers);
        when(client.getAddress(anyString(), anyBoolean(), anyBoolean()))
            .thenReturn(InetAddress.getByName("2a02:6b8:0:3400:0:3c9:0:10"));
        when(client.createVirtualServer(any(), any())).thenReturn(resultVirtualServer);
        when(client.createServiceConfig(anyInt(), anyList(), anyString())).thenReturn(resultServiceConfig);
        when(client.getVirtualServer(anyInt(), anyInt())).thenReturn(virtualServer);
        when(client.getServiceConfig(anyInt(), anyInt())).thenReturn(serviceConfig);
        when(client.deployConfig(anyInt(), anyInt())).thenReturn(deployResult);
        when(startrekSession.issues().get(anyString())).thenReturn(issue);
        when(issue.comment(anyString())).thenReturn(new Gson().fromJson("{\"id\": 12345}", Comment.class));

        job.execute(jobContext);

        // некоторые проверки на наличие вызовов, которые необходимы для создания нового сервиса в l3-manager.
        verify(client, times(1)).createService(anyString(), anyString());
        verify(client, times(1)).createVirtualServer(any(), any());
        verify(client, times(1)).createServiceConfig(anyInt(), anyList(), anyString());
        verify(client, times(1)).deployConfig(anyInt(), anyInt());
        validateMockitoUsage();
    }

    @Test
    public void executeIfTestingBalancerExists() throws Exception {
        when(client.findService(fqdn)).thenReturn(service);
        when(client.createService(anyString(), anyString())).thenReturn(resultService);
        when(client.getService(anyInt())).thenReturn(service);
        when(client.getAddress(anyString(), anyBoolean(), anyBoolean()))
            .thenReturn(InetAddress.getByName("2a02:6b8:0:3400:0:3c9:0:10"));
        when(client.createVirtualServer(any(), any())).thenReturn(resultVirtualServer);
        when(client.createServiceConfig(anyInt(), anyList(), anyString())).thenReturn(resultServiceConfig);
        when(client.getVirtualServer(anyInt(), anyInt())).thenReturn(virtualServer);
        when(client.getServiceConfig(anyInt(), anyInt())).thenReturn(serviceConfig);
        when(client.deployConfig(anyInt(), anyInt())).thenReturn(deployResult);
        when(startrekSession.issues().get(anyString())).thenReturn(issue);
        when(issue.comment(anyString())).thenReturn(new Gson().fromJson("{\"id\": 12345}", Comment.class));

        job.execute(jobContext);

        // Проверяем, что для существующего сервиса не были вызваны следующие методы
        verify(client, never()).createService(anyString(), anyString());
        verify(client, never()).createVirtualServer(any(), any());
        verify(client, never()).createServiceConfig(anyInt(), anyList(), anyString());
        verify(client, never()).deployConfig(anyInt(), anyInt());
        validateMockitoUsage();
    }

    @Test
    public void testGetVsPortsConfigurationHttp() {
        BalancerInfo balancer = balancerInfoBuilder
            .withHttpPort(80)
            .build();
        Assert.assertEquals(
            BaseBalancerJob.VsPortsConfiguration.HTTP,
            job.getVsPortsConfiguration(balancer)
        );
    }

    @Test
    public void testGetVsPortsConfigurationHttps() {
        BalancerInfo balancer = balancerInfoBuilder
            .withHttpsPort(443)
            .build();
        Assert.assertEquals(
            BaseBalancerJob.VsPortsConfiguration.HTTPS,
            job.getVsPortsConfiguration(balancer)
        );
    }

    @Test
    public void testGetVsPortsConfigurationHttpHttps() {
        BalancerInfo balancer = balancerInfoBuilder
            .withHttpPort(80)
            .withHttpsPort(443)
            .withRedirectToHttps(true)
            .build();
        Assert.assertEquals(
            BaseBalancerJob.VsPortsConfiguration.HTTP_HTTPS,
            job.getVsPortsConfiguration(balancer)
        );
    }

    public void testGetVsPortsConfigurationHttpHttpsNoRedirect() {
        BalancerInfo balancer = balancerInfoBuilder
            .withHttpPort(80)
            .withHttpsPort(443)
            .withRedirectToHttps(false)
            .build();
        Assert.assertEquals(
            BaseBalancerJob.VsPortsConfiguration.HTTP_HTTPS_NO_REDIRECT,
            job.getVsPortsConfiguration(balancer)
        );
    }


    @Test
    public void testGetVirtualServicePorts() {
        Assert.assertEquals(
            Set.of(80),
            job.getVirtualServicePorts(service)
        );
    }

    @Test
    public void testGetVirtualServicePortsNewService() {
        Assert.assertEquals(
            Collections.emptySet(),
            job.getVirtualServicePorts(newService)
        );
    }
}
