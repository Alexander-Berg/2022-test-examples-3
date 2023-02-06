package ru.yandex.market.deliverycalculator.workflow.service.modifier;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Sets;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.util.ReflectionAssertMatcher;
import ru.yandex.market.deliverycalculator.model.YaDeliveryTariffType;
import ru.yandex.market.deliverycalculator.storage.model.DeliveryServiceCode;
import ru.yandex.market.deliverycalculator.storage.model.modifier.Condition;
import ru.yandex.market.deliverycalculator.storage.model.modifier.DeliveryModifiersMeta;
import ru.yandex.market.deliverycalculator.storage.model.modifier.DeliveryModifiersMeta.CarrierAvailabilityModifierMeta;
import ru.yandex.market.deliverycalculator.storage.model.modifier.DeliveryModifiersMeta.ServicesModifierMeta;
import ru.yandex.market.deliverycalculator.storage.model.modifier.DeliveryModifiersMeta.ValueModifierMeta;
import ru.yandex.market.deliverycalculator.storage.model.modifier.PercentValueLimiter;
import ru.yandex.market.deliverycalculator.storage.model.modifier.ValueLimiter;
import ru.yandex.market.deliverycalculator.storage.model.modifier.ValueModificationRule;
import ru.yandex.market.deliverycalculator.workflow.test.FunctionalTest;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DeliveryModifierApplianceServiceTest extends FunctionalTest {

    @Autowired
    private DeliveryModifierApplianceService tested;

    /**
     * Тест для {@link DeliveryModifierApplianceService#findApplicableModifiers(DeliveryModifiersMeta,
     * DeliveryModifierAvailabilityFilter)} .
     */
    @Test
    void testFindApplicableModifiers() {
        DeliveryModifierAvailabilityFilter filter = new DeliveryModifierAvailabilityFilter.Builder()
                .withOfferWidth(15.00)
                .withOfferHeight(15.00)
                .withOfferLength(15.00)
                .withOfferWeight(10.00)
                .withOfferChargeableWeight(5.00)
                .withOfferCostInPenny(150000L)
                .withDeliveryCostInPenny(150000L)
                .withMaxDeliveryDays(25)
                .withDeliveryCarrierId(1L)
                .withDeliveryType(YaDeliveryTariffType.COURIER)
                .build();

        DeliveryModifiersMeta actual = tested.findApplicableModifiers(createAllShopModifiers(), filter);

        assertNotNull(actual);
        MatcherAssert.assertThat(actual, new ReflectionAssertMatcher<>(createExpectedShopModifiers()));
    }

    private DeliveryModifiersMeta createAllShopModifiers() {
        return new DeliveryModifiersMeta.Builder()
                .withCostModifiers(createInitialCostModifiers())
                .withTimeModifiers(createInitialTimeModifiers())
                .withCarrierAvailabilityModifiers(createInitialCarrierAvailabilityModifiers())
                .withServicesModifiers(createInitialServicesModifiers())
                .build();
    }

    private DeliveryModifiersMeta createExpectedShopModifiers() {
        return new DeliveryModifiersMeta.Builder()
                .withCostModifiers(createExpectedCostModifiers())
                .withCarrierAvailabilityModifiers(new ArrayList<>())
                .withTimeModifiers(createExpectedTimeModifiers())
                .withServicesModifiers(createExpectedServicesModifiers())
                .build();
    }

    private List<ValueModifierMeta<Integer>> createInitialTimeModifiers() {
        return asList(
                //Применимый модификатор. Имеет низкий приоритет.
                createApplicableLowPrioTimeModifier(),
                //Применимый модификатор. Имеет высокий приоритет
                createApplicableHighPrioTimeModifier(),
                //Неприменимый модификатор. Отфильтрован по критерию используемой службы досатвки.
                createNotApplicableTimeModifier()
        );
    }

    private List<ValueModifierMeta<Integer>> createExpectedTimeModifiers() {
        return asList(
                //Применимый модификатор. Имеет высокий приоритет
                createApplicableHighPrioTimeModifier(),
                //Применимый модификатор. Имеет низкий приоритет.
                createApplicableLowPrioTimeModifier()
        );
    }

    private List<ValueModifierMeta<Long>> createInitialCostModifiers() {
        return asList(
                //Применимый модификатор. Имеет низкий приоритет. Однако модифицирует на значение большее, чем остальные
                // модификаторы.
                createApplicableLowPrioCostModifier(),
                //Применимый модификатор. Имеет низкий приоритет
                createApplicableLowestPrioCostModifier(),
                //Неприменимый модификатор. Отфильтрован по критерию минимального веса.
                createNotApplicableCostModifier()
        );
    }

    private List<ValueModifierMeta<Long>> createExpectedCostModifiers() {
        return asList(
                //Применимый модификатор. Имеет низкий приоритет. Модифицирует на больше число чем первый модификатор.
                createApplicableLowPrioCostModifier(),
                //Применимый модификатор. Имеет низкий приоритет.
                createApplicableLowestPrioCostModifier()
        );
    }

    private List<CarrierAvailabilityModifierMeta> createInitialCarrierAvailabilityModifiers() {
        return singletonList(
                //Неприменимый модификатор. Отфильтровано по цене.
                createUnapplicableCarrierAvailabilityModifier()
        );
    }

    private List<ServicesModifierMeta> createInitialServicesModifiers() {
        return asList(
                //Применимый модификатор
                createApplicableServicesModifier1(),
                //Применимый модификатор
                createApplicableServicesModifier2()
        );
    }

    private List<ServicesModifierMeta> createExpectedServicesModifiers() {
        return asList(
                //Применимый модификатор
                createApplicableServicesModifier1(),
                //Применимый модификатор
                createApplicableServicesModifier2()
        );
    }


    private ServicesModifierMeta createApplicableServicesModifier1() {
        return new ServicesModifierMeta.Builder()
                .withId(41L)
                .withPriority(6)
                .withPaidByCustomerServices(Sets.newHashSet(DeliveryServiceCode.INSURANCE))
                .build();
    }

    private ServicesModifierMeta createApplicableServicesModifier2() {
        return new ServicesModifierMeta.Builder()
                .withId(41L)
                .withPriority(6)
                .withPaidByCustomerServices(Sets.newHashSet(DeliveryServiceCode.INSURANCE))
                .withCondition(new Condition.Builder()
                        .withCarrierIds(Sets.newHashSet(1L, 2L, 3L))
                        .withWeight(new ValueLimiter<>(5.00, 15.00))
                        .withDimension(new ValueLimiter<>(14.00, 100.00))
                        .withCost(new PercentValueLimiter.Builder()
                                .withMinValue(100000L)
                                .withMaxValue(150000L).build())
                        .withChargeableWeight(new ValueLimiter<>(0.00, 25.00))
                        .build())
                .build();
    }


    private CarrierAvailabilityModifierMeta createUnapplicableCarrierAvailabilityModifier() {
        return new CarrierAvailabilityModifierMeta.Builder()
                .withId(31L)
                .withPriority(6)
                .withIsCarrierAvailable(false)
                .withCondition(new Condition.Builder()
                        .withCost(new PercentValueLimiter.Builder()
                                .withMinValue(2500000L)
                                .build())
                        .build())
                .build();
    }

    private DeliveryModifiersMeta.ValueModifierMeta<Long> createNotApplicableCostModifier() {
        return new ValueModifierMeta.Builder<Long>()
                .withId(1L)
                .withPriority(6)
                .withModificationRule(new ValueModificationRule.Builder<Long>()
                        .withOperation(ValueModificationRule.OperationEnum.MULTIPLY)
                        .withParameter(10.00)
                        .build())
                .withCondition(new Condition.Builder()
                        .withWeight(new ValueLimiter<>(15.0, 25.0))
                        .build())
                .build();
    }

    private DeliveryModifiersMeta.ValueModifierMeta<Long> createApplicableLowPrioCostModifier() {
        return new ValueModifierMeta.Builder<Long>()
                .withId(1L)
                .withPriority(6)
                .withModificationRule(new ValueModificationRule.Builder<Long>()
                        .withOperation(ValueModificationRule.OperationEnum.MULTIPLY)
                        .withParameter(5.00)
                        .build())
                .withCondition(new Condition.Builder()
                        .withCarrierIds(Sets.newHashSet(1L, 2L))
                        .withCost(new PercentValueLimiter.Builder()
                                .withMinValue(10000L)
                                .build())
                        .build())
                .build();
    }

    private DeliveryModifiersMeta.ValueModifierMeta<Long> createApplicableLowestPrioCostModifier() {
        return new ValueModifierMeta.Builder<Long>()
                .withId(2L)
                .withPriority(6)
                .withModificationRule(new ValueModificationRule.Builder<Long>()
                        .withOperation(ValueModificationRule.OperationEnum.ADD)
                        .withParameter(10000.00)
                        .build())
                .withCondition(new Condition.Builder()
                        .withCarrierIds(Sets.newHashSet(1L, 2L))
                        .build())
                .build();
    }

    private DeliveryModifiersMeta.ValueModifierMeta<Integer> createApplicableLowPrioTimeModifier() {
        return new ValueModifierMeta.Builder<Integer>()
                .withId(22L)
                .withPriority(6)
                .withModificationRule(new ValueModificationRule.Builder<Integer>()
                        .withOperation(ValueModificationRule.OperationEnum.ADD)
                        .withParameter(10.00)
                        .build())
                .build();
    }

    private DeliveryModifiersMeta.ValueModifierMeta<Integer> createApplicableHighPrioTimeModifier() {
        return new ValueModifierMeta.Builder<Integer>()
                .withId(23L)
                .withPriority(1)
                .withModificationRule(new ValueModificationRule.Builder<Integer>()
                        .withOperation(ValueModificationRule.OperationEnum.ADD)
                        .withParameter(1.00)
                        .build())
                .withCondition(new Condition.Builder()
                        .withCarrierIds(Sets.newHashSet(1L, 2L, 3L))
                        .withWeight(new ValueLimiter<>(5.00, 15.00))
                        .withDimension(new ValueLimiter<>(14.00, 100.00))
                        .withCost(new PercentValueLimiter.Builder()
                                .withMinValue(10000L)
                                .withMaxValue(150000L).build())
                        .withChargeableWeight(new ValueLimiter<>(0.00, 25.00))
                        .build())
                .build();
    }

    private DeliveryModifiersMeta.ValueModifierMeta<Integer> createNotApplicableTimeModifier() {
        return new ValueModifierMeta.Builder<Integer>()
                .withId(24L)
                .withPriority(1)
                .withModificationRule(new ValueModificationRule.Builder<Integer>()
                        .withOperation(ValueModificationRule.OperationEnum.ADD)
                        .withParameter(1.00)
                        .build())
                .withCondition(new Condition.Builder()
                        .withCarrierIds(Sets.newHashSet(2L, 3L))
                        .build())
                .build();
    }

}
