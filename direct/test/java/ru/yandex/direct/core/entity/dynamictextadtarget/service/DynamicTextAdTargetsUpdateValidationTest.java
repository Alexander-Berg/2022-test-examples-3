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
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicTextAdTarget;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.WebpageRule;
import ru.yandex.direct.core.entity.dynamictextadtarget.utils.DynamicTextAdTargetHashUtils;
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
import static ru.yandex.direct.core.entity.dynamictextadtarget.service.validation.DynamicTextAdTargetConstants.MAX_DYNAMIC_TEXT_AD_TARGETS_IN_GROUP;
import static ru.yandex.direct.core.entity.dynamictextadtarget.service.validation.DynamicTextAdTargetDefects.invalidEmptyNameFormat;
import static ru.yandex.direct.core.testing.data.TestDynamicTextAdTargets.defaultDynamicTextAdTargetWithRandomRules;
import static ru.yandex.direct.core.validation.defects.MoneyDefects.invalidValueNotLessThan;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.validation.defect.CollectionDefects.duplicatedElement;
import static ru.yandex.direct.validation.defect.CommonDefects.inconsistentStateAlreadyExists;
import static ru.yandex.direct.validation.defect.CommonDefects.objectNotFound;
import static ru.yandex.direct.validation.defect.CommonDefects.validId;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class DynamicTextAdTargetsUpdateValidationTest {

    private static final BigDecimal NEW_PRICE = BigDecimal.valueOf(15);

    @Autowired
    private Steps steps;
    @Autowired
    private DynamicTextAdTargetService dynamicTextAdTargetService;
    @Autowired
    private ClientService clientService;

    private long operatorUid;
    private ClientId clientId;
    private DynamicTextAdTarget dynamicTextAdTarget;
    private AdGroupInfo dynamicTextAdGroup;

    @Before
    public void before() {
        dynamicTextAdGroup = steps.adGroupSteps().createActiveDynamicTextAdGroup();

        ClientInfo clientInfo = dynamicTextAdGroup.getClientInfo();
        operatorUid = clientInfo.getUid();
        clientId = clientInfo.getClientId();

        dynamicTextAdTarget = steps.dynamicTextAdTargetsSteps()
                .createDynamicTextAdTarget(dynamicTextAdGroup,
                        defaultDynamicTextAdTargetWithRandomRules(dynamicTextAdGroup))
                .getDynamicTextAdTarget();
    }

    @Test
    public void validate_success() {
        DynamicTextAdTarget newDynamicAdTarget = new DynamicTextAdTarget()
                .withId(dynamicTextAdTarget.getId())
                .withConditionName("new condition name")
                .withPrice(NEW_PRICE);

        MassResult<Long> result = updateDynamicTextAdTargets(List.of(newDynamicAdTarget));
        assertThat(result, isSuccessful(true));
    }

    @Test
    public void validate_whenAnotherClientDynamicAdTargetId() {
        AdGroupInfo anotherClientAdGroup = steps.adGroupSteps().createActiveDynamicTextAdGroup();

        Long anotherClientDynamicAdTargetId = steps.dynamicTextAdTargetsSteps()
                .createDefaultDynamicTextAdTarget(anotherClientAdGroup)
                .getDynamicTextAdTarget()
                .getId();

        DynamicTextAdTarget newDynamicAdTarget = new DynamicTextAdTarget()
                .withId(anotherClientDynamicAdTargetId)
                .withPrice(NEW_PRICE);

        MassResult<Long> result = updateDynamicTextAdTargets(List.of(newDynamicAdTarget));
        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0), field(DynamicAdTarget.ID)),
                        objectNotFound())));
    }

    @Test
    public void validate_whenDynamicFeedAdTargetId() {
        AdGroupInfo dynamicFeedAdGroup = steps.adGroupSteps()
                .createActiveDynamicFeedAdGroup(dynamicTextAdGroup.getClientInfo());

        Long dynamicFeedAdTargetId = steps.dynamicTextAdTargetsSteps()
                .createDefaultDynamicFeedAdTarget(dynamicFeedAdGroup)
                .getId();

        DynamicTextAdTarget newDynamicAdTarget = new DynamicTextAdTarget()
                .withId(dynamicFeedAdTargetId)
                .withPrice(NEW_PRICE);

        MassResult<Long> result = updateDynamicTextAdTargets(List.of(newDynamicAdTarget));
        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0), field(DynamicAdTarget.ID)),
                        objectNotFound())));
    }

    @Test
    public void validate_whenTwoDynamicAdTargets() {
        DynamicTextAdTarget newDynamicAdTarget = new DynamicTextAdTarget()
                .withId(dynamicTextAdTarget.getId())
                .withPrice(NEW_PRICE);

        DynamicTextAdTarget newDynamicAdTarget2 = new DynamicTextAdTarget()
                .withId(-1L)
                .withPrice(NEW_PRICE);

        MassResult<Long> result = updateDynamicTextAdTargets(List.of(newDynamicAdTarget, newDynamicAdTarget2));
        assumeThat(result, isSuccessful(true, false));

        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(1), field(DynamicAdTarget.ID)),
                        validId())));
    }

    @Test
    public void validate_whenEmptyName() {
        DynamicTextAdTarget newDynamicAdTarget = new DynamicTextAdTarget()
                .withId(dynamicTextAdTarget.getId())
                .withConditionName("");

        MassResult<Long> result = updateDynamicTextAdTargets(List.of(newDynamicAdTarget));
        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0), field(DynamicAdTarget.CONDITION_NAME)),
                        invalidEmptyNameFormat())));
    }

    @Test
    public void validate_whenDuplicateConditionInExisting() {
        DynamicTextAdTarget existingDynamicAdTarget = defaultDynamicTextAdTargetWithRandomRules(dynamicTextAdGroup);
        steps.dynamicTextAdTargetsSteps().createDynamicTextAdTarget(dynamicTextAdGroup, existingDynamicAdTarget);

        List<WebpageRule> condition = existingDynamicAdTarget.getCondition();
        DynamicTextAdTarget newDynamicAdTarget = new DynamicTextAdTarget()
                .withId(dynamicTextAdTarget.getId())
                .withCondition(condition)
                .withConditionHash(DynamicTextAdTargetHashUtils.getHash(condition))
                .withConditionUniqHash(DynamicTextAdTargetHashUtils.getUniqHash(condition));

        MassResult<Long> result = updateDynamicTextAdTargets(List.of(newDynamicAdTarget));
        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0), field(DynamicTextAdTarget.CONDITION)),
                        inconsistentStateAlreadyExists())));
    }

    @Test
    public void validate_whenDuplicateConditionInUpdating() {
        DynamicTextAdTarget dynamicTextAdTarget2 = defaultDynamicTextAdTargetWithRandomRules(dynamicTextAdGroup);
        steps.dynamicTextAdTargetsSteps().createDynamicTextAdTarget(dynamicTextAdGroup, dynamicTextAdTarget2);

        List<WebpageRule> condition = defaultDynamicTextAdTargetWithRandomRules(dynamicTextAdGroup)
                .getCondition();
        DynamicTextAdTarget newDynamicAdTarget = new DynamicTextAdTarget()
                .withId(dynamicTextAdTarget.getId())
                .withCondition(condition)
                .withConditionHash(DynamicTextAdTargetHashUtils.getHash(condition))
                .withConditionUniqHash(DynamicTextAdTargetHashUtils.getUniqHash(condition));

        DynamicTextAdTarget newDynamicAdTarget2 = new DynamicTextAdTarget()
                .withId(dynamicTextAdTarget2.getId())
                .withCondition(condition)
                .withConditionHash(DynamicTextAdTargetHashUtils.getHash(condition))
                .withConditionUniqHash(DynamicTextAdTargetHashUtils.getUniqHash(condition));

        MassResult<Long> result = updateDynamicTextAdTargets(List.of(newDynamicAdTarget, newDynamicAdTarget2));
        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0), field(DynamicTextAdTarget.CONDITION)),
                        duplicatedElement())));
    }

    @Test
    public void validate_whenMaxCountInAdGroup() {
        // одно условие уже есть в группе, добавляем еще MAX_DYNAMIC_TEXT_AD_TARGETS_IN_GROUP - 1
        for (int i = 0; i < MAX_DYNAMIC_TEXT_AD_TARGETS_IN_GROUP - 1; i++) {
            steps.dynamicTextAdTargetsSteps().createDynamicTextAdTarget(dynamicTextAdGroup,
                    defaultDynamicTextAdTargetWithRandomRules(dynamicTextAdGroup));
        }
        DynamicTextAdTarget newDynamicAdTarget = new DynamicTextAdTarget()
                .withId(dynamicTextAdTarget.getId())
                .withPrice(NEW_PRICE);

        MassResult<Long> result = updateDynamicTextAdTargets(List.of(newDynamicAdTarget));
        assertThat(result, isSuccessful(true));
    }

    @Test
    public void validate_whenOldPriceIsZero_andPriceDoesNotChange() {
        // проверяем что нет ошибки валидации когда price = 0 в БД и пользователь не меняет поле
        // (например для этой стратегии можно не указывать price)
        DynamicTextAdTarget dynamicAdTargetWithZeroPrice = defaultDynamicTextAdTargetWithRandomRules(dynamicTextAdGroup)
                .withPrice(BigDecimal.ZERO);
        steps.dynamicTextAdTargetsSteps().createDynamicTextAdTarget(dynamicTextAdGroup, dynamicAdTargetWithZeroPrice);

        DynamicTextAdTarget newDynamicAdTarget = new DynamicTextAdTarget()
                .withId(dynamicAdTargetWithZeroPrice.getId())
                .withPriceContext(NEW_PRICE);

        MassResult<Long> result = updateDynamicTextAdTargets(List.of(newDynamicAdTarget));
        assertThat(result, isSuccessful(true));
    }

    @Test
    public void validate_whenNewPriceIsZero() {
        DynamicTextAdTarget newDynamicAdTarget = new DynamicTextAdTarget()
                .withId(dynamicTextAdTarget.getId())
                .withPrice(BigDecimal.ZERO);

        MassResult<Long> result = updateDynamicTextAdTargets(List.of(newDynamicAdTarget));

        Currency currency = clientService.getWorkCurrency(clientId);
        Money minPrice = Money.valueOf(currency.getMinPrice(), currency.getCode());

        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0), field(DynamicAdTarget.PRICE)),
                        invalidValueNotLessThan(minPrice))));
    }

    @Test
    public void validate_whenNewPriceContextIsZero() {
        DynamicTextAdTarget newDynamicAdTarget = new DynamicTextAdTarget()
                .withId(dynamicTextAdTarget.getId())
                .withPriceContext(BigDecimal.ZERO);

        MassResult<Long> result = updateDynamicTextAdTargets(List.of(newDynamicAdTarget));

        Currency currency = clientService.getWorkCurrency(clientId);
        Money minPrice = Money.valueOf(currency.getMinPrice(), currency.getCode());

        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0), field(DynamicAdTarget.PRICE_CONTEXT)),
                        invalidValueNotLessThan(minPrice))));
    }

    @Test
    public void validate_whenOldPricesAreZero_andConditionChanged() {
        DynamicTextAdTarget dynamicAdTargetWithZeroPrice = defaultDynamicTextAdTargetWithRandomRules(dynamicTextAdGroup)
                .withPrice(BigDecimal.ZERO)
                .withPriceContext(BigDecimal.ZERO);
        steps.dynamicTextAdTargetsSteps().createDynamicTextAdTarget(dynamicTextAdGroup, dynamicAdTargetWithZeroPrice);

        // меняем condition чтобы динамическое условие пересоздавалось
        List<WebpageRule> condition = defaultDynamicTextAdTargetWithRandomRules(dynamicTextAdGroup)
                .getCondition();
        DynamicTextAdTarget newDynamicAdTarget = new DynamicTextAdTarget()
                .withId(dynamicAdTargetWithZeroPrice.getId())
                .withCondition(condition)
                .withConditionHash(DynamicTextAdTargetHashUtils.getHash(condition))
                .withConditionUniqHash(DynamicTextAdTargetHashUtils.getUniqHash(condition));

        // проверяем что не падает пересоздание динамического условия с price = null
        MassResult<Long> result = updateDynamicTextAdTargets(List.of(newDynamicAdTarget));
        assertThat(result, isSuccessful(true));
    }

    private MassResult<Long> updateDynamicTextAdTargets(List<DynamicTextAdTarget> newDynamicAdTargets) {
        List<ModelChanges<DynamicTextAdTarget>> modelChanges = mapList(newDynamicAdTargets, this::toModelChanges);
        return dynamicTextAdTargetService.updateDynamicTextAdTargets(clientId, operatorUid, modelChanges);
    }

    private ModelChanges<DynamicTextAdTarget> toModelChanges(DynamicTextAdTarget newDynamicAdTarget) {

        ModelChanges<DynamicTextAdTarget> modelChanges =
                new ModelChanges<>(newDynamicAdTarget.getId(), DynamicTextAdTarget.class);

        modelChanges.processNotNull(newDynamicAdTarget.getCondition(), DynamicTextAdTarget.CONDITION);
        modelChanges.processNotNull(newDynamicAdTarget.getConditionHash(), DynamicAdTarget.CONDITION_HASH);
        modelChanges.processNotNull(newDynamicAdTarget.getConditionUniqHash(), DynamicTextAdTarget.CONDITION_UNIQ_HASH);

        modelChanges.processNotNull(newDynamicAdTarget.getConditionName(), DynamicAdTarget.CONDITION_NAME);
        modelChanges.processNotNull(newDynamicAdTarget.getPrice(), DynamicAdTarget.PRICE);
        modelChanges.processNotNull(newDynamicAdTarget.getPriceContext(), DynamicAdTarget.PRICE_CONTEXT);
        modelChanges.processNotNull(newDynamicAdTarget.getAutobudgetPriority(), DynamicAdTarget.AUTOBUDGET_PRIORITY);

        return modelChanges;
    }
}
