package ru.yandex.market.logistics.iris.service.health;

import java.time.LocalDateTime;
import java.util.List;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.common.ping.DBConnectionChecker;
import ru.yandex.market.logistics.iris.core.domain.source.Source;
import ru.yandex.market.logistics.iris.core.domain.source.SourceType;
import ru.yandex.market.logistics.iris.entity.EmbeddableSource;
import ru.yandex.market.logistics.iris.entity.Interval;
import ru.yandex.market.logistics.iris.jobs.SourceRetrievalService;
import ru.yandex.market.logistics.iris.jobs.cache.SourceRetrievalInfo;
import ru.yandex.market.logistics.iris.jobs.model.QueueType;
import ru.yandex.market.logistics.iris.repository.JobIntervalRepository;
import ru.yandex.market.logistics.iris.service.health.desync.PartnersDesyncChecker;
import ru.yandex.market.logistics.iris.service.health.desync.PartnersDesyncCheckerImpl;
import ru.yandex.market.logistics.iris.service.health.logbroker.LogbrokerChecker;
import ru.yandex.market.logistics.iris.service.health.logbroker.LogbrokerCheckerImpl;

public class PartnersDesyncCheckerTest {
    private static final LocalDateTime ATTEMPT_DATETIME = LocalDateTime.of(2020, 4, 6, 13, 0);
    private static final LocalDateTime SUCCESSFUL_ATTEMPT_DATETIME = LocalDateTime.of(2020, 4, 6, 12, 0);

    private final SourceRetrievalService sourceRetrievalService = Mockito.mock(SourceRetrievalService.class);
    private final JobIntervalRepository jobIntervalRepository = Mockito.mock(JobIntervalRepository.class);
    private final PartnersDesyncChecker partnersDesyncChecker =
            new PartnersDesyncCheckerImpl(sourceRetrievalService, jobIntervalRepository);
    private final LogbrokerChecker logbrokerHealthChecker = Mockito.mock(LogbrokerCheckerImpl.class);
    private final HealthService healthService =
            new HealthService(null, partnersDesyncChecker, logbrokerHealthChecker, List.of(new DBConnectionChecker()));

    @Test
    public void latestSyncAttemptFailed() {
        Mockito.when(sourceRetrievalService.getSourceRetrievalInfo()).thenReturn(SourceRetrievalInfo.failed(
                ImmutableSet.of(),
                SUCCESSFUL_ATTEMPT_DATETIME,
                ATTEMPT_DATETIME,
                new RuntimeException("LMS UNAVAILABLE")
        ));

        Assert.assertEquals(
            HealthUtil.errorToAnswer("Cache sync failure detected. " +
                "Latest sync attempt was at [2020-04-06T13:00]. " +
                "Latest successful attempt was at [2020-04-06T12:00]. " +
                "Failure reason [java.lang.RuntimeException: LMS UNAVAILABLE]."),
            healthService.checkPartnersDesync()
        );
    }

    @Test
    public void noActiveSources() {
        Mockito.when(sourceRetrievalService.getSourceRetrievalInfo()).thenReturn(SourceRetrievalInfo.success(
            ImmutableSet.of(
                Source.of("172", SourceType.WAREHOUSE),
                Source.of("-1", SourceType.WAREHOUSE)
            ),
            ATTEMPT_DATETIME
        ));

        Mockito.when(jobIntervalRepository.findAll()).thenReturn(ImmutableList.of(
            intervalOf("172", SourceType.WAREHOUSE),
            intervalOf("173", SourceType.WAREHOUSE),
            intervalOf("174", SourceType.WAREHOUSE, QueueType.CONTENT_FULL_SYNC),
            intervalOf("146", SourceType.WAREHOUSE, false)
        ));

        Assert.assertEquals(
            HealthUtil.errorToAnswer("Iris DB contains sync intervals for partners " +
                "that should not be synced according to LMS-settings. Partner ids: [173]."),
            healthService.checkPartnersDesync()
        );
    }

    @Test
    public void checkOk() {
        Mockito.when(sourceRetrievalService.getSourceRetrievalInfo()).thenReturn(SourceRetrievalInfo.success(
            ImmutableSet.of(
                Source.of("172", SourceType.WAREHOUSE),
                Source.of("-1", SourceType.WAREHOUSE)
            ),
            ATTEMPT_DATETIME
        ));

        Mockito.when(jobIntervalRepository.findAll()).thenReturn(ImmutableList.of(
            intervalOf("172", SourceType.WAREHOUSE),
            intervalOf("174", SourceType.WAREHOUSE, QueueType.CONTENT_FULL_SYNC),
            intervalOf("146", SourceType.WAREHOUSE, false)
        ));

        Assert.assertEquals("0;OK", healthService.checkPartnersDesync());
    }

    @Nonnull
    private Interval intervalOf(String sourceId, SourceType sourceType, QueueType queueType) {
        Interval interval = intervalOf(sourceId, sourceType);
        interval.setSyncJobName(queueType);
        return interval;
    }

    @Nonnull
    private Interval intervalOf(String sourceId, SourceType sourceType, boolean active) {
        Interval interval = intervalOf(sourceId, sourceType);
        interval.setActive(active);
        return interval;
    }

    @Nonnull
    private Interval intervalOf(String sourceId, SourceType sourceType) {
        Interval interval = new Interval();
        interval.setActive(true);
        interval.setSource(new EmbeddableSource(sourceId, sourceType));
        interval.setSyncJobName(QueueType.REFERENCE_SYNC);
        interval.setInterval(30);
        return interval;
    }
}
