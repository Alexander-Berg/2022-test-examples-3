package ru.yandex.market.marketpromo.core.dao;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.marketpromo.core.test.ServiceTestBase;
import ru.yandex.market.marketpromo.core.test.generator.DatacampOfferPromoMechanics;
import ru.yandex.market.marketpromo.core.test.generator.Offers;
import ru.yandex.market.marketpromo.filter.AssortmentFilter;
import ru.yandex.market.marketpromo.filter.AssortmentRequest;
import ru.yandex.market.marketpromo.model.DatacampOffer;
import ru.yandex.market.marketpromo.model.DatacampOfferPromo;
import ru.yandex.market.marketpromo.model.DirectDiscountOfferPropertiesCore;
import ru.yandex.market.marketpromo.model.LocalOffer;
import ru.yandex.market.marketpromo.model.MechanicsType;
import ru.yandex.market.marketpromo.model.OfferId;
import ru.yandex.market.marketpromo.model.OfferPromoBase;
import ru.yandex.market.marketpromo.model.OfferPromoParticipation;
import ru.yandex.market.marketpromo.model.PagerList;
import ru.yandex.market.marketpromo.model.Promo;
import ru.yandex.market.marketpromo.model.PromoKey;
import ru.yandex.market.marketpromo.service.AssortmentService;
import ru.yandex.market.marketpromo.utils.IdentityUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.DEFAULT_SHOP_ID;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.activePromos;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.categoryId;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.disabled;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.generateDatacampOfferList;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.hasDefaults;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.name;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.potentialPromo;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.price;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.shop;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.shopSku;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.stocks;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.cheapestAsGift;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.cheapestAsGiftLocal;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.directDiscount;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.id;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.promo;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.status;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.trade;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.warehouse;

public class LocalPromoOfferDaoWithoutCacheTest extends ServiceTestBase{

    private static final long SHOP_ID = 12L;
    private static final String SSKU_1 = "ssku-1";
    private static final String SSKU_2 = "ssku-2";

    @Autowired
    private PromoDao promoDao;
    @Autowired
    private DatacampOfferDao offerDao;
    @Autowired
    private LocalPromoOfferDao localPromoOfferDao;
    @Autowired
    private AssortmentPromoParticipationDao assortmentPromoParticipationDao;
    @Autowired
    private AssortmentService assortmentService;
    @Autowired
    private ConfigurationDao configurationDao;

    @BeforeEach
    public void configure() {
        disableCache();
    }

    @Test
    void shouldSelectOffersByRequest() {
        List<DatacampOffer> datacampOffers = generateDatacampOfferList(100);

        offerDao.replace(datacampOffers);

        Promo promo = promoDao.replace(promoDao.replace(promo(
                id(IdentityUtils.hashId("promo 1")),
                directDiscount()
        )));

        PagerList<LocalOffer> pagerList = localPromoOfferDao.getOffersByRequest(
                AssortmentRequest.builder(promo.toPromoKey())
                        .build());

        assertThat(pagerList.getList(), hasSize(datacampOffers.size()));
        assertThat(pagerList.getList(), everyItem(allOf(
                hasDefaults(),
                hasProperty("offerId"),
                hasProperty("wareMd5"),
                hasProperty("stocksByWarehouse"),
                hasProperty("activePromos", hasSize(6)),
                hasProperty("promos", aMapWithSize(1))
        )));
        assertThat(pagerList.getList(), hasSize(datacampOffers.size()));
    }

