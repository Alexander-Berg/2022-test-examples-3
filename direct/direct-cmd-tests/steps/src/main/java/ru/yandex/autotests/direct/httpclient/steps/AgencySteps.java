package ru.yandex.autotests.direct.httpclient.steps;

import ru.yandex.autotests.direct.httpclient.core.BasicDirectFormParameters;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CMD;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.clients.AddAgencyClientRelationParametersDirect;
import ru.yandex.autotests.direct.httpclient.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

/**
 * @author Roman Kuhta (kuhtich@yandex-team.ru)
 */
public class AgencySteps extends DirectBackEndSteps {

    private ShowClientsSteps showClientsSteps;

    private ShowClientsSteps getShowClientsSteps() {
        return getInstance(ShowClientsSteps.class, config);
    }

    @Step("Добавляем агентству {0} клиента {1}")
    public DirectResponse addClientToAgency(String agencyLogin, String clientLogin) {
        BasicDirectFormParameters context = new BasicDirectFormParameters();
        context.setUlogin(agencyLogin);

        DirectResponse clients = getShowClientsSteps().openShowClients();
        DirectResponse clientRelations = openAddAgencyClientRelation(clients.getCSRFToken());
        return addAgencyClientRelation(clientRelations.getCSRFToken(), clientLogin);
    }

    @Step
    public DirectResponse addAgencyClientRelation(CSRFToken token, String clientLogin) {
        AddAgencyClientRelationParametersDirect params = new AddAgencyClientRelationParametersDirect();
        params.setClientLogin(clientLogin);
        params.setDoAdd("add");
        return addAgencyClientRelation(token, params);
    }

    @Step
    public DirectResponse openAddAgencyClientRelation(CSRFToken token) {
        return execute(getRequestBuilder().get(CMD.ADD_AGENCY_CLIENT_RELATION, token));
    }

    @Step("Добавляем клиента агентству {0}")
    public DirectResponse addAgencyClientRelation(CSRFToken token, AddAgencyClientRelationParametersDirect params) {
        return execute(getRequestBuilder().post(CMD.ADD_AGENCY_CLIENT_RELATION, token, params));
    }
}
