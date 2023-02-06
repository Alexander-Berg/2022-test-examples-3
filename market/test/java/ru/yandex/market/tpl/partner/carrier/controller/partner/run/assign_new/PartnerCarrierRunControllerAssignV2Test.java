package ru.yandex.market.tpl.partner.carrier.controller.partner.run.assign_new;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;

import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.carrier.core.domain.duty.Duty;
import ru.yandex.market.tpl.carrier.core.domain.duty.DutyGenerator;
import ru.yandex.market.tpl.carrier.core.domain.duty.DutyStatus;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.carrier.core.domain.partner.DsRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunDuty;
import ru.yandex.market.tpl.carrier.core.domain.run.RunDutyRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.RunRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.RunStatus;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.user.UserCommandService;
import ru.yandex.market.tpl.carrier.core.domain.user.UserUtil;
import ru.yandex.market.tpl.carrier.core.domain.user.commands.UserCommand;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.Transport;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.TransportRepository;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouse;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseGenerator;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.partner.carrier.BaseTplPartnerCarrierWebIntTest;
import ru.yandex.market.tpl.partner.carrier.model.run.partner.AssignDriverDto;
import ru.yandex.market.tpl.partner.carrier.model.run.partner.AssignRunTransportDto;
import ru.yandex.market.tpl.partner.carrier.model.transport.PartnerCarrierTransportCreateDto;
import ru.yandex.market.tpl.partner.carrier.service.user.transport.PartnerCarrierTransportService;
import ru.yandex.market.tpl.partner.carrier.web.PartnerCompanyHandler;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Sql("classpath:mockPartner/defaultDeliveryServices.sql")
@RequiredArgsConstructor(onConstructor_=@Autowired)
public class PartnerCarrierRunControllerAssignV2Test extends BaseTplPartnerCarrierWebIntTest {

    private static final long SORTING_CENTER_ID = 47819L;

    private final RunGenerator manualRunService;
    private final DutyGenerator dutyGenerator;
    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final TestUserHelper testUserHelper;
    private final PartnerCarrierTransportService partnerCarrierTransportService;
    private final DsRepository dsRepository;
    private final RunRepository runRepository;
    private final RunDutyRepository runDutyRepository;
    private final TransportRepository transportRepository;
    private final UserShiftRepository userShiftRepository;
    private final ObjectMapper tplObjectMapper;
    private final UserCommandService userCommandService;

    private Company company;
    private User user1;
    private User user2;
    private User user3;
    private Transport transport;
    private Transport transport2;
    private Run run;
    private Run run2;
    private Duty duty1;

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
        user3 = testUserHelper.findOrCreateUser(3L, Company.DEFAULT_COMPANY_NAME, UserUtil.ANOTHER_PHONE_2);
        userCommandService.markBlackListed(new UserCommand.MarkBlackListed(
                user3.getId(), null
        ));

        transport = transportRepository.findByIdOrThrow(partnerCarrierTransportService.create(PartnerCarrierTransportCreateDto.builder()
                .name("Машина")
                .capacity(new BigDecimal("1.23"))
                .palletsCapacity(123)
                .number(TestUserHelper.DEFAULT_TRANSPORT_NUMBER)
                .build(), company.getId()).getId());

