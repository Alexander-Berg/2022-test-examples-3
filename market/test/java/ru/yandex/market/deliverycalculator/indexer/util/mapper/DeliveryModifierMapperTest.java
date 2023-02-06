package ru.yandex.market.deliverycalculator.indexer.util.mapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.google.common.collect.Sets;
import org.hamcrest.MatcherAssert;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.util.ReflectionAssertMatcher;
import ru.yandex.market.deliverycalculator.DeliveryModifierProtos.DeliveryServicesModificationRule;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.YaDeliveryTariffTypeDTO;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.modifier.ActionDto;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.modifier.ConditionDto;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.modifier.DeliveryCostConditionDto;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.modifier.DeliveryModifierDto;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.modifier.PercentValueLimiterDto;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.modifier.ValueLimiterDto;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.modifier.ValueModificationRuleDto;
import ru.yandex.market.deliverycalculator.model.YaDeliveryTariffType;
import ru.yandex.market.deliverycalculator.storage.model.DeliveryServiceCode;
import ru.yandex.market.deliverycalculator.storage.model.modifier.Action;
import ru.yandex.market.deliverycalculator.storage.model.modifier.Condition;
import ru.yandex.market.deliverycalculator.storage.model.modifier.DeliveryCostCondition;
import ru.yandex.market.deliverycalculator.storage.model.modifier.DeliveryModifier;
import ru.yandex.market.deliverycalculator.storage.model.modifier.DeliveryModifiersMeta;
import ru.yandex.market.deliverycalculator.storage.model.modifier.DeliveryModifiersMeta.CarrierAvailabilityModifierMeta;
import ru.yandex.market.deliverycalculator.storage.model.modifier.DeliveryModifiersMeta.ServicesModifierMeta;
import ru.yandex.market.deliverycalculator.storage.model.modifier.DeliveryModifiersMeta.ValueModifierMeta;
import ru.yandex.market.deliverycalculator.storage.model.modifier.ValueLimiter;
import ru.yandex.market.deliverycalculator.storage.model.modifier.ValueModificationRule;
import ru.yandex.market.deliverycalculator.workflow.util.converter.BigDecimalConverter;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.deliverycalculator.DeliveryModifierProtos.ComparisonOperation;
import static ru.yandex.market.deliverycalculator.DeliveryModifierProtos.CostModificationRule;
import static ru.yandex.market.deliverycalculator.DeliveryModifierProtos.ModifierCondition;
import static ru.yandex.market.deliverycalculator.DeliveryModifierProtos.ShopDeliveryModifiers;
import static ru.yandex.market.deliverycalculator.DeliveryModifierProtos.TimeModificationRule;
import static ru.yandex.market.deliverycalculator.DeliveryModifierProtos.ValueModificationOperation;
import static ru.yandex.market.deliverycalculator.storage.model.modifier.DeliveryCostCondition.ComparisonOperation.MORE;

/**
 * Тест для {@link DeliveryModifierMapper}.
 */
class DeliveryModifierMapperTest {

