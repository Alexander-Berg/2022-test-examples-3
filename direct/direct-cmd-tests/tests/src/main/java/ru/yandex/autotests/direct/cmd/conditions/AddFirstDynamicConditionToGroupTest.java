package ru.yandex.autotests.direct.cmd.conditions;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.commons.StatusModerate;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.group.DynamicCondition;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DynamicBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.directapi.enums.StatusBsSynced;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyFields;

@Aqua.Test
@Description("Проверка статусов при добавлении первого условия (фраза)")
@Stories(TestFeatures.Groups.BANNER_MULTI_SAVE)
@Features(TestFeatures.GROUPS)
@Tag(CampTypeTag.DYNAMIC)
@Tag(TrunkTag.YES)
@Tag("TESTIRT-8612")
public class AddFirstDynamicConditionToGroupTest extends AddFirstConditionToGroupTestBase {

    private DynamicCondition expectedDynamicCond;

    @Override
    public BannersRule getBannersRule() {
        return new DynamicBannersRule()
                .overrideGroupTemplate(new Group().withDynamicConditions(emptyList()));
    }

    @Before
    public void before() {
        assumeThat("у группы нет условий", bannersRule.getCurrentGroup(),
                beanDiffer(new Group().withDynamicConditions(emptyList()).withRetargetings(emptyList()))
                        .useCompareStrategy(onlyFields(newPath("dynamicConditions"))));
        expectedDynamicCond = BeanLoadHelper
                .loadCmdBean(CmdBeans.COMMON_REQUEST_DYNAMIC_COND_FULL, DynamicCondition.class)
                .withIsSuspended(isSuspended);
        expectedGroup = bannersRule.getCurrentGroup();
        expectedGroup.withDynamicConditions(singletonList(expectedDynamicCond));
        cmdRule.cmdSteps().groupsSteps().prepareGroupForUpdate(expectedGroup, CampaignTypeEnum.DTO);
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("10856")
    public void addFistDynamicConditionToDraftGroupTest() {
        addFistConditionToDraftGroupTest();
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("10857")
    public void addFistDynamicConditionToActiveGroupTest() {
        addFistConditionToActiveGroupTest();
    }

    @Override
    protected Group getExpectedGroupDraft() {
        return super.getExpectedGroupDraft()
                .withDynamicConditions(singletonList(getExpectedDynamicCond()));
    }

    @Override
    protected Group getExpectedGroupActive() {
        return new Group()
                .withStatusModerate(StatusModerate.YES.toString())
                .withStatusBsSynced(StatusBsSynced.NO.toString())
                .withBanners(singletonList(new Banner()
                        .withStatusModerate(StatusModerate.YES.toString())
                        .withStatusBsSynced(StatusBsSynced.NO.toString()))
                )
                .withDynamicConditions(singletonList(getExpectedDynamicCond()));
    }

    private DynamicCondition getExpectedDynamicCond() {
        return new DynamicCondition()
                .withDynamicConditionName(expectedDynamicCond.getDynamicConditionName())
                .withStatusBsSynced(StatusBsSynced.NO.toString())
                .withIsSuspended("0".equals(isSuspended) ? "" : isSuspended);
    }
}
