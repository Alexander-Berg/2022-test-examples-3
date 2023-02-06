package ru.yandex.market.tpl.partner.carrier.controller.partner.run.multiple_shifts;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;

import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.movement.Movement;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementHistoryEventType;
import ru.yandex.market.tpl.carrier.core.domain.movement.event.history.MovementHistoryEvent;
import ru.yandex.market.tpl.carrier.core.domain.movement.event.history.MovementHistoryEventRepository;
import ru.yandex.market.tpl.carrier.core.domain.partner.DsRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.PlannedRoutePointData;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.RunItem;
import ru.yandex.market.tpl.carrier.core.domain.run.RunRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.RunStatus;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.user.UserUtil;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.Transport;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.TransportRepository;
import ru.yandex.market.tpl.carrier.core.domain.usershift.DriverChangeTask;
import ru.yandex.market.tpl.carrier.core.domain.usershift.DriverChangeTaskSubtype;
import ru.yandex.market.tpl.carrier.core.domain.usershift.TaskType;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShiftStatus;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouse;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseGenerator;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.partner.carrier.BaseTplPartnerCarrierWebIntTest;
import ru.yandex.market.tpl.partner.carrier.model.run.partner.AssignRunDto;
import ru.yandex.market.tpl.partner.carrier.model.run.partner.AssignUserShiftDto;
import ru.yandex.market.tpl.partner.carrier.model.transport.PartnerCarrierTransportCreateDto;
import ru.yandex.market.tpl.partner.carrier.service.user.transport.PartnerCarrierTransportService;
import ru.yandex.market.tpl.partner.carrier.web.PartnerCompanyHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Sql("classpath:mockPartner/defaultDeliveryServices.sql")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class PartnerCarrierRunControllerAssignMultipleUserShiftTest extends BaseTplPartnerCarrierWebIntTest {

    private final RunGenerator manualRunService;
    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final TestUserHelper testUserHelper;
    private final PartnerCarrierTransportService partnerCarrierTransportService;
    private final DsRepository dsRepository;
    private final RunRepository runRepository;
    private final TransportRepository transportRepository;
    private final ObjectMapper tplObjectMapper;
    private final MovementHistoryEventRepository movementHistoryEventRepository;

    private Company company;
    private User user1;
    private User user2;
    private Transport transport;
    private Transport transport2;
    private Run run;
    private Run run2;

    @BeforeEach
    void setUp() {
        company = testUserHelper.findOrCreateCompany(TestUserHelper.CompanyGenerateParam.builder()
                .campaignId(1234L)
                .companyName(Company.DEFAULT_COMPANY_NAME)
                .login("anotherLogin@yandex.ru")
                .build()
        );

        user1 = testUserHelper.findOrCreateUser(UID, Company.DEFAULT_COMPANY_NAME, UserUtil.PHONE);
        user2 = testUserHelper.findOrCreateUser(ANOTHER_UID, Company.DEFAULT_COMPANY_NAME, UserUtil.ANOTHER_PHONE);

        transport =
                transportRepository.findByIdOrThrow(partnerCarrierTransportService.create(PartnerCarrierTransportCreateDto.builder()
                        .name("Машина")
                        .capacity(new BigDecimal("1.23"))
                        .palletsCapacity(123)
                        .number(TestUserHelper.DEFAULT_TRANSPORT_NUMBER)
                        .build(), company.getId()).getId());

        transport2 =
                transportRepository.findByIdOrThrow(partnerCarrierTransportService.create(PartnerCarrierTransportCreateDto.builder()
                        .name("Машина2")
                        .capacity(new BigDecimal("3.45"))
                        .palletsCapacity(345)
                        .number("а921мр")
                        .build(), company.getId()).getId());


        Long deliveryServiceId = dsRepository.findByCompaniesId(company.getId()).iterator().next().getId();

        OrderWarehouse warehouseTo = orderWarehouseGenerator.generateWarehouse();

        run = manualRunService.generate(RunGenerator.RunGenerateParam.builder()
                .externalId("asd")
                .deliveryServiceId(deliveryServiceId)
                .campaignId(company.getCampaignId())
                .runDate(LocalDate.of(2021, 8, 5))
                .items(List.of(
                        new RunGenerator.RunItemGenerateParam(
                                MovementCommand.Create.builder()
                                        .externalId("123")
                                        .orderWarehouse(orderWarehouseGenerator.generateWarehouse())
                                        .orderWarehouseTo(warehouseTo)
                                        .outboundArrivalTime(ZonedDateTime.of(2021, 8, 5, 4, 0, 0, 0,
                                                DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                        .deliveryIntervalFrom(ZonedDateTime.of(2021, 8, 5, 4, 0, 0, 0,
                                                DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                        .deliveryIntervalTo(ZonedDateTime.of(2021, 8, 5, 5, 0, 0, 0,
                                                DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                        .build(),
                                1,
                                null,
                                null
                        ),
                        new RunGenerator.RunItemGenerateParam(
                                MovementCommand.Create.builder()
                                        .externalId("345")
                                        .orderWarehouse(orderWarehouseGenerator.generateWarehouse())
                                        .orderWarehouseTo(warehouseTo)
                                        .outboundArrivalTime(ZonedDateTime.of(2021, 8, 5, 2, 0, 0, 0,
                                                DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                        .deliveryIntervalFrom(ZonedDateTime.of(2021, 8, 5, 2, 0, 0, 0,
                                                DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                        .deliveryIntervalTo(ZonedDateTime.of(2021, 8, 5, 5, 0, 0, 0,
                                                DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                        .build(),
                                2,
                                null,
                                null
                        )
                ))
                .build()
        );

        run2 = manualRunService.generate(RunGenerator.RunGenerateParam.builder()
                .externalId("def")
                .deliveryServiceId(deliveryServiceId)
                .campaignId(company.getCampaignId())
                .runDate(LocalDate.of(2021, 8, 5))
                .clearItems()
                .item(
                        new RunGenerator.RunItemGenerateParam(
                                MovementCommand.Create.builder()
                                        .externalId("567")
                                        .orderWarehouse(orderWarehouseGenerator.generateWarehouse())
                                        .orderWarehouseTo(warehouseTo)
                                        .deliveryIntervalFrom(ZonedDateTime.of(2021, 8, 5, 7, 0, 0, 0,
                                                DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                        .deliveryIntervalTo(ZonedDateTime.of(2021, 8, 5, 8, 0, 0, 0,
                                                DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                        .build(),
                                1,
                                null,
                                null
                        )
                )
                .build()
        );

    }

    @SneakyThrows
    @Test
    void assignSimpleUserShift() {
        mockMvc.perform(post("/internal/partner/runs/{runId}/assign-user-shift", run.getId())
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .content(tplObjectMapper.writeValueAsString(new AssignRunDto("a123aa12", false,
                                List.of(new AssignUserShiftDto(null, transport.getId(), user1.getId(), null, false)))))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                )
                .andExpect(status().isOk());

        var newRun = runRepository.findById(run.getId()).orElseThrow();
        assertThat(newRun.streamUserShifts().toList()).hasSize(1);
        assertThat(newRun.streamUserShifts().findAny().orElseThrow().getUser()).isEqualTo(user1);
        assertThat(newRun.streamUserShifts().findAny().orElseThrow().getTransport()).isEqualTo(transport);

        mockMvc.perform(post("/internal/partner/runs/{runId}/assign-user-shift", run.getId())
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .content(tplObjectMapper.writeValueAsString(new AssignRunDto("б345бб56", false,
                                List.of(new AssignUserShiftDto(null, transport2.getId(), user2.getId(), null, true)))))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                )
                .andExpect(status().isOk());

        transactionTemplate.execute(tc -> {
            var newRun2 = runRepository.findById(run.getId()).orElseThrow();
            assertThat(newRun2.streamUserShifts().toList()).hasSize(1);
            assertThat(newRun2.streamUserShifts().findAny().orElseThrow().getUser()).isEqualTo(user2);
            assertThat(newRun2.streamUserShifts().findAny().orElseThrow().getTransport()).isEqualTo(transport2);
            List<Long> movementIds = newRun2.streamRunItems().map(RunItem::getMovement).map(Movement::getId).collect(Collectors.toList());
            List<MovementHistoryEvent> movementsWithCourierFoundEvent = StreamEx.of(
                    movementHistoryEventRepository.findByMovementIdIn(movementIds, Pageable.unpaged()).stream()
            )
                    .filterBy(MovementHistoryEvent::getType, MovementHistoryEventType.DROPSHIP_TASK_CREATED)
                    .collect(Collectors.toList());
            assertThat(movementsWithCourierFoundEvent).hasSize(3 * 2);
            return null;
        });
    }

    @SneakyThrows
    @Test
    void assignSimpleUserShiftMultishiftTrue() {
        mockMvc.perform(post("/internal/partner/runs/{runId}/assign-user-shift", run.getId())
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .content(tplObjectMapper.writeValueAsString(new AssignRunDto("a123aa12", true,
                                List.of(new AssignUserShiftDto(null, transport.getId(), user1.getId(), 2, true)))))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                )
                .andExpect(status().isOk());

        var newRun = runRepository.findById(run.getId()).orElseThrow();
        assertThat(newRun.streamUserShifts().toList()).hasSize(1);
        assertThat(newRun.streamUserShifts().findAny().orElseThrow().getUser()).isEqualTo(user1);
        assertThat(newRun.streamUserShifts().findAny().orElseThrow().getTransport()).isEqualTo(transport);

        transactionTemplate.execute(tc -> {
            var newRun2 = runRepository.findById(run.getId()).orElseThrow();
            assertThat(newRun2.streamUserShifts().toList()).hasSize(1);
            assertThat(newRun2.streamUserShifts().findAny().orElseThrow().getUser()).isEqualTo(user1);
            assertThat(newRun2.streamUserShifts().findAny().orElseThrow().getTransport()).isEqualTo(transport);
            List<Long> movementIds = newRun2.streamRunItems().map(RunItem::getMovement).map(Movement::getId).collect(Collectors.toList());
            List<MovementHistoryEvent> movementsWithCourierFoundEvent = StreamEx.of(
                            movementHistoryEventRepository.findByMovementIdIn(movementIds, Pageable.unpaged()).stream()
                    )
                    .filterBy(MovementHistoryEvent::getType, MovementHistoryEventType.DROPSHIP_TASK_CREATED)
                    .collect(Collectors.toList());
            assertThat(movementsWithCourierFoundEvent).hasSize(2);
            return null;
        });
    }

    @SneakyThrows
    @Test
    void assignSimpleUserShiftSeparately() {
        mockMvc.perform(post("/internal/partner/runs/{runId}/assign-user-shift", run.getId())
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .content(tplObjectMapper.writeValueAsString(new AssignRunDto("a123aa12", false,
                                List.of(new AssignUserShiftDto(null, transport.getId(), null, null, false)))))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                )
                .andExpect(status().isOk());

        var newRun = runRepository.findById(run.getId()).orElseThrow();
        assertThat(newRun.streamUserShifts().toList()).hasSize(1);
        assertThat(newRun.streamUserShifts().findAny().orElseThrow().getTransport()).isEqualTo(transport);

        mockMvc.perform(post("/internal/partner/runs/{runId}/assign-user-shift", run.getId())
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .content(tplObjectMapper.writeValueAsString(new AssignRunDto("б345бб56", false,
                                List.of(new AssignUserShiftDto(null, null, user1.getId(), null, true)))))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                )
                .andExpect(status().isOk());

        transactionTemplate.execute(tc -> {
            var newRun2 = runRepository.findById(run.getId()).orElseThrow();
            assertThat(newRun2.streamUserShifts().toList()).hasSize(1);
            assertThat(newRun2.streamUserShifts().findAny().orElseThrow().getUser()).isEqualTo(user1);
            assertThat(newRun2.getStatus()).isEqualTo(RunStatus.ASSIGNED);
            List<Long> movementIds = newRun2.streamRunItems().map(RunItem::getMovement).map(Movement::getId).collect(Collectors.toList());
            List<MovementHistoryEvent> movementsWithCourierFoundEvent = StreamEx.of(
                            movementHistoryEventRepository.findByMovementIdIn(movementIds, Pageable.unpaged()).stream()
                    )
                    .filterBy(MovementHistoryEvent::getType, MovementHistoryEventType.DROPSHIP_TASK_CREATED)
                    .collect(Collectors.toList());
            assertThat(movementsWithCourierFoundEvent).hasSize(2);
            return null;
        });
    }

    @SneakyThrows
    @Test
    void assignMultiUserShifts() {
        mockMvc.perform(post("/internal/partner/runs/{runId}/assign-user-shift", run.getId())
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .content(tplObjectMapper.writeValueAsString(new AssignRunDto("a123aa12", true,
                                List.of(
                                        new AssignUserShiftDto(null, transport.getId(), user1.getId(), 1, false),
                                        new AssignUserShiftDto(null, transport.getId(), user2.getId(), 1, false)
                                ))))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                )
                .andExpect(status().isOk());

        transactionTemplate.execute(tc -> {
            var newRun = runRepository.findById(run.getId()).orElseThrow();
            assertThat(newRun.streamUserShifts().toList()).hasSize(2);
            var firstUserShift = newRun.getFirstAssignedShift();
            assertThat(firstUserShift.getUser()).isEqualTo(user1);
            assertThat(firstUserShift.getTransport()).isEqualTo(transport);
            assertThat(firstUserShift.getStartDateTime()).isEqualTo(run.getStartDateTime());
            var tasksOnLastRoutePoint = firstUserShift.getLastRoutePoint().getUnfinishedTasks();
            assertThat(tasksOnLastRoutePoint).hasSize(1);
            assertThat(tasksOnLastRoutePoint.get(0).getType()).isEqualTo(TaskType.DRIVER_CHANGE);
            assertThat(((DriverChangeTask) tasksOnLastRoutePoint.get(0)).getSubtype())
                    .isEqualTo(DriverChangeTaskSubtype.PASS);

            assertThat(newRun.streamRunItems().toList()).allMatch(ri -> Objects.equals(ri.getOutboundUserShift().getUser().getId(),
                    user1.getId()));
            return null;
        });


        mockMvc.perform(post("/internal/partner/runs/{runId}/assign-user-shift", run.getId())
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .content(tplObjectMapper.writeValueAsString(new AssignRunDto("a123aa12", true,
                                List.of(new AssignUserShiftDto(null, transport2.getId(), user2.getId(), 3, true)))))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                )
                .andExpect(status().isOk());

        transactionTemplate.execute(tc -> {
            var newRun = runRepository.findById(run.getId()).orElseThrow();
            assertThat(newRun.streamUserShifts().toList()).hasSize(3);
            var secondUserShift = newRun.getLastAssignedShift();
            assertThat(secondUserShift.getUser()).isEqualTo(user2);
            assertThat(secondUserShift.getTransport()).isEqualTo(transport2);
            var shiftStart = run.streamRoutePointData()
                    .filterBy(PlannedRoutePointData::getExplicitOrderNumber, 1)
                    .map(PlannedRoutePointData::getExpectedDepartureTime)
                    .findFirst().orElseThrow();
            assertThat(secondUserShift.getStartDateTime()).isEqualTo(shiftStart);
            var tasksOnLastRoutePoint = secondUserShift.getLastRoutePoint().getUnfinishedTasks();
            assertThat(tasksOnLastRoutePoint).hasSize(1);

            assertThat(tasksOnLastRoutePoint.get(0).getType()).isEqualTo(TaskType.ORDER_RETURN);
            var tasksOnFirstRoutePoint = secondUserShift.getFirstRoutePoint().getUnfinishedTasks();
            assertThat(tasksOnFirstRoutePoint).hasSize(1);
            assertThat(tasksOnFirstRoutePoint.get(0).getType()).isEqualTo(TaskType.DRIVER_CHANGE);
            assertThat(((DriverChangeTask) tasksOnFirstRoutePoint.get(0)).getSubtype())
                    .isEqualTo(DriverChangeTaskSubtype.ACCEPT);

            assertThat(newRun.streamRunItems().toList()).allMatch(ri -> Objects.equals(ri.getOutboundUserShift().getUser().getId(),
                    user1.getId()));
            assertThat(newRun.streamRunItems().toList()).allMatch(ri -> Objects.equals(ri.getInboundUserShift().getUser().getId(),
                    user2.getId()));
            return null;
        });

    }

    @SneakyThrows
    @Test
    void assignMultiUserShiftsTwice() {
        mockMvc.perform(post("/internal/partner/runs/{runId}/assign-user-shift", run.getId())
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .content(tplObjectMapper.writeValueAsString(new AssignRunDto("a123aa12", true,
                                List.of(
                                        new AssignUserShiftDto(null, transport.getId(), user1.getId(), 1, false)
                                ))))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                )
                .andExpect(status().isOk());

        transactionTemplate.execute(tc -> {
            var newRun = runRepository.findById(run.getId()).orElseThrow();
            assertThat(newRun.streamUserShifts().toList()).hasSize(1);
            return null;
        });


        mockMvc.perform(post("/internal/partner/runs/{runId}/assign-user-shift", run.getId())
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .content(tplObjectMapper.writeValueAsString(new AssignRunDto("a123aa12", true,
                                List.of(
                                        new AssignUserShiftDto(null, transport.getId(), user1.getId(), 1, false),
                                        new AssignUserShiftDto(null, transport.getId(), user2.getId(), 1, true)
                                ))))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                )
                .andExpect(status().isOk());

        transactionTemplate.execute(tc -> {
            var newRun = runRepository.findById(run.getId()).orElseThrow();
            assertThat(newRun.streamUserShifts().toList()).hasSize(2);
            return null;
        });

    }

    @SneakyThrows
    @Test
    void assignMultiUserShiftsLastEdge() {
        mockMvc.perform(post("/internal/partner/runs/{runId}/assign-user-shift", run.getId())
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .content(tplObjectMapper.writeValueAsString(new AssignRunDto("a123aa12", true,
                                List.of(
                                        new AssignUserShiftDto(null, transport.getId(), user1.getId(), 1, false),
                                        new AssignUserShiftDto(null, transport.getId(), user1.getId(), 3, false),
                                        new AssignUserShiftDto(null, transport.getId(), user2.getId(), 3, true)
                                ))))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                )
                .andExpect(status().isOk());

        transactionTemplate.execute(tc -> {
            var newRun = runRepository.findById(run.getId()).orElseThrow();
            assertThat(newRun.streamUserShifts().toList()).hasSize(3);

            var firstUserShift = newRun.getFirstAssignedShift();
            assertThat(firstUserShift.getUser()).isEqualTo(user1);
            assertThat(firstUserShift.getTransport()).isEqualTo(transport);
            var tasksOnLastRoutePoint = firstUserShift.getLastRoutePoint().getUnfinishedTasks();
            assertThat(tasksOnLastRoutePoint).hasSize(1);
            assertThat(tasksOnLastRoutePoint.get(0).getType()).isEqualTo(TaskType.DRIVER_CHANGE);
            assertThat(((DriverChangeTask) tasksOnLastRoutePoint.get(0)).getSubtype())
                    .isEqualTo(DriverChangeTaskSubtype.PASS);

            var secondUserShift = newRun.getUserShiftsOrdered().get(1);
            assertThat(secondUserShift.getUser()).isEqualTo(user1);
            assertThat(secondUserShift.getTransport()).isEqualTo(transport);
            var tasksOnFirstRoutePoint = secondUserShift.getFirstRoutePoint().getUnfinishedTasks();
            assertThat(tasksOnFirstRoutePoint).hasSize(1);
            assertThat(tasksOnFirstRoutePoint.get(0).getType()).isEqualTo(TaskType.DRIVER_CHANGE);
            assertThat(((DriverChangeTask) tasksOnFirstRoutePoint.get(0)).getSubtype())
                    .isEqualTo(DriverChangeTaskSubtype.ACCEPT);
            tasksOnLastRoutePoint = secondUserShift.getLastRoutePoint().getUnfinishedTasks();
            assertThat(tasksOnLastRoutePoint).hasSize(1);
            assertThat(tasksOnLastRoutePoint.get(0).getType()).isEqualTo(TaskType.DRIVER_CHANGE);
            assertThat(((DriverChangeTask) tasksOnLastRoutePoint.get(0)).getSubtype())
                    .isEqualTo(DriverChangeTaskSubtype.PASS);

            var lastUserShift = newRun.getLastAssignedShift();
            assertThat(lastUserShift.getUser()).isEqualTo(user2);
            assertThat(lastUserShift.getTransport()).isEqualTo(transport);
            tasksOnLastRoutePoint = lastUserShift.getLastRoutePoint().getUnfinishedTasks();
            assertThat(tasksOnLastRoutePoint).hasSize(1);
            assertThat(tasksOnLastRoutePoint.get(0).getType()).isEqualTo(TaskType.ORDER_RETURN);


            return null;
        });
    }

    @SneakyThrows
    @Test
    void assignMultiUserShiftsWithSeveralShifts() {
        mockMvc.perform(post("/internal/partner/runs/{runId}/assign-user-shift", run.getId())
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .content(tplObjectMapper.writeValueAsString(new AssignRunDto("a123aa12", true,
                                List.of(new AssignUserShiftDto(null, transport.getId(), user1.getId(), 1, false),
                                        new AssignUserShiftDto(null, transport2.getId(), user2.getId(), 3, true)))))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                )
                .andExpect(status().isOk());

        transactionTemplate.execute(tc -> {
            var newRun = runRepository.findById(run.getId()).orElseThrow();
            assertThat(newRun.streamUserShifts().toList()).hasSize(2);
            var firstUserShift = newRun.getFirstAssignedShift();
            assertFalse(firstUserShift.getIsLast());
            assertThat(firstUserShift.getUser()).isEqualTo(user1);
            assertThat(firstUserShift.getTransport()).isEqualTo(transport);
            var tasksOnLastRoutePoint = firstUserShift.getLastRoutePoint().getUnfinishedTasks();
            assertThat(tasksOnLastRoutePoint).hasSize(1);
            assertThat(tasksOnLastRoutePoint.get(0).getType()).isEqualTo(TaskType.DRIVER_CHANGE);
            assertThat(((DriverChangeTask) tasksOnLastRoutePoint.get(0)).getSubtype())
                    .isEqualTo(DriverChangeTaskSubtype.PASS);

            var secondUserShift = newRun.getLastAssignedShift();
            assertTrue(secondUserShift.getIsLast());
            assertThat(secondUserShift.getUser()).isEqualTo(user2);
            assertThat(secondUserShift.getTransport()).isEqualTo(transport2);
            tasksOnLastRoutePoint = secondUserShift.getLastRoutePoint().getUnfinishedTasks();
            assertThat(tasksOnLastRoutePoint).hasSize(1);
            assertThat(tasksOnLastRoutePoint.get(0).getType()).isEqualTo(TaskType.ORDER_RETURN);
            var tasksOnFirstRoutePoint = secondUserShift.getFirstRoutePoint().getUnfinishedTasks();
            assertThat(tasksOnFirstRoutePoint).hasSize(1);
            assertThat(tasksOnFirstRoutePoint.get(0).getType()).isEqualTo(TaskType.DRIVER_CHANGE);
            assertThat(((DriverChangeTask) tasksOnFirstRoutePoint.get(0)).getSubtype())
                    .isEqualTo(DriverChangeTaskSubtype.ACCEPT);

            assertThat(newRun.streamRunItems().toList()).allMatch(ri -> Objects.equals(ri.getOutboundUserShift().getUser().getId(),
                    user1.getId()));
            assertThat(newRun.streamRunItems().toList()).allMatch(ri -> Objects.equals(ri.getInboundUserShift().getUser().getId(),
                    user2.getId()));
            return null;
        });

    }

    @SneakyThrows
    @Test
    void assignMultiUserShiftsWithSeveralDraftShiftsAndUpdate() {
        mockMvc.perform(post("/internal/partner/runs/{runId}/assign-user-shift", run.getId())
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .content(tplObjectMapper.writeValueAsString(new AssignRunDto("a123aa12", true,
                                List.of(new AssignUserShiftDto(null, transport.getId(), null, 1, false),
                                        new AssignUserShiftDto(null, null, user2.getId(), 3, true)))))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                )
                .andExpect(status().isOk());

        transactionTemplate.execute(tc -> {
            var newRun = runRepository.findById(run.getId()).orElseThrow();
            assertThat(newRun.streamUserShifts().toList()).hasSize(2);
            var firstUserShift = newRun.getFirstAssignedShift();
            assertFalse(firstUserShift.getIsLast());
            assertThat(firstUserShift.getStatus()).isEqualTo(UserShiftStatus.DRAFT);
            assertThat(firstUserShift.getUser()).isNull();
            assertThat(firstUserShift.getTransport()).isEqualTo(transport);
            var tasksOnLastRoutePoint = firstUserShift.getLastRoutePoint().getUnfinishedTasks();
            assertThat(tasksOnLastRoutePoint).hasSize(1);
            assertThat(tasksOnLastRoutePoint.get(0).getType()).isEqualTo(TaskType.DRIVER_CHANGE);
            assertThat(((DriverChangeTask) tasksOnLastRoutePoint.get(0)).getSubtype())
                    .isEqualTo(DriverChangeTaskSubtype.PASS);

            var secondUserShift = newRun.getLastAssignedShift();
            assertTrue(secondUserShift.getIsLast());
            assertThat(firstUserShift.getStatus()).isEqualTo(UserShiftStatus.DRAFT);
            assertThat(secondUserShift.getUser()).isEqualTo(user2);
            assertThat(secondUserShift.getTransport()).isNull();
            tasksOnLastRoutePoint = secondUserShift.getLastRoutePoint().getUnfinishedTasks();
            assertThat(tasksOnLastRoutePoint).hasSize(1);
            assertThat(tasksOnLastRoutePoint.get(0).getType()).isEqualTo(TaskType.ORDER_RETURN);
            var tasksOnFirstRoutePoint = secondUserShift.getFirstRoutePoint().getUnfinishedTasks();
            assertThat(tasksOnFirstRoutePoint).hasSize(1);
            assertThat(tasksOnFirstRoutePoint.get(0).getType()).isEqualTo(TaskType.DRIVER_CHANGE);
            assertThat(((DriverChangeTask) tasksOnFirstRoutePoint.get(0)).getSubtype())
                    .isEqualTo(DriverChangeTaskSubtype.ACCEPT);

            return null;
        });
        run = runRepository.findByIdOrThrow(run.getId());
        var firstShift = run.getFirstAssignedShift();
        var secondShift = run.getLastAssignedShift();

        mockMvc.perform(post("/internal/partner/runs/{runId}/assign-user-shift", run.getId())
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .content(tplObjectMapper.writeValueAsString(new AssignRunDto("a123aa12", true,
                                List.of(new AssignUserShiftDto(firstShift.getId(), transport.getId(), user1.getId(), 1, false),
                                        new AssignUserShiftDto(secondShift.getId(), transport2.getId(), user2.getId(), 3, true)))))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                )
                .andExpect(status().isOk());

        transactionTemplate.execute(tc -> {
            var newRun = runRepository.findById(run.getId()).orElseThrow();
            assertThat(newRun.streamUserShifts().toList()).hasSize(2);
            var firstUserShift = newRun.getFirstAssignedShift();
            assertFalse(firstUserShift.getIsLast());
            assertThat(firstUserShift.getStatus()).isEqualTo(UserShiftStatus.SHIFT_CREATED);
            assertThat(firstUserShift.getUser()).isEqualTo(user1);
            assertThat(firstUserShift.getTransport()).isEqualTo(transport);
            var tasksOnLastRoutePoint = firstUserShift.getLastRoutePoint().getUnfinishedTasks();
            assertThat(tasksOnLastRoutePoint).hasSize(1);
            assertThat(tasksOnLastRoutePoint.get(0).getType()).isEqualTo(TaskType.DRIVER_CHANGE);
            assertThat(((DriverChangeTask) tasksOnLastRoutePoint.get(0)).getSubtype())
                    .isEqualTo(DriverChangeTaskSubtype.PASS);

            var secondUserShift = newRun.getLastAssignedShift();
            assertTrue(secondUserShift.getIsLast());
            assertThat(firstUserShift.getStatus()).isEqualTo(UserShiftStatus.SHIFT_CREATED);
            assertThat(secondUserShift.getUser()).isEqualTo(user2);
            assertThat(secondUserShift.getTransport()).isEqualTo(transport2);
            tasksOnLastRoutePoint = secondUserShift.getLastRoutePoint().getUnfinishedTasks();
            assertThat(tasksOnLastRoutePoint).hasSize(1);
            assertThat(tasksOnLastRoutePoint.get(0).getType()).isEqualTo(TaskType.ORDER_RETURN);
            var tasksOnFirstRoutePoint = secondUserShift.getFirstRoutePoint().getUnfinishedTasks();
            assertThat(tasksOnFirstRoutePoint).hasSize(1);
            assertThat(tasksOnFirstRoutePoint.get(0).getType()).isEqualTo(TaskType.DRIVER_CHANGE);
            assertThat(((DriverChangeTask) tasksOnFirstRoutePoint.get(0)).getSubtype())
                    .isEqualTo(DriverChangeTaskSubtype.ACCEPT);

            assertThat(newRun.streamRunItems().toList()).allMatch(ri -> Objects.equals(ri.getOutboundUserShift().getUser().getId(),
                    user1.getId()));
            assertThat(newRun.streamRunItems().toList()).allMatch(ri -> Objects.equals(ri.getInboundUserShift().getUser().getId(),
                    user2.getId()));

            return null;
        });
    }


    @SneakyThrows
    @Test
    void assignMultiUserShiftsWithSeveralShiftsOneTransition() {
        mockMvc.perform(post("/internal/partner/runs/{runId}/assign-user-shift", run2.getId())
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .content(tplObjectMapper.writeValueAsString(new AssignRunDto("a123aa12", true,
                                List.of(new AssignUserShiftDto(null, transport.getId(), user1.getId(), 0, false),
                                        new AssignUserShiftDto(null, transport2.getId(), user2.getId(), 1, true)))))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                )
                .andExpect(status().isOk());

        transactionTemplate.execute(tc -> {
            var newRun = runRepository.findById(run2.getId()).orElseThrow();
            assertThat(newRun.streamUserShifts().toList()).hasSize(2);
            var firstUserShift = newRun.getFirstAssignedShift();
            assertThat(firstUserShift.getUser()).isEqualTo(user1);
            assertThat(firstUserShift.getTransport()).isEqualTo(transport);
            var tasksOnLastRoutePoint = firstUserShift.getLastRoutePoint().getUnfinishedTasks();
            assertThat(tasksOnLastRoutePoint).hasSize(1);
            assertThat(tasksOnLastRoutePoint.get(0).getType()).isEqualTo(TaskType.DRIVER_CHANGE);
            assertThat(((DriverChangeTask) tasksOnLastRoutePoint.get(0)).getSubtype())
                    .isEqualTo(DriverChangeTaskSubtype.PASS);

            var secondUserShift = newRun.getLastAssignedShift();
            assertThat(secondUserShift.getUser()).isEqualTo(user2);
            assertThat(secondUserShift.getTransport()).isEqualTo(transport2);
            tasksOnLastRoutePoint = secondUserShift.getLastRoutePoint().getUnfinishedTasks();
            assertThat(tasksOnLastRoutePoint).hasSize(1);
            assertThat(tasksOnLastRoutePoint.get(0).getType()).isEqualTo(TaskType.ORDER_RETURN);
            var tasksOnFirstRoutePoint = secondUserShift.getFirstRoutePoint().getUnfinishedTasks();
            assertThat(tasksOnFirstRoutePoint).hasSize(1);
            assertThat(tasksOnFirstRoutePoint.get(0).getType()).isEqualTo(TaskType.DRIVER_CHANGE);
            assertThat(((DriverChangeTask) tasksOnFirstRoutePoint.get(0)).getSubtype())
                    .isEqualTo(DriverChangeTaskSubtype.ACCEPT);
            assertThat(newRun.streamRunItems().toList()).allMatch(ri -> Objects.equals(ri.getOutboundUserShift().getUser().getId(),
                    user1.getId()));
            assertThat(newRun.streamRunItems().toList()).allMatch(ri -> Objects.equals(ri.getInboundUserShift().getUser().getId(),
                    user2.getId()));
            return null;
        });

    }

    @SneakyThrows
    @Test
    void assignMultiUserShiftsFailedChangeMultiShiftFlag() {
        mockMvc.perform(post("/internal/partner/runs/{runId}/assign-user-shift", run.getId())
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .content(tplObjectMapper.writeValueAsString(new AssignRunDto("a123aa12", true,
                                List.of(new AssignUserShiftDto(null, transport.getId(), user1.getId(), 1, false)))))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                )
                .andExpect(status().isOk());

        transactionTemplate.execute(tc -> {
            var newRun = runRepository.findById(run.getId()).orElseThrow();
            assertThat(newRun.streamUserShifts().toList()).hasSize(1);
            var firstUserShift = newRun.getLastAssignedShift();
            assertThat(firstUserShift.getUser()).isEqualTo(user1);
            assertThat(firstUserShift.getTransport()).isEqualTo(transport);
            var tasksOnLastRoutePoint = firstUserShift.getLastRoutePoint().getUnfinishedTasks();
            assertThat(tasksOnLastRoutePoint).hasSize(1);
            assertThat(tasksOnLastRoutePoint.get(0).getType()).isEqualTo(TaskType.DRIVER_CHANGE);
            assertThat(((DriverChangeTask) tasksOnLastRoutePoint.get(0)).getSubtype())
                    .isEqualTo(DriverChangeTaskSubtype.PASS);
            return null;
        });


        mockMvc.perform(post("/internal/partner/runs/{runId}/assign-user-shift", run.getId())
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .content(tplObjectMapper.writeValueAsString(new AssignRunDto("a123aa12", false,
                                List.of(new AssignUserShiftDto(null, transport2.getId(), user2.getId(), 2, true)))))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                )
                .andExpect(status().is4xxClientError());

    }
}