    /**
     * Тест для {@link DeliveryModifierMapper#modifiersToMeta(List, Map)}
     * Случай: успешный маппинг листа модификаторов
     */
    @Test
    void testModifiersToMeta_successfulMapping() {
        /*
        Given
         */
        DeliveryModifier costUnconditionalModifier = createTestCostUnconditionalModifier();
        DeliveryModifier costConditionalModifier = createTestCostConditionalModifier();
        DeliveryModifier timeModifierHighPrio = createTestTimeModifier(false);
        DeliveryModifier timeModifierLowPrio = createTestTimeModifier(true);
        DeliveryModifier serviceUnconditionalModifier = createTestUnconditionalServiceModifier();
        DeliveryModifier switcherConditionalModifier = createTestConditionalSwitcherModifier();

        Map<Long, Integer> modifiersPriorityMap = new HashMap<>();
        modifiersPriorityMap.put(1L, 6);
        modifiersPriorityMap.put(2L, 16);
        modifiersPriorityMap.put(3L, 4);
        modifiersPriorityMap.put(4L, 3);
        modifiersPriorityMap.put(5L, 6);
        modifiersPriorityMap.put(6L, 1);
        /*
        When
         */
        DeliveryModifiersMeta actual = DeliveryModifierMapper.modifiersToMeta(asList(costUnconditionalModifier,
                costConditionalModifier, timeModifierHighPrio, timeModifierLowPrio,
                serviceUnconditionalModifier, switcherConditionalModifier), modifiersPriorityMap);

        /*
        Then
         */
        assertNotNull(actual);

        //проверяем модификаторы цен
        assertNotNull(actual.getCostModifiers());
        MatcherAssert.assertThat(actual.getCostModifiers(),
                new ReflectionAssertMatcher<>(
                        asList(createExpectedMetaForUnconditionalCostModifier(),
                                createExpectedMetaForConditionalCostModifier())));

        //проверяем модификаторы срока доставки
        assertNotNull(actual.getTimeModifiers());
        MatcherAssert.assertThat(actual.getTimeModifiers(),
                new ReflectionAssertMatcher<>(
                        asList(createExpectedMetaForTimeModifierWithHighPrio(),
                                createExpectedMetaForTimeModifierWithLowPrio())));

        //проверяем модификаторы на сервисы
        assertNotNull(actual.getServicesModifiers());
        MatcherAssert.assertThat(actual.getServicesModifiers(),
                new ReflectionAssertMatcher<>(
                        asList(createExpectedMetaForUncoditionalServiceModifier())));

        //проверяем модификаторы на вкл/выкл СД
        assertNotNull(actual.getCarrierAvailabilityModifiers());
        MatcherAssert.assertThat(actual.getCarrierAvailabilityModifiers(),
                new ReflectionAssertMatcher<>(
                        asList(createExpectedMetaForConditionalSwitcherModifier())));
    }

    /**
     * Тест для {@link DeliveryModifierMapper#modifiersToMeta(List, Map)}
     * Случай: маппинг нулла
     */
    @Test
    void testModifiersToMeta_nullListMapping() {
        assertNull(DeliveryModifierMapper.modifiersToMeta(null, new HashMap<>()));
    }

    /**
     * Тест для {@link DeliveryModifierMapper#modifiersToMeta(List, Map)}
     * Случай: маппинг пустого листа модификаторов
     */
    @Test
    void testModifiersToMeta_emptyListMapping() {
        assertNull(DeliveryModifierMapper.modifiersToMeta(new ArrayList<>(), new HashMap<>()));
    }

    /**
     * Тест для {@link DeliveryModifierMapper#modifiersToMeta(List, Map)}
     * Случай: маппинг незаспличенных модификаторов (модификаторов, которые содержат больше одного правила модифкации)
     */
    @Test
    void testModifiersToMeta_mappingOfNonSplitModifiers() {
        assertThrows(IllegalArgumentException.class, () -> {
            DeliveryModifierMapper.modifiersToMeta(asList(createTestCompositeModifier()), new HashMap<>());
        });
    }

