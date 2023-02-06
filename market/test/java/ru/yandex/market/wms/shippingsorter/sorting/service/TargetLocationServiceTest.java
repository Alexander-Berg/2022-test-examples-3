package ru.yandex.market.wms.shippingsorter.sorting.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import ru.yandex.market.wms.shared.libs.configproperties.dao.GlobalConfigurationDao;
import ru.yandex.market.wms.shippingsorter.configuration.ShippingSorterSecurityTestConfiguration;
import ru.yandex.market.wms.shippingsorter.core.sorting.entity.BoxInfo;
import ru.yandex.market.wms.shippingsorter.sorting.IntegrationTest;
import ru.yandex.market.wms.shippingsorter.sorting.dto.ConfigDto;
import ru.yandex.market.wms.shippingsorter.sorting.entity.id.SorterExitId;
import ru.yandex.market.wms.shippingsorter.sorting.service.target.TargetLocationService;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Import(ShippingSorterSecurityTestConfiguration.class)
@RequiredArgsConstructor
class TargetLocationServiceTest extends IntegrationTest {

    @Autowired
    private TargetLocationService targetLocationService;

    @MockBean
    @Autowired
    @Qualifier("configPropertyPostgreSqlDao")
    private GlobalConfigurationDao configPropertyPostgreSqlDao;

    @Test
    @DatabaseSetup("/sorting/service/target-location/common.xml")
    void getTargetLocationWhenHasMultiExitAndCurrentTimeStartDay() {
        mockConfig(ConfigDto.builder().multiZoneSearchEnabled("0").build());

        final LocalTime currentTime = LocalTime.of(0, 0, 0);
        final int boxWeight = 2000;
        final LocalDateTime scheduledShipDate = getDateTime("2020-01-01 00:00:00");
        final String carrierId = "123456";
        final BoxInfo boxInfo = getBoxInfo(boxWeight, carrierId, scheduledShipDate);

        SorterExitId sorterExitId = targetLocationService.getTargetLocation(boxInfo, currentTime, "SSORT_ZONE");
        assertEquals("SR1_CH-2", sorterExitId.getId());
    }

    @Test
    @DatabaseSetup("/sorting/service/target-location/common.xml")
    void getTargetLocationWhenHasMultiExitAndCurrentTimeEndDay() {
        mockConfig(ConfigDto.builder().multiZoneSearchEnabled("0").build());

        final LocalTime currentTime = LocalTime.of(23, 59, 59);
        final int boxWeight = 2000;
        final LocalDateTime scheduledShipDate = getDateTime("2020-01-01 00:00:00");
        final String carrierId = "123456";
        final BoxInfo boxInfo = getBoxInfo(boxWeight, carrierId, scheduledShipDate);

        SorterExitId sorterExitId = targetLocationService.getTargetLocation(boxInfo, currentTime, "SSORT_ZONE");
        assertEquals("SR1_CH-3", sorterExitId.getId());
    }

    @Test
    @DatabaseSetup("/sorting/service/target-location/common.xml")
    void getTargetLocationWhenHasMultiExitAndCurrentTimeBeforeMidday() {
        mockConfig(ConfigDto.builder().multiZoneSearchEnabled("0").build());

        final LocalTime currentTime = LocalTime.of(10, 59, 59);
        final int boxWeight = 2000;
        final LocalDateTime scheduledShipDate = getDateTime("2020-01-01 00:00:00");
        final String carrierId = "123456";
        final BoxInfo boxInfo = getBoxInfo(boxWeight, carrierId, scheduledShipDate);

        SorterExitId sorterExitId = targetLocationService
                .getTargetLocation(boxInfo, currentTime, "SSORT_ZONE");
        assertEquals("SR1_CH-2", sorterExitId.getId());
    }

