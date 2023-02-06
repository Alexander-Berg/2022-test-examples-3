package ru.yandex.market.logistics.nesu.facade;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.delivery.calculator.client.model.DeliveryOption;
import ru.yandex.market.logistics.delivery.calculator.client.model.DeliveryOptionService;
import ru.yandex.market.logistics.delivery.calculator.client.model.enums.DeliveryOptionServicePriceRule;
import ru.yandex.market.logistics.delivery.calculator.client.model.enums.DeliveryServiceCode;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParam;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.nesu.AbstractTest;
import ru.yandex.market.logistics.nesu.converter.lms.LmsExternalParamConverter;
import ru.yandex.market.logistics.nesu.converter.modifier.CurrencyConverter;
import ru.yandex.market.logistics.nesu.dto.deliveryoptions.DeliveryOptionResultService;
import ru.yandex.market.logistics.nesu.dto.deliveryoptions.DeliveryOptionsFilterCost;
import ru.yandex.market.logistics.nesu.model.entity.ServiceType;
import ru.yandex.market.logistics.nesu.service.deliveryoption.calculator.CostCalculator;
import ru.yandex.market.logistics.nesu.service.deliveryoption.model.DeliveryOptionContext;

@ParametersAreNonnullByDefault
@DisplayName("Расчет стоимости для покупателя и магазина")
class CostCalculatorTest extends AbstractTest {

    private final CurrencyConverter currencyConverter = new CurrencyConverter();
    private final CostCalculator costCalculator = new CostCalculator(
        currencyConverter,
        new LmsExternalParamConverter()
    );
    private final DeliveryOption deliveryOption = DeliveryOption.builder()
        .cost(2000)
        .services(List.of(cashServiceBuilder().build()))
        .build();

    private final List<DeliveryOptionResultService> services = List.of(
        DeliveryOptionResultService.builder()
            .enabledByDefault(true)
            .customerPay(true)
            .code(ServiceType.DELIVERY)
            .cost(BigDecimal.TEN)
            .build(),
        DeliveryOptionResultService.builder()
            .enabledByDefault(false)
            .customerPay(false)
            .code(ServiceType.RETURN)
            .cost(BigDecimal.valueOf(200))
            .build(),
        DeliveryOptionResultService.builder()
            .enabledByDefault(true)
            .customerPay(true)
            .code(ServiceType.WAIT_20)
            .cost(BigDecimal.valueOf(30))
            .build()
    );

    private final PartnerResponse.PartnerResponseBuilder deliveryService = PartnerResponse.newBuilder()
        .id(400L)
        .marketId(0L)
        .partnerType(PartnerType.DELIVERY)
        .name("deliveryService400")
        .readableName("Delivery Service 400")
        .status(PartnerStatus.ACTIVE);

    @Test
    @DisplayName("Стоимость доставки после модификаторов")
    void modifiedDelivery() {
        softly.assertThat(costCalculator.getCost(createContext()).getDelivery())
            .isEqualByComparingTo(BigDecimal.valueOf(20));
    }

    @Test
    @DisplayName("Стоимость доставки для магазина")
    void costForSender() {
        softly.assertThat(costCalculator.getCost(createContext()).getDeliveryForSender())
            .isEqualByComparingTo(BigDecimal.valueOf(40));
    }

    @Test
    @DisplayName("Стоимость доставки для покупателя")
    void costForCustomer() {
        softly.assertThat(costCalculator.getCost(createContext()).getDeliveryForCustomer())
            .isEqualByComparingTo(BigDecimal.valueOf(50));
    }

    @Test
    @DisplayName("Процент кассового сбора")
    void cashServicePercent() {
        softly.assertThat(costCalculator.getCashServicePercent(createContext())).isEqualByComparingTo("0.022");
    }

