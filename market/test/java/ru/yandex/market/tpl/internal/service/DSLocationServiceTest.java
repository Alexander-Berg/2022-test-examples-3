package ru.yandex.market.tpl.internal.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.ds.DeliveryServiceRegionRepository;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint.ofLatLon;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@ExtendWith(SpringExtension.class)
class DSLocationServiceTest {

    @MockBean
    private DeliveryServiceRegionRepository deliveryServiceRegionRepository;
    @MockBean
    private ConfigurationProviderAdapter configurationProviderAdapter;
    @MockBean
    private SortingCenterService sortingCenterService;

    private DSLocationService dsLocationService;

    @BeforeEach
    void init() {
        dsLocationService = new DSLocationService(deliveryServiceRegionRepository, configurationProviderAdapter,
                sortingCenterService);
    }

    @Test
    void getDefaultDsId() {
        given(configurationProviderAdapter.getValueAsLong(ConfigurationProperties.DS_LOCATION_SERVICE_DEFAULT_DS_ID))
                .willReturn(Optional.of(123L));
        Optional<Long> defaultDsId = dsLocationService.getDefaultDsId();
        assertThat(defaultDsId).contains(123L);
    }

    @Test
    void getDsByLocationGeneralCase() {
        given(configurationProviderAdapter.getValueAsLongs(ConfigurationProperties.DS_LOCATION_SERVICE_DS_IDS))
                .willReturn(Set.of(1L, 2L));
        given(deliveryServiceRegionRepository.findDsIdByCourierRegionId(345, Set.of(1L, 2L)))
                .willReturn(List.of(1L));
        assertThat(dsLocationService.getDsByRegionId(345)).contains(1L);
    }

    @Test
    void getDsByGeoPointEmpty() {
        Optional<Long> dsByLocation = dsLocationService.getDsByGeoPoint(ofLatLon(
                new BigDecimal("37.587093"), new BigDecimal("55.733969")
        ));
        assertThat(dsByLocation).isEmpty();
    }

    @Test
    void getDsByLocationByGeo() {
        given(configurationProviderAdapter.getValueAsLongs(ConfigurationProperties.DS_LOCATION_SERVICE_DS_IDS))
                .willReturn(Set.of(1L, 2L));
        given(deliveryServiceRegionRepository.findDsIdByCourierRegionId(345, Set.of(1L, 2L)))
                .willReturn(List.of());
        given(sortingCenterService.findSortingCentersForDs(Set.of(1L, 2L)))
                .willReturn(Map.of(
                        1L, sortingCenterWithGeo(ofLatLon(new BigDecimal("55.730"), new BigDecimal("37.587"))),
                        2L, sortingCenterWithGeo(ofLatLon(new BigDecimal("55.740"), new BigDecimal("37.587")))
                ));
        GeoPoint destinationCloserTo1 = ofLatLon(new BigDecimal("55.733"), new BigDecimal("37.587"));
        GeoPoint destinationCloserTo2 = ofLatLon(new BigDecimal("55.737"), new BigDecimal("37.587"));
        assertThat(dsLocationService.getDsByGeoPoint(destinationCloserTo1)).contains(1L);
        assertThat(dsLocationService.getDsByGeoPoint(destinationCloserTo2)).contains(2L);
    }

    private SortingCenter sortingCenterWithGeo(GeoPoint geoPoint) {
        SortingCenter sortingCenter = new SortingCenter();
        sortingCenter.setLatitude(geoPoint.getLatitude());
        sortingCenter.setLongitude(geoPoint.getLongitude());
        return sortingCenter;
    }

}
