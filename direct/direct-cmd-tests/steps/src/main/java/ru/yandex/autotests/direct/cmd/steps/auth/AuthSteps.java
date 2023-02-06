package ru.yandex.autotests.direct.cmd.steps.auth;

import ru.yandex.autotests.direct.cmd.steps.base.DirectBackEndSteps;
import ru.yandex.autotests.direct.cmd.steps.base.DirectStepsContext;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.allure.annotations.Step;

public class AuthSteps extends DirectBackEndSteps {

    private PassportSteps passportSteps;
    private CsrfTokenSteps csrfTokenSteps;

    @Override
    protected void init(DirectStepsContext context) {
        super.init(context);
        passportSteps = getInstance(PassportSteps.class, context);
        csrfTokenSteps = getInstance(CsrfTokenSteps.class, context);
    }

    @Step("Аутентификация в паспорте + получение токена в кокаине")
    public void authenticate(User user) {
        passportSteps.authoriseAs(user.getLogin(), user.getPassword());
    }


}
