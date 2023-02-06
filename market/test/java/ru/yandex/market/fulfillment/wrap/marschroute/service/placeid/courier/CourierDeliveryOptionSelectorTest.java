package ru.yandex.market.fulfillment.wrap.marschroute.service.placeid.courier;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.fulfillment.wrap.marschroute.model.response.delivery.city.DeliveryOption;

import static org.assertj.core.api.Assertions.assertThat;

class CourierDeliveryOptionSelectorTest {

    @Nonnull
    static Stream<Arguments> data() {
        return Stream.of(
            Arguments.of(CourierDeliveryOptionSelectorScenarios.numberOne()),
            Arguments.of(CourierDeliveryOptionSelectorScenarios.numberTwo()),
            Arguments.of(CourierDeliveryOptionSelectorScenarios.numberThree()),
            Arguments.of(CourierDeliveryOptionSelectorScenarios.numberFour()),
            Arguments.of(CourierDeliveryOptionSelectorScenarios.numberFive()),
            Arguments.of(CourierDeliveryOptionSelectorScenarios.numberSix()),
            Arguments.of(CourierDeliveryOptionSelectorScenarios.numberSeven()),
            Arguments.of(CourierDeliveryOptionSelectorScenarios.numberEight()),
            Arguments.of(CourierDeliveryOptionSelectorScenarios.numberNine()),
            Arguments.of(CourierDeliveryOptionSelectorScenarios.numberTen()),
            Arguments.of(CourierDeliveryOptionSelectorScenarios.numberEleven()),
            Arguments.of(CourierDeliveryOptionSelectorScenarios.numberTwelve()),
            Arguments.of(CourierDeliveryOptionSelectorScenarios.numberThirteen()),
            Arguments.of(CourierDeliveryOptionSelectorScenarios.numberFourteen()),
            Arguments.of(CourierDeliveryOptionSelectorScenarios.numberFifteen()),
            Arguments.of(CourierDeliveryOptionSelectorScenarios.technicalNumberOne())
        );
    }

    @MethodSource("data")
    @ParameterizedTest
    void testCourierServiceSelection(CourierDeliveryOptionSelectorScenario scenario) throws Exception {
        CourierDeliveryOptionSelector selector = new CourierDeliveryOptionSelector(scenario.preferredServices);

        DeliveryOption actualOption = selector.select(
            scenario.deliveryId,
            scenario.deliveryDate,
            scenario.availableOptions
        );

        assertThat(actualOption)
            .as("Asserting that correct option was selected")
            .isSameAs(scenario.expectedDeliveryOption);
    }
}
