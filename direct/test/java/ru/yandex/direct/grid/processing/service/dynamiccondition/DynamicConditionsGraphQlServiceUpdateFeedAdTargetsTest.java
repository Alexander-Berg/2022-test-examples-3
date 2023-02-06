package ru.yandex.direct.grid.processing.service.dynamiccondition;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicAdTargetTab;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicFeedAdTarget;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicFeedRule;
import ru.yandex.direct.core.entity.dynamictextadtarget.repository.DynamicTextAdTargetRepository;
import ru.yandex.direct.core.entity.performancefilter.model.Operator;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.api.GdDefect;
import ru.yandex.direct.grid.processing.model.api.GdValidationResult;
import ru.yandex.direct.grid.processing.model.dynamiccondition.GdDynamicAdTargetTab;
import ru.yandex.direct.grid.processing.model.dynamiccondition.GdDynamicFeedConditionOperator;
import ru.yandex.direct.grid.processing.model.dynamiccondition.mutation.GdAddDynamicFeedCondition;
import ru.yandex.direct.grid.processing.model.dynamiccondition.mutation.GdUpdateDynamicAdTargetsPayload;
import ru.yandex.direct.grid.processing.model.dynamiccondition.mutation.GdUpdateDynamicFeedAdTargets;
import ru.yandex.direct.grid.processing.model.dynamiccondition.mutation.GdUpdateDynamicFeedAdTargetsItem;
import ru.yandex.direct.grid.processing.model.showcondition.GdShowConditionAutobudgetPriority;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;
import ru.yandex.direct.test.utils.differ.BigDecimalDiffer;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.testing.data.TestDynamicTextAdTargets.defaultDynamicFeedAdTarget;
import static ru.yandex.direct.grid.processing.util.GraphQlTestExecutor.validateResponseSuccessful;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;


