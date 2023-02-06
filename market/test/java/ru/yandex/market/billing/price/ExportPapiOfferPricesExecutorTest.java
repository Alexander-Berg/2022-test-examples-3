package ru.yandex.market.billing.price;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.SyncAPI.SyncChangeOffer;
import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.mds.s3.client.content.ContentProvider;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.logbroker.event.datacamp.PapiHiddenOfferDataCampEvent;
import ru.yandex.market.core.logbroker.event.datacamp.PapiOfferPriceDataCampEvent;
import ru.yandex.market.core.logbroker.event.datacamp.SyncChangeOfferLogbrokerEvent;
import ru.yandex.market.core.offer.IndexerOfferKey;
import ru.yandex.market.core.offer.OfferId;
import ru.yandex.market.core.offer.PapiHidingEvent;
import ru.yandex.market.core.offer.PapiMarketSkuOfferService;
import ru.yandex.market.core.price.ExpirablePapiOfferPrice;
import ru.yandex.market.core.price.PapiOfferPriceUpdate;
import ru.yandex.market.core.price.PapiOfferPriceValue;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.logbroker.LogbrokerService;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.proto.QPipe;
import ru.yandex.market.protobuf.streams.YandexSnappyInputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.billing.price.util.ChangeOfferLogbrokerTestUtil.assertEvent;
import static ru.yandex.market.billing.price.util.ChangeOfferLogbrokerTestUtil.assertEventType;
import static ru.yandex.market.billing.price.util.ChangeOfferLogbrokerTestUtil.assertHiddenOffer;
import static ru.yandex.market.billing.price.util.ChangeOfferLogbrokerTestUtil.getLogbrokerEvents;
import static ru.yandex.market.billing.price.util.ChangeOfferLogbrokerTestUtil.getOffers;
import static ru.yandex.market.core.logbroker.event.datacamp.DataCampEvent.toIdentifiers;

/**
 * @author Victor Nazarov &lt;sviperll@yandex-team.ru&gt;
 */
@DbUnitDataSet(
        before = "ExportPapiOfferPricesExecutorTest.before.csv"
)
class ExportPapiOfferPricesExecutorTest extends FunctionalTest {
    private static final int CLIENT_ID = 123;

    @Autowired
    private ExportPapiOfferPricesExecutor exportPapiOfferPricesExecutor;

    @Autowired
    private PapiMarketSkuOfferService papiMarketSkuOfferService;

    @Autowired
    private MdsS3Client mdsS3Client;

    @Autowired
    @Qualifier("marketQuickLogbrokerService")
    private LogbrokerEventPublisher<SyncChangeOfferLogbrokerEvent> marketQuickLogbrokerService;

    @Autowired
    private EnvironmentService environmentService;

    private static void assertIndexerOffer(IndexerOfferKey id, boolean isDeleted, QPipe.Offer offer,
                                           QPipe.MarketColor color) {
        assertEquals(id.feedId(), offer.getFeedId());
        assertEquals(id.marketSku(), offer.getMarketSku());
        if (id.hasMarketSku() && id.hasShopSku()) {
            assertEquals(id.marketSku(), offer.getMarketSku());
            assertEquals(id.shopSku(), offer.getOfferId());
        } else if (id.hasMarketSku()) {
            assertEquals(id.marketSku(), offer.getMarketSku());
        }
        if (id.hasShopSku()) {
            assertEquals(id.shopSku(), offer.getOfferId());
        }
        assertEquals(isDeleted, offer.getData(0).getFields().getPriceDeleted());
        assertEquals(color, offer.getMarketColor());
    }

    private static void assertShopOffer(IndexerOfferKey id, boolean isPriceDeleted, boolean isOfferDeleted,
                                        QPipe.Offer offer) {
        assertIndexerOffer(id, isPriceDeleted, offer, QPipe.MarketColor.WHITE);
        boolean actualOfferDeleted = offer.getData(0).getFields().getOfferDeleted();
        assertEquals(isOfferDeleted, actualOfferDeleted);
    }

    private static void assertMarketSkuOffer(IndexerOfferKey id, boolean isDeleted, QPipe.Offer offer) {
        assertIndexerOffer(id, isDeleted, offer, QPipe.MarketColor.BLUE);

    }

    private static void assertMarketSkuOfferSupplier(IndexerOfferKey id, boolean isDeleted, QPipe.Offer offer) {
        assertMarketSkuOffer(id, isDeleted, offer);
    }

    private static void assertMarketSkuOfferSupplier(IndexerOfferKey id, boolean isDeleted, long price,
                                                     QPipe.Offer offer) {
        assertMarketSkuOffer(id, isDeleted, price, offer);
    }

    private static void assertMarketSkuOffer(IndexerOfferKey id, boolean isDeleted, long price, QPipe.Offer offer) {
        assertMarketSkuOffer(id, isDeleted, offer);
        assertEquals(price, offer.getData(0).getFields().getBinaryPrice().getPrice());
        assertFalse(offer.getData(0).getFields().hasBinaryOldprice());
    }

    private static void assertMarketSkuOffer(
            IndexerOfferKey id,
            boolean isDeleted,
            long price,
            long discountBasePrice,
            QPipe.Offer offer) {
        assertMarketSkuOffer(id, isDeleted, offer);
        assertEquals(price, offer.getData(0).getFields().getBinaryPrice().getPrice());
        assertEquals(discountBasePrice, offer.getData(0).getFields().getBinaryOldprice().getPrice());
    }

