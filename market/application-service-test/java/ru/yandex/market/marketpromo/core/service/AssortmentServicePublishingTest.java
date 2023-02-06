package ru.yandex.market.marketpromo.core.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import Market.DataCamp.API.DatacampMessageOuterClass;
import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferPromos;
import Market.DataCamp.DataCampUnitedOffer;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.marketpromo.core.dao.DatacampOfferDao;
import ru.yandex.market.marketpromo.core.dao.LocalPromoOfferDao;
import ru.yandex.market.marketpromo.core.dao.PromoDao;
import ru.yandex.market.marketpromo.core.data.source.logbroker.OfferLogbrokerEvent;
import ru.yandex.market.marketpromo.core.data.source.offerstorage.util.OfferDataConverter;
import ru.yandex.market.marketpromo.core.service.impl.CachedAssortmentService;
import ru.yandex.market.marketpromo.core.service.task.AssortmentPublishingProcessorTask;
import ru.yandex.market.marketpromo.core.test.ServiceTestBase;
import ru.yandex.market.marketpromo.filter.AssortmentFilter;
import ru.yandex.market.marketpromo.filter.AssortmentRequest;
import ru.yandex.market.marketpromo.model.CategoryIdWithDiscount;
import ru.yandex.market.marketpromo.model.DatacampOfferPromo;
import ru.yandex.market.marketpromo.model.DirectDiscountOfferParticipation;
import ru.yandex.market.marketpromo.model.DirectDiscountOfferPropertiesCore;
import ru.yandex.market.marketpromo.model.DirectDiscountProperties;
import ru.yandex.market.marketpromo.model.LocalOffer;
import ru.yandex.market.marketpromo.model.MechanicsOfferProperties;
import ru.yandex.market.marketpromo.model.MechanicsType;
import ru.yandex.market.marketpromo.model.OfferId;
import ru.yandex.market.marketpromo.model.OfferPromoBase;
import ru.yandex.market.marketpromo.model.OfferPromoParticipation;
import ru.yandex.market.marketpromo.model.PagerList;
import ru.yandex.market.marketpromo.model.Promo;
import ru.yandex.market.marketpromo.model.processing.PublishingInfo;
import ru.yandex.market.marketpromo.model.processing.PublishingStatus;
import ru.yandex.market.marketpromo.service.AssortmentService;
import ru.yandex.market.marketpromo.utils.IdentityUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.hasValue;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.activePromos;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.basePrice;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.categoryId;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.datacampOffer;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.name;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.potentialPromo;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.price;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.shop;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.shopSku;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.warehouse;
import static ru.yandex.market.marketpromo.core.test.generator.PromoMechanics.minimalDiscountPercentSize;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.CAG_PROMO_KEY;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.DD_PROMO_KEY;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.cheapestAsGift;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.directDiscount;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.id;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.promo;

public class AssortmentServicePublishingTest extends ServiceTestBase {

    private static final int WAREHOUSE_ID = 123;
    private static final long SHOP_ID = 12L;
    private static final String SSKU_1 = "ssku-1";
    private static final String SSKU_2 = "ssku-2";
    private static final String SSKU_3 = "ssku-3";
    private static final String SSKU_4 = "ssku-4";

    @Autowired
    private AssortmentService assortmentService;
    @Autowired
    private PromoDao promoDao;
    @Autowired
    private LocalPromoOfferDao localPromoOfferDao;
    @Autowired
    private DatacampOfferDao datacampOfferDao;
    @Autowired
    private Queue<OfferLogbrokerEvent> mockedLogbrokerQueue;
    @Autowired
    private OfferDataConverter offerDataConverter;
    @Autowired
    private AssortmentPublishingProcessorTask publishingProcessorTask;
    @Autowired
    private CachedAssortmentService cachedAssortmentService;


    private Promo directDiscount;
    private Promo cheapestAsGift;

    @BeforeEach
    void setUp() {
        directDiscount = promoDao.replace(promo(
                id(DD_PROMO_KEY.getId()),
                directDiscount(
                        minimalDiscountPercentSize(10)
                )
        ));
        cheapestAsGift = promoDao.replace(promo(
                id(CAG_PROMO_KEY.getId()),
                cheapestAsGift()
        ));

        datacampOfferDao.replace(List.of(
                datacampOffer(
                        name(SSKU_1),
                        shopSku(SSKU_1),
                        warehouse(WAREHOUSE_ID),
                        shop(SHOP_ID),
                        price(1000),
                        basePrice(1500),
                        categoryId(123L),
                        potentialPromo(directDiscount.getId(), BigDecimal.valueOf(150))
                ),
                datacampOffer(
                        name(SSKU_2),
                        shopSku(SSKU_2),
                        warehouse(WAREHOUSE_ID),
                        shop(SHOP_ID),
                        price(1000),
                        basePrice(1500),
                        categoryId(123L),
                        potentialPromo(cheapestAsGift.getId())
                ),
                datacampOffer(
                        name(SSKU_3),
                        shopSku(SSKU_3),
                        warehouse(WAREHOUSE_ID),
                        shop(SHOP_ID),
                        price(1000),
                        basePrice(1500),
                        categoryId(123L),
                        potentialPromo(directDiscount.getId(), BigDecimal.valueOf(150)),
                        potentialPromo(cheapestAsGift.getId())
                )
        ));

        assertThat(mockedLogbrokerQueue, empty());

        cachedAssortmentService.refreshAssortmentCache();
    }

