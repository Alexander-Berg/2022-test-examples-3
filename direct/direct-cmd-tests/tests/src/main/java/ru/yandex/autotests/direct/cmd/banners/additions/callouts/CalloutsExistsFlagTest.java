package ru.yandex.autotests.direct.cmd.banners.additions.callouts;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.showcamp.ShowCampResponse;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Description("Флаг на группу group_has_callouts установлен, если в группе есть баннеры с дополнениями")
@Stories(TestFeatures.Banners.BANNERS_CALLOUTS)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SHOW_CAMP)
public class CalloutsExistsFlagTest {
    @ClassRule
    public static DirectCmdRule defaultClassRuleChain = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();
    protected CalloutsTestHelper helper;
    private String ulogin = "at-direct-banners-callouts-21";
    private String callout = "callout1";
    private Long cid;

    @Before
    public void setUp() {
        helper = new CalloutsTestHelper(ulogin, cmdRule.cmdSteps(), "");
        helper.clearCalloutsForClient();
    }

    @After
    public void after() {
        if (cid != null) {
            cmdRule.apiAggregationSteps().deleteActiveCampaignQuietly(ulogin, cid);
        }
    }

    @Test
    @Description("Поле group_has_callouts для текстовых баннеров")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9071")
    public void remoderateCalloutsForTextBanner() {

        cid = cmdRule.cmdSteps().campaignSteps().saveNewDefaultTextCampaign(ulogin);

        helper.overrideCid(cid.toString());

        helper.saveCallouts(helper.getRequestFor(helper.newGroupAndSet(callout)));

        checkFlag();
    }

    @Test
    @Description("Поле group_has_callouts для динамических баннеров")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9072")
    public void remoderateCalloutsForDynamicBanner() {

        cid = cmdRule.cmdSteps().campaignSteps().saveNewDefaultDynamicCampaign(ulogin);

        helper.overrideCid(cid.toString());

        helper.saveCalloutsForDynamic(helper.getRequestForDynamic(helper.newDynamicGroupAndSet(callout)));

        checkFlag();
    }


    private void checkFlag() {
        ShowCampResponse resp = cmdRule.cmdSteps().campaignSteps().getShowCamp(ulogin, cid.toString());

        assumeThat("В кампании есть 1 группа", resp.getGroups(), hasSize(1));

        assertThat("Поле group_has_callouts выставлено", resp.getGroups().get(0).getGroupHasCallouts(), equalTo("1"));
    }
}
