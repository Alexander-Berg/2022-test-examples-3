package ru.yandex.market.tpl.partner.carrier.controller.partner.run;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import one.util.streamex.StreamEx;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.RunRepository;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.Transport;
import ru.yandex.market.tpl.carrier.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouse;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseGenerator;
import ru.yandex.market.tpl.partner.carrier.BaseTplPartnerCarrierWebIntTest;
import ru.yandex.market.tpl.partner.carrier.model.run.partner.AssignDriverDto;
import ru.yandex.market.tpl.partner.carrier.model.run.partner.AssignRunTransportDto;
import ru.yandex.market.tpl.partner.carrier.web.PartnerCompanyHandler;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class PartnerCarrierRunControllerAssignIndexModeTest extends BaseTplPartnerCarrierWebIntTest {

    private final TestUserHelper testUserHelper;

    private final RunGenerator runGenerator;
    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final ObjectMapper objectMapper;

    private final RunRepository runRepository;

    private Company company;
    private User user;
    private Transport transport;
    private Run run;

    private OrderWarehouse warehouseA;
    private OrderWarehouse warehouseB;
    private OrderWarehouse warehouseC;
    private OrderWarehouse warehouseD;

    @BeforeEach
    void setUp() {
        company = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);

        user = testUserHelper.findOrCreateUser(1L);
        transport = testUserHelper.findOrCreateTransport();

        warehouseA = orderWarehouseGenerator.generateWarehouse();
        warehouseB = orderWarehouseGenerator.generateWarehouse();
        warehouseC = orderWarehouseGenerator.generateWarehouse();
        warehouseD = orderWarehouseGenerator.generateWarehouse();

        run = runGenerator.generate(RunGenerator.RunGenerateParam.builder()
                .runDate(LocalDate.now())
                .campaignId(company.getCampaignId())
                .deliveryServiceId(123L)
                .externalId("run1")
                .item(RunGenerator.RunItemGenerateParam.builder()
                        .movement(MovementCommand.Create.builder()
                                .orderWarehouse(warehouseA)
                                .orderWarehouseTo(warehouseC)
                                .build())
                        .orderNumber(1)
                        .fromIndex(0)
                        .toIndex(2)
                        .build())
                .item(RunGenerator.RunItemGenerateParam.builder()
                        .movement(MovementCommand.Create.builder()
                                .orderWarehouse(warehouseB)
                                .orderWarehouseTo(warehouseD)
                                .build())
                        .orderNumber(2)
                        .fromIndex(1)
                        .toIndex(3)
                        .build())
                .build());
    }


    @SneakyThrows
    @Test
    void shouldAssignRunAndCreateUserShift() {
        mockMvc.perform(post("/internal/partner/runs/{id}/assign", run.getId())
                .content(objectMapper.writeValueAsString(new AssignDriverDto(user.getId())))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
        ).andExpect(status().isOk());

        mockMvc.perform(post("/internal/partner/runs/{id}/assign-transport", run.getId())
                .content(objectMapper.writeValueAsString(new AssignRunTransportDto(transport.getId())))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
        ).andExpect(status().isOk());

        transactionTemplate.execute(tc -> {
            run = runRepository.findByIdOrThrow(run.getId());
            UserShift userShift = run.getFirstAssignedShift();

            List<String> addressArray = userShift.streamRoutePoints()
                    .map(RoutePoint::getAddressString)
                    .toList();

            Assertions.assertThat(addressArray)
                    .containsExactly(
                            StreamEx.of(warehouseA, warehouseB, warehouseC, warehouseD)
                            .map(ow -> ow.getAddress().getAddress())
                            .toArray(String[]::new)
                    );

            return null;
        });

    }
}
