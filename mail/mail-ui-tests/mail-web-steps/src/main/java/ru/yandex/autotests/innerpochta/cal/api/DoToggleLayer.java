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
import static ru.yandex.autotests.innerpochta.cal.util.handlers.SettingsConsts.HANDLER_DO_TOGGLE_LAYER;

/**
 * @author cosmopanda
 */
public class DoToggleLayer {

    private BodyReq toggleLayer;
    private RestAssuredAuthRule filter;
    private CalAccount accInfo;

    private DoToggleLayer() {
    }

    public static DoToggleLayer doToggleLayer() {
        return new DoToggleLayer();
    }

    public DoToggleLayer withAuth(RestAssuredAuthRule auth) {
        accInfo = infoHandler().withFilter(auth).callInfoHandler();
        filter = auth;
        return this;
    }

    public DoToggleLayer withLayerId(Long layerId, Boolean status) {
        toggleLayer = new BodyReq().withModels(Collections.singletonList(
            new Model()
                .withName(HANDLER_DO_TOGGLE_LAYER)
                .withParams(new Params().withId(layerId).withOn(status)))
        );
        return this;
    }

    public Response callDoToggleLayer() {
        return apiConfig()
            .getevents()
            .withReq(req -> req.addFilter(filter).setBody(toggleLayer))
            .withContentTypeHeader("application/json")
            .withXyandexmayauidHeader(accInfo.getUid())
            .withXyandexmayackeyHeader(accInfo.getCkey())
            .post(Function.identity());
    }
}