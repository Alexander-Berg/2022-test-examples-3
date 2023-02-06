package ru.yandex.market.api.opinion.fact;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.api.domain.v2.opinion.fact.FactStat;
import ru.yandex.market.api.domain.v2.opinion.fact.FactsSummary;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.matchers.FactorStatMatcher;
import ru.yandex.market.api.matchers.FactorSummaryMatcher;
import ru.yandex.market.api.util.ResourceHelpers;


public class FactSummaryParserTest extends UnitTestBase {
    private FactsSummaryParser parser;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        parser = new FactsSummaryParser();
    }

    @Test
    public void factorSummary() {
        FactsSummary summary = parse("model-fact-summary.json");


        Matcher<FactStat> factorStat1 = FactorStatMatcher.facts(
            FactorStatMatcher.id(745),
            FactorStatMatcher.title("Объем памяти"),
            FactorStatMatcher.count(121),
            FactorStatMatcher.value(4.3140497)
        );

        Matcher<FactStat> factorStat2 = FactorStatMatcher.facts(
            FactorStatMatcher.id(744),
            FactorStatMatcher.title("Время автономной работы"),
            FactorStatMatcher.count(121),
            FactorStatMatcher.value(4.2231407)
        );

        Assert.assertThat(
            summary,
            FactorSummaryMatcher.summary(
                FactorSummaryMatcher.recommendedRatio(Matchers.is(0.8403361345)),
                FactorSummaryMatcher.facts(
                    cast(
                        Matchers.containsInAnyOrder(
                            factorStat1,
                            factorStat2
                        )
                    )
                )
            )
        );
    }

    @Test
    public void factorSummariWhenRecommendedIsNull() {
        FactsSummary summary = parse("model-fact-summary-when-recommend-is-null.json");
        Assert.assertEquals(0.0, summary.getRecommendedRatio(), 0.0);
    }

    private FactsSummary parse(String filename) {
        return parser.parse(ResourceHelpers.getResource(filename));
    }

    private static <T> Matcher<T> cast(Matcher<?> matcher) {
        return (Matcher<T>) matcher;
    }
}
