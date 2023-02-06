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
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;

@Aqua.Test
@Features(TestFeatures.Banners.BANNERS_PARAMETERS)
@Stories(TestFeatures.BANNERS)
@Description("Добавляем группу с двумя баннерами в компанию")
@Tag(TrunkTag.YES)
public class AddGroupWithTwoBannersTest {

    private static final String CLIENT = "at-direct-b-bannersmultisave";

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
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("10637")
    public void createGroupWithTwoBanners() {
        GroupsParameters groupsParameters = GroupsParameters.forNewCamp(CLIENT, campaignRule.getCampaignId(), group);
        cmdRule.cmdSteps().groupsSteps().postSaveTextAdGroups(groupsParameters);

        List<Banner> bannerList = cmdRule.cmdSteps().groupsSteps().getBanners(CLIENT, campaignRule.getCampaignId());
        group.getBanners().get(0).setBid(bannerList.get(0).getBid());
        group.getBanners().get(1).setBid(bannerList.get(1).getBid());
        assertThat("баннеры сохранились успешно",
                bannerList, beanDiffer(group.getBanners()).useCompareStrategy(onlyExpectedFields()));
    }
}
