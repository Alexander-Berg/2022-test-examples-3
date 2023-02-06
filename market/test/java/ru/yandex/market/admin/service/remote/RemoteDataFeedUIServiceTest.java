package ru.yandex.market.admin.service.remote;

import java.util.List;

import javax.annotation.Nonnull;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.admin.FunctionalTest;
import ru.yandex.market.admin.ui.err.InvalidFeedURLException;
import ru.yandex.market.admin.ui.model.StringID;
import ru.yandex.market.admin.ui.model.feed.UIDataFeed;
import ru.yandex.market.admin.ui.model.feed.UIFeedSiteType;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit-тесты для {@link RemoteDataFeedUIService}.
 *
 * @author fbokovikov
 */
class RemoteDataFeedUIServiceTest extends FunctionalTest {

    @Autowired
    private RemoteDataFeedUIService remoteDataFeedUIService;

    @DisplayName("Запрещается создавать не фид по ссылке.")
    @Test
    void createFeed_uploadUrl_exception() {
        assertThrows(
                InvalidFeedURLException.class,
                () -> remoteDataFeedUIService.createFeed(
                        79620L,
                        uIFeed("http://market-mbi-test.s3.mdst.yandex.net/upload-feed/100/upload-feed-123456",
                                79620L, UIFeedSiteType.RED_MARKET)
                )
        );
    }

    @DisplayName("Создаем фид для PULL партнера.")
    @Test
    @DbUnitDataSet(
            before = "RemoteDataFeedUIService/csv/partnerSettings.csv",
            after = "RemoteDataFeedUIService/csv/createPullFeed.after.csv"
    )
    void createFeed_urlPull_successful() {
        UIDataFeed uiFeedInfo = uIFeed("http://aga.net/xls.yml", 983L, UIFeedSiteType.MARKET);
        Assertions.assertThat(remoteDataFeedUIService.createFeed(983L, uiFeedInfo))
                .isEqualTo(1);
    }

    @DisplayName("Создаем фид для ДБС партнера.")
    @Test
    @DbUnitDataSet(
            before = {
                    "RemoteDataFeedUIService/csv/partnerSettings.csv",
                    "RemoteDataFeedUIService/csv/createDsbsFeed.before.csv"
            },
            after = "RemoteDataFeedUIService/csv/createDsbsFeed.after.csv"
    )
    void createFeed_urlDbs_successful() {
        UIDataFeed uiFeedInfo = uIFeed("http://aga.net/xls.yml", 893L, UIFeedSiteType.MARKET);
        Assertions.assertThat(remoteDataFeedUIService.createFeed(893L, uiFeedInfo))
                .isEqualTo(12L);
    }

    @DisplayName("Создаем фид для PUSH партнера.")
    @Test
    @DbUnitDataSet(
            before = {
                    "RemoteDataFeedUIService/csv/partnerSettings.csv",
                    "RemoteDataFeedUIService/csv/createPushFeed.before.csv"
            },
            after = "RemoteDataFeedUIService/csv/createPushFeed.after.csv"
    )
    void createFeed_urlPush_successful() {
        UIDataFeed uiFeedInfo = uIFeed("http://aga.net/xls.yml", 893L, UIFeedSiteType.MARKET);
        Assertions.assertThat(remoteDataFeedUIService.createFeed(893L, uiFeedInfo))
                .isEqualTo(12);
    }

    @DisplayName("Создаем второй фид для PUSH партнера.")
    @Test
    @DbUnitDataSet(
            before = {
                    "RemoteDataFeedUIService/csv/partnerSettings.csv",
                    "RemoteDataFeedUIService/csv/createPushMultiFeed.before.csv"
            },
            after = "RemoteDataFeedUIService/csv/createPushMultiFeed.after.csv"
    )
    void createFeed_urlPushMulti_successful() {
        UIDataFeed uiFeedInfo = uIFeed("http://aga.net/xls.yml", 893L, UIFeedSiteType.MARKET);
        Assertions.assertThat(remoteDataFeedUIService.createFeed(893L, uiFeedInfo))
                .isEqualTo(1);
    }

    @DisplayName("Для магазина возвращаются все фиды, кроме дефолтного.")
    @Test
    @DbUnitDataSet(before = "RemoteDataFeedUIService/csv/getDatasourceFeeds.before.csv")
    void getDatasourceFeeds_byDatasourceId_withoutDefault() {
        List<UIDataFeed> feeds = remoteDataFeedUIService.getDatasourceFeeds(3031L);
        assertTrue(feeds.size() == 1 && feeds.get(0).getIntegerField(new StringID("ID")) == 1);
    }

    @Nonnull
    @SuppressWarnings("SameParameterValue")
    private UIDataFeed uIFeed(String url, long shopId, UIFeedSiteType siteType) {
        UIDataFeed uiDataFeed = new UIDataFeed();
        uiDataFeed.setField(UIDataFeed.URL, url);
        uiDataFeed.setField(UIDataFeed.DATASOURCE_ID, shopId);
        uiDataFeed.setField(UIDataFeed.SITE_TYPE, siteType);
        uiDataFeed.setField(UIDataFeed.ENABLED, true);
        return uiDataFeed;
    }
}
