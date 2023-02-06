package ru.yandex.market.global.checkout.domain.actualize.actualizer;

import io.github.benas.randombeans.api.EnhancedRandom;
import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.checkout.BaseFunctionalTest;
import ru.yandex.market.global.checkout.config.properties.CheckoutCommonProperties;
import ru.yandex.market.global.checkout.domain.actualize.OrderActualization;
import ru.yandex.market.global.checkout.factory.TestOrderFactory.CreateOrderActualizationBuilder;
import ru.yandex.market.global.checkout.util.RandomDataGenerator;

import static ru.yandex.market.global.checkout.factory.TestOrderFactory.buildOrderActualization;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class DefaultsActualizerTest extends BaseFunctionalTest {

    private static final EnhancedRandom RANDOM = RandomDataGenerator.dataRandom(DefaultsActualizerTest.class).build();

    private final DefaultsActualizer defaultsActualizer;
    private final CheckoutCommonProperties properties;

    @Test
    public void testValidWithCountry() {
        OrderActualization orderActualization = buildOrderActualization(CreateOrderActualizationBuilder.builder()
                .setupOrder(o -> o.setLocale("ru-RU"))
                .build()
        );
        OrderActualization actualize = defaultsActualizer.actualize(orderActualization);
        Assertions.assertThat(actualize.getErrors()).isEmpty();
        Assertions.assertThat(actualize.getWarnings()).isEmpty();
        Assertions.assertThat(actualize.getOrder().getLocale()).isEqualTo("ru-RU");
    }

    @Test
    public void testValid() {
        OrderActualization orderActualization = buildOrderActualization(CreateOrderActualizationBuilder.builder()
                .setupOrder(o -> o.setLocale("ru"))
                .build()
        );
        OrderActualization actualize = defaultsActualizer.actualize(orderActualization);
        Assertions.assertThat(actualize.getErrors()).isEmpty();
        Assertions.assertThat(actualize.getWarnings()).isEmpty();
        Assertions.assertThat(actualize.getOrder().getLocale()).isEqualTo("ru");
    }

    @Test
    public void testAutoFix() {
        OrderActualization orderActualization = buildOrderActualization(CreateOrderActualizationBuilder.builder()
                .setupOrder(o -> o.setLocale("en_US"))
                .build()
        );
        OrderActualization actualize = defaultsActualizer.actualize(orderActualization);
        Assertions.assertThat(actualize.getErrors()).isEmpty();
        Assertions.assertThat(actualize.getWarnings()).isEmpty();
        Assertions.assertThat(actualize.getOrder().getLocale()).isEqualTo("en-US");
    }

    @Test
    public void testInvalidLocale() {
        OrderActualization orderActualization = buildOrderActualization(CreateOrderActualizationBuilder.builder()
                .setupOrder(o -> o.setLocale("ndjandjaksndajkdnjakdnkjankdjan_US"))
                .build()
        );
        OrderActualization actualize = defaultsActualizer.actualize(orderActualization);
        Assertions.assertThat(actualize.getErrors()).isEmpty();
        Assertions.assertThat(actualize.getWarnings()).isEmpty();
        Assertions.assertThat(actualize.getOrder().getLocale()).isEqualTo(properties.getDefaultLocale());
    }

    @Test
    public void testNullLocale() {
        OrderActualization orderActualization = buildOrderActualization(CreateOrderActualizationBuilder.builder()
                .setupOrder(o -> o.setLocale(null))
                .build()
        );
        OrderActualization actualize = defaultsActualizer.actualize(orderActualization);
        Assertions.assertThat(actualize.getErrors()).isEmpty();
        Assertions.assertThat(actualize.getWarnings()).isEmpty();
        Assertions.assertThat(actualize.getOrder().getLocale()).isEqualTo(properties.getDefaultLocale());
    }

    @Test
    public void testIsYandexSetToDefaultIfEmpty() {
        OrderActualization orderActualization = buildOrderActualization(CreateOrderActualizationBuilder.builder()
                .setupOrder(o -> o.setYandex(null))
                .build()
        );
        OrderActualization actualize = defaultsActualizer.actualize(orderActualization);
        Assertions.assertThat(actualize.getOrder().getYandex()).isFalse();
    }

    @Test
    public void testIsYandexPreservedIfFilled() {
        OrderActualization orderActualization = buildOrderActualization(CreateOrderActualizationBuilder.builder()
                .setupOrder(o -> o.setYandex(true))
                .build()
        );
        OrderActualization actualize = defaultsActualizer.actualize(orderActualization);
        Assertions.assertThat(actualize.getOrder().getYandex()).isTrue();
    }

}
