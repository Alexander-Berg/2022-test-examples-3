package ru.yandex.market.tpl.carrier.planner.controller.manual;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import one.util.streamex.StreamEx;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.RunHelper;
import ru.yandex.market.tpl.carrier.core.domain.run.RunRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.RunStatus;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.UserUtil;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseGenerator;
import ru.yandex.market.tpl.carrier.planner.controller.BasePlannerWebTest;
import ru.yandex.market.tpl.carrier.planner.manual.movement.ManualCreateMovementDto;
import ru.yandex.market.tpl.carrier.planner.manual.run.ManualCreateRunDto;
import ru.yandex.market.tpl.carrier.planner.manual.run.ManualCreateRunItemDto;
import ru.yandex.market.tpl.carrier.planner.manual.run.ManualRunDto;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class ManualRunControllerTest extends BasePlannerWebTest {

    private final TestUserHelper testUserHelper;
    private final ObjectMapper tplObjectMapper;

    private final OrderWarehouseGenerator orderWarehouseGenerator;

    private final RunGenerator runGenerator;
    private final RunHelper runHelper;
    private final RunRepository runRepository;


    @SneakyThrows
    @Test
    void shouldCreateRun() {
        var company = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);

        var request = ManualCreateRunDto.builder()
                .externalId("extId")
                .deliveryServiceId(2234562L)
                .campaignId(company.getCampaignId())
                .runDate(LocalDate.of(2021, 12, 15))
                .items(List.of(
                        ManualCreateRunItemDto.builder()
                                .order(1)
                                .movementDto(ManualCreateMovementDto.builder()
                                        .orderWarehouseId(orderWarehouseGenerator.generateWarehouse().getId())
                                        .orderWarehouseToId(orderWarehouseGenerator.generateWarehouse().getId())
                                        .build())
                                .build(),
                        ManualCreateRunItemDto.builder()
                                .order(2)
                                .movementDto(ManualCreateMovementDto.builder()
                                        .orderWarehouseId(orderWarehouseGenerator.generateWarehouse().getId())
                                        .orderWarehouseToId(orderWarehouseGenerator.generateWarehouse().getId())
                                        .build())
                                .build()
                        )
                )
                .build();

        var responseString = mockMvc.perform(post("/manual/runs")
                .content(tplObjectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var response = tplObjectMapper.readValue(responseString, ManualRunDto.class);
        Assertions.assertThat(response.getExternalId()).isEqualTo("extId");
        Assertions.assertThat(response.getDate()).isEqualTo(LocalDate.now(clock));
        Assertions.assertThat(response.getDeliveryServiceId()).isEqualTo(2234562L);
        Assertions.assertThat(response.getItems()).hasSize(2);

        var item1 = StreamEx.of(response.getItems())
                .findFirst(i -> i.getOrderNumber() == 1)
                .orElseThrow();

        Assertions.assertThat(item1.getMovement()).isNotNull();

        var item2 = StreamEx.of(response.getItems())
                .findFirst(i -> i.getOrderNumber() == 2)
                .orElseThrow();

        Assertions.assertThat(item2.getMovement()).isNotNull();
    }

    @SneakyThrows
    @Test
    void shouldConfirmRun() {
        var run = runGenerator.generate();
        mockMvc.perform(post("/manual/runs/{id}/confirm", run.getId()))
                .andExpect(status().isOk());

        run = runRepository.findByIdOrThrow(run.getId());

        Assertions.assertThat(run.getStatus()).isEqualTo(RunStatus.CONFIRMED);
    }

    @SneakyThrows
    @Test
    void shouldReassignUser() {
        var user = testUserHelper.findOrCreateUser(1L);
        var user2 = testUserHelper.findOrCreateUser(2L, Company.DEFAULT_COMPANY_NAME, UserUtil.ANOTHER_PHONE_2);
        var transport = testUserHelper.findOrCreateTransport();

        var run = runGenerator.generate();
        runHelper.assignUserAndTransport(run, user, transport);

        mockMvc.perform(post("/manual/runs/{id}/assign-user", run.getId())
                .param("userId", String.valueOf(user2.getId()))
        ).andExpect(status().isOk());

        run = runRepository.findByIdOrThrow(run.getId());

        Assertions.assertThat(run.getUser()).isEqualTo(user2);
        Assertions.assertThat(run.getFirstAssignedShift().getUser()).isEqualTo(user2);
    }

    @SneakyThrows
    @Test
    void shouldReassignTransport() {
        var user = testUserHelper.findOrCreateUser(1L);
        var transport = testUserHelper.findOrCreateTransport();
        var transport2 = testUserHelper.findOrCreateTransport("another transport", Company.DEFAULT_COMPANY_NAME);

        var run = runGenerator.generate();
        runHelper.assignUserAndTransport(run, user, transport);

        mockMvc.perform(post("/manual/runs/{id}/assign-transport", run.getId())
                .param("transportId", String.valueOf(transport2.getId()))
        ).andExpect(status().isOk());

        run = runRepository.findByIdOrThrow(run.getId());

        Assertions.assertThat(run.getTransport()).isEqualTo(transport2);
        Assertions.assertThat(run.getFirstAssignedShift().getTransport()).isEqualTo(transport2);
    }
}
