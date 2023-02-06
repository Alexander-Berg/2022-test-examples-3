package ru.yandex.market.marketpromo.usecase;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.marketpromo.core.application.context.ProcessingQueueType;
import ru.yandex.market.marketpromo.core.application.properties.LogbrokerAssortmentPropagationTopicProperties;
import ru.yandex.market.marketpromo.core.dao.DatacampOfferDao;
import ru.yandex.market.marketpromo.core.dao.OffersPublishingBatchDao;
import ru.yandex.market.marketpromo.core.dao.PromoDao;
import ru.yandex.market.marketpromo.core.data.processing.PublishingBatch;
import ru.yandex.market.marketpromo.core.data.processing.result.PublishingCheckResult;
import ru.yandex.market.marketpromo.core.data.processing.result.PublishingRequest;
import ru.yandex.market.marketpromo.core.service.task.AssortmentPublishingProcessorTask;
import ru.yandex.market.marketpromo.core.test.generator.DatacampOfferPromoMechanics;
import ru.yandex.market.marketpromo.core.test.generator.Offers;
import ru.yandex.market.marketpromo.core.test.utils.OfferStorageTestHelper;
import ru.yandex.market.marketpromo.model.DatacampOffer;
import ru.yandex.market.marketpromo.model.DirectDiscountOfferParticipation;
import ru.yandex.market.marketpromo.model.OfferId;
import ru.yandex.market.marketpromo.model.OfferPromoParticipation;
import ru.yandex.market.marketpromo.model.Promo;
import ru.yandex.market.marketpromo.model.processing.ProcessingRequestType;
import ru.yandex.market.marketpromo.model.processing.PublishingInfo;
import ru.yandex.market.marketpromo.model.processing.PublishingStatus;
import ru.yandex.market.marketpromo.processing.ProcessingController;
import ru.yandex.market.marketpromo.processing.ProcessingStage;
import ru.yandex.market.marketpromo.service.AssortmentService;
import ru.yandex.market.marketpromo.test.MockedWebTestBase;
import ru.yandex.market.marketpromo.utils.IdentityUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.market.marketpromo.core.test.generator.DatacampOfferPromoMechanics.directDiscount;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.activePromos;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.business;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.categoryId;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.feed;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.generateDatacampOfferList;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.marketSku;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.name;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.potentialPromo;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.price;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.shop;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.shopSku;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.warehouse;
import static ru.yandex.market.marketpromo.core.test.generator.PromoMechanics.minimalDiscountPercentSize;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.directDiscount;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.id;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.promo;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.promoName;


public class AssortmentPublishingTest extends MockedWebTestBase {

    private static final String SSKU_1 = "hid.10588219";
    private static final String PROMO_ID = "#258445267";
    private static final long SHOP_ID = 10671634L;

    @Autowired
    private PromoDao promoDao;
    @Autowired
    private DatacampOfferDao datacampOfferDao;
    @Autowired
    private OffersPublishingBatchDao publishingBatchDao;
    @Autowired
    private AssortmentService assortmentService;
    @Autowired
    private LogbrokerAssortmentPropagationTopicProperties propagationTopicProperties;
    @Autowired
    private AssortmentPublishingProcessorTask publishingProcessorTask;
    @Autowired
    @ProcessingQueueType(ProcessingRequestType.PUBLISH_ASSORTMENT)
    private ProcessingController<PublishingStatus, ProcessingRequestType> publishingController;
    @Autowired
    private OfferStorageTestHelper offerStorageTestHelper;

    private Promo promo;

    @BeforeEach
    void configure() {
        promo = promoDao.replace(promo(
                id(IdentityUtils.hashId(PROMO_ID)),
                promoName("MBI_ 198118 24.12.2020"),
                directDiscount(
                        minimalDiscountPercentSize(10)
                )
        ));

        datacampOfferDao.replace(List.of(
                Offers.datacampOffer(
                        business(10728534),
                        shop(SHOP_ID),
                        warehouse(48339),
                        name("Накопительный электрический водонагреватель Stiebel Eltron PSH 50 Universal EL"),
                        categoryId(90575),
                        feed(200720578),
                        shopSku(SSKU_1),
                        marketSku(10588219),
                        price(9999),
                        potentialPromo(promo.getId())
                )
        ));
    }