    /**
     * Тест для {@link DeliveryModifierMapper#mapDtoToModel(DeliveryModifierDto)}.
     * Случай: все поля не налл
     */
    @Test
    void testMapModifierWithAllFieldsNotNull() {
        DeliveryModifierDto dto = createTestModifierDtoWithAllFieldsSet();

        DeliveryModifier actual = DeliveryModifierMapper.mapDtoToModel(dto);

        assertNotNull(actual);
        assertEquals(dto.getId(), actual.getId());
        assertEquals(dto.getTimestamp(), actual.getTimestamp());

        assertNotNull(actual.getAction());

        //validate cost modification rule
        assertNotNull(actual.getAction().getCostModificationRule());
        ValueModificationRule actualCostModificationRule = actual.getAction().getCostModificationRule();
        ValueModificationRuleDto dtoCostModificationRule = dto.getAction().getCostModificationRule();
        assertEquals(ValueModificationRule.OperationEnum.ADD, actualCostModificationRule.getOperation());
        assertEquals(getDoubleValue(dtoCostModificationRule.getParameter()), actualCostModificationRule.getParameter());
        assertNotNull(actualCostModificationRule.getResultLimit());
        assertValueLimiter(dtoCostModificationRule.getResultLimit(), actualCostModificationRule.getResultLimit(),
                BigDecimalConverter::toLong);

        //validate delivery date modification rule
        assertNotNull(actual.getAction().getTimeModificationRule());
        ValueModificationRule actualtimeModificationRule = actual.getAction().getTimeModificationRule();
        ValueModificationRuleDto dtoTimeModificationRule = dto.getAction().getTimeModificationRule();
        assertEquals(ValueModificationRule.OperationEnum.MULTIPLY, actualtimeModificationRule.getOperation());
        assertEquals(getDoubleValue(dtoTimeModificationRule.getParameter()), actualtimeModificationRule.getParameter());
        assertNull(actualtimeModificationRule.getResultLimit());

        //validate other actions
        assertTrue(actual.getAction().isCarrierTurnedOn());
        assertEquals(Sets.newHashSet(DeliveryServiceCode.INSURANCE, DeliveryServiceCode.CASH_SERVICE),
                actual.getAction().getPaidByCustomerServices());

        //validate conditions
        assertNotNull(actual.getCondition());
        assertValueLimiter(dto.getCondition().getWeight(), actual.getCondition().getWeight(), this::getDoubleValue);
        assertValueLimiter(dto.getCondition().getChargeableWeight(), actual.getCondition().getChargeableWeight(), this::getDoubleValue);
        assertValueLimiter(dto.getCondition().getDimension(), actual.getCondition().getDimension(), this::getDoubleValue);
        assertEquals(Sets.newHashSet(YaDeliveryTariffType.COURIER, YaDeliveryTariffType.PICKUP),
                actual.getCondition().getDeliveryTypes());
        assertEquals(dto.getCondition().getCarrierIds(), actual.getCondition().getCarrierIds());
        assertEquals(dto.getCondition().getDeliveryDestinations(), actual.getCondition().getDeliveryDestinations());
        assertNotNull(actual.getCondition().getCost());
        assertValueLimiter(dto.getCondition().getCost(), actual.getCondition().getCost(), BigDecimalConverter::toLong);
        assertEquals(getDoubleValue(dto.getCondition().getCost().getPercent()), actual.getCondition().getCost().getPercent());
        assertNotNull(actual.getCondition().getDeliveryCost());
        assertDeliveryCostCondition(actual.getCondition().getDeliveryCost(), dto.getCondition().getDeliveryCost());
    }

    /**
     * Тест для  {@link DeliveryModifierMapper#mapDtoToModel(DeliveryModifierDto)}.
     * Случай: маппинг модификатора, включающего определенную СД для некоторого направления доставки
     */
    @Test
    public void testMapModifierWithSomeFieldsNull() {
        DeliveryModifierDto dto = createModifierTurningOffServiceForRegions();

        DeliveryModifier actual = DeliveryModifierMapper.mapDtoToModel(dto);

        assertNotNull(actual);
        assertEquals(dto.getId(), actual.getId());
        assertEquals(dto.getTimestamp(), actual.getTimestamp());

        //validate actions
        assertNotNull(actual.getAction());
        assertNull(actual.getAction().getCostModificationRule());
        assertNull(actual.getAction().getTimeModificationRule());
        assertFalse(actual.getAction().isCarrierTurnedOn());
        assertNull(actual.getAction().getPaidByCustomerServices());

        //validate conditions
        assertNotNull(actual.getCondition());
        assertValueLimiter(dto.getCondition().getWeight(), actual.getCondition().getWeight(), this::getDoubleValue);
        assertValueLimiter(dto.getCondition().getChargeableWeight(), actual.getCondition().getChargeableWeight(), this::getDoubleValue);
        assertValueLimiter(dto.getCondition().getDimension(), actual.getCondition().getDimension(), this::getDoubleValue);
        assertNull(actual.getCondition().getDeliveryTypes());
        assertNull(actual.getCondition().getCarrierIds());
        assertEquals(dto.getCondition().getDeliveryDestinations(), actual.getCondition().getDeliveryDestinations());
        assertValueLimiter(dto.getCondition().getCost(), actual.getCondition().getCost(), BigDecimalConverter::toLong);
        assertNull(dto.getCondition().getDeliveryCost());
    }

