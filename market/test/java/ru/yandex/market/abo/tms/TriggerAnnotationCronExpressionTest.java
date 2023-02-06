package ru.yandex.market.abo.tms;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.springframework.core.annotation.AnnotationUtils;

import ru.yandex.market.abo.core.dynamic.service.CronExpressionsTest;
import ru.yandex.market.tms.quartz2.spring.CronTrigger;

/**
 * @author komarovns
 * @date 26.09.18
 */
public class TriggerAnnotationCronExpressionTest extends CronExpressionsTest {
    private static final String PACKAGE = "ru.yandex.market.abo.tms";

    @Override
    public List<String> getCronExpressions() {
        return Stream.concat(cronConfigsFromClasses(), cronConfigsFromMethods())
                .map(CronTrigger::cronExpression)
                .collect(Collectors.toList());
    }

    private Stream<CronTrigger> cronConfigsFromClasses() {
        return new Reflections(PACKAGE).getTypesAnnotatedWith(CronTrigger.class).stream()
                .map(clazz -> AnnotationUtils.findAnnotation(clazz, CronTrigger.class));
    }

    private Stream<CronTrigger> cronConfigsFromMethods() {
        return new Reflections(PACKAGE, new MethodAnnotationsScanner())
                .getMethodsAnnotatedWith(CronTrigger.class).stream()
                .map(method -> AnnotationUtils.findAnnotation(method, CronTrigger.class));
    }
}
