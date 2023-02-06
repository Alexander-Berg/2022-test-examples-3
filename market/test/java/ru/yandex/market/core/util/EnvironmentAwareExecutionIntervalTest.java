package ru.yandex.market.core.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;
import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import ru.yandex.market.mbi.environment.EnvironmentService;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link EnvironmentAwareExecutionInterval}.
 */
@ExtendWith(MockitoExtension.class)
class EnvironmentAwareExecutionIntervalTest {
    private static final String TEST_FAKE_ENV_VALUE = "fake_env_key";
    private static final LocalDate TODAY = LocalDate.now();
    private static final LocalDate YTD = TODAY.minusDays(1);
    private static final LocalDate DAY_BEFORE_YTD = TODAY.minusDays(2);
    @Mock
    private EnvironmentService environmentService;
    private EnvironmentAwareExecutionInterval service;

    @BeforeEach
    void beforeEach() {
        when(environmentService.getValue(eq(TEST_FAKE_ENV_VALUE), any()))
                .thenReturn(DAY_BEFORE_YTD.format(DateTimeFormatter.ISO_DATE));

        service = new EnvironmentAwareExecutionInterval(environmentService, TEST_FAKE_ENV_VALUE);
    }

    @DisplayName("iterate for each day")
    @Test
    void test_forEachDay() {
        List<LocalDate> actualDates = new ArrayList<>();
        service.forEachDay(actualDates::add);
        assertThat(actualDates, equalTo(ImmutableList.of(DAY_BEFORE_YTD, YTD, TODAY)));
    }

    @DisplayName("iterate before today")
    @Test
    void test_forEachDayExcl() {
        List<LocalDate> actualDates = new ArrayList<>();
        service.forEachDayButToday(actualDates::add);
        assertThat(actualDates, equalTo(ImmutableList.of(DAY_BEFORE_YTD, YTD)));
    }

}