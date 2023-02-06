package ru.yandex.market.tpl.internal.service.billing;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.tpl.api.model.company.CompanyRoleEnum;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointType;
import ru.yandex.market.tpl.api.model.shift.UserShiftStatus;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliverySubtaskStatus;
import ru.yandex.market.tpl.api.model.transport.RoutingVehicleType;
import ru.yandex.market.tpl.client.billing.dto.BillingBalanceInfoDto;
import ru.yandex.market.tpl.client.billing.dto.BillingCompanyContainerDto;
import ru.yandex.market.tpl.client.billing.dto.BillingCompanyDto;
import ru.yandex.market.tpl.client.billing.dto.BillingDropOffReturnMovementContainerDto;
import ru.yandex.market.tpl.client.billing.dto.BillingDropOffReturnMovementDto;
import ru.yandex.market.tpl.client.billing.dto.BillingIntakeContainerDto;
import ru.yandex.market.tpl.client.billing.dto.BillingIntakeDto;
import ru.yandex.market.tpl.client.billing.dto.BillingOrderContainerDto;
import ru.yandex.market.tpl.client.billing.dto.BillingOrderDto;
import ru.yandex.market.tpl.client.billing.dto.BillingShiftType;
import ru.yandex.market.tpl.client.billing.dto.BillingSortingCenterContainerDto;
import ru.yandex.market.tpl.client.billing.dto.BillingSortingCenterDto;
import ru.yandex.market.tpl.client.billing.dto.BillingSurchargeDto;
import ru.yandex.market.tpl.client.billing.dto.BillingUserContainerDto;
import ru.yandex.market.tpl.client.billing.dto.BillingUserDto;
import ru.yandex.market.tpl.client.billing.dto.BillingUserShiftContainerDto;
import ru.yandex.market.tpl.client.billing.dto.BillingUserShiftDto;
import ru.yandex.market.tpl.client.billing.dto.BillingUserType;
import ru.yandex.market.tpl.common.dsm.client.api.CourierApi;
import ru.yandex.market.tpl.common.dsm.client.api.EmployerApi;
import ru.yandex.market.tpl.common.dsm.client.model.CourierTypeDto;
import ru.yandex.market.tpl.common.dsm.client.model.CouriersSearchItemDto;
import ru.yandex.market.tpl.common.dsm.client.model.CouriersSearchResultDto;
import ru.yandex.market.tpl.common.dsm.client.model.EmployerDto;
import ru.yandex.market.tpl.common.dsm.client.model.EmployersSearchResultDto;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.company.Company;
import ru.yandex.market.tpl.core.domain.company.CompanyRepository;
import ru.yandex.market.tpl.core.domain.company.CompanyRole;
import ru.yandex.market.tpl.core.domain.company.CompanyRoleRepository;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.dropoffcargo.DropoffCargo;
import ru.yandex.market.tpl.core.domain.movement.Movement;
import ru.yandex.market.tpl.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.core.domain.movement.MovementGenerator;
import ru.yandex.market.tpl.core.domain.order.Dimensions;
import ru.yandex.market.tpl.core.domain.order.DimensionsClass;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.place.OrderPlaceDto;
import ru.yandex.market.tpl.core.domain.partial_return_order.PartialReturnOrderGenerateService;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterRepository;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.region.RegionDao;
import ru.yandex.market.tpl.core.domain.region.TplRegion;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.surcharge.entity.Surcharge;
import ru.yandex.market.tpl.core.domain.surcharge.entity.SurchargeResolution;
import ru.yandex.market.tpl.core.domain.surcharge.entity.SurchargeValidationStatus;
import ru.yandex.market.tpl.core.domain.surcharge.repository.SurchargeRepository;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.CollectDropshipTaskFactory;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.commands.CargoReference;
import ru.yandex.market.tpl.core.domain.usershift.commands.NewDeliveryRoutePointData;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.domain.warehouse.OrderWarehouseGenerator;
import ru.yandex.market.tpl.core.service.usershift.additionaldata.vehicle.AdditionalVehicleDataService;
import ru.yandex.market.tpl.core.service.usershift.additionaldata.vehicle.UpdateAdditionalVehicleDataDto;
import ru.yandex.market.tpl.core.service.vehicle.VehicleGenerateService;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplTestCargoFactory;
import ru.yandex.market.tpl.internal.controller.TplIntTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static ru.yandex.common.util.region.RegionType.CITY;
import static ru.yandex.common.util.region.RegionType.SUBJECT_FEDERATION;

@TplIntTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class BillingServiceTest {
    private static final int MOSCOW_REG_ID = 213;
    private static final int MOSCOW_OBL_REG_ID = 1;
    public static final String BICYCLE_NUMBER = "A777AA";
    public static final String NUMBER_REGION = "77";
    private final Clock clock;
    private final OrderGenerateService orderGenerateService;
    private final MovementGenerator movementGenerator;
    private final UserShiftCommandDataHelper userShiftCommandDataHelper;
    private final UserShiftCommandService userShiftCommandService;
    private final TestUserHelper userHelper;
    private final BillingService billingService;
    private final CompanyRepository companyRepository;
    private final BillingCompanyMapper billingCompanyMapper;
    private final BillingUserMapper billingUserMapper;
    private final SortingCenterRepository sortingCenterRepository;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final PartialReturnOrderGenerateService partialReturnOrderGenerateService;
    private final TplTestCargoFactory tplTestCargoFactory;
    private final TestDataFactory testDataFactory;
    private final PickupPointRepository pickupPointRepository;
    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final VehicleGenerateService vehicleGenerateService;
    private final AdditionalVehicleDataService additionalVehicleDataService;
    private final RegionDao regionDao;
    private final CompanyRoleRepository companyRoleRepository;
    private final SurchargeRepository surchargeRepository;
    private final List<Company> COMPANIES = new ArrayList<>();
    private final ConfigurationProviderAdapter configurationProviderAdapter;
    private final CourierApi courierApi;
    private final EmployerApi employerApi;
    private final Long BALANCE_CLIENT_ID = 1L;
    private final Long BALANCE_CONTRACT_ID = 2L;
    private final Long BALANCE_PERSON_ID = 3L;
    private final String COMPANY_OGRN = "ogrn";


    private Company dbsCompany;
    private Company company1;
    private Company company2;
    private CompanyRole partnerRole;

    @BeforeEach
    void setUp() {
        partnerRole = companyRoleRepository.findByName(CompanyRoleEnum.PARTNER).orElseThrow();
        CompanyRole dbsRole = companyRoleRepository.findByName(CompanyRoleEnum.DBS).orElseThrow();

        company1 = Company.builder()
                .id(1L)
                .dsmExternalId("1")
                .name("name1")
                .taxpayerNumber("inn1")
                .ogrn("ogrn1")
                .login("login1")
                .phoneNumber("phone1")
                .juridicalAddress("address1")
                .companyRole(partnerRole)
                .isSuperCompany(false)
                .build();
        company2 = Company.builder()
                .id(2L)
                .dsmExternalId("2")
                .name("name2")
                .taxpayerNumber("inn2")
                .ogrn("ogrn2")
                .login("login2")
                .phoneNumber("phone2")
                .juridicalAddress("address2")
                .isSuperCompany(true)
                .companyRole(partnerRole)
                .deactivated(true)
                .build();
        dbsCompany = Company.builder()
                .id(32543409L)
                .dsmExternalId("32543409")
                .name("name3")
                .taxpayerNumber("inn3")
                .ogrn("ogrn3")
                .login("login3")
                .phoneNumber("phone3")
                .juridicalAddress("address3")
                .isSuperCompany(false)
                .sortingCenters(new HashSet<>())
                .companyRole(dbsRole)
                .deactivated(true)
                .build();


        COMPANIES.add(company1);
        COMPANIES.add(company2);
        COMPANIES.add(dbsCompany);
        companyRepository.saveAll(COMPANIES);
        COMPANIES.remove(dbsCompany);
    }

    @Test
    void findUsersWhenDsmIntegrationDisabled() {
        Mockito.when(configurationProviderAdapter
                .isBooleanEnabled(ConfigurationProperties.RETRIEVE_USER_DATA_FROM_DSM_FOR_BILLING_ENABLED))
                .thenReturn(false);

        User user11 = userHelper.findOrCreateUserWithoutSchedule(11, "name1");
        User user12 = userHelper.findOrCreateUserWithoutSchedule(12, "name1");
        userHelper.findOrCreateUserWithoutSchedule(21, "name2");

        BillingUserContainerDto users = billingService.findUsers(user11.getCompany().getId(), null, null);
        assertThat("users", users.getUsers(),
                containsInAnyOrder(billingUserMapper.map(user11), billingUserMapper.map(user12))
        );

        BillingUserContainerDto usersPaging1 = billingService.findUsers(user11.getCompany().getId(), 0, 1);
        assertThat("users", usersPaging1.getUsers(),
                containsInAnyOrder(billingUserMapper.map(user11))
        );

        BillingUserContainerDto usersPaging2 = billingService.findUsers(user11.getCompany().getId(), 1, 1);
        assertThat("users", usersPaging2.getUsers(),
                containsInAnyOrder(billingUserMapper.map(user12))
        );
    }


    @Test
    void findUsersWhenDsmIntegrationEnabled() {
        User user11 = userHelper.findOrCreateUserWithoutSchedule(11, "name1");
        userHelper.findOrCreateUserWithoutSchedule(21, "name2");

        Mockito.when(configurationProviderAdapter
                .isBooleanEnabled(ConfigurationProperties.RETRIEVE_USER_DATA_FROM_DSM_FOR_BILLING_ENABLED))
                .thenReturn(true);
        Mockito.when(courierApi.couriersGet(eq(0), eq(500), any(), any(), any(), any()))
                .thenReturn(createFakeCouriersSearchResultDto(user11));

        BillingUserContainerDto users = billingService.findUsers(user11.getCompany().getId(), null, null);

        List<BillingUserDto> billingUsers = users.getUsers();
        assertThat(billingUsers, hasSize(1));

        BillingUserDto billingUserDto = billingUsers.get(0);
        assertEquals(billingUserDto.getId(), user11.getId());
        assertEquals(billingUserDto.getDsmId(), user11.getDsmExternalId());
        assertEquals(billingUserDto.getType(), BillingUserType.PARTNER);
        assertEquals(billingUserDto.getPhone(), user11.getPhone());
        assertBalanceFields(billingUserDto.getBalanceInfo());
    }

    @Test
    void findCompaniesWhenDsmIntegrationDisabled() {
        Mockito.when(configurationProviderAdapter
                .isBooleanEnabled(ConfigurationProperties.RETRIEVE_COMPANY_DATA_FROM_DSM_FOR_BILLING_ENABLED))
                .thenReturn(false);
        List<BillingCompanyDto> actual = billingService.findCompanies(null, null).getCompanies();
        assertEquals(billingCompanyMapper.map(COMPANIES), actual);

        List<BillingCompanyDto> paging1 = billingService.findCompanies(0, 1).getCompanies();
        assertEquals(billingCompanyMapper.map(List.of(company1)), paging1);

        List<BillingCompanyDto> paging2 = billingService.findCompanies(1, 1).getCompanies();
        assertEquals(billingCompanyMapper.map(List.of(company2)), paging2);
    }

    @Test
    void findCompaniesWhenDsmIntegrationEnabled() {
        Mockito.when(configurationProviderAdapter
                .isBooleanEnabled(ConfigurationProperties.RETRIEVE_COMPANY_DATA_FROM_DSM_FOR_BILLING_ENABLED))
                .thenReturn(true);
        Mockito.when(employerApi.employersGet(eq(0), eq(500), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(createFakeCompanyDto(company1));

        BillingCompanyContainerDto companies = billingService.findCompanies(null, null);

        List<BillingCompanyDto> billingCompanies = companies.getCompanies();
        assertThat(billingCompanies, hasSize(1));

        BillingCompanyDto dto = billingCompanies.get(0);
        assertEquals(dto.getId(), company1.getId());
        assertEquals(dto.getDsmId(), company1.getDsmExternalId());
        assertEquals(dto.getOgrn(), "ogrn");
        assertBalanceFields(dto.getBalanceInfo());
        assertFalse(dto.isDeactivated());
    }

    @NotNull
    private CouriersSearchResultDto createFakeCouriersSearchResultDto(User user) {
        CouriersSearchResultDto result = new CouriersSearchResultDto();
        CouriersSearchItemDto courierDsmItem = new CouriersSearchItemDto();
        courierDsmItem.setId(user.getDsmExternalId());
        courierDsmItem.setCourierType(CourierTypeDto.PARTNER);
        courierDsmItem.setBalanceClientId(BALANCE_CLIENT_ID);
        courierDsmItem.setBalanceContractId(BALANCE_CONTRACT_ID);
        courierDsmItem.setBalancePersonId(BALANCE_PERSON_ID);
        result.setContent(List.of(courierDsmItem));
        result.setTotalPages(1);
        return result;
    }


    @NotNull
    private EmployersSearchResultDto createFakeCompanyDto(Company company) {
        EmployersSearchResultDto result = new EmployersSearchResultDto();
        EmployerDto employerDto = new EmployerDto();
        employerDto.setId(company.getDsmExternalId());
        employerDto.setActive(true);
        employerDto.setOgrn(COMPANY_OGRN);
        employerDto.setBalanceClientId(BALANCE_CLIENT_ID);
        employerDto.setBalanceContractId(BALANCE_CONTRACT_ID);
        employerDto.setBalancePersonId(3L);
        result.setContent(List.of(employerDto));
        result.setTotalPages(1);
        return result;
    }

    private void assertBalanceFields(BillingBalanceInfoDto balance) {
        assertEquals(balance.getClientId(), BALANCE_CLIENT_ID.toString());
        assertEquals(balance.getContractId(), BALANCE_CONTRACT_ID.toString());
        assertEquals(balance.getPersonId(), BALANCE_PERSON_ID.toString());

    }

    @Test
    void findShifts() {
        Order order1 = createOrder();
        UserShift userShift1 = createUserShift(order1, 246);
        generateAndAssignVehicleToUserShift(userShift1, "Лада", "X-Ray", "A666AA");
        Order order2 = createOrder();
        Order order3 = createOrder();
        UserShift userShift2 = createUserShift(order2, 357);
        generateAndAssignVehicleToUserShift(userShift2, "BMW", "X5", "A000AA");
        UserShift userShift3 = createUserShift(order3, 890);
        User dbsUser = userHelper.createOrFindDbsUser();
        Order dbsOrder = createOrder();
        UserShift dbsUserShift = createUserShift(dbsOrder, dbsUser.getUid());
        userShiftCommandService.updateTransitDistance(
                new UserShiftCommand.UpdateTransitDistance(
                        userShift3.getId(),
                        BigDecimal.valueOf(123)
                )
        );
        BigDecimal distance = BigDecimal.TEN;
        createRoutingLog(userShift1, distance);
        createRoutingLog(userShift1, distance.add(BigDecimal.ONE));
        updateFailReasonType(order1.getId(), OrderDeliveryTaskFailReasonType.COURIER_REASSIGNED);

        BillingUserShiftContainerDto actual = billingService.findShifts(LocalDate.now());
        BillingUserShiftContainerDto expected = BillingUserShiftContainerDto.builder()
                .userShifts(List.of(
                        BillingUserShiftDto.builder()
                                .id(userShift1.getId())
                                .shiftType(BillingShiftType.AVTO)
                                .shiftDate(LocalDate.now())
                                .userId(userShift1.getUser().getId())
                                .userDsmId(userShift1.getUser().getDsmExternalId())
                                .companyId(userShift1.getUser().getCompany().getId())
                                .companyDsmId(userShift1.getUser().getCompany().getDsmExternalId())
                                .sortingCenterId(userShift1.getShift().getSortingCenter().getId())
                                .distance(distance.intValue())
                                .routingVehicleType(RoutingVehicleType.COMMON)
                                .transportTypeId(0L)
                                .takenOrderCount(0)
                                .takenFashionOrderCount(0)
                                .vehicleNumber("A666AA77")
                                .build(),
                        BillingUserShiftDto.builder()
                                .id(userShift2.getId())
                                .shiftType(BillingShiftType.AVTO)
                                .shiftDate(LocalDate.now())
                                .userId(userShift2.getUser().getId())
                                .userDsmId(userShift2.getUser().getDsmExternalId())
                                .companyId(userShift2.getUser().getCompany().getId())
                                .companyDsmId(userShift2.getUser().getCompany().getDsmExternalId())
                                .sortingCenterId(userShift2.getShift().getSortingCenter().getId())
                                .distance(0)
                                .routingVehicleType(RoutingVehicleType.COMMON)
                                .transportTypeId(0L)
                                .takenOrderCount(1)
                                .takenFashionOrderCount(0)
                                .vehicleNumber("A000AA77")
                                .build(),
                        BillingUserShiftDto.builder()
                                .id(userShift3.getId())
                                .shiftType(BillingShiftType.AVTO)
                                .shiftDate(LocalDate.now())
                                .userId(userShift3.getUser().getId())
                                .userDsmId(userShift3.getUser().getDsmExternalId())
                                .companyId(userShift3.getUser().getCompany().getId())
                                .companyDsmId(userShift3.getUser().getCompany().getDsmExternalId())
                                .sortingCenterId(userShift3.getShift().getSortingCenter().getId())
                                .distance(123)
                                .routingVehicleType(RoutingVehicleType.COMMON)
                                .transportTypeId(0L)
                                .takenOrderCount(1)
                                .takenFashionOrderCount(0)
                                //Мы больше не храним vehicle_number напрямую в UserShift. Валидно только для исторических данных
                                .vehicleNumber(null)
                                .build())
                )
                .build();
        assertEquals(expected, actual, "shifts");
    }

    @Test
    void findBicycleShifts() {
        //given
        //Bicycle courier...
        Order order1 = createOrder();
        UserShift userShift1 = createUserShift(order1, 246);
        generateAndAssignBicycleVehicleToUserShift(userShift1, "Stels", "Велосипед");

        //Courier without vehicle (default as Avto...
        Order order2 = createOrder();
        UserShift userShift2 = createUserShift(order2, 357);
        BigDecimal distance = BigDecimal.TEN;
        createRoutingLog(userShift1, distance);
        createRoutingLog(userShift1, distance.add(BigDecimal.ONE));
        updateFailReasonType(order1.getId(), OrderDeliveryTaskFailReasonType.COURIER_REASSIGNED);

        //when
        BillingUserShiftContainerDto actual = billingService.findShifts(LocalDate.now());

        //then
        BillingUserShiftContainerDto expected = BillingUserShiftContainerDto.builder()
                .userShifts(List.of(
                        BillingUserShiftDto.builder()
                                .id(userShift1.getId())
                                .shiftType(BillingShiftType.VELO)
                                .shiftDate(LocalDate.now())
                                .userId(userShift1.getUser().getId())
                                .userDsmId(userShift1.getUser().getDsmExternalId())
                                .companyId(userShift1.getUser().getCompany().getId())
                                .companyDsmId(userShift1.getUser().getCompany().getDsmExternalId())
                                .sortingCenterId(userShift1.getShift().getSortingCenter().getId())
                                .distance(distance.intValue())
                                .routingVehicleType(RoutingVehicleType.COMMON)
                                .transportTypeId(0L)
                                .takenOrderCount(0)
                                .takenFashionOrderCount(0)
                                .vehicleNumber(BICYCLE_NUMBER + NUMBER_REGION)
                                .build(),
                        BillingUserShiftDto.builder()
                                .id(userShift2.getId())
                                .shiftType(BillingShiftType.AVTO)
                                .shiftDate(LocalDate.now())
                                .userId(userShift2.getUser().getId())
                                .userDsmId(userShift2.getUser().getDsmExternalId())
                                .companyId(userShift2.getUser().getCompany().getId())
                                .companyDsmId(userShift2.getUser().getCompany().getDsmExternalId())
                                .sortingCenterId(userShift2.getShift().getSortingCenter().getId())
                                .distance(0)
                                .routingVehicleType(RoutingVehicleType.COMMON)
                                .transportTypeId(0L)
                                .takenOrderCount(1)
                                .takenFashionOrderCount(0)
                                .vehicleNumber(null)
                                .build())
                )
                .build();
        assertEquals(expected, actual, "shifts");
    }

    private void createRoutingLog(UserShift userShift1, BigDecimal distance) {
        namedParameterJdbcTemplate.update(
                "" +
                        "insert into routing_log(user_shift_id, shift_date, transit_distance, request_id, " +
                        "routing_type, " +
                        "  mock, created_at, updated_at) " +
                        "values(:user_shift_id, :shift_date, :distance, '', 'REROUTE', '', now(), now())",
                new MapSqlParameterSource()
                        .addValue("user_shift_id", userShift1.getId())
                        .addValue("shift_date", LocalDate.now())
                        .addValue("distance", distance)
        );
    }

    private void updateFailReasonType(Long orderId, OrderDeliveryTaskFailReasonType failReasonType) {
        namedParameterJdbcTemplate.update(
                "" +
                        "update task_order_delivery set fail_reason_type = :fail_reason where order_id = :order_id",
                new MapSqlParameterSource()
                        .addValue("fail_reason", String.valueOf(failReasonType))
                        .addValue("order_id", orderId)
        );
    }

    @Test
    void findOrdersWithBBSyncTest() {
        var order = createOrder();
        createOrder();
        var userShift = createUserShift(order, 136);
        var taskId = userShift.getDeliveryTaskForOrder(order.getId()).getId();

        namedParameterJdbcTemplate.update("" +
                        "insert into task_locker_delivery (id) values (:id) ",
                Map.of("id", taskId));
        namedParameterJdbcTemplate.update("" +
                        "update  task set type = 'LOCKER_DELIVERY' where id = :id ",
                Map.of("id", taskId));
        namedParameterJdbcTemplate.update("" +
                        "insert into subtask_locker_delivery (id, task_id, order_id, status, fail_reason_type, " +
                        "fail_source, created_at, updated_at) " +
                        "values (nextval('seq_subtask_locker_delivery'), :taskId, :orderId, :status, :failReason, " +
                        ":failSource, now(), now()) ",
                Map.of(
                        "taskId", taskId,
                        "orderId", order.getId(),
                        "status", LockerDeliverySubtaskStatus.FAILED.name(),
                        "failReason", OrderDeliveryTaskFailReasonType.FINISHED_BY_SUPPORT.name(),
                        "failSource", Source.COURIER.name()
                ));

        var orderBilling1 = billingService.findOrders(Set.of(userShift.getId())).getOrders().get(0);
        assertEquals("FAILED", orderBilling1.getPickupSubtaskStatus());
        assertEquals(orderBilling1.getFailReasonType(), OrderDeliveryTaskFailReasonType.FINISHED_BY_SUPPORT);
        assertEquals(orderBilling1.getFailSource(), Source.COURIER);

        namedParameterJdbcTemplate.update("insert into boxbot_order_sync_data (subtask_id, order_id, created_at, " +
                        "updated_at, started_at, status, order_status_before) " +
                        "select id as subtaskId, order_id, now(), now(), now(), 'FINISHED' as status, 'LOST' as " +
                        "order_status_before " +
                        "from subtask_locker_delivery where order_id = :orderId;",
                Map.of("orderId", order.getId()));

        // С учетом данных по синхронизации статусов с ББ задание в биллинге должно числиться выполненным
        var orderBilling2 = billingService.findOrders(Set.of(userShift.getId())).getOrders().get(0);
        assertEquals("FINISHED", orderBilling2.getPickupSubtaskStatus());
        assertNull(orderBilling2.getFailReasonType());
        assertNull(orderBilling2.getFailSource());
    }

    @Test
    void findOrders() {
        Order order = createOrder();
        UserShift userShift = createUserShift(order, 135);
        namedParameterJdbcTemplate.update("" +
                "update task_order_delivery " +
                "set fail_source = 'CLIENT', " +
                "    fail_reason_type = 'CANNOT_PAY' ", Map.of());
        BillingOrderContainerDto orders = billingService.findOrders(Set.of(userShift.getId()));
        GeoPoint geoPoint = GeoPointGenerator.generateLonLat();
        BillingOrderDto expected = BillingOrderDto.builder()
                .userShiftId(userShift.getId())
                .id(order.getId())
                .marketOrderId(order.getExternalOrderId())
                .longitude(geoPoint.getLongitude())
                .latitude(geoPoint.getLatitude())
                .multiOrderId(userShift.getDeliveryTaskForOrder(order.getId()).getMultiOrderId())
                .recipientPhone(order.getDelivery().getRecipientPhone())
                .deliveryTaskStatus("NOT_DELIVERED")
                .pickupSubtaskStatus(null)
                .collectDropshipTaskStatus(null)
                .taskId(userShift.getDeliveryTaskForOrder(order.getId()).getDeliveryTask().getId())
                .routePointId(userShift.findRoutePointIdByTaskId(userShift.getDeliveryTaskForOrder(order.getId())
                        .getDeliveryTask().getId()).get())
                .taskType("ORDER_DELIVERY")
                .pickupPointId(null)
                .pickupPointType(null)
                .placeCount(1)
                .deliveryIntervalFrom(Instant.parse("1990-01-01T08:00:00Z"))
                .deliveryIntervalTo(Instant.parse("1990-01-01T12:00:00Z"))
                .weight(BigDecimal.valueOf(1.2))
                .width(20)
                .height(30)
                .length(10)
                .dimensionsClass(DimensionsClass.REGULAR_CARGO.name())
                .failSource(Source.CLIENT)
                .failReasonType(OrderDeliveryTaskFailReasonType.CANNOT_PAY)
                .fashion(false)
                .partialReturnAllowed(false)
                .partialReturned(false)
                .build();
        BillingOrderDto actual = orders.getOrders().get(0);
        assertEquals(expected.getUserShiftId(), actual.getUserShiftId(), "userShiftId");
        assertEquals(expected.getId(), actual.getId(), "id");
        assertEquals(expected.getPlaceCount(), actual.getPlaceCount(), "boxCount");
        assertEquals(expected.getDeliveryTaskStatus(), actual.getDeliveryTaskStatus(), "deliveryTaskStatus");
        assertEquals(expected.getMarketOrderId(), actual.getMarketOrderId(), "marketOrderId");
        assertEquals(expected.getMultiOrderId(), actual.getMultiOrderId(), "multiOrderId");
        assertEquals(expected.getPickupPointId(), actual.getPickupPointId(), "pickupPointId");
        assertEquals(expected.getPickupPointType(), actual.getPickupPointType(), "pickupPointType");
        assertEquals(expected.getPickupSubtaskStatus(), actual.getPickupSubtaskStatus(),
                "pickupSubtaskStatus");
        assertEquals(expected.getRecipientPhone(), actual.getRecipientPhone(), "recipientPhone");
        assertEquals(expected.getRoutePointId(), actual.getRoutePointId(), "routePointId");
        assertEquals(expected.getTaskId(), actual.getTaskId(), "taskId");
        assertEquals(expected.getTaskType(), actual.getTaskType(), "taskType");
        assertEquals(expected.getDeliveryIntervalFrom(), actual.getDeliveryIntervalFrom(), "deliveryIntervalFrom");
        assertEquals(expected.getDeliveryIntervalTo(), actual.getDeliveryIntervalTo(), "deliveryIntervalTo");
        assertEquals(expected.getDeliveryIntervalTo(), actual.getDeliveryIntervalTo(), "deliveryIntervalTo");
        assertEquals(expected.getWeight(), actual.getWeight(), "weight");
        assertEquals(expected.getWidth(), actual.getWidth(), "width");
        assertEquals(expected.getLength(), actual.getLength(), "length");
        assertEquals(expected.getDimensionsClass(), actual.getDimensionsClass(), "dimensionsClass");
        assertEquals(expected.getFashion(), actual.getFashion(), "fashion");
        assertEquals(expected.getPartialReturned(), actual.getPartialReturned(), "partialReturned");
        assertEquals(expected.getPartialReturnAllowed(), actual.getPartialReturnAllowed(), "partialReturnAllowed");
    }

    @Test
    void findIntakesWithoutReturn() {
        Movement movement = createMovement();
        User user = userHelper.findOrCreateUser(824125L, LocalDate.now(clock));
        var shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));
        //Классическая заборка
        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(userShiftCommandDataHelper.taskCollectDropship(LocalDate.now(clock), movement)).build();
        UserShift userShift = userShiftCommandService.getOrCreateUserShift(createCommand);

        //Новая заборка
        PickupPoint pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.PVZ, 100500L, 1L)
        );
        Movement movement2 = movementGenerator.generate(
                MovementCommand.Create.builder()
                        .volume(BigDecimal.ONE)
                        .orderWarehouse(
                                orderWarehouseGenerator.generateWarehouse(
                                        wh -> wh.setYandexId(pickupPoint.getLogisticPointId().toString())
                                )
                        )
                        .build());
        addCargoTask(movement2, userShift.getId(), pickupPoint.getId(), false);

        //Возвратный поток
        Movement movement3 = movementGenerator.generate(
                MovementCommand.Create.builder()
                        .volume(BigDecimal.ONE)
                        .orderWarehouse(
                                orderWarehouseGenerator.generateWarehouse(
                                        wh -> wh.setYandexId(pickupPoint.getLogisticPointId().toString())
                                )
                        )
                        .build());
        addCargoTask(movement3, userShift.getId(), pickupPoint.getId(), true);

        Set<Long> movIds = billingService.findIntakes(Set.of(userShift.getId())).getIntakes().stream()
                .map(BillingIntakeDto::getMovementId)
                .collect(Collectors.toSet());
        Assertions.assertThat(movIds).containsExactlyInAnyOrder(movement.getId(), movement2.getId());
    }

    @Test
    void findIntakes() {
        Movement movement = createMovement();
        User user = userHelper.findOrCreateUser(824125L, LocalDate.now(clock));
        var shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));
        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(userShiftCommandDataHelper.taskCollectDropship(LocalDate.now(clock), movement)).build();
        UserShift userShift = userShiftCommandService.getOrCreateUserShift(createCommand);
        BillingIntakeContainerDto intakes = billingService.findIntakes(Set.of(userShift.getId()));
        GeoPoint geoPoint = GeoPointGenerator.generateLonLat();
        BillingIntakeDto expected = BillingIntakeDto.builder()
                .userShiftId(userShift.getId())
                .movementId(movement.getId())
                .movementExternalId(movement.getExternalId())
                .warehouseYandexId(movement.getWarehouse().getYandexId())
                .longitude(geoPoint.getLongitude())
                .latitude(geoPoint.getLatitude())
                .collectDropshipTaskStatus("NOT_STARTED")
                .taskType("COLLECT_DROPSHIP")
                .deliveryIntervalFrom(Instant.parse("1990-01-01T06:00:00Z"))
                .deliveryIntervalTo(Instant.parse("1990-01-01T15:00:00Z"))
                .build();
        BillingIntakeDto actual = intakes.getIntakes().get(0);
        assertEquals(expected.getUserShiftId(), actual.getUserShiftId(), "userShiftId");
        assertEquals(expected.getTaskType(), actual.getTaskType(), "taskType");
        assertEquals(expected.getMovementId(), actual.getMovementId(), "movementId");
        assertEquals(expected.getMovementExternalId(), actual.getMovementExternalId(), "movementExternalId");
        assertEquals(expected.getWarehouseYandexId(), actual.getWarehouseYandexId(), "warehouseYandexId()");
    }

    @Test
    void findDropOffDirect() {
        PickupPoint pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.PVZ, 100500L, 1L)
        );

        User user = userHelper.findOrCreateUser(824125L, LocalDate.now(clock));

        Shift shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));

        Movement movement = movementGenerator.generate(
                MovementCommand.Create.builder()
                        .volume(BigDecimal.ONE)
                        .orderWarehouse(
                                orderWarehouseGenerator.generateWarehouse(
                                        wh -> wh.setYandexId(pickupPoint.getLogisticPointId().toString())
                                )
                        )
                        .build());

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .active(true)
                .build();

        Long userShiftId = userShiftCommandService.createUserShift(createCommand);

        addCargoTask(movement, userShiftId, pickupPoint.getId(), false);

        String logisticPointIdTo = pickupPoint.getLogisticPointId().toString();
        DropoffCargo cargo1 = tplTestCargoFactory.createCargoDirect("BAG-001", logisticPointIdTo);
        DropoffCargo cargo2 = tplTestCargoFactory.createCargoDirect("BAG-002", logisticPointIdTo);


        tplTestCargoFactory.finishUpload(
                List.of(cargo1, cargo2),
                TplTestCargoFactory.ShiftContext.of(user, userShiftId)
        );

        BillingIntakeContainerDto intakes = billingService.findIntakes(Set.of(userShiftId));
        GeoPoint geoPoint = GeoPointGenerator.generateLonLat();
        BillingIntakeDto expected = BillingIntakeDto.builder()
                .userShiftId(userShiftId)
                .movementId(movement.getId())
                .movementExternalId(movement.getExternalId())
                .warehouseYandexId(movement.getWarehouse().getYandexId())
                .longitude(geoPoint.getLongitude())
                .latitude(geoPoint.getLatitude())
                .collectDropshipTaskStatus("NOT_STARTED")
                .taskType("LOCKER_DELIVERY")
                .deliveryIntervalFrom(Instant.parse("1990-01-01T06:00:00Z"))
                .deliveryIntervalTo(Instant.parse("1990-01-01T15:00:00Z"))
                .build();
        BillingIntakeDto actual = intakes.getIntakes().get(0);
        assertEquals(expected.getUserShiftId(), actual.getUserShiftId(), "userShiftId");
        assertEquals(expected.getTaskType(), actual.getTaskType(), "taskType");
        assertEquals(expected.getMovementId(), actual.getMovementId(), "movementId");
        assertEquals(expected.getMovementExternalId(), actual.getMovementExternalId(), "movementExternalId");
        assertEquals(expected.getWarehouseYandexId(), actual.getWarehouseYandexId(), "warehouseYandexId()");
    }

    @Test
    void findDropOffReturnMovements() {
        PickupPoint pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.PVZ, 100500L, 1L)
        );

        User user = userHelper.findOrCreateUser(824125L, LocalDate.now(clock));

        Shift shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));

        Movement movement = movementGenerator.generate(
                MovementCommand.Create.builder()
                        .volume(BigDecimal.ONE)
                        .orderWarehouseTo(
                                orderWarehouseGenerator.generateWarehouse(
                                        wh -> wh.setYandexId(pickupPoint.getLogisticPointId().toString())
                                )
                        )
                        .tags(List.of(Movement.TAG_DROPOFF_CARGO_RETURN))
                        .build());

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .active(true)
                .build();

        Long userShiftId = userShiftCommandService.createUserShift(createCommand);

        addCargoTask(movement, userShiftId, pickupPoint.getId(), true);

        String logisticPointIdTo = pickupPoint.getLogisticPointId().toString();
        DropoffCargo cargo1 = tplTestCargoFactory.createCargo("BAG-001", logisticPointIdTo);
        DropoffCargo cargo2 = tplTestCargoFactory.createCargo("BAG-002", logisticPointIdTo);

        tplTestCargoFactory.initPickupCargoFlow(
                TplTestCargoFactory.CargoPickupContext.of(List.of(), Set.of(cargo1.getId(), cargo2.getId()), Set.of()),
                TplTestCargoFactory.ShiftContext.of(user, userShiftId)
        );

        BillingDropOffReturnMovementContainerDto movements =
                billingService.findDropOffReturnMovements(Set.of(userShiftId));
        GeoPoint geoPoint = GeoPointGenerator.generateLonLat();
        BillingDropOffReturnMovementDto expected = BillingDropOffReturnMovementDto.builder()
                .userShiftId(userShiftId)
                .movementId(movement.getId())
                .movementExternalId(movement.getExternalId())
                .warehouseYandexId(movement.getWarehouseTo().getYandexId())
                .longitude(geoPoint.getLongitude())
                .latitude(geoPoint.getLatitude())
                .taskStatus("NOT_STARTED")
                .taskType("LOCKER_DELIVERY")
                .deliveryIntervalFrom(Instant.parse("1990-01-01T06:00:00Z"))
                .deliveryIntervalTo(Instant.parse("1990-01-01T15:00:00Z"))
                .deliveredCargoCount(0L)
                .build();
        BillingDropOffReturnMovementDto actual = movements.getMovements().get(0);
        assertEquals(expected.getUserShiftId(), actual.getUserShiftId(), "userShiftId");
        assertEquals(expected.getTaskType(), actual.getTaskType(), "taskType");
        assertEquals(expected.getTaskStatus(), actual.getTaskStatus(), "taskStatus");
        assertEquals(expected.getMovementId(), actual.getMovementId(), "movementId");
        assertEquals(expected.getMovementExternalId(), actual.getMovementExternalId(), "movementExternalId");
        assertEquals(expected.getWarehouseYandexId(), actual.getWarehouseYandexId(), "warehouseYandexId");
        assertEquals(expected.getDeliveredCargoCount(), actual.getDeliveredCargoCount(), "deliveredCargoCount");
    }

    @Test
    void findSortingCenters() {
        SortingCenter sortingCenter = new SortingCenter();
        sortingCenter.setId(3423490L);
        sortingCenter.setName("СЦ ГОРОД N");
        sortingCenter.setAddress("город N улица M");
        sortingCenter.setLatitude(BigDecimal.TEN);
        sortingCenter.setLongitude(BigDecimal.TEN);

        SortingCenter dbsSortingCenter = new SortingCenter();
        dbsSortingCenter.setId(10230L);
        dbsSortingCenter.setName("СЦ ГОРОД DBS");
        dbsSortingCenter.setAddress("город DBS улица DBS");
        dbsSortingCenter.setLatitude(BigDecimal.TEN);
        dbsSortingCenter.setLongitude(BigDecimal.TEN);

        Company company = Company.builder()
                .id(80938L)
                .name("name34242")
                .taxpayerNumber("inn2213")
                .ogrn("ogrnog")
                .login("login34509")
                .phoneNumber("phone134534")
                .sortingCenters(new HashSet<>())
                .juridicalAddress("address19203")
                .companyRole(partnerRole)
                .isSuperCompany(false)
                .build();

        dbsCompany.addSortingCenter(dbsSortingCenter);
        company.addSortingCenter(sortingCenter);

        sortingCenterRepository.save(sortingCenter);
        sortingCenterRepository.save(dbsSortingCenter);

        companyRepository.save(company);
        companyRepository.save(dbsCompany);

        BillingSortingCenterContainerDto actual = billingService.findSortingCenters();
        BillingSortingCenterContainerDto expected = BillingSortingCenterContainerDto.builder()
                .sortingCenters(
                        List.of(
                                BillingSortingCenterDto.builder()
                                        .id(sortingCenter.getId())
                                        .name(sortingCenter.getName())
                                        .latitude(sortingCenter.getLatitude().doubleValue())
                                        .longitude(sortingCenter.getLongitude().doubleValue())
                                        .build()
                        )
                )
                .build();
        assertEquals(actual, expected, "sorting centers");
        companyRepository.delete(company);
    }

    @Test
    void getRegionIdsByType() {
        BillingSortingCenterContainerDto sortingCenters = getSortingCenters();
        regionDao.updateRegions(getRegions());
        List<Long> actual = billingService.getRegionIdsByType(sortingCenters, SUBJECT_FEDERATION);
        assertEquals(MOSCOW_OBL_REG_ID, actual.get(0));
    }

    @Test
    void findUserSurcharges_success() {
        User user = userHelper.findOrCreateUser(824125L, LocalDate.now(clock));

        Surcharge surcharge1 = getSurcharge(SurchargeValidationStatus.VALID, company1.getId(), user.getId());
        Surcharge surcharge2 = getSurcharge(SurchargeValidationStatus.VALID, company2.getId(), null);
        surchargeRepository.save(surcharge1);
        surchargeRepository.save(surcharge2);
        surchargeRepository.save(
                getSurcharge(SurchargeValidationStatus.INVALID, company1.getId(), user.getId())
        );

        LocalDate date = LocalDate.now();
        Page<BillingSurchargeDto> result = billingService.findUserSurcharges(date, 0, 10);
        assertEquals(2, result.getContent().size());
        assertThat(
                "only valid surcharges",
                result.getContent().stream().map(BillingSurchargeDto::getId).collect(Collectors.toList()),
                containsInAnyOrder(surcharge1.getId(), surcharge2.getId())
        );

        Page<BillingSurchargeDto> resultPage2 = billingService.findUserSurcharges(date, 1, 10);
        assertEquals(0, resultPage2.getContent().size());
    }

    @NotNull
    private Surcharge getSurcharge(
            SurchargeValidationStatus validationStatus,
            Long companyId,
            Long userId
    ) {
        String id = UUID.randomUUID().toString();
        return new Surcharge(
                id,
                validationStatus,
                null,
                SurchargeResolution.COMMIT,
                "type",
                "cargo-type",
                LocalDate.of(2022, 1, 1),
                companyId,
                SortingCenter.DEFAULT_SC_ID,
                userId,
                null,
                null,
                1,
                "ticket",
                id + "changelog"
        );
    }

    private List<TplRegion> getRegions() {
        TplRegion MOSCOW = TplRegion.builder()
                .id(MOSCOW_REG_ID)
                .name("Москва")
                .englishName("MOSCOW")
                .tzOffset(3)
                .latitude(-1)
                .longitude(-1)
                .parentId(MOSCOW_OBL_REG_ID)
                .type(CITY)
                .build();
        TplRegion MOSCOW_OBL = TplRegion.builder()
                .id(MOSCOW_OBL_REG_ID)
                .name("Москва")
                .englishName("Moscow ")
                .tzOffset(3)
                .latitude(-1)
                .longitude(-1)
                .parentId(MOSCOW_OBL_REG_ID)
                .type(SUBJECT_FEDERATION).build();
        return List.of(MOSCOW, MOSCOW_OBL);

    }

    private BillingSortingCenterContainerDto getSortingCenters() {
        return BillingSortingCenterContainerDto.builder()
                .sortingCenters(List.of(
                        BillingSortingCenterDto.builder()
                                .id(1L)
                                .name("СЦ ГОРОД Х")
                                .regionId((long) MOSCOW_REG_ID)
                                .build()
                        )
                )
                .build();
    }

    @Test
    void getMultiOrderForLockerDelivery() {
        Order order2 = createOrder();
        createOrder();
        UserShift userShift2 = createUserShift(order2, 136);
        namedParameterJdbcTemplate.update("" +
                "update task_order_delivery " +
                "set multi_order_id = null " +
                "where id = :id", Map.of("id", userShift2.getDeliveryTaskForOrder(order2.getId()).getId()));

        namedParameterJdbcTemplate.update("" +
                        "insert into task_locker_delivery (id) values (:id) ",
                Map.of("id", userShift2.getDeliveryTaskForOrder(order2.getId()).getId()));

        BillingOrderContainerDto orders2 = billingService.findOrders(Set.of(userShift2.getId()));
        assertEquals("pickup" + userShift2.getDeliveryTaskForOrder(order2.getId()).getId(),
                orders2.getOrders().get(0).getMultiOrderId(), "multiOrderId");

        BillingOrderContainerDto orders3 = billingService.findOrders(Set.of(userShift2.getId()));
        assertEquals("pickup" + userShift2.getDeliveryTaskForOrder(order2.getId()).getId(),
                orders3.getOrders().get(0).getMultiOrderId(), "multiOrderId");
    }

    @Test
    void findOrderWithFashionAndPartialReturn() {
        //create fashion order
        var order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .items(OrderGenerateService.OrderGenerateParam.Items.builder().isFashion(true).build())
                .build()
        );
        var userShift = createUserShift(order, 136);

        billingService.findOrders(Set.of(userShift.getId())).getOrders().forEach(o -> {
            assertEquals(o.getFashion(), true);
            assertEquals(o.getPartialReturnAllowed(), true);
            assertEquals(o.getPartialReturned(), false);
        });

        partialReturnOrderGenerateService.generatePartialReturnWithOnlyOneReturnItemInstance(order);

        billingService.findOrders(Set.of(userShift.getId())).getOrders().forEach(o -> {
            assertEquals(o.getFashion(), true);
            assertEquals(o.getPartialReturnAllowed(), true);
            assertEquals(o.getPartialReturned(), true);
        });
    }

    @Test
    void findShiftsWithFashionOrder() {
        //create fashion order
        var order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .items(OrderGenerateService.OrderGenerateParam.Items.builder().isFashion(true).build())
                .build()
        );
        createUserShift(order, 136);

        billingService.findShifts(LocalDate.now()).getUserShifts().forEach(
                us -> {
                    assertEquals(us.getTakenFashionOrderCount(), 1);
                    assertEquals(us.getTakenOrderCount(), 1);
                }
        );
    }

    private Order createOrder() {
        return orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .places(List.of(
                        OrderPlaceDto.builder()
                                .dimensions(new Dimensions(BigDecimal.ONE, 10, 15, 20))
                                .build()
                        )
                )
                .deliveryInterval(LocalTimeInterval.valueOf("11:00-15:00"))
                .build()
        );
    }

    private Movement createMovement() {
        return movementGenerator.generate(MovementCommand.Create.builder()
                .deliveryServiceId(-1L)
                .build());
    }

    private UserShift createUserShift(Order order, long uid) {
        var user = userHelper.findOrCreateUser(uid);

        return userHelper.createShiftWithDeliveryTask(user, UserShiftStatus.SHIFT_OPEN, order);
    }

    private void generateAndAssignVehicleToUserShift(UserShift userShift, String brand, String model,
                                                     String registrationNumber) {
        var vehicle = vehicleGenerateService.generateVehicle(brand, model);
        additionalVehicleDataService.updateVehicleData(userShift.getUser(), UpdateAdditionalVehicleDataDto.builder()
                .userShiftId(userShift.getId())
                .vehicleDataDto(UpdateAdditionalVehicleDataDto.UpdateVehicleDataDto.builder()
                        .vehicle(vehicle)
                        .vehicleColor(vehicleGenerateService.generateVehicleColor("black"))
                        .registrationNumber(registrationNumber)
                        .registrationNumberRegion("77")
                        .build()
                )
                .build());


    }

    private void generateAndAssignBicycleVehicleToUserShift(UserShift userShift, String brand, String model) {
        var vehicle = vehicleGenerateService.generateBicycleVehicle(brand, model);
        additionalVehicleDataService.updateVehicleData(userShift.getUser(), UpdateAdditionalVehicleDataDto.builder()
                .userShiftId(userShift.getId())
                .vehicleDataDto(UpdateAdditionalVehicleDataDto.UpdateVehicleDataDto.builder()
                        .vehicle(vehicle)
                        .registrationNumber(BICYCLE_NUMBER)
                        .registrationNumberRegion(NUMBER_REGION)
                        .build()
                )
                .build());


    }


    private void addCargoTask(Movement movement, long userShiftId, long pickupPointId, boolean returnB) {
        userShiftCommandService.addDeliveryTask(
                null,
                new UserShiftCommand.AddDeliveryTask(
                       userShiftId,
                        NewDeliveryRoutePointData.builder()
                                .cargoReference(
                                        CargoReference.builder()
                                                .movementId(movement.getId())
                                                .isReturn(returnB)
                                                .build()
                                )
                                .address(CollectDropshipTaskFactory.fromWarehouseAddress(movement.getWarehouseTo().getAddress()))
                                .name(movement.getWarehouseTo().getAddress().getAddress())
                                .expectedArrivalTime(DateTimeUtil.todayAtHour(12, clock))
                                .type(RoutePointType.LOCKER_DELIVERY)
                                .pickupPointId(pickupPointId)
                                .build(),
                        SimpleStrategies.BY_DATE_INTERVAL_MERGE
                )
        );
    }
}
