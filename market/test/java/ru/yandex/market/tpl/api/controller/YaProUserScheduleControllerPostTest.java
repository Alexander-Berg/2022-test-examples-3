package ru.yandex.market.tpl.api.controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
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
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserRepository;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleRuleRepository;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleService;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleTestHelper;
import ru.yandex.market.tpl.server.model.ScheduleSlotDto;
import ru.yandex.market.tpl.server.model.SlotActionRejectedDto;
import ru.yandex.market.tpl.server.model.UserScheduleStatusDto;

import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.SELF_EMPLOYED_USER_SCHEDULE_RULE_STATUS_PLANNED_ENABLED;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class YaProUserScheduleControllerPostTest extends BaseApiTest {
    private final SortingCenterGroupRepository sortingCenterGroupRepository;
    private final SortingCenterRepository sortingCenterRepository;
    private final SlotMapper slotMapper;
    private final UserScheduleService userScheduleService;
    private final UserRepository userRepository;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final UserScheduleRuleRepository userScheduleRuleRepository;

    @BeforeEach
    void beforeEach() {
        configurationServiceAdapter.mergeValue(SELF_EMPLOYED_USER_SCHEDULE_RULE_STATUS_PLANNED_ENABLED, true);
    }

    @Test
    @Transactional
    void driverV1MarketCourierUserScheduleRulesPost() {
        SortingCenterGroup scGroup = new SortingCenterGroup();
        scGroup.setName("ТЕСТОВАЯ");
        scGroup = sortingCenterGroupRepository.save(scGroup);
        SortingCenter sc1 = sortingCenterRepository.getById(SortingCenter.DEFAULT_SC_ID);
        sc1.setSortingCenterGroup(scGroup);
        SortingCenter sc2 = userHelper.sortingCenter(34646453552L);
        sc2.setSortingCenterGroup(scGroup);
        sortingCenterRepository.save(sc1);
        sortingCenterRepository.save(sc2);

        Shift shift = userHelper.findOrCreateOpenShiftForSc(LocalDate.now(sc1.getZoneOffset()).minusDays(8),
                sc2.getId());
        User user = userHelper.findOrCreateUserWithoutSchedule(14275925823329L);
        user.setUserType(UserType.SELF_EMPLOYED);
        userRepository.save(user);
        userHelper.createEmptyShift(user, shift);

        configurationServiceAdapter.mergeValue(ConfigurationProperties.SC_USER_SCHEDULE_RULE_PLANNING_CUT_OFF,
                LocalTime.now(sc1.getZoneOffset()).plusMinutes(5));
        configurationServiceAdapter.mergeValue(ConfigurationProperties.SC_USER_SCHEDULE_RULE_PLANNING_HORIZONT, 14);

        var tomorrowDate = LocalDate.now(sc1.getZoneOffset()).plusDays(1);
        var badRightDate = LocalDate.now(sc1.getZoneOffset()).plusDays(15);
        var badLeftDate = LocalDate.now(sc1.getZoneOffset());
        var badSlotDate = LocalDate.now(sc1.getZoneOffset()).plusDays(8);

        var alreadySlot = userScheduleService.scheduleOverride(user.getUid(), UserScheduleTestHelper.ruleDto(
                UserScheduleType.OVERRIDE_WORK, badSlotDate,
                badSlotDate.plusDays(1)), true);

        var result = slotMapper.mapResponseToUpsert(userScheduleService.addSlots(scGroup.getId(),
                List.of(tomorrowDate, badRightDate, badLeftDate, badSlotDate), user.getId()), user.getId());

        var tomorrowSlotDto =
                result.getData().stream().filter(slot -> slot.getDate().isEqual(tomorrowDate)).findFirst().orElseThrow();
        var badRightSlotDto =
                result.getRejectedActions().stream().filter(slot -> slot.getDate().isEqual(badRightDate)).findFirst().orElseThrow();
        var badLeftSlotDto =
                result.getRejectedActions().stream().filter(slot -> slot.getDate().isEqual(badLeftDate)).findFirst().orElseThrow();
        var badSlotDateDto =
                result.getRejectedActions().stream().filter(slot -> slot.getDate().isEqual(badSlotDate)).findFirst().orElseThrow();
        assertSuccessResult(tomorrowSlotDto);
        assertRejectedResult(badRightSlotDto,  "Дата слота больше максимальной даты для добавления", badRightDate);
        assertRejectedResult(badLeftSlotDto, "Дата слота меньше минимальной даты для добавления", badLeftDate);
        assertRejectedResult(badSlotDateDto, "Слот на эту дату уже существует или был отменен", badSlotDate);

        Assertions.assertThat(userScheduleRuleRepository.getById(tomorrowSlotDto.getId()).getSortingCenterId()).isEqualTo(sc2.getId());

        configurationServiceAdapter.mergeValue(ConfigurationProperties.SC_USER_SCHEDULE_RULE_PLANNING_CUT_OFF,
                LocalTime.now(sc1.getZoneOffset()).minusMinutes(5));
        Assertions.assertThat(slotMapper.mapResponseToUpsert(userScheduleService.addSlots(scGroup.getId(),
                List.of(tomorrowDate), user.getId()), user.getId()).getRejectedActions()).hasSize(1);
    }

    private void assertRejectedResult(SlotActionRejectedDto dto, String message, LocalDate date) {
        Assertions.assertThat(dto.getId()).isNull();
        Assertions.assertThat(dto.getDate()).isEqualTo(date);
        Assertions.assertThat(dto.getExpectedRejection()).isEqualTo(true);
        Assertions.assertThat(dto.getMessage()).isEqualTo(message);
    }

    private void assertSuccessResult(ScheduleSlotDto dto) {
        Assertions.assertThat(dto.getId()).isNotNull();
        Assertions.assertThat(dto.getSortingCenterGroup()).isNotNull();
        Assertions.assertThat(dto.getStatus()).isEqualTo(UserScheduleStatusDto.PLANNED);
    }

    @Test
    void getRightBorderForSlotAddAction() {
        var firstSunday = LocalDate.of(2022, Month.SEPTEMBER, 19);//воскресенье
        int days = 8;
        configurationServiceAdapter.mergeValue(ConfigurationProperties.SC_USER_SCHEDULE_RULE_PLANNING_HORIZONT,
                days);
        var horizontDate = firstSunday.plusDays(days);

        userScheduleService.getRightBorderForSlotAddAction(firstSunday).isEqual(horizontDate);
        userScheduleService.getRightBorderForSlotAddAction(firstSunday.plusDays(1)).isEqual(horizontDate);
        userScheduleService.getRightBorderForSlotAddAction(firstSunday.plusDays(2)).isEqual(horizontDate);
        userScheduleService.getRightBorderForSlotAddAction(firstSunday.plusDays(3)).isEqual(horizontDate);
        userScheduleService.getRightBorderForSlotAddAction(firstSunday.plusDays(4)).isEqual(horizontDate);
        userScheduleService.getRightBorderForSlotAddAction(firstSunday.plusDays(5)).isEqual(horizontDate);
        userScheduleService.getRightBorderForSlotAddAction(firstSunday.plusDays(6)).isEqual(horizontDate);
        userScheduleService.getRightBorderForSlotAddAction(firstSunday.plusDays(7)).isEqual(firstSunday.plusDays(7 + days));
    }
}
