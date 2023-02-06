package ru.yandex.market.logistics.management.controller.lgw.callback;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.common.geocoder.client.GeoClient;
import ru.yandex.common.geocoder.model.request.GeoSearchParams;
import ru.yandex.common.geocoder.model.response.AddressInfo;
import ru.yandex.common.geocoder.model.response.AreaInfo;
import ru.yandex.common.geocoder.model.response.Boundary;
import ru.yandex.common.geocoder.model.response.CountryInfo;
import ru.yandex.common.geocoder.model.response.Kind;
import ru.yandex.common.geocoder.model.response.LocalityInfo;
import ru.yandex.common.geocoder.model.response.Precision;
import ru.yandex.common.geocoder.model.response.SimpleGeoObject;
import ru.yandex.common.geocoder.model.response.ToponymInfo;
import ru.yandex.market.logistics.management.AbstractContextualTest;

import static com.github.springtestdbunit.annotation.DatabaseOperation.INSERT;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

@DatabaseSetup("/data/controller/lgwCallback/getReferenceTimetableCouriers/before/prepare_data.xml")
class GetReferenceTimetableCouriersControllerTest extends AbstractContextualTest {
    private static final GeoSearchParams GEO_SEARCH_PARAMS = GeoSearchParams.builder()
        .withMinimalPrecision(Precision.ALL)
        .withPreferredLanguage(GeoSearchParams.Language.RU)
        .build();

    @Autowired
    private GeoClient geoClient;

    @AfterEach
    void teardown() {
        verifyNoMoreInteractions(geoClient);
    }

    @SneakyThrows
    @Test
    @ExpectedDatabase(
        value = "/data/controller/lgwCallback/getReferenceTimetableCouriers/after/one_snapshot.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void getReferenceTimetableCouriersSuccess() {
        configureGeoClientMock();

        mockMvc.perform(put("/lgw_callback/get_reference_timetable_couriers_success")
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent("data/controller/lgw/callback/get_reference_timetable_couriers_success.json")))
            .andExpect(status().isOk());