    @Test
    @Disabled
    void shouldSelectOffersForBigPromo() {
        Promo promo = promoDao.replace(promoDao.replace(promo(
                id(IdentityUtils.hashId("some promo")),
                directDiscount()
        )));

        OfferPromoBase promoBase = OfferPromoBase.builder()
                .id(promo.getId())
                .name(promo.getName())
                .updatedAt(promo.getUpdatedAt())
                .build();

        List<DatacampOffer> datacampOffers = generateDatacampOfferList(40000,
                o -> o.potentialPromos(Collections.singletonMap(promo.getId(), promoBase))
                        .activePromos(Collections.singletonMap(promo.getId(), DatacampOfferPromo.builderOf(promoBase)
                                .mechanicsProperties(DirectDiscountOfferPropertiesCore.builder()
                                        .fixedPrice(BigDecimal.ZERO)
                                        .fixedBasePrice(BigDecimal.valueOf(100))
                                        .build())
                                .build())));
        assortmentService.createAssortment(datacampOffers);

        PagerList<LocalOffer> pagerList = localPromoOfferDao.getOffersByRequest(
                AssortmentRequest.builder(promo.toPromoKey())
                        .limit(50)
                        .build());

        assertThat(pagerList.getList(), hasSize(50));
    }

    @Test
    void shouldSelectOffersByRequestWithPaging() {
        List<DatacampOffer> datacampOffers = generateDatacampOfferList(1000);

        offerDao.replace(datacampOffers);

        Promo promo = promoDao.replace(promoDao.replace(promo(
                id(IdentityUtils.hashId("promo 1")),
                directDiscount()
        )));

        PagerList<LocalOffer> pagerList = localPromoOfferDao.getOffersByRequest(
                AssortmentRequest.builder(promo.toPromoKey())
                        .limit(100)
                        .offset(100)
                        .build());

        long totalCount = localPromoOfferDao.getOffersCountByRequest(
                AssortmentRequest.builder(promo.toPromoKey()).build());

        assertThat(pagerList.getList(), hasSize(100));
        assertThat(pagerList.getList(), everyItem(allOf(
                hasDefaults(),
                hasProperty("offerId"),
                hasProperty("wareMd5"),
                hasProperty("stocksByWarehouse"),
                hasProperty("activePromos", hasSize(6)),
                hasProperty("promos", aMapWithSize(1))
        )));
        assertThat(totalCount, comparesEqualTo(1000L));
    }

    @Test
    void shouldSelectOffersByName() {
        List<DatacampOffer> datacampOffers = generateDatacampOfferList(10);
        offerDao.replace(datacampOffers);

        Promo promo = promoDao.replace(promoDao.replace(promo(
                id(IdentityUtils.hashId("promo 1")),
                directDiscount()
        )));

        PagerList<LocalOffer> pagerList = localPromoOfferDao.getOffersByRequest(
                AssortmentRequest.builder(promo.toPromoKey())
                        .filter(AssortmentFilter.NAME, datacampOffers.get(0).getName())
                        .build());

        assertThat(pagerList.getList(), everyItem(allOf(
                hasDefaults(),
                hasProperty("offerId"),
                hasProperty("wareMd5"),
                hasProperty("stocksByWarehouse"),
                hasProperty("activePromos", hasSize(6)),
                hasProperty("promos", aMapWithSize(1))
        )));

        assertThat(pagerList.getList(), hasSize(1));
    }

    @Test
    void shouldSelectOffersByWarehouse() {
        offerDao.replace(generateDatacampOfferList(10));

        Promo promo = promoDao.replace(promoDao.replace(promo(
                id(IdentityUtils.hashId("promo 1")),
                directDiscount()
        )));

        PagerList<LocalOffer> pagerList = localPromoOfferDao.getOffersByRequest(
                AssortmentRequest.builder(promo.toPromoKey())
                        .filter(AssortmentFilter.WAREHOUSE_ID, 3L)
                        .build());

        assertThat(pagerList.getList(), everyItem(allOf(
                hasDefaults(),
                hasProperty("offerId"),
                hasProperty("wareMd5"),
                hasProperty("stocksByWarehouse"),
                hasProperty("activePromos", hasSize(6)),
                hasProperty("promos", aMapWithSize(1))
        )));

        assertThat(pagerList.getList(), hasSize(1));
    }

