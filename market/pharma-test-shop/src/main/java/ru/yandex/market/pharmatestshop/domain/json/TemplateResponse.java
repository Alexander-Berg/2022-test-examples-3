package ru.yandex.market.pharmatestshop.domain.json;


import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import ru.yandex.market.pharmatestshop.domain.cart.Cart;
import ru.yandex.market.pharmatestshop.domain.cart.CartResponse;
import ru.yandex.market.pharmatestshop.domain.cart.delivery.DeliveryOption;
import ru.yandex.market.pharmatestshop.domain.cart.delivery.pickup.DeliveryOptionPickup;
import ru.yandex.market.pharmatestshop.domain.cart.delivery.pickup.date.DatesPickup;
import ru.yandex.market.pharmatestshop.domain.cart.delivery.pickup.outlet.Outlet;
import ru.yandex.market.pharmatestshop.domain.cart.delivery.yandex.DeliveryOptionYa;
import ru.yandex.market.pharmatestshop.domain.cart.delivery.yandex.date.DatesYa;
import ru.yandex.market.pharmatestshop.domain.cart.delivery.yandex.date.Interval;
import ru.yandex.market.pharmatestshop.domain.pharmacy.Pharmacy;

@Component
public class TemplateResponse {

    public static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    public static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    public CartResponse getCartResponse(Cart cart, Pharmacy currentPharmacy) {

        CartResponse cartResponse = new CartResponse();
        cartResponse.setMessage(currentPharmacy.getMessage());// MESSAGE
        cartResponse.setStatus(currentPharmacy.getStatus()); //STATUS
        cart.setDeliveryOptions(setDeliverOptionsToCart(currentPharmacy)); // DELIVERY_OPTIONS to CART
        cartResponse.setCart(cart);//CART

        return cartResponse;
    }

    public List<DeliveryOption> setDeliverOptionsToCart(Pharmacy currentPharmacy) {
        //1. Type of Delivery
        List<DeliveryOption> deliveryOptionList = new ArrayList<>();
        switch (currentPharmacy.getDeliveryTypes()) {
            case PICKUP:
                deliveryOptionList.add(setSettingsPickup(currentPharmacy));
                break;
            case DELIVERY:
                deliveryOptionList.add(setSettingsDelivery(currentPharmacy));
                break;
            case EXPRESS:
                deliveryOptionList.add(setSettingsExpress(currentPharmacy));
                break;
            case DELIVERY_PICKUP:
                deliveryOptionList.add(setSettingsDelivery(currentPharmacy));
                deliveryOptionList.add(setSettingsPickup(currentPharmacy));
                break;
            case EXPRESS_DELIVERY:
                deliveryOptionList.add(setSettingsExpress(currentPharmacy));
                deliveryOptionList.add(setSettingsDelivery(currentPharmacy));
                break;
            case EXPRESS_PICKUP:
                deliveryOptionList.add(setSettingsExpress(currentPharmacy));
                deliveryOptionList.add(setSettingsPickup(currentPharmacy));
                break;
            case EXPRESS_DELIVERY_PICKUP:

                deliveryOptionList.add(setSettingsExpress(currentPharmacy));
                deliveryOptionList.add(setSettingsDelivery(currentPharmacy));
                deliveryOptionList.add(setSettingsPickup(currentPharmacy));
                break;
            default:
                throw new IllegalArgumentException
                        ("Error 500 Incorrect delivery type Pharmacy with id "+currentPharmacy.getShopId());
        }

        return deliveryOptionList;
    }
    /*------------------ EXPRESS --------------------------*/

    DeliveryOptionYa setSettingsExpress(Pharmacy pharmacy) {


        return DeliveryOptionYa.builder()
                .type("DELIVERY")
                .serviceName("YandexDelivery Express")
                .dates(DatesYa.builder()
                        .fromDate(pharmacy.getFromDateExpress())
                        .toDate(pharmacy.getToDateExpress())
                        .intervals(generateIntervals(pharmacy.getFromDateExpress(), pharmacy.getToDateExpress()))
                        .build())
                .paymentMethods(mapPaymentMethods(pharmacy.getPaymentMethodExpress()))
                .build();

    }



    /*------------------ SIMPLE DELIVERY --------------------------*/

    DeliveryOptionYa setSettingsDelivery(Pharmacy pharmacy) {

        return DeliveryOptionYa.builder()
                .serviceName("YandexDelivery")
                .type("DELIVERY")
                .dates(DatesYa.builder()
                        .fromDate(pharmacy.getFromDateDelivery())
                        .toDate(pharmacy.getToDateDelivery())
                        .intervals(generateIntervals(pharmacy.getFromDateDelivery(), pharmacy.getToDateDelivery()))
                        .build())
                .paymentMethods(mapPaymentMethods(pharmacy.getPaymentMethodDelivery()))
                .build();
    }

    List<Interval> generateIntervals(LocalDate start, LocalDate end) {

        //TODO пока один интервал с 10:00 до 22:00
        List<Interval> intervals = new ArrayList<>();
        for (LocalDate i = start; !i.isAfter(end); i = i.plusDays(1)) {
            intervals.add(Interval.builder()
                    .date(i)
                    .fromTime(LocalTime.of(10, 0))
                    .toTime(LocalTime.of(22, 0))
                    .build());
        }
        return intervals;
    }

    /*------------------ PICKUP --------------------------*/

    DeliveryOptionPickup setSettingsPickup(Pharmacy pharmacy) {

        return DeliveryOptionPickup.builder()
                .serviceName("Самовывоз из аптеки")
                .type("PICKUP")
                .dates(DatesPickup.builder()
                        .fromDate(pharmacy.getFromDatePickup())
                        .toDate(pharmacy.getToDatePickup())
                        .build())
                .paymentMethods(mapPaymentMethods(pharmacy.getPaymentMethodPickup()))
                .outlets(generateOutlets(pharmacy))
                .build();
    }

    List<String> mapPaymentMethods(String paymentMethodsString) {
        return Arrays.stream(paymentMethodsString.split(",")).collect(Collectors.toList());
    }

    List<Outlet> generateOutlets(Pharmacy pharmacy) {
        List<Outlet> outlets = new ArrayList<>();
        if (pharmacy.getOutletIds().isBlank())
            return outlets;
        Arrays.stream(pharmacy.getOutletIds().split(",")).forEach(outletStr -> outlets.add(new Outlet(outletStr)));
        return outlets;
    }


}
