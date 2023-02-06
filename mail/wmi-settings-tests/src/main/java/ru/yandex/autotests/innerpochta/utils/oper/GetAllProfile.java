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
 * ask_validator - запрашивать ли поля, требующие валидации в чёрном ящике (например, список дефолтных адресов)
 * format
 */
@OperClass(
        apicommand = "get_all_profile",
        apipath = "/",
        description = "Таблица mail_profile. Возвращает все настройки для заданного пользователя"
)
public class GetAllProfile extends Oper<GetAllProfile> {

        public static GetAllProfile getAllProfile(Obj params) {
                return api(GetAllProfile.class).params(params).setHost(props().settingsUri().toString());
        }
}
