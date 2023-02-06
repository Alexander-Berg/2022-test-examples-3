package ru.yandex.market.deliverycalculator.indexer.service;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.factory.ResourceLocationFactory;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.deliverycalculator.indexer.FunctionalTest;
import ru.yandex.market.deliverycalculator.model.YaDeliveryTariffType;
import ru.yandex.market.deliverycalculator.storage.model.DeliveryServiceCode;
import ru.yandex.market.deliverycalculator.storage.model.metastorage.Generation;
import ru.yandex.market.deliverycalculator.storage.model.modifier.Action;
import ru.yandex.market.deliverycalculator.storage.model.modifier.Condition;
import ru.yandex.market.deliverycalculator.storage.model.modifier.DeliveryModifier;
import ru.yandex.market.deliverycalculator.storage.model.modifier.PercentValueLimiter;
import ru.yandex.market.deliverycalculator.storage.model.modifier.ValueLimiter;
import ru.yandex.market.deliverycalculator.storage.model.modifier.ValueModificationRule;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.deliverycalculator.storage.model.modifier.DeliveryModifier.ModifierType.CARRIER_SWITCH;
import static ru.yandex.market.deliverycalculator.storage.model.modifier.DeliveryModifier.ModifierType.COST;
import static ru.yandex.market.deliverycalculator.storage.model.modifier.DeliveryModifier.ModifierType.SERVICES;
import static ru.yandex.market.deliverycalculator.storage.model.modifier.DeliveryModifier.ModifierType.TIME;

/**
 * Тест для {@link DeliveryModifierService}.
 */
class DeliveryModifierServiceTest extends FunctionalTest {

    public static final String BUCKET_URL = "http://some.url.here";

    @Autowired
    private DeliveryModifierService tested;
    @Autowired
    private MdsS3Client mockedMdsClient;
    @Autowired
    private ResourceLocationFactory mockedLocationFactory;

    @Test
    @DbUnitDataSet(before = "regions.csv")
    void testModifiersPriorityCalculation() {
        Map<Long, Integer> modifiersPriorities = tested.calculatePrioritiesForModifiers(createTestModifiersForPrioCalculation());

        assertNotNull(modifiersPriorities);
        assertEquals(10, modifiersPriorities.size());
        assertEquals(Integer.valueOf(1), modifiersPriorities.get(1L));
        assertEquals(Integer.valueOf(2), modifiersPriorities.get(2L));
        assertEquals(Integer.valueOf(3), modifiersPriorities.get(3L));
        assertEquals(Integer.valueOf(7), modifiersPriorities.get(4L));
        assertEquals(Integer.valueOf(8), modifiersPriorities.get(5L));
        assertEquals(Integer.valueOf(9), modifiersPriorities.get(6L));
        assertEquals(Integer.valueOf(11), modifiersPriorities.get(7L));
        assertEquals(Integer.valueOf(15), modifiersPriorities.get(8L));
        assertEquals(Integer.valueOf(16), modifiersPriorities.get(9L));
        assertEquals(Integer.valueOf(16), modifiersPriorities.get(10L));
    }

    /**
     * Тест для {@link DeliveryModifierService#splitModifiers(java.util.Collection)}.
     * Случай: нулл передан как параметр
     */
    @Test
    void testSplitModifiers_nullHandling() {
        List<DeliveryModifier> splitModifiers = tested.splitModifiers(null);

        assertNotNull(splitModifiers);
        assertEquals(0, splitModifiers.size());
    }

    /**
     * Тест для {@link DeliveryModifierService#splitModifiers(java.util.Collection)}.
     * Случай: пустой лист передан как параметр
     */
    @Test
    void testSplitModifiers_emptyListHandling() {
        List<DeliveryModifier> splitModifiers = tested.splitModifiers(new ArrayList<>());

        assertNotNull(splitModifiers);
        assertEquals(0, splitModifiers.size());
    }