    @AfterEach
    void clean() {
        mockedLogbrokerQueue.clear();
    }

    @Test
    void shouldPublishDirectDiscountsToLogbroker() {
        final PagerList<LocalOffer> directDiscountAssortment =
                assortmentService.getAssortment(AssortmentRequest.builder(directDiscount.toPromoKey())
                        .build());

        assertThat(directDiscountAssortment.getList(), hasSize(2));

        final List<DirectDiscountOfferParticipation> offerParticipation = directDiscountAssortment.getList().stream()
                .map(offer -> directDiscountParticipationOf(directDiscount, offer))
                .collect(Collectors.toList());

        assortmentService.markDirectDiscountToParticipate(DD_PROMO_KEY, offerParticipation);
        assortmentService.publishPromoAssortment(DD_PROMO_KEY);

        assertThat(mockedLogbrokerQueue, hasSize(1));

        final DatacampMessageOuterClass.DatacampMessage payload = mockedLogbrokerQueue.poll().getPayload();
        assertThat(payload.getUnitedOffersList(), not(empty()));
        final List<DataCampUnitedOffer.UnitedOffer> offerList = payload.getUnitedOffers(0).getOfferList();
        assertThat(offerList, hasSize(2));
        assertThat(offerList, everyItem(allOf(
                hasProperty("basic", allOf(
                        hasProperty("promos", hasProperty("anaplanPromos",
                                hasProperty("activePromos",
                                        hasProperty("promosList", empty()))))
                )),
                hasProperty("serviceMap", hasValue(allOf(
                        hasProperty("promos", hasProperty("anaplanPromos",
                                hasProperty("activePromos",
                                        hasProperty("promosList", hasItem(allOf(
                                                hasProperty("id",
                                                        is(directDiscount.getPromoId())),
                                                hasProperty("directDiscount", allOf(
                                                        hasProperty("price"),
                                                        hasProperty("basePrice")
                                                ))
                                        ))))))
                )))
        )));
        assertThat(offerList, hasItems(
                hasProperty("basic", hasProperty("identifiers",
                        hasProperty("offerId", is(SSKU_1)))),
                hasProperty("basic", hasProperty("identifiers",
                        hasProperty("offerId", is(SSKU_3))))
        ));
    }

    @Test
    void shouldPublishCheapeastAsGiftToLogbroker() {
        final PagerList<LocalOffer> cheapestAsGiftAssortment =
                assortmentService.getAssortment(AssortmentRequest.builder(cheapestAsGift.toPromoKey())
                        .build());

        assertThat(cheapestAsGiftAssortment.getList(), not(empty()));

        final List<OfferPromoParticipation> offersParticipation = cheapestAsGiftAssortment.getList().stream()
                .map(offer -> participationOf(cheapestAsGift, offer))
                .collect(Collectors.toList());

        assortmentService.markToParticipate(cheapestAsGift, offersParticipation);
        assortmentService.publishPromoAssortment(CAG_PROMO_KEY);

        assertThat(mockedLogbrokerQueue, hasSize(1));

        final DatacampMessageOuterClass.DatacampMessage payload = mockedLogbrokerQueue.poll().getPayload();
        assertThat(payload.getUnitedOffersList(), not(empty()));
        final List<DataCampUnitedOffer.UnitedOffer> offerList = payload.getUnitedOffers(0).getOfferList();
        assertThat(offerList, hasSize(2));
        assertThat(offerList, everyItem(allOf(
                hasProperty("basic", allOf(
                        hasProperty("promos", hasProperty("anaplanPromos",
                                hasProperty("activePromos",
                                        hasProperty("promosList", empty()))))
                )),
                hasProperty("serviceMap", hasValue(allOf(
                        hasProperty("promos", hasProperty("anaplanPromos",
                                hasProperty("activePromos",
                                        hasProperty("promosList", hasItem(allOf(
                                                hasProperty("id",
                                                        is(cheapestAsGift.getPromoId())),
                                                hasProperty("cheapestAsGift")
                                        ))))))
                )))
        )));
        assertThat(offerList, hasItems(
                hasProperty("basic", hasProperty("identifiers",
                        hasProperty("offerId", is(SSKU_2)))),
                hasProperty("basic", hasProperty("identifiers",
                        hasProperty("offerId", is(SSKU_3))))
        ));
    }

