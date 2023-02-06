package ru.yandex.market.core.yt.dynamic.samovar.feed;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.campaign.model.PartnerId;
import ru.yandex.market.core.feed.model.FeedType;
import ru.yandex.market.core.misc.resource.ResourceAccessCredentials;
import ru.yandex.market.yt.samovar.SamovarContextOuterClass;

import static ru.yandex.market.core.test.utils.SamovarFeedTestUtils.checkFeedInfo;
import static ru.yandex.market.core.test.utils.SamovarFeedTestUtils.checkSamovarFeed;

/**
 * Date: 04.09.2020
 * Project: arcadia-market_mbi_mbi
 *
 * @author alexminakov
 */
class ShopSamovarFeedServiceTest extends FunctionalTest {

    @Qualifier("shopSamovarFeedService")
    @Autowired
    private SamovarFeedService shopSamovarFeedService;

    @DbUnitDataSet(before = "ShopSamovarFeedServiceTest/getFeed.before.csv")
    @DisplayName("Поиск ассортиментного фида, доступного для выгрузки через самовар")
    @ParameterizedTest(name = "feedId = {0} - auth = {2}")
    @CsvSource({
            "1,774,test.login,test.password",
            "4,774,,",
            "5,775,,"
    })
    void getFeed_correctAssortmentFeed_presentOptional(long feedId, long partnerId, String login, String password) {
        Optional<SamovarFeed> oSamovarFeed = shopSamovarFeedService.getFeedForOneTimeDownload(feedId,
                PartnerId.partnerId(partnerId, CampaignType.SHOP), FeedType.ASSORTMENT);
        Assertions.assertThat(oSamovarFeed)
                .isPresent();

        SamovarContextOuterClass.SamovarContext samovarContext = ProtoTestUtil.getProtoMessageByJson(
                SamovarContextOuterClass.SamovarContext.class, "proto/shop.feed" + feedId + ".json", getClass()
        );
        SamovarFeed expectedFeed = SamovarFeed.builder()
                .setUrl("http://test.feed.url/")
                .setCredentials(login == null || password == null
                        ? null
                        : new ResourceAccessCredentials(login, password))
                .setPeriodMinutes(20)
                .setContext(samovarContext)
                .setTimeoutSeconds(SamovarFeedMapper.DEFAULT_TIMEOUT_SECONDS)
                .setEnabled(true)
                .build();
        checkSamovarFeed(expectedFeed, oSamovarFeed.get());
    }

    @DbUnitDataSet(before = "ShopSamovarFeedServiceTest/getFeed.before.csv")
    @DisplayName("Служебный фид не доступен для парсинга через самовар")
    @ParameterizedTest(name = "feedId = {0} - type = {2}")
    @CsvSource({
            "1,774,STOCKS",
            "4,774,PRICES",
            "5,775,STOCKS",
            "12,782,PRICES"
    })
    void getFeed_priceOrStockFeed_emptyOptional(long feedId, long partnerId, FeedType feedType) {
        Optional<SamovarFeed> oSamovarFeed = shopSamovarFeedService.getFeedForOneTimeDownload(feedId,
                PartnerId.partnerId(partnerId, CampaignType.SHOP), feedType);
        Assertions.assertThat(oSamovarFeed)
                .isEmpty();
    }

    @DbUnitDataSet(before = "ShopSamovarFeedServiceTest/testSites.before.csv")
    @DisplayName("Поиск сайта, доступного для парсинга через самовар")
    @ParameterizedTest(name = "feedId = {0}")
    @CsvSource({
            "23",
            "24"
    })
    void getSiteForParsing_correctFeed_presentOptional(long feedId) {
        Optional<SamovarFeed> oSamovarFeed = shopSamovarFeedService.getSiteForParsing(feedId);
        Assertions.assertThat(oSamovarFeed)
                .isEmpty();
    }

    @SuppressWarnings({"unused", "RedundantSuppression"})
    @DbUnitDataSet(before = "ShopSamovarFeedServiceTest/getFeed.before.csv")
    @DisplayName("Поиск фида завершился ошибкой")
    @ParameterizedTest(name = "feedId = {0} - {3}")
    @CsvSource({
            "2,774,SHOP,Фид имеет файл для скачивания",
            "3,774,SHOP,Фид является дефолтным",
            "5,775,DIRECT,Тип кампании DIRECT",
            "5,775,SUPPLIER,Тип кампании SUPPLIER",
            "5,776,SHOP,неверный партнер",
            "6,776,SHOP,Фид находится в переключении в PULL схему",
            "7,777,SHOP,Фид находится в схеме по умолчанию (PULL)",
            "8,778,SHOP,Фид Директа",
            "9,779,SHOP,Сайт Директа",
            "10,780,SHOP,Фид Директа в MDS",
            "11,781,SHOP,Фид отключен",
            "12,782,SHOP,Фид отключен"
    })
    void getFeed_correctFeedWithoutCredentials_presentOptional(long id, long partnerId, CampaignType campaignType,
                                                               String description) {
        Optional<SamovarFeed> oSamovarFeed = shopSamovarFeedService.getFeedForOneTimeDownload(id,
                PartnerId.partnerId(partnerId, campaignType), FeedType.ASSORTMENT);
        Assertions.assertThat(oSamovarFeed)
                .isEmpty();
    }

    @Test
    @DisplayName("Список сайтов для регулярного парсинга")
    @DbUnitDataSet(before = {
            "ShopSamovarFeedServiceTest/testSites.before.csv"
    })
    void testGetSitesForParsing() {
        final List<SamovarFeedInfo> actual = shopSamovarFeedService.getSitesForParsing();
        Assertions.assertThat(actual)
                .isEmpty();
    }

    @Test
    @DisplayName("Список фидов для регулярного парсинга")
    @DbUnitDataSet(before = "ShopSamovarFeedServiceTest/testGetFeeds.before.csv")
    void testGetFeeds() {
        doTestGetFeeds(List.of("proto/shop.feed1001.json",
                "proto/shop.feed1002.json",
                "proto/shop.feed1004.json",
                "proto/shop.feed1009.json"
        ));
    }

    @Test
    @DisplayName("Список фидов для регулярного парсинга с директовыми фичами")
    @DbUnitDataSet(before = {
            "ShopSamovarFeedServiceTest/testGetFeeds.before.csv",
            "ShopSamovarFeedServiceTest/testGetFeedsDirectFeatures.before.csv"
    })
    void testGetFeedsWithDirectFeatures() {
        doTestGetFeeds(List.of("proto/shop.feed1001.json",
                "proto/shop.feed1002.json",
                "proto/shop.feed1004.json",
                "proto/shop.feed1009.json"
        ));
    }

    private void doTestGetFeeds(@Nonnull List<String> jsonFilesOfExpectedFeeds) {
        List<SamovarContextOuterClass.FeedInfo> actual = shopSamovarFeedService.getFeedsForRepeatingDownload()
                .stream()
                .map(SamovarFeedInfo::toFeedInfo)
                .collect(Collectors.toList());

        List<SamovarContextOuterClass.FeedInfo> expected = jsonFilesOfExpectedFeeds.stream()
                .map(e -> ProtoTestUtil.getProtoMessageByJson(SamovarContextOuterClass.FeedInfo.class, e, getClass()))
                .collect(Collectors.toList());

        checkFeedInfo(expected, actual);
    }
}
