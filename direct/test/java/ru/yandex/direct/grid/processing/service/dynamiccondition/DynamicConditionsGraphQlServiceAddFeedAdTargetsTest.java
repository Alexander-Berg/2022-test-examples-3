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
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
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
import ru.yandex.direct.grid.processing.model.dynamiccondition.mutation.GdAddDynamicAdTargetsPayload;
import ru.yandex.direct.grid.processing.model.dynamiccondition.mutation.GdAddDynamicFeedAdTargets;
import ru.yandex.direct.grid.processing.model.dynamiccondition.mutation.GdAddDynamicFeedAdTargetsItem;
import ru.yandex.direct.grid.processing.model.dynamiccondition.mutation.GdAddDynamicFeedCondition;
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
import static ru.yandex.direct.grid.processing.util.GraphQlTestExecutor.validateResponseSuccessful;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;


@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class DynamicConditionsGraphQlServiceAddFeedAdTargetsTest {

    private static final String CONDITION_NAME = "test name";
    private static final BigDecimal PRICE = BigDecimal.valueOf(10.5);
    private static final BigDecimal PRICE_CONTEXT = BigDecimal.valueOf(15);

    private static final String MUTATION_NAME = "addDynamicFeedAdTargets";
    private static final String QUERY_TEMPLATE = ""
            + "mutation {\n"
            + "  %s (input: %s) {\n"
            + "    addedItems {\n"
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
    private static final GraphQlTestExecutor.TemplateMutation<GdAddDynamicFeedAdTargets,
            GdAddDynamicAdTargetsPayload> MUTATION =
            new GraphQlTestExecutor.TemplateMutation<>(MUTATION_NAME, QUERY_TEMPLATE,
                    GdAddDynamicFeedAdTargets.class, GdAddDynamicAdTargetsPayload.class);

    @Autowired
    private GraphQlTestExecutor graphQlTestExecutor;
    @Autowired
    private Steps steps;
    @Autowired
    private DynamicTextAdTargetRepository dynamicTextAdTargetRepository;


    private User operator;
    private int shard;
    private Long adGroupId;
    private AdGroupInfo adGroupInfo;

    @Before
    public void before() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();
        operator = UserHelper.getUser(clientInfo.getClient());
        TestAuthHelper.setDirectAuthentication(operator);

        adGroupInfo = steps.adGroupSteps().createActiveDynamicFeedAdGroup(clientInfo);
        adGroupId = adGroupInfo.getAdGroupId();
    }

    @Test
    public void addDynamicFeedAdTargets_success() {
        GdAddDynamicFeedAdTargetsItem gdDynamicAdTarget = new GdAddDynamicFeedAdTargetsItem()
                .withAdGroupId(adGroupId)
                .withName(CONDITION_NAME)
                .withPrice(PRICE)
                .withPriceContext(PRICE_CONTEXT)
                .withAutobudgetPriority(GdShowConditionAutobudgetPriority.LOW)
                .withConditions(singletonList(
                        new GdAddDynamicFeedCondition()
                                .withField("categoryId")
                                .withOperator(GdDynamicFeedConditionOperator.EQUALS_ANY)
                                .withStringValue("[\"1\"]")
                ));

        GdAddDynamicAdTargetsPayload payload = addDynamicAdTarget(gdDynamicAdTarget);
        validateResponseSuccessful(payload);
        Long dynamicConditionId = payload.getAddedItems().get(0).getDynamicConditionId();
        Long id = payload.getAddedItems().get(0).getId();

        DynamicFeedRule<List<Long>> expectedDynamicFeedRule =
                new DynamicFeedRule<>("categoryId", Operator.EQUALS, "[\"1\"]");
        expectedDynamicFeedRule.setParsedValue(List.of(1L));

        DynamicFeedAdTarget expectedDynamicFeedAdTarget = new DynamicFeedAdTarget()
                .withId(id)
                .withDynamicConditionId(dynamicConditionId)
                .withAdGroupId(adGroupId)
                .withCampaignId(adGroupInfo.getCampaignId())
                .withConditionName(CONDITION_NAME)
                .withPrice(PRICE)
                .withPriceContext(PRICE_CONTEXT)
                .withAutobudgetPriority(1)
                .withTab(DynamicAdTargetTab.CONDITION)
                .withIsSuspended(false)
                .withCondition(singletonList(expectedDynamicFeedRule));

        checkDynamicAdTarget(dynamicConditionId, adGroupInfo.getClientId(), expectedDynamicFeedAdTarget);
    }

    @Test
    public void addDynamicFeedAdTargets_whenEmptyConditions() {
        GdAddDynamicFeedAdTargetsItem gdDynamicAdTarget = new GdAddDynamicFeedAdTargetsItem()
                .withAdGroupId(adGroupId)
                .withName(CONDITION_NAME)
                .withTab(GdDynamicAdTargetTab.ALL_PRODUCTS)
                .withConditions(List.of());

        GdAddDynamicAdTargetsPayload payload = addDynamicAdTarget(gdDynamicAdTarget);
        validateResponseSuccessful(payload);
        Long dynamicConditionId = payload.getAddedItems().get(0).getDynamicConditionId();

        DynamicFeedAdTarget expectedDynamicFeedAdTarget = new DynamicFeedAdTarget()
                .withAdGroupId(adGroupId)
                .withConditionName(CONDITION_NAME)
                .withTab(DynamicAdTargetTab.ALL_PRODUCTS)
                .withCondition(List.of());

        checkDynamicAdTarget(dynamicConditionId, adGroupInfo.getClientId(), expectedDynamicFeedAdTarget);
    }

    @Test
    public void addDynamicFeedAdTargets_validationError() {
        GdAddDynamicFeedAdTargetsItem gdDynamicAdTarget = new GdAddDynamicFeedAdTargetsItem()
                .withAdGroupId(adGroupId)
                .withName(CONDITION_NAME)
                .withConditions(singletonList(
                        new GdAddDynamicFeedCondition()
                                .withField("categoryId")
                                .withOperator(GdDynamicFeedConditionOperator.EQUALS_ANY)
                                .withStringValue("[-1]")
                ));

        GdAddDynamicAdTargetsPayload payload = addDynamicAdTarget(gdDynamicAdTarget);

        GdAddDynamicAdTargetsPayload expectedPayload = new GdAddDynamicAdTargetsPayload()
                .withAddedItems(singletonList(null))
                .withValidationResult(new GdValidationResult()
                        .withErrors(singletonList(new GdDefect()
                                .withCode("NumberDefectIds.MUST_BE_GREATER_THAN_OR_EQUAL_TO_MIN")
                                .withPath("addItems[0].conditions[0].stringValue")
                                .withParams(Map.of("min", 0))))
                        .withWarnings(emptyList()));

        assertThat(payload).is(matchedBy(beanDiffer(expectedPayload)));
    }

    private GdAddDynamicAdTargetsPayload addDynamicAdTarget(GdAddDynamicFeedAdTargetsItem gdDynamicAdTarget) {
        GdAddDynamicFeedAdTargets input = new GdAddDynamicFeedAdTargets()
                .withAddItems(singletonList(gdDynamicAdTarget));
        return graphQlTestExecutor.doMutationAndGetPayload(MUTATION, input, operator);
    }

    private void checkDynamicAdTarget(Long dynamicConditionId, ClientId clientId, DynamicFeedAdTarget expected) {
        DynamicFeedAdTarget actual = dynamicTextAdTargetRepository
                .getDynamicFeedAdTargets(shard, clientId, singletonList(dynamicConditionId))
                .get(0);

        CompareStrategy compareStrategy = DefaultCompareStrategies.onlyExpectedFields()
                .forFields(newPath("price"), newPath("priceContext")).useDiffer(new BigDecimalDiffer());

        assertThat(actual)
                .is(matchedBy(beanDiffer(expected)
                        .useCompareStrategy(compareStrategy)));
    }
}
