package ru.yandex.autotests.direct.httpclient.steps.sandbox;

import ru.yandex.autotests.direct.httpclient.core.BasicDirectFormParameters;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CMD;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.sandbox.APISandboxClientTypeEnum;
import ru.yandex.autotests.direct.httpclient.data.sandbox.AjaxInitSandboxUserParametersDirect;
import ru.yandex.autotests.direct.httpclient.data.sandbox.CurrentSandboxState;
import ru.yandex.autotests.direct.httpclient.steps.base.DirectBackEndSteps;
import ru.yandex.autotests.direct.utils.matchers.BeanEquals;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.irt.testutils.allure.AllureUtils;
import ru.yandex.qatools.allure.annotations.Step;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;


/**
 * Created by proxeter (Nikolay Mulyar - proxeter@yandex-team.ru) on 16.05.2014.
 */
public class APISandboxSteps extends DirectBackEndSteps {

    private static APISandboxSteps _instance;

    @Step("Начало использования API песочницы")
    public DirectResponse startUseSandboxAPI(
            APISandboxClientTypeEnum userType,
            Boolean initTestData,
            Currency initialCurrency,
            Boolean enableSharedAccount) {
        DirectResponse apiSandbox = openAPISandbox();
        return initSandboxUser(apiSandbox.getCSRFToken(), userType, initTestData, initialCurrency, enableSharedAccount);
    }

    @Step("Подготовка данных для запроса")
    private DirectResponse initSandboxUser(
            CSRFToken token,
            APISandboxClientTypeEnum user,
            Boolean initTestData,
            Currency initialCurrency,
            Boolean enableSharedAccount) {
        AjaxInitSandboxUserParametersDirect params = new AjaxInitSandboxUserParametersDirect();
        params.setSandboxClientType(user.getClientType());
        params.setInitTestData(initTestData ? "1" : "0");
        params.setInitialCurrency(initialCurrency.value());
        params.setEnableSharedAccount(enableSharedAccount ? "1" : "0");

        return initSandboxUser(token, params);
    }

    @Step("Запрос страницы API песочницы")
    public DirectResponse openAPISandbox() {
        return execute(getRequestBuilder().get(CMD.API_SANDBOX_SETTINGS));
    }

    @Step("Отправляем команду на начало использования API песочницы")
    public DirectResponse initSandboxUser(CSRFToken token, AjaxInitSandboxUserParametersDirect params) {
        return execute(getRequestBuilder().post(CMD.AJAX_INIT_SANDBOX_USER, token, params));
    }

    @Step("Отправляем команду на прекращение использования API песочницы")
    public DirectResponse stopUseSandboxAPI() {
        DirectResponse apiSandbox = openAPISandbox();
        return execute(getRequestBuilder().post(CMD.AJAX_DROP_SANDBOX_USER, apiSandbox.getCSRFToken(),
                new BasicDirectFormParameters()));
    }

    @Step("Проверяем ответ сервиса на удовлетворение условию {1}")
    public void shouldSeeResponse(CurrentSandboxState response, BeanEquals<CurrentSandboxState> matcher) {
        AllureUtils.addJsonAttachment("Ответ от сервиса", response.toJson());
        assertThat("Мастер-токен удовлетворяет условию", response, matcher);
    }

}
