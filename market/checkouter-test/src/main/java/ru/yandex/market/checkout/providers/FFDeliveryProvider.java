package ru.yandex.market.checkout.providers;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import ru.yandex.market.checkout.checkouter.order.OfferItem;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.common.report.model.FeedOfferId;
import ru.yandex.market.common.report.model.LocalDeliveryOption;

import static ru.yandex.market.checkout.test.providers.DeliveryProvider.FF_DELIVERY_SERVICE_ID;

/**
 * @author : poluektov
 * date: 14.11.17.
 */
@Deprecated
public final class FFDeliveryProvider {

    private FFDeliveryProvider() {
    }

    @Deprecated
    public static void setFFDeliveryParameters(Parameters parameters) {
        setFFDeliveryParameters(parameters, true);
    }

    @Deprecated
    public static void setFFDeliveryParameters(Parameters parameters, boolean free) {
        parameters.getOrder().setDelivery(DeliveryProvider.createYandexDeliveryResponse(free, DeliveryResponse::new));
        List<FeedOfferId> feedOfferIds = parameters.getOrder().getItems().stream()
                .map(OfferItem::getFeedOfferId)
                .collect(Collectors.toList());
        Map<FeedOfferId, List<LocalDeliveryOption>> offerDeliveryMap = new HashMap<>();
        feedOfferIds.forEach(id -> offerDeliveryMap.put(id,
                Collections.singletonList(getFFLocalDeliveryDelivery(free))));
        parameters.getReportParameters().setLocalDeliveryOptions(offerDeliveryMap);
    }

    private static LocalDeliveryOption getFFLocalDeliveryDelivery(boolean free) {
        LocalDeliveryOption localDeliveryOption = new LocalDeliveryOption();
        localDeliveryOption.setDeliveryServiceId(FF_DELIVERY_SERVICE_ID);
        localDeliveryOption.setDayFrom(0);
        localDeliveryOption.setDayTo(2);

        Set<String> paymentMethods = new HashSet<>();
        paymentMethods.add(PaymentMethod.YANDEX.name());
        paymentMethods.add(PaymentMethod.CASH_ON_DELIVERY.name());
        localDeliveryOption.setPaymentMethods(paymentMethods);

        if (free) {
            localDeliveryOption.setPrice(new BigDecimal("0"));
        } else {
            localDeliveryOption.setPrice(new BigDecimal("100"));
        }
        return localDeliveryOption;
    }
}
