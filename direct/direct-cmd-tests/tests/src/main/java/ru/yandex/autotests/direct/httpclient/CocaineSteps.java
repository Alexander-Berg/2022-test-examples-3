package ru.yandex.autotests.direct.httpclient;

import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;

public class CocaineSteps {
    public static CSRFToken getCsrfTokenFromCocaine(String uid) {
        return new CSRFToken(new DarkSideSteps().csrfSteps().getCsrfToken(uid));
    }
}
