package ru.yandex.market.loyalty.core.service.promogroup;

import com.google.common.collect.ImmutableList;

import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promogroup.PromoGroup;
import ru.yandex.market.loyalty.core.model.promogroup.PromoGroupImpl;
import ru.yandex.market.loyalty.core.model.promogroup.PromoGroupPromo;
import ru.yandex.market.loyalty.api.model.promogroup.PromoGroupType;
import ru.yandex.market.loyalty.core.service.avatar.AvatarImageId;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

public class PromoGroupUtils {

    public static final PromoGroupType DEFAULT_PROMO_GROUP_TYPE = PromoGroupType.EFIM;
    public static final String DEFAULT_TOKEN = "TOKEN";
    public static final String DEFAULT_NAME = "Default Promo Group";
    public static final String ANOTHER_NAME = "Another Promo Group";
    public static final int DEFAULT_SORT_ORDER = 0;

    public static PromoGroup createDefaultPromoGroup(Clock clock) {
        return createPromoGroup(clock, DEFAULT_TOKEN);
    }

    public static PromoGroup createPromoGroup(Clock clock, String token) {
        return PromoGroupImpl.builder()
                .setPromoGroupType(DEFAULT_PROMO_GROUP_TYPE)
                .setToken(token)
                .setName(DEFAULT_NAME)
                .setStartDate(LocalDateTime.now(clock))
                .setEndDate(LocalDateTime.now(clock).plusDays(1))
                .setImage(new AvatarImageId(0, "image_name"))
                .build();
    }

    public static List<PromoGroupPromo> createDefaultPromoGroupPromos(Long promoGroupId, Promo promo) {
        return ImmutableList.of(createDefaultPromoGroupPromo(promoGroupId, promo));
    }

    public static PromoGroupPromo createDefaultPromoGroupPromo(Long promoGroupId, Promo promo) {
        return createPromGroupPromo(promoGroupId, promo, DEFAULT_SORT_ORDER);
    }


    public static PromoGroupPromo createPromGroupPromo(Long promoGroupId, Promo promo, int sortOrder) {
        return PromoGroupPromo.builder()
                .setPromoGroupId(promoGroupId)
                .setPromoId(promo.getId())
                .setSortOrder(sortOrder)
                .build();
    }

    public static PromoGroup createBrandDayPromoGroup(LocalDateTime start_day, LocalDateTime end_day, String token) {
        return PromoGroupImpl.builder()
                .setPromoGroupType(PromoGroupType.BRAND_DAY)
                .setToken(token)
                .setName(DEFAULT_NAME)
                .setStartDate(start_day)
                .setEndDate(end_day)
                .setImage(new AvatarImageId(1, "image"))
                .build();
    }
}
