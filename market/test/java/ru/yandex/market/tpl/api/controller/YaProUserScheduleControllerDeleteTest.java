package ru.yandex.market.tpl.api.controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.tpl.api.BaseApiTest;
import ru.yandex.market.tpl.api.mapper.SlotMapper;
import ru.yandex.market.tpl.api.model.schedule.UserScheduleType;
import ru.yandex.market.tpl.api.model.user.UserType;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterGroup;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterGroupRepository;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterRepository;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserRepository;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleRuleRepository;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleService;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleTestHelper;
import ru.yandex.market.tpl.server.model.SlotActionRejectedDto;
import ru.yandex.market.tpl.server.model.UserScheduleStatusDto;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class YaProUserScheduleControllerDeleteTest extends BaseApiTest {
    private final UserRepository userRepository;
    private final SortingCenterRepository sortingCenterRepository;
    private final UserScheduleService userScheduleService;
    private final SlotMapper slotMapper;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final UserScheduleRuleRepository userScheduleRuleRepository;
    private final SortingCenterGroupRepository sortingCenterGroupRepository;

    private User user;


    @BeforeEach
    @Transactional
    void before() {
        user = userHelper.findOrCreateUserWithoutSchedule(142759234536873L);
        user.setUserType(UserType.SELF_EMPLOYED);
        userRepository.save(user);
        SortingCenterGroup scGroup = new SortingCenterGroup();
        scGroup.setName("ТЕСТОВАЯ");
        sortingCenterGroupRepository.save(scGroup);
        SortingCenter sc = sortingCenterRepository.getById(SortingCenter.DEFAULT_SC_ID);
        sc.setSortingCenterGroup(scGroup);
        sortingCenterRepository.save(sc);
    }


    @Test
    void driverV1MarketCourierUserScheduleRulesDelete() {
        var sc = sortingCenterRepository.getById(SortingCenter.DEFAULT_SC_ID);
        var todaySlot = userScheduleService.scheduleOverride(user.getUid(), UserScheduleTestHelper.ruleDto(
                UserScheduleType.OVERRIDE_WORK, LocalDate.now(sc.getZoneOffset()),
                LocalDate.now(sc.getZoneOffset()).plusDays(1)), true);
        var tomorrowSlotBeforeCutOff = userScheduleService.scheduleOverride(user.getUid(),
                UserScheduleTestHelper.ruleDto(
                        UserScheduleType.OVERRIDE_WORK, LocalDate.now(sc.getZoneOffset()).plusDays(1),
                        LocalDate.now(sc.getZoneOffset()).plusDays(2)), true);
        var tomorrowSlotAfterCutOff = userScheduleService.scheduleOverride(user.getUid(),
                UserScheduleTestHelper.ruleDto(
                        UserScheduleType.OVERRIDE_WORK, LocalDate.now(sc.getZoneOffset()).plusDays(1),
                        LocalDate.now(sc.getZoneOffset()).plusDays(2)), true);
        Long noSlotId = 23534643634353L;
        configurationServiceAdapter.mergeValue(ConfigurationProperties.SC_USER_SCHEDULE_RULE_CHANGE_CUT_OFF,
                LocalTime.now(sc.getZoneOffset()).plusMinutes(5));

        var result = slotMapper.mapResponseToUpsert(userScheduleService.deleteSlots(List.of(todaySlot.getId(),
                tomorrowSlotBeforeCutOff.getId(), noSlotId), user.getId()), user.getId());
        assertThat(result.getData()).isEmpty();
        assertThat(result.getRejectedActions().size()).isEqualTo(2);
        var todaySlotResult = result.getRejectedActions().stream().filter(res -> Objects.equals(res.getId(),
                todaySlot.getId())).findAny().orElseThrow();
        var notSlotResult = result.getRejectedActions().stream().filter(res -> Objects.equals(res.getId(),
                noSlotId)).findAny().orElseThrow();
        assertRejected(todaySlotResult, "Удаление отклонено для данного слота", todaySlot.getActiveFrom());
        assertRejected(notSlotResult, "Слот с id " + noSlotId + " не найден!", null);
        assertThat(userScheduleRuleRepository.getAllByIdIn(List.of(tomorrowSlotBeforeCutOff.getId()))).isEmpty();

        configurationServiceAdapter.mergeValue(ConfigurationProperties.SC_USER_SCHEDULE_RULE_CHANGE_CUT_OFF,
                LocalTime.now(sc.getZoneOffset()).minusMinutes(5));
        var result2 = slotMapper.mapResponseToUpsert(
                userScheduleService.deleteSlots(List.of(tomorrowSlotAfterCutOff.getId()), user.getId()), user.getId());
        var tomorrowSlotResult = result2.getData().stream().filter(res -> Objects.equals(res.getId(),
                tomorrowSlotAfterCutOff.getId())).findAny().orElseThrow();
        assertThat(tomorrowSlotResult.getStatus()).isEqualTo(UserScheduleStatusDto.REJECTED_BY_COURIER);
    }

    private void assertRejected(SlotActionRejectedDto slotRejected, String message, LocalDate date) {
        assertThat(slotRejected.getExpectedRejection()).isTrue();
        assertThat(slotRejected.getMessage()).isEqualTo(message);
        assertThat(slotRejected.getDate()).isEqualTo(date);
    }

}
