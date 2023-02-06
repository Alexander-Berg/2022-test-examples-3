package ru.yandex.market.marketpromo.core.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.Nonnull;

import com.google.common.collect.Sets;
import org.apache.commons.collections4.ListUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.marketpromo.core.dao.DatacampOfferDao;
import ru.yandex.market.marketpromo.core.dao.PromoDao;
import ru.yandex.market.marketpromo.core.service.impl.CachedAssortmentService;
import ru.yandex.market.marketpromo.core.test.ServiceTestBase;
import ru.yandex.market.marketpromo.core.test.generator.DatacampOfferPromoMechanics;
import ru.yandex.market.marketpromo.core.test.generator.Offers;
import ru.yandex.market.marketpromo.core.test.generator.Promos;
import ru.yandex.market.marketpromo.filter.AssortmentRequest;
import ru.yandex.market.marketpromo.model.DatacampOffer;
import ru.yandex.market.marketpromo.model.DatacampOfferPromo;
import ru.yandex.market.marketpromo.model.LocalOffer;
import ru.yandex.market.marketpromo.model.OfferId;
import ru.yandex.market.marketpromo.model.OfferPromoBase;
import ru.yandex.market.marketpromo.model.Promo;
import ru.yandex.market.marketpromo.model.SupplierType;
import ru.yandex.market.marketpromo.service.AssortmentService;
import ru.yandex.market.marketpromo.utils.IdentityUtils;
import ru.yandex.market.ydb.integration.model.Field;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.market.marketpromo.core.test.generator.OfferPromoBases.basePrice;
import static ru.yandex.market.marketpromo.core.test.generator.OfferPromoBases.id;
import static ru.yandex.market.marketpromo.core.test.generator.OfferPromoBases.promoBase;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.activePromos;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.datacampOffer;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.name;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.potentialPromo;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.potentialPromos;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.price;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.shop;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.shopSku;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.supplierType;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.directDiscount;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.promo;

public class AssortmentServiceTest extends ServiceTestBase {

    private static final long SHOP_ID = 12L;
    private static final String SSKU_1 = "ssku 1";

    @Autowired
    private AssortmentService assortmentService;
    @Autowired
    private DatacampOfferDao datacampOfferDao;
    @Autowired
    private PromoDao promoDao;
    @Autowired
    private CachedAssortmentService cachedAssortmentService;

    @Test
    void shouldAddNewDatacampOffers() {
        List<DatacampOffer> offers = generateOfferListForCreation(1000);

        assortmentService.createAssortment(offers);

        List<DatacampOffer> stored = ListUtils.partition(offers, Field.IN_CLAUSE_RESTRICTION).stream()
                .flatMap(ofrs -> datacampOfferDao.selectByOfferIds(ofrs.stream()
                        .map(DatacampOffer::getOfferId)
                        .collect(Collectors.toUnmodifiableSet())).stream())
                .collect(Collectors.toUnmodifiableList());

        assertThat(stored, hasSize(1000));
    }