    private static void assertOffer(int feedId, String offerId, boolean isDeleted, QPipe.Offer offer) {
        assertEquals(feedId, offer.getFeedId());
        assertEquals(offerId, offer.getOfferId());
        assertFalse(offer.hasMarketSku());
        assertEquals(isDeleted, offer.getData(0).getFields().getPriceDeleted());
        assertTrue(offer.getData(0).hasTimestamp());
        assertEquals(QPipe.MarketColor.WHITE, offer.getMarketColor());
    }

    private static void assertOffer(int feedId, String offerId, boolean isDeleted, long price, QPipe.Offer offer) {
        assertOffer(feedId, offerId, isDeleted, offer);
        assertEquals(price, offer.getData(0).getFields().getBinaryPrice().getPrice());
        assertFalse(offer.getData(0).getFields().hasBinaryOldprice());
    }

    private static void assertOffer(
            int feedId,
            String offerId,
            boolean isDeleted,
            long price,
            long discountBasePrice,
            QPipe.Offer offer) {
        assertOffer(feedId, offerId, isDeleted, offer);
        assertEquals(price, offer.getData(0).getFields().getBinaryPrice().getPrice());
        assertTrue(offer.getData(0).getFields().hasBinaryOldprice());
        assertEquals(discountBasePrice, offer.getData(0).getFields().getBinaryOldprice().getPrice());
    }

    private static OfferId offerId(long feedId, String offerId) {
        return OfferId.of(feedId, offerId);
    }

    private static IndexerOfferKey feedMSku(long feedId, long marketSku) {
        return IndexerOfferKey.marketSku(feedId, marketSku);
    }

    private static IndexerOfferKey feedMSku(long feedId, long marketSku, String shopSku) {
        return new IndexerOfferKey(feedId, marketSku, shopSku);
    }

    private static IndexerOfferKey marketShopSku(long feedId, long marketSku, String shopSku) {
        return IndexerOfferKey.anyMarketOrShopSkuUsedAndOtherIgnored(feedId, marketSku, shopSku);
    }

    private static IndexerOfferKey indexerOfferId(long feedId, String offerId) {
        return IndexerOfferKey.offerId(feedId, offerId);
    }

    private static ExpirablePapiOfferPrice expirablePrice(BigDecimal price) {
        return expirablePrice(price, null);
    }

    private static ExpirablePapiOfferPrice expirablePrice(BigDecimal price, @Nullable BigDecimal discountBasePrice) {
        PapiOfferPriceValue.Builder builder = new PapiOfferPriceValue.Builder();
        builder.setUpdatedAt(Instant.now());
        builder.setValue(price);
        builder.setCurrency(Currency.RUR);
        if (discountBasePrice != null) {
            builder.setDiscountBase(discountBasePrice);
        }
        PapiOfferPriceValue value = builder.build();
        return value.expirable(Instant.now().plus(Duration.ofDays(2)));
    }

    private static PapiOfferPriceUpdate priceUpdatedValue(BigDecimal price) {
        return expirablePrice(price).updatedBy(CLIENT_ID);
    }

    private static PapiOfferPriceUpdate priceUpdatedValue(BigDecimal price, BigDecimal discountBasePrice) {
        return expirablePrice(price, discountBasePrice).updatedBy(CLIENT_ID);
    }

    private static Map<OfferId, PapiOfferPriceUpdate> priceUpdate(long feedId, String offerId, BigDecimal price) {
        return Collections.singletonMap(offerId(feedId, offerId), priceUpdatedValue(price));
    }

    private static Map<IndexerOfferKey, ExpirablePapiOfferPrice> priceUpdate(IndexerOfferKey sku, BigDecimal price) {
        return Collections.singletonMap(sku, expirablePrice(price));
    }

    private static Map<IndexerOfferKey, ExpirablePapiOfferPrice> priceUpdate(
            IndexerOfferKey sku,
            BigDecimal price,
            BigDecimal discountBasePrice
    ) {
        return Collections.singletonMap(sku, expirablePrice(price, discountBasePrice));
    }

    private static Map<OfferId, PapiOfferPriceUpdate> priceUpdate(
            int feedId,
            String offerId,
            BigDecimal price,
            BigDecimal discountBasePrice) {
        return Collections.singletonMap(offerId(feedId, offerId), priceUpdatedValue(price, discountBasePrice));
    }

