package ru.yandex.market.marketpromo.core.test.utils;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import ru.yandex.market.marketpromo.core.test.generator.Promos;
import ru.yandex.market.marketpromo.model.CategoryIdWithDiscount;
import ru.yandex.market.marketpromo.model.CheapestAsGiftProperties;
import ru.yandex.market.marketpromo.model.DirectDiscountProperties;
import ru.yandex.market.marketpromo.model.MechanicsProperties;
import ru.yandex.market.marketpromo.model.MechanicsType;
import ru.yandex.market.marketpromo.model.Promo;
import ru.yandex.market.marketpromo.model.PromoKey;
import ru.yandex.market.marketpromo.model.PromoStatus;
import ru.yandex.market.marketpromo.model.User;

public class PromoTestHelper {

    @Deprecated
    public static Promo.PromoBuilder defaultPromoBuilder(PromoKey promoKey) {
        return Promo.builder()
                .id(promoKey.getId())
                .mechanicsType(promoKey.getMechanicsType())
                .promoId("#21098")
                .name("some promo")
                .url("some url")
                .description("some description")
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(10))
                .categories(Set.of(Promos.CATEGORY_1, Promos.CATEGORY_2))
                .categoriesWithDiscounts(List.of(CategoryIdWithDiscount.of(Promos.CATEGORY_1, BigDecimal.TEN),
                        CategoryIdWithDiscount.of(Promos.CATEGORY_2, BigDecimal.valueOf(0.7))))
                .publishDate(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .createRequestId(UUID.randomUUID().toString())
                .trade(User.builder().id(Promos.LOGIN_1).staffLogin(Promos.LOGIN_1).build())
                .status(PromoStatus.CREATED)
                .hasErrors(true)
                .mechanicsProperties(propertiesForTest(promoKey.getMechanicsType()));
    }

    @Deprecated
    public static Promo defaultPromo(PromoKey promoKey) {
        return defaultPromoBuilder(promoKey).build();
    }


    private static MechanicsProperties propertiesForTest(MechanicsType mechanicsType) {
        switch (mechanicsType) {
            case DIRECT_DISCOUNT:
                return DirectDiscountProperties.builder().minimalDiscountPercentSize(BigDecimal.TEN).build();
            case CHEAPEST_AS_GIFT:
                return CheapestAsGiftProperties.builder().quantityInBundle(3).build();
            default:
                return MechanicsProperties.EMPTY;
        }
    }

}
