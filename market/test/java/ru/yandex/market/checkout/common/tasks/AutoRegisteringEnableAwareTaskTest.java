package ru.yandex.market.checkout.common.tasks;

import java.util.Collections;
import java.util.EnumSet;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.common.util.application.EnvironmentType;
import ru.yandex.market.checkout.common.time.TestableClock;
import ru.yandex.market.checkout.common.web.HealthInfo;
import ru.yandex.market.checkout.common.web.HealthStateCachedProvider;
import ru.yandex.market.checkout.common.web.PingHandler;
import ru.yandex.market.checkout.common.web.ServicePingResult;
import ru.yandex.market.common.ping.CheckResult;
import ru.yandex.market.common.ping.ServiceInfo;
import ru.yandex.market.common.zk.ZooClient;

public class AutoRegisteringEnableAwareTaskTest {

    public static ServiceInfo getServiceInfo() {
        return new ServiceInfo("name", "description");
    }

    public static ServicePingResult getServicePingResultCrit() {
        return new ServicePingResult(getServiceInfo(), getCheckResultCrit());
    }

    private static CheckResult getCheckResultCrit() {
        return new CheckResult(CheckResult.Level.CRITICAL, "fuckup");
    }

    @BeforeEach
    void setUp() {
        PingHandler.setEnabled(true);
    }

    @Test
    public void shouldReturnCanRunFalseIfDisabledOnHost() {
        AutoRegisteringEnableAwareTask task = new AutoRegisteringEnableAwareTask();
        task.setRunnable(new ZooTaskRunnable() {
            @Override
            public void run(ZooTask task, CancellationToken cancellationToken) {
                // Do nothing
            }

            @Nonnull
            @Override
            public String getName() {
                return "TestTask";
            }
        });
        task.setZooClient(Mockito.mock(ZooClient.class));
        task.setClock(TestableClock.getInstance());
        task.setEnabledOnHost(false);
        task.setPermittedEnvironmentTypes(EnumSet.of(EnvironmentType.getActive()));

        Assertions.assertTrue(task.canRun());
        Assertions.assertFalse(task.canRunOnSchedule());
    }

    @Test
    public void testHealthConditionSwitch() {
        AutoRegisteringEnableAwareTask task = new AutoRegisteringEnableAwareTask();
        task.setRunnable(new ZooTaskRunnable() {
            @Override
            public void run(ZooTask task, CancellationToken cancellationToken) {
                // Do nothing
            }

            @Nonnull
            @Override
            public String getName() {
                return "TestTask";
            }
        });
        task.setZooClient(Mockito.mock(ZooClient.class));
        task.setClock(TestableClock.getInstance());
        HealthStateCachedProvider healthService = Mockito.mock(HealthStateCachedProvider.class);
        Mockito.when(healthService.getCurrentState())
                .thenReturn(new HealthInfo(Collections.singletonList(getServicePingResultCrit())));
        task.setHealthStateProvider(healthService);
        task.setHealthCheckEnabled(false);
        task.setPermittedEnvironmentTypes(EnumSet.of(EnvironmentType.getActive()));

        Assertions.assertTrue(task.canRun());
        Assertions.assertTrue(task.canRunOnSchedule());

        task.setHealthCheckEnabled(true);

        Assertions.assertFalse(task.canRunOnSchedule());
    }

    @Test
    public void shouldReturnCanRunFalseHostInBlacklist() {
        AutoRegisteringEnableAwareTask task = new AutoRegisteringEnableAwareTask();
        task.setRunnable(new ZooTaskRunnable() {
            @Override
            public void run(ZooTask task, CancellationToken cancellationToken) {
                // Do nothing
            }

            @Nonnull
            @Override
            public String getName() {
                return "TestTask";
            }
        });
        task.setZooClient(Mockito.mock(ZooClient.class));
        task.setClock(TestableClock.getInstance());
        task.setDc("man");
        task.setDcBlackList(Collections.singleton("MAN"));
        task.setPermittedEnvironmentTypes(EnumSet.of(EnvironmentType.getActive()));

        Assertions.assertFalse(task.canRun());
        Assertions.assertFalse(task.canRunOnSchedule());
    }

    @Test
    public void shouldReturnCanRunTrueHostNotInBlacklist() {
        AutoRegisteringEnableAwareTask task = new AutoRegisteringEnableAwareTask();
        task.setRunnable(new ZooTaskRunnable() {
            @Override
            public void run(ZooTask task, CancellationToken cancellationToken) {
                // Do nothing
            }

            @Nonnull
            @Override
            public String getName() {
                return "TestTask";
            }
        });
        task.setZooClient(Mockito.mock(ZooClient.class));
        task.setClock(TestableClock.getInstance());
        task.setDc("man");
        task.setDcBlackList(Collections.singleton("iva"));
        task.setPermittedEnvironmentTypes(EnumSet.of(EnvironmentType.getActive()));

        Assertions.assertTrue(task.canRun());
        Assertions.assertTrue(task.canRunOnSchedule());
    }
}
