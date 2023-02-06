package ru.yandex.autotests.direct.httpclient.newclient;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.OldTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.Logins;
import ru.yandex.autotests.direct.httpclient.data.newclient.AjaxSuggestLoginParameters;
import ru.yandex.autotests.direct.httpclient.util.PropertyLoader;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.containsString;
import static ru.yandex.autotests.directapi.matchers.beans.EveryItem.everyItem;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 06.11.14
 *         https://st.yandex-team.ru/TESTIRT-3207
 */

@Aqua.Test
@Description("Проверка контроллера AjaxSuggestLogin")
@Stories(TestFeatures.NewClient.AJAX_SUGGEST_LOGIN)
@Features(TestFeatures.NEW_CLIENT)
@Tag(CmdTag.AJAX_SUGGEST_LOGIN)
@Tag(OldTag.YES)
@RunWith(Parameterized.class)
public class AjaxSuggestLoginTest {

    private static final String NEW_LOGIN = "logindsfsd";
    private static final String FIRST_NAME = "fdgdfg";
    private static final String LAST_NAME = "login";

    private static final String SUGGEST_LOGIN_WITH_NUMBER = "l0gindsfsd";
    private static final String SUGGEST_FIRST_NAME_WITH_NUMBERS = "fd9dfg";
    private static final String SUGGEST_LAST_NAME_WITH_NUMBERS = "l0gin";
    private static final String SUGGEST_LAST_NAME_WITH_WITH_NUMBERS_AND_POSTFIX = "log1nf";

    private static final boolean GET_VARS = true;
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();
    @Parameterized.Parameter(value = 0)
    public String login;
    @Parameterized.Parameter(value = 1)
    public String description;

    private CSRFToken csrfToken;
    private AjaxSuggestLoginParameters ajaxSuggestLoginParameters;
    private PropertyLoader<AjaxSuggestLoginParameters> propertyLoader;
    private String trackId;

    @Parameterized.Parameters(name = "Пользователь: {0}, роль: {1}")
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][]{
                {Logins.AGENCY, "Агентство"},
                {Logins.SUPER, "Суперпользователь"},
                {Logins.MANAGER, "Менеджер"},
                {Logins.SUPPORT, "Саппорт"}
        };
        return Arrays.asList(data);
    }

    @Before
    public void before() {

        cmdRule.oldSteps().onPassport().authoriseAs(login, User.get(login).getPassword());
        DirectResponse createAgencyClientResponse =
                cmdRule.oldSteps().onShowRegisterLoginPage().openShowRegisterLoginPage();
        csrfToken = createAgencyClientResponse.getCSRFToken();
        trackId = cmdRule.oldSteps().commonSteps().
                readResponseJsonProperty(createAgencyClientResponse, "$..track_id[0]");
        propertyLoader = new PropertyLoader(AjaxSuggestLoginParameters.class);

    }

    @Test
    @Description("Проверяем suggest логина при одинаковых логине, имени и фамилии")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10527")
    public void checkLoginSuggest() {
        ajaxSuggestLoginParameters = propertyLoader.getHttpBean("ajaxSuggestLoginParameters");
        ajaxSuggestLoginParameters.setTrackId(trackId);
        cmdRule.oldSteps().onAjaxSuggestLogin().checkAjaxSuggestLoginResponse(csrfToken, ajaxSuggestLoginParameters,
                everyItem(anyOf(containsString(NEW_LOGIN), containsString(SUGGEST_LOGIN_WITH_NUMBER))));
    }

    @Test
    @Description("Проверяем ajaxSuggestLogin при различных логине, имени и фамилии")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10528")
    public void checkLoginSuggestError() {
        ajaxSuggestLoginParameters = propertyLoader.getHttpBean("ajaxSuggestLoginDifferentNames");
        ajaxSuggestLoginParameters.setTrackId(trackId);
        cmdRule.oldSteps().onAjaxSuggestLogin().checkAjaxSuggestLoginResponse(csrfToken, ajaxSuggestLoginParameters,
                everyItem(anyOf(
                        containsString(FIRST_NAME),
                        containsString(LAST_NAME),
                        containsString(SUGGEST_FIRST_NAME_WITH_NUMBERS),
                        containsString(SUGGEST_LAST_NAME_WITH_WITH_NUMBERS_AND_POSTFIX),
                        containsString(SUGGEST_LAST_NAME_WITH_NUMBERS))));
    }
}
