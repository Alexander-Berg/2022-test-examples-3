package ru.yandex.market.fulfillment.wrap.marschroute.service.placeid.courier;

import ru.yandex.market.fulfillment.wrap.marschroute.factory.MarschrouteDates;
import ru.yandex.market.fulfillment.wrap.marschroute.model.response.delivery.city.DeliveryOption;
import ru.yandex.market.fulfillment.wrap.marschroute.model.type.MarschrouteDate;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;

public class CourierDeliveryOptionSelectorScenario {

    public final ResourceId deliveryId;
    public final Collection<DeliveryOption> availableOptions;
    public final MarschrouteDate deliveryDate;
    public final DeliveryOption expectedDeliveryOption;
    public final Map<String, String> preferredServices;

    public static CourierDeliveryOptionSelectorScenario create(ResourceId deliveryId,
                                                               Collection<DeliveryOption> availableOptions,
                                                               LocalDate deliveryDate,
                                                               DeliveryOption expectedDeliveryOption,
                                                               Map<String, String> preferredCodes) {
        return new CourierDeliveryOptionSelectorScenario(
                deliveryId,
                availableOptions,
                MarschrouteDates.marschrouteDate(deliveryDate),
                expectedDeliveryOption,
                preferredCodes
        );
    }

    public CourierDeliveryOptionSelectorScenario(ResourceId deliveryId,
                                                 Collection<DeliveryOption> availableOptions,
                                                 MarschrouteDate deliveryDate,
                                                 DeliveryOption expectedDeliveryOption,
                                                 Map<String, String> preferredServices) {
        this.deliveryId = deliveryId;
        this.availableOptions = availableOptions;
        this.deliveryDate = deliveryDate;
        this.expectedDeliveryOption = expectedDeliveryOption;
        this.preferredServices = preferredServices;
    }


    @Override
    public String toString() {
        return "Testing that [" + expectedDeliveryOption + "] should be picked among [" + availableOptions + "].\n" +
                "Preferred codes are: [" + preferredServices + "]. Delivery date is [" + deliveryDate + "]\n";
    }
}
