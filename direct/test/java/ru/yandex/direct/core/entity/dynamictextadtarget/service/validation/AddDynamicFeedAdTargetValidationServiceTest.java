package ru.yandex.direct.core.entity.dynamictextadtarget.service.validation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicAdTarget;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicAdTargetTab;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicFeedAdTarget;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicFeedRule;
import ru.yandex.direct.core.entity.performancefilter.model.Operator;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.Currency;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.validation.defect.CollectionDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.dynamictextadtarget.service.validation.DynamicTextAdTargetConstants.MAX_DYNAMIC_TEXT_AD_TARGETS_IN_GROUP;
import static ru.yandex.direct.core.entity.dynamictextadtarget.service.validation.DynamicTextAdTargetDefects.adGroupNotFound;
import static ru.yandex.direct.core.entity.dynamictextadtarget.service.validation.DynamicTextAdTargetDefects.invalidEmptyNameFormat;
import static ru.yandex.direct.core.entity.dynamictextadtarget.service.validation.DynamicTextAdTargetDefects.maxDynamicTextAdTargetsInAdGroup;
import static ru.yandex.direct.core.entity.dynamictextadtarget.service.validation.DynamicTextAdTargetDefects.notAcceptableAdGroupType;
import static ru.yandex.direct.core.entity.performancefilter.service.PerformanceFilterDefects.unknownField;
import static ru.yandex.direct.core.testing.data.TestDynamicTextAdTargets.defaultDynamicFeedAdTarget;
import static ru.yandex.direct.core.testing.data.TestDynamicTextAdTargets.dynamicFeedAdTargetWithRandomRules;
import static ru.yandex.direct.core.validation.defects.MoneyDefects.invalidValueNotLessThan;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.inconsistentState;
import static ru.yandex.direct.validation.defect.CommonDefects.invalidFormat;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AddDynamicFeedAdTargetValidationServiceTest {

    @Autowired
    private AddDynamicFeedAdTargetValidationService addValidationService;
    @Autowired
    private ClientService clientService;
    @Autowired
    private Steps steps;

    private ClientId clientId;
    private DynamicFeedAdTarget dynamicFeedAdTarget;
    private AdGroupInfo adGroupInfo;

    @Before
    public void before() {
        adGroupInfo = steps.adGroupSteps().createActiveDynamicFeedAdGroup();
        clientId = adGroupInfo.getClientId();
        dynamicFeedAdTarget = defaultDynamicFeedAdTarget(adGroupInfo);
    }

    private ValidationResult<List<DynamicFeedAdTarget>, Defect> validate(List<DynamicFeedAdTarget> models) {
        return addValidationService.validateAdd(adGroupInfo.getShard(), adGroupInfo.getUid(), clientId, models);
    }

    @Test
    public void validate_success() {
        ValidationResult<List<DynamicFeedAdTarget>, Defect> actual = validate(List.of(dynamicFeedAdTarget));
        assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_failure_whenAnotherClientAdGroupId() {
        Long anotherClientAdGroupId = steps.adGroupSteps().createActiveTextAdGroup().getAdGroupId();

        ValidationResult<List<DynamicFeedAdTarget>, Defect> actual = validate(List.of(
                dynamicFeedAdTarget.withAdGroupId(anotherClientAdGroupId)));

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0)),
                        adGroupNotFound()))));
    }

    @Test
    public void validate_failure_whenNotDynamicAdGroup() {
        Long textAdGroupId = steps.adGroupSteps().createActiveMobileContentAdGroup(adGroupInfo.getClientInfo()).getAdGroupId();

        ValidationResult<List<DynamicFeedAdTarget>, Defect> actual = validate(List.of(
                dynamicFeedAdTarget.withAdGroupId(textAdGroupId)));

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(DynamicAdTarget.AD_GROUP_ID)),
                        inconsistentState()))));
    }

    @Test
    public void validate_failure_whenDynamicTextAdGroup() {
        Long dynamicTextAdGroupId = steps.adGroupSteps()
                .createActiveDynamicTextAdGroup(adGroupInfo.getClientInfo())
                .getAdGroupId();

        ValidationResult<List<DynamicFeedAdTarget>, Defect> actual = validate(List.of(
                dynamicFeedAdTarget.withAdGroupId(dynamicTextAdGroupId)));

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(DynamicAdTarget.AD_GROUP_ID)),
                        notAcceptableAdGroupType()))));
    }

    @Test
    public void validate_failure_whenPriceLessThanMin() {
        ValidationResult<List<DynamicFeedAdTarget>, Defect> actual = validate(List.of(
                dynamicFeedAdTarget.withPrice(BigDecimal.ZERO)));

        Currency currency = clientService.getWorkCurrency(clientId);
        Money minPrice = Money.valueOf(currency.getMinPrice(), currency.getCode());

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(DynamicAdTarget.PRICE)),
                        invalidValueNotLessThan(minPrice)))));
    }

    @Test
    public void validate_failure_whenConditionNameIsBlank() {
        ValidationResult<List<DynamicFeedAdTarget>, Defect> actual = validate(List.of(
                dynamicFeedAdTarget.withConditionName(" ")));

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(DynamicAdTarget.CONDITION_NAME)),
                        invalidEmptyNameFormat()))));
    }

    @Test
    public void validate_failure_whenFieldNotInFilterSchema() {
        DynamicFeedRule<Boolean> condition = new DynamicFeedRule<>("unknown", Operator.EQUALS, "test");
        condition.setParsedValue(null);

        ValidationResult<List<DynamicFeedAdTarget>, Defect> actual = validate(List.of(
                dynamicFeedAdTarget.withCondition(List.of(condition))));

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(validationError(
                path(index(0), field(DynamicFeedAdTarget.CONDITION), index(0), field(DynamicFeedRule.FIELD_NAME)),
                unknownField()))));
    }

    @Test
    public void validate_failure_whenInvalidConditionStringValue() {
        DynamicFeedRule<Boolean> condition = new DynamicFeedRule<>("price", Operator.RANGE, "test");
        condition.setParsedValue(null);

        ValidationResult<List<DynamicFeedAdTarget>, Defect> actual = validate(List.of(
                dynamicFeedAdTarget.withCondition(List.of(condition))));

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(validationError(
                path(index(0), field(DynamicFeedAdTarget.CONDITION), index(0), field(DynamicFeedRule.STRING_VALUE)),
                invalidFormat()))));
    }

    @Test
    public void validate_failure_whenDuplicateConditionInExisting() {
        DynamicFeedAdTarget existedDynamicAdTarget = steps.dynamicTextAdTargetsSteps()
                .createDefaultDynamicFeedAdTarget(adGroupInfo);

        ValidationResult<List<DynamicFeedAdTarget>, Defect> actual = validate(List.of(
                dynamicFeedAdTarget.withCondition(existedDynamicAdTarget.getCondition())));

        assertThat(actual).is(matchedBy(hasDefectDefinitionWith(
                validationError(CollectionDefects.duplicatedObject().defectId()))));
    }

    @Test
    public void validate_failure_whenDuplicateConditionInAdding() {
        ValidationResult<List<DynamicFeedAdTarget>, Defect> actual = validate(List.of(
                defaultDynamicFeedAdTarget(adGroupInfo),
                defaultDynamicFeedAdTarget(adGroupInfo)
        ));

        assertThat(actual).is(matchedBy(hasDefectDefinitionWith(
                validationError(CollectionDefects.duplicatedObject().defectId()))));
    }

    @Test
    public void validate_failure_whenDuplicateAndInvalidConditions() {
        DynamicFeedRule<Boolean> condition1 = new DynamicFeedRule<>("categoryId", Operator.GREATER, "test");
        condition1.setParsedValue(null);

        DynamicFeedRule<Boolean> condition2 = new DynamicFeedRule<>("categoryId", Operator.GREATER, "test");
        condition2.setParsedValue(null);

        // проверяем что не падает для не валидных условий (parsedValue = null)
        // при сериализации при поиске дубликатов
        ValidationResult<List<DynamicFeedAdTarget>, Defect> actual = validate(List.of(
                dynamicFeedAdTarget,
                defaultDynamicFeedAdTarget(adGroupInfo).withCondition(List.of(condition1)),
                defaultDynamicFeedAdTarget(adGroupInfo).withCondition(List.of(condition2))
        ));

        assertThat(actual).is(matchedBy(hasDefectDefinitionWith(
                validationError(invalidFormat().defectId()))));
    }

    @Test
    public void validate_success_whenAddMaxCountInNotEmptyGroup() {
        steps.dynamicTextAdTargetsSteps().createDefaultDynamicFeedAdTarget(adGroupInfo);

        // одно условие уже есть в группе, добавляем еще MAX_DYNAMIC_TEXT_AD_TARGETS_IN_GROUP - 1
        List<DynamicFeedAdTarget> dynamicAdTargetsToAdd = new ArrayList<>();
        for (int i = 0; i < MAX_DYNAMIC_TEXT_AD_TARGETS_IN_GROUP - 1; i++) {
            dynamicAdTargetsToAdd.add(dynamicFeedAdTargetWithRandomRules(adGroupInfo));
        }

        ValidationResult<List<DynamicFeedAdTarget>, Defect> actual = validate(dynamicAdTargetsToAdd);

        assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_failure_whenAddMoreThanMaxCountInNotEmptyGroup() {
        steps.dynamicTextAdTargetsSteps().createDefaultDynamicFeedAdTarget(adGroupInfo);

        // одно условие уже есть в группе, пытаемся добавить еще MAX_DYNAMIC_TEXT_AD_TARGETS_IN_GROUP
        List<DynamicFeedAdTarget> dynamicAdTargetsToAdd = new ArrayList<>();
        for (int i = 0; i < MAX_DYNAMIC_TEXT_AD_TARGETS_IN_GROUP; i++) {
            dynamicAdTargetsToAdd.add(dynamicFeedAdTargetWithRandomRules(adGroupInfo));
        }

        ValidationResult<List<DynamicFeedAdTarget>, Defect> actual = validate(dynamicAdTargetsToAdd);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0)),
                        maxDynamicTextAdTargetsInAdGroup(MAX_DYNAMIC_TEXT_AD_TARGETS_IN_GROUP)))));
    }

    @Test
    public void validate_failure_whenEmptyConditions_andTabCondition() {
        ValidationResult<List<DynamicFeedAdTarget>, Defect> actual = validate(List.of(
                dynamicFeedAdTarget.withCondition(List.of()).withTab(DynamicAdTargetTab.CONDITION)));

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(DynamicFeedAdTarget.CONDITION)),
                        CollectionDefects.notEmptyCollection()))));
    }

    @Test
    public void validate_failure_whenEmptyConditions_andTabTree() {
        ValidationResult<List<DynamicFeedAdTarget>, Defect> actual = validate(List.of(
                dynamicFeedAdTarget.withCondition(List.of()).withTab(DynamicAdTargetTab.TREE)));

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(DynamicFeedAdTarget.CONDITION)),
                        CollectionDefects.notEmptyCollection()))));
    }

    @Test
    public void validate_success_whenEmptyConditions_andTabAllProducts() {
        ValidationResult<List<DynamicFeedAdTarget>, Defect> actual = validate(List.of(
                dynamicFeedAdTarget.withCondition(List.of()).withTab(DynamicAdTargetTab.ALL_PRODUCTS)));

        assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
    }
}
