package ru.yandex.market.tpl.core.service.user.partner;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.schedule.UserScheduleRuleDto;
import ru.yandex.market.tpl.api.model.user.CourierVehicleType;
import ru.yandex.market.tpl.api.model.user.partner.PartnerUserDto;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.common.util.datetime.RelativeTimeInterval;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.domain.company.CompanyPermissionsProjection;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserRepository;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.vehicle.vehicle_instance.VehicleInstanceType;
import ru.yandex.market.tpl.core.service.user.SortingCenterPropertyService;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleData;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleRuleRepository;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleService;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleTestHelper;
import ru.yandex.market.tpl.core.service.vehicle.VehicleGenerateService;
import ru.yandex.market.tpl.core.test.ClockUtil;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.tpl.api.model.schedule.UserScheduleType.ALWAYS_WORKS;
import static ru.yandex.market.tpl.api.model.user.UserStatus.ACTIVE;
import static ru.yandex.market.tpl.api.model.user.UserStatus.FIRED;
import static ru.yandex.market.tpl.api.model.user.UserStatus.IN_PROCESS_OF_FIRING;
import static ru.yandex.market.tpl.api.model.user.UserStatus.NEWBIE;
import static ru.yandex.market.tpl.api.model.user.UserStatus.NOT_ACTIVE;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.IS_USER_FIRE_UPDATED;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.SHIFTS_AMOUNT_FOR_GRADE_UP;
import static ru.yandex.market.tpl.core.domain.partner.SortingCenter.DEFAULT_SC_ID;

@RequiredArgsConstructor
public class UserStatusServiceTest extends TplAbstractTest {
    private final TestUserHelper testUserHelper;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final UserStatusService userStatusService;
    private final UserScheduleService userScheduleService;
    private final Clock clock;
    private final PartnerUserDtoMapper partnerUserDtoMapper;
    private final PartnerUserService partnerUserService;
    private final UserRepository userRepository;
    private final UserScheduleRuleRepository userScheduleRuleRepository;
    private final UserShiftCommandDataHelper helper;
    private final OrderGenerateService orderGenerateService;
    private final UserShiftRepository userShiftRepository;
    private final UserShiftCommandService commandService;
    private final SortingCenterPropertyService sortingCenterPropertyService;
    private final TransactionTemplate transactionTemplate;
    private final VehicleGenerateService vehicleGenerateService;

    private User user;
    private CompanyPermissionsProjection company;

