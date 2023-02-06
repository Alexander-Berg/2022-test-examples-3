package ru.yandex.market.logistics.nesu.base;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.common.geocoder.client.GeoClient;
import ru.yandex.common.geocoder.model.response.AddressInfo;
import ru.yandex.common.geocoder.model.response.AreaInfo;
import ru.yandex.common.geocoder.model.response.Boundary;
import ru.yandex.common.geocoder.model.response.CountryInfo;
import ru.yandex.common.geocoder.model.response.LocalityInfo;
import ru.yandex.common.geocoder.model.response.SimpleGeoObject;
import ru.yandex.common.geocoder.model.response.ToponymInfo;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

public abstract class AbstractPostalCodeSearchTest extends AbstractContextualTest {

    private static final String ADDRESS = "Novosibirsk";
    private static final String QUERY = "Russia," + ADDRESS;

    @Autowired
    private GeoClient geoClient;

    @Test
    @DisplayName("Пустой ответ гео-поиска")
    void emptyResult() throws Exception {
        when(geoClient.find(QUERY)).thenReturn(List.of());

        search(ADDRESS)
            .andExpect(status().isOk())
            .andExpect(content().json(EMPTY_ARRAY));

        verify(geoClient).find(QUERY);
    }

    @Test
    @DisplayName("Результаты с индексом и без")
    void mixedResult() throws Exception {
        when(geoClient.find(QUERY)).thenReturn(List.of(
            SimpleGeoObject.newBuilder()
                .withToponymInfo(ToponymInfo.newBuilder().build())
                .withAddressInfo(AddressInfo.newBuilder()
                    .withCountryInfo(CountryInfo.newBuilder().build())
                    .withAreaInfo(AreaInfo.newBuilder().build())
                    .withLocalityInfo(LocalityInfo.newBuilder().build())
                    .build()
                )
                .withBoundary(Boundary.newBuilder().build())
                .build(),
            SimpleGeoObject.newBuilder()
                .withToponymInfo(ToponymInfo.newBuilder().build())
                .withAddressInfo(AddressInfo.newBuilder()
                    .withCountryInfo(CountryInfo.newBuilder().build())
                    .withAreaInfo(AreaInfo.newBuilder().build())
                    .withLocalityInfo(LocalityInfo.newBuilder()
                        .withPostalCode("630001")
                        .build()
                    )
                    .build()
                )
                .withBoundary(Boundary.newBuilder().build())
                .build()
        ));

        search(ADDRESS)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/location/response/postal_code.json"));
    }

    protected abstract ResultActions search(String address) throws Exception;

}
