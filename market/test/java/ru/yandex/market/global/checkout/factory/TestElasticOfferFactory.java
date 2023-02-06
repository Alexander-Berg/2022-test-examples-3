package ru.yandex.market.global.checkout.factory;

import java.util.List;
import java.util.function.Function;

import io.github.benas.randombeans.api.EnhancedRandom;

import ru.yandex.market.global.checkout.util.RandomDataGenerator;
import ru.yandex.mj.generated.server.model.OfferDto;
import ru.yandex.mj.generated.server.model.ShopCategoryDto;

public class TestElasticOfferFactory {

    private static final EnhancedRandom RANDOM = RandomDataGenerator.dataRandom(TestElasticOfferFactory.class).build();

    public static final long SHOP_ID = 40;
    public static final long BUSINESS_ID = 41;
    public static final String OFFER_ID = "OFFER_ID";
    public static final long PRICE = 123L;


    public List<OfferDto> buildOne() {
        return buildOne(Function.identity());
    }

    public List<OfferDto> buildOne(Function<OfferDto, OfferDto> setupOffer) {
        OfferDto offer = RANDOM.nextObject(OfferDto.class)
                .category(RANDOM.nextObject(ShopCategoryDto.class)
                        .id(RANDOM.nextLong()));

        offer = offer.offerId(OFFER_ID)
                .shopId(SHOP_ID)
                .price(PRICE)
                .businessId(BUSINESS_ID)
                .enabled(true);
        offer = setupOffer.apply(offer);
        return List.of(offer);
    }
}
