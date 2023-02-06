package ru.yandex.market.tpl.carrier.planner.controller.internal.putmovement.putmovement;

import java.util.List;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.context.jdbc.Sql;

import ru.yandex.market.logistic.api.model.common.ResourceId;
import ru.yandex.market.tpl.carrier.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.carrier.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.RunStatus;
import ru.yandex.market.tpl.carrier.core.domain.run.template.RunTemplateGenerator;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.planner.controller.BasePlannerWebTest;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;

import static ru.yandex.market.tpl.carrier.core.domain.run.template.RunTemplateGenerator.DELIVERY_SERVICE_ID;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class PutMovementControllerTest extends BasePlannerWebTest {

    private static final String NAME = "Маршрут 1";

    private final DbQueueTestUtil dbQueueTestUtil;
    private final RunRepository runRepository;
    private final RunTemplateGenerator runTemplateGenerator;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final TestUserHelper testUserHelper;
    private final PutMovementHelper putMovementHelper;

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
                .campaignId(RunTemplateGenerator.CAMPAIGN_ID)
                .build());

        runTemplateGenerator.generate((builder) -> builder.externalId(NAME));

        putMovementHelper.performPutMovement(PutMovementControllerTestUtil.wrap(
                PutMovementControllerTestUtil.prepareMovement(
                        new ResourceId("TMM1", null)
                )
        ));

        dbQueueTestUtil.assertQueueHasSize(QueueType.CREATE_RUN_FROM_TEMPLATE, 1);
        dbQueueTestUtil.executeSingleQueueItem(QueueType.CREATE_RUN_FROM_TEMPLATE);

        List<Run> runs = runRepository.findAll();
        Assertions.assertThat(runs).hasSize(1);

        Run run = runs.get(0);
        Assertions.assertThat(run.getStatus()).isEqualTo(RunStatus.CONFIRMED);
        Assertions.assertThat(run.getDeliveryServiceId()).isEqualTo(DELIVERY_SERVICE_ID);
        Assertions.assertThat(run.getCompany()).isNotNull();
        Assertions.assertThat(run.getCompany().getId()).isEqualTo(company.getId());
        Assertions.assertThat(run.getName()).isEqualTo(NAME);
    }


}