    @BeforeEach
    void init() {
        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                testUserHelper.sortingCenter(DEFAULT_SC_ID),
                SortingCenterProperties.ONE_DAY_SLOTS_SCHEDULE_ENABLED, true);
        LocalDate date = LocalDate.now();
        var tmpUser = testUserHelper.findOrCreateUser(955L, date);
        var vehicle = vehicleGenerateService.generateVehicle();
        vehicleGenerateService.assignVehicleToUser(VehicleGenerateService.VehicleInstanceGenerateParam.builder()
                .users(List.of(tmpUser))
                .type(VehicleInstanceType.PERSONAL)
                .vehicle(vehicle)
                .build());
        PartnerUserDto userDto = partnerUserDtoMapper.toPartnerUser(tmpUser);
        userDto.setPhone("9993214476");
        userDto.setUid(null);
        company = mock(CompanyPermissionsProjection.class);
        Mockito.when(company.isSuperCompany()).thenReturn(true);
        var newCourier = partnerUserService.createUser(userDto, company);
        user = userRepository.findByIdOrThrow(newCourier.getId());
    }

    @Test
    void checkCreatedUserIsNotActive() {
        assertThat(user.getStatus()).isEqualTo(NOT_ACTIVE);
    }

    @Test
    void checkDeletedCourierInFiredStatus() {
        configurationServiceAdapter.insertValue(IS_USER_FIRE_UPDATED, false);

        var userDto = transactionTemplate.execute(
                ts -> {
                    user = userRepository.findByIdOrThrow(user.getId());
                    var result =  partnerUserDtoMapper.toPartnerUser(user);
                    result.setStatus(null);
                    return result;
                }
        );
        userDto.setDeleted(true);

        partnerUserService.updateUser(user.getId(), userDto, company);

        user = userRepository.findByIdOrThrow(user.getId());
        assertThat(user.getStatus()).isEqualTo(FIRED);

    }


    @Test
    void checkUpdatedDeletedCourierInFiredStatus() {
        configurationServiceAdapter.insertValue(IS_USER_FIRE_UPDATED, true);

        var userDto = transactionTemplate.execute(
                ts -> {
                    user = userRepository.findByIdOrThrow(user.getId());
                    var result = partnerUserDtoMapper.toPartnerUser(user);
                    result.setStatus(null);
                    return result;
                }
        );
        userDto.setDeleted(true);

        partnerUserService.updateUser(user.getId(), userDto, company);

        user = userRepository.findByIdOrThrow(user.getId());
        assertThat(user.getStatus()).isEqualTo(IN_PROCESS_OF_FIRING);

        partnerUserService.fireUsersInProcessOfFiring();
        user = userRepository.findByIdOrThrow(user.getId());
        assertThat(user.getStatus()).isEqualTo(FIRED);
    }

    @Test
    void checkRecoveredCourierInNotActiveStatus() {
        configurationServiceAdapter.insertValue(IS_USER_FIRE_UPDATED, false);
        var userDto = transactionTemplate.execute(
                ts -> {
                    user = userRepository.findByIdOrThrow(user.getId());
                    return partnerUserDtoMapper.toPartnerUser(user);
                }
        );
        userDto.setDeleted(true);
        partnerUserService.updateUser(user.getId(), userDto, company);
        userDto.setDeleted(false);
        partnerUserService.updateUser(user.getId(), userDto, company);

        user = userRepository.findByIdOrThrow(user.getId());
        assertThat(user.getStatus()).isEqualTo(NOT_ACTIVE);
    }

    @Test
    void checkRecoveredCourierInNotActiveStatusUpdatedFire() {
        configurationServiceAdapter.insertValue(IS_USER_FIRE_UPDATED, true);
        var userDto = transactionTemplate.execute(
                ts -> {
                    user = userRepository.findByIdOrThrow(user.getId());
                    return partnerUserDtoMapper.toPartnerUser(user);
                }
        );
        userDto.setDeleted(true);
        partnerUserService.updateUser(user.getId(), userDto, company);
        userDto.setDeleted(false);
        partnerUserService.updateUser(user.getId(), userDto, company);

        user = userRepository.findByIdOrThrow(user.getId());
        assertThat(user.getStatus()).isEqualTo(NOT_ACTIVE);
    }

    @Test
    void checkAfterAddScheduleBySuperCompanyUserIsNewbie() {
        var ruleDto = createRuleDto();

        var createdRuleDto = userScheduleService.createRule(user.getId(), ruleDto, company.isSuperCompany());

        user = userRepository.findByIdOrThrow(user.getId());
        assertThat(user.getStatus()).isEqualTo(NEWBIE);
        var userSchedule = userScheduleRuleRepository.findByIdOrThrow(createdRuleDto.getId());
        assertThat(userSchedule.getScheduleData().getShiftStart()).isEqualTo(LocalTime.of(10, 0));
        assertThat(userSchedule.getScheduleData().getShiftEnd()).isEqualTo(LocalTime.of(18, 0));
    }

    @Test
    void checkAfterAddScheduleBySuperCompanyUserIsActive() {
        configurationServiceAdapter.insertValue(SHIFTS_AMOUNT_FOR_GRADE_UP, 0);
        Mockito.when(company.isSuperCompany()).thenReturn(true);
        var ruleDto = createRuleDto();
        var createdRuleDto = userScheduleService.createRule(user.getId(), ruleDto, company.isSuperCompany());

        user = userRepository.findByIdOrThrow(user.getId());
        assertThat(user.getStatus()).isEqualTo(ACTIVE);
        var userSchedule = userScheduleRuleRepository.findByIdOrThrow(createdRuleDto.getId());
        assertThat(userSchedule.getScheduleData().getShiftStart()).isEqualTo(LocalTime.of(10, 0));
        assertThat(userSchedule.getScheduleData().getShiftEnd()).isEqualTo(LocalTime.of(18, 0));
    }

    @Test
    void checkCourierWithScheduleNotDeactivated() {
        ClockUtil.initFixed(clock, LocalDateTime.now());
        var ruleDto = createRuleDto();
        userScheduleService.createRule(user.getId(), ruleDto, company.isSuperCompany());

        userStatusService.deactivateUsers();
        user = userRepository.findByIdOrThrow(user.getId());
        assertThat(user.getStatus()).isEqualTo(NEWBIE);
    }

    @Test
    void checkCourierWithoutScheduleDeactivated() {
        var ruleDto = createRuleDto();
        var createdRule = userScheduleService.createRule(user.getId(), ruleDto, company.isSuperCompany());
        userScheduleService.deleteRule(user.getId(), createdRule.getId(), company.isSuperCompany());

        userStatusService.deactivateUsers();

        assertThat(user.getStatus()).isEqualTo(NOT_ACTIVE);
    }

    @Test
    @Sql("classpath:mockPartner/deliveryServiceWithSCInDifferentTimeZone.sql")
    void checkUserWithoutDeliveryActivationAfterPickup() {
        var ruleDto = createRuleDto();
        userScheduleService.createRule(user.getId(), ruleDto, company.isSuperCompany());
        userStatusService.deactivateUsers();
        var shift = testUserHelper.findOrCreateOpenShiftForSc(LocalDate.now(clock), 100501);
        Order order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam
                .builder()
                .deliveryDate(LocalDate.now(clock))
                .deliveryInterval(LocalTimeInterval.valueOf("19:00-23:59"))
                .zoneId(ZoneId.ofOffset("UTC", ZoneOffset.ofHours(4)))
                .deliveryServiceId(100500L)
                .build()
        );
        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(helper.taskOrderPickup(clock.instant()))
                .routePoint(helper.taskPrepaid("addr1", 12, order.getId()))
                .mergeStrategy(SimpleStrategies.NO_MERGE)
                .build();
        var userShiftC = userShiftRepository.findById(commandService.createUserShift(createCommand))
                .orElseThrow();

        testUserHelper.checkinAndFinishPickup(userShiftC);

        user = userRepository.findByIdOrThrow(user.getId());
        assertThat(user.getStatus()).isEqualTo(NEWBIE);
    }

    private UserScheduleRuleDto createRuleDto() {
        return UserScheduleTestHelper.ruleDtoApplyFrom(
                ALWAYS_WORKS,
                LocalDate.now(clock),
                LocalDate.now(clock).plusDays(30),
                LocalDate.now(clock),
                ALWAYS_WORKS.getMaskWorkDays(),
                DEFAULT_SC_ID,
                new UserScheduleData(CourierVehicleType.CAR, RelativeTimeInterval.valueOf("10:00-18:00"))
        );
    }
}
