package ru.yandex.market.billing.fulfillment.tariffs;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import ru.yandex.market.billing.core.order.model.ValueType;
import ru.yandex.market.billing.fulfillment.OrderType;
import ru.yandex.market.billing.model.billing.BillingServiceType;
import ru.yandex.market.billing.order.model.BillingUnit;
import ru.yandex.market.fulfillment.entities.base.DateTimeInterval;

/**
 * Класс создания тариффов фулфилмента для тестов
 */
@SuppressWarnings("ParameterNumber")
public class TestTariffCreationUtil {

    private TestTariffCreationUtil() {
    }

    public static FulfillmentTariff createFulfillmentTariff(
            LocalDate from,
            LocalDate to,
            BillingServiceType serviceType,
            Long dimensionsTo,
            Long weightTo,
            int value,
            OrderType orderType) {
        return createFulfillmentTariff(from, to, serviceType, dimensionsTo, weightTo, value, orderType, null, null);
    }

    public static FulfillmentTariff createFulfillmentTariff(
            LocalDate from,
            LocalDate to,
            BillingServiceType serviceType,
            Long dimensionsTo,
            Long weightTo,
            int value,
            OrderType orderType,
            Long sourceArea,
            Long destArea) {
        return new FulfillmentTariff(
                new DateTimeInterval(
                        createOffsetDateTime(from),
                        createOffsetDateTime(to)
                ),
                serviceType,
                null,
                dimensionsTo,
                weightTo,
                value,
                null,
                null,
                ValueType.ABSOLUTE,
                BillingUnit.ITEM,
                orderType,
                null,
                sourceArea,
                destArea);
    }

    private static OffsetDateTime createOffsetDateTime(LocalDate date) {
        return ZonedDateTime.of(date, LocalTime.MIDNIGHT, ZoneId.systemDefault()).toOffsetDateTime();
    }
}