        transport2 = transportRepository.findByIdOrThrow(partnerCarrierTransportService.create(PartnerCarrierTransportCreateDto.builder()
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
                                        .outboundArrivalTime(ZonedDateTime.of(2021, 8, 5, 4, 0, 0, 0, DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                        .deliveryIntervalFrom(ZonedDateTime.of(2021, 8, 5, 4, 0, 0, 0, DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                        .deliveryIntervalTo(ZonedDateTime.of(2021, 8, 5, 5, 0, 0, 0, DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
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
                                        .outboundArrivalTime(ZonedDateTime.of(2021, 8, 5, 2, 0, 0, 0, DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                        .deliveryIntervalFrom(ZonedDateTime.of(2021, 8, 5, 2, 0, 0, 0, DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                        .deliveryIntervalTo(ZonedDateTime.of(2021, 8, 5, 5, 0, 0, 0, DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
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
                                        .deliveryIntervalFrom(ZonedDateTime.of(2021, 8, 5, 7, 0, 0, 0, DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                        .deliveryIntervalTo(ZonedDateTime.of(2021, 8, 5, 8, 0, 0, 0, DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                        .build(),
                                1,
                                        null,
                                        null
                        )
                )
                .item(
                        new RunGenerator.RunItemGenerateParam(
                                MovementCommand.Create.builder()
                                        .externalId("890")
                                        .orderWarehouse(orderWarehouseGenerator.generateWarehouse())
                                        .orderWarehouseTo(warehouseTo)
                                        .deliveryIntervalFrom(ZonedDateTime.of(2021, 8, 5, 9, 0, 0, 0, DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                        .deliveryIntervalTo(ZonedDateTime.of(2021, 8, 5, 10, 0, 0, 0, DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                        .build(),
                                2,
                                        null,
                                        null
                        )
                )
                .build()
        );

        duty1 = dutyGenerator.generate(d -> DutyGenerator.DutyGenerateParams.builder()
                .deliveryServiceId(deliveryServiceId)
                .dutyStartTime(ZonedDateTime.of(2021, 1, 1, 10, 0, 0, 0, DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                .dutyEndTime(ZonedDateTime.of(2021, 1, 1, 20, 0, 0, 0, DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                .pallets(33)
                .priceCents(700000L)
                .dutyWarehouseId(orderWarehouseGenerator.generateWarehouse().getYandexId()));

    }

    @SneakyThrows
    @Test
    void shouldAssignTransport() {
        mockMvc.perform(post("/internal/partner/runs/{runId}/assign-transport", run.getId())
                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                .content(tplObjectMapper.writeValueAsString(new AssignRunTransportDto(transport.getId())))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("transport").exists())
                .andExpect(jsonPath("transport").isNotEmpty());

        run = runRepository.findByIdOrThrow(run.getId());
        Assertions.assertThat(run.getTransport()).isNotNull();
        Assertions.assertThat(run.getTransport().getId()).isEqualTo(transport.getId());
    }

    @SneakyThrows
    @Test
    void shouldAssignUser() {
        mockMvc.perform(post("/internal/partner/runs/{runId}/assign-driver", run.getId())
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .content(tplObjectMapper.writeValueAsString(new AssignDriverDto(user1.getId())))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("driver").exists())
                .andExpect(jsonPath("driver").isNotEmpty());

        run = runRepository.findByIdOrThrow(run.getId());
        Assertions.assertThat(run.getUser()).isNotNull();
        Assertions.assertThat(run.getUser().getId()).isEqualTo(user1.getId());
    }


    @SneakyThrows
    @Test
    void shouldReassignTransport() {
        mockMvc.perform(post("/internal/partner/runs/{runId}/assign-transport", run.getId())
                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                .content(tplObjectMapper.writeValueAsString(new AssignRunTransportDto(transport.getId())))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
                .andExpect(status().isOk());

        mockMvc.perform(post("/internal/partner/runs/{runId}/assign-transport", run.getId())
                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                .content(tplObjectMapper.writeValueAsString(new AssignRunTransportDto(transport2.getId())))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
                .andExpect(status().isOk());

        run = runRepository.findByIdOrThrow(run.getId());
        Assertions.assertThat(run.getTransport()).isNotNull();
        Assertions.assertThat(run.getTransport().getId()).isEqualTo(transport2.getId());
    }


    @SneakyThrows
    @Test
    void shouldAssignTransportThenUserShift() {
        mockMvc.perform(post("/internal/partner/runs/{runId}/assign-transport", run.getId())
                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                .content(tplObjectMapper.writeValueAsString(new AssignRunTransportDto(transport.getId())))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
                .andExpect(status().isOk());

        mockMvc.perform(post("/internal/partner/runs/{runId}/assign-driver", run.getId())
                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                .content(tplObjectMapper.writeValueAsString(new AssignDriverDto(user1.getId())))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
                .andExpect(status().isOk());

        run = runRepository.findByIdOrThrow(run.getId());
        UserShift userShift = userShiftRepository.findByIdOrThrow(run.getFirstAssignedShift().getId());
        Assertions.assertThat(userShift.getStartDateTime())
                .isEqualTo(ZonedDateTime.of(2021, 8, 5, 2, 0, 0, 0, DateTimeUtil.DEFAULT_ZONE_ID).toInstant());
        Assertions.assertThat(userShift.getEndDateTime())
                .isEqualTo(ZonedDateTime.of(2021, 8, 5, 5, 0, 0, 0, DateTimeUtil.DEFAULT_ZONE_ID).toInstant());

        Assertions.assertThat(userShift.getTransport().getId()).isEqualTo(transport.getId());
        Assertions.assertThat(userShift.getTransport().getTransportType().getId()).isEqualTo(transport.getTransportType().getId());
    }

    @SneakyThrows
    @Test
    void shouldAssignTransportThenUserShiftForDutyAndPropagateToDuty() {
        Run run = runDutyRepository.findByDutyId(duty1.getId()).getRun();

        mockMvc.perform(post("/internal/partner/runs/{runId}/assign-transport", run.getId())
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .content(tplObjectMapper.writeValueAsString(new AssignRunTransportDto(transport.getId())))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                )
                .andExpect(status().isOk());

        mockMvc.perform(post("/internal/partner/runs/{runId}/assign-driver", run.getId())
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .content(tplObjectMapper.writeValueAsString(new AssignDriverDto(user1.getId())))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                )
                .andExpect(status().isOk());

        run = runRepository.findByIdOrThrow(run.getId());
        UserShift userShift = userShiftRepository.findByIdOrThrow(run.getFirstAssignedShift().getId());

        Assertions.assertThat(userShift.getStartDateTime())
                .isEqualTo(ZonedDateTime.of(2021, 1, 1, 10, 0, 0, 0, DateTimeUtil.DEFAULT_ZONE_ID).toInstant());
        Assertions.assertThat(userShift.getEndDateTime())
                .isEqualTo(ZonedDateTime.of(2021, 1, 1, 20, 0, 0, 0, DateTimeUtil.DEFAULT_ZONE_ID).toInstant());

        Assertions.assertThat(userShift.getTransport().getId()).isEqualTo(transport.getId());
        Assertions.assertThat(userShift.getTransport().getTransportType().getId()).isEqualTo(transport.getTransportType().getId());

        RunDuty runDuty = runDutyRepository.findByDutyId(duty1.getId());

        Assertions.assertThat(runDuty.getRun().getStatus()).isEqualTo(RunStatus.ASSIGNED);
        Assertions.assertThat(runDuty.getDuty().getStatus()).isEqualTo(DutyStatus.ASSIGNED);
    }

    @SneakyThrows
    @Test
    void shouldAllowToReassignUser() {
        mockMvc.perform(post("/internal/partner/runs/{runId}/assign-transport", run.getId())
                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                .content(tplObjectMapper.writeValueAsString(new AssignRunTransportDto(transport.getId())))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
                .andExpect(status().isOk());

        mockMvc.perform(post("/internal/partner/runs/{runId}/assign-driver", run.getId())
                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                .content(tplObjectMapper.writeValueAsString(new AssignDriverDto(user1.getId())))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
                .andExpect(status().isOk());

        mockMvc.perform(post("/internal/partner/runs/{runId}/assign-driver", run.getId())
                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                .content(tplObjectMapper.writeValueAsString(new AssignDriverDto(user2.getId())))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
                .andExpect(status().isOk());

        run = runRepository.findByIdOrThrow(run.getId());
        UserShift userShift = userShiftRepository.findByIdOrThrow(run.getFirstAssignedShift().getId());
        testUserHelper.openShift(userShift.getUser(), userShift.getId());

        Assertions.assertThat(userShift.getUser().getId()).isEqualTo(user2.getId());
        Assertions.assertThat(userShift.getTransport().getId()).isEqualTo(transport.getId());
        Assertions.assertThat(userShift.getTransport().getTransportType().getId()).isEqualTo(transport.getTransportType().getId());
    }

    @SneakyThrows
    @Test
    void shouldAllowToReassignTransportIfUserShiftIsAssigned() {
        mockMvc.perform(post("/internal/partner/runs/{runId}/assign-transport", run.getId())
                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                .content(tplObjectMapper.writeValueAsString(new AssignRunTransportDto(transport.getId())))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
                .andExpect(status().isOk());

        mockMvc.perform(post("/internal/partner/runs/{runId}/assign-driver", run.getId())
                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                .content(tplObjectMapper.writeValueAsString(new AssignDriverDto(user1.getId())))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
                .andExpect(status().isOk());

        mockMvc.perform(post("/internal/partner/runs/{runId}/assign-transport", run.getId())
                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                .content(tplObjectMapper.writeValueAsString(new AssignRunTransportDto(transport2.getId())))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
                .andExpect(status().isOk());

        run = runRepository.findByIdOrThrow(run.getId());
        Assertions.assertThat(run.getTransport()).isNotNull();
        Assertions.assertThat(run.getTransport().getId()).isEqualTo(transport2.getId());
        UserShift userShift = userShiftRepository.findByIdOrThrow(run.getFirstAssignedShift().getId());
        Assertions.assertThat(userShift.getTransport().getId()).isEqualTo(transport2.getId());
        Assertions.assertThat(userShift.getTransport().getTransportType().getId()).isEqualTo(transport2.getTransportType().getId());
    }

    @SneakyThrows
    @Test
    void shouldAllowToAssignMultipleRunsOnSameUser() {
        mockMvc.perform(post("/internal/partner/runs/{runId}/assign-transport", run.getId())
                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                .content(tplObjectMapper.writeValueAsString(new AssignRunTransportDto(transport.getId())))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
                .andExpect(status().isOk());

        mockMvc.perform(post("/internal/partner/runs/{runId}/assign-driver", run.getId())
                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                .content(tplObjectMapper.writeValueAsString(new AssignDriverDto(user1.getId())))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
                .andExpect(status().isOk());

        mockMvc.perform(post("/internal/partner/runs/{runId}/assign-transport", run2.getId())
                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                .content(tplObjectMapper.writeValueAsString(new AssignRunTransportDto(transport.getId())))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
                .andExpect(status().isOk());

        mockMvc.perform(post("/internal/partner/runs/{runId}/assign-driver", run2.getId())
                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                .content(tplObjectMapper.writeValueAsString(new AssignDriverDto(user1.getId())))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
                .andExpect(status().isOk());

        run = runRepository.findByIdOrThrow(run.getId());
        testUserHelper.openShift(user1, run.getFirstAssignedShift().getId());
        UserShift activeUserShift = userShiftRepository.findCurrentUserShift(user1).orElseThrow();

        run = runRepository.findByIdOrThrow(run.getId());
        run2 = runRepository.findByIdOrThrow(run2.getId());
        Assertions.assertThat(run.getFirstAssignedShift()).isEqualTo(activeUserShift);
        Assertions.assertThat(run2.getFirstAssignedShift()).isNotEqualTo(activeUserShift);
    }

    @SneakyThrows
    @Test
    void shouldCorrectlyActiveUserShiftIfMultipleRunsOnSameUser() {
        mockMvc.perform(post("/internal/partner/runs/{runId}/assign-transport", run2.getId())
                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                .content(tplObjectMapper.writeValueAsString(new AssignRunTransportDto(transport.getId())))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
                .andExpect(status().isOk());

        mockMvc.perform(post("/internal/partner/runs/{runId}/assign-driver", run2.getId())
                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                .content(tplObjectMapper.writeValueAsString(new AssignDriverDto(user1.getId())))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
                .andExpect(status().isOk());

        mockMvc.perform(post("/internal/partner/runs/{runId}/assign-transport", run.getId())
                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                .content(tplObjectMapper.writeValueAsString(new AssignRunTransportDto(transport.getId())))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
                .andExpect(status().isOk());

        mockMvc.perform(post("/internal/partner/runs/{runId}/assign-driver", run.getId())
                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                .content(tplObjectMapper.writeValueAsString(new AssignDriverDto(user1.getId())))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
                .andExpect(status().isOk());

        run = runRepository.findByIdOrThrow(run.getId());
        testUserHelper.openShift(user1, run.getFirstAssignedShift().getId());
        UserShift activeUserShift = userShiftRepository.findCurrentUserShift(user1).orElseThrow();

        run = runRepository.findByIdOrThrow(run.getId());
        run2 = runRepository.findByIdOrThrow(run2.getId());
        Assertions.assertThat(run.getFirstAssignedShift()).isEqualTo(activeUserShift);
    }

    @SneakyThrows
    @Test
    void shouldNotSwitchOpenedUserShift() {
        mockMvc.perform(post("/internal/partner/runs/{runId}/assign-transport", run2.getId())
                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                .content(tplObjectMapper.writeValueAsString(new AssignRunTransportDto(transport.getId())))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
                .andExpect(status().isOk());

        mockMvc.perform(post("/internal/partner/runs/{runId}/assign-driver", run2.getId())
                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                .content(tplObjectMapper.writeValueAsString(new AssignDriverDto(user1.getId())))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
                .andExpect(status().isOk());

        run2 = runRepository.findByIdOrThrow(run2.getId());
        testUserHelper.openShift(user1, run2.getFirstAssignedShift().getId());
        run2 = runRepository.findByIdOrThrow(run2.getId());
        Assertions.assertThat(run2.getFirstAssignedShift().isActive()).isTrue();


        mockMvc.perform(post("/internal/partner/runs/{runId}/assign-transport", run.getId())
                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                .content(tplObjectMapper.writeValueAsString(new AssignRunTransportDto(transport.getId())))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
                .andExpect(status().isOk());

        mockMvc.perform(post("/internal/partner/runs/{runId}/assign-driver", run.getId())
                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                .content(tplObjectMapper.writeValueAsString(new AssignDriverDto(user1.getId())))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
                .andExpect(status().isOk());

        run = runRepository.findByIdOrThrow(run.getId());
        run2 = runRepository.findByIdOrThrow(run2.getId());

        Assertions.assertThat(run.getFirstAssignedShift().isActive()).isFalse();
        Assertions.assertThat(run2.getFirstAssignedShift().isActive()).isTrue();
    }

    @SneakyThrows
    @Test
    void shouldNotAllowToAssignBlacklistedUser() {
        mockMvc.perform(post("/internal/partner/runs/{runId}/assign-driver", run.getId())
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .content(tplObjectMapper.writeValueAsString(new AssignDriverDto(user3.getId())))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                )
                .andExpect(status().isBadRequest())
                .andDo(log());
    }

    @SneakyThrows
    @Test
    void shouldNotAllowToAssignUserWithoutPassportIfForcePassportDataEnabled() {
        configurationServiceAdapter.mergeValue(
                ConfigurationProperties.FORCE_TO_ASSIGN_USERS_WITH_PASSPORTS_ENABLED, true
        );

        mockMvc.perform(post("/internal/partner/runs/{runId}/assign-driver", run.getId())
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .content(tplObjectMapper.writeValueAsString(new AssignDriverDto(user1.getId())))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("У водителя отсутствуют внесенные паспортные данные"))
                .andDo(log());
    }

}
