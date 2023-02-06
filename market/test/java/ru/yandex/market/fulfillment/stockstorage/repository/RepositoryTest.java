package ru.yandex.market.fulfillment.stockstorage.repository;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reflections.Reflections;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import ru.yandex.market.fulfillment.stockstorage.util.IgnoreInRepositoryTest;


public class RepositoryTest {

    private static String message = "Method %s#%s must contains ordering in case of losing data";

    private SoftAssertions assertions;

    @BeforeEach
    public void initSoftAssertions() {
        assertions = new SoftAssertions();
    }

    @AfterEach
    public void triggerSoftAssertions() {
        assertions.assertAll();
    }

    protected SoftAssertions assertions() {
        return assertions;
    }

    @Test
    public void checkUsingPageable() {
        List<Method> methodsWithPageable = getRepositoryMethods()
                .filter(m -> Arrays.asList(m.getParameterTypes()).contains(Pageable.class))
                .collect(Collectors.toList());

        assertions
                .assertThat(methodsWithPageable.isEmpty())
                .as(methodsWithPageable
                        .stream()
                        .map(this::getMethodName)
                        .collect(Collectors.joining(", ")))
                .overridingErrorMessage("Error: do not use Pageable due to DELIVERY-7013")
                .isTrue();

    }

    private String getMethodName(Method method) {
        return method.getDeclaringClass().getSimpleName() + "#" + method.getName();
    }

    /**
     * Тест проверяет наличие сортивки в запросах к БД использующих пагинацию.
     * Использование пагинации(LIMIT) без сортировки возможны пропуски повторения данных.
     * При необходимости использовать именно такие запросы можно на метод повесить аннотацию
     * {@link IgnoreInRepositoryTest}
     */
    @Test
    public void checkSortingOnPageableQueries() {
        getRepositoryMethods()
                .filter(this::shouldContainOrdering)
                .forEach(method -> assertions()
                        .assertThat(hasOrdering(method))
                        .as(getMethodName(method))
                        .overridingErrorMessage("Error: must contain ordering in case of losing data")
                        .isTrue()
                );
    }

    /**
     * Тест проверяет использование стримов в запросакх к БД. К сожалению, в рамках разбора DELIVERY-6571
     * было обнаружено что при использовании стримов, ResultSet все равно загружается полностью в память приложения.
     * За сим считаем что данное поведение неочевидно
     * При необходимости использовать именно такие запросы можно на метод повесить аннотацию
     * {@link IgnoreInRepositoryTest}
     */
    @Test
    public void checkStreamingInRepositories() {
        getRepositoryMethods().forEach(
                method -> assertions()
                        .assertThat(method.getReturnType().equals(Stream.class))
                        .as(getMethodName(method))
                        .overridingErrorMessage("Error: you should not use Stream in repositories. It does not work " +
                                "as " +
                                "expected.")
                        .isFalse()
        );

    }

    private Stream<Method> getRepositoryMethods() {
        Reflections reflections = new Reflections("ru.yandex.market.fulfillment.stockstorage");
        return reflections.getTypesAnnotatedWith(Repository.class).stream()
                .flatMap(c -> Arrays.stream(c.getDeclaredMethods()))
                .filter(m -> !m.isAnnotationPresent(IgnoreInRepositoryTest.class));
    }

    private boolean shouldContainOrdering(Method method) {
        Class<?>[] parameters = method.getParameterTypes();
        boolean paramsContainPageable = false;
        for (Class<?> parameter : parameters) {
            if (parameter.equals(Pageable.class)) {
                paramsContainPageable = true;
                break;
            }
        }
        Query query = method.getAnnotation(Query.class);
        boolean queryContainsLimit = false;
        if (query != null) {
            queryContainsLimit = query.value().toUpperCase().contains("LIMIT")
                    && !query.value().toUpperCase().contains("LIMIT 1");
        }
        return paramsContainPageable || queryContainsLimit;
    }

    private boolean hasOrdering(Method method) {
        Query query = method.getAnnotation(Query.class);
        boolean hasOrderByInQuery = false;
        boolean hasOrderByInName = false;

        if (query != null) {
            hasOrderByInQuery = query.value().toUpperCase().contains("ORDER BY");
        } else {
            hasOrderByInName = method.getName().toUpperCase().contains("ORDERBY");
        }
        return hasOrderByInQuery || hasOrderByInName;
    }

}

