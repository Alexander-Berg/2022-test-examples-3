package ru.yandex.market.tpl.carrier.planner.controller.internal.puttrip;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.RunStatus;
import ru.yandex.market.tpl.carrier.core.domain.run.template.RunTemplateGenerator;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.planner.controller.BasePlannerWebTest;
import ru.yandex.market.tpl.carrier.planner.controller.internal.putmovement.putmovement.PutMovementControllerTestUtil;
import ru.yandex.market.tpl.carrier.planner.controller.internal.putmovement.putmovement.PutMovementHelper;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;

import static ru.yandex.market.tpl.carrier.core.domain.run.template.RunTemplateGenerator.DELIVERY_SERVICE_ID;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class PutTripControllerTest extends BasePlannerWebTest {

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

    private Company company;

    @BeforeEach
    void setUp() {
        configurationServiceAdapter.mergeValue(ConfigurationProperties.RUN_TEMPLATE_ENABLED, true);

        company = testUserHelper.findOrCreateCompany(TestUserHelper.CompanyGenerateParam.builder()
                .companyName(Company.DEFAULT_COMPANY_NAME)
                .campaignId(RunTemplateGenerator.CAMPAIGN_ID)
                .deliveryServiceIds(Set.of(DELIVERY_SERVICE_ID))
                .build());
    }

    @Sql("classpath:mockPartner/deliveryServiceWithSCWithTokens.sql")
    @SneakyThrows
    @Test
    void shouldCreateRunWithPutTrip() {
        performPutTrip("TMT1", List.of("TMM1", "TMM2"));

        List<Run> runs = runRepository.findAll();
        Assertions.assertThat(runs).hasSize(1);

        Run run = runs.get(0);
        Assertions.assertThat(run.getStatus()).isEqualTo(RunStatus.CREATED);
        Assertions.assertThat(run.getStartDateTime()).isEqualTo(PutMovementControllerTestUtil.DEFAULT_INTERVAL.getFrom().toInstant());
        Assertions.assertThat(run.getEndDateTime()).isEqualTo(PutMovementControllerTestUtil.DEFAULT_INTERVAL.getTo().toInstant());
        Assertions.assertThat(run.getDeliveryServiceId()).isEqualTo(RunTemplateGenerator.DELIVERY_SERVICE_ID);
        Assertions.assertThat(run.getCompany().getId()).isEqualTo(company.getId());
    }

    @Sql("classpath:mockPartner/deliveryServiceWithSCWithTokens.sql")
    @SneakyThrows
    @Test
    void shouldUpdateRunWithPutTrip() {
        performPutTrip("TMT1", List.of("TMM1", "TMM2"));
        transactionTemplate.execute(tx -> {
            List<Run> runs = runRepository.findAll();
            Assertions.assertThat(runs).hasSize(1);
            var run = runs.get(0);
            Assertions.assertThat(run.streamMovements().count()).isEqualTo(2);
            return null;
        });

        performPutTrip("TMT1", List.of("TMM1", "TMM2", "TMM3"));
        transactionTemplate.execute(tx -> {
            var runs = runRepository.findAll();
            Assertions.assertThat(runs).hasSize(1);
            var run = runs.get(0);
            Assertions.assertThat(run.streamMovements().count()).isEqualTo(3);
            return null;
        });
    }

    private void performPutTrip(String tripExternalId, List<String> movementExternalIds) {
        var tripId = new ResourceId(tripExternalId, null);
        var movementIds = movementExternalIds.stream().map(id -> new ResourceId(id, null)).collect(Collectors.toList());
        putMovementHelper.performPutTrip(PutMovementControllerTestUtil.wrap(
                PutMovementControllerTestUtil.prepareTrip(
                        tripId,
                        PutMovementControllerTestUtil.prepareTripMovements(tripId, movementIds)
                )
        ));
    }

}
