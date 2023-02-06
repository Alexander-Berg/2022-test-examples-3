package ru.yandex.market.logistics.management.service.geosearch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.common.geocoder.client.GeoClient;
import ru.yandex.common.geocoder.model.request.GeoSearchParams;
import ru.yandex.common.geocoder.model.response.AddressInfo;
import ru.yandex.common.geocoder.model.response.AreaInfo;
import ru.yandex.common.geocoder.model.response.Boundary;
import ru.yandex.common.geocoder.model.response.CountryInfo;
import ru.yandex.common.geocoder.model.response.GeoObject;
import ru.yandex.common.geocoder.model.response.Kind;
import ru.yandex.common.geocoder.model.response.LocalityInfo;
import ru.yandex.common.geocoder.model.response.Precision;
import ru.yandex.common.geocoder.model.response.SimpleGeoObject;
import ru.yandex.common.geocoder.model.response.ToponymInfo;
import ru.yandex.market.logistics.management.AbstractContextualTest;

import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.util.TestUtil.testJson;

class GeoSearchServiceTest extends AbstractContextualTest {

    private static final String URI = "/geosearch";

    @Autowired
    private GeoClient geoClient;

    @Test
    void testEmptyResponse() throws Exception {
        when(geoClient.find(eq("requestForEmptyResponse"), any(GeoSearchParams.class)))
                .thenReturn(Collections.emptyList());

        search("?geocode=requestForEmptyResponse")
                .andExpect(status().isOk())
                .andExpect(testJson("data/service/geosearch/response_empty.json"));
    }

    @Test
    void findGeoDataWithoutAdministrativeArea() throws Exception {

        when(geoClient.find(eq("Рига, Уриекстес, дом 14а"), any(GeoSearchParams.class)))
                .thenReturn(singletonList(buildWithoutAdministrativeAreaAnswer()));

        search("?geocode=Рига, Уриекстес, дом 14а")
                .andExpect(status().isOk())
                .andExpect(testJson("data/service/geosearch/response_without_AdministrativeArea.json"));
    }

    @Test
    void findGeoDataByPosition() throws Exception {
        when(geoClient.find(eq("37.617671 55.755768"), any(GeoSearchParams.class)))
                .thenReturn(singletonList(buildDataByPositionAnswer()));

        search("?geocode=37.617671 55.755768&results=1")
                .andExpect(status().isOk())
                .andExpect(testJson("data/service/geosearch/response_to_data_by_position_request.json"));
    }

    @Test
    void findGeoDataWithDependentLocality() throws Exception {
        when(geoClient.find(eq("Пресненский район, Центральный административный округ, Москва, Россия"),
                any(GeoSearchParams.class)))
                .thenReturn(singletonList(buildWithDependentLocalityAnswer()));

        search("?geocode=Пресненский район, Центральный административный округ, Москва, Россия")
                .andExpect(status().isOk())
                .andExpect(testJson("data/service/geosearch/response_with_locality_with_dependentLocality.json"));
    }

    @Test
    void findDataWithMultipleObjects() throws Exception {
        when(geoClient.find(eq("RequestForMultipleObjects"), any(GeoSearchParams.class)))
                .thenReturn(buildMultipleObjectsAnswer());

        search("?geocode=RequestForMultipleObjects")
                .andExpect(status().isOk())
                .andExpect(testJson("data/service/geosearch/response_with_multiple_objects.json"));
    }