    @Test
    void shouldPublishAssortment() {
        assortmentService.markDirectDiscountToParticipate(promo.toPromoKey(),
                List.of(DirectDiscountOfferParticipation.builder()
                        .offerPromoParticipation(OfferPromoParticipation.builder()
                                .promoId(promo.getId())
                                .participate(true)
                                .offerId(OfferId.of(IdentityUtils.hashId(SSKU_1), SHOP_ID))
                                .build())
                        .fixedPrice(BigDecimal.valueOf(5000))
                        .fixedBasePrice(BigDecimal.valueOf(9999))
                        .build()));

        PublishingInfo publishingInfo =
                assortmentService.publishPromoAssortment(promo.toPromoKey());
        ProcessingStage<PublishingStatus> processingStage =
                publishingController.currentStageOf(publishingInfo.getProcessId().getId()).orElseThrow();
        PublishingRequest publishingRequest = processingStage.getDataAs(PublishingRequest.class);
        assertThat(publishingRequest, notNullValue());
        assertThat(processingStage.getState(), is(PublishingStatus.PUBLISHED));
        assertThat(publishingRequest.getBatches().keySet(), hasSize(1));
        List<PublishingBatch> publishingBatches =
                publishingBatchDao.readBatches(publishingInfo.getProcessId(), publishingRequest.getBatches().keySet());
        assertThat(publishingBatches, hasSize(1));
    }

    @Test
    void shouldPublishPartOfAssortment() {
        int count = propagationTopicProperties.getOffersBatchSize();

        List<DatacampOffer> datacampOffers = generateDatacampOfferList(count + 200, potentialPromo(promo.getId()));

        datacampOfferDao.replace(datacampOffers);
        assortmentService.markDirectDiscountToParticipate(promo.toPromoKey(),
                datacampOffers.stream()
                        .map(offer -> DirectDiscountOfferParticipation.builder()
                                .offerPromoParticipation(OfferPromoParticipation.builder()
                                        .promoId(promo.getId())
                                        .participate(true)
                                        .offerId(offer.getOfferId())
                                        .build())
                                .fixedPrice(BigDecimal.valueOf(5000))
                                .fixedBasePrice(BigDecimal.valueOf(9999))
                                .build())
                        .collect(Collectors.toUnmodifiableList()));

        PublishingInfo publishingInfo =
                assortmentService.publishPromoAssortment(promo.toPromoKey());
        ProcessingStage<PublishingStatus> processingStage =
                publishingController.currentStageOf(publishingInfo.getProcessId().getId()).orElseThrow();
        PublishingRequest publishingRequest = processingStage.getDataAs(PublishingRequest.class);
        assertThat(publishingRequest, notNullValue());
        assertThat(processingStage.getState(), is(PublishingStatus.PUBLISHING));
        assertThat(publishingRequest.getBatches().keySet(), hasSize(2));
        List<PublishingBatch> publishingBatches =
                publishingBatchDao.readBatches(publishingInfo.getProcessId(), publishingRequest.getBatches().keySet());
        assertThat(publishingBatches, hasSize(2));
    }

