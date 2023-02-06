package ru.yandex.direct.grid.processing.service.showcondition;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.retargeting.container.RetargetingConditionValidationData;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.model.Rule;
import ru.yandex.direct.core.entity.retargeting.model.RuleType;
import ru.yandex.direct.core.entity.retargeting.model.TargetingCategory;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingConditionRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.repository.TestTargetingCategoriesRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.common.GdResult;
import ru.yandex.direct.grid.processing.model.retargeting.GdGoalMinimal;
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingConditionRuleItemReq;
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingConditionRuleType;
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingConditionType;
import ru.yandex.direct.grid.processing.model.retargeting.mutation.GdCreateRetargetingConditionItem;
import ru.yandex.direct.grid.processing.model.retargeting.mutation.GdCreateRetargetingConditionPayloadItem;
import ru.yandex.direct.grid.processing.model.retargeting.mutation.GdCreateRetargetingConditions;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.validation.result.Path;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.retargeting.Constants.GOALS_PER_RULE_FOR_INTEREST_TARGETING;
import static ru.yandex.direct.core.entity.retargeting.Constants.INTEREST_LINK_TIME_VALUE;
import static ru.yandex.direct.core.entity.retargeting.Constants.RULES_PER_CONDITION_FOR_INTEREST_TARGETING;
import static ru.yandex.direct.core.entity.retargeting.model.ConditionType.metrika_goals;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingDefects.inconsistentStateTargetingCategoryUnavailable;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingDefects.retargetingConditionAlreadyExists;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingConditionType.AB_SEGMENTS;
import static ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingConditionType.METRIKA_GOALS;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.convertValue;
import static ru.yandex.direct.grid.processing.util.GraphQlTestExecutor.validateResponseSuccessful;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationHelper.toGdValidationResult;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.validation.defect.CollectionDefects.collectionSizeIsValid;
import static ru.yandex.direct.validation.defect.CommonDefects.inconsistentState;
import static ru.yandex.direct.validation.defect.CommonDefects.objectNotFound;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

/**
 * Тест на сервис, проверяем метод createRetargetingConditions.
 * Проверяем правильность создания условия нацеливания по интересам (для таргетинга по интересам)
 */
