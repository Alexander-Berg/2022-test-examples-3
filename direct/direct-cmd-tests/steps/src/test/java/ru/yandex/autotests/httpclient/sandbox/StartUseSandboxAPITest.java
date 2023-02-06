package ru.yandex.autotests.httpclient.sandbox;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.autotests.direct.httpclient.UserSteps;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.sandbox.APISandboxClientTypeEnum;
import ru.yandex.autotests.direct.httpclient.data.sandbox.CurrentSandboxState;
import ru.yandex.autotests.direct.httpclient.data.sandbox.SandboxResponseBean;
import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.direct.utils.matchers.BeanCompareStrategy;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;
import static ru.yandex.autotests.direct.utils.matchers.BeanEquals.beanEquals;

/**
 * Created by proxeter (Nikolay Mulyar - proxeter@yandex-team.ru) on 16.05.2014.
 */
@RunWith(Parameterized.class)
public class StartUseSandboxAPITest {

    private final String sandboxUserLogin = "at-direct-sandbox-client";
    private final String sandboxUserPassword = "at-tester1";

    private UserSteps user;
    private SandboxResponseBean response;

    @Parameterized.Parameter(value = 0)
    public APISandboxClientTypeEnum clientType;

    @Parameterized.Parameters(name = "Тест #{index}: тип пользователя {0}")
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][] {
                { APISandboxClientTypeEnum.AGENCY },
                { APISandboxClientTypeEnum.CLIENT }
        };
        return Arrays.asList(data);
    }

    @Before
    public void beforeTest() {
        user = new UserSteps(DirectTestRunProperties.getInstance());
        user.onPassport().authoriseAs(sandboxUserLogin, sandboxUserPassword);

        // Отключаем песочницу, если включена
        user.apiSandboxSteps().stopUseSandboxAPI();

        DirectResponse result = user.apiSandboxSteps().startUseSandboxAPI(clientType, true, null, false);
        response = SandboxResponseBean.readJson(result.getResponseContent().asString());
    }

    @After
    public void afterTest() {
        user.apiSandboxSteps().stopUseSandboxAPI();
    }

    @Test
    @Ignore
    @Title("Проверка корректного ответа при начале пользования песочницей API")
    public void sandboxAPIHttpClientInitializeTest() {
        CurrentSandboxState expected = new CurrentSandboxState();
        expected.setRole(clientType.getClientType());

        BeanCompareStrategy strategy = new BeanCompareStrategy();
        strategy.putFieldMatcher("masterToken", not(isEmptyString()));

        user.apiSandboxSteps().shouldSeeResponse(
                response.getCurrentSandboxState(), beanEquals(expected));
    }

}
