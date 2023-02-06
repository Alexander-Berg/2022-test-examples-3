package ru.yandex.direct.grid.processing.service.showcondition.retargeting;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import one.util.streamex.StreamEx;
import org.jooq.Select;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.common.TranslationService;
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
import ru.yandex.direct.crypta.client.impl.CryptaClientStub;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.core.util.yt.YtDynamicSupport;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.client.GdClientInfo;
import ru.yandex.direct.grid.processing.model.goal.GdGoalTruncated;
import ru.yandex.direct.grid.processing.model.goal.GdGoalType;
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingCondition;
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingConditionFilter;
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingConditionRuleItem;
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingConditionType;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder;
import ru.yandex.yt.ytclient.wire.UnversionedRowset;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static ru.yandex.direct.common.util.RepositoryUtils.booleanToLong;
import static ru.yandex.direct.core.entity.retargeting.service.RetargetingConditionShortcutService.CAMPAIGN_GOALS_SHORTCUT_NAME;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoalByType;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultRetCondition;
import static ru.yandex.direct.core.testing.data.TestRetargetings.defaultRetargeting;
import static ru.yandex.direct.grid.schema.yt.Tables.RETARGETING_CONDITIONSTABLE_DIRECT;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowBuilder.rowBuilder;
import static ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder.rowsetBuilder;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RetargetingDataServiceGetRetargetingConditionsRowsetTest {
    @Autowired
    private Steps steps;

    @Autowired
    private GridContextProvider gridContextProvider;

    @Autowired
    private RetargetingDataService retargetingDataService;

    @Autowired
    private YtDynamicSupport gridYtSupport;

    @Autowired
    private TranslationService translationService;

    @Autowired
    private CryptaClientStub cryptaClient;


    private ClientInfo clientInfo;
    private GdClientInfo gdClientInfo;

    private RetargetingCondition retargetingCondition;
    private RetargetingInfo retargetingInfo;

    private static final String GOAL_NAME = "cat";
    private static final String BUNDLE_NAME = "ru.yandex.direct.core.entity.crypta.CryptaGoalTranslations";

    private final List<Goal> notHostGoals = List.of(
            (Goal) defaultGoalByType(GoalType.INTERESTS).withName(GOAL_NAME),
            defaultGoalByType(GoalType.BEHAVIORS),
            defaultGoalByType(GoalType.BEHAVIORS),
            defaultGoalByType(GoalType.INTERESTS)
    );

    private final List<Goal> hostGoals = List.of(
            defaultGoalByType(GoalType.HOST),
            defaultGoalByType(GoalType.HOST),
            defaultGoalByType(GoalType.HOST),
            defaultGoalByType(GoalType.HOST)
    );

    private List<Goal> retargetingGoals;

    @Before
    public void initTestData() {
        var userInfo = steps.userSteps().createDefaultUser();
        clientInfo = userInfo.getClientInfo();
        var operator = userInfo.getUser();
        gdClientInfo = ContextHelper.toGdClientInfo(operator);
        var context = ContextHelper.buildContext(operator)
                .withFetchedFieldsReslover(null);
        gridContextProvider.setGridContext(context);

        CampaignInfo campaignInfo = steps.campaignSteps().createActiveCampaign(clientInfo, CampaignsPlatform.SEARCH);
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);

        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FeatureName.NEW_CUSTOM_AUDIENCE_ENABLED, true);

        retargetingGoals = Stream.of(
                notHostGoals,
                hostGoals
        ).flatMap(List::stream).collect(Collectors.toList());

        notHostGoals.forEach(sg -> steps.cryptaGoalsSteps().addGoals(sg)); // ???
        cryptaClient.addHosts(hostGoals.stream().map(Goal::getId).collect(Collectors.toList()));

        retargetingCondition = createInterestRetargetingCondition(CAMPAIGN_GOALS_SHORTCUT_NAME);

        var retargetingConditionInfo = steps.retConditionSteps().createRetCondition(
                retargetingCondition, clientInfo);

        var retargeting = defaultRetargeting(campaignInfo.getCampaignId(), adGroupInfo.getAdGroupId(),
                retargetingConditionInfo.getRetConditionId());

        retargetingInfo = steps.retargetingSteps().createRetargeting(
                retargeting, adGroupInfo, retargetingConditionInfo);

    }

    private RetargetingCondition createInterestRetargetingCondition(String interestName) {
        Rule rule = new Rule()
                .withGoals(retargetingGoals)
                .withType(RuleType.OR);
        return (RetargetingCondition) defaultRetCondition(clientInfo.getClientId())
                .withType(ConditionType.interests)
                .withName(interestName)
                .withRules(singletonList(rule))
                .withAvailable(true);
    }

    @Test
    public void getRetargetingConditionsRowset_ExpectChangesInGoalsWithHostType() {
        GdRetargetingConditionFilter filter = new GdRetargetingConditionFilter()
                .withRetargetingConditionIdIn(Set.of(retargetingCondition.getId()))
                .withRetargetingConditionTypeIn(Set.of(GdRetargetingConditionType.INTERESTS))
                .withInterest(false);

        doAnswer(getAnswerForConditions(List.of(retargetingInfo)))
                .when(gridYtSupport).selectRows(eq(clientInfo.getShard()), any(Select.class));

        var retargetingConditions = retargetingDataService
                .getRetargetingConditionsRowset(gdClientInfo, filter);

        var goalsWithHostType =
                retargetingConditions.stream()
                        .map(GdRetargetingCondition::getConditionRules)
                        .flatMap(List::stream)
                        .map(GdRetargetingConditionRuleItem::getGoals)
                        .flatMap(List::stream)
                        .filter(g -> g.getType() == GdGoalType.HOST)
                        .collect(Collectors.toList());

        assertFalse(goalsWithHostType.isEmpty());
        assertTrue(
                goalsWithHostType.stream()
                        .map(GdGoalTruncated::getName)
                        .allMatch(s -> s.startsWith("yandex")));
    }

    @Test
    public void getRetargetingConditionsRowset_ExpectTranslationsInGoalsWithNotHostType() {
        GdRetargetingConditionFilter filter = new GdRetargetingConditionFilter()
                .withRetargetingConditionIdIn(Set.of(retargetingCondition.getId()))
                .withRetargetingConditionTypeIn(Set.of(GdRetargetingConditionType.INTERESTS))
                .withInterest(false);

        doAnswer(getAnswerForConditions(List.of(retargetingInfo)))
                .when(gridYtSupport).selectRows(eq(clientInfo.getShard()), any(Select.class));

        var retargetingConditions = retargetingDataService
                .getRetargetingConditionsRowset(gdClientInfo, filter);

        var goalsWithNotHostType =
                retargetingConditions.stream()
                        .map(GdRetargetingCondition::getConditionRules)
                        .flatMap(List::stream)
                        .map(GdRetargetingConditionRuleItem::getGoals)
                        .flatMap(List::stream)
                        .filter(g -> g.getType() != GdGoalType.HOST)
                        .collect(Collectors.toList());

        assertFalse(goalsWithNotHostType.isEmpty());
        assertTrue(
                goalsWithNotHostType.stream()
                        .map(GdGoalTruncated::getName)
                        .allMatch(s -> s.equals(translationService.translate(BUNDLE_NAME, s))));
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
                    var conditionJson = "[{\"goals\":[" +
                            condition.collectGoals().stream().map(goal ->
                                    "{" +
                                            "\"goal_id\":" + goal.getId() +
                                            ",\"time\":" + goal.getTime() +
                                            ",\"name\":" + "\"" + goal.getName() + "\"" +
                                    "}").collect(Collectors.joining(", "))
                            + "]," + "\"type\":\"" + ruleType + "\"}]";
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
