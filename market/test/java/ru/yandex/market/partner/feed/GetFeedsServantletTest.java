package ru.yandex.market.partner.feed;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.feed.model.FullFeedInfo;
import ru.yandex.market.core.feed.model.PushFeedInfo;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

class GetFeedsServantletTest extends FunctionalTest {

    @DisplayName("Проверка на тип загруженного фида и дату обновления")
    @DbUnitDataSet(before = "GetFeedsServantletTest.getFeeds.before.csv")
    @Test
    void getFeedTypes() {
        final var feeds1 = getPushFeedInfos(10774L);
        final PushFeedInfo feedA = feeds1.get(1234L);
        Assertions.assertTrue(feedA.getComplete() && feedA.getUpdateTime() == 1539380000000L);
        final PushFeedInfo feedB = feeds1.get(4321L);
        Assertions.assertTrue(!feedB.getComplete() && feedB.getUpdateTime() == 1539378000000L);

        final var feeds2 = getPushFeedInfos(10105L);
        final PushFeedInfo feedC = feeds2.get(1212L);
        Assertions.assertTrue(!feedC.getComplete() && feedC.getUpdateTime() == 1539380000000L);

        final var feeds3 = getResponseFeeds(1010777L);
        Assertions.assertTrue(feeds3.size() == 1 && feeds3.get(0).getPushFeedInfo() == null);
    }

    @Test
    @DbUnitDataSet(before = "GetFeedsServantletTest.testDefaultFeedNotReturned.csv")
    void testDefaultFeedNotReturned() {
        final Set<Long> expectedIds = Set.of(11L, 12L);
        Set<Long> resultIds = getResponseFeeds(1001L).stream()
                .map(FullFeedInfo::getFeedId)
                .collect(Collectors.toSet());

        Assertions.assertEquals(expectedIds, resultIds);
    }

    private List<FullFeedInfo> getResponseFeeds(long id) {
        return FunctionalTestHelper
                .get(baseUrl + "/getFeeds?format=json&id=" + id, GetFeedsResponse.class)
                .getBody()
                .getFeeds();
    }

    private Map<Long, PushFeedInfo> getPushFeedInfos(final long campaignId) {
        return getResponseFeeds(campaignId)
                .stream()
                .collect(Collectors.toMap(FullFeedInfo::getFeedId, FullFeedInfo::getPushFeedInfo));
    }

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.NONE)
    private static class GetFeedsResponse {
        @XmlElement(name = "result", required = true)
        private List<List<FullFeedInfo>> result;

        public List<FullFeedInfo> getFeeds() {
            return result.get(0);
        }
    }
}