    /**
     * Тест для {@link DeliveryModifierMapper#modifiersToProto(long, List)}.
     * Случай: успешный процессинг модификаторов.
     */
    @Test
    void testMapToProto_successful() {
        List<DeliveryModifier> modifiers = asList(createTestCostConditionalModifier(),
                createTestTimeModifier(false),
                createTestUnconditionalServiceModifier(),
                createTestConditionalSwitcherModifier());

        ShopDeliveryModifiers actual = DeliveryModifierMapper.modifiersToProto(1L, modifiers);

        assertNotNull(actual);
        assertEquals(1L, actual.getGenerationId());
        assertEquals(4, actual.getModifiersCount());

        assertCostUnconditionalModifierProto(actual.getModifiers(0));
        assertTimeModifierWithLowPrioProto(actual.getModifiers(1));
        assertUnconditionalServiceModifierProto(actual.getModifiers(2));
        assertConditionalSwitcherModifierProto(actual.getModifiers(3));
    }

    /**
     * Тест для {@link DeliveryModifierMapper#modifiersToProto(long, List)}.
     * Случай: маппинг композитного модификатора.
     */
    @Test
    void testMapToProto_containsCompositeModifier() {
        assertThrows(IllegalArgumentException.class, () -> {
            DeliveryModifierMapper.modifiersToProto(1L, asList(createTestCompositeModifier()));
        });
    }

    private <T> void assertValueLimiter(ValueLimiterDto transformed,
                                        ValueLimiter actualValue,
                                        Function<BigDecimal, T> valueTransformer) {
        if (transformed != null) {
            assertNotNull(actualValue);
        } else {
            assertNull(actualValue);
            return;
        }

        assertEquals(valueTransformer.apply(transformed.getMinValue()), actualValue.getMinValue());
        assertEquals(valueTransformer.apply(transformed.getMaxValue()), actualValue.getMaxValue());
    }

    private DeliveryModifierDto createTestModifierDtoWithAllFieldsSet() {
        ActionDto action = new ActionDto.Builder()
                .withCostModificationRule(new ValueModificationRuleDto.Builder()
                        .withOperation(ValueModificationRuleDto.OperationEnum.ADD)
                        .withParameter(BigDecimal.valueOf(100))
                        .withResultLimit(new ValueLimiterDto(BigDecimal.valueOf(1200), BigDecimal.valueOf(1300)))
                        .build())
                .withTimeModificationRule(new ValueModificationRuleDto.Builder()
                        .withOperation(ValueModificationRuleDto.OperationEnum.MULTIPLY)
                        .withParameter(BigDecimal.valueOf(3))
                        .build())
                .withIsCarrierTurnedOn(true)
                .withPaidByCustomerServices(new HashSet<>(asList(ActionDto.DeliveryServiceCode.INSURANCE,
                        ActionDto.DeliveryServiceCode.CASH_SERVICE)))
                .build();
        ConditionDto condition = new ConditionDto.Builder()
                .withCost(new PercentValueLimiterDto.Builder()
                        .withMaxValue(BigDecimal.valueOf(10000))
                        .withMinValue(BigDecimal.valueOf(500))
                        .withPercent(BigDecimal.valueOf(100))
                        .build())
                .withDeliveryCost(new DeliveryCostConditionDto.Builder()
                        .withPercentFromOfferPrice(10.0)
                        .withComparisonOperation(DeliveryCostConditionDto.ComparisonOperation.MORE)
                        .build())
                .withWeight(new ValueLimiterDto(BigDecimal.valueOf(35.5), BigDecimal.valueOf(37.8)))
                .withChargeableWeight(new ValueLimiterDto(BigDecimal.valueOf(36), BigDecimal.valueOf(38)))
                .withDimension(new ValueLimiterDto(BigDecimal.valueOf(10), BigDecimal.valueOf(20)))
                .withDeliveryTypes(Sets.newHashSet(YaDeliveryTariffTypeDTO.COURIER,
                        YaDeliveryTariffTypeDTO.PICKUP))
                .withCarrierIds(Sets.newHashSet(1L, 2L))
                .withDeliveryDestinations(Sets.newHashSet(223, 224))
                .build();

        return new DeliveryModifierDto.Builder()
                .withId(1L)
                .withTimestamp(2L)
                .withAction(action)
                .withCondition(condition)
                .build();
    }

