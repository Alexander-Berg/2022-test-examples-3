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


/**
 * Created by IntelliJ IDEA.
 * User: lanwen
 * Date: 22.03.12
 * Time: 17:02
 * <p/>
 * Обновляет заданные настройки у пользователя
 * Параметры запроса:
 * suid
 * mdb
 * <param_name>
 */
@OperClass(
        apicommand = "update_params",
        apipath = "/",
        description = "Таблица user_parameters. Обновляет заданные настройки у пользователя"
)
public class UpdateParams extends Oper<UpdateParams> {

    public static UpdateParams updateOneParamsSetting(String uid, String name, String change) {
        return updateParams(settings(uid).set(true, name, change))
                .header(Headers.CLIENT_TYPE, "aqua-tests");
    }

    public static UpdateParams updateParams(Obj params) {
        return api(UpdateParams.class).params(params).setHost(props().settingsUri().toString());
    }

    public static UpdateParams updateParamsWithJsonData(String uid, String change) throws UnsupportedEncodingException {
        return updateParams(settings(uid).remove("format").setContent(on("=")
                .join(JSON_DATA, encode(change, UTF_8.toString()))))
                .header("Content-Type", "application/x-www-form-urlencoded");
    }

}
