package ru.yandex.autotests.direct.cmd.adjustment.demography;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.DemographyCondition;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.DemographyMultiplier;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.HierarchicalMultipliers;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.editadgroupsmobilecontent.EditAdGroupsMobileContentRequest;
import ru.yandex.autotests.direct.cmd.data.editadgroupsmobilecontent.EditAdGroupsMobileContentResponse;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.MobileBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.data.campaigns.adjustment.DemographyAgeEnum;
import ru.yandex.autotests.direct.httpclient.data.campaigns.adjustment.DemographyGenderEnum;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@Aqua.Test
@Description("Проверка настройки цен на мобильных устройствах для группы объявлений мобильных кампаний" +
        " (корректировка ставок demography_multiplier)")
@Stories(TestFeatures.Groups.ADJUSTMENT_DEMOGRAPHY)
@Features(TestFeatures.GROUPS)
@Tag(CmdTag.SAVE_MOBILE_ADGROUPS)
@Tag(ObjectTag.ADJUSTMENT)
@Tag(CampTypeTag.MOBILE)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class GroupDemographyMultiplierMobileAppCampTest {

    protected final static String CLIENT = "at-direct-backend-c";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Parameterized.Parameter
    public String multiplierPct;
    protected BannersRule bannersRule = new MobileBannersRule().withUlogin(CLIENT);
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    protected Long campaignId;

    @Parameterized.Parameters(name = "Значение параметра multiplier_pct = {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {"151"},
                {"100"},
                {"0"}
        });
    }

    @Before
    public void before() {
        campaignId = bannersRule.getCampaignId();
    }

    @Test
    @Description("Проверка ответа контроллера saveMobileAdGroups")
    @ru.yandex.qatools.allure.annotations.TestCaseId("8969")
    public void saveMobileAdGroupsTest() {
        cmdRule.cmdSteps().groupsSteps().
                postSaveMobileAdGroups(GroupsParameters.forExistingCamp(CLIENT, campaignId, getGroup()));
        EditAdGroupsMobileContentResponse actualResponse = cmdRule.cmdSteps().groupsSteps()
                .getEditAdGroupsMobileContent(new EditAdGroupsMobileContentRequest()
                        .withCid(campaignId)
                        .withAdGroupIds(bannersRule.getGroupId().toString())
                        .withUlogin(CLIENT));

        check(actualResponse);
    }

    private void check(EditAdGroupsMobileContentResponse actualResponse) {
        assumeThat("группа сохранилась", actualResponse.getCampaign().getGroups(), hasSize(1));

        assertThat("мобильный баннер в ответе контроллера соответствует отправленному в запросе",
                actualResponse.getCampaign().getGroups().get(0).getHierarchicalMultipliers(),
                beanDiffer(getHierarchicalMultipliers()));
    }

    private HierarchicalMultipliers getHierarchicalMultipliers() {
        List<DemographyCondition> demographyConditionList = new ArrayList<>();
        demographyConditionList.add(new DemographyCondition()
                .withGender(DemographyGenderEnum.FEMALE.getKey())
                .withAge(DemographyAgeEnum.BETWEEN_0_AND_17.getKey())
                .withMultiplierPct(multiplierPct));

        return new HierarchicalMultipliers()
                .withDemographyMultiplier(new DemographyMultiplier()
                        .withEnabled(1)
                        .withConditions(demographyConditionList));
    }

    private Group getGroup() {
        Group group = bannersRule.getGroup().
                withAdGroupID(bannersRule.getGroupId().toString()).
                withHierarchicalMultipliers(getHierarchicalMultipliers());
        group.getBanners().get(0).withBid(bannersRule.getBannerId());
        return group;
    }
}