    @Test
    void shouldSelectOffersByOfferId() {
        offerDao.replace(generateDatacampOfferList(10));

        Promo promo = promoDao.replace(promoDao.replace(promo(
                id(IdentityUtils.hashId("promo 1")),
                directDiscount()
        )));

        PagerList<LocalOffer> pagerList = localPromoOfferDao.getOffersByRequest(
                AssortmentRequest.builder(promo.toPromoKey())
                        .filter(AssortmentFilter.OFFER_ID,
                                OfferId.of(IdentityUtils.hashId("offer shop sku 1"), DEFAULT_SHOP_ID))
                        .build());

        assertThat(pagerList.getList(), everyItem(allOf(
                hasDefaults(),
                hasProperty("offerId"),
                hasProperty("wareMd5"),
                hasProperty("stocksByWarehouse"),
                hasProperty("activePromos", hasSize(6)),
                hasProperty("promos", aMapWithSize(1))
        )));

        assertThat(pagerList.getList(), hasSize(1));
    }

    @Test
    void shouldSelectOffersByShopSku() {
        List<DatacampOffer> datacampOffers = generateDatacampOfferList(10);
        offerDao.replace(datacampOffers);

        Promo promo = promoDao.replace(promoDao.replace(promo(
                id(IdentityUtils.hashId("promo 1")),
                directDiscount()
        )));

        PagerList<LocalOffer> pagerList = localPromoOfferDao.getOffersByRequest(
                AssortmentRequest.builder(promo.toPromoKey())
                        .filterList(AssortmentFilter.SSKU, List.of(datacampOffers.get(0).getShopSku()))
                        .build());

        assertThat(pagerList.getList(), everyItem(allOf(
                hasDefaults(),
                hasProperty("offerId"),
                hasProperty("wareMd5"),
                hasProperty("stocksByWarehouse"),
                hasProperty("activePromos", hasSize(6)),
                hasProperty("promos", aMapWithSize(1))
        )));

        assertThat(pagerList.getList(), hasSize(1));
    }

    @Test
    void shouldSelectOffersWithNonEmptyStocks() {
        offerDao.replace(generateDatacampOfferList(10));
        offerDao.replace(generateDatacampOfferList(1, Offers.warehouse(2), stocks(12)));

        Promo promo = promoDao.replace(promoDao.replace(promo(
                id(IdentityUtils.hashId("promo 1")),
                warehouse(2),
                directDiscount()
        )));

        PagerList<LocalOffer> pagerList = localPromoOfferDao.getOffersByRequest(
                AssortmentRequest.builder(promo.toPromoKey())
                        .filter(AssortmentFilter.HIDE_EMPTY_STOCKS, true)
                        .build());

        assertThat(pagerList.getList(), everyItem(allOf(
                hasDefaults(),
                hasProperty("offerId"),
                hasProperty("wareMd5"),
                hasProperty("stocksByWarehouse"),
                hasProperty("activePromos", hasSize(6)),
                hasProperty("promos", aMapWithSize(1))
        )));

        assertThat(pagerList.getList(), hasSize(1));
    }

    @Test
    void shouldSelectOffersByCategory() {
        offerDao.replace(generateDatacampOfferList(10));
        offerDao.replace(generateDatacampOfferList(1, categoryId(1234)));

        Promo promo = promoDao.replace(promoDao.replace(promo(
                id(IdentityUtils.hashId("promo 1")),
                directDiscount()
        )));

        PagerList<LocalOffer> pagerList = localPromoOfferDao.getOffersByRequest(
                AssortmentRequest.builder(promo.toPromoKey())
                        .filter(AssortmentFilter.CATEGORY_ID, 1234L)
                        .build());

        assertThat(pagerList.getList(), everyItem(allOf(
                hasDefaults(),
                hasProperty("offerId"),
                hasProperty("wareMd5"),
                hasProperty("stocksByWarehouse"),
                hasProperty("activePromos", hasSize(6)),
                hasProperty("promos", aMapWithSize(1))
        )));

        assertThat(pagerList.getList(), hasSize(1));
    }

