package ru.yandex.autotests.innerpochta.wmi.settings;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.SettingsSetupUpdateSomeObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsSetup;
import ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsSetupUpdateSome;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.ArrayList;
import java.util.List;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.SettingsValueMatcher.hasSetting;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.WaitForMatcherDecorator.withWaitFor;

/**
 * WMI-210
 */
@Aqua.Test
@Title("Тестирование настроек. Тестирование изменения настроек вебчата и show_unread.")
@Description("[WMI-210] Отдельное тестирование для настроек webchat_turned_off и show_unread")
@Features(MyFeatures.WMI)
@Stories(MyStories.SETTINGS)
@Issue("WMI-210")
@Credentials(loginGroup = "Lamertester")
public class WebchatAndUnreadMsgsSettingOnOff extends BaseTest {


    protected static List<String> settingsToCheck = new ArrayList<String>();


    /**
     * Список настроек
     */
    protected SettingsSetup settingsOper;

    @BeforeClass
    public static void init() throws Exception {
        settingsToCheck.add("webchat_turned_off");
        settingsToCheck.add("show_unread");
    }


    @Before
    public void prepare() throws Exception {
        settingsOper = jsx(SettingsSetup.class);
    }

    @Test
    @Description("Проверяет что есть такие настройки и они выключены")
    public void settingsWebchatUnreadMsgsDefaultOff() throws Exception {
        for (String setting : settingsToCheck) {
            assertThat(String.format("Settings %s is On, but expected: NOTHING ", setting), hc,
                    withWaitFor(hasSetting(setting, equalTo("")), SECONDS.toMillis(5)));
        }
    }

    @Test
    @Description("Проверяет что настройки\n" +
            "webchat_turned_off\n" +
            "show_unread включились\n" +
            "[!]Используем API ручку")
    public void settingsWebchatUnreadMsgsOn() throws Exception {
        for (String setting : settingsToCheck) {
            // Создание и выполнение объекта операции
            api(SettingsSetupUpdateSome.class)
                    .params(SettingsSetupUpdateSomeObj.getObjToTurnON(setting))
                    .post().via(hc);
        }

        for (String setting : settingsToCheck) {
            assertThat("Setting " + setting + " is OFF, but expected: ON" , hc,
                    withWaitFor(hasSetting(setting, equalTo("on")), SECONDS.toMillis(5)));
        }
    }

    @Test
    @Description("Проверяет что не остались включенными и все еще присутствуют\n" +
            "[!]Используем API ручку")
    public void settingsWebchatUnreadMsgsOff() throws Exception {
        for (String setting : settingsToCheck) {
            // Создание и выполнение объекта операции
            api(SettingsSetupUpdateSome.class)
                    .params(SettingsSetupUpdateSomeObj.getObjToTurnOFF(setting))
                    .post().via(hc);
        }
        for (String setting : settingsToCheck) {
            assertThat(String.format("Settings %s is On, but expected: NOTHING ", setting), hc,
                    withWaitFor(hasSetting(setting, equalTo("")), SECONDS.toMillis(5)));
        }
    }

}
