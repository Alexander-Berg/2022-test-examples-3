package ru.yandex.market.deliverycalculator.storage.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.deliverycalculator.storage.FunctionalTest;
import ru.yandex.market.deliverycalculator.storage.model.metastorage.MardoSenderSettingsGenerationEntity;
import ru.yandex.market.deliverycalculator.storage.model.metastorage.MardoSenderSettingsGenerationEntity.MardoSenderSettingsGenerationEntityId;
import ru.yandex.market.deliverycalculator.storage.model.modifier.Condition;
import ru.yandex.market.deliverycalculator.storage.model.modifier.DeliveryCostCondition;
import ru.yandex.market.deliverycalculator.storage.model.modifier.DeliveryModifiersMeta;
import ru.yandex.market.deliverycalculator.storage.model.modifier.DeliveryModifiersMeta.CarrierAvailabilityModifierMeta;
import ru.yandex.market.deliverycalculator.storage.model.modifier.DeliveryModifiersMeta.ServicesModifierMeta;
import ru.yandex.market.deliverycalculator.storage.model.modifier.DeliveryModifiersMeta.ValueModifierMeta;
import ru.yandex.market.deliverycalculator.storage.model.modifier.PercentValueLimiter;
import ru.yandex.market.deliverycalculator.storage.model.modifier.ValueLimiter;
import ru.yandex.market.deliverycalculator.storage.model.modifier.ValueModificationRule;
import ru.yandex.market.deliverycalculator.storage.model.yadelivery.YaDeliverySenderSettingsMeta;
import ru.yandex.market.deliverycalculator.storage.util.PostgresJsonDataType;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.skyscreamer.jsonassert.JSONCompare.compareJSON;
import static ru.yandex.market.deliverycalculator.model.YaDeliveryTariffType.COURIER;
import static ru.yandex.market.deliverycalculator.storage.model.DeliveryServiceCode.CASH_SERVICE;
import static ru.yandex.market.deliverycalculator.storage.model.DeliveryServiceCode.INSURANCE;
import static ru.yandex.market.deliverycalculator.storage.model.modifier.DeliveryCostCondition.ComparisonOperation.MORE;
import static ru.yandex.market.deliverycalculator.storage.model.modifier.ValueModificationRule.OperationEnum.FIX_VALUE;
import static ru.yandex.market.deliverycalculator.storage.model.modifier.ValueModificationRule.OperationEnum.SUBSTRACT;
import static ru.yandex.market.deliverycalculator.storage.model.modifier.ValueModificationRule.OperationEnum.UNKNOWN_VALUE;

/**
 * Тесты на {@link MardoSenderSettingsGenerationRepository}.
 */
class MardoSenderSettingsGenerationRepositoryTest extends FunctionalTest {

    @Autowired
    private MardoSenderSettingsGenerationRepository tested;
    @Autowired
    private TransactionTemplate transactionTemplate;

    /**
     * Тест для {@link MardoSenderSettingsGenerationRepository#save(Object)}.
     */
    @DbUnitDataSet(
            before = "database/storeSenderSettingsMeta.before.csv",
            after = "database/storeSenderSettingsMeta.after.csv"
    )
    @Test
    void testSave() throws JsonProcessingException {
        tested.save(createTestEntity());
    }

    /**
     * Тест для {@link MardoSenderSettingsGenerationRepository#findById(Object)}.
     */
    @DbUnitDataSet(
            before = "database/storeSenderSettingsMeta.after.csv"
    )
    @Test
    void testFindById() throws JsonProcessingException, JSONException {
        Optional<MardoSenderSettingsGenerationEntity> actual =
                tested.findById(new MardoSenderSettingsGenerationEntityId(1L, 2L));

        assertTrue(actual.isPresent());

        MardoSenderSettingsGenerationEntity expected = createTestEntity();

        assertEquals(expected.getSenderId(), actual.get().getSenderId());
        assertEquals(expected.getGenerationId(), actual.get().getGenerationId());
        assertEquals(expected.isDeleted(), actual.get().isDeleted());
        assertEquals(expected.getModifiersBucketUrl(), actual.get().getModifiersBucketUrl());
        assertEquals(expected.isDeleted(), actual.get().isDeleted());

        JSONCompareResult jsonCompareResult = compareJSON(expected.getSerializedMetaData(),
                actual.get().getSerializedMetaData(),
                JSONCompareMode.NON_EXTENSIBLE);
        assertFalse(jsonCompareResult.failed(), jsonCompareResult.getMessage());
    }