    @Test
    void shouldUpdateDatacampOffers() {
        List<DatacampOffer> offers = generateOfferListForCreation(2000);

        ExecutorService executorService = Executors.newFixedThreadPool(3);
        executorService.submit(() -> assortmentService.createOrUpdateOffers(offers.subList(0, 1000)));
        executorService.submit(() -> assortmentService.createOrUpdateOffers(offers.subList(1000, offers.size())));
        executorService.submit(() -> assortmentService.createOrUpdateOffers(generateOfferListForUpdate(5000)));


        try {
            Thread.sleep(10_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        List<DatacampOffer> stored = ListUtils.partition(offers, Field.IN_CLAUSE_RESTRICTION).stream()
                .flatMap(ofrs -> datacampOfferDao.selectByOfferIds(ofrs.stream()
                        .map(DatacampOffer::getOfferId)
                        .collect(Collectors.toUnmodifiableSet())).stream())
                .collect(Collectors.toUnmodifiableList());

        assertThat(stored, hasSize(2000));
    }

    @Test
    void shouldSkipNotExistedDatacampOffersOnUpdate() {
        List<DatacampOffer> offers = generateOfferListForCreation(10);

        assortmentService.createOrUpdateOffers(offers);
        assortmentService.createOrUpdateOffers(generateOfferListForUpdate(100));
        try {
            Thread.sleep(1_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        List<DatacampOffer> stored = ListUtils.partition(offers, Field.IN_CLAUSE_RESTRICTION).stream()
                .flatMap(ofrs -> datacampOfferDao.selectByOfferIds(ofrs.stream()
                        .map(DatacampOffer::getOfferId)
                        .collect(Collectors.toUnmodifiableSet())).stream())
                .collect(Collectors.toUnmodifiableList());

        assertThat(stored, hasSize(10));
    }

    @Test
    void shouldUpdateOnlyPromoRelationsIfNoBasicData() {
        DatacampOffer offer = Offers.datacampOffer(
                name(SSKU_1),
                shopSku(SSKU_1),
                shop(SHOP_ID),
                price(100),
                potentialPromos(generatePromos(1))
        );
        assortmentService.createAssortment(List.of(offer));

        assortmentService.createOrUpdateOffers(List.of(Offers.datacampOffer(
                shopSku(SSKU_1),
                shop(SHOP_ID),
                potentialPromo("some promo", BigDecimal.valueOf(200))
        )));
        try {
            Thread.sleep(1_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        DatacampOffer updated = datacampOfferDao.selectOne(OfferId.of(IdentityUtils.hashId(SSKU_1), SHOP_ID));

        assertThat(updated, notNullValue());
        assertThat(updated.getPotentialPromos(), hasEntry(
                is("some promo"),
                hasProperty("basePrice", comparesEqualTo(BigDecimal.valueOf(200)))
        ));
    }

    @Test
    @Disabled
    void shouldGetOffersForExport() {
        Promo promo = promoDao.replace(promoDao.replace(promo(
                Promos.id(IdentityUtils.hashId("promo 1")),
                directDiscount()
        )));
        int offersAmount = 300000;

        List<DatacampOffer> offers = generateOfferListForCreation(offersAmount);

        assortmentService.createAssortment(offers);
        cachedAssortmentService.refreshAssortmentCache();
        AssortmentRequest assortmentRequest = AssortmentRequest.builder(promo.toPromoKey()).build();
        LocalDateTime start = LocalDateTime.now();

        List<LocalOffer> receivedOffers = assortmentService.getAssortmentForExport(assortmentRequest);
        System.out.println("Start: " + start);
        System.out.println("Finish: " + LocalDateTime.now());
        assertThat(receivedOffers, hasSize(offersAmount));
    }

    @Nonnull
    private List<DatacampOffer> generateOfferListForCreation(int size) {
        return IntStream.range(1, size + 1)
                .mapToObj(someNumber -> datacampOffer(
                        name("offer " + someNumber),
                        shopSku("offer " + someNumber),
                        price(someNumber * 100),
                        potentialPromos(generatePromos(10)),
                        supplierType(SupplierType._1P),
                        activePromos(
                                Sets.union(
                                        generateActualDDPromos(1, 5),
                                        generateActualCAGPromos(6, 5)
                                )
                        )
                )).collect(Collectors.toUnmodifiableList());
    }

    @Nonnull
    private List<DatacampOffer> generateOfferListForUpdate(int size) {
        return IntStream.range(1, size + 1)
                .mapToObj(someNumber -> DatacampOffer.builder()
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .id(IdentityUtils.hashId("offer " + someNumber))
                        .shopSku("offer " + someNumber)
                        .potentialPromos(generatePromos(10))
                        .activePromos(Sets.union(
                                generateActualDDPromos(1, 5),
                                generateActualCAGPromos(6, 5)
                        ))
                        .build()).collect(Collectors.toUnmodifiableList());
    }

    @Nonnull
    private Set<OfferPromoBase> generatePromos(int size) {
        return IntStream.range(1, size + 1)
                .mapToObj(someNumber -> promoBase(
                        id(IdentityUtils.hashId("promo " + someNumber)),
                        basePrice(BigDecimal.TEN.multiply(BigDecimal.valueOf(someNumber)))
                )).collect(Collectors.toUnmodifiableSet());
    }

    @Nonnull
    private Set<DatacampOfferPromo> generateActualDDPromos(int start, int size) {
        return IntStream.range(start, start + size + 1)
                .mapToObj(someNumber -> DatacampOfferPromoMechanics.directDiscount("promo " + someNumber,
                        DatacampOfferPromoMechanics.price(BigDecimal.TEN))).collect(Collectors.toUnmodifiableSet());
    }

    @Nonnull
    private Set<DatacampOfferPromo> generateActualCAGPromos(int start, int size) {
        return IntStream.range(start, start + size + 1)
                .mapToObj(someNumber -> DatacampOfferPromoMechanics.cheapestAsGift("promo " + someNumber))
                .collect(Collectors.toUnmodifiableSet());
    }
}
