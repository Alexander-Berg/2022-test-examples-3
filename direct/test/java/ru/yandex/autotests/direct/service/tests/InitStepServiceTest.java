package ru.yandex.autotests.direct.service.tests;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.proxy.configuration.TestDirectProxyAppConfiguration;
import ru.yandex.direct.proxy.model.web.InitStepRequest;
import ru.yandex.direct.proxy.service.InitStepService;

import static org.hamcrest.CoreMatchers.notNullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestDirectProxyAppConfiguration.class})
public class InitStepServiceTest {
    private static final String CLIENT = "at-direct-backend-c";

    @Autowired
    InitStepService initStepService;

    @Test
    public void initStep() {
        InitStepRequest initStepRequest = new InitStepRequest().withLogin(CLIENT);
        String resultToken = initStepService.initSteps(initStepRequest);
        assertThat("Токен создался", resultToken, notNullValue());
    }
}
