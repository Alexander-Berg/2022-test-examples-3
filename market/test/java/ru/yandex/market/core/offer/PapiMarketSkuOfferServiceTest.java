package ru.yandex.market.core.offer;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.price.ExpirablePapiOfferPrice;
import ru.yandex.market.core.price.FeedMarketSkuPapiPriceUpdate;
import ru.yandex.market.core.price.PapiOfferPriceValue;
import ru.yandex.market.core.tax.model.VatRate;
import ru.yandex.market.mbi.web.paging.Paging;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.core.offer.IndexerOfferKey.anyMarketOrShopSkuUsedAndOtherIgnored;

@DbUnitDataSet
@ParametersAreNonnullByDefault
@DbUnitDataSet(before = "PapiMarketSkuOfferServiceTest.before.csv")
class PapiMarketSkuOfferServiceTest extends FunctionalTest {
    private static final long CLIENT_ID = 789L;
    private static final long DATASOURCE_ID = 987L;

    @Autowired
    private PapiMarketSkuOfferService marketSkuOfferService;

    private static BigDecimal bigDecimal(long value) {
        return BigDecimal.valueOf(value);
    }

    private static IndexerOfferKey feedMsku(long feedId, long marketSku) {
        return IndexerOfferKey.marketSku(feedId, marketSku);
    }

    private static IndexerOfferKey feedMsku(long feedId, long marketSku, String shopSku) {
        return new IndexerOfferKey(feedId, marketSku, shopSku);
    }

    private static IndexerOfferKey shopSku(long feedId, String shopSku) {
        return IndexerOfferKey.offerId(feedId, shopSku);
    }

    private static IndexerOfferKey marketShopSku(long feedId, long marketSku, String shopSku) {
        return anyMarketOrShopSkuUsedAndOtherIgnored(feedId, marketSku, shopSku);
    }

    @DisplayName("Для пуш-партнёров не должны выгружаться диффы папишных цен")
    @Test
    @DbUnitDataSet(before = "PapiMarketSkuOfferServiceTest.push.before.csv")
    void doNotFetchPushPartnerDiffs() {

        Map<IndexerOfferKey, PapiOfferPropertyDiff> diffs = new HashMap<>();

        //IS_PUSH_PARTNER = REAL
        final long realPushPartner = 101;

        marketSkuOfferService
                .updatePrices(priceUpdate(123, 3, "offer3", bigDecimal(115)), CLIENT_ID, CampaignType.SUPPLIER, realPushPartner);
        marketSkuOfferService
                .updatePrices(priceUpdate(123, 5, "offer5", bigDecimal(55)), CLIENT_ID, CampaignType.SUPPLIER, realPushPartner);

        // Генерируем diff между текущей таблицей и экспортированной таблицей
        marketSkuOfferService.generateDiff(1000);
        marketSkuOfferService.streamDiffRecords(100, diff -> {
            diffs.put(diff.indexerOfferKey(), diff.properties());
        });

        //Для REAL изменения не приходят
        Assertions.assertEquals(0, diffs.size());

        marketSkuOfferService.applyButDoNotClearDiff(1000);
        marketSkuOfferService.clearDiff();
        diffs.clear();

        //IS_PUSH_PARTNER = SBX
        final long sandboxPartner = 102;

        marketSkuOfferService
                .updatePrices(priceUpdate(124, 3, "offer3", bigDecimal(115)), CLIENT_ID, CampaignType.SUPPLIER, sandboxPartner);

        // Генерируем diff между текущей таблицей и экспортированной таблицей
        marketSkuOfferService.generateDiff(1000);
        marketSkuOfferService.streamDiffRecords(100, diff -> {
            diffs.put(diff.indexerOfferKey(), diff.properties());
        });

        //Для SBX изменения приходят
        Assertions.assertEquals(1, diffs.size());

        marketSkuOfferService.applyButDoNotClearDiff(1000);
        marketSkuOfferService.clearDiff();
        diffs.clear();

        //IS_PUSH_PARTNER = NO
        final long pullPartner = 103;

        marketSkuOfferService
                .updatePrices(priceUpdate(125, 3, "offer3", bigDecimal(115)), CLIENT_ID, CampaignType.SUPPLIER, pullPartner);
        marketSkuOfferService
                .updatePrices(priceUpdate(125, 5, "offer5", bigDecimal(55)), CLIENT_ID, CampaignType.SUPPLIER, pullPartner);
        marketSkuOfferService
                .updatePrices(priceUpdate(125, 7, "offer7", bigDecimal(155)), CLIENT_ID, CampaignType.SUPPLIER, pullPartner);

        // Генерируем diff между текущей таблицей и экспортированной таблицей
        marketSkuOfferService.generateDiff(1000);
        marketSkuOfferService.streamDiffRecords(100, diff -> {
            diffs.put(diff.indexerOfferKey(), diff.properties());
        });

        //Для NO изменения приходят
        Assertions.assertEquals(3, diffs.size());

        marketSkuOfferService.applyButDoNotClearDiff(1000);
        marketSkuOfferService.clearDiff();
        diffs.clear();

        //IS_PUSH_PARTNER = NULL
        final long pullNullPartner = 104;

        marketSkuOfferService
                .updatePrices(priceUpdate(126, 3, "offer3", bigDecimal(115)), CLIENT_ID, CampaignType.SUPPLIER, pullNullPartner);
        marketSkuOfferService
                .updatePrices(priceUpdate(126, 5, "offer5", bigDecimal(55)), CLIENT_ID, CampaignType.SUPPLIER, pullNullPartner);

        // Генерируем diff между текущей таблицей и экспортированной таблицей
        marketSkuOfferService.generateDiff(1000);
        marketSkuOfferService.streamDiffRecords(100, diff -> {
            diffs.put(diff.indexerOfferKey(), diff.properties());
        });

        //Для NULL изменения приходят
        Assertions.assertEquals(2, diffs.size());
    }

    @DisplayName("Для пуш-партнёров снэпшоты выгружаться не должны")
    @Test
    @DbUnitDataSet(before = "PapiMarketSkuOfferServiceTest.push.before.csv")
    void doNotFetchPushPartnerSnapshots() {

        //IS_PUSH_PARTNER = REAL
        final long realPushPartner = 101;

        marketSkuOfferService
                .updatePrices(priceUpdate(123, 3, "offer3", bigDecimal(115)), CLIENT_ID, CampaignType.SUPPLIER, realPushPartner);
        marketSkuOfferService
                .updatePrices(priceUpdate(123, 5, "offer5", bigDecimal(55)), CLIENT_ID, CampaignType.SUPPLIER, realPushPartner);

        marketSkuOfferService.generateDiff(1000);
        marketSkuOfferService.applyButDoNotClearDiff(1000);
        marketSkuOfferService.clearDiff();

        Map<IndexerOfferKey, PapiOfferProperties> offers = new HashMap<>();
        marketSkuOfferService.streamPublishedEntries(
                100,
                offer -> offers.put(offer.feedMarketSku(), offer.properties()));

        Assertions.assertEquals(offers.size(), 0);

        offers.clear();

        //IS_PUSH_PARTNER = SBX
        final long sandboxPartner = 102;

        marketSkuOfferService
                .updatePrices(priceUpdate(124, 3, "offer3", bigDecimal(115)), CLIENT_ID, CampaignType.SUPPLIER, sandboxPartner);

        marketSkuOfferService.generateDiff(1000);
        marketSkuOfferService.applyButDoNotClearDiff(1000);
        marketSkuOfferService.clearDiff();

        marketSkuOfferService.streamPublishedEntries(
                100,
                offer -> offers.put(offer.feedMarketSku(), offer.properties()));

        Assertions.assertEquals(offers.size(), 1);

        offers.clear();

        //IS_PUSH_PARTNER = NO
        final long pullPartner = 103;

        marketSkuOfferService
                .updatePrices(priceUpdate(125, 3, "offer3", bigDecimal(115)), CLIENT_ID, CampaignType.SUPPLIER, pullPartner);
        marketSkuOfferService
                .updatePrices(priceUpdate(125, 5, "offer5", bigDecimal(55)), CLIENT_ID, CampaignType.SUPPLIER, pullPartner);
        marketSkuOfferService
                .updatePrices(priceUpdate(125, 7, "offer7", bigDecimal(155)), CLIENT_ID, CampaignType.SUPPLIER, pullPartner);

        marketSkuOfferService.generateDiff(1000);
        marketSkuOfferService.applyButDoNotClearDiff(1000);
        marketSkuOfferService.clearDiff();

        marketSkuOfferService.streamPublishedEntries(
                100,
                offer -> offers.put(offer.feedMarketSku(), offer.properties()));

        Assertions.assertEquals(offers.size(), 4);

        offers.clear();

        //IS_PUSH_PARTNER = NULL
        final long pullNullPartner = 104;

        marketSkuOfferService
                .updatePrices(priceUpdate(126, 3, "offer3", bigDecimal(115)), CLIENT_ID, CampaignType.SUPPLIER, pullNullPartner);
        marketSkuOfferService
                .updatePrices(priceUpdate(126, 5, "offer5", bigDecimal(55)), CLIENT_ID, CampaignType.SUPPLIER, pullNullPartner);

        marketSkuOfferService.generateDiff(1000);
        marketSkuOfferService.applyButDoNotClearDiff(1000);
        marketSkuOfferService.clearDiff();

        marketSkuOfferService.streamPublishedEntries(
                100,
                offer -> offers.put(offer.feedMarketSku(), offer.properties()));

        Assertions.assertEquals(offers.size(), 6);
    }

    @DisplayName("Возможность сохранения одновременно marketSku и shopSku")
    @Test
    @DbUnitDataSet
    void priceUpdateWithMarketShopSku() {
        Map<IndexerOfferKey, ExpirablePapiOfferPrice> prices = new HashMap<>();
        prices.put(marketShopSku(123, 2, "10"), expirablePrice(bigDecimal(10), Duration.ofDays(2)));
        prices.put(marketShopSku(123, 2, "20"), expirablePrice(bigDecimal(20), Duration.ofDays(2)));
        marketSkuOfferService.updatePrices(prices, CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);

        prices.clear();
        prices.put(marketShopSku(123, 2, "10"), expirablePrice(bigDecimal(30), Duration.ofDays(2)));
        prices.put(marketShopSku(123, 2, "20"), expirablePrice(bigDecimal(40), Duration.ofDays(2)));
        marketSkuOfferService.updatePrices(prices, CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);

        marketSkuOfferService.generateDiff(1000);
        marketSkuOfferService.applyButDoNotClearDiff(1000);
        marketSkuOfferService.clearDiff();

        Map<IndexerOfferKey, PapiOfferProperties> offers = new HashMap<>();
        marketSkuOfferService.streamPublishedEntries(
                100,
                offer -> offers.put(offer.feedMarketSku(), offer.properties()));

        Assertions.assertEquals(offers.size(), 2);
        assertThat(offers.get(marketShopSku(123, 2, "10")).priceValue().get())
                .isEqualByComparingTo(bigDecimal(30));
        assertThat(offers.get(marketShopSku(123, 2, "20")).priceValue().get())
                .isEqualByComparingTo(bigDecimal(40));

        Assertions.assertNull(offers.get(feedMsku(123,  2, "offer2")));
        Assertions.assertNull(offers.get(shopSku(123, "20")));
    }

