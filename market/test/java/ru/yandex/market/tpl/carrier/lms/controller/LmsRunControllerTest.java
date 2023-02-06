package ru.yandex.market.tpl.carrier.lms.controller;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementGenerator;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementType;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunCommandService;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.RunHelper;
import ru.yandex.market.tpl.carrier.core.domain.run.RunRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.RunStatus;
import ru.yandex.market.tpl.carrier.core.domain.run.RunSubtype;
import ru.yandex.market.tpl.carrier.core.domain.run.commands.RunCommand;
import ru.yandex.market.tpl.carrier.core.domain.run.commands.RunItemData;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.user.UserUtil;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.Transport;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouse;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseGenerator;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehousePartner;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehousePartnerRepository;
import ru.yandex.market.tpl.carrier.planner.lms.IdDto;
import ru.yandex.market.tpl.carrier.planner.lms.run.view.LmsRunType;
import ru.yandex.market.tpl.common.util.DateTimeUtil;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_=@Autowired)
class LmsRunControllerTest extends LmsControllerTest {

    private static final long USER_ID_1 = 1L;

    private final Clock clock;
    private final ObjectMapper tplObjectMapper;
    private final RunRepository runRepository;

    private final TestUserHelper testUserHelper;
    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final MovementGenerator movementGenerator;
    private final RunCommandService runCommandService;
    private final RunGenerator runGenerator;
    private final RunHelper runHelper;
    private final OrderWarehousePartnerRepository orderWarehousePartnerRepository;

    private Instant expectedDateTimeFrom = ZonedDateTime.of(2021, 8, 4, 0, 0, 0, 0, DateTimeUtil.DEFAULT_ZONE_ID)
            .toInstant();
    private Instant expectedDateTimeTo = ZonedDateTime.of(2021, 8, 4, 23, 59, 59, 0, DateTimeUtil.DEFAULT_ZONE_ID)
            .toInstant();

    private Company company;
    private User user1;
    private Run run1;
    private Transport transport1;
    private OrderWarehouse warehouse1;
    private OrderWarehouse warehouse2;
    private String requestBody;

    @BeforeEach
    @SneakyThrows
    void setUp() {
        company = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);
        user1 = testUserHelper.findOrCreateUser(USER_ID_1, Company.DEFAULT_COMPANY_NAME, UserUtil.PHONE);
        transport1 = testUserHelper.findOrCreateTransport();

        warehouse1 = orderWarehouseGenerator.generateWarehouse();
        warehouse2 = orderWarehouseGenerator.generateWarehouse();

        var movement1 = movementGenerator.generate(MovementCommand.Create.builder()
                .orderWarehouse(warehouse1)
                .deliveryIntervalFrom(expectedDateTimeFrom)
                .orderWarehouseTo(warehouse2)
                .deliveryIntervalTo(expectedDateTimeTo)
                .type(MovementType.LINEHAUL)
                .build());
        run1 = runCommandService.create(RunCommand.Create.builder()
                .externalId("run1")
                .runDate(LocalDate.now())
                .subtype(RunSubtype.MAIN)
                .campaignId(company.getCampaignId())
                .items(List.of(RunItemData.builder()
                        .movement(movement1)
                        .orderNumber(1)
                        .build()))
                .build()
        );


