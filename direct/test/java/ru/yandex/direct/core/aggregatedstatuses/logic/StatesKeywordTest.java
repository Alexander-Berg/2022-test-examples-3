package ru.yandex.direct.core.aggregatedstatuses.logic;

import java.util.Collection;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.aggregatedstatuses.keyword.KeywordStatesEnum;
import ru.yandex.direct.core.entity.keyword.aggrstatus.StatusAggregationKeyword;
import ru.yandex.direct.core.entity.keyword.model.StatusModerate;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class StatesKeywordTest {
    private static KeywordStates keywordStatesCalculator;

    @Parameterized.Parameter
    public StatusAggregationKeyword keyword;

    @Parameterized.Parameter(1)
    public Collection<KeywordStatesEnum> expectedStates;

    @Parameterized.Parameters(name = "{index}: => States: {1}")
    public static Object[][] params() {
        return new Object[][]{
                {new StatusAggregationKeyword(),
                        List.of()},

                {new StatusAggregationKeyword()
                        .withIsSuspended(true),
                        List.of(KeywordStatesEnum.SUSPENDED)},

                {new StatusAggregationKeyword()
                        .withStatusModerate(StatusModerate.NEW),
                        List.of(KeywordStatesEnum.DRAFT)},

                {new StatusAggregationKeyword()
                        .withStatusModerate(StatusModerate.NO),
                        List.of(KeywordStatesEnum.REJECTED)},
        };
    }

    @BeforeClass
    public static void prepare() {
        keywordStatesCalculator = new KeywordStates();
    }

    @Test
    public void test() {
        Collection<KeywordStatesEnum> states = keywordStatesCalculator.calc(keyword);

        assertEquals("got right states", states, expectedStates);
    }
}
