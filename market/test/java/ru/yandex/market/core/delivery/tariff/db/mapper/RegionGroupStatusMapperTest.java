package ru.yandex.market.core.delivery.tariff.db.mapper;

import java.util.Arrays;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.common.util.id.HasId;
import ru.yandex.market.api.delivery.failure.RegionGroupFailureReason;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.delivery.RegionGroupPaymentType;

/**
 * Тесты для {@link RegionGroupStatusMapper}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class RegionGroupStatusMapperTest extends FunctionalTest {

    private static <T extends Enum & HasId<Integer>> void testReason(final Class<T> reasonType) {
        // id должен быть [1; Long.SIZE], иначе не уложимся в long, когда будем сериализовывать данные
        Arrays.stream(reasonType.getEnumConstants())
                .forEach(e -> Assertions.assertTrue(e.getId() >= 1 && e.getId() <= Long.SIZE, "Bad reason: " + e));
    }

    @Test
    @DisplayName("Конвертация: Set -> long")
    void testMapSetToLong() {
        final Set<RegionGroupPaymentType> set = ImmutableSet.of(
                RegionGroupPaymentType.PREPAYMENT_OTHER,
                RegionGroupPaymentType.COURIER_CARD
        );

        final long actual = RegionGroupStatusMapper.mapReasonToLong(set);
        Assertions.assertEquals(6L, actual);
    }

    @Test
    @DisplayName("Конвертация: Set(empty) -> long")
    void testMapEmptySetToLong() {
        final Set<RegionGroupPaymentType> set = ImmutableSet.of();

        final long actual = RegionGroupStatusMapper.mapReasonToLong(set);
        Assertions.assertEquals(0L, actual);
    }

    @Test
    @DisplayName("Конвертация: long (0) -> Set (empty)")
    void testMapZeroToSet() {
        final Set<RegionGroupPaymentType> actual = RegionGroupStatusMapper.mapReasonToSet(0L, RegionGroupPaymentType.class);
        final Set<RegionGroupPaymentType> expected = ImmutableSet.of();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Конвертация: long -> Set (Типы оплат)")
    void testMapPaymentToSet() {
        final Set<RegionGroupPaymentType> actual = RegionGroupStatusMapper.mapReasonToSet(5L, RegionGroupPaymentType.class);
        final Set<RegionGroupPaymentType> expected = ImmutableSet.of(
                RegionGroupPaymentType.PREPAYMENT_OTHER,
                RegionGroupPaymentType.COURIER_CASH
        );
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Конвертация: long -> Set (СиС)")
    void testMapDeliveryToSet() {
        final Set<RegionGroupFailureReason> actual = RegionGroupStatusMapper.mapReasonToSet(5L, RegionGroupFailureReason.class);
        final Set<RegionGroupFailureReason> expected = ImmutableSet.of(
                RegionGroupFailureReason.NO_DELIVERY,
                RegionGroupFailureReason.INVALID_DELIVERY_COST
        );
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Не привысили лимит на количество причин: СиС")
    void testDeliveryReasons() {
        testReason(RegionGroupFailureReason.class);
    }

    @Test
    @DisplayName("Не привысили лимит на количество причин: типы оплат")
    void testPaymentReasons() {
        testReason(RegionGroupPaymentType.class);
    }
}
