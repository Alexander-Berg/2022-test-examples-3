package ru.yandex.direct.testing.matchers.result;

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithItems;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithMatchers;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcherBaseTest.MatcherDescription.matcherDesc;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcherBaseTest.ResultDescription.resultDesc;

@RunWith(Parameterized.class)
public class MassResultMatcherItemsTest extends MassResultMatcherBaseTest {

    @Parameters(name = "Matcher: {0}. Result: {1}. Expected match: {2}.")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                // полное совпадение
                {
                        matcherDesc(isSuccessfulWithMatchers(equalTo(0L), equalTo(1L))),
                        resultDesc(true, asList(true, true)),
                        true
                },

                // проверка флагов по наличию матчеров

                // null - значит, результат должен быть положительным
                // not null - значит, результат должен быть отрицательным
                {
                        matcherDesc(isSuccessfulWithMatchers(equalTo(0L), null)),
                        resultDesc(true, asList(true, false)),
                        true
                },
                {
                        matcherDesc(isSuccessfulWithMatchers(null, equalTo(1L))),
                        resultDesc(true, asList(false, true)),
                        true
                },
                {
                        matcherDesc(isSuccessfulWithMatchers(null, null)),
                        resultDesc(true, asList(false, false)),
                        true
                },

                // передача null-матчера для существующего результата приводит к несовпадению
                {
                        matcherDesc(isSuccessfulWithMatchers(equalTo(0L), null)),
                        resultDesc(true, asList(true, true)),
                        false
                },
                {
                        matcherDesc(isSuccessfulWithMatchers(null, equalTo(1L))),
                        resultDesc(true, asList(true, true)),
                        false
                },

                // передача not-null-матчера для несуществующего результата приводит к несовпадению
                {
                        matcherDesc(isSuccessfulWithMatchers(equalTo(0L), equalTo(1L))),
                        resultDesc(true, asList(true, false)),
                        false
                },

                // несовпадение отдельных матчеров
                {
                        matcherDesc(isSuccessfulWithMatchers(equalTo(0L), greaterThan(1L))),
                        resultDesc(true, asList(true, true)),
                        false
                },
                {
                        matcherDesc(isSuccessfulWithMatchers(equalTo(0L), equalTo(1L))),
                        resultDesc(true, asList(true, false)),
                        false
                },

                // несовпадение количества элементов
                {
                        matcherDesc(isSuccessfulWithMatchers(equalTo(0L), equalTo(1L))),
                        resultDesc(true, asList(true, true, true)),
                        false
                },
                {
                        matcherDesc(isSuccessfulWithMatchers(equalTo(0L), equalTo(1L))),
                        resultDesc(true, singletonList(true)),
                        false
                },

                // не фэйлится, если результат отрицательный
                {
                        matcherDesc(isSuccessfulWithMatchers(equalTo(0L), equalTo(1L))),
                        resultDesc(false, asList(true, true)),
                        false
                },
                {
                        matcherDesc(isSuccessfulWithMatchers(null, null)),
                        resultDesc(false, asList(true, false)),
                        false
                },

                // работа через упрощенный фабричный метод
                {
                        matcherDesc(isSuccessfulWithItems(0L, 1L)),
                        resultDesc(true, asList(true, true)),
                        true
                },
                {
                        matcherDesc(isSuccessfulWithItems(0L, 2L)),
                        resultDesc(true, asList(true, true)),
                        false
                }
        });
    }

    @Test
    public void matches_worksFine() {
        super.matches_worksFine();
    }
}
