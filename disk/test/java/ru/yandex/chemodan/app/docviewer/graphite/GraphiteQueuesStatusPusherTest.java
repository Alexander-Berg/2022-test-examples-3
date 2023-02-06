package ru.yandex.chemodan.app.docviewer.graphite;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.docviewer.utils.scheduler.Scheduler;
import ru.yandex.commune.alive2.location.Location;
import ru.yandex.commune.graphite.GraphiteClient;
import ru.yandex.misc.env.EnvironmentType;
import ru.yandex.misc.version.SimpleAppName;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author akirakozov
 */
public class GraphiteQueuesStatusPusherTest {
    private static final String NAME_PREFIX = "media.docviewer.web.queue.";
    private GraphiteQueuesStatusPusher sut;

    @Mock
    private GraphiteClient graphiteClient;
    @Mock
    private Scheduler scheduler;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        sut = new GraphiteQueuesStatusPusher(
                null, graphiteClient,
                GraphitePrefixPathUtils.getCommonGraphitePrefix(
                        new SimpleAppName("docviewer", "web"), "queue", "v1-0-0", "some_host_ru",
                        Location.newLocation().dcName(Option.empty()).build()));
    }

    @Test
    public void pushQueueStatus() {
        int activeWorkerCount = 5;
        long queueLength = 3;

        when(scheduler.getActiveWorkers()).thenReturn(activeWorkerCount);
        when(scheduler.getQueueLength()).thenReturn(queueLength);

        String schedulerName = "test_scheduler";
        sut.pushQueueStatus("test_scheduler", scheduler);

        String prefix = NAME_PREFIX + EnvironmentType.getActive().getValue().toUpperCase()
                + ".unknown_dc.some_host_ru.v1-0-0." + schedulerName;
        verify(graphiteClient).spool(eq(prefix + ".queue_size"), eq(3L), any());
        verify(graphiteClient).spool(eq(prefix + ".active_workers"), eq(5), any());
    }
}
