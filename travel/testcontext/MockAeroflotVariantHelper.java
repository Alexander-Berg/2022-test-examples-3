package ru.yandex.travel.api.services.avia.testcontext;

import java.math.BigDecimal;

import org.javamoney.moneta.Money;

import ru.yandex.avia.booking.partners.gateways.aeroflot.model.AeroflotCategoryOffer;
import ru.yandex.avia.booking.partners.gateways.aeroflot.model.AeroflotPriceDetail;
import ru.yandex.avia.booking.partners.gateways.aeroflot.model.AeroflotTotalOffer;
import ru.yandex.avia.booking.partners.gateways.aeroflot.model.AeroflotVariant;

import static java.util.stream.Collectors.toList;

class MockAeroflotVariantHelper {
    public static AeroflotVariant changeVariantPrice(AeroflotVariant variantInfo) {
        return variantInfo.toBuilder()
                .offer(changeOfferPrice(variantInfo.getOffer()))
                .allTariffs(variantInfo.getAllTariffs().stream()
                        .map(MockAeroflotVariantHelper::changeOfferPrice)
                        .collect(toList()))
                .build();
    }

    private static AeroflotTotalOffer changeOfferPrice(AeroflotTotalOffer offer) {
        return offer.toBuilder()
                .totalPrice(changePrice(offer.getTotalPrice()))
                .categoryOffers(offer.getCategoryOffers().stream()
                        // the numbers wont sum up to the mocked total but it seems ok at the moment
                        .map(MockAeroflotVariantHelper::changeCategoryOfferPrice)
                        .collect(toList()))
                .build();
    }

    private static AeroflotCategoryOffer changeCategoryOfferPrice(AeroflotCategoryOffer categoryOffer) {
        AeroflotPriceDetail price = categoryOffer.getTotalPrice();
        return categoryOffer.toBuilder()
                .totalPrice(price.toBuilder()
                        .basePrice(changePrice(price.getBasePrice()))
                        .totalPrice(changePrice(price.getTotalPrice()))
                        .build())
                .build();
    }

    private static Money changePrice(Money origPrice) {
        return origPrice.add(Money.of(BigDecimal.TEN, origPrice.getCurrency()));
    }
}
