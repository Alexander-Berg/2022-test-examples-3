package ru.yandex.market.mbi.tms.monitor;

import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.stream.Collectors;

import org.reflections.Reflections;

import ru.yandex.market.tms.quartz2.model.Executor;

/**
 * Утилитный класс для проверки, что все классы в указанном пакете реализующие {@link Executor},
 * должны реализовывать интерфейс {@link MonitorFriendly}.
 *
 * @author vbudnev
 */
public class MonitorFriendlyExecutorUtils {

    public static void assertExecutorsAreFriendly(String packageName, Set<Class<? extends Executor>> ignoredClasses) {
        Reflections reflections = new Reflections(packageName);
        Set<Class<? extends Executor>> executors = reflections.getSubTypesOf(Executor.class);
        Set<Class<? extends MonitorFriendly>> friendly = reflections.getSubTypesOf(MonitorFriendly.class);

        if (executors.isEmpty()) {
            throw new AssertionError("Список тестируемых экзекьютеров пуст.");
        }

        executors.removeAll(friendly);
        executors.removeAll(ignoredClasses);

        Set<Class<? extends Executor>> notFriendly = executors.stream()
                .filter(x -> !Modifier.isAbstract(x.getModifiers()))
                .filter(x -> !isAnonymousAndFromIgnoredClass(x, ignoredClasses))
                .collect(Collectors.toSet());


        if (!notFriendly.isEmpty()) {

            String notFriendlyMsg = notFriendly.stream()
                    .map(Class::getSimpleName)
                    .sorted()
                    .collect(Collectors.joining("\n"));

            String assertionMsg = String.format(
                    "%s executors in package %s found not monitoring friendly :\n%s",
                    notFriendly.size(),
                    packageName,
                    notFriendlyMsg
            );

            throw new AssertionError(assertionMsg);
        }
    }

    // в новом VerboseExecutor'e, который приезжает в тест из common-mds-s3-tms
    // есть анонимный класс, который нужно скипнуть
    private static boolean isAnonymousAndFromIgnoredClass(
            Class<? extends Executor> notFriendlyClass,
            Set<Class<? extends Executor>> ignoredClasses
    ) {
        if (notFriendlyClass.isAnonymousClass()) {
            for (Class<? extends Executor> ignoredClass : ignoredClasses) {
                if (notFriendlyClass.getSuperclass().equals(ignoredClass)) {
                    return true;
                }
            }
        }

        return false;
    }
}
