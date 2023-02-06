package ru.yandex.autotests.direct.cmd.steps.client;

import ru.yandex.autotests.direct.cmd.data.CMD;
import ru.yandex.autotests.direct.cmd.data.clients.AjaxCreateClientRequest;
import ru.yandex.autotests.direct.cmd.data.clients.ClientData;
import ru.yandex.autotests.direct.cmd.data.clients.SaveClientIDRequest;
import ru.yandex.autotests.direct.cmd.data.clients.SearchClientIDRequest;
import ru.yandex.autotests.direct.cmd.data.commons.CommonResponse;
import ru.yandex.autotests.direct.cmd.data.redirect.RedirectResponse;
import ru.yandex.autotests.direct.cmd.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

public class ClientsSteps extends DirectBackEndSteps {

    @Step("GET cmd = searchClientID")
    public RedirectResponse getSearchClientIDFromCreateUser(String login, Integer country, String currency) {
        SearchClientIDRequest request = new SearchClientIDRequest()
                .withClientLogin(login)
                .withSearchCountry(country)
                .withSearchCurrency(currency)
                .withSubmitCreate("Сохранить")
                .withFrom("createUser");
        return get(CMD.SEARCH_CLIENT_ID, request, RedirectResponse.class);
    }

    @Step("GET cmd = saveClientID")
    public RedirectResponse getSaveClientIDFromCreateUser(String login, Integer country, String currency) {
        SaveClientIDRequest request = new SaveClientIDRequest()
                .withClientLogin(login)
                .withClientCountry(country)
                .withCurrency(currency)
                .withFrom("createUser");
        return get(CMD.SAVE_CLIENT_ID, request, RedirectResponse.class);
    }

    @Step("Создание клиента {2} с country = {0}, currency = {1}")
    public void ajaxCreateClient(Long country, String currency, String uLogin) {
        CommonResponse response = postAjaxCreateClient(new AjaxCreateClientRequest()
                .withClientData(new ClientData()
                        .withCountry(country)
                        .withCurrency(currency)
                        .withEmail("")
                        .withGdprAgreementAccepted(1)

                )
                .withUlogin(uLogin));
        assumeThat("создание завершилось успешно", response.getSuccess(), equalTo("1"));
    }

    @Step("POST cmd=ajaxCreateClient (создание клиента)")
    public CommonResponse postAjaxCreateClient(AjaxCreateClientRequest request) {
        return post(CMD.AJAX_CREATE_CLIENT, request, CommonResponse.class);
    }

}