    @NotNull
    private DeliveryModifier createTestCompositeModifier() {
        Action action = new Action.Builder()
                .withIsCarrierTurnedOn(true)
                .withPaidByCustomerServices(Sets.newHashSet(DeliveryServiceCode.CASH_SERVICE))
                .build();

        return new DeliveryModifier.Builder()
                .withAction(action)
                .withId(2L)
                .build();
    }

    @NotNull
    private DeliveryModifier createTestTimeModifier(boolean withHighestPossiblePrio) {
        Condition.Builder conditionBuilder = new Condition.Builder()
                .withWeight(new ValueLimiter<>(10.00, 20.00))
                .withCarrierIds(Sets.newHashSet(1L));
        Action.Builder actionBuilder = new Action.Builder()
                .withTimeModificationRule(new ValueModificationRule.Builder<Integer>()
                        .withOperation(ValueModificationRule.OperationEnum.UNKNOWN_VALUE)
                        .build());

        if (withHighestPossiblePrio) {
            conditionBuilder.withDeliveryDestinations(Sets.newHashSet(1));
        }

        return new DeliveryModifier.Builder()
                .withAction(actionBuilder.build())
                .withCondition(conditionBuilder.build())
                .withId(!withHighestPossiblePrio ? 3L : 4L)
                .build();
    }

    @NotNull
    private ValueModifierMeta<Integer> createExpectedMetaForTimeModifierWithHighPrio() {
        ValueModificationRule<Integer> modificationRule = new ValueModificationRule.Builder<Integer>()
                .withOperation(ValueModificationRule.OperationEnum.UNKNOWN_VALUE)
                .build();
        Condition condition = new Condition.Builder()
                .withCarrierIds(Sets.newHashSet(1L))
                .withWeight(new ValueLimiter<>(10.00, 20.00))
                .withDeliveryDestinations(Sets.newHashSet(1))
                .build();

        return new ValueModifierMeta.Builder<Integer>()
                .withPriority(3)
                .withModificationRule(modificationRule)
                .withCondition(condition)
                .withId(4L)
                .build();
    }

    @NotNull
    private ValueModifierMeta<Integer> createExpectedMetaForTimeModifierWithLowPrio() {
        ValueModificationRule<Integer> modificationRule = new ValueModificationRule.Builder<Integer>()
                .withOperation(ValueModificationRule.OperationEnum.UNKNOWN_VALUE)
                .build();
        Condition condition = new Condition.Builder()
                .withWeight(new ValueLimiter<>(10.00, 20.00))
                .withCarrierIds(Sets.newHashSet(1L))
                .build();

        return new ValueModifierMeta.Builder<Integer>()
                .withPriority(4)
                .withModificationRule(modificationRule)
                .withCondition(condition)
                .withId(3L)
                .build();
    }

    private void assertTimeModifierWithLowPrioProto(ru.yandex.market.deliverycalculator.DeliveryModifierProtos.DeliveryModifier deliveryModifier) {
        assertNotNull(deliveryModifier);
        assertTrue(deliveryModifier.hasAction());
        assertTrue(deliveryModifier.getAction().hasTimeModificationRule());

        TimeModificationRule timeModificationRule = deliveryModifier.getAction().getTimeModificationRule();
        assertEquals(ValueModificationOperation.UNKNOWN_VALUE, timeModificationRule.getOperation());
        assertFalse(timeModificationRule.hasParameter());
        assertFalse(timeModificationRule.hasResultLimit());

        assertFalse(deliveryModifier.hasCondition());
    }

    @NotNull
    private DeliveryModifier createTestUnconditionalServiceModifier() {
        return new DeliveryModifier.Builder()
                .withAction(new Action.Builder()
                        .withPaidByCustomerServices(Sets.newHashSet(DeliveryServiceCode.CASH_SERVICE))
                        .build())
                .withId(5L)
                .build();
    }

