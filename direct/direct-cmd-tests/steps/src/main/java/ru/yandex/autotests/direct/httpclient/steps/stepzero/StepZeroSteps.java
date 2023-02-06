package ru.yandex.autotests.direct.httpclient.steps.stepzero;

import org.hamcrest.Matcher;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CMD;
import ru.yandex.autotests.direct.httpclient.data.CmdBeans.stepzero.ClientsCmdBean;
import ru.yandex.autotests.direct.httpclient.data.stepzero.StepZeroParams;
import ru.yandex.autotests.direct.httpclient.steps.base.DirectBackEndSteps;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.BeanType;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPathJSONPopulater;
import ru.yandex.qatools.allure.annotations.Step;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Created by shmykov on 01.04.15.
 * TESTIRT-3642
 */
public class StepZeroSteps extends DirectBackEndSteps {

    @Step("Получаем ответ контроллера StepZero")
    public DirectResponse getStepZero(StepZeroParams params) {
        return execute(getRequestBuilder().get(CMD.STEP_ZERO, params));
    }

    @Step("Получаем ответ контроллера StepZero")
    public DirectResponse getStepZero() {
        return execute(getRequestBuilder().get(CMD.STEP_ZERO));
    }

    @Step("Получаем ответ контроллера StepZero и проверяем, что список клиентов в ответе соответствует {1}")
    public void getStepZeroAndCheckClientsListInResponse(StepZeroParams params, Matcher matcher) {
        DirectResponse response = getStepZero(params);

        ClientsCmdBean actualClientsList = JsonPathJSONPopulater.eval(
                response.getResponseContent().asString(),
                new ClientsCmdBean(), BeanType.RESPONSE);
        assertThat("список пользователей в ответе соответствует условию", actualClientsList.getLogins(), matcher);
    }
}