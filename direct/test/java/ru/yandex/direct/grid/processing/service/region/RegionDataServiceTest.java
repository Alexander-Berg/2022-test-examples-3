package ru.yandex.direct.grid.processing.service.region;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.direct.config.DirectConfig;
import ru.yandex.direct.core.entity.geo.model.GeoRegion;
import ru.yandex.direct.core.entity.geo.repository.GeoRegionRepository;
import ru.yandex.direct.geosearch.GeosearchClient;
import ru.yandex.direct.geosearch.model.GeoObject;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ParametersAreNonnullByDefault
@RunWith(MockitoJUnitRunner.class)
public class RegionDataServiceTest {
    @Mock
    private AdRegionDataService adRegionDataService;

    @Mock
    private GeosearchClient geosearchClient;

    @Mock
    private GeoRegionRepository geoRegionRepository;

    @Mock
    private DirectConfig config;

    @InjectMocks
    private RegionDataService regionDataService;

    @Test
    public void shouldReturnRegionWithNearestAdRegion() {
        given(config.getDuration(any())).willReturn(Duration.ofSeconds(1));
        var moscowGeoObject = new GeoObject.Builder().withGeoId(213L).withX(0.).withY(0.).build();
        given(geosearchClient.searchAddress(anyString())).willReturn(singletonList(moscowGeoObject));
        var moscowGeoRegion = new GeoRegion().withId(213L);
        given(geoRegionRepository.getGeoRegionsByIds(any())).willReturn(singletonList(moscowGeoRegion));
        given(adRegionDataService.findCommercialRegion(eq(0.), eq(0.)))
                .willReturn(CompletableFuture.failedFuture(new TimeoutException()));

        regionDataService.searchRegions("hello");

        verify(adRegionDataService).findCommercialRegion(eq(0.), eq(0.));
    }

    @Test
    public void shouldReturnNullInAdGeoRegionFieldWhenNearestRegionWasNotFound() {
        given(config.getDuration(any())).willReturn(Duration.ofSeconds(1));
        var moscowGeoObject = new GeoObject.Builder().withGeoId(213L).withX(0.).withY(0.).build();
        given(geosearchClient.searchAddress(anyString())).willReturn(singletonList(moscowGeoObject));
        var moscowGeoRegion = new GeoRegion().withId(213L);
        given(geoRegionRepository.getGeoRegionsByIds(any())).willReturn(singletonList(moscowGeoRegion));
        given(adRegionDataService.findCommercialRegion(eq(0.), eq(0.)))
                .willReturn(CompletableFuture.completedFuture(Optional.empty()));

        var actualRegion = regionDataService.searchRegions("hello");

        assertThat(actualRegion).extracting("adRegion").containsNull();
    }

    @Test
    public void shouldReturnNullInAdGeoRegionFieldWhenNearestRegionCallFailed() {
        given(config.getDuration(any())).willReturn(Duration.ofSeconds(1));
        var moscowGeoObject = new GeoObject.Builder().withGeoId(213L).withX(0.).withY(0.).build();
        given(geosearchClient.searchAddress(anyString())).willReturn(singletonList(moscowGeoObject));
        var moscowGeoRegion = new GeoRegion().withId(213L);
        given(geoRegionRepository.getGeoRegionsByIds(any())).willReturn(singletonList(moscowGeoRegion));
        given(adRegionDataService.findCommercialRegion(eq(0.), eq(0.)))
                .willReturn(CompletableFuture.failedFuture(new TimeoutException()));

        var actualRegion = regionDataService.searchRegions("hello");

        assertThat(actualRegion).extracting("adRegion").containsNull();
    }
}
