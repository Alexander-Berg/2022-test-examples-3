package ru.yandex.market.tpl.core.service.user.schedule;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.tpl.api.model.schedule.UpdateSlotDto;
import ru.yandex.market.tpl.api.model.schedule.UserScheduleType;
import ru.yandex.market.tpl.api.model.user.CourierVehicleType;
import ru.yandex.market.tpl.api.model.user.UserStatus;
import ru.yandex.market.tpl.common.util.datetime.RelativeTimeInterval;
import ru.yandex.market.tpl.core.domain.company.CompanyPermissionsProjection;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.service.user.SortingCenterPropertyService;
import ru.yandex.market.tpl.core.test.ClockUtil;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties.SLOT_DURATION_IN_MINUTES;

@RequiredArgsConstructor
class SlotTest extends TplAbstractTest {

    private final TestUserHelper userHelper;
    private final UserScheduleService userScheduleService;
    private final SlotDistributionService slotDistributionService;
    private final UserScheduleRuleRepository userScheduleRuleRepository;
    private final JdbcTemplate jdbcTemplate;
    private final SortingCenterPropertyService sortingCenterPropertyService;
    private final UserShiftCommandService userShiftCommandService;
    private final UserShiftRepository userShiftRepository;


    private final Clock clock;
    private User user1;
    private User user2;
    private User user3;
    private boolean isSuperCompany;
    private SortingCenter sortingCenter;
    private Shift shift;

