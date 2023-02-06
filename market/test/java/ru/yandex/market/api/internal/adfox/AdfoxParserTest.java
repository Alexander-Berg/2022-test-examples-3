package ru.yandex.market.api.internal.adfox;

import org.hamcrest.Matchers;
import org.junit.Test;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.util.ResourceHelpers;

import java.util.List;

import static org.junit.Assert.assertThat;
import static ru.yandex.market.api.matchers.AdfoxMatcher.adfox;
import static ru.yandex.market.api.matchers.AdfoxMatcher.attribute;

public class AdfoxParserTest extends UnitTestBase {
    @Test
    public void adfoxParse() {
        List<AdfoxAttributes> adfoxData = parse("adfox.json");

        assertThat(
            adfoxData,
            adfox(
                Matchers.containsInAnyOrder(
                    attribute("ew0KIC"),
                    attribute("ew0KICAg")
                )
            )
        );

    }

    private static List<AdfoxAttributes> parse(String filename) {
        AdfoxParser parser = new AdfoxParser();
        return parser.parse(ResourceHelpers.getResource(filename));
    }
}
