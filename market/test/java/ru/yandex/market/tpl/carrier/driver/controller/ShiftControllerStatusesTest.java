package ru.yandex.market.tpl.carrier.driver.controller;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.tpl.carrier.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.RunHelper;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.user.UserUtil;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.Transport;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.carrier.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseGenerator;
import ru.yandex.market.tpl.carrier.driver.BaseDriverApiIntTest;
import ru.yandex.market.tpl.carrier.driver.web.auth.ApiParams;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.tpl.carrier.core.domain.user.UserUtil.ANOTHER_UID;
import static ru.yandex.market.tpl.carrier.driver.api.model.shift.UserShiftStatus.NO_SHIFT;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class ShiftControllerStatusesTest extends BaseDriverApiIntTest {

    private final Clock clock;

    private final TestUserHelper testUserHelper;
    private final UserShiftCommandService userShiftCommandService;

    private final RunGenerator runGenerator;
    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final RunHelper runHelper;
    private final ConfigurationServiceAdapter configurationServiceAdapter;

    private User user;
    private User user2;
    private Transport transport;
    private Transport transport2;
    private Run run1;
    private Run run2;
    private Run run3;
    private UserShift userShift1;
    private UserShift userShift2;
    private UserShift userShift3;

    @BeforeEach
    void setUp() {
        Company company = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);
        transport = testUserHelper.findOrCreateTransport();
        transport2 = testUserHelper.findOrCreateTransport("transport-2", company.getName());
        user = testUserHelper.findOrCreateUser(UID);
        user2 = testUserHelper.findOrCreateUser(ANOTHER_UID, company.getName(), UserUtil.ANOTHER_PHONE);

        LocalDate runDate = LocalDate.now(clock);
        run1 = runGenerator.generate(RunGenerator.RunGenerateParam.builder()
                .externalId("asd1")
                .deliveryServiceId(123L)
                .campaignId(company.getCampaignId())
                .runDate(runDate.minusDays(2))
                .item(new RunGenerator.RunItemGenerateParam(
                        MovementCommand.Create.builder()
                                .externalId("1231")
                                .orderWarehouse(orderWarehouseGenerator.generateWarehouse())
                                .orderWarehouseTo(orderWarehouseGenerator.generateWarehouse())
                                .outboundArrivalTime(clock.instant().minus(48, ChronoUnit.HOURS))
                                .deliveryIntervalFrom(clock.instant().minus(48, ChronoUnit.HOURS))
                                .inboundArrivalTime(clock.instant().minus(48, ChronoUnit.HOURS))
                                .deliveryIntervalTo(clock.instant().minus(36, ChronoUnit.HOURS))
                                .weight(new BigDecimal("1000"))
                                .build(),
                        1,
                        null,
                        null
                ))
                .build()
        );

        run2 = runGenerator.generate(RunGenerator.RunGenerateParam.builder()
                .externalId("asd2")
                .deliveryServiceId(123L)
                .campaignId(company.getCampaignId())
                .runDate(runDate)
                .item(new RunGenerator.RunItemGenerateParam(
                        MovementCommand.Create.builder()
                                .externalId("1232")
                                .orderWarehouse(orderWarehouseGenerator.generateWarehouse())
                                .orderWarehouseTo(orderWarehouseGenerator.generateWarehouse())
                                .deliveryIntervalFrom(runDate.atTime(13, 0).toInstant(ZoneOffset.UTC))
                                .deliveryIntervalTo(runDate.atTime(14, 0).toInstant(ZoneOffset.UTC))
                                .weight(new BigDecimal("1000"))
                                .build(),
                        1,
                        null,
                        null
                ))
                .build()
        );

        run3 = runGenerator.generate(RunGenerator.RunGenerateParam.builder()
                .externalId("asd3")
                .deliveryServiceId(123L)
                .campaignId(company.getCampaignId())
                .runDate(runDate)
                .item(new RunGenerator.RunItemGenerateParam(
                        MovementCommand.Create.builder()
                                .externalId("12325")
                                .orderWarehouse(orderWarehouseGenerator.generateWarehouse())
                                .orderWarehouseTo(orderWarehouseGenerator.generateWarehouse())
                                .deliveryIntervalFrom(runDate.atTime(13, 0).toInstant(ZoneOffset.UTC))
                                .deliveryIntervalTo(runDate.atTime(14, 0).toInstant(ZoneOffset.UTC))
                                .weight(new BigDecimal("1000"))
                                .build(),
                        1,
                        null,
                        null
                ))
                .build()
        );

        userShift1 = runHelper.assignUserAndTransport(run1, user, transport);
        userShift2 = runHelper.assignUserAndTransport(run2, user, transport);
        userShift3 = runHelper.assignUserAndTransport(run3, user, transport);


        configurationServiceAdapter.mergeValue(ConfigurationProperties.DRIVER_NEXT_SHIFTS_IN_INTERVAL_ENABLED, true);
        configurationServiceAdapter.mergeValue(ConfigurationProperties.DRIVER_NEXT_SHIFTS_IN_INTERVAL_LOOK_AHEAD_MINUTES, 12 * 60);
        configurationServiceAdapter.mergeValue(ConfigurationProperties.DRIVER_NEXT_SHIFTS_IN_INTERVAL_LOOK_BEHIND_MINUTES, -12 * 60);
    }

    @SneakyThrows
    @Test
    void shouldForBackwardsCompatibilityReturnNextShiftInCreatedStatusIfNoOnTaskStatusPresent() {

        mockMvc.perform(get("/api/shifts/current")
                        .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                ).andExpect(status().isOk())
                .andExpect(ResultMatcher.matchAll(
                        jsonPath("$.id").value(userShift2.getId())
                ));
    }

    @SneakyThrows
    @Test
    void shouldReturnNextShift() {
        testUserHelper.openShift(user, userShift3.getId());

        mockMvc.perform(get("/api/shifts/current")
                        .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                ).andExpect(status().isOk())
                .andExpect(ResultMatcher.matchAll(
                        jsonPath("$.id").value(userShift3.getId())
                ));
    }

    @SneakyThrows
    @Test
    void shouldNotReturnFinishedShift() {
        testUserHelper.openShift(user, userShift2.getId());
        userShiftCommandService.closeUserShift(new UserShiftCommand.ForceClose(userShift2.getId()));

        testUserHelper.openShift(user, userShift3.getId());
        userShiftCommandService.closeUserShift(new UserShiftCommand.ForceClose(userShift3.getId()));

        mockMvc.perform(get("/api/shifts/current")
                        .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                ).andExpect(status().isOk())
                .andExpect(ResultMatcher.matchAll(
                        jsonPath("$.active").value(false),
                        jsonPath("$.['status']").value(NO_SHIFT.name())
                ));
    }

    @Test
    @SneakyThrows
    void shouldReturnNextShifts() {

        mockMvc.perform(
                get("/api/shifts")
                        .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
        ).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(Matchers.hasSize(2)));
    }


    @Test
    @SneakyThrows
    void shouldReturnCurrentShiftIfHasActive() {

        testUserHelper.openShift(user, userShift2.getId());

        mockMvc.perform(
                get("/api/shifts")
                        .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
        ).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(userShift2.getId()));
    }

    @Test
    @SneakyThrows
    void shouldReturnShiftsAfterReassigningUser() {

        testUserHelper.openShift(user, userShift2.getId());

        mockMvc.perform(get("/api/shifts")
                        .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                ).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(userShift2.getId()));

        userShift2 = runHelper.assignUserAndTransport(run2, user2, transport2);

        mockMvc.perform(get("/api/shifts")
                        .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                ).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(userShift3.getId()));
        ;

        mockMvc.perform(get("/driver/v1/market-carrier/shifts")
                        .header(ApiParams.TAXI_PARK_ID_HEADER, TAXI_PARK_ID_HEADER_VALUE)
                        .header(ApiParams.TAXI_PROFILE_ID_HEADER, TAXI_PROFILE_ID_HEADER_VALUE)
                        .header(ApiParams.TAXI_PASSPORT_UID_HEADER, String.valueOf(ANOTHER_UID))
                ).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(userShift2.getId()));
        ;
    }
}