    @Test
    void shouldPublishAssortmentAsync() {
        int count = propagationTopicProperties.getOffersBatchSize();

        List<DatacampOffer> datacampOffers = generateDatacampOfferList(count + 200, potentialPromo(promo.getId()));

        datacampOfferDao.replace(datacampOffers);
        assortmentService.markDirectDiscountToParticipate(promo.toPromoKey(),
                datacampOffers.stream()
                        .map(offer -> DirectDiscountOfferParticipation.builder()
                                .offerPromoParticipation(OfferPromoParticipation.builder()
                                        .promoId(promo.getId())
                                        .participate(true)
                                        .offerId(offer.getOfferId())
                                        .build())
                                .fixedPrice(BigDecimal.valueOf(5000))
                                .fixedBasePrice(BigDecimal.valueOf(9999))
                                .build())
                        .collect(Collectors.toUnmodifiableList()));

        PublishingInfo publishingInfo =
                assortmentService.publishPromoAssortment(promo.toPromoKey());
        assertThat(publishingInfo.getPublishingStatus(), is(PublishingStatus.PUBLISHING));

        publishingProcessorTask.process();

        ProcessingStage<PublishingStatus> processingStage =
                publishingController.currentStageOf(publishingInfo.getProcessId().getId()).orElseThrow();
        PublishingRequest publishingRequest = processingStage.getDataAs(PublishingRequest.class);
        assertThat(publishingRequest, notNullValue());
        assertThat(processingStage.getState(), is(PublishingStatus.PUBLISHED));
        assertThat(publishingRequest.getBatches().keySet(), hasSize(2));
        List<PublishingBatch> publishingBatches =
                publishingBatchDao.readBatches(publishingInfo.getProcessId(), publishingRequest.getBatches().keySet());
        assertThat(publishingBatches, hasSize(2));
    }

    @Test
    void shouldCheckAssortmentAsync() {
        int count = propagationTopicProperties.getOffersBatchSize();

        List<DatacampOffer> datacampOffers = generateDatacampOfferList(count + 200, potentialPromo(promo.getId()));

        datacampOfferDao.replace(datacampOffers);
        assortmentService.markDirectDiscountToParticipate(promo.toPromoKey(),
                datacampOffers.stream()
                        .map(offer -> DirectDiscountOfferParticipation.builder()
                                .offerPromoParticipation(OfferPromoParticipation.builder()
                                        .promoId(promo.getId())
                                        .participate(true)
                                        .offerId(offer.getOfferId())
                                        .build())
                                .fixedPrice(BigDecimal.valueOf(5000))
                                .fixedBasePrice(BigDecimal.valueOf(9999))
                                .build())
                        .collect(Collectors.toUnmodifiableList()));

        assertThat(promoDao.get(promo.toPromoKey()).getHasNotPublishedChanges(), is(true));

        PublishingInfo publishingInfo =
                assortmentService.publishPromoAssortment(promo.toPromoKey());
        assertThat(publishingInfo.getPublishingStatus(), is(PublishingStatus.PUBLISHING));

        publishingProcessorTask.process();

        ProcessingStage<PublishingStatus> processingStage =
                publishingController.currentStageOf(publishingInfo.getProcessId().getId()).orElseThrow();
        assertThat(processingStage.getState(), is(PublishingStatus.PUBLISHED));

        publishingProcessorTask.process();

        processingStage =
                publishingController.currentStageOf(publishingInfo.getProcessId().getId()).orElseThrow();

        assertThat(processingStage.getState(), is(PublishingStatus.CHECKING));

        PublishingCheckResult checkResult = processingStage.getDataAs(PublishingCheckResult.class);
        assertThat(checkResult, notNullValue());
        assertThat(checkResult.getOffersToCheck(), notNullValue());
        assertThat(checkResult.getOffersToCheck(), aMapWithSize(2));

        for (var key : checkResult.getOffersToCheck().keySet()) {
            offerStorageTestHelper.mockStrollerSearchOffersResponse(
                    datacampOfferDao.selectByOfferIds(Collections.singletonList(key))
                            .stream().map(o -> o.toBuilder())
                            .map(ob -> ob.updatedAt(clock.dateTime())
                                    .activePromo(directDiscount(promo.getId(),
                                            DatacampOfferPromoMechanics.basePrice(BigDecimal.valueOf(9999)),
                                            DatacampOfferPromoMechanics.price(BigDecimal.valueOf(5000))
                                    )))
                            .map(DatacampOffer.OfferBuilder::build)
                            .collect(Collectors.toUnmodifiableList())
            );
        }
        publishingProcessorTask.process();

        processingStage =
                publishingController.currentStageOf(publishingInfo.getProcessId().getId()).orElseThrow();
        assertThat(processingStage.getState(), is(PublishingStatus.CHECKED));

        publishingProcessorTask.process();

        processingStage =
                publishingController.currentStageOf(publishingInfo.getProcessId().getId()).orElseThrow();
        assertThat(processingStage.getState(), is(PublishingStatus.CLEARED));

        checkResult = processingStage.getDataAs(PublishingCheckResult.class);

        assertThat(checkResult, notNullValue());
        assertThat(publishingBatchDao.readBatches(publishingInfo.getProcessId(),
                checkResult.getOffersToCheck().keySet()), empty());

        assertThat(promoDao.get(promo.toPromoKey()).getHasNotPublishedChanges(), is(false));
        Set<OfferId> checkedOfferIds =
                checkResult.getChecked();
        List<DatacampOffer> publishedOffers =
                datacampOfferDao.selectByOfferIds(checkedOfferIds);

        assertThat(publishedOffers, hasSize(checkedOfferIds.size()));
        assertThat(publishedOffers, everyItem(
                hasProperty("activePromos", hasEntry(
                        is(promo.getId()),
                        hasProperty("mechanicsProperties", allOf(
                                hasProperty("fixedBasePrice", comparesEqualTo(BigDecimal.valueOf(9999))),
                                hasProperty("fixedPrice", comparesEqualTo(BigDecimal.valueOf(5000)))
                        ))))
        ));
    }