    @Test
    @DbUnitDataSet
    void testPriceation() {
        // Заполняем "текущую таблицу"
        marketSkuOfferService
                .updatePrices(priceUpdate(123, 1, "offer1", bigDecimal(15)), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);
        marketSkuOfferService
                .updatePrices(priceUpdate(123, 2, "offer2", bigDecimal(30)), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);
        marketSkuOfferService
                .updatePrices(priceUpdate(123, 3, "offer3", bigDecimal(45)), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);
        marketSkuOfferService
                .updatePrices(priceUpdate(123, 4, "offer4", bigDecimal(35)), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);
        marketSkuOfferService
                .updatePrices(priceUpdate(123, 5, "offer5", bigDecimal(60)), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);
        marketSkuOfferService
                .updatePrices(priceUpdate(123, 6, "offer6", bigDecimal(30)), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);
        marketSkuOfferService
                .updatePrices(priceUpdate(123, 7, "offer7", bigDecimal(45)), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);


        // Публикуем данные

        marketSkuOfferService.generateDiff(1000);
        marketSkuOfferService.applyButDoNotClearDiff(1000);
        marketSkuOfferService.clearDiff();

        // Проверяем опубликованные данные
        Map<IndexerOfferKey, PapiOfferProperties> offers = new HashMap<>();
        marketSkuOfferService.streamPublishedEntries(
                100,
                offer -> offers.put(offer.feedMarketSku(), offer.properties()));

        Assertions.assertEquals(7, offers.size());
        assertThat(offers.get(feedMsku(123, 1, "offer1")).priceValue().get())
                .isEqualByComparingTo(bigDecimal(15));
        assertThat(offers.get(feedMsku(123, 2, "offer2")).priceValue().get())
                .isEqualByComparingTo(bigDecimal(30));
        assertThat(offers.get(feedMsku(123, 3, "offer3")).priceValue().get())
                .isEqualByComparingTo(bigDecimal(45));
        assertThat(offers.get(feedMsku(123, 4, "offer4")).priceValue().get())
                .isEqualByComparingTo(bigDecimal(35));
        assertThat(offers.get(feedMsku(123, 5, "offer5")).priceValue().get())
                .isEqualByComparingTo(bigDecimal(60));
        assertThat(offers.get(feedMsku(123, 6, "offer6")).priceValue().get())
                .isEqualByComparingTo(bigDecimal(30));
        assertThat(offers.get(feedMsku(123, 7, "offer7")).priceValue().get())
                .isEqualByComparingTo(bigDecimal(45));

        // Делаем пачку изменений в текущей таблице (2 обновления, одно удаление, одно добавление)
        marketSkuOfferService.clearPrices(Collections.singleton(feedMsku(123, 2, "offer2")), Instant.now());
        marketSkuOfferService
                .updatePrices(priceUpdate(123, 3, "offer3", bigDecimal(115)), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);
        marketSkuOfferService
                .updatePrices(priceUpdate(123, 5, "offer5", bigDecimal(55)), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);
        marketSkuOfferService
                .updatePrices(priceUpdate(123, 8, "offer8", bigDecimal(20)), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);

        // Генерируем diff между текущей таблицей и экспортированной таблицей
        marketSkuOfferService.generateDiff(1000);

        Map<IndexerOfferKey, PapiOfferPropertyDiff> diffs = new HashMap<>();
        marketSkuOfferService.streamDiffRecords(100, diff -> {
            diffs.put(diff.indexerOfferKey(), diff.properties());
        });

        Assertions.assertEquals(4, diffs.size());
        Assertions.assertEquals(Optional.empty(), diffs.get(feedMsku(123, 2, "offer2")).offer().priceValue());
        assertThat(diffs.get(feedMsku(123, 3, "offer3")).offer().priceValue().get())
                .isEqualByComparingTo(bigDecimal(115));
        assertThat(diffs.get(feedMsku(123, 5, "offer5")).offer().priceValue().get())
                .isEqualByComparingTo(bigDecimal(55));
        assertThat(diffs.get(feedMsku(123, 8, "offer8")).offer().priceValue().get())
                .isEqualByComparingTo(bigDecimal(20));

        Assertions.assertTrue(diffs.get(feedMsku(123, 2, "offer2")).priceJustChanged());
        Assertions.assertTrue(diffs.get(feedMsku(123, 3, "offer3")).priceJustChanged());
        Assertions.assertTrue(diffs.get(feedMsku(123, 5, "offer5")).priceJustChanged());
        Assertions.assertTrue(diffs.get(feedMsku(123, 8, "offer8")).priceJustChanged());

        // Применяем diff к экспортированной таблице
        marketSkuOfferService.applyButDoNotClearDiff(1000);
        marketSkuOfferService.clearDiff();

        offers.clear();
        marketSkuOfferService.streamPublishedEntries(
                100,
                offer -> offers.put(offer.feedMarketSku(), offer.properties()));

        // Проверяем новое содержимое "экспортированной таблицы"
        Assertions.assertEquals(8, offers.size());
        Assertions.assertEquals(Optional.empty(), offers.get(feedMsku(123, 2, "offer2")).priceValue());
        assertThat(offers.get(feedMsku(123, 1, "offer1")).priceValue().get())
                .isEqualByComparingTo(bigDecimal(15));
        assertThat(offers.get(feedMsku(123, 3, "offer3")).priceValue().get())
                .isEqualByComparingTo(bigDecimal(115));
        assertThat(offers.get(feedMsku(123, 4, "offer4")).priceValue().get())
                .isEqualByComparingTo(bigDecimal(35));
        assertThat(offers.get(feedMsku(123, 5, "offer5")).priceValue().get())
                .isEqualByComparingTo(bigDecimal(55));
        assertThat(offers.get(feedMsku(123, 6, "offer6")).priceValue().get())
                .isEqualByComparingTo(bigDecimal(30));
        assertThat(offers.get(feedMsku(123, 7, "offer7")).priceValue().get())
                .isEqualByComparingTo(bigDecimal(45));
        assertThat(offers.get(feedMsku(123, 8, "offer8")).priceValue().get())
                .isEqualByComparingTo(bigDecimal(20));

        // Проверяем что пустые оферы удаляются
        marketSkuOfferService.removeEmptyOffers(1000);

        offers.clear();
        marketSkuOfferService.streamPublishedEntries(
                100,
                offer -> offers.put(offer.feedMarketSku(), offer.properties()));

        Assertions.assertEquals(7, offers.size());
        assertThat(offers.get(feedMsku(123, 1, "offer1")).priceValue().get())
                .isEqualByComparingTo(bigDecimal(15));
        assertThat(offers.get(feedMsku(123, 3, "offer3")).priceValue().get())
                .isEqualByComparingTo(bigDecimal(115));
        assertThat(offers.get(feedMsku(123, 4, "offer4")).priceValue().get())
                .isEqualByComparingTo(bigDecimal(35));
        assertThat(offers.get(feedMsku(123, 5, "offer5")).priceValue().get())
                .isEqualByComparingTo(bigDecimal(55));
        assertThat(offers.get(feedMsku(123, 6, "offer6")).priceValue().get())
                .isEqualByComparingTo(bigDecimal(30));
        assertThat(offers.get(feedMsku(123, 7, "offer7")).priceValue().get())
                .isEqualByComparingTo(bigDecimal(45));
        assertThat(offers.get(feedMsku(123, 8, "offer8")).priceValue().get())
                .isEqualByComparingTo(bigDecimal(20));
    }