    @Test
    void shouldSelectOnlyActiveOffers() {
        offerDao.replace(generateDatacampOfferList(10, disabled(true)));
        offerDao.replace(generateDatacampOfferList(1, disabled(false)));

        Promo promo = promoDao.replace(promoDao.replace(promo(
                id(IdentityUtils.hashId("promo 1")),
                directDiscount()
        )));

        PagerList<LocalOffer> pagerList = localPromoOfferDao.getOffersByRequest(
                AssortmentRequest.builder(promo.toPromoKey())
                        .filter(AssortmentFilter.HIDE_DISABLED, true)
                        .build());

        assertThat(pagerList.getList(), everyItem(allOf(
                hasDefaults(),
                hasProperty("offerId"),
                hasProperty("wareMd5"),
                hasProperty("stocksByWarehouse"),
                hasProperty("activePromos", hasSize(6)),
                hasProperty("promos", aMapWithSize(1))
        )));

        assertThat(pagerList.getList(), hasSize(1));
    }

    @Test
    void shouldSelectOnlyParticipatedOffers() {
        Promo promo = promoDao.replace(promoDao.replace(promo(
                id(IdentityUtils.hashId("promo 10")),
                directDiscount()
        )));

        offerDao.replace(generateDatacampOfferList(10));
        offerDao.replace(generateDatacampOfferList(1, activePromos(
                DatacampOfferPromoMechanics.directDiscount(promo.getId(),
                        DatacampOfferPromoMechanics.basePrice(BigDecimal.valueOf(9999)),
                        DatacampOfferPromoMechanics.price(BigDecimal.valueOf(5000))
                )
        )));

        PagerList<LocalOffer> pagerList = localPromoOfferDao.getOffersByRequest(
                AssortmentRequest.builder(promo.toPromoKey())
                        .filter(AssortmentFilter.PARTICIPATE, true)
                        .build());

        assertThat(pagerList.getList(), everyItem(allOf(
                hasDefaults(),
                hasProperty("offerId"),
                hasProperty("wareMd5"),
                hasProperty("stocksByWarehouse"),
                hasProperty("activePromos", hasSize(7)),
                hasProperty("promos", aMapWithSize(1))
        )));

        assertThat(pagerList.getList(), hasSize(1));
    }

    @Test
    void shouldFillWarehouseByStocksMap() {
        List<DatacampOffer> offers = generateDatacampOfferList(500);

        offerDao.replace(offers);

        Promo promo = promoDao.replace(promoDao.replace(promo(
                id(IdentityUtils.hashId("promo 1")),
                directDiscount()
        )));

        final List<OfferId> offerIds = offers.stream()
                .map(DatacampOffer::getOfferId)
                .collect(Collectors.toList());

        final List<LocalOffer> resultList = localPromoOfferDao.getOffers(promo, offerIds);

        assertThat(resultList, hasSize(offers.size()));
    }

