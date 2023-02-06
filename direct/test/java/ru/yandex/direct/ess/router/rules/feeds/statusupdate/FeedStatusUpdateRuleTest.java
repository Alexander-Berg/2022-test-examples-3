package ru.yandex.direct.ess.router.rules.feeds.statusupdate;

import java.math.BigInteger;
import java.util.List;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.binlog.model.Operation;
import ru.yandex.direct.ess.logicobjects.feeds.statusupdate.FeedStatusUpdateObject;
import ru.yandex.direct.ess.router.configuration.TestConfiguration;
import ru.yandex.direct.ess.router.testutils.FeedsChange;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.dbschema.ppc.Tables.FEEDS;
import static ru.yandex.direct.dbschema.ppc.enums.FeedsUpdateStatus.Done;
import static ru.yandex.direct.dbschema.ppc.enums.FeedsUpdateStatus.New;
import static ru.yandex.direct.ess.router.testutils.FeedsChange.createFeedsEvent;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfiguration.class)
public class FeedStatusUpdateRuleTest {
    private static final long CLIENT_ID = 1L;
    private static final long FEED_ID = 2L;

    @Autowired
    private FeedStatusUpdateRule rule;

    @Test
    public void testUpdateWithStatusChanges() {
        BigInteger feedId = BigInteger.valueOf(FEED_ID);
        FeedsChange feedsChange = new FeedsChange().withFeedId(feedId).withClientId(CLIENT_ID);
        feedsChange.addChangedColumn(FEEDS.UPDATE_STATUS, New.getLiteral(), Done.getLiteral());
        var binlogEvent = createFeedsEvent(List.of(feedsChange), Operation.UPDATE);
        var got = rule.mapBinlogEvent(binlogEvent);
        var expected = new FeedStatusUpdateObject(CLIENT_ID);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(got).hasSize(1);
        soft.assertThat(got.get(0)).isEqualToComparingFieldByField(expected);
        soft.assertAll();
    }

    @Test
    public void testUpdateWithoutStatusChanges() {
        BigInteger feedId = BigInteger.valueOf(FEED_ID);
        FeedsChange feedsChange = new FeedsChange().withFeedId(feedId).withClientId(CLIENT_ID);
        feedsChange.addChangedColumn(FEEDS.UPDATE_STATUS, New.getLiteral(), New.getLiteral());
        var binlogEvent = createFeedsEvent(List.of(feedsChange), Operation.UPDATE);
        var got = rule.mapBinlogEvent(binlogEvent);
        assertThat(got).hasSize(0);
    }
}
