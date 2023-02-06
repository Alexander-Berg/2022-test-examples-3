package ru.yandex.autotests.innerpochta.wmi.settings;

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

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.SettingsValueMatcher.hasSetting;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.WaitForMatcherDecorator.withWaitFor;

/**
 * <p>Нужно проверять на логине с точкой.
 * <p>Как воспроизвести
 * <p>Заходим в Настройки->Информация об отправителе. По-умолчанию стоит адрес отправителя name.family@yandex.ru.
 * <p>Меняем на name-family@yandex.ru. Перезагружаем страницу. Настройка сбросилась.
 * <p>Должно: сохранилось
 * <p>Fixed in WMI-333
 */
@Aqua.Test
@Title("Тестирование настроек. Проверка сохранения изменений алиасов имени - дефис или точка.")
@Description("Пофикшено в [WMI-333]. Не сохранялась настройка дефис или точка в имени")
@Features(MyFeatures.WMI)
@Stories(MyStories.SETTINGS)
@Issue("WMI-333")
@Credentials(loginGroup = "DotNamed")
public class DotNamedEmailAliasChangingTest extends BaseTest {

    private static String settingsName = "default_email";

    @Test
    @Description("Меняет значение на значение с точкой,\n" +
            "- смотрит что равно\n" +
            "Меняет на логин с \"-\"\n" +
            "- смотрит что изменилось")
    public void settingsWebchatUnreadMsgsDefaultOff() throws Exception {
        assertThat("Логин должен содержать точу или дефис",
                authClient.acc().getLogin(), anyOf(containsString("."), containsString("-")));

        logger.warn("Меняем настройку на логин с точкой");

        SettingsSetupUpdateSomeObj updSetting = SettingsSetupUpdateSomeObj
                .settings().setUpdates(settingsName, authClient.acc().getSelfEmail());
        SettingsSetupUpdateSome updOper = jsx(SettingsSetupUpdateSome.class).params(updSetting);
        updOper.post().via(hc);

        assertThat("Неверное значение настройки " + settingsName, hc,
                withWaitFor(hasSetting(settingsName, equalTo(authClient.acc().getSelfEmail())), SECONDS.toMillis(5)));

        logger.warn("Меняем настройку на логин с дефисом");
        String newSettingToSet = authClient.acc().getLogin().replace(".", "-") + "@" +
                authClient.acc().getDomain();
        updSetting.setUpdates(settingsName, newSettingToSet);
        updOper.post().via(hc);

        assertThat("Неверное значение настройки " + settingsName, hc,
                withWaitFor(hasSetting(settingsName, equalTo(newSettingToSet)), SECONDS.toMillis(5)));
    }

}
