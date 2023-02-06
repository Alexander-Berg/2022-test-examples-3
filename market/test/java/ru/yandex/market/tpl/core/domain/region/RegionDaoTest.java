package ru.yandex.market.tpl.core.domain.region;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import ru.yandex.common.util.region.RegionType;
import ru.yandex.market.tpl.core.CoreTest;

import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@Sql("classpath:geobase/RegionDaoTest.sql")
class RegionDaoTest {

    @Autowired
    RegionDao regionDao;

    @Test
    void shouldReturnExpectedParentRegions_WhenGetParentRegions_IfRegionsExist() {
        // given
        TplRegion moscowCity =
                new TplRegion(213, 1, "Москва", "Moscow", RegionType.CITY, 10800, 0, 55.753215, 37.622504);

        // when
        List<TplRegion> regions = regionDao.getParentRegions((long) moscowCity.getId());

        // then
        TplRegion city =
                new TplRegion(
                        110299,
                        11071,
                        "Каринторф",
                        "Karintorf",
                        RegionType.CITY_DISTRICT,
                        10800,
                        0,
                        58.552299,
                        50.187986);
        TplRegion moscowRegion =
                new TplRegion(
                        1,
                        3,
                        "Москва и Московская область",
                        "Moscow and Moscow Oblast",
                        RegionType.SUBJECT_FEDERATION,
                        10800,
                        213,
                        55.815792,
                        37.380031);
        TplRegion centralRegion =
                new TplRegion(
                        3,
                        225,
                        "Центральный федеральный округ",
                        "Central Federal District",
                        RegionType.COUNTRY_DISTRICT,
                        10800,
                        213,
                        54.873745,
                        38.064718);
        TplRegion country =
                new TplRegion(
                        225,
                        10001,
                        "Россия",
                        "Russia",
                        RegionType.COUNTRY,
                        0,
                        213,
                        61.698653,
                        99.505405);
        TplRegion continent = new TplRegion(10001, 10000, "Евразия", "Eurasia", RegionType.CONTINENT, 0, 0, 56.0, 85.0);
        TplRegion earth = new TplRegion(10000, 0, "Земля", "Earth", RegionType.OTHERS_UNIVERSAL, 0, 0, 0, 0);
        assertThat(regions).isEqualTo(List.of(moscowCity, moscowRegion, centralRegion, country, continent, earth));
    }

    @Test
    void shouldReturnTheOneOriginalRegion_WhenGetParentRegions_IfThereAreNoParentsSome() {
        // given
        TplRegion city =
                new TplRegion(
                        110299,
                        11071,
                        "Каринторф",
                        "Karintorf",
                        RegionType.CITY_DISTRICT,
                        10800,
                        0,
                        58.552299,
                        50.187986);

        // when
        List<TplRegion> regions = regionDao.getParentRegions((long) city.getId());

        // then
        assertThat(regions).isEqualTo(List.of(city));
    }
}
