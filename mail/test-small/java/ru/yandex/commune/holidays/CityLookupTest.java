package ru.yandex.commune.holidays;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import ru.yandex.inside.geobase.GeobaseIds;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CityLookupTest {

    @Test
    void lookupCountryByCity() {
        Optional<Integer> germanyId = CityLookup.lookupCountryByCity("177");
        Optional<Integer> czechRepublicId = CityLookup.lookupCountryByCity("10511");

        assertTrue(germanyId.isPresent());
        assertEquals(germanyId.get(), GeobaseIds.GERMANY);
        assertTrue(czechRepublicId.isPresent());
        assertEquals(czechRepublicId.get(), GeobaseIds.CZECH_REPUBLIC);
        assertTrue(CityLookup.lookupCountryByCity("joke").isEmpty());
    }
}
