package ru.yandex.market.tpl.carrier.driver.controller;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.RunHelper;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseAddress;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseGenerator;
import ru.yandex.market.tpl.carrier.driver.BaseDriverApiIntTest;
import ru.yandex.market.tpl.carrier.driver.api.model.task.LocationDto;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class LocationControllerAreaTimeTest extends BaseDriverApiIntTest {

    private final UserShiftRepository userShiftRepository;

    private final TestUserHelper testUserHelper;
    private final RunHelper runHelper;
    private final RunGenerator runGenerator;
    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final TransactionTemplate transactionTemplate;

    private final ObjectMapper objectMapper;

    private UserShift userShift;

    @BeforeEach
    void setUp() {
        var company = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);
        var user = testUserHelper.findOrCreateUser(1L);
        var transport = testUserHelper.findOrCreateTransport();
        var run = runGenerator.generate(RunGenerator.RunGenerateParam.builder()
                .runDate(LocalDate.now())
                .externalId("runId")
                .campaignId(company.getCampaignId())
                .deliveryServiceId(123L)
                .item(RunGenerator.RunItemGenerateParam.builder()
                        .movement(MovementCommand.Create.builder()
                                .orderWarehouse(orderWarehouseGenerator.generateWarehouse(
                                        ow -> {
                                            OrderWarehouseAddress address = ow.getAddress();
                                            address.setLatitude(new BigDecimal("9.9997"));
                                            address.setLongitude(new BigDecimal("10.0003"));
                                        }
                                ))
                                .orderWarehouseTo(orderWarehouseGenerator.generateWarehouse())
                                .build())
                        .orderNumber(1)
                        .build())
                .build());

        userShift = runHelper.assignUserAndTransport(run, user, transport);
        testUserHelper.openShift(user, userShift.getId());
    }

    @SneakyThrows
    @Test
    void shouldSaveAreaArrivalTime() {
        mockMvc.perform(post("/api/location")
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .content(objectMapper.writeValueAsString(LocationDto.builder()
                        .latitude(BigDecimal.TEN)
                        .longitude(BigDecimal.TEN)
                        .userShiftId(userShift.getId())
                        .build()
                ))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        ).andExpect(status().isOk());

        transactionTemplate.execute(tc -> {
            userShift = userShiftRepository.findByIdOrThrow(userShift.getId());
            RoutePoint routePoint = userShift.streamRoutePoints().findFirst().orElseThrow();
            Assertions.assertThat(routePoint.getAreaArrivalTime()).isNotNull();
            return null;
        });

        mockMvc.perform(post("/api/location")
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .content(objectMapper.writeValueAsString(LocationDto.builder()
                        .latitude(BigDecimal.ONE)
                        .longitude(BigDecimal.ONE)
                        .userShiftId(userShift.getId())
                        .build()
                ))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        ).andExpect(status().isOk());

        transactionTemplate.execute(tc -> {
            userShift = userShiftRepository.findByIdOrThrow(userShift.getId());
            RoutePoint routePoint = userShift.streamRoutePoints().findFirst().orElseThrow();
            Assertions.assertThat(routePoint.getDepartureTime()).isNotNull();
            return null;
        });

    }
}