        IdDto dto = new IdDto();
        dto.setId(run1.getId());
        requestBody = tplObjectMapper.writeValueAsString(dto);

    }

    @Test
    @SneakyThrows
    void shouldTransferToManuallyCompletedFromCreated() {
        mockMvc.perform(post("/LMS/carrier/runs/complete")
                                .content(requestBody)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                )
                .andExpect(status().isOk());

        Run run = runRepository.findById(run1.getId()).orElseThrow();
        Assertions.assertEquals(RunStatus.MANUALLY_COMPLETED, run.getStatus());
    }

    @Test
    @SneakyThrows
    void shouldTransferToManuallyCompletedFromConfirmed() {

        runCommandService.confirm(new RunCommand.Confirm(run1.getId()));

        mockMvc.perform(post("/LMS/carrier/runs/complete")
                        .content(requestBody)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                )
                .andExpect(status().isOk());

        Run run = runRepository.findById(run1.getId()).orElseThrow();
        Assertions.assertEquals(RunStatus.MANUALLY_COMPLETED, run.getStatus());
    }

    @Test
    @SneakyThrows
    void shouldTransferToManuallyCompletedFromAssigned() {

        runHelper.assignUserAndTransport(run1, user1,transport1);

        mockMvc.perform(post("/LMS/carrier/runs/complete")
                        .content(requestBody)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                )
                .andExpect(status().isOk());

        Run run = runRepository.findById(run1.getId()).orElseThrow();
        Assertions.assertEquals(RunStatus.MANUALLY_COMPLETED, run.getStatus());
    }

    @Test
    @SneakyThrows
    void shouldTransferToManuallyCompletedFromStarted() {
        runHelper.assignUserAndTransport(run1, user1,transport1);
        runCommandService.start(new RunCommand.Start(run1.getId()));

        mockMvc.perform(post("/LMS/carrier/runs/complete")
                        .content(requestBody)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                )
                .andExpect(status().isOk());

        Run run = runRepository.findById(run1.getId()).orElseThrow();
        Assertions.assertEquals(RunStatus.MANUALLY_COMPLETED, run.getStatus());
    }

    @Test
    @SneakyThrows
    void shouldNotTransferToManuallyCompletedFromCompleted() {
        runCommandService.complete(new RunCommand.Complete(run1.getId()));

        mockMvc.perform(post("/LMS/carrier/runs/complete")
                        .content(requestBody)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                )
                .andExpect(status().isOk());

        Run run = runRepository.findById(run1.getId()).orElseThrow();
        Assertions.assertEquals(RunStatus.COMPLETED, run.getStatus());
    }

    @Test
    @SneakyThrows
    void shouldNotTransferToManuallyCompletedFromManuallyCompleted() {
        runCommandService.complete(new RunCommand.ManualComplete(run1.getId()));

        mockMvc.perform(post("/LMS/carrier/runs/complete")
                        .content(requestBody)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                )
                .andExpect(status().isOk());

        Run run = runRepository.findById(run1.getId()).orElseThrow();
        Assertions.assertEquals(RunStatus.MANUALLY_COMPLETED, run.getStatus());
    }

    @Test
    @SneakyThrows
    void shouldGetRuns() {
        mockMvc.perform(get("/LMS/carrier/runs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].values.externalId").value("run1"))
        ;
    }

    @Test
    @SneakyThrows
    void shouldGetRun() {
        mockMvc.perform(get("/LMS/carrier/runs/{id}", run1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.item.values.externalId").value("run1"))

        ;
    }

    @Test
    @SneakyThrows
    void shouldFilterByExternalId() {
        mockMvc.perform(get("/LMS/carrier/runs")
                .param("externalId", "run1")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").value(Matchers.hasSize(1)))
        ;
        mockMvc.perform(get("/LMS/carrier/runs")
                .param("externalId", "run2")

        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
        ;
    }


    @Test
    @SneakyThrows
    void shouldShowRunType() {
        var xdockRun = runGenerator.generate(RunGenerator.RunGenerateParam.builder()
                .externalId("xdockRun")
                .deliveryServiceId(1234L)
                .campaignId(company.getCampaignId())
                .runDate(LocalDate.now(clock))
                .price(10_000L)
                .item(RunGenerator.RunItemGenerateParam.builder()
                        .movement(MovementCommand.Create.builder()
                                .externalId("123")
                                .orderWarehouse(orderWarehouseGenerator.generateWarehouse(ow -> {
                                    ow.setPartner(orderWarehousePartnerRepository.saveAndFlush(
                                            new OrderWarehousePartner("402", "РЦ Климовск")
                                    ));
                                }))
                                .orderWarehouseTo(orderWarehouseGenerator.generateWarehouse())
                                .deliveryIntervalFrom(ZonedDateTime.now(clock).minusHours(3).toInstant())
                                .deliveryIntervalTo(ZonedDateTime.now(clock).minusHours(2).toInstant())
                                .type(MovementType.XDOC_TRANSPORT)
                                .build()
                        )
                        .orderNumber(1)
                        .build())
                .build()
        );

        mockMvc.perform(get("/LMS/carrier/runs")
                .param("externalId", "run1")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].values.type").value(LmsRunType.LINEHAUL.getName()));

        mockMvc.perform(get("/LMS/carrier/runs")
                .param("externalId", "xdockRun")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].values.type").value(LmsRunType.XDOCK.getName()));
    }

    @Test
    @SneakyThrows
    void shouldMoveFromDraft() {
        Run run = runGenerator.generate(b -> b.totalCount(4));
        Assertions.assertEquals(RunStatus.DRAFT, run.getStatus());

        mockMvc.perform(post("/LMS/carrier/runs/moveFromDraft")
                .content(tplObjectMapper.writeValueAsString(new IdDto(run.getId())))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
                .andExpect(status().isOk());

        run = runRepository.findByIdOrThrow(run.getId());
        Assertions.assertEquals(run.getStatus(), RunStatus.CREATED);

    }
}
