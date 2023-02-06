package ru.yandex.autotests.innerpochta.utils.oper;

import ru.yandex.autotests.innerpochta.objstruct.oper.OperClass;
import ru.yandex.autotests.innerpochta.wmi.core.obj.Obj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.Oper;

import static ru.yandex.autotests.innerpochta.utils.SettingsProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;

@OperClass(
        apicommand = "remove",
        apipath = "/",
        description = "Удаляем настройки пользователя"
)
public class Remove extends Oper<Remove> {
    public static Remove remove(Obj obj) {
        return api(Remove.class).params(obj).setHost(props().settingsUri().toString());
    }
}

