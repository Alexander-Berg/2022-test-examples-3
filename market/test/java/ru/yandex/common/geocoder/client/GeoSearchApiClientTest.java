package ru.yandex.common.geocoder.client;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.google.common.collect.ImmutableSet;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import yandex.maps.proto.common2.response.ResponseOuterClass;

import ru.yandex.common.geocoder.model.request.GeoSearchParams;
import ru.yandex.common.geocoder.model.request.QueryHints;
import ru.yandex.common.geocoder.model.response.GeoObject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GeoSearchApiClientTest extends AbstractGeoSearchClientTest {

    private final GeoClient geoSearchClient = GeoClientBuilder.newBuilder().withApiBaseUrl(GEO_SEARCH_API_URL)
            .withHttpClient(client).withTvmTicketProvider(Optional::empty).build();

    @Test
    @DisplayName("Поиск топонимов с дефолтными параметрами")
    void findGeoObjectsDefaultParamsTest() throws IOException {
        ResponseOuterClass.Response jsonForGeoObject = createProtobufResponseObject(
                "response/single_geoobject_geocoding_response.json"
        );
        when(client.execute(any(HttpUriRequest.class))).thenReturn(createHttpResponse(jsonForGeoObject));
        GeoObject result = Objects.requireNonNull(geoSearchClient.findFirst(QUERY)).orElseThrow(RuntimeException::new);

        assertThat(result).isEqualTo(createSingleGeoObject());
        verify(client).execute(argThat(uriMatcher.apply(URI.create(
                GEO_SEARCH_API_URL + ENCODED_QUERY + "&lang=ru-RU&ms=pb&type=geo"
        ))));
    }

    @Test
    @DisplayName("Поиск топонима обратное геокодирование")
    void findGeoObjectBackwardGeocodingTest() throws IOException {
        ResponseOuterClass.Response jsonForGeoObject = createProtobufResponseObject(
                "response/multi_geoobjects_geocoding_response.json"
        );
        when(client.execute(any(HttpUriRequest.class))).thenReturn(createHttpResponse(jsonForGeoObject));

        List<GeoObject> result = geoSearchClient.find("83.110567,54.85803", GeoSearchParams.DEFAULT);

        assertThat(result).containsExactlyInAnyOrderElementsOf(createMultiGeoObjects());
        verify(client).execute(argThat(uriMatcher.apply(URI.create(
                GEO_SEARCH_API_URL + URLEncoder.encode("83.110567,54.85803", StandardCharsets.UTF_8.name())
                        + "&lang=ru-RU&ms=pb&type=geo"
        ))));
    }

    @Test
    @DisplayName("Поиск топонимов с кастомными параметрами")
    void findGeoObjectsCustomParamsTest() throws IOException {
        ResponseOuterClass.Response jsonForGeoObject = createProtobufResponseObject(
                "response/empty_geocoding_response.json"
        );
        when(client.execute(any(HttpUriRequest.class))).thenReturn(createHttpResponse(jsonForGeoObject));

        GeoSearchParams params = GeoSearchParams.builder()
                .withRegionId(1)
                .withSearchAreaCenter(new GeoSearchParams.Coordinates(1, 1))
                .withSearchAreaSize(new GeoSearchParams.SearchAreaSize(1, 1))
                .withSearchAreaLimited(true)
                .build();

        List<GeoObject> result = geoSearchClient.find(QUERY, params);

        assertThat(result).isEmpty();
        verify(client).execute(argThat(uriMatcher.apply(
                URI.create(GEO_SEARCH_API_URL + ENCODED_QUERY
                        + "&lang=ru-RU&ms=pb&type=geo&lr=1&ll=1.000000%2C1.000000&spn=1.000000%2C1.000000&rspn=1")
        )));
    }

    @Test
    @DisplayName("Поиск топонима с дополнительными параметрами")
    void findGeoObjectQueryHintsParamsTest() throws IOException {
        ResponseOuterClass.Response jsonForGeoObject = createProtobufResponseObject(
                "response/single_geoobject_geocoding_response.json"
        );
        QueryHints hints = QueryHints.segmentedAddress(ImmutableSet.copyOf(Arrays.asList("Новосибирск", "Николаева")));

        when(client.execute(any(HttpUriRequest.class))).thenReturn(createHttpResponse(jsonForGeoObject));

        List<GeoObject> result = geoSearchClient.find(QUERY, GeoSearchParams.DEFAULT, hints);

        assertThat(result).containsExactlyInAnyOrder(createSingleGeoObject());
        verify(client).execute(argThat(uriMatcher.apply(
                hints.augmentUrl(URI.create(GEO_SEARCH_API_URL + ENCODED_QUERY + "&lang=ru-RU&ms=pb&type=geo")
                ))));
    }
}
