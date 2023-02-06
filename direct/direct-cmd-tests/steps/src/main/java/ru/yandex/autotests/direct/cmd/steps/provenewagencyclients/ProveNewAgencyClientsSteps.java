package ru.yandex.autotests.direct.cmd.steps.provenewagencyclients;

import ru.yandex.autotests.direct.cmd.data.CMD;
import ru.yandex.autotests.direct.cmd.data.provenewagencyclients.ProveNewAgencyClientsRequest;
import ru.yandex.autotests.direct.cmd.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

public class ProveNewAgencyClientsSteps extends DirectBackEndSteps {

    @Step("GET cmd = proveNewAgencyClients (дать доступ к логику агенству)")
    public void getProveNewAgencyClients(ProveNewAgencyClientsRequest request) {
        get(CMD.PROVE_NEW_AGENCY_CLIENTS, request, Void.class);
    }

    @Step("Отправляем подтверждение на разрешение агентстсву доступа к логину")
    public void getProveNewAgencyClients(String data) {
        getProveNewAgencyClients(new ProveNewAgencyClientsRequest().withData(data));
    }
}
