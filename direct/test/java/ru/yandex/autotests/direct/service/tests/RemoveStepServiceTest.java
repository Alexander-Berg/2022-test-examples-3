package ru.yandex.autotests.direct.service.tests;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.proxy.configuration.TestDirectProxyAppConfiguration;
import ru.yandex.direct.proxy.model.web.InitStepRequest;
import ru.yandex.direct.proxy.service.InitStepService;
import ru.yandex.direct.proxy.service.RemoveStepService;
import ru.yandex.direct.proxy.service.TokenStorageService;

import static org.hamcrest.Matchers.is;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestDirectProxyAppConfiguration.class})
public class RemoveStepServiceTest {
    private static final String CLIENT = "at-direct-backend-c";
    @Autowired
    InitStepService initStepService;
    @Autowired
    private TokenStorageService tokenStorageService;
    @Autowired
    private RemoveStepService removeStepService;
    private String token;

    @Before
    public void before() {
        InitStepRequest initStepRequest = new InitStepRequest().withLogin(CLIENT);
        token = initStepService.initSteps(initStepRequest);
    }

    @Test
    public void removeStep() {
        removeStepService.remove(token);
        assertThat("Api степы удалены", tokenStorageService.contains(token), is(false));
    }
}