@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RetargetingMutationsGraphQlServiceCreateInterestTest {

    private static final long TARGETING_CATEGORY_IMPORT_ID_1 = 21672155L;
    private static final long TARGETING_CATEGORY_IMPORT_ID_2 = 21672160L;
    private static final long TARGETING_CATEGORY_IMPORT_ID_NOT_AVAILABLE = 21672170L;
    private static final long TARGETING_CATEGORY_IMPORT_ID_THAT_CLIENT_EXIST = 21672180L;

    private static final String MUTATION_TEMPLATE = ""
            + "mutation {\n"
            + "  %s (input: %s) {\n"
            + "    validationResult {\n"
            + "      errors {\n"
            + "        code\n"
            + "        path\n"
            + "        params\n"
            + "      }\n"
            + "    }\n"
            + "    rowset {"
            + "      retargetingConditionId"
            + "    }\n"
            + "  }\n"
            + "}";
    private static final GraphQlTestExecutor.TemplateMutation<GdCreateRetargetingConditions, GdResult> CREATE_RETARGETING_CONDITION =
            new GraphQlTestExecutor.TemplateMutation<>("createRetargetingConditions", MUTATION_TEMPLATE,
                    GdCreateRetargetingConditions.class, GdResult.class);

    @Autowired
    private GraphQlTestExecutor processor;
    @Autowired
    private Steps steps;
    @Autowired
    private TestTargetingCategoriesRepository targetingCategoriesRepository;
    @Autowired
    private RetargetingConditionRepository retargetingConditionRepository;

    private User operator;
    private int shard;
    private ClientId clientId;

    @Before
    public void before() {
        UserInfo userInfo = steps.userSteps().createUser(generateNewUser());
        ClientInfo clientInfo = userInfo.getClientInfo();
        shard = userInfo.getShard();
        operator = userInfo.getUser();
        clientId = clientInfo.getClientId();

        var category1 = new TargetingCategory(51L, null, "Утилиты", "TOOLS",
                BigInteger.valueOf(TARGETING_CATEGORY_IMPORT_ID_1), true);
        var category2 = new TargetingCategory(52L, null, "Покупки", "SHOPPING",
                BigInteger.valueOf(TARGETING_CATEGORY_IMPORT_ID_2), true);
        var categoryParent = new TargetingCategory(53L, null, "Погода", "WEATHER",
                BigInteger.valueOf(TARGETING_CATEGORY_IMPORT_ID_NOT_AVAILABLE), false);
        var category3 = new TargetingCategory(854L, 53L, "Транспорт", "TRANSPORTATION",
                BigInteger.valueOf(TARGETING_CATEGORY_IMPORT_ID_THAT_CLIENT_EXIST), true);
        targetingCategoriesRepository.addTargetingCategory(category1);
        targetingCategoriesRepository.addTargetingCategory(category2);
        targetingCategoriesRepository.addTargetingCategory(categoryParent);
        targetingCategoriesRepository.addTargetingCategory(category3);

        TestAuthHelper.setDirectAuthentication(operator);
    }

    /**
     * Проверяем успешное создание интереса через GraphQl запрос
     */
    @Test
    public void checkSuccessfulCreationInterest() {
        var sendGoal = createGoal(TARGETING_CATEGORY_IMPORT_ID_1);
        var sendRule = createRule(singletonList(sendGoal));

        GdResult response = sendRequest(METRIKA_GOALS, singletonList(sendRule));
        validateResponseSuccessful(response);

        var payloadItem = convertValue(response.getRowset().get(0), GdCreateRetargetingConditionPayloadItem.class);
        Long retargetingConditionId = payloadItem.getRetargetingConditionId();

        Goal expectGoal = (Goal) new Goal()
                .withId(TARGETING_CATEGORY_IMPORT_ID_1)
                .withTime(INTEREST_LINK_TIME_VALUE);
        Rule expectRule = new Rule()
                .withType(RuleType.ALL)
                .withGoals(singletonList(expectGoal));
        var expectRetargetingCondition = (RetargetingCondition) new RetargetingCondition()
                .withType(metrika_goals)
                .withName("")
                .withDescription("")
                .withClientId(clientId.asLong())
                .withInterest(true)
                .withId(retargetingConditionId)
                .withDeleted(false)
                .withRules(singletonList(expectRule));

        var retargetingConditions =
                retargetingConditionRepository.getConditions(shard, singletonList(retargetingConditionId));
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(retargetingConditions).as("created count of interests").hasSize(1);
            soft.assertThat(retargetingConditions.get(0)).as("interest")
                    .is(matchedBy(beanDiffer(expectRetargetingCondition).useCompareStrategy(onlyExpectedFields())));
        });
    }

    /**
     * Проверяем что при успешном создании интереса было корректно заполнено поле condition_json (без лишних данных)
     */
    @Test
    public void checkThatConditionJsonIsFilledCorrectly() {
        var goal = createGoal(TARGETING_CATEGORY_IMPORT_ID_1);
        var rule = createRule(List.of(goal));

        GdResult response = sendRequest(METRIKA_GOALS, singletonList(rule));
        validateResponseSuccessful(response);

        var payloadItem = convertValue(response.getRowset().get(0), GdCreateRetargetingConditionPayloadItem.class);
        Long retCondId = payloadItem.getRetargetingConditionId();

        String expectRulesJson = "[{\"goals\":[{\"goal_id\":" + TARGETING_CATEGORY_IMPORT_ID_1
                + ",\"time\":90}],\"type\":\"all\"}]";
        var expectedValidationData = new RetargetingConditionValidationData(retCondId, "", expectRulesJson);

        List<RetargetingConditionValidationData> validationData =
                retargetingConditionRepository.getValidationData(shard, clientId);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(validationData).as("created count of interests").hasSize(1);
            soft.assertThat(validationData.get(0)).as("interest data")
                    .is(matchedBy(beanDiffer(expectedValidationData).useCompareStrategy(onlyExpectedFields())));
        });
    }

    /**
     * При отправке некорректного типа (не METRIKA_GOALS) retargeting_condition - получаем ошибку
     */
    @Test
    public void sendWithWrongType_InconsistentState() {
        var goal = createGoal(TARGETING_CATEGORY_IMPORT_ID_1);
        var rule = createRule(singletonList(goal));

        GdResult response = sendRequest(AB_SEGMENTS, singletonList(rule));

        var expectedValidationResult = toGdValidationResult(getPathToRetCondField("type"), inconsistentState())
                .withWarnings(null);
        assertThat(response.getValidationResult())
                .is(matchedBy(beanDiffer(expectedValidationResult).useCompareStrategy(onlyExpectedFields())));
    }

    /**
     * При отправке двух rules получаем ошибку
     */
    @Test
    public void sendWithTwoRules_InvalidCollectionSize() {
        var goal1 = createGoal(TARGETING_CATEGORY_IMPORT_ID_1);
        var goal2 = createGoal(TARGETING_CATEGORY_IMPORT_ID_2);
        var rule1 = createRule(singletonList(goal1));
        var rule2 = createRule(singletonList(goal2));

        GdResult response = sendRequest(METRIKA_GOALS, List.of(rule1, rule2));

        var expectedValidationResult = toGdValidationResult(getPathToRetCondField("rules"),
                collectionSizeIsValid(0, 0)).withWarnings(null);
        expectedValidationResult.getErrors().get(0).setParams(Map.of(
                "maxSize", RULES_PER_CONDITION_FOR_INTEREST_TARGETING,
                "minSize", RULES_PER_CONDITION_FOR_INTEREST_TARGETING));

        assertThat(response.getValidationResult())
                .is(matchedBy(beanDiffer(expectedValidationResult).useCompareStrategy(onlyExpectedFields())));
    }

    /**
     * При отправке некорректного типа у rule (не ALL) получаем ошибку
     */
    @Test
    public void sendWithWrongRuleType_InconsistentState() {
        var goal1 = createGoal(TARGETING_CATEGORY_IMPORT_ID_1);
        var rule = createRule(singletonList(goal1))
                .withType(GdRetargetingConditionRuleType.OR);

        GdResult response = sendRequest(METRIKA_GOALS, singletonList(rule));

        var expectedValidationResult = toGdValidationResult(getPathToRuleField("type"), inconsistentState())
                .withWarnings(null);
        assertThat(response.getValidationResult())
                .is(matchedBy(beanDiffer(expectedValidationResult).useCompareStrategy(onlyExpectedFields())));
    }

    /**
     * При отправке rule c двумя целями получаем ошибку
     */
    @Test
    public void sendRuleWithTwoGoals_InvalidCollectionSize() {
        var goal1 = createGoal(TARGETING_CATEGORY_IMPORT_ID_1);
        var goal2 = createGoal(TARGETING_CATEGORY_IMPORT_ID_2);
        var rule = createRule(List.of(goal1, goal2));

        GdResult response = sendRequest(METRIKA_GOALS, singletonList(rule));

        var expectedValidationResult = toGdValidationResult(getPathToRuleField("goals"),
                collectionSizeIsValid(0, 0)).withWarnings(null);
        expectedValidationResult.getErrors().get(0).setParams(Map.of(
                "maxSize", GOALS_PER_RULE_FOR_INTEREST_TARGETING,
                "minSize", GOALS_PER_RULE_FOR_INTEREST_TARGETING));

        assertThat(response.getValidationResult())
                .is(matchedBy(beanDiffer(expectedValidationResult).useCompareStrategy(onlyExpectedFields())));
    }

    /**
     * При отправке цели-интереса, который уже есть у клиента - получаем ошибку
     */
    @Test
    public void sendGoalThatClientExist_RetargetingConditionAlreadyExists() {
        Goal clientGoal = (Goal) new Goal()
                .withId(TARGETING_CATEGORY_IMPORT_ID_THAT_CLIENT_EXIST)
                .withTime(INTEREST_LINK_TIME_VALUE);
        Rule clientRule = new Rule()
                .withType(RuleType.ALL)
                .withGoals(singletonList(clientGoal));
        var clientRetargetingCondition = new RetargetingCondition();
        clientRetargetingCondition.withType(metrika_goals)
                .withName("")
                .withClientId(clientId.asLong())
                .withLastChangeTime(LocalDateTime.now())
                .withInterest(true)
                .withDeleted(false)
                .withRules(singletonList(clientRule));
        retargetingConditionRepository.add(shard, singletonList(clientRetargetingCondition));

        var sendGoal = createGoal(TARGETING_CATEGORY_IMPORT_ID_THAT_CLIENT_EXIST);
        var sendRule = createRule(singletonList(sendGoal));
        GdResult response = sendRequest(METRIKA_GOALS, singletonList(sendRule));

        var expectedValidationResult = toGdValidationResult(getPathToGoalField("id"),
                retargetingConditionAlreadyExists()).withWarnings(null);
        assertThat(response.getValidationResult())
                .is(matchedBy(beanDiffer(expectedValidationResult).useCompareStrategy(onlyExpectedFields())));
    }

    /**
     * При отправке цели-интереса не из targeting_categories - получаем ошибку
     */
    @Test
    public void sendGoalNotFromTargetingCategories_ObjectNotFound() {
        var goal = createGoal(555L);
        var rule = createRule(singletonList(goal));

        GdResult response = sendRequest(METRIKA_GOALS, singletonList(rule));

        var expectedValidationResult = toGdValidationResult(getPathToGoalField("id"), objectNotFound())
                .withWarnings(null);
        assertThat(response.getValidationResult())
                .is(matchedBy(beanDiffer(expectedValidationResult).useCompareStrategy(onlyExpectedFields())));
    }

    /**
     * При отправке недоступной цели-интереса (доступны только те категории, которые являются листьями
     * (не имеют дочерних категорий)) - получаем ошибку
     */
    @Test
    public void sendGoalThatNotAvailable_InconsistentStateTargetingCategory() {
        var goal = createGoal(TARGETING_CATEGORY_IMPORT_ID_NOT_AVAILABLE);
        var rule = createRule(List.of(goal));

        GdResult response = sendRequest(METRIKA_GOALS, singletonList(rule));

        var expectedValidationResult = toGdValidationResult(getPathToGoalField("id"),
                inconsistentStateTargetingCategoryUnavailable()).withWarnings(null);
        assertThat(response.getValidationResult())
                .is(matchedBy(beanDiffer(expectedValidationResult).useCompareStrategy(onlyExpectedFields())));
    }

    private Path getPathToRetCondField(String fieldName) {
        return path(field("retargetingConditions"), index(0), field(fieldName));
    }

    private Path getPathToRuleField(String fieldName) {
        return path(field("retargetingConditions"), index(0), field("rules"), index(0), field(fieldName));
    }

    @SuppressWarnings("SameParameterValue")
    private Path getPathToGoalField(String fieldName) {
        return path(field("retargetingConditions"), index(0), field("rules"), index(0),
                field("goals"), index(0), field(fieldName));
    }

    private GdGoalMinimal createGoal(Long id) {
        return new GdGoalMinimal()
                .withId(id)
                .withTime(INTEREST_LINK_TIME_VALUE);
    }

    private GdRetargetingConditionRuleItemReq createRule(List<GdGoalMinimal> goals) {
        return new GdRetargetingConditionRuleItemReq()
                .withType(GdRetargetingConditionRuleType.ALL)
                .withGoals(goals);
    }

    private GdResult sendRequest(GdRetargetingConditionType conditionType,
                                 List<GdRetargetingConditionRuleItemReq> rules) {
        var retargetingCondition = new GdCreateRetargetingConditionItem()
                .withIsInterest(true)
                .withName("")
                .withType(conditionType)
                .withConditionRules(rules);
        var input = new GdCreateRetargetingConditions()
                .withRetargetingConditions(List.of(retargetingCondition));
        return processor.doMutationAndGetPayload(CREATE_RETARGETING_CONDITION, input, operator);
    }
}
