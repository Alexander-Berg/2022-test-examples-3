package ru.yandex.market.tpl.carrier.driver.controller;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunCommandService;
import ru.yandex.market.tpl.carrier.core.domain.run.RunCommentRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.RunHelper;
import ru.yandex.market.tpl.carrier.core.domain.run.RunMessage;
import ru.yandex.market.tpl.carrier.core.domain.run.RunMessageRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.commands.RunItemData;
import ru.yandex.market.tpl.carrier.core.domain.run.commands.RunCommand;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseGenerator;
import ru.yandex.market.tpl.carrier.core.service.region.DummyCarrierRegionService;
import ru.yandex.market.tpl.carrier.driver.BaseDriverApiIntTest;
import ru.yandex.market.tpl.carrier.driver.api.model.run.RunMessageDto;
import ru.yandex.market.tpl.carrier.driver.api.model.run.RunMessageType;
import ru.yandex.market.tpl.carrier.driver.api.model.task.LocationDto;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class RunMessageControllerIntTest extends BaseDriverApiIntTest {

    private final TestUserHelper testUserHelper;
    private final ObjectMapper tplObjectMapper;

    private final RunCommandService runCommandService;
    private final RunCommentRepository runCommentRepository;
    private final RunHelper runHelper;
    private final MovementGenerator movementGenerator;
    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final Clock clock;

    private final RunMessageRepository runMessageRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = testUserHelper.findOrCreateUser(UID);
    }

    @SneakyThrows
    @Test
    void shouldCreateStop() {
        var company = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);
        var transport = testUserHelper.findOrCreateTransport();

        Run run = runCommandService.create(RunCommand.Create.builder()
                .campaignId(company.getCampaignId())
                .externalId("abc")
                .deliveryServiceId(123L)
                .runDate(LocalDate.now(clock))
                .items(List.of(RunItemData.builder()
                        .movement(movementGenerator.generate(MovementCommand.Create
                                .builder()
                                .orderWarehouse(orderWarehouseGenerator.generateWarehouse())
                                .orderWarehouseTo(orderWarehouseGenerator.generateWarehouse())
                                .build())
                        )
                        .orderNumber(1)
                        .build()
                ))
                .build());
        var userShift = runHelper.assignUserAndTransport(run, user, transport);
        testUserHelper.openShift(user, userShift.getId());

        RunMessageDto runIncidentDto = new RunMessageDto();
        runIncidentDto.setLocation(new LocationDto(new BigDecimal("12.34"), new BigDecimal("56.78")));
        runIncidentDto.setType(RunMessageType.CRITICAL_ISSUE);
        runIncidentDto.setMessage("comment");

        mockMvc.perform(post("/api/run/message")
                        .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(tplObjectMapper.writeValueAsString(runIncidentDto))
                ).andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("CRITICAL_ISSUE"));

        Assertions.assertThat(runCommentRepository.findByRunIdOrderByCreatedAt(run.getId()).size()).isEqualTo(2);
    }

    @SneakyThrows
    @Test
    void shouldCreateForcedStop() {
        var company = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);
        var transport = testUserHelper.findOrCreateTransport();

        Run run = runCommandService.create(RunCommand.Create.builder()
                .campaignId(company.getCampaignId())
                .externalId("abc")
                .deliveryServiceId(123L)
                .runDate(LocalDate.now())
                .items(List.of(RunItemData.builder()
                        .movement(movementGenerator.generate(MovementCommand.Create
                                .builder()
                                .orderWarehouse(orderWarehouseGenerator.generateWarehouse())
                                .orderWarehouseTo(orderWarehouseGenerator.generateWarehouse())
                                .build())
                        )
                        .orderNumber(1)
                        .build()
                ))
                .build());
        var userShift = runHelper.assignUserAndTransport(run, user, transport);
        testUserHelper.openShift(user, userShift.getId());

        RunMessageDto runIncidentDto = new RunMessageDto();
        runIncidentDto.setLocation(new LocationDto(new BigDecimal("12.34"), new BigDecimal("56.78")));
        runIncidentDto.setType(RunMessageType.CRITICAL_ISSUE);
        runIncidentDto.setMessage("comment");
        runIncidentDto.setForced(true);

        mockMvc.perform(post("/api/run/message")
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(tplObjectMapper.writeValueAsString(runIncidentDto))
        ).andExpect(status().isOk());

        List<RunMessage> messages = runMessageRepository.findByRunId(run.getId());
        Assertions.assertThat(messages).hasSize(1);
        RunMessage message = messages.get(0);
        Assertions.assertThat(message.isForced()).isTrue();
        Assertions.assertThat(message.getMessage()).isEqualTo("comment");

        Assertions.assertThat(message.getRegionId()).isEqualTo(DummyCarrierRegionService.DEFAULT_REGION_ID);
        Assertions.assertThat(message.getTimezone()).isEqualTo(DummyCarrierRegionService.DEFAULT_TIMEZONE);
        Assertions.assertThat(message.getLongitude()).isEqualTo(new BigDecimal("12.34"));
        Assertions.assertThat(message.getLatitude()).isEqualTo(new BigDecimal("56.78"));
    }

    @SneakyThrows
    @Test
    void shouldGetStopTypes() {
        int expectedSize = (int) Arrays.stream(RunMessageType.values())
                .filter(RunMessageType::isCarrier)
                .count();

        mockMvc.perform(get("/api/run/message/types")
                        .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                )

                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(Matchers.hasSize(expectedSize)));
    }
}
