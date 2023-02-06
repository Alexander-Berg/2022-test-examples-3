package ru.yandex.market.tpl.partner.carrier.controller.partner.run;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.tpl.carrier.core.audit.CarrierAuditTracer;
import ru.yandex.market.tpl.carrier.core.audit.CarrierSource;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementGenerator;
import ru.yandex.market.tpl.carrier.core.domain.partner.DsRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.RunRepository;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.Transport;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.carrier.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseGenerator;
import ru.yandex.market.tpl.partner.carrier.BaseTplPartnerCarrierWebIntTest;
import ru.yandex.market.tpl.partner.carrier.model.run.partner.AssignDriverDto;
import ru.yandex.market.tpl.partner.carrier.model.run.partner.AssignRunTransportDto;
import ru.yandex.market.tpl.partner.carrier.web.PartnerCompanyHandler;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Sql("classpath:mockPartner/defaultDeliveryServices.sql")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class PartnerCarrierRunControllerAssignMultipleTest extends BaseTplPartnerCarrierWebIntTest {

    static {
        CarrierAuditTracer.putSource(CarrierSource.SYSTEM);
    }

    private static final long SORTING_CENTER_ID = 47819L;
    private final MovementGenerator movementGenerator;
    private final RunGenerator manualRunService;
    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final TestUserHelper testUserHelper;

    private final DsRepository dsRepository;
    private final UserShiftCommandService userShiftCommandService;
    private final RunRepository runRepository;
    private final Clock clock;
    private final ObjectMapper tplObjectMapper;

    private Company simpleCompany;

    private Transport transport;
    private User user;

    private Run run1;
    private Run run2;

    @BeforeEach
    void setUp() {
        LocalDate today = LocalDate.now(clock);
        LocalDate tomorrow = today.plusDays(1);

        simpleCompany = testUserHelper.findOrCreateCompany(TestUserHelper.CompanyGenerateParam.builder()
                .campaignId(1234L)
                .companyName(Company.DEFAULT_COMPANY_NAME)
                .login("anotherLogin@yandex.ru")
                .build());

        Long deliveryServiceId = dsRepository.findByCompaniesId(simpleCompany.getId()).iterator().next().getId();

        run1 = manualRunService.generate(b -> b
                .externalId("1")
                .deliveryServiceId(deliveryServiceId)
                .campaignId(simpleCompany.getCampaignId())
                .runDate(today)
                .clearItems()
                .items(
                        List.of(
                                new RunGenerator.RunItemGenerateParam(
                                        MovementCommand.Create
                                                .builder()
                                                .externalId("123")
                                                .orderWarehouse(orderWarehouseGenerator.generateWarehouse())
                                                .orderWarehouseTo(orderWarehouseGenerator.generateWarehouse())
                                                .build()
                                        ,
                                        1,
                                        null,
                                        null
                                )
                        )
                )
        );

        run2 = manualRunService.generate(b -> b
                .externalId("2")
                .deliveryServiceId(deliveryServiceId)
                .campaignId(simpleCompany.getCampaignId())
                .runDate(tomorrow)
                .clearItems()
                .items(
                        List.of(
                                new RunGenerator.RunItemGenerateParam(
                                        MovementCommand.Create
                                                .builder()
                                                .externalId("234")
                                                .orderWarehouse(orderWarehouseGenerator.generateWarehouse())
                                                .orderWarehouseTo(orderWarehouseGenerator.generateWarehouse())
                                                .build()
                                        ,
                                        1,
                                        null,
                                        null
                                )
                        )
                )
        );

        user = testUserHelper.findOrCreateUser(1L);
        transport = testUserHelper.findOrCreateTransport();
    }

    @SneakyThrows
    @Test
    void shouldAssignToDifferentShifts() {
        performAssignMovement(run1)
                .andExpect(status().isOk());

        run1 = runRepository.findByIdOrThrow(run1.getId());

        testUserHelper.openShift(user, run1.getFirstAssignedShift().getId());
        transactionTemplate.execute(tc -> {
            run1 = runRepository.findByIdOrThrow(run1.getId());
            var shift = run1.getFirstAssignedShift();
            shift.streamCollectDropshipTasks().forEach(cdt -> {
                testUserHelper.arriveAtRoutePoint(shift, cdt.getRoutePoint().getId());
                userShiftCommandService.collectDropships(user, new UserShiftCommand.CollectDropships(
                        shift.getId(), cdt.getRoutePoint().getId(), cdt.getId()
                ));
            });
            testUserHelper.finishFullReturnAtEnd(shift);
            return null;
        });


        performAssignMovement(run2)
                .andExpect(status().isOk());

    }

    @SneakyThrows
    private ResultActions performAssignMovement(Run run) {
        mockMvc.perform(
                post("/internal/partner/runs/{runId}/assign-transport", run.getId())
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(tplObjectMapper.writeValueAsString(new AssignRunTransportDto(transport.getId())))
                        .header(PartnerCompanyHandler.COMPANY_HEADER, simpleCompany.getCampaignId())
        );
        return mockMvc.perform(
                post("/internal/partner/runs/{runId}/assign", run.getId())
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(tplObjectMapper.writeValueAsString(new AssignDriverDto(user.getId())))
                        .header(PartnerCompanyHandler.COMPANY_HEADER, simpleCompany.getCampaignId())
        );
    }
}
