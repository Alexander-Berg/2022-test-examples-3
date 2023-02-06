package ru.yandex.market.tpl.carrier.driver.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.carrier.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.RunHelper;
import ru.yandex.market.tpl.carrier.core.domain.run.RunRepository;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.transport.TestTransportTypeHelper.TransportTypeGenerateParam;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.user.UserUtil;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.EcologicalClass;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.Transport;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouse;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseGenerator;
import ru.yandex.market.tpl.carrier.driver.BaseDriverApiIntTest;
import ru.yandex.market.tpl.common.util.DateTimeUtil;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class ShiftControllerIntTest extends BaseDriverApiIntTest {

    private final TestUserHelper testUserHelper;

    private final RunGenerator runGenerator;
    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final RunHelper runHelper;
    private final RunRepository runRepository;
    private final TransactionTemplate transactionTemplate;
    private final ConfigurationServiceAdapter configurationServiceAdapter;

    private User user;
    private Run run;
    private UserShift userShift;

    private String firstRoutePointName;
    private String lastRoutePointName;

    @BeforeEach
    void setUp() {
        Company company = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);

        TransportTypeGenerateParam transportTypeGenerateParam = TransportTypeGenerateParam.builder()
                .name(TestUserHelper.DEFAULT_TRANSPORT_NAME)
                .capacity(BigDecimal.ZERO)
                .palletsCapacity(33)
                .company(company)
                .grossWeightTons(new BigDecimal("2.0"))
                .maxLoadOnAxleTons(new BigDecimal("4.0"))
                .maxWeightTons(new BigDecimal("8.0"))
                .lengthMeters(new BigDecimal("3"))
                .heightMeters(new BigDecimal("4"))
                .widthMeters(new BigDecimal("5"))
                .ecologicalClass(EcologicalClass.EURO5)
                .build();
        Transport transport = testUserHelper.findOrCreateTransport(TestUserHelper.DEFAULT_TRANSPORT_NAME,
                transportTypeGenerateParam, company);
        user = testUserHelper.findOrCreateUser(UID);

        OrderWarehouse firstWarehouse = orderWarehouseGenerator.generateWarehouse();
        OrderWarehouse secondWarehouse = orderWarehouseGenerator.generateWarehouse();
        firstRoutePointName = firstWarehouse.getRoutePointName();
        lastRoutePointName = secondWarehouse.getRoutePointName();

        run = runGenerator.generate(RunGenerator.RunGenerateParam.builder()
                .externalId("asd")
                .deliveryServiceId(123L)
                .campaignId(company.getCampaignId())
                .runDate(LocalDate.of(2021, 8, 5))
                .item(new RunGenerator.RunItemGenerateParam(
                        MovementCommand.Create.builder()
                                .externalId("123")
                                .orderWarehouse(firstWarehouse)
                                .orderWarehouseTo(secondWarehouse)
                                .deliveryIntervalFrom(ZonedDateTime.of(2021, 8, 5, 4, 0, 0, 0,
                                        DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                .deliveryIntervalTo(ZonedDateTime.of(2021, 8, 5, 5, 0, 0, 0,
                                        DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                .weight(new BigDecimal("1000"))
                                .build(),
                        1,
                        null,
                        null
                ))
                .build()
        );

        userShift = runHelper.assignUserAndTransport(run, user, transport);
        testUserHelper.openShift(user, userShift.getId());

        configurationServiceAdapter.mergeValue(ConfigurationProperties.DRIVER_NEXT_SHIFTS_IN_INTERVAL_ENABLED, true);
        configurationServiceAdapter.mergeValue(ConfigurationProperties.DRIVER_NEXT_SHIFTS_IN_INTERVAL_LOOK_AHEAD_MINUTES, 12 * 60);
        configurationServiceAdapter.mergeValue(ConfigurationProperties.DRIVER_NEXT_SHIFTS_IN_INTERVAL_LOOK_BEHIND_MINUTES, -12 * 60);
    }

    @SneakyThrows
    @Test
    void shouldReturnUserInApiShiftsCurrent() {
        mockMvc.perform(get("/api/shifts/current")
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
        )
                .andExpect(status().isOk())
                .andExpect(ResultMatcher.matchAll(
                        jsonPath("$.user.firstName").value(UserUtil.FIRST_NAME),
                        jsonPath("$.user.lastName").value(UserUtil.LAST_NAME)
                ));
    }

    @SneakyThrows
    @Test
    void shouldReturnTasksSummaryInApiShiftsCurrent() {
        mockMvc.perform(get("/api/shifts/current")
                        .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                ).andExpect(status().isOk())
                .andExpect(ResultMatcher.matchAll(
                        jsonPath("$.collectNumber").value(1),
                        jsonPath("$.returnNumber").value(1),
                        jsonPath("$.firstRoutePointName").value(firstRoutePointName),
                        jsonPath("$.lastRoutePointName").value(lastRoutePointName)
                ));
    }

    @SneakyThrows
    @Test
    void shouldReturnRunAndCompanyInApiShiftsCurrent() {
        mockMvc.perform(get("/api/shifts/current")
                        .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                ).andExpect(status().isOk())
                .andExpect(ResultMatcher.matchAll(
                        jsonPath("$.runId").value(run.getId()),
                        jsonPath("$.companyName").value(Company.DEFAULT_COMPANY_NAME)
                ));
    }

    @SneakyThrows
    @Test
    void shouldReturnTransportInApiShiftsCurrent() {
        mockMvc.perform(get("/api/shifts/current")
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
        ).andExpect(status().isOk())
                .andExpect(ResultMatcher.matchAll(
                        jsonPath("$.transport").exists(),
                        jsonPath("$.transport").isMap(),
                        jsonPath("$.transport.number").value(TestUserHelper.DEFAULT_TRANSPORT_NUMBER),
                        jsonPath("$.transport.model").value("2114"),
                        jsonPath("$.transport.brand").value("ВАЗ"),
                        jsonPath("$.transport.actualWeightTons").value(new BigDecimal("2.0")),
                        jsonPath("$.transport.grossWeightTons").value(new BigDecimal("2.0")),
                        jsonPath("$.transport.maxLoadOnAxleTons").value(new BigDecimal("4.0")),
                        jsonPath("$.transport.maxWeightTons").value(new BigDecimal("8.0")),
                        jsonPath("$.transport.lengthMeters").value(new BigDecimal("3")),
                        jsonPath("$.transport.heightMeters").value(new BigDecimal("4")),
                        jsonPath("$.transport.widthMeters").value(new BigDecimal("5")),
                        jsonPath("$.transport.ecologicalClass").value(EcologicalClass.EURO5.name())
                ));
    }

    @SneakyThrows
    @Test
    void shouldReturnActualWeightTonsAfterFirstDropshipIsCompleted() {

        transactionTemplate.execute(tc -> {
            run = runRepository.findByIdOrThrow(run.getId());

            testUserHelper.finishCollectDropships(run.getFirstAssignedShift().streamCollectDropshipTasks().findFirst().orElseThrow().getRoutePoint());
            return null;
        });


        mockMvc.perform(get("/api/shifts/current")
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
        ).andExpect(status().isOk())
                .andExpect(ResultMatcher.matchAll(
                        jsonPath("$.transport").exists(),
                        jsonPath("$.transport").isMap(),
                        jsonPath("$.transport.number").value(TestUserHelper.DEFAULT_TRANSPORT_NUMBER),
                        jsonPath("$.transport.model").value("2114"),
                        jsonPath("$.transport.brand").value("ВАЗ"),
                        jsonPath("$.transport.actualWeightTons").value(new BigDecimal("3.0")),
                        jsonPath("$.transport.grossWeightTons").value(new BigDecimal("2.0")),
                        jsonPath("$.transport.maxLoadOnAxleTons").value(new BigDecimal("4.0")),
                        jsonPath("$.transport.maxWeightTons").value(new BigDecimal("8.0")),
                        jsonPath("$.transport.lengthMeters").value(new BigDecimal("3")),
                        jsonPath("$.transport.heightMeters").value(new BigDecimal("4")),
                        jsonPath("$.transport.widthMeters").value(new BigDecimal("5")),
                        jsonPath("$.transport.ecologicalClass").value(EcologicalClass.EURO5.name())
                ));
    }
}