    /**
     * Тест для {@link DeliveryModifierService#splitModifiers(java.util.Collection)}.
     * Случай: успешная обработка листа
     */
    @Test
    @DbUnitDataSet(before = "regions.csv")
    void testSplitModifiers_successfulCase() {
        DeliveryModifier modifier1 = createFirstTestModifier();
        DeliveryModifier modifier2 = createSecondTestModifier();
        DeliveryModifier modifier3 = createThirdTestModifier();

        List<DeliveryModifier> splitModifiers = tested.splitModifiers(asList(modifier1, modifier2, modifier3));

        assertNotNull(splitModifiers);
        assertEquals(9, splitModifiers.size());

        assertSplitModifier(splitModifiers.get(0), modifier1, COST);
        assertSplitModifier(splitModifiers.get(1), modifier1, TIME);
        assertSplitModifier(splitModifiers.get(2), modifier1, SERVICES);
        assertSplitModifier(splitModifiers.get(3), modifier2, COST);
        assertSplitModifier(splitModifiers.get(4), modifier2, CARRIER_SWITCH);
        assertSplitModifier(splitModifiers.get(5), modifier2, COST);
        assertSplitModifier(splitModifiers.get(6), modifier2, CARRIER_SWITCH);
        assertSplitModifier(splitModifiers.get(7), modifier3, COST);
        assertSplitModifier(splitModifiers.get(8), modifier3, TIME);
    }

    /**
     * Тест для {@link DeliveryModifierService#uploadToMds(List, Generation)}.
     * Случай: успешное сохранение информации о модификаторах
     *
     * @throws MalformedURLException - в случае если создание тестового урла бакета произошло с ошибкой
     */
    @Test
    @DbUnitDataSet(after = "uploadModifiersToMds.after.csv")
    void testUploadToMds_modifiersNotNull() throws MalformedURLException {
        ResourceLocation resourceLocation = ResourceLocation.create("bucket", "key");
        doReturn(resourceLocation).when(mockedLocationFactory).createLocation("buckets/modifiers/gen-3-3.pb.sn");
        doReturn(new URL(BUCKET_URL)).when(mockedMdsClient).getUrl(resourceLocation);

        DeliveryModifier modifier = new DeliveryModifier.Builder()
                .withId(2L)
                .withTimestamp(2345678901L)
                .withAction(new Action.Builder()
                        .withCostModificationRule(new ValueModificationRule.Builder<Long>()
                                .withOperation(ValueModificationRule.OperationEnum.SUBSTRACT)
                                .withParameter(20000.00)
                                .build())
                        .build())
                .build();

        String actualUrl = tested.uploadToMds(asList(modifier), new Generation(3L, 3L));

        assertEquals(BUCKET_URL, actualUrl);
        verify(mockedMdsClient, times(1)).upload(same(resourceLocation), any());
    }

    /**
     * Тест для {@link DeliveryModifierService#uploadToMds(List, Generation)}.
     * Случай: лист модификаторов - нулл
     */
    @Test
    void testUploadToMds_modifiersNull() {
        String actualUrl = tested.uploadToMds(null, new Generation(3L, 3L));

        assertNull(actualUrl);
        verify(mockedMdsClient, never()).upload(any(), any());
    }

    /**
     * Тест для {@link DeliveryModifierService#uploadToMds(List, Generation)}.
     * Случай: лист модификаторов пустой
     */
    @Test
    void testUploadToMds_modifiersEmpty() {
        String actualUrl = tested.uploadToMds(new ArrayList<>(), new Generation(3L, 3L));

        assertNull(actualUrl);
        verify(mockedMdsClient, never()).upload(any(), any());
    }

    private void assertSplitModifier(DeliveryModifier splitModifier,
                                     DeliveryModifier parentModifier,
                                     DeliveryModifier.ModifierType modifierType) {
        assertNotNull(splitModifier);
        assertInheritedFields(splitModifier, parentModifier);
        assertNotNull(splitModifier.getAction());

        switch (modifierType) {
            case COST:
                assertEquals(parentModifier.getAction().getCostModificationRule(),
                        splitModifier.getAction().getCostModificationRule());
                assertNull(splitModifier.getAction().getPaidByCustomerServices());
                assertNull(splitModifier.getAction().getTimeModificationRule());
                assertNull(splitModifier.getAction().isCarrierTurnedOn());
                break;
            case TIME:
                assertEquals(parentModifier.getAction().getTimeModificationRule(),
                        splitModifier.getAction().getTimeModificationRule());
                assertNull(splitModifier.getAction().getPaidByCustomerServices());
                assertNull(splitModifier.getAction().getCostModificationRule());
                assertNull(splitModifier.getAction().isCarrierTurnedOn());
                break;
            case SERVICES:
                assertEquals(parentModifier.getAction().getPaidByCustomerServices(),
                        splitModifier.getAction().getPaidByCustomerServices());
                assertNull(splitModifier.getAction().getTimeModificationRule());
                assertNull(splitModifier.getAction().getCostModificationRule());
                assertNull(splitModifier.getAction().isCarrierTurnedOn());
                break;
            case CARRIER_SWITCH:
                assertEquals(parentModifier.getAction().isCarrierTurnedOn(),
                        splitModifier.getAction().isCarrierTurnedOn());
                assertNull(splitModifier.getAction().getTimeModificationRule());
                assertNull(splitModifier.getAction().getCostModificationRule());
                assertNull(splitModifier.getAction().getPaidByCustomerServices());
                break;
            default:
                break;
        }
    }

