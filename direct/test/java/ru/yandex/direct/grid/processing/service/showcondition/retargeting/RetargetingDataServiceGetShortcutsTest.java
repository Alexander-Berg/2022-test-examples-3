package ru.yandex.direct.grid.processing.service.showcondition.retargeting;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import one.util.streamex.StreamEx;
import org.jooq.Select;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.entity.retargeting.model.ConditionType;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.model.Rule;
import ru.yandex.direct.core.entity.retargeting.model.RuleType;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.info.RetargetingInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.core.util.yt.YtDynamicSupport;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.client.GdClientInfo;
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingConditionFilter;
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingConditionType;
import ru.yandex.direct.grid.processing.service.campaign.CampaignInfoService;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder;
import ru.yandex.yt.ytclient.wire.UnversionedRowset;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.common.util.RepositoryUtils.booleanToLong;
import static ru.yandex.direct.core.entity.retargeting.service.RetargetingConditionShortcutService.CAMPAIGN_GOALS_SHORTCUT_NAME;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoalByType;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultRetCondition;
import static ru.yandex.direct.core.testing.data.TestRetargetings.defaultRetargeting;
import static ru.yandex.direct.grid.processing.service.showcondition.converter.RetargetingConverter.toGdRetargetingCondition;
import static ru.yandex.direct.grid.schema.yt.Tables.RETARGETING_CONDITIONSTABLE_DIRECT;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowBuilder.rowBuilder;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder.rowsetBuilder;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RetargetingDataServiceGetShortcutsTest {
    @Autowired
    private Steps steps;

    @Autowired
    private GridContextProvider gridContextProvider;

    @Autowired
    private RetargetingDataService retargetingDataService;

    @Autowired
    private CampaignInfoService campaignInfoService;

    @Autowired
    private YtDynamicSupport gridYtSupport;

    private ClientInfo clientInfo;
    private GdClientInfo gdClientInfo;
    private CampaignInfo campaignInfo;
    private AdGroupInfo adGroupInfo;
    private Goal savedGoal;
    private RetargetingCondition shortcutRetargetingCondition;
    private RetargetingInfo shortcutRetargetingInfo;
    private RetargetingInfo shortcutRetargetingInfoDuplicate;

    @Before
    public void initTestData() {
        var userInfo = steps.userSteps().createDefaultUser();
        clientInfo = userInfo.getClientInfo();
        var operator = userInfo.getUser();
        gdClientInfo = ContextHelper.toGdClientInfo(operator);
        var context = ContextHelper.buildContext(operator)
                .withFetchedFieldsReslover(null);
        gridContextProvider.setGridContext(context);

        campaignInfo = steps.campaignSteps().createActiveCampaign(clientInfo, CampaignsPlatform.SEARCH);
        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);

        savedGoal = defaultGoalByType(GoalType.GOAL);
        shortcutRetargetingCondition = createShortcutRetargetingCondition(CAMPAIGN_GOALS_SHORTCUT_NAME);
        var shortcutRetargetingConditionInfo = steps.retConditionSteps().createRetCondition(
                shortcutRetargetingCondition, clientInfo);

        var retargeting = defaultRetargeting(campaignInfo.getCampaignId(), adGroupInfo.getAdGroupId(),
                shortcutRetargetingConditionInfo.getRetConditionId());
        shortcutRetargetingInfo = steps.retargetingSteps().createRetargeting(
                retargeting, adGroupInfo, shortcutRetargetingConditionInfo);
        shortcutRetargetingInfoDuplicate = steps.retargetingSteps().createRetargeting(
                retargeting, adGroupInfo, shortcutRetargetingConditionInfo);
    }

    private RetargetingCondition createShortcutRetargetingCondition(String shortcutName) {
        Rule rule = new Rule()
                .withGoals(List.of(savedGoal))
                .withType(RuleType.OR);
        return (RetargetingCondition) defaultRetCondition(clientInfo.getClientId())
                .withType(ConditionType.shortcuts)
                .withName(shortcutName)
                .withRules(singletonList(rule))
                .withAvailable(true);
    }

    @Test
    public void getRetargetingConditionsAndAvailableShortcutsRowset_NoShortcuts() {
        GdRetargetingConditionFilter filter = new GdRetargetingConditionFilter()
                .withRetargetingConditionTypeIn(Set.of(GdRetargetingConditionType.METRIKA_GOALS))
                .withInterest(false);
        var campaignId = campaignInfo.getCampaignId();

        doAnswer(getAnswerForConditions(List.of()))
                .when(gridYtSupport).selectRows(eq(clientInfo.getShard()), any(Select.class));

        var retargetingConditions = retargetingDataService
                .getRetargetingConditionsAndAvailableShortcutsRowset(gdClientInfo, filter, campaignId);

        var expected = List.of();

        assertThat(retargetingConditions)
                .is(matchedBy(beanDiffer(expected).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void getRetargetingConditionsAndAvailableShortcutsRowset_OnlySavedShortcuts() {
        GdRetargetingConditionFilter filter = new GdRetargetingConditionFilter()
                .withRetargetingConditionTypeIn(Set.of(GdRetargetingConditionType.SHORTCUTS))
                .withInterest(false);
        var campaignId = campaignInfo.getCampaignId();

        doAnswer(getAnswerForConditions(List.of(shortcutRetargetingInfo)))
                .when(gridYtSupport).selectRows(eq(clientInfo.getShard()), any(Select.class));

        var retargetingConditions = retargetingDataService
                .getRetargetingConditionsAndAvailableShortcutsRowset(gdClientInfo, filter, campaignId);

        var campaigns = campaignInfoService.getTruncatedCampaigns(clientInfo.getClientId(), List.of(campaignId));
        var expected = List.of(toGdRetargetingCondition(shortcutRetargetingCondition)
                .withAdGroupIds(Set.of(adGroupInfo.getAdGroupId()))
                .withCampaigns(new ArrayList<>(campaigns.values())));

        assertThat(retargetingConditions)
                .is(matchedBy(beanDiffer(expected).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void getRetargetingConditionsAndAvailableShortcutsRowset_DuplicateShortcutsExist_NoException() {
        GdRetargetingConditionFilter filter = new GdRetargetingConditionFilter()
                .withRetargetingConditionTypeIn(Set.of(GdRetargetingConditionType.SHORTCUTS))
                .withInterest(false);
        var campaignId = campaignInfo.getCampaignId();

        doAnswer(getAnswerForConditions(List.of(shortcutRetargetingInfo, shortcutRetargetingInfoDuplicate)))
                .when(gridYtSupport).selectRows(eq(clientInfo.getShard()), any(Select.class));

        var retargetingConditions = retargetingDataService
                .getRetargetingConditionsAndAvailableShortcutsRowset(gdClientInfo, filter, campaignId);

        var campaigns = campaignInfoService.getTruncatedCampaigns(clientInfo.getClientId(), List.of(campaignId));
        var expected = List.of(toGdRetargetingCondition(shortcutRetargetingCondition)
                .withAdGroupIds(Set.of(adGroupInfo.getAdGroupId()))
                .withCampaigns(new ArrayList<>(campaigns.values())));

        assertThat(retargetingConditions)
                .is(matchedBy(beanDiffer(expected).useCompareStrategy(onlyExpectedFields())));
    }

    private static Answer<UnversionedRowset> getAnswerForConditions(List<RetargetingInfo> retargetings) {
        List<RetargetingCondition> retargetingConditions = StreamEx.of(retargetings)
                .map(RetargetingInfo::getRetConditionInfo)
                .map(RetConditionInfo::getRetCondition)
                .toList();

        return invocation -> convertToRetargetingConditionNode(retargetingConditions);
    }

    private static UnversionedRowset convertToRetargetingConditionNode(List<RetargetingCondition> retargetingConditions) {
        RowsetBuilder builder = rowsetBuilder();
        retargetingConditions.forEach(condition -> {
                    var rule = condition.getRules().get(0);
                    var ruleType = rule.getType().toString().toLowerCase();
                    var goal = rule.getGoals().get(0);
                    var conditionJson = "[{\"goals\":[{\"goal_id\":" + goal.getId() + ",\"time\":" + goal.getTime()
                            + "}]," + "\"type\":\"" + ruleType + "\"}]";
                    builder.add(
                            rowBuilder()
                                    .withColValue(RETARGETING_CONDITIONSTABLE_DIRECT.CLIENT_ID, condition.getClientId())
                                    .withColValue(RETARGETING_CONDITIONSTABLE_DIRECT.RET_COND_ID, condition.getId())
                                    .withColValue(RETARGETING_CONDITIONSTABLE_DIRECT.RETARGETING_CONDITIONS_TYPE,
                                            condition.getType().name())
                                    .withColValue(RETARGETING_CONDITIONSTABLE_DIRECT.CONDITION_NAME,
                                            condition.getName())
                                    .withColValue(RETARGETING_CONDITIONSTABLE_DIRECT.CONDITION_DESC,
                                            condition.getDescription())
                                    .withColValue(RETARGETING_CONDITIONSTABLE_DIRECT.IS_DELETED,
                                            booleanToLong(condition.getDeleted()))
                                    .withColValue(RETARGETING_CONDITIONSTABLE_DIRECT.NEGATIVE,
                                            booleanToLong(condition.getNegative()))
                                    .withColValue(RETARGETING_CONDITIONSTABLE_DIRECT.INTEREST,
                                            booleanToLong(condition.getInterest()))
                                    .withColValue(RETARGETING_CONDITIONSTABLE_DIRECT.CONDITION_JSON, conditionJson)
                    );
                }
        );

        return builder.build();
    }
}