    @Test
    @DbUnitDataSet
    void testHidingation() {
        // Заполняем "текущую таблицу"
        marketSkuOfferService.hideOffers(hidingSku(123, 1, "offer11"), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);
        marketSkuOfferService.hideOffers(hidingSku(123, 2, "offer22"), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);
        marketSkuOfferService.hideOffers(hidingSku(123, 3, "offer33"), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);
        marketSkuOfferService.hideOffers(hidingSku(123, 4, "offer44"), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);
        marketSkuOfferService.hideOffers(hidingSku(123, 5, "offer55"), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);
        marketSkuOfferService.hideOffers(hidingSku(123, 6, "offer66"), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);
        marketSkuOfferService.hideOffers(hidingSku(123, 7, "offer77"), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);
        marketSkuOfferService.hideOffers(hidingShopSku(234, "offer1"), CLIENT_ID, CampaignType.SHOP, DATASOURCE_ID);
        marketSkuOfferService.hideOffers(hidingShopSku(234, "offer2"), CLIENT_ID, CampaignType.SHOP, DATASOURCE_ID);

        // Публикуем данные

        marketSkuOfferService.generateDiff(1000);
        marketSkuOfferService.applyButDoNotClearDiff(1000);
        marketSkuOfferService.clearDiff();

        // Проверяем опубликованные данные
        Map<IndexerOfferKey, PapiOfferProperties> offers = new HashMap<>();
        marketSkuOfferService.streamPublishedEntries(
                100,
                offer -> offers.put(offer.feedMarketSku(), offer.properties()));

        Assertions.assertEquals(9, offers.size());
        Assertions.assertTrue(offers.get(feedMsku(123, 1, "offer11")).isHidden());
        Assertions.assertTrue(offers.get(feedMsku(123, 2, "offer22")).isHidden());
        Assertions.assertTrue(offers.get(feedMsku(123, 3, "offer33")).isHidden());
        Assertions.assertTrue(offers.get(feedMsku(123, 4, "offer44")).isHidden());
        Assertions.assertTrue(offers.get(feedMsku(123, 5, "offer55")).isHidden());
        Assertions.assertTrue(offers.get(feedMsku(123, 6, "offer66")).isHidden());
        Assertions.assertTrue(offers.get(feedMsku(123, 7, "offer77")).isHidden());
        Assertions.assertTrue(offers.get(shopSku(234, "offer1")).isHidden());
        Assertions.assertTrue(offers.get(shopSku(234, "offer2")).isHidden());

        // Делаем пачку изменений
        marketSkuOfferService.showOffers(Collections.singleton(feedMsku(123, 2, "offer22")), Instant.now());
        marketSkuOfferService.showOffers(Collections.singleton(feedMsku(123, 5, "offer55")), Instant.now());
        marketSkuOfferService.showOffers(Collections.singleton(shopSku(234, "offer1")), Instant.now());
        marketSkuOfferService.hideOffers(hidingSku(123, 8, "offer88"), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);
        marketSkuOfferService.hideOffers(hidingSku(123, 9, "offer99"), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);
        marketSkuOfferService.hideOffers(hidingShopSku(234, "offer3"), CLIENT_ID, CampaignType.SHOP, DATASOURCE_ID);

        // Генерируем diff между текущей таблицей и экспортированной таблицей
        marketSkuOfferService.generateDiff(1000);

        Map<IndexerOfferKey, PapiOfferPropertyDiff> diffs = new HashMap<>();
        marketSkuOfferService.streamDiffRecords(100, diff -> {
            diffs.put(diff.indexerOfferKey(), diff.properties());
        });

        Assertions.assertEquals(6, diffs.size());
        Assertions.assertFalse(diffs.get(feedMsku(123, 2, "offer22")).offer().isHidden());
        Assertions.assertFalse(diffs.get(feedMsku(123, 5, "offer55")).offer().isHidden());
        Assertions.assertTrue(diffs.get(feedMsku(123, 8, "offer88")).offer().isHidden());
        Assertions.assertTrue(diffs.get(feedMsku(123, 9, "offer99")).offer().isHidden());
        Assertions.assertFalse(diffs.get(shopSku(234, "offer1")).offer().isHidden());
        Assertions.assertTrue(diffs.get(shopSku(234, "offer3")).offer().isHidden());

        Assertions.assertTrue(diffs.get(feedMsku(123, 2, "offer22")).visibilityJustChanged());
        Assertions.assertTrue(diffs.get(feedMsku(123, 5, "offer55")).visibilityJustChanged());
        Assertions.assertTrue(diffs.get(feedMsku(123, 8, "offer88")).visibilityJustChanged());
        Assertions.assertTrue(diffs.get(feedMsku(123, 9, "offer99")).visibilityJustChanged());
        Assertions.assertTrue(diffs.get(shopSku(234, "offer1")).visibilityJustChanged());
        Assertions.assertTrue(diffs.get(shopSku(234, "offer3")).visibilityJustChanged());

        // Применяем diff к экспортированной таблице
        marketSkuOfferService.applyButDoNotClearDiff(1000);
        marketSkuOfferService.clearDiff();

        offers.clear();
        marketSkuOfferService.streamPublishedEntries(
                100,
                offer -> offers.put(offer.feedMarketSku(), offer.properties()));

        // Проверяем новое содержимое "экспортированной таблицы"
        Assertions.assertEquals(12, offers.size());
        Assertions.assertTrue(offers.get(feedMsku(123, 1, "offer11")).isHidden());
        Assertions.assertFalse(offers.get(feedMsku(123, 2, "offer22")).isHidden());
        Assertions.assertTrue(offers.get(feedMsku(123, 3, "offer33")).isHidden());
        Assertions.assertTrue(offers.get(feedMsku(123, 4, "offer44")).isHidden());
        Assertions.assertFalse(offers.get(feedMsku(123, 5, "offer55")).isHidden());
        Assertions.assertTrue(offers.get(feedMsku(123, 6, "offer66")).isHidden());
        Assertions.assertTrue(offers.get(feedMsku(123, 7, "offer77")).isHidden());
        Assertions.assertTrue(offers.get(feedMsku(123, 8, "offer88")).isHidden());
        Assertions.assertTrue(offers.get(feedMsku(123, 9, "offer99")).isHidden());
        Assertions.assertFalse(offers.get(shopSku(234, "offer1")).isHidden());
        Assertions.assertTrue(offers.get(shopSku(234, "offer2")).isHidden());
        Assertions.assertTrue(offers.get(shopSku(234, "offer3")).isHidden());

        // Проверяем что пустые оферы удаляются
        marketSkuOfferService.removeEmptyOffers(1000);

        offers.clear();
        marketSkuOfferService.streamPublishedEntries(
                100,
                offer -> offers.put(offer.feedMarketSku(), offer.properties()));

        Assertions.assertEquals(9, offers.size());
        Assertions.assertTrue(offers.get(feedMsku(123, 1, "offer11")).isHidden());
        Assertions.assertTrue(offers.get(feedMsku(123, 3, "offer33")).isHidden());
        Assertions.assertTrue(offers.get(feedMsku(123, 4, "offer44")).isHidden());
        Assertions.assertTrue(offers.get(feedMsku(123, 6, "offer66")).isHidden());
        Assertions.assertTrue(offers.get(feedMsku(123, 7, "offer77")).isHidden());
        Assertions.assertTrue(offers.get(feedMsku(123, 8, "offer88")).isHidden());
        Assertions.assertTrue(offers.get(feedMsku(123, 9, "offer99")).isHidden());
        Assertions.assertTrue(offers.get(shopSku(234, "offer2")).isHidden());
        Assertions.assertTrue(offers.get(shopSku(234, "offer3")).isHidden());
    }

    @Test
    @DbUnitDataSet
    void testMixedChanges() {
        marketSkuOfferService
                .updatePrices(priceUpdate(123, 1, "offer1", bigDecimal(15)), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);
        marketSkuOfferService
                .updatePrices(priceUpdate(123, 2, "offer2", bigDecimal(30)), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);
        marketSkuOfferService
                .updatePrices(priceUpdate(123, 3, "offer3", bigDecimal(45)), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);

        marketSkuOfferService.hideOffers(hidingSku(123, 3, "offer3"), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);
        marketSkuOfferService.hideOffers(hidingSku(123, 4, "offer4"), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);
        marketSkuOfferService.hideOffers(hidingSku(123, 5, "offer5"), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);

        marketSkuOfferService.generateDiff(1000);
        marketSkuOfferService.applyButDoNotClearDiff(1000);

        marketSkuOfferService.clearDiff();

        // Проверяем опубликованные данные
        Map<IndexerOfferKey, PapiOfferProperties> offers = new HashMap<>();
        marketSkuOfferService.streamPublishedEntries(
                100,
                offer -> offers.put(offer.feedMarketSku(), offer.properties()));

        Assertions.assertEquals(5, offers.size());

        assertThat(offers.get(feedMsku(123, 1, "offer1")).priceValue().get())
                .isEqualByComparingTo(bigDecimal(15));
        assertThat(offers.get(feedMsku(123, 2, "offer2")).priceValue().get())
                .isEqualByComparingTo(bigDecimal(30));
        assertThat(offers.get(feedMsku(123, 3, "offer3")).priceValue().get())
                .isEqualByComparingTo(bigDecimal(45));
        Assertions.assertEquals(Optional.empty(), offers.get(feedMsku(123, 4, "offer4")).priceValue());
        Assertions.assertEquals(Optional.empty(), offers.get(feedMsku(123, 5, "offer5")).priceValue());

        Assertions.assertFalse(offers.get(feedMsku(123, 1, "offer1")).isHidden());
        Assertions.assertFalse(offers.get(feedMsku(123, 2, "offer2")).isHidden());
        Assertions.assertTrue(offers.get(feedMsku(123, 3, "offer3")).isHidden());
        Assertions.assertTrue(offers.get(feedMsku(123, 4, "offer4")).isHidden());
        Assertions.assertTrue(offers.get(feedMsku(123, 5, "offer5")).isHidden());

        // Делаем пачку изменений
        marketSkuOfferService.hideOffers(hidingSku(123, 1, "offer1"), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);
        marketSkuOfferService.showOffers(Collections.singleton(feedMsku(123, 4, "offer4")), Instant.now());
        marketSkuOfferService.hideOffers(hidingSku(123, 6, "offer6"), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);

        marketSkuOfferService.clearPrices(Collections.singleton(feedMsku(123, 2, "offer2")), Instant.now());
        marketSkuOfferService
                .updatePrices(priceUpdate(123, 3, "offer3", bigDecimal(115)), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);
        marketSkuOfferService
                .updatePrices(priceUpdate(123, 7, "offer7", bigDecimal(20)), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);

        // Генерируем diff между текущей таблицей и экспортированной таблицей
        marketSkuOfferService.generateDiff(1000);

        Map<IndexerOfferKey, PapiOfferPropertyDiff> diffs = new HashMap<>();
        marketSkuOfferService.streamDiffRecords(100, diff -> {
            diffs.put(diff.indexerOfferKey(), diff.properties());
        });

        Assertions.assertEquals(6, diffs.size());

        Assertions.assertTrue(diffs.get(feedMsku(123, 1, "offer1")).offer().isHidden());
        Assertions.assertFalse(diffs.get(feedMsku(123, 2, "offer2")).offer().isHidden());
        Assertions.assertTrue(diffs.get(feedMsku(123, 3, "offer3")).offer().isHidden());
        Assertions.assertFalse(diffs.get(feedMsku(123, 4, "offer4")).offer().isHidden());
        Assertions.assertTrue(diffs.get(feedMsku(123, 6, "offer6")).offer().isHidden());
        Assertions.assertFalse(diffs.get(feedMsku(123, 7, "offer7")).offer().isHidden());

        Assertions.assertTrue(diffs.get(feedMsku(123, 1, "offer1")).visibilityJustChanged());
        Assertions.assertFalse(diffs.get(feedMsku(123, 2, "offer2")).visibilityJustChanged());
        Assertions.assertFalse(diffs.get(feedMsku(123, 3, "offer3")).visibilityJustChanged());
        Assertions.assertTrue(diffs.get(feedMsku(123, 4, "offer4")).visibilityJustChanged());
        Assertions.assertTrue(diffs.get(feedMsku(123, 6, "offer6")).visibilityJustChanged());
        Assertions.assertFalse(diffs.get(feedMsku(123, 7, "offer7")).visibilityJustChanged());

        assertThat(diffs.get(feedMsku(123, 1, "offer1")).offer().priceValue().get())
                .isEqualByComparingTo(bigDecimal(15));
        Assertions.assertEquals(Optional.empty(), diffs.get(feedMsku(123, 2, "offer2")).offer().priceValue());
        assertThat(diffs.get(feedMsku(123, 3, "offer3")).offer().priceValue().get())
                .isEqualByComparingTo(bigDecimal(115));
        Assertions.assertEquals(Optional.empty(), diffs.get(feedMsku(123, 4, "offer4")).offer().priceValue());
        Assertions.assertEquals(Optional.empty(), diffs.get(feedMsku(123, 6, "offer6")).offer().priceValue());
        assertThat(diffs.get(feedMsku(123, 7, "offer7")).offer().priceValue().get())
                .isEqualByComparingTo(bigDecimal(20));

        Assertions.assertFalse(diffs.get(feedMsku(123, 1, "offer1")).priceJustChanged());
        Assertions.assertTrue(diffs.get(feedMsku(123, 2, "offer2")).priceJustChanged());
        Assertions.assertTrue(diffs.get(feedMsku(123, 3, "offer3")).priceJustChanged());
        Assertions.assertFalse(diffs.get(feedMsku(123, 4, "offer4")).priceJustChanged());
        Assertions.assertFalse(diffs.get(feedMsku(123, 6, "offer6")).priceJustChanged());
        Assertions.assertTrue(diffs.get(feedMsku(123, 7, "offer7")).priceJustChanged());

        // Применяем diff к экспортированной таблице
        marketSkuOfferService.applyButDoNotClearDiff(1000);
        marketSkuOfferService.clearDiff();

        offers.clear();
        marketSkuOfferService.streamPublishedEntries(
                100,
                offer -> offers.put(offer.feedMarketSku(), offer.properties()));

        // Проверяем новое содержимое "экспортированной таблицы"
        Assertions.assertEquals(7, offers.size());

        Assertions.assertTrue(offers.get(feedMsku(123, 1, "offer1")).isHidden());
        Assertions.assertFalse(offers.get(feedMsku(123, 2, "offer2")).isHidden());
        Assertions.assertTrue(offers.get(feedMsku(123, 3, "offer3")).isHidden());
        Assertions.assertFalse(offers.get(feedMsku(123, 4, "offer4")).isHidden());
        Assertions.assertTrue(offers.get(feedMsku(123, 5, "offer5")).isHidden());
        Assertions.assertTrue(offers.get(feedMsku(123, 6, "offer6")).isHidden());
        Assertions.assertFalse(offers.get(feedMsku(123, 7, "offer7")).isHidden());

        assertThat(offers.get(feedMsku(123, 1, "offer1")).priceValue().get())
                .isEqualByComparingTo(bigDecimal(15));
        Assertions.assertEquals(Optional.empty(), offers.get(feedMsku(123, 2, "offer2")).priceValue());
        assertThat(diffs.get(feedMsku(123, 3, "offer3")).offer().priceValue().get())
                .isEqualByComparingTo(bigDecimal(115));
        Assertions.assertEquals(Optional.empty(), offers.get(feedMsku(123, 4, "offer4")).priceValue());
        Assertions.assertEquals(Optional.empty(), offers.get(feedMsku(123, 5, "offer5")).priceValue());
        Assertions.assertEquals(Optional.empty(), offers.get(feedMsku(123, 6, "offer6")).priceValue());
        assertThat(diffs.get(feedMsku(123, 7, "offer7")).offer().priceValue().get())
                .isEqualByComparingTo(bigDecimal(20));

        // Проверяем что пустые оферы удаляются
        marketSkuOfferService.removeEmptyOffers(1000);

        offers.clear();
        marketSkuOfferService.streamPublishedEntries(
                100,
                offer -> offers.put(offer.feedMarketSku(), offer.properties()));

        System.out.println(offers);
        Assertions.assertEquals(5, offers.size());

        Assertions.assertTrue(offers.get(feedMsku(123, 1, "offer1")).isHidden());
        Assertions.assertTrue(offers.get(feedMsku(123, 3, "offer3")).isHidden());
        Assertions.assertTrue(offers.get(feedMsku(123, 5, "offer5")).isHidden());
        Assertions.assertTrue(offers.get(feedMsku(123, 6, "offer6")).isHidden());
        Assertions.assertFalse(offers.get(feedMsku(123, 7, "offer7")).isHidden());

        assertThat(offers.get(feedMsku(123, 1, "offer1")).priceValue().get())
                .isEqualByComparingTo(bigDecimal(15));
        assertThat(diffs.get(feedMsku(123, 3, "offer3")).offer().priceValue().get())
                .isEqualByComparingTo(bigDecimal(115));
        Assertions.assertEquals(Optional.empty(), offers.get(feedMsku(123, 5, "offer5")).priceValue());
        Assertions.assertEquals(Optional.empty(), offers.get(feedMsku(123, 6, "offer6")).priceValue());
        assertThat(diffs.get(feedMsku(123, 7, "offer7")).offer().priceValue().get())
                .isEqualByComparingTo(bigDecimal(20));
    }

