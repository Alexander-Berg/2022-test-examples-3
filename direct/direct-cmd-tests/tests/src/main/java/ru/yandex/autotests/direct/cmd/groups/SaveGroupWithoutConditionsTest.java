package ru.yandex.autotests.direct.cmd.groups;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.BannersRuleFactory;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.SmokeTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyFields;

@Aqua.Test
@Description("Проверка сохранения группы без условий нацеливания")
@Stories(TestFeatures.Groups.BANNER_MULTI_SAVE)
@Features(TestFeatures.GROUPS)
@RunWith(Parameterized.class)
@Tag(CampTypeTag.TEXT)
@Tag(CampTypeTag.MOBILE)
@Tag(CampTypeTag.DYNAMIC)
@Tag(CampTypeTag.PERFORMANCE)
@Tag(TrunkTag.YES)
@Tag(SmokeTag.YES)
@Tag("TESTIRT-8612")
public class SaveGroupWithoutConditionsTest {

    private static final String CLIENT = Logins.DEFAULT_CLIENT;

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private BannersRule bannersRule;

    @Rule
    public DirectCmdRule cmdRule;

    private Group saveGroup;
    private Group expectedGroup;

    @Parameterized.Parameters(name = "Проверка статусов после остановки последней фразы у {0} кампании")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {CampaignTypeEnum.TEXT, new Group().withPhrases(Collections.emptyList())},
                {CampaignTypeEnum.MOBILE, new Group().withPhrases(Collections.emptyList())},
                {CampaignTypeEnum.DTO, new Group()
                        .withPhrases(Collections.emptyList())
                        .withDynamicConditions(Collections.emptyList())},
                {CampaignTypeEnum.DMO, new Group().withPerformanceFilters(Collections.emptyList())}
        });
    }

    public SaveGroupWithoutConditionsTest(CampaignTypeEnum campaignType, Group expectedGroup) {
        this.expectedGroup = expectedGroup;
        bannersRule = BannersRuleFactory.getBannersRuleBuilderByCampType(campaignType).withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Before
    public void before() {
        saveGroup = bannersRule.getGroup()
                .withAdGroupID(bannersRule.getGroupId().toString());
        saveGroup.getBanners().get(0).withBid(bannersRule.getBannerId());
        setAllConditionsToEmpty();
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("10878")
    public void saveGroupWithoutConditions() {
        bannersRule.saveGroup(GroupsParameters.forExistingCamp(CLIENT, bannersRule.getCampaignId(), saveGroup));
        Group actualGroup = bannersRule.getCurrentGroup();
        assertThat("у группы не условий нацеливания", actualGroup, beanDiffer(expectedGroup)
                .useCompareStrategy(onlyFields(newPath("phrases"), newPath("dynamicConditions"),
                        newPath("performanceFilters"), newPath("retargetings"))));
    }

    private void setAllConditionsToEmpty() {
        if (saveGroup.getPhrases() != null) saveGroup.setPhrases(Collections.emptyList());
        if (saveGroup.getDynamicConditions() != null) saveGroup.setDynamicConditions(Collections.emptyList());
        if (saveGroup.getPerformanceFilters() != null) saveGroup.setPerformanceFilters(Collections.emptyList());
        if (saveGroup.getRetargetings() != null) saveGroup.setRetargetings(Collections.emptyList());
    }
}