    @Test
    void shouldGetCheapeastAsGift() {
        final PagerList<LocalOffer> cheapestAsGiftAssortment =
                assortmentService.getAssortment(AssortmentRequest.builder(cheapestAsGift.toPromoKey())
                        .filterList(AssortmentFilter.SSKU, List.of(SSKU_2))
                        .build());

        assertThat(cheapestAsGiftAssortment.getList(), not(empty()));
    }

    @Test
    void shouldFillLocalMechanicsPropertiesForPublishingWithoutActiveMechanicPropertiesIfExist() {
        datacampOfferDao.replace(List.of(
                datacampOffer(
                        name(SSKU_4),
                        shopSku(SSKU_4),
                        warehouse(WAREHOUSE_ID),
                        shop(SHOP_ID),
                        price(1000),
                        basePrice(1500),
                        categoryId(123L),
                        potentialPromo(directDiscount.getId(), BigDecimal.valueOf(150)),
                        activePromos(Set.of(
                                DatacampOfferPromo.builderOf(
                                        OfferPromoBase.builder()
                                                .id(directDiscount.getId())
                                                .updatedAt(clock.dateTime())
                                                .build()
                                )
                                        .mechanicsProperties(MechanicsOfferProperties.emptyOf(MechanicsType.UNKNOWN))
                                        .build()
                        ))
                )));

        final BigDecimal fixedBasePrice = BigDecimal.valueOf(1000);
        final BigDecimal fixedPrice = BigDecimal.valueOf(500);
        assortmentService.markDirectDiscountToParticipate(directDiscount.toPromoKey(), List.of(
                DirectDiscountOfferParticipation.builder()
                        .offerPromoParticipation(
                                OfferPromoParticipation.builder()
                                        .promoId(directDiscount.getId())
                                        .participate(true)
                                        .offerId(OfferId.of(IdentityUtils.hashId(SSKU_4), SHOP_ID))
                                        .build())
                        .minimalPercentSize(BigDecimal.TEN)
                        .fixedBasePrice(fixedBasePrice)
                        .fixedPrice(fixedPrice)
                        .build()
        ));

        assortmentService.publishPromoAssortment(directDiscount.toPromoKey());

        assertThat(mockedLogbrokerQueue, hasSize(1));
        final OfferLogbrokerEvent event = mockedLogbrokerQueue.peek();
        assert event != null;
        final List<DataCampOffer.Offer> offers = new ArrayList<>(
                event.getPayload()
                        .getUnitedOffers(0)
                        .getOffer(0)
                        .getServiceMap()
                        .values());
        assertThat(offers, hasSize(1));
        final DataCampOfferPromos.Promo.DirectDiscount directDiscount =
                offers.get(0).getPromos().getAnaplanPromos().getActivePromos().getPromos(0).getDirectDiscount();
        Assert.assertEquals(directDiscount.getPrice(), offerDataConverter.convertToPriceExpression(fixedPrice));
        Assert.assertEquals(directDiscount.getBasePrice(), offerDataConverter.convertToPriceExpression(fixedBasePrice));
    }

    @Test
    void shouldPublishWithoutActiveMechanicProperties() {
        datacampOfferDao.replace(List.of(
                datacampOffer(
                        name(SSKU_4),
                        shopSku(SSKU_4),
                        warehouse(WAREHOUSE_ID),
                        shop(SHOP_ID),
                        price(1000),
                        basePrice(1500),
                        categoryId(123L),
                        potentialPromo(cheapestAsGift.getId(), BigDecimal.valueOf(150))
                )));

        assortmentService.markToParticipate(cheapestAsGift, List.of(
                OfferPromoParticipation.builder()
                        .promoId(cheapestAsGift.getId())
                        .participate(true)
                        .offerId(OfferId.of(IdentityUtils.hashId(SSKU_4), SHOP_ID))
                        .build())
        );

        assortmentService.publishPromoAssortment(cheapestAsGift.toPromoKey());

        assertThat(mockedLogbrokerQueue, hasSize(1));
        final OfferLogbrokerEvent event = mockedLogbrokerQueue.peek();
        assert event != null;
        final List<DataCampOffer.Offer> offers = new ArrayList<>(
                event.getPayload()
                        .getUnitedOffers(0)
                        .getOffer(0)
                        .getServiceMap()
                        .values());
        assertThat(offers, hasSize(1));
        final DataCampOfferPromos.Promos activePromos = offers.get(0).getPromos().getAnaplanPromos().getActivePromos();
        assertThat(activePromos.getPromosList().get(0).getId(), is(cheapestAsGift.getPromoId()));
        assertThat(activePromos.getPromosList().get(0).hasCheapestAsGift(), is(true));
    }

