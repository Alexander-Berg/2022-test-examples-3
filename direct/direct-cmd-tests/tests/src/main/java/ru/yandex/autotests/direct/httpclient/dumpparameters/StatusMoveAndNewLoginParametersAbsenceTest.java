package ru.yandex.autotests.direct.httpclient.dumpparameters;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.OldTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.Logins;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.is;
import static ru.yandex.autotests.direct.httpclient.CocaineSteps.getCsrfTokenFromCocaine;
import static ru.yandex.autotests.direct.httpclient.JsonResponse.hasJsonProperty;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 22.09.14
 *         https://st.yandex-team.ru/TESTIRT-2833
 */

@Aqua.Test
@Description("Проверка отсутствия параметров statusMove и NewLogin")
@Stories(TestFeatures.SeveralControllerParameters.STATUS_MOVE_AND_NEW_LOGIN_PARAMETERS_ABSENCE)
@Features(TestFeatures.SEVERAL_CONTROLLER_PARAMETERS)
@Tag(TrunkTag.YES)
@Tag(OldTag.YES)
public class StatusMoveAndNewLoginParametersAbsenceTest {

    private static final String LOGIN = "at-daybudget-c";
    private static final String SUPER = Logins.SUPER;
    private static final String AGENCY = "at-direct-arch-subclient-a";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private BannersRule bannersRule = new TextBannersRule().withUlogin(LOGIN);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    private Matcher matcher;

    @Before
    public void before() {
        cmdRule.oldSteps().onPassport().authoriseAs(SUPER, User.get(SUPER).getPassword());
        matcher = allOf(hasJsonProperty("$..statusMove", is(emptyCollectionOf(Object.class))),
                hasJsonProperty("$..newLogin", is(emptyCollectionOf(Object.class))));
    }

    @Test
    @Description("Проверяем отсутсвие параметров для контроллера showClients")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10460")
    public void checkParametersAbsenceOnShowClients() {
        DirectResponse response = cmdRule.oldSteps().onShowClients().openShowClients(AGENCY);
        cmdRule.oldSteps().commonSteps().checkDirectResponse(response, matcher);
    }

    @Test
    @Description("Проверяем отсутсвие параметров для контроллера showManagerMyClients")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10461")
    public void checkParametersAbsenceOnShowManagerMyClients() {
        DirectResponse response = cmdRule.oldSteps().onShowManagerMyClients().openShowManagerMyClients(LOGIN);
        cmdRule.oldSteps().commonSteps().checkDirectResponse(response, matcher);
    }

    @Test
    @Description("Проверяем отсутсвие параметров для контроллера showUserEmails")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10462")
    public void checkParametersAbsenceOnShowUserEmails() {
        DirectResponse response = cmdRule.oldSteps().onShowUserEmails().openShowUserEmailsAtJsonFormat();
        cmdRule.oldSteps().commonSteps().checkDirectResponse(response, matcher);
    }

    @Test
    @Ignore
    @Description("Проверяем отсутсвие параметров для контроллера agSearch")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10463")
    public void checkParametersAbsenceOnAgSearch() {
        cmdRule.oldSteps().onPassport().authoriseAs(AGENCY, User.get(AGENCY).getPassword());
        DirectResponse response = cmdRule.oldSteps().onAgSearch().openAgSearch();
        cmdRule.oldSteps().commonSteps().checkDirectResponse(response, matcher);
    }

    @Test
    @Ignore
    @Description("Проверяем отсутсвие параметров для контроллера changeManagerOfClient")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10464")
    public void checkParametersAbsenceOnChangeManagerOfClient() {
        DirectResponse response = cmdRule.oldSteps().onChangeManagerOfClient().openChangeManagerOfClient();
        cmdRule.oldSteps().commonSteps().checkDirectResponse(response, matcher);
    }

    @Test
    @Ignore
    @Description("Проверяем отсутсвие параметров для контроллера changeManagerOfAgency")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10465")
    public void checkParametersAbsenceOnChangeManagerOfAgency() {
        CSRFToken csrfToken = getCsrfTokenFromCocaine(User.get(SUPER).getPassportUID());
        DirectResponse response = cmdRule.oldSteps().onChangeManagerOfAgency().openChangeManagerOfAgency(csrfToken);
        cmdRule.oldSteps().commonSteps().checkDirectResponse(response, matcher);
    }

    @Test
    @Description("Проверяем отсутсвие параметров для контроллера modifyUser")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10466")
    public void checkParametersAbsenceOnModifyUser() {
        CSRFToken csrfToken = getCsrfTokenFromCocaine(User.get(SUPER).getPassportUID());
        DirectResponse response = cmdRule.oldSteps().onModifyUser().openModifyUser(LOGIN, csrfToken);
        cmdRule.oldSteps().commonSteps().checkDirectResponse(response, matcher);
    }

    @Test
    @Ignore
    @Description("Проверяем отсутсвие параметров для контроллера showStaff")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10467")
    public void checkParametersAbsenceOnShowStaff() {
        DirectResponse response = cmdRule.oldSteps().onShowStaff().openShowStaff();
        cmdRule.oldSteps().commonSteps().checkDirectResponse(response, matcher);
    }

    @Test
    @Ignore
    @Description("Проверяем отсутсвие параметров для контроллера manageTeamleaders")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10468")
    public void checkParametersAbsenceOnManageTeamleaders() {
        DirectResponse response = cmdRule.oldSteps().onManageTeamleaders().openManageTeamleaders();
        cmdRule.oldSteps().commonSteps().checkDirectResponse(response, matcher);
    }

    @Test
    @Ignore
    @Description("Проверяем отсутсвие параметров для контроллера manageSTeamleaders")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10469")
    public void checkParametersAbsenceOnManageSTeamleaders() {
        DirectResponse response = cmdRule.oldSteps().onManageSTeamleaders().openManageSTeamleaders();
        cmdRule.oldSteps().commonSteps().checkDirectResponse(response, matcher);
    }


}