    @NotNull
    private ServicesModifierMeta createExpectedMetaForUncoditionalServiceModifier() {
        return new ServicesModifierMeta.Builder()
                .withId(5L)
                .withPaidByCustomerServices(Sets.newHashSet(DeliveryServiceCode.CASH_SERVICE))
                .withPriority(6)
                .build();
    }

    private void assertUnconditionalServiceModifierProto(ru.yandex.market.deliverycalculator.DeliveryModifierProtos.DeliveryModifier deliveryModifier) {
        assertNotNull(deliveryModifier);
        assertTrue(deliveryModifier.hasAction());
        assertTrue(deliveryModifier.getAction().hasServicesModificationRule());

        DeliveryServicesModificationRule serviceRule = deliveryModifier.getAction().getServicesModificationRule();
        assertEquals(1, serviceRule.getPayedByCustomerServicesCount());
        assertEquals("CASH_SERVICE", serviceRule.getPayedByCustomerServices(0));

        assertFalse(deliveryModifier.hasCondition());
    }

    @NotNull
    private DeliveryModifier createTestConditionalSwitcherModifier() {
        return new DeliveryModifier.Builder()
                .withAction(new Action.Builder()
                        .withIsCarrierTurnedOn(true)
                        .build())
                .withId(6L)
                .withCondition(new Condition.Builder()
                        .withCarrierIds(Sets.newHashSet(1L))
                        .withDeliveryDestinations(Sets.newHashSet(2))
                        .build())
                .build();
    }

    @NotNull
    private CarrierAvailabilityModifierMeta createExpectedMetaForConditionalSwitcherModifier() {
        Condition condition = new Condition.Builder()
                .withCarrierIds(Sets.newHashSet(1L))
                .withDeliveryDestinations(Sets.newHashSet(2))
                .build();

        return new CarrierAvailabilityModifierMeta.Builder()
                .withPriority(1)
                .withId(6L)
                .withIsCarrierAvailable(true)
                .withCondition(condition)
                .build();
    }

    private void assertConditionalSwitcherModifierProto(ru.yandex.market.deliverycalculator.DeliveryModifierProtos.DeliveryModifier deliveryModifier) {
        assertNotNull(deliveryModifier);
        assertTrue(deliveryModifier.hasAction());
        assertTrue(deliveryModifier.getAction().hasRegionsAvailable());
        assertTrue(deliveryModifier.getAction().getRegionsAvailable());

        assertTrue(deliveryModifier.hasCondition());
        assertEquals(1, deliveryModifier.getCondition().getRegionsCount());
        assertEquals(2L, deliveryModifier.getCondition().getRegions(0));
    }

    @NotNull
    private DeliveryModifier createTestCostConditionalModifier() {
        Action.Builder actionBuilder = new Action.Builder()
                .withCostModificationRule(new ValueModificationRule.Builder<Long>()
                        .withOperation(ValueModificationRule.OperationEnum.SUBSTRACT)
                        .withParameter(10000.00)
                        .withResultLimit(new ValueLimiter<>(10000L, 120000L))
                        .build());
        Condition.Builder conditionBuilder = new Condition.Builder()
                .withDeliveryCost(new DeliveryCostCondition.Builder()
                        .withComparisonOperation(MORE)
                        .withPercentFromOfferPrice(10.00)
                        .build())
                .withCarrierIds(Sets.newHashSet(1L));

        return new DeliveryModifier.Builder()
                .withAction(actionBuilder.build())
                .withCondition(conditionBuilder.build())
                .withId(2L)
                .build();
    }

    @NotNull
    private DeliveryModifier createTestCostUnconditionalModifier() {
        Action.Builder actionBuilder = new Action.Builder()
                .withCostModificationRule(new ValueModificationRule.Builder<Long>()
                        .withOperation(ValueModificationRule.OperationEnum.ADD)
                        .withParameter(10000.00)
                        .build());

        return new DeliveryModifier.Builder()
                .withAction(actionBuilder.build())
                .withId(1L)
                .build();
    }

