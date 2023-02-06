package ru.yandex.autotests.direct.cmd.groups.rarelyloaded;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.BannersRuleFactory;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.qatools.allure.annotations.Description;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;


@RunWith(Parameterized.class)
public abstract class RarelyLoadedFlagTestBase {
    private static final String CLIENT = "at-direct-backend-c";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private BannersRule bannersRule = BannersRuleFactory.getBannersRuleBuilderByCampType(getCampaignType()).withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    @Parameterized.Parameter(0)
    public Integer rarelyLoaded;

    @Parameterized.Parameter(1)
    public Integer expectedRarelyLoaded;

    public abstract CampaignTypeEnum getCampaignType();

    @Before
    public void before() {
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).adGroupsSteps()
                .setBsRarelyLoaded(bannersRule.getGroupId(), rarelyLoaded == 1);
    }

    @Description("Проверка получения флага is_rarely_loaded в ответе showCamp")
    public void rarelyLoadedShowCampTest() {
        Banner actualGroup = cmdRule.cmdSteps().campaignSteps()
                .getShowCamp(CLIENT, bannersRule.getCampaignId().toString()).getGroups().get(0);

        assertThat("is_rarely_loaded соответсвует ожиданиям", actualGroup,
                beanDiffer(getExpectedBanner()).useCompareStrategy(onlyExpectedFields()));
    }

    @Description("Проверка получения флага is_rarely_loaded в ответе showCampMultiEdit")
    public void rarelyLoadedShowCampMultiEditTest() {
        Group actualGroup = cmdRule.cmdSteps().campaignSteps()
                .getShowCampMultiEdit(CLIENT, bannersRule.getCampaignId()).getCampaign().getGroups().get(0);

        assertThat("is_rarely_loaded соответсвует ожиданиям", actualGroup,
                beanDiffer(getExpectedGroup()).useCompareStrategy(onlyExpectedFields()));
    }

    protected Banner getExpectedBanner() {
        return new Banner().withIsBsRarelyLoaded(expectedRarelyLoaded);
    }

    protected Group getExpectedGroup() {
        return new Group().withIsBsRarelyLoaded(expectedRarelyLoaded);
    }
}
