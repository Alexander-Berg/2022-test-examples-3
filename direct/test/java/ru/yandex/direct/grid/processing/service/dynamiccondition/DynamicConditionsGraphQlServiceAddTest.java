package ru.yandex.direct.grid.processing.service.dynamiccondition;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicAdTargetTab;
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
import ru.yandex.direct.grid.processing.model.dynamiccondition.mutation.GdAddDynamicAdTargetsPayload;
import ru.yandex.direct.grid.processing.model.dynamiccondition.mutation.GdAddDynamicWebpageAdTargets;
import ru.yandex.direct.grid.processing.model.dynamiccondition.mutation.GdAddDynamicWebpageAdTargetsItem;
import ru.yandex.direct.grid.processing.model.dynamiccondition.mutation.GdAddDynamicWebpageCondition;
import ru.yandex.direct.grid.processing.model.showcondition.GdShowConditionAutobudgetPriority;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;
import ru.yandex.direct.multitype.entity.LimitOffset;
import ru.yandex.direct.test.utils.differ.BigDecimalDiffer;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.grid.processing.util.GraphQlTestExecutor.validateResponseSuccessful;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;


@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class DynamicConditionsGraphQlServiceAddTest {

    private static final String CONDITION_NAME = "test name";
    private static final BigDecimal PRICE = BigDecimal.valueOf(10.5);
    private static final BigDecimal PRICE_CONTEXT = BigDecimal.valueOf(15);
    private static final List<String> WEBPAGE_RULE_VALUE = singletonList("test");

    private static final String MUTATION_NAME = "addDynamicWebpageAdTargets";
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
    private static final GraphQlTestExecutor.TemplateMutation<GdAddDynamicWebpageAdTargets,
            GdAddDynamicAdTargetsPayload> MUTATION =
            new GraphQlTestExecutor.TemplateMutation<>(MUTATION_NAME, QUERY_TEMPLATE,
                    GdAddDynamicWebpageAdTargets.class, GdAddDynamicAdTargetsPayload.class);

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

        adGroupInfo = steps.adGroupSteps().createActiveDynamicTextAdGroup(clientInfo);
        adGroupId = adGroupInfo.getAdGroupId();
    }

    @Test
    public void addDynamicConditions_success() {
        GdAddDynamicWebpageAdTargetsItem gdDynamicCondition = new GdAddDynamicWebpageAdTargetsItem()
                .withAdGroupId(adGroupId)
                .withName(CONDITION_NAME)
                .withPrice(PRICE)
                .withPriceContext(PRICE_CONTEXT)
                .withAutobudgetPriority(GdShowConditionAutobudgetPriority.LOW)
                .withConditions(singletonList(
                        new GdAddDynamicWebpageCondition()
                                .withOperand(GdDynamicWebpageConditionOperand.PAGE_CONTENT)
                                .withOperator(GdDynamicWebpageConditionOperator.CONTAINS_ANY)
                                .withArguments(WEBPAGE_RULE_VALUE)
                ));

        GdAddDynamicAdTargetsPayload payload = addDynamicCondition(gdDynamicCondition);
        validateResponseSuccessful(payload);
        Long dynamicConditionId = payload.getAddedItems().get(0).getDynamicConditionId();
        Long id = payload.getAddedItems().get(0).getId();

        DynamicTextAdTarget expectedDynamicCondition = new DynamicTextAdTarget()
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
                .withCondition(singletonList(
                        new WebpageRule()
                                .withType(WebpageRuleType.CONTENT)
                                .withKind(WebpageRuleKind.EXACT)
                                .withValue(WEBPAGE_RULE_VALUE)));

        checkDynamicCondition(dynamicConditionId, adGroupInfo.getClientId(), expectedDynamicCondition);
    }

    @Test
    public void addDynamicConditions_whenEmptyConditions() {
        GdAddDynamicWebpageAdTargetsItem gdDynamicCondition = new GdAddDynamicWebpageAdTargetsItem()
                .withAdGroupId(adGroupId)
                .withName(CONDITION_NAME)
                .withConditions(emptyList()); // создается условие нацеливания с типом «Все страницы сайта»

        GdAddDynamicAdTargetsPayload payload = addDynamicCondition(gdDynamicCondition);
        validateResponseSuccessful(payload);
        Long dynamicConditionId = payload.getAddedItems().get(0).getDynamicConditionId();

        DynamicTextAdTarget expectedDynamicCondition = new DynamicTextAdTarget()
                .withAdGroupId(adGroupId)
                .withConditionName(CONDITION_NAME)
                .withCondition(DynamicTextAdTargetConstants.ALL_PAGE_CONDITION);

        checkDynamicCondition(dynamicConditionId, adGroupInfo.getClientId(), expectedDynamicCondition);
    }

    @Test
    public void addDynamicConditions_validationError_whenInvalidOperator() {
        GdAddDynamicWebpageAdTargetsItem gdDynamicCondition = new GdAddDynamicWebpageAdTargetsItem()
                .withAdGroupId(adGroupId)
                .withName(CONDITION_NAME)
                .withConditions(singletonList(
                        new GdAddDynamicWebpageCondition()
                                .withOperand(GdDynamicWebpageConditionOperand.PAGE_CONTENT)
                                .withOperator(GdDynamicWebpageConditionOperator.EQUALS_ANY)
                                .withArguments(WEBPAGE_RULE_VALUE)
                ));

        GdAddDynamicAdTargetsPayload payload = addDynamicCondition(gdDynamicCondition);

        GdAddDynamicAdTargetsPayload expectedPayload = new GdAddDynamicAdTargetsPayload()
                .withAddedItems(singletonList(null))
                .withValidationResult(new GdValidationResult()
                        .withErrors(singletonList(new GdDefect()
                                .withCode("DynamicTextAdTargetDefectIds.Num.INVALID_FORMAT_WEBPAGE_CONDITION")
                                .withPath("addItems[0].conditions[0].operator")))
                        .withWarnings(emptyList()));

        assertThat(payload)
                .is(matchedBy(beanDiffer(expectedPayload)
                        .useCompareStrategy(onlyExpectedFields())));
    }

    private GdAddDynamicAdTargetsPayload addDynamicCondition(GdAddDynamicWebpageAdTargetsItem gdDynamicCondition) {
        GdAddDynamicWebpageAdTargets input = new GdAddDynamicWebpageAdTargets()
                .withAddItems(singletonList(gdDynamicCondition));
        return graphQlTestExecutor.doMutationAndGetPayload(MUTATION, input, operator);
    }

    private void checkDynamicCondition(Long dynamicConditionId, ClientId clientId, DynamicTextAdTarget expected) {
        DynamicTextAdTarget actual = dynamicTextAdTargetRepository
                .getDynamicTextAdTargetsWithDomainType(shard, clientId,
                        singletonList(dynamicConditionId), true, LimitOffset.maxLimited())
                .get(0);

        CompareStrategy compareStrategy = DefaultCompareStrategies.onlyExpectedFields()
                .forFields(newPath("price"), newPath("priceContext")).useDiffer(new BigDecimalDiffer());

        assertThat(actual)
                .is(matchedBy(beanDiffer(expected)
                        .useCompareStrategy(compareStrategy)));
    }
}
