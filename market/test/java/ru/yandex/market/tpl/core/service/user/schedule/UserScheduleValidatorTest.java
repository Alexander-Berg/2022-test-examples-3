package ru.yandex.market.tpl.core.service.user.schedule;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.common.web.exception.TplInvalidActionException;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserRepository;
import ru.yandex.market.tpl.core.service.user.SortingCenterPropertyService;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;

@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class UserScheduleValidatorTest {

    private final UserScheduleValidator userScheduleValidator;
    private final TestUserHelper testUserHelper;
    private final UserRepository userRepository;
    @MockBean
    private SortingCenterPropertyService sortingCenterPropertyService;
    @MockBean
    private ConfigurationProviderAdapter configurationProviderAdapter;
    @MockBean
    private Clock clock;

    private User user;
    private SortingCenter anotherSc;

    @BeforeEach
    void init() {
        Mockito.when(sortingCenterPropertyService.findPropertyValueForSortingCenterOrDefault(
                        any(), anyLong()))
                .thenReturn(LocalTime.of(18, 0));

        User userWithoutSchedule = testUserHelper.createUserWithoutSchedule(123L);
        user = userRepository.save(userWithoutSchedule);
        anotherSc = testUserHelper.sortingCenter(123L);
    }

    @Test
    void whenCurrentTimeBeforeDeadlineCanEditScheduleForTomorrow() {
        Mockito.when(configurationProviderAdapter.isBooleanEnabled(ConfigurationProperties.CHANGE_USER_SCHEDULE_DEADLINE_ENABLED))
                .thenReturn(true);

        LocalDate today = LocalDate.of(2021, Month.FEBRUARY, 1);
        LocalDate tomorrow = today.plusDays(1L);
        setClockDateTime(LocalDateTime.of(today, LocalTime.of(11, 10)));

        userScheduleValidator.checkCanEditScheduleByPartnerCompany(tomorrow, anotherSc.getId());
    }

    @Test
    void whenCurrentTimeAfterDeadlineCantEditScheduleForTomorrow() {
        Mockito.when(configurationProviderAdapter.isBooleanEnabled(ConfigurationProperties.CHANGE_USER_SCHEDULE_DEADLINE_ENABLED))
                .thenReturn(true);

        LocalDate today = LocalDate.of(2021, Month.FEBRUARY, 1);
        LocalDate tomorrow = today.plusDays(1L);
        setClockDateTime(LocalDateTime.of(today, LocalTime.of(18, 10)));

        assertThrows(
                TplInvalidActionException.class,
                () -> userScheduleValidator.checkCanEditScheduleByPartnerCompany(tomorrow, anotherSc.getId()),
                "Изменение расписания запрещено после 18:00"
        );
    }

    @Test
    void whenOverrideDeadlineCanEditScheduleForTomorrow() {
        Mockito.when(configurationProviderAdapter.isBooleanEnabled(ConfigurationProperties.CHANGE_USER_SCHEDULE_DEADLINE_ENABLED))
                .thenReturn(true);
        Mockito.when(sortingCenterPropertyService.findPropertyValueForSortingCenterOrDefault(
                        any(), anyLong()))
                .thenReturn(LocalTime.of(19, 0));

        LocalDate today = LocalDate.of(2021, Month.FEBRUARY, 1);
        LocalDate tomorrow = today.plusDays(1L);
        setClockDateTime(LocalDateTime.of(today, LocalTime.of(18, 10)));

        userScheduleValidator.checkCanEditScheduleByPartnerCompany(tomorrow, anotherSc.getId());
    }

    @Test
    void whenCurrentTimeAfterDeadlineCanEditScheduleForDayAfterTomorrow() {
        Mockito.when(configurationProviderAdapter.isBooleanEnabled(ConfigurationProperties.CHANGE_USER_SCHEDULE_DEADLINE_ENABLED))
                .thenReturn(true);

        LocalDate today = LocalDate.of(2021, Month.FEBRUARY, 1);
        LocalDate dayAfterTomorrow = today.plusDays(2L);
        setClockDateTime(LocalDateTime.of(today, LocalTime.of(18, 10)));

        userScheduleValidator.checkCanEditScheduleByPartnerCompany(dayAfterTomorrow, anotherSc.getId());
    }

    @Test
    void whenCurrentTimeAfterDeadlineCantEditScheduleForToday() {
        Mockito.when(configurationProviderAdapter.isBooleanEnabled(ConfigurationProperties.CHANGE_USER_SCHEDULE_DEADLINE_ENABLED))
                .thenReturn(true);

        LocalDate today = LocalDate.of(2021, Month.FEBRUARY, 1);
        setClockDateTime(LocalDateTime.of(today, LocalTime.of(18, 10)));

        assertThrows(
                TplInvalidActionException.class,
                () -> userScheduleValidator.checkCanEditScheduleByPartnerCompany(today, anotherSc.getId()),
                "Изменение расписания запрещено после 18:00"
        );
    }


    private void setClockDateTime(LocalDateTime now) {
        Mockito.when(clock.instant())
                .thenReturn(now.atZone(DateTimeUtil.DEFAULT_ZONE_ID).toInstant());
        Mockito.when(clock.getZone())
                .thenReturn(DateTimeUtil.DEFAULT_ZONE_ID);
    }

}
