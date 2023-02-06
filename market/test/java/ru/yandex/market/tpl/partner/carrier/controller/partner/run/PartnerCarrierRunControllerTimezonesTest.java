package ru.yandex.market.tpl.partner.carrier.controller.partner.run;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.carrier.core.audit.CarrierAuditTracer;
import ru.yandex.market.tpl.carrier.core.audit.CarrierSource;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunCommandService;
import ru.yandex.market.tpl.carrier.core.domain.run.RunHelper;
import ru.yandex.market.tpl.carrier.core.domain.run.RunRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.commands.RunCommand;
import ru.yandex.market.tpl.carrier.core.domain.run.commands.RunItemData;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.user.UserUtil;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.Transport;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouse;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseGenerator;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.partner.carrier.BaseTplPartnerCarrierWebIntTest;
import ru.yandex.market.tpl.partner.carrier.web.PartnerCompanyHandler;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class PartnerCarrierRunControllerTimezonesTest extends BaseTplPartnerCarrierWebIntTest {

    static {
        CarrierAuditTracer.putSource(CarrierSource.SYSTEM);
    }

    private static final long USER_ID_1 = 1L;
    private static final long USER_ID_2 = 2L;
    private static final long USER_ID_3 = 3L;

    private final TestUserHelper testUserHelper;
    private final ObjectMapper tplObjectMapper;

    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final MovementGenerator movementGenerator;
    private final RunCommandService runCommandService;
    private final RunHelper runHelper;
    private final RunRepository runRepository;

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

    private Instant expectedDateTimeFrom = ZonedDateTime.of(2021, 8, 4, 0, 0, 0, 0, DateTimeUtil.DEFAULT_ZONE_ID)
            .toInstant();
    private Instant expectedDateTimeTo = ZonedDateTime.of(2021, 8, 4, 23, 59, 59, 0, DateTimeUtil.DEFAULT_ZONE_ID)
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
                .deliveryIntervalFrom(expectedDateTimeFrom)
                .orderWarehouseTo(warehouse2)
                .deliveryIntervalTo(expectedDateTimeTo)
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
    }

    @Test
    @SneakyThrows
    void shouldReturnTimestamps() {
        var firstRoutePoint = transactionTemplate.execute(tc -> {
            run1 = runRepository.findByIdOrThrow(run1.getId());
            return run1.getFirstAssignedShift().getFirstRoutePoint();
        });
        var timestamp = firstRoutePoint.getExpectedDateTime();
        String defaultTimezone = "Europe/Moscow";
        var defaultTimestamp = ZonedDateTime.ofInstant(timestamp, ZoneId.of(defaultTimezone));
        String localTimezone = firstRoutePoint.getTimezone();
        var localTimestamp = ZonedDateTime.ofInstant(timestamp, ZoneId.of(localTimezone));

        var result = mockMvc.perform(
                get("/internal/partner/runs")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
        )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<LinkedHashMap<String, List<LinkedHashMap<String, LinkedHashMap<String, String>>>>> filter = tplObjectMapper.readValue(result, List.class);
        var actualDefaultTimestamp = filter.get(0).get("points").get(0).get("defaultExpectedArrivalTimestamp").get("timestamp");
        var actualDefaultTimezone = filter.get(0).get("points").get(0).get("defaultExpectedArrivalTimestamp").get("timezoneName");
        var actualLocalTimestamp = filter.get(0).get("points").get(0).get("localExpectedArrivalTimestamp").get("timestamp");
        var actualLocalTimezone = filter.get(0).get("points").get(0).get("localExpectedArrivalTimestamp").get("timezoneName");
        Assertions.assertThat(ZonedDateTime.parse(actualDefaultTimestamp)).isEqualTo(defaultTimestamp);
        Assertions.assertThat(actualDefaultTimezone).isEqualTo(defaultTimezone);
        Assertions.assertThat(ZonedDateTime.parse(actualLocalTimestamp)).isEqualTo(localTimestamp);
        Assertions.assertThat(actualLocalTimezone).isEqualTo(localTimezone);
    }
}
