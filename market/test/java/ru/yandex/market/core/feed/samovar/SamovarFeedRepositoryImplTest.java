package ru.yandex.market.core.feed.samovar;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.campaign.model.PartnerId;
import ru.yandex.market.core.feed.model.FeedType;
import ru.yandex.market.core.misc.resource.ResourceAccessCredentials;
import ru.yandex.market.core.yt.dynamic.samovar.feed.SamovarFeed;
import ru.yandex.market.core.yt.dynamic.samovar.feed.SamovarFeedMapper;
import ru.yandex.market.yt.samovar.SamovarContextOuterClass;

import static ru.yandex.market.core.test.utils.SamovarFeedTestUtils.checkSamovarFeed;

/**
 * Date: 01.10.2020
 * Project: arcadia-market_mbi_mbi
 *
 * @author alexminakov
 */
class SamovarFeedRepositoryImplTest extends FunctionalTest {

    @Autowired
    private SamovarFeedRepository samovarFeedRepository;

    @DbUnitDataSet(before = "SamovarFeedRepositoryImplTest/getFeedById.before.csv")
    @DisplayName("Поиск фида, доступного для выгрузки через самовар")
    @ParameterizedTest(name = "feedId = {0} - auth = {2}")
    @CsvSource({
            "1,774,test.login,test.password",
            "4,774,,",
            "5,775,,"
    })
    void getFeedById_correctFeed_presentOptional(long feedId, long partnerId, String login, String password) {
        Optional<SamovarFeed> oSamovarFeed = samovarFeedRepository.getFeedById(feedId,
                PartnerId.datasourceId(partnerId), FeedType.ASSORTMENT);
        Assertions.assertTrue(oSamovarFeed.isPresent());

        SamovarContextOuterClass.SamovarContext samovarContext = ProtoTestUtil.getProtoMessageByJson(
                SamovarContextOuterClass.SamovarContext.class, "proto/shop.feed" + feedId + ".json", getClass()
        );
        SamovarFeed expectedFeed = SamovarFeed.builder()
                .setUrl("http://test.feed.url/")
                .setCredentials(login == null || password == null
                        ? null
                        : new ResourceAccessCredentials(login, password))
                .setPeriodMinutes(60)
                .setContext(samovarContext)
                .setTimeoutSeconds(SamovarFeedMapper.DEFAULT_TIMEOUT_SECONDS)
                .setEnabled(true)
                .build();
        checkSamovarFeed(expectedFeed, oSamovarFeed.get());
    }

    @DbUnitDataSet(before = "SamovarFeedRepositoryImplTest/getSiteById.before.csv")
    @DisplayName("Поиск сайта, доступного для парсинга через самовар")
    @ParameterizedTest(name = "feedId = {0}")
    @CsvSource({
            "3"
    })
    void getFeedById_correctFeed_presentOptional(long feedId) {
        Optional<SamovarFeed> oSamovarFeed = samovarFeedRepository.getSiteParsingFeedById(feedId);
        Assertions.assertTrue(oSamovarFeed.isPresent());

        SamovarContextOuterClass.SamovarContext samovarContext = ProtoTestUtil.getProtoMessageByJson(
                SamovarContextOuterClass.SamovarContext.class, "proto/shop.feed" + feedId + ".json", getClass()
        );

        SamovarFeed expectedFeed = SamovarFeed.builder()
                .setUrl("http://test.site.url/")
                .setPeriodMinutes(60)
                .setContext(samovarContext)
                .setTimeoutSeconds(SamovarFeedMapper.DEFAULT_TIMEOUT_SECONDS)
                .setEnabled(true)
                .build();
        checkSamovarFeed(expectedFeed, oSamovarFeed.get());
    }

    @SuppressWarnings({"unused", "RedundantSuppression"})
    @DbUnitDataSet(before = "SamovarFeedRepositoryImplTest/getSiteById.before.csv")
    @DisplayName("Поиск фида завершился ошибкой")
    @ParameterizedTest(name = "feedId = {0} - {1}")
    @CsvSource({
            "2,774,Фид имеет файл для скачивания",
            "3,774,Фид является дефолтным",
            "6,776,Фид находится в переключении в PULL схему",
            "7,777,Фид находится в схеме по умолчанию (PULL)"
    })
    void getFeedById_correctFeedWithoutCredentials_presentOptional(long id, long partnerId, String description) {
        Optional<SamovarFeed> oSamovarFeed = samovarFeedRepository.getFeedById(id,
                PartnerId.datasourceId(partnerId), FeedType.ASSORTMENT);
        Assertions.assertTrue(oSamovarFeed.isEmpty());
    }

    @SuppressWarnings({"unused", "RedundantSuppression"})
    @DbUnitDataSet(before = "SamovarFeedRepositoryImplTest/getSiteById.before.csv")
    @DisplayName("Поиск сайта завершился ошибкой")
    @ParameterizedTest(name = "feedId = {0} - {1}")
    @CsvSource({
            "1,FeedSiteType не SITE_PARSING",
            "2,Фид disabled",
            "4,Фида нет"
    })
    void getSiteById_correctFeedWithoutCredentials_presentOptional(long id, String description) {
        Optional<SamovarFeed> oSamovarFeed = samovarFeedRepository.getSiteParsingFeedById(id);
        Assertions.assertTrue(oSamovarFeed.isEmpty());
    }

}
