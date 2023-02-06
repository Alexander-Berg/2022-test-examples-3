package ru.yandex.autotests.innerpochta.wmi.settings;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsSetup;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;

import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 06.12.13
 * Time: 16:07
 */
@Aqua.Test
@Title("Тестирование настроек. Тестирование настройки pop3_archivate")
@Description("Пока только проверяем наличие настройки")
@Features(MyFeatures.WMI)
@Stories(MyStories.SETTINGS)
@Credentials(loginGroup = "Pop3Archivate")
public class SettingsPop3ArchivateTest extends BaseTest {

    public static final String ARCHIVATE_SETTING_NAME = "pop3_archivate";
    public static final String ON = "on";
    public static final String OFF = "off";

    @Test
    @Description("Проверяем наличие настройки pop3_archivate по умолчанию\n" +
            "Ожидаемый результат: должно быть \"on\"")
    public void pop3ArchivateDefaultOnTest() throws IOException {
        jsx(SettingsSetup.class).post().via(hc).settingShouldBe(ARCHIVATE_SETTING_NAME, ON);
    }
}
