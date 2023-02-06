package ru.yandex.direct.hourglass.implementations.updateschedule;

import java.time.Duration;
import java.util.concurrent.ThreadFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.hourglass.InstanceId;
import ru.yandex.direct.hourglass.MonitoringWriter;
import ru.yandex.direct.hourglass.implementations.InstanceIdImpl;
import ru.yandex.direct.hourglass.implementations.ThreadsHierarchy;
import ru.yandex.direct.hourglass.storage.Storage;
import ru.yandex.direct.hourglass.updateschedule.MainScheduleVersionExtractor;
import ru.yandex.direct.hourglass.updateschedule.SchedulerInstancesRepository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScheduleUpdateServiceImplTest {

    private MainScheduleVersionExtractor mainScheduleVersionExtractor;
    private SchedulerInstancesRepository schedulerInstancesRepository;
    private Storage storage;
    private InstanceId instanceId = new InstanceIdImpl();
    private MonitoringWriter monitoringWriter;
    private ThreadsHierarchy threadsHierarchy;

    /* В тестах не будет запускаться отдельный поток */
    private Duration frequency = Duration.ZERO;

    private ScheduleUpdateServiceImpl scheduleUpdateService;

    @BeforeEach
    void before() {
        this.mainScheduleVersionExtractor = mock(MainScheduleVersionExtractor.class);
        this.schedulerInstancesRepository = mock(SchedulerInstancesRepository.class);
        var threadFactory = mock(ThreadFactory.class);
        var thread = mock(Thread.class);
        when(threadFactory.newThread(any())).thenReturn(thread);
        this.threadsHierarchy = mock(ThreadsHierarchy.class);
        when(threadsHierarchy.getSystemThreadFactory()).thenReturn(threadFactory);
        this.monitoringWriter = mock(MonitoringWriter.class);
        this.storage = mock(Storage.class);
    }

    /**
     * Тест проверяет, что если версия не основная, то будет вызван метод, помечающий, что версия неосновная
     */
    @Test
    void notMainVersionTest() {
        var currentVersion = "1";
        scheduleUpdateService = new ScheduleUpdateServiceImpl.Builder()
                .withStorage(storage)
                .withFrequency(frequency)
                .withMainVersionExtractor(mainScheduleVersionExtractor)
                .withCurrentVersion(currentVersion)
                .withSchedulerInstancesRepository(schedulerInstancesRepository)
                .withInstanceId(instanceId)
                .withThreadsHierarchy(threadsHierarchy)
                .withMonitoringWriter(monitoringWriter)
                .build();

        when(mainScheduleVersionExtractor.getVersion()).thenReturn("2");

        scheduleUpdateService.checkLeaderAndUpdateSchedule();

        verify(schedulerInstancesRepository, times(1)).markInstanceAsNotMain(instanceId);

        verify(schedulerInstancesRepository, never()).markInstanceAsMain(instanceId);

        verify(storage, never()).setNewSchedule(any());

    }

    /**
     * Тест проверяет, что если версия основная, то будет вызван метод, помечающий, что версия основная
     */
    @Test
    void mainVersionTest() {
        var currentVersion = "2";
        scheduleUpdateService = new ScheduleUpdateServiceImpl.Builder()
                .withStorage(storage)
                .withFrequency(frequency)
                .withMainVersionExtractor(mainScheduleVersionExtractor)
                .withCurrentVersion(currentVersion)
                .withSchedulerInstancesRepository(schedulerInstancesRepository)
                .withInstanceId(instanceId)
                .withThreadsHierarchy(threadsHierarchy)
                .withMonitoringWriter(monitoringWriter)
                .build();

        when(mainScheduleVersionExtractor.getVersion()).thenReturn("2");
        when(schedulerInstancesRepository.isLeaderVersion("2")).thenReturn(false);

        scheduleUpdateService.checkLeaderAndUpdateSchedule();

        verify(schedulerInstancesRepository, never()).markInstanceAsNotMain(instanceId);

        verify(schedulerInstancesRepository, times(1)).markInstanceAsMain(instanceId);

        verify(storage, never()).setNewSchedule(any());
    }

    /**
     * Тест проверяет, что если версия основная, то будет вызван метод, помечающий, что версия основная
     * При этом если версия лидер - то она вызовет метод, обновляющий расписание
     */
    @Test
    void leaderVersionTest() {
        var currentVersion = "2";
        scheduleUpdateService = new ScheduleUpdateServiceImpl.Builder()
                .withStorage(storage)
                .withFrequency(frequency)
                .withMainVersionExtractor(mainScheduleVersionExtractor)
                .withCurrentVersion(currentVersion)
                .withSchedulerInstancesRepository(schedulerInstancesRepository)
                .withInstanceId(instanceId)
                .withThreadsHierarchy(threadsHierarchy)
                .withMonitoringWriter(monitoringWriter)
                .build();

        when(mainScheduleVersionExtractor.getVersion()).thenReturn("2");
        when(schedulerInstancesRepository.isLeaderVersion("2")).thenReturn(true);

        scheduleUpdateService.checkLeaderAndUpdateSchedule();

        verify(schedulerInstancesRepository, never()).markInstanceAsNotMain(instanceId);

        verify(schedulerInstancesRepository, times(1)).markInstanceAsMain(instanceId);

        verify(storage, times(1)).setNewSchedule(null);
    }
}