    /**
     * Тест для {@link MardoSenderSettingsGenerationRepository#findByGenerationId(long)}.
     */
    @DbUnitDataSet(
            before = "database/storeSenderSettingsMeta.after.csv"
    )
    @Test
    void testFindByGenerationId() {
        List<MardoSenderSettingsGenerationEntity> actual = tested.findByGenerationId(3L);

        assertNotNull(actual);
        assertEquals(2, actual.size());
        assertTrue(actual.stream().anyMatch(sender -> Long.valueOf(3L).equals(sender.getSenderId())));
        assertTrue(actual.stream().anyMatch(sender -> Long.valueOf(4L).equals(sender.getSenderId())));
    }

    /**
     * Тест для {@link MardoSenderSettingsGenerationRepository#deleteByGenerationIdAndSenderIdIn(long, Set)}.
     */
    @DbUnitDataSet(
            before = "database/deleteExportedSenderSettings.before.csv",
            after = "database/deleteExportedSenderSettings.after.csv"
    )
    @Test
    void testDeleteByGenerationAndSender() {
        transactionTemplate.execute(status -> {
                    tested.deleteByGenerationIdAndSenderIdIn(2L, Sets.newHashSet(1L, 2L));
                    return null;
                }
        );
    }

    @NotNull
    private MardoSenderSettingsGenerationEntity createTestEntity() throws JsonProcessingException {
        YaDeliverySenderSettingsMeta deserializedSenderMeta = new YaDeliverySenderSettingsMeta.Builder()
                .withStartingRegionId(213)
                .withCollaboratingCarrierIds(newHashSet(1L, 2L))
                .withModifiers(new DeliveryModifiersMeta.Builder()
                        .withCostModifiers(asList(
                                new ValueModifierMeta.Builder<Long>()
                                        .withCondition(new Condition.Builder()
                                                .withCost(new PercentValueLimiter.Builder()
                                                        .withMinValue(50000L)
                                                        .withMaxValue(1000000L)
                                                        .withPercent(100.00)
                                                        .build())
                                                .withDeliveryCost(new DeliveryCostCondition.Builder()
                                                        .withComparisonOperation(MORE)
                                                        .withPercentFromOfferPrice(10.00)
                                                        .build())
                                                .withWeight(new ValueLimiter<>(35.5, 37.8))
                                                .withChargeableWeight(new ValueLimiter<>(36.0, 38.0))
                                                .withDimension(new ValueLimiter<>(10.00, 20.00))
                                                .withDeliveryTypes(newHashSet(COURIER))
                                                .withCarrierIds(newHashSet(1L, 2L))
                                                .withDeliveryDestinations(newHashSet(224))
                                                .build())
                                        .withModificationRule(new ValueModificationRule.Builder<Long>()
                                                .withOperation(SUBSTRACT)
                                                .withParameter(20000.00)
                                                .withResultLimit(new ValueLimiter<>(50000L, null))
                                                .build())
                                        .withId(1L)
                                        .withPriority(1)
                                        .build(),
                                new ValueModifierMeta.Builder<Long>()
                                        .withCondition(new Condition.Builder()
                                                .withDeliveryDestinations(newHashSet(224))
                                                .build())
                                        .withModificationRule(new ValueModificationRule.Builder<Long>()
                                                .withOperation(UNKNOWN_VALUE)
                                                .build())
                                        .withId(2L)
                                        .withPriority(2)
                                        .build()))
                        .withTimeModifiers(
                                asList(
                                        new ValueModifierMeta.Builder<Integer>()
                                                .withCondition(new Condition.Builder()
                                                        .withCost(new PercentValueLimiter.Builder()
                                                                .withMinValue(50000L)
                                                                .withMaxValue(1000000L)
                                                                .withPercent(100.00)
                                                                .build())
                                                        .withWeight(new ValueLimiter<>(35.5, 37.8))
                                                        .withChargeableWeight(new ValueLimiter<>(36.0, 38.0))
                                                        .withDimension(new ValueLimiter<>(10.00, 20.00))
                                                        .withDeliveryTypes(newHashSet(COURIER))
                                                        .withCarrierIds(newHashSet(1L, 2L))
                                                        .withDeliveryDestinations(newHashSet(224))
                                                        .build())
                                                .withModificationRule(new ValueModificationRule.Builder<Integer>()
                                                        .withOperation(FIX_VALUE)
                                                        .withParameter(5.00)
                                                        .build())
                                                .withId(1L)
                                                .withPriority(1)
                                                .build(),
                                        new ValueModifierMeta.Builder<Integer>()
                                                .withCondition(new Condition.Builder()
                                                        .withDeliveryTypes(newHashSet(COURIER))
                                                        .withCarrierIds(newHashSet(10L))
                                                        .build())
                                                .withModificationRule(new ValueModificationRule.Builder<Integer>()
                                                        .withOperation(FIX_VALUE)
                                                        .withParameter(3.00)
                                                        .build())
                                                .withId(3L)
                                                .withPriority(4)
                                                .build()))
                        .withCarrierAvailabilityModifiers(
                                asList(
                                        new CarrierAvailabilityModifierMeta.Builder()
                                                .withCondition(new Condition.Builder()
                                                        .withCost(new PercentValueLimiter.Builder()
                                                                .withMinValue(50000L)
                                                                .withMaxValue(1000000L)
                                                                .withPercent(100.00)
                                                                .build())
                                                        .withWeight(new ValueLimiter<>(35.5, 37.8))
                                                        .withChargeableWeight(new ValueLimiter<>(36.0, 38.0))
                                                        .withDimension(new ValueLimiter<>(10.00, 20.00))
                                                        .withDeliveryTypes(newHashSet(COURIER))
                                                        .withCarrierIds(newHashSet(1L, 2L))
                                                        .withDeliveryDestinations(newHashSet(224))
                                                        .build())
                                                .withId(1L)
                                                .withIsCarrierAvailable(true)
                                                .withPriority(1)
                                                .build()))
                        .withServicesModifiers(
                                asList(
                                        new ServicesModifierMeta.Builder()
                                                .withCondition(new Condition.Builder()
                                                        .withWeight(new ValueLimiter<>(35.5, 37.8))
                                                        .build())
                                                .withId(8L)
                                                .withPaidByCustomerServices(Sets.newHashSet(INSURANCE))
                                                .withPriority(1)
                                                .build(),
                                        new ServicesModifierMeta.Builder()
                                                .withCondition(new Condition.Builder()
                                                        .withWeight(new ValueLimiter<>(35.5, 37.8))
                                                        .build())
                                                .withId(9L)
                                                .withPaidByCustomerServices(Sets.newHashSet(CASH_SERVICE))
                                                .withPriority(1)
                                                .build()))
                        .build())
                .build();


        MardoSenderSettingsGenerationEntity entity = new MardoSenderSettingsGenerationEntity();
        entity.setSenderId(1L);
        entity.setGenerationId(2L);
        entity.setDeleted(false);
        entity.setSerializedMetaData(PostgresJsonDataType.OBJECT_MAPPER.writeValueAsString(deserializedSenderMeta));
        return entity;
    }
}
