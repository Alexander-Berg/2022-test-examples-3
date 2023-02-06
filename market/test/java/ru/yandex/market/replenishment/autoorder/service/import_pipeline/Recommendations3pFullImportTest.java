package ru.yandex.market.replenishment.autoorder.service.import_pipeline;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.api.dto.user_fillters.recommendation.RecommendationFilters;
import ru.yandex.market.replenishment.autoorder.model.ABC;
import ru.yandex.market.replenishment.autoorder.model.RecommendationFilter;
import ru.yandex.market.replenishment.autoorder.repository.postgres.EnvironmentRepository;
import ru.yandex.market.replenishment.autoorder.security.WithMockLogin;
import ru.yandex.market.replenishment.autoorder.service.AlertsService;
import ru.yandex.market.replenishment.autoorder.service.RecommendationsCheckService;
import ru.yandex.market.replenishment.autoorder.service.RecommendationsTransitsService;
import ru.yandex.market.replenishment.autoorder.service.Replenishment3pLoaderService;
import ru.yandex.market.replenishment.autoorder.service.TimeService;
import ru.yandex.market.replenishment.autoorder.service.environment.EnvironmentService;
import ru.yandex.market.replenishment.autoorder.service.not_grouped_recommendations_processing_service.AuxDataLoadingServiceNew;
import ru.yandex.market.replenishment.autoorder.service.not_grouped_recommendations_processing_service.Recommendations3PGroupingLoader;
import ru.yandex.market.replenishment.autoorder.service.not_grouped_recommendations_processing_service.Recommendations3PGroupingService;
import ru.yandex.market.replenishment.autoorder.service.not_grouped_recommendations_processing_service.RecommendationsGroupingService;
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
import ru.yandex.market.yql_test.annotation.YqlTest;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.replenishment.autoorder.utils.TestUtils.dtoToString;

@AutoConfigureMockMvc
@WithMockLogin("user1")
public class Recommendations3pFullImportTest extends BaseImportPipelineTest {
    private static final int WAREHOUSE_ID = 145;

    @Value("${yql.market.db-profile:testing}")
    private String dbProfile;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    ObjectFactory<Recommendations3PGroupingService> groupingServiceFactory;

    @Autowired
    EnvironmentRepository environmentRepository;

    @Autowired
    AuxDataLoadingServiceNew auxDataLoadingService;

    @Autowired
    ObjectFactory<RecommendationsGroupingService> demandLinkerService;

    @Autowired
    QueryService queryService;

    @Qualifier("yqlJdbcTemplate")
    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    RecommendationsTransitsService recommendationsTransitsService;

    @Autowired
    AlertsService alertService;

    @Autowired
    RecommendationsCheckService recommendationsCheckService;

    @Test
    @DbUnitDataSet(before = "Recommendations3pFullImportTest.testFullImport.before.csv",
        after = "Recommendations3pFullImportTest.testFullImport.after.csv")
    @YqlTest(
        schemasDir = "/yt/schemas",
        schemas = {
            "//home/market/testing/replenishment/autoorder/import_preparation/actual_fit/2020-05-15",
            "//home/market/testing/replenishment/autoorder/import_preparation/forecast_oos/2020-05-15",
            "//home/market/production/mbi/dictionaries/partner_biz_snapshot/latest",
            "//home/market/production/mstat/analyst/regular/cubes_vertica/cube_order_item_dict/2020-05",
            "//home/market/production/mstat/analyst/regular/cubes_vertica/fact_stock_flattened/2020-05",
            "//home/market/production/mstat/dictionaries/mbo/sku_transitions/latest",
            "//home/market/production/replenishment/order_planning/2020-04-30/outputs/simulation",
            "//home/market/production/replenishment/order_planning/2020-05-15/intermediate/forecast_region",
            "//home/market/production/replenishment/order_planning/2020-05-15/intermediate/manual_stock_model",
            "//home/market/production/replenishment/order_planning/2020-05-15/intermediate/ss_region_reduced",
            "//home/market/production/replenishment/order_planning/2020-05-15/intermediate/stock_alpaca",
            "//home/market/production/replenishment/order_planning/2020-05-15/intermediate/suppliers",
            "//home/market/production/replenishment/order_planning/2020-05-15/intermediate/warehouses",
            "//home/market/production/replenishment/order_planning/2020-05-15/outputs/recommendations",
            "//home/market/production/replenishment/order_planning/2020-05-15/intermediate/transits_raw",
            "//home/market/production/replenishment/order_planning/2020-05-15/intermediate/stock_with_lifetime",
            "//home/market/production/replenishment/order_planning/2020-05-15/intermediate/suppliers_demand",
            "//home/market/production/replenishment/order_planning/2020-05-15/outputs/transits",
            "//home/market/production/replenishment/order_planning/2020-05-14/intermediate/suppliers_demand",
        },
        csv = "Recommendations3pFullImportTest_import.yql.csv",
        yqlMock = "Recommendations3pFullImportTest.yql.mock"
    )
    public void testFullImport() throws Exception {
        TimeService timeService = mock(TimeService.class);
        EnvironmentRepository environmentRepository = mock(EnvironmentRepository.class);

        LocalDate nowDate = LocalDate.of(2020, 5, 15);
        LocalDateTime nowDateTime = LocalDateTime.of(nowDate, LocalTime.of(0, 0));

        when(timeService.getNowDate()).thenReturn(nowDate);
        when(timeService.getNowDateTime()).thenReturn(nowDateTime);

        runAndRestoreYtWatchLog(() -> runPipeline(timeService, environmentRepository));
        runPipeline(timeService, environmentRepository);

        testEndpointRecommendationsWithCount();
        testEndpointDemands();
    }

