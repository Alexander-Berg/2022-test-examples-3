package ru.yandex.market.replenishment.autoorder.service.import_pipeline;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.repository.postgres.EnvironmentRepository;
import ru.yandex.market.replenishment.autoorder.service.AlertsService;
import ru.yandex.market.replenishment.autoorder.service.RecommendationsTransitsService;
import ru.yandex.market.replenishment.autoorder.service.ReplenishmentTenderLoaderService;
import ru.yandex.market.replenishment.autoorder.service.TimeService;
import ru.yandex.market.replenishment.autoorder.service.environment.EnvironmentService;
import ru.yandex.market.replenishment.autoorder.service.not_grouped_recommendations_processing_service.RecommendationsTenderGroupingLoader;
import ru.yandex.market.replenishment.autoorder.service.not_grouped_recommendations_processing_service.TmpRecommendationTenderProcessingLoader;
import ru.yandex.market.replenishment.autoorder.service.yt.YtTableService;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.not_grouped_recommendations.NotGroupedTenderLoader;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.recommendation_info.partitioned_by_demand.country_info.RecommendationCountryInfoLoaderTender;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.recommendation_info.partitioned_by_demand.region_info.RecommendationRegionInfoLoaderTender;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.recommendation_info.partitioned_by_demand.wh_info.RecommendationWarehouseInfoLoaderTender;
import ru.yandex.market.yql_query_service.service.QueryService;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RecommendationsTenderImportTest extends BaseImportPipelineTest {

    @Autowired
    RecommendationsTransitsService recommendationsTransitsService;

    @Autowired
    AlertsService alertService;

    @Autowired
    TimeService timeService;

    @Autowired
    EnvironmentService environmentService;

    @Value("${yql.market.db-profile:testing}")
    private String dbProfile;

    @Test
    @DbUnitDataSet(before = "RecommendationsTenderImportTest.testImport.before.csv",
            after = "RecommendationsTenderImportTest.testImport.after.csv")
    public void testImport() {
        EnvironmentRepository environmentRepository = mock(EnvironmentRepository.class);
        EnvironmentService environmentService = new EnvironmentService(environmentRepository);

        final YtTableService ytTableService = Mockito.mock(YtTableService.class);
        when(ytTableService.checkYtTableExists(anyString())).thenReturn(true);

        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        QueryService queryService = mock(QueryService.class);

        LocalDate nowDate = LocalDate.of(2020, 10, 5);
        LocalDateTime nowDateTime = LocalDateTime.of(nowDate, LocalTime.of(0, 0));

        setTestTime(nowDateTime);

        load(session -> new ReplenishmentTenderLoaderService(environmentService, timeService, session));

        load(session -> new NotGroupedTenderLoader(jdbcTemplate, environmentService, queryService, timeService,
                session));

        load(session -> new RecommendationWarehouseInfoLoaderTender(jdbcTemplate, environmentService, queryService,
                timeService, session));

        load(session -> {
                    RecommendationRegionInfoLoaderTender loader =
                            new RecommendationRegionInfoLoaderTender(
                                    jdbcTemplate,
                                    environmentService,
                                    queryService,
                                    timeService,
                                    session,
                                    ytTableService);
                    ReflectionTestUtils.setField(loader, "dbProfile", dbProfile);
                    return loader;
                }
        );

        load(session -> new RecommendationCountryInfoLoaderTender(jdbcTemplate, environmentService, queryService,
                timeService, session));

        load(session -> new RecommendationsTenderGroupingLoader(null, null, session,
                timeService, recommendationsTransitsService, new EnvironmentService(environmentRepository)) {
            @Override
            protected void processNotGroupedRecommendations() {
                //
            }
        });

        load(session -> new TmpRecommendationTenderProcessingLoader(session, timeService, alertService, environmentService));
    }

}
