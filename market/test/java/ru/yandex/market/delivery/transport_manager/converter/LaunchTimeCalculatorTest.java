package ru.yandex.market.delivery.transport_manager.converter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.delivery.transport_manager.config.properties.TmProperties;
import ru.yandex.market.delivery.transport_manager.domain.entity.TmPropertyKey;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationType;
import ru.yandex.market.delivery.transport_manager.service.TmPropertyService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class LaunchTimeCalculatorTest {

    private static final long MOVING_PARTNER_ID = 1000;
    private static final LocalDateTime DATE = LocalDate.ofYearDay(2021, 256).atStartOfDay();

    private final TmProperties tmProperties = new TmProperties();
    private final TmPropertyService propertyService = Mockito.mock(TmPropertyService.class);
    private final TestableClock clock = new TestableClock();
    private final LaunchTimeCalculator calculator = new LaunchTimeCalculator(
        tmProperties,
        propertyService,
        clock
    );

    @Nonnull
    public static Stream<Arguments> seedTimes() {
        return Stream.of(
            LocalTime.of(20, 39, 0),
            LocalTime.of(20, 37, 0),
            LocalTime.of(20, 15, 0),
            LocalTime.of(20, 30, 0),
            LocalTime.of(20, 59, 0)
        ).map(Arguments::of);
    }

    @BeforeEach
    void setup() {
        tmProperties.setDaysToLaunchBeforePlannedOutboundTime(Map.of(TmProperties.DEFAULT, 0));
        tmProperties.setPartnersWithRandomPlannedTime(Set.of());
    }

    @ParameterizedTest
    @MethodSource("seedTimes")
    void uniformelyRandomMinutes(LocalTime time) {
        tmProperties.setPartnersWithRandomPlannedTime(Set.of(MOVING_PARTNER_ID));

        assertThat(calculator.launchTime(DATE, TransportationType.ORDERS_OPERATION, MOVING_PARTNER_ID))
            .isEqualTo(LocalDateTime.of(DATE.toLocalDate(), time));
    }

    @Test
    void fixedTime() {
        assertThat(calculator.launchTime(DATE, TransportationType.ORDERS_OPERATION, MOVING_PARTNER_ID))
            .isEqualTo(LocalDateTime.of(DATE.toLocalDate(), LocalTime.of(21, 0, 0)));
    }

    @Test
    void daysToLaunchBefore() {
        tmProperties.setDaysToLaunchBeforePlannedOutboundTime(Map.of("ORDERS_OPERATION", 1));

        assertThat(calculator.launchTime(DATE, TransportationType.ORDERS_OPERATION, MOVING_PARTNER_ID))
            .isEqualTo(LocalDateTime.of(DATE.toLocalDate().minusDays(1), LocalTime.of(21, 0, 0)));
    }

    @Test
    void linehaulLaunchTime() {
        when(propertyService.getInt(TmPropertyKey.LINEHAUL_LAUNCH_OFFSET_IN_HOURS)).thenReturn(24);

        assertThat(calculator.launchTime(
            LocalDateTime.parse("2021-12-02T04:39:25.011000"),
            TransportationType.LINEHAUL,
            999L
        )).isEqualTo("2021-12-01T04:39:25.011000");
    }

    @Test
    void orderReturnLaunchTime() {

        assertThat(calculator.launchTime(
            LocalDateTime.parse("2021-12-02T04:39:25.011000"),
            TransportationType.ORDERS_RETURN,
            999L
        )).isEqualTo("2021-12-01T04:39:25.011000");
    }

    @Test
    void customCutoffForMovementsOfPartners() {
        tmProperties.setDaysToLaunchBeforePlannedOutboundTime(Map.of("ORDERS_OPERATION", 1));
        when(propertyService.getMap(TmPropertyKey.CUSTOM_MOVEMENT_START_TIME)).thenReturn(Map.of("123", "16:00:00"));

        assertThat(calculator.launchTime(
            DATE,
            TransportationType.ORDERS_OPERATION,
            123L
        )).isEqualTo(DATE.toLocalDate().minusDays(1).atTime(LocalTime.of(16, 0, 0)));
    }

    @Test
    void customCutoffForMovementsOfPartnersExceptionHandled() {
        tmProperties.setDaysToLaunchBeforePlannedOutboundTime(Map.of("ORDERS_OPERATION", 1));
        when(propertyService.getMap(TmPropertyKey.CUSTOM_MOVEMENT_START_TIME)).thenReturn(Map.of("123", "aaa16:00:00"));

        assertThat(calculator.launchTime(
            DATE,
            TransportationType.ORDERS_OPERATION,
            123L
        )).isEqualTo(DATE.toLocalDate().minusDays(1).atTime(LocalTime.of(21, 0, 0)));
    }

    @Test
    void customPlannedOutboundDateForPartner() {
        when(propertyService.getMap(TmPropertyKey.CUSTOM_MOVEMENT_GENERATION_OFFSET))
            .thenReturn(Map.of("2", 48L));

        assertThat(calculator.launchTime(
            DATE,
            TransportationType.INTERWAREHOUSE,
            2L
        )).isEqualTo(DATE.toLocalDate().minusDays(2).atTime(LocalTime.of(0, 0, 0)));

    }

    @Test
    void customPlannedOutboundDateForPartnerWithoutInterwarehouse() {
        tmProperties.setDaysToLaunchBeforePlannedOutboundTime(Map.of("ORDERS_OPERATION", 1));
        when(propertyService.getMap(TmPropertyKey.CUSTOM_MOVEMENT_GENERATION_OFFSET))
            .thenReturn(Map.of("2", 48L));

        assertThat(calculator.launchTime(
            DATE,
            TransportationType.ORDERS_OPERATION,
            2L
        )).isEqualTo(DATE.toLocalDate().minusDays(1).atTime(LocalTime.of(21, 0, 0)));

    }

    @Test
    void interwarehouseVirtualLaunchTime() {
        clock.setFixed(Instant.parse("2021-12-01T03:39:25.011000Z"), ZoneOffset.UTC);
        assertThat(calculator.launchTime(
            LocalDateTime.parse("2021-12-02T04:39:25.011000"),
            TransportationType.INTERWAREHOUSE_VIRTUAL,
            999L
        )).isEqualTo("2021-12-01T06:39:25.011000");
    }
}
