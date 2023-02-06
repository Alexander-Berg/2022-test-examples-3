package ru.yandex.direct.core.entity.dynamictextadtarget.service;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicAdTarget;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicFeedAdTarget;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicFeedRule;
import ru.yandex.direct.core.entity.performancefilter.model.Operator;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.Currency;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.result.MassResult;

import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.dynamictextadtarget.service.validation.DynamicTextAdTargetDefects.invalidEmptyNameFormat;
import static ru.yandex.direct.core.entity.dynamictextadtarget.utils.DynamicTextAdTargetHashUtils.getHashForDynamicFeedRules;
import static ru.yandex.direct.core.testing.data.TestDynamicTextAdTargets.dynamicFeedAdTargetWithRandomRules;
import static ru.yandex.direct.core.validation.defects.MoneyDefects.invalidValueNotLessThan;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.validation.defect.CollectionDefects.duplicatedObject;
import static ru.yandex.direct.validation.defect.CommonDefects.invalidFormat;
import static ru.yandex.direct.validation.defect.CommonDefects.objectNotFound;
import static ru.yandex.direct.validation.defect.CommonDefects.validId;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class DynamicFeedAdTargetsUpdateValidationTest {

    private static final BigDecimal NEW_PRICE = BigDecimal.valueOf(15);

    @Autowired
    private Steps steps;
    @Autowired
    private DynamicTextAdTargetService dynamicTextAdTargetService;
    @Autowired
    private ClientService clientService;

    private long operatorUid;
    private ClientId clientId;
    private DynamicFeedAdTarget dynamicFeedAdTarget;
    private AdGroupInfo dynamicFeedAdGroup;

    @Before
    public void before() {
        dynamicFeedAdGroup = steps.adGroupSteps().createActiveDynamicFeedAdGroup();

        ClientInfo clientInfo = dynamicFeedAdGroup.getClientInfo();
        operatorUid = clientInfo.getUid();
        clientId = clientInfo.getClientId();

        dynamicFeedAdTarget = steps.dynamicTextAdTargetsSteps()
                .createDefaultDynamicFeedAdTarget(dynamicFeedAdGroup);
    }

    @Test
    public void validate_success() {
        DynamicFeedAdTarget newDynamicAdTarget = new DynamicFeedAdTarget()
                .withId(dynamicFeedAdTarget.getId())
                .withConditionName("new condition name")
                .withPrice(NEW_PRICE);

        MassResult<Long> result = updateDynamicFeedAdTargets(List.of(newDynamicAdTarget));
        assertThat(result, isSuccessful(true));
    }

    @Test
    public void validate_whenDynamicAdTargetIdIsDeleted() {
        dynamicTextAdTargetService.deleteDynamicAdTargets(operatorUid, clientId,
                List.of(dynamicFeedAdTarget.getDynamicConditionId()));

        DynamicFeedAdTarget newDynamicAdTarget = new DynamicFeedAdTarget()
                .withId(dynamicFeedAdTarget.getId())
                .withPrice(NEW_PRICE);

        MassResult<Long> result = updateDynamicFeedAdTargets(List.of(newDynamicAdTarget));
        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0), field(DynamicAdTarget.ID)),
                        objectNotFound())));
    }

    @Test
    public void validate_whenDynamicTextAdTargetId() {
        AdGroupInfo dynamicTextAdGroup = steps.adGroupSteps()
                .createActiveDynamicTextAdGroup(dynamicFeedAdGroup.getClientInfo());

        Long dynamicTextAdTargetId = steps.dynamicTextAdTargetsSteps()
                .createDefaultDynamicTextAdTarget(dynamicTextAdGroup)
                .getDynamicTextAdTarget()
                .getId();

        DynamicFeedAdTarget newDynamicAdTarget = new DynamicFeedAdTarget()
                .withId(dynamicTextAdTargetId)
                .withPrice(NEW_PRICE);

        MassResult<Long> result = updateDynamicFeedAdTargets(List.of(newDynamicAdTarget));
        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0), field(DynamicAdTarget.ID)),
                        objectNotFound())));
    }

    @Test
    public void validate_whenTwoDynamicAdTargets() {
        DynamicFeedAdTarget newDynamicAdTarget = new DynamicFeedAdTarget()
                .withId(dynamicFeedAdTarget.getId())
                .withPrice(NEW_PRICE);

        DynamicFeedAdTarget newDynamicAdTarget2 = new DynamicFeedAdTarget()
                .withId(-1L)
                .withPrice(NEW_PRICE);

        MassResult<Long> result = updateDynamicFeedAdTargets(List.of(newDynamicAdTarget, newDynamicAdTarget2));
        assumeThat(result, isSuccessful(true, false));

        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(1), field(DynamicAdTarget.ID)),
                        validId())));
    }

    @Test
    public void validate_whenEmptyName() {
        DynamicFeedAdTarget newDynamicAdTarget = new DynamicFeedAdTarget()
                .withId(dynamicFeedAdTarget.getId())
                .withConditionName("");

        MassResult<Long> result = updateDynamicFeedAdTargets(List.of(newDynamicAdTarget));
        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0), field(DynamicAdTarget.CONDITION_NAME)),
                        invalidEmptyNameFormat())));
    }

    @Test
    public void validate_whenZeroPrice() {
        DynamicFeedAdTarget newDynamicAdTarget = new DynamicFeedAdTarget()
                .withId(dynamicFeedAdTarget.getId())
                .withPrice(BigDecimal.ZERO);

        MassResult<Long> result = updateDynamicFeedAdTargets(List.of(newDynamicAdTarget));

        Currency currency = clientService.getWorkCurrency(clientId);
        Money minPrice = Money.valueOf(currency.getMinPrice(), currency.getCode());

        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0), field(DynamicAdTarget.PRICE)),
                        invalidValueNotLessThan(minPrice))));
    }

    @Test
    public void validate_whenPriceContextIsLessThanMin() {
        DynamicFeedAdTarget newDynamicAdTarget = new DynamicFeedAdTarget()
                .withId(dynamicFeedAdTarget.getId())
                .withPriceContext(BigDecimal.valueOf(0.01));

        MassResult<Long> result = updateDynamicFeedAdTargets(List.of(newDynamicAdTarget));

        Currency currency = clientService.getWorkCurrency(clientId);
        Money minPrice = Money.valueOf(currency.getMinPrice(), currency.getCode());

        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0), field(DynamicAdTarget.PRICE_CONTEXT)),
                        invalidValueNotLessThan(minPrice))));
    }

    @Test
    public void validate_whenInvalidCondition() {
        DynamicFeedRule<Boolean> rule = new DynamicFeedRule<>("price", Operator.RANGE, "test");
        rule.setParsedValue(null);
        List<DynamicFeedRule> condition = List.of(rule);

        DynamicFeedAdTarget newDynamicAdTarget = new DynamicFeedAdTarget()
                .withId(dynamicFeedAdTarget.getId())
                .withCondition(condition)
                .withConditionHash(getHashForDynamicFeedRules(condition));

        MassResult<Long> result = updateDynamicFeedAdTargets(List.of(newDynamicAdTarget));
        assertThat(result.getValidationResult(), hasDefectWithDefinition(validationError(
                path(index(0), field(DynamicFeedAdTarget.CONDITION), index(0), field(DynamicFeedRule.STRING_VALUE)),
                invalidFormat())));
    }

    @Test
    public void validate_whenDuplicateConditionInExisting() {
        DynamicFeedAdTarget existingDynamicAdTarget = dynamicFeedAdTargetWithRandomRules(dynamicFeedAdGroup);
        steps.dynamicTextAdTargetsSteps().createDynamicFeedAdTarget(dynamicFeedAdGroup, existingDynamicAdTarget);

        List<DynamicFeedRule> condition = existingDynamicAdTarget.getCondition();
        DynamicFeedAdTarget newDynamicAdTarget = new DynamicFeedAdTarget()
                .withId(dynamicFeedAdTarget.getId())
                .withCondition(condition)
                .withConditionHash(getHashForDynamicFeedRules(condition));

        MassResult<Long> result = updateDynamicFeedAdTargets(List.of(newDynamicAdTarget));
        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0)),
                        duplicatedObject())));
    }

    @Test
    public void validate_whenDuplicateConditionInUpdating() {
        DynamicFeedAdTarget dynamicFeedAdTarget2 = dynamicFeedAdTargetWithRandomRules(dynamicFeedAdGroup);
        steps.dynamicTextAdTargetsSteps().createDynamicFeedAdTarget(dynamicFeedAdGroup, dynamicFeedAdTarget2);

        List<DynamicFeedRule> condition = dynamicFeedAdTargetWithRandomRules(dynamicFeedAdGroup).getCondition();
        DynamicFeedAdTarget newDynamicAdTarget = new DynamicFeedAdTarget()
                .withId(dynamicFeedAdTarget.getId())
                .withCondition(condition)
                .withConditionHash(getHashForDynamicFeedRules(condition));

        DynamicFeedAdTarget newDynamicAdTarget2 = new DynamicFeedAdTarget()
                .withId(dynamicFeedAdTarget2.getId())
                .withCondition(condition)
                .withConditionHash(getHashForDynamicFeedRules(condition));

        MassResult<Long> result = updateDynamicFeedAdTargets(List.of(newDynamicAdTarget, newDynamicAdTarget2));
        assumeThat(result, isSuccessful(false, false));

        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0)),
                        duplicatedObject())));
    }

    @Test
    public void validate_whenTwoValidDynamicAdTargets() {
        DynamicFeedAdTarget dynamicFeedAdTarget2 = dynamicFeedAdTargetWithRandomRules(dynamicFeedAdGroup);
        steps.dynamicTextAdTargetsSteps().createDynamicFeedAdTarget(dynamicFeedAdGroup, dynamicFeedAdTarget2);

        List<DynamicFeedRule> condition = dynamicFeedAdTargetWithRandomRules(dynamicFeedAdGroup).getCondition();
        DynamicFeedAdTarget newDynamicAdTarget = new DynamicFeedAdTarget()
                .withId(dynamicFeedAdTarget.getId())
                .withCondition(condition)
                .withConditionHash(getHashForDynamicFeedRules(condition));

        List<DynamicFeedRule> condition2 = dynamicFeedAdTargetWithRandomRules(dynamicFeedAdGroup).getCondition();
        DynamicFeedAdTarget newDynamicAdTarget2 = new DynamicFeedAdTarget()
                .withId(dynamicFeedAdTarget2.getId())
                .withCondition(condition2)
                .withConditionHash(getHashForDynamicFeedRules(condition2));

        MassResult<Long> result = updateDynamicFeedAdTargets(List.of(newDynamicAdTarget, newDynamicAdTarget2));
        assertThat(result, isSuccessful(true, true));
    }

    private MassResult<Long> updateDynamicFeedAdTargets(List<DynamicFeedAdTarget> newDynamicAdTargets) {
        List<ModelChanges<DynamicFeedAdTarget>> modelChanges = mapList(newDynamicAdTargets, this::toModelChanges);
        return dynamicTextAdTargetService.updateDynamicFeedAdTargets(clientId, operatorUid, modelChanges);
    }

    private ModelChanges<DynamicFeedAdTarget> toModelChanges(DynamicFeedAdTarget newDynamicAdTarget) {

        ModelChanges<DynamicFeedAdTarget> modelChanges =
                new ModelChanges<>(newDynamicAdTarget.getId(), DynamicFeedAdTarget.class);

        modelChanges.processNotNull(newDynamicAdTarget.getCondition(), DynamicFeedAdTarget.CONDITION);
        modelChanges.processNotNull(newDynamicAdTarget.getConditionHash(), DynamicAdTarget.CONDITION_HASH);
        modelChanges.processNotNull(newDynamicAdTarget.getTab(), DynamicAdTarget.TAB);

        modelChanges.processNotNull(newDynamicAdTarget.getConditionName(), DynamicAdTarget.CONDITION_NAME);
        modelChanges.processNotNull(newDynamicAdTarget.getPrice(), DynamicAdTarget.PRICE);
        modelChanges.processNotNull(newDynamicAdTarget.getPriceContext(), DynamicAdTarget.PRICE_CONTEXT);
        modelChanges.processNotNull(newDynamicAdTarget.getAutobudgetPriority(), DynamicAdTarget.AUTOBUDGET_PRIORITY);

        return modelChanges;
    }
}
