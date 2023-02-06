package ru.yandex.market.core.feed;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

import ru.yandex.common.framework.core.MultipartRemoteFile;
import ru.yandex.common.framework.core.RemoteFile;
import ru.yandex.market.common.excel.MarketTemplate;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.feed.datacamp.FeedParsingType;
import ru.yandex.market.core.feed.db.FeedUploadException;
import ru.yandex.market.core.feed.event.DataCampCreateUpdateFeedEventListener;
import ru.yandex.market.core.feed.event.PartnerParsingFeedEvent;
import ru.yandex.market.core.feed.mds.FeedFileStorage;
import ru.yandex.market.core.feed.mds.StoreInfo;
import ru.yandex.market.core.feed.model.FeedInfo;
import ru.yandex.market.core.feed.model.FeedSiteType;
import ru.yandex.market.core.feed.model.FeedUpload;
import ru.yandex.market.core.feed.model.TooManyFeedsException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Тесты на {@link FeedService}.
 *
 * @author fbokovikov
 */
class FeedServiceTest extends FunctionalTest {
    private static final RemoteFile FILE_FOR_UPLOADING =
            new MultipartRemoteFile(new MockMultipartFile("test.file.name", "test.content".getBytes()));

    @Autowired
    private FeedFileStorage feedFileStorage;

    @Autowired
    private FeedService feedService;

    @Autowired
    private DataCampCreateUpdateFeedEventListener dataCampCreateUpdateFeedEventListener;

    /**
     * Проверка сценария, когда загруженный фид не найден, поиск по shopId + url.
     */
    @Test
    void testFeedUploadNotFound() {
        assertThat(feedService.getFeedUpload(123454L, "not.exists.url")).isEmpty();
    }

    /**
     * Тест на {@link FeedService#getFeedUpload(long, String) получение загруженного фида}.
     * Поиск по shopId + url.
     */
    @Test
    @DbUnitDataSet(before = "testGetFeedUpload.csv")
    void testGetFeedUpload() {
        var expected = new FeedUpload(774L, "test.feed.name", new Date());
        expected.setUrl("test.feed.url");
        expected.setId(1234L);
        expected.setSize(100500);

        var feedUpload = feedService.getFeedUpload(774, "test.feed.url");
        assertThat(feedUpload).get()
                .usingRecursiveComparison()
                .ignoringFieldsOfTypes(Date.class)
                .isEqualTo(expected);
    }

    /**
     * Тест на {@link FeedService#uploadFeed(FeedUpload, RemoteFile) загрузку фида}.
     */
    @Test
    @DbUnitDataSet(
            before = "testFeedUploading.before.csv",
            after = "testFeedUploading.after.csv"
    )
    void testFeedUploading() throws IOException {
        when(feedFileStorage.upload(any(RemoteFile.class), anyLong()))
                .thenReturn(new StoreInfo((int) FILE_FOR_UPLOADING.getSize(), "new.feed.url"));

        final long datasourceId = 79620;
        var feedUpload = new FeedUpload(datasourceId, "new.feed.name", new Date());
        var feedUploadResult = feedService.uploadFeed(feedUpload, FILE_FOR_UPLOADING);
        assertThat(feedUploadResult.getSize())
                .isNotNull()
                .isEqualTo(FILE_FOR_UPLOADING.getSize());
        assertThat(feedUploadResult.getUrl()).isEqualTo("new.feed.url");
    }

    /**
     * После добавления фида отправится PartnerParsingFeedEvent.
     */
    @Test
    @DbUnitDataSet(
            before = "testPartnerParsingFeedEvent.before.csv"
    )
    void testPartnerParsingFeedEvent() {
        var feedInfo = new FeedInfo();
        feedInfo.setDefault(false);
        feedInfo.setEnabled(true);
        feedInfo.setSiteType(FeedSiteType.MARKET);
        feedInfo.setDatasourceId(100L);
        feedInfo.setUrl("http://url.url");
        feedService.createFeed(feedInfo, FeedParsingType.COMPLETE_FEED, 100500);

        var eventCaptor =
                ArgumentCaptor.forClass(PartnerParsingFeedEvent.class);
        verify(dataCampCreateUpdateFeedEventListener, times(1)).onApplicationEvent(eventCaptor.capture());
        var actual = eventCaptor.getValue();

        assertThat(actual.getBusinessId()).isEqualTo(11L);
        assertThat(actual.getPartnerId()).isEqualTo(100L);
        assertThat(actual.getFeedId()).isEqualTo(1L);
    }

