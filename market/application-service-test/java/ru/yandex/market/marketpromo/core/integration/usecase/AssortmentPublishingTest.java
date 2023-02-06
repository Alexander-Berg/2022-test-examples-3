package ru.yandex.market.marketpromo.core.integration.usecase;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import Market.DataCamp.API.DatacampMessageOuterClass;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import ru.yandex.kikimr.persqueue.auth.Credentials;
import ru.yandex.market.marketpromo.core.application.context.Logbroker;
import ru.yandex.market.marketpromo.core.application.properties.LogbrokerAssortmentPropagationTopicProperties;
import ru.yandex.market.marketpromo.core.application.properties.LogbrokerAssortmentTopicProperties;
import ru.yandex.market.marketpromo.core.dao.DatacampOfferDao;
import ru.yandex.market.marketpromo.core.dao.PromoDao;
import ru.yandex.market.marketpromo.core.data.source.logbroker.MessageConsumer;
import ru.yandex.market.marketpromo.core.data.source.logbroker.MessagesStreamListener;
import ru.yandex.market.marketpromo.core.data.source.offerstorage.util.OfferDataConverter;
import ru.yandex.market.marketpromo.core.test.ServiceTestBase;
import ru.yandex.market.marketpromo.core.test.generator.Offers;
import ru.yandex.market.marketpromo.core.utils.LogbrokerUtils;
import ru.yandex.market.marketpromo.model.DatacampOffer;
import ru.yandex.market.marketpromo.model.DirectDiscountOfferParticipation;
import ru.yandex.market.marketpromo.model.OfferId;
import ru.yandex.market.marketpromo.model.OfferPromoParticipation;
import ru.yandex.market.marketpromo.model.Promo;
import ru.yandex.market.marketpromo.model.processing.PublishingInfo;
import ru.yandex.market.marketpromo.model.processing.PublishingStatus;
import ru.yandex.market.marketpromo.service.AssortmentService;
import ru.yandex.market.marketpromo.utils.IdentityUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.marketpromo.core.test.config.TestApplicationProfiles.LOGBROKER_ACTIVE;
import static ru.yandex.market.marketpromo.core.test.config.TestApplicationProfiles.OFFER_STORAGE_ACTIVE;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.business;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.categoryId;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.feed;
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

@ActiveProfiles({LOGBROKER_ACTIVE, OFFER_STORAGE_ACTIVE})
@TestPropertySource(
        properties = {
                LogbrokerAssortmentPropagationTopicProperties.PREFIX + ".topic=/marketpromo/testing" +
                        "/promo_assortment_changes_propagation",
                LogbrokerAssortmentPropagationTopicProperties.PREFIX + ".consumer=/marketpromo/testing" +
                        "/promo_assortment_changes_propagation_cns",
                LogbrokerAssortmentTopicProperties.PREFIX +
                        ".topic=/marketpromo/testing/promo_assortment_changes",
                LogbrokerAssortmentTopicProperties.PREFIX +
                        ".consumer=/marketpromo/testing/promo_assortment_changes_cns_for_test",
                "market.marketpromo.ciface-promo.tvm.clientId=2021562",
                "market.marketpromo.ciface-promo.tvm.logbroker.clientId=2001059",
                "market.marketpromo.ciface-promo.tvm.stroller.clientId=2011472",
                "market.marketpromo.ciface-promo.offerstorage.dataCampUrl=" +
                        "http://datacamp.blue.tst.vs.market.yandex.net",
                "market.marketpromo.ciface-promo.offerstorage.dataCampUrl=" +
                        "http://datacamp.blue.tst.vs.market.yandex.net",
                "client_secret={secret-key}"
        }
)
@Disabled
public class AssortmentPublishingTest extends ServiceTestBase {

    private static final long SHOP_ID = 10264169L;
    private static final String SSKU_1 = "00100.televizzzor2";

    @Autowired
    private PromoDao promoDao;
    @Autowired
    private DatacampOfferDao datacampOfferDao;
    @Autowired
    private AssortmentService assortmentService;
    @Autowired
    private Supplier<Credentials> credentialsSupplier;
    @Autowired
    private LogbrokerAssortmentPropagationTopicProperties assortmentPropagationTopicProperties;
    @Autowired
    private LogbrokerAssortmentTopicProperties assortmentTopicProperties;
    @Autowired
    private OfferDataConverter offerDataConverter;
    @Autowired
    @Logbroker
    private ExecutorService executorService;

    private Promo promo;

