package ru.yandex.market.delivery.tracker.dao;

import java.sql.Date;
import java.time.Clock;
import java.util.Collections;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.tracker.AbstractContextualTest;
import ru.yandex.market.delivery.tracker.dao.repository.ConsumerDao;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

class ConsumerDaoTest extends AbstractContextualTest {

    private static final long TRACK_ID = 1L;
    private static final Set<Long> CHECKPOINTS_IDS = Collections.singleton(2L);
    private static final long CONSUMER_ID = 10L;
    private static final String BACK_URL = "shmurl";
    private static final long CONSUMER_ID_2 = 15L;
    private static final String BACK_URL_2 = "shmurl_2";

    @Autowired
    private ConsumerDao consumerDao;

    @Autowired
    private Clock clock;

    @Test
    @DatabaseSetup("/database/states/consumer/consumer_dao.xml")
    @ExpectedDatabase(
        value = "/database/expected/consumer/track_consumer_created.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void testConsumerAdded() {
        consumerDao.insertTrackConsumer(TRACK_ID, CONSUMER_ID, BACK_URL);
    }

    @Test
    @DatabaseSetup("/database/states/consumer/track_consumer_exists.xml")
    @ExpectedDatabase(
        value = "/database/expected/consumer/two_consumers_created.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void testSecondConsumerAdded() {
        consumerDao.insertTrackConsumer(TRACK_ID, CONSUMER_ID_2, BACK_URL_2);
    }

    @Test
    @DatabaseSetup("/database/states/consumer/track_consumer_exists.xml")
    @ExpectedDatabase(
        value = "/database/states/consumer/track_consumer_exists.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void testNotFailWhenDuplicateInserted() {
        consumerDao.insertTrackConsumer(TRACK_ID, CONSUMER_ID, BACK_URL);
    }

    @Test
    @DatabaseSetup("/database/states/consumer/consumer_dao.xml")
    @ExpectedDatabase(
        value = "/database/expected/consumer/checkpoint_consumer_created.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void testCheckpointConsumerAdded() {
        consumerDao.insertCheckpointNotifySuccessTs(CONSUMER_ID, Date.from(clock.instant()), CHECKPOINTS_IDS);
    }

    @Test
    @DatabaseSetup("/database/states/consumer/checkpoint_consumer_exists.xml")
    @ExpectedDatabase(
        value = "/database/expected/consumer/checkpoint_consumer_exists.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void testNothingHappensWhenCheckpointDuplicateInserted() {
        consumerDao.insertCheckpointNotifySuccessTs(CONSUMER_ID, Date.from(clock.instant()), CHECKPOINTS_IDS);
    }

    @Test
    @DatabaseSetup("/database/states/consumer/checkpoint_consumer_exists.xml")
    @ExpectedDatabase(
        value = "/database/expected/consumer/two_checkpoint_consumer_added.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void testSecondCheckpointConsumerAdded() {
        consumerDao.insertCheckpointNotifySuccessTs(CONSUMER_ID_2, Date.from(clock.instant()), CHECKPOINTS_IDS);
    }
}
