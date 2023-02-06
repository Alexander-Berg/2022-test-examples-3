package ru.yandex.market.tpl.api.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.BaseApiTest;
import ru.yandex.market.tpl.api.mapper.SlotMapper;
import ru.yandex.market.tpl.api.model.schedule.UserScheduleRuleDto;
import ru.yandex.market.tpl.api.model.schedule.UserScheduleType;
import ru.yandex.market.tpl.api.model.user.UserType;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterGroup;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterGroupRepository;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterRepository;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserRepository;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleRule;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleRuleRepository;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleService;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleTestHelper;
import ru.yandex.market.tpl.server.model.ScheduleSlotDto;
import ru.yandex.market.tpl.server.model.UserScheduleActionTypeDto;
import ru.yandex.market.tpl.server.model.UserScheduleResponseDto;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.SELF_EMPLOYED_USER_SCHEDULE_RULE_STATUS_PLANNED_ENABLED;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class YaProUserScheduleControllerGetTest extends BaseApiTest {
    private final SlotMapper slotMapper;
    private final TestUserHelper userHelper;
    private final UserScheduleService userScheduleService;
    private final SortingCenterRepository sortingCenterRepository;
    private final SortingCenterGroupRepository sortingCenterGroupRepository;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final UserScheduleRuleRepository userScheduleRuleRepository;
    private final UserRepository userRepository;
    private final TransactionTemplate transactionTemplate;

    private User user;
    private UserShift userShift;
    private SortingCenter sc;
    private UserScheduleRuleDto notSlot;
    private UserScheduleRuleDto slotWithShift;
    private UserScheduleRuleDto tomorrowSlot;
    private UserScheduleRuleDto todaySlot;
    private UserScheduleRuleDto slotOutOfSlotCount;
    private UserScheduleRuleDto oldSlot;

    @BeforeEach
    void before() {
        transactionTemplate.execute(status -> {
            configurationServiceAdapter.mergeValue(SELF_EMPLOYED_USER_SCHEDULE_RULE_STATUS_PLANNED_ENABLED, true);
            configurationServiceAdapter.insertValue(ConfigurationProperties.SELFEMPLOYED_COURIER_SLOTS_COUNT_TO_SEE, 5);
            SortingCenterGroup scGroup = new SortingCenterGroup();
            scGroup.setName("ТЕСТОВАЯ");
            sortingCenterGroupRepository.save(scGroup);
            sc = sortingCenterRepository.getById(SortingCenter.DEFAULT_SC_ID);
            sc.setSortingCenterGroup(scGroup);
            sortingCenterRepository.save(sc);
            user = userHelper.findOrCreateUserWithoutSchedule(14275925829L);
            userShift = userHelper.createEmptyShift(user, LocalDate.now(sc.getZoneOffset()).plusDays(5));
            user.setUserType(UserType.SELF_EMPLOYED);
            userRepository.save(user);
            configurationServiceAdapter.mergeValue(ConfigurationProperties.SC_USER_SCHEDULE_RULE_CHANGE_CUT_OFF,
                    LocalTime.now(sc.getZoneOffset()).plusMinutes(5));

            notSlot = userScheduleService.createRule(user.getId(), UserScheduleTestHelper.ruleDto(
                    UserScheduleType.TWO_TWO, LocalDate.now(sc.getZoneOffset()),
                    LocalDate.now(sc.getZoneOffset()).plusDays(1)), true);

            slotWithShift = userScheduleService.scheduleOverride(user.getUid(), UserScheduleTestHelper.ruleDto(
                    UserScheduleType.OVERRIDE_WORK, LocalDate.now(sc.getZoneOffset()).plusDays(5),
                    LocalDate.now(sc.getZoneOffset()).plusDays(6)), true);

            tomorrowSlot = userScheduleService.scheduleOverride(user.getUid(), UserScheduleTestHelper.ruleDto(
                    UserScheduleType.OVERRIDE_WORK, LocalDate.now(sc.getZoneOffset()).plusDays(1),
                    LocalDate.now(sc.getZoneOffset()).plusDays(2)), true);

            todaySlot = userScheduleService.scheduleOverride(user.getUid(), UserScheduleTestHelper.ruleDto(
                    UserScheduleType.OVERRIDE_WORK, LocalDate.now(sc.getZoneOffset()),
                    LocalDate.now(sc.getZoneOffset()).plusDays(1)), true);

            slotOutOfSlotCount = userScheduleService.scheduleOverride(user.getUid(), UserScheduleTestHelper.ruleDto(
                    UserScheduleType.OVERRIDE_WORK, LocalDate.now(sc.getZoneOffset()).plusDays(6),
                    LocalDate.now(sc.getZoneOffset()).plusDays(7)), true);

            oldSlot = userScheduleService.scheduleOverride(user.getUid(), UserScheduleTestHelper.ruleDto(
                    UserScheduleType.OVERRIDE_WORK, LocalDate.now(sc.getZoneOffset()).minusDays(1),
                    LocalDate.now(sc.getZoneOffset())), true);
            return status;
        });
    }


    @Test
    void courierSlotsGet() {
        var result = slotMapper.mapResponse(userScheduleService.findSlotsForUser(user.getId()), user.getId());
        assertThat(result.getTotalElements()).isEqualTo(3);

        assertSlot(getSlot(result, slotWithShift.getId()), userScheduleRuleRepository.getById(slotWithShift.getId()),
                LocalDateTime.of(slotWithShift.getActiveFrom(), userShift.getScheduleData().getShiftStart()),
                List.of(UserScheduleActionTypeDto.SC_CHANGE, UserScheduleActionTypeDto.DELETE));

        assertSlot(getSlot(result, tomorrowSlot.getId()), userScheduleRuleRepository.getById(tomorrowSlot.getId()),
                null, List.of(UserScheduleActionTypeDto.SC_CHANGE, UserScheduleActionTypeDto.DELETE));

        assertSlot(getSlot(result, todaySlot.getId()), userScheduleRuleRepository.getById(todaySlot.getId()),
                null, List.of());

        configurationServiceAdapter.mergeValue(ConfigurationProperties.SC_USER_SCHEDULE_RULE_CHANGE_CUT_OFF,
                LocalTime.now(sc.getZoneOffset()).minusMinutes(5));
        var result2 = slotMapper.mapResponse(userScheduleService.findSlotsForUser(user.getId()), user.getId());
        assertSlot(getSlot(result2, tomorrowSlot.getId()), userScheduleRuleRepository.getById(tomorrowSlot.getId()),
                null, List.of(UserScheduleActionTypeDto.DELETE));

    }

    private ScheduleSlotDto getSlot(UserScheduleResponseDto response, Long id) {
        return response.getData().stream().filter(dto -> Objects.equals(dto.getId(), id)).findAny().orElseThrow();
    }

    private void assertSlot(ScheduleSlotDto slotDto, UserScheduleRule rule, LocalDateTime shiftStart,
                            List<UserScheduleActionTypeDto> actions) {
        assertThat(rule.getId()).isEqualTo(slotDto.getId());
        assertThat(rule.getSortingCenter()).isNotNull();
        assertThat(slotDto.getSortingCenterGroup().getId()).isEqualTo(rule.getSortingCenter().getSortingCenterGroup().getId());
        assertThat(slotDto.getSortingCenterGroup().getName()).isEqualTo(rule.getSortingCenter().getSortingCenterGroup().getName());
        assertThat(rule.getActiveFrom()).isEqualTo(slotDto.getDate());
        assertThat(slotMapper.mapStatus(rule.getUserScheduleStatus())).isEqualTo(slotDto.getStatus());
        assertThat(slotDto.getShiftStart()).isEqualTo(shiftStart);
        assertThat(slotDto.getActions()).containsAll(actions);
    }
}
