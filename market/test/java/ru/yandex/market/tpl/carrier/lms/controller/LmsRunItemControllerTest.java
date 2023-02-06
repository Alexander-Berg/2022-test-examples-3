package ru.yandex.market.tpl.carrier.lms.controller;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.carrier.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.carrier.core.audit.CarrierAuditTracer;
import ru.yandex.market.tpl.carrier.core.audit.CarrierSource;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.RunHelper;
import ru.yandex.market.tpl.carrier.core.domain.run.RunItem;
import ru.yandex.market.tpl.carrier.core.domain.run.RunRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.template.RunTemplateGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.template.RunTemplateItem;
import ru.yandex.market.tpl.carrier.core.domain.run.template.commands.NewRunTemplateItemData;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.Transport;
import ru.yandex.market.tpl.carrier.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouse;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseGenerator;
import ru.yandex.market.tpl.carrier.lms.service.run.item.LmsRunItemDtoMapper;
import ru.yandex.market.tpl.carrier.planner.lms.StringIdsDto;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class LmsRunItemControllerTest extends LmsControllerTest {

    static {
        CarrierAuditTracer.putSource(CarrierSource.SYSTEM);
    }

    private final ObjectMapper objectMapper;
    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final UserShiftRepository userShiftRepository;
    private final RunRepository runRepository;
    private final RunHelper runHelper;
    private final RunTemplateGenerator runTemplateGenerator;
    private final RunGenerator runGenerator;
    private final TestUserHelper testUserHelper;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final Clock clock;

    private final TransactionTemplate transactionTemplate;

    private Company company;
    private User user;
    private Transport transport;

    @BeforeEach
    void setUp() {
        company = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);
        user = testUserHelper.findOrCreateUser(1L);
        transport = testUserHelper.findOrCreateTransport();
    }

    @SneakyThrows
    @Test
    void shouldCreateRunItemIfNotStarted() {
        OrderWarehouse dropshipA = orderWarehouseGenerator.generateWarehouse();
        OrderWarehouse dropshipB = orderWarehouseGenerator.generateWarehouse();
        OrderWarehouse sortingCenter = orderWarehouseGenerator.generateWarehouse();

        var runTemplate = runTemplateGenerator.generate(cb -> {
            cb
                    .campaignId(company.getCampaignId())
                    .items(List.of(
                            new NewRunTemplateItemData(
                                    dropshipA.getYandexId(),
                                    sortingCenter.getYandexId(),
                                    1,
                                    EnumSet.allOf(DayOfWeek.class),
                                    false,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null
                            ),
                            new NewRunTemplateItemData(
                                    dropshipB.getYandexId(),
                                    sortingCenter.getYandexId(),
                                    2,
                                    EnumSet.allOf(DayOfWeek.class),
                                    false,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null
                            ))
                    );
        });

        var run = runGenerator.generate(RunGenerator.RunGenerateParam.builder()
                .externalId("run1")
                .deliveryServiceId(9001L)
                .campaignId(company.getCampaignId())
                .runDate(LocalDate.now(clock))
                .runTemplateId(runTemplate.getId())
                .build()
        );

        mockMvc.perform(post("/LMS/carrier/runs/{id}/items/add", run.getId())
                .content(objectMapper.writeValueAsString(new StringIdsDto(List.of(
                        LmsRunItemDtoMapper.RUN_TEMPLATE_ITEM_PREFIX + runTemplate.streamItems().findFirst().map(RunTemplateItem::getId).orElseThrow()
                ))))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
                .andExpect(status().isOk());

        transactionTemplate.execute(tc -> {
            var changed = runRepository.findByIdOrThrow(run.getId());

            Assertions.assertThat(changed.streamRunItems().toList())
                    .hasSize(1);

            return null;
        });
    }

    @SneakyThrows
    @Test
    void shouldAddRoutePointsToIntakeShift() {
        OrderWarehouse dropshipA = orderWarehouseGenerator.generateWarehouse();
        OrderWarehouse dropshipB = orderWarehouseGenerator.generateWarehouse();
        OrderWarehouse sortingCenter = orderWarehouseGenerator.generateWarehouse();

        var runTemplate = runTemplateGenerator.generate(cb -> {
            cb
                    .campaignId(company.getCampaignId())
                    .items(List.of(
                            new NewRunTemplateItemData(
                                    dropshipA.getYandexId(),
                                    sortingCenter.getYandexId(),
                                    1,
                                    EnumSet.allOf(DayOfWeek.class),
                                    false,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null
                            ),
                            new NewRunTemplateItemData(
                                    dropshipB.getYandexId(),
                                    sortingCenter.getYandexId(),
                                    2,
                                    EnumSet.allOf(DayOfWeek.class),
                                    false,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null
                            ))
                    );
        });

        var runTemplateItems = runTemplate.streamItems().toList();

        var run = runGenerator.generate(RunGenerator.RunGenerateParam.builder()
                .externalId("run1")
                .deliveryServiceId(9001L)
                .campaignId(company.getCampaignId())
                .runDate(LocalDate.now(clock))
                .runTemplateId(runTemplate.getId())
                .build()
        );

        mockMvc.perform(post("/LMS/carrier/runs/{id}/items/add", run.getId())
                .content(objectMapper.writeValueAsString(new StringIdsDto(List.of(
                        LmsRunItemDtoMapper.RUN_TEMPLATE_ITEM_PREFIX + runTemplateItems.get(0).getId()
                ))))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
                .andExpect(status().isOk());

        runHelper.assignUserAndTransport(run, user, transport);

        mockMvc.perform(post("/LMS/carrier/runs/{id}/items/add", run.getId())
                .content(objectMapper.writeValueAsString(new StringIdsDto(List.of(
                        LmsRunItemDtoMapper.RUN_TEMPLATE_ITEM_PREFIX + runTemplateItems.get(1).getId()
                ))))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
                .andExpect(status().isOk());

        transactionTemplate.execute(tc -> {
            var changed = runRepository.findByIdOrThrow(run.getId());

            List<RunItem> runItems = changed.streamRunItems().toList();

            Assertions.assertThat(runItems)
                    .hasSize(2);

            var changedShift = userShiftRepository.findByIdOrThrow(changed.getFirstAssignedShift().getId());

            List<RoutePoint> routePoints = changedShift.streamRoutePoints().toList();
            Assertions.assertThat(routePoints)
                    .hasSize(3);

            Assertions.assertThat(runItems.get(0).getCollectTaskId()).isEqualTo(
                    routePoints.get(0).streamCollectDropshipTasks().findFirst().orElseThrow().getId()
            );
            Assertions.assertThat(runItems.get(0).getReturnTaskId()).isEqualTo(
                    routePoints.get(2).streamReturnTasks().findFirst().orElseThrow().getId()
            );
            Assertions.assertThat(runItems.get(1).getCollectTaskId()).isEqualTo(
                    routePoints.get(1).streamCollectDropshipTasks().findFirst().orElseThrow().getId()
            );
            Assertions.assertThat(runItems.get(1).getReturnTaskId()).isEqualTo(
                    routePoints.get(2).streamReturnTasks().findFirst().orElseThrow().getId()
            );

            return null;
        });
    }

    @SneakyThrows
    @Test
    void shouldAddRoutePointsTo2x2Shift() {
        OrderWarehouse a = orderWarehouseGenerator.generateWarehouse();
        OrderWarehouse b = orderWarehouseGenerator.generateWarehouse();
        OrderWarehouse c = orderWarehouseGenerator.generateWarehouse();
        OrderWarehouse d = orderWarehouseGenerator.generateWarehouse();

        var runTemplate = runTemplateGenerator.generate(cb -> {
            cb
                    .campaignId(company.getCampaignId())
                    .items(List.of(
                            new NewRunTemplateItemData(
                                    a.getYandexId(),
                                    d.getYandexId(),
                                    1,
                                    EnumSet.allOf(DayOfWeek.class),
                                    false,
                                    1,
                                    4,
                                    null,
                                    null,
                                    null
                            ),
                            new NewRunTemplateItemData(
                                    b.getYandexId(),
                                    c.getYandexId(),
                                    2,
                                    EnumSet.allOf(DayOfWeek.class),
                                    false,
                                    2,
                                    3,
                                    null,
                                    null,
                                    null
                            ))
                    );
        });

        var runTemplateItems = runTemplate.streamItems().toList();

        var run = runGenerator.generate(RunGenerator.RunGenerateParam.builder()
                .externalId("run1")
                .deliveryServiceId(9001L)
                .campaignId(company.getCampaignId())
                .runDate(LocalDate.now(clock))
                .runTemplateId(runTemplate.getId())
                .build()
        );

        mockMvc.perform(post("/LMS/carrier/runs/{id}/items/add", run.getId())
                .content(objectMapper.writeValueAsString(new StringIdsDto(List.of(
                        LmsRunItemDtoMapper.RUN_TEMPLATE_ITEM_PREFIX + runTemplateItems.get(0).getId()
                ))))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
                .andExpect(status().isOk());

        runHelper.assignUserAndTransport(run, user, transport);

        mockMvc.perform(post("/LMS/carrier/runs/{id}/items/add", run.getId())
                .content(objectMapper.writeValueAsString(new StringIdsDto(List.of(
                        LmsRunItemDtoMapper.RUN_TEMPLATE_ITEM_PREFIX + runTemplateItems.get(1).getId()
                ))))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
                .andExpect(status().isOk());

        transactionTemplate.execute(tc -> {
            var changed = runRepository.findByIdOrThrow(run.getId());

            List<RunItem> runItems = changed.streamRunItems()
                    .sortedBy(RunItem::getOrderNumber)
                    .toList();

            Assertions.assertThat(runItems)
                    .hasSize(2);

            var changedShift = changed.getFirstAssignedShift();

            List<RoutePoint> routePoints = changedShift.streamRoutePoints()
                    .sortedBy(RoutePoint::getExplicitOrderNumber)
                    .toList();
            Assertions.assertThat(routePoints)
                    .hasSize(4);

            Assertions.assertThat(runItems.get(0).getCollectTaskId()).isEqualTo(
                    routePoints.get(0).streamCollectDropshipTasks().findFirst().orElseThrow().getId()
            );
            Assertions.assertThat(runItems.get(0).getReturnTaskId()).isEqualTo(
                    routePoints.get(3).streamReturnTasks().findFirst().orElseThrow().getId()
            );
            Assertions.assertThat(runItems.get(1).getCollectTaskId()).isEqualTo(
                    routePoints.get(1).streamCollectDropshipTasks().findFirst().orElseThrow().getId()
            );
            Assertions.assertThat(runItems.get(1).getReturnTaskId()).isEqualTo(
                    routePoints.get(2).streamReturnTasks().findFirst().orElseThrow().getId()
            );

            return null;
        });
    }


    @SneakyThrows
    @Test
    void shouldAllowToCreateRunItemInStarted() {
        OrderWarehouse dropshipA = orderWarehouseGenerator.generateWarehouse();
        OrderWarehouse dropshipB = orderWarehouseGenerator.generateWarehouse();
        OrderWarehouse sortingCenter = orderWarehouseGenerator.generateWarehouse();

        var runTemplate = runTemplateGenerator.generate(cb -> {
            cb
                    .campaignId(company.getCampaignId())
                    .items(List.of(
                            new NewRunTemplateItemData(
                                    dropshipA.getYandexId(),
                                    sortingCenter.getYandexId(),
                                    1,
                                    EnumSet.allOf(DayOfWeek.class),
                                    false,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null
                            ),
                            new NewRunTemplateItemData(
                                    dropshipB.getYandexId(),
                                    sortingCenter.getYandexId(),
                                    2,
                                    EnumSet.allOf(DayOfWeek.class),
                                    false,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null
                            ))
                    );
        });

        var run = runGenerator.generate(RunGenerator.RunGenerateParam.builder()
                .externalId("run1")
                .deliveryServiceId(9001L)
                .campaignId(company.getCampaignId())
                .runDate(LocalDate.now(clock))
                .runTemplateId(runTemplate.getId())
                .item(RunGenerator.RunItemGenerateParam.builder()
                        .movement(MovementCommand.Create.builder()
                                .orderWarehouse(dropshipA)
                                .orderWarehouseTo(sortingCenter)
                                .build())
                        .orderNumber(1)
                        .build()
                ).build()
        );
        List<RunTemplateItem> runTemplateItems = runTemplate.streamItems().toList();

        var userShift = runHelper.assignUserAndTransport(run, user, transport);

        testUserHelper.openShift(user, userShift.getId());

        mockMvc.perform(post("/LMS/carrier/runs/{id}/items/add", run.getId())
                .content(objectMapper.writeValueAsString(new StringIdsDto(List.of(
                        LmsRunItemDtoMapper.RUN_TEMPLATE_ITEM_PREFIX + runTemplate.streamItems().toList().get(1).getId()
                ))))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
                .andExpect(status().isOk());

        transactionTemplate.execute(tc -> {
           var update = runRepository.findByIdOrThrow(run.getId());

           Assertions.assertThat(update.streamRunItems().toList())
                   .hasSize(2);

           Assertions.assertThat(update.getFirstAssignedShift().streamRoutePoints().toList())
                   .hasSize(3);

           return null;
        });
    }

    @SneakyThrows
    @Test
    void shouldRemoveRoutePointFromDirectRun() {
        var run = runGenerator.generate(RunGenerator.RunGenerateParam.builder()
                .externalId("run1")
                .deliveryServiceId(9001L)
                .campaignId(company.getCampaignId())
                .runDate(LocalDate.now(clock))
                .item(RunGenerator.RunItemGenerateParam.builder()
                        .movement(MovementCommand.Create.builder()
                                .orderWarehouseTo(orderWarehouseGenerator.generateWarehouse())
                                .build())
                        .orderNumber(1)
                        .build()
                )
                .item(RunGenerator.RunItemGenerateParam.builder()
                        .movement(MovementCommand.Create.builder()
                                .orderWarehouseTo(orderWarehouseGenerator.generateWarehouse())
                                .build())
                        .orderNumber(2)
                        .build()
                ).build()
        );

        var userShift = runHelper.assignUserAndTransport(run, user, transport);
        Assertions.assertThat(userShift.streamRoutePoints().count()).isEqualTo(4);

        RunItem runItemToDelete = run.streamRunItems().findFirst().orElseThrow();

        mockMvc.perform(post("/LMS/carrier/runs/{runId}/items/delete", run.getId())
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(objectMapper.writeValueAsString(new StringIdsDto(List.of(LmsRunItemDtoMapper.RUN_ITEM_PREFIX + runItemToDelete.getId()))))
        ).andExpect(status().isOk());

        transactionTemplate.execute(tc -> {
            var userShift2 = userShiftRepository.findByIdOrThrow(userShift.getId());

            Assertions.assertThat(userShift2.streamRoutePoints().toList()).hasSize(2);
            return null;
        });
    }

    @SneakyThrows
    @Test
    void shouldRemoveRoutePointFromIntakeRun() {
        OrderWarehouse orderWarehouseTo = orderWarehouseGenerator.generateWarehouse();
        var run = runGenerator.generate(RunGenerator.RunGenerateParam.builder()
                .externalId("run1")
                .deliveryServiceId(9001L)
                .campaignId(company.getCampaignId())
                .runDate(LocalDate.now(clock))
                .item(RunGenerator.RunItemGenerateParam.builder()
                        .movement(MovementCommand.Create.builder()
                                .orderWarehouseTo(orderWarehouseTo)
                                .build())
                        .orderNumber(1)
                        .build()
                )
                .item(RunGenerator.RunItemGenerateParam.builder()
                        .movement(MovementCommand.Create.builder()
                                .orderWarehouseTo(orderWarehouseTo)
                                .build())
                        .orderNumber(2)
                        .build()
                ).build()
        );

        var userShift = runHelper.assignUserAndTransport(run, user, transport);
        Assertions.assertThat(userShift.streamRoutePoints().count()).isEqualTo(3);

        RunItem runItemToDelete = run.streamRunItems().findFirst().orElseThrow();

        mockMvc.perform(post("/LMS/carrier/runs/{runId}/items/delete", run.getId())
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(objectMapper.writeValueAsString(new StringIdsDto(List.of(LmsRunItemDtoMapper.RUN_ITEM_PREFIX + runItemToDelete.getId()))))
        ).andExpect(status().isOk());

        transactionTemplate.execute(tc -> {
            var userShift2 = userShiftRepository.findByIdOrThrow(userShift.getId());

            Assertions.assertThat(userShift2.streamRoutePoints().toList()).hasSize(2);
            return null;
        });
    }
}
