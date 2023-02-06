package ru.yandex.direct.testing.matchers.result;

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFailed;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcherBaseTest.MatcherDescription.matcherDesc;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcherBaseTest.ResultDescription.resultDesc;

@RunWith(Parameterized.class)
public class MassResultMatcherFlagsTest extends MassResultMatcherBaseTest {

    @Parameters(name = "Matcher: {0}. Result: {1}. Expected match: {2}.")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                // проверка глобального флага успешности результата
                {
                        matcherDesc(MassResultMatcher.isSuccessful()),
                        resultDesc(true, asList(true, true)),
                        true
                },
                {
                        matcherDesc(MassResultMatcher.isSuccessful()),
                        resultDesc(true, asList(true, false)),
                        true
                },
                {
                        matcherDesc(MassResultMatcher.isSuccessful()),
                        resultDesc(true, asList(false, false)),
                        true
                },
                {
                        matcherDesc(MassResultMatcher.isSuccessful()),
                        resultDesc(false, asList(true, true)),
                        false
                },
                {
                        matcherDesc(isFailed()),
                        resultDesc(false, asList(true, true)),
                        true
                },
                {
                        matcherDesc(isFailed()),
                        resultDesc(false, asList(false, true)),
                        true
                },
                {
                        matcherDesc(isFailed()),
                        resultDesc(true, asList(false, false)),
                        false
                },
                {
                        matcherDesc(isFailed()),
                        resultDesc(true, asList(true, true)),
                        false
                },

                // проверка всех флагов элементов одной опцией
                {
                        matcherDesc(isFullySuccessful()),
                        resultDesc(true, asList(true, true)),
                        true
                },
                {
                        matcherDesc(isFullySuccessful()),
                        resultDesc(false, asList(true, true)),
                        false
                },
                {
                        matcherDesc(isFullySuccessful()),
                        resultDesc(true, asList(true, false)),
                        false
                },
                {
                        matcherDesc(isFullySuccessful()),
                        resultDesc(true, asList(false, false)),
                        false
                },

                // проверка флагов отдельных элементов - положительный результат

                // все сходится
                {
                        matcherDesc(isSuccessful(true)),
                        resultDesc(true, singletonList(true)),
                        true
                },
                {
                        matcherDesc(isSuccessful(true, true)),
                        resultDesc(true, asList(true, true)),
                        true
                },

                // не сходится глобальный флаг
                {
                        matcherDesc(isSuccessful(true, true)),
                        resultDesc(false, asList(true, true)),
                        false
                },

                // не сходятся флаги элементов
                {
                        matcherDesc(isSuccessful(true)),
                        resultDesc(true, singletonList(false)),
                        false
                },
                {
                        matcherDesc(isSuccessful(false)),
                        resultDesc(true, singletonList(true)),
                        false
                },
                {
                        matcherDesc(isSuccessful(true, true)),
                        resultDesc(true, asList(true, false)),
                        false
                },
                {
                        matcherDesc(isSuccessful(true, true)),
                        resultDesc(true, asList(false, true)),
                        false
                },
                {
                        matcherDesc(isSuccessful(true, true)),
                        resultDesc(true, asList(false, false)),
                        false
                },

                // не сходятся ни глобальный, ни флаги элементов
                {
                        matcherDesc(isSuccessful(true, true)),
                        resultDesc(false, asList(false, false)),
                        false
                },

                // не сходится количество элементов
                {
                        matcherDesc(isSuccessful(true, true, true)),
                        resultDesc(true, asList(false, false)),
                        false
                },
                {
                        matcherDesc(isSuccessful(true, true, true)),
                        resultDesc(true, emptyList()),
                        false
                },

                // несколько дополнительных проверок с ожиданием отрицательного элемента
                {
                        matcherDesc(isSuccessful(true, false)),
                        resultDesc(true, asList(true, false)),
                        true
                },
                {
                        matcherDesc(isSuccessful(true, false)),
                        resultDesc(true, asList(false, true)),
                        false
                },
                {
                        matcherDesc(isSuccessful(true, false)),
                        resultDesc(false, asList(true, false)),
                        false
                }
        });
    }

    @Test
    public void matches_worksFine() {
        super.matches_worksFine();
    }
}
