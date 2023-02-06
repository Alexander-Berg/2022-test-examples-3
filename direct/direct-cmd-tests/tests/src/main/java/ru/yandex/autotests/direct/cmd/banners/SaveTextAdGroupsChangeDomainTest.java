package ru.yandex.autotests.direct.cmd.banners;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.List;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyFields;

//TESTIRT-2987
@Aqua.Test
@Description("Тесты группового изменения домена у баннеров в группе")
@Stories(TestFeatures.Banners.SAVE_TEXT_ADGROUPS_FIELDS )
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(ObjectTag.BANNER)
@Tag(CampTypeTag.TEXT)
public class SaveTextAdGroupsChangeDomainTest {

    private static final String CLIENT = "at-direct-bg-client";
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    private static String NEW_DOMAIN = "booking.com";
    private TextBannersRule bannersRule = new TextBannersRule()
            .overrideGroupTemplate(BeanLoadHelper.loadCmdBean(
                    CmdBeans.COMMON_REQUEST_GROUP_TEXT_WITH_TWO_BANNERS, Group.class))
            .withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    @Test
    @Description("Массовое изменение домена у баннеров группы")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9053")
    public void changeDomainSaveTextAdGroups() {

        Group expectedGroup = bannersRule.getGroup();

        GroupsParameters groupsParameters = GroupsParameters.forExistingCamp(
                CLIENT, bannersRule.getCampaignId(), expectedGroup);
        groupsParameters.setCampBannersDomain(NEW_DOMAIN);
        cmdRule.cmdSteps().authSteps().authenticate(User.get(CLIENT));
        cmdRule.cmdSteps().groupsSteps().postSaveTextAdGroups(groupsParameters);

        List<Banner> actualBanners = cmdRule.cmdSteps().groupsSteps().getGroups(
                CLIENT, bannersRule.getCampaignId()).get(0).getBanners();

        assertThat("данные баннеров не изменились", actualBanners,
                beanDiffer(expectedGroup.getBanners()).useCompareStrategy(onlyFields(
                        newPath("domain"), newPath("domainRedir"))));
    }
}
