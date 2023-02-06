package ru.yandex.market.pvz.core.domain.pickup_point.location;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.geobase.HttpGeobase;
import ru.yandex.geobase.beans.GeobaseRegionData;
import ru.yandex.market.pvz.core.domain.configuration.global.ConfigurationGlobalCommandService;
import ru.yandex.market.pvz.core.domain.logbroker.crm.produce.CrmLogbrokerEventPublisher;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointCommandService;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointParams;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointQueryService;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.CRM_PVZ_SUPPORT_CARD;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointLocationTestParams.DEFAULT_LAT;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointLocationTestParams.DEFAULT_LNG;

@Slf4j
@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class PickupPointLocationServiceTest {

    private static final int MOSCOW_ID = 213;
    private static final String MOSCOW_NAME = "Europe/Moscow";
    private static final int MOSCOW_OFFSET_IN_SECONDS = 10800;
    private static final int MOSCOW_OFFSET_IN_HOURS = 3;

    private static final int ZELENOGRAD_ID = 216;
    private static final double ZELENOGRAD_LAT = 55.990809;
    private static final double ZELENOGRAD_LNG = 37.225679;

    private static final int YEKATERINBURG_ID = 56;
    private static final String YEKATERINBURG_NAME = "Asia/Yekaterinburg";
    private static final int YEKATERINBURG_OFFSET_IN_SECONDS = 18000;
    private static final int YEKATERINBURG_OFFSET_IN_HOURS = 5;

    private final TestPickupPointFactory pickupPointFactory;
    private final PickupPointLocationService pickupPointLocationService;
    private final PickupPointQueryService pickupPointQueryService;
    private final PickupPointCommandService pickupPointCommandService;
    private final ConfigurationGlobalCommandService configurationGlobalCommandService;

    @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
    private HttpGeobase httpGeobase;

    @MockBean
    private CrmLogbrokerEventPublisher crmLogbrokerEventPublisher;

    @BeforeEach
    void setUp() {
        when(httpGeobase.getRegionId(DEFAULT_LAT.doubleValue(), DEFAULT_LNG.doubleValue())).thenReturn(MOSCOW_ID);
        when(httpGeobase.getRegionId(ZELENOGRAD_LAT, ZELENOGRAD_LNG)).thenReturn(ZELENOGRAD_ID);

        GeobaseRegionData regionDataMoscow = mock(GeobaseRegionData.class);
        when(regionDataMoscow.getParentId()).thenReturn(0);
        when(regionDataMoscow.getType()).thenReturn(6);
        when(regionDataMoscow.getId()).thenReturn(MOSCOW_ID);
        when(httpGeobase.getRegion(eq(MOSCOW_ID))).thenReturn(regionDataMoscow);

        GeobaseRegionData regionDataZelenograd = mock(GeobaseRegionData.class);
        when(regionDataZelenograd.getParentId()).thenReturn(213);
        when(regionDataZelenograd.getType()).thenReturn(6);
        when(regionDataZelenograd.getId()).thenReturn(ZELENOGRAD_ID);
        when(httpGeobase.getRegion(eq(ZELENOGRAD_ID))).thenReturn(regionDataZelenograd);

        when(httpGeobase.getTimezone(eq(MOSCOW_ID)))
                .thenReturn(MOSCOW_NAME);
        when(httpGeobase.getTzInfo(eq(MOSCOW_NAME)).getOffset())
                .thenReturn(MOSCOW_OFFSET_IN_SECONDS);

        when(httpGeobase.getTimezone(eq(YEKATERINBURG_ID)))
                .thenReturn(YEKATERINBURG_NAME);
        when(httpGeobase.getTzInfo(eq(YEKATERINBURG_NAME)).getOffset())
                .thenReturn(YEKATERINBURG_OFFSET_IN_SECONDS);

        configurationGlobalCommandService.setValue(CRM_PVZ_SUPPORT_CARD, true);
    }

    @Test
    void updateLocation() {
        var pickupPoint = pickupPointFactory.createPickupPointFromCrm();

        verify(crmLogbrokerEventPublisher, times(1)).publish(isA(PickupPoint.class));

        var created = pickupPointQueryService.getHeavy(pickupPoint.getId());
        created.setTimeOffset(YEKATERINBURG_OFFSET_IN_SECONDS);
        created.getLocation().setLocationId(null);
        pickupPointCommandService.update(pickupPoint.getId(), created);

        verify(crmLogbrokerEventPublisher, times(2)).publish(isA(PickupPoint.class));

        pickupPointLocationService.updateLocation(pickupPoint.getId());

        verify(crmLogbrokerEventPublisher, times(3)).publish(isA(PickupPoint.class));

        PickupPointParams updated = pickupPointQueryService.getHeavy(pickupPoint.getId());

        assertThat(updated.getLocation().getLocationId()).isEqualTo(MOSCOW_ID);
        assertThat(updated.getTimeOffset()).isEqualTo(MOSCOW_OFFSET_IN_HOURS);
    }

    @Test
    void updateTimezone() {
        var pickupPoint = pickupPointFactory.createPickupPointFromCrm();
        var created = pickupPointQueryService.getHeavy(pickupPoint.getId());
        created.getLocation().setLocationId((long) YEKATERINBURG_ID);

        pickupPointCommandService.update(pickupPoint.getId(), created);

        pickupPointLocationService.updateLocation(pickupPoint.getId());

        PickupPointParams updated = pickupPointQueryService.getHeavy(pickupPoint.getId());

        assertThat(updated.getTimeOffset()).isEqualTo(YEKATERINBURG_OFFSET_IN_HOURS);
    }

    @Test
    void findByRegions() {
        var pickupPointInZelenograd = pickupPointFactory.createPickupPointFromCrm(
                TestPickupPointFactory.CreatePickupPointBuilder.builder()
                        .params(TestPickupPointFactory.PickupPointTestParams.builder()
                                .location(TestPickupPointFactory.PickupPointLocationTestParams.builder()
                                        .lat(BigDecimal.valueOf(ZELENOGRAD_LAT))
                                        .lng(BigDecimal.valueOf(ZELENOGRAD_LNG))
                                        .build())
                                .build())
                        .build());

        var pickupPointInMoscow = pickupPointFactory.createPickupPointFromCrm();

        List<PickupPointLocationService.PickupPointWithCoordinates> actual = pickupPointLocationService.findByRegions(
                Set.of(MOSCOW_ID))
                .stream()
                .sorted(Comparator.comparingLong(PickupPointLocationService.PickupPointWithCoordinates::getId))
                .collect(Collectors.toList());

        assertThat(actual).hasSize(2);

        var expectedZelenograd = PickupPointLocationService.PickupPointWithCoordinates.builder()
                .id(pickupPointInZelenograd.getId())
                .pvzMarketId(pickupPointInZelenograd.getPvzMarketId())
                .lmsId(pickupPointInZelenograd.getLmsId())
                .name(pickupPointInZelenograd.getName())
                .active(pickupPointInZelenograd.getActive())
                .lat(BigDecimal.valueOf(ZELENOGRAD_LAT))
                .lng(BigDecimal.valueOf(ZELENOGRAD_LNG))
                .build();

        var expectedMoscow = PickupPointLocationService.PickupPointWithCoordinates.builder()
                .id(pickupPointInMoscow.getId())
                .pvzMarketId(pickupPointInMoscow.getPvzMarketId())
                .lmsId(pickupPointInMoscow.getLmsId())
                .name(pickupPointInMoscow.getName())
                .active(pickupPointInMoscow.getActive())
                .lat(DEFAULT_LAT)
                .lng(DEFAULT_LNG)
                .build();

        assertThat(actual).containsExactly(expectedZelenograd, expectedMoscow);
    }
}
