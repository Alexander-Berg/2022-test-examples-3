package ru.yandex.market.tsum.sox.rtc;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.tsum.agent.ResourceInfo;
import ru.yandex.market.tsum.core.agent.AgentMongoDao;
import ru.yandex.market.tsum.core.agent.PackageType;
import ru.yandex.market.tsum.core.agent.RtcYpResourceStatus;
import ru.yandex.market.tsum.registry.proto.model.DeployType;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class SandboxResourceRtcYpValidationServiceTest {
    private final static Set<String> MARKET_PREFIXES = Set.of("testing_market_");

    @InjectMocks
    private final SandboxResourceRtcYpValidationService service  = new SandboxResourceRtcYpValidationService();

    @Mock
    private AgentMongoDao agentMongoDao;

    @Mock
    private Set<String> nannyMarketPrefixes = MARKET_PREFIXES;

    @Mock
    private Set<String> ydMarketPrefixes = MARKET_PREFIXES;

    private final RtcServiceSandboxResource rtcServiceSandboxResource1;
    private final RtcServiceSandboxResource rtcServiceSandboxResource2;
    private final RtcServiceSandboxResource rtcServiceSandboxResource3;
    private final RtcServiceSandboxResource rtcServiceSandboxResourceWrong;

    public SandboxResourceRtcYpValidationServiceTest() {
        this.rtcServiceSandboxResource1 = new RtcServiceSandboxResource(
            DeployType.NANNY, "testing_market_service-1", 1, "MARKET_RESOURCE_1");
        this.rtcServiceSandboxResource2 = new RtcServiceSandboxResource(
            DeployType.NANNY, "testing_market_service-2", 2, "MARKET_RESOURCE_1");
        this.rtcServiceSandboxResource3 = new RtcServiceSandboxResource(
            DeployType.YANDEX_DEPLOY, "testing_market_service-3.service-3", 3, "MARKET_RESOURCE_2");
        this.rtcServiceSandboxResourceWrong = new RtcServiceSandboxResource(
            DeployType.YANDEX_DEPLOY, "wrong_service", 4, "MARKET_RESOURCE_1");
    }

    @Test
    public void testGroupResourcesByType() {
        List<RtcServiceSandboxResource> resources = List.of(
            rtcServiceSandboxResource1, rtcServiceSandboxResource2, rtcServiceSandboxResource3);
        Map<String, List<RtcServiceSandboxResource>> actual = service.groupResourcesByType(resources);
        Map<String, List<RtcServiceSandboxResource>> expected = Map.of(
            "MARKET_RESOURCE_1", List.of(rtcServiceSandboxResource1, rtcServiceSandboxResource2),
            "MARKET_RESOURCE_2", List.of(rtcServiceSandboxResource3)
        );
        Assert.assertEquals(actual, expected);
    }

    @Test
    public void testGetStatusForResources() {
        when(agentMongoDao.getResourceInfo(anyString(), anyLong())).thenReturn(
            Optional.of(new AgentMongoDao.ResourceInfoWrapper.ResourceInfoEntity(ResourceInfo.newBuilder().build()))
        );

        RtcYpResourceStatus actual = service.getStatusForResources("MARKET_RESOURCE_1", List.of(
                rtcServiceSandboxResource1, rtcServiceSandboxResource2, rtcServiceSandboxResourceWrong));
        RtcYpResourceStatus expected = new RtcYpResourceStatus("MARKET_RESOURCE_1", PackageType.SOX);
        expected.addInstanceStatus(
            true,
            rtcServiceSandboxResource1.getRtcServiceType(),
            rtcServiceSandboxResource1.getRtcServiceId(),
            rtcServiceSandboxResource1.getSandboxTaskId()
        );
        expected.addInstanceStatus(
            true,
            rtcServiceSandboxResource2.getRtcServiceType(),
            rtcServiceSandboxResource2.getRtcServiceId(),
            rtcServiceSandboxResource2.getSandboxTaskId()
        );
        expected.addInvalidServiceName(
            rtcServiceSandboxResourceWrong.getRtcServiceType(),
            rtcServiceSandboxResourceWrong.getRtcServiceId()
        );
        Assert.assertEquals(expected, actual);
    }
}
