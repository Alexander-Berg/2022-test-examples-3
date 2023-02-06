package ru.yandex.market.pers.author.tms.live;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.live.LiveStreamingTarantinoClient;
import ru.yandex.market.live.model.LiveStreamingPreview;
import ru.yandex.market.pers.author.PersAuthorTest;
import ru.yandex.market.pers.author.tms.live.model.LiveStream;
import ru.yandex.market.pers.author.tms.live.model.LiveStreamState;
import ru.yandex.market.util.db.ConfigurationService;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PlannedLiveStreamFetchExecutorTest extends PersAuthorTest {

    @Autowired
    private PlannedLiveStreamFetchExecutor executor;
    @Autowired
    private LiveStreamDbService liveStreamDbService;
    @Autowired
    private LiveStreamingTarantinoClient liveStreamingTarantinoClient;

    @Test
    void fetchPlannedLiveStream() {
        List<LiveStream> scheduledLiveStreams = liveStreamDbService.getScheduledLiveStreams();
        Assertions.assertEquals(0, scheduledLiveStreams.size());

        List<LiveStreamingPreview> oneLiveStreamPreview = new ArrayList<>();
        oneLiveStreamPreview.add(new LiveStreamingPreview(123, "first_live", "Первая лайв трансляция", "2021-05-19T17:00:00.000Z", 60));
        when(liveStreamingTarantinoClient.getScheduledLiveStreams()).then(invocation -> oneLiveStreamPreview);

        executor.fetchPlannedLiveStream();
        scheduledLiveStreams = liveStreamDbService.getScheduledLiveStreams();
        Assertions.assertEquals(1, scheduledLiveStreams.size());
        Assertions.assertEquals(oneLiveStreamPreview.get(0).getCmsPageId(), scheduledLiveStreams.get(0).getId());
        Assertions.assertEquals(LiveStreamState.SCHEDULED, scheduledLiveStreams.get(0).getState());
        Assertions.assertEquals(60, ChronoUnit.MINUTES.between(
                scheduledLiveStreams.get(0).getStartTime(), scheduledLiveStreams.get(0).getEndTime()));

        List<LiveStreamingPreview> previews = new ArrayList<>();
        previews.add(new LiveStreamingPreview(123, "first_live", "Первая лайв трансляция", "2021-05-19T17:30:00.000Z", 120));
        previews.add(new LiveStreamingPreview(124, "second_live", "Вторая лайв трансляция", "2021-05-20T17:00:00.000Z", 60));
        when(liveStreamingTarantinoClient.getScheduledLiveStreams()).then(invocation -> previews);

        executor.fetchPlannedLiveStream();
        scheduledLiveStreams = liveStreamDbService.getScheduledLiveStreams();
        Assertions.assertEquals(2, scheduledLiveStreams.size());
        Assertions.assertEquals(previews.get(0).getCmsPageId(), scheduledLiveStreams.get(0).getId());
        Assertions.assertEquals(LiveStreamState.SCHEDULED, scheduledLiveStreams.get(0).getState());
        Assertions.assertEquals(120, ChronoUnit.MINUTES.between(
                scheduledLiveStreams.get(0).getStartTime(), scheduledLiveStreams.get(0).getEndTime()));

        Assertions.assertEquals(previews.get(1).getCmsPageId(), scheduledLiveStreams.get(1).getId());
        Assertions.assertEquals(LiveStreamState.SCHEDULED, scheduledLiveStreams.get(1).getState());
        Assertions.assertEquals(60, ChronoUnit.MINUTES.between(
                scheduledLiveStreams.get(1).getStartTime(), scheduledLiveStreams.get(1).getEndTime()));
    }

    @Test
    public void checkDeleteCancelledStreams() {
        List<LiveStream> scheduledLiveStreams = liveStreamDbService.getScheduledLiveStreams();
        Assertions.assertEquals(0, scheduledLiveStreams.size());

        List<LiveStreamingPreview> previews = new ArrayList<>();
        previews.add(new LiveStreamingPreview(123, "first_live", "Первая лайв трансляция", "2021-05-19T17:30:00.000Z", 120));
        previews.add(new LiveStreamingPreview(124, "second_live", "Вторая лайв трансляция", "2021-05-20T17:00:00.000Z", 60));
        when(liveStreamingTarantinoClient.getScheduledLiveStreams()).then(invocation -> previews);

        executor.fetchPlannedLiveStream();

        scheduledLiveStreams = liveStreamDbService.getScheduledLiveStreams();
        Assertions.assertEquals(2, scheduledLiveStreams.size());


        List<LiveStreamingPreview> oneLiveStreamPreview = new ArrayList<>();
        oneLiveStreamPreview.add(new LiveStreamingPreview(123, "first_live", "Первая лайв трансляция", "2021-05-19T17:00:00.000Z", 60));
        when(liveStreamingTarantinoClient.getScheduledLiveStreams()).then(invocation -> oneLiveStreamPreview);

        executor.fetchPlannedLiveStream();

        scheduledLiveStreams = liveStreamDbService.getScheduledLiveStreams();
        Assertions.assertEquals(1, scheduledLiveStreams.size());
        Assertions.assertEquals(123, scheduledLiveStreams.get(0).getId());
    }
}