    @Test
    void shouldGetParticipatedOfferKeys() {
        Promo promo = promoDao.replace(promoDao.replace(promo(
                id(IdentityUtils.hashId("promo 1")),
                directDiscount()
        )));

        offerDao.replace(List.of(
                Offers.datacampOffer(
                        name(SSKU_1),
                        shopSku(SSKU_1),
                        shop(SHOP_ID),
                        price(1000),
                        potentialPromo(promo.getId(), BigDecimal.valueOf(1500))
                ),
                Offers.datacampOffer(
                        name(SSKU_2),
                        shopSku(SSKU_2),
                        shop(SHOP_ID),
                        price(1000),
                        potentialPromo(promo.getId(), BigDecimal.valueOf(1500))
                )
        ));
        OfferPromoParticipation assortmentItem = OfferPromoParticipation.builder()
                .promoId(promo.getId())
                .offerId(OfferId.of(IdentityUtils.hashId(SSKU_1), SHOP_ID))
                .participate(true)
                .build();
        OfferPromoParticipation notAssortmentItem = OfferPromoParticipation.builder()
                .promoId(promo.getId())
                .offerId(OfferId.of(IdentityUtils.hashId(SSKU_2), SHOP_ID))
                .participate(false)
                .build();

        assortmentPromoParticipationDao.saveParticipateToPromo(
                PromoKey.of(promo.getId(), MechanicsType.DIRECT_DISCOUNT),
                List.of(assortmentItem, notAssortmentItem));

        List<LocalOffer> offers = localPromoOfferDao.getParticipatedOffers(promo);

        assertThat(offers, hasSize(2));
        assertThat(offers,
                hasItems(allOf(
                        hasProperty("shopSku", is(SSKU_1)),
                        hasProperty("activePromos", not(empty()))
                ), allOf(
                        hasProperty("shopSku", is(SSKU_2)),
                        hasProperty("activePromos", empty())
                ))
        );
    }

    @Test
    void shouldSelectLocalActiveInfoOnlyForSelectedPromo() {
        Promo promo = promoDao.replace(promoDao.replace(promo(
                id(IdentityUtils.hashId("promo 1")),
                directDiscount()
        )));

        Promo anotherPromo = promoDao.replace(promoDao.replace(promo(
                id(IdentityUtils.hashId("promo 2")),
                directDiscount()
        )));

        offerDao.replace(List.of(
                Offers.datacampOffer(
                        name(SSKU_1),
                        shopSku(SSKU_1),
                        shop(SHOP_ID),
                        price(1000),
                        potentialPromo(promo.getId(), BigDecimal.valueOf(1500)),
                        potentialPromo(anotherPromo.getId(), BigDecimal.valueOf(1500)),
                        activePromos(DatacampOfferPromoMechanics.directDiscount(promo.getId(),
                                DatacampOfferPromoMechanics.basePrice(BigDecimal.valueOf(9999)),
                                DatacampOfferPromoMechanics.price(BigDecimal.valueOf(5000))
                        ))
                )
        ));

        OfferPromoParticipation assortmentItem = OfferPromoParticipation.builder()
                .promoId(anotherPromo.getId())
                .offerId(OfferId.of(IdentityUtils.hashId(SSKU_1), SHOP_ID))
                .participate(true)
                .build();

        assortmentPromoParticipationDao.saveParticipateToPromo(
                PromoKey.of(anotherPromo.getId(), MechanicsType.DIRECT_DISCOUNT),
                List.of(assortmentItem));

        LocalOffer localOffer = localPromoOfferDao.getOffers(promo,
                List.of(OfferId.of(IdentityUtils.hashId(SSKU_1), SHOP_ID))).get(0);

        assertThat(localOffer.getActivePromos(), hasSize(1));
        assertThat(localOffer.getActivePromos(), hasItem(
                hasProperty("promoId", is(promo.getId()))
        ));

        assertThat(localOffer.getPromos(), aMapWithSize(2));
        assertThat(localOffer.getPromos(), hasEntry(
                is(promo.toPromoKey()),
                hasProperty("participation", is(true))
        ));
        assertThat(localOffer.getPromos(), hasEntry(
                is(anotherPromo.toPromoKey()),
                hasProperty("participation", is(false))
        ));
    }

