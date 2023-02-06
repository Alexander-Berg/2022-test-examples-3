package ru.yandex.autotests.direct.cmd.groups.rarelyloaded;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.CampaignStrategy;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.commons.phrase.Phrase;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.BannersRuleFactory;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.cmd.util.CmdStrategyBeans;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.direct.utils.strategy.data.Strategies;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategy;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.IsNull.nullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;


@Aqua.Test
@Features(TestFeatures.Groups.RARELY_LOADED_FLAG)
@Stories(TestFeatures.GROUPS)
@Description("Проверка отсутствия данных торгов при установленном флаге мало показов")
@Tag(CmdTag.SHOW_CAMP)
@Tag(CmdTag.SHOW_CAMP_MULTI_EDIT)
@Tag(CampTypeTag.TEXT)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class BiddingRarelyLoadedFlagTest {
    private static final String CLIENT = "at-direct-backend-c";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private BannersRule bannersRule;

    @Rule
    public DirectCmdRule cmdRule;

    @Parameterized.Parameters(name = "Проверка отсутствия данных торгов при мало показов. Тип кампании: {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {CampaignTypeEnum.TEXT, CmdStrategyBeans.getStrategyBean(Strategies.HIGHEST_POSITION_DEFAULT)},
                {CampaignTypeEnum.MOBILE, CmdStrategyBeans.getStrategyBean(Strategies.HIGHEST_POSITION_DEFAULT)},
                {CampaignTypeEnum.TEXT, CmdStrategyBeans.getStrategyBean(Strategies.SHOWS_DISABLED_MAX_COVERADGE)},
                {CampaignTypeEnum.MOBILE, CmdStrategyBeans.getStrategyBean(Strategies.SHOWS_DISABLED_MAX_COVERADGE)},
        });
    }

    public BiddingRarelyLoadedFlagTest(CampaignTypeEnum campaignType, CampaignStrategy strategy) {
        bannersRule = BannersRuleFactory.getBannersRuleBuilderByCampType(campaignType)
                .overrideCampTemplate(new SaveCampRequest().withJsonStrategy(strategy))
                .withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Before
    public void before() {
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).adGroupsSteps()
                .setBsRarelyLoaded(bannersRule.getGroupId(), true);
    }

    @Test
    @Description("Проверка осутствия данных торгов в ответе showCamp")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10683")
    public void biddingDataShowCampTest() {
        Banner actualGroup = cmdRule.cmdSteps().campaignSteps()
                .getShowCamp(CLIENT, bannersRule.getCampaignId().toString()).getGroups().get(0);

        assertThat("данные о торгах соответсвуют ожиданию", actualGroup,
                beanDiffer(getExpectedBanner()).useCompareStrategy(getCompareStrategy()));
    }

    @Test
    @Description("Проверка осутствия данных торгов в ответе showCampMultiEdit")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10684")
    public void biddingDataShowCampMultiEditTest() {
        Group actualGroup = cmdRule.cmdSteps().campaignSteps()
                .getShowCampMultiEdit(CLIENT, bannersRule.getCampaignId()).getCampaign().getGroups().get(0);

        assertThat("данные о торгах соответсвуют ожиданию", actualGroup,
                beanDiffer(getExpectedGroup()).useCompareStrategy(getCompareStrategy()));
    }

    private Banner getExpectedBanner() {
        return new Banner().withPhrases(singletonList(getExpectedPhrase()));
    }

    private Group getExpectedGroup() {
        return new Group().withPhrases(singletonList(getExpectedPhrase()));
    }

    private Phrase getExpectedPhrase() {
        return new Phrase()
                .withGuarantee(emptyList())
                .withPremium(emptyList())
                .withPokazometerData(new Object());
    }

    private DefaultCompareStrategy getCompareStrategy() {
        return DefaultCompareStrategies.onlyExpectedFields()
                .forFields(
                        newPath("phrases", ".*", "guarantee"),
                        newPath("phrases", ".*", "premium")
                ).useMatcher(empty())
                .forFields(newPath("phrases", ".*", "pokazometerData")).useMatcher(nullValue());
    }
}
