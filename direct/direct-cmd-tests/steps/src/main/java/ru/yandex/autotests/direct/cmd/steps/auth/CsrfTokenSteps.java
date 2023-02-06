package ru.yandex.autotests.direct.cmd.steps.auth;

import ru.yandex.autotests.direct.cmd.data.CSRFToken;
import ru.yandex.autotests.direct.cmd.steps.base.DirectBackEndSteps;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.qatools.allure.annotations.Step;

public class CsrfTokenSteps extends DirectBackEndSteps {

    @Step("Получение csrf-токена в кокаине (passport uid = {0})")
    public void obtainCsrfToken(String passportUid) {
        CSRFToken token = getCsrfToken(passportUid);
        setupContextCsrfToken(token);
    }

    private CSRFToken getCsrfToken(String passportUid) {
        String token = new DarkSideSteps(getContext().getProperties())
                .csrfSteps().getCsrfToken(passportUid);
        return new CSRFToken(token);
    }

    private void setupContextCsrfToken(CSRFToken token) {
        getContext().getAuthConfig().setCsrfToken(token);
    }

}
