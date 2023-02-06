package ru.yandex.market.marketpromo.core.dao;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.marketpromo.core.test.ServiceTestBase;
import ru.yandex.market.marketpromo.core.test.generator.Offers;
import ru.yandex.market.marketpromo.core.test.generator.Promos;
import ru.yandex.market.marketpromo.model.CacheLocalOffer;
import ru.yandex.market.marketpromo.model.LocalOffer;
import ru.yandex.market.marketpromo.model.OfferId;
import ru.yandex.market.marketpromo.model.Promo;
import ru.yandex.market.marketpromo.model.WarehouseFeedKey;
import ru.yandex.market.marketpromo.utils.IdentityUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.generateCacheLocalOfferList;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.generateLocalDDPromos;
import static ru.yandex.market.marketpromo.core.test.generator.PromoMechanics.minimalDiscountPercentSize;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.id;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.warehouse;


public class CachedAssortmentDaoTest extends ServiceTestBase {

    private static final Long WAREHOUSE_ID = 145L;
    private static final Long FEED_ID = 10L;
    private static final Long STOCKS = 100L;

    @Autowired
    private CachedAssortmentDao dao;

    private Promo promo;

    @BeforeEach
    void setUp() {
        promo = Promos.promo(
                id(IdentityUtils.hashId("some promo")),
                Promos.directDiscount(
                        minimalDiscountPercentSize(13)
                ),
                Promos.category(123L, 10),
                Promos.category(124L, 15),
                Promos.category(125L, 20),
                warehouse(WAREHOUSE_ID)
        );
    }

    @Test
    public void shouldWriteOfferListToTable() {
        List<CacheLocalOffer> localOffers = generateCacheLocalOfferList(1000);
        dao.replace(localOffers, promo);
        Map<OfferId, LocalOffer.OfferBuilder> offerIds = dao.selectOfferBuilderMapByPromoId(promo.getId());
        assertThat(offerIds.size(), is(1000));
    }

    @Test
    public void shouldWriteOfferWithStocks() {
        CacheLocalOffer offer = Offers.cacheLocalOffer(
                Offers.shopSku("qxr.215928"),
                Offers.marketSku(526781L),
                Offers.name("Зубочистка"),
                Offers.promosCache(
                        generateLocalDDPromos(1, 3)
                ),
                Offers.stocksByWarehouseCache(WarehouseFeedKey.of(WAREHOUSE_ID, FEED_ID), STOCKS)
        );

        dao.replace(offer, promo);
        Map<OfferId, LocalOffer.OfferBuilder> offerIds = dao.selectOfferBuilderMapByPromoId(promo.getId());
        assertThat(offerIds.size(), is(1));
    }
}
