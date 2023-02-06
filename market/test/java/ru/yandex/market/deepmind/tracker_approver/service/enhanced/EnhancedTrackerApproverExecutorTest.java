package ru.yandex.market.deepmind.tracker_approver.service.enhanced;

import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.deepmind.tracker_approver.BaseTrackerApproverTest;
import ru.yandex.market.deepmind.tracker_approver.configuration.EnhancedTrackerApproverConfiguration;
import ru.yandex.market.deepmind.tracker_approver.pojo.MyKey;
import ru.yandex.market.deepmind.tracker_approver.pojo.MyMeta;
import ru.yandex.market.deepmind.tracker_approver.pojo.ProcessRequest;
import ru.yandex.market.deepmind.tracker_approver.pojo.TicketState;
import ru.yandex.market.deepmind.tracker_approver.pojo.TrackerApproverRawDataHistory;
import ru.yandex.market.deepmind.tracker_approver.pojo.TrackerApproverTicketRawStatusHistory;
import ru.yandex.market.deepmind.tracker_approver.repository.TrackerApproverDataHistoryRepository;
import ru.yandex.market.deepmind.tracker_approver.repository.TrackerApproverTicketStatusHistoryRepository;
import ru.yandex.market.deepmind.tracker_approver.service.TrackerApproverExecutionContext;
import ru.yandex.market.deepmind.tracker_approver.service.TrackerApproverFactory;
import ru.yandex.market.deepmind.tracker_approver.strategies.AllIsOkStrategy;
import ru.yandex.market.deepmind.tracker_approver.strategies.AllIsOkV2Strategy;
import ru.yandex.market.deepmind.tracker_approver.strategies.ChangeKeyMetaAndNotReadyStrategy;
import ru.yandex.market.deepmind.tracker_approver.strategies.ChangeMetaAndNotReadyStrategy;
import ru.yandex.market.deepmind.tracker_approver.strategies.FailEverythingStrategy;
import ru.yandex.market.deepmind.tracker_approver.strategies.FailThenCloseStrategy;
import ru.yandex.market.deepmind.tracker_approver.strategies.FailThenNotReadyStrategy;
import ru.yandex.market.deepmind.tracker_approver.strategies.ReopenAndThenClosePostProcessStrategy;
import ru.yandex.market.deepmind.tracker_approver.strategies.ReopenAndThenCloseStrategy;
import ru.yandex.market.deepmind.tracker_approver.strategies.SkipThenCloseStrategy;
import ru.yandex.market.deepmind.tracker_approver.utils.CurrentThreadExecutorService;
import ru.yandex.market.mbo.storage.StorageKeyValueService;

public class EnhancedTrackerApproverExecutorTest extends BaseTrackerApproverTest {
    @Autowired
    private TrackerApproverTicketStatusHistoryRepository ticketStatusHistoryRepository;
    @Autowired
    private TrackerApproverDataHistoryRepository dataHistoryRepository;
    private TrackerApproverFactory trackerApproverFactory;
    private EnhancedTrackerApproverExecutor executor;
    private TrackerApproverExecutionContext executionContext;
    private StorageKeyValueService storageKeyValueServiceMock;


