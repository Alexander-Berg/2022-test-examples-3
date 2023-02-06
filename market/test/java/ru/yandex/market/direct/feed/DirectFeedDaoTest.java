package ru.yandex.market.direct.feed;

import java.util.List;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.direct.feed.DirectFeedDao;
import ru.yandex.market.core.direct.feed.model.DirectFeedInfo;
import ru.yandex.market.shop.FunctionalTest;

/**
 * @author moskovkin@yandex-team.ru
 * @since 22.12.2020
 */
@DbUnitDataSet(
    before = "DirectFeedTest.csv"
)
public class DirectFeedDaoTest extends FunctionalTest {
    private static final long DIRECT_CLIENT_ID_1 = 11;
    private static final long DIRECT_FEED_1 = 12;
    private static final long DIRECT_OWNER_1 = 13;
    private static final long BUSINESS_ID_1 = 14;
    private static final long PARTNER_ID_1 = 15;
    private static final long FEED_ID_1 = 16;
    private static final long CLIENT_ID_UNKNOWN = 3333;
    private static final String URL_1 = "http://test.me/feed";

    private static final long DIRECT_CLIENT_ID_2 = 21;
    private static final long DIRECT_FEED_2 = 22;
    private static final long DIRECT_OWNER_2 = 23;
    private static final long BUSINESS_ID_2 = 24;
    private static final long PARTNER_ID_2 = 25;
    private static final long FEED_ID_2 = 26;
    private static final String URL_2 = "http://test.me/feed2";

    @Autowired
    private DirectFeedDao directFeedDao;

    @Test
    public void testFindDirectFeedInfo() {
        Optional<DirectFeedInfo> feedInfo = directFeedDao.getByClidAndDirectFeedId(DIRECT_CLIENT_ID_1, DIRECT_FEED_1);
        Assertions.assertThat(feedInfo).contains(
                DirectFeedInfo.builder()
                        .directClientId(DIRECT_CLIENT_ID_1)
                        .directFeedId(DIRECT_FEED_1)
                        .directOwnerUid(DIRECT_OWNER_1)
                        .feedId(FEED_ID_1)
                        .url(URL_1)
                        .build()
        );
    }

    @Test
    public void testNotFindDirectFeedInfo() {
        Optional<DirectFeedInfo> feedInfo = directFeedDao.getByClidAndDirectFeedId(CLIENT_ID_UNKNOWN, DIRECT_FEED_1);
        Assertions.assertThat(feedInfo).isEmpty();
    }

    @Test
    public void testFindByFeedInfoByFeedId() {
        Optional<DirectFeedInfo> feedInfo = directFeedDao.getByFeedId(FEED_ID_1);
        Assertions.assertThat(feedInfo).contains(
                DirectFeedInfo.builder()
                        .directClientId(DIRECT_CLIENT_ID_1)
                        .directFeedId(DIRECT_FEED_1)
                        .directOwnerUid(DIRECT_OWNER_1)
                        .feedId(FEED_ID_1)
                        .url(URL_1)
                        .build()
        );
    }

    @Test
    public void testNotFindDirectFeedInfoByFeedId() {
        Optional<DirectFeedInfo> feedInfo = directFeedDao.getByFeedId(FEED_ID_2);
        Assertions.assertThat(feedInfo).isEmpty();
    }

    @Test
    public void testCreateDirectFeedInfo() {
        Optional<DirectFeedInfo> before = directFeedDao.getByClidAndDirectFeedId(DIRECT_CLIENT_ID_2, DIRECT_FEED_2);
        Assertions.assertThat(before).isEmpty();

        DirectFeedInfo directFeedInfo = DirectFeedInfo.builder()
                .directClientId(DIRECT_CLIENT_ID_2)
                .directFeedId(DIRECT_FEED_2)
                .directOwnerUid(DIRECT_OWNER_2)
                .feedId(FEED_ID_2)
                .url(URL_2)
                .build();
        directFeedDao.createDirectFeedInfo(directFeedInfo);

        Optional<DirectFeedInfo> after = directFeedDao.getByClidAndDirectFeedId(DIRECT_CLIENT_ID_2, DIRECT_FEED_2);
        Assertions.assertThat(after).contains(directFeedInfo);
    }

    @Test
    public void testDirectFeedInfosByClientId() {
        List<DirectFeedInfo> feedInfos = directFeedDao.getDirectFeedInfosByClientId(DIRECT_CLIENT_ID_1);
        Assertions.assertThat(feedInfos)
                .containsExactly(
                    DirectFeedInfo.builder()
                        .directClientId(DIRECT_CLIENT_ID_1)
                        .directFeedId(DIRECT_FEED_1)
                        .directOwnerUid(DIRECT_OWNER_1)
                        .feedId(FEED_ID_1)
                        .url(URL_1)
                        .build()
                );
    }
}