    /**
     * Тест на работы Job'ы по выгрузке diff'ов и snapshot'ов привязанных к offer-id.
     *
     * <ul>
     * <li>Запускаем джобу на пустой базе.
     * <li>Смотрим, что выгрузился snapshot, так как до этого не было snapshot'ов.
     * <li>Проверяем что snapshot пустой.
     * <li>Заполняем цены.
     * <li>Запускаем джобу.
     * <li>Смотрим, что выгрузился только diff, так как есть изменения, а snapshot был недавно.
     * <li>Проверяем содержимое diff'а.
     * <li>Запускаем джобу ещё раз смотрим, что
     * ничего не выгрузилось, так как изменений нет, а snapshot был недавно.
     * <li>Делаем несколько изменений.
     * <li>Запускаем джобу.
     * <li>Смотрим, что выгрузился только diff, так как есть изменения, а snapshot был недавно.
     * <li>Проверяем содержимое diff'а.
     * <li>Проверяем, что в каждом офере внутри одного и того же снапшота выставлено одно и тоже время и
     * что время первого снапшота меньше, чем время второго.
     * </ul>
     */
    @Test
    @DbUnitDataSet
    void testOfferIdExport() throws InterruptedException {
        MdsUploadAnswers uploads = new MdsUploadAnswers();
        Mockito.doAnswer(uploads).when(mdsS3Client).upload(Mockito.any(), Mockito.any());

        exportPapiOfferPricesExecutor.doJob(Mockito.mock(JobExecutionContext.class));
        assertEquals(1, uploads.answers.size());
        MdsUploadAnswer upload = uploads.answers.get(0);
        assertThat(upload.resourceLocation.getKey(),
                Matchers.startsWith("api/prices/snapshots/sn."));

        assertEquals(0, upload.offers.size());

        uploads.answers.clear();

        updateWhitePricesInTwoStorages(priceUpdate(123, "offer1", BigDecimal.valueOf(1595, 2)));
        updateWhitePricesInTwoStorages(
                priceUpdate(123, "offer2", BigDecimal.valueOf(3025, 2), BigDecimal.valueOf(2095, 2)));
        updateWhitePricesInTwoStorages(priceUpdate(123, "offer3", BigDecimal.valueOf(45)));
        updateWhitePricesInTwoStorages(priceUpdate(123, "offer4", BigDecimal.valueOf(35)));
        updateWhitePricesInTwoStorages(priceUpdate(234, "offer1", BigDecimal.valueOf(60)));
        updateWhitePricesInTwoStorages(priceUpdate(234, "offer2", BigDecimal.valueOf(30)));
        updateWhitePricesInTwoStorages(priceUpdate(234, "offer3", BigDecimal.valueOf(45)));

        exportPapiOfferPricesExecutor.doJob(Mockito.mock(JobExecutionContext.class));

        assertEquals(1, uploads.answers.size());
        upload = uploads.answers.get(0);
        assertThat(upload.resourceLocation.getKey(),
                Matchers.startsWith("api/prices/diffs/diff."));

        assertEquals(7, upload.offers.size());

        assertOffer(123, "offer1", false, 15_9500000L, upload.offers.get(0));
        assertOffer(123, "offer2", false, 30_2500000L, 20_9500000L, upload.offers.get(1));
        assertOffer(123, "offer3", false, 45_0000000L, upload.offers.get(2));
        assertOffer(123, "offer4", false, 35_0000000L, upload.offers.get(3));
        assertOffer(234, "offer1", false, 60_0000000L, upload.offers.get(4));
        assertOffer(234, "offer2", false, 30_0000000L, upload.offers.get(5));
        assertOffer(234, "offer3", false, 45_0000000L, upload.offers.get(6));

        int timestamp0 = upload.offers.get(0).getData(0).getTimestamp();
        assertTrue(timestamp0 <= upload.offers.get(1).getData(0).getTimestamp());
        assertTrue(timestamp0 <= upload.offers.get(2).getData(0).getTimestamp());
        assertTrue(timestamp0 <= upload.offers.get(3).getData(0).getTimestamp());
        assertTrue(timestamp0 <= upload.offers.get(4).getData(0).getTimestamp());
        assertTrue(timestamp0 <= upload.offers.get(5).getData(0).getTimestamp());
        assertTrue(timestamp0 <= upload.offers.get(6).getData(0).getTimestamp());

        uploads.answers.clear();

        exportPapiOfferPricesExecutor.doJob(Mockito.mock(JobExecutionContext.class));

        // Проверяем, что при пустом diff'e ничего не выгружается
        assertEquals(0, uploads.answers.size());

        Thread.sleep(1000);

        updateWhitePricesInTwoStorages(priceUpdate(123, "offer3", BigDecimal.valueOf(145)));
        updateWhitePricesInTwoStorages(priceUpdate(234, "offer2", BigDecimal.valueOf(130)));
        deleteWhitePricesInTwoStorages(Collections.singleton(offerId(123, "offer4")));

        exportPapiOfferPricesExecutor.doJob(Mockito.mock(JobExecutionContext.class));

        assertEquals(1, uploads.answers.size());
        upload = uploads.answers.get(0);
        assertThat(upload.resourceLocation.getKey(),
                Matchers.startsWith("api/prices/diffs/diff."));

        assertEquals(2, upload.offers.size());

        assertOffer(123, "offer3", false, 145_0000000L, upload.offers.get(0));
        assertOffer(234, "offer2", false, 130_0000000L, upload.offers.get(1));

        int timestamp1 = upload.offers.get(0).getData(0).getTimestamp();
        assertTrue(timestamp1 <= upload.offers.get(1).getData(0).getTimestamp());
        assertTrue(timestamp1 <= upload.offers.get(1).getData(0).getTimestamp());

        assertTrue(timestamp0 < timestamp1);
    }

