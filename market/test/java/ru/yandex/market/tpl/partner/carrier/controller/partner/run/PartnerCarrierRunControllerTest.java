package ru.yandex.market.tpl.partner.carrier.controller.partner.run;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import one.util.streamex.StreamEx;
import org.assertj.core.api.Assertions;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.carrier.core.domain.company.Company;
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
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouse;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseGenerator;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.partner.carrier.BaseTplPartnerCarrierWebIntTest;
import ru.yandex.market.tpl.partner.carrier.model.run.RunStatus;
import ru.yandex.market.tpl.partner.carrier.model.run.partner.location.PartnerRunLocationDto;
import ru.yandex.market.tpl.partner.carrier.model.run.partner.location.PartnerRunPointDto;
import ru.yandex.market.tpl.partner.carrier.web.PartnerCompanyHandler;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class PartnerCarrierRunControllerTest extends BaseTplPartnerCarrierWebIntTest {

    private static final long USER_ID_1 = 1L;
    private static final long USER_ID_2 = 2L;
    private static final long USER_ID_3 = 3L;

    private final TestUserHelper testUserHelper;
    private final ObjectMapper tplObjectMapper;

    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final MovementGenerator movementGenerator;
    private final RunCommandService runCommandService;
    private final RunHelper runHelper;

    private Company company;

    private Run run1;
    private Run run2;
    private Run run3;
    private User user1;
    private User user2;
    private User user3;
    private Transport transport1;
    private Transport transport2;
    private Transport transport3;

    private Instant ouboundArrivalTime = ZonedDateTime.of(2021, 8, 4, 0, 0, 0, 0, DateTimeUtil.DEFAULT_ZONE_ID)
            .toInstant();
    private Instant outboundDepartureTime = ZonedDateTime.of(2021, 8, 4, 1, 0, 0, 0, DateTimeUtil.DEFAULT_ZONE_ID)
            .toInstant();
    private Instant expectedDateTimeFrom = ZonedDateTime.of(2021, 8, 4, 0, 0, 0, 0, DateTimeUtil.DEFAULT_ZONE_ID)
            .toInstant();
    private Instant expectedDateTimeTo = ZonedDateTime.of(2021, 8, 4, 23, 59, 59, 0, DateTimeUtil.DEFAULT_ZONE_ID)
            .toInstant();
    private Instant inboundArrivalTime = ZonedDateTime.of(2021, 8, 4, 22, 59, 59, 0, DateTimeUtil.DEFAULT_ZONE_ID)
            .toInstant();

    private OrderWarehouse warehouse1;
    private OrderWarehouse warehouse2;
    private OrderWarehouse warehouse3;
    private OrderWarehouse warehouse4;
    private OrderWarehouse warehouse5;
    private OrderWarehouse warehouse6;

    @BeforeEach
    void setUp() {
        company = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);

        user1 = testUserHelper.findOrCreateUser(USER_ID_1, Company.DEFAULT_COMPANY_NAME, UserUtil.PHONE);
        user2 = testUserHelper.findOrCreateUser(USER_ID_2, Company.DEFAULT_COMPANY_NAME, UserUtil.ANOTHER_PHONE);
        user3 = testUserHelper.findOrCreateUser(USER_ID_3, Company.DEFAULT_COMPANY_NAME, UserUtil.ANOTHER_PHONE_2);
        transport1 = testUserHelper.findOrCreateTransport();
        transport2 = testUserHelper.findOrCreateTransport("Другая машина", Company.DEFAULT_COMPANY_NAME);
        transport3 = testUserHelper.findOrCreateTransport("Другая машина 2", Company.DEFAULT_COMPANY_NAME);

        warehouse1 = orderWarehouseGenerator.generateWarehouse();
        warehouse2 = orderWarehouseGenerator.generateWarehouse();
        var movement1 = movementGenerator.generate(MovementCommand.Create.builder()
                .orderWarehouse(warehouse1)
                .outboundArrivalTime(ouboundArrivalTime)
                .deliveryIntervalFrom(outboundDepartureTime)
                .outboundDepartureTime(outboundDepartureTime)
                .orderWarehouseTo(warehouse2)
                .deliveryIntervalTo(expectedDateTimeTo)
                .inboundArrivalTime(inboundArrivalTime)
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
        testUserHelper.openShift(user1, userShift1.getId());

        warehouse3 = orderWarehouseGenerator.generateWarehouse();
        warehouse4 = orderWarehouseGenerator.generateWarehouse();
        var movement2 = movementGenerator.generate(MovementCommand.Create.builder()
                .orderWarehouse(warehouse3)
                .deliveryIntervalFrom(expectedDateTimeFrom)
                .orderWarehouseTo(warehouse4)
                .deliveryIntervalTo(expectedDateTimeTo)
                .inboundArrivalTime(inboundArrivalTime)
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
        testUserHelper.openShift(user2, userShift2.getId());

        warehouse5 = orderWarehouseGenerator.generateWarehouse();
        warehouse6 = orderWarehouseGenerator.generateWarehouse();
        var movement3 = movementGenerator.generate(MovementCommand.Create.builder()
                .orderWarehouse(warehouse5)
                .deliveryIntervalFrom(expectedDateTimeFrom)
                .orderWarehouseTo(warehouse6)
                .deliveryIntervalTo(expectedDateTimeTo)
                .inboundArrivalTime(inboundArrivalTime)
                .build());
        run3 = runCommandService.create(RunCommand.Create.builder()
                .externalId("run3")
                .runDate(LocalDate.now())
                .campaignId(company.getCampaignId())
                .items(List.of(RunItemData.builder()
                        .movement(movement2)
                        .orderNumber(1)
                        .build()))
                .build()
        );
    }

    @SneakyThrows
    @Test
    void shouldGetRuns() {
        var responseString = mockMvc.perform(get("/internal/partner/runs")
                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
        )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<PartnerRunLocationDto> runs = tplObjectMapper.readValue(responseString,
                new TypeReference<List<PartnerRunLocationDto>>() {
        });

        Assertions.assertThat(runs).hasSize(2);

        PartnerRunLocationDto runDto1 =
                StreamEx.of(runs).filterBy(PartnerRunLocationDto::getDriverId, user1.getId()).findFirst().orElseThrow();
        Assertions.assertThat(runDto1.getRunId()).isEqualTo(run1.getId());
        Assertions.assertThat(runDto1.getRunDate()).isEqualTo(run1.getRunDate());
        Assertions.assertThat(runDto1.getDriver()).isNotNull();
        Assertions.assertThat(runDto1.getDriver().getName()).isEqualTo(user1.getName());
        Assertions.assertThat(runDto1.getDriver().getFirstName()).isEqualTo(user1.getFirstName());
        Assertions.assertThat(runDto1.getDriver().getLastName()).isEqualTo(user1.getLastName());
        Assertions.assertThat(runDto1.getDriver().getPatronymic()).isEqualTo(user1.getPatronymic());
        Assertions.assertThat(runDto1.getTransport()).isNotNull();
        assertRoutePoint(runDto1.getPoints().get(0), warehouse1, ouboundArrivalTime);
        assertRoutePoint(runDto1.getPoints().get(1), warehouse2, inboundArrivalTime);
    }

    private void assertRoutePoint(PartnerRunPointDto point, OrderWarehouse warehouse, Instant expectedDateTime) {
        Assertions.assertThat(point.getAddress()).isEqualTo(warehouse.getAddress().getAddress());
        Assertions.assertThat(point.getCoordinates().getLatitude()).isEqualTo(warehouse.getAddress().getLatitude());
        Assertions.assertThat(point.getCoordinates().getLongitude()).isEqualTo(warehouse.getAddress().getLongitude());
        Assertions.assertThat(point.getExpectedArrivalTimestamp()).isEqualTo(expectedDateTime);
    }


    @Test
    @SneakyThrows
    void shouldFilterByStatus() {
        mockMvc.perform(
                get("/internal/partner/runs")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .param("status", RunStatus.CREATED.name())
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(Matchers.hasSize(0)));
    }

}
