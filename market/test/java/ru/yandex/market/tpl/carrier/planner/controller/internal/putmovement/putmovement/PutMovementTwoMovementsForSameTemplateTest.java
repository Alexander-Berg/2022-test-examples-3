package ru.yandex.market.tpl.carrier.planner.controller.internal.putmovement.putmovement;

import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import ru.yandex.market.logistic.api.model.common.ResourceId;
import ru.yandex.market.tpl.carrier.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.carrier.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.template.RunTemplate;
import ru.yandex.market.tpl.carrier.core.domain.run.template.RunTemplateGenerator;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.planner.controller.BasePlannerWebTest;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

@RequiredArgsConstructor(onConstructor_=@Autowired)
@Sql("classpath:mockPartner/deliveryServiceWithSCWithTokens.sql")
public class PutMovementTwoMovementsForSameTemplateTest extends BasePlannerWebTest {

    private static final LocalDate DATE = PutMovementControllerTestUtil.DEFAULT_INTERVAL.getFrom().toLocalDate();;

    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final RunTemplateGenerator runTemplateGenerator;
    private final PutMovementHelper putMovementHelper;
    private final RunRepository runRepository;
    private final TestUserHelper testUserHelper;
    private final DbQueueTestUtil dbQueueTestUtil;

    private RunTemplate runTemplate;

    @BeforeEach
    void setUp() {
        testUserHelper.findOrCreateCompany(TestUserHelper.CompanyGenerateParam.builder()
                .companyName(Company.DEFAULT_COMPANY_NAME)
                .campaignId(RunTemplateGenerator.CAMPAIGN_ID)
                .build());
        runTemplate = runTemplateGenerator.generate();

        configurationServiceAdapter.mergeValue(ConfigurationProperties.RUN_TEMPLATE_ENABLED, true);
    }

    @SneakyThrows
    @Test
    void shouldCreateTwoRunsForOneTemplate() {
        putMovementHelper.performPutMovement(
                PutMovementControllerTestUtil.wrap(PutMovementControllerTestUtil.prepareMovement(
                        new ResourceId("TMM1", null),
                        new ResourceId("20", "20000"),
                        new ResourceId("200", "22000")
                ))
        ).andExpect(xpath("//root/requestState/isError").booleanValue(false));

        putMovementHelper.performPutMovement(
                PutMovementControllerTestUtil.wrap(PutMovementControllerTestUtil.prepareMovement(
                        new ResourceId("TMM2", null),
                        new ResourceId("20", "20000"),
                        new ResourceId("200", "22000")
                ))
        ).andExpect(xpath("//root/requestState/isError").booleanValue(false));;

        dbQueueTestUtil.assertQueueHasSize(QueueType.CREATE_RUN_FROM_TEMPLATE, 2);
        dbQueueTestUtil.executeAllQueueItems(QueueType.CREATE_RUN_FROM_TEMPLATE);

        List<Run> runs = runRepository.findByTemplateIdAndRunDate(runTemplate.getId(), DATE);

        Assertions.assertThat(runs).hasSize(2);
        Assertions.assertThat(runs).extracting(Run::getExternalId)
                .containsExactlyInAnyOrder(
                        "RTG" + runTemplate.getId() + "-" + DATE,
                        "RTG" + runTemplate.getId() + "-" + DATE + "-2"
                );

    }
}
