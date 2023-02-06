package ru.yandex.market.loyalty.admin.utils;

import com.google.common.collect.ImmutableSet;
import net.javacrumbs.shedlock.core.SchedulerLock;
import org.junit.Test;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.Schedules;
import org.springframework.scheduling.support.CronSequenceGenerator;

import ru.yandex.market.loyalty.admin.tms.MonitorTypeMark;
import ru.yandex.market.loyalty.core.utils.CoreCollectionUtils;
import ru.yandex.market.loyalty.monitoring.MonitorType;
import ru.yandex.market.loyalty.monitoring.beans.NoSchedulerLock;
import ru.yandex.market.loyalty.test.SourceScanner;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.loyalty.admin.config.TmsConfig.LOCK_AT_LEAST;
import static ru.yandex.market.loyalty.admin.monitoring.AdminMonitorType.PROCESS_CHECKOUTER_BUCKET;

public class CheckTmsConfigTest {
    @Test
    public void checkAllOfScheduledSchedulerLockAndMonitorTypeMarkAnnotationsSpecified() {
        List<Method> methodsWithScheduledButWithoutSchedulerLock = SourceScanner.findAllClasses(
                "ru.yandex.market.loyalty.admin")
                .flatMap(bean -> Arrays.stream(bean.getDeclaredMethods())
                        .filter(method -> method.getDeclaredAnnotationsByType(Scheduled.class).length > 0)
                        .filter(method -> (method.getDeclaredAnnotation(SchedulerLock.class) == null
                                && method.getDeclaredAnnotation(NoSchedulerLock.class) == null)
                                || method.getDeclaredAnnotation(MonitorTypeMark.class) == null)
                )
                .collect(Collectors.toList());

        assertThat(methodsWithScheduledButWithoutSchedulerLock, is(empty()));
    }

    @Test
    public void checkAllScheduledAnnotationsUseValidCron() {
        List<Method> methodsWithScheduledThatNotUseCron =
                SourceScanner.findAllClasses("ru.yandex.market.loyalty.admin")
                        .flatMap(clazz -> Arrays.stream(clazz.getDeclaredMethods()))
                        .filter(method -> {
                            if (method.isAnnotationPresent(Scheduled.class)) {
                                var cron = method.getDeclaredAnnotation(Scheduled.class).cron();
                                return cron.isEmpty() || !CronSequenceGenerator.isValidExpression(cron);
                            } else if (method.isAnnotationPresent(Schedules.class)) {
                                return Arrays.stream(method.getDeclaredAnnotationsByType(Schedules.class))
                                        .flatMap(it -> Arrays.stream(it.value()).map(Scheduled::cron))
                                        .anyMatch(cron -> cron.isEmpty()
                                                || !CronSequenceGenerator.isValidExpression(cron));
                            }
                            return false;
                        })
                        .collect(Collectors.toList());

        assertThat(methodsWithScheduledThatNotUseCron, is(empty()));
    }

    @Test
    public void checkAllSchedulerLockSpecifiedLockAtMostFor() {
        List<Method> methodsWithSchedulerLockThatNotSpecifiedLockAtMostFor = SourceScanner.findSpringBeans("ru.yandex" +
                ".market.loyalty.admin")
                .flatMap(bean -> Arrays.stream(bean.getDeclaredMethods())
                        .filter(method -> method.getDeclaredAnnotation(SchedulerLock.class) != null)
                        .filter(method -> method.getDeclaredAnnotation(SchedulerLock.class).lockAtMostFor() == -1)
                )
                .collect(Collectors.toList());

        assertThat(methodsWithSchedulerLockThatNotSpecifiedLockAtMostFor, is(empty()));
    }

    @Test
    public void checkAllSchedulerLockSpecifiedLockAtLeastFor() {
        List<Method> methodsWithSchedulerLockThatNotSpecifiedLockAtLeastFor = SourceScanner.findSpringBeans("ru" +
                ".yandex.market.loyalty.admin")
                .flatMap(bean -> Arrays.stream(bean.getDeclaredMethods())
                        .filter(method -> method.getDeclaredAnnotation(SchedulerLock.class) != null)
                        .filter(method -> method.getDeclaredAnnotation(SchedulerLock.class).lockAtLeastFor() != LOCK_AT_LEAST)
                )
                .collect(Collectors.toList());

        assertThat(methodsWithSchedulerLockThatNotSpecifiedLockAtLeastFor, is(empty()));
    }

    @Test
    public void methodsWithSchedulerLockMustReturnVoid() {
        List<Method> methodsWithSchedulerLockThatReturnNotVoid = SourceScanner.findSpringBeans("ru.yandex.market" +
                ".loyalty.admin")
                .flatMap(bean -> Arrays.stream(bean.getDeclaredMethods())
                        .filter(method -> method.getDeclaredAnnotation(SchedulerLock.class) != null)
                        .filter(method -> method.getReturnType().equals(Void.class))
                )
                .collect(Collectors.toList());

        assertThat(methodsWithSchedulerLockThatReturnNotVoid, is(empty()));
    }

    @Test
    public void schedulerLockNamesMustBeUnique() {
        List<String> schedulerLockNames = SourceScanner.findSpringBeans("ru.yandex.market.loyalty.admin")
                .flatMap(bean -> Arrays.stream(bean.getDeclaredMethods())
                        .map(method -> method.getDeclaredAnnotation(SchedulerLock.class))
                        .filter(Objects::nonNull)
                )
                .map(SchedulerLock::name)
                .collect(Collectors.toList());

        assertThat(CoreCollectionUtils.findDuplicates(schedulerLockNames), is(empty()));
    }

    @Test
    public void monitorTypeMarksMustBeUnique() {
        List<MonitorType> schedulerLockNames = SourceScanner.findSpringBeans("ru.yandex.market.loyalty.admin")
                .flatMap(bean -> Arrays.stream(bean.getDeclaredMethods())
                        .map(method -> method.getDeclaredAnnotation(MonitorTypeMark.class))
                        .filter(Objects::nonNull)
                )
                .map(MonitorTypeMark::value)
                .filter(t -> !ImmutableSet.of(PROCESS_CHECKOUTER_BUCKET).contains(t))
                .collect(Collectors.toList());

        assertThat(CoreCollectionUtils.findDuplicates(schedulerLockNames), is(empty()));
    }
}
