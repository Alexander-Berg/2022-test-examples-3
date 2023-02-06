package ru.yandex.market.logistics.lom.service.flow.platform.faas;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.lom.AbstractTest;
import ru.yandex.market.logistics.lom.entity.Order;
import ru.yandex.market.logistics.lom.entity.enums.PlatformClient;

@ParametersAreNonnullByDefault
public class FaasBarcodeGenerationStrategyTest extends AbstractTest {

    @Test
    @DisplayName("Стратегия для простановки штрихкода заказа FaaS")
    public void testBarcode() {
        FaasBarcodeGenerationStrategy strategy = new FaasBarcodeGenerationStrategy();

        Order order = new Order()
            .setPlatformClient(PlatformClient.FAAS)
            .setExternalId("externalId");

        softly.assertThat(strategy.generateBarcode(order)).isEqualTo("FF-externalId");
    }
}
