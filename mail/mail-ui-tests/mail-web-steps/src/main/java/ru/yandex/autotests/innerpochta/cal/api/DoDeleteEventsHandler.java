package ru.yandex.autotests.innerpochta.cal.api;

import io.restassured.response.Response;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.steps.beans.bodyReq.BodyReq;
import ru.yandex.autotests.innerpochta.steps.beans.bodyReq.Model;
import ru.yandex.autotests.innerpochta.steps.beans.bodyReq.Params;
import ru.yandex.autotests.innerpochta.steps.beans.calAccount.CalAccount;

import java.util.Collections;
import java.util.function.Function;

import static ru.yandex.autotests.innerpochta.cal.api.CalApiConfig.apiConfig;
import static ru.yandex.autotests.innerpochta.cal.api.InfoHandler.infoHandler;
import static ru.yandex.autotests.innerpochta.cal.util.handlers.SettingsConsts.HANDLER_DO_DELETE_EVENT;

/**
 * @author cosmopanda
 */
public class DoDeleteEventsHandler {

    private BodyReq deleteEventBody;
    private RestAssuredAuthRule filter;
    private CalAccount accInfo;

    private DoDeleteEventsHandler() {
    }

    public static DoDeleteEventsHandler deleteUserEvents() {
        return new DoDeleteEventsHandler();
    }

    /**
     * Делает активный вызов к календарю! Если вызвать до того как отработает рула авторизации, отвалится без кук
     *
     * @param auth
     * @return
     */
    public DoDeleteEventsHandler withAuth(RestAssuredAuthRule auth) {
        accInfo = infoHandler().withFilter(auth).callInfoHandler();
        filter = auth;
        return this;
    }

    public DoDeleteEventsHandler withEvent(Long eventId) {
        deleteEventBody = new BodyReq().withModels(Collections.singletonList(
            new Model()
                .withName(HANDLER_DO_DELETE_EVENT)
                .withParams(new Params().withId(eventId))
        ));
        return this;
    }

    public Response callDeleteEvents() {
        return apiConfig()
            .dodeleteevent()
            .withReq(req -> req.addFilter(filter).setBody(deleteEventBody))
            .withContentTypeHeader("application/json")
            .withXyandexmayauidHeader(accInfo.getUid())
            .withXyandexmayackeyHeader(accInfo.getCkey())
            .post(Function.identity());
    }
}
