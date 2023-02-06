package ru.yandex.autotests.direct.httpclient.steps;

import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CMD;
import ru.yandex.autotests.direct.httpclient.data.clients.ProveNewAgencyClientsRequestBean;
import ru.yandex.autotests.direct.httpclient.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 25.05.15
 */
public class ProveNewAgencyClientsSteps extends DirectBackEndSteps {

    private static final String YOUR_LOGIN_CONFIRMED = "p-prove-login-thanks__message";

    @Step("Отправляем подтверждение на разрешение агентстсву доступа к логину")
    public void proveNewAgencyClients(String data) {
        ProveNewAgencyClientsRequestBean params = new ProveNewAgencyClientsRequestBean();
        params.setData(data);
        DirectResponse response = execute(getRequestBuilder().get(CMD.PROVE_NEW_AGENCY_CLIENTS, params));
        assertThat("Подтверждение логина прошло успешно", response.getResponseContent().asString(),
                containsString(YOUR_LOGIN_CONFIRMED));
    }
}
