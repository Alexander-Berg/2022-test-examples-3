package ru.yandex.autotests.direct.cmd.groups;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.rules.CampaignRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.List;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyFields;

/**
 * https://st.yandex-team.ru/TESTIRT-5366
 */
@Aqua.Test
@Features(TestFeatures.Banners.ADD_FIRST_GROUP_WITH_TWO_DIFF_BANNERS)
@Stories(TestFeatures.BANNERS)
@Description("Добавляем группу с двумя баннерами разных типов в компанию")
@Tag(TrunkTag.YES)
public class AddGroupWithTwoDiffBannersTest {
    private static final String CLIENT = "at-direct-b-addbannermultiedit";
    @ClassRule
    public static DirectCmdRule directCmdClassRule = DirectCmdRule.defaultClassRule();
    public CampaignRule campaignRule = new CampaignRule().withMediaType(CampaignTypeEnum.TEXT).withUlogin(CLIENT);
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(campaignRule);
    private Group group;

    @Before
    public void before() {
        group = BeanLoadHelper.loadCmdBean(CmdBeans.COMMON_REQUEST_GROUP_TEXT_WITH_TWO_BANNERS, Group.class);
        group.setCampaignID(campaignRule.getCampaignId().toString());
        group.getBanners().stream().forEach(b -> b.withCid(campaignRule.getCampaignId()));
        group.getBanners().get(0).withBannerType("mobile");
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("9768")
    public void createGroupWithTextAndMobileBanners() {
        createFirstGroup();

        List<Banner> bannerList = cmdRule.cmdSteps().groupsSteps().getBanners(CLIENT, campaignRule.getCampaignId());
        assertThat("баннеры сохранились успешно",
                bannerList, beanDiffer(group.getBanners()).useCompareStrategy(onlyFields(
                        newPath("/banner_type"))));
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("9769")
    public void addGroupWithTextAndMobileBanners() {
        createFirstGroup();
        String firstGroupId = cmdRule.cmdSteps().groupsSteps().getGroups(CLIENT, campaignRule.getCampaignId())
                .get(0).getAdGroupID();
        GroupsParameters groupsParameters = GroupsParameters.forExistingCamp(CLIENT, campaignRule.getCampaignId(), group);
        cmdRule.cmdSteps().groupsSteps().postSaveTextAdGroups(groupsParameters);
        String newGroupId = cmdRule.cmdSteps().groupsSteps().getGroups(CLIENT, campaignRule.getCampaignId())
                .stream()
                .filter((t -> !t.getAdGroupID().equals(firstGroupId)))
                .findFirst().get().getAdGroupID();

        List<Banner> bannerList = cmdRule.cmdSteps().groupsSteps().getBanners(
                CLIENT, campaignRule.getCampaignId(), Long.parseLong(newGroupId));
        assertThat("баннеры сохранились успешно",
                bannerList, beanDiffer(group.getBanners()).useCompareStrategy(onlyFields(
                        newPath("/banner_type"))));
    }

    private void createFirstGroup() {
        GroupsParameters groupsParameters = GroupsParameters.forNewCamp(CLIENT, campaignRule.getCampaignId(), group);
        cmdRule.cmdSteps().groupsSteps().postSaveTextAdGroups(groupsParameters);
    }
}
