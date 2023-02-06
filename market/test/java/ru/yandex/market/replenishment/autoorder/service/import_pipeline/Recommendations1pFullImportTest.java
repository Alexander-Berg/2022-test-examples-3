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
import ru.yandex.market.replenishment.autoorder.service.RecommendationsMskuInfoLoader;
import ru.yandex.market.replenishment.autoorder.service.RecommendationsTransitsService;
import ru.yandex.market.replenishment.autoorder.service.ReplenishmentLoaderService;
import ru.yandex.market.replenishment.autoorder.service.TimeService;
import ru.yandex.market.replenishment.autoorder.service.XdocScheduleService;
import ru.yandex.market.replenishment.autoorder.service.auto_processing.DemandAutoProcessingService;
import ru.yandex.market.replenishment.autoorder.service.environment.EnvironmentService;
import ru.yandex.market.replenishment.autoorder.service.environment.FeatureFlagService;
import ru.yandex.market.replenishment.autoorder.service.not_grouped_recommendations_processing_service.AuxDataLoadingServiceNew;
import ru.yandex.market.replenishment.autoorder.service.not_grouped_recommendations_processing_service.Recommendations1PGroupingLoader;
import ru.yandex.market.replenishment.autoorder.service.not_grouped_recommendations_processing_service.RecommendationsGroupingService;
import ru.yandex.market.replenishment.autoorder.service.not_grouped_recommendations_processing_service.TmpRecommendation1pProcessingLoader;
import ru.yandex.market.replenishment.autoorder.service.special_order.DemandCreatorBySpecialOrdersService;
import ru.yandex.market.replenishment.autoorder.service.yt.YtTableService;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.CreateRecommendationsByAutoDemandsLoader;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.DailyWarehouseLimitsLoader;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.RecommendationsCheck1PLoader;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.RegionalAssortmentLoader;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.SalesLoader;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.SpecialOrderRecommendationsCreationLoader;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.StockWithLifetime1PLoader;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.Transit1PLoader;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.not_grouped_recommendations.NotGrouped1PLoader;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.recommendation_info.partitioned_by_demand.country_info.RecommendationCountryInfoLoader1p;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.recommendation_info.partitioned_by_demand.region_info.RecommendationRegionInfoLoader1p;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.recommendation_info.partitioned_by_demand.wh_info.RecommendationWarehouseInfoLoader1p;
import ru.yandex.market.replenishment.autoorder.yql_repository.SalesYqlRepository;
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
public class Recommendations1pFullImportTest extends BaseImportPipelineTest {
    private static final int WAREHOUSE_ID = 145;
    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    ObjectFactory<DemandCreatorBySpecialOrdersService> demandCreatorFactory;
    @Autowired
    EnvironmentRepository environmentRepository;
    @Autowired
    AuxDataLoadingServiceNew auxDataLoadingService;
    @Autowired
    FeatureFlagService featureFlagService;
    @Autowired
    ObjectFactory<RecommendationsGroupingService> demandLinkerService;
    @Autowired
    XdocScheduleService xdocScheduleService;
    @Autowired
    QueryService queryService;
    @Qualifier("yqlJdbcTemplate")
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    DemandAutoProcessingService demandAutoProcessingService;
    @Autowired
    RecommendationsCheckService recommendationsCheckService;
    @Autowired
    RecommendationsTransitsService recommendationsTransitsService;
    @Autowired
    AlertsService alertService;
    @Autowired
    SalesYqlRepository salesYqlRepository;
    @Value("${yql.market.db-profile:testing}")
    private String dbProfile;