    @Test
    void shouldCreateSecondPublishingProcessAfterFirstCompletion() {
        List<DatacampOffer> datacampOffers = generateDatacampOfferList(10, potentialPromo(promo.getId()));

        datacampOfferDao.replace(datacampOffers);

        assortmentService.markDirectDiscountToParticipate(promo.toPromoKey(),
                datacampOffers.stream()
                        .map(offer -> DirectDiscountOfferParticipation.builder()
                                .offerPromoParticipation(OfferPromoParticipation.builder()
                                        .promoId(promo.getId())
                                        .participate(true)
                                        .offerId(offer.getOfferId())
                                        .build())
                                .fixedPrice(BigDecimal.valueOf(5000))
                                .fixedBasePrice(BigDecimal.valueOf(9999))
                                .build())
                        .collect(Collectors.toUnmodifiableList()));

        PublishingInfo publishingInfo =
                assortmentService.publishPromoAssortment(promo.toPromoKey());

        assertThat(publishingInfo.getPublishingStatus(), is(PublishingStatus.PUBLISHED));

        publishingProcessorTask.process();

        ProcessingStage<PublishingStatus> processingStage =
                publishingController.currentStageOf(publishingInfo.getProcessId().getId()).orElseThrow();
        PublishingCheckResult checkResult = processingStage.getDataAs(PublishingCheckResult.class);

        assertThat(checkResult, notNullValue());
        assertThat(checkResult.getOffersToCheck(), notNullValue());
        assertThat(checkResult.getOffersToCheck(), aMapWithSize(1));

        offerStorageTestHelper.mockStrollerSearchOffersResponse(
                datacampOfferDao.selectByOfferIds(checkResult.getOffersToCheck().keySet()).stream()
                        .map(o -> o.toBuilder())
                        .map(ob -> ob.updatedAt(clock.dateTime())
                                .activePromo(directDiscount(promo.getId(),
                                        DatacampOfferPromoMechanics.basePrice(BigDecimal.valueOf(9999)),
                                        DatacampOfferPromoMechanics.price(BigDecimal.valueOf(5000))
                                )))
                        .map(DatacampOffer.OfferBuilder::build)
                        .collect(Collectors.toUnmodifiableList())
        );

        publishingProcessorTask.process();
        publishingProcessorTask.process();

        processingStage =
                publishingController.currentStageOf(publishingInfo.getProcessId().getId()).orElseThrow();

        assertThat(processingStage.getState(), is(PublishingStatus.CLEARED));

        datacampOffers = generateDatacampOfferList(10, potentialPromo(promo.getId()));

        datacampOfferDao.replace(datacampOffers);

        assortmentService.markDirectDiscountToParticipate(promo.toPromoKey(),
                datacampOffers.stream()
                        .map(offer -> DirectDiscountOfferParticipation.builder()
                                .offerPromoParticipation(OfferPromoParticipation.builder()
                                        .promoId(promo.getId())
                                        .participate(true)
                                        .offerId(offer.getOfferId())
                                        .build())
                                .fixedPrice(BigDecimal.valueOf(5000))
                                .fixedBasePrice(BigDecimal.valueOf(9999))
                                .build())
                        .collect(Collectors.toUnmodifiableList()));

        assertThat(promoDao.get(promo.toPromoKey()).getHasNotPublishedChanges(), is(true));

        publishingInfo = assortmentService.publishPromoAssortment(promo.toPromoKey());

        assertThat(publishingInfo.getPublishingStatus(), is(PublishingStatus.PUBLISHED));
    }

