package ru.yandex.market.tpl.core.domain.partner;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.core.domain.partner.DayOffTestDataFactory.DS_1_DAY_OFFS;
import static ru.yandex.market.tpl.core.domain.partner.DayOffTestDataFactory.DS_2_DAY_OFFS;
import static ru.yandex.market.tpl.core.domain.partner.DayOffTestDataFactory.DS_ID_1;
import static ru.yandex.market.tpl.core.domain.partner.DayOffTestDataFactory.DS_ID_2;

@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class DeliveryServiceDayOffServiceTest {
    private final DayOffTestDataFactory dayOffTestDataFactory;
    private final DeliveryServiceDayOffService deliveryServiceDayOffService;

    @MockBean
    private ConfigurationProviderAdapter configurationProviderAdapter;

    @BeforeEach
    public void init() {
        dayOffTestDataFactory.createTestData();
    }

    @Test
    public void testIsDayOffEnabledFlag() {
        Mockito.when(configurationProviderAdapter.isBooleanEnabled(ConfigurationProperties.IS_DELIVERY_SERVICE_DAY_OFFS_ENABLED))
                .thenReturn(true);

        DS_1_DAY_OFFS.forEach(
                dayOff -> assertThat(deliveryServiceDayOffService.isDayOff(dayOff, DS_ID_1)).isTrue()
        );
        DS_2_DAY_OFFS.forEach(
                dayOff -> assertThat(deliveryServiceDayOffService.isDayOff(dayOff, DS_ID_2)).isTrue()
        );

        assertThat(deliveryServiceDayOffService.isDayOff(
                DS_1_DAY_OFFS.iterator().next(),
                -1L
        )).isFalse();

        assertThat(deliveryServiceDayOffService.isDayOff(
                DS_1_DAY_OFFS.iterator().next().plusDays(100L),
                DS_ID_1
        )).isFalse();
    }

    @Test
    public void testIsDayOffDisabledFlag() {
        Mockito.when(configurationProviderAdapter.isBooleanEnabled(ConfigurationProperties.IS_DELIVERY_SERVICE_DAY_OFFS_ENABLED))
                .thenReturn(false);

        DS_1_DAY_OFFS.forEach(
                dayOff -> assertThat(deliveryServiceDayOffService.isDayOff(dayOff, DS_ID_1)).isFalse()
        );
        DS_2_DAY_OFFS.forEach(
                dayOff -> assertThat(deliveryServiceDayOffService.isDayOff(dayOff, DS_ID_2)).isFalse()
        );
    }

    @Test
    public void testGetRescheduleDatesWithoutDayOffsEnabledFlag() {
        Mockito.when(configurationProviderAdapter.isBooleanEnabled(ConfigurationProperties.IS_DELIVERY_SERVICE_DAY_OFFS_ENABLED))
                .thenReturn(true);

        var datesWithoutDayOffs = deliveryServiceDayOffService.getRescheduleDatesWithoutDayOffs(
                new ArrayList<>(DS_1_DAY_OFFS),
                DS_ID_1
        );
        assertThat(datesWithoutDayOffs).hasSize(DS_1_DAY_OFFS.size());
        assertThat(datesWithoutDayOffs).containsExactlyElementsOf(
                DS_1_DAY_OFFS.stream()
                        .map(day -> day.plusDays(DS_1_DAY_OFFS.size()))
                        .sorted()
                        .collect(Collectors.toList())
        );

        var rescheduleDaysNotOnDayOff = DS_1_DAY_OFFS.stream()
                .map(day -> day.minusDays(10L))
                .sorted()
                .collect(Collectors.toList());
        datesWithoutDayOffs = deliveryServiceDayOffService.getRescheduleDatesWithoutDayOffs(
                rescheduleDaysNotOnDayOff,
                DS_ID_1
        );
        assertThat(datesWithoutDayOffs).hasSize(DS_1_DAY_OFFS.size());
        assertThat(datesWithoutDayOffs).containsExactlyElementsOf(rescheduleDaysNotOnDayOff);

        var rescheduleDaysPartiallyOnDayOffLastDays = DS_2_DAY_OFFS.stream()
                .map(day -> day.minusDays(2L))
                .sorted()
                .collect(Collectors.toList());
        datesWithoutDayOffs = deliveryServiceDayOffService.getRescheduleDatesWithoutDayOffs(
                rescheduleDaysPartiallyOnDayOffLastDays,
                DS_ID_2
        );
        assertThat(datesWithoutDayOffs).hasSize(DS_2_DAY_OFFS.size());
        assertThat(datesWithoutDayOffs).containsExactlyElementsOf(
                List.of(
                        LocalDate.of(2020, 12, 18),
                        LocalDate.of(2020, 12, 19),
                        LocalDate.of(2020, 12, 24),
                        LocalDate.of(2020, 12, 25)
                )
        );

        var rescheduleDaysPartiallyOnDayOffFirstDays = DS_2_DAY_OFFS.stream()
                .map(day -> day.plusDays(2L))
                .sorted()
                .collect(Collectors.toList());
        datesWithoutDayOffs = deliveryServiceDayOffService.getRescheduleDatesWithoutDayOffs(
                rescheduleDaysPartiallyOnDayOffFirstDays,
                DS_ID_2
        );
        assertThat(datesWithoutDayOffs).hasSize(DS_2_DAY_OFFS.size());
        assertThat(datesWithoutDayOffs).containsExactlyElementsOf(
                List.of(
                        LocalDate.of(2020, 12, 24),
                        LocalDate.of(2020, 12, 25),
                        LocalDate.of(2020, 12, 26),
                        LocalDate.of(2020, 12, 27)
                )
        );
    }

    @Test
    public void testGetRescheduleDatesWithoutDayOffsDisabledFlag() {
        Mockito.when(configurationProviderAdapter.isBooleanEnabled(ConfigurationProperties.IS_DELIVERY_SERVICE_DAY_OFFS_ENABLED))
                .thenReturn(false);

        var datesWithoutDayOffs = deliveryServiceDayOffService.getRescheduleDatesWithoutDayOffs(
                new ArrayList<>(DS_1_DAY_OFFS),
                DS_ID_1
        );
        assertThat(datesWithoutDayOffs).hasSize(DS_1_DAY_OFFS.size());
        assertThat(datesWithoutDayOffs).containsExactlyElementsOf(
                DS_1_DAY_OFFS
        );
    }
}
