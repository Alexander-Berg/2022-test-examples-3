package ru.yandex.market.deliverycalculator.workflow.service.modifier;

import java.util.Set;
import java.util.stream.Stream;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.deliverycalculator.model.YaDeliveryTariffType;
import ru.yandex.market.deliverycalculator.storage.model.modifier.Condition;
import ru.yandex.market.deliverycalculator.storage.model.modifier.DeliveryModifiersMeta.ModifierMeta;
import ru.yandex.market.deliverycalculator.storage.model.modifier.PercentValueLimiter;
import ru.yandex.market.deliverycalculator.storage.model.modifier.ValueLimiter;
import ru.yandex.market.deliverycalculator.workflow.test.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Тест для {@link DeliveryModifierAvailabilityService}.
 */
@DbUnitDataSet(before = "regions.csv")
class DeliveryModifierAvailabilityServiceTest extends FunctionalTest {

    @Autowired
    private DeliveryModifierAvailabilityService tested;

    private static final DeliveryModifierAvailabilityFilter FILTER = createFilter(12850L);
    private static final DeliveryModifierAvailabilityFilter FILTER_NO_OFFER_COST = createFilter(null);
    private static final Condition CONDITION_FULL = createCondition();

    /**
     * Если в фильтре не указана цена (а в условии модификатора присутствует), то не нужно применять модификатор
     */
    @Test
    void testNoOfferCostInFilter() {
        ModifierMeta modifierMeta = new ModifierMeta(1L, CONDITION_FULL, 1);

        assertFalse(tested.isAvailable(modifierMeta, FILTER_NO_OFFER_COST));
    }

