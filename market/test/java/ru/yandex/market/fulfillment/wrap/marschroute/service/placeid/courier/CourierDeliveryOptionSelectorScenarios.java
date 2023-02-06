package ru.yandex.market.fulfillment.wrap.marschroute.service.placeid.courier;

import com.google.common.collect.ImmutableMap;
import ru.yandex.market.fulfillment.wrap.marschroute.model.response.delivery.city.DeliveryOption;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;

import javax.annotation.Nonnull;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static ru.yandex.market.fulfillment.wrap.marschroute.factory.DeliveryOptions.courierDeliveryOption;


/**
 * Отражает набор сценариев из
 * https://wiki.yandex-team.ru/users/kotovdv/courierserviceselection-algorithm/
 */
public class CourierDeliveryOptionSelectorScenarios {

    private static final int X = 100; //Abstract price
    private static final String PREFERRED = "GOOD";
    private static final String PREFERRED_2 = "GOOD_2";
    private static final String NOT_PREFERRED = "BAD";
    private static final String NOT_PREFERRED_2 = "BAD_2";
    private static final LocalDate CORRECT_DELIVERY_DATE = LocalDate.of(1997, 7, 7);
    private static final LocalDate WRONG_DELIVERY_DATE = CORRECT_DELIVERY_DATE.minusDays(1);
    private static final ResourceId PREFERRED_DELIVERY_ID = new ResourceId("1", null);
    private static final ResourceId NOT_PREFERRED_DELIVERY_ID = new ResourceId("-1", null);


    public static CourierDeliveryOptionSelectorScenario numberOne() {
        return createScenario(Arrays.asList(
                courierDeliveryOption(PREFERRED, CORRECT_DELIVERY_DATE, X),
                courierDeliveryOption(NOT_PREFERRED, CORRECT_DELIVERY_DATE, X)
        ), 0, PREFERRED_DELIVERY_ID);
    }

    public static CourierDeliveryOptionSelectorScenario numberTwo() {
        return createScenario(Arrays.asList(
                courierDeliveryOption(PREFERRED, WRONG_DELIVERY_DATE, X),
                courierDeliveryOption(NOT_PREFERRED, CORRECT_DELIVERY_DATE, X)
        ), 0, PREFERRED_DELIVERY_ID);
    }

    public static CourierDeliveryOptionSelectorScenario numberThree() {
        return createScenario(Arrays.asList(
                courierDeliveryOption(PREFERRED, WRONG_DELIVERY_DATE, 2 * X),
                courierDeliveryOption(NOT_PREFERRED, CORRECT_DELIVERY_DATE, X)
        ), 0, PREFERRED_DELIVERY_ID);
    }

    public static CourierDeliveryOptionSelectorScenario numberFour() {
        return createScenario(Arrays.asList(
                courierDeliveryOption(PREFERRED, CORRECT_DELIVERY_DATE, X),
                courierDeliveryOption(PREFERRED_2, CORRECT_DELIVERY_DATE, X)
        ), 0, PREFERRED_DELIVERY_ID);
    }


    public static CourierDeliveryOptionSelectorScenario numberFive() {
        return createScenario(Arrays.asList(
                courierDeliveryOption(PREFERRED, CORRECT_DELIVERY_DATE, X),
                courierDeliveryOption(PREFERRED_2, CORRECT_DELIVERY_DATE, 2 * X)
        ), 0, PREFERRED_DELIVERY_ID);
    }

    public static CourierDeliveryOptionSelectorScenario numberSix() {
        return createScenario(Arrays.asList(
                courierDeliveryOption(PREFERRED, CORRECT_DELIVERY_DATE, X),
                courierDeliveryOption(PREFERRED_2, WRONG_DELIVERY_DATE, X)
        ), 0, PREFERRED_DELIVERY_ID);
    }

    public static CourierDeliveryOptionSelectorScenario numberSeven() {
        return createScenario(Arrays.asList(
                courierDeliveryOption(PREFERRED, CORRECT_DELIVERY_DATE, 2 * X),
                courierDeliveryOption(PREFERRED_2, WRONG_DELIVERY_DATE, X)
        ), 0, PREFERRED_DELIVERY_ID);
    }

