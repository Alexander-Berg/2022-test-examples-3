package ru.yandex.market.api.geo.parser;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.api.domain.v2.GeoSuggest;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.util.ResourceHelpers;

import java.util.List;

import static ru.yandex.market.api.matchers.GeoSuggestMatcher.fullName;
import static ru.yandex.market.api.matchers.GeoSuggestMatcher.id;
import static ru.yandex.market.api.matchers.GeoSuggestMatcher.suggest;

public class GeoSuggestsParserTest extends UnitTestBase {

    @Test
    public void getParsed() {
        GeoSuggestsParser parser = new GeoSuggestsParser();
        List<GeoSuggest> geoSuggests = parser.parse(ResourceHelpers.getResource("suggest-maps__suggest-geo-tune-list.json"));

        Assert.assertEquals(7, geoSuggests.size());
        Assert.assertThat(
            geoSuggests,
            Matchers.containsInAnyOrder(
                suggest(
                    id(119038),
                    fullName("Николаевка (Уфимский район, Республика Башкортостан, Россия)")
                ),
                suggest(
                    id(101244),
                    fullName("Николаевка (Николаевский район, Ульяновская область, Россия)")
                ),
                suggest(
                    id(121906),
                    fullName("Николаевка (Городской округ Саранск, Республика Мордовия, Россия)")
                ),
                suggest(
                    id(103561),
                    fullName("Николаевка (Симферопольский район)")
                ),
                suggest(
                    id(119023),
                    fullName("Николаевка (Смидовичский район, Еврейская автономная область, Россия)")
                ),
                suggest(
                    id(119022),
                    fullName("Николаевка (Партизанский район, Приморский край, Россия)")
                ),
                suggest(
                    id(119020),
                    fullName("Николаевка (Елизовский район, Камчатский край, Россия)")
                )
            )
        );
    }
}
