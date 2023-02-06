package ru.yandex.autotests.direct.cmd.campaigns.abtest;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.campaigns.CreateABTestResponse;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.steps.campaings.AbTestingHelper;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Calendar;
import java.util.Date;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Негативные тесты создания эксперимента")
@Stories(TestFeatures.Campaigns.CREATE_AB_TEST)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.CREATE_AB_TEST)
@Tag(ObjectTag.CAMPAIGN)
@Tag(CampTypeTag.TEXT)
@Tag(TrunkTag.YES)
public class AbTestingNegativeTest {
    protected static final String CLIENT = "at-direct-backend-rus-os9";
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    protected static Integer SHARD;
    protected BannersRule bannersRuleFirst = new TextBannersRule().withUlogin(CLIENT);
    protected BannersRule bannersRuleSecond = new TextBannersRule().withUlogin(CLIENT);
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRuleFirst, bannersRuleSecond);
    private int percent = 70;
    private AbTestingHelper abTestingHelper;


    @Before
    public void before() {
        SHARD = TestEnvironment.newDbSteps().shardingSteps().getShardByLogin(CLIENT);
        abTestingHelper = new AbTestingHelper().withClient(CLIENT).withCmdRule(cmdRule);
    }

    @Test
    @Description("Попытка создания эксперимента с началом в текущий день")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9380")
    public void createExperimentFromToday() {
        CreateABTestResponse actualCreateABTestResponse = cmdRule.cmdSteps().campaignSteps().createABTest(
                CLIENT,
                bannersRuleFirst.getCampaignId(),
                bannersRuleSecond.getCampaignId(),
                percent,
                Calendar.getInstance().getTime(),
                Calendar.getInstance().getTime()
        );
        assertThat("ошибка соответсвует ожиданиям", "Дата начала не может быть раньше завтра",
                equalTo(actualCreateABTestResponse.getError())
        );

    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("9381")
    public void createTooLongDateExperiment() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, 1);
        Date fromDate = cal.getTime();
        cal.add(Calendar.DATE, 110);
        Date toDate = cal.getTime();
        CreateABTestResponse actualCreateABTestResponse = cmdRule.cmdSteps().campaignSteps().createABTest(
                CLIENT,
                bannersRuleFirst.getCampaignId(),
                bannersRuleSecond.getCampaignId(),
                percent,
                fromDate,
                toDate
        );
        assertThat("ошибка соответсвует ожиданиям", "Продолжительность не должна превышать 90 дней",
                equalTo(actualCreateABTestResponse.getError()));

    }

}
