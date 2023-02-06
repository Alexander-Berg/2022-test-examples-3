package ru.yandex.autotests.direct.cmd.conditions;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.commons.group.Retargeting;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.directapi.model.User;
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
@Description("Проверка статусов при добавлении первого условия (ретаргетинг)")
@Stories(TestFeatures.Groups.BANNER_MULTI_SAVE)
@Features(TestFeatures.GROUPS)
@Tag(CampTypeTag.TEXT)
@Tag(TrunkTag.YES)
@Tag("TESTIRT-8612")
public class AddFirstRetargetingToGroupTest extends AddFirstConditionToGroupTestBase {

    private Retargeting expectedRetargeting;

    @Override
    public BannersRule getBannersRule() {
        return new TextBannersRule().overrideGroupTemplate(new Group().withPhrases(emptyList()));
    }

    @Before
    public void before() {
        assumeThat("у группы нет условий", bannersRule.getCurrentGroup(),
                beanDiffer(new Group().withPhrases(emptyList()).withRetargetings(emptyList()))
                        .useCompareStrategy(onlyFields(newPath("phrases"), newPath("retargetings"))));
        expectedRetargeting = new Retargeting()
                .withRetCondId(getRetargetingCondition().longValue())
                .withIsSuspended(isSuspended);
        expectedGroup = bannersRule.getCurrentGroup();
        expectedGroup.withRetargetings(singletonList(expectedRetargeting));
        cmdRule.cmdSteps().groupsSteps().prepareGroupForUpdate(expectedGroup, CampaignTypeEnum.TEXT);
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("10865")
    public void addFistRetargetingToDraftGroupTest() {
        addFistConditionToDraftGroupTest();
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("10864")
    public void addFistRetargetingToActiveGroupTest() {
        addFistConditionToActiveGroupTest();
    }

    @Override
    protected String getClient() {
        return "at-direct-back-ret5";
    }

    private Integer getRetargetingCondition() {
        TestEnvironment.newDbSteps().useShardForLogin(getClient()).retargetingConditionSteps()
                .deleteUnusedRetargetingsConditions(Long.valueOf(User.get(getClient()).getClientID()));
        return cmdRule.apiSteps().retargetingSteps().addRandomRetargetingCondition(getClient());
    }

    @Override
    protected Group getExpectedGroupDraft() {
        return super.getExpectedGroupDraft().withRetargetings(singletonList(expectedRetargeting));
    }

    @Override
    protected Group getExpectedGroupActive() {
        return super.getExpectedGroupActive().withRetargetings(singletonList(expectedRetargeting));
    }
}
