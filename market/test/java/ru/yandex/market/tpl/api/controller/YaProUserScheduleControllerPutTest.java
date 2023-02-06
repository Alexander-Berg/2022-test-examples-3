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
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleService;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleTestHelper;
import ru.yandex.market.tpl.server.model.SlotActionRejectedDto;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.SELF_EMPLOYED_USER_SCHEDULE_RULE_STATUS_PLANNED_ENABLED;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class YaProUserScheduleControllerPutTest extends BaseApiTest {
    private final SortingCenterGroupRepository sortingCenterGroupRepository;
    private final SortingCenterRepository sortingCenterRepository;
    private final UserScheduleService userScheduleService;
    private final UserRepository userRepository;
    private final SlotMapper slotMapper;
    private final ConfigurationServiceAdapter configurationServiceAdapter;

    private SortingCenter sc;
    private SortingCenter sc2;
    private SortingCenterGroup scGroup;
    private SortingCenterGroup scGroup2;
    private User user;

    @BeforeEach
    @Transactional
    void before() {
        configurationServiceAdapter.mergeValue(SELF_EMPLOYED_USER_SCHEDULE_RULE_STATUS_PLANNED_ENABLED, true);
        user = userHelper.findOrCreateUserWithoutSchedule(142752536873L);
        user.setUserType(UserType.SELF_EMPLOYED);
        userRepository.save(user);
        scGroup = new SortingCenterGroup();
        scGroup.setName("ТЕСТОВАЯ4546");
        scGroup2 = new SortingCenterGroup();
        scGroup2.setName("ТЕСТОВАЯ454");
        sortingCenterGroupRepository.save(scGroup);
        sortingCenterGroupRepository.save(scGroup2);
        sc = sortingCenterRepository.getById(SortingCenter.DEFAULT_SC_ID);
        sc.setSortingCenterGroup(scGroup);
        sortingCenterRepository.save(sc);
        sc2 = userHelper.sortingCenter(3464645793L);
        sc2.setSortingCenterGroup(scGroup2);
        sortingCenterRepository.save(sc2);
    }


    @Test
    void driverV1MarketCourierUserScheduleRulesPut() {
        var todaySlot = userScheduleService.scheduleOverride(user.getUid(), UserScheduleTestHelper.ruleDto(
                UserScheduleType.OVERRIDE_WORK, LocalDate.now(sc.getZoneOffset()),
                LocalDate.now(sc.getZoneOffset()).plusDays(1)), true);
        var tomorrowSlot = userScheduleService.scheduleOverride(user.getUid(),
                UserScheduleTestHelper.ruleDto(
                        UserScheduleType.OVERRIDE_WORK, LocalDate.now(sc.getZoneOffset()).plusDays(1),
                        LocalDate.now(sc.getZoneOffset()).plusDays(2)), true);
        Long noSlotId = 235346343533L;
        configurationServiceAdapter.mergeValue(ConfigurationProperties.SC_USER_SCHEDULE_RULE_CHANGE_CUT_OFF,
                LocalTime.now(sc.getZoneOffset()).plusMinutes(5));
        var result = slotMapper.mapResponseToUpsert(userScheduleService.changeSc(scGroup2.getId(),
                List.of(todaySlot.getId(),
                tomorrowSlot.getId(), noSlotId), user.getId()), user.getId());
        assertThat(result.getData().size()).isEqualTo(1);
        assertThat(result.getRejectedActions().size()).isEqualTo(2);
        var todaySlotResult = result.getRejectedActions().stream().filter(res -> Objects.equals(res.getId(),
                todaySlot.getId())).findAny().orElseThrow();
        var notSlotResult = result.getRejectedActions().stream().filter(res -> Objects.equals(res.getId(),
                noSlotId)).findAny().orElseThrow();
        var tomorrowSlotResult = result.getData().stream().filter(res -> Objects.equals(res.getId(),
                tomorrowSlot.getId())).findAny().orElseThrow();
        assertRejected(todaySlotResult, "Смена сц отклонена для данного слота", todaySlot.getActiveFrom(),
                todaySlot.getId());
        assertRejected(notSlotResult, "Слот с id " + noSlotId + " не найден!", null, noSlotId);
        assertThat(tomorrowSlotResult.getSortingCenterGroup().getId()).isEqualTo(scGroup2.getId());

        configurationServiceAdapter.mergeValue(ConfigurationProperties.SC_USER_SCHEDULE_RULE_CHANGE_CUT_OFF,
                LocalTime.now(sc.getZoneOffset()).minusMinutes(5));
        var result2 = slotMapper.mapResponseToUpsert(
                userScheduleService.changeSc(scGroup.getId(), List.of(tomorrowSlot.getId()), user.getId()),
                user.getId());
        var tomorrowSlotResult2 = result2.getRejectedActions().stream().filter(res -> Objects.equals(res.getId(),
                tomorrowSlot.getId())).findAny().orElseThrow();
        assertRejected(tomorrowSlotResult2, "Смена сц отклонена для данного слота", tomorrowSlot.getActiveFrom(),
                tomorrowSlot.getId());
    }

    private void assertRejected(SlotActionRejectedDto slotRejected, String message, LocalDate date, Long id) {
        assertThat(slotRejected.getExpectedRejection()).isTrue();
        assertThat(slotRejected.getMessage()).isEqualTo(message);
        assertThat(slotRejected.getDate()).isEqualTo(date);
        assertThat(slotRejected.getId()).isEqualTo(id);
    }
}