    @Before
    public void setUp() {
        executionContext = new TrackerApproverExecutionContext().setThreadCount(1);
        trackerApproverFactory = new TrackerApproverFactory(dataRepository, ticketRepository, transactionTemplate,
            objectMapper);

        storageKeyValueServiceMock = Mockito.mock(StorageKeyValueService.class);
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
    public void timestampOfExecutionIsBeingSet() {
        //act
        executor.run();

        //assert
        Mockito.verify(storageKeyValueServiceMock, Mockito.times(1))
            .getOffsetDateTime(Mockito.eq(executor.getLastExecutionKey()), Mockito.any());
        Mockito.verify(storageKeyValueServiceMock, Mockito.times(1))
            .putOffsetDateTime(Mockito.eq(executor.getLastExecutionKey()), Mockito.any());
    }

    @Test
    public void timestampIsBeingResetIfRuntimeExceptionCaught() {
        //arrange
        trackerApproverFactory.registerStrategy(new AllIsOkStrategy());
        var facade = trackerApproverFactory.getFacade(AllIsOkStrategy.ALL_IS_OK);
        facade.start(List.of(new MyKey(1, "1")));

        var executorSpy = Mockito.spy(executor);
        Mockito.doThrow(RuntimeException.class).when(executorSpy).processTicket(Mockito.any(), Mockito.any());

        //act
        try {
            executorSpy.run();
        } catch (RuntimeException e) {
            //do nothing
        }

        //assert that previous execution datetime is being set on RuntimeException caught
        Mockito.verify(storageKeyValueServiceMock, Mockito.times(1))
            .getOffsetDateTime(Mockito.eq(executor.getLastExecutionKey()), Mockito.any());
        Mockito.verify(storageKeyValueServiceMock, Mockito.times(2))
            .putOffsetDateTime(Mockito.eq(executor.getLastExecutionKey()), Mockito.any());
    }

    @Test
    public void allIsOkRun() {
        trackerApproverFactory.registerStrategy(new AllIsOkStrategy());
        var facade = trackerApproverFactory.getFacade(AllIsOkStrategy.ALL_IS_OK);

        var ticket = facade.start(List.of(new MyKey(1, "a")));

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);

        // one more run
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);
    }

    @Test
    public void reopenAndThenToClose() {
        trackerApproverFactory.registerStrategy(new ReopenAndThenCloseStrategy());
        var facade = trackerApproverFactory.getFacade(ReopenAndThenCloseStrategy.TYPE);

        var ticket = facade.start(List.of(new MyKey(1, "a")));

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);

        // one more run
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);
    }

    @Test
    public void reopenAndThenToClosePostProcess() {
        trackerApproverFactory.registerStrategy(new ReopenAndThenClosePostProcessStrategy());
        var facade = trackerApproverFactory.getFacade(ReopenAndThenClosePostProcessStrategy.TYPE);

        var ticket = facade.start(List.of(new MyKey(1, "a")));

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);

        // one more run
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);
    }

    @Test
    public void skipThenClose() {
        trackerApproverFactory.registerStrategy(new SkipThenCloseStrategy());
        var facade = trackerApproverFactory.getFacade(SkipThenCloseStrategy.TYPE);

        var ticket = facade.start(List.of(new MyKey(1, "a")));

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.PREPROCESSED);
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);

        // one more run
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);
    }

    @Test
    public void failThenClose() {
        trackerApproverFactory.registerStrategy(new FailThenCloseStrategy());
        var facade = trackerApproverFactory.getFacade(FailThenCloseStrategy.TYPE);

        var ticket = facade.start(List.of(new MyKey(1, "a")));

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.PREPROCESSED);
        Assertions.assertThat(facade.findTicketStatus(ticket).getRetryCount()).isEqualTo(1);

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);
        Assertions.assertThat(facade.findTicketStatus(ticket).getRetryCount()).isEqualTo(0);

        // one more run
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);
        Assertions.assertThat(facade.findTicketStatus(ticket).getRetryCount()).isEqualTo(0);
    }

    @Test
    public void failThenNotReady() {
        trackerApproverFactory.registerStrategy(new FailThenNotReadyStrategy());
        var facade = trackerApproverFactory.getFacade(FailThenNotReadyStrategy.TYPE);

        var ticket = facade.start(List.of(new MyKey(1, "a")));

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.PREPROCESSED);
        Assertions.assertThat(facade.findTicketStatus(ticket).getRetryCount()).isEqualTo(1);

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.PREPROCESSED);
        Assertions.assertThat(facade.findTicketStatus(ticket).getRetryCount()).isEqualTo(0);

        // one more run
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.PREPROCESSED);
        Assertions.assertThat(facade.findTicketStatus(ticket).getRetryCount()).isEqualTo(0);
    }

    @Test
    public void dontRunMoreThenMaxRetryCount() {
        executionContext.setMaxRetryCount(2);

        var strategy = Mockito.spy(new FailEverythingStrategy());
        trackerApproverFactory.registerStrategy(strategy);
        var facade = trackerApproverFactory.getFacade(FailEverythingStrategy.TYPE);

        var ticket = facade.start(List.of(new MyKey(1, "a")));

        executor.run();
        var ticketStatus1 = facade.findTicketStatus(ticket);
        Assertions.assertThat(ticketStatus1.getState()).isEqualTo(TicketState.NEW);
        Assertions.assertThat(ticketStatus1.getRetryCount()).isEqualTo(1);
        Assertions.assertThat(ticketStatus1.getLastException()).contains("enrich is failed");

        executor.run();
        var ticketStatus2 = facade.findTicketStatus(ticket);
        Assertions.assertThat(ticketStatus2.getState()).isEqualTo(TicketState.NEW);
        Assertions.assertThat(ticketStatus2.getRetryCount()).isEqualTo(2);
        Assertions.assertThat(ticketStatus2.getLastException()).contains("enrich is failed");

        executor.run();
        var ticketStatus3 = facade.findTicketStatus(ticket);
        Assertions.assertThat(ticketStatus3.getState()).isEqualTo(TicketState.NEW);
        Assertions.assertThat(ticketStatus3.getRetryCount()).isEqualTo(2); // NO MORE THAN MAX_RUN_COUNT
        Assertions.assertThat(ticketStatus3.getLastException()).contains("enrich is failed");
        Mockito.verify(strategy, Mockito.times(2)).enrich(Mockito.any());
    }

    @Test
    public void testTicketStatusHistoryAllIsOk() {
        trackerApproverFactory.registerStrategy(new AllIsOkStrategy());
        var facade = trackerApproverFactory.getFacade(AllIsOkStrategy.ALL_IS_OK);

        var ticket = facade.start(List.of(new MyKey(1, "a")));

        var events1 = ticketStatusHistoryRepository.findAllByTicketOrderedByModifiedTs(ticket);
        Assertions.assertThat(events1).usingElementComparatorOnFields("state")
            .containsExactly(
                new TrackerApproverTicketRawStatusHistory().setState(TicketState.NEW)
            );

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);
        var events2 = ticketStatusHistoryRepository.findAllByTicketOrderedByModifiedTs(ticket);
        Assertions.assertThat(events2).usingElementComparatorOnFields("state")
            .containsExactly(
                new TrackerApproverTicketRawStatusHistory().setState(TicketState.NEW),
                new TrackerApproverTicketRawStatusHistory().setState(TicketState.ENRICHED),
                new TrackerApproverTicketRawStatusHistory().setState(TicketState.PREPROCESSED),
                new TrackerApproverTicketRawStatusHistory().setState(TicketState.PROCESSED),
                new TrackerApproverTicketRawStatusHistory().setState(TicketState.POSTPROCESSED),
                new TrackerApproverTicketRawStatusHistory().setState(TicketState.CLOSED)
            );
    }

    @Test
    public void testTicketStatusHistoryFailThenClose() {
        trackerApproverFactory.registerStrategy(new FailThenCloseStrategy());
        var facade = trackerApproverFactory.getFacade(FailThenCloseStrategy.TYPE);

        var ticket = facade.start(List.of(new MyKey(1, "a")));

        var events1 = ticketStatusHistoryRepository.findAllByTicketOrderedByModifiedTs(ticket);
        Assertions.assertThat(events1).usingElementComparatorOnFields("state")
            .containsExactly(
                new TrackerApproverTicketRawStatusHistory().setState(TicketState.NEW)
            );

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.PREPROCESSED);
        var events2 = ticketStatusHistoryRepository.findAllByTicketOrderedByModifiedTs(ticket);
        Assertions.assertThat(events2).usingElementComparatorOnFields("state", "retryCount")
            .containsExactly(
                new TrackerApproverTicketRawStatusHistory().setState(TicketState.NEW),
                new TrackerApproverTicketRawStatusHistory().setState(TicketState.ENRICHED),
                new TrackerApproverTicketRawStatusHistory().setState(TicketState.PREPROCESSED),
                new TrackerApproverTicketRawStatusHistory().setState(TicketState.PREPROCESSED).setRetryCount(1)
            );

        // one more run
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);
        var events3 = ticketStatusHistoryRepository.findAllByTicketOrderedByModifiedTs(ticket);
        Assertions.assertThat(events3).usingElementComparatorOnFields("state", "retryCount")
            .containsExactly(
                new TrackerApproverTicketRawStatusHistory().setState(TicketState.NEW),
                new TrackerApproverTicketRawStatusHistory().setState(TicketState.ENRICHED),
                new TrackerApproverTicketRawStatusHistory().setState(TicketState.PREPROCESSED),
                new TrackerApproverTicketRawStatusHistory().setState(TicketState.PREPROCESSED).setRetryCount(1),
                new TrackerApproverTicketRawStatusHistory().setState(TicketState.PROCESSED),
                new TrackerApproverTicketRawStatusHistory().setState(TicketState.POSTPROCESSED),
                new TrackerApproverTicketRawStatusHistory().setState(TicketState.CLOSED)
            );
    }

    @Test
    public void testTicketStatusHistoryFailThenNotReady() {
        trackerApproverFactory.registerStrategy(new FailThenNotReadyStrategy());
        var facade = trackerApproverFactory.getFacade(FailThenNotReadyStrategy.TYPE);

        var ticket = facade.start(List.of(new MyKey(1, "a")));

        var events1 = ticketStatusHistoryRepository.findAllByTicketOrderedByModifiedTs(ticket);
        Assertions.assertThat(events1).usingElementComparatorOnFields("state")
            .containsExactly(
                new TrackerApproverTicketRawStatusHistory().setState(TicketState.NEW)
            );

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.PREPROCESSED);
        var events2 = ticketStatusHistoryRepository.findAllByTicketOrderedByModifiedTs(ticket);
        Assertions.assertThat(events2).usingElementComparatorOnFields("state", "retryCount")
            .containsExactly(
                new TrackerApproverTicketRawStatusHistory().setState(TicketState.NEW),
                new TrackerApproverTicketRawStatusHistory().setState(TicketState.ENRICHED),
                new TrackerApproverTicketRawStatusHistory().setState(TicketState.PREPROCESSED),
                new TrackerApproverTicketRawStatusHistory().setState(TicketState.PREPROCESSED).setRetryCount(1)
            );

        // one more run
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.PREPROCESSED);
        var events3 = ticketStatusHistoryRepository.findAllByTicketOrderedByModifiedTs(ticket);
        Assertions.assertThat(events3).usingElementComparatorOnFields("state", "retryCount")
            .containsExactly(
                new TrackerApproverTicketRawStatusHistory().setState(TicketState.NEW),
                new TrackerApproverTicketRawStatusHistory().setState(TicketState.ENRICHED),
                new TrackerApproverTicketRawStatusHistory().setState(TicketState.PREPROCESSED),
                new TrackerApproverTicketRawStatusHistory().setState(TicketState.PREPROCESSED).setRetryCount(1),
                new TrackerApproverTicketRawStatusHistory().setState(TicketState.PREPROCESSED)
            );
    }

    @Test
    public void testTicketStatusHistoryChangeMetaAndNotReadyStrategy() {
        trackerApproverFactory.registerStrategy(new ChangeMetaAndNotReadyStrategy(new MyMeta("changed")));
        var facade = trackerApproverFactory.getFacade(ChangeMetaAndNotReadyStrategy.TYPE);

        var ticket = facade.start(List.of(new MyKey(1, "a")), new MyMeta("started"));

        var events1 = ticketStatusHistoryRepository.findAllByTicketOrderedByModifiedTs(ticket);
        Assertions.assertThat(events1).usingElementComparatorOnFields("state")
            .containsExactly(
                new TrackerApproverTicketRawStatusHistory().setState(TicketState.NEW)
            );

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.PREPROCESSED);
        var events2 = ticketStatusHistoryRepository.findAllByTicketOrderedByModifiedTs(ticket);
        Assertions.assertThat(events2).usingElementComparatorOnFields("state", "retryCount")
            .containsExactly(
                new TrackerApproverTicketRawStatusHistory().setState(TicketState.NEW),
                new TrackerApproverTicketRawStatusHistory().setState(TicketState.ENRICHED),
                new TrackerApproverTicketRawStatusHistory().setState(TicketState.PREPROCESSED),
                new TrackerApproverTicketRawStatusHistory().setState(TicketState.PREPROCESSED)
            );

        Assertions.assertThat(events2.get(0).getTicketMeta(MyMeta.class)).isEqualTo(new MyMeta("started"));
        Assertions.assertThat(events2.get(1).getTicketMeta(MyMeta.class)).isEqualTo(new MyMeta("started"));
        Assertions.assertThat(events2.get(2).getTicketMeta(MyMeta.class)).isEqualTo(new MyMeta("started"));
        Assertions.assertThat(events2.get(3).getTicketMeta(MyMeta.class)).isEqualTo(new MyMeta("changed"));

        // one more run
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.PREPROCESSED);
        var events3 = ticketStatusHistoryRepository.findAllByTicketOrderedByModifiedTs(ticket);
        Assertions.assertThat(events3).usingElementComparatorOnFields("state")
            .containsExactly(
                new TrackerApproverTicketRawStatusHistory().setState(TicketState.NEW),
                new TrackerApproverTicketRawStatusHistory().setState(TicketState.ENRICHED),
                new TrackerApproverTicketRawStatusHistory().setState(TicketState.PREPROCESSED),
                new TrackerApproverTicketRawStatusHistory().setState(TicketState.PREPROCESSED)
            );

        Assertions.assertThat(events3.get(0).getTicketMeta(MyMeta.class)).isEqualTo(new MyMeta("started"));
        Assertions.assertThat(events3.get(1).getTicketMeta(MyMeta.class)).isEqualTo(new MyMeta("started"));
        Assertions.assertThat(events3.get(2).getTicketMeta(MyMeta.class)).isEqualTo(new MyMeta("started"));
        Assertions.assertThat(events3.get(3).getTicketMeta(MyMeta.class)).isEqualTo(new MyMeta("changed"));
    }

    @Test
    public void testKeyHistoryChangeMetaAndNotReadyStrategy() {
        trackerApproverFactory.registerStrategy(new ChangeKeyMetaAndNotReadyStrategy());
        var facade = trackerApproverFactory.getFacade(ChangeKeyMetaAndNotReadyStrategy.TYPE);

        var ticket = facade.start(List.of(new MyKey(1, "a")));

        var events1 = dataHistoryRepository.findAllByTicketOrderedByModifiedTs(ticket);
        Assertions.assertThat(events1).usingElementComparatorOnFields("state")
            .containsExactly(
                new TrackerApproverRawDataHistory().setState(TicketState.NEW)
            );

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.PREPROCESSED);
        dataHistoryRepository.findAllByTicketOrderedByModifiedTs(ticket);

        var events2 = dataHistoryRepository.findAllByTicketOrderedByModifiedTs(ticket);
        Assertions.assertThat(events2).usingElementComparatorOnFields("state")
            .containsExactly(
                new TrackerApproverRawDataHistory().setState(TicketState.NEW),
                new TrackerApproverRawDataHistory().setState(TicketState.ENRICHED),
                new TrackerApproverRawDataHistory().setState(TicketState.PREPROCESSED),
                new TrackerApproverRawDataHistory().setState(TicketState.PREPROCESSED)
            );

        Assertions.assertThat(events2.get(0).getKeyMeta(MyMeta.class)).isEqualTo(null);
        Assertions.assertThat(events2.get(1).getKeyMeta(MyMeta.class)).isEqualTo(new MyMeta("enrich"));
        Assertions.assertThat(events2.get(2).getKeyMeta(MyMeta.class)).isEqualTo(new MyMeta("check"));
        Assertions.assertThat(events2.get(3).getKeyMeta(MyMeta.class)).isEqualTo(new MyMeta("process"));

        // one more run
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.PREPROCESSED);
        var events3 = dataHistoryRepository.findAllByTicketOrderedByModifiedTs(ticket);
        Assertions.assertThat(events3).usingElementComparatorOnFields("state")
            .containsExactly(
                new TrackerApproverRawDataHistory().setState(TicketState.NEW),
                new TrackerApproverRawDataHistory().setState(TicketState.ENRICHED),
                new TrackerApproverRawDataHistory().setState(TicketState.PREPROCESSED),
                new TrackerApproverRawDataHistory().setState(TicketState.PREPROCESSED)
            );

        Assertions.assertThat(events2.get(0).getKeyMeta(MyMeta.class)).isEqualTo(null);
        Assertions.assertThat(events2.get(1).getKeyMeta(MyMeta.class)).isEqualTo(new MyMeta("enrich"));
        Assertions.assertThat(events2.get(2).getKeyMeta(MyMeta.class)).isEqualTo(new MyMeta("check"));
        Assertions.assertThat(events2.get(3).getKeyMeta(MyMeta.class)).isEqualTo(new MyMeta("process"));
    }

    @Test
    public void startStrategiesWithDifferentVersion() {
        var v1 = Mockito.spy(new AllIsOkStrategy());
        var v2 = Mockito.spy(new AllIsOkV2Strategy());

        trackerApproverFactory.registerStrategy(v1);
        trackerApproverFactory.registerStrategy(v2);

        var facade = trackerApproverFactory.getFacade(AllIsOkStrategy.ALL_IS_OK);
        var facade1 = trackerApproverFactory.getFacade(AllIsOkStrategy.ALL_IS_OK, v1.getStrategyVersion());
        var facade2 = trackerApproverFactory.getFacade(AllIsOkStrategy.ALL_IS_OK, v2.getStrategyVersion());
        Assertions.assertThat(facade.getStrategyVersion()).isEqualTo(v2.getStrategyVersion());
        Assertions.assertThat(facade1.getStrategyVersion()).isEqualTo(v1.getStrategyVersion());
        Assertions.assertThat(facade2.getStrategyVersion()).isEqualTo(v2.getStrategyVersion());

        var ticket1 = facade1.start(List.of(new MyKey(1, "a")));
        var ticket2 = facade2.start(List.of(new MyKey(2, "b")));

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket1).getState()).isEqualTo(TicketState.CLOSED);
        Assertions.assertThat(facade.findTicketStatus(ticket2).getState()).isEqualTo(TicketState.CLOSED);

        Mockito.verify(v1, Mockito.times(1)).process(Mockito.refEq(
            ProcessRequest.of(ticket1, List.of(new MyKey(1, "a")), null, Map.of())
        ));
        Mockito.verify(v2, Mockito.times(1)).process(Mockito.refEq(
            ProcessRequest.of(ticket2, List.of(new MyKey(2, "b")), null, Map.of())
        ));
    }
}