    @Test
    void shouldNotPublishWithoutLocalMechanicProperties() {
        datacampOfferDao.replace(List.of(
                datacampOffer(
                        name(SSKU_4),
                        shopSku(SSKU_4),
                        warehouse(WAREHOUSE_ID),
                        shop(SHOP_ID),
                        price(1000),
                        basePrice(1500),
                        categoryId(123L),
                        potentialPromo(cheapestAsGift.getId(), BigDecimal.valueOf(150)),
                        activePromos(Set.of(
                                DatacampOfferPromo.builderOf(
                                        OfferPromoBase.builder()
                                                .id(cheapestAsGift.getId())
                                                .updatedAt(clock.dateTime())
                                                .build()
                                )
                                        .mechanicsProperties(MechanicsOfferProperties.emptyOf(MechanicsType.UNKNOWN))
                                        .build()
                        ))
                )));

        PublishingInfo publishingInfo = assortmentService.publishPromoAssortment(cheapestAsGift.toPromoKey());

        assertThat(publishingInfo, notNullValue());
        assertThat(publishingInfo.getOffers(), empty());
        assertThat(publishingInfo.getPublishedOffers(), empty());
        assertThat(publishingInfo.getPublishingStatus(), is(PublishingStatus.CLEARED));
    }

    @Test
    void shouldCheckOffersWithoutDoublingThemInRequest() {
        LocalOffer localOffer =
                localPromoOfferDao.getOffers(directDiscount,
                        List.of(OfferId.of(IdentityUtils.hashId(SSKU_1), SHOP_ID))).get(0);

        assortmentService.markDirectDiscountToParticipate(directDiscount.toPromoKey(),
                List.of(DirectDiscountOfferParticipation.builder()
                        .offerPromoParticipation(OfferPromoParticipation.builder()
                                .promoId(directDiscount.getId())
                                .participate(true)
                                .offerId(OfferId.of(IdentityUtils.hashId(SSKU_1), SHOP_ID))
                                .build())
                        .fixedPrice(BigDecimal.valueOf(5000))
                        .fixedBasePrice(BigDecimal.valueOf(9999))
                        .build()));

        PublishingInfo publishingInfo = assortmentService.publishPromoAssortment(directDiscount.toPromoKey());

        assertThat(publishingInfo.getPublishingStatus(), is(PublishingStatus.PUBLISHED));
        assertThat(publishingInfo.getOffers(), hasSize(1));
        assertThat(publishingInfo.getOffers(), hasItem(localOffer.getOfferId()));

        publishingProcessorTask.process();
        publishingProcessorTask.process();
        publishingProcessorTask.process();

        publishingInfo = assortmentService.retrievePublishingState(publishingInfo.getProcessId());

        assertThat(publishingInfo.getPublishingStatus(), is(PublishingStatus.CHECKING));
        assertThat(publishingInfo.getOffers(), hasSize(1));
    }

    @Nonnull
    private DirectDiscountOfferParticipation directDiscountParticipationOf(@Nonnull Promo promo,
                                                                           @Nonnull LocalOffer offer) {
        final DirectDiscountProperties directDiscountProperties =
                promo.getMechanicsPropertiesAs(DirectDiscountProperties.class).orElseThrow();
        final Map<Long, BigDecimal> minimalPercentByCategory = promo.getCategoriesWithDiscounts().stream()
                .collect(Collectors.toUnmodifiableMap(
                        CategoryIdWithDiscount::getCategoryId,
                        CategoryIdWithDiscount::getDiscount, (e1, e2) -> e1));
        if (offer.isParticipateIn(promo)) {
            final DirectDiscountOfferPropertiesCore directDiscountOfferPropertiesCore =
                    offer.getPromos().get(promo.toPromoKey())
                            .getMechanicsPropertiesAs(DirectDiscountOfferPropertiesCore.class);

            return DirectDiscountOfferParticipation.builder()
                    .offerPromoParticipation(participationOf(promo, offer))
                    .fixedBasePrice(directDiscountOfferPropertiesCore == null ? null :
                            directDiscountOfferPropertiesCore.getFixedBasePrice())
                    .fixedPrice(directDiscountOfferPropertiesCore == null ? null :
                            directDiscountOfferPropertiesCore.getFixedPrice())
                    .minimalPercentSize(minimalPercentByCategory.getOrDefault(offer.getCategoryId(),
                            directDiscountProperties.getMinimalDiscountPercentSize()))
                    .build();
        } else {
            return DirectDiscountOfferParticipation.builder()
                    .offerPromoParticipation(participationOf(promo, offer))
                    .minimalPercentSize(minimalPercentByCategory.getOrDefault(offer.getCategoryId(),
                            directDiscountProperties.getMinimalDiscountPercentSize()))
                    .build();
        }
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
