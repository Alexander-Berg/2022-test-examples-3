package ru.yandex.autotests.innerpochta.cal.api;

import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.steps.beans.calAccount.CalAccount;
import ru.yandex.autotests.innerpochta.steps.beans.qa.ApiQa;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static ru.yandex.autotests.innerpochta.api.RestAssuredLoggingFilter.log;
import static ru.yandex.autotests.innerpochta.cal.api.CalApiConfig.apiInfoConfig;
import static ru.yandex.autotests.innerpochta.steps.api.ApiDefaultSteps.getJsonPathConfig;
import static ru.yandex.autotests.innerpochta.util.Utils.isCorp;

/**
 * @author cosmopanda
 * Вспомогательная ручка, специально для qa - чтобы получить информацию о пользователе.
 * Дергается только внутри остальных ручек. В других пакетах не используется.
 */
class InfoHandler {

    static private InfoHandler infoHandler;
    private RestAssuredAuthRule filter;
    private Map<String, CalAccount> accounts = new HashMap<>();

    private InfoHandler() {
    }

    static InfoHandler infoHandler() {
        if (infoHandler == null) {
            infoHandler = new InfoHandler();
        }
        return infoHandler;
    }

    InfoHandler withFilter(RestAssuredAuthRule filter) {
        this.filter = filter;
        return this;
    }

    CalAccount callInfoHandler() {
        if (accounts.containsKey(filter.getLogin())) {
            return accounts.get(filter.getLogin());
        }
        ApiQa baseRequest = apiInfoConfig().qa()
            .withReq(req -> req.addFilter(filter).addFilter(log()));
        if (isCorp())
            baseRequest
                .withHostHeader("calendar.qa.yandex-team.ru")
                .withConnectionHeader("keep-alive");
        else
            baseRequest
                .withDefaults();
        CalAccount result = baseRequest
            .get(Function.identity()).then().extract().jsonPath(getJsonPathConfig())
            .getObject(".", CalAccount.class);
        accounts.put(filter.getLogin(), result);
        return result;
    }
}
