package ru.yandex.direct.core.entity.vcard.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.configuration.CoreTest;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class GeoRegionLookupTest {

    private static final String MOSCOW_NAME_RU = "Москва";
    private static final Long MOSCOW_ID = 213L;

    private static final String SPB_NAME_EN = "Saint Petersburg";
    private static final Long SPB_ID = 2L;

    private static final String DUPLICATED_NAME = "Троицк";

    @Autowired
    private GeoRegionLookup geoRegionLookup;

    @Test
    public void getRegionIdsByCities_ExistingRussianCity_ReturnsRegion() {
        Map<String, Long> cities = geoRegionLookup.getRegionIdsByCities(ImmutableSet.of(MOSCOW_NAME_RU));
        assertThat(cities.get(MOSCOW_NAME_RU), is(MOSCOW_ID));
    }

    @Test
    public void getRegionIdsByCities_ExistingEnglishCity_ReturnsRegion() {
        Map<String, Long> cities = geoRegionLookup.getRegionIdsByCities(ImmutableSet.of(SPB_NAME_EN));
        assertThat(cities.get(SPB_NAME_EN), is(SPB_ID));
    }

    @Test
    public void getRegionIdsByCities_UnExistingCity_ReturnsNull() {
        String name = "abra kadabra";
        Map<String, Long> cities = geoRegionLookup.getRegionIdsByCities(ImmutableSet.of(name));
        assertThat(cities.size(), is(0));
    }

    @Test
    public void getRegionIdsByCities_ExistingDuplicatedCity_ReturnsNull() {
        Map<String, Long> cities = geoRegionLookup.getRegionIdsByCities(ImmutableSet.of(DUPLICATED_NAME));
        assertThat(cities.size(), is(0));
    }

    @Test
    public void getRegionIdsByCities_SeveralExistingCitiesAndUnexisting_ReturnsNull() {
        String unexistingName = "abra kadabra";
        Set<String> nameSet = ImmutableSet.of(DUPLICATED_NAME, unexistingName, MOSCOW_NAME_RU, SPB_NAME_EN);
        Map<String, Long> cities = geoRegionLookup.getRegionIdsByCities(nameSet);
        assertThat(cities.get(MOSCOW_NAME_RU), is(MOSCOW_ID));
        assertThat(cities.get(SPB_NAME_EN), is(SPB_ID));
        assertThat(cities.get(unexistingName), nullValue());
        assertThat(cities.get(DUPLICATED_NAME), nullValue());
        assertThat(cities.size(), is(2));
    }

    @Test
    public void getRegionIdsByCountries_SimpleCountries_ReturnCountries() {
        Map<String, Long> customCountries = new HashMap<>();
        customCountries.put("Россия", 225L);
        customCountries.put("Казахстан", 159L);
        customCountries.put("Беларусь", 149L);
        customCountries.put("Турция", 983L);
        customCountries.put("Бельгия", 114L);
        customCountries.put("Италия", 205L);

        Map<String, Long> regionIds = geoRegionLookup.getRegionIdsByCountries(customCountries.keySet());
        for (String country : customCountries.keySet()) {
            assertThat(customCountries.get(country), is(regionIds.get(country)));
        }
    }
}
