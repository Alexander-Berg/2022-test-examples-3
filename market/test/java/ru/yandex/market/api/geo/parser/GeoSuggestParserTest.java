package ru.yandex.market.api.geo.parser;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.api.domain.v2.GeoSuggest;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.util.ResourceHelpers;

public class GeoSuggestParserTest extends UnitTestBase {
    @Test
    public void shouldParse() {
        GeoSuggestParser parser = new GeoSuggestParser();
        GeoSuggest suggest = parser.parse(ResourceHelpers.getResource("suggest-maps__suggest-geo-tune.json"));

        Assert.assertEquals("Николаевка", suggest.getName());
        Assert.assertEquals(119023, suggest.getId());
        Assert.assertEquals("Николаевка (Смидовичский район, Еврейская автономная область, Россия)", suggest.getFullName());
        Assert.assertEquals("Смидовичский район, Еврейская автономная область, Россия", suggest.getSubtitle());
    }
}
