package ru.yandex.market.sc.api.controller;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.sc.api.domain.control.AppControl;
import ru.yandex.market.sc.api.test.ScApiControllerTest;
import ru.yandex.market.sc.api.utils.TestControllerCaller;
import ru.yandex.market.sc.core.domain.flow.repository.FlowSystemName;
import ru.yandex.market.sc.core.domain.operation.repository.OperationSystemName;
import ru.yandex.market.sc.core.domain.operation_log.repository.OperationLogRepository;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.user.model.UserPermission;
import ru.yandex.market.sc.core.domain.user.model.UserRole;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.domain.user.repository.UserProperty;
import ru.yandex.market.sc.core.domain.user.repository.UserPropertySource;
import ru.yandex.market.sc.core.domain.user_auth_log.UserAuthQRCodeService;
import ru.yandex.market.sc.core.domain.user_auth_log.model.AuthOperationType;
import ru.yandex.market.sc.core.domain.user_auth_log.model.LoginUserDto;
import ru.yandex.market.sc.core.domain.user_auth_log.model.LogoutUserDto;
import ru.yandex.market.sc.core.domain.user_auth_log.model.UserAuthResult;
import ru.yandex.market.sc.core.domain.user_auth_log.repository.UserAuthLog;
import ru.yandex.market.sc.core.domain.user_auth_log.repository.UserAuthLogRepository;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.sc.core.domain.process.ProcessQueryService.COMMON_PROCESS_SYSTEM_NAME;
import static ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey.SC_OPERATION_MODE_WITH_ZONE_ENABLED;
import static ru.yandex.market.sc.core.domain.user.model.UserPermission.CAN_CHANGE_CELL_FILLED_STATUS;
import static ru.yandex.market.sc.core.domain.user.model.UserPermission.CAN_CHANGE_SC;
import static ru.yandex.market.sc.core.domain.user.model.UserPermission.CAN_CREATE_PASSWORD;
import static ru.yandex.market.sc.core.domain.user.model.UserPermission.CAN_MOVE_ORDERS;
import static ru.yandex.market.sc.core.domain.user.model.UserPermission.CAN_SHOW_ADDITIONAL_PRINT_TASK_INFO;
import static ru.yandex.market.sc.core.domain.user.model.UserPermission.CAN_SKIP_2FA;
import static ru.yandex.market.sc.core.domain.user.model.UserPermission.CAN_SKIP_SORTING;
import static ru.yandex.market.sc.core.util.Constants.Header.SC_ZONE;

@ScApiControllerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class UserControllerTest {

    @MockBean
    Clock clock;

    private final MockMvc mockMvc;
    private final TestFactory testFactory;
    private final JdbcTemplate jdbcTemplate;
    private final UserPropertySource userPropertySource;
    private final OperationLogRepository operationLogRepository;

    private TestControllerCaller caller;

    private final UserAuthQRCodeService userAuthQRCodeService;

    private final UserAuthLogRepository userAuthLogRepository;


    private final String checkUserJsonTemplate = """
            {
                "sortingCenterId": %d,
                "sortingCenter": {
                    "id": %d,
                    "name": "%s"
                },
                "properties": %s,
                "role": "%s",
                "controls": %s,
                "settings": %s
            }""";

    SortingCenter sortingCenter;
    User user;
    User supportUser;
    List<AppControl> defaultAppControls;

    @BeforeEach
    void init() throws JsonProcessingException {
        testFactory.setupMockClock(clock);
        sortingCenter = testFactory.storedSortingCenter();
        user = testFactory.getOrCreateStoredUser(sortingCenter);
        supportUser = testFactory.getOrCreateSupportStoredUser(sortingCenter);
        caller = TestControllerCaller.createCaller(mockMvc);
        defaultAppControls = List.of(
                AppControl.InitialAcceptance,
                AppControl.SortingOrders,
                AppControl.AcceptReturns,
                AppControl.CourierDispatch,
                AppControl.WarehouseDispatch,
                AppControl.Inventorying,
                AppControl.Settings
        );
    }

    @Test
    void checkUser() throws Exception {
        mockMvc.perform(buildCheckUserRequest(user))
                .andExpect(status().isOk())
                .andExpect(content().json(String.format(checkUserJsonTemplate,
                                sortingCenter.getId(), sortingCenter.getId(), sortingCenter.getScName(),
                                "{}", user.getRole(), jsonFromEnumArray(defaultAppControls),
                                jsonFromEnumMap(Map.of(UserPermission.USE_HARD_LOGOUT, true))),
                        true));
    }

    @Test
    void checkUserTheDeprecatedWay() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/checkUser")
                        .header("Authorization", "OAuth uid-" + user.getUid())
                ).andExpect(status().isOk())
                .andExpect(content().json(String.format(checkUserJsonTemplate,
                        sortingCenter.getId(), sortingCenter.getId(), sortingCenter.getScName(),
                        "{}", user.getRole(), jsonFromEnumArray(defaultAppControls),
                        jsonFromEnumMap(Map.of(UserPermission.USE_HARD_LOGOUT, true))), true));
    }

    @Test
    void checkUserWithValidAppControlsInDB() throws Exception {
        List<AppControl> expected = List.of(AppControl.Palletization);
        testFactory.setSortingCenterProperty(
                sortingCenter.getId(),
                SortingCenterPropertiesKey.APP_CONTROL_ELEMENTS,
                new ObjectMapper().writeValueAsString(expected)
        );
        ResultActions resultActions = mockMvc.perform(buildCheckUserRequest(user));
        checkResultActionsAppControls(expected, resultActions);
    }

    @Test
    void checkUserWithUnparsableAppControlsInDB() throws Exception {
        testFactory.setSortingCenterProperty(
                sortingCenter.getId(),
                SortingCenterPropertiesKey.APP_CONTROL_ELEMENTS,
                "invalidJson"
        );
        ResultActions resultActions = mockMvc.perform(buildCheckUserRequest(user));
        checkResultActionsAppControls(defaultAppControls, resultActions);
    }

    @Test
    void checkUserWithInvalidAppControlsInDB() throws Exception {
        var appControls = List.of(AppControl.Palletization.name(), "invalid1", "invalid2");
        testFactory.setSortingCenterProperty(
                sortingCenter.getId(),
                SortingCenterPropertiesKey.APP_CONTROL_ELEMENTS,
                new ObjectMapper().writeValueAsString(appControls)
        );
        var validAppControls = List.of(appControls.get(0));
        mockMvc.perform(buildCheckUserRequest(user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.controls[0]", is(validAppControls.get(0))));
    }

    @Test
    void checkAppControlsConditions() throws Exception {
        List<SortingCenterPropertiesKey> sortingCenterPropertiesKeys;
        List<AppControl> expectedAppControls;

        sortingCenterPropertiesKeys = List.of();
        expectedAppControls = defaultAppControls;
        checkAppControls(sortingCenterPropertiesKeys, expectedAppControls);

        sortingCenterPropertiesKeys = List.of(SortingCenterPropertiesKey.XDOC_ENABLED);
        expectedAppControls = List.of(AppControl.XDocAcceptance, AppControl.XDocAcceptanceFinish,
                AppControl.XdocSorting,
                AppControl.XdocFixInbound, AppControl.XdocPrepareLot, AppControl.XdocLotInventory,
                AppControl.XdocOutboundTasks, AppControl.Settings);
        checkAppControls(sortingCenterPropertiesKeys, expectedAppControls);

        sortingCenterPropertiesKeys = List.of(SortingCenterPropertiesKey.PRE_SHIP_ENABLED);
        expectedAppControls = List.of(AppControl.InitialAcceptance, AppControl.SortingOrders,
                AppControl.AcceptReturns, AppControl.PrepareDispatch,
                AppControl.CourierDispatch, AppControl.WarehouseDispatch, AppControl.Inventorying,
                AppControl.Settings);
        checkAppControls(sortingCenterPropertiesKeys, expectedAppControls);

        sortingCenterPropertiesKeys = List.of(SortingCenterPropertiesKey.PRE_RETURN_ENABLED);
        expectedAppControls = List.of(AppControl.InitialAcceptance, AppControl.SortingOrders,
                AppControl.AcceptReturns, AppControl.PrepareLot,
                AppControl.Palletization, AppControl.CourierDispatch, AppControl.WarehouseDispatch,
                AppControl.Inventorying, AppControl.Settings);
        checkAppControls(sortingCenterPropertiesKeys, expectedAppControls);

        sortingCenterPropertiesKeys = List.of(SortingCenterPropertiesKey.PRE_RETURN_ENABLED,
                SortingCenterPropertiesKey.XDOC_ENABLED);
        expectedAppControls = List.of(AppControl.PrepareLot, AppControl.Palletization, AppControl.XDocAcceptance,
                AppControl.XDocAcceptanceFinish, AppControl.XdocSorting, AppControl.XdocFixInbound,
                AppControl.XdocPrepareLot, AppControl.XdocLotInventory, AppControl.XdocOutboundTasks,
                AppControl.Settings);
        checkAppControls(sortingCenterPropertiesKeys, expectedAppControls);

        sortingCenterPropertiesKeys = List.of(SortingCenterPropertiesKey.IS_DROPOFF);
        expectedAppControls = List.of(AppControl.SortingOrders, AppControl.AcceptReturns, AppControl.Palletization,
                AppControl.CourierDispatch, AppControl.WarehouseDispatch, AppControl.Settings);
        checkAppControls(sortingCenterPropertiesKeys, expectedAppControls);

        sortingCenterPropertiesKeys = List.of(SortingCenterPropertiesKey.XDOC_ENABLED,
                SortingCenterPropertiesKey.IS_DROPOFF);
        expectedAppControls = List.of(AppControl.XDocAcceptance, AppControl.XDocAcceptanceFinish,
                AppControl.XdocSorting,
                AppControl.XdocFixInbound, AppControl.XdocPrepareLot, AppControl.XdocLotInventory,
                AppControl.XdocOutboundTasks, AppControl.Settings);
        checkAppControls(sortingCenterPropertiesKeys, expectedAppControls);

        sortingCenterPropertiesKeys = List.of(SortingCenterPropertiesKey.BUFFER_RETURNS_ENABLED);
        expectedAppControls = List.of(AppControl.InitialAcceptance, AppControl.SortingOrders,
                AppControl.AcceptReturns, AppControl.CourierDispatch,
                AppControl.WarehouseDispatch, AppControl.CollectRoute, AppControl.Inventorying,
                AppControl.Settings);
        checkAppControls(sortingCenterPropertiesKeys, expectedAppControls);
    }

    @Test
    void checkUserWithUserProperty() throws Exception {
        userPropertySource.addBooleanUserProperty(user.getId(), UserProperty.PRE_SHIP_ENABLED, true);
        mockMvc.perform(buildCheckUserRequest(user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.properties", is(Map.of("pre_ship_enabled", true))));
    }

    @Test
    void checkUserWithUnknownSortingCenterProperties() throws Exception {
        jdbcTemplate.update("""
                        insert into sorting_center_property (created_at, updated_at, sorting_center_id, key, value)
                        values (now(), now(), ?, ?, ?)
                        """,
                sortingCenter.getId(),
                "_INVALID_PROPERTY_FOR_checkUserWithUnknownSortingCenterProperties_TEST_",
                true
        );
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.XDOC_ENABLED, true);
        mockMvc.perform(buildCheckUserRequest(user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.properties", is(Map.of("xdoc_enabled", true))));
    }

    @Test
    void checkUserWithLogoutAutomaticallyProperty() throws Exception {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.LOGOUT_AUTOMATICALLY, true);
        mockMvc.perform(buildCheckUserRequest(user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.properties", is(Map.of("logout_automatically", true))));
    }

    @Test
    void checkUserWithUserAndSortingCenterProperty() throws Exception {
        userPropertySource.addBooleanUserProperty(user.getId(), UserProperty.PRE_SHIP_ENABLED, true);
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.XDOC_ENABLED, true);

        mockMvc.perform(buildCheckUserRequest(user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.properties", is(Map.of("pre_ship_enabled", true, "xdoc_enabled", true))));
    }

    @Test
    void userCheckWithUserProperty() throws Exception {
        userPropertySource.addBooleanUserProperty(user.getId(), UserProperty.PRE_SHIP_ENABLED, true);
        mockMvc.perform(buildCheckUserRequest(user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.properties", is(Map.of("pre_ship_enabled", true))));
    }

    @Test
    void userChangeSortingCenter() throws Exception {
        var sortingCenter2 = testFactory.storedSortingCenter(1234L);
        mockMvc.perform(
                MockMvcRequestBuilders.put("/api/users/changeSortingCenter")
                        .header("Authorization", "OAuth uid-" + supportUser.getUid())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scId\": " + sortingCenter2.getId() + "}")
        ).andExpect(status().isOk());
    }

    @Test
    void userCanNotMockSortingCenter() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.put("/api/users/changeSortingCenter")
                        .header("Authorization", "OAuth uid-" + user.getUid())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scId\": " + user.getSortingCenter().getId() + "}")
        ).andExpect(status().is4xxClientError());
    }

    @Test
    void supportUserMockSortingCenter() throws Exception {
        var sortingCenter2 = testFactory.storedSortingCenter(1234L);
        mockMvc.perform(
                        MockMvcRequestBuilders.put("/api/users/changeSortingCenter")
                                .header("Authorization", "OAuth uid-" + supportUser.getUid())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"scId\": " + sortingCenter2.getId() + "}")
                )
                .andExpect(status().isOk());
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/users/check")
                                .header("Authorization", "OAuth uid-" + supportUser.getUid())
                )
                .andExpect(status().isOk())
                .andExpect(content().json(String.format(checkUserJsonTemplate,
                        sortingCenter2.getId(), sortingCenter2.getId(), sortingCenter2.getScName(),
                        "{}", supportUser.getRole(), jsonFromEnumArray(defaultAppControls),
                        jsonFromEnumMap(Map.of(
                                        CAN_MOVE_ORDERS, Boolean.TRUE,
                                        CAN_CHANGE_SC, Boolean.TRUE,
                                        CAN_CREATE_PASSWORD, Boolean.TRUE,
                                        CAN_SKIP_SORTING, Boolean.TRUE,
                                        CAN_SHOW_ADDITIONAL_PRINT_TASK_INFO, Boolean.TRUE,
                                        CAN_CHANGE_CELL_FILLED_STATUS, Boolean.TRUE,
                                        CAN_SKIP_2FA, Boolean.TRUE
                                )
                        )
                ), true));
    }

    @Test
    @SneakyThrows
    @DisplayName("Включён режим зон")
    void userCheckReturnFlagZoneMode() {
        testFactory.setSortingCenterProperty(sortingCenter, SC_OPERATION_MODE_WITH_ZONE_ENABLED, true);
        mockMvc.perform(buildCheckUserRequest(user))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.properties",
                        hasEntry(SC_OPERATION_MODE_WITH_ZONE_ENABLED.name().toLowerCase(), true)));
    }

    @Test
    @SneakyThrows
    @DisplayName("Кнопка CommonInfo доступна только Старшему кладовщику и старшему смены")
    public void userCheckCommonInfoButton() {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.COMMON_INFO_ENABLED, true);
        mockMvc.perform(buildCheckUserRequest(user))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.controls",
                        not(hasItem(AppControl.CommonInfo.name()))));

        var seniorStockman = testFactory.storedUser(sortingCenter, 11L, UserRole.SENIOR_STOCKMAN);
        mockMvc.perform(buildCheckUserRequest(seniorStockman))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.controls",
                        hasItem(AppControl.CommonInfo.name())));

        var masterStockman = testFactory.storedUser(sortingCenter, 22L, UserRole.MASTER_STOCKMAN);
        mockMvc.perform(buildCheckUserRequest(masterStockman))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.controls",
                        hasItem(AppControl.CommonInfo.name())));
    }

    @Test
    @SneakyThrows
    @DisplayName("Кнопка CommonInfo приходит первой")
    public void userCheckCommonInfoFirst() {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.COMMON_INFO_ENABLED, true);

        var seniorStockman = testFactory.storedUser(sortingCenter, 11L, UserRole.SENIOR_STOCKMAN);
        mockMvc.perform(buildCheckUserRequest(seniorStockman))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.controls[0]", is(AppControl.CommonInfo.name())));

    }

    @Test
    @SneakyThrows
    @DisplayName("success 2fa login user")
    public void user2faLoginTest() {
        var seniorStockman = testFactory.storedUser(sortingCenter, 11L, UserRole.SENIOR_STOCKMAN);
        var password = userAuthQRCodeService
                .encryptAuthCode(seniorStockman.getSortingCenter().getId(), Instant.now(clock).getEpochSecond());
        caller.loginUser(seniorStockman, new LoginUserDto(password, null))
                .andExpect(status().is2xxSuccessful());

        var password2 = userAuthQRCodeService
                .encryptAuthCode(seniorStockman.getSortingCenter().getId(), Instant.now(clock).getEpochSecond() - 3);
        caller.loginUser(seniorStockman, new LoginUserDto(password2, null))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    @SneakyThrows
    @DisplayName("fails 2fa login user")
    public void userWrong2faLoginTest() {
        var seniorStockman = testFactory.storedUser(sortingCenter, 11L, UserRole.SENIOR_STOCKMAN);
        var password = userAuthQRCodeService
                .encryptAuthCode(seniorStockman.getSortingCenter().getId(), Instant.now(clock).getEpochSecond() - 10);
        caller.loginUser(seniorStockman, new LoginUserDto(password, "tsd-serial"))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.message")
                        .value("Сканируемый QR код просрочен. Отсканируйте новый."));

        var password2 = userAuthQRCodeService
                .encryptAuthCode(seniorStockman.getSortingCenter().getId(), Instant.now(clock).getEpochSecond() + 10);
        caller.loginUser(seniorStockman, new LoginUserDto(password2, "tsd-serial"))
                .andExpect(status().is4xxClientError()).andExpect(jsonPath("$.message")
                        .value("Сканируемый QR код просрочен. Отсканируйте новый."));

        var password3 = userAuthQRCodeService
                .encryptAuthCode(seniorStockman.getSortingCenter().getId() + 1, Instant.now(clock).getEpochSecond());
        caller.loginUser(seniorStockman, new LoginUserDto(password3, "tsd-serial"))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.message")
                        .value("Пользователь находится на неправильном СЦ"));
        var password4 = "password";
        caller.loginUser(seniorStockman, new LoginUserDto(password4, "tsd-serial"))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.message")
                        .value("Для текущей операции сканируется некорректный QR код"));
    }

    @Test
    @SneakyThrows
    @DisplayName("simple validate login user")
    public void userLoginTest() {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.REQUIRE_VALIDATE_SC_ID, true);
        var seniorStockman = testFactory.storedUser(sortingCenter, 11L, UserRole.SENIOR_STOCKMAN);
        var password = seniorStockman.getSortingCenter().getId().toString();
        var wrongPassword = String.valueOf(seniorStockman.getSortingCenter().getId() + 1);

        caller.loginUser(seniorStockman, new LoginUserDto(password, "tsd-serial"))
                .andExpect(status().isOk());

        caller.loginUser(seniorStockman, new LoginUserDto(wrongPassword, "tsd-serial"))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.message")
                        .value("Для текущей операции сканируется некорректный QR код"));
    }

    @Test
    @SneakyThrows
    @DisplayName("проверка записи tsd serial в лог после успешного логина")
    public void successUserLoginCheckLogger() {
        testFactory.setSortingCenterProperty(
                sortingCenter, SortingCenterPropertiesKey.REQUIRE_VALIDATE_SC_ID, true
        );
        var seniorStockman = testFactory.storedUser(sortingCenter, 11L, UserRole.SENIOR_STOCKMAN);
        var password = seniorStockman.getSortingCenter().getId().toString();
        var tsdSerial = "tsd-serial-test-login-success___";
        caller.loginUser(seniorStockman, new LoginUserDto(password, tsdSerial))
                .andExpect(status().isOk());
        UserAuthLog authLog = userAuthLogRepository.findByTsdSerial(tsdSerial).get(0);
        assertEquals(UserAuthResult.OK, authLog.getResult());
        assertEquals(AuthOperationType.LOGIN, authLog.getAuthType());
        assertEquals(seniorStockman, authLog.getUser());
    }

    @Test
    @SneakyThrows
    @DisplayName("проверка записи tsd serial в лог после ошибки логина")
    public void failUserLoginCheckLogger() {
        testFactory.setSortingCenterProperty(
                sortingCenter, SortingCenterPropertiesKey.REQUIRE_VALIDATE_SC_ID, true
        );
        var seniorStockman = testFactory.storedUser(sortingCenter, 11L, UserRole.SENIOR_STOCKMAN);
        var wrongPassword = String.valueOf(seniorStockman.getSortingCenter().getId() + 1);
        var tsdSerial = "tsd-serial-test-login-fail___";
        caller.loginUser(seniorStockman, new LoginUserDto(wrongPassword, tsdSerial))
                .andExpect(status().is4xxClientError());
        UserAuthLog authLog = userAuthLogRepository.findByTsdSerial(tsdSerial).get(0);
        assertEquals(AuthOperationType.LOGIN, authLog.getAuthType());
        assertEquals(UserAuthResult.ERROR, authLog.getResult());
        assertEquals(seniorStockman, authLog.getUser());
    }

    @Test
    @SneakyThrows
    @DisplayName("проверка записи tsd serial при логауте")
    public void userLogoutCheckLogger() {
        testFactory.setSortingCenterProperty(
                sortingCenter, SortingCenterPropertiesKey.REQUIRE_VALIDATE_SC_ID, true
        );
        var seniorStockman = testFactory.storedUser(sortingCenter, 11L, UserRole.SENIOR_STOCKMAN);
        var tsdSerial = "tsd-serial-test-logout___";
        caller.logoutUser(seniorStockman, new LogoutUserDto(AuthOperationType.MANUAL_LOGOUT, tsdSerial))
                .andExpect(status().isOk());
        UserAuthLog authLog = userAuthLogRepository.findByTsdSerial(tsdSerial).get(0);
        assertEquals(UserAuthResult.OK, authLog.getResult());
        assertEquals(AuthOperationType.MANUAL_LOGOUT, authLog.getAuthType());
        assertEquals(seniorStockman, authLog.getUser());
    }

    @Test
    @SneakyThrows
    @DisplayName("Проверка записи в operation_log выхода из зоны при логауте")
    public void userLogoutCheckLeaveZone() {
        testFactory.setSortingCenterProperty(sortingCenter, SC_OPERATION_MODE_WITH_ZONE_ENABLED, true);
        var op = testFactory.storedOperation(OperationSystemName.ZONE_LEAVE.name());
        var flow = testFactory.storedFlow(FlowSystemName.COMMON.name(), FlowSystemName.COMMON.name(), List.of(op));
        var process = testFactory.storedProcess(COMMON_PROCESS_SYSTEM_NAME, COMMON_PROCESS_SYSTEM_NAME, List.of(flow));
        var zone = testFactory.storedZone(sortingCenter, "zn-1", process);
        var seniorStockman = testFactory.storedUser(sortingCenter, 11L, UserRole.SENIOR_STOCKMAN);
        var tsdSerial = "tsd-serial-test-logout___";

        var headers = new HttpHeaders();
        headers.add(SC_ZONE, zone.getId().toString());
        caller.logoutUser(seniorStockman, new LogoutUserDto(AuthOperationType.MANUAL_LOGOUT, tsdSerial), headers)
                .andExpect(status().isOk());

        assertThat(operationLogRepository.findAll())
                .filteredOn(ol -> ol.getOperationId().equals(op.getId()))
                .extracting("zoneId", "operationId")
                .contains(tuple(zone.getId(), op.getId()));
    }

    private MockHttpServletRequestBuilder buildCheckUserRequest(User user) {
        return MockMvcRequestBuilders.get("/api/users/check")
                .header("Authorization", "OAuth uid-" + user.getUid());
    }

    private MockHttpServletRequestBuilder buildLoginUserRequest(User user, String password) {
        return MockMvcRequestBuilders.get("/api/users/login")
                .header("Authorization", "OAuth uid-" + user.getUid());
    }

    private String jsonFromEnumArray(List<AppControl> defaultAppControls) throws Exception {
        return new ObjectMapper().writeValueAsString(defaultAppControls);
    }

    private <K extends Enum<?>, V> String jsonFromEnumMap(Map<K, V> map) throws Exception {
        return new ObjectMapper().writeValueAsString(map);
    }

    private void checkAppControls(List<SortingCenterPropertiesKey> keys, List<AppControl> expected) throws Exception {
        jdbcTemplate.update("delete from sorting_center_property where sorting_center_id = ?", sortingCenter.getId());
        keys.forEach((key) -> testFactory.setSortingCenterProperty(sortingCenter, key, true));
        ResultActions resultActions = mockMvc.perform(buildCheckUserRequest(user));
        checkResultActionsAppControls(expected, resultActions);
    }

    private void checkResultActionsAppControls(List<AppControl> expected,
                                               ResultActions resultActions) throws Exception {
        resultActions.andExpect(status().isOk());
        resultActions.andExpect(jsonPath("$.controls", hasSize(expected.size())));
        for (int i = 0; i < expected.size(); i++) {
            resultActions.andExpect(jsonPath(String.format("$.controls[%d]", i), is(expected.get(i).name())));
        }
    }

}
