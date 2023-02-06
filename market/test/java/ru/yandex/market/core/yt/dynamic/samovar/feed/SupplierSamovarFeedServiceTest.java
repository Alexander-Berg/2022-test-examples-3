package ru.yandex.market.core.yt.dynamic.samovar.feed;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.protobuf.Timestamp;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.campaign.model.PartnerId;
import ru.yandex.market.core.feed.model.FeedType;
import ru.yandex.market.core.misc.resource.ResourceAccessCredentials;
import ru.yandex.market.yt.samovar.SamovarContextOuterClass;

/**
 * Тесты проверяют правильность построения {@link SamovarFeed} в
 * {@link SupplierSamovarFeedService}.
 */
class SupplierSamovarFeedServiceTest extends FunctionalTest {

    private static final String[] IGNORE_FIELDS = {
            ".*bitField0_.*",
            ".*memoizedHashCode.*",
            ".*memoizedIsInitialized.*",
            ".*updatedAt.*"
    };

    @Autowired
    private SamovarFeedService supplierSamovarFeedService;

    @DbUnitDataSet(before = "SupplierSamovarFeedService/before.csv")
    @DisplayName("Поиск ассортиментного/ценового фида, доступного для выгрузки через самовар")
    @ParameterizedTest(name = "feedId = {0} - feedType = {3} - feedUrl = {4}")
    @CsvSource({
            "10,SUPPLIER,111,ASSORTMENT,http://feed.ru/,testLogin,testPwd",
            "12,SUPPLIER,113,ASSORTMENT,http://feed1.ru,,",
            "15,SUPPLIER,115,ASSORTMENT,http://feed2.ru,,",
            "16,BUSINESS,116,ASSORTMENT,http://feed2.ru,,"
    })
    void getFeed_correctAssortmentFeed_presentOptional(long feedId, String campaign, long partnerId, FeedType feedType,
                                                       String url, String login, String password) {
        var campaignType = CampaignType.getById(campaign);
        Optional<SamovarFeed> oSamovarFeed = supplierSamovarFeedService.getFeedForOneTimeDownload(feedId,
                PartnerId.partnerId(partnerId, campaignType), feedType);
        Assertions.assertThat(oSamovarFeed)
                .isPresent();

        SamovarContextOuterClass.SamovarContext samovarContext = ProtoTestUtil.getProtoMessageByJson(
                SamovarContextOuterClass.SamovarContext.class,
                "SupplierSamovarFeedService/proto/supplier.feed" + feedId + ".context.json", getClass()
        );
        SamovarFeed expectedFeed = SamovarFeed.builder()
                .setUrl(url)
                .setCredentials(login == null || password == null
                        ? null
                        : new ResourceAccessCredentials(login, password))
                .setPeriodMinutes(feedId == 15 || feedId == 16 ? 180 : 20)
                .setContext(samovarContext)
                .setTimeoutSeconds(SamovarFeedMapper.DEFAULT_TIMEOUT_SECONDS)
                .setEnabled(true)
                .build();
        Assertions.assertThat(oSamovarFeed.get())
                .usingRecursiveComparison()
                .ignoringFieldsMatchingRegexes(IGNORE_FIELDS)
                .isEqualTo(expectedFeed);
    }

    @SuppressWarnings("unused")
    @DbUnitDataSet(before = "SupplierSamovarFeedService/before.csv")
    @DisplayName("Поиск фида завершился ошибкой")
    @ParameterizedTest(name = "feedId = {0} - {4}")
    @CsvSource({
            "10,111,SUPPLIER,STOCKS,Стоковый фид для партнера не в Екат",
            "12,113,SHOP,PRICES,Тип кампании SHOP",
            "15,115,SUPPLIER,STOCKS,Стоковый фид для партнера в Екат",
            "15,115,SUPPLIER,PRICES,Ценовой фид для партнера в Екат",
            "15,115,DIRECT,ASSORTMENT,Тип кампании DIRECT",
            "12,115,SUPPLIER,ASSORTMENT,Неверный партнер"
    })
    void getFeed_priceOrStockFeed_emptyOptional(long feedId, long partnerId, CampaignType campaignType,
                                                FeedType feedType, String description) {
        Optional<SamovarFeed> oSamovarFeed = supplierSamovarFeedService.getFeedForOneTimeDownload(feedId,
                PartnerId.partnerId(partnerId, campaignType), feedType);
        Assertions.assertThat(oSamovarFeed)
                .isEmpty();
    }

    @Test
    @DisplayName("Правильно строятся строки для выгрузки фидов в YT для самовара с авторизацией")
    @DbUnitDataSet(before = "SupplierSamovarFeedService/before.csv")
    void testSupplierSamovarFeedService() {
        checkGetFeeds(
                "SupplierSamovarFeedService/proto/supplier.feed10.json",
                "SupplierSamovarFeedService/proto/supplier.feed12.json",
                "SupplierSamovarFeedService/proto/supplier.feed15.json",
                "SupplierSamovarFeedService/proto/supplier.feed13.json"
        );
    }

    @Test
    @DisplayName("Правильно строятся строки для YT в случае отсутствия фидов")
    void testSupplierSamovarFeedServiceWithoutFeeds() {
        checkGetFeeds();
    }

    private void checkGetFeeds(String... expectedProtos) {
        List<SamovarContextOuterClass.FeedInfo> actual = supplierSamovarFeedService.getFeedsForRepeatingDownload()
                .stream()
                .map(SamovarFeedInfo::toFeedInfo)
                .map(SamovarContextOuterClass.FeedInfo::toBuilder)
                .map(builder -> builder.setUpdatedAt(Timestamp.getDefaultInstance()))
                .map(SamovarContextOuterClass.FeedInfo.Builder::build)
                .collect(Collectors.toList());

        List<SamovarContextOuterClass.FeedInfo> expected = Arrays.stream(expectedProtos)
                .map(e -> ProtoTestUtil.getProtoMessageByJson(SamovarContextOuterClass.FeedInfo.class, e, getClass()))
                .collect(Collectors.toList());

        Assertions.assertThat(actual)
                .containsExactlyInAnyOrderElementsOf(expected);
    }
}
