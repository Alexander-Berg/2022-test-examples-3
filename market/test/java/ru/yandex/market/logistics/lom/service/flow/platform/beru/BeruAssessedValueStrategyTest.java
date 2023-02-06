package ru.yandex.market.logistics.lom.service.flow.platform.beru;

import java.math.BigDecimal;
import java.util.stream.Stream;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.lom.AbstractTest;
import ru.yandex.market.logistics.lom.entity.Order;
import ru.yandex.market.logistics.lom.entity.PartnerSettings;
import ru.yandex.market.logistics.lom.entity.WaybillSegment;
import ru.yandex.market.logistics.lom.entity.embedded.Cost;
import ru.yandex.market.logistics.lom.entity.enums.DeliveryType;
import ru.yandex.market.logistics.lom.entity.enums.PartnerType;

import static org.assertj.core.api.Assertions.assertThat;

@ParametersAreNonnullByDefault
public class BeruAssessedValueStrategyTest extends AbstractTest {

    @DisplayName("Стратегия для расчета ОЦ для партнеров беру")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("arguments")
    public void testAssessedValue(String name, WaybillSegment waybillSegment, int expectedAssessedValue) {
        var strategy = new BeruAssessedValueStrategy();

        assertThat(strategy.convertForPartner(waybillSegment).intValue()).isEqualTo(expectedAssessedValue);
    }

    private static Stream<Arguments> arguments() {
        return Stream.of(
            Triple.of(
                "Партнёр СД, ОЦ < 1000",
                getWaybillSegment(999L, 123L, PartnerType.DELIVERY, DeliveryType.COURIER, false),
                999
            ),
            Triple.of(
                "Партнёр СД, ОЦ = 1000",
                getWaybillSegment(1000L, 123L, PartnerType.DELIVERY, DeliveryType.COURIER, false),
                1000
            ),
            Triple.of(
                "Партнёр СД, ОЦ > 1000",
                getWaybillSegment(1001L, 123L, PartnerType.DELIVERY, DeliveryType.COURIER, false),
                1001
            ),
            Triple.of(
                "Партнёр СЦ, ОЦ < 1000",
                getWaybillSegment(999L, 123L, PartnerType.SORTING_CENTER, DeliveryType.COURIER, false),
                999
            ),
            Triple.of(
                "Партнёр Почта России, ОЦ < 1000",
                getWaybillSegment(999L, 1005486L, PartnerType.DELIVERY, DeliveryType.COURIER, false),
                999
            ),
            Triple.of(
                "Партнёр СД, ОЦ < 1000, доставка Почтой Росии",
                getWaybillSegment(999L, 123L, PartnerType.DELIVERY, DeliveryType.POST, false),
                999
            ),
            Triple.of(
                "Партнёр СД, ОЦ < 1000, у партнёра включена настройка 'ОЦ не меньше наложенного платежа'",
                getWaybillSegment(999L, 123L, PartnerType.DELIVERY, DeliveryType.COURIER, true),
                999
            )
        )
            .map(q -> Arguments.of(q.getLeft(), q.getMiddle(), q.getRight()));
    }

    private static WaybillSegment getWaybillSegment(
        long assessedValue,
        long partnerId,
        PartnerType partnerType,
        DeliveryType deliveryType,
        boolean assessedValueTotalCheck
    ) {
        return new WaybillSegment()
            .setPartnerId(partnerId)
            .setOrder(
                new Order()
                    .setCost(
                        new Cost()
                            .setAssessedValue(BigDecimal.valueOf(assessedValue))
                    )
                    .setDeliveryType(deliveryType)
            )
            .setPartnerType(partnerType)
            .setPartnerSettings(
                PartnerSettings.builder()
                    .assessedValueTotalCheck(assessedValueTotalCheck)
                    .build()
            );
    }
}
