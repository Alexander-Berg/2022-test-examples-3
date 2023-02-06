package ru.yandex.market.global.checkout.domain.actualize.actualizer;

import java.math.BigDecimal;

import io.github.benas.randombeans.api.EnhancedRandom;
import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.checkout.BaseFunctionalTest;
import ru.yandex.market.global.checkout.domain.actualize.OrderActualization;
import ru.yandex.market.global.checkout.domain.delivery.DeliveryTariffService;
import ru.yandex.market.global.checkout.util.RandomDataGenerator;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class OrderDeliveryCostsActualizerTest extends BaseFunctionalTest {
    private static final EnhancedRandom RANDOM =
            RandomDataGenerator.dataRandom(OrderDeliveryCostsActualizerTest.class).build();

    private final DeliveryTariffService deliveryTariffService;
    private final OrderDeliveryCostsActualizer orderDeliveryCostsActualizer;

    @Test
    public void testSmallDeliveryCost() {
        OrderActualization orderActualization = RANDOM.nextObject(OrderActualization.class);

        orderActualization.getOrderDelivery().getRecipientAddress().getCoordinates()
                .setLat(BigDecimal.valueOf(32.076156))
                .setLon(BigDecimal.valueOf(34.779765));

        orderActualization.getOrderDelivery().getShopAddress().getCoordinates()
                .setLat(BigDecimal.valueOf(32.075874))
                .setLon(BigDecimal.valueOf(34.786943));

        OrderActualization deliveryCostActualization = orderDeliveryCostsActualizer.actualize(orderActualization);
        Assertions.assertThat(deliveryCostActualization.getOrder().getDeliveryCostForRecipient())
                .isEqualTo(10_00);
    }

    @Test
    public void testBigDeliveryCost() {
        //32.076156, 34.779765
        OrderActualization orderActualization = RANDOM.nextObject(OrderActualization.class);

        orderActualization.getOrderDelivery().getRecipientAddress().getCoordinates()
                .setLat(BigDecimal.valueOf(32.076156))
                .setLon(BigDecimal.valueOf(34.779765));

        orderActualization.getOrderDelivery().getShopAddress().getCoordinates()
                .setLat(BigDecimal.valueOf(32.075874))
                .setLon(BigDecimal.valueOf(34.797654));

        OrderActualization deliveryCostActualization = orderDeliveryCostsActualizer.actualize(orderActualization);
        Assertions.assertThat(deliveryCostActualization.getOrder().getDeliveryCostForRecipient())
                .isEqualTo(16_00);
    }

}
