package ru.yandex.autotests.innerpochta.utils.oper;

import ru.yandex.autotests.innerpochta.objstruct.oper.OperClass;
import ru.yandex.autotests.innerpochta.wmi.core.obj.Obj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.Oper;

import java.io.UnsupportedEncodingException;

import static com.google.common.base.Joiner.on;
import static java.net.URLEncoder.encode;
import static java.nio.charset.StandardCharsets.UTF_8;
import static ru.yandex.autotests.innerpochta.utils.SettingsProperties.props;
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
 * <param_name> - набор параметров, для которых имя совпадает с именем настройки;
 * каждый параметр содержит обновлённое значение.
 */
@OperClass(
        apicommand = "update_profile",
        apipath = "/",
        description = "Таблица mail_profile. Обновляем заданные настройки у пользователя"
)
public class UpdateProfile extends Oper<UpdateProfile> {
    public static final String SIGNS_SETTING = "signs";
    public static final String JSON_DATA = "json_data";


    public static UpdateProfile updateProfile(Obj params) {
        return api(UpdateProfile.class).params(params).setHost(props().settingsUri().toString());
    }

    public static UpdateProfile updateOneProfileSetting(String uid, String name, String change) throws UnsupportedEncodingException {
        return updateProfile(settings(uid).add(true, name, encode(change, UTF_8.toString())));
    }


    public static UpdateProfile updateSign(String uid, String change) {
        return updateProfile(settings(uid).remove("format").setContent(on("=").join(SIGNS_SETTING, change)))
                .header("Content-Type", "application/x-www-form-urlencoded");
    }


    public static UpdateProfile updateJsonData(String uid, String change) {
        return updateProfile(settings(uid).remove("format").setContent(on("=").join(JSON_DATA, change)))
                .header("Content-Type", "application/x-www-form-urlencoded");
    }
}
