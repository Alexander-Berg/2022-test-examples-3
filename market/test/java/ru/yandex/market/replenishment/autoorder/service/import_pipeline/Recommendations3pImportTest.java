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
import ru.yandex.market.replenishment.autoorder.service.RecommendationsCheckService;
import ru.yandex.market.replenishment.autoorder.service.RecommendationsTransitsService;
import ru.yandex.market.replenishment.autoorder.service.Replenishment3pLoaderService;
import ru.yandex.market.replenishment.autoorder.service.environment.EnvironmentService;
import ru.yandex.market.replenishment.autoorder.service.not_grouped_recommendations_processing_service.Recommendations3PGroupingLoader;
import ru.yandex.market.replenishment.autoorder.service.not_grouped_recommendations_processing_service.TmpRecommendation3pProcessingLoader;
import ru.yandex.market.replenishment.autoorder.service.yt.YtTableService;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.RecommendationsCheck3PLoader;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.StockWithLifetime3PLoader;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.Transit3PLoader;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.not_grouped_recommendations.NotGrouped3PLoader;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.recommendation_info.partitioned_by_demand.country_info.RecommendationCountryInfoLoader3p;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.recommendation_info.partitioned_by_demand.region_info.RecommendationRegionInfoLoader3p;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.recommendation_info.partitioned_by_demand.region_info.RecommendationRegionSupplierInfoLoader3p;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.recommendation_info.partitioned_by_demand.wh_info.RecommendationWarehouseInfoLoader3p;
import ru.yandex.market.yql_query_service.service.QueryService;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
public class Recommendations3pImportTest extends BaseImportPipelineTest {

    @Autowired
    RecommendationsTransitsService recommendationsTransitsService;

    @Autowired
    RecommendationsCheckService recommendationsCheckService;

    @Autowired
    AlertsService alertService;

    @Value("${yql.market.db-profile:testing}")
    private String dbProfile;

    @Test
    @DbUnitDataSet(before = "Recommendations3pImportTest.testImport.before.csv",
            after = "Recommendations3pImportTest.testImport.after.csv")
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

        when(queryService.getQuery(anyString(), anyMap())).thenReturn("Fake query");

        load(session -> new Replenishment3pLoaderService(environmentService, timeService, session));

        load(session -> new RecommendationRegionSupplierInfoLoader3p(jdbcTemplate, environmentService, queryService,
                timeService, session));

        load(session -> new NotGrouped3PLoader(jdbcTemplate, environmentService, queryService, timeService, session));

        load(session -> new RecommendationWarehouseInfoLoader3p(jdbcTemplate, environmentService, queryService,
                timeService, session));

        load(session -> {
                    RecommendationRegionInfoLoader3p loader
                            = new RecommendationRegionInfoLoader3p(
                            jdbcTemplate,
                            environmentService,
                            queryService,
                            timeService,
                            session,
                            ytTableService
                    );
                    ReflectionTestUtils.setField(loader, "dbProfile", dbProfile);
                    return loader;
                }
        );


        load(session -> new RecommendationCountryInfoLoader3p(jdbcTemplate, environmentService, queryService,
                timeService, session));

        load(session -> new StockWithLifetime3PLoader(jdbcTemplate, session, queryService, timeService));

        load(session -> new Transit3PLoader(jdbcTemplate, session, queryService, timeService));

        load(session -> new Recommendations3PGroupingLoader(null, null, null, session,
                timeService, recommendationsTransitsService, new EnvironmentService(environmentRepository)) {
            @Override
            protected void processNotGroupedRecommendations() {
                //
            }
        });

        load(session -> new TmpRecommendation3pProcessingLoader(session, timeService, alertService, environmentService));

        load(session -> new RecommendationsCheck3PLoader(recommendationsCheckService, session, timeService));
    }

}
