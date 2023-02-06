package ru.yandex.autotests.innerpochta.cal.api;

import io.restassured.response.Response;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.steps.beans.calAccount.CalAccount;
import ru.yandex.autotests.innerpochta.steps.beans.modelsdoupdateusersettings.Model;
import ru.yandex.autotests.innerpochta.steps.beans.modelsdoupdateusersettings.Params;
import ru.yandex.autotests.innerpochta.steps.beans.modelsdoupdateusersettings.UpdateSettingsBody;

import java.util.Collections;
import java.util.function.Function;

import static ru.yandex.autotests.innerpochta.cal.api.CalApiConfig.apiConfig;
import static ru.yandex.autotests.innerpochta.cal.api.InfoHandler.infoHandler;
import static ru.yandex.autotests.innerpochta.cal.util.handlers.SettingsConsts.HANDLER_DO_SETTINGS;

/**
 * @author cosmopanda
 */
public class DoUpdateUserSettingsHandler {

    private UpdateSettingsBody body;
    private RestAssuredAuthRule filter;
    private CalAccount accInfo;

    private DoUpdateUserSettingsHandler() {
    }

    public static DoUpdateUserSettingsHandler doUpdateUserSettings() {
        return new DoUpdateUserSettingsHandler();
    }

    public DoUpdateUserSettingsHandler withAuth(RestAssuredAuthRule auth) {
        accInfo = infoHandler().withFilter(auth).callInfoHandler();
        filter = auth;
        return this;
    }

    public DoUpdateUserSettingsHandler withSettings(Params params) {
        //TODO: сделать так, чтобы не уходили null, если не определили настройку.
        //сейчас определяем разворот левой колонки, иначе она сворачивается всегда
        if (params.getIsAsideExpanded() == null){
            params.setIsAsideExpanded(true);
        }
        body = new UpdateSettingsBody().withModels(Collections.singletonList(
            new Model()
                .withName(HANDLER_DO_SETTINGS)
                .withParams(params)
        ));
        return this;
    }

    public Response callUpdateUserSettings() {
        return apiConfig()
            .doupdateusersettings()
            .withUpdateSettingsBody(body)
            .withReq(req -> req.addFilter(filter))
            .withXyandexmayauidHeader(accInfo.getUid())
            .withXyandexmayackeyHeader(accInfo.getCkey())
            .post(Function.identity());
    }
}