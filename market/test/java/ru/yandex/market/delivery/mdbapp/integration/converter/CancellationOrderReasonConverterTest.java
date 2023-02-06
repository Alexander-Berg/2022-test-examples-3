package ru.yandex.market.delivery.mdbapp.integration.converter;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Predicate;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.delivery.mdbapp.AbstractTest;
import ru.yandex.market.logistics.lom.model.enums.CancellationOrderReason;

@DisplayName("Тест маппинга подстатусов чекаутера и причин отмены лома")
class CancellationOrderReasonConverterTest extends AbstractTest {

    private static final Set<CancellationOrderReason> LOM_IGNORED_REASONS = Set.of(
        CancellationOrderReason.SHOP_CANCELLED, // статус для ЯДо, а не для маркетплейса
        CancellationOrderReason.DIMENSIONS_EXCEEDED, // TODO: Поддержать новую причину в DELIVERY-46700
        CancellationOrderReason.UNKNOWN
    );

    private final CancellationOrderReasonConverter converter = new CancellationOrderReasonConverter();

    @Disabled
    @Test
    @DisplayName("Конвертация подстатуса в причину отмены")
    void checkMappingSubstatusToReason() {
        Arrays.stream(OrderSubstatus.values())
            .filter(substatus -> substatus.getStatus().equals(OrderStatus.CANCELLED))
            .forEach(
                substatus ->
                    softly.assertThat(converter.convertSubstatusToReason(substatus, CancellationOrderReason.UNKNOWN))
                    .as(substatus.name() + " converts to UNKNOWN")
                    .isNotEqualTo(CancellationOrderReason.UNKNOWN)
        );
    }

    @Test
    @DisplayName("Конвертация причины отмены в подстатус")
    void checkMappingReasonToSubstatus() {
        CancellationOrderReason.valuesStream()
            .filter(Predicate.not(LOM_IGNORED_REASONS::contains))
            .forEach(
                reason ->
                    softly.assertThat(converter.convertToSubstatusOrDefault(reason, OrderSubstatus.UNKNOWN))
                    .as(reason.getName() + " converts to UNKNOWN")
                    .isNotEqualTo(OrderSubstatus.UNKNOWN)
            );
    }

    @Test
    @DisplayName("Конвертация пустой причины отмены")
    void checkMappingNullReason() {
            softly.assertThat(converter.convertToSubstatusOrDefault(null, OrderSubstatus.UNKNOWN))
                .isEqualTo(OrderSubstatus.UNKNOWN);
    }

    @Test
    @DisplayName("Конвертация пустого подстатуса")
    void checkMappingNullSubstatus() {
        softly.assertThat(converter.convertSubstatusToReason(null, CancellationOrderReason.UNKNOWN))
            .isEqualTo(CancellationOrderReason.UNKNOWN);
    }
}
