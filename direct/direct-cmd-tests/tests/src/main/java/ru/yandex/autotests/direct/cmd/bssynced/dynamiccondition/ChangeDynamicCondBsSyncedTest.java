package ru.yandex.autotests.direct.cmd.bssynced.dynamiccondition;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.bssynced.BsSyncedHelper;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.group.DynamicCondition;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.DynamicBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.enums.StatusBsSynced;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Collections;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Проверка сброса статуса bsSynced группы при изменении данных условия нацеливания ДТО")
@Stories(TestFeatures.Conditions.AJAX_EDIT_DYNAMIC_CONDITIONS)
@Features(TestFeatures.CONDITIONS)
@Tag(ObjectTag.DYN_COND)
@Tag(CampTypeTag.DYNAMIC)
public class ChangeDynamicCondBsSyncedTest {

    private static final String CLIENT = Logins.DEFAULT_CLIENT;
    private static final String TARGET_NAME = "{adtarget_name}";
    private static final String NEW_DYN_COND_NAME = "New dynamic condition";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private BannersRule bannersRule = new DynamicBannersRule()
            .overrideGroupTemplate(new Group()
                    .withDynamicConditions(Collections.singletonList(BeanLoadHelper
                            .loadCmdBean(CmdBeans.COMMON_REQUEST_DYNAMIC_COND_DEFAULT, DynamicCondition.class))))
            .withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);


    private Long campaignId;

    @Before
    public void before() {
        campaignId = bannersRule.getCampaignId();

        BsSyncedHelper.setGroupBsSynced(cmdRule, bannersRule.getGroupId(), StatusBsSynced.YES);
    }

    @Test
    @Ignore("https://st.yandex-team.ru/DIRECT-55000")
    @Description("Проверяем сброс статуса bsSynced группы при изменении названия")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9323")
    public void checkBsSyncedChangeNameDynamicCondTest() {
        Group group = getGroupWithIds();
        group.setHrefParams(TARGET_NAME);
        bannersRule.saveGroup(GroupsParameters.forExistingCamp(CLIENT, campaignId, group));

        cmdRule.apiSteps().bannersFakeSteps().makeBannerFullyModerated(bannersRule.getBannerId());
        cmdRule.apiSteps().bannersFakeSteps().makeBannerActive(bannersRule.getBannerId());
        cmdRule.apiSteps().groupFakeSteps().makeGroupFullyModerated(bannersRule.getGroupId());
        BsSyncedHelper.setGroupBsSynced(cmdRule, bannersRule.getGroupId(), StatusBsSynced.YES);
        changeDynamicCondition(getDynamicCondition().withDynamicConditionName(NEW_DYN_COND_NAME));

        BsSyncedHelper.checkGroupBsSynced(bannersRule.getCurrentGroup(),
                StatusBsSynced.NO, StatusBsSynced.SENDING);
    }

    private void checkDynamicConditionBsSynced() {
        assertThat("статусы bsSynced соответствуют ожиданию",
                bannersRule.getCurrentGroup().getDynamicConditions().get(0).getStatusBsSynced(),
                equalTo(StatusBsSynced.NO.toString()));
    }

    private void changeDynamicCondition(DynamicCondition condition) {
        cmdRule.cmdSteps().ajaxEditDynamicConditionsSteps()
                .dynamicConditionsChangeWithAssumption(campaignId, bannersRule.getGroupId(), condition, CLIENT);
    }

    private Group getGroupWithIds() {
        Group group = bannersRule.getGroup();
        group.setAdGroupID(String.valueOf(bannersRule.getGroupId()));
        group.getBanners().get(0).setBid(bannersRule.getBannerId());
        return group;
    }

    private DynamicCondition getDynamicCondition() {
        return cmdRule.cmdSteps().campaignSteps()
                .getShowCamp(CLIENT, String.valueOf(campaignId))
                .getGroups().get(0).getDynamicConditions().get(0)
                .withIsSuspended(null)
                .withPriceContext(null)
                .withPrice(null);
    }

}
