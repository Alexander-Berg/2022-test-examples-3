package ru.yandex.market.logistics.nesu.base;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.common.geocoder.client.GeoClient;
import ru.yandex.common.geocoder.model.response.AddressInfo;
import ru.yandex.common.geocoder.model.response.AreaInfo;
import ru.yandex.common.geocoder.model.response.Boundary;
import ru.yandex.common.geocoder.model.response.Component;
import ru.yandex.common.geocoder.model.response.CountryInfo;
import ru.yandex.common.geocoder.model.response.GeoObject;
import ru.yandex.common.geocoder.model.response.Kind;
import ru.yandex.common.geocoder.model.response.LocalityInfo;
import ru.yandex.common.geocoder.model.response.SimpleGeoObject;
import ru.yandex.common.geocoder.model.response.ToponymInfo;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

public abstract class AbstractLocationSearchTest extends AbstractContextualTest {
    private static final Set<Kind> DELIVERY_LOCATION_KINDS = EnumSet.of(
        Kind.LOCALITY,
        Kind.DISTRICT
    );

    @Autowired
    private GeoClient geoSearchClient;
    @Captor
    private ArgumentCaptor<String> queryCaptor;

    @Test
    @DisplayName("Пустой ответ от геокодера")
    void emptyResult() throws Exception {
        search("test")
            .andExpect(status().isOk())
            .andExpect(content().json(EMPTY_ARRAY));

        verify(geoSearchClient).find(queryCaptor.capture());
        softly.assertThat(queryCaptor.getValue()).isEqualTo("Russia,test");
    }

    @Test
    @DisplayName("Ответы неподходящего типа")
    void noSuitableResult() throws Exception {
        List<GeoObject> locations = Stream.of(Kind.values())
            .filter(kind -> !DELIVERY_LOCATION_KINDS.contains(kind))
            .map(kind -> SimpleGeoObject.newBuilder()
                .withToponymInfo(ToponymInfo.newBuilder()
                    .withGeoid("1")
                    .withComponents(List.of(
                        new Component("Russia", List.of(Kind.COUNTRY)),
                        new Component("test", List.of(kind))
                    ))
                    .build()
                )
                .withAddressInfo(AddressInfo.newBuilder()
                    .withAddressLine("Invalid component type")
                    .withCountryInfo(CountryInfo.newBuilder().build())
                    .withAreaInfo(AreaInfo.newBuilder().build())
                    .withLocalityInfo(LocalityInfo.newBuilder().build())
                    .build()
                )
                .withBoundary(Boundary.newBuilder().build())
                .build()
            )
            .collect(Collectors.toList());

        when(geoSearchClient.find(any()))
            .thenReturn(locations);

        search("invalid")
            .andExpect(status().isOk())
            .andExpect(content().json(EMPTY_ARRAY));

        verify(geoSearchClient).find(queryCaptor.capture());
        softly.assertThat(queryCaptor.getValue()).isEqualTo("Russia,invalid");
    }

    @Test
    @DisplayName("Подходящие ответы")
    void validResult() throws Exception {
        List<GeoObject> locations = List.of(
            SimpleGeoObject.newBuilder()
                .withToponymInfo(ToponymInfo.newBuilder()
                    .withGeoid("1")
                    .withComponents(List.of(
                        new Component("Russia", List.of(Kind.COUNTRY)),
                        new Component("Novosibirsk Area", List.of(Kind.PROVINCE)),
                        new Component("Novosibirsk", List.of(Kind.LOCALITY))
                    ))
                    .build()
                )
                .withAddressInfo(AddressInfo.newBuilder()
                    .withAddressLine("Locality")
                    .withCountryInfo(CountryInfo.newBuilder().build())
                    .withAreaInfo(AreaInfo.newBuilder().build())
                    .withLocalityInfo(LocalityInfo.newBuilder().build())
                    .build()
                )
                .withBoundary(Boundary.newBuilder().build())
                .build(),
            SimpleGeoObject.newBuilder()
                .withToponymInfo(ToponymInfo.newBuilder()
                    .withGeoid("1")
                    .withComponents(List.of(
                        new Component("Russia", List.of(Kind.COUNTRY)),
                        new Component("Novosibirsk Area", List.of(Kind.PROVINCE)),
                        new Component("Novosibirsk", List.of(Kind.LOCALITY)),
                        new Component("Akademgorodok", List.of(Kind.DISTRICT))
                    ))
                    .build()
                )
                .withAddressInfo(AddressInfo.newBuilder()
                    .withAddressLine("District")
                    .withCountryInfo(CountryInfo.newBuilder().build())
                    .withAreaInfo(AreaInfo.newBuilder().build())
                    .withLocalityInfo(LocalityInfo.newBuilder().build())
                    .build()
                )
                .withBoundary(Boundary.newBuilder().build())
                .build()
        );

        when(geoSearchClient.find(any()))
            .thenReturn(locations);

        search("valid")
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/api/location/location_search.json"));

        verify(geoSearchClient).find(queryCaptor.capture());
        softly.assertThat(queryCaptor.getValue()).isEqualTo("Russia,valid");
    }