    /**
     * Тест на работы Job'ы по выгрузке diff'ов и snapshot'ов привязанных к offer-id.
     *
     * <ul>
     * <li>Заполняем цены.
     * <li>Запускаем джобу.
     * <li>Смотрим, что выгрузился snapshot, так как до этого не было snapshot'ов.
     * <li>Проверяем содержимое snapshot'а.
     * </ul>
     */
    @Test
    @DbUnitDataSet
    void testOfferIdSnapshotExport() {
        MdsUploadAnswers uploads = new MdsUploadAnswers();
        Mockito.doAnswer(uploads).when(mdsS3Client).upload(Mockito.any(), Mockito.any());

        updateWhitePricesInTwoStorages(priceUpdate(123, "offer1", BigDecimal.valueOf(1595, 2)));
        updateWhitePricesInTwoStorages(
                priceUpdate(123, "offer2", BigDecimal.valueOf(3025, 2), BigDecimal.valueOf(2095, 2)));
        updateWhitePricesInTwoStorages(priceUpdate(123, "offer3", BigDecimal.valueOf(45)));
        updateWhitePricesInTwoStorages(priceUpdate(123, "offer4", BigDecimal.valueOf(35)));
        updateWhitePricesInTwoStorages(priceUpdate(234, "offer1", BigDecimal.valueOf(60)));
        updateWhitePricesInTwoStorages(priceUpdate(234, "offer2", BigDecimal.valueOf(30)));
        updateWhitePricesInTwoStorages(priceUpdate(234, "offer3", BigDecimal.valueOf(45)));

        exportPapiOfferPricesExecutor.doJob(Mockito.mock(JobExecutionContext.class));

        assertEquals(1, uploads.answers.size());
        MdsUploadAnswer upload = uploads.answers.get(0);
        assertThat(upload.resourceLocation.getKey(),
                Matchers.startsWith("api/prices/snapshots/sn."));

        assertEquals(7, upload.offers.size());

        assertOffer(123, "offer1", false, 15_9500000L, upload.offers.get(0));
        assertOffer(123, "offer2", false, 30_2500000L, 20_9500000L, upload.offers.get(1));
        assertOffer(123, "offer3", false, 45_0000000L, upload.offers.get(2));
        assertOffer(123, "offer4", false, 35_0000000L, upload.offers.get(3));
        assertOffer(234, "offer1", false, 60_0000000L, upload.offers.get(4));
        assertOffer(234, "offer2", false, 30_0000000L, upload.offers.get(5));
        assertOffer(234, "offer3", false, 45_0000000L, upload.offers.get(6));
    }

    /**
     * Тест на работы Job'ы по выгрузке diff'ов и snapshot'ов привязанных к market-sku.
     *
     * <ul>
     * <li>Заполняем цены.
     * <li>Запускаем джобу.
     * <li>Смотрим, что выгрузился snapshot, так как до этого не было snapshot'ов.
     * <li>Проверяем содержимое snapshot'а.
     * <li>Запускаем джобу ещё раз смотрим, что
     * ничего не выгрузилось, так как изменений нет, а snapshot был недавно.
     * <li>Делаем несколько изменений.
     * <li>Запускаем джобу.
     * <li>Смотрим, что выгрузился только diff, так как есть изменения, а snapshot был недавно.
     * <li>Проверяем содержимое diff'а.
     * </ul>
     */
    @Test
    @DbUnitDataSet
    void testMarketSkuExport() {
        papiMarketSkuOfferService.updatePrices(priceUpdate(feedMSku(123, 1, "offer1"), BigDecimal.valueOf(1595, 2)),
                CLIENT_ID,
                CampaignType.SUPPLIER, 774);
        papiMarketSkuOfferService.updatePrices(
                priceUpdate(feedMSku(123, 2, "offer2"), BigDecimal.valueOf(3025, 2), BigDecimal.valueOf(2095, 2)),
                CLIENT_ID,
                CampaignType.SUPPLIER, 774);
        papiMarketSkuOfferService
                .updatePrices(priceUpdate(feedMSku(123, 3, "offer3"), BigDecimal.valueOf(45)), CLIENT_ID,
                        CampaignType.SUPPLIER,
                        774);
        papiMarketSkuOfferService
                .updatePrices(priceUpdate(feedMSku(123, 4, "offer4"), BigDecimal.valueOf(35)), CLIENT_ID,
                        CampaignType.SUPPLIER,
                        774);
        papiMarketSkuOfferService
                .updatePrices(priceUpdate(feedMSku(124, 4, "offer1"), BigDecimal.valueOf(35)), CLIENT_ID,
                        CampaignType.DIRECT,
                        775);
        papiMarketSkuOfferService.hideOffers(
                ImmutableMap.of(
                        indexerOfferId(234, "qwerty"),
                        new PapiHidingEvent.Builder()
                                .setHiddenAt(Instant.now())
                                .setHidingExpiresAt(Instant.now().plus(10, ChronoUnit.DAYS))
                                .build()
                ),
                CLIENT_ID,
                CampaignType.SHOP,
                774);
        papiMarketSkuOfferService
                .updatePrices(priceUpdate(feedMSku(234, 2, "offer2"), BigDecimal.valueOf(30)), CLIENT_ID,
                        CampaignType.SUPPLIER,
                        774);
        papiMarketSkuOfferService
                .updatePrices(priceUpdate(feedMSku(234, 3, "offer3"), BigDecimal.valueOf(45)), CLIENT_ID,
                        CampaignType.SUPPLIER,
                        774);

        MdsUploadAnswers uploads = new MdsUploadAnswers();
        Mockito.doAnswer(uploads).when(mdsS3Client).upload(Mockito.any(), Mockito.any());

        exportPapiOfferPricesExecutor.doJob(Mockito.mock(JobExecutionContext.class));

        assertEquals(1, uploads.answers.size());
        MdsUploadAnswer upload = uploads.answers.get(0);
        assertThat(upload.resourceLocation.getKey(),
                Matchers.startsWith("api/prices/snapshots/sn."));

        assertEquals(7, upload.offers.size());

        assertMarketSkuOfferSupplier(feedMSku(123, 1, "offer1"), false, 15_9500000L, upload.offers.get(0));
        assertMarketSkuOffer(feedMSku(123, 2, "offer2"), false, 30_2500000L, 20_9500000L, upload.offers.get(1));
        assertMarketSkuOfferSupplier(feedMSku(123, 3, "offer3"), false, 45_0000000L, upload.offers.get(2));
        assertMarketSkuOfferSupplier(feedMSku(123, 4, "offer4"), false, 35_0000000L, upload.offers.get(3));
        assertMarketSkuOfferSupplier(feedMSku(234, 2, "offer2"), false, 30_0000000L, upload.offers.get(4));
        assertMarketSkuOfferSupplier(feedMSku(234, 3, "offer3"), false, 45_0000000L, upload.offers.get(5));
        assertShopOffer(indexerOfferId(234, "qwerty"), false, true, upload.offers.get(6));
        for (QPipe.Offer offer : upload.offers) {
            assertTrue(offer.getData(0).hasTimestamp());
        }

        uploads.answers.clear();

        exportPapiOfferPricesExecutor.doJob(Mockito.mock(JobExecutionContext.class));

        // Проверяем, что при пустом diff'e ничего не выгружается
        assertEquals(0, uploads.answers.size());

        papiMarketSkuOfferService
                .updatePrices(priceUpdate(feedMSku(123, 3, "offer3"), BigDecimal.valueOf(145)), CLIENT_ID,
                        CampaignType.SUPPLIER,
                        774);
        papiMarketSkuOfferService.showOffers(Collections.singleton(indexerOfferId(234, "qwerty")), Instant.now());
        papiMarketSkuOfferService.clearPrices(Collections.singleton(feedMSku(123, 4, "offer4")), Instant.now());

        exportPapiOfferPricesExecutor.doJob(Mockito.mock(JobExecutionContext.class));

        assertEquals(1, uploads.answers.size());
        upload = uploads.answers.get(0);
        assertThat(upload.resourceLocation.getKey(),
                Matchers.startsWith("api/prices/diffs/diff."));

        assertEquals(1, upload.offers.size());

        assertMarketSkuOfferSupplier(feedMSku(123, 3, "offer3"), false, 145_0000000L, upload.offers.get(0));
        for (QPipe.Offer offer : upload.offers) {
            assertTrue(offer.getData(0).hasTimestamp());
        }
    }

