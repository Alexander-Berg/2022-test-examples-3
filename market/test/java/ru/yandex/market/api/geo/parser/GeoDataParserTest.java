package ru.yandex.market.api.geo.parser;

import it.unimi.dsi.fastutil.ints.IntList;
import org.junit.Test;
import ru.yandex.market.api.geo.GeoData;
import ru.yandex.market.api.geo.domain.GeoRegion;
import ru.yandex.market.api.geo.domain.RegionType;
import ru.yandex.market.api.geo.domain.Suggest;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.util.ResourceHelpers;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
public class GeoDataParserTest extends UnitTestBase {

    @Test
    public void shouldParseGeodata() {
        GeoData geoData = new GeoDataParser().parse(ResourceHelpers.getResource("geo-data-stripped.xml"));

        GeoRegion earth = geoData.getRegion(1);

        assertEquals("Земля", earth.getName());
        assertEquals("Землю", earth.getNameAccusative());
        assertEquals("Земли", earth.getNameGenitive());
        assertEquals("Земля", earth.getFullName());
        assertEquals(RegionType.OTHER, earth.getType());

        GeoRegion russia = geoData.getRegion(2);
        assertEquals(1, (int) russia.getParentId());
        assertEquals(2, (int) russia.getChildrenCount());

        IntList children = geoData.getChildren(2);
        assertEquals(2, children.size());
        assertEquals(3, children.getInt(0));
        assertEquals(5, children.getInt(1));

        List<Suggest> suggests = geoData.getSuggests("мо");
        assertEquals(2, suggests.size());

        Suggest moscowSuggest = suggests.get(0);
        assertEquals("Москва", moscowSuggest.getRegion().getName());

        Suggest morkovSuggest = suggests.get(1);
        assertEquals("Морковь", morkovSuggest.getRegion().getName());
    }
}
