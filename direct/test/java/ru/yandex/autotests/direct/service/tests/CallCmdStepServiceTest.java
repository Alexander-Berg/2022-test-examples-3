package ru.yandex.autotests.direct.service.tests;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.direct.cmd.DirectCmdSteps;
import ru.yandex.autotests.direct.service.data.ApiStepParamsData;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.direct.proxy.configuration.TestDirectProxyAppConfiguration;
import ru.yandex.direct.proxy.model.StepTypeEnum;
import ru.yandex.direct.proxy.model.web.InitStepRequest;
import ru.yandex.direct.proxy.model.web.callstep.CallStepRequest;
import ru.yandex.direct.proxy.service.CallStepService;
import ru.yandex.direct.proxy.service.InitStepService;
import ru.yandex.direct.proxy.service.TokenStorageService;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestDirectProxyAppConfiguration.class})
public class CallCmdStepServiceTest {
    private static final String CLIENT = "at-direct-backend-c";
    @Autowired
    private CallStepService callStepService;
    @Autowired
    private InitStepService initStepService;
    @Autowired
    private TokenStorageService tokenStorageService;

    private String token;

    @Before
    public void before() {
        InitStepRequest initStepRequest = new InitStepRequest()
                .withStepsTypeEnum(StepTypeEnum.CMD)
                .withDirectStage("TS").withLogin(CLIENT);
        token = initStepService.initSteps(initStepRequest);
    }

    @Test
    public void callStep() {
        CallStepRequest callStepRequest = new CallStepRequest()
                .withToken(token)
                .withParameterList(ApiStepParamsData.defaultGetShowCamp())
                .withStepPath("campaignSteps.getShowCamp");
        DirectCmdSteps step = tokenStorageService.get(token).getSteps(DirectCmdSteps.class);
        step.authSteps().authenticate(User.get(CLIENT));
        callStepService.callStepNew(callStepRequest);

    }


}
