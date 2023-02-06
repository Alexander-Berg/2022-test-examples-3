package ru.yandex.market.mboc.common.masterdata.parsing.utils;

import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class GeobaseCountryUtilTest {

    @Test
    public void whenSearchByCanonicalNameShouldReturnCountry() {
        GeobaseCountry china = new GeobaseCountry(134, "Китай", "CN");
        GeobaseCountry russia = new GeobaseCountry(225, "Россия", "RU");

        Optional<GeobaseCountry> foundChina1 = GeobaseCountryUtil.countryByName(" \tкиТай \n");
        Optional<GeobaseCountry> foundChina2 = GeobaseCountryUtil.countryByName("Китай");
        Optional<GeobaseCountry> foundRussia1 = GeobaseCountryUtil.countryByName(" россиЯ ");
        Optional<GeobaseCountry> foundRussia2 = GeobaseCountryUtil.countryByName("Россия");
        Assertions.assertThat(foundChina1).contains(china);
        Assertions.assertThat(foundChina2).contains(china);
        Assertions.assertThat(foundRussia1).contains(russia);
        Assertions.assertThat(foundRussia2).contains(russia);
    }

    @Test
    public void whenSearchByGeoIdShouldReturnCountry() {
        GeobaseCountry china = new GeobaseCountry(134, "Китай", "CN");
        GeobaseCountry russia = new GeobaseCountry(225, "Россия", "RU");

        Optional<GeobaseCountry> foundChina1 = GeobaseCountryUtil.countryByGeoId(134);
        Optional<GeobaseCountry> foundRussia1 = GeobaseCountryUtil.countryByGeoId(225);
        Assertions.assertThat(foundChina1).contains(china);
        Assertions.assertThat(foundRussia1).contains(russia);
    }

    @Test
    public void whenSearchBySynonymShouldReturnCountry() {
        GeobaseCountry china = new GeobaseCountry(134, "Китай", "CN");
        GeobaseCountry russia = new GeobaseCountry(225, "Россия", "RU");

        Optional<GeobaseCountry> foundChina = GeobaseCountryUtil.countryByName(" \tкнр \n");
        Optional<GeobaseCountry> foundRussia1 = GeobaseCountryUtil.countryByName(" российская феДерация  ");
        Optional<GeobaseCountry> foundRussia2 = GeobaseCountryUtil.countryByName(" Рф  ");
        Assertions.assertThat(foundChina).contains(china);
        Assertions.assertThat(foundRussia1).contains(russia);
        Assertions.assertThat(foundRussia2).contains(russia);
    }

    @Test
    public void whenSearchByInvalidIdentifiersShouldReturnEmpty() {
        Assertions.assertThat(GeobaseCountryUtil.countryByName(null)).isEmpty();
        Assertions.assertThat(GeobaseCountryUtil.countryByName("")).isEmpty();
        Assertions.assertThat(GeobaseCountryUtil.countryByName("  \t \n  ")).isEmpty();
        Assertions.assertThat(GeobaseCountryUtil.countryByName("Эквестрия")).isEmpty();
        Assertions.assertThat(GeobaseCountryUtil.countryByGeoId(-508)).isEmpty();
        Assertions.assertThat(GeobaseCountryUtil.countryByGeoId(0)).isEmpty();
        Assertions.assertThat(GeobaseCountryUtil.countryByGeoId(786)).isEmpty();
    }
}
