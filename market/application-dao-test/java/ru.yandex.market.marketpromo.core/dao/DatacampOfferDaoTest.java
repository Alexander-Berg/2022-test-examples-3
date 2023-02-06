package ru.yandex.market.marketpromo.core.dao;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.Nonnull;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.marketpromo.core.test.ServiceTestBase;
import ru.yandex.market.marketpromo.core.test.generator.DatacampOfferPromoMechanics;
import ru.yandex.market.marketpromo.core.test.generator.Promos;
import ru.yandex.market.marketpromo.model.DatacampOffer;
import ru.yandex.market.marketpromo.model.DatacampOfferPromo;
import ru.yandex.market.marketpromo.model.OfferDisabledSource;
import ru.yandex.market.marketpromo.model.OfferId;
import ru.yandex.market.marketpromo.model.OfferPromoBase;
import ru.yandex.market.marketpromo.utils.IdentityUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.hasValue;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.market.marketpromo.core.test.generator.OfferPromoBases.basePrice;
import static ru.yandex.market.marketpromo.core.test.generator.OfferPromoBases.id;
import static ru.yandex.market.marketpromo.core.test.generator.OfferPromoBases.promoBase;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.DEFAULT_SHOP_ID;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.activePromos;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.datacampOffer;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.disabled;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.disabledSource;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.generateDatacampCAGPromos;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.generateDatacampDDPromos;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.generateDatacampOfferList;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.hasDefaults;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.marketSku;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.name;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.potentialPromo;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.potentialPromos;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.price;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.shopSku;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.stocks;

public class DatacampOfferDaoTest extends ServiceTestBase {

    private static final String OFFER_1 = "offer 1";
    private static final String OFFER_2 = "offer 2";

    @Autowired
    private DatacampOfferDao offerDao;

    @Test
    void shouldStreamOffersFromTable() {
        offerDao.replace(generateDatacampOfferList(100));

        assertThat(offerDao.stream(1000).collect(Collectors.toUnmodifiableSet()), hasSize(100));
    }

    @Test
    void shouldInsertOffersBatch() {
        List<DatacampOffer> expected = List.of(
                datacampOffer(
                        name(OFFER_1),
                        shopSku(OFFER_1),
                        price(1000),
                        potentialPromo(Promos.DEFAULT_PROMO_ID, BigDecimal.valueOf(1500))
                ),
                datacampOffer(
                        name(OFFER_2),
                        shopSku(OFFER_2),
                        price(1500),
                        stocks(34L),
                        potentialPromo(Promos.DEFAULT_PROMO_ID, BigDecimal.valueOf(1500))
                )
        );
        offerDao.replace(expected);

        List<DatacampOffer> offers = offerDao.selectByOfferIds(List.of(
                OfferId.of(IdentityUtils.hashId(OFFER_1), DEFAULT_SHOP_ID),
                OfferId.of(IdentityUtils.hashId(OFFER_2), DEFAULT_SHOP_ID)
        ));

        assertThat(offers, hasSize(2));
        assertThat(offers, hasItems(
                allOf(
                        hasDefaults(),
                        hasProperty("shopSku", is(OFFER_1)),
                        hasProperty("name", is(OFFER_1)),
                        hasProperty("price", comparesEqualTo(BigDecimal.valueOf(1000))),
                        hasProperty("potentialPromos", hasValue(allOf(
                                hasProperty("id", is(Promos.DEFAULT_PROMO_ID)),
                                hasProperty("basePrice", comparesEqualTo(BigDecimal.valueOf(1500)))
                        )))
                ), allOf(
                        hasDefaults(),
                        hasProperty("shopSku", is(OFFER_2)),
                        hasProperty("name", is(OFFER_2)),
                        hasProperty("price", comparesEqualTo(BigDecimal.valueOf(1500))),
                        hasProperty("stocks", is(34L)),
                        hasProperty("potentialPromos", hasValue(allOf(
                                hasProperty("id", is(Promos.DEFAULT_PROMO_ID)),
                                hasProperty("basePrice", comparesEqualTo(BigDecimal.valueOf(1500)))
                        )))
                )));
    }

