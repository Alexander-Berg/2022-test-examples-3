package ru.yandex.common.geocoder.mapper.url;

import java.net.URI;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.common.geocoder.model.request.GeoSearchParams;
import ru.yandex.common.geocoder.model.response.Kind;
import ru.yandex.common.geocoder.model.response.Precision;

import static org.assertj.core.api.Assertions.assertThat;

class GeoSearchUrlCreationTest {

    private static final String GEO_SEARCH_API_URL = "http://addrs-testing.search.yandex" +
            ".net/search/stable/yandsearch?origin=market-geocoder-lib&text=";
    private static final String QUERY = "Запрос";
    private final GeoSearchUrlBuilder geoSearchUrlBuilder = new ApacheGeoSearchUrlBuilder();

    @Test
    @DisplayName("Создание Url со всеми параметрами")
    void createUrlAllParamsTest() {
        GeoSearchParams params = GeoSearchParams.builder()
                .withRegionId(1)
                .withSearchAreaCenter(new GeoSearchParams.Coordinates(1, 1))
                .withSearchAreaSize(new GeoSearchParams.SearchAreaSize(1, 1))
                .withSearchAreaLimited(true)
                .withAdditionalParameter("kind", Kind.METRO.name().toLowerCase())
                .withAdditionalParameter("precision", Precision.EXACT.name().toLowerCase())
                .build();

        URI actualUri = URI.create(String.valueOf(geoSearchUrlBuilder.prepareUrl(
                GEO_SEARCH_API_URL,
                QUERY,
                params
        )));
        assertThat(actualUri).hasHost("addrs-testing.search.yandex.net")
                .hasPath("/search/stable/yandsearch")
                .hasParameter("origin", "market-geocoder-lib")
                .hasParameter("text", QUERY)
                .hasParameter("lang", "ru-RU")
                .hasParameter("ms", "pb")
                .hasParameter("type", "geo")
                .hasParameter("lr", "1")
                .hasParameter("ll", "1.000000,1.000000")
                .hasParameter("spn", "1.000000,1.000000")
                .hasParameter("rspn", "1")
                .hasParameter("kind", Kind.METRO.name().toLowerCase())
                .hasParameter("precision", Precision.EXACT.name().toLowerCase());
    }

    @Test
    @DisplayName("Создание Url только с required параметрами")
    void createUrlRequiredParamsTest() {
        URI actualUri = URI.create(String.valueOf(geoSearchUrlBuilder.prepareUrl(
                GEO_SEARCH_API_URL,
                QUERY,
                GeoSearchParams.DEFAULT
        )));
        assertThat(actualUri).hasHost("addrs-testing.search.yandex.net")
                .hasPath("/search/stable/yandsearch")
                .hasParameter("origin", "market-geocoder-lib")
                .hasParameter("text", QUERY)
                .hasParameter("lang", "ru-RU")
                .hasParameter("ms", "pb")
                .hasParameter("type", "geo");
    }
}
