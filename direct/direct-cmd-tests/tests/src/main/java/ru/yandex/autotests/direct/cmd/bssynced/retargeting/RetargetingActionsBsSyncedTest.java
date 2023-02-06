package ru.yandex.autotests.direct.cmd.bssynced.retargeting;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.bssynced.BsSyncedHelper;
import ru.yandex.autotests.direct.cmd.data.ajaxupdatehowconditions.AjaxUpdateShowConditions;
import ru.yandex.autotests.direct.cmd.data.ajaxupdatehowconditions.AjaxUpdateShowConditionsObjects;
import ru.yandex.autotests.direct.cmd.data.ajaxupdatehowconditions.AjaxUpdateShowConditionsRequest;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.commons.group.Retargeting;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.BannersRuleFactory;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.directapi.enums.StatusBsSynced;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Description("Проверка сброса статуса bsSynced группы при действиях над ретаргетингом")
@Stories(TestFeatures.Retargeting.AJAX_SAVE_RETARGETING_CONDITIONS)
@Features(TestFeatures.RETARGETING)
@Tag(ObjectTag.RETAGRETING)
@Tag(CampTypeTag.TEXT)
@Tag(CampTypeTag.MOBILE)
@RunWith(Parameterized.class)
public class RetargetingActionsBsSyncedTest {

    protected static final String CLIENT = "at-direct-retargeting75";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule;
    public CampaignTypeEnum campaignType;
    private BannersRule bannersRule;
    private Long retCondId;
    private Long campaignId;

    public RetargetingActionsBsSyncedTest(CampaignTypeEnum campaignType) {
        this.campaignType = campaignType;
        bannersRule = BannersRuleFactory.getBannersRuleBuilderByCampType(campaignType).withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Parameterized.Parameters(name = "Сброс bsSynced группы после изменения ретаргетинга. Тип кампании: {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {CampaignTypeEnum.TEXT},
                {CampaignTypeEnum.MOBILE}
        });
    }

    @Before
    public void before() {
        campaignId = bannersRule.getCampaignId();
        retCondId = addRetargetingCondition();
    }

    @Test
    @Description("Проверяем сброс статуса bsSynced группы при удалении ретаргетинга из группы")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9365")
    public void checkBsSyncedAfterDeleteRetargetingTest() {
        cmdRule.apiSteps().retargetingSteps()
                .addRetargetingToBanner(CLIENT, bannersRule.getBannerId(), retCondId.intValue());
        setStatuses();
        Group group = getGroupWithIds();
        group.setRetargetings(emptyList());
        bannersRule.saveGroup(GroupsParameters.forExistingCamp(CLIENT, campaignId, group));

        BsSyncedHelper.checkGroupBsSynced(bannersRule.getCurrentGroup(), StatusBsSynced.NO);
    }

    @Test
    @Description("Проверяем сброс статуса bsSynced группы при остановке ретаргетинга")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9366")
    public void checkBsSyncedAfterSuspendRetargetingTest() {
        cmdRule.apiSteps().retargetingSteps()
                .addRetargetingToBanner(CLIENT, bannersRule.getBannerId(), retCondId.intValue());
        setStatuses();
        suspendRetargeting("1");

        BsSyncedHelper.checkGroupBsSynced(bannersRule.getCurrentGroup(), StatusBsSynced.NO);
    }

    @Test
    @Description("Проверяем сброс статуса bsSynced группы при включении ретаргетинга")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9367")
    public void checkBsSyncedAfterEnableRetargetingTest() {
        cmdRule.apiSteps().retargetingSteps()
                .addRetargetingToBanner(CLIENT, bannersRule.getBannerId(), retCondId.intValue());
        suspendRetargeting("1");
        setStatuses();
        suspendRetargeting("0");

        BsSyncedHelper.checkGroupBsSynced(bannersRule.getCurrentGroup(), StatusBsSynced.NO);
    }

    @Test
    @Description("Проверяем сброс статуса bsSynced группы при добавлении ретаргетинга")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9368")
    public void checkBsSyncedAfterAddRetargetingTest() {
        setStatuses();
        Group group = getGroupWithIds();
        group.setRetargetings(singletonList(new Retargeting()
                .withRetCondId(retCondId)
                .withPriceContext("3")));
        bannersRule.saveGroup(GroupsParameters.forExistingCamp(CLIENT, campaignId, group));

        BsSyncedHelper.checkGroupBsSynced(bannersRule.getCurrentGroup(), StatusBsSynced.NO);
    }

    private Group getGroupWithIds() {
        Group group = bannersRule.getGroup();
        group.setAdGroupID(String.valueOf(bannersRule.getGroupId()));
        group.getBanners().get(0).setBid(bannersRule.getBannerId());
        return group;
    }

    private void suspendRetargeting(String suspendFlag) {
        String retargetingId = cmdRule.cmdSteps().campaignSteps().getShowCamp(CLIENT, String.valueOf(campaignId))
                .getGroups().get(0).getRetargetings().get(0).getRetId().toString();
        AjaxUpdateShowConditionsRequest request = new AjaxUpdateShowConditionsRequest()
                .withRetargetings(bannersRule.getGroupId().toString(),
                        new AjaxUpdateShowConditionsObjects()
                                .withEdited(retargetingId, new AjaxUpdateShowConditions()
                                        .withIsSuspended(suspendFlag)))
                .withCid(String.valueOf(campaignId))
                .withUlogin(CLIENT);
        cmdRule.cmdSteps().ajaxUpdateShowConditionsSteps().postAjaxUpdateShowConditions(request);
        assumeThat("последний ретаргетинг остановлен", cmdRule.cmdSteps().campaignSteps()
                .getShowCamp(CLIENT, String.valueOf(campaignId))
                .getGroups().get(0).getRetargetings().get(0).getIsSuspended(), equalTo(suspendFlag));
    }

    private void setStatuses() {
        cmdRule.apiSteps().groupFakeSteps().makeGroupFullyModerated(bannersRule.getGroupId());
        cmdRule.apiSteps().bannersFakeSteps().makeBannerFullyModerated(bannersRule.getBannerId());
        cmdRule.apiSteps().groupFakeSteps()
                .setGroupFakeStatusBsSynced(bannersRule.getGroupId(), StatusBsSynced.YES.toString());
        cmdRule.apiSteps().campaignFakeSteps().setStatusModerate(bannersRule.getCampaignId(), "Yes");
    }

    private Long addRetargetingCondition() {
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).retargetingConditionSteps()
                .deleteUnusedRetargetingsConditions(Long.valueOf(User.get(CLIENT).getClientID()));
        return cmdRule.apiSteps().retargetingSteps()
                .addRandomRetargetingCondition(CLIENT).longValue();
    }
}
