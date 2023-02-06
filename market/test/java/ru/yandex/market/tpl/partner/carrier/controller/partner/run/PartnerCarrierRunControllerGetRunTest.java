package ru.yandex.market.tpl.partner.carrier.controller.partner.run;

import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.duty.Duty;
import ru.yandex.market.tpl.carrier.core.domain.duty.DutyGenerator;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.carrier.core.domain.partner.DsRepository;
import ru.yandex.market.tpl.carrier.core.domain.photo.Photo;
import ru.yandex.market.tpl.carrier.core.domain.photo.PhotoRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunCommandService;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.RunRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.car_request_docs.CarRequestDoc;
import ru.yandex.market.tpl.carrier.core.domain.run.car_request_docs.CarRequestDocRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.commands.RunCommand;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.Transport;
import ru.yandex.market.tpl.carrier.core.domain.usershift.CollectDropshipTask;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.carrier.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouse;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseGenerator;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehousePartner;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehousePartnerRepository;
import ru.yandex.market.tpl.partner.carrier.BaseTplPartnerCarrierWebIntTest;
import ru.yandex.market.tpl.partner.carrier.model.run.RunStatus;
import ru.yandex.market.tpl.partner.carrier.service.run.PartnerCarrierRunService;
import ru.yandex.market.tpl.partner.carrier.web.PartnerCompanyHandler;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Sql("classpath:mockPartner/defaultDeliveryServices.sql")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class PartnerCarrierRunControllerGetRunTest extends BaseTplPartnerCarrierWebIntTest {
    private static final long SORTING_CENTER_ID = 47819L;
    private static final String RUN_NAME = "Маршрут 1";
    private static final String DUTY_NAME = "Дежурство 1";

    private final TestUserHelper testUserHelper;
    private final RunGenerator manualRunService;
    private final PartnerCarrierRunService partnerCarrierRunService;
    private final UserShiftCommandService userShiftCommandService;
    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final RunCommandService runCommandService;
    private final CarRequestDocRepository carRequestDocRepository;
    private final DutyGenerator dutyGenerator;

    private final DsRepository dsRepository;
    private final PhotoRepository photoRepository;
    private final RunRepository runRepository;
    private final UserShiftRepository userShiftRepository;
    private final OrderWarehousePartnerRepository orderWarehousePartnerRepository;

    private Company company;
    private Run run;
    private CarRequestDoc doc;
    private Duty duty;
    private Run dutyRun;
    private CarRequestDoc doc2;
    private User user1;
    private OrderWarehouse warehouseFrom;
    private OrderWarehouse warehouseTo;
    private Transport transport;

    @BeforeEach
    void setUp() {
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
        run = manualRunService.generate(b -> b
                .externalId("asd")
                .deliveryServiceId(deliveryServiceId)
                .campaignId(company.getCampaignId())
                .runDate(LocalDate.now())
                .name(RUN_NAME)
                .clearItems()
                .items(
                        List.of(
                                new RunGenerator.RunItemGenerateParam(
                                        MovementCommand.Create.builder()
                                                .externalId("123")
                                                .orderWarehouse(warehouseFrom)
                                                .orderWarehouseTo(warehouseTo)
                                                .build(),
                                        1,
                                        null,
                                        null
                                )
                        )
                )
        );

        doc = carRequestDocRepository.findCarRequestDocByRun(run).get(0);

        duty = dutyGenerator.generate(dpb -> dpb.deliveryServiceId(deliveryServiceId).name(DUTY_NAME));
        dutyRun = duty.getRun();
        doc2 = carRequestDocRepository.findCarRequestDocByRun(dutyRun).get(0);
    }

    @SneakyThrows
    @Test
    void shouldGetRun() {
        // given
        partnerCarrierRunService.assignTransportToRun(run.getId(), transport.getId(), company.getId());
        partnerCarrierRunService.assignCourierToRun(run.getId(), user1.getId(), company.getId());

        transactionTemplate.execute(tc -> {
            run = runRepository.findById(run.getId()).orElseThrow();

            var userShift = run.getFirstAssignedShift();

            var collectDropshipTask = userShift.streamCollectDropshipTasks().findFirst().orElseThrow();

            testUserHelper.openShift(user1, userShift.getId());
            testUserHelper.arriveAtRoutePoint(userShift, collectDropshipTask.getRoutePoint().getId());
            userShiftCommandService.collectDropships(user1, new UserShiftCommand.CollectDropships(
                    userShift.getId(),
                    collectDropshipTask.getRoutePoint().getId(),
                    collectDropshipTask.getId())
            );

            testUserHelper.finishFullReturnAtEnd(userShift);
            return null;
        });

        // then
        mockMvc.perform(
                MockMvcRequestBuilders.get("/internal/partner/runs/{runId}", run.getId())
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
        )

                // expect

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value(run.getId()))
                .andExpect(jsonPath("$.name").value(RUN_NAME))
                .andExpect(jsonPath("$.status").value(RunStatus.COMPLETED.name()))
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
                .andExpect(jsonPath("$.document.docRunId").value(doc.getId()))
        ;
    }

    @SneakyThrows
    @Test
    void shouldGetUnassignedRun() {

        // then
        mockMvc.perform(
                MockMvcRequestBuilders.get("/internal/partner/runs/{runId}", run.getId())
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
        )

                // expect

                .andExpect(ResultMatcher.matchAll(
                        status().isOk(),
                        jsonPath("$.points").isNotEmpty(),
                        jsonPath("$.points").value(Matchers.hasSize(2)),
                        jsonPath("$.points[0].id").value(warehouseFrom.getId()),
                        jsonPath("$.points[0].address").value(warehouseFrom.getAddress().getAddress()),
                        jsonPath("$.points[1].id").value(warehouseTo.getId()),
                        jsonPath("$.points[1].address").value(warehouseTo.getAddress().getAddress()),
                        jsonPath("$.document").isEmpty()
                ));
    }

    @SneakyThrows
    @Test
    void shouldGetTransportFromRunIfTransportAssigned() {
        partnerCarrierRunService.assignTransportToRun(run.getId(), transport.getId(), company.getId());
        // then
        mockMvc.perform(
                MockMvcRequestBuilders.get("/internal/partner/runs/{runId}", run.getId())
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
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
        partnerCarrierRunService.assignTransportToRun(run.getId(), transport.getId(), company.getId());
        partnerCarrierRunService.assignCourierToRun(run.getId(), user1.getId(), company.getId());

        // then
        mockMvc.perform(
                MockMvcRequestBuilders.get("/internal/partner/runs/{runId}", run.getId())
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
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
    void shouldGetPhotosFromRun() {
        runCommandService.assignTransport(new RunCommand.AssignTransport(run.getId(), transport.getId()));
        partnerCarrierRunService.assignCourierToRun(run.getId(), user1.getId(), company.getId());

        CollectDropshipTask task = transactionTemplate.execute(tc -> {
            run = runRepository.findByIdOrThrow(run.getId());
            UserShift userShift = run.getFirstAssignedShift();
            return userShift.streamCollectDropshipTasks().findFirst().orElseThrow();
        });

        photoRepository.save(new Photo(task.getId(), "http://example.org", 3L, null, null, null, null));

        mockMvc.perform(
                MockMvcRequestBuilders.get("/internal/partner/runs/{id}", run.getId())
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
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
                MockMvcRequestBuilders.get("/internal/partner/runs/{id}", run.getId())
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
        ).andExpect(status().isOk())
         .andExpect(jsonPath("$.points[1].defaultExpectedArrivalTimestamp.timezoneName").value("Europe/Moscow"))
         .andExpect(jsonPath("$.points[1].defaultExpectedArrivalTimestamp.timestamp").value("1990-01-01T18:00:00+03:00"))
         .andExpect(jsonPath("$.points[1].localExpectedArrivalTimestamp.timezoneName").value("Asia/Yekaterinburg"))
         .andExpect(jsonPath("$.points[1].localExpectedArrivalTimestamp.timestamp").value("1990-01-01T20:00:00+05:00"));
    }

    @SneakyThrows
    @Test
    void shouldGetDutyRunWithDoc() {
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/internal/partner/runs/{id}", dutyRun.getId())
                                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                ).andExpect(status().isOk())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value(dutyRun.getId()))
                .andExpect(jsonPath("$.name").value(DUTY_NAME))
                .andExpect(jsonPath("$.document.docDutyId").value(doc2.getId()))
        ;
    }
}
