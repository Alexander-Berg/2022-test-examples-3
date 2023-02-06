package ru.yandex.market.tpl.partner.carrier.controller.partner.run.multiple_shifts;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;

import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.carrier.core.domain.partner.DsRepository;
import ru.yandex.market.tpl.carrier.core.domain.registry.RegistryRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.RunRepository;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.user.UserUtil;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.Transport;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.TransportRepository;
import ru.yandex.market.tpl.carrier.core.domain.usershift.DriverChangeTask;
import ru.yandex.market.tpl.carrier.core.domain.usershift.DriverChangeTaskSubtype;
import ru.yandex.market.tpl.carrier.core.domain.usershift.TaskType;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShift;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Sql("classpath:mockPartner/defaultDeliveryServices.sql")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class PartnerCarrierRunControllerReassignStartedTest extends BaseTplPartnerCarrierWebIntTest {
    private final RunGenerator manualRunService;
    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final TestUserHelper testUserHelper;
    private final PartnerCarrierTransportService partnerCarrierTransportService;
    private final DsRepository dsRepository;
    private final RunRepository runRepository;
    private final TransportRepository transportRepository;
    private final ObjectMapper tplObjectMapper;

    private Company company;
    private User user1;
    private User user2;
    private User user3;
    private Transport transport;
    private Transport transport2;
    private Transport transport3;
    private Run run;
    private Run run2;

    @BeforeEach
    void setUp() {
        configurationServiceAdapter.mergeValue(ConfigurationProperties.CREATE_REGISTRY_ON_POINTS, true);
        company = testUserHelper.findOrCreateCompany(TestUserHelper.CompanyGenerateParam.builder()
                .campaignId(1234L)
                .companyName(Company.DEFAULT_COMPANY_NAME)
                .login("anotherLogin@yandex.ru")
                .build()
        );

        user1 = testUserHelper.findOrCreateUser(UID, Company.DEFAULT_COMPANY_NAME, UserUtil.PHONE);
        user2 = testUserHelper.findOrCreateUser(ANOTHER_UID, Company.DEFAULT_COMPANY_NAME, UserUtil.ANOTHER_PHONE);
        user3 = testUserHelper.findOrCreateUser(ANOTHER_UID_2, Company.DEFAULT_COMPANY_NAME, UserUtil.ANOTHER_PHONE_2);

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

        transport3 =
                transportRepository.findByIdOrThrow(partnerCarrierTransportService.create(PartnerCarrierTransportCreateDto.builder()
                        .name("Машина3")
                        .capacity(new BigDecimal("3.45"))
                        .palletsCapacity(345)
                        .number("а909мр")
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

    @Nested
    class OneShiftRun extends BaseTplPartnerCarrierWebIntTest {

        @SneakyThrows
        @Test
        void assignMultiUserShiftsSuccessUpdateOnPoint() {
            mockMvc.perform(post("/internal/partner/runs/{runId}/assign-user-shift", run2.getId())
                            .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                            .content(tplObjectMapper.writeValueAsString(new AssignRunDto("a123aa12", false,
                                    List.of(new AssignUserShiftDto(null, transport.getId(), user1.getId(), 1, true)))))
                            .contentType(MediaType.APPLICATION_JSON_UTF8)
                    )
                    .andExpect(status().isOk());

            transactionTemplate.execute(tc -> {
                var newRun = runRepository.findById(run2.getId()).orElseThrow();
                assertThat(newRun.streamUserShifts().toList()).hasSize(1);
                var firstUserShift = newRun.getLastAssignedShift();
                assertThat(firstUserShift.getUser()).isEqualTo(user1);
                assertThat(firstUserShift.getTransport()).isEqualTo(transport);
                var tasksOnLastRoutePoint = firstUserShift.getLastRoutePoint().getUnfinishedTasks();
                assertThat(tasksOnLastRoutePoint).hasSize(1);
                testUserHelper.openShift(user1, firstUserShift.getId());
                testUserHelper.arriveAtRoutePoint(firstUserShift.getCurrentRoutePoint());
                return null;
            });

            UserShift lastAssigned = runRepository.findByIdOrThrow(run2.getId()).getLastAssignedShift();

            mockMvc.perform(post("/internal/partner/runs/{runId}/assign-user-shift", run2.getId())
                            .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                            .content(tplObjectMapper.writeValueAsString(new AssignRunDto("a123aa12", false,
                                    List.of(new AssignUserShiftDto(lastAssigned.getId(), transport2.getId(), user2.getId(), 1, true)))))
                            .contentType(MediaType.APPLICATION_JSON_UTF8)
                    )
                    .andExpect(status().isOk());


            transactionTemplate.execute(tc -> {
                var newRun = runRepository.findById(run2.getId()).orElseThrow();
                assertThat(newRun.streamUserShifts().toList()).hasSize(2);
                var firstUserShift = newRun.getFirstAssignedShift();
                assertThat(firstUserShift.getUser()).isEqualTo(user1);
                assertThat(firstUserShift.getTransport()).isEqualTo(transport);
                var tasksOnFirstRoutePoint = firstUserShift.getFirstRoutePoint().getUnfinishedTasks();
                assertThat(tasksOnFirstRoutePoint).hasSize(1);
                assertThat(tasksOnFirstRoutePoint.get(0).getType()).isEqualTo(TaskType.COLLECT_DROPSHIP);
                var tasksOnLastRoutePoint = firstUserShift.getLastRoutePoint().getUnfinishedTasks();
                assertThat(tasksOnLastRoutePoint).hasSize(1);
                assertThat(tasksOnLastRoutePoint.get(0).getType()).isEqualTo(TaskType.DRIVER_CHANGE);
                assertThat(((DriverChangeTask) tasksOnLastRoutePoint.get(0)).getSubtype())
                        .isEqualTo(DriverChangeTaskSubtype.PASS);

                var lastUserShift = newRun.getLastAssignedShift();
                assertThat(lastUserShift.getUser()).isEqualTo(user2);
                assertThat(lastUserShift.getTransport()).isEqualTo(transport2);
                var tasksOnFirstRoutePointLastShift = lastUserShift.getFirstRoutePoint().getUnfinishedTasks();
                assertThat(tasksOnFirstRoutePointLastShift).hasSize(1);
                assertThat(tasksOnFirstRoutePointLastShift.get(0).getType()).isEqualTo(TaskType.DRIVER_CHANGE);
                assertThat(((DriverChangeTask) tasksOnFirstRoutePointLastShift.get(0)).getSubtype())
                        .isEqualTo(DriverChangeTaskSubtype.ACCEPT);
                tasksOnLastRoutePoint = lastUserShift.getLastRoutePoint().getUnfinishedTasks();
                assertThat(tasksOnLastRoutePoint).hasSize(1);
                assertThat(tasksOnLastRoutePoint.get(0).getType()).isEqualTo(TaskType.ORDER_RETURN);
                return null;
            });

        }

        @SneakyThrows
        @Test
        void assignMultiUserShiftsSuccessUpdateAfterFirst() {
            mockMvc.perform(post("/internal/partner/runs/{runId}/assign-user-shift", run2.getId())
                            .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                            .content(tplObjectMapper.writeValueAsString(new AssignRunDto("a123aa12", false,
                                    List.of(new AssignUserShiftDto(null, transport.getId(), user1.getId(), 1, true)))))
                            .contentType(MediaType.APPLICATION_JSON_UTF8)
                    )
                    .andExpect(status().isOk());

            transactionTemplate.execute(tc -> {
                var newRun = runRepository.findById(run2.getId()).orElseThrow();
                assertThat(newRun.streamUserShifts().toList()).hasSize(1);
                var firstUserShift = newRun.getLastAssignedShift();
                assertThat(firstUserShift.getUser()).isEqualTo(user1);
                assertThat(firstUserShift.getTransport()).isEqualTo(transport);
                var tasksOnLastRoutePoint = firstUserShift.getLastRoutePoint().getUnfinishedTasks();
                assertThat(tasksOnLastRoutePoint).hasSize(1);
                testUserHelper.openShift(user1, firstUserShift.getId());
                testUserHelper.arriveAtRoutePoint(firstUserShift.getCurrentRoutePoint());
                testUserHelper.finishCollectDropships(firstUserShift.getCurrentRoutePoint());
                return null;
            });

            UserShift lastAssigned = runRepository.findByIdOrThrow(run2.getId()).getLastAssignedShift();

            mockMvc.perform(post("/internal/partner/runs/{runId}/assign-user-shift", run2.getId())
                            .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                            .content(tplObjectMapper.writeValueAsString(new AssignRunDto("a123aa12", false,
                                    List.of(new AssignUserShiftDto(lastAssigned.getId(), transport2.getId(), user2.getId(), 1, true)))))
                            .contentType(MediaType.APPLICATION_JSON_UTF8)
                    )
                    .andExpect(status().isOk());


            transactionTemplate.execute(tc -> {
                var newRun = runRepository.findById(run2.getId()).orElseThrow();
                assertThat(newRun.streamUserShifts().toList()).hasSize(2);
                var firstUserShift = newRun.getFirstAssignedShift();
                assertThat(firstUserShift.getUser()).isEqualTo(user1);
                assertThat(firstUserShift.getTransport()).isEqualTo(transport);
                var tasksOnFirstRoutePoint = firstUserShift.getFirstRoutePoint().streamTasks().toList();
                assertThat(tasksOnFirstRoutePoint).hasSize(1);
                assertThat(tasksOnFirstRoutePoint.get(0).getType()).isEqualTo(TaskType.COLLECT_DROPSHIP);
                var tasksOnLastRoutePoint = firstUserShift.getLastRoutePoint().getUnfinishedTasks();
                assertThat(tasksOnLastRoutePoint).hasSize(1);
                assertThat(tasksOnLastRoutePoint.get(0).getType()).isEqualTo(TaskType.DRIVER_CHANGE);
                assertThat(((DriverChangeTask) tasksOnLastRoutePoint.get(0)).getSubtype())
                        .isEqualTo(DriverChangeTaskSubtype.PASS);

                var lastUserShift = newRun.getLastAssignedShift();
                assertThat(lastUserShift.getUser()).isEqualTo(user2);
                assertThat(lastUserShift.getTransport()).isEqualTo(transport2);
                tasksOnFirstRoutePoint = lastUserShift.getFirstRoutePoint().getUnfinishedTasks();
                assertThat(tasksOnFirstRoutePoint).hasSize(1);
                assertThat(tasksOnFirstRoutePoint.get(0).getType()).isEqualTo(TaskType.DRIVER_CHANGE);
                assertThat(((DriverChangeTask) tasksOnFirstRoutePoint.get(0)).getSubtype())
                        .isEqualTo(DriverChangeTaskSubtype.ACCEPT);
                tasksOnLastRoutePoint = lastUserShift.getLastRoutePoint().getUnfinishedTasks();
                assertThat(tasksOnLastRoutePoint).hasSize(1);
                assertThat(tasksOnLastRoutePoint.get(0).getType()).isEqualTo(TaskType.ORDER_RETURN);
                return null;
            });

        }

        @SneakyThrows
        @Test
        void assignMultiUserShiftsSuccessUpdateOnLastPoint() {
            mockMvc.perform(post("/internal/partner/runs/{runId}/assign-user-shift", run2.getId())
                            .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                            .content(tplObjectMapper.writeValueAsString(new AssignRunDto("a123aa12", false,
                                    List.of(new AssignUserShiftDto(null, transport.getId(), user1.getId(), 1, true)))))
                            .contentType(MediaType.APPLICATION_JSON_UTF8)
                    )
                    .andExpect(status().isOk());

            transactionTemplate.execute(tc -> {
                var newRun = runRepository.findById(run2.getId()).orElseThrow();
                assertThat(newRun.streamUserShifts().toList()).hasSize(1);
                var firstUserShift = newRun.getLastAssignedShift();
                assertThat(firstUserShift.getUser()).isEqualTo(user1);
                assertThat(firstUserShift.getTransport()).isEqualTo(transport);
                var tasksOnLastRoutePoint = firstUserShift.getLastRoutePoint().getUnfinishedTasks();
                assertThat(tasksOnLastRoutePoint).hasSize(1);
                testUserHelper.openShift(user1, firstUserShift.getId());
                testUserHelper.arriveAtRoutePoint(firstUserShift.getCurrentRoutePoint());
                testUserHelper.finishCollectDropships(firstUserShift.getCurrentRoutePoint());
                testUserHelper.arriveAtRoutePoint(firstUserShift.getLastRoutePoint());
                return null;
            });

            UserShift lastAssigned = runRepository.findByIdOrThrow(run2.getId()).getLastAssignedShift();

            mockMvc.perform(post("/internal/partner/runs/{runId}/assign-user-shift", run2.getId())
                            .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                            .content(tplObjectMapper.writeValueAsString(new AssignRunDto("a123aa12", false,
                                    List.of(new AssignUserShiftDto(lastAssigned.getId(), transport2.getId(),
                                            user2.getId(), 1, true)))))
                            .contentType(MediaType.APPLICATION_JSON_UTF8)
                    )
                    .andExpect(status().is4xxClientError());
        }

        @SneakyThrows
        @Test
        void assignMultiUserShiftsSuccessUpdateBeforeFirst() {
            mockMvc.perform(post("/internal/partner/runs/{runId}/assign-user-shift", run2.getId())
                            .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                            .content(tplObjectMapper.writeValueAsString(new AssignRunDto("a123aa12", false,
                                    List.of(new AssignUserShiftDto(null, transport.getId(), user1.getId(), 1, true)))))
                            .contentType(MediaType.APPLICATION_JSON_UTF8)
                    )
                    .andExpect(status().isOk());

            transactionTemplate.execute(tc -> {
                var newRun = runRepository.findById(run2.getId()).orElseThrow();
                assertThat(newRun.streamUserShifts().toList()).hasSize(1);
                var firstUserShift = newRun.getLastAssignedShift();
                assertThat(firstUserShift.getUser()).isEqualTo(user1);
                assertThat(firstUserShift.getTransport()).isEqualTo(transport);
                var tasksOnLastRoutePoint = firstUserShift.getLastRoutePoint().getUnfinishedTasks();
                assertThat(tasksOnLastRoutePoint).hasSize(1);
                testUserHelper.openShift(user1, firstUserShift.getId());
                return null;
            });

            UserShift lastAssigned = runRepository.findByIdOrThrow(run2.getId()).getLastAssignedShift();

            mockMvc.perform(post("/internal/partner/runs/{runId}/assign-user-shift", run2.getId())
                            .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                            .content(tplObjectMapper.writeValueAsString(new AssignRunDto("a123aa12", false,
                                    List.of(new AssignUserShiftDto(lastAssigned.getId(), transport2.getId(), user2.getId(), 1, true)))))
                            .contentType(MediaType.APPLICATION_JSON_UTF8)
                    )
                    .andExpect(status().isOk());


            transactionTemplate.execute(tc -> {
                var newRun = runRepository.findById(run2.getId()).orElseThrow();
                assertThat(newRun.streamUserShifts().toList()).hasSize(2);
                var firstUserShift = newRun.getLastAssignedShift();
                assertThat(firstUserShift.getUser()).isEqualTo(user2);
                assertThat(firstUserShift.getTransport()).isEqualTo(transport2);
                var tasksOnLastRoutePoint = firstUserShift.getLastRoutePoint().getUnfinishedTasks();
                assertThat(tasksOnLastRoutePoint).hasSize(1);
                return null;
            });

        }

    }

    @Nested
    class MultipleShiftRun extends BaseTplPartnerCarrierWebIntTest {

        @SneakyThrows
        @Test
        void assignMultiUserShiftsSuccessUpdateFirstShiftStarted() {
            mockMvc.perform(post("/internal/partner/runs/{runId}/assign-user-shift", run2.getId())
                            .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                            .content(tplObjectMapper.writeValueAsString(new AssignRunDto("a123aa12", true,
                                    List.of(new AssignUserShiftDto(null, transport.getId(), user1.getId(), 0, false),
                                            new AssignUserShiftDto(null, transport2.getId(), user2.getId(), 1, true)))))
                            .contentType(MediaType.APPLICATION_JSON_UTF8)
                    )
                    .andExpect(status().isOk());

            UserShift firstAssigned = runRepository.findByIdOrThrow(run2.getId()).getFirstAssignedShift();
            UserShift secondAssigned = runRepository.findByIdOrThrow(run2.getId()).getLastAssignedShift();

            transactionTemplate.execute(tc -> {
                testUserHelper.openShift(user1, firstAssigned.getId());
                return null;
            });


            mockMvc.perform(post("/internal/partner/runs/{runId}/assign-user-shift", run2.getId())
                            .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                            .content(tplObjectMapper.writeValueAsString(new AssignRunDto("a123aa12", true,
                                    List.of(new AssignUserShiftDto(firstAssigned.getId(), transport3.getId(), user3.getId(), 0, false),
                                            new AssignUserShiftDto(secondAssigned.getId(), transport2.getId(), user2.getId(), 1, true)))))
                            .contentType(MediaType.APPLICATION_JSON_UTF8)
                    )
                    .andExpect(status().isOk());


            transactionTemplate.execute(tc -> {
                var newRun = runRepository.findByIdOrThrow(run2.getId());
                assertThat(newRun.streamUserShifts().toList()).hasSize(3);
                var newShift = newRun.streamUserShifts().filter(us -> us.getTransportId().equals(transport3.getId())).findAny().orElseThrow();
                assertThat(newShift.getUser()).isEqualTo(user3);
                assertThat(newShift.getTransport()).isEqualTo(transport3);
                assertThat(newShift.getOrderNumber()).isEqualTo(1);
                var tasksOnLastRoutePoint = newShift.getLastRoutePoint().getUnfinishedTasks();
                assertThat(tasksOnLastRoutePoint).hasSize(1);
                var lastShift = newRun.getLastAssignedShift();
                assertThat(lastShift.getOrderNumber()).isEqualTo(2);
                return null;
            });
        }

        @SneakyThrows
        @Test
        void assignMultiUserShiftsSuccessUpdateTransportFirstShiftStarted() {
            mockMvc.perform(post("/internal/partner/runs/{runId}/assign-user-shift", run2.getId())
                            .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                            .content(tplObjectMapper.writeValueAsString(new AssignRunDto("a123aa12", true,
                                    List.of(new AssignUserShiftDto(null, transport.getId(), user1.getId(), 0, false),
                                            new AssignUserShiftDto(null, transport2.getId(), user2.getId(), 1, true)))))
                            .contentType(MediaType.APPLICATION_JSON_UTF8)
                    )
                    .andExpect(status().isOk());

            UserShift firstAssigned = runRepository.findByIdOrThrow(run2.getId()).getFirstAssignedShift();
            UserShift secondAssigned = runRepository.findByIdOrThrow(run2.getId()).getLastAssignedShift();

            transactionTemplate.execute(tc -> {
                testUserHelper.openShift(user1, firstAssigned.getId());
                return null;
            });


            mockMvc.perform(post("/internal/partner/runs/{runId}/assign-user-shift", run2.getId())
                            .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                            .content(tplObjectMapper.writeValueAsString(new AssignRunDto("a123aa12", true,
                                    List.of(new AssignUserShiftDto(firstAssigned.getId(), transport3.getId(), user1.getId(), 0, false),
                                            new AssignUserShiftDto(secondAssigned.getId(), transport2.getId(), user2.getId(), 1, true)))))
                            .contentType(MediaType.APPLICATION_JSON_UTF8)
                    )
                    .andExpect(status().isOk());


            transactionTemplate.execute(tc -> {
                var newRun = runRepository.findByIdOrThrow(run2.getId());
                assertThat(newRun.streamUserShifts().toList()).hasSize(2);
                var newShift = newRun.getFirstAssignedShift();
                assertThat(newShift.getUser()).isEqualTo(user1);
                assertThat(newShift.getTransport()).isEqualTo(transport3);
                assertThat(newShift.getOrderNumber()).isEqualTo(0);
                var tasksOnLastRoutePoint = newShift.getLastRoutePoint().getUnfinishedTasks();
                assertThat(tasksOnLastRoutePoint).hasSize(1);
                var lastShift = newRun.getLastAssignedShift();
                assertThat(lastShift.getOrderNumber()).isEqualTo(1);
                return null;
            });
        }

        @SneakyThrows
        @Test
        void assignMultiUserShiftsSuccessUpdateFirstShiftNotStarted() {
            mockMvc.perform(post("/internal/partner/runs/{runId}/assign-user-shift", run2.getId())
                            .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                            .content(tplObjectMapper.writeValueAsString(new AssignRunDto("a123aa12", true,
                                    List.of(new AssignUserShiftDto(null, transport.getId(), user1.getId(), 0, false),
                                            new AssignUserShiftDto(null, transport2.getId(), user2.getId(), 1, true)))))
                            .contentType(MediaType.APPLICATION_JSON_UTF8)
                    )
                    .andExpect(status().isOk());

            UserShift firstAssigned = runRepository.findByIdOrThrow(run2.getId()).getFirstAssignedShift();
            UserShift secondAssigned = runRepository.findByIdOrThrow(run2.getId()).getLastAssignedShift();

            mockMvc.perform(post("/internal/partner/runs/{runId}/assign-user-shift", run2.getId())
                            .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                            .content(tplObjectMapper.writeValueAsString(new AssignRunDto("a123aa12", true,
                                    List.of(new AssignUserShiftDto(firstAssigned.getId(), transport3.getId(), user3.getId(), 0, false),
                                            new AssignUserShiftDto(secondAssigned.getId(), transport2.getId(), user2.getId(), 1, true)))))
                            .contentType(MediaType.APPLICATION_JSON_UTF8)
                    )
                    .andExpect(status().isOk());


            transactionTemplate.execute(tc -> {
                var newRun = runRepository.findByIdOrThrow(run2.getId());
                assertThat(newRun.streamUserShifts().toList()).hasSize(2);
                var firstShift = newRun.getFirstAssignedShift();
                assertThat(firstShift.getUser()).isEqualTo(user3);
                assertThat(firstShift.getTransport()).isEqualTo(transport3);
                assertThat(firstShift.getOrderNumber()).isEqualTo(0);
                var tasksOnLastRoutePoint = firstShift.getLastRoutePoint().getUnfinishedTasks();
                assertThat(tasksOnLastRoutePoint).hasSize(1);
                var lastShift = newRun.getLastAssignedShift();
                assertThat(lastShift.getOrderNumber()).isEqualTo(1);
                return null;
            });
        }

        @SneakyThrows
        @Test
        void assignMultiUserShiftsSuccessUpdateBothShiftsNotStarted() {
            mockMvc.perform(post("/internal/partner/runs/{runId}/assign-user-shift", run2.getId())
                            .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                            .content(tplObjectMapper.writeValueAsString(new AssignRunDto("a123aa12", true,
                                    List.of(new AssignUserShiftDto(null, transport.getId(), user1.getId(), 0, false),
                                            new AssignUserShiftDto(null, transport2.getId(), user2.getId(), 1, true)))))
                            .contentType(MediaType.APPLICATION_JSON_UTF8)
                    )
                    .andExpect(status().isOk());

            UserShift firstAssigned = runRepository.findByIdOrThrow(run2.getId()).getFirstAssignedShift();
            UserShift secondAssigned = runRepository.findByIdOrThrow(run2.getId()).getLastAssignedShift();

            mockMvc.perform(post("/internal/partner/runs/{runId}/assign-user-shift", run2.getId())
                            .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                            .content(tplObjectMapper.writeValueAsString(new AssignRunDto("a123aa12", true,
                                    List.of(new AssignUserShiftDto(firstAssigned.getId(), transport2.getId(), user2.getId(), 0, false),
                                            new AssignUserShiftDto(secondAssigned.getId(), transport3.getId(), user3.getId(), 1, true)))))
                            .contentType(MediaType.APPLICATION_JSON_UTF8)
                    )
                    .andExpect(status().isOk());


            transactionTemplate.execute(tc -> {
                var newRun = runRepository.findByIdOrThrow(run2.getId());
                assertThat(newRun.streamUserShifts().toList()).hasSize(2);
                var firstShift = newRun.getFirstAssignedShift();
                assertThat(firstShift.getUser()).isEqualTo(user2);
                assertThat(firstShift.getTransport()).isEqualTo(transport2);
                assertThat(firstShift.getOrderNumber()).isEqualTo(0);
                var tasksOnLastRoutePoint = firstShift.getLastRoutePoint().getUnfinishedTasks();
                assertThat(tasksOnLastRoutePoint).hasSize(1);
                var lastShift = newRun.getLastAssignedShift();
                assertThat(lastShift.getOrderNumber()).isEqualTo(1);
                assertThat(lastShift.getUser()).isEqualTo(user3);
                assertThat(lastShift.getTransport()).isEqualTo(transport3);
                return null;
            });
        }


        @SneakyThrows
        @Test
        void assignMultiUserShiftsSuccessUpdateSecondShiftFirstStarted() {
            mockMvc.perform(post("/internal/partner/runs/{runId}/assign-user-shift", run2.getId())
                            .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                            .content(tplObjectMapper.writeValueAsString(new AssignRunDto("a123aa12", true,
                                    List.of(new AssignUserShiftDto(null, transport.getId(), user1.getId(), 0, false),
                                            new AssignUserShiftDto(null, transport2.getId(), user2.getId(), 1, true)))))
                            .contentType(MediaType.APPLICATION_JSON_UTF8)
                    )
                    .andExpect(status().isOk());

            UserShift firstAssigned = runRepository.findByIdOrThrow(run2.getId()).getFirstAssignedShift();
            UserShift secondAssigned = runRepository.findByIdOrThrow(run2.getId()).getLastAssignedShift();

            transactionTemplate.execute(tc -> {
                testUserHelper.openShift(user1, firstAssigned.getId());
                return null;
            });

            mockMvc.perform(post("/internal/partner/runs/{runId}/assign-user-shift", run2.getId())
                            .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                            .content(tplObjectMapper.writeValueAsString(new AssignRunDto("a123aa12", true,
                                    List.of(new AssignUserShiftDto(firstAssigned.getId(), transport.getId(),
                                                    user1.getId(), 0, false),
                                            new AssignUserShiftDto(secondAssigned.getId(), transport3.getId(),
                                                    user3.getId(), 1, true)))))
                            .contentType(MediaType.APPLICATION_JSON_UTF8)
                    )
                    .andExpect(status().isOk());


            transactionTemplate.execute(tc -> {
                var newRun = runRepository.findByIdOrThrow(run2.getId());
                assertThat(newRun.streamUserShifts().toList()).hasSize(2);
                var firstShift = newRun.getFirstAssignedShift();
                assertThat(firstShift.getUser()).isEqualTo(user1);
                assertThat(firstShift.getTransport()).isEqualTo(transport);
                assertThat(firstShift.getOrderNumber()).isEqualTo(0);
                var tasksOnLastRoutePoint = firstShift.getLastRoutePoint().getUnfinishedTasks();
                assertThat(tasksOnLastRoutePoint).hasSize(1);
                var lastShift = newRun.getLastAssignedShift();
                assertThat(lastShift.getOrderNumber()).isEqualTo(1);
                assertThat(lastShift.getUser()).isEqualTo(user3);
                assertThat(lastShift.getTransport()).isEqualTo(transport3);
                return null;
            });
        }

        @SneakyThrows
        @Test
        void assignMultiUserShiftsSuccessUpdateSecondShiftFirstFinished() {
            mockMvc.perform(post("/internal/partner/runs/{runId}/assign-user-shift", run2.getId())
                            .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                            .content(tplObjectMapper.writeValueAsString(new AssignRunDto("a123aa12", true,
                                    List.of(new AssignUserShiftDto(null, transport.getId(), user1.getId(), 0, false),
                                            new AssignUserShiftDto(null, transport2.getId(), user2.getId(), 1, true)))))
                            .contentType(MediaType.APPLICATION_JSON_UTF8)
                    )
                    .andExpect(status().isOk());

            UserShift firstAssigned = runRepository.findByIdOrThrow(run2.getId()).getFirstAssignedShift();
            UserShift secondAssigned = runRepository.findByIdOrThrow(run2.getId()).getLastAssignedShift();

            transactionTemplate.execute(tc -> {
                var firstShift = runRepository.findByIdOrThrow(run2.getId()).getFirstAssignedShift();
                testUserHelper.openShift(user1, firstShift.getId());
                testUserHelper.finishCollectDropships(firstShift.getFirstRoutePoint());
                testUserHelper.finishDriverChange(firstShift);
                return null;
            });

            mockMvc.perform(post("/internal/partner/runs/{runId}/assign-user-shift", run2.getId())
                            .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                            .content(tplObjectMapper.writeValueAsString(new AssignRunDto("a123aa12", true,
                                    List.of(new AssignUserShiftDto(firstAssigned.getId(), transport.getId(),
                                                    user1.getId(), 0, false),
                                            new AssignUserShiftDto(secondAssigned.getId(), transport3.getId(),
                                                    user3.getId(), 1, true)))))
                            .contentType(MediaType.APPLICATION_JSON_UTF8)
                    )
                    .andExpect(status().isOk());


            transactionTemplate.execute(tc -> {
                var newRun = runRepository.findByIdOrThrow(run2.getId());
                assertThat(newRun.streamUserShifts().toList()).hasSize(2);
                var lastShift = newRun.getLastAssignedShift();
                assertThat(lastShift.getOrderNumber()).isEqualTo(1);
                assertThat(lastShift.getUser()).isEqualTo(user3);
                assertThat(lastShift.getTransport()).isEqualTo(transport3);
                return null;
            });
        }

        @SneakyThrows
        @Test
        void assignMultiUserShiftsSuccessUpdateSecondShiftFirstFinishedSecondStartedNotArrived() {
            mockMvc.perform(post("/internal/partner/runs/{runId}/assign-user-shift", run2.getId())
                            .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                            .content(tplObjectMapper.writeValueAsString(new AssignRunDto("a123aa12", true,
                                    List.of(new AssignUserShiftDto(null, transport.getId(), user1.getId(), 0, false),
                                            new AssignUserShiftDto(null, transport2.getId(), user2.getId(), 1, true)))))
                            .contentType(MediaType.APPLICATION_JSON_UTF8)
                    )
                    .andExpect(status().isOk());

            UserShift firstAssigned = runRepository.findByIdOrThrow(run2.getId()).getFirstAssignedShift();
            UserShift secondAssigned = runRepository.findByIdOrThrow(run2.getId()).getLastAssignedShift();

            transactionTemplate.execute(tc -> {
                var firstShift = runRepository.findByIdOrThrow(run2.getId()).getFirstAssignedShift();
                testUserHelper.openShift(user1, firstShift.getId());
                testUserHelper.finishCollectDropships(firstShift.getFirstRoutePoint());
                testUserHelper.finishDriverChange(firstShift);
                var secondShift = runRepository.findByIdOrThrow(run2.getId()).getLastAssignedShift();
                testUserHelper.openShift(user2, secondShift.getId());
                return null;
            });

            mockMvc.perform(post("/internal/partner/runs/{runId}/assign-user-shift", run2.getId())
                            .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                            .content(tplObjectMapper.writeValueAsString(new AssignRunDto("a123aa12", true,
                                    List.of(new AssignUserShiftDto(firstAssigned.getId(), transport.getId(),
                                                    user1.getId(), 0, false),
                                            new AssignUserShiftDto(secondAssigned.getId(), transport3.getId(),
                                                    user3.getId(), 1, true)))))
                            .contentType(MediaType.APPLICATION_JSON_UTF8)
                    )
                    .andExpect(status().isOk());


            transactionTemplate.execute(tc -> {
                var newRun = runRepository.findByIdOrThrow(run2.getId());
                assertThat(newRun.streamUserShifts().toList()).hasSize(3);
                var lastShift = newRun.getLastAssignedShift();
                assertThat(lastShift.getOrderNumber()).isEqualTo(2);
                assertThat(lastShift.getFirstRoutePoint().getUnfinishedTasks().get(0).getType())
                        .isEqualTo(TaskType.DRIVER_CHANGE);
                assertThat(lastShift.getUser()).isEqualTo(user3);
                assertThat(lastShift.getTransport()).isEqualTo(transport3);
                return null;
            });
        }

        @SneakyThrows
        @Test
        void assignMultiUserShiftsSuccessUpdateSecondShiftFirstFinishedSecondStartedArrived() {
            mockMvc.perform(post("/internal/partner/runs/{runId}/assign-user-shift", run2.getId())
                            .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                            .content(tplObjectMapper.writeValueAsString(new AssignRunDto("a123aa12", true,
                                    List.of(new AssignUserShiftDto(null, transport.getId(), user1.getId(), 0, false),
                                            new AssignUserShiftDto(null, transport2.getId(), user2.getId(), 1, true)))))
                            .contentType(MediaType.APPLICATION_JSON_UTF8)
                    )
                    .andExpect(status().isOk());

            UserShift firstAssigned = runRepository.findByIdOrThrow(run2.getId()).getFirstAssignedShift();
            UserShift secondAssigned = runRepository.findByIdOrThrow(run2.getId()).getLastAssignedShift();

            transactionTemplate.execute(tc -> {
                var firstShift = runRepository.findByIdOrThrow(run2.getId()).getFirstAssignedShift();
                testUserHelper.openShift(user1, firstShift.getId());
                testUserHelper.finishCollectDropships(firstShift.getFirstRoutePoint());
                testUserHelper.finishDriverChange(firstShift);
                var secondShift = runRepository.findByIdOrThrow(run2.getId()).getLastAssignedShift();
                testUserHelper.openShift(user2, secondShift.getId());
                testUserHelper.finishDriverChange(secondShift);
                return null;
            });

            mockMvc.perform(post("/internal/partner/runs/{runId}/assign-user-shift", run2.getId())
                            .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                            .content(tplObjectMapper.writeValueAsString(new AssignRunDto("a123aa12", true,
                                    List.of(new AssignUserShiftDto(firstAssigned.getId(), transport.getId(),
                                                    user1.getId(), 0, false),
                                            new AssignUserShiftDto(secondAssigned.getId(), transport3.getId(),
                                                    user3.getId(), 1, true)))))
                            .contentType(MediaType.APPLICATION_JSON_UTF8)
                    )
                    .andExpect(status().isOk());


            transactionTemplate.execute(tc -> {
                var newRun = runRepository.findByIdOrThrow(run2.getId());
                assertThat(newRun.streamUserShifts().toList()).hasSize(3);
                var lastShift = newRun.getLastAssignedShift();
                assertThat(lastShift.getOrderNumber()).isEqualTo(2);
                assertThat(lastShift.getFirstRoutePoint().getUnfinishedTasks().get(0).getType())
                        .isEqualTo(TaskType.DRIVER_CHANGE);
                assertThat(lastShift.getUser()).isEqualTo(user3);
                assertThat(lastShift.getTransport()).isEqualTo(transport3);
                return null;
            });
        }

        @SneakyThrows
        @Test
        void assignMultiUserShiftsSuccessUpdateFirstStarted() {
            mockMvc.perform(post("/internal/partner/runs/{runId}/assign-user-shift", run2.getId())
                            .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                            .content(tplObjectMapper.writeValueAsString(new AssignRunDto("a123aa12", true,
                                    List.of(new AssignUserShiftDto(null, transport.getId(), user1.getId(), 0, false)))))
                            .contentType(MediaType.APPLICATION_JSON_UTF8)
                    )
                    .andExpect(status().isOk());

            UserShift firstAssigned = runRepository.findByIdOrThrow(run2.getId()).getFirstAssignedShift();

            transactionTemplate.execute(tc -> {
                testUserHelper.openShift(user1, firstAssigned.getId());
                return null;
            });

            mockMvc.perform(post("/internal/partner/runs/{runId}/assign-user-shift", run2.getId())
                            .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                            .content(tplObjectMapper.writeValueAsString(new AssignRunDto("a123aa12", true,
                                    List.of(new AssignUserShiftDto(firstAssigned.getId(), transport3.getId(),
                                            user3.getId(), 0, false)))))
                            .contentType(MediaType.APPLICATION_JSON_UTF8)
                    )
                    .andExpect(status().isOk());


            transactionTemplate.execute(tc -> {
                var newRun = runRepository.findByIdOrThrow(run2.getId());
                assertThat(newRun.streamUserShifts().toList()).hasSize(2);
                var firstShift = newRun.getFirstAssignedShift();
                assertThat(firstShift.getUser()).isEqualTo(user1);
                assertThat(firstShift.getTransport()).isEqualTo(transport);
                assertThat(firstShift.getOrderNumber()).isEqualTo(0);
                var tasksOnLastRoutePoint = firstShift.getLastRoutePoint().getUnfinishedTasks();
                assertThat(tasksOnLastRoutePoint).hasSize(1);
                assertThat(tasksOnLastRoutePoint.get(0).getType()).isEqualTo(TaskType.DRIVER_CHANGE);
                var lastShift = newRun.getLastAssignedShift();
                assertThat(lastShift.getOrderNumber()).isEqualTo(1);
                assertThat(lastShift.getUser()).isEqualTo(user3);
                assertThat(lastShift.getTransport()).isEqualTo(transport3);
                var tasksOnFirstRoutePoint = lastShift.getFirstRoutePoint().getUnfinishedTasks();
                assertThat(tasksOnFirstRoutePoint).hasSize(1);
                assertThat(tasksOnFirstRoutePoint.get(0).getType()).isEqualTo(TaskType.COLLECT_DROPSHIP);
                return null;
            });
        }


        @SneakyThrows
        @Test
        void assignMultiUserShiftsSuccessUpdateFirstStartedBetweenPoints() {
            mockMvc.perform(post("/internal/partner/runs/{runId}/assign-user-shift", run2.getId())
                            .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                            .content(tplObjectMapper.writeValueAsString(new AssignRunDto("a123aa12", true,
                                    List.of(new AssignUserShiftDto(null, transport.getId(), user1.getId(), 0, false)))))
                            .contentType(MediaType.APPLICATION_JSON_UTF8)
                    )
                    .andExpect(status().isOk());

            UserShift firstAssigned = runRepository.findByIdOrThrow(run2.getId()).getFirstAssignedShift();

            transactionTemplate.execute(tc -> {
                var firstShift = runRepository.findByIdOrThrow(run2.getId()).getFirstAssignedShift();
                testUserHelper.openShift(user1, firstShift.getId());
                testUserHelper.arriveAtRoutePoint(firstShift.getFirstRoutePoint());
                testUserHelper.finishCollectDropships(firstShift.getFirstRoutePoint());
                return null;
            });

            mockMvc.perform(post("/internal/partner/runs/{runId}/assign-user-shift", run2.getId())
                            .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                            .content(tplObjectMapper.writeValueAsString(new AssignRunDto("a123aa12", true,
                                    List.of(new AssignUserShiftDto(firstAssigned.getId(), transport3.getId(),
                                            user3.getId(), 0, false)))))
                            .contentType(MediaType.APPLICATION_JSON_UTF8)
                    )
                    .andExpect(status().isOk());


            transactionTemplate.execute(tc -> {
                var newRun = runRepository.findByIdOrThrow(run2.getId());
                assertThat(newRun.streamUserShifts().toList()).hasSize(2);
                var firstShift = newRun.getFirstAssignedShift();
                assertThat(firstShift.getUser()).isEqualTo(user1);
                assertThat(firstShift.getTransport()).isEqualTo(transport);
                assertThat(firstShift.getOrderNumber()).isEqualTo(0);
                var tasksOnLastRoutePoint = firstShift.getLastRoutePoint().getUnfinishedTasks();
                assertThat(tasksOnLastRoutePoint).hasSize(1);
                assertThat(tasksOnLastRoutePoint.get(0).getType()).isEqualTo(TaskType.DRIVER_CHANGE);
                var lastShift = newRun.getLastAssignedShift();
                assertThat(lastShift.getOrderNumber()).isEqualTo(1);
                assertThat(lastShift.getUser()).isEqualTo(user3);
                assertThat(lastShift.getTransport()).isEqualTo(transport3);
                var tasksOnFirstRoutePoint = lastShift.getFirstRoutePoint().getUnfinishedTasks();
                assertThat(tasksOnFirstRoutePoint).hasSize(1);
                assertThat(tasksOnFirstRoutePoint.get(0).getType()).isEqualTo(TaskType.DRIVER_CHANGE);
                return null;
            });
        }

        @SneakyThrows
        @Test
        void assignMultiUserShiftsSuccessUpdateForUpdatedShift() {
            mockMvc.perform(post("/internal/partner/runs/{runId}/assign-user-shift", run2.getId())
                            .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                            .content(tplObjectMapper.writeValueAsString(new AssignRunDto("a123aa12", false,
                                    List.of(new AssignUserShiftDto(null, transport.getId(), user1.getId(), 1, true)))))
                            .contentType(MediaType.APPLICATION_JSON_UTF8)
                    )
                    .andExpect(status().isOk());
            transactionTemplate.execute(tc -> {
                var newRun = runRepository.findById(run2.getId()).orElseThrow();
                assertThat(newRun.streamUserShifts().toList()).hasSize(1);
                var firstUserShift = newRun.getLastAssignedShift();
                assertThat(firstUserShift.getUser()).isEqualTo(user1);
                assertThat(firstUserShift.getTransport()).isEqualTo(transport);
                var tasksOnLastRoutePoint = firstUserShift.getLastRoutePoint().getUnfinishedTasks();
                assertThat(tasksOnLastRoutePoint).hasSize(1);
                testUserHelper.openShift(user1, firstUserShift.getId());
                testUserHelper.arriveAtRoutePoint(firstUserShift.getCurrentRoutePoint());
                testUserHelper.finishCollectDropships(firstUserShift.getCurrentRoutePoint());
                return null;
            });

            UserShift lastAssigned = runRepository.findByIdOrThrow(run2.getId()).getLastAssignedShift();

            mockMvc.perform(post("/internal/partner/runs/{runId}/assign-user-shift", run2.getId())
                            .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                            .content(tplObjectMapper.writeValueAsString(new AssignRunDto("a123aa12", false,
                                    List.of(new AssignUserShiftDto(lastAssigned.getId(), transport2.getId(), user2.getId(), 1, true)))))
                            .contentType(MediaType.APPLICATION_JSON_UTF8)
                    )
                    .andExpect(status().isOk());


            transactionTemplate.execute(tc -> {
                var newRun = runRepository.findById(run2.getId()).orElseThrow();
                assertThat(newRun.streamUserShifts().toList()).hasSize(2);
                var firstUserShift = newRun.getLastAssignedShift();
                assertThat(firstUserShift.getUser()).isEqualTo(user2);
                assertThat(firstUserShift.getTransport()).isEqualTo(transport2);
                var tasksOnLastRoutePoint = firstUserShift.getLastRoutePoint().getUnfinishedTasks();
                assertThat(tasksOnLastRoutePoint).hasSize(1);
                return null;
            });

            lastAssigned = runRepository.findByIdOrThrow(run2.getId()).getLastAssignedShift();

            mockMvc.perform(post("/internal/partner/runs/{runId}/assign-user-shift", run2.getId())
                            .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                            .content(tplObjectMapper.writeValueAsString(new AssignRunDto("a123aa12", false,
                                    List.of(new AssignUserShiftDto(lastAssigned.getId(), transport2.getId(), user1.getId(), 1, true)))))
                            .contentType(MediaType.APPLICATION_JSON_UTF8)
                    )
                    .andExpect(status().isOk());


            transactionTemplate.execute(tc -> {
                var newRun = runRepository.findById(run2.getId()).orElseThrow();
                assertThat(newRun.streamUserShifts().toList()).hasSize(2);
                var firstUserShift = newRun.getLastAssignedShift();
                assertThat(firstUserShift.getUser()).isEqualTo(user1);
                assertThat(firstUserShift.getTransport()).isEqualTo(transport2);
                var tasksOnLastRoutePoint = firstUserShift.getLastRoutePoint().getUnfinishedTasks();
                assertThat(tasksOnLastRoutePoint).hasSize(1);
                return null;
            });

        }
    }

}
