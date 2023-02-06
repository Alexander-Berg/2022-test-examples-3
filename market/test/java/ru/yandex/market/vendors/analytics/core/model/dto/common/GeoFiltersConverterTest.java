package ru.yandex.market.vendors.analytics.core.model.dto.common;

import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.vendors.analytics.core.model.common.GeoFilters;
import ru.yandex.market.vendors.analytics.core.model.dto.common.geo.GeoFiltersConverter;
import ru.yandex.market.vendors.analytics.core.model.dto.common.geo.GeoFiltersDTO;
import ru.yandex.market.vendors.analytics.core.model.region.CityPopulation;
import ru.yandex.market.vendors.analytics.core.model.region.ClickhouseCityType;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author antipov93.
 */
public class GeoFiltersConverterTest {

    @Test
    void simpleGeoFilter() {
        var expected = GeoFilters.builder()
                .federalDistrictIds(Set.of(1L))
                .federalSubjectIds(Set.of())
                .clickhouseCityTypes(Set.of())
                .build();

        var actual = GeoFiltersConverter.convert(new GeoFiltersDTO(Set.of(1L), null, null));
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Если в фильтре выбраны все возможные населения городов, то не учитываем их")
    void allCitiesPopulations() {
        var expected = GeoFilters.builder()
                .federalDistrictIds(Set.of())
                .federalSubjectIds(Set.of())
                .clickhouseCityTypes(Set.of())
                .build();

        var actual = GeoFiltersConverter.convert(new GeoFiltersDTO(null, null, EnumSet.allOf(CityPopulation.class)));
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Города миллионники включают в себя Москву и Санкт-Петербург")
    void mksAndSobAreMillionaires() {
        var expected = GeoFilters.builder()
                .federalDistrictIds(Set.of())
                .federalSubjectIds(Set.of())
                .clickhouseCityTypes(Set.of(
                        ClickhouseCityType.MOSCOW, ClickhouseCityType.SAINT_PETERSBURG,
                        ClickhouseCityType.POPULATION_MORE_1M)
                )
                .build();

        var actual = GeoFiltersConverter.convert(new GeoFiltersDTO(null, null, Set.of(CityPopulation.MILLION)));
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Севастополь учитывается в городах меньше 500 тысяч")
    void sevastopolIsLess500k() {

        var expected = GeoFilters.builder()
                .federalDistrictIds(Set.of())
                .federalSubjectIds(Set.of())
                .clickhouseCityTypes(Set.of(ClickhouseCityType.POPULATION_LESS_500K, ClickhouseCityType.SEVASTOPOL))
                .build();

        var actual = GeoFiltersConverter.convert(
                new GeoFiltersDTO(null, null, Set.of(CityPopulation.LESS_HALF_MILLION))
        );
        assertEquals(expected, actual);
    }
}
