package ru.yandex.autotests.direct.cmd.clients.greenurl;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.clients.ModifyUserModel;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.UsersStatusblocked;
import ru.yandex.autotests.direct.db.steps.UsersSteps;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Description("Невозможность редактирования настройки пользователя no_display_hrefs " +
        "под клиентом (контроллер modifyUser)")
@Stories(TestFeatures.Banners.DISPLAY_HREF)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.MODIFY_USER)
@Tag(ObjectTag.USER)
@Tag(TrunkTag.YES)
public class NoDisplayHrefsParamAtModifyUserUnderClientTest {

    private static final String CLIENT = "at-direct-backend-modifyuser-5";
    private static final String RESET_VALUE = "";
    private static final String NEW_VALUE = "1";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();


    private ModifyUserModel modifyUserModel;

    @Before
    public void before() {
        // unblock client
        UsersSteps usersSteps = cmdRule.dbSteps().useShardForLogin(CLIENT).usersSteps();
        usersSteps.setBlocked(usersSteps.getUidByLogin(CLIENT), UsersStatusblocked.No);

        resetOption();
    }

    @Test
    @Description("Невозможность редактирования настройки пользователя no_display_hrefs " +
            "под клиентом (контроллер modifyUser)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9592")
    public void testNoDisplayHrefsParamAtModifyUser() {
        cmdRule.cmdSteps().authSteps().authenticate(User.get(CLIENT));
        modifyUserModel.withNoDisplayHrefs(NEW_VALUE);
        cmdRule.cmdSteps().modifyUserSteps().postModifyUser(modifyUserModel);

        cmdRule.cmdSteps().authSteps().authenticate(User.get(Logins.SUPER));
        String actual = cmdRule.cmdSteps().modifyUserSteps().getModifyUser(CLIENT).getNoDisplayHrefs();
        assertThat("настройка пользователя no_display_hrefs не изменилась под ролью \"клиент\"",
                actual, getExpectedMatcher(RESET_VALUE));
    }

    private void resetOption() {
        modifyUserModel = cmdRule.cmdSteps().modifyUserSteps().getModifyUser(CLIENT);
        modifyUserModel.withPhone("+79999999999").withNoDisplayHrefs(RESET_VALUE).withUlogin(CLIENT);
        cmdRule.cmdSteps().modifyUserSteps().postModifyUser(modifyUserModel);
        String actual = cmdRule.cmdSteps().modifyUserSteps().getModifyUser(CLIENT).getNoDisplayHrefs();
        assumeThat("перед тестом удалось сбросить настройку пользователя no_display_hrefs",
                actual, getExpectedMatcher(RESET_VALUE));
    }

    private Matcher getExpectedMatcher(String value) {
        return "".equals(value) || "0".equals(value) ? anyOf(nullValue(), equalTo("0")) : equalTo("1");
    }
}