    @Test
    @DbUnitDataSet(before = "Recommendations1pFullImportTest.testFullImport.before.csv",
        after = "Recommendations1pFullImportTest.testFullImport.after.csv")
    @YqlTest(
        schemasDir = "/yt/schemas",
        schemas = {
            "//home/market/testing/replenishment/autoorder/import_preparation/actual_fit/2020-05-15",
            "//home/market/testing/replenishment/autoorder/import_preparation/forecast_oos/2020-05-15",
            "//home/market/production/mbi/dictionaries/partner_biz_snapshot/latest",
            "//home/market/production/mstat/analyst/regular/cubes_vertica/fact_new_order_item_dict/2020-05",
            "//home/market/production/mstat/analyst/regular/cubes_vertica/fact_new_order_dict",
            "//home/market/production/mstat/analyst/regular/cubes_vertica/fact_new_order_delivery",
            "//home/market/production/mstat/dictionaries/shop_real_supplier/latest",
            "//home/market/production/mstat/analyst/regular/cubes_vertica/fact_stock_flattened/2020-05",
            "//home/market/production/mstat/dictionaries/mbo/sku_transitions/latest",
            "//home/market/production/replenishment/order_planning/2020-04-30/outputs/simulation",
            "//home/market/production/replenishment/order_planning/2020-05-15/intermediate/forecast_region",
            "//home/market/production/replenishment/order_planning/2020-05-15/intermediate/manual_stock_model",
            "//home/market/production/replenishment/order_planning/2020-05-15/intermediate/ss_region_reduced",
            "//home/market/production/replenishment/order_planning/2020-05-15/intermediate/stock_alpaca",
            "//home/market/production/replenishment/order_planning/2020-05-15/intermediate/suppliers",
            "//home/market/production/replenishment/order_planning/2020-05-15/intermediate/suppliers_demand",
            "//home/market/production/replenishment/order_planning/2020-05-15/intermediate/warehouses",
            "//home/market/production/replenishment/order_planning/2020-05-15/outputs/recommendations",
            "//home/market/production/replenishment/order_planning/latest/outputs/transits",
            "//home/market/production/replenishment/order_planning/2020-05-15/intermediate" +
                "/delivery_limit_groups",
            "//home/market/production/replenishment/order_planning/2020-05-15/intermediate/transits_raw",
            "//home/market/production/replenishment/order_planning/2020-05-15/intermediate/stock_with_lifetime",
            "//home/market/production/replenishment/order_planning/2020-05-15/intermediate/msku_info",
            "//home/market/production/monetize/dynamic_pricing/expiring_goods/expiring_prices/2020-05-14T04--colon" +
                "--00--colon--00",
            "//home/market/production/replenishment/order_planning/2020-05-15/intermediate/regional_assortment",
            "//home/market/production/mstat/dictionaries/mbo/sku_transitions/latest"
        },
        csv = "Recommendations1pFullImportTest_import.yql.csv",
        yqlMock = "Recommendations1pFullImportTest.yql.mock"
    )
    public void testFullImport() throws Exception {
        TimeService timeService = mock(TimeService.class);
        EnvironmentRepository environmentRepository = mock(EnvironmentRepository.class);

        LocalDate nowDate = LocalDate.of(2020, 5, 15);
        LocalDateTime nowDateTime = LocalDateTime.of(nowDate, LocalTime.of(0, 0));

        when(timeService.getNowDate()).thenReturn(nowDate);
        when(timeService.getNowDateTime()).thenReturn(nowDateTime);
        setTestTime(nowDateTime);

        runAndRestoreYtWatchLog(() -> runPipeline(timeService, environmentRepository));
        runPipeline(timeService, environmentRepository);

        testEndpointRecommendationsWithCount();
        testEndpointDemands();
    }

