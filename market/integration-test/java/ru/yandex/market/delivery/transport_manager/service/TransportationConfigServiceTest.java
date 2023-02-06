package ru.yandex.market.delivery.transport_manager.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.enums.ConfigTransportationType;
import ru.yandex.market.delivery.transport_manager.domain.enums.DimensionsClass;
import ru.yandex.market.delivery.transport_manager.domain.yt.PartnerCutoffData;
import ru.yandex.market.delivery.transport_manager.domain.yt.RoutingConfigDto;
import ru.yandex.market.delivery.transport_manager.domain.yt.TransportationConfigDto;
import ru.yandex.market.delivery.transport_manager.domain.yt.TransportationConfigDto.TransportationConfigDtoBuilder;
import ru.yandex.market.delivery.transport_manager.domain.yt.YtHoliday;
import ru.yandex.market.delivery.transport_manager.domain.yt.YtSchedule;

class TransportationConfigServiceTest extends AbstractContextualTest {

    @Autowired
    TransportationConfigService transportationConfigService;

    @DatabaseSetup("/repository/schedule/setup/transportation_with_schedule.xml")
    @ExpectedDatabase(
        value = "/repository/schedule/setup/transportation_with_schedule_after_update.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @Test
    void testUpdate() {
        transportationConfigService.update(List.of(TransportationConfigDto.builder()
            .outboundPartnerId(10L)
            .outboundLogisticsPointId(10L)
            .movingPartnerId(20L)
            .inboundPartnerId(30L)
            .inboundLogisticsPointId(30L)
            .weight(10)
            .volume(100)
            .duration(24)
            .hash("some_hash")
            .transportationType(ConfigTransportationType.ORDERS_RETURN)
            .transportationSchedule(List.of(
                new YtSchedule(1L, 1, LocalTime.of(12, 0), LocalTime.of(20, 0))
            ))
            .transportationHolidays(
                List.of(
                    new YtHoliday(LocalDate.of(2021, 5, 9), 10L),
                    new YtHoliday(LocalDate.of(2021, 5, 10), 30L)
                )
            )
            .routingConfig(new RoutingConfigDto(
                true,
                DimensionsClass.BULKY_CARGO,
                1.1D,
                false,
                "DEFAULT"
            ))
            .movementSegmentId(101L)
            .build()));
    }

    @DatabaseSetup("/repository/schedule/setup/transportation_with_schedule.xml")
    @ExpectedDatabase(
        value = "/repository/schedule/setup/transportation_with_schedule.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @Test
    void testUpdateNothingChanged() {
        transportationConfigService.update(List.of(
            defaultConfig()
                .duration(24)
                .hash("hash1")
                .transportationSchedule(List.of(
                    new YtSchedule(1L, 1, LocalTime.of(12, 0), LocalTime.of(20, 0)),
                    new YtSchedule(2L, 2, LocalTime.of(12, 0), LocalTime.of(20, 0))
                ))
                .routingConfig(new RoutingConfigDto(
                    true,
                    DimensionsClass.BULKY_CARGO,
                    1.1D,
                    false,
                    "DEFAULT"
                ))
                .build()
        ));
    }

    @DatabaseSetup("/repository/schedule/setup/with_same_hash.xml")
    @ExpectedDatabase(
        value = "/repository/schedule/expected/with_same_hash_updated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testWithSameHash() {
        transportationConfigService.update(List.of(
            defaultConfig()
                .transportationSchedule(List.of(
                    new YtSchedule(1L, 1, LocalTime.of(12, 0), LocalTime.of(20, 0))
                ))
                .transportationHolidays(List.of())
                .build(),
            TransportationConfigDto.builder()
                .outboundPartnerId(3L)
                .outboundLogisticsPointId(20L)
                .movingPartnerId(9L)
                .inboundPartnerId(6L)
                .inboundLogisticsPointId(40L)
                .weight(10)
                .volume(100)
                .duration(24)
                .hash("hash1")
                .transportationType(ConfigTransportationType.ORDERS_OPERATION)
                .transportationSchedule(List.of(
                    new YtSchedule(1L, 1, LocalTime.of(12, 0), LocalTime.of(20, 0))
                ))
                .routingConfig(new RoutingConfigDto(
                    true,
                    DimensionsClass.BULKY_CARGO,
                    1.1D,
                    false,
                    "DEFAULT"
                ))
                .transportationHolidays(List.of())
                .build()
        ));
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/schedule/setup/merged_configs.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void testOneConfigMerge() {
        transportationConfigService.update(List.of(
            defaultConfig()
                .transportationSchedule(List.of(
                    new YtSchedule(1L, 1, LocalTime.of(12, 0), LocalTime.of(20, 0))
                ))
                .routingConfig(new RoutingConfigDto(
                    true,
                    DimensionsClass.BULKY_CARGO,
                    1.1D,
                    false,
                    "DEFAULT"
                ))
                .transportationHolidays(List.of())
                .build(),
            defaultConfig()
                .transportationSchedule(List.of(
                    new YtSchedule(1L, 1, LocalTime.of(12, 0), LocalTime.of(20, 0)),
                    new YtSchedule(2L, 1, LocalTime.of(13, 0), LocalTime.of(20, 0))
                ))
                .routingConfig(new RoutingConfigDto(
                    false,
                    DimensionsClass.MEDIUM_SIZE_CARGO,
                    2.1D,
                    false,
                    "DEFAULT"
                ))
                .transportationHolidays(List.of(
                    new YtHoliday(LocalDate.of(2021, 9, 1), 1L)
                ))
                .build()
        ));
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/schedule/expected/cutoff_data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void testCutoffData() {
        transportationConfigService.update(List.of(
            defaultConfig()
                .transportationSchedule(List.of(
                    new YtSchedule(1L, 1, LocalTime.of(12, 0), LocalTime.of(20, 0)),
                    new YtSchedule(2L, 2, LocalTime.of(12, 0), LocalTime.of(20, 0))
                ))
                .transportationHolidays(List.of())
                .partnerCutoffData(
                    PartnerCutoffData.builder()
                        .cutoffTime(LocalTime.of(16, 0))
                        .warehouseOffsetSeconds(60 * 60)
                        .handlingTimeDays(1)
                        .build()
                )
                .build()
        ));
    }

    @DisplayName("Проверяем заполнение полей по-умолчанию, если из YT придёт null")
    @Test
    @ExpectedDatabase(
        value = "/repository/schedule/expected/insert_routing_config_defaults.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void testRoutingConfigDefaultValues() {
        transportationConfigService.update(List.of(TransportationConfigDto.builder()
            .outboundPartnerId(10L)
            .outboundLogisticsPointId(10L)
            .movingPartnerId(20L)
            .inboundPartnerId(30L)
            .inboundLogisticsPointId(30L)
            .weight(10)
            .volume(100)
            .duration(24)
            .hash("some_hash")
            .transportationType(ConfigTransportationType.ORDERS_RETURN)
            .transportationSchedule(List.of(
                new YtSchedule(1L, 1, LocalTime.of(12, 0), LocalTime.of(20, 0))
            ))
            .transportationHolidays(
                List.of(
                    new YtHoliday(LocalDate.of(2021, 5, 9), 10L),
                    new YtHoliday(LocalDate.of(2021, 5, 10), 30L)
                )
            )
            .routingConfig(new RoutingConfigDto(
                true,
                null,
                null,
                false,
                null
            ))
            .movementSegmentId(101L)
            .build()));
    }

    @Nonnull
    private TransportationConfigDtoBuilder defaultConfig() {
        return TransportationConfigDto.builder()
            .outboundPartnerId(1L)
            .outboundLogisticsPointId(10L)
            .movingPartnerId(2L)
            .inboundPartnerId(3L)
            .inboundLogisticsPointId(30L)
            .weight(10)
            .volume(100)
            .duration(36)
            .hash("hash2")
            .transportationType(ConfigTransportationType.ORDERS_OPERATION);
    }

}
