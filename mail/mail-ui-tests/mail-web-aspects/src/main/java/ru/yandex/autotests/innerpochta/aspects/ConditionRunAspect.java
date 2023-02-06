package ru.yandex.autotests.innerpochta.aspects;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.junit.runner.Description;
import ru.yandex.autotests.innerpochta.annotations.RunOnlyForCondition;
import ru.yandex.autotests.innerpochta.annotations.TestCategory;
import ru.yandex.autotests.innerpochta.rules.WatchRule;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;

import static org.junit.Assume.assumeTrue;
import static ru.yandex.autotests.innerpochta.util.props.UrlProps.urlProps;

/**
 * Аспект предназначен для запуска отдельных тестов, чьи категории соответствуют категориям запуска.
 * Категории запуска указываются в проперте UrlProps#runCondition с «_» в качестве разделителя.
 * Для того, чтобы тест запустился должно выолняться одно из условий:
 * а) в аннотации {@link TestCategory} к тесту должны содержаться ВСЕ категории из
 * проперти UrlProps#runCondition;
 * б) в проперте UrlProps#runCondition присутствует хотя бы один из категории, указанных в {@link RunOnlyForCondition}.
 * Тестовый класс должен иметь подключённую рулу {@link WatchRule}
 * <p>
 * Если UrlProps#runCondition null, то запускаются все тестовые классы.
 *
 * @author a-zoshchuk
 */
@Aspect
public class ConditionRunAspect {
    private HashSet<String> runConditionsString = runConditions();

    @After("execution(* ru.yandex.autotests.innerpochta.rules.WatchRule.starting(..))")
    public void IgnoreIfInappropriateEnvironment(JoinPoint joinPoint) throws Throwable {
        System.out.println(runConditions());
        Description descr = (Description) joinPoint.getArgs()[0];
        HashSet<String> testCategory = new HashSet<>();
        HashSet<String> runOnlyConditions = new HashSet<>();
        Annotation testCategoryAnnotation = descr.getAnnotation(TestCategory.class);
        Annotation runOnlyAnnotation = descr.getAnnotation(RunOnlyForCondition.class);
        if (testCategoryAnnotation != null)
            testCategory = new HashSet<>(Arrays.asList(descr.getAnnotation(TestCategory.class).value().split("_")));
        if (runOnlyAnnotation != null)
            runOnlyConditions = new HashSet<>(Arrays.asList(descr.getAnnotation(RunOnlyForCondition.class)
                .value().split("_")));
        System.out.println("Test categories: " + testCategory.toString());
        System.out.println("Test run only for conditions: " + runOnlyConditions.toString());
        if (runConditionsString.size() != 0)
            assumeTrue(
                String.format("Пак запущен только для тестов категории «%s»", runConditionsString),
                (testCategory.size() != 0) || (runOnlyConditions.size() != 0)
            );
        if (testCategory.size() != 0)
            assumeTrue(
                String.format("Тест не выполняется для условий «%s»", runConditionsString),
                testCategory.containsAll(runConditionsString)
            );
        if (runOnlyConditions.size() != 0)
            assumeTrue(
                String.format("Тест выполняется только для условий «%s»", runOnlyConditions),
                runConditionsString.stream().anyMatch(runOnlyConditions::contains)
            );
    }

    private HashSet<String> runConditions() {
        if (urlProps().getRunCondition() != null)
            return new HashSet<>(Arrays.asList(urlProps().getRunCondition().split("_")));
        return new HashSet<>();
    }
}
