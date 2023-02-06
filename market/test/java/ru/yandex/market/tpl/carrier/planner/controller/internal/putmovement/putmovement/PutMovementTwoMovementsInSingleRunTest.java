package ru.yandex.market.tpl.carrier.planner.controller.internal.putmovement.putmovement;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.context.jdbc.Sql;

import ru.yandex.market.logistic.api.model.common.ResourceId;
import ru.yandex.market.tpl.carrier.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.carrier.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunItem;
import ru.yandex.market.tpl.carrier.core.domain.run.RunRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.RunStatus;
import ru.yandex.market.tpl.carrier.core.domain.run.template.RunTemplateGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.template.commands.NewRunTemplateItemData;
import ru.yandex.market.tpl.carrier.core.domain.run.template.commands.RunTemplateCommand;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.planner.controller.BasePlannerWebTest;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.tpl.carrier.planner.controller.internal.putmovement.putmovement.PutMovementControllerTestUtil.prepareMovement;
import static ru.yandex.market.tpl.carrier.planner.controller.internal.putmovement.putmovement.PutMovementControllerTestUtil.wrap;

@RequiredArgsConstructor(onConstructor_=@Autowired)
public class PutMovementTwoMovementsInSingleRunTest extends BasePlannerWebTest {
    private static final long DELIVERY_SERVICE_ID = 100500L;
    private static final long CAMPAIGN_ID = 1001381924L;

    private final DbQueueTestUtil dbQueueTestUtil;
    private final RunRepository runRepository;
    private final RunTemplateGenerator runTemplateGenerator;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final TestUserHelper testUserHelper;

    private final XmlMapper xmlMapper = Jackson2ObjectMapperBuilder.xml()
            .createXmlMapper(true)
            .build();


    @BeforeEach
    void setUp() {
        configurationServiceAdapter.mergeValue(ConfigurationProperties.RUN_TEMPLATE_ENABLED, true);
        configurationServiceAdapter.mergeValue(ConfigurationProperties.DELIVERY_SERVICES_TO_AUTOCONFIRM, DELIVERY_SERVICE_ID);
    }

    @Sql("classpath:mockPartner/deliveryServiceWithSCWithTokens.sql")
    @SneakyThrows
    @Test
    void shouldCreateDbQueueEntryOnPutMovement() {
        Company company = testUserHelper.findOrCreateCompany(TestUserHelper.CompanyGenerateParam.builder()
                .companyName(Company.DEFAULT_COMPANY_NAME)
                .campaignId(CAMPAIGN_ID)
                .build());

        prepareRunTemplate();

        mockMvc.perform(post("/delivery/query-gateway/putMovement")
                .content(xmlMapper.writeValueAsString(wrap(
                        prepareMovement(
                                new ResourceId("TMM1", null),
                                new ResourceId("1", "1234"),
                                new ResourceId("3", "1234")
                        )
                )))
                .contentType(MediaType.TEXT_XML_VALUE))
                .andExpect(status().isOk());

        mockMvc.perform(post("/delivery/query-gateway/putMovement")
                .content(xmlMapper.writeValueAsString(wrap(
                        prepareMovement(
                                new ResourceId("TMM2", null),
                                new ResourceId("2", "1234"),
                                new ResourceId("3", "1234")

                        )
                )))
                .contentType(MediaType.TEXT_XML_VALUE))
                .andExpect(status().isOk());

        dbQueueTestUtil.assertQueueHasSize(QueueType.CREATE_RUN_FROM_TEMPLATE, 2);
        dbQueueTestUtil.executeAllQueueItems(QueueType.CREATE_RUN_FROM_TEMPLATE);

        transactionTemplate.execute(tc -> {
        List<Run> runs = runRepository.findAll();
        Assertions.assertThat(runs).isNotEmpty();

        Run run = runs.get(0);
        Assertions.assertThat(run.getStatus()).isEqualTo(RunStatus.CONFIRMED);
        Assertions.assertThat(run.getDeliveryServiceId()).isEqualTo(DELIVERY_SERVICE_ID);
        Assertions.assertThat(run.getCompany().getId()).isEqualTo(company.getId());
        Assertions.assertThat(run.streamRunItems().toList()).hasSize(2);

        RunItem item1 = run.streamRunItems().findFirst(ri -> ri.getOrderNumber() == 1).orElseThrow();
        Assertions.assertThat(item1.getOrderNumber()).isEqualTo(1);
        Assertions.assertThat(item1.getMovement().getExternalId()).isEqualTo("TMM1");

        RunItem item2 = run.streamRunItems().findFirst(ri -> ri.getOrderNumber() == 2).orElseThrow();
        Assertions.assertThat(item2.getOrderNumber()).isEqualTo(2);
        Assertions.assertThat(item2.getMovement().getExternalId()).isEqualTo("TMM2");
        return null;
        });
    }

    private void prepareRunTemplate() {
        runTemplateGenerator.generate(RunTemplateCommand.Create.builder()
                .deliveryServiceId(DELIVERY_SERVICE_ID)
                .externalId("abc123")
                .campaignId(CAMPAIGN_ID)
                .items(List.of(
                        NewRunTemplateItemData.builder()
                                .warehouseYandexIdFrom("1")
                                .warehouseYandexIdTo("3")
                                .orderNumber(1)
                                .daysOfWeek(Set.of(DayOfWeek.WEDNESDAY))
                                .build(),
                        NewRunTemplateItemData.builder()
                                .warehouseYandexIdFrom("2")
                                .warehouseYandexIdTo("3")
                                .orderNumber(2)
                                .daysOfWeek(Set.of(DayOfWeek.WEDNESDAY))
                                .build()
                ))
                .build()
        );
    }
}
