package ru.yandex.market.tpl.partner.carrier.controller.partner.run;

import java.time.LocalDate;
import java.time.Month;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.carrier.core.domain.partner.DsRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.RunRepository;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.Transport;
import ru.yandex.market.tpl.carrier.core.domain.usershift.CollectDropshipTask;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.carrier.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseGenerator;
import ru.yandex.market.tpl.partner.carrier.BaseTplPartnerCarrierWebIntTest;
import ru.yandex.market.tpl.partner.carrier.model.run.RunStatus;
import ru.yandex.market.tpl.partner.carrier.model.run.partner.AssignDriverDto;
import ru.yandex.market.tpl.partner.carrier.model.run.partner.AssignRunTransportDto;
import ru.yandex.market.tpl.partner.carrier.web.PartnerCompanyHandler;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Sql("classpath:mockPartner/defaultDeliveryServices.sql")
@RequiredArgsConstructor(onConstructor_=@Autowired)
public class PartnerCarrierRunControllerCompleteTest extends BaseTplPartnerCarrierWebIntTest {

    private final RunGenerator runGenerator;
    private final TestUserHelper testUserHelper;
    private final ObjectMapper tplObjectMapper;

    private final OrderWarehouseGenerator orderWarehouseGenerator;

    private final DsRepository dsRepository;
    private final UserShiftRepository userShiftRepository;
    private final RunRepository runRepository;
    private final UserShiftCommandService userShiftCommandService;


    private Company company;
    private Run run;
    private User user;
    private Transport transport;

    @BeforeEach
    void setUp() throws Exception {
        company = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);
        user = testUserHelper.findOrCreateUser(UID);
        transport = testUserHelper.findOrCreateTransport();

        long deliveryServiceId = dsRepository.findByCompaniesId(company.getId()).iterator().next().getId();

        run = runGenerator.generate(RunGenerator.RunGenerateParam.builder()
                .campaignId(company.getCampaignId())
                .deliveryServiceId(deliveryServiceId)
                .externalId("run")
                .runDate(LocalDate.of(2021, Month.JULY, 27))
                .item(RunGenerator.RunItemGenerateParam.builder()
                        .orderNumber(0)
                        .movement(MovementCommand.Create.builder()
                                .orderWarehouse(orderWarehouseGenerator.generateWarehouse())
                                .orderWarehouseTo(orderWarehouseGenerator.generateWarehouse())
                                .build())
                        .build())
                .build());

        performAssignRun(run).andExpect(status().isOk());
    }

    @SneakyThrows
    @Test
    void runShouldBeCompletedIfShiftIsFinished() {
        transactionTemplate.execute(tc -> {
            run = runRepository.findByIdOrThrow(run.getId());
            var userShift = run.getFirstAssignedShift();

            testUserHelper.openShift(user, userShift.getId());

            CollectDropshipTask collectDropshipTask = userShift.streamCollectDropshipTasks().findAny().orElseThrow();
            var routePoint = collectDropshipTask.getRoutePoint();
            testUserHelper.arriveAtRoutePoint(routePoint);
            userShiftCommandService.collectDropships(user, new UserShiftCommand.CollectDropships(userShift.getId(), routePoint.getId(), collectDropshipTask.getId()));

            testUserHelper.finishFullReturnAtEnd(userShift);
            return null;
        });


        mockMvc.perform(get("/internal/partner/runs/v2")
                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                .param("status", RunStatus.COMPLETED.name())
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(run.getId()));
    }

    @SneakyThrows
    private ResultActions performAssignRun(Run run) {
        mockMvc.perform(
                post("/internal/partner/runs/{runId}/assign-transport", run.getId())
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(tplObjectMapper.writeValueAsString(new AssignRunTransportDto(transport.getId())))
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
        );
        return mockMvc.perform(
                post("/internal/partner/runs/{runId}/assign", run.getId())
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(tplObjectMapper.writeValueAsString(new AssignDriverDto(user.getId())))
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
        );
    }
}
