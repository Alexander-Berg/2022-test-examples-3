package ru.yandex.autotests.innerpochta.utils.oper;

import org.json.JSONArray;
import ru.yandex.autotests.innerpochta.objstruct.oper.OperClass;
import ru.yandex.autotests.innerpochta.wmi.core.obj.Obj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.Oper;

import java.util.List;

import static ru.yandex.autotests.innerpochta.utils.SettingsProperties.props;
import static ru.yandex.autotests.innerpochta.utils.SettingsApiObj.settings;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;

@OperClass(
        apicommand = "delete_params",
        apipath = "/",
        description = "Удаляет заданные параметры пользователя"
)
public class DeleteParams extends Oper<DeleteParams> {

    public static DeleteParams deleteParams(Obj params) {
        return api(DeleteParams.class).params(params).setHost(props().settingsUri().toString());
    }

    public static DeleteParams deleteParamsWithSettingsList(String uid, List<String> params)  {
        return deleteParams(settings(uid).setContent(new JSONArray(params).toString()));
    }
}
