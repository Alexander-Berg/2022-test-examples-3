package ru.yandex.market.tpl.carrier.planner.controller.api;

import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.tpl.carrier.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementType;
import ru.yandex.market.tpl.carrier.core.domain.partner.DsRepository;
import ru.yandex.market.tpl.carrier.core.domain.photo.Photo;
import ru.yandex.market.tpl.carrier.core.domain.photo.PhotoRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunCommandService;
import ru.yandex.market.tpl.carrier.core.domain.run.RunCommentScope;
import ru.yandex.market.tpl.carrier.core.domain.run.RunCommentSeverity;
import ru.yandex.market.tpl.carrier.core.domain.run.RunCommentType;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.RunHelper;
import ru.yandex.market.tpl.carrier.core.domain.run.RunManager;
import ru.yandex.market.tpl.carrier.core.domain.run.RunSubtype;
import ru.yandex.market.tpl.carrier.core.domain.run.commands.NewRunCommentData;
import ru.yandex.market.tpl.carrier.core.domain.run.commands.RunCommand;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.Transport;
import ru.yandex.market.tpl.carrier.core.domain.usershift.CollectDropshipTask;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.carrier.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouse;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseGenerator;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehousePartner;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehousePartnerRepository;
import ru.yandex.market.tpl.carrier.planner.controller.BasePlannerWebTest;
import ru.yandex.mj.generated.server.model.RunStatusDto;
import ru.yandex.mj.generated.server.model.RunTypeDto;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class RunControllerDetailTest extends BasePlannerWebTest {

    private static final String RUN_NAME = "TMT135135";

    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final RunGenerator runGenerator;
    private final TestUserHelper testUserHelper;
    private final DsRepository dsRepository;
    private final OrderWarehousePartnerRepository orderWarehousePartnerRepository;
    private final RunHelper runHelper;
    private final PhotoRepository photoRepository;
    private final RunCommandService runCommandService;
    private final UserShiftCommandService userShiftCommandService;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final RunManager runManager;

    private Company company;
    private User user1;
    private Transport transport;
    private Run run;
    private OrderWarehouse warehouseFrom;
    private OrderWarehouse warehouseTo;


    @BeforeEach
    void setUp() {
        configurationServiceAdapter.mergeValue(
                ConfigurationProperties.MILLIS_TO_ALLOW_ACTUAL_ARRIVAL_TIME_EDIT_AFTER_EXPECTED_TIME, 600000); //10 mins

        company = testUserHelper.findOrCreateCompany(TestUserHelper.CompanyGenerateParam.builder()
                .campaignId(1234L)
                .companyName(Company.DEFAULT_COMPANY_NAME)
                .login("anotherLogin@yandex.ru")
                .build()
        );

        user1 = testUserHelper.findOrCreateUser(UID);
        transport = testUserHelper.findOrCreateTransport();

        Long deliveryServiceId = dsRepository.findByCompaniesId(company.getId()).iterator().next().getId();

        warehouseFrom = orderWarehouseGenerator.generateWarehouse(
                ow -> ow.setPartner(orderWarehousePartnerRepository.saveAndFlush(
                        new OrderWarehousePartner("123", "Рога и копыта")
                ))
        );
        warehouseTo = orderWarehouseGenerator.generateWarehouse(
                ow -> {
                    ow.setPartner(orderWarehousePartnerRepository.saveAndFlush(
                            new OrderWarehousePartner("234", "Стулья даром")
                    ));
                    ow.setTimezone("Asia/Yekaterinburg");
                }
        );
        run = runGenerator.generate(b -> b
                .externalId("asd")
                .deliveryServiceId(deliveryServiceId)
                .campaignId(company.getCampaignId())
                .runDate(LocalDate.now())
                .name(RUN_NAME)
                .runSubtype(RunSubtype.MAIN)
                .clearItems()
                .items(
                        List.of(
                                new RunGenerator.RunItemGenerateParam(
                                        MovementCommand.Create.builder()
                                                .externalId("123")
                                                .orderWarehouse(warehouseFrom)
                                                .orderWarehouseTo(warehouseTo)
                                                .pallets(33)
                                                .type(MovementType.LINEHAUL)
                                                .build(),
                                        1,
                                        null,
                                        null
                                )
                        )
                )
        );
    }

    @SneakyThrows
    @Test
    void shouldReturnRun() {
        mockMvc.perform(get("/internal/runs/{id}", run.getId())
                .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(ResultMatcher.matchAll(
                        status().isOk(),
                        jsonPath("$.points").isNotEmpty(),
                        jsonPath("$.points").value(Matchers.hasSize(2)),
                        jsonPath("$.points[0].id").value(warehouseFrom.getId()),
                        jsonPath("$.points[0].address").value(warehouseFrom.getAddress().getAddress()),
                        jsonPath("$.points[1].id").value(warehouseTo.getId()),
                        jsonPath("$.points[1].address").value(warehouseTo.getAddress().getAddress())
                ));
    }

    @SneakyThrows
    @Test
    void shouldGetRun() {
        // given
        var userShift = runHelper.assignUserAndTransport(run, user1, transport);

        var collectDropshipTask = userShift.streamCollectDropshipTasks().findFirst().orElseThrow();

        testUserHelper.openShift(user1, userShift.getId());
        testUserHelper.finishCollectDropships(collectDropshipTask.getRoutePoint());
        testUserHelper.finishFullReturnAtEnd(userShift);

        runCommandService.addComment(
                new RunCommand.AddComment(
                        run.getId(),
                        new NewRunCommentData(
                                RunCommentScope.DELAY_REPORT,
                                RunCommentType.DISRUPTION_TRANSPORT_COMPANY,
                                "водитель пьян",
                                "yandex-login",
                                null
                        )
                )
        );

        // then
        mockMvc.perform(
                MockMvcRequestBuilders.get("/internal/runs/{runId}", run.getId())
        )

                // expect

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value(run.getId()))
                .andExpect(jsonPath("$.name").value(RUN_NAME))
                .andExpect(jsonPath("$.pallets").value(33))
                .andExpect(jsonPath("$.company.id").value(company.getId()))
                .andExpect(jsonPath("$.company.name").value(Company.DEFAULT_COMPANY_NAME))
                .andExpect(jsonPath("$.company.deliveryServiceId").value(run.getDeliveryServiceId()))
                .andExpect(jsonPath("$.status").value(RunStatusDto.COMPLETED.name()))
                .andExpect(jsonPath("$.type").value(RunTypeDto.LINEHAUL.name()))
                .andExpect(jsonPath("$.subtype").value(RunSubtype.MAIN.name()))
                .andExpect(jsonPath("$.points").isArray())
                .andExpect(jsonPath("$.points").value(Matchers.hasSize(2)))
                .andExpect(jsonPath("$.points[0].id").value(warehouseFrom.getId()))
                .andExpect(jsonPath("$.points[0].address").value(warehouseFrom.getAddress().getAddress()))
                .andExpect(jsonPath("$.points[0].partnerName").value(warehouseFrom.getPartner().getName()))
                .andExpect(jsonPath("$.points[1].id").value(warehouseTo.getId()))
                .andExpect(jsonPath("$.points[1].address").value(warehouseTo.getAddress().getAddress()))
                .andExpect(jsonPath("$.points[1].partnerName").value(warehouseTo.getPartner().getName()))
                .andExpect(jsonPath("$.messages").isArray())
                .andExpect(jsonPath("$.messages").value(Matchers.hasSize(5)))
                .andExpect(jsonPath("$.messages[*].message").value(Matchers.contains(List.of(
                        Matchers.equalTo("Смена начата"),
                        Matchers.containsString("На Месте"),
                        Matchers.containsString("Груз получил"),
                        Matchers.containsString("На Месте"),
                        Matchers.containsString("Груз отгружен")
                ))))
                .andExpect(jsonPath("$.comments").isArray())
                .andExpect(jsonPath("$.comments").value(Matchers.hasSize(7)))
                .andExpect(jsonPath("$.comments[6].createdAt").isString())
                .andExpect(jsonPath("$.comments[6].scope").value(RunCommentScope.DELAY_REPORT.name()))
                .andExpect(jsonPath("$.comments[6].type").value(RunCommentType.DISRUPTION_TRANSPORT_COMPANY.name()))
                .andExpect(jsonPath("$.comments[6].severity").value(RunCommentSeverity.WARNING.name()))
                .andExpect(jsonPath("$.comments[6].text").value("водитель пьян"))
                .andExpect(jsonPath("$.comments[6].author").value("yandex-login"))
        ;
    }

    @SneakyThrows
    @Test
    void shouldGetUnassignedRun() {

        // then
        mockMvc.perform(
                MockMvcRequestBuilders.get("/internal/runs/{runId}", run.getId())
        )

                // expect

                .andExpect(ResultMatcher.matchAll(
                        status().isOk(),
                        jsonPath("$.points").isNotEmpty(),
                        jsonPath("$.points").value(Matchers.hasSize(2)),
                        jsonPath("$.points[0].id").value(warehouseFrom.getId()),
                        jsonPath("$.points[0].address").value(warehouseFrom.getAddress().getAddress()),
                        jsonPath("$.points[1].id").value(warehouseTo.getId()),
                        jsonPath("$.points[1].address").value(warehouseTo.getAddress().getAddress())
                ));
    }

    @SneakyThrows
    @Test
    void shouldGetTransportFromRunIfTransportAssigned() {
        runManager.assignTransport(run.getId(), transport.getId());
        // then
        mockMvc.perform(
                MockMvcRequestBuilders.get("/internal/runs/{runId}", run.getId())
        )

                // expect

                .andExpect(ResultMatcher.matchAll(
                        status().isOk(),
                        jsonPath("$.runId").value(run.getId()),
                        jsonPath("$.transport.name").value(TestUserHelper.DEFAULT_TRANSPORT_NAME)
                ));
    }

    @SneakyThrows
    @Test
    void shouldGetTransportFromRunIfUserShiftAssigned() {
        runHelper.assignUserAndTransport(run, user1, transport);

        // then
        mockMvc.perform(
                MockMvcRequestBuilders.get("/internal/runs/{runId}", run.getId())
        )

                // expect

                .andExpect(ResultMatcher.matchAll(
                        status().isOk(),
                        jsonPath("$.runId").value(run.getId()),
                        jsonPath("$.transport.name").value(TestUserHelper.DEFAULT_TRANSPORT_NAME),
                        jsonPath("$.transport.number").value(TestUserHelper.DEFAULT_TRANSPORT_NUMBER)
                ));
    }

    @SneakyThrows
    @Test
    void shouldGetPhotosFromRun() {
        UserShift userShift = runHelper.assignUserAndTransport(run, user1, transport);
        CollectDropshipTask task = userShift.streamCollectDropshipTasks().findFirst().orElseThrow();

        photoRepository.save(new Photo(task.getId(), "http://example.org", 3L, null, null, null, null));

        mockMvc.perform(
                MockMvcRequestBuilders.get("/internal/runs/{id}", run.getId())
        )

                .andExpect(ResultMatcher.matchAll(
                        status().isOk(),
                        jsonPath("$.runId").value(run.getId()),
                        jsonPath("$.points[0].photos").exists(),
                        jsonPath("$.points[0].photos").isArray(),
                        jsonPath("$.points[0].photos").value(Matchers.hasSize(1)),
                        jsonPath("$.points[0].photos[0].url").value("http://example.org")
                ));
    }

    @SneakyThrows
    @Test
    void shouldGetTimestampsFromRun() {
        mockMvc.perform(
                MockMvcRequestBuilders.get("/internal/runs/{id}", run.getId())
        ).andExpect(status().isOk())
                .andExpect(jsonPath("$.points[1].defaultExpectedArrivalTimestamp.timezoneName").value("Europe/Moscow"))
                .andExpect(jsonPath("$.points[1].defaultExpectedArrivalTimestamp.timestamp").value("1990-01-01T18:00:00+03:00"))
                .andExpect(jsonPath("$.points[1].localExpectedArrivalTimestamp.timezoneName").value("Asia/Yekaterinburg"))
                .andExpect(jsonPath("$.points[1].localExpectedArrivalTimestamp.timestamp").value("1990-01-01T20:00:00+05:00"));
    }

    @SneakyThrows
    @Test
    void shouldGetNotFinishedRunPoints() {
        var userShift = runHelper.assignUserAndTransport(run, user1, transport);
        testUserHelper.openShift(user1, userShift.getId());

        mockMvc.perform(
                MockMvcRequestBuilders.get("/internal/runs/{id}", run.getId())
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points").isArray())
                .andExpect(jsonPath("$.points").value(Matchers.hasSize(2)))
                .andExpect(jsonPath("$.points[0].id").value(warehouseFrom.getId()))
                .andExpect(jsonPath("$.points[0].address").value(warehouseFrom.getAddress().getAddress()))
                .andExpect(jsonPath("$.points[0].partnerName").value(warehouseFrom.getPartner().getName()))
                .andExpect(jsonPath("$.points[0].expectedArrivalTimestamp")
                        .value("1990-01-01T06:00:00Z"))
                .andExpect(jsonPath("$.points[0].defaultExpectedArrivalTimestamp.timestamp")
                        .value("1990-01-01T09:00:00+03:00"))
                .andExpect(jsonPath("$.points[0].arrivalTimestamp").doesNotExist())
                .andExpect(jsonPath("$.points[0].defaultArrivalTimestamp.timestamp").doesNotExist())
                .andExpect(jsonPath("$.points[0].defaultArrivalTimestamp.timezoneName")
                        .value("Europe/Moscow"))
                .andExpect(jsonPath("$.points[1].id").value(warehouseTo.getId()))
                .andExpect(jsonPath("$.points[1].address").value(warehouseTo.getAddress().getAddress()))
                .andExpect(jsonPath("$.points[1].partnerName").value(warehouseTo.getPartner().getName()));
    }

    @SneakyThrows
    @Test
    void shouldGetFinishedRunPoints() {
        var userShift = runHelper.assignUserAndTransport(run, user1, transport);
        testUserHelper.openShift(user1, userShift.getId());

        var collectDropshipTask = userShift.streamCollectDropshipTasks().findFirst().orElseThrow();
        testUserHelper.arriveAtRoutePoint(userShift, collectDropshipTask.getRoutePoint().getId());
        userShiftCommandService.collectDropships(user1, new UserShiftCommand.CollectDropships(
                userShift.getId(),
                collectDropshipTask.getRoutePoint().getId(),
                collectDropshipTask.getId())
        );
        testUserHelper.finishFullReturnAtEnd(userShift);

        // then
        mockMvc.perform(
                MockMvcRequestBuilders.get("/internal/runs/{id}", run.getId())
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points").isArray())
                .andExpect(jsonPath("$.points").value(Matchers.hasSize(2)))
                .andExpect(jsonPath("$.points[0].id").value(warehouseFrom.getId()))
                .andExpect(jsonPath("$.points[0].address").value(warehouseFrom.getAddress().getAddress()))
                .andExpect(jsonPath("$.points[0].partnerName").value(warehouseFrom.getPartner().getName()))
                .andExpect(jsonPath("$.points[0].expectedArrivalTimestamp")
                        .value("1990-01-01T06:00:00Z"))
                .andExpect(jsonPath("$.points[0].defaultExpectedArrivalTimestamp.timestamp")
                        .value("1990-01-01T09:00:00+03:00"))
                .andExpect(jsonPath("$.points[0].arrivalTimestamp")
                        .value("1989-12-31T21:00:00Z"))
                .andExpect(jsonPath("$.points[0].defaultArrivalTimestamp.timestamp")
                        .value("1990-01-01T00:00:00+03:00"))
                .andExpect(jsonPath("$.points[1].id").value(warehouseTo.getId()))
                .andExpect(jsonPath("$.points[1].address").value(warehouseTo.getAddress().getAddress()))
                .andExpect(jsonPath("$.points[1].partnerName").value(warehouseTo.getPartner().getName()));
    }


    @Test
    @SneakyThrows
    void shouldReturnIsEditableFalseIfRunNotAssigned() {
        mockMvc.perform(
                MockMvcRequestBuilders.get("/internal/runs/{id}", run.getId())
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points[0].isArrivalTimestampEditable").value(false))
                .andExpect(jsonPath("$.points[1].isArrivalTimestampEditable").value(false));
    }
}
