package ru.yandex.market.tpl.carrier.planner.controller.internal.putmovement.putmovement;

import java.math.BigDecimal;
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
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistic.api.model.common.ResourceId;
import ru.yandex.market.tpl.carrier.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.carrier.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.carrier.core.domain.movement.Movement;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementRepository;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementStatus;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunItem;
import ru.yandex.market.tpl.carrier.core.domain.run.RunRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.template.RunTemplate;
import ru.yandex.market.tpl.carrier.core.domain.run.template.RunTemplateGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.template.commands.NewRunTemplateItemData;
import ru.yandex.market.tpl.carrier.core.domain.run.template.commands.RunTemplateCommand;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.planner.controller.BasePlannerWebTest;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_=@Autowired)
@Sql("classpath:mockPartner/deliveryServiceWithSCWithTokens.sql")
public class PutMovementWithZeroVolumeRunTest extends BasePlannerWebTest {
    private static final long CAMPAIGN_ID = 1001381924L;
    private static final long DELIVERY_SERVICE_ID = 100500L;
    private static final long PARTNER_ID = 2234562L;

    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final DbQueueTestUtil dbQueueTestUtil;
    private final TestUserHelper testUserHelper;
    private final RunTemplateGenerator runTemplateGenerator;
    private final XmlMapper xmlMapper = Jackson2ObjectMapperBuilder.xml().build();

    private final MovementRepository movementRepository;
    private final RunRepository runRepository;
    private final TransactionTemplate transactionTemplate;

    @SneakyThrows
    @BeforeEach
    void setUp() {
        configurationServiceAdapter.mergeValue(ConfigurationProperties.RUN_TEMPLATE_ENABLED, true);
        configurationServiceAdapter.mergeValue(ConfigurationProperties.CANCEL_MOVEMENT_WHERE_VOLUME_IS_ZERO, true);
        configurationServiceAdapter.mergeValue(ConfigurationProperties.DELIVERY_SERVICES_TO_CANCEL_MOVEMENTS_IF_VOLUME_IS_ZERO, DELIVERY_SERVICE_ID);

        Company company = testUserHelper.findOrCreateCompany(TestUserHelper.CompanyGenerateParam.builder()
                .companyName(Company.DEFAULT_COMPANY_NAME)
                .campaignId(CAMPAIGN_ID)
                .build());


    }

    @SneakyThrows
    @Test
    void shouldNotCreateRunForMovementWithZeroVolume() {
        prepareRunTemplate();

        mockMvc.perform(post("/delivery/query-gateway/putMovement")
                .content(xmlMapper.writeValueAsString(
                        PutMovementControllerTestUtil.wrap(
                                PutMovementControllerTestUtil.prepareMovement(
                                        new ResourceId("TMM1", String.valueOf(PARTNER_ID)),
                                        BigDecimal.ZERO,
                                        new ResourceId("20", "1234"),
                                        new ResourceId("200", "1234")
                                )
                        )
                ))
                .contentType(MediaType.TEXT_XML_VALUE))
                .andDo(log())
                .andExpect(status().isOk());

        dbQueueTestUtil.assertQueueHasSize(QueueType.CREATE_RUN_FROM_TEMPLATE, 1);
        dbQueueTestUtil.executeSingleQueueItem(QueueType.CREATE_RUN_FROM_TEMPLATE);

        Movement movement = movementRepository.findByExternalId("TMM1").orElseThrow();
        Assertions.assertThat(movement).isNotNull();
        Assertions.assertThat(movement.getStatus()).isEqualTo(MovementStatus.CANCELLED);

        List<Run> runs = runRepository.findAll();
        Assertions.assertThat(runs).isEmpty();
    }

    @SneakyThrows
    @Test
    void shouldCreateRunForMovementWithZeroVolumeIfIgnoreVolume() {
        prepareRunTemplateIgnoreVolume();

        mockMvc.perform(post("/delivery/query-gateway/putMovement")
                .content(xmlMapper.writeValueAsString(
                        PutMovementControllerTestUtil.wrap(
                                PutMovementControllerTestUtil.prepareMovement(
                                        new ResourceId("TMM1", String.valueOf(PARTNER_ID)),
                                        BigDecimal.ZERO,
                                        new ResourceId("20", "1234"),
                                        new ResourceId("200", "1234")
                                )
                        )
                ))
                .contentType(MediaType.TEXT_XML_VALUE))
                .andDo(log())
                .andExpect(status().isOk());

        dbQueueTestUtil.assertQueueHasSize(QueueType.CREATE_RUN_FROM_TEMPLATE, 1);
        dbQueueTestUtil.executeSingleQueueItem(QueueType.CREATE_RUN_FROM_TEMPLATE);

        Movement movement = movementRepository.findByExternalId("TMM1").orElseThrow();
        Assertions.assertThat(movement).isNotNull();
        Assertions.assertThat(movement.getStatus()).isEqualTo(MovementStatus.CANCELLED);

        transactionTemplate.execute(tc -> {
            List<Run> runs = runRepository.findAll();
            Assertions.assertThat(runs).hasSize(1);

            List<RunItem> runItems = runs.get(0).streamRunItems().toList();
            Assertions.assertThat(runItems).hasSize(1);
            return null;
        });

    }

    private RunTemplate prepareRunTemplate() {
        return runTemplateGenerator.generate(RunTemplateCommand.Create.builder()
                .deliveryServiceId(DELIVERY_SERVICE_ID)
                .externalId("abc")
                .campaignId(CAMPAIGN_ID)
                .items(List.of(
                        NewRunTemplateItemData.builder()
                                .warehouseYandexIdFrom("20")
                                .warehouseYandexIdTo("200")
                                .orderNumber(1)
                                .daysOfWeek(Set.of(DayOfWeek.WEDNESDAY))
                                .build()
                ))
                .build());
    }

    private RunTemplate prepareRunTemplateIgnoreVolume() {
        return runTemplateGenerator.generate(RunTemplateCommand.Create.builder()
                .deliveryServiceId(DELIVERY_SERVICE_ID)
                .externalId("abc")
                .campaignId(CAMPAIGN_ID)
                .items(List.of(
                        NewRunTemplateItemData.builder()
                                .warehouseYandexIdFrom("20")
                                .warehouseYandexIdTo("200")
                                .orderNumber(1)
                                .daysOfWeek(Set.of(DayOfWeek.WEDNESDAY))
                                .ignoreVolume(true)
                                .build()
                ))
                .build());
    }
}
