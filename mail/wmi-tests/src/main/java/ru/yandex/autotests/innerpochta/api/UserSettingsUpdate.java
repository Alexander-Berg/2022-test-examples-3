package ru.yandex.autotests.innerpochta.api;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.SettingsSetupUpdateSomeObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsSetup;
import ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsSetupUpdateSome;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;

@Aqua.Test
@Title("[API] Изменение настроек")
@Description("Редактирование подписи")
@Features(MyFeatures.API_WMI)
@Stories(MyStories.SETTINGS)
@Credentials(loginGroup = UserSettingsUpdate.GROUP_NAME)
public class UserSettingsUpdate extends BaseTest {

    public static final String GROUP_NAME = "ApiFunkTest";

    @Test
    @Description("Апдейтит настройку signature через мобильный httpClient\n" +
            "ставит случайную строку и сравнивает значение")
    public void updateSettings() throws Exception {
        String value = Util.getRandomString();
        api(SettingsSetupUpdateSome.class).params(SettingsSetupUpdateSomeObj.settings().setUpdates("signature", value))
                .post().via(hc);

        api(SettingsSetup.class).post().via(hc).and().settingShouldBe("signature", value);
    }
}