@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class DynamicConditionsGraphQlServiceUpdateFeedAdTargetsTest {

    private static final String CONDITION_NAME = "test name";
    private static final BigDecimal PRICE = BigDecimal.valueOf(10.5);
    private static final BigDecimal PRICE_CONTEXT = BigDecimal.valueOf(15);

    private static final String MUTATION_NAME = "updateDynamicFeedAdTargets";
    private static final String QUERY_TEMPLATE = ""
            + "mutation {\n"
            + "  %s (input: %s) {\n"
            + "    updatedItems {\n"
            + "         dynamicConditionId,\n"
            + "         id\n"
            + "     }\n"
            + "    validationResult{\n"
            + "      errors{\n"
            + "        code,\n"
            + "        params,\n"
            + "        path\n"
            + "      }\n"
            + "      warnings{\n"
            + "        code,\n"
            + "        params,\n"
            + "        path\n"
            + "      }\n"
            + "    }"
            + "  }\n"
            + "}";
    private static final GraphQlTestExecutor.TemplateMutation<GdUpdateDynamicFeedAdTargets,
            GdUpdateDynamicAdTargetsPayload> MUTATION =
            new GraphQlTestExecutor.TemplateMutation<>(MUTATION_NAME, QUERY_TEMPLATE,
                    GdUpdateDynamicFeedAdTargets.class, GdUpdateDynamicAdTargetsPayload.class);

    @Autowired
    private GraphQlTestExecutor graphQlTestExecutor;
    @Autowired
    private Steps steps;
    @Autowired
    private DynamicTextAdTargetRepository dynamicTextAdTargetRepository;

    private User operator;
    private int shard;
    private ClientId clientId;
    private DynamicFeedAdTarget dynamicFeedAdTarget;

    @Before
    public void before() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();
        clientId = clientInfo.getClientId();

        operator = UserHelper.getUser(clientInfo.getClient());
        TestAuthHelper.setDirectAuthentication(operator);

        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveDynamicFeedAdGroup(clientInfo);

        dynamicFeedAdTarget = defaultDynamicFeedAdTarget(adGroupInfo).withAutobudgetPriority(null);
        steps.dynamicTextAdTargetsSteps().createDynamicFeedAdTarget(adGroupInfo, dynamicFeedAdTarget);
    }

    @Test
    public void updateDynamicFeedAdTargets_success() {
        GdUpdateDynamicFeedAdTargetsItem gdDynamicAdTarget = new GdUpdateDynamicFeedAdTargetsItem()
                .withId(dynamicFeedAdTarget.getId())
                .withName(CONDITION_NAME)
                .withPrice(PRICE)
                .withPriceContext(PRICE_CONTEXT)
                .withAutobudgetPriority(GdShowConditionAutobudgetPriority.LOW)
                .withIsSuspended(true)
                .withTab(GdDynamicAdTargetTab.CONDITION)
                .withConditions(singletonList(
                        new GdAddDynamicFeedCondition()
                                .withField("categoryId")
                                .withOperator(GdDynamicFeedConditionOperator.EQUALS_ANY)
                                .withStringValue("[\"1\"]")
                ));

        GdUpdateDynamicAdTargetsPayload payload = updateDynamicAdTarget(gdDynamicAdTarget);
        validateResponseSuccessful(payload);
        Long dynamicConditionId = payload.getUpdatedItems().get(0).getDynamicConditionId();
        Long id = payload.getUpdatedItems().get(0).getId();

        DynamicFeedRule<List<Long>> expectedDynamicFeedRule =
                new DynamicFeedRule<>("categoryId", Operator.EQUALS, "[\"1\"]");
        expectedDynamicFeedRule.setParsedValue(List.of(1L));

        DynamicFeedAdTarget expectedDynamicFeedAdTarget = new DynamicFeedAdTarget()
                .withId(id)
                .withDynamicConditionId(dynamicConditionId)
                .withAdGroupId(dynamicFeedAdTarget.getAdGroupId())
                .withConditionName(CONDITION_NAME)
                .withPrice(PRICE)
                .withPriceContext(PRICE_CONTEXT)
                .withAutobudgetPriority(1)
                .withIsSuspended(true)
                .withTab(DynamicAdTargetTab.CONDITION)
                .withCondition(singletonList(expectedDynamicFeedRule));

        checkDynamicAdTarget(dynamicFeedAdTarget.getId(), expectedDynamicFeedAdTarget);
    }

    @Test
    public void updateDynamicFeedAdTargets_whenConditionDoesNotChange() {
        GdUpdateDynamicFeedAdTargetsItem gdDynamicAdTarget = new GdUpdateDynamicFeedAdTargetsItem()
                .withId(dynamicFeedAdTarget.getId())
                .withPrice(PRICE)
                .withConditions(null);

        GdUpdateDynamicAdTargetsPayload payload = updateDynamicAdTarget(gdDynamicAdTarget);
        validateResponseSuccessful(payload);

        DynamicFeedAdTarget expectedDynamicFeedAdTarget = new DynamicFeedAdTarget()
                .withPrice(PRICE)
                .withCondition(dynamicFeedAdTarget.getCondition())
                .withConditionHash(dynamicFeedAdTarget.getConditionHash());

        checkDynamicAdTarget(dynamicFeedAdTarget.getId(), expectedDynamicFeedAdTarget);
    }

    @Test
    public void updateDynamicFeedAdTargets_validationError() {
        GdUpdateDynamicFeedAdTargetsItem gdDynamicAdTarget = new GdUpdateDynamicFeedAdTargetsItem()
                .withId(dynamicFeedAdTarget.getId())
                .withConditions(singletonList(
                        new GdAddDynamicFeedCondition()
                                .withField("categoryId")
                                .withOperator(GdDynamicFeedConditionOperator.EQUALS_ANY)
                                .withStringValue("[\"-1\"]")
                ));

        GdUpdateDynamicAdTargetsPayload payload = updateDynamicAdTarget(gdDynamicAdTarget);

        GdUpdateDynamicAdTargetsPayload expectedPayload = new GdUpdateDynamicAdTargetsPayload()
                .withUpdatedItems(singletonList(null))
                .withValidationResult(new GdValidationResult()
                        .withErrors(singletonList(new GdDefect()
                                .withCode("NumberDefectIds.MUST_BE_GREATER_THAN_OR_EQUAL_TO_MIN")
                                .withPath("updateItems[0].conditions[0].stringValue")
                                .withParams(Map.of("min", 0))))
                        .withWarnings(emptyList()));

        assertThat(payload).is(matchedBy(beanDiffer(expectedPayload)));
    }

    private GdUpdateDynamicAdTargetsPayload updateDynamicAdTarget(GdUpdateDynamicFeedAdTargetsItem gdDynamicAdTarget) {
        GdUpdateDynamicFeedAdTargets input = new GdUpdateDynamicFeedAdTargets()
                .withUpdateItems(singletonList(gdDynamicAdTarget));
        return graphQlTestExecutor.doMutationAndGetPayload(MUTATION, input, operator);
    }

    private void checkDynamicAdTarget(Long id, DynamicFeedAdTarget expected) {
        DynamicFeedAdTarget actual = dynamicTextAdTargetRepository
                .getDynamicFeedAdTargetsByIds(shard, clientId, singletonList(id))
                .get(0);

        CompareStrategy compareStrategy = onlyExpectedFields()
                .forFields(newPath("price"), newPath("priceContext")).useDiffer(new BigDecimalDiffer());

        assertThat(actual).is(matchedBy(beanDiffer(expected).useCompareStrategy(compareStrategy)));
    }
}
