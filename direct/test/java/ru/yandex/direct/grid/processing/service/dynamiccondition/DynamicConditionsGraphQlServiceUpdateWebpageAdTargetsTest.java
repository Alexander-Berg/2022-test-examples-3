package ru.yandex.direct.grid.processing.service.dynamiccondition;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicTextAdTarget;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.WebpageRule;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.WebpageRuleKind;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.WebpageRuleType;
import ru.yandex.direct.core.entity.dynamictextadtarget.repository.DynamicTextAdTargetRepository;
import ru.yandex.direct.core.entity.dynamictextadtarget.service.validation.DynamicTextAdTargetConstants;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.api.GdDefect;
import ru.yandex.direct.grid.processing.model.api.GdValidationResult;
import ru.yandex.direct.grid.processing.model.dynamiccondition.GdDynamicWebpageConditionOperand;
import ru.yandex.direct.grid.processing.model.dynamiccondition.GdDynamicWebpageConditionOperator;
import ru.yandex.direct.grid.processing.model.dynamiccondition.mutation.GdAddDynamicWebpageCondition;
import ru.yandex.direct.grid.processing.model.dynamiccondition.mutation.GdUpdateDynamicAdTargetsPayload;
import ru.yandex.direct.grid.processing.model.dynamiccondition.mutation.GdUpdateDynamicWebpageAdTargets;
import ru.yandex.direct.grid.processing.model.dynamiccondition.mutation.GdUpdateDynamicWebpageAdTargetsItem;
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
import static ru.yandex.direct.core.testing.data.TestDynamicTextAdTargets.defaultDynamicTextAdTargetWithRandomRules;
import static ru.yandex.direct.grid.processing.util.GraphQlTestExecutor.validateResponseSuccessful;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;