    @Test
    void shouldReplaceOffersBatch() {
        List<DatacampOffer> expected = List.of(
                datacampOffer(
                        name(OFFER_1),
                        shopSku(OFFER_1),
                        price(1000),
                        potentialPromo(Promos.DEFAULT_PROMO_ID, BigDecimal.valueOf(1500))
                ),
                datacampOffer(
                        name(OFFER_2),
                        shopSku(OFFER_2),
                        disabled(true),
                        stocks(34L),
                        price(1500),
                        potentialPromo(Promos.DEFAULT_PROMO_ID, BigDecimal.valueOf(1500))
                )
        );
        offerDao.replace(expected);
        offerDao.replace(expected);

        List<DatacampOffer> offers = offerDao.selectByOfferIds(List.of(
                OfferId.of(IdentityUtils.hashId(OFFER_1), DEFAULT_SHOP_ID),
                OfferId.of(IdentityUtils.hashId(OFFER_2), DEFAULT_SHOP_ID)
        ));

        assertThat(offers, hasSize(2));
        assertThat(offers, hasItems(
                allOf(
                        hasDefaults(),
                        hasProperty("shopSku", is(OFFER_1)),
                        hasProperty("name", is(OFFER_1)),
                        hasProperty("price", comparesEqualTo(BigDecimal.valueOf(1000))),
                        hasProperty("disabled", is(false)),
                        hasProperty("potentialPromos", hasValue(allOf(
                                hasProperty("id", is(Promos.DEFAULT_PROMO_ID)),
                                hasProperty("basePrice", comparesEqualTo(BigDecimal.valueOf(1500)))
                        )))
                ), allOf(
                        hasDefaults(),
                        hasProperty("shopSku", is(OFFER_2)),
                        hasProperty("name", is(OFFER_2)),
                        hasProperty("price", comparesEqualTo(BigDecimal.valueOf(1500))),
                        hasProperty("disabled", is(true)),
                        hasProperty("stocks", is(34L)),
                        hasProperty("potentialPromos", hasValue(allOf(
                                hasProperty("id", is(Promos.DEFAULT_PROMO_ID)),
                                hasProperty("basePrice", comparesEqualTo(BigDecimal.valueOf(1500)))
                        )))
                )));
    }

    @Test
    void shouldUpdateOffer() {
        DatacampOffer expected = datacampOffer(
                name(OFFER_1),
                shopSku(OFFER_1),
                price(1000),
                potentialPromo(Promos.DEFAULT_PROMO_ID, BigDecimal.valueOf(1500))
        );

        offerDao.replace(List.of(expected));

        assertThat(offerDao.selectOne(OfferId.of(IdentityUtils.hashId(OFFER_1), DEFAULT_SHOP_ID)), allOf(
                hasDefaults(),
                hasProperty("shopSku", is(OFFER_1)),
                hasProperty("name", is(OFFER_1)),
                hasProperty("price", comparesEqualTo(BigDecimal.valueOf(1000))),
                hasProperty("disabled", is(false)),
                hasProperty("potentialPromos", hasValue(allOf(
                        hasProperty("id", is(Promos.DEFAULT_PROMO_ID)),
                        hasProperty("basePrice", comparesEqualTo(BigDecimal.valueOf(1500)))
                )))
        ));

        Set<DatacampOfferPromo> promos = Sets.union(
                generateDatacampDDPromos(1, 5),
                generateDatacampCAGPromos(6, 5)
        );

        offerDao.update(datacampOffer(
                name("some new offer"),
                shopSku(OFFER_1),
                marketSku(123L),
                disabledSource(OfferDisabledSource.MARKET_ABO),
                disabledSource(OfferDisabledSource.MARKET_IDX),
                potentialPromos(promos.stream()
                        .map(DatacampOfferPromo::getPromoBase)
                        .collect(Collectors.toUnmodifiableSet())),
                activePromos(promos),
                price(2000),
                disabled(true),
                potentialPromo(Promos.DEFAULT_PROMO_ID, BigDecimal.valueOf(2500))
        ));

        assertThat(offerDao.selectOne(OfferId.of(IdentityUtils.hashId(OFFER_1), DEFAULT_SHOP_ID)), allOf(
                hasDefaults(),
                hasProperty("shopSku", is(OFFER_1)),
                hasProperty("name", is("some new offer")),
                hasProperty("marketSku", comparesEqualTo(123L)),
                hasProperty("price", comparesEqualTo(BigDecimal.valueOf(2000))),
                hasProperty("disabled", is(true)),
                hasProperty("disabledSources", hasItems(
                        OfferDisabledSource.MARKET_ABO, OfferDisabledSource.MARKET_IDX)),
                hasProperty("activePromos", aMapWithSize(10)),
                hasProperty("potentialPromos", hasValue(allOf(
                        hasProperty("id", is(Promos.DEFAULT_PROMO_ID)),
                        hasProperty("basePrice", comparesEqualTo(BigDecimal.valueOf(2500)))
                )))
        ));
    }

    @Test
    void shouldSelectOffers() {
        List<DatacampOffer> offers = generateDatacampOfferList(100);

        offerDao.replace(offers);

        Set<OfferId> shopSkus = offers.stream()
                .map(DatacampOffer::getOfferId)
                .collect(Collectors.toUnmodifiableSet());

        List<DatacampOffer> resultList = offerDao.selectByOfferIds(
                shopSkus
        );

        assertThat(resultList, hasSize(offers.size()));
        assertThat(resultList, everyItem(hasProperty("potentialPromos", aMapWithSize(10))));
        assertThat(resultList, everyItem(hasProperty("activePromos", aMapWithSize(6))));
    }

    @Test
    void shouldMarkToDeleteOffers() {
        List<DatacampOffer> offers = generateDatacampOfferList(100);

        offerDao.replace(offers);
        offerDao.delete(offers);

        assertThat(offerDao.stream(100).collect(Collectors.toUnmodifiableSet()), everyItem(
                hasProperty("deletedAt", notNullValue())
        ));
    }
}
