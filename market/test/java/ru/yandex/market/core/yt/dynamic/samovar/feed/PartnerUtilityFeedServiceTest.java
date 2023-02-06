package ru.yandex.market.core.yt.dynamic.samovar.feed;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import ru.yandex.market.core.misc.resource.RemoteResource;
import ru.yandex.market.core.misc.resource.ResourceAccessCredentials;
import ru.yandex.market.yt.samovar.SamovarContextOuterClass;
import ru.yandex.market.yt.samovar.SamovarContextOuterClass.FeedInfo;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Date: 11.06.2021
 * Project: arcadia-market_mbi_mbi
 *
 * @author alexminakov
 */
class PartnerUtilityFeedServiceTest extends FunctionalTest {

    private static final String[] IGNORE_FIELDS = {
            "periodMinutes", // иначе наводки от соседних тестов, тк используется дефолт из env в SamovarFeedMapper
            ".*bitField0_.*",
            ".*memoizedHashCode.*",
            ".*memoizedIsInitialized.*",
            ".*updatedAt.*"
    };

    @Qualifier("partnerUtilityFeedService")
    @Autowired
    private SamovarFeedService samovarFeedService;

    @SuppressWarnings("checkstyle:parameterNumber")
    @DbUnitDataSet(before = "PartnerUtilityFeedService/csv/before.csv")
    @DisplayName("Поиск служебного фида, доступного для выгрузки через самовар")
    @ParameterizedTest(name = "feedId = {0} - feedType = {2} - campaignType = {3} - feedUrl = {4}")
    @CsvSource({
            "15,115,STOCKS,SUPPLIER,https://market.net/stocks,,,150",
            "15,115,PRICES,SUPPLIER,https://market.net/price_100,42,no,150",
            "999,773,PRICES,SHOP,https://market.net/price_shop_test,42,no,150",
            "1004,775,STOCKS,SHOP,https://market.net/stocks_shop,,,1000"
    })
    void getFeed_correctAssortmentFeed_presentOptional(long feedId, long partnerId, FeedType feedType,
                                                       CampaignType campaignType, String url, String login,
                                                       String password, int timeout) {
        Optional<SamovarFeed> oSamovarFeed = samovarFeedService.getFeedForOneTimeDownload(feedId,
                PartnerId.partnerId(partnerId, campaignType), feedType);
        assertThat(oSamovarFeed)
                .isPresent();

        SamovarContextOuterClass.SamovarContext samovarContext = ProtoTestUtil.getProtoMessageByJson(
                SamovarContextOuterClass.SamovarContext.class,
                "PartnerUtilityFeedService/proto/" + partnerId + "_" + feedType.name().toLowerCase() + ".context.json",
                getClass()
        );
        SamovarFeed expectedFeed = SamovarFeed.builder()
                .setUrl(url)
                .setCredentials(login == null || password == null
                        ? null
                        : new ResourceAccessCredentials(login, password))
                .setPeriodMinutes(20) // whatever
                .setContext(samovarContext)
                .setTimeoutSeconds(timeout)
                .setEnabled(true)
                .build();
        assertThat(oSamovarFeed.get())
                .usingRecursiveComparison()
                .ignoringFieldsMatchingRegexes(IGNORE_FIELDS)
                .isEqualTo(expectedFeed);
    }

    @SuppressWarnings("unused")
    @DbUnitDataSet(before = "PartnerUtilityFeedService/csv/before.csv")
    @DisplayName("Поиск фида завершился ошибкой")
    @ParameterizedTest(name = "feedId = {0} - {4}")
    @CsvSource({
            "10,111,SUPPLIER,STOCKS,Стоковый upload фид",
            "1001,774,SHOP,PRICES,Ценовой upload фид",
            "10,111,SUPPLIER,PRICES,Ценовой фид отсутствует",
            "1001,774,SHOP,STOCKS,Стоковый фид отсутствует",
            "15,115,SUPPLIER,ASSORTMENT,Ассортиментный фид не поддерживается"
    })
    void getFeed_priceOrStockFeed_emptyOptional(long feedId, long partnerId, CampaignType campaignType,
                                                FeedType feedType, String description) {
        Optional<SamovarFeed> oSamovarFeed = samovarFeedService.getFeedForOneTimeDownload(feedId,
                PartnerId.partnerId(partnerId, campaignType), feedType);
        assertThat(oSamovarFeed)
                .isEmpty();
    }

