package ru.yandex.market.global.checkout.domain.actualize.actualizer;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.checkout.BaseFunctionalTest;
import ru.yandex.market.global.checkout.domain.actualize.ActualizationError;
import ru.yandex.market.global.checkout.domain.actualize.OrderActualization;
import ru.yandex.market.global.checkout.factory.TestOrderFactory;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ShopDisabledActualizerTest extends BaseFunctionalTest {
    private static final RecursiveComparisonConfiguration RECURSIVE_COMPARISON_CONFIGURATION =
            RecursiveComparisonConfiguration.builder()
                    .withIgnoreAllExpectedNullFields(true)
                    .build();

    private final TestOrderFactory testOrderFactory;
    private final ShopDisabledActualizer shopDisabledActualizer;

    @Test
    public void testDisabledProduceError() {
        OrderActualization actualization = testOrderFactory.buildOrderActualization(
                TestOrderFactory.CreateOrderActualizationBuilder.builder()
                        .setupShop(s -> s.enabled(false))
                        .build()
        );

        actualization = shopDisabledActualizer.actualize(actualization);

        Assertions.assertThat(actualization.getErrors())
                .usingRecursiveFieldByFieldElementComparator(RECURSIVE_COMPARISON_CONFIGURATION)
                .contains(new ActualizationError()
                        .setCode(ActualizationError.Code.SHOP_DISABLED)
                );
    }

    @Test
    public void testEnableProduceNoErrors() {
        OrderActualization actualization = testOrderFactory.buildOrderActualization(
                TestOrderFactory.CreateOrderActualizationBuilder.builder()
                        .setupShop(s -> s.enabled(true))
                        .build()
        );

        actualization = shopDisabledActualizer.actualize(actualization);

        Assertions.assertThat(actualization.getErrors()).isEmpty();
    }

}
