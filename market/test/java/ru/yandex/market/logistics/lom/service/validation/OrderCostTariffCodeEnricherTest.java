package ru.yandex.market.logistics.lom.service.validation;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.lom.AbstractTest;
import ru.yandex.market.logistics.lom.entity.Order;
import ru.yandex.market.logistics.lom.entity.embedded.Cost;
import ru.yandex.market.logistics.lom.jobs.processor.validation.ValidateAndEnrichContext;
import ru.yandex.market.logistics.lom.jobs.processor.validation.ValidateAndEnrichResults;
import ru.yandex.market.logistics.lom.jobs.processor.validation.validator.OrderCostTariffCodeEnricher;
import ru.yandex.market.logistics.lom.service.tarifficator.TarifficatorServiceImpl;
import ru.yandex.market.logistics.tarifficator.client.TarifficatorClient;
import ru.yandex.market.logistics.tarifficator.model.dto.TariffDto;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Валидация и обогащение кода тарифа")
class OrderCostTariffCodeEnricherTest extends AbstractTest {
    private static final Set<Long> TRANSLATABLE_TARIFF_IDS = Set.of(100038L, 100039L, 100005L, 100667L);
    private final TarifficatorClient tarifficatorClient = mock(TarifficatorClient.class);
    private final OrderCostTariffCodeEnricher orderCostTariffCodeEnricher =
        new OrderCostTariffCodeEnricher(new TarifficatorServiceImpl(tarifficatorClient));
    private final ValidateAndEnrichContext context = new ValidateAndEnrichContext();

    @DisplayName("Тариф с указанным идентификатором не найден")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("translatableTariffIds")
    void tariffNotFound(long tariffId) {
        Order order = new Order().setCost(new Cost().setTariffId(tariffId));
        ValidateAndEnrichResults results = orderCostTariffCodeEnricher.validateAndEnrich(order, context);
        softly.assertThat(results.isValidationPassed()).isFalse();
        softly.assertThat(results.getErrorMessage())
            .isEqualTo("Failed to find tariff name for " + tariffId);
    }

    @Nonnull
    private static Stream<Arguments> translatableTariffIds() {
        return TRANSLATABLE_TARIFF_IDS.stream().sorted().map(Arguments::of);
    }

    @Test
    @DisplayName("Тариф, для которого не требуется tariffCode")
    void nonTranslatableTariff() {
        Order order = new Order().setCost(new Cost().setTariffId(100L));
        ValidateAndEnrichResults results = orderCostTariffCodeEnricher.validateAndEnrich(order, context);
        softly.assertThat(results.isValidationPassed()).isTrue();
        softly.assertThat(results.getOrderModifier().apply(order).getCost().getTariffCode()).isNull();
    }

    @DisplayName("Заказ успешно обогащается tariffCode'ом")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("translatableTariffIds")
    void enrichingSucceeded(long tariffId) {
        when(tarifficatorClient.getOptionalTariff(tariffId))
            .thenReturn(Optional.of(TariffDto.builder().id(tariffId).code("code-" + tariffId).build()));
        Order order = new Order().setCost(new Cost().setTariffId(tariffId));
        ValidateAndEnrichResults results = orderCostTariffCodeEnricher.validateAndEnrich(order, context);
        softly.assertThat(results.isValidationPassed()).isTrue();
        softly.assertThat(results.getOrderModifier().apply(order).getCost().getTariffCode())
            .isEqualTo("code-" + tariffId);
    }

    @Nonnull
    private static Stream<Arguments> oldYaDoTariffs() {
        return Stream.of(
            Arguments.of(4152L, "МСК - Курьер онлайн"),
            Arguments.of(4386L, "МСК - Посылка нестандартная негабаритная")
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Заказ успешно обогащается кодом почтового тарифа из старой доставки")
    @MethodSource("oldYaDoTariffs")
    void enrichingPostYaDoTariffCodeSucceeded(long tariffId, String tariffCode) {
        Order order = new Order().setCost(new Cost().setTariffId(tariffId));
        ValidateAndEnrichResults results = orderCostTariffCodeEnricher.validateAndEnrich(order, context);
        softly.assertThat(results.isValidationPassed()).isTrue();
        softly.assertThat(results.getOrderModifier().apply(order).getCost().getTariffCode()).isEqualTo(tariffCode);
    }
}
