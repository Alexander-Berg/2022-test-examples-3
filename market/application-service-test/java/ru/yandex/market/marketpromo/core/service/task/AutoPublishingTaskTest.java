package ru.yandex.market.marketpromo.core.service.task;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.marketpromo.core.dao.DatacampOfferDao;
import ru.yandex.market.marketpromo.core.dao.PromoDao;
import ru.yandex.market.marketpromo.core.data.source.logbroker.OfferLogbrokerEvent;
import ru.yandex.market.marketpromo.core.service.impl.CachedAssortmentService;
import ru.yandex.market.marketpromo.core.test.ServiceTestBase;
import ru.yandex.market.marketpromo.core.test.generator.Offers;
import ru.yandex.market.marketpromo.filter.AssortmentRequest;
import ru.yandex.market.marketpromo.model.LocalOffer;
import ru.yandex.market.marketpromo.model.PagerList;
import ru.yandex.market.marketpromo.model.Promo;
import ru.yandex.market.marketpromo.model.SupplierType;
import ru.yandex.market.marketpromo.model.processing.ProcessingRequestType;
import ru.yandex.market.marketpromo.model.processing.PublishingInfo;
import ru.yandex.market.marketpromo.model.processing.PublishingStatus;
import ru.yandex.market.marketpromo.processing.ProcessId;
import ru.yandex.market.marketpromo.service.AssortmentService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.basePrice;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.categoryId;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.datacampOffer;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.name;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.potentialPromo;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.price;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.shop;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.shopSku;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.warehouse;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.ANOTHER_CAG_PROMO_KEY;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.CAG_PROMO_KEY;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.assortmentAutopublication;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.cheapestAsGift;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.ends;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.id;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.promo;

class AutoPublishingTaskTest extends ServiceTestBase {

    private static final int WAREHOUSE_ID = 123;
    private static final long SHOP_ID = 12L;
    private static final String SSKU_1 = "ssku-1";
    private static final String SSKU_2 = "ssku-2";
    private static final String SSKU_3 = "ssku-3";

    @Autowired
    private AssortmentService assortmentService;
    @Autowired
    private PromoDao promoDao;
    @Autowired
    private DatacampOfferDao datacampOfferDao;
    @Autowired
    private Queue<OfferLogbrokerEvent> mockedLogbrokerQueue;
    @Autowired
    private CachedAssortmentService cachedAssortmentService;
    @Autowired
    private AutoPublishingTask autoPublishingTask;

    private Promo cheapestAsGift;
    private Promo anotherCheapestAsGift;

    @BeforeEach
    void setUp() {

        cheapestAsGift = promoDao.replace(promo(
                id(CAG_PROMO_KEY.getId()),
                cheapestAsGift(),
                assortmentAutopublication(),
                ends(LocalDateTime.of(2022, 10, 10, 10, 10))
        ));
        anotherCheapestAsGift = promoDao.replace(promo(
                id(ANOTHER_CAG_PROMO_KEY.getId()),
                cheapestAsGift(),
                ends(LocalDateTime.of(2022, 10, 10, 10, 10))
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
                        Offers.supplierType(SupplierType._1P),
                        potentialPromo(cheapestAsGift.getId(), BigDecimal.valueOf(150))
                ),
                datacampOffer(
                        name(SSKU_2),
                        shopSku(SSKU_2),
                        warehouse(WAREHOUSE_ID),
                        shop(SHOP_ID),
                        price(1000),
                        basePrice(1500),
                        categoryId(123L),
                        Offers.supplierType(SupplierType._1P),
                        potentialPromo(anotherCheapestAsGift.getId())
                ),
                datacampOffer(
                        name(SSKU_3),
                        shopSku(SSKU_3),
                        warehouse(WAREHOUSE_ID),
                        shop(SHOP_ID),
                        price(1000),
                        basePrice(1500),
                        categoryId(123L),
                        Offers.supplierType(SupplierType._1P),
                        potentialPromo(cheapestAsGift.getId()),
                        potentialPromo(anotherCheapestAsGift.getId())
                )
        ));

        assertThat(mockedLogbrokerQueue, empty());

    }

    @AfterEach
    void clean() {
        mockedLogbrokerQueue.clear();
    }

    @Test
    void shouldBeMarkedAndPublished() throws ExecutionException, InterruptedException {
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> autoPublishingTask.process());
        future.get();

        final ProcessId processId = ProcessId.of(ProcessingRequestType.PUBLISH_ASSORTMENT, cheapestAsGift.getId());
        PublishingInfo publishingInfo = assortmentService.retrievePublishingState(processId);

        assertThat(publishingInfo.getPublishingStatus(), is(PublishingStatus.PUBLISHED));
        assertThat(publishingInfo.getOffers(), hasSize(2));
    }

    @Test
    void shouldNotBeMarkedAndPublished() {
        autoPublishingTask.process();
        final ProcessId wrongProcessId = ProcessId.of(ProcessingRequestType.PUBLISH_ASSORTMENT,
                anotherCheapestAsGift.getId());
        PublishingInfo wrongPublishingInfo = assortmentService.retrievePublishingState(wrongProcessId);
        assertThat(wrongPublishingInfo, nullValue());
    }

    @Test
    void cachedShouldBeRefreshedAfterPublication() throws ExecutionException, InterruptedException {
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> autoPublishingTask.process());
        future.get();

        PagerList<LocalOffer> assortment = assortmentService.getAssortment(AssortmentRequest
                .builder(cheapestAsGift.toPromoKey()).build());
        assertThat(assortment.getList(), hasSize(2));
    }

}