    @BeforeEach
    void init() {
        ClockUtil.initFixed(clock);
        user1 = userHelper.findOrCreateUserWithoutSchedule(1L);
        user2 = userHelper.findOrCreateUserWithoutSchedule(2L);
        user3 = userHelper.findOrCreateUserWithoutSchedule(3L);
        sortingCenter = userHelper.sortingCenter(SortingCenter.DEFAULT_SC_ID);
        isSuperCompany = true;
        shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));
    }

    @Test
    void getSlotsForSc_couriersWithoutSlots() {
        var rule1 = userScheduleService.createRule(user1.getId(), UserScheduleTestHelper.ruleDto(
                UserScheduleType.FIVE_TWO, LocalDate.now(clock), null), isSuperCompany);
        var rule2 = userScheduleService.createRule(user2.getId(), UserScheduleTestHelper.ruleDto(
                UserScheduleType.FIVE_TWO, LocalDate.now(clock), null), isSuperCompany);
        var rule3 = userScheduleService.createRule(user3.getId(), UserScheduleTestHelper.ruleDto(
                UserScheduleType.FIVE_TWO, LocalDate.now(clock), null), isSuperCompany);
        removeLoadingStartTime(rule1.getId());
        removeLoadingStartTime(rule2.getId());
        removeLoadingStartTime(rule3.getId());

        var slotsDistribution = slotDistributionService.getSlotDistributionForSc(SortingCenter.DEFAULT_SC_ID,
                LocalDate.now(clock), isSuperCompany);

        assertThat(slotsDistribution.getNotDistributed()).isEqualTo(3);
        assertThat(slotsDistribution.getSlots()).isEmpty();
    }

    @Test
    void getSlotsForSc_oneCourierWithSlot() {
        var rule1Dto = userScheduleService.createRule(user1.getId(), UserScheduleTestHelper.ruleDto(
                UserScheduleType.FIVE_TWO, LocalDate.now(clock), null), isSuperCompany);
        setLoadingInterval(rule1Dto.getId(), LocalTime.of(10, 0));

        var rule2Dto = userScheduleService.createRule(user2.getId(), UserScheduleTestHelper.ruleDto(
                UserScheduleType.FIVE_TWO, LocalDate.now(clock), null), isSuperCompany);
        var rule3Dto = userScheduleService.createRule(user3.getId(), UserScheduleTestHelper.ruleDto(
                UserScheduleType.FIVE_TWO, LocalDate.now(clock), null), isSuperCompany);
        removeLoadingStartTime(rule2Dto.getId());
        removeLoadingStartTime(rule3Dto.getId());

        var slotsDistribution = slotDistributionService.getSlotDistributionForSc(SortingCenter.DEFAULT_SC_ID,
                LocalDate.now(clock), isSuperCompany);

        assertThat(slotsDistribution.getNotDistributed()).isEqualTo(2);
        assertThat(slotsDistribution.getSlots()).hasSize(1);
        var slot = slotsDistribution.getSlots().iterator().next();
        assertThat(slot.getCourierCount()).isEqualTo(1L);
        assertThat(slot.getStart()).isEqualTo(LocalTime.of(10, 0));
    }

    @Test
    void getSlotsForSc_allCouriersWithSameSlot() {
        var rule1Dto = userScheduleService.createRule(user1.getId(), UserScheduleTestHelper.ruleDto(
                UserScheduleType.FIVE_TWO, LocalDate.now(clock), null), isSuperCompany);
        setLoadingInterval(rule1Dto.getId(), LocalTime.of(10, 0));

        var rule2Dto = userScheduleService.createRule(user2.getId(), UserScheduleTestHelper.ruleDto(
                UserScheduleType.FIVE_TWO, LocalDate.now(clock), null), isSuperCompany);
        setLoadingInterval(rule2Dto.getId(), LocalTime.of(10, 0));

        var rule3Dto = userScheduleService.createRule(user3.getId(), UserScheduleTestHelper.ruleDto(
                UserScheduleType.FIVE_TWO, LocalDate.now(clock), null), isSuperCompany);
        setLoadingInterval(rule3Dto.getId(), LocalTime.of(10, 0));

        var slotsDistribution = slotDistributionService.getSlotDistributionForSc(SortingCenter.DEFAULT_SC_ID,
                LocalDate.now(clock), isSuperCompany);

        assertThat(slotsDistribution.getNotDistributed()).isEqualTo(0);
        assertThat(slotsDistribution.getSlots()).hasSize(1);
        var slot = slotsDistribution.getSlots().iterator().next();
        assertThat(slot.getCourierCount()).isEqualTo(3L);
        assertThat(slot.getStart()).isEqualTo(LocalTime.of(10, 0));
    }

    @Test
    void getSlotsForSc_allCouriersWithDifferentSlot() {
        var rule1Dto = userScheduleService.createRule(user1.getId(), UserScheduleTestHelper.ruleDto(
                UserScheduleType.FIVE_TWO, LocalDate.now(clock), null), isSuperCompany);
        setLoadingInterval(rule1Dto.getId(), LocalTime.of(10, 0));

        var rule2Dto = userScheduleService.createRule(user2.getId(), UserScheduleTestHelper.ruleDto(
                UserScheduleType.FIVE_TWO, LocalDate.now(clock), null), isSuperCompany);
        setLoadingInterval(rule2Dto.getId(), LocalTime.of(10, 30));

        var rule3Dto = userScheduleService.createRule(user3.getId(), UserScheduleTestHelper.ruleDto(
                UserScheduleType.FIVE_TWO, LocalDate.now(clock), null), isSuperCompany);
        setLoadingInterval(rule3Dto.getId(), LocalTime.of(11, 0));

        var slotsDistribution = slotDistributionService.getSlotDistributionForSc(SortingCenter.DEFAULT_SC_ID,
                LocalDate.now(clock), isSuperCompany);

        assertThat(slotsDistribution.getNotDistributed()).isEqualTo(0);
        assertThat(slotsDistribution.getSlots()).hasSize(3);
        var slot = slotsDistribution.getSlots().get(0);
        assertThat(slot.getCourierCount()).isEqualTo(1L);
        assertThat(slot.getStart()).isEqualTo(LocalTime.of(10, 0));
        slot = slotsDistribution.getSlots().get(1);
        assertThat(slot.getCourierCount()).isEqualTo(1L);
        assertThat(slot.getStart()).isEqualTo(LocalTime.of(10, 30));
        slot = slotsDistribution.getSlots().get(2);
        assertThat(slot.getCourierCount()).isEqualTo(1L);
        assertThat(slot.getStart()).isEqualTo(LocalTime.of(11, 0));
    }

    @Test
    void getSlotsForSc_allCouriersWithInvalidSlot() {
        var rule1Dto = userScheduleService.createRule(user1.getId(), UserScheduleTestHelper.ruleDto(
                UserScheduleType.FIVE_TWO, LocalDate.now(clock), null), isSuperCompany);
        setInvalidLoadingInterval(rule1Dto.getId());

        var rule2Dto = userScheduleService.createRule(user2.getId(), UserScheduleTestHelper.ruleDto(
                UserScheduleType.FIVE_TWO, LocalDate.now(clock), null), isSuperCompany);
        setInvalidLoadingInterval(rule2Dto.getId());

        var rule3Dto = userScheduleService.createRule(user3.getId(), UserScheduleTestHelper.ruleDto(
                UserScheduleType.FIVE_TWO, LocalDate.now(clock), null), isSuperCompany);
        setInvalidLoadingInterval(rule3Dto.getId());

        var slotsDistribution = slotDistributionService.getSlotDistributionForSc(SortingCenter.DEFAULT_SC_ID,
                LocalDate.now(clock), isSuperCompany);

        assertThat(slotsDistribution.getNotDistributed()).isEqualTo(3);
        assertThat(slotsDistribution.getSlots()).isEmpty();
    }

    @Test
    void setSlots_allSlotsValid() {
        var ruleDto1 = userScheduleService.createRule(user1.getId(),
                UserScheduleTestHelper.ruleDtoApplyFrom(
                        UserScheduleType.FIVE_TWO,
                        LocalDate.now(clock),
                        null,
                        LocalDate.now(clock),
                        UserScheduleType.FIVE_TWO.getMaskWorkDays(),
                        SortingCenter.DEFAULT_SC_ID,
                        new UserScheduleData(CourierVehicleType.CAR, RelativeTimeInterval.valueOf("10:00-21:00"))
                ), isSuperCompany);
        changeUserStatus(user2, UserStatus.ACTIVE);
        var ruleDto2 = userScheduleService.createRule(user2.getId(), UserScheduleTestHelper.ruleDto(
                UserScheduleType.FIVE_TWO, LocalDate.now(clock), null), isSuperCompany);
        changeUserStatus(user3, UserStatus.NOT_ACTIVE);

        var result = slotDistributionService.setSlots(
                Map.of(
                        ruleDto1.getId(), new UpdateSlotDto(LocalTime.of(10, 0)),
                        ruleDto2.getId(), new UpdateSlotDto(LocalTime.of(10, 30))
                ),
                CompanyPermissionsProjection.builder().sortingCentersIds(Set.of(SortingCenter.DEFAULT_SC_ID)).build()
        );

        var rules = result.getScheduleRules();
        var rule1 = rules.get(ruleDto1.getId());
        assertThat(rule1.getLoadInterval()).isNotNull();
        assertThat(rule1.getLoadInterval().getStart()).isEqualTo(LocalTime.of(10, 0));
        assertThat(rule1.getShiftStart()).isEqualTo(LocalTime.of(10, 30));
        assertThat(rule1.getShiftEnd()).isEqualTo(LocalTime.of(21, 0));
        var rule2 = rules.get(ruleDto2.getId());
        assertThat(rule2.getLoadInterval()).isNotNull();
        assertThat(rule2.getLoadInterval().getStart()).isEqualTo(LocalTime.of(10, 30));
        assertThat(rule2.getShiftStart()).isEqualTo(LocalTime.of(11, 0));
        assertThat(rule2.getShiftEnd()).isEqualTo(LocalTime.of(18, 0));
    }

    @Test
    void getSchedules_filterBySlot() {
        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                sortingCenter, SortingCenterProperties.ONE_DAY_SLOTS_SCHEDULE_ENABLED, true);
        var ruleDto1 = userScheduleService.createRule(user1.getId(), UserScheduleTestHelper.ruleDto(
                UserScheduleType.FIVE_TWO, LocalDate.now(clock), null), isSuperCompany);
        changeUserStatus(user2, UserStatus.ACTIVE);
        var ruleDto2 = userScheduleService.createRule(user2.getId(), UserScheduleTestHelper.ruleDto(
                UserScheduleType.FIVE_TWO, LocalDate.now(clock), null), isSuperCompany);
        changeUserStatus(user3, UserStatus.NOT_ACTIVE);
        removeLoadingStartTime(ruleDto2.getId());

        slotDistributionService.setSlots(
                Map.of(
                        ruleDto1.getId(), new UpdateSlotDto(LocalTime.of(10, 0))
                ),
                CompanyPermissionsProjection.builder().sortingCentersIds(Set.of(SortingCenter.DEFAULT_SC_ID)).build()
        );

        var request = new UserSchedulesSearchRequest(
                List.of(),
                List.of(),
                List.of(SortingCenter.DEFAULT_SC_ID),
                LocalDate.now(clock),
                LocalDate.now(clock),
                List.of(),
                List.of(),
                List.of(),
                "10:00:00"
        );

        var result = userScheduleService.findSchedules(request, isSuperCompany);
        assertThat(result.getUsers().get(user1.getId()).getCalendar()).isNotEmpty();
        assertThat(result.getUsers().get(user2.getId()).getCalendar()).isEmpty();

        request = new UserSchedulesSearchRequest(
                List.of(),
                List.of(),
                List.of(SortingCenter.DEFAULT_SC_ID),
                LocalDate.now(clock),
                LocalDate.now(clock),
                List.of(),
                List.of(),
                List.of(),
                UserSchedulesSearchRequest.NOT_DISTRIBUTED_SLOT
        );

        result = userScheduleService.findSchedules(request, isSuperCompany);
        assertThat(result.getUsers().get(user2.getId()).getCalendar()).isNotEmpty();
        assertThat(result.getUsers().get(user1.getId()).getCalendar()).isEmpty();
    }

    @Test
    void userShiftCreatedWithLoadingStartInterval() {
        var loadingStartTime = LocalTime.of(9, 0);
        var data = new UserScheduleData(CourierVehicleType.CAR, new RelativeTimeInterval(LocalTime.of(10, 0),
                LocalTime.of(22, 0)));
        data.setLoadingStartTime(loadingStartTime);

        var createCommand = UserShiftCommand.Create.builder()
                .scheduleData(data)
                .userId(user1.getId())
                .active(true)
                .shiftId(shift.getId())
                .mergeStrategy(SimpleStrategies.BY_DATE_INTERVAL_MERGE)
                .build();

        long userShiftId = userShiftCommandService.createUserShift(createCommand);
        var userShift = userShiftRepository.findByIdOrThrow(userShiftId);
        assertThat(userShift.getScheduleData().getLoadingStartTime()).isEqualTo(loadingStartTime);
    }

    @Test
    void getUserScheduleReport() {
        var rule1Dto = userScheduleService.createRule(user1.getId(), UserScheduleTestHelper.ruleDto(
                UserScheduleType.FIVE_TWO, LocalDate.now(clock), null), isSuperCompany);
        setLoadingInterval(rule1Dto.getId(), LocalTime.of(10, 0));

        var reportDto = userScheduleService.findScheduleReportDto(new UserSchedulesSearchRequest(
                List.of(),
                List.of(),
                List.of(),
                LocalDate.now(clock),
                LocalDate.now(clock),
                List.of(),
                List.of(),
                List.of(),
                null
        ));

        assertThat(reportDto).hasSize(1);
        var dto = reportDto.iterator().next();
        assertThat(dto.getSlots()).isNotNull();
        assertThat(dto.getSlots()).hasSize(1);
        assertThat(dto.getSlots().values().iterator().next()).isEqualTo("10:00");
    }

    private void setLoadingInterval(Long ruleId, LocalTime loadingStart) {
        var rule = userScheduleRuleRepository.findByIdOrThrow(ruleId);
        assertThat(rule.getScheduleData()).isNotNull();
        rule.getScheduleData().setLoadingStartTime(loadingStart);
        Objects.requireNonNull(rule.getScheduleData()).setShiftStart(
                (LocalTime) Duration.ofMinutes(
                        SLOT_DURATION_IN_MINUTES.getDefaultValue()).addTo(loadingStart)
        );
        userScheduleRuleRepository.save(rule);
    }

    private void removeLoadingStartTime(Long ruleId) {
        var rule = userScheduleRuleRepository.findByIdOrThrow(ruleId);
        rule.getScheduleData().setLoadingStartTime(null);
        userScheduleRuleRepository.save(rule);
    }

    private void setInvalidLoadingInterval(Long ruleId) {
        var rule = userScheduleRuleRepository.findByIdOrThrow(ruleId);
        var shiftStart = Objects.requireNonNull(rule.getScheduleData()).getShiftStart();
        rule.getScheduleData().setLoadingStartTime((LocalTime) Duration.ofHours(1).subtractFrom(shiftStart));
        userScheduleRuleRepository.save(rule);
    }

    private void changeUserStatus(User user, UserStatus userStatus) {
        jdbcTemplate.update("UPDATE users SET status = ? WHERE id = ?", userStatus.toString(), user.getId());
    }

}