    /**
     * Тест на работы Job'ы по выгрузке diff'ов и snapshot'ов привязанных к market-sku и shop-sku.
     *
     * <ul>
     * <li>Заполняем цены.
     * <li>Запускаем джобу.
     * <li>Смотрим, что выгрузился snapshot, так как до этого не было snapshot'ов.
     * <li>Проверяем содержимое snapshot'а.
     * <li>Запускаем джобу ещё раз смотрим, что
     * ничего не выгрузилось, так как изменений нет, а snapshot был недавно.
     * <li>Делаем несколько изменений.
     * <li>Запускаем джобу.
     * <li>Смотрим, что выгрузился только diff, так как есть изменения, а snapshot был недавно.
     * <li>Проверяем содержимое diff'а.
     * </ul>
     */
    @Test
    @DbUnitDataSet
    void marketShopSkuExport() {
        papiMarketSkuOfferService
                .updatePrices(priceUpdate(marketShopSku(123, 1, "1"), BigDecimal.valueOf(1595, 2)), CLIENT_ID,
                        CampaignType.SUPPLIER, 774);
        papiMarketSkuOfferService.updatePrices(
                priceUpdate(marketShopSku(123, 2, "3"), BigDecimal.valueOf(3025, 2), BigDecimal.valueOf(2095, 2)),
                CLIENT_ID, CampaignType.SUPPLIER, 774);
        papiMarketSkuOfferService
                .updatePrices(priceUpdate(marketShopSku(123, 3, "33"), BigDecimal.valueOf(45)), CLIENT_ID,
                        CampaignType.SUPPLIER, 774);
        papiMarketSkuOfferService
                .updatePrices(priceUpdate(marketShopSku(123, 4, "4"), BigDecimal.valueOf(35)), CLIENT_ID,
                        CampaignType.SUPPLIER, 774);
        papiMarketSkuOfferService.hideOffers(
                ImmutableMap.of(
                        indexerOfferId(234, "qwerty"),
                        new PapiHidingEvent.Builder()
                                .setHiddenAt(Instant.now())
                                .setHidingExpiresAt(Instant.now().plus(10, ChronoUnit.DAYS))
                                .build()
                ),
                CLIENT_ID,
                CampaignType.SHOP,
                774);
        papiMarketSkuOfferService
                .updatePrices(priceUpdate(marketShopSku(234, 2, "3"), BigDecimal.valueOf(30)), CLIENT_ID,
                        CampaignType.SUPPLIER, 774);
        papiMarketSkuOfferService
                .updatePrices(priceUpdate(marketShopSku(234, 3, "4"), BigDecimal.valueOf(45)), CLIENT_ID,
                        CampaignType.SUPPLIER, 774);

        MdsUploadAnswers uploads = new MdsUploadAnswers();
        Mockito.doAnswer(uploads).when(mdsS3Client).upload(Mockito.any(), Mockito.any());

        exportPapiOfferPricesExecutor.doJob(Mockito.mock(JobExecutionContext.class));

        assertEquals(1, uploads.answers.size());
        MdsUploadAnswer upload = uploads.answers.get(0);
        assertThat(upload.resourceLocation.getKey(),
                Matchers.startsWith("api/prices/snapshots/sn."));

        assertEquals(7, upload.offers.size());

        assertMarketSkuOfferSupplier(marketShopSku(123, 1, "1"), false, 15_9500000L, upload.offers.get(0));
        assertMarketSkuOffer(marketShopSku(123, 2, "3"), false, 30_2500000L, 20_9500000L, upload.offers.get(1));
        assertMarketSkuOfferSupplier(marketShopSku(123, 3, "33"), false, 45_0000000L, upload.offers.get(2));
        assertMarketSkuOfferSupplier(marketShopSku(123, 4, "4"), false, 35_0000000L, upload.offers.get(3));
        assertMarketSkuOfferSupplier(marketShopSku(234, 2, "3"), false, 30_0000000L, upload.offers.get(4));
        assertMarketSkuOfferSupplier(marketShopSku(234, 3, "4"), false, 45_0000000L, upload.offers.get(5));
        assertShopOffer(indexerOfferId(234, "qwerty"), false, true, upload.offers.get(6));
        for (QPipe.Offer offer : upload.offers) {
            assertTrue(offer.getData(0).hasTimestamp());
        }

        uploads.answers.clear();

        exportPapiOfferPricesExecutor.doJob(Mockito.mock(JobExecutionContext.class));

        // Проверяем, что при пустом diff'e ничего не выгружается
        assertEquals(0, uploads.answers.size());

        papiMarketSkuOfferService
                .updatePrices(priceUpdate(marketShopSku(123, 3, "33"), BigDecimal.valueOf(145)), CLIENT_ID,
                        CampaignType.SUPPLIER, 774);
        papiMarketSkuOfferService.showOffers(Collections.singleton(indexerOfferId(234, "qwerty")), Instant.now());
        papiMarketSkuOfferService.clearPrices(Collections.singleton(marketShopSku(123, 4, "4")), Instant.now());

        exportPapiOfferPricesExecutor.doJob(Mockito.mock(JobExecutionContext.class));

        assertEquals(1, uploads.answers.size());
        upload = uploads.answers.get(0);
        assertThat(upload.resourceLocation.getKey(),
                Matchers.startsWith("api/prices/diffs/diff."));

        assertEquals(1, upload.offers.size());

        //Все что не deleted теперь удаляется, в продакшн оно и раньше удалялось, в тесте не было триггера
        assertMarketSkuOfferSupplier(marketShopSku(123, 3, "33"), false, 145_0000000L, upload.offers.get(0));
        for (QPipe.Offer offer : upload.offers) {
            assertTrue(offer.getData(0).hasTimestamp());
        }
    }

