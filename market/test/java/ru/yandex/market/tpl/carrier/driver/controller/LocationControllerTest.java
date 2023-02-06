package ru.yandex.market.tpl.carrier.driver.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import ru.yandex.market.tpl.carrier.core.dbqueue.model.DriverQueueType;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.location.TestLocationService;
import ru.yandex.market.tpl.carrier.core.domain.location.UserLocation;
import ru.yandex.market.tpl.carrier.core.domain.location.UserLocationRepository;
import ru.yandex.market.tpl.carrier.core.domain.movement.Movement;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunCommandService;
import ru.yandex.market.tpl.carrier.core.domain.run.RunHelper;
import ru.yandex.market.tpl.carrier.core.domain.run.commands.RunItemData;
import ru.yandex.market.tpl.carrier.core.domain.run.commands.RunCommand;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.user.UserUtil;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.Transport;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.carrier.core.domain.usershift.commands.LocationData;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseGenerator;
import ru.yandex.market.tpl.carrier.driver.BaseDriverApiIntTest;
import ru.yandex.market.tpl.carrier.driver.api.model.task.LocationDto;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class LocationControllerTest extends BaseDriverApiIntTest {

    private final long USER_ID_1 = 1L;
    private final long USER_ID_2 = 2L;

    private final TestUserHelper testUserHelper;
    private final TestLocationService testLocationService;
    private final ObjectMapper tplObjectMapper;
    private final RunCommandService runCommandService;
    private final RunHelper runHelper;

    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final MovementGenerator movementGenerator;

    private final UserShiftRepository userShiftRepository;
    private final UserLocationRepository userLocationRepository;

    private final DbQueueTestUtil dbQueueTestUtil;

    private Company company;

    private User user1;
    private Run run1;
    private User user2;
    private Run run2;
    private Transport transport1;
    private Transport transport2;
    private Movement movement1;
    private Movement movement2;
    private UserShift userShift1;
    private UserShift userShift2;

    @BeforeEach
    void setUp() {
        company = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);

        user1 = testUserHelper.findOrCreateUser(USER_ID_1, Company.DEFAULT_COMPANY_NAME, UserUtil.PHONE);
        user2 = testUserHelper.findOrCreateUser(USER_ID_2, Company.DEFAULT_COMPANY_NAME, UserUtil.ANOTHER_PHONE);
        transport1 = testUserHelper.findOrCreateTransport();
        transport2 = testUserHelper.findOrCreateTransport("Другая машина", Company.DEFAULT_COMPANY_NAME);

        movement1 = movementGenerator.generate(MovementCommand.Create.builder()
                .orderWarehouse(orderWarehouseGenerator.generateWarehouse())
                .orderWarehouseTo(orderWarehouseGenerator.generateWarehouse())
                .build());
        run1 = runCommandService.create(RunCommand.Create.builder()
                .externalId("run1")
                .runDate(LocalDate.now())
                .campaignId(company.getCampaignId())
                .items(List.of(RunItemData.builder()
                        .movement(movement1)
                        .orderNumber(1)
                        .build()))
                .build()
        );
        userShift1 = runHelper.assignUserAndTransport(run1, user1, transport1);

        movement2 = movementGenerator.generate(MovementCommand.Create.builder()
                .orderWarehouse(orderWarehouseGenerator.generateWarehouse())
                .orderWarehouseTo(orderWarehouseGenerator.generateWarehouse())
                .build());
        run2 = runCommandService.create(RunCommand.Create.builder()
                .externalId("run2")
                .runDate(LocalDate.now())
                .campaignId(company.getCampaignId())
                .items(List.of(RunItemData.builder()
                        .movement(movement2)
                        .orderNumber(1)
                        .build()))
                .build()
        );
        userShift2 = runHelper.assignUserAndTransport(run2, user2, transport2);
    }

    @SneakyThrows
    @Test
    void shouldReturnTransportInApiShiftsCurrent() {
        testLocationService.saveLocation(user1, LocationData.builder()
            .longitude(new BigDecimal("12.34"))
            .latitude(new BigDecimal("23.45"))
            .deviceId("")
            .userShiftId(userShift1.getId())
            .build());
        testLocationService.saveLocation(user1, LocationData.builder()
            .longitude(new BigDecimal("23.45"))
            .latitude(new BigDecimal("34.56"))
            .deviceId("")
            .userShiftId(userShift1.getId())
            .build());
        testLocationService.saveLocation(user2, LocationData.builder()
            .longitude(new BigDecimal("34.45"))
            .latitude(new BigDecimal("45.67"))
            .deviceId("")
            .userShiftId(userShift2.getId())
            .build()
        );

        LocationDto locationDto =
            new LocationDto(BigDecimal.TEN, BigDecimal.TEN, "Samsung", userShift1.getId());

        mockMvc.perform(post("/api/location")
            .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .content(tplObjectMapper.writeValueAsString(locationDto))
        ).andExpect(status().isOk());

        var result = userShiftRepository.findById(userShift1.getId()).get();
        dbQueueTestUtil.assertQueueHasSize(DriverQueueType.UPDATE_ESTIMATE_TIME, 2); // на каждого юзера по 1
    }

    @SneakyThrows
    @Test
    void shouldSaveSpeedAndDirection() {
        LocationDto locationDto =
                new LocationDto(BigDecimal.TEN, BigDecimal.TEN,
                                BigDecimal.valueOf(123.1), BigDecimal.valueOf(321.2),
                        "Samsung", userShift1.getId());

        mockMvc.perform(post("/api/location")
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(tplObjectMapper.writeValueAsString(locationDto))
        ).andExpect(status().isOk());

        UserLocation userLocation = userLocationRepository.findLastLocation(user1.getId(), userShift1.getId())
                .orElseThrow();

        Assertions.assertEquals(BigDecimal.valueOf(123.1), userLocation.getSpeedMs());
        Assertions.assertEquals(BigDecimal.valueOf(321.2), userLocation.getBearingDegrees());
    }

}