    @Test
    @DbUnitDataSet(before = "PapiMarketSkuOfferServiceTest.testFeedToLinkMapping.csv")
    void testFeedToLinkMapping() {
        marketSkuOfferService
                .updatePrices(priceUpdate(123, 1, "offer1", bigDecimal(15)), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);
        marketSkuOfferService
                .updatePrices(priceUpdate(123, 2, "offer2", bigDecimal(30)), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);
        marketSkuOfferService
                .updatePrices(priceUpdate(123, 3, "offer3", bigDecimal(45)), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);

        marketSkuOfferService.hideOffers(hidingSku(123, 3, "offer3"), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);
        marketSkuOfferService.hideOffers(hidingSku(123, 4, "offer4"), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);
        marketSkuOfferService.hideOffers(hidingSku(123, 5, "offer5"), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);

        marketSkuOfferService.generateDiff(1000);
        marketSkuOfferService.applyButDoNotClearDiff(1000);

        marketSkuOfferService.clearDiff();

        // Проверяем опубликованные данные
        Map<IndexerOfferKey, PapiOfferProperties> offers = new HashMap<>();
        marketSkuOfferService.streamPublishedEntries(
                100,
                offer -> offers.put(offer.feedMarketSku(), offer.properties()));

        Assertions.assertEquals(10, offers.size());

        assertThat(offers.get(feedMsku(124, 1, "offer1")).priceValue().get())
                .isEqualByComparingTo(bigDecimal(15));
        assertThat(offers.get(feedMsku(125, 1, "offer1")).priceValue().get())
                .isEqualByComparingTo(bigDecimal(15));
        assertThat(offers.get(feedMsku(124, 2, "offer2")).priceValue().get())
                .isEqualByComparingTo(bigDecimal(30));
        assertThat(offers.get(feedMsku(125, 2, "offer2")).priceValue().get())
                .isEqualByComparingTo(bigDecimal(30));
        assertThat(offers.get(feedMsku(124, 3, "offer3")).priceValue().get())
                .isEqualByComparingTo(bigDecimal(45));
        assertThat(offers.get(feedMsku(125, 3, "offer3")).priceValue().get())
                .isEqualByComparingTo(bigDecimal(45));
        Assertions.assertEquals(Optional.empty(), offers.get(feedMsku(124, 4, "offer4")).priceValue());
        Assertions.assertEquals(Optional.empty(), offers.get(feedMsku(125, 4, "offer4")).priceValue());
        Assertions.assertEquals(Optional.empty(), offers.get(feedMsku(124, 5, "offer5")).priceValue());
        Assertions.assertEquals(Optional.empty(), offers.get(feedMsku(125, 5, "offer5")).priceValue());

        Assertions.assertFalse(offers.get(feedMsku(124, 1, "offer1")).isHidden());
        Assertions.assertFalse(offers.get(feedMsku(125, 1, "offer1")).isHidden());
        Assertions.assertFalse(offers.get(feedMsku(124, 2, "offer2")).isHidden());
        Assertions.assertFalse(offers.get(feedMsku(125, 2, "offer2")).isHidden());
        Assertions.assertTrue(offers.get(feedMsku(124, 3, "offer3")).isHidden());
        Assertions.assertTrue(offers.get(feedMsku(125, 3, "offer3")).isHidden());
        Assertions.assertTrue(offers.get(feedMsku(124, 4, "offer4")).isHidden());
        Assertions.assertTrue(offers.get(feedMsku(125, 4, "offer4")).isHidden());
        Assertions.assertTrue(offers.get(feedMsku(124, 5, "offer5")).isHidden());
        Assertions.assertTrue(offers.get(feedMsku(125, 5, "offer5")).isHidden());

        // Делаем пачку изменений
        marketSkuOfferService.hideOffers(hidingSku(123, 1, "offer1"), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);
        marketSkuOfferService.showOffers(Collections.singleton(feedMsku(123, 4, "offer4")), Instant.now());
        marketSkuOfferService.hideOffers(hidingSku(123, 6, "offer6"), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);

        marketSkuOfferService.clearPrices(Collections.singleton(feedMsku(123, 2, "offer2")), Instant.now());
        marketSkuOfferService
                .updatePrices(priceUpdate(123, 3, "offer3", bigDecimal(115)), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);
        marketSkuOfferService
                .updatePrices(priceUpdate(123, 7, "offer7", bigDecimal(20)), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);

        // Генерируем diff между текущей таблицей и экспортированной таблицей
        marketSkuOfferService.generateDiff(1000);

        Map<IndexerOfferKey, PapiOfferPropertyDiff> diffs = new HashMap<>();
        marketSkuOfferService.streamDiffRecords(100, diff -> {
            diffs.put(diff.indexerOfferKey(), diff.properties());
        });

        Assertions.assertEquals(12, diffs.size());

        Assertions.assertTrue(diffs.get(feedMsku(124, 1, "offer1")).offer().isHidden());
        Assertions.assertTrue(diffs.get(feedMsku(125, 1, "offer1")).offer().isHidden());
        Assertions.assertFalse(diffs.get(feedMsku(124, 2, "offer2")).offer().isHidden());
        Assertions.assertFalse(diffs.get(feedMsku(125, 2, "offer2")).offer().isHidden());
        Assertions.assertTrue(diffs.get(feedMsku(124, 3, "offer3")).offer().isHidden());
        Assertions.assertTrue(diffs.get(feedMsku(125, 3, "offer3")).offer().isHidden());
        Assertions.assertFalse(diffs.get(feedMsku(124, 4, "offer4")).offer().isHidden());
        Assertions.assertFalse(diffs.get(feedMsku(125, 4, "offer4")).offer().isHidden());
        Assertions.assertTrue(diffs.get(feedMsku(124, 6, "offer6")).offer().isHidden());
        Assertions.assertTrue(diffs.get(feedMsku(125, 6, "offer6")).offer().isHidden());
        Assertions.assertFalse(diffs.get(feedMsku(124, 7, "offer7")).offer().isHidden());
        Assertions.assertFalse(diffs.get(feedMsku(125, 7, "offer7")).offer().isHidden());

        Assertions.assertTrue(diffs.get(feedMsku(124, 1, "offer1")).visibilityJustChanged());
        Assertions.assertTrue(diffs.get(feedMsku(125, 1, "offer1")).visibilityJustChanged());
        Assertions.assertFalse(diffs.get(feedMsku(124, 2, "offer2")).visibilityJustChanged());
        Assertions.assertFalse(diffs.get(feedMsku(125, 2, "offer2")).visibilityJustChanged());
        Assertions.assertFalse(diffs.get(feedMsku(124, 3, "offer3")).visibilityJustChanged());
        Assertions.assertFalse(diffs.get(feedMsku(125, 3, "offer3")).visibilityJustChanged());
        Assertions.assertTrue(diffs.get(feedMsku(124, 4, "offer4")).visibilityJustChanged());
        Assertions.assertTrue(diffs.get(feedMsku(125, 4, "offer4")).visibilityJustChanged());
        Assertions.assertTrue(diffs.get(feedMsku(124, 6, "offer6")).visibilityJustChanged());
        Assertions.assertTrue(diffs.get(feedMsku(125, 6, "offer6")).visibilityJustChanged());
        Assertions.assertFalse(diffs.get(feedMsku(124, 7, "offer7")).visibilityJustChanged());
        Assertions.assertFalse(diffs.get(feedMsku(125, 7, "offer7")).visibilityJustChanged());

        assertThat(diffs.get(feedMsku(124, 1, "offer1")).offer().priceValue().get())
                .isEqualByComparingTo(bigDecimal(15));
        assertThat(diffs.get(feedMsku(125, 1, "offer1")).offer().priceValue().get())
                .isEqualByComparingTo(bigDecimal(15));
        Assertions.assertEquals(Optional.empty(), diffs.get(feedMsku(124, 2, "offer2")).offer().priceValue());
        Assertions.assertEquals(Optional.empty(), diffs.get(feedMsku(125, 2, "offer2")).offer().priceValue());
        assertThat(diffs.get(feedMsku(124, 3, "offer3")).offer().priceValue().get())
                .isEqualByComparingTo(bigDecimal(115));
        assertThat(diffs.get(feedMsku(125, 3, "offer3")).offer().priceValue().get())
                .isEqualByComparingTo(bigDecimal(115));
        Assertions.assertEquals(Optional.empty(), diffs.get(feedMsku(124, 4, "offer4")).offer().priceValue());
        Assertions.assertEquals(Optional.empty(), diffs.get(feedMsku(125, 4, "offer4")).offer().priceValue());
        Assertions.assertEquals(Optional.empty(), diffs.get(feedMsku(124, 6, "offer6")).offer().priceValue());
        Assertions.assertEquals(Optional.empty(), diffs.get(feedMsku(125, 6, "offer6")).offer().priceValue());
        assertThat(diffs.get(feedMsku(124, 7, "offer7")).offer().priceValue().get())
                .isEqualByComparingTo(bigDecimal(20));
        assertThat(diffs.get(feedMsku(125, 7, "offer7")).offer().priceValue().get())
                .isEqualByComparingTo(bigDecimal(20));

        Assertions.assertFalse(diffs.get(feedMsku(124, 1, "offer1")).priceJustChanged());
        Assertions.assertFalse(diffs.get(feedMsku(125, 1, "offer1")).priceJustChanged());
        Assertions.assertTrue(diffs.get(feedMsku(124, 2, "offer2")).priceJustChanged());
        Assertions.assertTrue(diffs.get(feedMsku(125, 2, "offer2")).priceJustChanged());
        Assertions.assertTrue(diffs.get(feedMsku(124, 3, "offer3")).priceJustChanged());
        Assertions.assertTrue(diffs.get(feedMsku(125, 3, "offer3")).priceJustChanged());
        Assertions.assertFalse(diffs.get(feedMsku(124, 4, "offer4")).priceJustChanged());
        Assertions.assertFalse(diffs.get(feedMsku(125, 4, "offer4")).priceJustChanged());
        Assertions.assertFalse(diffs.get(feedMsku(124, 6, "offer6")).priceJustChanged());
        Assertions.assertFalse(diffs.get(feedMsku(125, 6, "offer6")).priceJustChanged());
        Assertions.assertTrue(diffs.get(feedMsku(124, 7, "offer7")).priceJustChanged());
        Assertions.assertTrue(diffs.get(feedMsku(125, 7, "offer7")).priceJustChanged());
    }