    /**
     * Сценарий, реализованной в тесте:
     * <ol>
     * <li>Происходит апдейт цены у белого оффера</li>
     * <li>Происходит скрытие белого оффера</li>
     * <li>Запускается джоба</li>
     * <li>В snapshot попадает 2 записи по одному офферу<li/>
     * <li>Запускается джоба<li/>
     * <li>Пустой snapshot и diff<li/>
     * <li>Происзодит открытие оффера - попадает в diff</li>
     * <li>Происходит изменение цены - попадает в diff</li>
     * <li>Измненеие цены и скрытия - двойной diff</li>
     * </ol>
     * В снэпшот и дифф выгружаются 2 записи со следующими непротиворечащими апдейтами:
     * [feed_id: 234
     * offer_id: "qwerty"
     * data {
     * fields {
     * binary_price {
     * price: 159500000
     * id: "RUR"
     * }
     * }
     * source: API
     * type: PRICES
     * timestamp: 1532603221
     * }
     * market_color: WHITE
     * , feed_id: 234
     * offer_id: "qwerty"
     * data {
     * fields {
     * offer_deleted: true
     * }
     * source: API
     * type: PRICES
     * timestamp: 1532603221
     * }
     * market_color: WHITE
     * ]
     */
    @Test
    @DbUnitDataSet
    void hiddenAndPriceParallelUpdateFowWhiteOffer() {
        updateWhitePricesInTwoStorages(priceUpdate(234, "qwerty", BigDecimal.valueOf(1595, 2)));
        papiMarketSkuOfferService.hideOffers(
                ImmutableMap.of(
                        indexerOfferId(234, "qwerty"),
                        new PapiHidingEvent.Builder()
                                .setHiddenAt(Instant.now())
                                .setHidingExpiresAt(Instant.now().plus(10, ChronoUnit.DAYS))
                                .build()
                ),
                CLIENT_ID,
                CampaignType.SHOP,
                774);
        MdsUploadAnswers uploads = new MdsUploadAnswers();
        Mockito.doAnswer(uploads).when(mdsS3Client).upload(Mockito.any(), Mockito.any());

        exportPapiOfferPricesExecutor.doJob(Mockito.mock(JobExecutionContext.class));

        assertEquals(1, uploads.answers.size());
        MdsUploadAnswer upload = uploads.answers.get(0);
        assertThat(upload.resourceLocation.getKey(),
                Matchers.startsWith("api/prices/snapshots/sn."));

        assertEquals(1, upload.offers.size());
        assertOffer(234, "qwerty", false, 15_9500000L, upload.offers.get(0));
        assertShopOffer(indexerOfferId(234, "qwerty"), false, true, upload.offers.get(0));

        uploads.answers.clear();

        exportPapiOfferPricesExecutor.doJob(Mockito.mock(JobExecutionContext.class));

        // Проверяем, что при пустом diff'e ничего не выгружается
        assertEquals(0, uploads.answers.size());

        papiMarketSkuOfferService.showOffers(Collections.singleton(indexerOfferId(234, "qwerty")), Instant.now());
        exportPapiOfferPricesExecutor.doJob(Mockito.mock(JobExecutionContext.class));

        assertEquals(1, uploads.answers.size());
        upload = uploads.answers.get(0);
        assertThat(upload.resourceLocation.getKey(),
                Matchers.startsWith("api/prices/diffs/diff."));
        assertEquals(1, upload.offers.size());
        assertShopOffer(indexerOfferId(234, "qwerty"), false, false, upload.offers.get(0));

        uploads.answers.clear();

        updateWhitePricesInTwoStorages(priceUpdate(234, "qwerty", BigDecimal.valueOf(145)));
        exportPapiOfferPricesExecutor.doJob(Mockito.mock(JobExecutionContext.class));

        assertEquals(1, uploads.answers.size());
        upload = uploads.answers.get(0);
        assertThat(upload.resourceLocation.getKey(),
                Matchers.startsWith("api/prices/diffs/diff."));
        assertEquals(1, upload.offers.size());
        assertOffer(234, "qwerty", false, 145_0000000L, upload.offers.get(0));

        uploads.answers.clear();

        updateWhitePricesInTwoStorages(priceUpdate(234, "qwerty", BigDecimal.valueOf(777)));
        papiMarketSkuOfferService.hideOffers(
                ImmutableMap.of(
                        indexerOfferId(234, "qwerty"),
                        new PapiHidingEvent.Builder()
                                .setHiddenAt(Instant.now())
                                .setHidingExpiresAt(Instant.now().plus(10, ChronoUnit.DAYS))
                                .build()
                ),
                CLIENT_ID,
                CampaignType.SHOP,
                774);
        exportPapiOfferPricesExecutor.doJob(Mockito.mock(JobExecutionContext.class));

        assertEquals(1, uploads.answers.size());
        upload = uploads.answers.get(0);
        assertThat(upload.resourceLocation.getKey(),
                Matchers.startsWith("api/prices/diffs/diff."));
        assertEquals(1, upload.offers.size());
        assertOffer(234, "qwerty", false, 777_0000000L, upload.offers.get(0));
        assertShopOffer(indexerOfferId(234, "qwerty"), false, true, upload.offers.get(0));
    }

