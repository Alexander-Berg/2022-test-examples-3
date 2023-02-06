package ru.yandex.market.tpl.partner.carrier.controller.partner.run;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.duty.Duty;
import ru.yandex.market.tpl.carrier.core.domain.duty.DutyGenerator;
import ru.yandex.market.tpl.carrier.core.domain.duty.DutyRepository;
import ru.yandex.market.tpl.carrier.core.domain.duty.DutyStatus;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementStatus;
import ru.yandex.market.tpl.carrier.core.domain.partner.DsRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.RunRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.RunStatus;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseGenerator;
import ru.yandex.market.tpl.partner.carrier.BaseTplPartnerCarrierWebIntTest;
import ru.yandex.market.tpl.partner.carrier.web.PartnerCompanyHandler;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Sql("classpath:mockPartner/defaultDeliveryServices.sql")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class PartnerCarrierRunControllerConfirmAndRejectTest extends BaseTplPartnerCarrierWebIntTest {
    private static final long SORTING_CENTER_ID = 47819L;

    private final TestUserHelper testUserHelper;
    private final RunGenerator manualRunService;
    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final DutyGenerator dutyGenerator;

    private final DsRepository dsRepository;
    private final RunRepository runRepository;
    private final DutyRepository dutyRepository;


    private Company company;
    private User user1;
    private Run run;
    private Run runWithDuty;
    private Duty duty;


    @BeforeEach
    void setUp() {

        company = testUserHelper.findOrCreateCompany(TestUserHelper.CompanyGenerateParam.builder()
                .campaignId(1234L)
                .companyName(Company.DEFAULT_COMPANY_NAME)
                .login("anotherLogin@yandex.ru")
                .build()
        );
        Long deliveryServiceId = dsRepository.findByCompaniesId(company.getId()).iterator().next().getId();


        user1 = testUserHelper.findOrCreateUser(UID);

        run = manualRunService.generate(RunGenerator.RunGenerateParam.builder()
                .externalId("asd")
                .deliveryServiceId(deliveryServiceId)
                .campaignId(company.getCampaignId())
                .runDate(LocalDate.now())
                .items(List.of(
                        new RunGenerator.RunItemGenerateParam(
                                MovementCommand.Create.builder()
                                        .externalId("123")
                                        .orderWarehouse(orderWarehouseGenerator.generateWarehouse())
                                        .orderWarehouseTo(orderWarehouseGenerator.generateWarehouse())
                                        .build(),
                                1,
                                null,
                                null
                        ),
                        new RunGenerator.RunItemGenerateParam(
                                MovementCommand.Create.builder()
                                        .externalId("345")
                                        .orderWarehouse(orderWarehouseGenerator.generateWarehouse())
                                        .orderWarehouseTo(orderWarehouseGenerator.generateWarehouse())
                                        .build(),
                                2,
                                null,
                                null
                        )
                )).build()
        );

        duty = dutyGenerator.generate(DutyGenerator.DutyGenerateParams.builder()
                .dutyStartTime(Instant.parse("2021-01-01T10:00:00.00Z"))
                .dutyEndTime(Instant.parse("2021-01-01T22:00:00.00Z"))
                .pallets(33)
                .priceCents(600000L)
                .dutyWarehouseId(orderWarehouseGenerator.generateWarehouse().getYandexId())
                .deliveryServiceId(deliveryServiceId)
                .build());
        runWithDuty = duty
                .getRunDuty()
                .get(0)
                .getRun();
    }

    @SneakyThrows
    @Test
    void shouldConfirmRunAndPropagateToMovements() {
        mockMvc.perform(
                post("/internal/partner/runs/{runId}/confirm", run.getId())
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
        )
                .andExpect(status().isOk())
                .andExpect(ResultMatcher.matchAll(
                        jsonPath("$.id").value(run.getId()),
                        jsonPath("$.status").value(RunStatus.CONFIRMED.name())
                ));

        transactionTemplate.execute(tc -> {
            run = runRepository.findByIdOrThrow(this.run.getId());

            Assertions.assertThat(run.getStatus()).isEqualTo(RunStatus.CONFIRMED);

            Assertions.assertThat(run.streamMovements().toList())
                    .allMatch(m -> m.getStatus() == MovementStatus.INITIALLY_CONFIRMED);
            return null;
        });
    }

    @SneakyThrows
    @Test
    void shouldConfirmRunAndPropagateToDuty() {
        mockMvc.perform(
                        post("/internal/partner/runs/{runId}/confirm", runWithDuty.getId())
                                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                )
                .andExpect(status().isOk())
                .andExpect(ResultMatcher.matchAll(
                        jsonPath("$.id").value(runWithDuty.getId()),
                        jsonPath("$.status").value(RunStatus.CONFIRMED.name())
                ));

        transactionTemplate.execute(tc -> {
            runWithDuty = runRepository.findByIdOrThrow(this.runWithDuty.getId());

            Assertions.assertThat(runWithDuty.getStatus()).isEqualTo(RunStatus.CONFIRMED);

            Assertions.assertThat(runWithDuty.streamRunDuties().toList())
                    .allMatch(runDuty -> runDuty.getDuty().getStatus() == DutyStatus.CONFIRMED);
            return null;
        });
    }

    @SneakyThrows
    @Test
    void shouldRejectRunAndPropagateToMovements() {
        mockMvc.perform(
                        post("/internal/partner/runs/{runId}/reject", run.getId())
                                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                )
                .andExpect(status().isOk())
                .andExpect(ResultMatcher.matchAll(
                        jsonPath("$.id").value(run.getId()),
                        jsonPath("$.status").value(RunStatus.CANCELLED.name())
                ));

        transactionTemplate.execute(tc -> {
            run = runRepository.findByIdOrThrow(this.run.getId());

            Assertions.assertThat(run.getStatus()).isEqualTo(RunStatus.CANCELLED);

            Assertions.assertThat(run.streamMovements().toList())
                    .allMatch(m -> m.getStatus() == MovementStatus.CANCELLED);
            return null;
        });
    }

    @SneakyThrows
    @Test
    void shouldRejectRunAndPropagateToDuty() {
        mockMvc.perform(
                        post("/internal/partner/runs/{runId}/reject", duty.getRun().getId())
                                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                )
                .andExpect(status().isOk())
                .andExpect(ResultMatcher.matchAll(
                        jsonPath("$.id").value(duty.getRun().getId()),
                        jsonPath("$.status").value(RunStatus.CANCELLED.name())
                ));

        run = runRepository.findByIdOrThrow(duty.getRun().getId());
        duty = dutyRepository.findByIdOrThrow(duty.getId());

        Assertions.assertThat(run.getStatus()).isEqualTo(RunStatus.CANCELLED);

        Assertions.assertThat(duty.getStatus()).isEqualTo(DutyStatus.CANCELLED);
    }
}
