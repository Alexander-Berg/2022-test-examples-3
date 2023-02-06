package ru.yandex.market.tpl.core.service.user.schedule;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import ru.yandex.market.tpl.api.model.schedule.UserScheduleDto;
import ru.yandex.market.tpl.api.model.schedule.UserScheduleMetaType;
import ru.yandex.market.tpl.api.model.schedule.UserScheduleRuleDto;
import ru.yandex.market.tpl.api.model.schedule.UserScheduleType;
import ru.yandex.market.tpl.api.model.transport.PartnerTransportTypeDto;
import ru.yandex.market.tpl.api.model.transport.RoutingVehicleType;
import ru.yandex.market.tpl.api.model.user.CourierVehicleType;
import ru.yandex.market.tpl.common.db.exception.TplEntityNotFoundException;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.common.util.exception.TplInvalidParameterException;
import ru.yandex.market.tpl.common.web.exception.TplInvalidActionException;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterRepository;
import ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.service.user.SortingCenterPropertyService;
import ru.yandex.market.tpl.core.test.ClockUtil;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.market.tpl.api.model.schedule.ViewMode.ONE_DAY;
import static ru.yandex.market.tpl.api.model.schedule.ViewMode.ONE_DAY_SLOTS;
import static ru.yandex.market.tpl.api.model.schedule.ViewMode.ONE_MONTH;
import static ru.yandex.market.tpl.api.model.schedule.ViewMode.ONE_WEEK;
import static ru.yandex.market.tpl.core.service.user.schedule.UserScheduleTestHelper.ruleDtoApplyFrom;

/**
 * @author valter
 */
@RequiredArgsConstructor
class UserScheduleServiceTest extends TplAbstractTest {

    private final TestUserHelper userHelper;
    private final UserScheduleService service;
    private final SortingCenterPropertyService sortingCenterPropertyService;
    private final SortingCenterRepository sortingCenterRepository;
    private final Clock clock;
    private User user;
    private boolean company;

    private static List<UserScheduleRuleDto> notValidRuleRequests() {
        var today = ClockUtil.defaultDateTime().toLocalDate();
        var yesterday = today.minusDays(1);
        var tomorrow = today.plusDays(1);
        return List.of(
                ruleDto(UserScheduleType.OVERRIDE_WORK, today, null), // open activeTo in override
                ruleDto(UserScheduleType.FIVE_TWO, yesterday, today), // expired
                ruleDtoApplyFrom(UserScheduleType.SIX_ONE, today, today, tomorrow), // apply after start
                ruleDto(UserScheduleType.FIVE_TWO, tomorrow, today) // empty active interval
        );
    }

    // isWorkday

    /**
     * Dates:               def-1 def def+1 def+2 def+3
     * <p/>
     * Init rule:           [def-1, inf)
     * Rule creation date:  def-1
     * Rule change date:    def
     */
    private static List<LocalDate> activeToChanges() {
        var def = ClockUtil.defaultDateTime();
        return Arrays.asList(
                def.toLocalDate(),
                def.plusDays(1).toLocalDate(),
                def.plusDays(3).toLocalDate(),
                null
        );
    }

    /**
     * Dates:               def-1 def def+1 def+2 def+3
     * <p/>
     * Init rule:           [def-1, inf)
     * Rule creation date:  def-1
     * Rule change date:    def
     * New rule:            [def+1, def+3]
     */
    private static List<LocalDate> activeFromChanges() {
        var def = ClockUtil.defaultDateTime();
        return Arrays.asList(
                def.minusDays(1).toLocalDate(),
                def.toLocalDate(),
                def.plusDays(1).toLocalDate(),
                def.plusDays(3).toLocalDate()
        );
    }

    private static UserScheduleRuleDto ruleDto(UserScheduleType type, LocalDate from, LocalDate to) {
        return UserScheduleTestHelper.ruleDto(type, from, to);
    }


    @BeforeEach
    void init() {
        ClockUtil.initFixed(clock);
        user = userHelper.findOrCreateUserWithoutSchedule(1L);
        userHelper.sortingCenter(SortingCenter.DEFAULT_SC_ID);
        company = true;
    }

    @Test
    void isWorkdayNoSchedule() {
        checkWorkDay(today(), false);
        checkWorkDay(today().plusDays(1), false);
        checkWorkDay(today().plusDays(2), false);
        checkWorkDay(today().plusDays(3), false);
        checkWorkDay(today().plusDays(4), false);
        checkWorkDay(today().plusDays(5), false);
        checkWorkDay(today().plusDays(6), false);
        checkWorkDay(today().plusDays(7), false);
    }

    @Test
    void isWorkdayTwoTwo() {
        service.createRule(user.getId(), ruleDto(UserScheduleType.TWO_TWO, today(), null), company);
        checkWorkDay(today(), true);
        checkWorkDay(today().plusDays(1), true);
        checkWorkDay(today().plusDays(2), false);
        checkWorkDay(today().plusDays(3), false);
        checkWorkDay(today().plusDays(4), true);
        checkWorkDay(today().plusDays(5), true);
        checkWorkDay(today().plusDays(6), false);
        checkWorkDay(today().plusDays(7), false);
    }

    @Test
    void isWorkdayFiveTwo() {
        service.createRule(user.getId(), ruleDto(UserScheduleType.FIVE_TWO, today(), null), company);
        checkWorkDay(today(), true);
        checkWorkDay(today().plusDays(1), true);
        checkWorkDay(today().plusDays(2), true);
        checkWorkDay(today().plusDays(3), true);
        checkWorkDay(today().plusDays(4), true);
        checkWorkDay(today().plusDays(5), false);
        checkWorkDay(today().plusDays(6), false);
        checkWorkDay(today().plusDays(7), true);
    }


    // get

    @Test
    void isWorkdaySixOne() {
        service.createRule(user.getId(), ruleDto(UserScheduleType.SIX_ONE, today(), null), company);
        checkWorkDay(today(), true);
        checkWorkDay(today().plusDays(1), true);
        checkWorkDay(today().plusDays(2), true);
        checkWorkDay(today().plusDays(3), true);
        checkWorkDay(today().plusDays(4), true);
        checkWorkDay(today().plusDays(5), true);
        checkWorkDay(today().plusDays(6), false);
        checkWorkDay(today().plusDays(7), true);
    }

