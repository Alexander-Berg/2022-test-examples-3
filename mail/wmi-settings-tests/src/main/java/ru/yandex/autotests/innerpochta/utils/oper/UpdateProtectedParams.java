package ru.yandex.autotests.innerpochta.utils.oper;

import ru.yandex.autotests.innerpochta.objstruct.oper.OperClass;
import ru.yandex.autotests.innerpochta.wmi.core.consts.Headers;
import ru.yandex.autotests.innerpochta.wmi.core.obj.Obj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.Oper;

import java.io.UnsupportedEncodingException;

import static com.google.common.base.Joiner.on;
import static java.net.URLEncoder.encode;
import static java.nio.charset.StandardCharsets.UTF_8;
import static ru.yandex.autotests.innerpochta.utils.SettingsProperties.props;
import static ru.yandex.autotests.innerpochta.utils.oper.UpdateProfile.JSON_DATA;
import static ru.yandex.autotests.innerpochta.utils.SettingsApiObj.settings;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;


@OperClass(
        apicommand = "update_protected_params",
        apipath = "/",
        description = "Таблица user_parameters. Обновляет заданные настройки у пользователя"
)
public class UpdateProtectedParams extends Oper<UpdateProtectedParams> {

    public static UpdateProtectedParams updateOneProtectedParamsSetting(String uid, String name, String change) {
        return updateProtectedParams(settings(uid).set(true, name, change))
                .header(Headers.CLIENT_TYPE, "aqua-tests");
    }

    public static UpdateProtectedParams updateProtectedParams(Obj params) {
        return api(UpdateProtectedParams.class).params(params).setHost(props().settingsUri().toString());
    }

    public static UpdateProtectedParams updateProtectedParamsWithJsonData(String uid, String change) throws UnsupportedEncodingException {
        return updateProtectedParams(settings(uid).remove("format").setContent(on("=")
                .join(JSON_DATA, encode(change, UTF_8.toString()))))
                .header("Content-Type", "application/x-www-form-urlencoded");
    }

}
