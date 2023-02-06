package ru.yandex.market.tpl.carrier.planner.controller.api.duty;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;

import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.duty.Duty;
import ru.yandex.market.tpl.carrier.core.domain.duty.DutyRepository;
import ru.yandex.market.tpl.carrier.core.domain.duty.DutyStatus;
import ru.yandex.market.tpl.carrier.core.domain.run.RunRepository;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouse;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseGenerator;
import ru.yandex.market.tpl.carrier.planner.controller.BasePlannerWebTest;
import ru.yandex.market.tpl.carrier.planner.service.api.duty.DutySpecification;
import ru.yandex.market.tpl.carrier.planner.service.api.duty.InternalDutyFilter;
import ru.yandex.mj.generated.server.model.DutyCreateDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class DutyControllerCreatorTest extends BasePlannerWebTest {
    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final TestUserHelper testUserHelper;
    private final ObjectMapper objectMapper;
    private final DutyRepository dutyRepository;
    private final RunRepository runRepository;

    private Duty duty1;
    private Duty duty2;

    private OrderWarehouse orderWarehouse;

    @BeforeEach
    void setup() {
        Company company = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);
        Company company1 = testUserHelper.findOrCreateCompany(TestUserHelper.CompanyGenerateParam.builder()
                .companyName("Another company")
                .campaignId(1001L)
                .deliveryServiceIds(Set.of(124L))
                .login("login-1")
                .build());
        testUserHelper.deliveryService(123L, Set.of(company));
        testUserHelper.deliveryService(124L, Set.of(company1));
        orderWarehouse = orderWarehouseGenerator.generateWarehouse();
    }

    @Test
    void testCreate() throws Exception {
        var dto = new DutyCreateDto();
        dto.setDutyEndTime(OffsetDateTime.of(2021, 1, 1, 20, 0, 0, 0, ZoneOffset.of("+3")));
        dto.setDutyStartTime(OffsetDateTime.of(2021, 1, 1, 8, 0, 0, 0, ZoneOffset.of("+3")));
        dto.setDeliveryServiceId(123L);
        dto.setPallets(33);
        dto.setPriceCents(600000L);
        dto.setDutyLogisticPointId(Long.parseLong(orderWarehouse.getYandexId()));

        mockMvc.perform(post("/internal/duties/")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        assertThat(dutyRepository.findAll(new DutySpecification(InternalDutyFilter.builder().build()), Pageable.unpaged())
                .getTotalElements()
        )
                .isEqualTo(1L);
    }

    @Test
    void testCreate1() throws Exception {
        var dto = new DutyCreateDto();
        dto.setDutyStartTime(OffsetDateTime.of(2020, 12, 31, 20, 0, 0, 0, ZoneOffset.of("+3")));
        dto.setDutyEndTime(OffsetDateTime.of(2021, 1, 1, 8, 0, 0, 0, ZoneOffset.of("+3")));
        dto.setDeliveryServiceId(123L);
        dto.setPallets(33);
        dto.setPriceCents(600000L);
        dto.setDutyLogisticPointId(Long.parseLong(orderWarehouse.getYandexId()));

        mockMvc.perform(post("/internal/duties/")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dutyLogisticPointId").value(Long.parseLong(orderWarehouse.getYandexId())))
                .andExpect(jsonPath("$.deliveryServiceId").value(123L))
                .andExpect(jsonPath("$.status").value(DutyStatus.CREATED.name()))
                .andExpect(jsonPath("$.pallets").value(33))
                .andExpect(jsonPath("$.priceCents").value(600000L))
                .andExpect(jsonPath("$.dutyStartTime").value("2020-12-31T17:00:00Z"))
                .andExpect(jsonPath("$.dutyEndTime").value("2021-01-01T05:00:00Z"));

        assertThat(dutyRepository.findAll(new DutySpecification(InternalDutyFilter.builder().build()), Pageable.unpaged())
                .getTotalElements()
        )
                .isEqualTo(1L);

        transactionTemplate.execute(t -> {
            assertThat(dutyRepository.findAll(new DutySpecification(InternalDutyFilter.builder().build())).get(0))
                .extracting(
                    Duty::getDutyWarehouse,
                    Duty::getPallets,
                    Duty::getStatus,
                    Duty::getDutyStartTime,
                    Duty::getDutyEndTime
                )
                .containsExactly(
                    orderWarehouse,
                    33,
                    DutyStatus.CREATED,
                    Instant.parse("2020-12-31T17:00:00.00Z"),
                    Instant.parse("2021-01-01T05:00:00.00Z")
                );
            return null;
        });

        assertThat(runRepository.findAll()).hasSize(1);
        assertThat(runRepository.findAll().get(0).getPriceCents()).isEqualTo(600000);
    }
}