    @Test
    @DatabaseSetup("/sorting/service/target-location/common.xml")
    void getTargetLocationWhenHasMultiExitAndCurrentTimeAfterMidday() {
        mockConfig(ConfigDto.builder().multiZoneSearchEnabled("0").build());

        final LocalTime currentTime = LocalTime.of(16, 30);
        final int boxWeight = 2000;
        final LocalDateTime scheduledShipDate = getDateTime("2020-01-01 00:00:00");
        final String carrierId = "123456";
        final BoxInfo boxInfo = getBoxInfo(boxWeight, carrierId, scheduledShipDate);

        SorterExitId sorterExitId = targetLocationService
                .getTargetLocation(boxInfo, currentTime, "SSORT_ZONE");
        assertEquals("SR1_CH-3", sorterExitId.getId());
    }

    @Test
    @DatabaseSetup("/sorting/service/target-location/common.xml")
    void getTargetLocationFromAnotherZoneWhenMultiZoneDisabled() {
        mockConfig(ConfigDto.builder().multiZoneSearchEnabled("0").build());

        final LocalTime currentTime = LocalTime.of(15, 30);
        final int boxWeight = 2000;
        final LocalDateTime scheduledShipDate = getDateTime("2020-01-01 00:00:00");
        final String carrierId = "123456";
        final BoxInfo boxInfo = getBoxInfo(boxWeight, carrierId, scheduledShipDate);

        SorterExitId sorterExitId = targetLocationService
                .getTargetLocation(boxInfo, currentTime, "SSORT_ZONE");
        assertEquals("SR1_NOK", sorterExitId.getId());
    }

    @Test
    @DatabaseSetup("/sorting/service/target-location/common.xml")
    void getTargetLocationNoExitFoundWhenMultiZoneEnabled() {
        mockConfig(ConfigDto.builder().multiZoneSearchEnabled("1").build());

        final LocalTime currentTime = LocalTime.of(15, 30);
        final int boxWeight = 500;
        final LocalDateTime scheduledShipDate = getDateTime("2020-01-01 00:00:00");
        final String carrierId = "123456";
        final BoxInfo boxInfo = getBoxInfo(boxWeight, carrierId, scheduledShipDate);

        SorterExitId sorterExitId = targetLocationService
                .getTargetLocation(boxInfo, currentTime, "SSORT_ZN_2");
        assertEquals("SR2_NOK", sorterExitId.getId());
    }

    @Test
    @DatabaseSetup("/sorting/service/target-location/common.xml")
    void getTargetLocationFromAnotherZoneWhenMultiZoneEnabled() {
        mockConfig(ConfigDto.builder().multiZoneSearchEnabled("1").build());

        final LocalTime currentTime = LocalTime.of(15, 30);
        final int boxWeight = 2000;
        final LocalDateTime scheduledShipDate = getDateTime("2020-01-01 00:00:00");
        final String carrierId = "123456";
        final BoxInfo boxInfo = getBoxInfo(boxWeight, carrierId, scheduledShipDate);

        SorterExitId sorterExitId = targetLocationService
                .getTargetLocation(boxInfo, currentTime, "SSORT_ZONE");
        assertEquals("SR2_CH-2", sorterExitId.getId());
    }

    private LocalDateTime getDateTime(String value) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return LocalDateTime.parse(value, formatter);
    }

    private BoxInfo getBoxInfo(int boxWeight, String carrierId, LocalDateTime scheduledShipDate) {
        return BoxInfo.builder()
                .boxWeight(boxWeight)
                .carrierCode(carrierId)
                .operationDayId(
                        Duration.between(
                                Instant.EPOCH,
                                scheduledShipDate.toInstant(ZoneOffset.UTC)
                        ).toDays())
                .build();
    }

    private void mockConfig(ConfigDto dto) {
        Mockito.when(configPropertyPostgreSqlDao.getStringConfigValue("MULTI_ZONE_SEARCH_ENABLED"))
                .thenReturn(dto.getMultiZoneSearchEnabled());
    }
}
