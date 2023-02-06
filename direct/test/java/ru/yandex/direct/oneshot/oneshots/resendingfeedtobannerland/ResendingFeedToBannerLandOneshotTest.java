package ru.yandex.direct.oneshot.oneshots.resendingfeedtobannerland;

import org.apache.commons.lang3.RandomUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.feed.model.Feed;
import ru.yandex.direct.core.entity.feed.model.UpdateStatus;
import ru.yandex.direct.core.entity.feed.repository.FeedRepository;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.ClientSteps;
import ru.yandex.direct.core.testing.steps.FeedSteps;
import ru.yandex.direct.oneshot.configuration.OneshotTest;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;

@OneshotTest
@RunWith(SpringRunner.class)
public class ResendingFeedToBannerLandOneshotTest {

    @Autowired
    private FeedSteps feedSteps;

    @Autowired
    private ClientSteps clientSteps;

    @Autowired
    private FeedRepository feedRepository;

    @Autowired
    private ResendingFeedToBannerLandOneshot oneshot;

    private Long feedId1;
    private Long feedId2;
    int shard;

    @Before
    public void setUp() {
        ClientInfo defaultClient = clientSteps.createDefaultClient();
        shard = defaultClient.getShard();
        feedId1 = feedSteps.createDefaultFeed(defaultClient).getFeedId();
        feedId2 = feedSteps.createDefaultFeed(defaultClient).getFeedId();
    }


    @Test
    public void nonexistentFeedId_NothingChanged() {

        var inputData = new ResendingFeedToBannerLandOneshotData();
        inputData.setFeedId(RandomUtils.nextLong(100, Long.MAX_VALUE));

        oneshot.execute(inputData, null, shard);

        Feed actualFeed1 = getFeed(feedId1);
        Feed actualFeed2 = getFeed(feedId2);

        assertThat(actualFeed1.getUpdateStatus()).isEqualTo(UpdateStatus.DONE);
        assertThat(actualFeed2.getUpdateStatus()).isEqualTo(UpdateStatus.DONE);
    }


    @Test
    public void feed1_SuccessUpdate() {

        var inputData = new ResendingFeedToBannerLandOneshotData();
        inputData.setFeedId(feedId1);

        oneshot.execute(inputData, null, shard);

        Feed actualFeed1 = getFeed(feedId1);
        Feed actualFeed2 = getFeed(feedId2);

        assertThat(actualFeed1.getUpdateStatus()).isEqualTo(UpdateStatus.NEW);
        assertThat(actualFeed2.getUpdateStatus()).isEqualTo(UpdateStatus.DONE);
    }

    private Feed getFeed(Long feedId) {
        return feedRepository.get(shard, singleton(feedId)).get(0);
    }

}
