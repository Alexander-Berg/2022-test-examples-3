package ru.yandex.direct.testing.matchers.result;

import org.junit.Ignore;

import static org.junit.Assert.assertThat;

/**
 * Для того, чтобы посмотреть выдачу в различных ситуациях,
 * переиспользует кейсы из теста флагов, при этом оверрайдит сам тест таким образом,
 * чтобы использовать матчер внутри Assert.assertThat, всегда ожидая совпадения.
 * В результате ожидаемо падает на всех кейсах, где совпадения быть не должно и показывает вывод матчера.
 */
@Ignore("Визуальный тест текстового вывода матчера")
public class MassResultMatcherFlagsOutputTest extends MassResultMatcherFlagsTest {

    @Override
    public void matches_worksFine() {
        assertThat("ожидаемое падение", resultDescription.getResult(),
                matcherDescription.getMassResultMatcher());
    }
}
