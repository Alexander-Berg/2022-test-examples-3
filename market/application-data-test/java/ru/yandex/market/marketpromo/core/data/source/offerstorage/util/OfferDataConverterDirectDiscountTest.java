package ru.yandex.market.marketpromo.core.data.source.offerstorage.util;

import java.math.BigDecimal;
import java.util.List;

import javax.annotation.Nonnull;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferPromos;
import Market.DataCamp.DataCampUnitedOffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.marketpromo.core.dao.DatacampOfferDao;
import ru.yandex.market.marketpromo.core.dao.LocalPromoOfferDao;
import ru.yandex.market.marketpromo.core.dao.PromoDao;
import ru.yandex.market.marketpromo.core.test.ServiceTestBase;
import ru.yandex.market.marketpromo.core.test.generator.DatacampOfferPromoMechanics;
import ru.yandex.market.marketpromo.core.test.generator.Offers;
import ru.yandex.market.marketpromo.model.DirectDiscountOfferParticipation;
import ru.yandex.market.marketpromo.model.LocalOffer;
import ru.yandex.market.marketpromo.model.OfferId;
import ru.yandex.market.marketpromo.model.OfferPromoParticipation;
import ru.yandex.market.marketpromo.model.Promo;
import ru.yandex.market.marketpromo.service.AssortmentService;
import ru.yandex.market.marketpromo.utils.IdentityUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.activePromos;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.potentialPromo;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.price;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.shop;
import static ru.yandex.market.marketpromo.core.test.generator.PromoMechanics.minimalDiscountPercentSize;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.directDiscount;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.id;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.promo;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.promoName;

public class OfferDataConverterDirectDiscountTest extends ServiceTestBase {

    private static final long SHOP_ID = 12L;
    private static final String SSKU_1 = "ssku 1";
    public static final String PROMO_ID = "some promo";

    @Autowired
    private DatacampOfferDao datacampOfferDao;
    @Autowired
    private LocalPromoOfferDao localPromoOfferDao;
    @Autowired
    private OfferDataConverter offerDataConverter;
    @Autowired
    private AssortmentService assortmentService;
    @Autowired
    private PromoDao promoDao;

    private Promo promo;

    @BeforeEach
    void configure() {
        promo = promoDao.replace(promo(
                id(IdentityUtils.hashId(PROMO_ID)),
                promoName(PROMO_ID),
                directDiscount(
                        minimalDiscountPercentSize(10)
                )
        ));
    }

    @Test
    void shouldMapBasePriceIfOnlyPotentialDataSet() {
        datacampOfferDao.replace(List.of(Offers.datacampOffer(
                Offers.name(SSKU_1),
                Offers.shopSku(SSKU_1),
                shop(SHOP_ID),
                price(100),
                potentialPromo(IdentityUtils.hashId(PROMO_ID), BigDecimal.valueOf(100)),
                activePromos(
                        DatacampOfferPromoMechanics.directDiscount(IdentityUtils.hashId(PROMO_ID))
                )
        )));

        LocalOffer localOffer = localPromoOfferDao.getOffers(promo,
                List.of(OfferId.of(IdentityUtils.hashId(SSKU_1), SHOP_ID))).get(0);

        assertThat(localOffer, notNullValue());

        DataCampUnitedOffer.UnitedOffer unitedOffer = offerDataConverter.convertToDataCampOffer(localOffer);
        DataCampOffer.Offer serviceOffer = unitedOffer.getServiceOrThrow((int) localOffer.getShopId());

        checkBaseProperties(unitedOffer, serviceOffer);

        assertThat(offerDataConverter.convertToPrice(serviceOffer.getPromos().getAnaplanPromos().getActivePromos()
                .getPromos(0).getDirectDiscount().getBasePrice()), nullValue());
    }

    @Test
    void shouldMapBasePriceIfLocalSet() {
        datacampOfferDao.replace(List.of(Offers.datacampOffer(
                Offers.name(SSKU_1),
                Offers.shopSku(SSKU_1),
                shop(SHOP_ID),
                price(100),
                potentialPromo(IdentityUtils.hashId(PROMO_ID), BigDecimal.valueOf(100)),
                activePromos(
                        DatacampOfferPromoMechanics.directDiscount(IdentityUtils.hashId(PROMO_ID))
                )
        )));

        assortmentService.markDirectDiscountToParticipate(promo.toPromoKey(),
                List.of(DirectDiscountOfferParticipation.builder()
                        .offerPromoParticipation(OfferPromoParticipation.builder()
                                .promoId(promo.getId())
                                .participate(true)
                                .offerId(OfferId.of(IdentityUtils.hashId(SSKU_1), SHOP_ID))
                                .build())
                        .fixedBasePrice(BigDecimal.valueOf(200))
                        .build()));

        LocalOffer localOffer = localPromoOfferDao.getOffers(promo, List.of(OfferId.of(
                IdentityUtils.hashId(SSKU_1),
                SHOP_ID))).get(0);

        assertThat(localOffer, notNullValue());

        DataCampUnitedOffer.UnitedOffer unitedOffer = offerDataConverter.convertToDataCampOffer(localOffer);
        DataCampOffer.Offer serviceOffer = unitedOffer.getServiceOrThrow((int) localOffer.getShopId());

        checkBaseProperties(unitedOffer, serviceOffer);

        assertThat(offerDataConverter.convertToPrice(serviceOffer.getPromos().getAnaplanPromos().getActivePromos()
                .getPromos(0).getDirectDiscount().getBasePrice()), nullValue());
    }

    private void checkBaseProperties(@Nonnull DataCampUnitedOffer.UnitedOffer unitedOffer,
                                     @Nonnull DataCampOffer.Offer serviceOffer) {
        assertTrue(unitedOffer.hasBasic());
        assertTrue(unitedOffer.getBasic().hasIdentifiers());
        assertFalse(unitedOffer.getBasic().hasPromos());
        assertTrue(serviceOffer.hasPromos());
        assertTrue(serviceOffer.getPromos().hasAnaplanPromos());

        //Мы обновляем актуальный асортимент
        assertTrue(serviceOffer.getPromos().getAnaplanPromos().hasActivePromos());
        assertTrue(serviceOffer.getPromos().getAnaplanPromos().getActivePromos().hasMeta());
        assertFalse(serviceOffer.getPromos().getAnaplanPromos().getActivePromos().getPromosList().isEmpty());
        for (DataCampOfferPromos.Promo promo :
                serviceOffer.getPromos().getAnaplanPromos().getActivePromos().getPromosList()) {
            assertFalse(promo.hasBudget());
            assertThat(promo.getId(), is(PROMO_ID));
 //           assertTrue(promo.hasDirectDiscount());
            //assertTrue(promo.getDirectDiscount().hasBasePrice());
            //assertTrue(promo.hasDiscountOldprice());
        }
        //Мы не обновляем потенциальный асортимент
        assertFalse(serviceOffer.getPromos().getAnaplanPromos().hasAllPromos());
    }
}
