package ru.yandex.autotests.innerpochta.utils.oper;

import ru.yandex.autotests.innerpochta.objstruct.oper.OperClass;
import ru.yandex.autotests.innerpochta.wmi.core.obj.Obj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.Oper;

import static ru.yandex.autotests.innerpochta.utils.SettingsProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;

/**
 * Created by IntelliJ IDEA.
 * User: lanwen
 * Date: 22.03.12
 * Time: 17:02
 * <p/>
 * Возвращает список запрошенных настроек пользователя.
 * Параметры запроса:
 * suid
 * mdb
 * settings_list
 * format
 */
@OperClass(
        apicommand = "get_params",
        apipath = "/",
        description = "Таблица user_parameters. Возвращает список запрошенных настроек пользователя"
)
public class GetParams extends Oper<GetParams> {

    public static GetParams getParams(Obj obj) {
        return api(GetParams.class).params(obj).setHost(props().settingsUri().toString());
    }
}
