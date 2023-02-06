package ru.yandex.market.core.feed.supplier.db;


import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.feed.model.FeedType;
import ru.yandex.market.core.feed.supplier.model.PartnerUtilityFeed;
import ru.yandex.market.core.misc.resource.RemoteResource;
import ru.yandex.market.core.util.DateTimes;

@SuppressWarnings("unused")
@DbUnitDataSet(before = "PartnerUtilityFeedDao/before.csv")
class PartnerUtilityFeedDaoTest extends FunctionalTest {

    private static final Instant NOW = DateTimes.toInstant(2020, 1, 1);

    @Nonnull
    private static Stream<Arguments> partnerWithFeedData() {
        return Stream.of(
                Arguments.of(
                        "PRICES по файлу",
                        101L,
                        FeedType.PRICES,
                        PartnerUtilityFeed.builder()
                                .setPartnerId(101L)
                                .setBusinessId(667L)
                                .setUploadId(11L)
                                .setOriginalFileName("file.name")
                                .setType(FeedType.PRICES)
                                .setUpdatedAt(NOW)
                                .build()
                ),
                Arguments.of(
                        "STOCKS по файлу",
                        101L,
                        FeedType.STOCKS,
                        PartnerUtilityFeed.builder()
                                .setPartnerId(101L)
                                .setBusinessId(667L)
                                .setUploadId(10L)
                                .setOriginalFileName("file.name")
                                .setType(FeedType.STOCKS)
                                .setUpdatedAt(NOW)
                                .build()
                ),
                Arguments.of(
                        "PRICES по ссылке",
                        104L,
                        FeedType.PRICES,
                        PartnerUtilityFeed.builder()
                                .setPartnerId(104L)
                                .setBusinessId(668L)
                                .setType(FeedType.PRICES)
                                .setPeriod(10)
                                .setTimeout(1)
                                .setResource(RemoteResource.of("url", "f_log", "f_pass"))
                                .setValidationId(11L)
                                .setUpdatedAt(NOW)
                                .build()
                )
        );
    }

    @Autowired
    private PartnerUtilityFeedDao partnerUtilityFeedDao;

    @DisplayName("Добавление нового стокового фида партнера.")
    @DbUnitDataSet(after = "PartnerUtilityFeedDao/insertStock.after.csv")
    @Test
    void insertOrUpdate_newStockFeed_created() {
        PartnerUtilityFeed feed = PartnerUtilityFeed.builder()
                .setPartnerId(102L)
                .setBusinessId(666L)
                .setUploadId(20L)
                .setType(FeedType.STOCKS)
                .setUpdatedAt(NOW)
                .build();
        partnerUtilityFeedDao.insertOrUpdate(feed);
    }

    @DisplayName("Сохранение ценового фида по ссылке")
    @DbUnitDataSet(after = "PartnerUtilityFeedDao/insertPriceByUrl.after.csv")
    @Test
    void insertOrUpdate_newPriceFeedByUrl_created() {
        PartnerUtilityFeed feed = PartnerUtilityFeed.builder()
                .setPartnerId(104L)
                .setBusinessId(668L)
                .setType(FeedType.PRICES)
                .setPeriod(10)
                .setTimeout(1)
                .setResource(RemoteResource.of("url", "f_log", "f_pass"))
                .setValidationId(11L)
                .setUpdatedAt(NOW)
                .build();
        partnerUtilityFeedDao.insertOrUpdate(feed);
    }

    @DisplayName("Обновление существующего стокового фида партнера.")
    @DbUnitDataSet(after = "PartnerUtilityFeedDao/updateStock.after.csv")
    @Test
    void insertOrUpdate_existedStockFeed_updated() {
        PartnerUtilityFeed feed = PartnerUtilityFeed.builder()
                .setPartnerId(101L)
                .setBusinessId(667L)
                .setUploadId(20L)
                .setType(FeedType.STOCKS)
                .setUpdatedAt(NOW)
                .build();
        partnerUtilityFeedDao.insertOrUpdate(feed);
    }

    @DisplayName("Добавление нового ценового фида партнера.")
    @DbUnitDataSet(after = "PartnerUtilityFeedDao/insertPrice.after.csv")
    @Test
    void insertOrUpdate_newPriceFeed_created() {
        PartnerUtilityFeed feed = PartnerUtilityFeed.builder()
                .setPartnerId(102L)
                .setBusinessId(666L)
                .setUploadId(21L)
                .setType(FeedType.PRICES)
                .setUpdatedAt(NOW)
                .build();
        partnerUtilityFeedDao.insertOrUpdate(feed);
    }

    @DisplayName("Обновление существующего ценового фида партнера.")
    @DbUnitDataSet(after = "PartnerUtilityFeedDao/updatePrice.after.csv")
    @Test
    void insertOrUpdate_existedPriceFeed_updated() {
        PartnerUtilityFeed feed = PartnerUtilityFeed.builder()
                .setPartnerId(101L)
                .setBusinessId(667L)
                .setUploadId(21L)
                .setType(FeedType.PRICES)
                .setUpdatedAt(NOW)
                .build();
        partnerUtilityFeedDao.insertOrUpdate(feed);
    }

