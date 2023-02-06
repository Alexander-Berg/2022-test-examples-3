package ru.yandex.autotests.innerpochta.cal.api;

import io.restassured.response.Response;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.steps.beans.calAccount.CalAccount;
import ru.yandex.autotests.innerpochta.steps.beans.layer.Layer;
import ru.yandex.autotests.innerpochta.steps.beans.modelsdocreatelayer.CreateLayerBody;
import ru.yandex.autotests.innerpochta.steps.beans.modelsdocreatelayer.Model;
import ru.yandex.autotests.innerpochta.steps.beans.modelsdocreatelayer.Params;

import java.util.function.Function;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static ru.yandex.autotests.innerpochta.cal.api.CalApiConfig.apiConfig;
import static ru.yandex.autotests.innerpochta.cal.api.InfoHandler.infoHandler;
import static ru.yandex.autotests.innerpochta.cal.util.handlers.SettingsConsts.HANDLER_DO_CREATE_LAYER;

/**
 * @author cosmopanda
 */
public class CreateLayerHandler {

    private CreateLayerBody createLayerBody;
    private RestAssuredAuthRule filter;
    private CalAccount accInfo;

    private CreateLayerHandler() {
    }

    public static CreateLayerHandler createLayer() {
        return new CreateLayerHandler();
    }

    public CreateLayerHandler withAuth(RestAssuredAuthRule auth) {
        accInfo = infoHandler().withFilter(auth).callInfoHandler();
        filter = auth;
        return this;
    }

    public CreateLayerHandler withLayer(Layer layer) {
        createLayerBody = new CreateLayerBody().withModels(singletonList(
            new Model()
                .withName(HANDLER_DO_CREATE_LAYER)
                .withParams(new Params()
                    .withName(layer.getName())
                    .withColor(layer.getColor())
                    .withType(layer.getType())
                    .withAffectAvailability(layer.getAffectsAvailability())
                    .withIsDefault(layer.getIsDefault())
                    .withIsClosed(layer.getIsClosed())
                    .withNotifyAboutEventChanges(layer.getIsOwner())
                    .withIsEventsClosedByDefault(layer.getIsEventsClosedByDefault())
                    .withNotifications(emptyList())
                )
        ));
        return this;
    }

    public Response callCreateLayer() {
        return apiConfig()
            .docreatelayer()
            .withCreateLayerBody(createLayerBody)
            .withReq(req -> req.addFilter(filter))
            .withXyandexmayauidHeader(accInfo.getUid())
            .withXyandexmayackeyHeader(accInfo.getCkey())
            .post(Function.identity());
    }
}