    private GeoObject buildWithoutAdministrativeAreaAnswer() {
        return SimpleGeoObject.newBuilder()
                .withToponymInfo(ToponymInfo.newBuilder()
                        .withGeoid("11474")
                        .withPrecision(Precision.EXACT)
                        .withKind(Kind.HOUSE)
                        .withPoint("24.106147 56.989213")
                        .withGeocoderObjectId("3122080915")
                        .build()
                )
                .withAddressInfo(AddressInfo.newBuilder()
                        .withCountryInfo(CountryInfo.newBuilder()
                                .withCountryName("Латвия")
                                .withCountryCode("LV")
                                .build()
                        )
                        .withAreaInfo(AreaInfo.newBuilder()
                                .build()
                        )
                        .withLocalityInfo(LocalityInfo.newBuilder()
                                .withLocalityName("Рига")
                                .withThoroughfareName("улица Уриекстес")
                                .withPremiseNumber("14A")
                                .build()
                        )
                        .withAddressLine("Латвия, Рига, улица Уриекстес, 14A")
                        .build()
                )
                .withBoundary(Boundary.newBuilder()
                        .withEnvelopeLower("24.102042 56.986972")
                        .withEnvelopeUpper("24.110252 56.991454")
                        .build()
                )
                .withAccuracy("1.0")
                .build();
    }

    private GeoObject buildDataByPositionAnswer() {
        return SimpleGeoObject.newBuilder()
                .withToponymInfo(ToponymInfo.newBuilder()
                        .withGeoid("213")
                        .withPrecision(Precision.STREET)
                        .withKind(Kind.STREET)
                        .withPoint("37.617698 55.755783")
                        .withGeocoderObjectId("8061020")
                        .build()
                )
                .withAddressInfo(AddressInfo.newBuilder()
                        .withCountryInfo(CountryInfo.newBuilder()
                                .withCountryName("Россия")
                                .withCountryCode("RU")
                                .build()
                        )
                        .withAreaInfo(AreaInfo.newBuilder()
                                .withAdministrativeAreaName("Москва")
                                .build()
                        )
                        .withLocalityInfo(LocalityInfo.newBuilder()
                                .withLocalityName("Москва")
                                .withThoroughfareName("проезд Воскресенские Ворота")
                                .build()
                        )
                        .withAddressLine("Россия, Москва, проезд Воскресенские Ворота")
                        .build()
                )
                .withBoundary(Boundary.newBuilder()
                        .withEnvelopeLower("37.617411 55.75511")
                        .withEnvelopeUpper("37.618965 55.755935")
                        .build()
                )
                .withAccuracy("1.0")
                .build();
    }

    private GeoObject buildWithDependentLocalityAnswer() {
        return SimpleGeoObject.newBuilder()
                .withToponymInfo(ToponymInfo.newBuilder()
                        .withGeoid("120538")
                        .withPrecision(Precision.OTHER)
                        .withKind(Kind.DISTRICT)
                        .withPoint("37.562389 55.763432")
                        .withGeocoderObjectId("53211745")
                        .build()
                )
                .withAddressInfo(AddressInfo.newBuilder()
                        .withCountryInfo(CountryInfo.newBuilder()
                                .withCountryName("Россия")
                                .withCountryCode("RU")
                                .build()
                        )
                        .withAreaInfo(AreaInfo.newBuilder()
                                .withAdministrativeAreaName("Москва")
                                .build()
                        )
                        .withLocalityInfo(LocalityInfo.newBuilder()
                                .withLocalityName("Москва")
                                .withDependentLocalityName("Пресненский район")
                                .build()
                        )
                        .withAddressLine("Россия, Москва, Центральный административный округ, Пресненский район")
                        .build()
                )
                .withBoundary(Boundary.newBuilder()
                        .withEnvelopeLower("37.51441 55.744556")
                        .withEnvelopeUpper("37.609811 55.775667")
                        .build()
                )
                .withAccuracy("1.0")
                .build();
    }

    private List<GeoObject> buildMultipleObjectsAnswer() {
        List<GeoObject> answer = new ArrayList<>(3);
        answer.add(buildDataByPositionAnswer());
        answer.add(buildWithDependentLocalityAnswer());
        answer.add(buildWithoutAdministrativeAreaAnswer());
        return answer;
    }

    private ResultActions search(String paramString) throws Exception {
        return mockMvc.perform(get(URI + paramString)
                .contentType(MediaType.APPLICATION_JSON_UTF8));
    }

}
