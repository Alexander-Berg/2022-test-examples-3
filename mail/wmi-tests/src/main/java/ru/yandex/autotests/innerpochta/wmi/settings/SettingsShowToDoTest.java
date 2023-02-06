package ru.yandex.autotests.innerpochta.wmi.settings;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.oper.ComposeCheck;
import ru.yandex.autotests.innerpochta.wmi.core.rules.SSHRule;
import ru.yandex.qatools.allure.annotations.*;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.SettingsValueMatcher.hasSetting;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.WaitForMatcherDecorator.withWaitFor;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.onlyapi.SettingsUpdateTodo.settingsUpdateTodo;

/* CAL-4660
 * нужно убедиться что на машине есть ssh доступ
 */

@Aqua.Test
@Title("Тестирование настроек. Установка настройки отображения туду спецовой ручкой")
@Description("Тестируется по ssh. Устанавливает пользователю настройку отображения тудушницы")
@Features(MyFeatures.WMI)
@Stories(MyStories.SETTINGS)
@Credentials(loginGroup = "Group1")
public class SettingsShowToDoTest extends BaseTest {

    public static final String SHOW_TODO_SETTING_NAME = "show_todo";
    public static final String ON = "on";
    public static final String OFF = "off";

    @Test
    @Issue("CAL-4660")
    @Description("CAL-4660\n" +
            "Проверка ручки settings_update_todo?uid=146152574&show_todo=off\n" +
            "Работает только с определенных машин, тестировать вручную через wget\n" +
            "изменяет настройку отоображения ТУДУ\n" +
            "show_todo")
    public void testUpdateSettingTodo() throws Exception {
        String uid = getUid();
        logger.warn("Включаем и выключаем чат ручкой, работающей без авторизации [CAL-4660]");

        settingsUpdateTodo(sshAuthRule.ssh().conn(), uid, ON);
        assertThat(hc, withWaitFor(hasSetting(SHOW_TODO_SETTING_NAME, ON), SECONDS.toMillis(5)));
        settingsUpdateTodo(sshAuthRule.ssh().conn(), uid, OFF);
        assertThat(hc, withWaitFor(hasSetting(SHOW_TODO_SETTING_NAME, OFF), SECONDS.toMillis(5)));
    }

    /**
     * Возвращает Uid пользователя.
     *
     * @return Striing uid пользователя
     * @throws Exception
     */
    private String getUid() throws Exception {
        return api(ComposeCheck.class).post().via(hc).getUid();
    }

}