    @Test
    void isWorkdayOverrideSkip() {
        service.createRule(user.getId(), ruleDto(UserScheduleType.SIX_ONE, today(), null), company);
        service.createRule(user.getId(), ruleDto(UserScheduleType.OVERRIDE_SKIP, tomorrow(), tomorrow()), company);
        checkWorkDay(today(), true);
        checkWorkDay(today().plusDays(1), false);
        checkWorkDay(today().plusDays(2), true);
        checkWorkDay(today().plusDays(3), true);
        checkWorkDay(today().plusDays(4), true);
        checkWorkDay(today().plusDays(5), true);
        checkWorkDay(today().plusDays(6), false);
        checkWorkDay(today().plusDays(7), true);
    }

    @Test
    void isWorkdayOverrideWork() {
        service.createRule(user.getId(), ruleDto(UserScheduleType.TWO_TWO, today(), null), company);
        service.createRule(user.getId(),
                ruleDto(UserScheduleType.OVERRIDE_WORK, tomorrow().plusDays(1), tomorrow().plusDays(1)), company);
        checkWorkDay(today(), true);
        checkWorkDay(today().plusDays(1), true);
        checkWorkDay(today().plusDays(2), true);
        checkWorkDay(today().plusDays(3), false);
        checkWorkDay(today().plusDays(4), true);
        checkWorkDay(today().plusDays(5), true);
        checkWorkDay(today().plusDays(6), false);
        checkWorkDay(today().plusDays(7), false);
    }

    @Test
    void isWorkdayTwoTwoApplyFromYesterday() {
        UserScheduleRuleDto ruleDto = ruleDto(UserScheduleType.TWO_TWO, today(), null);
        ruleDto.setApplyFrom(yesterday());
        service.createRule(user.getId(), ruleDto, company);
        checkWorkDay(today(), true);
        checkWorkDay(today().plusDays(1), false);
        checkWorkDay(today().plusDays(2), false);
        checkWorkDay(today().plusDays(3), true);
        checkWorkDay(today().plusDays(4), true);
        checkWorkDay(today().plusDays(5), false);
        checkWorkDay(today().plusDays(6), false);
        checkWorkDay(today().plusDays(7), true);
    }

    @Test
    void isWorkdayFiveTwoApplyFromYesterdayShort() {
        UserScheduleRuleDto ruleDto = ruleDto(UserScheduleType.FIVE_TWO, 6);
        ruleDto.setApplyFrom(yesterday());
        service.createRule(user.getId(), ruleDto, company);
        checkWorkDay(today(), true);
        checkWorkDay(today().plusDays(1), true);
        checkWorkDay(today().plusDays(2), true);
        checkWorkDay(today().plusDays(3), true);
        checkWorkDay(today().plusDays(4), false);
        checkWorkDay(today().plusDays(5), false);
        checkWorkDay(today().plusDays(6), false);
        checkWorkDay(today().plusDays(7), false);
    }

    @Test
    void isWorkdayFiveTwoStartFromTuesday() {
        UserScheduleRuleDto ruleDto = ruleDto(UserScheduleType.FIVE_TWO, tomorrow(), null);
        ruleDto.setApplyFrom(today());
        service.createRule(user.getId(), ruleDto, company);
        checkWorkDay(today(), false);
        checkWorkDay(today().plusDays(1), true);
        checkWorkDay(today().plusDays(2), true);
        checkWorkDay(today().plusDays(3), true);
        checkWorkDay(today().plusDays(4), true);
        checkWorkDay(today().plusDays(5), false);
        checkWorkDay(today().plusDays(6), false);
        checkWorkDay(today().plusDays(7), true);
    }

    @Test
    void duplicateScheduleTest() {
        var ruleDto1 = ruleDto(
                UserScheduleType.ALWAYS_WORKS, LocalDate.of(2021, 4, 27), null
        );
        var ruleDto2 = ruleDto(
                UserScheduleType.ALWAYS_WORKS, LocalDate.of(2021, 4, 28), null
        );
        var rule1 = service.createRule(user.getId(), ruleDto1, company);
        service.createRule(user.getId(), ruleDto2, company);

        assertThatThrownBy(() -> service.changeRule(user.getId(), rule1.getId(), ruleDto1, company))
                .isInstanceOf(TplInvalidActionException.class)
                .hasMessageContaining("conflict");
    }

    @Test
    void getScheduleRulesEmpty() {
        assertThat(service.findScheduleRules(emptyGetRequest(), pageable())).isEmpty();
    }

    @Test
    void getAllScheduleRules() {
        var type = UserScheduleType.FIVE_TWO;
        UserScheduleRuleDto ruleDto = service.createRule(user.getId(), ruleDto(type), company);

        Map<Long, UserScheduleDto> searchResult = service.findSchedules(new UserSchedulesSearchRequest(
                Set.of(), Set.of(user.getId()), Set.of(), ruleDto.getActiveFrom(), ruleDto.getActiveTo(), Set.of(),
                Set.of(), Set.of(), null
        ), company).getUsers();

        assertThat(searchResult).hasSize(1)
                .containsKey(user.getId())
                .containsValue(new UserScheduleDto(
                        Map.of(ruleDto.getActiveFrom(), ruleDto.getId()),
                        Map.of(ruleDto.getId().toString(), ruleDto),
                        user.getUid(),
                        user.getName(),
                        new PartnerTransportTypeDto(
                                0L,
                                "Стандартный грузовой автомобиль",
                                10.0,
                                Set.of(),
                                RoutingVehicleType.COMMON
                        )
                ));
    }

    @Test
    void getAllScheduleRulesViewMode_slotsDisabled() {
        var type = UserScheduleType.FIVE_TWO;
        UserScheduleRuleDto ruleDto = service.createRule(user.getId(), ruleDto(type), company);
        var sortingCenterId = ruleDto.getSortingCenterId();
        var sortingCenter = sortingCenterRepository.findByIdOrThrow(sortingCenterId);
        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                sortingCenter, SortingCenterProperties.ONE_DAY_SLOTS_SCHEDULE_ENABLED, false);

