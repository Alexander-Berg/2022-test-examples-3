package ru.yandex.market.marketpromo.core.dao;

import java.util.List;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.marketpromo.core.test.ServiceTestBase;
import ru.yandex.market.marketpromo.model.MechanicsType;
import ru.yandex.market.marketpromo.model.OfferId;
import ru.yandex.market.marketpromo.model.OfferPromoParticipation;
import ru.yandex.market.marketpromo.model.Promo;
import ru.yandex.market.marketpromo.model.PromoKey;
import ru.yandex.market.marketpromo.utils.IdentityUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.cheapestAsGift;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.promo;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.promoId;

public class AssortmentPromoParticipationDaoTest extends ServiceTestBase {

    private static final long SHOP_ID = 12L;
    private static final String SSKU_1 = "ssku-1";
    private static final String SSKU_2 = "ssku-2";
    private static final String SSKU_3 = "ssku-3";

    @Autowired
    private AssortmentPromoParticipationDao assortmentPromoParticipationDao;
    @Autowired
    private PromoDao promoDao;

    private Promo cheapestAsGiftPromo;

    @BeforeEach
    void configuration() {
        cheapestAsGiftPromo = promoDao.replace(promoDao.replace(promo(
                promoId(DigestUtils.md5Hex("promo 1")),
                cheapestAsGift()
        )));
    }

    @Test
    void shouldMarkToParticipateInPromo() {
        OfferPromoParticipation assortmentItem = OfferPromoParticipation.builder()
                .promoId(cheapestAsGiftPromo.getId())
                .offerId(OfferId.of(IdentityUtils.hashId(SSKU_1), SHOP_ID))
                .participate(true)
                .build();
        OfferPromoParticipation notAssortmentItem = OfferPromoParticipation.builder()
                .promoId(cheapestAsGiftPromo.getId())
                .offerId(OfferId.of(IdentityUtils.hashId(SSKU_2), SHOP_ID))
                .participate(false)
                .build();

        assortmentPromoParticipationDao.saveParticipateToPromo(
                PromoKey.of(cheapestAsGiftPromo.getId(), MechanicsType.DIRECT_DISCOUNT),
                List.of(assortmentItem, notAssortmentItem));

        Map<OfferId, OfferPromoParticipation> result =
                assortmentPromoParticipationDao.getOfferParticipationInPromo(cheapestAsGiftPromo.toPromoKey(),
                        List.of(
                                assortmentItem.getOfferId(),
                                notAssortmentItem.getOfferId(),
                                OfferId.of(IdentityUtils.hashId(SSKU_3), SHOP_ID)
                        ));

        assertThat(result.get(assortmentItem.getOfferId()),
                hasProperty("participate", is(true)));
        assertThat(result.get(notAssortmentItem.getOfferId()),
                hasProperty("participate", is(false)));
        assertThat(result, not(hasKey(hasProperty("shopSku", is(SSKU_3)))));
    }

    @Test
    void shouldResetParticipationInfo() {
        OfferPromoParticipation assortmentItem = OfferPromoParticipation.builder()
                .promoId(cheapestAsGiftPromo.getId())
                .offerId(OfferId.of(IdentityUtils.hashId(SSKU_1), SHOP_ID))
                .participate(true)
                .build();

        assortmentPromoParticipationDao.saveParticipateToPromo(
                PromoKey.of(cheapestAsGiftPromo.getId(), MechanicsType.DIRECT_DISCOUNT),
                List.of(assortmentItem));

        assortmentPromoParticipationDao.resetParticipateToPromo(List.of(assortmentItem.getOfferId()));

        Map<OfferId, OfferPromoParticipation> result =
                assortmentPromoParticipationDao.getOfferParticipationInPromo(cheapestAsGiftPromo.toPromoKey());

        assertThat(result, anEmptyMap());
    }

}
