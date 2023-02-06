package ru.yandex.market.tpl.tms.executor;

import java.util.HashSet;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import ru.yandex.market.tms.quartz2.spring.CronTrigger;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class TplExecutorsPackagesTest {

    @DisplayName("Проверить, что в пакете executor лежат классы и все они наследуются от TplTmsExecutor")
    @Test
    public void check() {
        String packageStr = "ru.yandex.market.tpl.tms.executor";

        Reflections reflections = new Reflections(
                new ConfigurationBuilder()
                        .setUrls(ClasspathHelper.forPackage(packageStr))
                        .setScanners(new SubTypesScanner(), new TypeAnnotationsScanner())
        );

        //Классы, помеченные аннотацией CronTrigger
        Set<Class<?>> allExecutorWithCronTrigger = reflections.getTypesAnnotatedWith(CronTrigger.class);

        //Классы, которые наследуют TplTmsExecutor и на них висит аннотация CronTrigger
        Set<Class<? extends TplTmsExecutor>> allExecutorExtendedTplTmsExecutor =
                reflections.getSubTypesOf(TplTmsExecutor.class);
        Set<Class<?>> allExecutorWithTrulyImplementation = new HashSet<>();
        allExecutorExtendedTplTmsExecutor.forEach(
                aClass -> {
                    if (aClass.isAnnotationPresent(CronTrigger.class)) {
                        allExecutorWithTrulyImplementation.add(aClass);
                    }
                }
        );

        if (!allExecutorWithTrulyImplementation.containsAll(allExecutorWithCronTrigger)) {
            var difference = new HashSet<>(allExecutorWithCronTrigger);
            difference.removeAll(allExecutorWithTrulyImplementation);
            difference.forEach(aClass ->
                    log.error(aClass + " не наследуется от TplTmsExecutor")
            );
        }

        assertTrue(allExecutorWithTrulyImplementation.containsAll(allExecutorWithCronTrigger));
    }

}
