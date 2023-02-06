package ru.yandex.autotests.direct.cmd.groups.mobile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.editadgroupsmobilecontent.EditAdGroupsMobileContentRequest;
import ru.yandex.autotests.direct.cmd.data.interest.TargetInterests;
import ru.yandex.autotests.direct.cmd.data.showcamp.ShowCampResponse;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.MobileBannersRule;
import ru.yandex.autotests.direct.cmd.steps.retargeting.RetargetingHelper;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.db.beans.retargeting.Goal;
import ru.yandex.autotests.direct.db.beans.retargeting.RetargetingConditionRule;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.BidsRetargetingRecord;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.RetargetingConditionsRecord;
import ru.yandex.autotests.direct.db.models.jooq.ppcdict.tables.records.TargetingCategoriesRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsNull.notNullValue;
import static ru.yandex.autotests.direct.cmd.data.interest.TargetInterestsFactory.defaultInterests;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@Aqua.Test
@Description("Проверка сохранения интересов в мобильных-группах")
@Stories(TestFeatures.Groups.BANNER_MULTI_SAVE)
@Features(TestFeatures.GROUPS)
@Tag(CmdTag.SAVE_MOBILE_ADGROUPS)
@Tag(ObjectTag.GROUP)
@Tag(ObjectTag.TARGET_INTERESTS)
@Tag(CampTypeTag.MOBILE)
@Tag(TrunkTag.YES)
@Tag(TestFeatures.Groups.BANNER_MULTI_SAVE)
@Tag(TestFeatures.GROUPS)
public class SaveMobileTextAdGroupWithInterestsTest {
    private static final Double EXPECTED_PRICE_CONTEXT = 0.88d;
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    protected static String CLIENT = Logins.CLIENT_WITH_INTERESTS;
    private MobileBannersRule bannersRule = new MobileBannersRule().withUlogin(CLIENT);
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    private Long categoryId;
    private Long secondCategoryId;
    private RetargetingConditionsRecord expectedRetargeting;

    @Before
    public void before() {
        TargetingCategoriesRecord targetingCategory =
                TestEnvironment.newDbSteps().useShardForLogin(CLIENT).interestSteps()
                        .getTargetingCategoriesRecords(RetargetingHelper.getRandomTargetCategoryId());
        expectedRetargeting = new RetargetingConditionsRecord();
        expectedRetargeting.setProperties("interest");
        expectedRetargeting.setConditionJson(Collections.singletonList(new RetargetingConditionRule()
                .withType("all")
                .withGoals(
                        Collections.singletonList(new Goal().withGoalId(targetingCategory.getImportId().longValue()).withTime(90l)))
                .toString()).toString());
        categoryId = targetingCategory.getCategoryId();
        secondCategoryId = getAnotherCategoryId(categoryId);
    }