    /**
     * Проверяем, что метод {@link PapiMarketSkuOfferService#isDiffEmpty()}.
     * <p>
     * Смотрим, что результат метода полностью определяется тем были ли изменения с момента последней публикации.
     */
    @Test
    @DbUnitDataSet
    void testEmptyDiff() {
        marketSkuOfferService
                .updatePrices(priceUpdate(123, 1, "offer1", bigDecimal(15)), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);
        marketSkuOfferService
                .updatePrices(priceUpdate(123, 2, "offer2", bigDecimal(30)), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);
        marketSkuOfferService
                .updatePrices(priceUpdate(123, 3, "offer3", bigDecimal(45)), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);

        marketSkuOfferService.generateDiff(1000);

        Assertions.assertFalse(marketSkuOfferService.isDiffEmpty());

        marketSkuOfferService.applyButDoNotClearDiff(1000);

        Assertions.assertFalse(marketSkuOfferService.isDiffEmpty());

        marketSkuOfferService.clearDiff();

        Assertions.assertTrue(marketSkuOfferService.isDiffEmpty());

        marketSkuOfferService.generateDiff(1000);

        Assertions.assertTrue(marketSkuOfferService.isDiffEmpty());
    }

    /**
     * Проверяем, постраничное чтение данных о ценах.
     */
    @Test
    @DbUnitDataSet
    void testBrowsingPrices() {
        marketSkuOfferService
                .updatePrices(priceUpdate(123, 1, "offer1", bigDecimal(15)), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);
        marketSkuOfferService
                .updatePrices(priceUpdate(123, 2, "offer2", bigDecimal(30)), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);
        marketSkuOfferService
                .updatePrices(priceUpdate(123, 3, "offer3", bigDecimal(45)), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);
        marketSkuOfferService
                .updatePrices(priceUpdate(123, 4, "offer4", bigDecimal(46)), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);
        marketSkuOfferService
                .updatePrices(priceUpdate(123, 5, "offer5", bigDecimal(47)), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);
        marketSkuOfferService
                .updatePrices(priceUpdate(124, 1, "offer1", bigDecimal(115)), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);
        marketSkuOfferService
                .updatePrices(priceUpdate(124, 2, "offer2", bigDecimal(130)), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);
        marketSkuOfferService
                .updatePrices(priceUpdate(124, 3, "offer3", bigDecimal(145)), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);
        marketSkuOfferService
                .updatePrices(priceUpdate(124, 4, "offer4", bigDecimal(146)), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);
        marketSkuOfferService
                .updatePrices(priceUpdate(124, 5, "offer5", bigDecimal(147)), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);

        Assertions.assertEquals(5, marketSkuOfferService.countOffersWithPriceSet(Collections.singleton(123L)));
        Assertions.assertEquals(5, marketSkuOfferService.countOffersWithPriceSet(Collections.singleton(124L)));
        List<FeedMarketSkuPapiPriceUpdate> prices =
                marketSkuOfferService.priceEntries(Collections.singleton(123L), Paging.firstN(3));

        Assertions.assertEquals(3, prices.size());
        Assertions.assertEquals(feedMsku(123, 1, "offer1"), prices.get(0).feedMarketSku());
        Assertions.assertEquals(CLIENT_ID, prices.get(0).details().clientId());
        assertThat(prices.get(0).details().price().value()).isEqualByComparingTo(bigDecimal(15));

        Assertions.assertEquals(feedMsku(123, 2, "offer2"), prices.get(1).feedMarketSku());
        Assertions.assertEquals(CLIENT_ID, prices.get(1).details().clientId());
        assertThat(prices.get(1).details().price().value()).isEqualByComparingTo(bigDecimal(30));

        Assertions.assertEquals(feedMsku(123, 3, "offer3"), prices.get(2).feedMarketSku());
        Assertions.assertEquals(CLIENT_ID, prices.get(2).details().clientId());
        assertThat(prices.get(2).details().price().value()).isEqualByComparingTo(bigDecimal(45));

        prices = marketSkuOfferService.priceEntries(Collections.singleton(123L), Paging.firstN(3).skip(3));

        Assertions.assertEquals(2, prices.size());
        Assertions.assertEquals(feedMsku(123, 4, "offer4"), prices.get(0).feedMarketSku());
        Assertions.assertEquals(CLIENT_ID, prices.get(0).details().clientId());
        assertThat(prices.get(0).details().price().value()).isEqualByComparingTo(bigDecimal(46));

        Assertions.assertEquals(feedMsku(123, 5, "offer5"), prices.get(1).feedMarketSku());
        Assertions.assertEquals(CLIENT_ID, prices.get(1).details().clientId());
        assertThat(prices.get(1).details().price().value()).isEqualByComparingTo(bigDecimal(47));

        marketSkuOfferService.clearPricesByFeedIds(Collections.singleton(123L), Instant.now());
        marketSkuOfferService.removeExpiredPrices(1000, Instant.now().plusSeconds(1));

        Assertions.assertEquals(Optional.empty(),
                marketSkuOfferService.getPrice(feedMsku(123, 1, "offer1")).map(entry -> entry.details().price().value()));
        Assertions.assertEquals(Optional.empty(),
                marketSkuOfferService.getPrice(feedMsku(123, 3, "offer3")).map(entry -> entry.details().price().value()));
        Assertions.assertEquals(Optional.empty(),
                marketSkuOfferService.getPrice(feedMsku(123, 6, "offer6")).map(entry -> entry.details().price().value()));
        assertThat(marketSkuOfferService.getPrice(feedMsku(124, 1, "offer1"))
                    .map(entry -> entry.details().price().value()).get())
                .isEqualByComparingTo(bigDecimal(115));
        assertThat(marketSkuOfferService.getPrice(feedMsku(124, 5, "offer5"))
                    .map(entry -> entry.details().price().value()).get())
                .isEqualByComparingTo(bigDecimal(147));
        Assertions.assertEquals(Optional.empty(),
                marketSkuOfferService.getPrice(feedMsku(124, 7, "offer7")).map(entry -> entry.details().price().value()));

        Assertions.assertEquals(0, marketSkuOfferService.countOffersWithPriceSet(Collections.singleton(123L)));
        Assertions.assertEquals(5, marketSkuOfferService.countOffersWithPriceSet(Collections.singleton(124L)));
        prices = marketSkuOfferService.priceEntries(Collections.singleton(123L), Paging.firstN(3));
        Assertions.assertEquals(0, prices.size());
        prices = marketSkuOfferService.priceEntries(Collections.singleton(124L), Paging.firstN(3));
        Assertions.assertEquals(3, prices.size());
    }

