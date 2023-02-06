package ru.yandex.market.tpl.partner.carrier.controller.partner.run;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunCommandService;
import ru.yandex.market.tpl.carrier.core.domain.run.RunHelper;
import ru.yandex.market.tpl.carrier.core.domain.run.RunRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.commands.NewPriceControlData;
import ru.yandex.market.tpl.carrier.core.domain.run.commands.RunCommand;
import ru.yandex.market.tpl.carrier.core.domain.run.commands.RunItemData;
import ru.yandex.market.tpl.carrier.core.domain.run.price_control.PriceControlStatus;
import ru.yandex.market.tpl.carrier.core.domain.run.price_control.PriceControlType;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouse;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseGenerator;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.partner.carrier.BaseTplPartnerCarrierWebIntTest;
import ru.yandex.market.tpl.partner.carrier.model.run.PartnerRunPriceStatus;
import ru.yandex.market.tpl.partner.carrier.web.PartnerCompanyHandler;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class PartnerCarrierRunPriceTest extends BaseTplPartnerCarrierWebIntTest {

    private final MovementGenerator movementGenerator;
    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final RunCommandService runCommandService;
    private final TestUserHelper testUserHelper;
    private final ObjectMapper tplObjectMapper;
    private final RunRepository runRepository;
    private final RunHelper runHelper;
    private final DbQueueTestUtil dbQueueTestUtil;

    private Company company;
    private OrderWarehouse warehouse1;
    private OrderWarehouse warehouse2;
    private OrderWarehouse warehouse3;
    private Run run1;

    @BeforeEach
    void setUp() {
        company = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);

        warehouse1 = orderWarehouseGenerator.generateWarehouse();
        warehouse2 = orderWarehouseGenerator.generateWarehouse();
        warehouse3 = orderWarehouseGenerator.generateWarehouse();
        var movement1 = movementGenerator.generate(MovementCommand.Create.builder()
                .orderWarehouse(warehouse1)
                .orderWarehouseTo(warehouse2)
                .build());
        var movement2 = movementGenerator.generate(MovementCommand.Create.builder()
                .orderWarehouse(warehouse3)
                .orderWarehouseTo(warehouse2)
                .build());
        run1 = runCommandService.create(RunCommand.Create.builder()
                .externalId("run1")
                .runDate(LocalDate.now())
                .campaignId(company.getCampaignId())
                .items(
                        List.of(
                                RunItemData.builder()
                                        .movement(movement1)
                                        .orderNumber(1)
                                        .build(),
                                RunItemData.builder()
                                        .movement(movement2)
                                        .orderNumber(2)
                                        .build()
                        )
                )
                .build()
        );
    }

    @SneakyThrows
    @Test
    void shouldGetPriceInfo() {
        NewPriceControlData penalty = NewPriceControlData.builder()
                .type(PriceControlType.AUTO_DELAY)
                .cent(-3000_00)
                .comment("Auto penalty, 1 hour")
                .author("robot")
                .status(PriceControlStatus.CONFIRMED)
                .build();
        NewPriceControlData bonus = NewPriceControlData.builder()
                .type(PriceControlType.MANUAL_EXTENSION)
                .cent(3000_00)
                .comment("bonus")
                .author("robot")
                .status(PriceControlStatus.CONFIRMED)
                .build();
        runCommandService.addPriceControl(new RunCommand.AddPriceControl(run1.getId(), penalty));
        runCommandService.addPriceControl(new RunCommand.AddPriceControl(run1.getId(), bonus));
        runCommandService.finaliseRunOld(run1.getId());
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/internal/partner/runs/" + run1.getId() + "/priceInfo")
                                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priceStatus")
                        .value(PartnerRunPriceStatus.NEED_DS_CONFIRMATION.name())
                )
                .andExpect(jsonPath("$.totalCostCent").value("300000"))
                .andExpect(jsonPath("$.priceControls").value(Matchers.hasSize(2)))
                .andExpect(jsonPath("$.priceControls[*].comment")
                        .value(Matchers.contains("Auto " + "penalty, 1 hour", "bonus")));
    }
}
