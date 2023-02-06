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
 * Возвращает настройки из обеих таблиц
 * Параметры запроса:
 * suid
 * mdb
 * ask_validator - запрашивать ли поля, требующие валидации в чёрном ящике (например, список дефолтных адресов)
 * format
 */
@OperClass(
        apicommand = "get_all",
        apipath = "/",
        description = "Возвращает настройки из обеих таблиц (user_parameters, mail_profile)"
)
public class GetAll extends Oper<GetAll> {
    public static GetAll getAll(Obj params) {
        return api(GetAll.class).params(params).setHost(props().settingsUri().toString());
    }
}
