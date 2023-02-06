package ru.yandex.autotests.direct.service.tests;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.proxy.configuration.TestDirectProxyAppConfiguration;
import ru.yandex.direct.proxy.model.Converter;
import ru.yandex.direct.proxy.model.ExecuteStepData;
import ru.yandex.direct.proxy.model.StepArgument;
import ru.yandex.direct.proxy.model.web.InitStepRequest;
import ru.yandex.direct.proxy.service.CallStepsHelper;
import ru.yandex.direct.proxy.service.InitStepService;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestDirectProxyAppConfiguration.class})
public class ExecuteStepServiceTest {
    @Autowired
    CallStepsHelper callStepsHelper;

    @Autowired
    InitStepService initStepService;
    private String token;

    @Before
    public void before() {
        InitStepRequest initStepRequest = new InitStepRequest().withLogin("at-direct-super");
        token = initStepService.initSteps(initStepRequest);
    }

    @Test
    public void test() {
        ExecuteStepData executeStep = new ExecuteStepData();
        String argument =
                "{\"campaigns\" : [{\"Name\": \"name\", \"StartDate\": \"2018-02-21\", \"TextCampaign\": {\"BiddingStrategy\":{\"Search\": {\"BiddingStrategyType\": \"HIGHEST_POSITION\"}, \"Network\": {\"BiddingStrategyType\": \"MAXIMUM_COVERAGE\"}}}}]}";
        List<StepArgument> stepArguments = Arrays.asList(
                new StepArgument().withValue(argument).withName("parameters"),
                new StepArgument().withValue("\"at-direct-backend-c\"").withName("login"));
        executeStep.withToken(token)
                .withStepPath(Converter.stringToStepPath("ApiSteps.campaignSteps.campaignsAdd"))
                .withArgs(stepArguments);

        callStepsHelper.executeStep(executeStep);
    }
}
