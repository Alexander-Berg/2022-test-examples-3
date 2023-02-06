package ru.yandex.autotests.direct.cmd.banners.additions.callouts;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.bssynced.BsSyncedHelper;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Callout;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.BannersRuleFactory;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BannersStatusbssynced;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assume.assumeThat;

@Aqua.Test
@Description("BSSynced при действиях с текстовыми дополнениями")
@Stories(TestFeatures.Banners.STATUS_BS_SYNCED)
@Features(TestFeatures.BANNERS)
@Tag(TrunkTag.YES)
@Tag(ObjectTag.CALLOUTS)
@Tag(ObjectTag.BANNER)
@RunWith(Parameterized.class)
public class BannersCalloutsBsSyncedTest {
    public static final String CLIENT = "at-direct-bssync-banners2";
    @ClassRule
    public static DirectCmdRule defaultClassRuleChain = DirectCmdRule.defaultClassRule();
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule;
    public CampaignTypeEnum campaignType;
    private BannersRule bannersRule;
    private Banner createdBanner;
    private Group createdGroup;
    private Callout[] createdCallouts;

    public BannersCalloutsBsSyncedTest(CampaignTypeEnum campaignType) {
        this.campaignType = campaignType;
        bannersRule = BannersRuleFactory.getBannersRuleBuilderByCampType(campaignType).withUlogin(CLIENT);
        bannersRule.overrideBannerTemplate(
                new Banner().withCallouts(
                        new Callout().withCalloutText("some text")
                )
        );
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Parameterized.Parameters(name = "Сброс statusBsSynced баннера. Тип кампании: {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {CampaignTypeEnum.TEXT},
                {CampaignTypeEnum.DTO}
        });
    }

    @Before
    public void before() {
        List<Callout> calloutList = cmdRule.cmdSteps().bannersAdditionsSteps().getCallouts(CLIENT).getCallouts();
        assumeThat("дополнение сохранилось на клиенте", calloutList, hasSize(greaterThan(0)));
        createdCallouts = calloutList.toArray(new Callout[calloutList.size()]);
        makeCampSynced();
        createdGroup = bannersRule.getCurrentGroup();
        createdBanner = createdGroup.getBanners().get(0);
    }

    @After
    public void after() {
        new CalloutsTestHelper(CLIENT, cmdRule.cmdSteps(), bannersRule.getCampaignId().toString())
                .clearCalloutsForClient();
    }

    @Test
    @Description("Удаление дополнения должно сбрасывать статус синхронизации баннера")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9068")
    public void deletingCalloutsShouldResetBannerStatusBsSynced() {
        cmdRule.cmdSteps().bannersAdditionsSteps().deleteClientCalloutsSafe(CLIENT, createdCallouts);
        check();
    }

    @Test
    @Description("Добавление дополнения должно сбрасывать статус синхронизации баннера")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9069")
    public void addCalloutShouldResetBannerStatusBsSynced() {
        createdBanner.getCallouts().add(new Callout().withCalloutText("some text 2"));
        editGroup();
        check();
    }

    @Test
    @Description("Отвязка дополнения от баннера должна сбрасывать статус синхронизации баннера")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9070")
    public void removeCallouFromBannerShouldResetBannerStatusBsSynced() {
        createdBanner.setCallouts(emptyList());
        editGroup();
        check();
    }

    private void editGroup() {
        cmdRule.cmdSteps().groupsSteps().prepareGroupForUpdate(createdGroup, campaignType);
        bannersRule.saveGroup(GroupsParameters.forExistingCamp(CLIENT, bannersRule.getCampaignId(), createdGroup));
    }

    private void check() {
        BsSyncedHelper.checkBannerBsSynced(CLIENT, bannersRule.getBannerId(), BannersStatusbssynced.No);
    }

    private void makeCampSynced() {
        BsSyncedHelper.moderateCamp(cmdRule, bannersRule.getCampaignId());
        BsSyncedHelper.syncCamp(cmdRule, bannersRule.getCampaignId());
    }
}
