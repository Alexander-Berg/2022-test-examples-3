package ru.yandex.market.core.yt.dynamic.samovar.feed;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import ru.yandex.market.core.misc.resource.RemoteResource;
import ru.yandex.market.core.misc.resource.ResourceAccessCredentials;
import ru.yandex.market.core.test.utils.SamovarFeedTestUtils;
import ru.yandex.market.yt.samovar.SamovarContextOuterClass;

import static ru.yandex.market.core.test.utils.SamovarFeedTestUtils.assertSamovarFeedInfoLists;
import static ru.yandex.market.core.test.utils.SamovarFeedTestUtils.checkFeedInfo;
import static ru.yandex.market.core.test.utils.SamovarFeedTestUtils.checkSamovarFeed;

/**
 * Date: 02.03.2021
 *
 * @author moskovkin
 */
class DirectFeedSamovarServiceTest extends FunctionalTest {

    @Qualifier("directFeedSamovarService")
    @Autowired
    private SamovarFeedService directFeedSamovarService;

    @DbUnitDataSet(before = "ShopSamovarFeedServiceTest/getFeed.before.csv")
    @DisplayName("Поиск фида, доступного для выгрузки через самовар")
    @ParameterizedTest(name = "feedId = {0} - auth = {1}")
    @CsvSource({
            "8,,",
    })
    void getFeed_correctFeed_presentOptional(long feedId, String login, String password) {
        Optional<SamovarFeed> oSamovarFeed = directFeedSamovarService.getFeedForOneTimeDownload(feedId,
                PartnerId.partnerId(778L, CampaignType.DIRECT), FeedType.ASSORTMENT);
        Assertions.assertThat(oSamovarFeed)
                .isPresent();

        SamovarContextOuterClass.SamovarContext samovarContext = ProtoTestUtil.getProtoMessageByJson(
                SamovarContextOuterClass.SamovarContext.class, "proto/shop.feed" + feedId + ".json", getClass()
        );

        SamovarFeed expectedFeed = SamovarFeed.builder()
                .setUrl("http://test.feed" + feedId + ".url/")
                .setCredentials(login == null || password == null
                        ? null
                        : new ResourceAccessCredentials(login, password))
                .setPeriodMinutes(120)
                .setContext(samovarContext)
                .setTimeoutSeconds(SamovarFeedMapper.DEFAULT_TIMEOUT_SECONDS)
                .setEnabled(true)
                .build();
        checkSamovarFeed(expectedFeed, oSamovarFeed.get());
    }

    @DbUnitDataSet(before = "ShopSamovarFeedServiceTest/testSites.before.csv")
    @DisplayName("Поиск сайта, доступного для парсинга через самовар")
    @ParameterizedTest(name = "feedId = {0}")
    @CsvSource({
            "23,120",
            "24,"
    })
    void getSiteForParsing_correctFeed_presentOptional(long feedId, Integer reparseInterval) {
        Optional<SamovarFeed> oSamovarFeed = directFeedSamovarService.getSiteForParsing(feedId);
        Assertions.assertThat(oSamovarFeed)
                .isPresent();

        SamovarContextOuterClass.SamovarContext samovarContext = ProtoTestUtil.getProtoMessageByJson(
                SamovarContextOuterClass.SamovarContext.class, "proto/shop.context.feed" + feedId + ".json", getClass()
        );

        SamovarFeed expectedFeed = SamovarFeed.builder()
                .setUrl("http://test.site" + feedId + ".url/")
                .setPeriodMinutes(reparseInterval == null ? SamovarFeedMapper.DEFAULT_PERIOD_MINUTES : reparseInterval)
                .setContext(samovarContext)
                .setTimeoutSeconds(SamovarFeedMapper.DEFAULT_TIMEOUT_SECONDS)
                .setEnabled(true)
                .build();
        checkSamovarFeed(expectedFeed, oSamovarFeed.get());
    }

    @SuppressWarnings({"unused", "RedundantSuppression"})
    @DbUnitDataSet(before = "ShopSamovarFeedServiceTest/getFeed.before.csv")
    @DisplayName("Поиск фида завершился ошибкой")
    @ParameterizedTest(name = "feedId = {0} - {3}")
    @CsvSource({
            "1,774,DIRECT,Фид не от DIRECT",
            "4,774,DIRECT,Фид не от DIRECT",
            "5,775,DIRECT,Фид не от DIRECT",
            "8,778,SHOP,Тип кампании SHOP",
            "8,778,SUPPLIER,Тип кампании SUPPLIER",
            "9,779,DIRECT,Фид EXTERNAL_MDS",
            "10,780,DIRECT,Фид SITES_PARSING"
    })
    void getFeed_correctFeedWithoutCredentials_presentOptional(long id, long partnerId, CampaignType campaignType,
                                                               String description) {
        Optional<SamovarFeed> oSamovarFeed = directFeedSamovarService.getFeedForOneTimeDownload(id,
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
        var samovarFeedInfo = directFeedSamovarService.getSitesForParsing();

        List<SamovarFeedInfo> expectedList = List.of(
                SamovarFeedTestUtils.createSamovarFeedInfo(
                        45L,
                        CampaignType.DIRECT,
                        23L,
                        RemoteResource.of("http://test.site23.url/"),
                        null,
                        120,
                        1440
                ),
                SamovarFeedTestUtils.createSamovarFeedInfo(
                        45L,
                        CampaignType.DIRECT,
                        24L,
                        RemoteResource.of("http://test.site24.url/"),
                        null,
                        null,
                        1440
                )
        );

        assertSamovarFeedInfoLists(samovarFeedInfo, expectedList);

        List<SamovarContextOuterClass.FeedInfo> actual = samovarFeedInfo
                .stream()
                .map(SamovarFeedInfo::toFeedInfo)
                .collect(Collectors.toList());

        List<SamovarContextOuterClass.FeedInfo> expected = Stream.of(
                "proto/shop.feed23.json",
                "proto/shop.feed24.json"
        )
                .map(e -> ProtoTestUtil.getProtoMessageByJson(SamovarContextOuterClass.FeedInfo.class, e, getClass()))
                .collect(Collectors.toList());

        checkFeedInfo(expected, actual);
    }

    @Test
    @DisplayName("Список фидов для регулярного парсинга")
    @DbUnitDataSet(before = "ShopSamovarFeedServiceTest/getFeed.before.csv")
    void testGetFeeds() {
        var samovarFeedInfo = directFeedSamovarService.getFeedsForRepeatingDownload();

        List<SamovarFeedInfo> expectedList = List.of(
                SamovarFeedTestUtils.createSamovarFeedInfo(
                        778L,
                        CampaignType.DIRECT,
                        8L,
                        RemoteResource.of("http://test.feed8.url/"),
                        null,
                        120,
                        1440
                )
        );

        assertSamovarFeedInfoLists(samovarFeedInfo, expectedList);

        List<SamovarContextOuterClass.FeedInfo> actual = samovarFeedInfo
                .stream()
                .map(SamovarFeedInfo::toFeedInfo)
                .collect(Collectors.toList());

        List<SamovarContextOuterClass.FeedInfo> expected = Stream.of("proto/shop.feed.info8.json")
                .map(e -> ProtoTestUtil.getProtoMessageByJson(SamovarContextOuterClass.FeedInfo.class, e, getClass()))
                .collect(Collectors.toList());

        checkFeedInfo(expected, actual);
    }
}
