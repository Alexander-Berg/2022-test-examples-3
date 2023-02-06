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
import ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsSetupUpdateSome;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.SettingsValueMatcher.hasSetting;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.WaitForMatcherDecorator.withWaitFor;

/**
 * Тестирование последовательного изменения группы определенных настроек
 * Не подходит в текущем виде для корпа
 */
@Aqua.Test
@Title("Тестирование настроек. Тестирование последовательного изменения группы настроек.")
@Description("Изменяем цепочку настроек. Проверяем что все изменилось")
@Features(MyFeatures.WMI)
@Stories(MyStories.SETTINGS)
@Credentials(loginGroup = "ChainSettingsChange")
public class UserSettingsTest extends BaseTest {

    private static String login;

    /**
     * Объект настройки
     */
    private SettingsSetupUpdateSomeObj updObj;
    /**
     * Объект операции изменения
     */
    private SettingsSetupUpdateSome updOper;

    @BeforeClass
    public static void init() throws Exception {
        login = authClient.acc().getLogin();
    }

    @Before
    public void prepare() throws Exception {
        // Создание объекта настройки
        updObj = SettingsSetupUpdateSomeObj.settings();
        // Создание объекта операции
        updOper = jsx(SettingsSetupUpdateSome.class).params(updObj);
    }

    @Test
    @Description("Последовательно меняем дефолтный адрес и 2 раза цветовую схему\n" +
            "- Смотрим что дефолтный адрес изменился")
    public void testSpecifiedChanges() throws Exception {
        // Изменение настройки
        updObj.setUpdates("default_email", login + "@ya.ru");
        updOper.post().via(hc);

        // Изменение настройки
        updObj.setUpdates("color_scheme", "classic");
        updOper.post().via(hc);

        // Изменение настройки
        updObj.setUpdates("color_scheme", "blue");
        updOper.post().via(hc);

        // Проверка
        startCheckingSettingUpdate("default_email", login + "@ya.ru");
    }

    @Test
    @Description("Изменение домена пользователя на ya.ru и обратно\n" +
            "Проверка, что в обоих случаях все ок")
    public void optionChanging() throws Exception {
        // Изменение настройки
        updObj.setUpdates("default_email", login + "@ya.ru");
        updOper.post().via(hc);

        // Проверка
        startCheckingSettingUpdate("default_email", login + "@ya.ru");

        // Изменение настройки
        updObj.setUpdates("default_email", login + "@yandex.ru");
        updOper.post().via(hc);

        // Проверка
        startCheckingSettingUpdate("default_email", login + "@yandex.ru");
    }


    /**
     * Запуск проверки изменения настройки
     *
     * @param opt           - настройка для проверки
     * @param expectedValue - что ожидаем получить
     */
    private void startCheckingSettingUpdate(String opt, String expectedValue) {
        assertThat(hc, withWaitFor(hasSetting(opt, expectedValue), SECONDS.toMillis(5)));
    }
}