        var viewMode = service.findSchedules(new UserSchedulesSearchRequest(
                Set.of(), Set.of(user.getId()), Set.of(sortingCenterId), ruleDto.getActiveFrom(),
                ruleDto.getActiveFrom(), Set.of(), Set.of(), Set.of(), null
        ), company).getViewMode();

        assertThat(viewMode.getMode()).isEqualTo(ONE_DAY);
    }

    @Test
    void getAllScheduleRulesViewMode_slotsEnabledSuperCompany() {
        var type = UserScheduleType.FIVE_TWO;
        UserScheduleRuleDto ruleDto = service.createRule(user.getId(), ruleDto(type), company);
        var sortingCenterId = ruleDto.getSortingCenterId();
        var sortingCenter = sortingCenterRepository.findByIdOrThrow(sortingCenterId);
        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                sortingCenter, SortingCenterProperties.ONE_DAY_SLOTS_SCHEDULE_ENABLED, true);

        var viewMode = service.findSchedules(new UserSchedulesSearchRequest(
                Set.of(), Set.of(user.getId()), Set.of(sortingCenterId), ruleDto.getActiveFrom(),
                ruleDto.getActiveFrom(), Set.of(), Set.of(), Set.of(), null
        ), company).getViewMode();

        assertThat(viewMode.getMode()).isEqualTo(ONE_DAY_SLOTS);
    }

    @Test
    void getAllScheduleRulesViewMode_slotsEnabledNotSuperCompany() {
        boolean company = false;
        var type = UserScheduleType.FIVE_TWO;
        UserScheduleRuleDto ruleDto = service.createRule(user.getId(), ruleDto(type), company);
        var sortingCenterId = ruleDto.getSortingCenterId();
        var sortingCenter = sortingCenterRepository.findByIdOrThrow(sortingCenterId);
        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                sortingCenter, SortingCenterProperties.ONE_DAY_SLOTS_SCHEDULE_ENABLED, true);

        var viewMode = service.findSchedules(new UserSchedulesSearchRequest(
                Set.of(), Set.of(user.getId()), Set.of(sortingCenterId), ruleDto.getActiveFrom(),
                ruleDto.getActiveFrom(), Set.of(), Set.of(), Set.of(), null
        ), company).getViewMode();

        assertThat(viewMode.getMode()).isEqualTo(ONE_DAY);
    }

    @Test
    void getAllScheduleRulesViewMode_oneWeek() {
        var type = UserScheduleType.FIVE_TWO;
        service.createRule(user.getId(), ruleDto(type), company);

        var now = LocalDate.now(clock);
        var viewMode = service.findSchedules(new UserSchedulesSearchRequest(
                Set.of(), Set.of(user.getId()), Set.of(), now.with(DayOfWeek.MONDAY),
                now.with(DayOfWeek.SUNDAY), Set.of(), Set.of(), Set.of(), null
        ), company).getViewMode();

        assertThat(viewMode.getMode()).isEqualTo(ONE_WEEK);

        viewMode = service.findSchedules(new UserSchedulesSearchRequest(
                Set.of(), Set.of(user.getId()), Set.of(), now.plusMonths(1L).with(DayOfWeek.MONDAY),
                now.plusMonths(1L).with(DayOfWeek.SUNDAY), Set.of(), Set.of(), Set.of(), null
        ), company).getViewMode();

        assertThat(viewMode.getMode()).isEqualTo(ONE_WEEK);
    }

    @Test
    void getAllScheduleRulesViewMode_oneMonth() {
        var type = UserScheduleType.FIVE_TWO;
        service.createRule(user.getId(), ruleDto(type), company);

        var now = LocalDate.now(clock);
        var viewMode = service.findSchedules(new UserSchedulesSearchRequest(
                Set.of(), Set.of(user.getId()), Set.of(), now.with(TemporalAdjusters.firstDayOfMonth()),
                now.with(TemporalAdjusters.lastDayOfMonth()), Set.of(), Set.of(), Set.of(), null
        ), company).getViewMode();

        assertThat(viewMode.getMode()).isEqualTo(ONE_MONTH);

        viewMode = service.findSchedules(new UserSchedulesSearchRequest(
                Set.of(), Set.of(user.getId()), Set.of(),
                now.plusMonths(1L).with(TemporalAdjusters.firstDayOfMonth()),
                now.plusMonths(1L).with(TemporalAdjusters.lastDayOfMonth()),
                Set.of(), Set.of(), Set.of(), null
        ), company).getViewMode();

        assertThat(viewMode.getMode()).isEqualTo(ONE_MONTH);
    }

    @Test
    void getScheduleRulesFilledRequest() {
        var type = UserScheduleType.FIVE_TWO;
        UserScheduleRuleDto ruleDto = service.createRule(user.getId(), ruleDto(type), company);
        assertThat(service.findScheduleRules(filledGetRequest(type), pageable()).getContent())
                .isEqualTo(List.of(ruleDto));
    }

    @Test
    void getScheduleRulesBounds() {
        var dayA = today();
        var dayB = dayA.plusDays(1L);
        var dayC = dayA.plusDays(2L);
        var dayD = dayA.plusDays(3L);
        var dayE = dayA.plusDays(4L);

        var a = service.createRule(user.getId(), ruleDto(UserScheduleType.FIVE_TWO, dayA, dayA), company);
        var b = service.createRule(user.getId(), ruleDto(UserScheduleType.FIVE_TWO, dayB, dayB), company);
        var c = service.createRule(user.getId(), ruleDto(UserScheduleType.FIVE_TWO, dayC, dayC), company);
        var d = service.createRule(user.getId(), ruleDto(UserScheduleType.FIVE_TWO, dayD, dayD), company);
        var e = service.createRule(user.getId(), ruleDto(UserScheduleType.FIVE_TWO, dayE, null), company);

        assertThat(service.findScheduleRules(getRequest(dayA, dayA), pageable()).getContent())
                .isEqualTo(List.of(a));
        assertThat(service.findScheduleRules(getRequest(null, null), pageable()).getContent())
                .isEqualTo(List.of(a, b, c, d, e));
        assertThat(service.findScheduleRules(getRequest(dayB, null), pageable()).getContent())
                .isEqualTo(List.of(b, c, d, e));
        assertThat(service.findScheduleRules(getRequest(null, dayC), pageable()).getContent())
                .isEqualTo(List.of(a, b, c));
        assertThat(service.findScheduleRules(getRequest(dayB, dayC), pageable()).getContent())
                .isEqualTo(List.of(b, c));
        assertThat(service.findScheduleRules(getRequest(dayA, dayD), pageable()).getContent())
                .isEqualTo(List.of(a, b, c, d));
        assertThat(service.findScheduleRules(getRequest(dayE, null), pageable()).getContent())
                .isEqualTo(List.of(e));
    }

    @Test
    void getScheduleRulesByMetaType() {
        UserScheduleRuleDto expected = service.createRule(user.getId(), ruleDto(UserScheduleType.FIVE_TWO), company);
        service.createRule(user.getId(), ruleDto(UserScheduleType.OVERRIDE_SKIP), company);
        var request = new UserScheduleRulesSearchRequest(
                user.getId(), null, UserScheduleMetaType.BASIC, null, null, null
        );
        assertThat(service.findScheduleRules(request, pageable()).getContent())
                .isEqualTo(List.of(expected));
    }

    @Test
    void getScheduleRulesByMetaTypeMultiple() {
        UserScheduleRuleDto expected1 = service.createRule(user.getId(),
                ruleDto(UserScheduleType.OVERRIDE_SKIP,
                        today(), today()), company);
        UserScheduleRuleDto expected2 = service.createRule(user.getId(),
                ruleDto(UserScheduleType.OVERRIDE_WORK,
                        tomorrow(), tomorrow()), company);
        service.createRule(user.getId(), ruleDto(UserScheduleType.SIX_ONE, today(), null), company);
        var request = new UserScheduleRulesSearchRequest(
                user.getId(), null, UserScheduleMetaType.OVERRIDE, null, null, null
        );
        assertThat(service.findScheduleRules(request, pageable()).getContent())
                .isEqualTo(List.of(expected1, expected2));
    }

    @Test
    void getScheduleRulesFilledRequestEmptyActiveTo() {
        var type = UserScheduleType.FIVE_TWO;
        UserScheduleRuleDto ruleDto = service.createRule(user.getId(), ruleDto(type, today(), null), company);
        assertThat(service.findScheduleRules(filledGetRequest(type), pageable()).getContent())
                .isEqualTo(List.of(ruleDto));
    }


    // create

    @Test
    void getScheduleRulesEmptyRequest() {
        UserScheduleRuleDto ruleDto = service.createRule(user.getId(), ruleDto(UserScheduleType.TWO_TWO), company);
        assertThat(service.findScheduleRules(emptyGetRequest(), pageable()).getContent())
                .isEqualTo(List.of(ruleDto));
    }

    @Test
    void getScheduleRulesEmptyRequestEmptyActiveTo() {
        UserScheduleRuleDto ruleDto = service.createRule(user.getId(), ruleDto(UserScheduleType.TWO_TWO,
                today(), null), company);
        assertThat(service.findScheduleRules(emptyGetRequest(), pageable()).getContent())
                .isEqualTo(List.of(ruleDto));
    }

    @Test
    void getUserSchedule() {
        UserScheduleRuleDto ruleDto = service.createRule(user.getId(),
                ruleDto(UserScheduleType.TWO_TWO, today(), null),
                company);
        LocalDate from = today().minusDays(1);
        LocalDate to = today().plusDays(100);
        UserScheduleDto scheduleDto = service.getUserScheduleDto(user.getId(), from, to);

        checkScheduleRules(scheduleDto, List.of(ruleDto));
        checkScheduleDate(scheduleDto, from, null);
        AtomicInteger dayNum = new AtomicInteger(0);
        from.datesUntil(to).skip(1).forEach(date -> {
            int t = dayNum.getAndIncrement() % 4;
            checkScheduleDate(scheduleDto, date, t == 0 || t == 1 ? ruleDto : null);
        });
    }

    @Test
    void getUserScheduleTwoBasicRules() {
        UserScheduleRuleDto first = service.createRule(
                user.getId(), ruleDto(UserScheduleType.TWO_TWO, today(), today()), company);
        UserScheduleRuleDto second = service.createRule(
                user.getId(), ruleDto(UserScheduleType.SIX_ONE, tomorrow(), tomorrow()), company);
        UserScheduleDto scheduleDto = service.getUserScheduleDto(user.getId(), today(), tomorrow());
        checkScheduleRules(scheduleDto, List.of(first, second));
        checkScheduleDate(scheduleDto, today(), first);
        checkScheduleDate(scheduleDto, tomorrow(), second);
    }

    @Test
    void getUserScheduleWithOverride() {
        int duration = 3;
        UserScheduleRuleDto baseRuleDto = service.createRule(user.getId(), ruleDto(UserScheduleType.SIX_ONE,
                duration), company);
        LocalDate from = baseRuleDto.getActiveFrom();
        LocalDate to = from.plusDays(duration);

        UserScheduleRuleDto override1Dto = service.createRule(
                user.getId(), ruleDto(UserScheduleType.OVERRIDE_WORK, from, from), company);
        service.createRule(user.getId(), ruleDto(UserScheduleType.OVERRIDE_SKIP,
                from.plusDays(2), from.plusDays(2)), company);

        UserScheduleDto scheduleDto = service.getUserScheduleDto(user.getId(), from, to);

        checkScheduleRules(scheduleDto, List.of(baseRuleDto, override1Dto)); // no override_skip
        checkScheduleDate(scheduleDto, from, override1Dto);
        checkScheduleDate(scheduleDto, from.plusDays(1), baseRuleDto);
        checkScheduleDate(scheduleDto, from.plusDays(2), null);
    }

    @Test
    void createRule() {
        UserScheduleRuleDto expected = ruleDto(UserScheduleType.TWO_TWO);
        assertThat(service.createRule(user.getId(), expected, company))
                .isEqualToIgnoringGivenFields(expected, "id", "loadInterval");
    }

    @Test
    void createBasicRuleOpenActiveTo() {
        UserScheduleRuleDto expected = ruleDto(UserScheduleType.TWO_TWO, today(), null);
        assertThat(service.createRule(user.getId(), expected, company))
                .isEqualToIgnoringGivenFields(expected, "id", "loadInterval");
    }

    @ParameterizedTest
    @MethodSource("notValidRuleRequests")
    void createNotValidRuleExpired(UserScheduleRuleDto notValid) {
        assertThatThrownBy(() -> service.createRule(user.getId(), notValid, company))
                .isInstanceOf(TplInvalidActionException.class)
                .hasMessageContaining("valid");
    }

    @Test
    void createRuleApplyBeforeStart() {
        UserScheduleRuleDto expected = ruleDtoApplyFrom(UserScheduleType.TWO_TWO, today(), today(), yesterday());
        assertThat(service.createRule(user.getId(), expected, company))
                .isEqualToIgnoringGivenFields(expected, "id", "loadInterval");
    }

    @Test
    void createOverrideWorkRuleNoBasic() {
        UserScheduleRuleDto expected = ruleDto(UserScheduleType.OVERRIDE_WORK);
        assertThat(service.createRule(user.getId(), expected, company))
                .isEqualToIgnoringGivenFields(expected, "id", "loadInterval");
    }

    @Test
    void createOverrideSkipRuleNoBasic() {
        UserScheduleRuleDto expected = ruleDto(UserScheduleType.OVERRIDE_SKIP);
        assertThat(service.createRule(user.getId(), expected, company))
                .isEqualToIgnoringGivenFields(expected, "id");
    }

    @Test
    void createOverrideRule() {
        UserScheduleRuleDto expected = ruleDto(UserScheduleType.OVERRIDE_SKIP);
        assertThat(service.createRule(user.getId(), expected, company))
                .isEqualToIgnoringGivenFields(expected, "id");
    }

    @Test
    void createTwoOverrideRulesNoIntersection() {
        UserScheduleRuleDto first = ruleDto(UserScheduleType.OVERRIDE_SKIP, today(), today());
        UserScheduleRuleDto expected = ruleDto(UserScheduleType.OVERRIDE_WORK, tomorrow(), tomorrow());
        service.createRule(user.getId(), first, company);
        assertThat(service.createRule(user.getId(), expected, company))
                .isEqualToIgnoringGivenFields(expected, "id", "loadInterval");
    }

    @Test
    void createTwoBasicRulesNoIntersection() {
        service.createRule(user.getId(), ruleDto(UserScheduleType.SIX_ONE, today(), today()), company);
        UserScheduleRuleDto expected = ruleDto(UserScheduleType.FIVE_TWO, tomorrow(), tomorrow().plusDays(5));
        assertThat(service.createRule(user.getId(), expected, company))
                .isEqualToIgnoringGivenFields(expected, "id", "loadInterval");
    }

    @Test
    void createRuleSplitBasicBasic() {
        service.createRule(user.getId(), ruleDto(UserScheduleType.SIX_ONE, today(), tomorrow()), company);
        assertThat(service.createRule(user.getId(),
                ruleDto(UserScheduleType.SIX_ONE, tomorrow(), tomorrow().plusDays(1)), company)).isNotNull();
    }

    @Test
    void createRuleConflictBasicBasic() {
        service.createRule(user.getId(), ruleDto(UserScheduleType.SIX_ONE, today(), tomorrow()), company);
        assertThatThrownBy(() -> service.createRule(user.getId(),
                ruleDto(UserScheduleType.SIX_ONE, today(), null), company))
                .isInstanceOf(TplInvalidActionException.class)
                .hasMessageContaining("conflict");
    }

    @Test
    void createRuleBasicOverActiveBasic() {
        var yesterday = yesterday();
        var today = today();

        ClockUtil.initFixed(clock, yesterday.atStartOfDay());
        var old = service.createRule(user.getId(), ruleDto(UserScheduleType.SIX_ONE, yesterday, null), company);

        ClockUtil.initFixed(clock, today.atStartOfDay());
        UserScheduleRuleDto expected = ruleDto(UserScheduleType.FIVE_TWO, today, null);
        assertThat(service.createRule(user.getId(), expected, company))
                .isEqualToIgnoringGivenFields(expected, "id", "loadInterval");

        var expectedOld = old.toBuilder().activeTo(yesterday).build();
        assertThat(service.findScheduleRules(new UserScheduleRulesSearchRequest(
                user.getId(), null, null, yesterday, yesterday, null
        ), pageable()).getContent()).usingElementComparatorIgnoringFields("id", "loadInterval")
                .isEqualTo(List.of(expectedOld));
    }

    @Test
    void createRuleWithLoadingStartInterval() {
        var ruleDto = ruleDto(UserScheduleType.TWO_TWO);

        var createdRule = service.createRule(user.getId(), ruleDto, company);

        assertThat(createdRule.getLoadInterval()).isNotNull();
        assertThat(createdRule.getLoadInterval().getStart()).isEqualTo(LocalTime.of(9, 30));
    }


    // change

    @Test
    void createRulesMultipleSplit() {
        var dayA = today();
        var dayB = dayA.plusDays(1L);
        var dayC = dayA.plusDays(2L);
        var dayD = dayA.plusDays(3L);

        ClockUtil.initFixed(clock, dayA.atStartOfDay());
        var a = service.createRule(user.getId(), ruleDto(UserScheduleType.FIVE_TWO, dayA, null), company);
        ClockUtil.initFixed(clock, dayB.atStartOfDay());
        var b = service.createRule(user.getId(), ruleDto(UserScheduleType.FIVE_TWO, dayB, null), company);
        var c = service.createRule(user.getId(), ruleDto(UserScheduleType.FIVE_TWO, dayC, null), company);
        var d = service.createRule(user.getId(), ruleDto(UserScheduleType.FIVE_TWO, dayD, null), company);

        assertThat(service.findScheduleRules(emptyGetRequest(), pageable()).getContent())
                .isEqualTo(List.of(
                        a.toBuilder().activeTo(dayA).build(),
                        b.toBuilder().activeTo(dayB).build(),
                        c.toBuilder().activeTo(dayC).build(),
                        d
                ));
    }

    @Test
    void createRuleConflictBasicBasicOpenActiveFrom() {
        service.createRule(user.getId(), ruleDto(UserScheduleType.SIX_ONE, tomorrow(), null), company);
        assertThatThrownBy(() -> service.createRule(user.getId(),
                ruleDto(UserScheduleType.SIX_ONE, today(), tomorrow().plusDays(1)), company))
                .isInstanceOf(TplInvalidActionException.class)
                .hasMessageContaining("conflict");
    }

    @Test
    void createRuleOverrideBasic() {
        service.createRule(user.getId(), ruleDto(UserScheduleType.OVERRIDE_SKIP, today(), today()), company);
        UserScheduleRuleDto expected = ruleDto(UserScheduleType.TWO_TWO, today(), today());
        assertThat(service.createRule(user.getId(), expected, company))
                .isEqualToIgnoringGivenFields(expected, "id", "loadInterval");
    }

    @Test
    void createRuleConflictOverrideOverride() {
        service.createRule(user.getId(), ruleDto(UserScheduleType.OVERRIDE_SKIP, today(), today()), company);
        UserScheduleRuleDto notValid = ruleDto(UserScheduleType.OVERRIDE_WORK, today(), today());
        assertThatThrownBy(() -> service.createRule(user.getId(), notValid, company))
                .isInstanceOf(TplInvalidActionException.class)
                .hasMessageContaining("conflict");
    }

    @Test
    void changeFutureRule() {
        UserScheduleRuleDto rule = service.createRule(user.getId(),
                ruleDtoApplyFrom(UserScheduleType.TWO_TWO, today(), today(), yesterday()),
                company);
        SortingCenter newSortingCenter = userHelper.sortingCenter(2L);

        UserScheduleRuleDto newRule = ruleDtoApplyFrom(UserScheduleType.FIVE_TWO, today(), tomorrow(), yesterday())
                .toBuilder()
                .sortingCenterId(newSortingCenter.getId())
                .vehicleType(CourierVehicleType.NONE)
                .build();

        assertThat(service.changeRule(user.getId(), rule.getId(), newRule, company))
                .isEqualToIgnoringGivenFields(newRule, "id", "loadInterval");
    }

    @Test
    void changeRuleLoadingIntervalIsPresent() {
        var rule = service.createRule(user.getId(),
                ruleDtoApplyFrom(UserScheduleType.TWO_TWO, today(), today(), yesterday()),
                company);
        var updateRuleDto = rule.toBuilder()
                .shiftStart(LocalTime.of(12, 0))
                .build();

        var updatedRule = service.changeRule(user.getId(), rule.getId(), updateRuleDto, company);

        assertThat(updatedRule.getLoadInterval()).isNotNull();
        assertThat(updatedRule.getLoadInterval().getStart()).isEqualTo(LocalTime.of(11, 30));
    }

    @Test
    void changeNotExistingRule() {
        assertThatThrownBy(() -> service.changeRule(user.getId(), 1L, ruleDto(UserScheduleType.TWO_TWO), company))
                .isInstanceOf(TplEntityNotFoundException.class);
    }

    @ParameterizedTest
    @MethodSource("notValidRuleRequests")
    void changeNotValidRequest(UserScheduleRuleDto notValid) {
        UserScheduleRuleDto rule = service.createRule(user.getId(), ruleDto(UserScheduleType.TWO_TWO), company);
        if (notValid.getApplyFrom().isBefore(LocalDate.now(clock))) {
            // can change expired
            assertThat(service.changeRule(user.getId(), rule.getId(), notValid, company)).isNotNull();
        } else {
            assertThatThrownBy(() -> service.changeRule(user.getId(), rule.getId(), notValid, company))
                    .isInstanceOf(TplInvalidActionException.class)
                    .hasMessageContaining("valid");
        }
    }

    @Test
    void changeNotValidBasicToOverride() {
        UserScheduleRuleDto rule = service.createRule(user.getId(), ruleDto(UserScheduleType.TWO_TWO), company);
        assertThatThrownBy(() ->
                service.changeRule(user.getId(), rule.getId(), ruleDto(UserScheduleType.OVERRIDE_SKIP), company))
                .isInstanceOf(TplInvalidActionException.class)
                .hasMessageContaining("valid");
    }

    @Test
    void changeNotValidOverrideToBasic() {
        UserScheduleRuleDto rule = service.createRule(user.getId(), ruleDto(UserScheduleType.OVERRIDE_WORK), company);
        assertThatThrownBy(() ->
                service.changeRule(user.getId(), rule.getId(), ruleDto(UserScheduleType.SIX_ONE), company))
                .isInstanceOf(TplInvalidActionException.class)
                .hasMessageContaining("valid");
    }

    @Test
    void changeOldRule() {
        var now = ClockUtil.defaultDateTime();
        ClockUtil.initFixed(clock, now);
        UserScheduleRuleDto rule = service.createRule(
                user.getId(), ruleDto(UserScheduleType.TWO_TWO, today(), today()), company);
        ClockUtil.initFixed(clock, now.plusDays(1L));
        assertThatThrownBy(() ->
                service.changeRule(user.getId(), rule.getId(), ruleDto(UserScheduleType.SIX_ONE), company))
                .isInstanceOf(TplInvalidActionException.class)
                .hasMessageContaining("old");
    }


    // delete

    @ParameterizedTest
    @MethodSource("activeToChanges")
    void changeActiveToInActiveRuleToFuture(LocalDate newActiveTo) {
        var defMinus1 = ClockUtil.defaultDateTime().minusDays(1);
        var def = defMinus1.plusDays(1);
        ClockUtil.initFixed(clock, defMinus1);
        LocalDate ruleActiveDate = defMinus1.toLocalDate();
        UserScheduleRuleDto rule = service.createRule(user.getId(),
                ruleDtoApplyFrom(UserScheduleType.TWO_TWO, ruleActiveDate, null, ruleActiveDate), company);
        ClockUtil.initFixed(clock, def);

        var expected = rule.toBuilder().activeTo(newActiveTo).build();
        assertThat(service.changeRule(user.getId(), rule.getId(), expected, company))
                .isEqualToIgnoringGivenFields(expected, "id");
        assertThat(service.findScheduleRules(emptyGetRequest(), pageable()).getContent())
                .usingElementComparatorIgnoringFields("id")
                .isEqualTo(List.of(expected));
    }

    @ParameterizedTest
    @MethodSource("activeFromChanges")
    void changeActiveRuleManyFields(LocalDate activeFrom) {
        var defMinus1 = ClockUtil.defaultDateTime().minusDays(1);
        var def = defMinus1.plusDays(1);
        ClockUtil.initFixed(clock, defMinus1);
        LocalDate ruleActiveFrom = defMinus1.toLocalDate();
        UserScheduleRuleDto rule = service.createRule(user.getId(),
                ruleDtoApplyFrom(UserScheduleType.TWO_TWO, ruleActiveFrom, null, ruleActiveFrom), company);
        ClockUtil.initFixed(clock, def);

        SortingCenter newSortingCenter = userHelper.sortingCenter(2L);

        var newRule = new UserScheduleRuleDto(
                null,
                UserScheduleType.FIVE_TWO,
                activeFrom, activeFrom.plusDays(3L), activeFrom,
                LocalTime.of(11, 0), LocalTime.of(12, 50),
                CourierVehicleType.NONE,
                newSortingCenter.getId(),
                UserScheduleType.FIVE_TWO.getMaskWorkDays(),
                null
        );

        LocalDate expectedNewActiveFrom = DateTimeUtil.max(activeFrom, def.toLocalDate());
        var expectedOld = rule.toBuilder().activeTo(expectedNewActiveFrom.minusDays(1L)).build();
        assertThat(service.changeRule(user.getId(), rule.getId(), newRule, company))
                .isEqualToIgnoringGivenFields(expectedOld, "id");

        var expectedNew = newRule.toBuilder()
                .activeFrom(expectedNewActiveFrom)
                .build();
        assertThat(service.findScheduleRules(new UserScheduleRulesSearchRequest(
                user.getId(), UserScheduleType.FIVE_TWO, UserScheduleType.FIVE_TWO.getMetaType(),
                null, null, CourierVehicleType.NONE
        ), pageable()).getContent()).usingElementComparatorIgnoringFields("id", "loadInterval")
                .isEqualTo(List.of(expectedNew));
    }

    @Test
    void deleteBasicRule() {
        UserScheduleRuleDto ruleDto = service.createRule(
                user.getId(), ruleDto(UserScheduleType.SIX_ONE, today(), null), company);
        service.deleteRule(user.getId(), ruleDto.getId(), company);
        assertThat(service.findScheduleRules(emptyGetRequest(), pageable())).isEmpty();
    }

    @Test
    void deleteOverrideRule() {
        UserScheduleRuleDto ruleDto = service.createRule(
                user.getId(), ruleDto(UserScheduleType.OVERRIDE_WORK, today(), today()), company);
        service.deleteRule(user.getId(), ruleDto.getId(), company);
        assertThat(service.findScheduleRules(emptyGetRequest(), pageable())).isEmpty();
    }

    @Test
    void deleteNotExistingRule() {
        assertThatThrownBy(() -> service.deleteRule(user.getId(), 1, company))
                .isInstanceOf(TplEntityNotFoundException.class);
    }

    @Test
    void deleteOldRule() {
        LocalDateTime now = ClockUtil.defaultDateTime();
        ClockUtil.initFixed(clock, now);
        UserScheduleRuleDto ruleDto = service.createRule(
                user.getId(), ruleDto(UserScheduleType.SIX_ONE, today(), today()), company);
        ClockUtil.initFixed(clock, now.plusDays(1L));
        assertThatThrownBy(() -> service.deleteRule(user.getId(), ruleDto.getId(), company))
                .isInstanceOf(TplInvalidActionException.class)
                .hasMessageContaining("удалить");
    }

    @Test
    void deleteActiveRule() {
        LocalDateTime now = ClockUtil.defaultDateTime();
        ClockUtil.initFixed(clock, now);
        UserScheduleRuleDto ruleDto = service.createRule(
                user.getId(), ruleDto(UserScheduleType.SIX_ONE, today(), tomorrow().plusDays(1)), company);
        ClockUtil.initFixed(clock, now.plusDays(1L));
        assertThatThrownBy(() -> service.deleteRule(user.getId(), ruleDto.getId(), company))
                .isInstanceOf(TplInvalidActionException.class)
                .hasMessageContaining("удалить");
    }

    // override

    @Test
    void deleteBasicRuleOverrideStays() {
        UserScheduleRuleDto ruleDto = service.createRule(
                user.getId(), ruleDto(UserScheduleType.SIX_ONE, today(), null), company);
        UserScheduleRuleDto expected = service.createRule(
                user.getId(), ruleDto(UserScheduleType.OVERRIDE_SKIP, tomorrow(), tomorrow()), company);
        service.deleteRule(user.getId(), ruleDto.getId(), company);
        assertThat(service.findScheduleRules(emptyGetRequest(), pageable()).getContent())
                .isEqualTo(List.of(expected));
    }

    @Test
    void fire() {
        LocalDateTime now = ClockUtil.defaultDateTime();
        ClockUtil.initFixed(clock, now);
        service.createRule(user.getId(), ruleDto(UserScheduleType.SIX_ONE, today(), null), company);
        // old rule
        service.createRule(user.getId(), ruleDto(UserScheduleType.OVERRIDE_WORK, today(), today()), company);
        // future rules
        ClockUtil.initFixed(clock, now.plusDays(1L));
        service.createRule(user.getId(), ruleDto(UserScheduleType.OVERRIDE_WORK, tomorrow(), tomorrow()), company);
        service.createRule(user.getId(), ruleDto(UserScheduleType.OVERRIDE_SKIP, today().plusDays(2),
                today().plusDays(2)), company);

        service.fireUser(user.getId(), company);

        List<UserScheduleRuleDto> rules = service.findScheduleRules(emptyGetRequest(), pageable()).getContent();
        assertThat(rules)
                .describedAs("Правила в будущем удалали")
                .hasSize(2);
        assertThat(rules.stream()
                .filter(r -> r.getScheduleMetaType() == UserScheduleMetaType.BASIC).findFirst().orElseThrow())
                .describedAs("Открытое правило закрыли")
                .extracting(UserScheduleRuleDto::getActiveTo).isEqualTo(today());
        assertThat(rules.stream()
                .filter(r -> r.getScheduleMetaType() != UserScheduleMetaType.BASIC).findFirst().orElseThrow())
                .describedAs("Старое правило не изменилось")
                .extracting(UserScheduleRuleDto::getActiveTo).isEqualTo(yesterday());
    }

    @Test
    void scheduleOverrideWork() {
        service.createRule(user.getId(), ruleDto(UserScheduleType.TWO_TWO, today(), null), company);
        service.scheduleOverride(user.getUid(), today().plusDays(2), true, company);
        checkWorkDay(today().plusDays(2), true);
    }

    @Test
    void scheduleOverrideWorkWithoutBasicSchedule() {
        assertThatThrownBy(() -> service.scheduleOverride(user.getUid(), today(), true, company))
                .isInstanceOf(TplInvalidActionException.class)
                .hasMessageContaining("basic");
    }

    @Test
    void scheduleOverrideWorkNotNeeded() {
        service.createRule(user.getId(), ruleDto(UserScheduleType.SIX_ONE, today(), null), company);
        service.scheduleOverride(user.getUid(), today(), true, company);
        assertThat(service.findScheduleRules(emptyGetRequest(), pageable()).getContent()).hasSize(1);
    }

    // util

    @Test
    void scheduleOverrideSkip() {
        service.createRule(user.getId(), ruleDto(UserScheduleType.SIX_ONE, today(), null), company);
        service.scheduleOverride(user.getUid(), today(), false, company);
        checkWorkDay(today(), false);
    }

    @Test
    void scheduleOverrideNotValidCustomBasic() {
        assertThatThrownBy(() -> service.scheduleOverride(user.getUid(), today(), ruleDto(UserScheduleType.TWO_TWO),
                company))
                .isInstanceOf(TplInvalidActionException.class)
                .hasMessageContaining("override");
    }

    @Test
    void customUserSchedule() {
        service.createRule(
                user.getId(),
                UserScheduleTestHelper.ruleDtoApplyFrom(
                        UserScheduleType.CUSTOM_USER,
                        today().plusDays(3), //среда
                        today().plusDays(15),
                        today(),
                        new boolean[]{true, false, true, false, true, false, false}, //работаем понедельник, среда,
                        // пятница
                        SortingCenter.DEFAULT_SC_ID,
                        UserScheduleTestHelper.createScheduleData(CourierVehicleType.CAR)),
                company);

        checkWorkDay(today(), false);
        checkWorkDay(today().plusDays(1), false); // вторник
        checkWorkDay(today().plusDays(2), false); // среда
        checkWorkDay(today().plusDays(3), false); // четверг
        checkWorkDay(today().plusDays(4), true);  // пятница
        checkWorkDay(today().plusDays(5), false); // суббота
        checkWorkDay(today().plusDays(6), false); // воскресенье
        checkWorkDay(today().plusDays(7), true);  // следующий понедельник

        checkWorkDay(today().plusDays(14), true);
        checkWorkDay(today().plusDays(15), false);
        checkWorkDay(today().plusDays(16), false);
    }

    @Test
    void throwException_WhenCreateCustomUserScheduleWithApplyDateNotMonday() {
        assertThatThrownBy(() -> service.createRule(user.getId(),
                UserScheduleTestHelper.ruleDtoApplyFrom(
                        UserScheduleType.CUSTOM_USER,
                        today().plusDays(3), //среда
                        today().plusDays(15),
                        today().plusDays(1), // applyTo - вторник
                        new boolean[]{true, false, true, false, true, false, false},
                        SortingCenter.DEFAULT_SC_ID,
                        UserScheduleTestHelper.createScheduleData(CourierVehicleType.CAR)), company)
        ).isInstanceOf(TplInvalidParameterException.class);
    }

    @Test
    void throwException_WhenCreateCustomUserScheduleWithIncorrectMask() {
        assertThatThrownBy(() -> service.createRule(user.getId(),
                UserScheduleTestHelper.ruleDtoApplyFrom(
                        UserScheduleType.CUSTOM_USER,
                        today().plusDays(3), //среда
                        today().plusDays(15),
                        today(),
                        new boolean[]{true, false, true, false, true, false}, // 6 дней вместо 7
                        SortingCenter.DEFAULT_SC_ID,
                        UserScheduleTestHelper.createScheduleData(CourierVehicleType.CAR)), company)
        ).isInstanceOf(TplInvalidParameterException.class);
    }

    private void checkWorkDay(LocalDate date, boolean expected) {
        boolean isWorkDay = service.findUserScheduleForDate(user, date).isWorkDay(date);
        assertThat(isWorkDay).isEqualTo(expected);
    }

    private void checkScheduleDate(UserScheduleDto schedule, LocalDate date, UserScheduleRuleDto expectedRule) {
        if (expectedRule == null) {
            assertThat(schedule.getCalendar().get(date)).isNull();
        } else {
            assertThat(schedule.getScheduleRules().get(Objects.toString(schedule.getCalendar().get(date))))
                    .isEqualTo(expectedRule);
        }
    }

    private void checkScheduleRules(UserScheduleDto schedule, List<UserScheduleRuleDto> expectedRules) {
        assertThat(schedule.getScheduleRules().values()).containsExactlyInAnyOrderElementsOf(expectedRules);
    }

    private UserScheduleRuleDto ruleDto(UserScheduleType type) {
        return ruleDto(type, 1);
    }

    private UserScheduleRuleDto ruleDto(UserScheduleType type, int durationDays) {
        LocalDate today = today();
        return UserScheduleTestHelper.ruleDto(type, today, today.plusDays(durationDays - 1));
    }

    private UserScheduleRulesSearchRequest filledGetRequest(UserScheduleType type) {
        return new UserScheduleRulesSearchRequest(
                user.getId(), type, type.getMetaType(), today(), today(), CourierVehicleType.CAR
        );
    }

    private UserScheduleRulesSearchRequest getRequest(LocalDate activeFrom, LocalDate activeTo) {
        return new UserScheduleRulesSearchRequest(
                user.getId(), null, null, activeFrom, activeTo, null
        );
    }

    private UserScheduleRulesSearchRequest emptyGetRequest() {
        return new UserScheduleRulesSearchRequest(
                user.getId(), null, null, null, null, null
        );
    }

    private Pageable pageable() {
        return PageRequest.of(0, 100, Sort.by(Sort.Direction.ASC, "activeFrom"));
    }

    // monday
    private LocalDate today() {
        return LocalDate.now(clock);
    }

    // tuesday
    private LocalDate tomorrow() {
        return today().plusDays(1);
    }

    // sunday
    private LocalDate yesterday() {
        return today().minusDays(1);
    }

}