    private void runPipeline(TimeService timeService, EnvironmentRepository environmentRepository) {
        final YtTableService ytTableService = Mockito.mock(YtTableService.class);
        when(ytTableService.checkYtTableExists(anyString())).thenReturn(true);
        EnvironmentService environmentService = new EnvironmentService(environmentRepository);

        load(session -> new Replenishment3pLoaderService(environmentService, timeService, session));

        load(session -> new NotGrouped3PLoader(jdbcTemplate, environmentService, queryService, timeService, session));

        load(session -> new RecommendationWarehouseInfoLoader3p(jdbcTemplate, environmentService, queryService,
            timeService, session));

        load(session -> {
                RecommendationRegionInfoLoader3p loader =
                    new RecommendationRegionInfoLoader3p(
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

        load(session -> new RecommendationRegionSupplierInfoLoader3p(jdbcTemplate, environmentService, queryService,
            timeService, session));

        load(session -> new StockWithLifetime3PLoader(jdbcTemplate, session, queryService, timeService));

        load(session -> new Transit3PLoader(jdbcTemplate, session, queryService, timeService));

        load(session -> new Recommendations3PGroupingLoader(auxDataLoadingService, groupingServiceFactory,
            demandLinkerService, session, timeService, recommendationsTransitsService,
            new EnvironmentService(environmentRepository)));

        load(session -> new TmpRecommendation3pProcessingLoader(session, timeService, alertService, environmentService));

        load(session -> new RecommendationsCheck3PLoader(recommendationsCheckService, session, timeService));
    }

    private void testEndpointRecommendationsWithCount() throws Exception {
        final RecommendationFilters recommendationFilters = createFilters(105L);
        mockMvc.perform(post("/api/v2/recommendations/with-count?demandType=TYPE_3P")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(recommendationFilters)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.recommendations.length()").value(1))

            .andExpect(jsonPath("$.recommendations[0].msku").value(234))
            .andExpect(jsonPath("$.recommendations[0].title").value("234"))
            .andExpect(jsonPath("$.recommendations[0].packageNumInSpike").isEmpty())
            .andExpect(jsonPath("$.recommendations[0].groupId").value(0L))
            .andExpect(jsonPath("$.recommendations[0].demandId").value(105L))
            .andExpect(jsonPath("$.recommendations[0].abc").value(ABC.A.toString()))
            .andExpect(jsonPath("$.recommendations[0].warehouseId").value(WAREHOUSE_ID))
            .andExpect(jsonPath("$.recommendations[0].categoryName").isEmpty());
    }

    private void testEndpointDemands() throws Exception {
        mockMvc.perform(get("/demands?demandType=TYPE_3P&id=101,102")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(102))
            .andExpect(jsonPath("$[0].supplier.name").value("Ромашка"))
            .andExpect(jsonPath("$[0].supplier.rsId").value("004000"))
            .andExpect(jsonPath("$[0].warehouse.name").value("Маршрут"))
            .andExpect(jsonPath("$[0].errors").isEmpty())
            .andExpect(jsonPath("$[0].warnings.length()").value(0))
            .andExpect(jsonPath("$[0].minPurchase").value(5))
            .andExpect(jsonPath("$[0].demandType").value("TYPE_3P"))

            .andExpect(jsonPath("$[1].id").value(101))
            .andExpect(jsonPath("$[1].supplier.name").value("Ромашка"))
            .andExpect(jsonPath("$[1].supplier.rsId").value("004000"))
            .andExpect(jsonPath("$[1].warehouse.name").value("Маршрут"))
            .andExpect(jsonPath("$[1].errors").isEmpty())
            .andExpect(jsonPath("$[1].warnings.length()").value(0))
            .andExpect(jsonPath("$[1].minPurchase").value(5))
            .andExpect(jsonPath("$[1].demandType").value("TYPE_3P"));
    }

    @NotNull
    private RecommendationFilters createFilters(Long... demandIds) {
        final RecommendationFilters recommendationFilters = new RecommendationFilters();
        final RecommendationFilter recommendationFilter = new RecommendationFilter();
        recommendationFilter.setDemandIds(Arrays.asList(demandIds));
        recommendationFilters.setFilter(recommendationFilter);
        return recommendationFilters;
    }
}
