package ru.yandex.autotests.innerpochta.cal.api;

import io.restassured.response.Response;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.steps.beans.bodyReq.BodyReq;
import ru.yandex.autotests.innerpochta.steps.beans.bodyReq.Model;
import ru.yandex.autotests.innerpochta.steps.beans.bodyReq.Params;
import ru.yandex.autotests.innerpochta.steps.beans.calAccount.CalAccount;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static ru.yandex.autotests.innerpochta.cal.api.CalApiConfig.apiConfig;
import static ru.yandex.autotests.innerpochta.cal.api.InfoHandler.infoHandler;
import static ru.yandex.autotests.innerpochta.cal.util.handlers.SettingsConsts.HANDLER_GET_EVENT;

/**
 * @author cosmopanda
 */
public class GetUserEventsHandler {

    private BodyReq getEventBody;
    private RestAssuredAuthRule filter;
    private CalAccount accInfo;

    private GetUserEventsHandler() {
        getEventBody = new BodyReq().withModels(Collections.singletonList(
            new Model().withName(HANDLER_GET_EVENT).withParams(
                new Params().withFrom("2010-01-01").withTo("2025-01-01")
            )));
    }

    public static GetUserEventsHandler getUserEvents() {
        return new GetUserEventsHandler();
    }

    public GetUserEventsHandler withAuth(RestAssuredAuthRule auth) {
        accInfo = infoHandler().withFilter(auth).callInfoHandler();
        filter = auth;
        return this;
    }

    public GetUserEventsHandler withDate(String fromDate, String toDate) {
        getEventBody.getModels().get(0).getParams().setFrom(fromDate);
        getEventBody.getModels().get(0).getParams().setTo(toDate);
        return this;
    }

    public GetUserEventsHandler withLayers(List<Long> layers) {
        getEventBody.getModels().get(0).getParams().setLayerId(layers);
        return this;
    }

    public Response callGetEvents() {
        return apiConfig()
            .getevents()
            .withReq(req -> req.addFilter(filter).setBody(getEventBody))
            .withContentTypeHeader("application/json")
            .withXyandexmayauidHeader(accInfo.getUid())
            .withXyandexmayackeyHeader(accInfo.getCkey())
            .post(Function.identity());
    }
}