    @Test
    void shouldDisableActivePromoIfSet() {
        DatacampOffer datacampOffer = Offers.datacampOffer(
                business(10728534),
                shop(SHOP_ID),
                warehouse(48339),
                name("Накопительный электрический водонагреватель Stiebel Eltron PSH 50 Universal EL"),
                categoryId(90575),
                feed(200720578),
                shopSku(SSKU_1),
                marketSku(10588219),
                price(9999),
                potentialPromo(promo.getId(), BigDecimal.valueOf(6000)),
                activePromos(directDiscount(promo.getId(),
                        DatacampOfferPromoMechanics.basePrice(BigDecimal.valueOf(9999)),
                        DatacampOfferPromoMechanics.price(BigDecimal.valueOf(5000))
                ))
        );

        datacampOfferDao.replace(List.of(datacampOffer));
        assortmentService.markDirectDiscountToParticipate(promo.toPromoKey(), List.of(
                DirectDiscountOfferParticipation.builder()
                        .offerPromoParticipation(OfferPromoParticipation.builder()
                                .participate(false)
                                .promoId(promo.getId())
                                .offerId(datacampOffer.getOfferId())
                                .build())
                        .build()
        ));

        assertThat(promoDao.get(promo.toPromoKey()).getHasNotPublishedChanges(), is(true));

        PublishingInfo publishingInfo =
                assortmentService.publishPromoAssortment(promo.toPromoKey());
        assertThat(publishingInfo.getPublishingStatus(), is(PublishingStatus.PUBLISHED));
        assertThat(publishingInfo.getOffers(), hasItem(datacampOffer.getOfferId()));

        publishingProcessorTask.process();

        ProcessingStage<PublishingStatus> processingStage =
                publishingController.currentStageOf(publishingInfo.getProcessId().getId()).orElseThrow();
        assertThat(processingStage.getState(), is(PublishingStatus.CHECKING));

        PublishingCheckResult checkResult = processingStage.getDataAs(PublishingCheckResult.class);
        assertThat(checkResult, notNullValue());
        assertThat(checkResult.getOffersToCheck(), notNullValue());
        assertThat(checkResult.getOffersToCheck(), aMapWithSize(1));

        offerStorageTestHelper.mockStrollerSearchOffersResponse(List.of(datacampOffer.toBuilder()
                .hidePromo(promo.getId())
                .updatedAt(clock.dateTime())
                .build()));
        publishingProcessorTask.process();

        processingStage =
                publishingController.currentStageOf(publishingInfo.getProcessId().getId()).orElseThrow();
        assertThat(processingStage.getState(), is(PublishingStatus.CHECKED));

        publishingProcessorTask.process();

        processingStage =
                publishingController.currentStageOf(publishingInfo.getProcessId().getId()).orElseThrow();
        assertThat(processingStage.getState(), is(PublishingStatus.CLEARED));

        checkResult = processingStage.getDataAs(PublishingCheckResult.class);

        assertThat(checkResult, notNullValue());
        assertThat(publishingBatchDao.readBatches(publishingInfo.getProcessId(),
                checkResult.getOffersToCheck().keySet()), empty());

        assertThat(promoDao.get(promo.toPromoKey()).getHasNotPublishedChanges(), is(false));

        assertThat(datacampOfferDao.selectOne(datacampOffer.getOfferId()).getActivePromos(), anEmptyMap());
    }
}
