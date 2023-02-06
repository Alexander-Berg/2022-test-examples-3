package ru.yandex.market.checkout.checkouter.promo.promocode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Multimap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.promo.PromoDiscount;
import ru.yandex.market.checkout.checkouter.promo.loyalty.client.DiscountResponseConverter;
import ru.yandex.market.loyalty.api.model.PaymentType;
import ru.yandex.market.loyalty.api.model.PromoType;
import ru.yandex.market.loyalty.api.model.delivery.DeliveryDiscountWithPromoType;
import ru.yandex.market.loyalty.api.model.delivery.DeliveryPromoResponse;
import ru.yandex.market.loyalty.api.model.delivery.DeliveryResponse;

public class DiscountResponseConverterTest {

    @Test
    public void shouldConvertDeliveryResponsesToDiscountMap() {
        DiscountResponseConverter converter = new DiscountResponseConverter();

        List<DeliveryResponse> deliveries = new ArrayList<>();

        DeliveryDiscountWithPromoType freePickupWithoutDiscount = new DeliveryDiscountWithPromoType(BigDecimal.ZERO,
                PromoType.FREE_PICKUP);

        Map<PaymentType, DeliveryDiscountWithPromoType> discountByPayment = new HashMap<>();
        discountByPayment.put(PaymentType.SHOP_PREPAID, freePickupWithoutDiscount);

        List<DeliveryPromoResponse> deliveryPromoResponses = new ArrayList<>();

        DeliveryPromoResponse deliveryPromoResponse =
                new DeliveryPromoResponse(BigDecimal.ZERO, PromoType.FREE_PICKUP,
                        null, null, null, discountByPayment);

        deliveryPromoResponses.add(deliveryPromoResponse);

        DeliveryResponse emptyPromos = new DeliveryResponse("1", null);
        DeliveryResponse filledPromos = new DeliveryResponse("2", deliveryPromoResponses);
        deliveries.add(emptyPromos);
        deliveries.add(filledPromos);

        Multimap<String, PromoDiscount> stringPromoDiscountMultimap = converter.toDiscountMap(deliveries);
        Assertions.assertNotNull(stringPromoDiscountMultimap);
        Assertions.assertFalse(stringPromoDiscountMultimap.isEmpty());

        Assertions.assertTrue(stringPromoDiscountMultimap.get("1").isEmpty());
        Assertions.assertFalse(stringPromoDiscountMultimap.get("2").isEmpty());
    }
}