    /**
     * После добавления фида отправится PartnerParsingFeedEvent.
     */
    @Test
    @DbUnitDataSet(
            before = "testPartnerParsingFeedEvent.before.csv"
    )
    void testPartnerParsingFeedEventWithParsingFields() {
        var feedInfo = new FeedInfo();
        feedInfo.setDefault(false);
        feedInfo.setEnabled(true);
        feedInfo.setSiteType(FeedSiteType.MARKET);
        feedInfo.setDatasourceId(100L);
        feedInfo.setUrl("http://url.url");
        feedService.createFeed(feedInfo, FeedParsingType.COMPLETE_FEED, List.of("id", "price", "adult"), 100500);

        var eventCaptor =
                ArgumentCaptor.forClass(PartnerParsingFeedEvent.class);
        verify(dataCampCreateUpdateFeedEventListener, times(1)).onApplicationEvent(eventCaptor.capture());
        var actual = eventCaptor.getValue();

        assertThat(actual.getBusinessId()).isEqualTo(11L);
        assertThat(actual.getPartnerId()).isEqualTo(100L);
        assertThat(actual.getFeedId()).isEqualTo(1L);
        assertThat(actual.getParsingFields()).isEqualTo(List.of("id", "price", "adult"));
    }

    @Test
    @DbUnitDataSet(before = "testGetTemplateDatasourceFeed.csv")
    void testGetTemplateDatasourceFeed() {
        assertThat(feedService.getDatasourceUploadTemplateFeed(774L, MarketTemplate.ALCOHOL))
                .isEmpty();

        var feed = feedService.getDatasourceUploadTemplateFeed(774L, MarketTemplate.COMMON);
        assertThat(feed)
                .as("в дальнейшем будем искать только по шаблону, поэтому upload ids это fallback")
                .contains(buildFeed(
                        1L, 774L, true,
                        1234L, MarketTemplate.COMMON,
                        "test.feed.url",
                        FeedSiteType.MARKET
                ));
    }

    /**
     * Тест проверяет что после обновления фида обновится дата загрузки.<p>
     * {@link FeedService#getFeed(long) получение фида}
     * {@link FeedService#getFeedUpload(long) получение загруженного фида}
     */
    @Test
    @DbUnitDataSet(before = "testUpdateUploadDate.csv")
    void updateUploadDate() {
        var feedInfo = feedService.getFeed(1);
        var before = feedService.getFeedUpload(1234).orElseThrow().getUploadDate();
        feedService.updateFeed(feedInfo, FeedParsingType.COMPLETE_FEED, 1);
        var after = feedService.getFeedUpload(1234).orElseThrow().getUploadDate();
        assertThat(after).isAfter(before);
    }

    @DbUnitDataSet(before = "testUpdateUploadDate.csv")
    @Test
    @DisplayName("Попытка обновления фида с типом UPDATE_FEED для фида по ссылке")
    void updateFeed_feedByUrlWithUpdateFeedType_exception() {
        var feedInfo = feedService.getFeed(2L);
        assertThatExceptionOfType(FeedUploadException.class)
                .isThrownBy(() -> feedService.updateFeed(feedInfo, FeedParsingType.UPDATE_FEED, 1));
    }