        verifyGeoClientMock();
    }

    @SneakyThrows
    @Test
    @DatabaseSetup(
        value = "/data/controller/lgwCallback/getReferenceTimetableCouriers/before/snapshots.xml",
        type = INSERT
    )
    @ExpectedDatabase(
        value = "/data/controller/lgwCallback/getReferenceTimetableCouriers/after/two_snapshots.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void getReferenceTimetableCouriersSuccessWithOldSnapshots() {
        configureGeoClientMock();

        mockMvc.perform(put("/lgw_callback/get_reference_timetable_couriers_success")
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent("data/controller/lgw/callback/get_reference_timetable_couriers_success.json")))
            .andExpect(status().isOk());

        verifyGeoClientMock();
    }

    @SneakyThrows
    @Test
    @ExpectedDatabase(
        value = "/data/controller/lgwCallback/getReferenceTimetableCouriers/after/one_snapshot.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void getReferenceTimetableCouriersSuccessWithLocationIdLocationFoundInGeosearch() {
        configureGeoClientMock();

        mockMvc.perform(put("/lgw_callback/get_reference_timetable_couriers_success")
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent(
                "data/controller/lgw/callback/get_reference_timetable_couriers_success_with_location_id.json"
            )))
            .andExpect(status().isOk());

        verifyGeoClientMock();
    }

    @SneakyThrows
    @Test
    @ExpectedDatabase(
        value = "/data/controller/lgwCallback/getReferenceTimetableCouriers/after/one_snapshot_location_id_66.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void getReferenceTimetableCouriersSuccessWithLocationIdLocationNotFoundInGeosearch() {
        mockMvc.perform(put("/lgw_callback/get_reference_timetable_couriers_success")
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent(
                "data/controller/lgw/callback/get_reference_timetable_couriers_success_with_location_id.json"
            )))
            .andExpect(status().isOk());
        verifyGeoClientMock();
    }

    @SneakyThrows
    @Test
    @ExpectedDatabase(
        value = "/data/controller/lgwCallback/getReferenceTimetableCouriers/before/prepare_data.xml",
        assertionMode = NON_STRICT
    )
    void getReferenceTimetableCouriersSuccessLocationIdNotFound() {
        mockMvc.perform(put("/lgw_callback/get_reference_timetable_couriers_success")
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent("data/controller/lgw/callback/get_reference_timetable_couriers_success.json")))
            .andExpect(status().isOk());
        verifyGeoClientMock();
    }

    @SneakyThrows
    @Test
    @ExpectedDatabase(
        value = "/data/controller/lgwCallback/getReferenceTimetableCouriers/before/prepare_data.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void getReferenceTimetableCouriersError() {
        mockMvc.perform(put("/lgw_callback/get_reference_timetable_couriers_error")
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent(
                "data/controller/lgw/callback/get_reference_timetable_couriers_error.json"
            )))
            .andExpect(status().isOk());
    }

    @SneakyThrows
    @Test
    @DatabaseSetup(
        "/data/controller/lgwCallback/getReferenceTimetableCouriers/before/prepare_data_with_old_snapshots.xml"
    )
    @ExpectedDatabase(
        value = "/data/controller/lgwCallback/getReferenceTimetableCouriers/before/prepare_data_with_old_snapshots.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void addingSnapshotWithTheSameContentsNoChange() {
        configureGeoClientMock();
        mockMvc.perform(put("/lgw_callback/get_reference_timetable_couriers_success")
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent(
                "data/controller/lgw/callback/equal_contents_snapshot.json"
            )))
            .andExpect(status().isOk());
        verifyGeoClientMock();
    }

    @SneakyThrows
    @Test
    @ExpectedDatabase(
        value = "/data/controller/lgwCallback/getReferenceTimetableCouriers/after/duplicate_locations_in_one.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void mergeDuplicateLocations() {
        configureGeoClientMock();
        mockMvc.perform(put("/lgw_callback/get_reference_timetable_couriers_success")
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent(
                "data/controller/lgw/callback/snapshot_with_duplicates.json"
            )))
            .andExpect(status().isOk());
        verifyGeoClientMock(1, 2);
    }

    private void configureGeoClientMock() {
        when(geoClient.find(
            eq("Россия, Новосибирская область, Новосибирск"),
            refEq(GEO_SEARCH_PARAMS)
        ))
            .thenReturn(List.of(
                    SimpleGeoObject.newBuilder()
                            .withToponymInfo(ToponymInfo.newBuilder()
                                    .withGeoid("65")
                                    .withKind(Kind.LOCALITY)
                                    .build()
                            )
                            .withAddressInfo(AddressInfo.newBuilder()
                                    .withCountryInfo(CountryInfo.newBuilder()
                                            .build()
                                    )
                                    .withAreaInfo(AreaInfo.newBuilder()
                                            .build()
                                    )
                                    .withLocalityInfo(LocalityInfo.newBuilder()
                                            .build()
                                    )
                                    .build()
                            )
                            .withBoundary(Boundary.newBuilder()
                                    .build()
                            )
                            .build()
            ));

        when(geoClient.find(
            eq("Россия, Москва и Московская область, Москва"),
            refEq(GEO_SEARCH_PARAMS)
        ))
            .thenReturn(List.of(
                    SimpleGeoObject.newBuilder()
                            .withToponymInfo(ToponymInfo.newBuilder()
                                    .withGeoid("213")
                                    .withKind(Kind.LOCALITY)
                                    .build()
                            )
                            .withAddressInfo(AddressInfo.newBuilder()
                                    .withCountryInfo(CountryInfo.newBuilder()
                                            .build()
                                    )
                                    .withAreaInfo(AreaInfo.newBuilder()
                                            .build()
                                    )
                                    .withLocalityInfo(LocalityInfo.newBuilder()
                                            .build()
                                    )
                                    .build()
                            )
                            .withBoundary(Boundary.newBuilder()
                                    .build()
                            )
                            .build()
            ));
    }

    private void verifyGeoClientMock() {
        verifyGeoClientMock(1, 1);
    }

    private void verifyGeoClientMock(int novosibirskInvokeTimes, int moscowInvokeTimes) {
        verify(geoClient, Mockito.times(novosibirskInvokeTimes)).find(
            eq("Россия, Новосибирская область, Новосибирск"),
            refEq(GEO_SEARCH_PARAMS)
        );

        verify(geoClient, Mockito.times(moscowInvokeTimes)).find(
            eq("Россия, Москва и Московская область, Москва"),
            refEq(GEO_SEARCH_PARAMS)
        );
    }
}
