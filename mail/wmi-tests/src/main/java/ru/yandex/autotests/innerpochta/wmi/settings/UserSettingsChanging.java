package ru.yandex.autotests.innerpochta.wmi.settings;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.SettingsSetupUpdateSomeObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsSetupUpdateSome;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.BackupSettingWithWmiRule;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Arrays;
import java.util.Collection;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.SettingsValueMatcher.hasSetting;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.WaitForMatcherDecorator.withWaitFor;

@Aqua.Test
@Title("Тестирование настроек. Тестирование различных настроек.")
@Description("Изменяем различные настройки и проверяем что они сохранились")
@RunWith(value = Parameterized.class)
@Features(MyFeatures.WMI)
@Stories(MyStories.SETTINGS)
@Credentials(loginGroup = "UserSettingsChanging")
public class UserSettingsChanging extends BaseTest {

    private String option;
    private String firstChanging;
    private String secondChanging;

    //@params: имя настройки, первое изменение настройки, второе изменение настройки
    @Parameters
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][]
                {
                        new Object[]{"first_login", "on", "off"},
                        new Object[]{"color_scheme", Util.getRandomString(), Util.getRandomString()},
                        new Object[]{"color_scheme", "classic", "blue"},
                        new Object[]{"from_name", Util.getRandomString(), "yandex"},
                        new Object[]{"signature", Util.getRandomString(), "signature"},
                        new Object[]{"default_mailbox", "ya.ru", "yandex.ru"},
                        new Object[]{"messages_per_page", "200", "45"},
                        new Object[]{"page_after_delete", "current_list", "next_message"},
                        new Object[]{"page_after_delete", "deleted_list", "inbox"},
                        new Object[]{"page_after_move", "current_list", "next_message"},
                        new Object[]{"page_after_move", "source_folder", "dest_folder"},
                        new Object[]{"page_after_send", "done", "current_list"},
                        new Object[]{"page_after_send", "sent_list", "done"},
                        new Object[]{"dnd_enabled", "on", "off"},
                        new Object[]{"show_chat", "on", "off"},
                        new Object[]{"show_news", "on", "off"},
                        new Object[]{"show_avatars", "on", "off"},
                        new Object[]{"subs_show_unread", "on", "off"},
                        new Object[]{"subs_show_item", "on", "off"},
                        new Object[]{"enable_firstline", "on", "off"},
                        new Object[]{"enable_autosave", "on", "off"},
                        new Object[]{"save_sent", "on", "off"},
                        new Object[]{"use_monospace_in_text", "on", "off"},
                        new Object[]{"pop3_archivate", "off", "on"},
                };
        return Arrays.asList(data);
    }

    public UserSettingsChanging(String option, String firstChanging, String secondChanging) {
        this.option = option;
        this.firstChanging = firstChanging;
        this.secondChanging = secondChanging;
        backup.backup(option);
    }

    @Rule
    public BackupSettingWithWmiRule backup = BackupSettingWithWmiRule.with(authClient).defaultOff();

    @Test
    @Description("Тестируем изменение настроек.\n" +
            "Для настроек выполняем следующее:\n" +
            "изменяем настройку\n" +
            "проверяем, что настройка изменилась\n" +
            "еще раз изменяем настройку\n" +
            "проверяем, что настройка изменилась")
    public void optionChanging() throws Exception {
        // Создание объекта настройки
        SettingsSetupUpdateSomeObj updObj = SettingsSetupUpdateSomeObj
                .settings().setUpdates(option, firstChanging);
        // Создание объекта операции
        SettingsSetupUpdateSome updOper = jsx(SettingsSetupUpdateSome.class).params(updObj);
        // Изменение настройки
        updOper.post().via(hc);

        // Проверка что настройка изменилась
        startCheckingSettingUpdate(option, firstChanging);

        // Назначение второго изменения настройки
        updObj.setUpdates(option, secondChanging);
        updOper.post().via(hc);

        startCheckingSettingUpdate(option, secondChanging);
    }

    /**
     * Запуск проверки изменения настройки
     *
     * @param opt           - настройка для проверки
     * @param expectedValue - что ожидаем получить
     * @throws InterruptedException - возможно при прерывании задачи
     */
    private void startCheckingSettingUpdate(String opt, String expectedValue) throws InterruptedException {
        assertThat(hc, withWaitFor(hasSetting(opt, expectedValue), SECONDS.toMillis(5)));
    }
}