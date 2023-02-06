package ru.yandex.market.direct.feed;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.direct.feed.DirectSitePreviewDao;
import ru.yandex.market.core.direct.feed.model.DirectSitePreviewData;
import ru.yandex.market.core.direct.feed.model.FeedIds;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.shop.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;

public class DirectSitePreviewDaoTest extends FunctionalTest {

    @Autowired
    private DirectSitePreviewDao directSitePreviewDao;
    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;
    @Autowired
    private EnvironmentService environmentService;

    @Test
    @DbUnitDataSet(before = "DirectSitePreviewDao.before.csv",
            after = "DirectSitePreviewDao.after.csv")
    public void testInsert() {
        DirectSitePreviewData result =
                directSitePreviewDao.upsert("https://yandex.ru", 7);
        assertThat(result.getNumOffers()).isEqualTo(7);
    }

    @Test
    @DbUnitDataSet(before = "DirectSitePreviewDao.before.csv",
            after = "DirectSitePreviewDao.update.after.csv")
    public void testUpdate() {
        DirectSitePreviewData result =
                directSitePreviewDao.upsert("http://ya.ru", 11);
        assertThat(result.getNumOffers()).isEqualTo(11);
    }

    @Test
    @DbUnitDataSet(before = "DirectSitePreviewDao.before.csv",
            after = "DirectSitePreviewDao.before.csv")
    public void testExisting() {
        DirectSitePreviewData result =
                directSitePreviewDao.upsert("http://ya.ru", 10);
        assertThat(result.getNumOffers()).isEqualTo(10);
        assertThat(result.getBusinessId()).isEqualTo(11);
        assertThat(result.getPartnerId()).isEqualTo(21);
        assertThat(result.getFeedId()).isEqualTo(31);
    }

    @Test
    @DbUnitDataSet(before = "DirectSitePreviewDao.before.csv")
    public void testLimit() {
        environmentService.setValue(DirectSitePreviewDao.ENV_MAX_ROWS_FOR_DIRECT_SITE_PREVIEW, "0");

        var dao = new DirectSitePreviewDao(jdbcTemplate, environmentService);

        Assertions.assertThrows(IllegalStateException.class, () ->
                dao.upsert("http://someurl.me", 22)
        );
    }

    @Test
    @DbUnitDataSet(before = "DirectSitePreviewDao.delete.before.csv",
            after = "DirectSitePreviewDao.delete.before.csv")
    public void testSelectByPartnerAndFeedIds() {
        List<FeedIds> feedIds = List.of(FeedIds.builder().setPartnerId(23).setFeedId(33).build(),
                FeedIds.builder().setPartnerId(24).setFeedId(34).build(),
                FeedIds.builder().setPartnerId(20).setFeedId(30).build(),
                FeedIds.builder().setPartnerId(21).setFeedId(32).build());

        Map<Long, DirectSitePreviewData> result = directSitePreviewDao.selectByPartnerAndFeedIds(feedIds);

        assertThat(result).hasSize(2);
        assertThat(result).containsKey(1002L);
        assertThat(result.get(1002L).getUrl()).isEqualTo("https://domain.com");
        assertThat(result).containsKey(1003L);
        assertThat(result.get(1003L).getUrl()).isEqualTo("http://abc.com");
    }

    @Test
    @DbUnitDataSet(before = "DirectSitePreviewDao.delete.before.csv",
            after = "DirectSitePreviewDao.delete.after.csv")
    public void testDelete() {
        List<FeedIds> feedIds = List.of(FeedIds.builder().setPartnerId(23).setFeedId(33).build(),
                FeedIds.builder().setPartnerId(24).setFeedId(34).build(),
                FeedIds.builder().setPartnerId(20).setFeedId(30).build(),
                FeedIds.builder().setPartnerId(21).setFeedId(32).build());

        Map<Long, DirectSitePreviewData> result = directSitePreviewDao.deleteByPartnerAndFeedIds(feedIds);

        assertThat(result).hasSize(2);
    }
}
