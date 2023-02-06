package ru.yandex.autotests.direct.httpclient;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.UrlPath;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.OldTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.data.CMD;
import ru.yandex.autotests.direct.httpclient.data.Logins;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 07.10.14
 *         https://st.yandex-team.ru/TESTIRT-2958
 */

@Aqua.Test
@Description("Проверка редиректов контроллера advertize")
@Stories(TestFeatures.Redirects.ADVERTIZE_REDIRECTS)
@Features(TestFeatures.REDIRECTS)
@Tag(TrunkTag.YES)
@Tag(CmdTag.ADVERTIZE)
@Tag(OldTag.YES)
@RunWith(Parameterized.class)
public class AdvertizeTest {

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();

    @Parameterized.Parameter(value = 0)
    public String login;

    @Parameterized.Parameter(value = 1)
    public String urlSubstring;

    @Parameterized.Parameter(value = 2)
    public String clientType;


    @Parameterized.Parameters(name = "Клиент: {2}, редирект на: {1}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {Logins.SUPER, appendCmdPrefix(CMD.SHOW_SEARCH_PAGE), "супер"},
                {"at-direct-advertize-c2",  UrlPath.WELCOME_PAGE.getUrlPath(), "новый клиент"},
                {"at-direct-adv-std", "/wizard/campaigns/new", "клиент в профессиональном интерфейсе"},
                {Logins.MANAGER, appendCmdPrefix(CMD.SHOW_MANAGER_MY_CLIENTS), "менеджер"},
                {"at-direct-agency", appendCmdPrefix(CMD.SHOW_CLIENTS), "агентство"},
                {Logins.SUPPORT, appendCmdPrefix(CMD.SHOW_SEARCH_PAGE), "саппорт"},
                {Logins.MEDIAPLANER, appendCmdPrefix(CMD.SHOW_SEARCH_PAGE), "медиапланер"},
                {Logins.PLACER, appendCmdPrefix(CMD.SHOW_SEARCH_PAGE), "вешальщик"},


        });
    }

    @Before
    public void before() {
        cmdRule.oldSteps().onPassport().authoriseAs(login, User.get(login).getPassword());
    }

    private static String appendCmdPrefix(CMD cmd) {
        return "cmd=" + cmd.getName();
    }

    @Test
    @Description("Проверяем правильность редиректа")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10056")
    public void checkAdvertizeRedirect() {
        cmdRule.oldSteps().onAdvertize().checkAdvertizeRedirect(urlSubstring);
    }

}
