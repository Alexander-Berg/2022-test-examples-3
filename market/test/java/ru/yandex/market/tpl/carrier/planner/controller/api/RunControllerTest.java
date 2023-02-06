package ru.yandex.market.tpl.carrier.planner.controller.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.tuple.Pair;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.tpl.carrier.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.carrier.core.audit.CarrierAuditTracer;
import ru.yandex.market.tpl.carrier.core.audit.CarrierSource;
import ru.yandex.market.tpl.carrier.core.db.QueryCountAssertions;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementSubtype;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementType;
import ru.yandex.market.tpl.carrier.core.domain.registry.RegistryCommandService;
import ru.yandex.market.tpl.carrier.core.domain.registry.RegistryStatus;
import ru.yandex.market.tpl.carrier.core.domain.registry.command.RegistryCommand;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunCommandService;
import ru.yandex.market.tpl.carrier.core.domain.run.RunCommentScope;
import ru.yandex.market.tpl.carrier.core.domain.run.RunCommentSeverity;
import ru.yandex.market.tpl.carrier.core.domain.run.RunCommentType;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.RunHelper;
import ru.yandex.market.tpl.carrier.core.domain.run.RunMessageSource;
import ru.yandex.market.tpl.carrier.core.domain.run.RunMessageType;
import ru.yandex.market.tpl.carrier.core.domain.run.RunStatus;
import ru.yandex.market.tpl.carrier.core.domain.run.RunSubtype;
import ru.yandex.market.tpl.carrier.core.domain.run.RunType;
import ru.yandex.market.tpl.carrier.core.domain.run.commands.NewRunMessageData;
import ru.yandex.market.tpl.carrier.core.domain.run.commands.RunCommand;
import ru.yandex.market.tpl.carrier.core.domain.run.price_control.RunPriceStatus;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.user.UserUtil;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.Transport;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouse;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseGenerator;
import ru.yandex.market.tpl.carrier.planner.controller.BasePlannerWebTest;
import ru.yandex.mj.generated.server.model.NewPriceControlDto;
import ru.yandex.mj.generated.server.model.PriceControlPenaltyStatusDto;
import ru.yandex.mj.generated.server.model.PriceControlPenaltyTypeDto;
import ru.yandex.mj.generated.server.model.RunMessageTypeDto;
import ru.yandex.mj.generated.server.model.RunPriceInfoDto;
import ru.yandex.mj.generated.server.model.UpdatePriceControlDto;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
class RunControllerTest extends BasePlannerWebTest {

    static {
        CarrierAuditTracer.putSource(CarrierSource.SYSTEM);
    }

    private static final LocalDate DATE_FROM = LocalDate.of(2021, 11, 17);
    private static final LocalDate DATE_TO = LocalDate.of(2021, 11, 19);
    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final RunGenerator runGenerator;
    private final RunHelper runHelper;
    private final TestUserHelper testUserHelper;
    private final RunCommandService runCommandService;
    private final ObjectMapper tplObjectMapper;
    private final RegistryCommandService registryCommandService;

    private final ConfigurationServiceAdapter configurationServiceAdapter;


    private Run run;
    private Run run2;
    private OrderWarehouse warehouseA;
    private OrderWarehouse warehouseB;
    private OrderWarehouse warehouseC;
    private OrderWarehouse warehouseD;
    private OrderWarehouse warehouseE;
    private User user;
    private Transport transport;