    @Test
    void shouldReturnActiveInfo() {
        Promo promo = promoDao.replace(promoDao.replace(promo(
                id(IdentityUtils.hashId("promo 1")),
                directDiscount()
        )));

        Promo anotherPromo = promoDao.replace(promoDao.replace(promo(
                id(IdentityUtils.hashId("promo 2")),
                directDiscount()
        )));

        offerDao.replace(List.of(
                Offers.datacampOffer(
                        name(SSKU_1),
                        shopSku(SSKU_1),
                        shop(SHOP_ID),
                        price(1000),
                        potentialPromo(promo.getId(), BigDecimal.valueOf(1500)),
                        potentialPromo(anotherPromo.getId(), BigDecimal.valueOf(1500)),
                        activePromos(DatacampOfferPromoMechanics.directDiscount(promo.getId(),
                                DatacampOfferPromoMechanics.basePrice(BigDecimal.valueOf(9999)),
                                DatacampOfferPromoMechanics.price(BigDecimal.valueOf(5000))
                        ))
                )
        ));

        OfferPromoParticipation assortmentItem = OfferPromoParticipation.builder()
                .promoId(anotherPromo.getId())
                .offerId(OfferId.of(IdentityUtils.hashId(SSKU_1), SHOP_ID))
                .participate(true)
                .build();

        assortmentPromoParticipationDao.saveParticipateToPromo(
                PromoKey.of(anotherPromo.getId(), MechanicsType.DIRECT_DISCOUNT),
                List.of(assortmentItem));

        LocalOffer localOffer = localPromoOfferDao.getOffers(anotherPromo,
                List.of(OfferId.of(IdentityUtils.hashId(SSKU_1), SHOP_ID))).get(0);

        assertThat(localOffer.getActivePromos(), hasSize(2));
        assertThat(localOffer.getActivePromos(), hasItems(
                hasProperty("promoId", is(promo.getId())),
                hasProperty("promoId", is(anotherPromo.getId()))
        ));

        assertThat(localOffer.getPromos(), aMapWithSize(2));
        assertThat(localOffer.getPromos(), hasEntry(
                is(promo.toPromoKey()),
                hasProperty("participation", is(true))
        ));
        assertThat(localOffer.getPromos(), hasEntry(
                is(anotherPromo.toPromoKey()),
                hasProperty("participation", is(true))
        ));
    }

    @Test
    void shouldNotReturnDeletedOffersInGetOffersByRequestMethod() {
        disableCache();

        List<DatacampOffer> offers = generateDatacampOfferList(100);

        Promo promo = promoDao.replace(promoDao.replace(promo(
                id(IdentityUtils.hashId("promo 1")),
                directDiscount()
        )));

        offerDao.replace(offers);

        var pagerList =
                localPromoOfferDao.getOffersByRequest(AssortmentRequest.builder(promo.toPromoKey()).build());

        assertThat(
                pagerList.getList(),
                hasSize(100)
        );

        long total =
                localPromoOfferDao.getOffersCountByRequest(AssortmentRequest.builder(promo.toPromoKey()).build());

        assertThat(total, comparesEqualTo(100L));

        offerDao.delete(offers);

        pagerList =
                localPromoOfferDao.getOffersByRequest(AssortmentRequest.builder(promo.toPromoKey()).build());

        total = localPromoOfferDao.getOffersCountByRequest(AssortmentRequest.builder(promo.toPromoKey()).build());

        assertThat(
                pagerList.getList(),
                empty()
        );

        assertThat(total, comparesEqualTo(0L));
    }

    @Test
    void shouldNotReturnDeletedOffersInGetOffersCountByRequestMethod() {
        List<DatacampOffer> offers = generateDatacampOfferList(100);

        Promo promo = promoDao.replace(promoDao.replace(promo(
                id(IdentityUtils.hashId("promo 1")),
                directDiscount()
        )));

        offerDao.replace(offers);

        assertThat(
                localPromoOfferDao.getOffersCountByRequest(AssortmentRequest.builder(promo.toPromoKey()).build()),
                comparesEqualTo(100L)
        );

        offerDao.delete(offers);

        assertThat(
                localPromoOfferDao.getOffersCountByRequest(AssortmentRequest.builder(promo.toPromoKey()).build()),
                comparesEqualTo(0L)
        );
    }

