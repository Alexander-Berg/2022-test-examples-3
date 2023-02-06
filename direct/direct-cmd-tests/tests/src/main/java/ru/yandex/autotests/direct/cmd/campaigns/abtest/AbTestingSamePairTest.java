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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

//TESTIRT-9619
@Aqua.Test
@Description("Создание перевернутого эксперимента")
@Stories(TestFeatures.Campaigns.CREATE_AB_TEST)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.CREATE_AB_TEST)
@Tag(ObjectTag.CAMPAIGN)
@Tag(CampTypeTag.TEXT)
@Tag(TrunkTag.YES)
public class AbTestingSamePairTest {
    protected static final String CLIENT = "at-direct-backend-rus-os9";
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    protected static Integer SHARD;
    protected BannersRule bannersRuleFirst = new TextBannersRule().withUlogin(CLIENT);
    protected BannersRule bannersRuleSecond = new TextBannersRule().withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRuleFirst, bannersRuleSecond);
    private AbTestingHelper abTestingHelper;

    @Before
    public void before() {
        SHARD = TestEnvironment.newDbSteps().shardingSteps().getShardByLogin(CLIENT);
        abTestingHelper = new AbTestingHelper().withClient(CLIENT).withCmdRule(cmdRule);
    }

    @Test
    @Description("создание перевернутого эксперимента для 2 кампаний одного типа с primary_percent 70")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9382")
    public void sameSamePairsOfCampsWithDiffPercentage() {
        abTestingHelper.saveWithDefaultDates(70, bannersRuleFirst.getCampaignId(), bannersRuleSecond.getCampaignId());
        CreateABTestResponse actualResponse = abTestingHelper.saveWithDefaultDates(70, bannersRuleFirst.getCampaignId(), bannersRuleSecond.getCampaignId());
        assertThat("ошибка соотвествует ожиданиям", actualResponse.getError(),
                equalTo("Кампания " + bannersRuleFirst.getCampaignId() + " уже участвует в эксперименте"));
    }

    @Test
    @Description("создание перевернутого эксперимента для 2 кампаний одного типа с primary_percent 50")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9383")
    public void sameSamePairsOfCampsWithSamePercentage() {
        abTestingHelper.saveWithDefaultDates(50, bannersRuleFirst.getCampaignId(), bannersRuleSecond.getCampaignId());
        CreateABTestResponse actualResponse = abTestingHelper.saveWithDefaultDates(50, bannersRuleFirst.getCampaignId(), bannersRuleSecond.getCampaignId());
        assertThat("ошибка соотвествует ожиданиям", actualResponse.getError(),
                equalTo("Кампания " + bannersRuleFirst.getCampaignId() + " уже участвует в эксперименте"));
    }
}
