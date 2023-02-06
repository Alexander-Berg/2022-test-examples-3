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
 * Возвращает все настройки для заданного пользователя.
 * Параметры запроса:
 * suid
 * mdb
 * format
 */
@OperClass(
        apicommand = "get_all_params",
        apipath = "/",
        description = "Таблица user_parameters. Возвращает все настройки для заданного пользователя"
)
public class GetAllParams extends Oper<GetAllParams> {
    public static GetAllParams getAllParams(Obj params) {
        return api(GetAllParams.class).params(params).setHost(props().settingsUri().toString());
    }
}