    @BeforeEach
    void setUp() {
        warehouseA = orderWarehouseGenerator.generateWarehouse(wh -> {
            wh.setYandexId("1111");
        });
        warehouseB = orderWarehouseGenerator.generateWarehouse(wh -> {
            wh.setYandexId("2222");
        });
        warehouseC = orderWarehouseGenerator.generateWarehouse(wh -> {
            wh.setYandexId("3333");
        });
        warehouseD = orderWarehouseGenerator.generateWarehouse(wh -> {
            wh.setYandexId("4444");
        });

        user = testUserHelper.findOrCreateUser(123L, "88001231231", "Джексон", "Майкл", "Кужегетович");
        transport = testUserHelper.findOrCreateTransport();

        run = runGenerator.generate(b ->
                b.clearItems()
                        .externalId("a-1")
                        .item(new RunGenerator.RunItemGenerateParam(
                                MovementCommand.Create.builder()
                                        .orderWarehouse(warehouseA)
                                        .orderWarehouseTo(warehouseB)
                                        .deliveryIntervalFrom(Instant.now(clock).plus(1, ChronoUnit.DAYS))
                                        .inboundArrivalTime(Instant.now(clock).plus(1, ChronoUnit.DAYS))
                                        .inboundDepartureTime(Instant.now(clock).plus(1, ChronoUnit.DAYS).plus(1,
                                                ChronoUnit.HOURS))
                                        .type(MovementType.LINEHAUL)
                                        .build(),
                                1,
                                0,
                                3
                        ))
                        .item(new RunGenerator.RunItemGenerateParam(
                                MovementCommand.Create.builder()
                                        .orderWarehouse(warehouseE)
                                        .orderWarehouseTo(warehouseB)
                                        .type(MovementType.LINEHAUL)
                                        .deliveryIntervalFrom(Instant.now(clock).plus(1, ChronoUnit.DAYS))
                                        .inboundArrivalTime(Instant.now(clock).plus(1, ChronoUnit.DAYS))
                                        .inboundDepartureTime(Instant.now(clock).plus(1, ChronoUnit.DAYS).plus(2,
                                                ChronoUnit.HOURS))
                                        .build(),
                                2,
                                1,
                                2
                        ))
                        .price(10_000L)
        );

        run2 = runGenerator.generate(b ->
                b.clearItems()
                        .externalId("a-2")
                        .item(new RunGenerator.RunItemGenerateParam(
                                MovementCommand.Create.builder()
                                        .orderWarehouse(warehouseC)
                                        .orderWarehouseTo(warehouseD)
                                        .type(MovementType.LINEHAUL)
                                        .deliveryIntervalFrom(Instant.now(clock).plus(7, ChronoUnit.DAYS))
                                        .inboundArrivalTime(Instant.now(clock).plus(7, ChronoUnit.DAYS))
                                        .inboundDepartureTime(Instant.now(clock).plus(7, ChronoUnit.DAYS))
                                        .build(),
                                1,
                                0,
                                1
                        ))
                        .item(new RunGenerator.RunItemGenerateParam(
                                MovementCommand.Create.builder()
                                        .orderWarehouse(warehouseE)
                                        .orderWarehouseTo(warehouseD)
                                        .type(MovementType.LINEHAUL)
                                        .deliveryIntervalFrom(Instant.now(clock).plus(7, ChronoUnit.DAYS))
                                        .inboundArrivalTime(Instant.now(clock).plus(7, ChronoUnit.DAYS))
                                        .inboundDepartureTime(Instant.now(clock).plus(7, ChronoUnit.DAYS))
                                        .build(),
                                2,
                                1,
                                2
                        ))
                        .price(10_000L)
        );
    }

