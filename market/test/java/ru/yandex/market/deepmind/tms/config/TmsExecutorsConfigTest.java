package ru.yandex.market.deepmind.tms.config;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.google.common.reflect.ClassPath;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.Test;
import org.quartz.CronExpression;
import org.springframework.scheduling.quartz.CronTriggerFactoryBean;

import ru.yandex.market.tms.quartz2.spring.CronTrigger;
import ru.yandex.market.tms.quartz2.spring.CronTriggerCreationException;
import ru.yandex.market.tms.quartz2.spring.MonitoringConfig;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.tms.quartz2.spring.MonitoringConfig.UNDEFINED_NUMBER_VALUE;

/**
 * Тест проверяет соответствие cron-расписания конфигурации мониторинга для каждого экзекутора.
 * Расписание задаётся в аннотации {@link CronTrigger}, параметры мониторинга
 * в аннотации {@link ru.yandex.market.mboc.common.config.CommonMonitoringConfig.MonitorJobExecutionTime}.
 * Под последними подразумевается указание числа падений экзекутора, после которого загорается WARN,
 * и число падений, после которого загорается CRIT.
 * <p>
 * На данный момент следует придерживаться следующих правил.
 * а) Если джоба запускается раз в день или реже, то зажигаем CRIT при первом же падении.
 * б) Для остальных (более регулярных) джоб число падений до крита вычисляем так, чтобы суммарное время от первого
 * падения до последнего, вызывающего CRIT, было больше или равно 10 минутам, но меньше дня.
 * Таким образом каждая регулярная джоба получает шанс "исправиться" за некоторое время (мало ли флапнул Yt,
 * подвис коннект и т.д.). Ограничение в один день сверху только для того, чтобы отлавливать абсурдно большие
 * значения счётчика падений.
 * в) Если у джобы нерегулярное расписание - только в определённые дни недели, в промежутки по часам и т.д. - то лучше
 * добавить имя её бина в список исключений в этом тесте. Такой экзекутор проверяться не будет, а добросовестность
 * настройки мониторинга полностью ложится на ваши плечи.
 * г) Если джоба не должна срабатывать автоматически, то не нужно указывать фейковое расписание с 9000-м годом.
 * В этом случае просто не нужно вешать на неё аннотации вообще (кроме @Bean). Такая джоба не будет частью ТМС-ки,
 * но будет по-прежнему доступна для ручного запуска через ручку.
 */
@Slf4j
public class TmsExecutorsConfigTest {
    private static final long TEN_MINS_IN_SECS = 600;
    private static final long ONE_DAY_IN_SECS = 3600 * 24;
    private static Set<Method> allExistingMethods;

    @BeforeClass
    @SuppressWarnings("UnstableApiUsage")
    public static void beforeClass() throws IOException {
        allExistingMethods = new HashSet<>();
        var classLoader = TmsExecutorsConfigTest.class.getClassLoader();
        var classesInPackage = ClassPath.from(classLoader)
            .getTopLevelClassesRecursive("ru.yandex.market.deepmind");
        try {
            for (ClassPath.ClassInfo classInfo : classesInPackage) {
                Class<?> aClass = classLoader.loadClass(classInfo.getName());
                allExistingMethods.addAll(List.of(aClass.getDeclaredMethods()));
            }
        } catch (NoClassDefFoundError | ClassNotFoundException e) {
            // skip
        }
    }

    /**
     * Все экзекуторы должны либо иметь расписание + мониторинг, либо не иметь ни одну из двух аннотаций.
     */
    @Test
    public void testExecutorsProperlyAnnotated() {
        Set<Method> monitored = new HashSet<>(getJobsToCheck(MonitoringConfig.class));
        Set<Method> triggered = new HashSet<>(getJobsToCheck(CronTrigger.class));
        Assertions.assertThat(monitored).containsExactlyInAnyOrderElementsOf(triggered);
    }