    private void runPipeline(TimeService timeService, EnvironmentRepository environmentRepository) {
        final YtTableService ytTableService = Mockito.mock(YtTableService.class);
        when(ytTableService.checkYtTableExists(anyString())).thenReturn(true);
        EnvironmentService environmentService = new EnvironmentService(environmentRepository);

        load(session -> new ReplenishmentLoaderService(environmentService, timeService, session));

        load(session -> new NotGrouped1PLoader(jdbcTemplate, environmentService, queryService, timeService, session));

        load(session -> new RecommendationWarehouseInfoLoader1p(jdbcTemplate, environmentService, queryService,
            timeService, session));

        load(session -> {
                RecommendationRegionInfoLoader1p loader =
                    new RecommendationRegionInfoLoader1p(
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

        load(session -> new RecommendationCountryInfoLoader1p(jdbcTemplate, environmentService, queryService,
            timeService, session));

        load(session -> new SalesLoader(environmentService, timeService, session, salesYqlRepository));

        load(session -> new RecommendationsMskuInfoLoader(jdbcTemplate, queryService, session, timeService));

        load(session -> new RegionalAssortmentLoader(session, jdbcTemplate, queryService, timeService));

        load(session -> new DailyWarehouseLimitsLoader(jdbcTemplate, queryService, timeService, session));

        load(session -> new StockWithLifetime1PLoader(jdbcTemplate, session, queryService, timeService));

        load(session -> new Transit1PLoader(jdbcTemplate, session, queryService, timeService));

        load(session -> new SpecialOrderRecommendationsCreationLoader(session, timeService, demandCreatorFactory,
            xdocScheduleService));

        load(session -> new Recommendations1PGroupingLoader(auxDataLoadingService, demandLinkerService, session,
            timeService, recommendationsTransitsService, new EnvironmentService(environmentRepository)));

        load(session -> new TmpRecommendation1pProcessingLoader(session, timeService,
            new EnvironmentService(environmentRepository), alertService));

        load(session -> new RecommendationsCheck1PLoader(recommendationsCheckService,
            session, timeService));

        load(session -> new CreateRecommendationsByAutoDemandsLoader(session, timeService,
            featureFlagService, demandAutoProcessingService));
    }

    private void testEndpointRecommendationsWithCount() throws Exception {
        final RecommendationFilters recommendationFilters = createFilters(105L);
        mockMvc.perform(post("/api/v2/recommendations/with-count")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(recommendationFilters)))
            .andExpect(jsonPath("$.recommendations.length()").value(1))

            .andExpect(jsonPath("$.recommendations[0].msku").value(234))
            .andExpect(jsonPath("$.recommendations[0].title").value("234"))
            .andExpect(jsonPath("$.recommendations[0].packageNumInSpike").isEmpty())
            .andExpect(jsonPath("$.recommendations[0].groupId").value(0L))
            .andExpect(jsonPath("$.recommendations[0].demandId").value(105L))
            .andExpect(jsonPath("$.recommendations[0].abc").value(ABC.A.toString()))
            .andExpect(jsonPath("$.recommendations[0].warehouseId").value(WAREHOUSE_ID))
            .andExpect(jsonPath("$.recommendations[0].categoryName").isEmpty())
            .andExpect(jsonPath("$.recommendations[0].supplierLifetime").value(80))
            .andExpect(jsonPath("$.recommendations[0].inLifetime").value(90))
            .andExpect(jsonPath("$.recommendations[0].outLifetime").value(100));
    }

    private void testEndpointDemands() throws Exception {
        mockMvc.perform(get("/demands?id=101,102&demandType=TYPE_1P")
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
            .andExpect(jsonPath("$[0].demandType").value("TYPE_1P"))
            .andExpect(jsonPath("$[0].catteamId").value(1))


            .andExpect(jsonPath("$[1].id").value(101))
            .andExpect(jsonPath("$[1].supplier.name").value("Ромашка"))
            .andExpect(jsonPath("$[1].supplier.rsId").value("004000"))
            .andExpect(jsonPath("$[1].warehouse.name").value("Маршрут"))
            .andExpect(jsonPath("$[1].errors").isEmpty())
            .andExpect(jsonPath("$[1].warnings.length()").value(0))
            .andExpect(jsonPath("$[1].minPurchase").value(5))
            .andExpect(jsonPath("$[1].demandType").value("TYPE_1P"))
            .andExpect(jsonPath("$[1].catteamId").value(1));

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