    /**
     * Проверяем, постраничное чтение данных о скрытиях.
     */
    @Test
    @DbUnitDataSet
    void testBrowsingHidings() {
        marketSkuOfferService.hideOffers(hidingSku(123, 1, "offer1"), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);
        marketSkuOfferService.hideOffers(hidingSku(123, 2, "offer2"), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);
        marketSkuOfferService.hideOffers(hidingSku(123, 3, "offer3"), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);
        marketSkuOfferService.hideOffers(hidingSku(123, 4, "offer4"), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);
        marketSkuOfferService.hideOffers(hidingSku(123, 5, "offer5"), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);
        marketSkuOfferService.hideOffers(hidingSku(124, 1, "offer1"), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);
        marketSkuOfferService.hideOffers(hidingSku(124, 2, "offer2"), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);
        marketSkuOfferService.hideOffers(hidingSku(124, 3, "offer3"), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);
        marketSkuOfferService.hideOffers(hidingSku(124, 4, "offer4"), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);
        marketSkuOfferService.hideOffers(hidingSku(124, 5, "offer5"), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);

        Assertions.assertEquals(5, marketSkuOfferService.countHiddenOffers(Collections.singleton(123L)));
        Assertions.assertEquals(5, marketSkuOfferService.countHiddenOffers(Collections.singleton(124L)));
        List<PapiFeedMarketSkuHiding> hidings =
                marketSkuOfferService.hiddenOffersEntries(Collections.singleton(123L), Paging.firstN(3));

        Assertions.assertEquals(3, hidings.size());
        Assertions.assertEquals(feedMsku(123, 1, "offer1"), hidings.get(0).feedMarketSku());
        Assertions.assertEquals(CLIENT_ID, hidings.get(0).details().clientId());

        Assertions.assertEquals(feedMsku(123, 2, "offer2"), hidings.get(1).feedMarketSku());
        Assertions.assertEquals(CLIENT_ID, hidings.get(1).details().clientId());

        Assertions.assertEquals(feedMsku(123, 3, "offer3"), hidings.get(2).feedMarketSku());
        Assertions.assertEquals(CLIENT_ID, hidings.get(2).details().clientId());

        hidings = marketSkuOfferService.hiddenOffersEntries(Collections.singleton(123L), Paging.firstN(3).skip(3));

        Assertions.assertEquals(2, hidings.size());
        Assertions.assertEquals(feedMsku(123, 4, "offer4"), hidings.get(0).feedMarketSku());
        Assertions.assertEquals(CLIENT_ID, hidings.get(0).details().clientId());

        Assertions.assertEquals(feedMsku(123, 5, "offer5"), hidings.get(1).feedMarketSku());
        Assertions.assertEquals(CLIENT_ID, hidings.get(1).details().clientId());

        marketSkuOfferService.showOffersByFeedIds(Collections.singleton(123L), Instant.now());

        Assertions.assertFalse(marketSkuOfferService.getHiding(new IndexerOfferKey(123, 1, "offer1")).isPresent());
        Assertions.assertFalse(marketSkuOfferService.getHiding(new IndexerOfferKey(123, 4, "offer4")).isPresent());
        Assertions.assertFalse(marketSkuOfferService.getHiding(new IndexerOfferKey(123, 6, "offer6")).isPresent());
        Assertions.assertTrue(marketSkuOfferService.getHiding(new IndexerOfferKey(124, 1, "offer1")).isPresent());
        Assertions.assertTrue(marketSkuOfferService.getHiding(new IndexerOfferKey(124, 5, "offer5")).isPresent());
        Assertions.assertFalse(marketSkuOfferService.getHiding(new IndexerOfferKey(124, 7, "offer7")).isPresent());

        Assertions.assertEquals(0, marketSkuOfferService.countHiddenOffers(Collections.singleton(123L)));
        Assertions.assertEquals(5, marketSkuOfferService.countHiddenOffers(Collections.singleton(124L)));
        hidings = marketSkuOfferService.hiddenOffersEntries(Collections.singleton(123L), Paging.firstN(3));
        Assertions.assertEquals(0, hidings.size());
        hidings = marketSkuOfferService.hiddenOffersEntries(Collections.singleton(124L), Paging.firstN(3));
        Assertions.assertEquals(3, hidings.size());

    }

    /**
     * Проверяем, методы подсчитывающие количество цен/скрытий.
     */
    @Test
    @DbUnitDataSet
    void testPricesAndHidingsCounts() {
        marketSkuOfferService
                .updatePrices(priceUpdate(123, 1, "offer1", bigDecimal(15)), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);
        marketSkuOfferService
                .updatePrices(priceUpdate(123, 2, "offer2", bigDecimal(30)), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);
        marketSkuOfferService
                .updatePrices(priceUpdate(123, 3, "offer3", bigDecimal(45)), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);

        marketSkuOfferService
                .updatePrices(priceUpdate(124, 1, "offer1", bigDecimal(15)), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);
        marketSkuOfferService
                .updatePrices(priceUpdate(124, 2, "offer2", bigDecimal(30)), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);
        marketSkuOfferService
                .updatePrices(priceUpdate(124, 3, "offer3", bigDecimal(45)), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);

        marketSkuOfferService.hideOffers(hidingSku(123, 3, "offer3"), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);
        marketSkuOfferService.hideOffers(hidingSku(123, 4, "offer4"), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);
        marketSkuOfferService.hideOffers(hidingSku(123, 5, "offer5"), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);

        marketSkuOfferService.hideOffers(hidingSku(124, 3, "offer3"), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);
        marketSkuOfferService.hideOffers(hidingSku(124, 4, "offer4"), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);
        marketSkuOfferService.hideOffers(hidingSku(124, 5, "offer5"), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);

        Assertions.assertEquals(10, marketSkuOfferService.countAll());

        Assertions.assertEquals(3, marketSkuOfferService.countOffersWithPriceSet(Collections.singleton(123L)));
        Assertions.assertEquals(3, marketSkuOfferService.countHiddenOffers(Collections.singleton(123L)));
        Assertions.assertEquals(3, marketSkuOfferService.countOffersWithPriceSet(Collections.singleton(124L)));
        Assertions.assertEquals(3, marketSkuOfferService.countHiddenOffers(Collections.singleton(124L)));

        marketSkuOfferService.clearPricesByFeedIds(Collections.singleton(123L), Instant.now());
        marketSkuOfferService.removeExpiredPrices(1000, Instant.now().plusSeconds(1));

        Assertions.assertEquals(8, marketSkuOfferService.countAll());

        Assertions.assertEquals(0, marketSkuOfferService.countOffersWithPriceSet(Collections.singleton(123L)));
        Assertions.assertEquals(3, marketSkuOfferService.countHiddenOffers(Collections.singleton(123L)));
        Assertions.assertEquals(3, marketSkuOfferService.countOffersWithPriceSet(Collections.singleton(124L)));
        Assertions.assertEquals(3, marketSkuOfferService.countHiddenOffers(Collections.singleton(124L)));

        marketSkuOfferService.showOffersByFeedIds(Collections.singleton(124L), Instant.now());

        Assertions.assertEquals(8, marketSkuOfferService.countAll());

        Assertions.assertEquals(0, marketSkuOfferService.countOffersWithPriceSet(Collections.singleton(123L)));
        Assertions.assertEquals(3, marketSkuOfferService.countHiddenOffers(Collections.singleton(123L)));
        Assertions.assertEquals(3, marketSkuOfferService.countOffersWithPriceSet(Collections.singleton(124L)));
        Assertions.assertEquals(0, marketSkuOfferService.countHiddenOffers(Collections.singleton(124L)));

        marketSkuOfferService.generateDiff(1000);
        marketSkuOfferService.applyButDoNotClearDiff(1000);
        marketSkuOfferService.clearDiff();
        marketSkuOfferService.removeEmptyOffers(1000);

        Assertions.assertEquals(6, marketSkuOfferService.countAll());

        Assertions.assertEquals(0, marketSkuOfferService.countOffersWithPriceSet(Collections.singleton(123L)));
        Assertions.assertEquals(3, marketSkuOfferService.countHiddenOffers(Collections.singleton(123L)));
        Assertions.assertEquals(3, marketSkuOfferService.countOffersWithPriceSet(Collections.singleton(124L)));
        Assertions.assertEquals(0, marketSkuOfferService.countHiddenOffers(Collections.singleton(124L)));

    }

