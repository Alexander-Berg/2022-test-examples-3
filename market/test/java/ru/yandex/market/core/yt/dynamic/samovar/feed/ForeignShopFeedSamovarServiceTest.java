package ru.yandex.market.core.yt.dynamic.samovar.feed;

import java.util.List;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.campaign.model.PartnerId;
import ru.yandex.market.core.feed.model.FeedType;
import ru.yandex.market.yt.samovar.SamovarContextOuterClass;

/**
 * Тесты для {@link ForeignShopFeedSamovarService}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class ForeignShopFeedSamovarServiceTest extends FunctionalTest {

    @Autowired
    private ForeignShopFeedSamovarService foreignShopFeedSamovarService;

    @Test
    void testGetFeedForSingleDownload() {
        Optional<SamovarFeed> actual = foreignShopFeedSamovarService.getFeedForOneTimeDownload(100L, PartnerId.datasourceId(1L), FeedType.ASSORTMENT);
        Assertions.assertThat(actual)
                .isEmpty();
    }

    @Test
    void testGetFeedForRepeatedDownloads() {
        List<SamovarFeedInfo> actual = foreignShopFeedSamovarService.getFeedsForRepeatingDownload();
        Assertions.assertThat(actual)
                .isEmpty();
    }

    @Test
    @DbUnitDataSet(before = "ForeignShopFeedSamovarServiceTest/testSites.before.csv")
    void testGetSiteForRepeatedParings() {
        List<SamovarFeedInfo> actual = foreignShopFeedSamovarService.getSitesForParsing();
        Assertions.assertThat(actual)
                .hasSize(1);

        SamovarContextOuterClass.FeedInfo actualFeed = actual.get(0).toFeedInfo();
        SamovarContextOuterClass.FeedInfo expected = ProtoTestUtil.getProtoMessageByJson(
                SamovarContextOuterClass.FeedInfo.class,
                "ForeignShopFeedSamovarServiceTest/testSite.samovarTask.proto.json",
                getClass()
        );
        ProtoTestUtil.assertThat(actualFeed)
                .ignoringFieldsMatchingRegexes(".*updatedAt.*")
                .isEqualTo(expected);
    }

    @Test
    @DbUnitDataSet(before = "ForeignShopFeedSamovarServiceTest/testSites.before.csv")
    void testGetSiteForSingleParing() {
        Optional<SamovarFeed> actual = foreignShopFeedSamovarService.getSiteForParsing(10L);
        Assertions.assertThat(actual)
                .isPresent();

        SamovarContextOuterClass.FeedInfo actualFeed = actual.get().getContext().getFeeds(0);
        SamovarContextOuterClass.FeedInfo expected = ProtoTestUtil.getProtoMessageByJson(
                SamovarContextOuterClass.FeedInfo.class,
                "ForeignShopFeedSamovarServiceTest/testSite.samovarTask.proto.json",
                getClass()
        );
        ProtoTestUtil.assertThat(actualFeed)
                .ignoringFieldsMatchingRegexes(".*updatedAt.*")
                .isEqualTo(expected);
    }
}
