package ru.yandex.autotests.innerpochta.cal.api;

import io.restassured.response.Response;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.steps.beans.bodyReq.BodyReq;
import ru.yandex.autotests.innerpochta.steps.beans.bodyReq.Model;
import ru.yandex.autotests.innerpochta.steps.beans.calAccount.CalAccount;

import java.util.Collections;
import java.util.function.Function;

import static ru.yandex.autotests.innerpochta.cal.api.CalApiConfig.apiConfig;
import static ru.yandex.autotests.innerpochta.cal.api.InfoHandler.infoHandler;
import static ru.yandex.autotests.innerpochta.cal.util.handlers.SettingsConsts.HANDLER_GET_LAYERS;

/**
 * @author cosmopanda
 */
public class GetUserLayersHandler {

    private BodyReq body;
    private RestAssuredAuthRule filter;
    private CalAccount accInfo;

    private GetUserLayersHandler() {
        body = new BodyReq().withModels(Collections.singletonList(
            new Model().withName(HANDLER_GET_LAYERS)
        ));
    }

    public static GetUserLayersHandler getLayersHandler() {
        return new GetUserLayersHandler();
    }

    public GetUserLayersHandler withAuth(RestAssuredAuthRule auth) {
        accInfo = infoHandler().withFilter(auth).callInfoHandler();
        filter = auth;
        return this;
    }

    public Response callGetLayersHandler() {
        return apiConfig().getuserlayers()
            .withReq(req -> req.addFilter(filter).setBody(body))
            .withContentTypeHeader("application/json")
            .withXyandexmayauidHeader(accInfo.getUid())
            .withXyandexmayackeyHeader(accInfo.getCkey())
            .post(Function.identity());
    }
}