    @Test
    @DbUnitDataSet(
            before = "ExportPapiOfferPricesExecutorTest.ttl.before.csv",
            after = "ExportPapiOfferPricesExecutorTest.ttl.after.csv"
    )
    void testTtl() {
        environmentService.setValue("market.quick.partner-api.send.to.logbroker", "true");
        exportPapiOfferPricesExecutor.doJob(Mockito.mock(JobExecutionContext.class));

        List<SyncChangeOfferLogbrokerEvent> defaultLogbrokerEvents = getLogbrokerEvents(marketQuickLogbrokerService, 2);

        assertEvent(defaultLogbrokerEvents, 0, PapiHiddenOfferDataCampEvent.class);
        assertEvent(defaultLogbrokerEvents, 1, PapiOfferPriceDataCampEvent.class);

        assertWhite(defaultLogbrokerEvents);
        assertBlue(defaultLogbrokerEvents, 0);
    }

    private void assertBlue(List<SyncChangeOfferLogbrokerEvent> supplierLogbrokerEvents, int index) {
        Map<DataCampOfferIdentifiers.OfferIdentifiers, DataCampOffer.Offer> dataCampEvents =
                getOffers(supplierLogbrokerEvents, index);

        Assertions.assertThat(dataCampEvents).hasSize(8);

        assertHiddenOffer(
                dataCampEvents.get(toIdentifiers(null, 1003, CampaignType.SUPPLIER,
                        new IndexerOfferKey(105, 123, "1003", null))),
                false,
                DataCampOfferMeta.DataSource.PUSH_PARTNER_API,
                DataCampOfferMeta.MarketColor.BLUE
        );
        assertHiddenOffer(
                dataCampEvents.get(toIdentifiers(null, 1003, CampaignType.SUPPLIER,
                        new IndexerOfferKey(105, 124, "###", null))),
                false,
                DataCampOfferMeta.DataSource.PUSH_PARTNER_API,
                DataCampOfferMeta.MarketColor.BLUE
        );
        assertHiddenOffer(
                dataCampEvents.get(toIdentifiers(null, 1001, CampaignType.SUPPLIER,
                        new IndexerOfferKey(101, 124, "1004", null))),
                false,
                DataCampOfferMeta.DataSource.PULL_PARTNER_API,
                DataCampOfferMeta.MarketColor.BLUE
        );
        assertHiddenOffer(
                dataCampEvents.get(toIdentifiers(null, 1001, CampaignType.SUPPLIER,
                        new IndexerOfferKey(101, 127, "1007", null))),
                false,
                DataCampOfferMeta.DataSource.PULL_PARTNER_API,
                DataCampOfferMeta.MarketColor.BLUE
        );
    }

