package ru.yandex.autotests.direct.cmd.groups.rarelyloaded;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.BannersRuleFactory;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.CampaignsArchived;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.CampaignsRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;

@Aqua.Test
@Features(TestFeatures.Groups.RARELY_LOADED_FLAG)
@Stories(TestFeatures.GROUPS)
@Description("Проверка флага мало показов в архивной кампании")
@Tag(CmdTag.SHOW_CAMP)
@Tag(CmdTag.SHOW_CAMP_MULTI_EDIT)
@Tag(CampTypeTag.TEXT)
@Tag(CampTypeTag.MOBILE)
@Tag(CampTypeTag.DYNAMIC)
@Tag(CampTypeTag.TEXT)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class RarelyLoadedFlagArchiveCampTest {

    private static final Integer EXPECTED_VALUE = 0;
    private static final String CLIENT = "at-direct-backend-c";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private BannersRule bannersRule;

    @Rule
    public DirectCmdRule cmdRule;

    @Parameterized.Parameters(name = "флаг is_bs_rarely_loaded для архивных кампаний. Тип кампании: {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {CampaignTypeEnum.TEXT},
                {CampaignTypeEnum.MOBILE},
                {CampaignTypeEnum.DTO},
                {CampaignTypeEnum.DMO},
        });
    }

    public RarelyLoadedFlagArchiveCampTest(CampaignTypeEnum campaignType) {
        bannersRule = BannersRuleFactory.getBannersRuleBuilderByCampType(campaignType).withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Before
    public void before() {
        CampaignsRecord record = TestEnvironment.newDbSteps().useShardForLogin(CLIENT).campaignsSteps()
                .getCampaignById(bannersRule.getCampaignId());
        record.setArchived(CampaignsArchived.Yes);
        TestEnvironment.newDbSteps().campaignsSteps().updateCampaigns(record);
        TestEnvironment.newDbSteps().adGroupsSteps()
                .setBsRarelyLoaded(bannersRule.getGroupId(), true);
    }

    @Test
    @Description("Проверка получения сброшенного флага is_rarely_loaded в ответе showCamp")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10686")
    public void rarelyLoadedShowCampTest() {
        Banner actualGroup = cmdRule.cmdSteps().campaignSteps()
                .getShowCamp(CLIENT, bannersRule.getCampaignId().toString()).getGroups().get(0);

        assertThat("is_rarely_loaded сброшен", actualGroup,
                beanDiffer(getExpectedBanner()).useCompareStrategy(onlyExpectedFields()));
    }

    protected Banner getExpectedBanner() {
        return new Banner().withIsBsRarelyLoaded(EXPECTED_VALUE);
    }

    protected Group getExpectedGroup() {
        return new Group().withIsBsRarelyLoaded(EXPECTED_VALUE);
    }
}