    /**
     * Проверяем, что цены и скрытия удаляются по истечении времени жизни и не удалятся в противном случае.
     */
    @Test
    @DbUnitDataSet
    void testPricesAndHidingsExpiration() {
        Duration duration = Duration.ofDays(2);
        marketSkuOfferService
                .updatePrices(priceUpdate(123, 1, "offer1", bigDecimal(15), duration), CLIENT_ID, CampaignType.SUPPLIER,
                        DATASOURCE_ID);
        marketSkuOfferService
                .updatePrices(priceUpdate(123, 2, "offer2", bigDecimal(30), duration.negated()), CLIENT_ID, CampaignType.SUPPLIER,
                        DATASOURCE_ID);
        marketSkuOfferService
                .updatePrices(priceUpdate(123, 3, "offer3", bigDecimal(45), duration), CLIENT_ID, CampaignType.SUPPLIER,
                        DATASOURCE_ID);
        marketSkuOfferService
                .updatePrices(priceUpdate(123, 4, "offer4", bigDecimal(45), duration.negated()), CLIENT_ID, CampaignType.SUPPLIER,
                        DATASOURCE_ID);
        marketSkuOfferService
                .updatePrices(priceUpdate(123, 5, "offer5", bigDecimal(45), duration.negated()), CLIENT_ID, CampaignType.SUPPLIER,
                        DATASOURCE_ID);
        marketSkuOfferService
                .updatePrices(priceUpdate(123, 6, "offer6", bigDecimal(45), duration), CLIENT_ID, CampaignType.SUPPLIER,
                        DATASOURCE_ID);

        marketSkuOfferService.hideOffers(hiding(feedMsku(123, 3, "offer3"), duration.negated()), CLIENT_ID, CampaignType.SUPPLIER,
                DATASOURCE_ID);
        marketSkuOfferService
                .hideOffers(hiding(feedMsku(123, 4, "offer4"), duration), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);
        marketSkuOfferService.hideOffers(hiding(feedMsku(123, 5, "offer5"), duration.negated()), CLIENT_ID, CampaignType.SUPPLIER,
                DATASOURCE_ID);
        marketSkuOfferService
                .hideOffers(hiding(feedMsku(123, 6, "offer6"), duration), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);
        marketSkuOfferService.hideOffers(hiding(feedMsku(123, 7, "offer7"), duration.negated()), CLIENT_ID, CampaignType.SUPPLIER,
                DATASOURCE_ID);
        marketSkuOfferService
                .hideOffers(hiding(feedMsku(123, 8, "offer8"), duration), CLIENT_ID, CampaignType.SUPPLIER, DATASOURCE_ID);

        marketSkuOfferService.generateDiff(1000);
        marketSkuOfferService.applyButDoNotClearDiff(1000);
        marketSkuOfferService.clearDiff();

        Assertions.assertEquals(6, marketSkuOfferService.countOffersWithPriceSet(Collections.singleton(123L)));
        Assertions.assertEquals(
                ImmutableSet.of(feedMsku(123, 1, "offer1"),
                        feedMsku(123, 2, "offer2"),
                        feedMsku(123, 3, "offer3"),
                        feedMsku(123, 4, "offer4"),
                        feedMsku(123, 5, "offer5"),
                        feedMsku(123, 6, "offer6")),
                marketSkuOfferService.priceEntries(Collections.singleton(123L), Paging.firstN(1000))
                        .stream()
                        .map(FeedMarketSkuPapiPriceUpdate::feedMarketSku)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(6, marketSkuOfferService.countHiddenOffers(Collections.singleton(123L)));
        Assertions.assertEquals(
                ImmutableSet.of(feedMsku(123, 3, "offer3"),
                        feedMsku(123, 4, "offer4"),
                        feedMsku(123, 5, "offer5"),
                        feedMsku(123, 6, "offer6"),
                        feedMsku(123, 7, "offer7"),
                        feedMsku(123, 8, "offer8")),
                marketSkuOfferService.hiddenOffersEntries(Collections.singleton(123L), Paging.firstN(1000))
                        .stream()
                        .map(PapiFeedMarketSkuHiding::feedMarketSku)
                        .collect(Collectors.toSet()));

        marketSkuOfferService.removeExpiredPrices(1000, Instant.now().plusSeconds(1));
        marketSkuOfferService.removeExpiredHidings(1000, Instant.now().plusSeconds(1));
        marketSkuOfferService.removeEmptyOffers(1000);

        Assertions.assertEquals(3, marketSkuOfferService.countOffersWithPriceSet(Collections.singleton(123L)));
        Assertions.assertEquals(
                ImmutableSet.of(feedMsku(123, 1, "offer1"),
                        feedMsku(123, 3, "offer3"),
                        feedMsku(123, 6, "offer6")),
                marketSkuOfferService.priceEntries(Collections.singleton(123L), Paging.firstN(1000))
                        .stream()
                        .map(FeedMarketSkuPapiPriceUpdate::feedMarketSku)
                        .collect(Collectors.toSet()));

        Assertions.assertEquals(3, marketSkuOfferService.countHiddenOffers(Collections.singleton(123L)));
        Assertions.assertEquals(
                ImmutableSet.of(feedMsku(123, 4, "offer4"),
                        feedMsku(123, 6, "offer6"),
                        feedMsku(123, 8, "offer8")),
                marketSkuOfferService.hiddenOffersEntries(Collections.singleton(123L), Paging.firstN(1000))
                        .stream()
                        .map(PapiFeedMarketSkuHiding::feedMarketSku)
                        .collect(Collectors.toSet()));
    }

    /**
     * Смотрим, комментарии о скрытиях не теряются.
     */
    @Test
    @DbUnitDataSet
    void passingEditionalDataThroughHiding() {
        Instant now = Instant.now();
        PapiHidingEvent hiding = new PapiHidingEvent.Builder()
                .setHiddenAt(now)
                .setHidingExpiresAt(now.plus(Duration.ofDays(7)))
                .setComment("Test321")
                .build();
        marketSkuOfferService
                .hideOffers(Collections.singletonMap(feedMsku(234, 567, "offer567"), hiding), 890, CampaignType.SUPPLIER,
                        DATASOURCE_ID);
        List<PapiFeedMarketSkuHiding> hidings =
                marketSkuOfferService.hiddenOffersEntries(Collections.singleton(234L), Paging.firstN(100));
        Assertions.assertEquals(1, hidings.size());
        PapiFeedMarketSkuHiding persistedHiding = hidings.get(0);
        Assertions.assertEquals(feedMsku(234, 567, "offer567"), persistedHiding.feedMarketSku());
        Assertions.assertEquals(890L, persistedHiding.details().clientId());
        Assertions.assertEquals(Optional.of("Test321"), persistedHiding.details().comment());
        Assertions.assertEquals(now, persistedHiding.details().hiddenAt());
        Assertions.assertEquals(
                now.plus(Duration.ofDays(7)),
                persistedHiding.details().hidingExpiresAt());
    }

    /**
     * Смотрим, что и даные об НДС и данные о размере скидки не теряются (а значит в конце концов попадут в инексатор).
     */
    @Test
    @DbUnitDataSet
    void passingEditionalDataThroughPrices() {
        Instant now = Instant.now();
        PapiOfferPriceValue price = new PapiOfferPriceValue.Builder()
                .setCurrency(Currency.USD)
                .setValue(BigDecimal.valueOf(1234, 2))
                .setDiscountBase(BigDecimal.valueOf(1256, 2))
                .setUpdatedAt(now)
                .setVat(VatRate.VAT_10_110)
                .build();
        ExpirablePapiOfferPrice expirable = price.expirable(now.plus(Duration.ofDays(7)));
        marketSkuOfferService
                .updatePrices(Collections.singletonMap(feedMsku(234, 567, "offer567"), expirable), 890, CampaignType.SUPPLIER,
                        DATASOURCE_ID);
        List<FeedMarketSkuPapiPriceUpdate> prices =
                marketSkuOfferService.priceEntries(Collections.singleton(234L), Paging.firstN(100));
        Assertions.assertEquals(1, prices.size());
        FeedMarketSkuPapiPriceUpdate persistedPrice = prices.get(0);
        Assertions.assertEquals(feedMsku(234, 567, "offer567"), persistedPrice.feedMarketSku());
        Assertions.assertEquals(890L, persistedPrice.details().clientId());
        Assertions.assertEquals(now, persistedPrice.details().updatedAt());
        Assertions.assertEquals(now.plus(Duration.ofDays(7)), persistedPrice.details().expiresAt());
        assertThat(persistedPrice.details().price().value()).isEqualByComparingTo(BigDecimal.valueOf(1234, 2));
        assertThat(persistedPrice.details().price().discountBase().get())
                .isEqualByComparingTo(BigDecimal.valueOf(1256, 2));
        Assertions.assertEquals(Optional.of(VatRate.VAT_10_110), persistedPrice.details().price().vat());

        marketSkuOfferService.generateDiff(1000);
        marketSkuOfferService.applyButDoNotClearDiff(1000);

        List<PapiFeedMarketSkuOfferDiff> diffs = new ArrayList<>();
        marketSkuOfferService.streamDiffRecords(100, diffs::add);

        Assertions.assertEquals(1, diffs.size());
        PapiFeedMarketSkuOfferDiff offerDiff = diffs.get(0);
        Assertions.assertEquals(feedMsku(234, 567, "offer567"), offerDiff.indexerOfferKey());

        Assertions.assertTrue(offerDiff.properties().priceJustChanged());
        Assertions.assertTrue(offerDiff.properties().price().isPresent());

        PapiOfferPriceValue publishingPrice = offerDiff.properties().price().get();

        Assertions.assertEquals(now, publishingPrice.updatedAt());
        assertThat(publishingPrice.value()).isEqualByComparingTo(BigDecimal.valueOf(1234, 2));
        assertThat(publishingPrice.discountBase().get()).isEqualByComparingTo(BigDecimal.valueOf(1256, 2));
        Assertions.assertEquals(Optional.of(VatRate.VAT_10_110), publishingPrice.vat());

        marketSkuOfferService.clearDiff();

        List<PapiFeedMarketSkuOffer> offers = new ArrayList<>();
        marketSkuOfferService.streamPublishedEntries(100, offers::add);

        Assertions.assertEquals(1, offers.size());
        PapiFeedMarketSkuOffer offer = offers.get(0);
        Assertions.assertEquals(feedMsku(234, 567, "offer567"), offer.feedMarketSku());

        Assertions.assertTrue(offer.properties().price().isPresent());

        PapiOfferPriceValue publishedPrice = offer.properties().price().get();

        Assertions.assertEquals(now, publishedPrice.updatedAt());
        assertThat(publishedPrice.value()).isEqualByComparingTo(BigDecimal.valueOf(1234, 2));
        assertThat(publishedPrice.discountBase().get()).isEqualByComparingTo(BigDecimal.valueOf(1256, 2));
        Assertions.assertEquals(Optional.of(VatRate.VAT_10_110), publishedPrice.vat());
    }

    @DisplayName("Проверяем, что удалятся записи с ценой")
    @Test
    @DbUnitDataSet(before = "papiMarketSkuOfferServiceCheckDeleteTest.before.csv",
            after = "papiMarketSkuOfferServiceCheckDeleteTest.after.csv")
    void checkDeleteByPartnerId() {
        long partnerId = 1001L;
        marketSkuOfferService.removeOffersPricesByPartner(partnerId);
    }

    @DisplayName("Проверяем, что удалятся скрытые записи")
    @Test
    @DbUnitDataSet(before = "papiMarketSkuOfferServiceCheckDeleteHiddenTest.before.csv",
            after = "papiMarketSkuOfferServiceCheckDeleteHiddenTest.after.csv")
    void checkDeleteHiddenOffersByPartnerId() {
        long partnerId = 1001L;
        marketSkuOfferService.removeHiddenOffersByPartner(partnerId);
    }

    @DisplayName("Сохранение скрытий")
    @Test
    @DbUnitDataSet(
            before = "hideOfferCheckPriority.before.csv",
            after = "hideOfferCheckPriority.after.csv"
    )
    void hideOffers_checkPriority_successful() {
        long partnerId = 1001L;
        marketSkuOfferService.hideOffers(
                hiding(feedMsku(101, 123), Duration.ofDays(2), null, null, null),
                CLIENT_ID, CampaignType.SUPPLIER, partnerId
        );
        marketSkuOfferService.hideOffers(
                hiding(feedMsku(101, 124), Duration.ofDays(2), false, null, PapiHidingSource.PULL_PARTNER_API),
                CLIENT_ID, CampaignType.SUPPLIER, partnerId
        );
        marketSkuOfferService.hideOffers
                (
                hiding(shopSku(101, "offer3"), Duration.ofDays(2), true, "Превышение порога цены",
                        PapiHidingSource.PUSH_PARTNER_API),
                CLIENT_ID, CampaignType.SUPPLIER, partnerId
        );
        marketSkuOfferService.hideOffers(
                hiding(shopSku(101, "offer1"), Duration.ofDays(2), false, null, PapiHidingSource.MARKET_PRICELABS),
                CLIENT_ID, CampaignType.SUPPLIER, partnerId
        );
        marketSkuOfferService.hideOffers(
                hiding(feedMsku(101, 113), Duration.ofDays(2), true, "Превышение порога цены", null),
                CLIENT_ID, CampaignType.SUPPLIER, partnerId
        );
    }

    @DisplayName("Проверяем, что корректно удаляются приоритетные скрытия по истечению TTL")
    @Test
    @DbUnitDataSet(
            before = "removeExpiredHidingsPriority.before.csv",
            after = "removeExpiredHidingsPriority.after.csv"
    )
    void removeExpiredHidings_checkPriority_successful() {
        marketSkuOfferService.removeExpiredHidings(1000, Instant.now().plusSeconds(1));
    }

    @DisplayName("Получение скрытий")
    @Test
    @DbUnitDataSet(before = "hiddenOffersEntriesHidingsPriority.before.csv")
    void hiddenOffersEntries_checkPriority_successful() {
        List<PapiFeedMarketSkuHiding> hidings = marketSkuOfferService.hiddenOffersEntries(Set.of(101L),
                Paging.firstN(1000));

        assertThat(hidings.size())
                .isEqualTo(3);
        assertThat(hidings.stream()
                .map(PapiFeedMarketSkuHiding::details)
                .map(PapiHidingDetails::hidingEvent)
                .collect(Collectors.toList()))
                .usingRecursiveFieldByFieldElementComparator()
                .usingElementComparatorOnFields("comment", "priority", "source")
                .containsExactlyInAnyOrder(
                        PapiHidingEvent.builder()
                                .setPriority(true)
                                .setComment("aga")
                                .build(),
                        PapiHidingEvent.builder()
                                .setPriority(true)
                                .setSource(PapiHidingSource.PUSH_PARTNER_API)
                                .build(),
                        PapiHidingEvent.builder()
                                .setSource(PapiHidingSource.MARKET_PRICELABS)
                                .build()
                );
    }

    @DisplayName("Получение деталей события о скрытии")
    @Test
    @DbUnitDataSet(before = "hiddenOffersEntriesHidingsPriority.before.csv")
    void getHiddenOfferEvents_partner_successful() {
        Map<IndexerOfferKey, PapiHidingEvent> hidings = marketSkuOfferService.getHiddenOfferEvents(Set.of(101L, 102L),
                Set.of("offer1", "offer2"), Set.of(103L, 123L));

        assertThat(hidings.size())
                .isEqualTo(3);
        assertPapiHidingEvent(hidings.get(anyMarketOrShopSkuUsedAndOtherIgnored(101L, null, "offer1")),
                PapiHidingEvent.builder()
                        .setPriority(true)
                        .setComment("aga")
                        .build());
        assertPapiHidingEvent(hidings.get(anyMarketOrShopSkuUsedAndOtherIgnored(101L, 103L, "offer3")),
                PapiHidingEvent.builder()
                        .setPriority(true)
                        .setSource(PapiHidingSource.PUSH_PARTNER_API)
                        .build());
        assertPapiHidingEvent(hidings.get(anyMarketOrShopSkuUsedAndOtherIgnored(101L, 123L, "offer4")),
                PapiHidingEvent.builder()
                        .setSource(PapiHidingSource.MARKET_PRICELABS)
                        .build());
    }

    @DisplayName("Получение деталей события о скрытии при отсутсвии marketSkus")
    @Test
    @DbUnitDataSet(before = "hiddenOffersEntriesHidingsPriority.before.csv")
    void getHiddenOfferEvents_emptyMarketSkus_successful() {
        Map<IndexerOfferKey, PapiHidingEvent> hidings = marketSkuOfferService.getHiddenOfferEvents(Set.of(101L, 102L),
                Set.of("offer1", "offer2"), Collections.emptyList());

        assertThat(hidings.size())
                .isEqualTo(1);
        assertPapiHidingEvent(hidings.get(anyMarketOrShopSkuUsedAndOtherIgnored(101L, null, "offer1")),
                PapiHidingEvent.builder()
                        .setPriority(true)
                        .setComment("aga")
                        .build());
    }

    @DisplayName("Получение деталей события о скрытии при отсутсвии shopSkus")
    @Test
    @DbUnitDataSet(before = "hiddenOffersEntriesHidingsPriority.before.csv")
    void getHiddenOfferEvents_emptyShopSkus_successful() {
        Map<IndexerOfferKey, PapiHidingEvent> hidings = marketSkuOfferService.getHiddenOfferEvents(Set.of(101L, 102L),
                Collections.emptyList(), Set.of(103L, 123L));

        assertThat(hidings.size())
                .isEqualTo(2);
        assertPapiHidingEvent(hidings.get(anyMarketOrShopSkuUsedAndOtherIgnored(101L, 103L, "offer3")),
                PapiHidingEvent.builder()
                        .setPriority(true)
                        .setSource(PapiHidingSource.PUSH_PARTNER_API)
                        .build());
        assertPapiHidingEvent(hidings.get(anyMarketOrShopSkuUsedAndOtherIgnored(101L, 123L, "offer4")),
                PapiHidingEvent.builder()
                        .setSource(PapiHidingSource.MARKET_PRICELABS)
                        .build());
    }

    @DisplayName("Получение деталей события о скрытии при отсутсвии shopSkus и marketSkus")
    @Test
    @DbUnitDataSet(before = "hiddenOffersEntriesHidingsPriority.before.csv")
    void getHiddenOfferEvents_emptyShopSkusAndMarketSkus_emptyMap() {
        Map<IndexerOfferKey, PapiHidingEvent> hidings = marketSkuOfferService.getHiddenOfferEvents(Set.of(101L, 102L),
                Collections.emptyList(), Collections.emptyList());

        assertThat(hidings.isEmpty())
                .isTrue();
    }

    @DisplayName("Получение деталей события о скрытии при отсутсвии feedIds")
    @Test
    @DbUnitDataSet(before = "hiddenOffersEntriesHidingsPriority.before.csv")
    void getHiddenOfferEvents_emptyFeedIds_emptyMap() {
        Map<IndexerOfferKey, PapiHidingEvent> hidings =
                marketSkuOfferService.getHiddenOfferEvents(Collections.emptyList(),
                        Set.of("offer1", "offer2"), Set.of(103L, 123L));

        assertThat(hidings.isEmpty())
                .isTrue();
    }

    private void assertPapiHidingEvent(PapiHidingEvent expected, PapiHidingEvent actual) {
        assertThat(actual)
                .usingRecursiveComparison()
                .ignoringFields("hiddenAt", "hidingExpiresAt")
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("Миграция приоритетных скрытий в MARKET_PRICELABS")
    @DbUnitDataSet(before = "testMigrateHidings.before.csv", after = "testMigrateHidingsPriorityPush.after.csv")
    void testMigrateHidingsPriorityPush() {
        marketSkuOfferService.migrateHidings(100, 1001L, CampaignType.SHOP, true, PapiHidingSource.MARKET_PRICELABS);
    }

    @Test
    @DisplayName("Миграция приоритетных скрытий в PULL_PARTNER_API")
    @DbUnitDataSet(before = "testMigrateHidings.before.csv", after = "testMigrateHidingsPriorityPull.after.csv")
    void testMigrateHidingsPriorityPull() {
        marketSkuOfferService.migrateHidings(100, 1001L, CampaignType.SHOP, true, PapiHidingSource.PULL_PARTNER_API);
    }

    @Test
    @DisplayName("Миграция не приоритетных скрытий в PUSH_PARTNER_API")
    @DbUnitDataSet(before = "testMigrateHidings.before.csv", after = "testMigrateHidingsNonPriorityPush.after.csv")
    void testMigrateHidingsNonPriorityPush() {
        marketSkuOfferService.migrateHidings(100, 1001L, CampaignType.SHOP, false, PapiHidingSource.PUSH_PARTNER_API);
    }

    @Test
    @DisplayName("Миграция не приоритетных скрытий в PULL_PARTNER_API")
    @DbUnitDataSet(before = "testMigrateHidings.before.csv", after = "testMigrateHidingsNonPriorityPull.after.csv")
    void testMigrateHidingsNonPriorityPull() {
        marketSkuOfferService.migrateHidings(100, 1001L, CampaignType.SHOP, false, PapiHidingSource.PULL_PARTNER_API);
    }

    private Map<IndexerOfferKey, PapiHidingEvent> hiding(IndexerOfferKey indexerOfferKey, Duration duration) {
        return hiding(indexerOfferKey, duration, null, null, null);
    }

    private Map<IndexerOfferKey, PapiHidingEvent> hiding(IndexerOfferKey indexerOfferKey,
                                                         Duration duration,
                                                         @Nullable Boolean priority,
                                                         @Nullable String comment,
                                                         @Nullable PapiHidingSource source) {
        PapiHidingEvent details = new PapiHidingEvent.Builder()
                .setHiddenAt(Instant.now())
                .setHidingExpiresAt(Instant.now().plus(duration))
                .setPriority(priority)
                .setComment(comment)
                .setSource(source)
                .build();
        return Collections.singletonMap(indexerOfferKey, details)   ;
    }

    private Map<IndexerOfferKey, PapiHidingEvent> hidingSku(long feedId, long marketSku, String shopSku) {
        return hiding(feedMsku(feedId, marketSku, shopSku), Duration.ofDays(2));
    }

    private Map<IndexerOfferKey, PapiHidingEvent> hidingShopSku(long feedId, String offerId) {
        return hiding(shopSku(feedId, offerId), Duration.ofDays(2));
    }

    private Map<IndexerOfferKey, ExpirablePapiOfferPrice> priceUpdate(
            long feedId,
            long marketSku,
            String shopSku,
            BigDecimal price,
            Duration duration) {
        return Collections.singletonMap(feedMsku(feedId, marketSku, shopSku), expirablePrice(price, duration));
    }

    private Map<IndexerOfferKey, ExpirablePapiOfferPrice> priceUpdate(
            long feedId,
            long marketSku,
            String shopSku,
            BigDecimal price) {
        return Collections
                .singletonMap(marketShopSku(feedId, marketSku, shopSku), expirablePrice(price, Duration.ofDays(2)));
    }

    private ExpirablePapiOfferPrice expirablePrice(BigDecimal price, Duration duration) {
        PapiOfferPriceValue value = new PapiOfferPriceValue.Builder()
                .setUpdatedAt(Instant.now())
                .setValue(price)
                .setCurrency(Currency.RUR)
                .build();
        return value.expirable(Instant.now().plus(duration));
    }
}
