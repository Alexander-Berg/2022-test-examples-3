package ru.yandex.autotests.innerpochta.wmi.filters;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.SettingsFilterCheckPasswordObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsFilterCheckPassword;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;

@Aqua.Test
@Title("Проверка пароля для фильтров")
@Description("Тестирование на разных пользователях")
@RunWith(Parameterized.class)
@Features(MyFeatures.WMI)
@Stories(MyStories.FILTERS)
public class FilterCheckPwdTesting extends BaseTest {

    @Parameterized.Parameters(name = "user: {0}")
    public static Collection<Object[]> data() {
        List<Object[]> groups = new ArrayList<Object[]>();
        groups.add(new Object[]{"MultiActionFilter"});
        groups.add(new Object[]{"NonEqualMailPortalName"});
        groups.add(new Object[]{"NonEqDottedName"});

        return groups;
    }

    @Parameterized.Parameter
    public String groupName;

    private String password;

    @Before
    public void prepare() {
        authClient.with(groupName).login();
        hc = authClient.authHC();
        password = authClient.acc().getPassword();
    }


    @Test
    @Title("Проверка верного и неверного пользовательского пароля")
    @Issues({@Issue("DARIA-2234"), @Issue("DARIA-37897")})
    @Description("В некоторых случаях будет приходить пароль " +
            "(если правило настроено на пересылку, уведомление или автоовет).\n" +
            "С этим паролем надо ходить в блекбокс\n" +
            "- и если тот ответил ОК - правило сохранять.\n" +
            "- в противном случае возвращать ошибку.\n" +
            "settings_filter_check_password\n" +
            "[DARIA-2234]\n" +
            "[DARIA-37897]")
    public void passwordCheckingMethod() throws Exception {
        logger.warn("Проверка верного пользовательского пароля [DARIA-21045][DARIA-37897]");
        assertThat("Проверка верного пользовательского пароля провалилась [DARIA-21045][DARIA-37897]",
                jsx(SettingsFilterCheckPassword.class)
                        .params(SettingsFilterCheckPasswordObj.withPwd(password))
                        .post().via(hc).getCheckResult(), equalTo("true"));
        logger.warn("Проверка неверного пользовательского пароля");
        assertThat("Password check on random string gots that all ok [DARIA-21045][DARIA-37897]",
                jsx(SettingsFilterCheckPassword.class)
                        .params(SettingsFilterCheckPasswordObj.withPwd(Util.getRandomString()))
                        .post().via(hc).getCheckResult(), not(equalToIgnoringCase("true")));
    }

}