    @SneakyThrows
    @Test
    void shouldGetRunById() {
        mockMvc.perform(get("/internal/runs")
                        .param("runId", run.getId().toString())
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @SneakyThrows
    @Test
    void shouldGetRunDetailsById() {
        configurationServiceAdapter.mergeValue(
                ConfigurationProperties.MILLIS_TO_ALLOW_ACTUAL_ARRIVAL_TIME_EDIT_AFTER_EXPECTED_TIME,
                -100_00
        );
        UserShift userShift = runHelper.assignUserAndTransport(run, user, transport);

        var registry = registryCommandService.create(new RegistryCommand.Create(
                userShift.getFirstRoutePoint().streamTasks().findFirst().orElseThrow(), RegistryStatus.CREATED
        ));
        registryCommandService.addItem(new RegistryCommand.AddItem(registry.getId(), "TEST_BARCODE"));

        mockMvc.perform(get("/internal/runs/" + run.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points[0].registry").value(Matchers.notNullValue()));
    }


    @SneakyThrows
    @Test
    void shouldGetRunIdsByLogisticPointsAndStatus() {
        assert run.getExternalId() != null;
        mockMvc.perform(get("/internal/runs/ids")
                        .param("fromLogisticPointIds", "1111")
                        .param("toLogisticPointIds", "2222")
                        .param("statuses", "CREATED", "CONFIRMED", "ASSIGNED")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(run.getId()))
                .andExpect(jsonPath("$[0].externalId").value(run.getExternalId()))
        ;
    }


    @SneakyThrows
    @Test
    void shouldNotFailOnDistinctSelect() {
        mockMvc.perform(get("/internal/runs" +
                        "?deliveryServiceId" +
                        "&startDateFrom=2021-12-23" +
                        "&startDateTo=2021-12-24" +
                        "&runId" +
                        "&toLogisticPointIds=10001791764" +
                        "&types=LINEHAUL" +
                        "&page" +
                        "&size=300" +
                        "&sort=id,DESC")
                )
                .andExpect(status().isOk());
    }

    @SneakyThrows
    @Test
    void shouldNotReturnBadRequestOnMissingSortingAttribute() {
        mockMvc.perform(get("/internal/runs" +
                        "?deliveryServiceId" +
                        "&startDateFrom=2021-12-23" +
                        "&startDateTo=2021-12-24" +
                        "&runId" +
                        "&toLogisticPointIds=10001791764" +
                        "&types=LINEHAUL" +
                        "&page" +
                        "&size=300")
                )
                .andExpect(status().isOk());
    }

    @SneakyThrows
    @Test
    void shouldReturnRunWithToAndFromPoint() {
        mockMvc.perform(get("/internal/runs" +
                        "?deliveryServiceId" +
                        "&runId" +
                        "&toLogisticPointIds=" + warehouseB.getYandexId() +
                        "&fromLogisticPointIds=" + warehouseA.getYandexId() +
                        "&page" +
                        "&size=300")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)));

        mockMvc.perform(get("/internal/runs" +
                        "?deliveryServiceId" +
                        "&runId" +
                        "&toLogisticPointIds=" + warehouseD.getYandexId() +
                        "&fromLogisticPointIds=" + warehouseA.getYandexId() +
                        "&page" +
                        "&size=300")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(0)));
    }

    @SneakyThrows
    @Test
    void shouldReturnBadRequestOnInvalidSortingAttribute() {
        mockMvc.perform(get("/internal/runs" +
                        "?deliveryServiceId" +
                        "&startDateFrom=2021-12-23" +
                        "&startDateTo=2021-12-24" +
                        "&runId" +
                        "&toLogisticPointIds=10001791764" +
                        "&types=LINEHAUL" +
                        "&page" +
                        "&size=300" +
                        "&sort=ya-invalid-attribute,DESC")
                )
                .andDo(print())
                .andExpect(status().isBadRequest());
        ;
    }

    @SneakyThrows
    @Test
    void shouldGetRuns() {
        QueryCountAssertions.assertQueryCountTotalEqual(14, () ->
                mockMvc.perform(get("/internal/runs"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content").value(Matchers.hasSize(2)))
                        .andExpect(jsonPath("$.content[0].points").value(Matchers.hasSize(3)))
        );
        ;
    }

    @SneakyThrows
    @Test
    void shouldGetRunsWithEstimatedTime() {
        UserShift userShift = runHelper.assignUserAndTransport(run, user, transport);
        Instant expected = LocalDateTime.of(2021, 1, 1, 1, 1).toInstant(ZoneOffset.UTC);
        testUserHelper.openShift(user, userShift.getId());
        testUserHelper.estimateTime(userShift.getId(), expected);
        mockMvc.perform(get("/internal/runs?runId=" + run.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].expectedDateTime").exists())
        ;
    }

    @SneakyThrows
    @Test
    void shouldGetRunsByStatuses() {
        mockMvc.perform(get("/internal/runs")
                        .param("statuses", RunStatus.CREATED.name())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(2)))
        ;

        mockMvc.perform(get("/internal/runs")
                        .param("statuses", RunStatus.DRAFT.name())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(0)))
        ;
    }

    @SneakyThrows
    @Test
    void shouldGetRunsByLogisticPointIdFrom() {
        mockMvc.perform(get("/internal/runs")
                        .param("fromLogisticPointIds", warehouseC.getYandexId())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(run2.getId()))
        ;

        mockMvc.perform(get("/internal/runs")
                        .param("toLogisticPointIds", warehouseD.getYandexId())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(run2.getId()))
        ;
    }

    @SneakyThrows
    @Test
    void shouldGetRunsByRunDate() {
        mockMvc.perform(get("/internal/runs")
                        .param("startDateFrom", LocalDate.now(clock).plusDays(1).toString())
                        .param("startDateTo", LocalDate.now(clock).plusDays(1).toString())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(run.getId()))
        ;

        mockMvc.perform(get("/internal/runs")
                        .param("startDateFrom", LocalDate.now(clock).plusDays(7).toString())
                        .param("startDateTo", LocalDate.now(clock).plusDays(7).toString())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(run2.getId()))
        ;
    }

    @SneakyThrows
    @Test
    void shouldSaveRunCommentToRun() {
        String body = String.format(
                "{" +
                        "\"scope\": \"%s\", " +
                        "\"type\": \"%s\", " +
                        "\"severity\": \"%s\", " +
                        "\"text\": \"Водитель пьян!!!\", " +
                        "\"author\": \"yandex-login\"" +
                        "}",
                RunCommentScope.DELAY_REPORT,
                RunCommentType.DISRUPTION_TRANSPORT_COMPANY,
                RunCommentSeverity.WARNING
        );
        mockMvc.perform(put("/internal/runs/{runId}/comment", run.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().json(body))
                .andExpect(jsonPath("$.createdAt").value(Matchers.not(Matchers.isEmptyOrNullString())));
    }

    @SneakyThrows
    @Test
    void shouldGetRunComments() {
        String body = String.format(
                "{" +
                        "\"scope\": \"%s\", " +
                        "\"type\": \"%s\", " +
                        "\"severity\": \"%s\", " +
                        "\"text\": \"Водитель пьян!!!\", " +
                        "\"author\": \"yandex-login\"" +
                        "}",
                RunCommentScope.DELAY_REPORT,
                RunCommentType.DISRUPTION_TRANSPORT_COMPANY,
                RunCommentSeverity.CRITICAL
        );
        mockMvc.perform(put("/internal/runs/{runId}/comment", run.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
        mockMvc.perform(get("/internal/runs/{runId}/comments", run.getId().toString())
                        .param("scopes", RunCommentScope.DELAY_REPORT.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.comments[0].defaultTimestamp.timestamp").exists())
                .andExpect(jsonPath("$.comments[0].defaultTimestamp.timezoneName").exists())
        ;

        mockMvc.perform(get("/internal/runs/{runId}/comments", run.getId().toString())
                        .param("scopes", RunCommentScope.TO_DRIVER.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments").value(Matchers.hasSize(0)));
    }

    @SneakyThrows
    @Test
    void driverLogShouldContainLastMessageType() {
        Transport transport = testUserHelper.findOrCreateTransport();
        UserShift userShift = runHelper.assignUserAndTransport(run, user, transport);
        testUserHelper.openShift(user, userShift.getId());

        NewRunMessageData message = new NewRunMessageData(
                new BigDecimal("12"),
                new BigDecimal("34"),
                RunMessageType.CRITICAL_ISSUE,
                "Не грузят!",
                false,
                1,
                "Europe/Moscow",
                "Иван Иванов",
                RunMessageSource.CARRIER);
        runCommandService.addMessage(new RunCommand.AddMessage(run.getId(), message));

        mockMvc.perform(get("/internal/runs")
                        .param("runId", run.getId().toString())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].driverLog.lastMessageType").value(
                        RunMessageTypeDto.CRITICAL_ISSUE.name()));
    }

    @SneakyThrows
    @Test
    void shouldAddPriceControl() {
        var priceCorrectionAdd = new NewPriceControlDto()
                .type(PriceControlPenaltyTypeDto.RUN_TARIFF_ADD)
                .penaltyCent(3000_00L)
                .comment("Больше денег для ТК")
                .author("ogonek")
                .penaltyStatus(PriceControlPenaltyStatusDto.CONFIRMED);

        mockMvc.perform(post("/internal/runs/" + run.getId() + "/priceControl")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(tplObjectMapper.writeValueAsString(priceCorrectionAdd))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priceStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$.totalCostCent").value(3100_00))
                .andExpect(jsonPath("$.priceControls[0].author").value("ogonek"))
                .andExpect(jsonPath("$.priceControls[0].comment").value("Больше денег для ТК"))
                .andExpect(jsonPath("$.priceControls[0].penaltyCent").value(3000_00));

        var priceCorrectionSub = new NewPriceControlDto()
                .type(PriceControlPenaltyTypeDto.RUN_TARIFF_SUB)
                .penaltyCent(-900_00L)
                .comment("Меньше денег для ТК")
                .author("ogonek")
                .penaltyStatus(PriceControlPenaltyStatusDto.CONFIRMED);

        mockMvc.perform(post("/internal/runs/" + run.getId() + "/priceControl")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(tplObjectMapper.writeValueAsString(priceCorrectionSub))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priceStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$.totalCostCent").value(2200_00));
    }


    @SneakyThrows
    @Test
    void shouldUpdatePriceControl() {
        var priceControl = new NewPriceControlDto()
                .type(PriceControlPenaltyTypeDto.AUTO_DELAY)
                .penaltyCent(-3000_00L)
                .comment("Задержка на час на первой точке")
                .author("robot")
                .penaltyStatus(PriceControlPenaltyStatusDto.NEED_CONFIRMATION);

        var result = mockMvc.perform(post("/internal/runs/" + run.getId() + "/priceControl")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(tplObjectMapper.writeValueAsString(priceControl))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priceStatus").value("WARNING"))
                .andReturn().getResponse().getContentAsString();
        RunPriceInfoDto priceInfoDto = new ObjectMapper().readValue(result, RunPriceInfoDto.class);


        var update = new UpdatePriceControlDto()
                .id(priceInfoDto.getPriceControls().get(0).getId())
                .penaltyStatus(PriceControlPenaltyStatusDto.CONFIRMED)
                .statusChangeAuthor("ogonek");

        mockMvc.perform(put("/internal/runs/" + run.getId() + "/updatePriceControl")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(tplObjectMapper.writeValueAsString(update))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priceStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$.totalCostCent").value(100_00))
                .andExpect(jsonPath("$.priceControls").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.priceControls[0].penaltyStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$.priceControls[0].penaltyCent").value(-3000_00L));
    }

    @SneakyThrows
    @Test
    void shouldReactOnPrice() {
        runCommandService.updatePriceStatus(run.getId(), RunPriceStatus.REJECTED_BY_DS);
        mockMvc.perform(post("/internal/runs/" + run.getId() + "/price/APPROVE_BY_MARKET")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                )
                .andExpect(status().isOk());
    }

    @SneakyThrows
    @Test
    void shouldGetRunsWithPriceStatusFilter() {
        var priceControl = new NewPriceControlDto()
                .type(PriceControlPenaltyTypeDto.AUTO_DELAY)
                .penaltyCent(-3000_00L)
                .comment("Задержка на час на первой точке")
                .author("robot")
                .penaltyStatus(PriceControlPenaltyStatusDto.NEED_CONFIRMATION);

        mockMvc.perform(post("/internal/runs/" + run.getId() + "/priceControl")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(tplObjectMapper.writeValueAsString(priceControl))
                )
                .andExpect(status().isOk());

        mockMvc.perform(get("/internal/runs")
                        .param("showOnlyNotConfirmedFinance", "true")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(run.getId()))
        ;
    }

    @SneakyThrows
    @Test
    void shouldGetRunsStatuses() {
        var priceControl = new NewPriceControlDto()
                .type(PriceControlPenaltyTypeDto.AUTO_DELAY)
                .penaltyCent(-3000_00L)
                .comment("Задержка на час на первой точке")
                .author("robot")
                .penaltyStatus(PriceControlPenaltyStatusDto.NEED_CONFIRMATION);

        mockMvc.perform(post("/internal/runs/" + run.getId() + "/priceControl")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(tplObjectMapper.writeValueAsString(priceControl))
                )
                .andExpect(status().isOk());

        mockMvc.perform(get("/internal/runs")
                        .param("runId", run.getId().toString())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].priceStatus").value("WARNING"));
    }

    @SneakyThrows
    @Test
    void shouldNotAddPriceControl() {
        var priceControl = new NewPriceControlDto()
                .type(PriceControlPenaltyTypeDto.MANUAL_DELAY)
                .penaltyCent(3000_00L)
                .comment("Задержка на час на первой точке")
                .author("ogonek")
                .penaltyStatus(PriceControlPenaltyStatusDto.CONFIRMED);

        mockMvc.perform(post("/internal/runs/" + run.getId() + "/priceControl")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(tplObjectMapper.writeValueAsString(priceControl))
                )
                .andExpect(status().is4xxClientError());
    }

    @SneakyThrows
    @Test
    void shouldFilterByDriverIdEquals() {
        User user1 = testUserHelper.findOrCreateUser(1L, "88001231233", "Иванов", "Василий", "Кужегетович");
        User user2 = user; //Комаров Пашка
        Transport transport1 = testUserHelper.findOrCreateTransport("Машинка 1", Company.DEFAULT_COMPANY_NAME);
        Transport transport2 = testUserHelper.findOrCreateTransport("Машинка 2", Company.DEFAULT_COMPANY_NAME);
        runHelper.assignUserAndTransport(run, user1, transport1);
        runHelper.assignUserAndTransport(run2, user2, transport2);

        mockMvc.perform(get("/internal/runs")
                        .param("driverFullName", UserUtil.FIRST_NAME)
                        .param("driverId", String.valueOf(user1.getId()))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(run.getId()))
        ;
    }

    @SneakyThrows
    @Test
    void shouldFilterByTransportIdEquals() {
        User user1 = testUserHelper.findOrCreateUser(1L, "88001231233", "Иванов", "Василий", "Кужегетович");
        User user2 = user; //Комаров Пашка
        Transport transport1 = testUserHelper.findOrCreateTransport("Машинка 1", Company.DEFAULT_COMPANY_NAME);
        Transport transport2 = testUserHelper.findOrCreateTransport("Машинка 2", Company.DEFAULT_COMPANY_NAME);
        runHelper.assignUserAndTransport(run, user1, transport1);
        runHelper.assignUserAndTransport(run2, user2, transport2);

        mockMvc.perform(get("/internal/runs")
                        .param("driverFullName", UserUtil.FIRST_NAME)
                        .param("transportId", String.valueOf(transport1.getId()))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(run.getId()))
        ;
    }

    @SneakyThrows
    @Test
    void shouldReturnEmptyWithExpectedTime() {
        mockMvc.perform(get("/internal/runs")
                        .param("lastPointArrivalDateFrom", String.valueOf(LocalDate.now(clock).plusDays(2)))
                        .param("lastPointArrivalDateTo", String.valueOf(LocalDate.now(clock).plusDays(2)))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(0)))
        ;
    }

    @SneakyThrows
    @Test
    void shouldReturnWithExpectedTime() {
        mockMvc.perform(get("/internal/runs")
                        .param("lastPointArrivalDateFrom", String.valueOf(LocalDate.now(clock).plusDays(-1L)))
                        .param("lastPointArrivalDateTo", String.valueOf(LocalDate.now(clock).plusDays(1L)))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(String.valueOf(run.getId())))
                .andExpect(jsonPath("$.content[0].lastPointArrivalTime").value("1990-01-01T21:00:00Z"))
                .andExpect(jsonPath("$.content[0].defaultLastPointArrivalTime.timestamp")
                        .value("1990-01-02T00:00:00+03:00"))
        ;
    }

    @SneakyThrows
    @Test
    void shouldReturnWithExpectedTimeFrom() {
        mockMvc.perform(get("/internal/runs")
                        .param("lastPointArrivalDateFrom", String.valueOf(LocalDate.now(clock)))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(String.valueOf(run.getId())))
                .andExpect(jsonPath("$.content[0].lastPointArrivalTime").value("1990-01-01T21:00:00Z"))
        ;
    }

    @SneakyThrows
    @Test
    void shouldReturnWithExpectedTimeTo() {
        mockMvc.perform(get("/internal/runs")
                        .param("lastPointArrivalDateTo", String.valueOf(LocalDate.now(clock)))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(String.valueOf(run.getId())))
        ;
    }

    @SneakyThrows
    @Test
    void shouldReturnEmptyWithExpectedTimeWithSeveralShifts() {
        mockMvc.perform(get("/internal/runs")
                        .param("lastPointArrivalDateFrom", String.valueOf(LocalDate.now(clock)))
                        .param("lastPointArrivalDateTo", String.valueOf(LocalDate.now(clock)))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(String.valueOf(run.getId())))
        ;
    }

    @SneakyThrows
    @Test
    void shouldReturnRunTypesAndSubtypesLists() {
        var run = runGenerator.generate(
                runTuner -> runTuner.runType(RunType.LINEHAUL).runSubtype(RunSubtype.MAIN),
                List.of(
                        Pair.of(
                                riTuner -> riTuner,
                                movementTuner -> movementTuner.type(MovementType.LINEHAUL).subtype(MovementSubtype.MAIN)
                        ),
                        Pair.of(
                                riTuner -> riTuner,
                                movementTuner -> movementTuner.type(MovementType.LINEHAUL).subtype(MovementSubtype.SUPPLEMENTARY_2)
                        ),
                        Pair.of(
                                riTuner -> riTuner,
                                movementTuner -> movementTuner.type(MovementType.INTERWAREHOUSE).subtype(null)
                        )
                )
        );

        mockMvc.perform(get("/internal/runs").param("types", "INTERWAREHOUSE"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].subtypes").value(Matchers.hasSize(2)))
                .andExpect(jsonPath("$.content[0].types").value(Matchers.hasSize(2)));

        mockMvc.perform(get("/internal/runs/" + run.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtypes").value(Matchers.hasSize(2)))
                .andExpect(jsonPath("$.types").value(Matchers.hasSize(2)));
    }

    @SneakyThrows
    @Test
    void shouldReturnDistinctRunsIfAssignedOnSameDriver() {
        runHelper.assignUserShifts(run.getId(), List.of(
                new RunHelper.AssignUserShift(null, user.getId(), transport.getId(), 0, false),
                new RunHelper.AssignUserShift(null, user.getId(), transport.getId(), 1, true)
        ));

        mockMvc.perform(get("/internal/runs")
                .param("driverId", String.valueOf(user.getId()))
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)));
    }

}