    private void assertWhite(List<SyncChangeOfferLogbrokerEvent> supplierLogbrokerEvents) {
        Map<DataCampOfferIdentifiers.OfferIdentifiers, DataCampOffer.Offer> dataCampEvents =
                getOffers(supplierLogbrokerEvents, 0);

        Assertions.assertThat(dataCampEvents).hasSize(8);

        assertHiddenOffer(
                dataCampEvents.get(toIdentifiers(null, 1002, CampaignType.SHOP,
                        new IndexerOfferKey(102, 127, "1007"))),
                false,
                DataCampOfferMeta.DataSource.PULL_PARTNER_API,
                DataCampOfferMeta.MarketColor.WHITE
        );

        assertHiddenOffer(
                dataCampEvents.get(toIdentifiers(null, 1004, CampaignType.SHOP,
                        new IndexerOfferKey(106, 0, "offerId"))),
                false,
                DataCampOfferMeta.DataSource.MARKET_PRICELABS,
                DataCampOfferMeta.MarketColor.WHITE
        );

    }

    private void updateWhitePricesInTwoStorages(Map<OfferId, PapiOfferPriceUpdate> updates) {
        HashMap<IndexerOfferKey, ExpirablePapiOfferPrice> newStorageSystemUpdates = updates.entrySet()
                .stream()
                .map(e -> Collections.singletonMap(e.getKey().toIndexerOfferKey(), e.getValue().expirablePrice()))
                .collect(HashMap::new, Map::putAll, Map::putAll);
        papiMarketSkuOfferService.updatePrices(newStorageSystemUpdates, CLIENT_ID, CampaignType.SHOP, 774);
    }

    private void deleteWhitePricesInTwoStorages(Set<OfferId> offerIds) {
        Set<IndexerOfferKey> newStorageSystemKeys =
                offerIds.stream().map(OfferId::toIndexerOfferKey).collect(Collectors.toSet());
        papiMarketSkuOfferService.clearPrices(newStorageSystemKeys, Instant.now());
    }

    private static class MdsUploadAnswers implements Answer<Void> {
        private List<MdsUploadAnswer> answers = new ArrayList<>();

        @Nullable
        @Override
        public Void answer(@Nonnull InvocationOnMock invocation) {
            Object[] arguments = invocation.getArguments();
            ResourceLocation resourceLocation = (ResourceLocation) arguments[0];
            ContentProvider contentProvider = (ContentProvider) arguments[1];
            try (YandexSnappyInputStream stream = new YandexSnappyInputStream(contentProvider.getInputStream())) {
                OfferReader offerReader = new OfferReader(stream);
                List<QPipe.Offer> offers = offerReader.readOffers();
                answers.add(new MdsUploadAnswer(resourceLocation, offers));
                return null;
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }
    }

    private static class MdsUploadAnswer {
        private final ResourceLocation resourceLocation;
        private final List<QPipe.Offer> offers;

        MdsUploadAnswer(ResourceLocation resourceLocation, List<QPipe.Offer> offers) {
            this.resourceLocation = resourceLocation;
            this.offers = offers;
        }
    }

    private static class OfferReader {
        private final YandexSnappyInputStream stream;

        public OfferReader(YandexSnappyInputStream stream) {
            this.stream = stream;
        }

        @Nonnull
        private List<QPipe.Offer> readOffers() throws IOException {
            List<QPipe.Offer> result = new ArrayList<>();
            verifyMagic();
            Optional<QPipe.Offer> offer = readOffer();
            while (offer.isPresent()) {
                result.add(offer.get());
                offer = readOffer();
            }
            return result;
        }

        @Nonnull
        private Optional<QPipe.Offer> readOffer() throws IOException {
            OptionalInt length = readLittleEndianInt();
            if (length.isEmpty()) {
                return Optional.empty();
            } else {
                byte[] buffer = new byte[length.getAsInt()];
                int read = stream.read(buffer);
                assertEquals(buffer.length, read);
                return Optional.of(QPipe.Offer.parseFrom(buffer));
            }
        }

        @Nonnull
        private OptionalInt readLittleEndianInt() throws IOException {
            int b = stream.read();
            if (b < 0) {
                return OptionalInt.empty();
            }
            int result = b;
            for (int i = 1; i < 4; i++) {
                b = stream.read();
                assertTrue(b >= 0);
                result = result | (b << (i * 8));
            }
            return OptionalInt.of(result);
        }

        private void verifyMagic() throws IOException {
            byte[] magic = PapiOfferPriceExportService.QPIPE_MAGIC.getBytes(StandardCharsets.US_ASCII);
            byte[] readMagic = new byte[magic.length];
            int result = stream.read(readMagic);
            assertEquals(readMagic.length, result);
            assertArrayEquals(magic, readMagic);
        }
    }
}