    @Test
    public void testSmallPeriodJobsHaveTenMinsTolerance() {
        List<Method> methods = getJobsToCheck(CronTrigger.class)
            .stream()
            .filter(m -> getEstimatedCronPeriodSec(m) < ONE_DAY_IN_SECS)
            .collect(Collectors.toList());

        methods.forEach(method -> {
            int failsToCrit = norm(method.getAnnotation(MonitoringConfig.class).failsToCritCount());
            long periodSec = getEstimatedCronPeriodSec(method);
            long multiplication = failsToCrit * periodSec;
            log.info("Job {} considered a short-period with T = {} min.", method.getName(), (float) periodSec / 60);

            assertNotEquals(String.format("In job '%s' periodSec (%d) must be != %d",
                method.getName(), periodSec, 0),
                0, periodSec);
            assertTrue(String.format("In job '%s' failsToCrit * periodSec (%d*%d=%d) must be >= %d",
                method.getName(), failsToCrit, periodSec, multiplication, TEN_MINS_IN_SECS),
                multiplication >= TEN_MINS_IN_SECS);
            assertTrue(String.format("In job '%s' failsToCrit * periodSec (%d*%d=%d) must be <= %d",
                method.getName(), failsToCrit, periodSec, multiplication, ONE_DAY_IN_SECS),
                multiplication <= ONE_DAY_IN_SECS);
        });
    }

    @Test
    public void testDayPeriodJobsHaveOneFailThreshold() {
        List<Method> methods = getJobsToCheck(CronTrigger.class)
            .stream()
            .filter(m -> getEstimatedCronPeriodSec(m) >= ONE_DAY_IN_SECS)
            .collect(Collectors.toList());

        methods.forEach(method -> {
            int failsToCrit = norm(method.getAnnotation(MonitoringConfig.class).failsToCritCount());
            long periodSec = getEstimatedCronPeriodSec(method);
            log.info("Job {} considered a day-period with T = {} hours.",
                method.getName(), (float) periodSec / 60 / 60);

            assertNotEquals(String.format("In job '%s' periodSec (%d) must be != %d",
                method.getName(), periodSec, 0),
                0, periodSec);
            assertEquals(String.format("In job '%s' failsToCrit (%d) must be = %d",
                method.getName(), failsToCrit, 1),
                1, failsToCrit);
        });
    }

    @Test
    public void testAllCroneExpressions() {
        var methods = getMethodsAnnotatedWith(CronTrigger.class);
        for (var method : methods) {
            var cronTrigger = method.getAnnotation(CronTrigger.class);
            var name = method.getName();
            var crone = cronTrigger.cronExpression();

            try {
                var triggerFactory = new CronTriggerFactoryBean();
                triggerFactory.setName(name);
                triggerFactory.setCronExpression(crone);
                triggerFactory.afterPropertiesSet();
            } catch (ParseException e) {
                throw new CronTriggerCreationException(name, crone, e);
            }
        }
    }

    private long getEstimatedCronPeriodSec(Method job) {
        String cronExpression = job.getAnnotation(CronTrigger.class).cronExpression();
        try {
            CronExpression expression = new CronExpression(cronExpression);
            Date next1 = expression.getNextValidTimeAfter(
                new GregorianCalendar(2036, Calendar.JANUARY, 1).getTime());
            Date next2 = expression.getNextValidTimeAfter(next1);
            long diffInMillis = Math.abs(next2.getTime() - next1.getTime());
            return TimeUnit.SECONDS.convert(diffInMillis, TimeUnit.MILLISECONDS);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private List<Method> getJobsToCheck(Class<? extends Annotation> annotation) {
        List<Method> scheduled = getMethodsAnnotatedWith(annotation);
        Set<String> ignoredNames = getMethodsAnnotatedWith(MonitoringConfig.class).stream()
            .filter(m -> {
                var config = m.getAnnotation(MonitoringConfig.class);
                return !config.isSkipHealthCheck();
            })
            .map(Method::getName)
            .collect(Collectors.toSet());
        return scheduled.stream().filter(j -> !ignoredNames.contains(j.getName())).collect(Collectors.toList());
    }

    private static int norm(int value) {
        return value == UNDEFINED_NUMBER_VALUE ? 1 : value;
    }

    private static List<Method> getMethodsAnnotatedWith(Class<? extends Annotation> annotation) {
        return allExistingMethods.stream()
            .filter(method -> method.isAnnotationPresent(annotation))
            .collect(Collectors.toList());
    }
}
