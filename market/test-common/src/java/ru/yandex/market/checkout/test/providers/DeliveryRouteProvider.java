package ru.yandex.market.checkout.test.providers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.common.report.model.ActualDelivery;
import ru.yandex.market.common.report.model.ActualDeliveryOption;
import ru.yandex.market.common.report.model.ActualDeliveryResult;
import ru.yandex.market.common.report.model.DeliveryRoute;
import ru.yandex.market.common.report.model.DeliveryRouteOption;
import ru.yandex.market.common.report.model.DeliveryRouteResult;
import ru.yandex.market.common.report.model.LocalDeliveryOption;
import ru.yandex.market.common.report.model.PickupOption;

public abstract class DeliveryRouteProvider {

    @NotNull
    public static DeliveryRoute fromActualDelivery(ActualDelivery actualDelivery, DeliveryType deliveryType) {
        ActualDeliveryResult actualDeliveryResult = actualDelivery.getResults().get(0);
        DeliveryRoute deliveryRoute = new DeliveryRoute();
        DeliveryRouteResult deliveryRouteResult = DeliveryResultProvider.getDeliveryRouteResult();
        deliveryRoute.setResults(Collections.singletonList(deliveryRouteResult));
        deliveryRouteResult.setLargeSize(actualDeliveryResult.getLargeSize());
        if (deliveryType == DeliveryType.DELIVERY) {
            ActualDeliveryOption actualDeliveryOption = actualDeliveryResult.getDelivery().get(0);
            DeliveryRouteOption deliveryRouteOption = copy(actualDeliveryOption);

            if (actualDeliveryOption.getTimeIntervals() != null && !actualDeliveryOption.getTimeIntervals().isEmpty()) {
                deliveryRouteOption.setTimeIntervals(actualDeliveryOption.getTimeIntervals().subList(0, 1));
            }
            deliveryRouteResult.setOption(deliveryRouteOption);
        } else if (deliveryType == DeliveryType.PICKUP) {
            DeliveryRouteOption deliveryRouteOption = copy(actualDeliveryResult.getPickup().get(0));
            deliveryRouteResult.setOption(deliveryRouteOption);
        } else if (deliveryType == DeliveryType.POST) {
            PickupOption postOption = actualDeliveryResult.getPost().get(0);
            DeliveryRouteOption deliveryRouteOption = copy(postOption);
            deliveryRouteOption.setOutletIds(postOption.getOutletIds());
            deliveryRouteResult.setOption(deliveryRouteOption);
        }
        return deliveryRoute;
    }

    private static DeliveryRouteOption copy(LocalDeliveryOption option) {
        DeliveryRouteOption deliveryRouteOption = new DeliveryRouteOption();
        deliveryRouteOption.setDeliveryServiceId(option.getDeliveryServiceId());
        deliveryRouteOption.setDayFrom(option.getDayFrom());
        deliveryRouteOption.setDayTo(option.getDayTo());
        deliveryRouteOption.setPaymentMethods(option.getPaymentMethods());
        deliveryRouteOption.setPrice(option.getPrice());
        deliveryRouteOption.setCurrency(option.getCurrency());
        deliveryRouteOption.setShipmentDay(option.getShipmentDay());
        deliveryRouteOption.setShipmentDate(option.getShipmentDate());
        deliveryRouteOption.setPackagingTime(option.getPackagingTime());
        deliveryRouteOption.setSupplierShipmentDateTime(option.getSupplierShipmentDateTime());
        deliveryRouteOption.setPartnerType(option.getPartnerType());
        deliveryRouteOption.setIsExternalLogistics(option.isExternalLogistics());
        deliveryRouteOption.setEstimated(option.getEstimated());
        deliveryRouteOption.setExtraCharge(option.getExtraCharge());
        return deliveryRouteOption;
    }

    public static void cleanActualDelivery(ActualDelivery actualDelivery) {
        actualDelivery.getResults().get(0).setDelivery(
                actualDelivery.getResults().get(0).getDelivery().stream()
                        .map(DeliveryRouteProvider::mapToCleanActualDeliveryOption)
                        .collect(Collectors.toList())
        );
        actualDelivery.getResults().get(0).setPickup(
                actualDelivery.getResults().get(0).getPickup().stream()
                        .map(DeliveryRouteProvider::mapToCleanPickupOption)
                        .collect(Collectors.toList())
        );
        actualDelivery.getResults().get(0).setPost(
                actualDelivery.getResults().get(0).getPost().stream()
                        .map(DeliveryRouteProvider::mapToCleanPostOption)
                        .collect(Collectors.toList())
        );
    }

    private static ActualDeliveryOption mapToCleanActualDeliveryOption(ActualDeliveryOption source) {
        ActualDeliveryOption res = new ActualDeliveryOption();
        mapToCleanDeliveryOption(source, res);
        res.setTimeIntervals(new ArrayList<>(source.getTimeIntervals()));
        return res;
    }

    private static PickupOption mapToCleanPickupOption(PickupOption source) {
        PickupOption res = new PickupOption();
        mapToCleanDeliveryOption(source, res);
        res.setOutletIds(new ArrayList<>(source.getOutletIds()));
        res.setOutletTimeIntervals(new ArrayList<>(source.getOutletTimeIntervals()));
        return res;
    }

    private static PickupOption mapToCleanPostOption(PickupOption source) {
        PickupOption res = mapToCleanPickupOption(source);
        res.setPostCodes(new ArrayList<>(source.getPostCodes()));
        return res;
    }

    private static void mapToCleanDeliveryOption(LocalDeliveryOption source, LocalDeliveryOption target) {
        target.setDayFrom(source.getDayFrom());
        target.setDayTo(source.getDayTo());
        target.setPaymentMethods(source.getPaymentMethods());
        target.setPrice(source.getPrice());
        target.setCurrency(source.getCurrency());
        target.setIsOnDemand(source.isOnDemand());
        target.setIsDeferredCourier(source.isDeferredCourier());
    }
}
