package ru.yandex.autotests.direct.cmd.banners.additions.callouts;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.banners.additions.GetBannersAdditionsResponse;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/*
* todo javadoc
*/
@Aqua.Test
@Description("Сохранение уточнений на клиента")
@Stories(TestFeatures.Banners.BANNERS_CALLOUTS)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
public class SaveSameCalloutsForDifferentClientsTest {
    @ClassRule
    public static DirectCmdRule defaultClassRuleChain = DirectCmdRule.defaultClassRule();

    public String ulogin1 = "at-direct-banners-callouts-13";
    public String ulogin2 = "at-direct-banners-callouts-14";

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();

    @Before
    public void setUp() {
        CalloutsTestHelper helper = new CalloutsTestHelper(ulogin1, cmdRule.cmdSteps(), "");
        helper.clearCalloutsForClient();
        helper = new CalloutsTestHelper(ulogin2, cmdRule.cmdSteps(), "");
        helper.clearCalloutsForClient();
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("9096")
    public void saveCallouts() {
        String[] expectedCallouts = {"callout1"};
        cmdRule.cmdSteps().bannersAdditionsSteps().saveBannersCalloutsSafe(ulogin1, expectedCallouts);
        cmdRule.cmdSteps().bannersAdditionsSteps().saveBannersCalloutsSafe(ulogin2, expectedCallouts);
        GetBannersAdditionsResponse actual1 = cmdRule.cmdSteps().bannersAdditionsSteps().getCallouts(ulogin1);
        GetBannersAdditionsResponse actual2 = cmdRule.cmdSteps().bannersAdditionsSteps().getCallouts(ulogin2);

        assertThat("у клиента " + ulogin1 + " 1 дополнение", actual1.getCallouts(), hasSize(1));
        assertThat("у клиента " + ulogin2 + " 1 дополнение", actual2.getCallouts(), hasSize(1));
        assertThat("id дополнений на для разных клиентов разные", actual1.getCallouts().get(0).getAdditionsItemId(),
                not(equalTo(actual2.getCallouts().get(0).getAdditionsItemId())));
    }
}
