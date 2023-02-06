package ru.yandex.autotests.direct.cmd.conditions;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.interest.TargetInterests;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.MobileBannersRule;
import ru.yandex.autotests.direct.cmd.steps.retargeting.RetargetingHelper;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.directapi.enums.StatusBsSynced;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static ru.yandex.autotests.direct.cmd.data.interest.TargetInterestsFactory.defaultTargetInterest;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyFields;

@Aqua.Test
@Description("Проверка статусов при добавлении первого условия (интерес)")
@Stories(TestFeatures.Groups.BANNER_MULTI_SAVE)
@Features(TestFeatures.GROUPS)
@Tag(CampTypeTag.MOBILE)
@Tag(TrunkTag.YES)
@Tag("TESTIRT-8612")
public class AddFirstInterestToGroupTest extends AddFirstConditionToGroupTestBase {

    private Long categoryId;

    @Override
    public BannersRule getBannersRule() {
        return new MobileBannersRule().overrideGroupTemplate(new Group().withPhrases(emptyList()));
    }

    @Before
    public void before() {
        assumeThat("у группы нет условий", bannersRule.getCurrentGroup(),
                beanDiffer(new Group().withPhrases(emptyList()).withRetargetings(emptyList()))
                        .useCompareStrategy(onlyFields(newPath("phrases"), newPath("retargetings"))));
        categoryId = RetargetingHelper.getRandomTargetCategoryId();
        expectedGroup = bannersRule.getCurrentGroup();
        expectedGroup.withTargetInterests(singletonList(
                defaultTargetInterest(categoryId).withIsSuspended(Integer.valueOf(isSuspended))));
        cmdRule.cmdSteps().groupsSteps().prepareGroupForUpdate(expectedGroup, CampaignTypeEnum.TEXT);
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("10859")
    public void addFistRetargetingToDraftGroupTest() {
        addFistConditionToDraftGroupTest();
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("10858")
    public void addFistRetargetingToActiveGroupTest() {
        addFistConditionToActiveGroupTest();
    }

    @Override
    protected String getClient() {
        return "at-direct-back-ret5";
    }

    @Override
    protected Group getExpectedGroupDraft() {
        return super.getExpectedGroupDraft().withTargetInterests(singletonList(
                new TargetInterests()
                        .withTargetCategoryId(categoryId)
                        .withIsSuspended(Integer.valueOf(isSuspended))
                        .withStatusBsSynced(StatusBsSynced.NO.toString())

        ));
    }

    @Override
    protected Group getExpectedGroupActive() {
        return super.getExpectedGroupActive().withTargetInterests(singletonList(
                new TargetInterests()
                        .withTargetCategoryId(categoryId)
                        .withIsSuspended(Integer.valueOf(isSuspended))
                        .withStatusBsSynced(StatusBsSynced.NO.toString())
        ));
    }
}
