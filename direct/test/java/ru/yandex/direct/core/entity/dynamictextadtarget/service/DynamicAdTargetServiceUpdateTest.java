package ru.yandex.direct.core.entity.dynamictextadtarget.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import one.util.streamex.StreamEx;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicAdTarget;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicAdTargetTab;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicFeedAdTarget;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicFeedRule;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicTextAdTarget;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.WebpageRule;
import ru.yandex.direct.core.entity.dynamictextadtarget.repository.DynamicTextAdTargetRepository;
import ru.yandex.direct.core.entity.dynamictextadtarget.utils.DynamicTextAdTargetHashUtils;
import ru.yandex.direct.core.entity.performancefilter.model.Operator;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.BigDecimalComparator.BIG_DECIMAL_COMPARATOR;
import static ru.yandex.direct.core.entity.dynamictextadtarget.utils.DynamicTextAdTargetHashUtils.getHashForDynamicFeedRules;
import static ru.yandex.direct.core.testing.data.TestDynamicTextAdTargets.defaultDynamicTextAdTargetWithRandomRules;
import static ru.yandex.direct.utils.FunctionalUtils.listToMap;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class DynamicAdTargetServiceUpdateTest {

    @Autowired
    private Steps steps;
    @Autowired
    private DynamicAdTargetsAddUpdateHelper helper;
    @Autowired
    private DynamicTextAdTargetRepository dynamicTextAdTargetRepository;

    private int shard;
    private ClientId clientId;
    private AdGroupInfo dynamicFeedAdGroup;
    private AdGroupInfo dynamicTextAdGroup;
    private DynamicFeedAdTarget dynamicFeedAdTarget;
    private DynamicTextAdTarget dynamicTextAdTarget;

    @Before
    public void before() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClientAndUser();
        shard = clientInfo.getShard();
        clientId = clientInfo.getClientId();

        dynamicFeedAdGroup = steps.adGroupSteps().createActiveDynamicFeedAdGroup(clientInfo);
        dynamicTextAdGroup = steps.adGroupSteps().createActiveDynamicTextAdGroup(clientInfo);

        dynamicFeedAdTarget = steps.dynamicTextAdTargetsSteps()
                .createDefaultDynamicFeedAdTarget(dynamicFeedAdGroup);
        dynamicTextAdTarget = steps.dynamicTextAdTargetsSteps()
                .createDefaultDynamicTextAdTarget(dynamicTextAdGroup)
                .getDynamicTextAdTarget();
    }

    @Test
    public void updateDynamicFeedAdTargets_whenPriceChanged() {
        DynamicFeedAdTarget newDynamicAdTarget = new DynamicFeedAdTarget()
                .withId(dynamicFeedAdTarget.getId())
                .withConditionName("condition name " + RandomStringUtils.randomNumeric(4))
                .withPrice(BigDecimal.valueOf(15))
                .withPriceContext(BigDecimal.valueOf(25))
                .withAutobudgetPriority(5)
                .withTab(DynamicAdTargetTab.CONDITION);

        updateDynamicFeedAdTargets(List.of(newDynamicAdTarget));

        DynamicFeedAdTarget actual = getDynamicFeedAdTarget(newDynamicAdTarget.getId());
        assertThat(actual)
                .usingComparatorForType(BIG_DECIMAL_COMPARATOR, BigDecimal.class)
                .isEqualToIgnoringNullFields(newDynamicAdTarget);
        assertThat(actual.getDynamicConditionId()).isEqualTo(dynamicFeedAdTarget.getDynamicConditionId());
    }

    @Test
    public void updateDynamicTextAdTargets_whenPriceChanged() {
        DynamicTextAdTarget newDynamicAdTarget = new DynamicTextAdTarget()
                .withId(dynamicTextAdTarget.getId())
                .withConditionName("condition name " + RandomStringUtils.randomNumeric(4))
                .withPrice(BigDecimal.valueOf(15))
                .withPriceContext(BigDecimal.valueOf(25))
                .withAutobudgetPriority(5);

        updateDynamicTextAdTargets(List.of(newDynamicAdTarget));

        DynamicTextAdTarget actual = getDynamicTextAdTarget(newDynamicAdTarget.getId());
        assertThat(actual)
                .usingComparatorForType(BIG_DECIMAL_COMPARATOR, BigDecimal.class)
                .isEqualToIgnoringNullFields(newDynamicAdTarget);
        assertThat(actual.getDynamicConditionId()).isEqualTo(dynamicTextAdTarget.getDynamicConditionId());
    }

    @Test
    public void updateDynamicFeedAdTargets_whenConditionChanged() {
        List<DynamicFeedRule> rules = newDynamicFeedRules();
        DynamicFeedAdTarget newDynamicAdTarget = new DynamicFeedAdTarget()
                .withId(dynamicFeedAdTarget.getId())
                .withCondition(rules)
                .withConditionHash(getHashForDynamicFeedRules(rules));

        updateDynamicFeedAdTargets(List.of(newDynamicAdTarget));

        DynamicFeedAdTarget actual = getDynamicFeedAdTarget(newDynamicAdTarget.getId());
        assertThat(actual).isEqualToIgnoringNullFields(newDynamicAdTarget);
        assertThat(actual.getDynamicConditionId()).isNotEqualTo(dynamicFeedAdTarget.getDynamicConditionId());
    }

    @Test
    public void updateDynamicFeedAdTargets_whenConditionChanged_withUpdateAllowed() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.UPDATE_DYNAMIC_CONDITIONS_ALLOWED, true);

        var rules = newDynamicFeedRules();
        var newDynamicAdTarget = new DynamicFeedAdTarget()
                .withId(dynamicFeedAdTarget.getId())
                .withCondition(rules)
                .withConditionHash(getHashForDynamicFeedRules(rules));

        updateDynamicFeedAdTargets(List.of(newDynamicAdTarget));

        var actual = getDynamicFeedAdTarget(newDynamicAdTarget.getId());
        assertThat(actual).isEqualToIgnoringNullFields(newDynamicAdTarget);
        // в отличие от теста без включенной фичи, dynamicConditionId не должен измениться
        assertThat(actual.getDynamicConditionId()).isEqualTo(dynamicFeedAdTarget.getDynamicConditionId());
    }

    @Test
    public void updateDynamicTextAdTargets_reuseDeletedCondition() {
        DynamicTextAdTarget deletedDynamicAdTarget = defaultDynamicTextAdTargetWithRandomRules(dynamicTextAdGroup);
        steps.dynamicTextAdTargetsSteps().createDynamicTextAdTarget(dynamicTextAdGroup, deletedDynamicAdTarget);

        dynamicTextAdTargetRepository.deleteDynamicTextAdTargets(shard,
                List.of(deletedDynamicAdTarget.getDynamicConditionId()));

        // переиспользуем удаленное условие
        List<WebpageRule> rules = deletedDynamicAdTarget.getCondition();

        DynamicTextAdTarget newDynamicAdTarget = new DynamicTextAdTarget()
                .withId(dynamicTextAdTarget.getId())
                .withCondition(rules)
                .withConditionHash(DynamicTextAdTargetHashUtils.getHash(rules))
                .withConditionUniqHash(DynamicTextAdTargetHashUtils.getUniqHash(rules));

        updateDynamicTextAdTargets(List.of(newDynamicAdTarget));

        DynamicTextAdTarget actual = getDynamicTextAdTarget(newDynamicAdTarget.getId());
        assertThat(actual).isEqualToIgnoringNullFields(newDynamicAdTarget);
        assertThat(actual.getDynamicConditionId()).isEqualTo(deletedDynamicAdTarget.getDynamicConditionId());
    }

    @Test
    public void updateDynamicTextAdTargets_noReuseDeletedCondition_withUpdateAllowed() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.UPDATE_DYNAMIC_CONDITIONS_ALLOWED, true);

        var deletedDynamicAdTarget = defaultDynamicTextAdTargetWithRandomRules(dynamicTextAdGroup);
        steps.dynamicTextAdTargetsSteps().createDynamicTextAdTarget(dynamicTextAdGroup, deletedDynamicAdTarget);

        dynamicTextAdTargetRepository.deleteDynamicTextAdTargets(shard,
                List.of(deletedDynamicAdTarget.getDynamicConditionId()));

        // переиспользуем удаленное условие
        var rules = deletedDynamicAdTarget.getCondition();

        var newDynamicAdTarget = new DynamicTextAdTarget()
                .withId(dynamicTextAdTarget.getId())
                .withCondition(rules)
                .withConditionHash(DynamicTextAdTargetHashUtils.getHash(rules))
                .withConditionUniqHash(DynamicTextAdTargetHashUtils.getUniqHash(rules));

        updateDynamicTextAdTargets(List.of(newDynamicAdTarget));

        var actual = getDynamicTextAdTarget(newDynamicAdTarget.getId());
        assertThat(actual).isEqualToIgnoringNullFields(newDynamicAdTarget);
        // в отличие от теста без включенной фичи, dynamicConditionId не должен измениться:
        // вместо переиспользования старое эквивалентное условие удаляется, а текущее - редактируется
        assertThat(actual.getDynamicConditionId()).isEqualTo(dynamicTextAdTarget.getDynamicConditionId());
    }

    @Test
    public void updateDynamicTextAdTargets_whenPriceNameAndConditionChanged() {
        List<WebpageRule> rules = defaultDynamicTextAdTargetWithRandomRules(dynamicTextAdGroup).getCondition();

        DynamicTextAdTarget newDynamicAdTarget = new DynamicTextAdTarget()
                .withId(dynamicTextAdTarget.getId())
                .withConditionName("condition name " + RandomStringUtils.randomNumeric(4))
                .withPrice(BigDecimal.valueOf(15))
                .withCondition(rules)
                .withConditionHash(DynamicTextAdTargetHashUtils.getHash(rules))
                .withConditionUniqHash(DynamicTextAdTargetHashUtils.getUniqHash(rules));

        updateDynamicTextAdTargets(List.of(newDynamicAdTarget));

        DynamicTextAdTarget actual = getDynamicTextAdTarget(newDynamicAdTarget.getId());
        assertThat(actual)
                .usingComparatorForType(BIG_DECIMAL_COMPARATOR, BigDecimal.class)
                .isEqualToIgnoringNullFields(newDynamicAdTarget);

        assertThat(actual.getDynamicConditionId()).isNotEqualTo(dynamicTextAdTarget.getDynamicConditionId());
    }

    @Test
    public void updateDynamicTextAdTargets_whenPriceNameAndConditionChanged_withUpdateAllowed() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.UPDATE_DYNAMIC_CONDITIONS_ALLOWED, true);

        var rules = defaultDynamicTextAdTargetWithRandomRules(dynamicTextAdGroup).getCondition();

        var newDynamicAdTarget = new DynamicTextAdTarget()
                .withId(dynamicTextAdTarget.getId())
                .withConditionName("condition name " + RandomStringUtils.randomNumeric(4))
                .withPrice(BigDecimal.valueOf(15))
                .withCondition(rules)
                .withConditionHash(DynamicTextAdTargetHashUtils.getHash(rules))
                .withConditionUniqHash(DynamicTextAdTargetHashUtils.getUniqHash(rules));

        updateDynamicTextAdTargets(List.of(newDynamicAdTarget));

        var actual = getDynamicTextAdTarget(newDynamicAdTarget.getId());
        assertThat(actual)
                .usingComparatorForType(BIG_DECIMAL_COMPARATOR, BigDecimal.class)
                .isEqualToIgnoringNullFields(newDynamicAdTarget);
        // в отличие от теста без включенной фичи, dynamicConditionId не должен измениться
        assertThat(actual.getDynamicConditionId()).isEqualTo(dynamicTextAdTarget.getDynamicConditionId());
    }

    @Test
    public void updateDynamicFeedAdTargets_whenTwoDynamicAdTargets() {
        DynamicFeedAdTarget dynamicAdTargetWithPriceChanged = new DynamicFeedAdTarget()
                .withId(dynamicFeedAdTarget.getId())
                .withPrice(BigDecimal.valueOf(15))
                .withConditionName("condition name " + RandomStringUtils.randomNumeric(4));

        AdGroupInfo secondDynamicFeedAdGroup = steps.adGroupSteps().createActiveDynamicFeedAdGroup(
                dynamicFeedAdGroup.getClientInfo());
        Long secondDynamicFeedAdTargetId = steps.dynamicTextAdTargetsSteps()
                .createDefaultDynamicFeedAdTarget(secondDynamicFeedAdGroup)
                .getId();

        List<DynamicFeedRule> rules = newDynamicFeedRules();
        DynamicFeedAdTarget dynamicAdTargetWithConditionChanged = new DynamicFeedAdTarget()
                .withId(secondDynamicFeedAdTargetId)
                .withCondition(rules)
                .withConditionHash(getHashForDynamicFeedRules(rules));

        updateDynamicFeedAdTargets(List.of(dynamicAdTargetWithPriceChanged, dynamicAdTargetWithConditionChanged));

        DynamicFeedAdTarget actualWithPriceChanged = getDynamicFeedAdTarget(dynamicAdTargetWithPriceChanged.getId());
        assertThat(actualWithPriceChanged)
                .usingComparatorForType(BIG_DECIMAL_COMPARATOR, BigDecimal.class)
                .isEqualToIgnoringNullFields(dynamicAdTargetWithPriceChanged);

        DynamicFeedAdTarget actualWithConditionChanged = getDynamicFeedAdTarget(
                dynamicAdTargetWithConditionChanged.getId());
        assertThat(actualWithConditionChanged)
                .isEqualToIgnoringNullFields(dynamicAdTargetWithConditionChanged);
    }

    @Test
    public void updateDynamicTextAdTargets_whenTwoDynamicAdTargets_swapConditions() {
        DynamicTextAdTarget secondDynamicTextAdTarget = defaultDynamicTextAdTargetWithRandomRules(dynamicTextAdGroup);
        steps.dynamicTextAdTargetsSteps().createDynamicTextAdTarget(dynamicTextAdGroup, secondDynamicTextAdTarget);

        List<WebpageRule> firstCondition = dynamicTextAdTarget.getCondition();
        List<WebpageRule> secondCondition = secondDynamicTextAdTarget.getCondition();

        // меняем местами условия
        DynamicTextAdTarget firstNewDynamicAdTarget = new DynamicTextAdTarget()
                .withId(dynamicTextAdTarget.getId())
                .withCondition(secondCondition)
                .withConditionHash(DynamicTextAdTargetHashUtils.getHash(secondCondition))
                .withConditionUniqHash(DynamicTextAdTargetHashUtils.getUniqHash(secondCondition));

        DynamicTextAdTarget secondNewDynamicAdTarget = new DynamicTextAdTarget()
                .withId(secondDynamicTextAdTarget.getId())
                .withCondition(firstCondition)
                .withConditionHash(DynamicTextAdTargetHashUtils.getHash(firstCondition))
                .withConditionUniqHash(DynamicTextAdTargetHashUtils.getUniqHash(firstCondition));

        updateDynamicTextAdTargets(List.of(firstNewDynamicAdTarget, secondNewDynamicAdTarget));

        DynamicTextAdTarget first = getDynamicTextAdTarget(firstNewDynamicAdTarget.getId());
        assertThat(first).isEqualToIgnoringNullFields(firstNewDynamicAdTarget);

        DynamicTextAdTarget second = getDynamicTextAdTarget(secondNewDynamicAdTarget.getId());
        assertThat(second).isEqualToIgnoringNullFields(secondNewDynamicAdTarget);

        // проверяем что dynamicConditionId поменялись местами
        assertThat(first.getDynamicConditionId()).isEqualTo(secondDynamicTextAdTarget.getDynamicConditionId());
        assertThat(second.getDynamicConditionId()).isEqualTo(dynamicTextAdTarget.getDynamicConditionId());
    }

    @Test
    public void updateDynamicTextAdTargets_whenTwoDynamicAdTargets_swapConditions_withUpdateAllowed() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.UPDATE_DYNAMIC_CONDITIONS_ALLOWED, true);

        var secondDynamicTextAdTarget = defaultDynamicTextAdTargetWithRandomRules(dynamicTextAdGroup);
        steps.dynamicTextAdTargetsSteps().createDynamicTextAdTarget(dynamicTextAdGroup, secondDynamicTextAdTarget);

        var firstCondition = dynamicTextAdTarget.getCondition();
        var secondCondition = secondDynamicTextAdTarget.getCondition();

        // меняем местами условия
        var firstNewDynamicAdTarget = new DynamicTextAdTarget()
                .withId(dynamicTextAdTarget.getId())
                .withCondition(secondCondition)
                .withConditionHash(DynamicTextAdTargetHashUtils.getHash(secondCondition))
                .withConditionUniqHash(DynamicTextAdTargetHashUtils.getUniqHash(secondCondition));

        var secondNewDynamicAdTarget = new DynamicTextAdTarget()
                .withId(secondDynamicTextAdTarget.getId())
                .withCondition(firstCondition)
                .withConditionHash(DynamicTextAdTargetHashUtils.getHash(firstCondition))
                .withConditionUniqHash(DynamicTextAdTargetHashUtils.getUniqHash(firstCondition));

        updateDynamicTextAdTargets(List.of(firstNewDynamicAdTarget, secondNewDynamicAdTarget));

        var first = getDynamicTextAdTarget(firstNewDynamicAdTarget.getId());
        assertThat(first).isEqualToIgnoringNullFields(firstNewDynamicAdTarget);

        var second = getDynamicTextAdTarget(secondNewDynamicAdTarget.getId());
        assertThat(second).isEqualToIgnoringNullFields(secondNewDynamicAdTarget);

        // в отличие от теста без включенной фичи, dynamicConditionId не должны изменяться
        assertThat(first.getDynamicConditionId()).isEqualTo(dynamicTextAdTarget.getDynamicConditionId());
        assertThat(second.getDynamicConditionId()).isEqualTo(secondDynamicTextAdTarget.getDynamicConditionId());
    }

    @Test
    public void updateDynamicFeedAdTargets_whenConditionChangedBack() {
        List<DynamicFeedRule> rules = newDynamicFeedRules();
        DynamicFeedAdTarget newDynamicAdTarget = new DynamicFeedAdTarget()
                .withId(dynamicFeedAdTarget.getId())
                .withCondition(rules)
                .withConditionHash(getHashForDynamicFeedRules(rules));

        updateDynamicFeedAdTargets(List.of(newDynamicAdTarget));

        // возвращаем исходные conditions
        // но меняем name и price (чтобы проверить что пересоздаются с нужными параметрами)
        newDynamicAdTarget = new DynamicFeedAdTarget()
                .withId(dynamicFeedAdTarget.getId())
                .withCondition(dynamicFeedAdTarget.getCondition())
                .withConditionHash(dynamicFeedAdTarget.getConditionHash())
                .withConditionName("condition name " + RandomStringUtils.randomNumeric(4))
                .withPrice(BigDecimal.valueOf(15));

        updateDynamicFeedAdTargets(List.of(newDynamicAdTarget));

        // проверяем что conditions такие же как были (dynamicConditionId должен вернуться к исходному значению)
        DynamicFeedAdTarget actual = getDynamicFeedAdTarget(newDynamicAdTarget.getId());
        DynamicFeedAdTarget expected = dynamicFeedAdTarget
                .withConditionName(newDynamicAdTarget.getConditionName())
                .withPrice(newDynamicAdTarget.getPrice());
        assertThat(actual)
                .usingComparatorForType(BIG_DECIMAL_COMPARATOR, BigDecimal.class)
                .isEqualToIgnoringNullFields(expected);
    }

    private static List<DynamicFeedRule> newDynamicFeedRules() {
        DynamicFeedRule<List<String>> dynamicFeedRule =
                new DynamicFeedRule<>("vendor", Operator.CONTAINS, "[\"test\"]");
        dynamicFeedRule.setParsedValue(List.of("test"));
        return List.of(dynamicFeedRule);
    }

    private void updateDynamicFeedAdTargets(List<DynamicFeedAdTarget> newDynamicAdTargets) {
        List<AppliedChanges<DynamicFeedAdTarget>> dynamicFeedAdTargetChanges =
                getDynamicFeedAdTargetChanges(newDynamicAdTargets);

        // обновляем условия
        helper.updateDynamicAdTargets(shard, clientId, dynamicFeedAdTargetChanges);
    }

    private void updateDynamicTextAdTargets(List<DynamicTextAdTarget> newDynamicAdTargets) {
        List<AppliedChanges<DynamicTextAdTarget>> dynamicTextAdTargetChanges =
                getDynamicTextAdTargetChanges(newDynamicAdTargets);

        // обновляем условия
        helper.updateDynamicAdTargets(shard, clientId, dynamicTextAdTargetChanges);
    }

    private List<AppliedChanges<DynamicFeedAdTarget>> getDynamicFeedAdTargetChanges(
            List<DynamicFeedAdTarget> newDynamicAdTargets) {

        List<ModelChanges<DynamicFeedAdTarget>> modelChanges = StreamEx.of(newDynamicAdTargets)
                .map(this::toDynamicFeedAdTargetModelChanges)
                .toList();
        List<Long> ids = mapList(modelChanges, ModelChanges::getId);

        List<DynamicFeedAdTarget> oldDynamicAdTargets = dynamicTextAdTargetRepository
                .getDynamicFeedAdTargetsByIds(shard, clientId, ids);
        Map<Long, DynamicFeedAdTarget> oldDynamicAdTargetById = listToMap(oldDynamicAdTargets, DynamicAdTarget::getId);

        return StreamEx.of(modelChanges)
                .map(changes -> changes.applyTo(oldDynamicAdTargetById.get(changes.getId())))
                .filter(AppliedChanges::hasActuallyChangedProps)
                .toList();
    }

    private List<AppliedChanges<DynamicTextAdTarget>> getDynamicTextAdTargetChanges(
            List<DynamicTextAdTarget> newDynamicAdTargets) {

        List<ModelChanges<DynamicTextAdTarget>> modelChanges = StreamEx.of(newDynamicAdTargets)
                .map(this::toDynamicTextAdTargetModelChanges)
                .toList();
        List<Long> ids = mapList(modelChanges, ModelChanges::getId);

        List<DynamicTextAdTarget> oldDynamicAdTargets = dynamicTextAdTargetRepository
                .getDynamicTextAdTargetsByIds(shard, clientId, ids);
        Map<Long, DynamicTextAdTarget> oldDynamicAdTargetById = listToMap(oldDynamicAdTargets, DynamicAdTarget::getId);

        return StreamEx.of(modelChanges)
                .map(changes -> changes.applyTo(oldDynamicAdTargetById.get(changes.getId())))
                .filter(AppliedChanges::hasActuallyChangedProps)
                .toList();
    }

    private ModelChanges<DynamicFeedAdTarget> toDynamicFeedAdTargetModelChanges(
            DynamicFeedAdTarget newDynamicAdTarget) {

        ModelChanges<DynamicFeedAdTarget> changes =
                new ModelChanges<>(newDynamicAdTarget.getId(), DynamicFeedAdTarget.class);

        changes.processNotNull(newDynamicAdTarget.getCondition(), DynamicFeedAdTarget.CONDITION);
        changes.processNotNull(newDynamicAdTarget.getConditionHash(), DynamicAdTarget.CONDITION_HASH);
        changes.processNotNull(newDynamicAdTarget.getTab(), DynamicAdTarget.TAB);

        changes.processNotNull(newDynamicAdTarget.getConditionName(), DynamicAdTarget.CONDITION_NAME);
        changes.processNotNull(newDynamicAdTarget.getPrice(), DynamicAdTarget.PRICE);
        changes.processNotNull(newDynamicAdTarget.getPriceContext(), DynamicAdTarget.PRICE_CONTEXT);
        changes.processNotNull(newDynamicAdTarget.getAutobudgetPriority(), DynamicAdTarget.AUTOBUDGET_PRIORITY);

        return changes;
    }

    private ModelChanges<DynamicTextAdTarget> toDynamicTextAdTargetModelChanges(
            DynamicTextAdTarget newDynamicAdTarget) {

        ModelChanges<DynamicTextAdTarget> changes =
                new ModelChanges<>(newDynamicAdTarget.getId(), DynamicTextAdTarget.class);

        changes.processNotNull(newDynamicAdTarget.getCondition(), DynamicTextAdTarget.CONDITION);
        changes.processNotNull(newDynamicAdTarget.getConditionHash(), DynamicAdTarget.CONDITION_HASH);
        changes.processNotNull(newDynamicAdTarget.getConditionUniqHash(), DynamicTextAdTarget.CONDITION_UNIQ_HASH);

        changes.processNotNull(newDynamicAdTarget.getConditionName(), DynamicAdTarget.CONDITION_NAME);
        changes.processNotNull(newDynamicAdTarget.getPrice(), DynamicAdTarget.PRICE);
        changes.processNotNull(newDynamicAdTarget.getPriceContext(), DynamicAdTarget.PRICE_CONTEXT);
        changes.processNotNull(newDynamicAdTarget.getAutobudgetPriority(), DynamicAdTarget.AUTOBUDGET_PRIORITY);

        return changes;
    }

    private DynamicFeedAdTarget getDynamicFeedAdTarget(Long id) {
        List<DynamicFeedAdTarget> dynamicAdTargets = dynamicTextAdTargetRepository
                .getDynamicFeedAdTargetsByIds(shard, clientId, List.of(id));

        assertThat(dynamicAdTargets).hasSize(1);
        return dynamicAdTargets.get(0);
    }

    private DynamicTextAdTarget getDynamicTextAdTarget(Long id) {
        List<DynamicTextAdTarget> dynamicAdTargets = dynamicTextAdTargetRepository
                .getDynamicTextAdTargetsByIds(shard, clientId, List.of(id));

        assertThat(dynamicAdTargets).hasSize(1);
        return dynamicAdTargets.get(0);
    }
}
