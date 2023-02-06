package ru.yandex.market.tpl.carrier.lms.controller;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.tpl.carrier.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.movement.Movement;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.template.RunTemplate;
import ru.yandex.market.tpl.carrier.core.domain.run.template.RunTemplateCommandService;
import ru.yandex.market.tpl.carrier.core.domain.run.template.commands.NewRunTemplateItemData;
import ru.yandex.market.tpl.carrier.core.domain.run.template.commands.RunTemplateCommand;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseGenerator;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehousePartner;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehousePartnerRepository;
import ru.yandex.market.tpl.carrier.lms.service.runplanner.RunTemplateAndDate;
import ru.yandex.market.tpl.carrier.planner.lms.IdsDto;
import ru.yandex.market.tpl.carrier.planner.lms.runplanner.LmsRunPlannerDto;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.common.util.DateTimeUtil;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class LmsRunPlannerTest extends LmsControllerTest {

    private static final LocalDate TODAY = LocalDate.of(2021, Month.NOVEMBER, 1);
    private final RunTemplateCommandService runTemplateCommandService;
    private final TestUserHelper testUserHelper;
    private final ObjectMapper tplObjectMapper;
    private final MovementGenerator movementGenerator;
    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final OrderWarehousePartnerRepository orderWarehousePartnerRepository;

    private final DbQueueTestUtil dbQueueTestUtil;

    @SneakyThrows
    @Test
    void shouldGetRunPlannerPage() {
        mockMvc.perform(MockMvcRequestBuilders.get("/LMS/carrier/run-planner"))
                .andExpect(status().isOk());
    }

    @SneakyThrows
    @Test
    void shouldPostRunPlannerSearch() {
        var company = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);

        runTemplateCommandService.create(RunTemplateCommand.Create.builder()
                .externalId("name")
                .campaignId(company.getCampaignId())
                .deliveryServiceId(9001L)
                .items(List.of(
                        NewRunTemplateItemData.builder()
                                .warehouseYandexIdFrom("123")
                                .warehouseYandexIdTo("234")
                                .daysOfWeek(Set.of(DayOfWeek.MONDAY))
                                .build()
                ))
                .build());

        mockMvc.perform(MockMvcRequestBuilders.post("/LMS/carrier/run-planner/carrierRunPlanner/carrierRunPlanner")
                .content(tplObjectMapper.writeValueAsString(new LmsRunPlannerDto(TODAY.toString(), null)))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").value(Matchers.hasSize(1)));
    }

    @SneakyThrows
    @Test
    void shouldPostRunPlannerSearchAndFilterByDeliveryServiceId() {
        var company = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);

        runTemplateCommandService.create(RunTemplateCommand.Create.builder()
                .externalId("name")
                .campaignId(company.getCampaignId())
                .deliveryServiceId(9001L)
                .items(List.of(
                        NewRunTemplateItemData.builder()
                                .warehouseYandexIdFrom("123")
                                .warehouseYandexIdTo("234")
                                .daysOfWeek(Set.of(DayOfWeek.MONDAY))
                                .build()
                ))
                .build());

        RunTemplate runTemplate2 = runTemplateCommandService.create(RunTemplateCommand.Create.builder()
                .externalId("name2")
                .campaignId(company.getCampaignId())
                .deliveryServiceId(9002L)
                .items(List.of(
                        NewRunTemplateItemData.builder()
                                .warehouseYandexIdFrom("234")
                                .warehouseYandexIdTo("345")
                                .daysOfWeek(Set.of(DayOfWeek.MONDAY))
                                .build()
                ))
                .build());

        mockMvc.perform(MockMvcRequestBuilders.post("/LMS/carrier/run-planner/carrierRunPlanner/carrierRunPlanner")
                .content(tplObjectMapper.writeValueAsString(new LmsRunPlannerDto(TODAY.toString(), 9002L)))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.items[0].values.template.id").value(runTemplate2.getId()));
    }

    @SneakyThrows

    @Test
    void shouldNotReturnDeletedRuns() {
        var company = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);

        RunTemplate runTemplate = runTemplateCommandService.create(RunTemplateCommand.Create.builder()
                .externalId("name")
                .campaignId(company.getCampaignId())
                .deliveryServiceId(9001L)
                .items(List.of(
                        NewRunTemplateItemData.builder()
                                .warehouseYandexIdFrom("123")
                                .warehouseYandexIdTo("234")
                                .daysOfWeek(Set.of(DayOfWeek.MONDAY))
                                .build()
                ))
                .build());

        runTemplateCommandService.delete(new RunTemplateCommand.Delete(runTemplate.getId()));


        mockMvc.perform(MockMvcRequestBuilders.post("/LMS/carrier/run-planner/carrierRunPlanner/carrierRunPlanner")
                .content(tplObjectMapper.writeValueAsString(new LmsRunPlannerDto(TODAY.toString(), 9001L)))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @SneakyThrows

    @Test
    void shouldNotReturnDeletedRunItems() {
        var company = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);

        RunTemplate runTemplate = runTemplateCommandService.create(RunTemplateCommand.Create.builder()
                .externalId("name")
                .campaignId(company.getCampaignId())
                .deliveryServiceId(9001L)
                .items(List.of(
                        NewRunTemplateItemData.builder()
                                .warehouseYandexIdFrom("123")
                                .warehouseYandexIdTo("234")
                                .daysOfWeek(Set.of(DayOfWeek.MONDAY))
                                .build()
                ))
                .build());

        runTemplateCommandService.deleteItem(new RunTemplateCommand.DeleteItem(
                runTemplate.getId(), runTemplate.streamItems().findFirst().get().getId()
        ));


        mockMvc.perform(MockMvcRequestBuilders.post("/LMS/carrier/run-planner/carrierRunPlanner/carrierRunPlanner")
                .content(tplObjectMapper.writeValueAsString(new LmsRunPlannerDto(TODAY.toString(), 9001L)))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @SneakyThrows
    @Test
    void shouldNotReturnDuplicatedRunTemplates() {
        var company = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);

        RunTemplate runTemplate = runTemplateCommandService.create(RunTemplateCommand.Create.builder()
                .externalId("name")
                .campaignId(company.getCampaignId())
                .deliveryServiceId(9001L)
                .items(List.of(
                        NewRunTemplateItemData.builder()
                                .orderNumber(1)
                                .warehouseYandexIdFrom("123")
                                .warehouseYandexIdTo("234")
                                .daysOfWeek(Set.of(DayOfWeek.MONDAY))
                                .build(),
                        NewRunTemplateItemData.builder()
                                .orderNumber(2)
                                .warehouseYandexIdFrom("234")
                                .warehouseYandexIdTo("345")
                                .daysOfWeek(Set.of(DayOfWeek.MONDAY))
                                .build()
                ))
                .build());

        mockMvc.perform(MockMvcRequestBuilders.post("/LMS/carrier/run-planner/carrierRunPlanner/carrierRunPlanner")
                .content(tplObjectMapper.writeValueAsString(new LmsRunPlannerDto(TODAY.toString(), 9001L)))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").value(Matchers.hasSize(1)));
    }


    @SneakyThrows

    @Test
    void shouldGetRunTemplateItems() {
        var company = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);

        Movement movement = movementGenerator.generate(MovementCommand.Create.builder()
                .orderWarehouse(orderWarehouseGenerator.generateWarehouse(w -> {
                    w.setYandexId("123");
                    w.setPartner(orderWarehousePartnerRepository.save(new OrderWarehousePartner("234", "Even partner")));
                }))
                .orderWarehouseTo(orderWarehouseGenerator.generateWarehouse(w -> {
                    w.setYandexId("234");
                    w.setPartner(orderWarehousePartnerRepository.save(new OrderWarehousePartner("345", "Odd partner")));
                }))
                .deliveryIntervalFrom(TODAY.atTime(9, 0).toInstant(DateTimeUtil.DEFAULT_ZONE_ID))
                .build());

        RunTemplate runTemplate = runTemplateCommandService.create(RunTemplateCommand.Create.builder()
                .externalId("name")
                .campaignId(company.getCampaignId())
                .deliveryServiceId(9001L)
                .items(List.of(
                        NewRunTemplateItemData.builder()
                                .warehouseYandexIdFrom("123")
                                .warehouseYandexIdTo("234")
                                .daysOfWeek(Set.of(DayOfWeek.MONDAY))
                                .build()
                ))
                .build());

        mockMvc.perform(MockMvcRequestBuilders.get("/LMS/carrier/run-planner/{id}/items",
                new RunTemplateAndDate(runTemplate.getId(), TODAY).toId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.items[0].values.movement").exists())
                .andExpect(jsonPath("$.items[0].values.movement.id").value(String.valueOf(movement.getId())))
                .andExpect(jsonPath("$.items[0].values.searchMovementLink.url").value(
                        Matchers.startsWith(
                            "http://localhost:8998/transport-manager/transportations"
                        )
                ));

    }

    @SneakyThrows
    @Test
    void shouldMatchMovementForRunTemplateItem() {
        var company = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);

        Movement movement = movementGenerator.generate(MovementCommand.Create.builder()
                .orderWarehouse(orderWarehouseGenerator.generateWarehouse(w -> w.setYandexId("1234")))
                .orderWarehouseTo(orderWarehouseGenerator.generateWarehouse(w -> w.setYandexId("2344")))
                .deliveryIntervalFrom(TODAY.atTime(9, 0).toInstant(DateTimeUtil.DEFAULT_ZONE_ID))
                .build());

        Movement movement2 = movementGenerator.generate(MovementCommand.Create.builder()
                .orderWarehouse(orderWarehouseGenerator.generateWarehouse(w -> w.setYandexId("123")))
                .orderWarehouseTo(orderWarehouseGenerator.generateWarehouse(w -> w.setYandexId("234")))
                .deliveryIntervalFrom(TODAY.atTime(12, 0).toInstant(DateTimeUtil.DEFAULT_ZONE_ID))
                .build());

        RunTemplate runTemplate = runTemplateCommandService.create(RunTemplateCommand.Create.builder()
                .externalId("name")
                .campaignId(company.getCampaignId())
                .deliveryServiceId(9001L)
                .items(List.of(
                        NewRunTemplateItemData.builder()
                                .warehouseYandexIdFrom("123")
                                .warehouseYandexIdTo("234")
                                .daysOfWeek(Set.of(DayOfWeek.MONDAY))
                                .fromTime(LocalTime.of(12, 0))
                                .build()
                ))
                .build());

        mockMvc.perform(MockMvcRequestBuilders.get("/LMS/carrier/run-planner/{id}/items",
                new RunTemplateAndDate(runTemplate.getId(), TODAY).toId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.items[0].values.movement").exists())
                .andExpect(jsonPath("$.items[0].values.movement.id").value(String.valueOf(movement2.getId())));

    }

    @SneakyThrows
    @Test
    void shouldRetryRunTemplateItems() {
        var company = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);

        movementGenerator.generate(MovementCommand.Create.builder()
                .orderWarehouse(orderWarehouseGenerator.generateWarehouse(w -> w.setYandexId("123")))
                .orderWarehouseTo(orderWarehouseGenerator.generateWarehouse(w -> w.setYandexId("234")))
                .deliveryIntervalFrom(TODAY.atTime(9, 0).toInstant(DateTimeUtil.DEFAULT_ZONE_ID))
                .build());

        RunTemplate runTemplate = runTemplateCommandService.create(RunTemplateCommand.Create.builder()
                .externalId("name")
                .campaignId(company.getCampaignId())
                .deliveryServiceId(9001L)
                .items(List.of(
                        NewRunTemplateItemData.builder()
                                .warehouseYandexIdFrom("123")
                                .warehouseYandexIdTo("234")
                                .daysOfWeek(Set.of(DayOfWeek.MONDAY))
                                .build()
                ))
                .build());

        long runTemplateItemId = runTemplate.streamItems().findFirst().orElseThrow().getId();


        mockMvc.perform(MockMvcRequestBuilders.post("/LMS/carrier/run-planner/{id}/items/retry",
                new RunTemplateAndDate(runTemplate.getId(), TODAY).toId())
                .content(tplObjectMapper.writeValueAsString(new IdsDto(List.of(runTemplateItemId))))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
                .andExpect(status().isOk());

        dbQueueTestUtil.assertQueueHasSize(QueueType.CREATE_RUN_FROM_TEMPLATE, 1);
    }
}