    @Test
    @DbUnitDataSet(before = "getFeeds.csv")
    void getFeeds() {
        var feeds = feedService.getFeeds(Arrays.asList(2L, 3L, 5L, 8L));
        assertThat(feeds)
                .containsExactlyInAnyOrder(
                        buildFeed(2L, 2L, false, 5L, MarketTemplate.COMMON, "url2.ru",
                           FeedSiteType.MARKET),
                        buildFeed(3L, 3L, true, 4L, MarketTemplate.COMMON, "url3.ru",
                           FeedSiteType.MARKET),
                        buildFeed(5L, 4L, false, 3L, MarketTemplate.COMMON, "url4.ru",
                           FeedSiteType.MARKET),
                        buildFeed(8L, 5L, true, 2L, MarketTemplate.COMMON, "url5.ru",
                           FeedSiteType.MARKET)
                );
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private static FeedInfo buildFeed(
            long feedId, long shopId, boolean isEnabled,
            Long uploadId, MarketTemplate uploadTemplate,
            String url, FeedSiteType feedSiteType
    ) {
        return buildFeed(feedId, shopId, isEnabled, uploadId, uploadTemplate, url, null, null, feedSiteType, null);
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private static FeedInfo buildFeed(
            long feedId, long shopId, boolean isEnabled,
            Long uploadId, MarketTemplate uploadTemplate,
            String url, FeedSiteType feedSiteType,
            Integer reparseIntervalMinutes
    ) {
        return buildFeed(feedId, shopId, isEnabled, uploadId, uploadTemplate, url,
                 null, null, feedSiteType, reparseIntervalMinutes);
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private static FeedInfo buildFeed(
            long feedId, long shopId, boolean isEnabled,
            Long uploadId, MarketTemplate uploadTemplate,
            String url, String login, String password, FeedSiteType feedSiteType,
            Integer reparseIntervalMinutes
    ) {
        var feedInfo = new FeedInfo();
        feedInfo.setId(feedId);
        feedInfo.setDatasourceId(shopId);
        feedInfo.setEnabled(isEnabled);
        feedInfo.setUpload(uploadId, uploadTemplate);
        feedInfo.setUrl(url);
        feedInfo.setLogin(login);
        feedInfo.setPassword(password);
        feedInfo.setSiteType(feedSiteType);
        feedInfo.setReparseIntervalMinutes(reparseIntervalMinutes);
        return feedInfo;
    }

    @Test
    @DbUnitDataSet(before = "getDatasourcesFeedsNoDefaultFeed.csv")
    void getDatasourcesFeedsWithoutDefaultFeed() {
        var datasourceIds = List.of(101L, 102L, 103L);
        var expectedFeeds = Set.of(12L, 13L);
        var actualFeeds = feedService.getDatasourcesFeeds(datasourceIds, false)
                .values()
                .stream()
                .flatMap(Collection::stream)
                .map(FeedInfo::getId)
                .collect(Collectors.toSet());

        assertThat(actualFeeds).isEqualTo(expectedFeeds);
    }

    @Test
    @DisplayName("Пуш не может добавить себе второй аплоадный фид")
    @DbUnitDataSet(before = "pushPartnerWithUploadFeed.before.csv")
    void pushPartner_secondUploadFeed_limitException() {
        var feedInfo = new FeedInfo();
        feedInfo.setUrl("http://new-url.ru");
        feedInfo.setDatasourceId(101L);
        feedInfo.setUpload(11L, null);
        feedInfo.setEnabled(true);
        feedInfo.setDefault(false);
        feedInfo.setSiteType(FeedSiteType.MARKET);
        assertThatExceptionOfType(TooManyFeedsException.class)
                .isThrownBy(() -> feedService.createFeed(feedInfo, FeedParsingType.COMPLETE_FEED, 1));
    }

    @Test
    @DisplayName("Пуш может обновить себе аплоадный фид, даже если их несколько")
    @DbUnitDataSet(
            before = {
                    "pushPartnerWithUploadFeed.before.csv",
                    "secondUploadFeed.before.csv"
            },
            after = "pushPartnerWithTwoUploadFeed.update.after.csv"
    )
    void pushPartnerWithTwoUploadFeed_updateFeed_success() {
        var feedInfo = new FeedInfo();
        feedInfo.setId(12L);
        feedInfo.setUrl("http://url3.ru");
        feedInfo.setUpload(10L, null);
        feedInfo.setDatasourceId(101L);
        feedInfo.setEnabled(true);
        feedInfo.setDefault(false);
        feedInfo.setSiteType(FeedSiteType.MARKET);
        feedService.updateFeed(feedInfo, FeedParsingType.COMPLETE_FEED, 100500);
    }

    @Test
    @DisplayName("Включена схема мультифидовости. Пуш может добавить второй фид")
    @DbUnitDataSet(
            before = "checkPushPartnerFeedsCountLimitExceeded.csv",
            after = "checkPushPartnerFeedsNumberForPush.after.csv"
    )
    void checkPushPartnerFeedsNumberForPush() {
        var feedInfo = new FeedInfo();
        feedInfo.setUrl("http://new-url.ru");
        feedInfo.setDatasourceId(101L);
        feedInfo.setEnabled(true);
        feedInfo.setDefault(false);
        feedInfo.setSiteType(FeedSiteType.MARKET);
        feedInfo.setReparseIntervalMinutes(60);
        feedService.createFeed(feedInfo, FeedParsingType.COMPLETE_FEED, 1);
    }

    @Test
    @DisplayName("Автотестовый магазин может добавить себе больше 30 фидов")
    @DbUnitDataSet(
            before = {
                    "checkPushPartnerFeedsCountLimitExceeded.csv",
                    "feedLimit.extended.csv"
            },
            after = "feedLimit.extended.after.csv"
    )
    void checkExtendedList() {
        var feedInfo = new FeedInfo();
        feedInfo.setDatasourceId(101L);
        feedInfo.setEnabled(true);
        feedInfo.setDefault(false);
        feedInfo.setSiteType(FeedSiteType.MARKET);
        feedInfo.setReparseIntervalMinutes(60);

        for (int i = 0; i <= 31; ++i) {
            feedInfo.setUrl("http://new-url.ru/" + i);
            feedService.createFeed(feedInfo, FeedParsingType.COMPLETE_FEED, 1);
        }
    }

    @Test
    @DbUnitDataSet(before = "checkDefaultFeedDoesNotCountToLimit.csv")
    @DisplayName("Проверка, что дефолтный фид не учитывается при подсчете фидов")
    void checkDefaultFeedDoesNotCountToLimit() {
        var feedInfo = new FeedInfo();
        feedInfo.setUrl("http://new-url.ru");
        feedInfo.setDatasourceId(101L);
        feedInfo.setEnabled(true);
        feedInfo.setDefault(false);
        feedInfo.setSiteType(FeedSiteType.MARKET);

        feedService.createFeed(feedInfo, FeedParsingType.COMPLETE_FEED, 1);
    }

    @Test
    @DbUnitDataSet(before = "checkPullPartnerFeedsCountLimitExceeded.csv")
    @DisplayName("Попытка добавления фида сверх лимита pull-партнером")
    void checkPullPartnerFeedsCountLimit() {
        var feedInfo = new FeedInfo();
        feedInfo.setUrl("http://new-url.ru");
        feedInfo.setDatasourceId(101L);
        feedInfo.setEnabled(true);
        feedInfo.setDefault(false);
        feedInfo.setSiteType(FeedSiteType.MARKET);

        assertThatExceptionOfType(TooManyFeedsException.class)
                .isThrownBy(() -> feedService.createFeed(feedInfo, FeedParsingType.COMPLETE_FEED, 1));
    }

    @Test
    @DbUnitDataSet(before = "checkPullPartnerFeedsCountLimitExceeded.csv")
    @DisplayName("Проверка отсутствия лимита на добавление дефолтного фида")
    void checkLimitNotExceededByDefaultFeedCreation() {
        var feedInfo = new FeedInfo();
        feedInfo.setUrl(null);
        feedInfo.setDatasourceId(101L);
        feedInfo.setEnabled(true);
        feedInfo.setDefault(true);
        feedInfo.setSiteType(FeedSiteType.MARKET);

        feedService.createFeed(feedInfo, FeedParsingType.COMPLETE_FEED, 1);
    }

    @Test
    @DisplayName("Получить список фидов по ссылке для пуш-партнеров")
    @DbUnitDataSet(before = "testGetAllFeedsWithPushSchema.before.csv")
    void testGetAllFeedsWithPushSchema() {
        checkFeedsForSamovar(List.of(12L, 14L, 18L, 21L));
    }

    private void checkFeedsForSamovar(List<Long> expectedIds) {
        var actual = feedService.getEnabledMarketFeedsForSamovar(CampaignType.SHOP, FeedSiteType.MARKET);

        var actualIds = actual.stream()
                .map(FeedInfo::getId)
                .collect(Collectors.toList());

        assertThat(actualIds)
                .containsExactlyInAnyOrderElementsOf(expectedIds);
    }

    @Test
    @DisplayName("Выключенный, но живой фид обходится каждые 3 часа, если не перегружено в environment")
    @DbUnitDataSet(before = "testGetAllFeedsWithPushSchema.before.csv")
    void testDisabledButAliveFeed() {
        var allFeeds = feedService.getEnabledMarketFeedsForSamovar(CampaignType.SHOP, FeedSiteType.MARKET);
        var actualFeed = allFeeds.stream().filter(e -> e.getId().equals(21L)).findFirst();

        assertThat(actualFeed)
                .map(FeedInfo::getReparseIntervalMinutes)
                .contains(3 * 60);
    }

    @Test
    @DisplayName("Период обхода выключенного, но живого фида берется из samovar.inactive.feed.period.minutes")
    @DbUnitDataSet(before = {
            "testGetAllFeedsWithPushSchema.before.csv",
            "samovarCustomPeriod.before.csv"
    })
    void testDisabledButAliveFeedCustomPeriod() {
        List<FeedInfo> allFeeds = feedService.getEnabledMarketFeedsForSamovar(CampaignType.SHOP, FeedSiteType.MARKET);
        Optional<FeedInfo> actualFeed = allFeeds.stream().filter(e -> e.getId().equals(21L)).findFirst();

        assertThat(actualFeed)
                .map(FeedInfo::getReparseIntervalMinutes)
                .contains(600);
    }

    @Test
    @DisplayName("Выключенный и не живой фид не обходится")
    @DbUnitDataSet(before = "testGetAllFeedsWithPushSchema.before.csv")
    void testDisabledAndNotAliveFeed() {
        var allFeeds = feedService.getEnabledMarketFeedsForSamovar(CampaignType.SHOP, FeedSiteType.MARKET);

        assertThat(allFeeds.stream().map(FeedInfo::getId))
                .doesNotContain(22L);
    }

    @Test
    @DisplayName("Получить список фидов по ссылке для пуш-партнеров Директа")
    @DbUnitDataSet(before = "testGetAllFeedsWithPushSchema.before.csv")
    void testGetAllDirectFeedsWithPushSchema() {
        var actual = feedService.getEnabledMarketFeedsForSamovar(CampaignType.DIRECT, FeedSiteType.MARKET);

        assertThat(actual.stream().map(FeedInfo::getId))
                .containsExactlyInAnyOrder(20L);
    }

    @Test
    @DisplayName("Получить список фидов Директа для Самовара")
    @DbUnitDataSet(before = "testGetAllFeedsWithPushSchema.before.csv")
    void testGetAllDirectFeedsForSamovar() {
        var actual = feedService.getDirectFeedsForSamovar(FeedSiteType.MARKET);

        assertThat(actual.get(0).getId()).isEqualTo(20L);
    }

    @Test
    @DisplayName("Получить фид по ссылке для пуш-партнера магазина  по id")
    @DbUnitDataSet(before = "testGetAllFeedsWithPushSchema.before.csv")
    void getShopFeedWithPushSchemaById_existFeed_present() {
        var actual = feedService.getFeedWithPushSchemaById(CampaignType.SHOP, FeedSiteType.MARKET, 12L);
        assertThat(actual).isPresent();
        assertThat(actual.get().getId()).isEqualTo(12L);
    }

    @Test
    @DisplayName("Получить фид по ссылке для пуш-партнера директа по id")
    @DbUnitDataSet(before = "testGetAllFeedsWithPushSchema.before.csv")
    void getDirectFeedWithPushSchemaById_existFeed_present() {
        var actual = feedService.getFeedWithPushSchemaById(CampaignType.DIRECT, FeedSiteType.MARKET, 20L);
        assertThat(actual).isPresent();
        assertThat(actual.get().getId()).isEqualTo(20L);
    }

    @Test
    @DisplayName("Получить фид по ссылке для обкачки сайта по id")
    @DbUnitDataSet(before = "testSitesForParsing.before.csv")
    void getFeedForSiteParsingById_existFeed_present() {
        var actual = feedService.getFeedForSiteParsingById(3L);
        assertThat(actual).isPresent();
        assertThat(actual.get().getId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("Отсутсвует фид по ссылке для пуш-партнера магазина по id")
    @DbUnitDataSet(before = "testGetAllFeedsWithPushSchema.before.csv")
    void getShopFeedWithPushSchemaById_notPushFeed_empty() {
        var actual = feedService.getFeedWithPushSchemaById(CampaignType.SHOP, FeedSiteType.MARKET, 11L);
        assertThat(actual).isEmpty();
    }

    @Test
    @DisplayName("Отсутсвует фид по ссылке для пуш-партнера магазина по id")
    @DbUnitDataSet(before = "testGetAllFeedsWithPushSchema.before.csv")
    void getDirectFeedWithPushSchemaById_notPushFeed_empty() {
        var actual = feedService.getFeedWithPushSchemaById(CampaignType.DIRECT, FeedSiteType.MARKET, 12L);
        assertThat(actual).isEmpty();
    }

    @Test
    @DisplayName("Отсутсвует фид по ссылке для обкачки сайта по id")
    @DbUnitDataSet(before = "testSitesForParsing.before.csv")
    void getFeedForSiteParsingById_disabled_empty() {
        var actual = feedService.getFeedForSiteParsingById(2L);
        assertThat(actual).isEmpty();
    }

    @Test
    @DisplayName("Получить фиды по ссылке для пуш-партнера по id магазина")
    @DbUnitDataSet(before = "testGetAllFeedsWithPushSchema.before.csv")
    void getFeedsWithPushSchemaByShopId_existFeeds_present() {
        var actual = feedService.getFeedsWithPushSchemaByShopId(103L);
        assertThat(actual)
                .containsExactlyInAnyOrder(
                        buildFeed(14L, 103L, true, null, null, "test.feed.url", "test.login", "test.password",
                                FeedSiteType.MARKET, null),
                        buildFeed(15L, 103L, true, 101L, MarketTemplate.ALCOHOL, "test.feed.url", "test.login", "test" +
                                ".password", FeedSiteType.MARKET, null),
                        buildFeed(18L, 103L, true, null, null, "test2.feed.url", null, null, FeedSiteType.MARKET, null)
                );
    }

    @Test
    @DisplayName("Получить фиды по ссылке для пуш-партнера по id магазина")
    @DbUnitDataSet(before = "testSitesForParsing.before.csv")
    void getAllFeedsForSiteParsing_existFeeds_present() {
        var actual = feedService.getEnabledMarketFeedsForSamovar(CampaignType.DIRECT, FeedSiteType.SITE_PARSING);
        assertThat(actual)
                .containsExactlyInAnyOrder(
                        buildFeed(3L, 45L, true, null, null, "http://test.site.url/",
                                FeedSiteType.SITE_PARSING, 180),
                        buildFeed(4L, 45L, true, null, null, "http://test.site4.url/",
                                FeedSiteType.SITE_PARSING, 180)
                );
    }

    @Test
    @DisplayName("Отсутсвуют фиды по ссылке для пуш-партнера по id магазина")
    @DbUnitDataSet(before = "testGetAllFeedsWithPushSchema.before.csv")
    void getFeedsWithPushSchemaByShopId_notPushFeeds_empty() {
        var actual = feedService.getFeedsWithPushSchemaByShopId(101L);
        assertThat(actual).isEmpty();
    }

    @Test
    @DisplayName("Попытка добавления фида с типом UPDATE_FEED для фида по ссылке")
    void createFeed_feedByUrlWithUpdateFeedType_exception() {
        var feedInfo = new FeedInfo();
        feedInfo.setUrl("http://new-url.ru");
        feedInfo.setDatasourceId(101L);
        feedInfo.setEnabled(true);
        feedInfo.setDefault(false);
        feedInfo.setSiteType(FeedSiteType.MARKET);

        assertThatExceptionOfType(FeedUploadException.class)
                .isThrownBy(() -> feedService.createFeed(feedInfo, FeedParsingType.UPDATE_FEED, 1));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testPushPartnerRestrictionsData")
    @DisplayName("Проверка режимов работы с ассортиментов (добавление аплоадных фидов и фидов по ссылке)")
    @DbUnitDataSet(before = "testPushPartnerCreationRestrictions.before.csv")
    void testPushPartnerCreationRestrictions(String name, FeedInfo feedInfo, boolean exceptionExpected) {
        if (!exceptionExpected) {
            feedService.createFeed(feedInfo, FeedParsingType.COMPLETE_FEED, 1);
            return;
        }

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> feedService.createFeed(feedInfo, FeedParsingType.COMPLETE_FEED, 1));
    }

    private static Stream<Arguments> testPushPartnerRestrictionsData() {
        return Stream.of(
                Arguments.of(
                        "Pull магазин. Добавляем аплоадный, когда уже есть ссылочный",
                        createUploadFeed(0, 101, 1101),
                        false
                ),
                Arguments.of(
                        "Push магазин. Добавляем первый фид. По файлу",
                        createUploadFeed(0, 102, 1102),
                        false
                ),
                Arguments.of(
                        "Push магазин. Добавляем первый фид. По ссылке",
                        createUrlFeed(0, 102),
                        false
                ),
                Arguments.of(
                        "Push магазин. Есть аплоадный фид. Добавляем ссылочный",
                        createUrlFeed(0, 103),
                        true
                ),
                Arguments.of(
                        "Push магазин. Есть ссылочный фид. Добавляем аплоадный",
                        createUploadFeed(0, 104, 1104),
                        true
                )
        );
    }

    @Test
    @DisplayName("Замена дефолтного фида основным для push партнера")
    @DbUnitDataSet(
            before = "testPushPartnerCreationRestrictions.before.csv",
            after = "updatePushDefaultFeed.after.csv"
    )
    void updateFeed_isDefault_successful() {
        var urlFeed = createUrlFeed(2001, 101);

        feedService.updateFeed(urlFeed, FeedParsingType.COMPLETE_FEED, 1);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testPushPartnerUpdatingRestrictionsData")
    @DisplayName("Проверка режимов работы с ассортиментов (обновление аплоадных фидов и фидов по ссылке)")
    @DbUnitDataSet(before = "testPushPartnerCreationRestrictions.before.csv")
    void testPushPartnerUpdatingRestrictions(String name, FeedInfo feedInfo, boolean exceptionExpected) {

        if (!exceptionExpected) {
            feedService.updateFeed(feedInfo, FeedParsingType.COMPLETE_FEED, 1);
            return;
        }

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> feedService.updateFeed(feedInfo, FeedParsingType.COMPLETE_FEED, 1));
    }

    private static Stream<Arguments> testPushPartnerUpdatingRestrictionsData() {
        return Stream.of(
                Arguments.of(
                        "Pull магазин. Обновляем ссылочный на аплоадный",
                        createUploadFeed(2002, 101, 1101),
                        false
                ),
                Arguments.of(
                        "Push магазин. Обновляем ссылочный на аплоадный",
                        createUploadFeed(2005, 104, 1104),
                        true
                ),
                Arguments.of(
                        "Push магазин. Обновляем единственный аплоадный на ссылочный",
                        createUrlFeed(2004, 103),
                        false
                ),
                Arguments.of(
                        "Push магазин. Несколько аплоадных. Обновляем один из них на ссылочный",
                        createUrlFeed(2006, 105),
                        true
                )
        );
    }

    private static FeedInfo createUploadFeed(long feedId, long partnerId, long uploadId) {
        var upload = new FeedUpload();
        upload.setId(uploadId);
        upload.setSize(111);
        upload.setUrl("http://mds.ru");
        upload.setDatasourceId(partnerId);
        upload.setUploadDate(new Date());
        return buildFeed(feedId, partnerId, true, uploadId, null, upload.getUrl(),
               FeedSiteType.MARKET, 180);
    }

    private static FeedInfo createUrlFeed(long feedId, long partnerId) {
        return buildFeed(feedId, partnerId, true, null, null,
                "http://nowhere.ru", FeedSiteType.MARKET, 180);
    }


    @DisplayName("Проверка url на корректность")
    @ParameterizedTest
    @CsvSource({
            "http://ya.ru/feed1, true",
            "https://ya.ru/feed1, true",
            "http://ya.ru:1234/feed1, true",
            "blablalba, false",
            "http://идн-тест.яндекс.рф/mptest/фиды/smallsvyaznoy05.xml, true"
    })
    void testIsValidFeedURL(String url, boolean expected) {
        boolean actual = FeedService.isValidFeedURL(url);
        assertThat(actual).isEqualTo(expected);
    }
}
