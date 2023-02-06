package ru.yandex.market.api.geo.parser;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.api.geo.domain.GeoAddress;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.util.ResourceHelpers;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.market.api.matchers.GeoAddressMatcher.*;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
public class GeoAddressParserTest extends UnitTestBase {

    @Test
    public void shouldParseOneAddressXml() {
        List<GeoAddress> list = parse("one-address.xml");

        Assert.assertEquals(1, list.size());

        GeoAddress address = list.get(0);
        Assert.assertEquals("Россия", address.getCountry());
        Assert.assertEquals("Свердловская область", address.getRegion());
        Assert.assertEquals("городской округ Екатеринбург", address.getSubRegion());
        Assert.assertEquals("Екатеринбург", address.getLocality());
        Assert.assertEquals(null, address.getSubLocality());
        Assert.assertEquals("улица Хохрякова", address.getThoroughfare());
        Assert.assertEquals("10", address.getPremiseNumber());
    }

    @Test
    public void shouldParseManyAddressesXml() throws Exception {
        List<GeoAddress> list = parse("many-addresses.xml");

        Assert.assertEquals(13, list.size());

        GeoAddress address = list.get(0);
        Assert.assertEquals(54L, address.getRegionId());
        Assert.assertEquals("Россия", address.getCountry());
        Assert.assertEquals("Свердловская область", address.getRegion());
        Assert.assertEquals("городской округ Екатеринбург", address.getSubRegion());
        Assert.assertEquals("Екатеринбург", address.getLocality());
        Assert.assertEquals("микрорайон Втузгородок", address.getSubLocality());
        Assert.assertEquals("улица Мира", address.getThoroughfare());
        Assert.assertEquals("19", address.getPremiseNumber());

        address = list.get(1);
        Assert.assertEquals(54L, address.getRegionId());
        Assert.assertEquals("Россия", address.getCountry());
        Assert.assertEquals("Свердловская область", address.getRegion());
        Assert.assertEquals("городской округ Екатеринбург", address.getSubRegion());
        Assert.assertEquals("Екатеринбург", address.getLocality());
        Assert.assertEquals("поселок Чусовское Озеро", address.getSubLocality());
        Assert.assertEquals("улица Мира", address.getThoroughfare());
        Assert.assertEquals("19", address.getPremiseNumber());
    }

    @Test
    public void shouldParseMoscowAddressXml() throws Exception {
        List<GeoAddress> list = parse("moscow.xml");

        Assert.assertEquals(1, list.size());

        GeoAddress address = list.get(0);
        Assert.assertEquals(20279L, address.getRegionId());
        Assert.assertEquals("Россия", address.getCountry());
        Assert.assertEquals("Центральный федеральный округ", address.getRegion());
        Assert.assertEquals(null, address.getSubRegion());
        Assert.assertEquals("Москва", address.getLocality());
        Assert.assertEquals("Центральный административный округ", address.getSubLocality());
        Assert.assertEquals("Красная площадь", address.getThoroughfare());
        Assert.assertEquals("1", address.getPremiseNumber());
    }

    @Test
    public void shouldParseSanktPeterburgAddressXml() throws Exception {
        List<GeoAddress> list = parse("sankt-peterburg.xml");

        Assert.assertEquals(1, list.size());

        GeoAddress address = list.get(0);
        Assert.assertEquals(2L, address.getRegionId());
        Assert.assertEquals("Россия", address.getCountry());
        Assert.assertEquals("Северо-Западный федеральный округ", address.getRegion());
        Assert.assertEquals(null, address.getSubRegion());
        Assert.assertEquals("Санкт-Петербург", address.getLocality());
        Assert.assertEquals(null, address.getSubLocality());
        Assert.assertEquals("Невский проспект", address.getThoroughfare());
        Assert.assertEquals("12", address.getPremiseNumber());
    }

    @Test
    public void shouldParseZelenogradAddressXml() throws Exception {
        List<GeoAddress> list = parse("zelenograd-house.xml");

        Assert.assertEquals(1, list.size());

        GeoAddress address = list.get(0);
        Assert.assertEquals(216L, address.getRegionId());
        Assert.assertEquals("Россия", address.getCountry());
        Assert.assertEquals("Центральный федеральный округ", address.getRegion());
        Assert.assertEquals("Москва", address.getSubRegion());
        Assert.assertEquals("Зеленоград", address.getLocality());
        Assert.assertEquals(null, address.getSubLocality());
        Assert.assertEquals(null, address.getThoroughfare());
        Assert.assertEquals("к333", address.getPremiseNumber());
    }

    @Test
    public void shouldParseChertanovoAddressXml() {
        List<GeoAddress> list = parse("chertanovo-street-house.xml");

        assertThat(list, hasSize(1));

        assertThat(
            list.get(0),
            geoAddress(
                regionId(120579),
                country("Россия"),
                region("Москва"),
                subregion("микрорайон Северное Чертаново"),
                locality("Москва"),
                premiseNumber("3кБ")
            )
        );
    }

    private List<GeoAddress> parse(String filename) {
        return new GeoAddressParser().parse(
            ResourceHelpers.getResource(filename)
        );
    }

}