    @BeforeEach
    void configure() {
        promo = promoDao.replace(promo(
                id(IdentityUtils.hashId("#6304")),
                promoName("Тестовая акция ПИ для КИ 2 (smelesh)"),
                directDiscount(
                        minimalDiscountPercentSize(10)
                )
        ));

        datacampOfferDao.replace(List.of(
                Offers.datacampOffer(
                        business(10447296),
                        shop(SHOP_ID),
                        warehouse(145),
                        name("Телевизор LG 49LJ594V 48.5\\\" (2017)"),
                        categoryId(90639),
                        feed(200344511),
                        shopSku(SSKU_1),
                        marketSku(1723739563),
                        price(5200),
                        potentialPromo(promo.getId())
                )
        ));
    }

    @Test
    void shouldPublishAssortment() throws InterruptedException {
        Semaphore publishedSignal = new Semaphore(0);
        AtomicReference<List<DatacampOffer>> publishedReference = new AtomicReference<>();
        MessageConsumer publishingConsumer = makeMessageConsumer(MessagesStreamListener.ofThrowable(messages -> {
            publishedReference.set(messages.stream()
                    .flatMap(m -> m.getUnitedOffersList().stream())
                    .flatMap(unitedOffersBatch -> unitedOffersBatch.getOfferList().stream())
                    .map(unitedOffer -> offerDataConverter.mergeDataToOffer(unitedOffer))
                    .filter(offer -> offer.getShopSku().equals(SSKU_1))
                    .collect(Collectors.toUnmodifiableList()));
            if (!publishedReference.get().isEmpty()) {
                publishedSignal.release();
            }
        }, DatacampMessageOuterClass.DatacampMessage::parseFrom));

        Semaphore updatedSignal = new Semaphore(0);
        AtomicReference<List<DatacampOffer>> updatedReference = new AtomicReference<>();
        MessageConsumer backloopConsumer = makeMessageConsumerFromDataCamp(
                MessagesStreamListener.ofThrowable(messages -> {
                    updatedReference.set(messages.stream()
                            .flatMap(m -> m.getUnitedOffersList().stream())
                            .flatMap(unitedOffersBatch -> unitedOffersBatch.getOfferList().stream())
                            .map(unitedOffer -> offerDataConverter.mergeDataToOffer(unitedOffer))
                            .filter(offer -> offer.getShopSku().equals(SSKU_1))
                            .collect(Collectors.toUnmodifiableList()));
                    if (!updatedReference.get().isEmpty()) {
                        updatedSignal.release();
                    }
                }, DatacampMessageOuterClass.DatacampMessage::parseFrom));

        publishingConsumer.consume();
        backloopConsumer.consume();

        assortmentService.markDirectDiscountToParticipate(promo.toPromoKey(),
                List.of(DirectDiscountOfferParticipation.builder()
                        .offerPromoParticipation(OfferPromoParticipation.builder()
                                .promoId(promo.getId())
                                .participate(true)
                                .offerId(OfferId.of(IdentityUtils.hashId(SSKU_1), SHOP_ID))
                                .build())
                        .fixedBasePrice(BigDecimal.valueOf(9999))
                        .fixedPrice(BigDecimal.valueOf(7000))
                        .build()));

        PublishingInfo publishingInfo =
                assortmentService.publishPromoAssortment(promo.toPromoKey());

        assertThat(publishingInfo.getPublishingStatus(), is(PublishingStatus.PUBLISHED));

        assertTrue(publishedSignal.tryAcquire(15, TimeUnit.SECONDS));
        assertTrue(updatedSignal.tryAcquire(25, TimeUnit.MINUTES));

        publishingConsumer.stop();
        backloopConsumer.stop();

        assertThat(publishedReference.get(), not(empty()));
        assertThat(publishedReference.get().iterator().next().getActivePromo(promo.getId()), notNullValue());
        assertThat(updatedReference.get(), not(empty()));
        assertThat(updatedReference.get().iterator().next().getActivePromo(promo.getId()), notNullValue());
    }

    @Nonnull
    private <T> MessageConsumer makeMessageConsumer(@Nonnull MessagesStreamListener<T> listener) {
        return LogbrokerUtils.listenMessages(
                LogbrokerUtils.makeInstallation(assortmentPropagationTopicProperties, credentialsSupplier,
                        executorService),
                LogbrokerUtils.makeConsumerConfig(assortmentPropagationTopicProperties, credentialsSupplier,
                        executorService),
                listener
        );
    }

    @Nonnull
    private <T> MessageConsumer makeMessageConsumerFromDataCamp(@Nonnull MessagesStreamListener<T> listener) {
        return LogbrokerUtils.listenMessages(
                LogbrokerUtils.makeInstallation(assortmentTopicProperties, credentialsSupplier, executorService),
                LogbrokerUtils.makeConsumerConfig(assortmentTopicProperties, credentialsSupplier, executorService),
                listener
        );
    }
}
