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
import static ru.yandex.autotests.innerpochta.cal.util.handlers.SettingsConsts.HANDLER_DO_DELETE_LAYER;

/**
 * @author cosmopanda
 */
public class DoDeleteLayerHandler {

    private BodyReq deleteLayerBody;
    private RestAssuredAuthRule filter;
    private CalAccount accInfo;

    private DoDeleteLayerHandler() {
    }

    public static DoDeleteLayerHandler deleteLayer() {
        return new DoDeleteLayerHandler();
    }

    public DoDeleteLayerHandler withAuth(RestAssuredAuthRule auth) {
        accInfo = infoHandler().withFilter(auth).callInfoHandler();
        filter = auth;
        return this;
    }

    public DoDeleteLayerHandler withLayerID(Long layerID) {
        deleteLayerBody = new BodyReq().withModels(Collections.singletonList(
            new Model()
                .withName(HANDLER_DO_DELETE_LAYER)
                .withParams(new Params().withId(layerID))
        ));
        return this;
    }

    public Response callDeleteLayer() {
        return apiConfig()
            .dodeletelayer()
            .withReq(req -> req.addFilter(filter).setBody(deleteLayerBody))
            .withContentTypeHeader("application/json")
            .withXyandexmayauidHeader(accInfo.getUid())
            .withXyandexmayackeyHeader(accInfo.getCkey())
            .post(Function.identity());
    }
}