    /**
     * Тест для {@link DeliveryModifierAvailabilityService#isAvailable(ModifierMeta,
     * DeliveryModifierAvailabilityFilter)}
     *
     * @param testDescription - описание теста для корректного отображения
     * @param condition       - условие применение анализируемого модификатора
     * @param expectedResult  - ожидаемый результат проверки применимости модификатора.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("argumentsForFilter")
    void testFilter(String testDescription, Condition condition, boolean expectedResult) {
        ModifierMeta modifierMeta = new ModifierMeta(1L, condition, 1);

        assertEquals(expectedResult, tested.isAvailable(modifierMeta, FILTER));
    }

    static Stream<Arguments> argumentsForFilter() {
        return Stream.of(
                Arguments.of("Модификатор без условий",
                        null, true),
                Arguments.of("Условие полностью соответсвтующее фильтру. Все поля заполнены", CONDITION_FULL, true),
                Arguments.of("Условие полностью соответсвует фильтру. Нет условия не цену",
                        new Condition.Builder()
                                .withWeight(new ValueLimiter<>(10.00, 20.00))
                                .withChargeableWeight(new ValueLimiter<>(2.00, 10.00))
                                .withDimension(new ValueLimiter<>(10.00, 20.00))
                                .withCarrierIds(Sets.newHashSet(1L, 2L, 3L, 4L, 5L))
                                .withDeliveryTypes(Sets.newHashSet(YaDeliveryTariffType.COURIER, YaDeliveryTariffType.POST))
                                .withDeliveryDestinations(Sets.newHashSet(213, 120999))
                                .build(), true),
                Arguments.of("Условие полностью соответсвует фильтру. Нет условия на максимальное значение цены",
                        new Condition.Builder()
                                .withCost(new PercentValueLimiter.Builder().withMinValue(10000L).build())
                                .withWeight(new ValueLimiter<>(10.00, 20.00))
                                .withChargeableWeight(new ValueLimiter<>(2.00, 10.00))
                                .withDimension(new ValueLimiter<>(10.00, 20.00))
                                .withCarrierIds(Sets.newHashSet(1L, 2L, 3L, 4L, 5L))
                                .withDeliveryTypes(Sets.newHashSet(YaDeliveryTariffType.COURIER, YaDeliveryTariffType.POST))
                                .withDeliveryDestinations(Sets.newHashSet(213, 120999))
                                .build(), true),
                Arguments.of("Условие полностью соответсвует фильтру. Нет условия на минимальное значение цены",
                        new Condition.Builder()
                                .withCost(new PercentValueLimiter.Builder().withMaxValue(15000L).build())
                                .withWeight(new ValueLimiter<>(10.00, 20.00))
                                .withChargeableWeight(new ValueLimiter<>(2.00, 10.00))
                                .withDimension(new ValueLimiter<>(10.00, 20.00))
                                .withCarrierIds(Sets.newHashSet(1L, 2L, 3L, 4L, 5L))
                                .withDeliveryTypes(Sets.newHashSet(YaDeliveryTariffType.COURIER, YaDeliveryTariffType.POST))
                                .withDeliveryDestinations(Sets.newHashSet(213, 120999))
                                .build(), true),
                Arguments.of("Не проходит по условию цены. Меньше минимума.",
                        new Condition.Builder()
                                .withCost(new PercentValueLimiter.Builder().withMinValue(13000L).withMaxValue(15000L).build())
                                .withWeight(new ValueLimiter<>(10.00, 20.00))
                                .withChargeableWeight(new ValueLimiter<>(2.00, 10.00))
                                .withDimension(new ValueLimiter<>(10.00, 20.00))
                                .withCarrierIds(Sets.newHashSet(1L, 2L, 3L, 4L, 5L))
                                .withDeliveryTypes(Sets.newHashSet(YaDeliveryTariffType.COURIER, YaDeliveryTariffType.POST))
                                .withDeliveryDestinations(Sets.newHashSet(213, 120999))
                                .build(), false),
                Arguments.of("Не проходит по условию цены. Больше максимума.",
                        new Condition.Builder()
                                .withCost(new PercentValueLimiter.Builder().withMinValue(10000L).withMaxValue(12000L).build())
                                .withWeight(new ValueLimiter<>(10.00, 20.00))
                                .withChargeableWeight(new ValueLimiter<>(2.00, 10.00))
                                .withDimension(new ValueLimiter<>(10.00, 20.00))
                                .withCarrierIds(Sets.newHashSet(1L, 2L, 3L, 4L, 5L))
                                .withDeliveryTypes(Sets.newHashSet(YaDeliveryTariffType.COURIER, YaDeliveryTariffType.POST))
                                .withDeliveryDestinations(Sets.newHashSet(213, 120999))
                                .build(), false),
                Arguments.of("Не проходит по условию веса. Меньше минимума.",
                        new Condition.Builder()
                                .withCost(new PercentValueLimiter.Builder().withMinValue(10000L).withMaxValue(15000L).build())
                                .withWeight(new ValueLimiter<>(16.00, 20.00))
                                .withChargeableWeight(new ValueLimiter<>(2.00, 10.00))
                                .withDimension(new ValueLimiter<>(10.00, 20.00))
                                .withCarrierIds(Sets.newHashSet(1L, 2L, 3L, 4L, 5L))
                                .withDeliveryTypes(Sets.newHashSet(YaDeliveryTariffType.COURIER, YaDeliveryTariffType.POST))
                                .withDeliveryDestinations(Sets.newHashSet(213, 120999))
                                .build(), false),
                Arguments.of("Не проходит по условию веса. Больше максимума.",
                        new Condition.Builder()
                                .withCost(new PercentValueLimiter.Builder().withMinValue(10000L).withMaxValue(15000L).build())
                                .withWeight(new ValueLimiter<>(10.00, 13.00))
                                .withChargeableWeight(new ValueLimiter<>(2.00, 10.00))
                                .withDimension(new ValueLimiter<>(10.00, 20.00))
                                .withCarrierIds(Sets.newHashSet(1L, 2L, 3L, 4L, 5L))
                                .withDeliveryTypes(Sets.newHashSet(YaDeliveryTariffType.COURIER, YaDeliveryTariffType.POST))
                                .withDeliveryDestinations(Sets.newHashSet(213, 120999))
                                .build(), false),
                Arguments.of("Не проходит по условию платного веса. Меньше минимума.",
                        new Condition.Builder()
                                .withCost(new PercentValueLimiter.Builder().withMinValue(10000L).withMaxValue(15000L).build())
                                .withWeight(new ValueLimiter<>(10.00, 20.00))
                                .withChargeableWeight(new ValueLimiter<>(10.00, 20.00))
                                .withDimension(new ValueLimiter<>(10.00, 20.00))
                                .withCarrierIds(Sets.newHashSet(1L, 2L, 3L, 4L, 5L))
                                .withDeliveryTypes(Sets.newHashSet(YaDeliveryTariffType.COURIER, YaDeliveryTariffType.POST))
                                .withDeliveryDestinations(Sets.newHashSet(213, 120999))
                                .build(), false),
                Arguments.of("Не проходит по условию платного веса. Больше максимума.",
                        new Condition.Builder()
                                .withCost(new PercentValueLimiter.Builder().withMinValue(10000L).withMaxValue(15000L).build())
                                .withWeight(new ValueLimiter<>(10.00, 20.00))
                                .withChargeableWeight(new ValueLimiter<>(2.00, 8.99))
                                .withDimension(new ValueLimiter<>(10.00, 20.00))
                                .withCarrierIds(Sets.newHashSet(1L, 2L, 3L, 4L, 5L))
                                .withDeliveryTypes(Sets.newHashSet(YaDeliveryTariffType.COURIER, YaDeliveryTariffType.POST))
                                .withDeliveryDestinations(Sets.newHashSet(213, 120999))
                                .build(), false),
                Arguments.of("Не проходит по условию габаритов. Ширина меньше минимума.",
                        new Condition.Builder()
                                .withCost(new PercentValueLimiter.Builder().withMinValue(10000L).withMaxValue(15000L).build())
                                .withWeight(new ValueLimiter<>(10.00, 20.00))
                                .withChargeableWeight(new ValueLimiter<>(2.00, 10.00))
                                .withDimension(new ValueLimiter<>(12.20, 20.00))
                                .withCarrierIds(Sets.newHashSet(1L, 2L, 3L, 4L, 5L))
                                .withDeliveryTypes(Sets.newHashSet(YaDeliveryTariffType.COURIER, YaDeliveryTariffType.POST))
                                .withDeliveryDestinations(Sets.newHashSet(213, 120999))
                                .build(), false),
                Arguments.of("Не проходит по условию габаритов. Длина больше максимума.",
                        new Condition.Builder()
                                .withCost(new PercentValueLimiter.Builder().withMinValue(10000L).withMaxValue(15000L).build())
                                .withWeight(new ValueLimiter<>(10.00, 20.00))
                                .withChargeableWeight(new ValueLimiter<>(2.00, 10.00))
                                .withDimension(new ValueLimiter<>(10.00, 13.52))
                                .withCarrierIds(Sets.newHashSet(1L, 2L, 3L, 4L, 5L))
                                .withDeliveryTypes(Sets.newHashSet(YaDeliveryTariffType.COURIER, YaDeliveryTariffType.POST))
                                .withDeliveryDestinations(Sets.newHashSet(213, 120999))
                                .build(), false),
                Arguments.of("Не проходит по условию типа доставки.",
                        new Condition.Builder()
                                .withCost(new PercentValueLimiter.Builder().withMinValue(10000L).withMaxValue(15000L).build())
                                .withWeight(new ValueLimiter<>(10.00, 20.00))
                                .withChargeableWeight(new ValueLimiter<>(2.00, 10.00))
                                .withDimension(new ValueLimiter<>(10.00, 20.00))
                                .withCarrierIds(Sets.newHashSet(1L, 2L, 3L, 4L, 5L))
                                .withDeliveryTypes(Sets.newHashSet(YaDeliveryTariffType.POST))
                                .withDeliveryDestinations(Sets.newHashSet(213, 120999))
                                .build(), false),
                Arguments.of("Не проходит по условию используемой службы доставки.",
                        new Condition.Builder()
                                .withCost(new PercentValueLimiter.Builder().withMinValue(10000L).withMaxValue(15000L).build())
                                .withWeight(new ValueLimiter<>(10.00, 20.00))
                                .withChargeableWeight(new ValueLimiter<>(2.00, 10.00))
                                .withDimension(new ValueLimiter<>(10.00, 20.00))
                                .withCarrierIds(Sets.newHashSet(1L, 2L))
                                .withDeliveryTypes(Sets.newHashSet(YaDeliveryTariffType.COURIER, YaDeliveryTariffType.POST))
                                .withDeliveryDestinations(Sets.newHashSet(213, 2120999))
                                .build(), false),
                Arguments.of("Не проходит по условию региона доставки.",
                        new Condition.Builder()
                                .withCost(new PercentValueLimiter.Builder().withMinValue(10000L).withMaxValue(15000L).build())
                                .withWeight(new ValueLimiter<>(10.00, 20.00))
                                .withChargeableWeight(new ValueLimiter<>(2.00, 10.00))
                                .withDimension(new ValueLimiter<>(10.00, 20.00))
                                .withCarrierIds(Sets.newHashSet(1L, 2L))
                                .withDeliveryTypes(Sets.newHashSet(YaDeliveryTariffType.COURIER, YaDeliveryTariffType.POST))
                                .withDeliveryDestinations(Sets.newHashSet(241))
                                .build(), false),
                Arguments.of("Соответствует фильтру. Проверка корректности при граничных значениях.",
                        new Condition.Builder()
                                .withCost(new PercentValueLimiter.Builder().withMinValue(12749L).withMaxValue(12850L).build())
                                .withWeight(new ValueLimiter<>(14.99, 15.00))
                                .withChargeableWeight(new ValueLimiter<>(8.99, 9.00))
                                .withDimension(new ValueLimiter<>(12.09, 13.53))
                                .withCarrierIds(Sets.newHashSet(5L))
                                .withDeliveryTypes(Sets.newHashSet(YaDeliveryTariffType.COURIER))
                                .withDeliveryDestinations(Sets.newHashSet(10000))
                                .build(), true)
        );
    }

    private static Condition createCondition() {
        return new Condition.Builder()
                .withCost(new PercentValueLimiter.Builder().withMinValue(10000L).withMaxValue(15000L).build())
                .withWeight(new ValueLimiter<>(10.00, 20.00))
                .withChargeableWeight(new ValueLimiter<>(2.00, 10.00))
                .withDimension(new ValueLimiter<>(10.00, 20.00))
                .withCarrierIds(Sets.newHashSet(1L, 2L, 3L, 4L, 5L))
                .withDeliveryTypes(Sets.newHashSet(YaDeliveryTariffType.COURIER, YaDeliveryTariffType.POST))
                .withDeliveryDestinations(Sets.newHashSet(213, 120999))
                .build();
    }

    private static DeliveryModifierAvailabilityFilter createFilter(Long offerCost) {
        return new DeliveryModifierAvailabilityFilter.Builder()
                .withOfferWidth(12.10)
                .withOfferHeight(13.50)
                .withOfferLength(13.53)
                .withOfferWeight(15.00)
                .withOfferChargeableWeight(9.00)
                .withOfferCostInPenny(offerCost)
                .withDeliveryType(YaDeliveryTariffType.COURIER)
                .withDeliveryCarrierId(5L)
                .withTargetRegionIds(Set.of(213))
                .build();
    }

}
