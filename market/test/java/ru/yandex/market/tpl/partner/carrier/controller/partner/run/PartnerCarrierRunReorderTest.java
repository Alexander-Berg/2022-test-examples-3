package ru.yandex.market.tpl.partner.carrier.controller.partner.run;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.tpl.carrier.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunCommandService;
import ru.yandex.market.tpl.carrier.core.domain.run.RunHelper;
import ru.yandex.market.tpl.carrier.core.domain.run.RunRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.commands.RunItemData;
import ru.yandex.market.tpl.carrier.core.domain.run.commands.RunCommand;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.carrier.core.domain.usershift.RoutePointStatus;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouse;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseGenerator;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.partner.carrier.BaseTplPartnerCarrierWebIntTest;
import ru.yandex.market.tpl.partner.carrier.model.run.partner.ReorderRunItemDto;
import ru.yandex.market.tpl.partner.carrier.model.run.partner.ReorderRunItemsDto;
import ru.yandex.market.tpl.partner.carrier.web.PartnerCompanyHandler;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class PartnerCarrierRunReorderTest extends BaseTplPartnerCarrierWebIntTest {

    private final MovementGenerator movementGenerator;
    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final RunCommandService runCommandService;
    private final TestUserHelper testUserHelper;
    private final ObjectMapper tplObjectMapper;
    private final UserShiftRepository userShiftRepository;
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
    void shouldReorderRun() {
        mockMvc.perform(post("/internal/partner/runs/{runId}/reorder", run1.getId())
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(tplObjectMapper.writeValueAsString(new ReorderRunItemsDto(
                                List.of(
                                        new ReorderRunItemDto(warehouse3.getId()),
                                        new ReorderRunItemDto(warehouse1.getId()),
                                        new ReorderRunItemDto(warehouse2.getId())
                                )
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points").isArray())
                .andExpect(jsonPath("$.points").value(Matchers.hasSize(3)))
                .andExpect(jsonPath("$.points[0].id").value(warehouse3.getId()))
                .andExpect(jsonPath("$.points[1].id").value(warehouse1.getId()))
                .andExpect(jsonPath("$.points[2].id").value(warehouse2.getId()))
        ;
    }

    @SneakyThrows
    @Test
    void shouldReorderAssignedRun() {
        var user = testUserHelper.findOrCreateUser(UID);
        var transport = testUserHelper.findOrCreateTransport();

        runHelper.assignUserAndTransport(run1, user, transport);

        mockMvc.perform(post("/internal/partner/runs/{runId}/reorder", run1.getId())
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(tplObjectMapper.writeValueAsString(new ReorderRunItemsDto(
                                List.of(
                                        new ReorderRunItemDto(warehouse3.getId()),
                                        new ReorderRunItemDto(warehouse1.getId()),
                                        new ReorderRunItemDto(warehouse2.getId())
                                )
                        ))))
                .andExpect(status().isOk());

        transactionTemplate.execute(tc -> {

            run1 = runRepository.findByIdOrThrow(run1.getId());
            UserShift userShift = run1.getFirstAssignedShift();
            List<RoutePoint> routePoints = userShift.streamRoutePoints().toList();

            Assertions.assertThat(routePoints.get(0).getAddressString())
                    .isEqualTo(warehouse3.getAddress().getAddress());
            Assertions.assertThat(routePoints.get(1).getAddressString())
                    .isEqualTo(warehouse1.getAddress().getAddress());
            return null;
        });

    }


    @SneakyThrows
    @Test
    void shouldReorderOnTaskPoint() {
        var user = testUserHelper.findOrCreateUser(UID);
        var transport = testUserHelper.findOrCreateTransport();

        UserShift userShift = runHelper.assignUserAndTransport(run1, user, transport);
        testUserHelper.openShift(user, userShift.getId());

        mockMvc.perform(post("/internal/partner/runs/{runId}/reorder", run1.getId())
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(tplObjectMapper.writeValueAsString(new ReorderRunItemsDto(
                                List.of(
                                        new ReorderRunItemDto(warehouse3.getId()),
                                        new ReorderRunItemDto(warehouse1.getId()),
                                        new ReorderRunItemDto(warehouse2.getId())
                                )
                        ))))
                .andExpect(status().isOk());

        transactionTemplate.execute(tc -> {
            run1 = runRepository.findByIdOrThrow(run1.getId());
            var userShift1 = run1.getFirstAssignedShift();
            List<RoutePoint> routePoints = userShift1.streamRoutePoints().toList();

            Assertions.assertThat(routePoints.get(0).getAddressString())
                    .isEqualTo(warehouse3.getAddress().getAddress());
            Assertions.assertThat(routePoints.get(0).getStatus())
                    .isEqualTo(RoutePointStatus.IN_TRANSIT);
            Assertions.assertThat(routePoints.get(1).getAddressString())
                    .isEqualTo(warehouse1.getAddress().getAddress());
            Assertions.assertThat(routePoints.get(1).getStatus())
                    .isEqualTo(RoutePointStatus.NOT_STARTED);

            Assertions.assertThat(userShift1.getCurrentRoutePoint().getAddressString())
                    .isEqualTo(warehouse3.getAddress().getAddress());

            dbQueueTestUtil.assertQueueHasSize(QueueType.PUSH_CARRIER_NOTIFICATION, 1);
            return null;
        });


    }

    @SneakyThrows
    @Test
    void shouldNotAllowToReorderFinishedPoint() {
        var user = testUserHelper.findOrCreateUser(UID);
        var transport = testUserHelper.findOrCreateTransport();

        var userShift = runHelper.assignUserAndTransport(run1, user, transport);
        testUserHelper.openShift(user, userShift.getId());
        userShift = userShiftRepository.findByIdOrThrow(userShift.getId());
        testUserHelper.finishCollectDropships(userShift.getCurrentRoutePoint());


        mockMvc.perform(post("/internal/partner/runs/{runId}/reorder", run1.getId())
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(tplObjectMapper.writeValueAsString(new ReorderRunItemsDto(
                                List.of(
                                        new ReorderRunItemDto(warehouse3.getId()),
                                        new ReorderRunItemDto(warehouse1.getId()),
                                        new ReorderRunItemDto(warehouse2.getId())
                                )
                        ))))
                .andExpect(status().isBadRequest());

    }


    @SneakyThrows
    @Test
    void shouldNotAllowToReorderReturnPoint() {
        mockMvc.perform(post("/internal/partner/runs/{runId}/reorder", run1.getId())
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(tplObjectMapper.writeValueAsString(new ReorderRunItemsDto(
                                List.of(
                                        new ReorderRunItemDto(warehouse2.getId()),
                                        new ReorderRunItemDto(warehouse1.getId()),
                                        new ReorderRunItemDto(warehouse3.getId())
                                )
                        ))))
                .andExpect(status().isBadRequest());

    }
}
