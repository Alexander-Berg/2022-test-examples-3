package ru.yandex.market.api.opinion.fact;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.api.domain.v2.opinion.fact.Fact;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.matchers.FactMatcher;
import ru.yandex.market.api.util.ResourceHelpers;

import java.util.List;

public class FactJsonListParserTest extends UnitTestBase {
    private FactJsonListParser parser;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        parser = new FactJsonListParser();
    }

    @Test
    public void parseFactors() {
        List<Fact> facts = parse("fact-list.json");
        Assert.assertThat(
            facts,
            Matchers.containsInAnyOrder(
                FactMatcher.facts(
                    FactMatcher.id(742),
                    FactMatcher.title("Экран"),
                    FactMatcher.description("Экран - очень важная часть телефона"),
                    FactMatcher.value(null)
                ),
                FactMatcher.facts(
                    FactMatcher.id(743),
                    FactMatcher.title("Камера"),
                    FactMatcher.description(null),
                    FactMatcher.value(null)
                ),
                FactMatcher.facts(
                    FactMatcher.id(746),
                    FactMatcher.title("Производительность"),
                    FactMatcher.description(""),
                    FactMatcher.value(null)
                )
            )
        );
    }

    private List<Fact> parse(String filename) {
        return parser.parse(ResourceHelpers.getResource(filename));
    }
}