    @Nonnull
    private static Stream<Arguments> invalidDeliveryOptionSource() {
        return Stream.of(
            Arguments.of("100% значение страховки", 1D),
            Arguments.of("150% значение страховки", 1.5D)
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("invalidDeliveryOptionSource")
    @DisplayName("Невалидный вариант доставки")
    void wrongDeliveryOption(String caseName, double priceCalculationParameter) {
        DeliveryOptionContext context = defaultContextBuilder()
            .filterCost(filterCost())
            .deliveryOption(newDeliveryOptionWithInsurance(priceCalculationParameter).build())
            .deliveryService(deliveryService.params(paramsWithAssessedValueCheck()).build())
            .build();

        softly.assertThatThrownBy(() -> costCalculator.getCost(context))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid price calculation parameter value");
    }

    @Test
    @DisplayName("Не требуется корректировать объявленную ценность, если СД не требует указывать ее выше total")
    void noFixAssessedValue() {
        DeliveryOptionContext context = defaultContextBuilder()
            .filterCost(filterCost().setAssessedValue(BigDecimal.valueOf(300)))
            .deliveryOption(newDeliveryOptionWithInsurance().build())
            .deliveryService(deliveryService.build())
            .build();

        softly.assertThat(costCalculator.getCost(context).getAssessedValue())
            .isEqualTo(BigDecimal.valueOf(300));
    }

    @Test
    @DisplayName("Не можем скорректировать объявленную ценность, если не указана стоимость товаров")
    void cannotFixAssessedValueWithoutItemsSum() {
        DeliveryOptionContext context = defaultContextBuilder()
            .filterCost(filterCost().setItemsSum(null).setAssessedValue(BigDecimal.valueOf(300)))
            .deliveryOption(newDeliveryOptionWithInsurance().build())
            .deliveryService(deliveryService.params(paramsWithAssessedValueCheck()).build())
            .build();

        softly.assertThat(costCalculator.getCost(context).getAssessedValue())
            .isEqualTo(BigDecimal.valueOf(300));
    }

    @Nonnull
    private static Stream<Arguments> calculateNewAssessedValueSource() {
        return Stream.of(
            Arguments.of(null, BigDecimal.valueOf(981.88)),
            Arguments.of(BigDecimal.valueOf(300), BigDecimal.valueOf(981.88)),
            Arguments.of(BigDecimal.valueOf(981.88), BigDecimal.valueOf(981.88)),
            Arguments.of(BigDecimal.valueOf(990), BigDecimal.valueOf(990))
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("calculateNewAssessedValueSource")
    @DisplayName("Вычисляем новую объявленную ценность")
    void calculateNewAssessedValue(@Nullable BigDecimal currentAssessedValue, BigDecimal newAssessedValue) {
        DeliveryOptionContext context = defaultContextBuilder()
            .filterCost(filterCost().setAssessedValue(currentAssessedValue))
            .deliveryOption(newDeliveryOptionWithInsurance().build())
            .deliveryService(deliveryService.params(paramsWithAssessedValueCheck()).build())
            .build();

        softly.assertThat(costCalculator.getCost(context).getAssessedValue())
            .isEqualTo(newAssessedValue);
    }

    @Nonnull
    private DeliveryOptionsFilterCost filterCost() {
        return new DeliveryOptionsFilterCost()
            .setItemsSum(BigDecimal.valueOf(627.50));
    }

    @Nonnull
    private DeliveryOptionContext createContext() {
        return defaultContextBuilder().build();
    }

    @Nonnull
    private DeliveryOptionContext.DeliveryOptionContextBuilder defaultContextBuilder() {
        return DeliveryOptionContext.builder()
            .deliveryOption(deliveryOption)
            .services(services)
            .deliveryService(deliveryService.build());
    }

    @Nonnull
    private List<PartnerExternalParam> paramsWithAssessedValueCheck() {
        return List.of(new PartnerExternalParam(
            PartnerExternalParamType.ASSESSED_VALUE_TOTAL_CHECK.name(),
            null,
            "1"
        ));
    }

    @Nonnull
    private DeliveryOption.DeliveryOptionBuilder newDeliveryOptionWithInsurance() {
        return DeliveryOption.builder()
            .cost(2000)
            .services(List.of(
                cashServiceBuilder().build(),
                insuranceBuilder().build()
            ));
    }

    @Nonnull
    private DeliveryOption.DeliveryOptionBuilder newDeliveryOptionWithInsurance(double priceCalculationParameter) {
        return DeliveryOption.builder()
            .cost(2000)
            .services(List.of(
                cashServiceBuilder().build(),
                insuranceBuilder().priceCalculationParameter(priceCalculationParameter).build()
            ));
    }

    @Nonnull
    private DeliveryOptionService.DeliveryOptionServiceBuilder insuranceBuilder() {
        return DeliveryOptionService.builder()
            .code(DeliveryServiceCode.INSURANCE)
            .minPrice(100)
            .maxPrice(70000)
            .priceCalculationRule(DeliveryOptionServicePriceRule.PERCENT_COST)
            .priceCalculationParameter(0.31)
            .enabledByDefault(true);
    }

    @Nonnull
    private DeliveryOptionService.DeliveryOptionServiceBuilder cashServiceBuilder() {
        return DeliveryOptionService.builder()
            .code(DeliveryServiceCode.CASH_SERVICE)
            .minPrice(3000)
            .maxPrice(150000)
            .priceCalculationRule(DeliveryOptionServicePriceRule.PERCENT_CASH)
            .priceCalculationParameter(0.022)
            .enabledByDefault(true);
    }

}
