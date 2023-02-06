package ru.yandex.market.tpl.core.service.user.schedule;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.api.model.schedule.UserScheduleRuleDto;
import ru.yandex.market.tpl.api.model.schedule.UserScheduleType;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.projection.UserRoutingProjection;
import ru.yandex.market.tpl.core.service.user.SortingCenterPropertyService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author zaxarello
 */
@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class UserScheduleQueryServiceTest {
    private final static Long UID = 12395L;

    private final TestUserHelper testUserHelper;
    private final SortingCenterPropertyService sortingCenterPropertyService;
    private final UserScheduleRoutingService userScheduleRoutingService;
    private final UserScheduleRuleRepository userScheduleRuleRepository;
    private final UserScheduleService userScheduleService;
    private final Clock clock;

    @Test
    void checkShiftEndWithDropship() {
        SortingCenter sortingCenter = testUserHelper.sortingCenter(1L);
        User user = testUserHelper.createUserWithTransportTags(UID, List.of("dropship"));
        UserScheduleRuleDto dto = UserScheduleRuleDto.builder()
                .activeFrom(LocalDate.now(clock))
                .activeTo(LocalDate.now(clock))
                .scheduleType(UserScheduleType.ALWAYS_WORKS)
                .shiftStart(LocalTime.of(20, 0))
                .shiftEnd(LocalTime.of(21, 0))
                .sortingCenterId(sortingCenter.getId())
                .applyFrom(LocalDate.now(clock))
                .build();
        userScheduleService.createRule(user.getId(), dto, user.getCompany().isSuperCompany());
        UserScheduleRule rule = userScheduleRuleRepository.findUserScheduleForDate(user, LocalDate.now(clock))
                .getPrimaryActiveRule(LocalDate.now(clock)).get();
        rule.getScheduleData().setShiftEnd(LocalTime.of(21, 0));
        sortingCenterPropertyService.upsertPropertyToSortingCenter(sortingCenter,
                SortingCenterProperties.MAX_SHIFT_DROPSHIP_END_TIME, LocalTime.of(20, 0));
        assertThat(userScheduleRoutingService.calculateShiftEnd(rule)).isEqualTo(LocalTime.of(20, 0));
    }

    @Test
    void checkShiftEndWithoutDropship() {
        SortingCenter sortingCenter = testUserHelper.sortingCenter(1L);
        User user = testUserHelper.findOrCreateUser(UID);
        UserScheduleRule rule = userScheduleRuleRepository.findUserScheduleForDate(user, LocalDate.now(clock))
                .getPrimaryActiveRule(LocalDate.now(clock)).get();
        rule.getScheduleData().setShiftEnd(LocalTime.of(23, 31));
        assertThat(userScheduleRoutingService.calculateShiftEnd(rule)).isEqualTo(UserRoutingProjection.MAX_SHIFT_END_TIME);
    }
}
