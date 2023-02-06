package ru.yandex.market.deepmind.tracker_approver.monitoring;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.deepmind.tracker_approver.BaseTrackerApproverTest;
import ru.yandex.market.deepmind.tracker_approver.configuration.EnhancedTrackerApproverConfiguration;
import ru.yandex.market.deepmind.tracker_approver.monitoring.TrackerApproverMonitoringState.Status;
import ru.yandex.market.deepmind.tracker_approver.pojo.MyKey;
import ru.yandex.market.deepmind.tracker_approver.service.TrackerApproverExecutionContext;
import ru.yandex.market.deepmind.tracker_approver.service.TrackerApproverFactory;
import ru.yandex.market.deepmind.tracker_approver.service.enhanced.EnhancedTrackerApproverExecutor;
import ru.yandex.market.deepmind.tracker_approver.strategies.FailEverythingStrategy;
import ru.yandex.market.deepmind.tracker_approver.utils.CurrentThreadExecutorService;
import ru.yandex.market.mbo.storage.StorageKeyValueService;

public class TrackerApproverMonitoringServiceTest extends BaseTrackerApproverTest {
    private TrackerApproverFactory trackerApproverFactory;
    private EnhancedTrackerApproverExecutor executor;
    private TrackerApproverExecutionContext executionContext;
    private TrackerApproverMonitoringService trackerApproverMonitoringService;

    @Before
    public void setUp() {
        trackerApproverFactory = new TrackerApproverFactory(dataRepository, ticketRepository, transactionTemplate,
                objectMapper);
        executionContext = new TrackerApproverExecutionContext()
            .setThreadCount(1);
        trackerApproverMonitoringService = new TrackerApproverMonitoringService(executionContext, ticketRepository);

        var storageKeyValueServiceMock = Mockito.mock(StorageKeyValueService.class);
        Mockito.when(storageKeyValueServiceMock.getOffsetDateTime(Mockito.any(), Mockito.any())).thenReturn(null);

        var configuration = new EnhancedTrackerApproverConfiguration(
            "",
            "",
            trackerApproverFactory,
            executionContext
        );
        executor = new EnhancedTrackerApproverExecutor(
            ticketRepository,
            configuration,
            null,
            transactionTemplate,
            storageKeyValueServiceMock
        );
        executor.setExecutorService(new CurrentThreadExecutorService());
    }

    @Test
    public void monitorWhenRetry() {
        executionContext.setWarnRetryCount(1);
        executionContext.setCritRetryCount(3);
        executionContext.setMaxRetryCount(4);

        trackerApproverFactory.registerStrategy(new FailEverythingStrategy());
        var facade = trackerApproverFactory.getFacade(FailEverythingStrategy.TYPE);

        var ticket = facade.start(List.of(new MyKey(1, "a")));

        // first try, everything is ok
        Assertions.assertThat(trackerApproverMonitoringService.monitor())
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new TrackerApproverMonitoringState(FailEverythingStrategy.TYPE, ticket, Status.OK, null)
            );

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getRetryCount()).isEqualTo(1);

        // second try, everything warn status
        Assertions.assertThat(trackerApproverMonitoringService.monitor())
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new TrackerApproverMonitoringState(FailEverythingStrategy.TYPE, ticket, Status.WARN,
                    "java.lang.RuntimeException: enrich is failed")
            );

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getRetryCount()).isEqualTo(2);

        // third try, everything warn status
        Assertions.assertThat(trackerApproverMonitoringService.monitor())
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new TrackerApproverMonitoringState(FailEverythingStrategy.TYPE, ticket, Status.WARN,
                    "java.lang.RuntimeException: enrich is failed")
            );

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getRetryCount()).isEqualTo(3);

        // third try, everything crit status
        Assertions.assertThat(trackerApproverMonitoringService.monitor())
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new TrackerApproverMonitoringState(FailEverythingStrategy.TYPE, ticket, Status.CRIT,
                    "java.lang.RuntimeException: enrich is failed")
            );

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getRetryCount()).isEqualTo(4);

        // forth try, everything crit status
        Assertions.assertThat(trackerApproverMonitoringService.monitor())
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new TrackerApproverMonitoringState(FailEverythingStrategy.TYPE, ticket, Status.CRIT,
                    "Stopped due to retry_count(4) >= maxRetryCount(4); " +
                        "Last exception: java.lang.RuntimeException: enrich is failed")
            );

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getRetryCount()).isEqualTo(4);

        // fifth try, everything crit status
        Assertions.assertThat(trackerApproverMonitoringService.monitor())
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new TrackerApproverMonitoringState(FailEverythingStrategy.TYPE, ticket, Status.CRIT,
                    "Stopped due to retry_count(4) >= maxRetryCount(4); " +
                        "Last exception: java.lang.RuntimeException: enrich is failed")
            );
    }
}
