package ru.yandex.market.global.checkout.domain.actualize.actualizer;

import java.util.List;

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
public class ShopHiddenActualizerTest extends BaseFunctionalTest {
    private static final long COMMON_UID = 1;
    private static final long SPECIAL_UID = 2;

    private static final RecursiveComparisonConfiguration RECURSIVE_COMPARISON_CONFIGURATION =
            RecursiveComparisonConfiguration.builder()
                    .withIgnoreAllExpectedNullFields(true)
                    .build();

    private final TestOrderFactory testOrderFactory;
    private final ShopHiddenActualizer shopHiddenActualizer;

    @Test
    public void testHiddenProduceErrorForCommonUser() {
        OrderActualization actualization = testOrderFactory.buildOrderActualization(
                TestOrderFactory.CreateOrderActualizationBuilder.builder()
                        .setupOrder(o -> o.setUid(COMMON_UID))
                        .setupShop(s -> s
                                .hidden(true)
                                .hiddenExceptUids(List.of(SPECIAL_UID))
                        )
                        .build()
        );

        actualization = shopHiddenActualizer.actualize(actualization);

        Assertions.assertThat(actualization.getErrors())
                .usingRecursiveFieldByFieldElementComparator(RECURSIVE_COMPARISON_CONFIGURATION)
                .contains(new ActualizationError()
                        .setCode(ActualizationError.Code.SHOP_HIDDEN)
                );
    }

    @Test
    public void testHiddenProduceNoErrorForSpecialUser() {
        OrderActualization actualization = testOrderFactory.buildOrderActualization(
                TestOrderFactory.CreateOrderActualizationBuilder.builder()
                        .setupOrder(o -> o.setUid(SPECIAL_UID))
                        .setupShop(s -> s
                                .hidden(true)
                                .hiddenExceptUids(List.of(SPECIAL_UID))
                        )
                        .build()
        );

        actualization = shopHiddenActualizer.actualize(actualization);

        Assertions.assertThat(actualization.getErrors()).isEmpty();
    }

    @Test
    public void testVisibleProduceNoErrorForCommonUser() {
        OrderActualization actualization = testOrderFactory.buildOrderActualization(
                TestOrderFactory.CreateOrderActualizationBuilder.builder()
                        .setupOrder(o -> o.setUid(COMMON_UID))
                        .setupShop(s -> s.hidden(false))
                        .build()
        );

        actualization = shopHiddenActualizer.actualize(actualization);

        Assertions.assertThat(actualization.getErrors()).isEmpty();
    }

}
