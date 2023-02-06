package ru.yandex.market.tsum.pipelines.sre.jobs;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.tsum.pipelines.sre.resources.BalancerEnvironment;
import ru.yandex.market.tsum.pipelines.sre.resources.ConfigForGenerateXslbConfig;
import ru.yandex.market.tsum.pipelines.sre.resources.balancer.BalancerInfo;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class GenerateAnsibleJugglerConfigJobTest {

    @InjectMocks
    private GenerateAnsibleJugglerConfigJob ansibleJugglerConfigJob = new GenerateAnsibleJugglerConfigJob();

    @Mock
    private ConfigForGenerateXslbConfig jobConfig;

    @Mock
    private BalancerInfo balancerInfo = BalancerPipelineTestFactory.getBalancerInfo();
    private final BalancerEnvironment balancerEnvironment = BalancerEnvironment.TESTING;

    public GenerateAnsibleJugglerConfigJobTest() throws IOException {
    }

    @Test
    public void getParams() {
        when(balancerInfo.getFqdn()).thenReturn("some.tst.vs.market.yandex.net");
        when(balancerInfo.calculateHaproxyPort()).thenReturn(7777);
        when(balancerInfo.calculateConductorGroup()).thenReturn("market_slb_search-testing");

        Map<String, Object> expectedParams = new HashMap<>();
        expectedParams.put("children", "market_slb_search-testing");
        expectedParams.put("balancerInfo", balancerInfo);
        expectedParams.put("checkName", "some.tst.vs.market.yandex.net_7777");

        Map<String, Object> resultParams = ansibleJugglerConfigJob.getParams();

        Assert.assertEquals(expectedParams, resultParams);
        verify(balancerInfo, times(1)).getFqdn();
        verify(balancerInfo, times(1)).calculateConductorGroup();
        verify(balancerInfo, times(1)).calculateHaproxyPort();
    }
}
