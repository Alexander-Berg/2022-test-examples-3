package ru.yandex.common.geocoder.client;

import java.io.IOException;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;

import org.apache.http.client.methods.HttpUriRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import yandex.maps.proto.common2.response.ResponseOuterClass;

import ru.yandex.common.geocoder.model.response.GeoObject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Тесты на кэширующийся GeoSearchApiClient")
class GeoSearchApiClientCachedTest extends AbstractGeoSearchClientTest {

    private final GeoClient geoSearchClient = GeoClientBuilder.newBuilder().withApiBaseUrl(GEO_SEARCH_API_URL)
            .withHttpClient(client).withTvmTicketProvider(Optional::empty).withIsCached(true).build();

    @Test
    @DisplayName("Поиск топонимов с дефолтными параметрами")
    void findGeoObjectsDefaultParamsTest() throws IOException {
        ResponseOuterClass.Response jsonForGeoObject = createProtobufResponseObject(
                "response/single_geoobject_geocoding_response.json"
        );
        when(client.execute(any(HttpUriRequest.class))).thenReturn(createHttpResponse(jsonForGeoObject));
        GeoObject firstResult = Objects.requireNonNull(geoSearchClient.findFirst(QUERY))
                .orElseThrow(RuntimeException::new);
        GeoObject shouldBeCachedResult = Objects.requireNonNull(geoSearchClient.findFirst(QUERY))
                .orElseThrow(RuntimeException::new);

        assertThat(firstResult)
                .isEqualTo(shouldBeCachedResult)
                .isEqualTo(createSingleGeoObject());
        verify(client).execute(argThat(uriMatcher.apply(URI.create(
                GEO_SEARCH_API_URL + ENCODED_QUERY + "&lang=ru-RU&ms=pb&type=geo"
        ))));
    }
}
