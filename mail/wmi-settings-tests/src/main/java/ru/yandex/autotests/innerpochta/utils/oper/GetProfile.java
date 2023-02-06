package ru.yandex.autotests.innerpochta.utils.oper;

import com.jayway.jsonpath.JsonPath;
import org.apache.http.impl.client.DefaultHttpClient;
import ru.yandex.autotests.innerpochta.objstruct.oper.OperClass;
import ru.yandex.autotests.innerpochta.utils.beans.SignBean;
import ru.yandex.autotests.innerpochta.wmi.core.obj.Obj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.Oper;

import java.io.IOException;
import java.util.List;

import static org.eclipse.jetty.http.HttpStatus.OK_200;
import static ru.yandex.autotests.innerpochta.utils.SettingsProperties.props;
import static ru.yandex.autotests.innerpochta.utils.oper.UpdateProfile.SIGNS_SETTING;
import static ru.yandex.autotests.innerpochta.utils.SettingsApiObj.settings;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;

/**
 * Created by IntelliJ IDEA.
 * User: lanwen
 * Date: 22.03.12
 * Time: 17:02
 * <p/>
 * Возвращает список запрошенных настроек пользователя.
 * Параметры запроса:
 * suid - идентификатор пользователя
 * mdb - имя оракловой базы, на которой живет пользователь
 * settings_list - CSV с именами настроек, которые необходимо вернуть. В качестве разделителя используется запятая
 * format - формат выдачи. Возможные значения - json / xml
 */
@OperClass(
        apicommand = "get_profile",
        apipath = "/",
        description = "Таблица mail_profile. Возвращает список запрошенных настроек пользователя"
)
public class GetProfile extends Oper<GetProfile> {


    public static GetProfile getProfile(Obj obj) {
          return api(GetProfile.class).params(obj).setHost(props().settingsUri().toString());
    }

    public List<SignBean> signs() {
        Object read = JsonPath.read(respAsString, "settings.signs");
        return SignBean.fromJson(read);
    }

    public static List<SignBean> returnSignsFor(String uid) throws IOException {
        return signsInProfile(uid)
                .get().via(new DefaultHttpClient()).statusCodeShouldBe(OK_200).withDebugPrint().signs();
    }

    public static GetProfile signsInProfile(String uid) {
        return getProfile(settings(uid).settingsList(SIGNS_SETTING));
    }
}