    @NotNull
    private ValueModifierMeta<Long> createExpectedMetaForConditionalCostModifier() {
        ValueModificationRule<Long> valueModificationRule = new ValueModificationRule.Builder<Long>()
                .withOperation(ValueModificationRule.OperationEnum.SUBSTRACT)
                .withParameter(10000.00)
                .withResultLimit(new ValueLimiter<>(10000L, 120000L))
                .build();
        Condition condition = new Condition.Builder()
                .withDeliveryCost(new DeliveryCostCondition.Builder()
                        .withComparisonOperation(MORE)
                        .withPercentFromOfferPrice(10.00)
                        .build())
                .withCarrierIds(Sets.newHashSet(1L))
                .build();

        return new ValueModifierMeta.Builder<Long>()
                .withPriority(16)
                .withModificationRule(valueModificationRule)
                .withCondition(condition)
                .withId(2L)
                .build();
    }

    private void assertCostUnconditionalModifierProto(ru.yandex.market.deliverycalculator.DeliveryModifierProtos.DeliveryModifier deliveryModifier) {
        assertNotNull(deliveryModifier);
        assertTrue(deliveryModifier.hasAction());
        assertTrue(deliveryModifier.getAction().hasCostModificationRule());

        CostModificationRule costModificationRule = deliveryModifier.getAction().getCostModificationRule();
        assertEquals(ValueModificationOperation.ADD, costModificationRule.getOperation());
        assertTrue(costModificationRule.hasParameter());
        assertEquals(-10000L, costModificationRule.getParameter());
        assertTrue(costModificationRule.hasResultLimit());
        assertEquals(10000L, costModificationRule.getResultLimit().getMinValue());
        assertEquals(120000L, costModificationRule.getResultLimit().getMaxValue());

        assertTrue(deliveryModifier.hasCondition());
        ModifierCondition condition = deliveryModifier.getCondition();
        assertEquals(0, condition.getRegionsCount());
        assertTrue(condition.hasDeliveryCost());
        assertEquals(ComparisonOperation.MORE, condition.getDeliveryCost().getComparisonOperation());
        assertTrue(condition.getDeliveryCost().hasPercentFromOfferPrice());
        assertEquals(10.00, condition.getDeliveryCost().getPercentFromOfferPrice());
    }

    @NotNull
    private ValueModifierMeta<Long> createExpectedMetaForUnconditionalCostModifier() {
        ValueModificationRule<Long> valueModificationRule = new ValueModificationRule.Builder<Long>()
                .withOperation(ValueModificationRule.OperationEnum.ADD)
                .withParameter(10000.00)
                .build();

        return new ValueModifierMeta.Builder<Long>()
                .withPriority(6)
                .withModificationRule(valueModificationRule)
                .withId(1L)
                .build();
    }

    private void assertDeliveryCostCondition(DeliveryCostCondition actual, DeliveryCostConditionDto dto) {
        if (dto == null) {
            assertNull(actual);
            return;
        }

        assertNotNull(actual);
        assertEquals(actual.getPercentFromOfferPrice(), dto.getPercentFromOfferPrice());
        switch (dto.getComparisonOperation()) {
            case LESS:
                assertEquals(DeliveryCostCondition.ComparisonOperation.LESS, actual.getComparisonOperation());
                break;
            case MORE:
                assertEquals(DeliveryCostCondition.ComparisonOperation.MORE, actual.getComparisonOperation());
                break;
            case EQUAL:
                assertEquals(DeliveryCostCondition.ComparisonOperation.EQUAL, actual.getComparisonOperation());
                break;
            default:
                throw new RuntimeException("Unknown comparison operation type");
        }
    }

    private DeliveryModifierDto createModifierTurningOffServiceForRegions() {
        ConditionDto condition = new ConditionDto.Builder()
                .withDeliveryDestinations(Sets.newHashSet(223, 224))
                .build();
        ActionDto action = new ActionDto.Builder()
                .withIsCarrierTurnedOn(false)
                .build();

        return new DeliveryModifierDto.Builder()
                .withId(1L)
                .withTimestamp(2L)
                .withAction(action)
                .withCondition(condition)
                .build();
    }

    private Double getDoubleValue(BigDecimal source) {
        return source == null ? null : source.doubleValue();
    }
}