    @Test
    void shouldNotReturnDeletedOffersInGetOffersWithOfferIdListMethod() {
        List<DatacampOffer> offers = generateDatacampOfferList(100);

        Promo promo = promoDao.replace(promoDao.replace(promo(
                id(IdentityUtils.hashId("promo 1")),
                directDiscount()
        )));

        offerDao.replace(offers);

        var offerIds = offers.stream()
                .map(DatacampOffer::getOfferId)
                .collect(Collectors.toUnmodifiableList());

        assertThat(localPromoOfferDao.getOffers(promo, offerIds), hasSize(100));

        offerDao.delete(offers);

        assertThat(localPromoOfferDao.getOffers(promo, offerIds), empty());
    }

    @Test
    @Disabled
    void shouldNotReturnDeletedOffersInGetParticipatedOffersMethod() {
        List<DatacampOffer> offers = generateDatacampOfferList(100);

        Promo promo = promoDao.replace(promoDao.replace(promo(
                id(IdentityUtils.hashId("promo 1")),
                directDiscount()
        )));

        offerDao.replace(offers);

        assortmentPromoParticipationDao.saveParticipateToPromo(
                PromoKey.of(promo.getId(), MechanicsType.DIRECT_DISCOUNT),
                offers.stream()
                        .map(offer -> OfferPromoParticipation.builder()
                                .promoId(promo.getId())
                                .offerId(offer.getOfferId())
                                .participate(true)
                                .build())
                        .collect(Collectors.toUnmodifiableList()));

        assertThat(localPromoOfferDao.getParticipatedOffers(promo), hasSize(100));

        offerDao.delete(offers);

        assertThat(localPromoOfferDao.getParticipatedOffers(promo), empty());
    }

    @Test
    void shouldNotReturnDeletedOffersInGetMinimalOffersMethod() {
        List<DatacampOffer> offers = generateDatacampOfferList(100);

        Promo promo = promoDao.replace(promoDao.replace(promo(
                id(IdentityUtils.hashId("promo 1")),
                directDiscount()
        )));

        offerDao.replace(offers);

        var offerIds = offers.stream()
                .map(DatacampOffer::getOfferId)
                .collect(Collectors.toUnmodifiableList());

        assertThat(localPromoOfferDao.getMinimalOffers(offerIds), hasSize(100));

        offerDao.delete(offers);

        assertThat(localPromoOfferDao.getMinimalOffers(offerIds), empty());
    }

    @Test
    void shouldReturnOffersForLocalPromo() {
        List<DatacampOffer> offers = generateDatacampOfferList(2);

        Promo promo = promoDao.replace(promoDao.replace(promo(
                id(IdentityUtils.hashId("promo 1")),
                cheapestAsGiftLocal()
        )));

        offerDao.replace(offers);

        final PagerList<LocalOffer> cheapestAsGiftAssortment =
                assortmentService.getAssortment(AssortmentRequest.builder(promo.toPromoKey())
                        .build());


        final List<OfferPromoParticipation> offersParticipation = cheapestAsGiftAssortment.getList().stream()
                .map(offer -> participationOf(promo, offer))
                .collect(Collectors.toList());

        assortmentService.markToParticipate(promo, offersParticipation);

        final PagerList<LocalOffer> cheapestAsGiftAssortmentResult =
                assortmentService.getAssortment(AssortmentRequest.builder(promo.toPromoKey())
                        .build(), true);

        assertThat(cheapestAsGiftAssortmentResult.getList(), hasSize(2));
    }

    private void disableCache() {
        configurationDao.set("assortment.cache.useAssortmentCache", false);
    }


    @Nonnull
    private OfferPromoParticipation participationOf(@Nonnull Promo promo,
                                                    @Nonnull LocalOffer offer) {
        return OfferPromoParticipation.builder()
                .promoId(promo.getId())
                .offerId(offer.getOfferId())
                .updatedAt(clock.dateTime())
                .participate(true)
                .build();
    }

}
