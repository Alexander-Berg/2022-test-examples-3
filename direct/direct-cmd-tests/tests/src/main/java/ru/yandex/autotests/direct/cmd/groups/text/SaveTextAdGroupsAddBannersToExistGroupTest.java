package ru.yandex.autotests.direct.cmd.groups.text;

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
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.direct.cmd.util.BeanLoadHelper.loadCmdBean;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;

@Aqua.Test
@Description("Проверка контроллера saveTextAdGroups при добавлении баннера к существующей группе")
@Stories(TestFeatures.Banners.BANNERS_PARAMETERS)
@Features(TestFeatures.BANNERS)
@Tag(TrunkTag.YES)
public class SaveTextAdGroupsAddBannersToExistGroupTest {
    private static final String CLIENT = "at-direct-b-bannersmultisave";

    @ClassRule
    public static DirectCmdRule directCmdClassRule = DirectCmdRule.defaultClassRule();

    public TextBannersRule textBannersRule = new TextBannersRule().withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(textBannersRule);

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("10638")
    public void createGroupWithTwoBanners() {
        Group group = textBannersRule.getGroup();
        group.setAdGroupID(textBannersRule.getGroupId().toString());
        group.getBanners().get(0).setBid(textBannersRule.getBannerId());
        group.getBanners().add(loadCmdBean(CmdBeans.COMMON_REQUEST_GROUP_TEXT_DEFAULT2, Group.class).getBanners().get(0));

        GroupsParameters groupsParameters = GroupsParameters.forExistingCamp(CLIENT, textBannersRule.getCampaignId(), group);
        cmdRule.cmdSteps().groupsSteps().postSaveTextAdGroups(groupsParameters);

        List<Banner> bannerList = cmdRule.cmdSteps().groupsSteps().getBanners(CLIENT, textBannersRule.getCampaignId());
        assumeThat("в группе 2 баннера", bannerList, hasSize(2));
        Banner banner =  group.getBanners().get(1);
        banner.withBid(bannerList.get(1).getBid())
                .withAutobudget(null)
                .withDayBudget(null)
                .withHref("http://ya.ru")
                .withIsVcardOpen(null)
                .withVcardCollapsed(null)
                .withLoadVCardFromClient(null)
                .withModelId(null)
                .withVcardCollapsed(null);
        banner.setSiteLinks(null);
        group.getBanners().set(1, banner);
        assertThat("баннеры сохранились успешно",
                bannerList.get(1), beanDiffer(group.getBanners().get(1)).useCompareStrategy(onlyExpectedFields()));
    }
}