    @Test
    @Description("сохраняем группу с интересами")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10656")
    public void saveGroupWithInterests() {
        Group group = bannersRule.getGroup();
        group.setCampaignID(bannersRule.getCampaignId().toString());
        group.getBanners().stream().forEach(b -> {
            b.withAdType("text");
            b.withCid(bannersRule.getCampaignId());
        });

        group.setTargetInterests(defaultInterests(categoryId));


        GroupsParameters groupRequest = GroupsParameters.forExistingCamp(CLIENT, bannersRule.getCampaignId(), group);
        bannersRule.saveGroup(groupRequest);

        Long groupId = cmdRule.cmdSteps().campaignSteps().getShowCamp(CLIENT,
                bannersRule.getCampaignId().toString()).getGroups().get(1).getAdGroupId();

        BidsRetargetingRecord bidsRetargeting = TestEnvironment.newDbSteps().useShardForLogin(CLIENT)
                .bidsSteps()
                .getBidsRetargetingRecordByPid(groupId).get(0);
        assumeThat("У группы есть ретаргетинг", bidsRetargeting, notNullValue());
        RetargetingConditionsRecord retargeting = TestEnvironment.newDbSteps().useShardForLogin(CLIENT)
                .retargetingConditionSteps()
                .getRetargeingConditionByRetCondId(
                        bidsRetargeting.getRetCondId()
                );
        assertThat("Ретартгетинг сохранился с правильным типом",
                retargeting.intoMap(),
                beanDiffer(expectedRetargeting.intoMap())
                        .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields()));
    }

    @Test
    @Description("сохраняем группу с пустым интересом")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10653")
    public void deleteFromGroupInterests() {
        Group group = bannersRule.getCurrentGroup();
        group.setTargetInterests(new ArrayList<>());
        cmdRule.cmdSteps().groupsSteps().prepareGroupForUpdate(group, bannersRule.getMediaType());
        GroupsParameters groupRequest = GroupsParameters.forExistingCamp(CLIENT, bannersRule.getCampaignId(), group);

        bannersRule.saveGroup(groupRequest);

        ShowCampResponse showCamp = cmdRule.cmdSteps().campaignSteps().getShowCamp(CLIENT,
                bannersRule.getCampaignId().toString());

        List<TargetInterests> actualInterests = showCamp.getGroups().get(0).getTargetInterests();

        assertThat("Сохранилось ождаемое число интересов", actualInterests, Matchers.nullValue());

    }

    @Test
    @Description("сохраняем группу с 2 интересами")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10652")
    public void addToGroupTwoInterests() {
        Group group = bannersRule.getGroup();
        group.setCampaignID(bannersRule.getCampaignId().toString());
        group.getBanners().stream().forEach(b -> {
            b.withAdType("text");
            b.withCid(bannersRule.getCampaignId());
        });


        group.setTargetInterests(defaultInterests(categoryId, secondCategoryId));

        GroupsParameters groupRequest = GroupsParameters.forExistingCamp(CLIENT, bannersRule.getCampaignId(), group);
        bannersRule.saveGroup(groupRequest);

        Long groupId = cmdRule.cmdSteps().campaignSteps().getShowCamp(CLIENT,
                bannersRule.getCampaignId().toString()).getGroups().get(1).getAdGroupId();

        List<BidsRetargetingRecord> bidsRetargeting = TestEnvironment.newDbSteps().useShardForLogin(CLIENT)
                .bidsSteps()
                .getBidsRetargetingRecordByPid(groupId);
        assertThat("У группы есть есть ожидаемые ретаргетинги", bidsRetargeting, hasSize(2));
    }

    @Test
    @Description("удаляем 1 интерес")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10654")
    public void deleteOneOfTwoInterests() {
        Group group = bannersRule.getGroup();

        group.setTargetInterests(defaultInterests(categoryId, secondCategoryId));

        GroupsParameters groupRequest = GroupsParameters.forExistingCamp(CLIENT, bannersRule.getCampaignId(), group);
        bannersRule.saveGroup(groupRequest);
        ShowCampResponse showCampResponse = cmdRule.cmdSteps().campaignSteps().getShowCamp(CLIENT,
                bannersRule.getCampaignId().toString());
        Long groupId = showCampResponse.getGroups().get(1).getAdGroupId();
        Long bannerId = showCampResponse.getGroups().get(1).getBid();

        Group savedGroup =
                cmdRule.cmdSteps().groupsSteps().getEditAdGroupsMobileContent(EditAdGroupsMobileContentRequest
                        .forSingleBanner(CLIENT, bannersRule.getCampaignId(), groupId, bannerId))
                        .getCampaign().getGroups().get(0);
        cmdRule.cmdSteps().groupsSteps().prepareGroupForUpdate(savedGroup, bannersRule.getMediaType());

        List<BidsRetargetingRecord> bidsRetargeting = TestEnvironment.newDbSteps().useShardForLogin(CLIENT)
                .bidsSteps()
                .getBidsRetargetingRecordByPid(groupId);

        assumeThat("У группы есть есть ожидаемые ретаргетинги", bidsRetargeting, hasSize(2));

        savedGroup.setTargetInterests(defaultInterests(categoryId));
        groupRequest = GroupsParameters.forExistingCamp(CLIENT, bannersRule.getCampaignId(), savedGroup);
        bannersRule.saveGroup(groupRequest);


        bidsRetargeting = TestEnvironment.newDbSteps().useShardForLogin(CLIENT)
                .bidsSteps()
                .getBidsRetargetingRecordByPid(groupId);
        assertThat("У группы есть есть ожидаемые ретаргетинги", bidsRetargeting, hasSize(1));
    }

    @Test
    @Description("Устанавливаем price context")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10655")
    public void changePriceContext() {
        Group group = bannersRule.getCurrentGroup();

        List<TargetInterests> interestses = defaultInterests(categoryId);
        interestses.get(0).withPriceContext(EXPECTED_PRICE_CONTEXT);
        group.setTargetInterests(interestses);

        cmdRule.cmdSteps().groupsSteps().prepareGroupForUpdate(group, bannersRule.getMediaType());
        GroupsParameters groupRequest = GroupsParameters.forExistingCamp(CLIENT, bannersRule.getCampaignId(), group);

        bannersRule.saveGroup(groupRequest);
        List<BidsRetargetingRecord> bidsRetargeting = TestEnvironment.newDbSteps().useShardForLogin(CLIENT)
                .bidsSteps()
                .getBidsRetargetingRecordByPid(bannersRule.getGroupId());

        assertThat("Изменения сохранились",
                bidsRetargeting.get(0).getPriceContext().doubleValue(),
                equalTo(EXPECTED_PRICE_CONTEXT));
    }

    // Возвращает случайное значение categoryId, отличное от переданного
    private Long getAnotherCategoryId(Long categoryId) {
        Long anotherCategoryId = RetargetingHelper.getRandomTargetCategoryId();
        int tries_count = 100;
        while (anotherCategoryId == categoryId && tries_count > 0) {
            anotherCategoryId = RetargetingHelper.getRandomTargetCategoryId();
            tries_count--;
        }
        return anotherCategoryId;
    }
}