    @DisplayName("Добавление нового ценового фида партнера, если есть стоковый фид.")
    @DbUnitDataSet(after = "PartnerUtilityFeedDao/insertPriceWhenStockExisted.after.csv")
    @Test
    void insertOrUpdate_newPriceFeedWhenStockFeedExisted_created() {
        PartnerUtilityFeed feed = PartnerUtilityFeed.builder()
                .setPartnerId(103L)
                .setBusinessId(669L)
                .setUploadId(31L)
                .setType(FeedType.PRICES)
                .setUpdatedAt(NOW)
                .build();
        partnerUtilityFeedDao.insertOrUpdate(feed);
    }

    @ParameterizedTest(name = "{1}")
    @DisplayName("Поиск uploadId по партнеру, у которого есть фид.")
    @MethodSource("partnerWithFeedData")
    @DbUnitDataSet(before = "PartnerUtilityFeedDao/partnerWithFeedData.before.csv")
    void getUploadId_partnerWithPriceFeed_uploadId(String title, long partnerId, FeedType feedType,
                                                   PartnerUtilityFeed expected) {
        List<PartnerUtilityFeed> actual = partnerUtilityFeedDao.getUtilityFeeds(List.of(partnerId), List.of(feedType));
        Assertions.assertThat(actual)
                .hasSize(1)
                .containsExactlyInAnyOrder(expected);
    }

    @Test
    @DisplayName("Поиск служебных фидов при пустом списке партнеров")
    void getUtilityFeedsWhenPartnerIdsEmpty() {
        List<FeedType> feedTypes = List.of(FeedType.PRICES, FeedType.STOCKS);
        Assertions.assertThat(partnerUtilityFeedDao.getUtilityFeeds(List.of(),feedTypes)).isEmpty();
    }

        @Test
    @DisplayName("Поиск всех служебных фидов партнера.")
    void getUtilityFeeds_partnerIdsAndTypes_all() {
        List<Long> partnerIds = List.of(101L, 103L);
        List<FeedType> feedTypes = List.of(FeedType.PRICES, FeedType.STOCKS);

        Assertions.assertThat(partnerUtilityFeedDao.getUtilityFeeds(partnerIds, feedTypes))
                .hasSize(3)
                .containsExactlyInAnyOrder(
                        PartnerUtilityFeed.builder()
                                .setPartnerId(101L)
                                .setBusinessId(667L)
                                .setUploadId(10L)
                                .setType(FeedType.STOCKS)
                                .setUpdatedAt(NOW)
                                .setOriginalFileName("file.name")
                                .build(),
                        PartnerUtilityFeed.builder()
                                .setPartnerId(101L)
                                .setBusinessId(667L)
                                .setUploadId(11L)
                                .setType(FeedType.PRICES)
                                .setUpdatedAt(NOW)
                                .setOriginalFileName("file.name")
                                .build(),
                        PartnerUtilityFeed.builder()
                                .setPartnerId(103L)
                                .setBusinessId(669L)
                                .setUploadId(30L)
                                .setType(FeedType.STOCKS)
                                .setUpdatedAt(NOW)
                                .setOriginalFileName("file.name")
                                .build()
                );
    }

    @CsvSource({
            "PRICES",
            "STOCKS"
    })
    @ParameterizedTest(name = "{0}")
    @DisplayName("Поиск uploadId по партнеру, у которого нет фида.")
    void getUploadId_partnerWithoutPriceFeed_empty(FeedType feedType) {
        Assertions.assertThat(partnerUtilityFeedDao.getUtilityFeeds(List.of(102L), List.of(feedType)))
                .isEmpty();
    }

    @DbUnitDataSet(after = "PartnerUtilityFeedDao/deleteStock.after.csv")
    @DisplayName("Удаление стокового фида партнера.")
    @Test
    void delete_stock_success() {
        delete(List.of(FeedType.STOCKS));
    }

    @DbUnitDataSet(after = "PartnerUtilityFeedDao/deletePrice.after.csv")
    @DisplayName("Удаление ценового фида партнера.")
    @Test
    void delete_price_success() {
        delete(List.of(FeedType.PRICES));
    }

    @DbUnitDataSet(after = "PartnerUtilityFeedDao/deleteAll.after.csv")
    @DisplayName("Удаление ценового и стокового фида партнера.")
    @Test
    void delete_priceAndStock_success() {
        delete(List.of(FeedType.PRICES, FeedType.STOCKS));
    }

    private void delete(@Nonnull Collection<FeedType> feedTypes) {
        partnerUtilityFeedDao.delete(101L, feedTypes);
    }
}
