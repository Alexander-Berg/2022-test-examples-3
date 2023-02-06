package ru.yandex.market.tpl.core.domain.order.postpone;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.service.order.PostponeOrderService;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.test.ClockUtil;
import ru.yandex.market.tpl.core.test.TestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;


@RequiredArgsConstructor(onConstructor = @__(@Autowired))

@CoreTest
public class AvailableDurationsPostponementTest {
    private final TestUserHelper testUserHelper;
    private final SortingCenterService sortingCenterService;
    private final TestDataFactory testDataFactory;
    private final UserShiftRepository userShiftRepository;
    private final PostponeOrderService postponeOrderService;

    @MockBean
    private Clock clock;

    private UserShift userShift;

    @BeforeEach
    void init() {
        mockTime(LocalTime.parse("07:00"));

        User user = testUserHelper.findOrCreateUser(1L);
        Shift shift = testUserHelper.findOrCreateOpenShiftForSc(LocalDate.now(clock),
                sortingCenterService.findSortCenterForDs(239).getId());
        userShift = userShiftRepository
                .findByIdOrThrow(testDataFactory.createEmptyShift(shift.getId(), user));
    }

    @ParameterizedTest
    @ArgumentsSource(TestDataProvider.class)
    @DisplayName("Доступные промежутки времени для переноса в течение дня")
    void getAvailableDurations(LocalTime nowTime, int expectedDurationsSize) {
        mockTime(nowTime);

        List<Duration> availableDurationsPostponement =
                postponeOrderService.getAvailableDurationsPostponement(userShift);

        assertThat(availableDurationsPostponement).hasSize(expectedDurationsSize);
    }

    private void mockTime(LocalTime newTime) {
        ClockUtil.initFixed(clock, LocalDateTime.of(LocalDate.now(), newTime));
    }

    static class TestDataProvider implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    Arguments.of(LocalTime.parse("08:00"), 9),
                    Arguments.of(LocalTime.parse("10:00"), 8),
                    Arguments.of(LocalTime.parse("13:00"), 6),
                    Arguments.of(LocalTime.parse("15:00"), 5),
                    Arguments.of(LocalTime.parse("18:00"), 2),
                    Arguments.of(LocalTime.parse("19:50"), 0),
                    Arguments.of(LocalTime.parse("23:59"), 0)
            );
        }
    }

}
