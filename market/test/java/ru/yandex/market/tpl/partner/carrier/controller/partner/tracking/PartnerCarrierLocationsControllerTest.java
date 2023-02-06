package ru.yandex.market.tpl.partner.carrier.controller.partner.tracking;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.carrier.core.db.QueryCountAssertions;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.location.TestLocationService;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementGenerator;
import ru.yandex.market.tpl.carrier.core.domain.partner.DeliveryService;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunCommandService;
import ru.yandex.market.tpl.carrier.core.domain.run.RunHelper;
import ru.yandex.market.tpl.carrier.core.domain.run.commands.RunItemData;
import ru.yandex.market.tpl.carrier.core.domain.run.commands.RunCommand;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.user.UserUtil;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.carrier.core.domain.usershift.commands.LocationData;
import ru.yandex.market.tpl.carrier.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseGenerator;
import ru.yandex.market.tpl.partner.carrier.BaseTplPartnerCarrierWebIntTest;
import ru.yandex.market.tpl.partner.carrier.model.tracking.PartnerLocationDto;
import ru.yandex.market.tpl.partner.carrier.web.PartnerCompanyHandler;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_=@Autowired)
public class PartnerCarrierLocationsControllerTest extends BaseTplPartnerCarrierWebIntTest {

    private final long USER_ID_1 = 1L;
    private final long USER_ID_2 = 2L;

    private final TestUserHelper testUserHelper;
    private final TestLocationService testLocationService;
    private final ObjectMapper tplObjectMapper;
    private final RunCommandService runCommandService;
    private final UserShiftCommandService userShiftCommandService;
    private final RunHelper runHelper;

    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final MovementGenerator movementGenerator;

    private Company company;

    private User user1;
    private Run run1;
    private User user2;
    private Run run2;

    @BeforeEach
    void setUp() {
        company = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);

        user1 = testUserHelper.findOrCreateUser(USER_ID_1, Company.DEFAULT_COMPANY_NAME, UserUtil.PHONE);
        user2 = testUserHelper.findOrCreateUser(USER_ID_2, Company.DEFAULT_COMPANY_NAME, UserUtil.ANOTHER_PHONE);
        var transport1 = testUserHelper.findOrCreateTransport();
        var transport2 = testUserHelper.findOrCreateTransport("Другая машина", Company.DEFAULT_COMPANY_NAME);

        var movement1 = movementGenerator.generate(MovementCommand.Create.builder()
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

        var userShift1 = runHelper.assignUserAndTransport(run1, user1, transport1);
        userShiftCommandService.startShift(user1, new UserShiftCommand.Start(userShift1.getId()));

        var movement2 = movementGenerator.generate(MovementCommand.Create.builder()
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
        var userShift2 = runHelper.assignUserAndTransport(run2, user2, transport2);
        userShiftCommandService.startShift(user2, new UserShiftCommand.Start(userShift2.getId()));

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
    }

    @SneakyThrows
    @Test
    void shouldGetTrackings() {
        var responseString = QueryCountAssertions.assertQueryCountTotalEqual(14,
                () -> mockMvc.perform(get("/internal/partner/users/locations")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString()
        );

        List<PartnerLocationDto> locations = tplObjectMapper.readValue(responseString, new TypeReference<List<PartnerLocationDto>>() {});

        Assertions.assertThat(locations).hasSize(2);
        Assertions.assertThat(locations)
                .anyMatch(l -> {
                    return l.getDriverId() == user1.getId() &&
                            l.getRunId() == run1.getId() &&
                            new BigDecimal("23.45").equals(l.getCoordinates().getLongitude()) &&
                            new BigDecimal("34.56").equals(l.getCoordinates().getLatitude());
                });

        Assertions.assertThat(locations)
                .anyMatch(l -> {
                    return l.getDriverId() == user2.getId() &&
                            l.getRunId() == run2.getId() &&
                            new BigDecimal("34.45").equals(l.getCoordinates().getLongitude()) &&
                            new BigDecimal("45.67").equals(l.getCoordinates().getLatitude());
                });
    }

    @SneakyThrows
    @Test
    void shouldGetUserLocation() {
        var responseString = mockMvc.perform(get(String.format("/internal/partner/users/%s/location", user1.getId()))
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        PartnerLocationDto location = tplObjectMapper.readValue(responseString, new TypeReference<PartnerLocationDto>() {});


        Assertions.assertThat(location.getCoordinates().getLatitude()).describedAs("latitude")
                .isEqualTo(new BigDecimal("34.56"));
        Assertions.assertThat(location.getCoordinates().getLongitude()).describedAs("longitude")
                .isEqualTo(new BigDecimal("23.45"));
        Assertions.assertThat(location.getDriverId()).describedAs("driver id")
                .isEqualTo(user1.getId());
        Assertions.assertThat(location.getRunId()).describedAs("run id")
                .isEqualTo(run1.getId());
    }

    @SneakyThrows
    @Test
    void shouldNotReturnOtherCompanyDriverLocations() {
        Company otherCompany = testUserHelper.createCompany(
                Set.of(testUserHelper.deliveryService(DeliveryService.DEFAULT_DS_ID)),
                666777888L, "company name other", "login");

        mockMvc.perform(get(String.format("/internal/partner/users/%d/location", user1.getId()))
                        .header(PartnerCompanyHandler.COMPANY_HEADER, otherCompany.getCampaignId())
                )
                .andExpect(status().isForbidden());
    }

    @SneakyThrows
    @Test
    void shouldFailOnUnknownDriverId() {
        Company otherCompany = testUserHelper.createCompany(
                Set.of(testUserHelper.deliveryService(DeliveryService.DEFAULT_DS_ID)),
                666777888L, "company name other", "login");

        mockMvc.perform(get(String.format("/internal/partner/users/%d/location", 123456L))
                        .header(PartnerCompanyHandler.COMPANY_HEADER, otherCompany.getCampaignId())
                )
                .andExpect(status().isForbidden());
    }
}
