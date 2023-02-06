package ru.yandex.autotests.direct.service.tests;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.direct.service.data.ApiStepParamsData;
import ru.yandex.direct.proxy.configuration.TestDirectProxyAppConfiguration;
import ru.yandex.direct.proxy.model.web.InitStepRequest;
import ru.yandex.direct.proxy.model.web.callstep.CallStepRequest;
import ru.yandex.direct.proxy.service.CallStepService;
import ru.yandex.direct.proxy.service.InitStepService;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestDirectProxyAppConfiguration.class})
public class CallApiStepServiceTest {
    private static final String CLIENT = "at-direct-backend-c";
    @Autowired
    private CallStepService callStepService;
    @Autowired
    private InitStepService initStepService;

    private String token;

    @Before
    public void before() {
        InitStepRequest initStepRequest = new InitStepRequest()
                .withDirectStage("TS").withLogin(CLIENT);
        token = initStepService.initSteps(initStepRequest);
    }

    @Test
    public void callStep() {
        CallStepRequest callStepRequest = new CallStepRequest()
                .withToken(token)
                .withParameterList(ApiStepParamsData.defaultCreateDefaultCampaignParams())
                .withStepPath("clientSteps.getClientInfo");
        callStepService.callApiStep(callStepRequest);
    }

    @Test
    @Ignore
    public void testClientsUpdate() {
        CallStepRequest callStepRequest = new CallStepRequest()
                .withToken(token)
                .withParameterList(ApiStepParamsData.defaultClientUpdate())
                .withStepPath("clientsStepsV5.clientsUpdate");
        callStepService.callApiStep(callStepRequest);
    }

    @Test
    public void testCampaignAdd() {
        CallStepRequest callStepRequest = new CallStepRequest()
                .withToken(token)
                .withParameterList(ApiStepParamsData.defaultCampaignAdd())
                .withStepPath("campaignSteps.campaignsAdd");
        callStepService.callApiStep(callStepRequest);
    }
}
