package ru.yandex.market.marketpromo.core.dao;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.marketpromo.core.test.ServiceTestBase;
import ru.yandex.market.marketpromo.model.DirectDiscountOfferParticipation;
import ru.yandex.market.marketpromo.model.MechanicsType;
import ru.yandex.market.marketpromo.model.OfferId;
import ru.yandex.market.marketpromo.model.OfferPromoParticipation;
import ru.yandex.market.marketpromo.model.Promo;
import ru.yandex.market.marketpromo.model.PromoKey;
import ru.yandex.market.marketpromo.utils.IdentityUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.directDiscount;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.promo;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.promoId;

public class DirectDiscountAssortmentDaoTest extends ServiceTestBase {

    private static final long SHOP_ID = 12L;

    @Autowired
    private DirectDiscountAssortmentDao directDiscountAssortmentDao;
    @Autowired
    private PromoDao promoDao;

    private Promo directDiscountPromo;

    @BeforeEach
    void configuration() {
        directDiscountPromo = promoDao.replace(promoDao.replace(promo(
                promoId(DigestUtils.md5Hex("promo 1")),
                directDiscount()
        )));
    }

    @Test
    void shouldMarkParticipation() {
        OfferPromoParticipation base = OfferPromoParticipation.builder()
                .promoId(directDiscountPromo.getId())
                .offerId(OfferId.of(IdentityUtils.hashId("ssku-111"), SHOP_ID))
                .participate(true)
                .build();

        DirectDiscountOfferParticipation item = DirectDiscountOfferParticipation.builder()
                .offerPromoParticipation(base)
                .fixedBasePrice(BigDecimal.TEN)
                .fixedPrice(BigDecimal.ONE)
                .minimalPercentSize(BigDecimal.ONE)
                .build();

        directDiscountAssortmentDao.saveParticipationToPromo(
                PromoKey.of(directDiscountPromo.getId(), MechanicsType.DIRECT_DISCOUNT),
                Collections.singletonList(item));

        Map<OfferId, DirectDiscountOfferParticipation> result = directDiscountAssortmentDao
                .getParticipationPromoDetails(directDiscountPromo.toPromoKey(), Map.of(
                        base.getOfferId(),
                        OfferPromoParticipation.builder()
                                .promoId(directDiscountPromo.getId())
                                .offerId(base.getOfferId())
                                .participate(true).build(),
                        OfferId.of(IdentityUtils.hashId("1246"), SHOP_ID),
                        OfferPromoParticipation.builder()
                                .offerId(OfferId.of(IdentityUtils.hashId("1246"), SHOP_ID))
                                .promoId(directDiscountPromo.getId())
                                .participate(true).build()
                ));

        assertThat(result.get(base.getOfferId()).getFixedBasePrice(),
                comparesEqualTo(item.getFixedBasePrice()));
        assertThat(result.get(base.getOfferId()).getFixedPrice(),
                comparesEqualTo(item.getFixedPrice()));
        assertThat(result.get(base.getOfferId()).getMinimalPercentSize(),
                comparesEqualTo(item.getMinimalPercentSize()));
        assertThat(result.containsKey(OfferId.of(IdentityUtils.hashId("1246"), SHOP_ID)), is(false));
    }

    @Test
    void shouldMarkParticipationIgnoreBasePrice() {
        OfferPromoParticipation base = OfferPromoParticipation.builder()
                .promoId(directDiscountPromo.getId())
                .offerId(OfferId.of(IdentityUtils.hashId("ssku-111"), SHOP_ID))
                .participate(true)
                .build();

        DirectDiscountOfferParticipation item = DirectDiscountOfferParticipation.builder()
                .offerPromoParticipation(base)
                .fixedBasePrice(BigDecimal.TEN)
                .minimalPercentSize(BigDecimal.ONE)
                .build();

        directDiscountAssortmentDao.saveParticipationToPromo(
                PromoKey.of(directDiscountPromo.getId(), MechanicsType.DIRECT_DISCOUNT),
                Collections.singletonList(item));

        Map<OfferId, DirectDiscountOfferParticipation> result = directDiscountAssortmentDao
                .getParticipationPromoDetails(directDiscountPromo.toPromoKey(), Map.of(
                        base.getOfferId(),
                        OfferPromoParticipation.builder()
                                .promoId(directDiscountPromo.getId())
                                .offerId(base.getOfferId())
                                .participate(true).build(),
                        OfferId.of(IdentityUtils.hashId("1246"), SHOP_ID),
                        OfferPromoParticipation.builder()
                                .offerId(OfferId.of(IdentityUtils.hashId("1246"), SHOP_ID))
                                .promoId(directDiscountPromo.getId())
                                .participate(true).build()
                ));

        assertThat(result.get(base.getOfferId()).getFixedBasePrice(),
                nullValue());
        assertThat(result.get(base.getOfferId()).getFixedPrice(),
                nullValue());
        assertThat(result.get(base.getOfferId()).getMinimalPercentSize(),
                comparesEqualTo(item.getMinimalPercentSize()));
        assertThat(result.containsKey(OfferId.of(IdentityUtils.hashId("1246"), SHOP_ID)), is(false));
    }

    @Test
    void shouldResetParticipation() {
        OfferPromoParticipation base = OfferPromoParticipation.builder()
                .promoId(directDiscountPromo.getId())
                .offerId(OfferId.of(IdentityUtils.hashId("ssku-111"), SHOP_ID))
                .participate(true)
                .build();

        DirectDiscountOfferParticipation item = DirectDiscountOfferParticipation.builder()
                .offerPromoParticipation(base)
                .fixedBasePrice(BigDecimal.ONE)
                .fixedPrice(BigDecimal.ONE)
                .minimalPercentSize(BigDecimal.ONE)
                .build();

        directDiscountAssortmentDao.saveParticipationToPromo(
                PromoKey.of(directDiscountPromo.getId(), MechanicsType.DIRECT_DISCOUNT),
                Collections.singletonList(item));
        directDiscountAssortmentDao.resetParticipateToPromo(List.of(item.getOfferPromoParticipation().getOfferId()));

        Map<OfferId, DirectDiscountOfferParticipation> result = directDiscountAssortmentDao
                .getParticipationPromoDetails(directDiscountPromo.toPromoKey(),
                        Map.of(
                                item.getOfferPromoParticipation().getOfferId(),
                                OfferPromoParticipation.builder()
                                        .offerId(item.getOfferPromoParticipation().getOfferId())
                                        .promoId(directDiscountPromo.getId())
                                        .participate(true).build()
                        ));

        assertThat(result, anEmptyMap());
    }

}