@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class DynamicConditionsGraphQlServiceUpdateWebpageAdTargetsTest {

    private static final String CONDITION_NAME = "test name";
    private static final BigDecimal PRICE = BigDecimal.valueOf(10.5);
    private static final BigDecimal PRICE_CONTEXT = BigDecimal.valueOf(15);
    private static final List<String> WEBPAGE_RULE_VALUE = singletonList("test");

    private static final String MUTATION_NAME = "updateDynamicWebpageAdTargets";
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
    private static final GraphQlTestExecutor.TemplateMutation<GdUpdateDynamicWebpageAdTargets,
            GdUpdateDynamicAdTargetsPayload> MUTATION =
            new GraphQlTestExecutor.TemplateMutation<>(MUTATION_NAME, QUERY_TEMPLATE,
                    GdUpdateDynamicWebpageAdTargets.class, GdUpdateDynamicAdTargetsPayload.class);

    @Autowired
    private GraphQlTestExecutor graphQlTestExecutor;
    @Autowired
    private Steps steps;
    @Autowired
    private DynamicTextAdTargetRepository dynamicTextAdTargetRepository;

    private User operator;
    private ClientId clientId;
    private int shard;
    private DynamicTextAdTarget dynamicTextAdTarget;

    @Before
    public void before() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();
        shard = clientInfo.getShard();

        operator = UserHelper.getUser(clientInfo.getClient());
        TestAuthHelper.setDirectAuthentication(operator);

        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveDynamicTextAdGroup(clientInfo);

        dynamicTextAdTarget = defaultDynamicTextAdTargetWithRandomRules(adGroupInfo)
                .withAutobudgetPriority(null);
        steps.dynamicTextAdTargetsSteps().createDynamicTextAdTarget(adGroupInfo, dynamicTextAdTarget);
    }

    @Test
    public void updateDynamicAdTarget_success() {
        GdUpdateDynamicWebpageAdTargetsItem gdUpdateItem = new GdUpdateDynamicWebpageAdTargetsItem()
                .withId(dynamicTextAdTarget.getId())
                .withName(CONDITION_NAME)
                .withPrice(PRICE)
                .withPriceContext(PRICE_CONTEXT)
                .withAutobudgetPriority(GdShowConditionAutobudgetPriority.LOW)
                .withIsSuspended(true)
                .withConditions(singletonList(
                        new GdAddDynamicWebpageCondition()
                                .withOperand(GdDynamicWebpageConditionOperand.PAGE_CONTENT)
                                .withOperator(GdDynamicWebpageConditionOperator.CONTAINS_ANY)
                                .withArguments(WEBPAGE_RULE_VALUE)
                ));

        GdUpdateDynamicAdTargetsPayload payload = updateDynamicAdTarget(gdUpdateItem);
        validateResponseSuccessful(payload);
        Long dynamicConditionId = payload.getUpdatedItems().get(0).getDynamicConditionId();
        Long id = payload.getUpdatedItems().get(0).getId();

        DynamicTextAdTarget expectedDynamicAdTarget = new DynamicTextAdTarget()
                .withId(id)
                .withDynamicConditionId(dynamicConditionId)
                .withAdGroupId(dynamicTextAdTarget.getAdGroupId())
                .withConditionName(CONDITION_NAME)
                .withPrice(PRICE)
                .withPriceContext(PRICE_CONTEXT)
                .withAutobudgetPriority(1)
                .withIsSuspended(true)
                .withCondition(singletonList(
                        new WebpageRule()
                                .withType(WebpageRuleType.CONTENT)
                                .withKind(WebpageRuleKind.EXACT)
                                .withValue(WEBPAGE_RULE_VALUE)));

        checkDynamicAdTarget(dynamicTextAdTarget.getId(), expectedDynamicAdTarget);
    }

    @Test
    public void updateDynamicAdTarget_whenEmptyConditions() {
        GdUpdateDynamicWebpageAdTargetsItem gdUpdateItem = new GdUpdateDynamicWebpageAdTargetsItem()
                .withId(dynamicTextAdTarget.getId())
                .withConditions(emptyList()); // условие нацеливания с типом «Все страницы сайта»

        GdUpdateDynamicAdTargetsPayload payload = updateDynamicAdTarget(gdUpdateItem);
        validateResponseSuccessful(payload);
        Long dynamicConditionId = payload.getUpdatedItems().get(0).getDynamicConditionId();

        DynamicTextAdTarget expectedDynamicAdTarget = new DynamicTextAdTarget()
                .withDynamicConditionId(dynamicConditionId)
                .withCondition(DynamicTextAdTargetConstants.ALL_PAGE_CONDITION);

        checkDynamicAdTarget(dynamicTextAdTarget.getId(), expectedDynamicAdTarget);
    }

    @Test
    public void updateDynamicAdTarget_whenConditionDoesNotChange() {
        GdUpdateDynamicWebpageAdTargetsItem gdUpdateItem = new GdUpdateDynamicWebpageAdTargetsItem()
                .withId(dynamicTextAdTarget.getId())
                .withPrice(PRICE)
                .withConditions(null);

        GdUpdateDynamicAdTargetsPayload payload = updateDynamicAdTarget(gdUpdateItem);
        validateResponseSuccessful(payload);
        Long dynamicConditionId = payload.getUpdatedItems().get(0).getDynamicConditionId();

        DynamicTextAdTarget expectedDynamicAdTarget = new DynamicTextAdTarget()
                .withDynamicConditionId(dynamicConditionId)
                .withPrice(PRICE)
                .withCondition(dynamicTextAdTarget.getCondition())
                .withConditionHash(dynamicTextAdTarget.getConditionHash());

        checkDynamicAdTarget(dynamicTextAdTarget.getId(), expectedDynamicAdTarget);
    }

    @Test
    public void updateDynamicAdTarget_validationError_whenInvalidOperator() {
        GdUpdateDynamicWebpageAdTargetsItem gdUpdateItem = new GdUpdateDynamicWebpageAdTargetsItem()
                .withId(dynamicTextAdTarget.getId())
                .withConditions(singletonList(
                        new GdAddDynamicWebpageCondition()
                                .withOperand(GdDynamicWebpageConditionOperand.PAGE_CONTENT)
                                .withOperator(GdDynamicWebpageConditionOperator.EQUALS_ANY)
                                .withArguments(WEBPAGE_RULE_VALUE)
                ));

        GdUpdateDynamicAdTargetsPayload payload = updateDynamicAdTarget(gdUpdateItem);

        GdUpdateDynamicAdTargetsPayload expectedPayload = new GdUpdateDynamicAdTargetsPayload()
                .withUpdatedItems(singletonList(null))
                .withValidationResult(new GdValidationResult()
                        .withErrors(singletonList(new GdDefect()
                                .withCode("DynamicTextAdTargetDefectIds.Num.INVALID_FORMAT_WEBPAGE_CONDITION")
                                .withPath("updateItems[0].conditions[0].operator")))
                        .withWarnings(emptyList()));

        assertThat(payload)
                .is(matchedBy(beanDiffer(expectedPayload)
                        .useCompareStrategy(onlyExpectedFields())));
    }

    private GdUpdateDynamicAdTargetsPayload updateDynamicAdTarget(GdUpdateDynamicWebpageAdTargetsItem gdUpdateItem) {
        GdUpdateDynamicWebpageAdTargets input = new GdUpdateDynamicWebpageAdTargets()
                .withUpdateItems(singletonList(gdUpdateItem));
        return graphQlTestExecutor.doMutationAndGetPayload(MUTATION, input, operator);
    }

    private void checkDynamicAdTarget(Long id, DynamicTextAdTarget expected) {
        DynamicTextAdTarget actual = dynamicTextAdTargetRepository
                .getDynamicTextAdTargetsByIds(shard, clientId, singletonList(id))
                .get(0);

        CompareStrategy compareStrategy = onlyExpectedFields()
                .forFields(newPath("price"), newPath("priceContext")).useDiffer(new BigDecimalDiffer());

        assertThat(actual).is(matchedBy(beanDiffer(expected).useCompareStrategy(compareStrategy)));
    }
}
