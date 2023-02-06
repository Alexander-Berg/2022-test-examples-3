package ru.yandex.market.tpl.carrier.planner.controller.api.duty;

import java.time.Instant;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.duty.Duty;
import ru.yandex.market.tpl.carrier.core.domain.duty.DutyGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunHelper;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouse;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseGenerator;
import ru.yandex.market.tpl.carrier.planner.controller.BasePlannerWebTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class DutyControllerRetrievalTest extends BasePlannerWebTest {
    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final DutyGenerator dutyGenerator;
    private final TestUserHelper testUserHelper;
    private final RunHelper runHelper;

    private Duty duty1;
    private Duty duty2;

    private Company company;
    private Company company1;
    private OrderWarehouse orderWarehouse;

    @BeforeEach
    void setup() {
        company = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);
        company1 = testUserHelper.findOrCreateCompany(TestUserHelper.CompanyGenerateParam.builder()
                .companyName("Another company")
                .campaignId(1001L)
                .deliveryServiceIds(Set.of(124L))
                .login("login-1")
                .build());
        testUserHelper.deliveryService(123L, Set.of(company));
        testUserHelper.deliveryService(124L, Set.of(company1));
        orderWarehouse = orderWarehouseGenerator.generateWarehouse();
        duty1 = dutyGenerator.generate();
        duty2 = dutyGenerator.generate(
                DutyGenerator.DutyGenerateParams.builder()
                        .deliveryServiceId(124L)
                        .dutyStartTime(Instant.parse("2021-01-02T10:00:00.00Z"))
                        .dutyEndTime(Instant.parse("2021-01-02T22:00:00.00Z"))
                        .pallets(33)
                        .name("Караул в чистом поле за Уралом")
                        .dutyWarehouseId(orderWarehouse.getYandexId())
                        .build()
        );
    }

    @Test
    void getAll() throws Exception {
        mockMvc.perform(get("/internal/duties"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(2)));

    }

    @Test
    void getWithLogisticPointIds() throws Exception {
        mockMvc.perform(get("/internal/duties?dutyLogisticPoint=" + orderWarehouse.getYandexId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)));
    }

    @Test
    void getWithDate() throws Exception {
        mockMvc.perform(get("/internal/duties?dutyDate=2021-01-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(duty2.getId()))
                .andExpect(jsonPath("$.content[0].name").value("Караул в чистом поле за Уралом"));
    }

    @Test
    void getWithStatus() throws Exception {
        mockMvc.perform(get("/internal/duties?status=CREATED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(2)));
    }

    @Test
    void getWithDeliveryServiceId() throws Exception {
        mockMvc.perform(get("/internal/duties?deliveryServiceId=123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)));
    }

    @Test
    void getWithUserAssigned() throws Exception {
        Company company2 = testUserHelper.findOrCreateCompany(TestUserHelper.CompanyGenerateParam.builder()
                .companyName("Another company 2")
                .campaignId(1002L)
                .deliveryServiceIds(Set.of(135L))
                .login("login-2")
                .build());
        testUserHelper.deliveryService(135L, Set.of(company2));

        var user = testUserHelper.findOrCreateUser(UID);
        var transport = testUserHelper.findOrCreateTransport();
        Duty duty3 = dutyGenerator.generate(
                dgp -> dgp.deliveryServiceId(135L)
        );
        Run run = duty3.getRun();
        runHelper.assignUserAndTransport(run, user, transport);

        mockMvc.perform(get("/internal/duties")
                        .param("deliveryServiceId", "135"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].run.user.id").value(user.getId()))
                .andExpect(jsonPath("$.content[0].run.user.name").value(user.getName()));
    }

    @Test
    void getById() throws Exception {
        mockMvc.perform(get("/internal/duties/{id}", duty2.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(duty2.getId()))
                .andExpect(jsonPath("$.name").value("Караул в чистом поле за Уралом"))
                .andExpect(jsonPath("$.deliveryServiceId").value(124L))
                .andExpect(jsonPath("$.dutyLogisticPointId").value(Long.parseLong(orderWarehouse.getYandexId())))
                .andExpect(jsonPath("$.dutyStartTime").value("2021-01-02T10:00:00Z"))
                .andExpect(jsonPath("$.dutyEndTime").value("2021-01-02T22:00:00Z"))
                .andExpect(jsonPath("$.pallets").value(33))
                .andExpect(jsonPath("$.company.id").value(company1.getId()))
                .andExpect(jsonPath("$.company.deliveryServiceId").value(124L))
                .andExpect(jsonPath("$.company.name").value(company1.getName()))
        ;
    }

}