    private void assertInheritedFields(DeliveryModifier splitModifier,
                                       DeliveryModifier parentModifier) {
        assertEquals(Long.valueOf(parentModifier.getId()), splitModifier.getParentId());
        assertEquals(parentModifier.getTimestamp(), splitModifier.getTimestamp());
        if (parentModifier.getCondition() == null) {
            assertNull(splitModifier.getCondition());
        } else {
            assertNotSame(parentModifier.getCondition(), splitModifier.getCondition());
            assertNotNull(splitModifier.getCondition());
            assertEquals(parentModifier.getCondition().getCost(), splitModifier.getCondition().getCost());
            assertEquals(parentModifier.getCondition().getWeight(), splitModifier.getCondition().getWeight());
            assertEquals(parentModifier.getCondition().getChargeableWeight(), splitModifier.getCondition().getChargeableWeight());
            assertEquals(parentModifier.getCondition().getDimension(), splitModifier.getCondition().getDimension());
            assertEquals(parentModifier.getCondition().getCarrierIds(), splitModifier.getCondition().getCarrierIds());
            assertEquals(parentModifier.getCondition().getDeliveryTypes(), splitModifier.getCondition().getDeliveryTypes());
            assertEquals(parentModifier.getCondition().getDeliveryCost(), splitModifier.getCondition().getDeliveryCost());
        }
    }

    @NotNull
    private DeliveryModifier createSecondTestModifier() {
        return new DeliveryModifier.Builder()
                .withId(2L)
                .withTimestamp(2345678901L)
                .withCondition(new Condition.Builder()
                        .withCost(new PercentValueLimiter.Builder()
                                .withMinValue(100000L)
                                .withMaxValue(200000L)
                                .withPercent(10.0)
                                .build())
                        .withDeliveryDestinations(Sets.newHashSet(213, 10776))
                        .build())
                .withAction(new Action.Builder()
                        .withCostModificationRule(new ValueModificationRule.Builder<Long>()
                                .withOperation(ValueModificationRule.OperationEnum.SUBSTRACT)
                                .withParameter(20000.00)
                                .withResultLimit(new ValueLimiter<>(100000L, null))
                                .build())
                        .withIsCarrierTurnedOn(true)
                        .build())
                .build();
    }

    @NotNull
    private DeliveryModifier createThirdTestModifier() {
        return new DeliveryModifier.Builder()
                .withId(3L)
                .withTimestamp(2345678901L)
                .withAction(new Action.Builder()
                        .withCostModificationRule(new ValueModificationRule.Builder<Long>()
                                .withOperation(ValueModificationRule.OperationEnum.SUBSTRACT)
                                .withParameter(20000.00)
                                .withResultLimit(new ValueLimiter<>(100000L, null))
                                .build())
                        .withTimeModificationRule(new ValueModificationRule.Builder<Integer>()
                                .withOperation(ValueModificationRule.OperationEnum.ADD)
                                .withParameter(1.00)
                                .build())
                        .build())
                .build();
    }

    @NotNull
    private DeliveryModifier createFirstTestModifier() {
        return new DeliveryModifier.Builder()
                .withId(1L)
                .withTimestamp(1234567890L)
                .withCondition(new Condition.Builder()
                        .withCost(new PercentValueLimiter.Builder()
                                .withMinValue(1000L)
                                .withMaxValue(2000L)
                                .withPercent(100.0)
                                .build())
                        .withWeight(new ValueLimiter<>(20.0, 30.0))
                        .withDimension(new ValueLimiter<>(30.0, 40.0))
                        .withChargeableWeight(new ValueLimiter<>(40.0, 50.0))
                        .withDeliveryDestinations(Sets.newHashSet(1, 10776))
                        .withCarrierIds(Sets.newHashSet(2L))
                        .withDeliveryTypes(Sets.newHashSet(YaDeliveryTariffType.COURIER))
                        .build())
                .withAction(new Action.Builder()
                        .withCostModificationRule(new ValueModificationRule.Builder<Long>()
                                .withOperation(ValueModificationRule.OperationEnum.SUBSTRACT)
                                .withParameter(20000.00)
                                .withResultLimit(new ValueLimiter<>(100000L, null))
                                .build())
                        .withTimeModificationRule(new ValueModificationRule.Builder<Integer>()
                                .withOperation(ValueModificationRule.OperationEnum.ADD)
                                .withParameter(1.00)
                                .build())
                        .withPaidByCustomerServices(Sets.newHashSet(DeliveryServiceCode.INSURANCE))
                        .build())
                .build();
    }