    @Test
    @DisplayName("В некоторых объектах не указаны обязательные поля")
    void invalidResult() throws Exception {
        List<GeoObject> locations = List.of(
            SimpleGeoObject.newBuilder()
                .withToponymInfo(ToponymInfo.newBuilder()
                    .withGeoid("1")
                    .withComponents(List.of(
                        new Component("Novosibirsk Area", List.of(Kind.PROVINCE)),
                        new Component("Novosibirsk", List.of(Kind.LOCALITY))
                    ))
                    .build()
                )
                .withAddressInfo(AddressInfo.newBuilder()
                    .withAddressLine("Locality")
                    .withCountryInfo(CountryInfo.newBuilder().build())
                    .withAreaInfo(AreaInfo.newBuilder().build())
                    .withLocalityInfo(LocalityInfo.newBuilder().build())
                    .build()
                )
                .withBoundary(Boundary.newBuilder()
                    .build()
                )
                .build(),
            SimpleGeoObject.newBuilder()
                .withToponymInfo(ToponymInfo.newBuilder()
                    .withGeoid("1")
                    .withComponents(List.of(
                        new Component("Russia", List.of(Kind.COUNTRY)),
                        new Component("Novosibirsk", List.of(Kind.LOCALITY)),
                        new Component("Akademgorodok", List.of(Kind.DISTRICT))
                    ))
                    .build()
                )
                .withAddressInfo(AddressInfo.newBuilder()
                    .withAddressLine("District")
                    .withCountryInfo(CountryInfo.newBuilder().build())
                    .withAreaInfo(AreaInfo.newBuilder().build())
                    .withLocalityInfo(LocalityInfo.newBuilder().build())
                    .build()
                )
                .withBoundary(Boundary.newBuilder()
                    .build()
                )
                .build(),
            SimpleGeoObject.newBuilder()
                .withToponymInfo(ToponymInfo.newBuilder()
                    .withGeoid("1")
                    .withComponents(List.of(
                        new Component("Russia", List.of(Kind.COUNTRY)),
                        new Component("Novosibirsk Area", List.of(Kind.PROVINCE)),
                        new Component("Akademgorodok", List.of(Kind.DISTRICT))
                    ))
                    .build()
                )
                .withAddressInfo(AddressInfo.newBuilder()
                    .withAddressLine("District")
                    .withCountryInfo(CountryInfo.newBuilder().build())
                    .withAreaInfo(AreaInfo.newBuilder().build())
                    .withLocalityInfo(LocalityInfo.newBuilder().build())
                    .build()
                )
                .withBoundary(Boundary.newBuilder().build())
                .build(),
            SimpleGeoObject.newBuilder()
                .withToponymInfo(ToponymInfo.newBuilder()
                    .withGeoid("1")
                    .withComponents(List.of(
                        new Component("Russia", List.of(Kind.COUNTRY)),
                        new Component("Novosibirsk Area", List.of(Kind.PROVINCE)),
                        new Component("Novosibirsk", List.of(Kind.LOCALITY)),
                        new Component("Akademgorodok", List.of(Kind.DISTRICT))
                    ))
                    .build()
                )
                .withAddressInfo(AddressInfo.newBuilder()
                    .withAddressLine("District")
                    .withCountryInfo(CountryInfo.newBuilder().build())
                    .withAreaInfo(AreaInfo.newBuilder().build())
                    .withLocalityInfo(LocalityInfo.newBuilder().build())
                    .build()
                )
                .withBoundary(Boundary.newBuilder().build())
                .build()
        );

        when(geoSearchClient.find(any()))
            .thenReturn(locations);

        search("valid")
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/api/location/search_without_fields.json"));

        verify(geoSearchClient).find(queryCaptor.capture());
        softly.assertThat(queryCaptor.getValue()).isEqualTo("Russia,valid");
    }

    @Test
    @DisplayName("Тест на хак для поиска Москвы")
    void moscowProvince() throws Exception {
        String requestText = "Москва";
        List<GeoObject> locations = List.of(
            SimpleGeoObject.newBuilder()
                .withToponymInfo(ToponymInfo.newBuilder()
                    .withGeoid("213")
                    .withComponents(List.of(
                        new Component("Россия", List.of(Kind.COUNTRY)),
                        new Component(requestText, List.of(Kind.PROVINCE))
                    ))
                    .build()
                )
                .withAddressInfo(AddressInfo.newBuilder()
                    .withAddressLine(requestText)
                    .withCountryInfo(CountryInfo.newBuilder().build())
                    .withAreaInfo(AreaInfo.newBuilder().build())
                    .withLocalityInfo(LocalityInfo.newBuilder().build())
                    .build()
                )
                .withBoundary(Boundary.newBuilder().build())
                .build()
        );
        when(geoSearchClient.find("Russia," + requestText))
            .thenReturn(locations);
        search(requestText)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/api/location/moscow_province.json"));
    }

    protected abstract ResultActions search(String term) throws Exception;

}