    @DisplayName("Получение всех служебных фидов по ссылке для регулярного скачиваяния")
    @Test
    @DbUnitDataSet(before = "PartnerUtilityFeedService/csv/before.csv")
    void getFeedsForRepeatingDownload() {
        Map<Long, Map<FeedInfo.FeedType, SamovarFeedInfo>> actual = samovarFeedService.getFeedsForRepeatingDownload()
                .stream()
                .collect(Collectors.toMap(
                        SamovarFeedInfo::getPartnerId,
                        feedInfo -> {
                            Map<FeedInfo.FeedType, SamovarFeedInfo> map = new HashMap<>();
                            map.put(feedInfo.toFeedInfo().getFeedType(), feedInfo);
                            return map;
                        },
                        (map1, map2) -> {
                            map1.putAll(map2);
                            return map1;
                        }
                ));

        Map<Long, Map<FeedInfo.FeedType, FeedInfo>> expectedFeedInfos = Stream.of(
                "PartnerUtilityFeedService/proto/113_price.json",
                "PartnerUtilityFeedService/proto/115_price.json",
                "PartnerUtilityFeedService/proto/115_stock.json",
                "PartnerUtilityFeedService/proto/773_price.json",
                "PartnerUtilityFeedService/proto/775_price.json",
                "PartnerUtilityFeedService/proto/775_stock.json"
        )
                .map(e -> ProtoTestUtil.getProtoMessageByJson(FeedInfo.class, e, getClass()))
                .collect(Collectors.toMap(
                        FeedInfo::getShopId,
                        feedInfo -> {
                            Map<FeedInfo.FeedType, FeedInfo> map = new HashMap<>();
                            map.put(feedInfo.getFeedType(), feedInfo);
                            return map;
                        },
                        (map1, map2) -> {
                            map1.putAll(map2);
                            return map1;
                        }
                ));
        Map<Long, Map<FeedInfo.FeedType, RemoteResource>> expectedRemoteResource = Map.of(
                113L,
                Map.of(
                        FeedInfo.FeedType.PRICES,
                        RemoteResource.of("https://market.net/price",
                                ResourceAccessCredentials.of("42", "no"))
                ),
                115L,
                Map.of(
                        FeedInfo.FeedType.PRICES,
                        RemoteResource.of("https://market.net/price_100",
                                ResourceAccessCredentials.of("42", "no")),
                        FeedInfo.FeedType.STOCKS,
                        RemoteResource.of("https://market.net/stocks")
                ),
                773L,
                Map.of(
                        FeedInfo.FeedType.PRICES,
                        RemoteResource.of("https://market.net/price_shop_test",
                                ResourceAccessCredentials.of("42", "no"))
                ),
                775L,
                Map.of(
                        FeedInfo.FeedType.PRICES,
                        RemoteResource.of("https://market.net/price_shop",
                                ResourceAccessCredentials.of("42", "no")),
                        FeedInfo.FeedType.STOCKS,
                        RemoteResource.of("https://market.net/stocks_shop")
                )
        );
        Map<Long, Map<FeedInfo.FeedType, Integer>> expectedPeriod = Map.of(
                113L, Map.of(FeedInfo.FeedType.PRICES, 100),
                775L, Map.of(FeedInfo.FeedType.STOCKS, 20)
        );
        Map<Long, Map<FeedInfo.FeedType, Integer>> expectedTimeout = Map.of(
                113L, Map.of(FeedInfo.FeedType.PRICES, 550),
                775L, Map.of(FeedInfo.FeedType.STOCKS, 1000)
        );

        actual.forEach((partnerId, feedTypeFeedInfoMapActual) -> {
            Map<FeedInfo.FeedType, FeedInfo> feedTypeFeedInfoMapExpected = expectedFeedInfos.get(partnerId);
            Map<FeedInfo.FeedType, RemoteResource> feedTypeRRMapExpected = expectedRemoteResource.get(partnerId);
            Map<FeedInfo.FeedType, Integer> feedTypePeriodMapExpected = expectedPeriod.get(partnerId);
            Map<FeedInfo.FeedType, Integer> feedTypeTimeoutMapExpected = expectedTimeout.get(partnerId);
            assertThat(feedTypeFeedInfoMapExpected)
                    .hasSameSizeAs(feedTypeFeedInfoMapActual);
            assertThat(feedTypeRRMapExpected)
                    .hasSameSizeAs(feedTypeFeedInfoMapActual);
            feedTypeFeedInfoMapActual.forEach((feedType, feedInfo) -> {
                assertThat(feedInfo.getResource())
                        .isEqualTo(feedTypeRRMapExpected.get(feedType));
                assertThat(feedInfo.getPeriod())
                        .isEqualTo(feedTypePeriodMapExpected == null
                                ? null
                                : feedTypePeriodMapExpected.get(feedType));
                assertThat(feedInfo.getTimeout())
                        .isEqualTo(feedTypeTimeoutMapExpected == null
                                ? null
                                : feedTypeTimeoutMapExpected.get(feedType));
                FeedInfo expectedFeedInfo = feedTypeFeedInfoMapExpected.get(feedType);
                ProtoTestUtil.assertThat(feedInfo.toFeedInfo())
                        .ignoringFieldsMatchingRegexes(".*updatedAt.*")
                        .isEqualTo(expectedFeedInfo);
            });
        });
    }
}