    private List<DeliveryModifier> createTestModifiersForPrioCalculation() {
        return Arrays.asList(
                //модификатор, действующий на определенную СД при доставке в москву
                new DeliveryModifier.Builder()
                        .withAction(new Action.Builder().withIsCarrierTurnedOn(true).build())
                        .withId(1L)
                        .withTimestamp(0L)
                        .withCondition(new Condition.Builder()
                                .withCarrierIds(Sets.newHashSet(1L))
                                .withDeliveryDestinations(Sets.newHashSet(213))
                                .build())
                        .build(),
                //модификатор, действующий на определенную СД при доставке в Моск. обл.
                new DeliveryModifier.Builder()
                        .withAction(new Action.Builder().withIsCarrierTurnedOn(true).build())
                        .withId(2L)
                        .withTimestamp(0L)
                        .withCondition(new Condition.Builder()
                                .withCarrierIds(Sets.newHashSet(1L))
                                .withDeliveryDestinations(Sets.newHashSet(1))
                                .build())
                        .build(),
                //модификатор, действующий на определенную СД при доставке в центр. фед. окр.
                new DeliveryModifier.Builder()
                        .withAction(new Action.Builder().withIsCarrierTurnedOn(true).build())
                        .withId(3L)
                        .withTimestamp(0L)
                        .withCondition(new Condition.Builder()
                                .withCarrierIds(Sets.newHashSet(1L))
                                .withDeliveryDestinations(Sets.newHashSet(3))
                                .build())
                        .build(),
                //модификатор, действующий на определенную СД при доставке на планету Земля.
                new DeliveryModifier.Builder()
                        .withAction(new Action.Builder().withIsCarrierTurnedOn(true).build())
                        .withId(4L)
                        .withTimestamp(0L)
                        .withCondition(new Condition.Builder()
                                .withCarrierIds(Sets.newHashSet(1L))
                                .withDeliveryDestinations(Sets.newHashSet(10_000))
                                .build())
                        .build(),
                //модификатор, действующий на определенную СД без ограничений на регион доставки.
                new DeliveryModifier.Builder()
                        .withAction(new Action.Builder().withIsCarrierTurnedOn(true).build())
                        .withId(5L)
                        .withTimestamp(0L)
                        .withCondition(new Condition.Builder()
                                .withCarrierIds(Sets.newHashSet(1L))
                                .build())
                        .build(),
                //модификатор, действующий без ограничения на СД, действующий при доставке в Москву.
                new DeliveryModifier.Builder()
                        .withAction(new Action.Builder().withIsCarrierTurnedOn(true).build())
                        .withId(6L)
                        .withTimestamp(0L)
                        .withCondition(new Condition.Builder()
                                .withDeliveryDestinations(Sets.newHashSet(213))
                                .build())
                        .build(),
                //модификатор, действующий без ограничения на СД, действующий при доставке в Центр. фед. окр.
                new DeliveryModifier.Builder()
                        .withAction(new Action.Builder().withIsCarrierTurnedOn(true).build())
                        .withId(7L)
                        .withTimestamp(0L)
                        .withCondition(new Condition.Builder()
                                .withDeliveryDestinations(Sets.newHashSet(3))
                                .build())
                        .build(),
                //модификатор, действующий без ограничения на СД, действующий при доставке на планету Земля
                new DeliveryModifier.Builder()
                        .withAction(new Action.Builder().withIsCarrierTurnedOn(true).build())
                        .withId(8L)
                        .withTimestamp(0L)
                        .withCondition(new Condition.Builder()
                                .withDeliveryDestinations(Sets.newHashSet(10_000))
                                .build())
                        .build(),
                //модификатор, действующий без ограничения на СД, без ограничения на регион
                new DeliveryModifier.Builder()
                        .withAction(new Action.Builder().withIsCarrierTurnedOn(true).build())
                        .withId(9L)
                        .withTimestamp(0L)
                        .withCondition(new Condition.Builder()
                                .withDeliveryTypes(Sets.newHashSet(YaDeliveryTariffType.POST))
                                .build())
                        .build(),
                //безусловный модификатор
                new DeliveryModifier.Builder()
                        .withAction(new Action.Builder().withIsCarrierTurnedOn(true).build())
                        .withId(10L)
                        .withTimestamp(0L)
                        .build()
        );
    }
}