    public static CourierDeliveryOptionSelectorScenario numberEight() {
        return createScenario(Arrays.asList(
                courierDeliveryOption(PREFERRED, WRONG_DELIVERY_DATE, X),
                courierDeliveryOption(PREFERRED_2, WRONG_DELIVERY_DATE, X)
        ), 0, PREFERRED_DELIVERY_ID);
    }

    public static CourierDeliveryOptionSelectorScenario numberNine() {
        return createScenario(Arrays.asList(
                courierDeliveryOption(PREFERRED, WRONG_DELIVERY_DATE, X),
                courierDeliveryOption(PREFERRED_2, WRONG_DELIVERY_DATE, 2 * X)
        ), 0, PREFERRED_DELIVERY_ID);
    }

    public static CourierDeliveryOptionSelectorScenario numberTen() {
        return createScenario(Arrays.asList(
                courierDeliveryOption(NOT_PREFERRED, CORRECT_DELIVERY_DATE, X),
                courierDeliveryOption(NOT_PREFERRED_2, CORRECT_DELIVERY_DATE, X)
        ), 0, NOT_PREFERRED_DELIVERY_ID);
    }

    public static CourierDeliveryOptionSelectorScenario numberEleven() {
        return createScenario(Arrays.asList(
                courierDeliveryOption(NOT_PREFERRED, CORRECT_DELIVERY_DATE, X),
                courierDeliveryOption(NOT_PREFERRED_2, CORRECT_DELIVERY_DATE, 2 * X)
        ), 0, NOT_PREFERRED_DELIVERY_ID);
    }

    public static CourierDeliveryOptionSelectorScenario numberTwelve() {
        return createScenario(Arrays.asList(
                courierDeliveryOption(NOT_PREFERRED, CORRECT_DELIVERY_DATE, X),
                courierDeliveryOption(NOT_PREFERRED_2, WRONG_DELIVERY_DATE, X)
        ), 0, NOT_PREFERRED_DELIVERY_ID);
    }

    public static CourierDeliveryOptionSelectorScenario numberThirteen() {
        return createScenario(Arrays.asList(
                courierDeliveryOption(NOT_PREFERRED, CORRECT_DELIVERY_DATE, 2 * X),
                courierDeliveryOption(NOT_PREFERRED_2, WRONG_DELIVERY_DATE, X)
        ), 0, NOT_PREFERRED_DELIVERY_ID);
    }

    public static CourierDeliveryOptionSelectorScenario numberFourteen() {
        return createScenario(Arrays.asList(
                courierDeliveryOption(NOT_PREFERRED, WRONG_DELIVERY_DATE, X),
                courierDeliveryOption(NOT_PREFERRED_2, WRONG_DELIVERY_DATE, X)
        ), 0, NOT_PREFERRED_DELIVERY_ID);
    }

    public static CourierDeliveryOptionSelectorScenario numberFifteen() {
        return createScenario(Arrays.asList(
                courierDeliveryOption(NOT_PREFERRED, WRONG_DELIVERY_DATE, X),
                courierDeliveryOption(NOT_PREFERRED_2, WRONG_DELIVERY_DATE, 2 * X)
        ), 0, NOT_PREFERRED_DELIVERY_ID);
    }

    public static CourierDeliveryOptionSelectorScenario technicalNumberOne() {
        return createScenario(Arrays.asList(
                courierDeliveryOption(PREFERRED, CORRECT_DELIVERY_DATE, 3 * X),
                courierDeliveryOption(PREFERRED, CORRECT_DELIVERY_DATE, X),
                courierDeliveryOption(PREFERRED, CORRECT_DELIVERY_DATE, 4 * X)
        ), 1, PREFERRED_DELIVERY_ID);
    }

    @Nonnull
    private static CourierDeliveryOptionSelectorScenario createScenario(List<DeliveryOption> options,
                                                                        int optionToSelect,
                                                                        ResourceId deliveryId) {
        return CourierDeliveryOptionSelectorScenario.create(
                deliveryId,
                options,
                CORRECT_DELIVERY_DATE,
                options.get(optionToSelect),
                ImmutableMap.of(
                        "1", PREFERRED.toLowerCase().trim(),
                        "2", PREFERRED_2.toLowerCase().trim()
                )
        );
    }
}
