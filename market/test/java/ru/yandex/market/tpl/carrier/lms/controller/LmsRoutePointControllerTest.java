package ru.yandex.market.tpl.carrier.lms.controller;

import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import ru.yandex.market.tpl.carrier.core.audit.CarrierAuditTracer;
import ru.yandex.market.tpl.carrier.core.audit.CarrierSource;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.carrier.core.domain.run.RunCommandService;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.RunHelper;
import ru.yandex.market.tpl.carrier.core.domain.run.RunRepository;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseGenerator;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_=@Autowired)
class LmsRoutePointControllerTest extends LmsControllerTest {

    static {
        CarrierAuditTracer.putSource(CarrierSource.SYSTEM);
    }

    private final TestUserHelper testUserHelper;

    private final RunGenerator manualRunService;
    private final RunCommandService runCommandService;
    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final RunRepository runRepository;
    private final RunHelper runHelper;

    private UserShift userShift;

    @BeforeEach
    void setUp() {
        Company company = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);

        var run = manualRunService.generate(RunGenerator.RunGenerateParam.builder()
                .externalId("asd")
                .deliveryServiceId(9001L)
                .campaignId(company.getCampaignId())
                .runDate(LocalDate.now())
                .name("asd")
                .items(
                    List.of(
                            new RunGenerator.RunItemGenerateParam(
                                    MovementCommand.Create.builder()
                                            .externalId("123")
                                            .orderWarehouse(orderWarehouseGenerator.generateWarehouse())
                                            .orderWarehouseTo(orderWarehouseGenerator.generateWarehouse())
                                            .build(),
                                    1,
                                    null,
                                    null
                            )
                    )
                ).build()
        );

        var user = testUserHelper.findOrCreateUser(1L);
        var transport = testUserHelper.findOrCreateTransport();
        userShift = runHelper.assignUserAndTransport(run, user, transport);
    }

    @Test
    void getRoutePoints() throws Exception {
        mockMvc.perform(
                get("/LMS/carrier/user-shifts/{userShiftId}/routePoints", userShift.getId())
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_UTF8_VALUE)
        )
                .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isArray())
        .andExpect(jsonPath("$.items").value(Matchers.hasSize(2)));
    }

}
