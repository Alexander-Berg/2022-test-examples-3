package ru.yandex.market.replenishment.autoorder.service.recommendations_grouping;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.stream.StreamSupport;

import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.model.DemandType;
import ru.yandex.market.replenishment.autoorder.model.RecommendationNew;
import ru.yandex.market.replenishment.autoorder.model.dto.DemandDTO;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.NotGroupedRecommendation;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.RecommendationRegionInfo;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.RecommendationWarehouseInfo;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.SpecialOrderItem;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.StocksWithLifetimes;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.WarehouseRegion;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.YtTableWatchLog;
import ru.yandex.market.replenishment.autoorder.repository.postgres.DemandDTORepository;
import ru.yandex.market.replenishment.autoorder.repository.postgres.RecommendationRegionInfoRepository;
import ru.yandex.market.replenishment.autoorder.repository.postgres.RecommendationRepository;
import ru.yandex.market.replenishment.autoorder.repository.postgres.RecommendationWarehouseInfoRepository;
import ru.yandex.market.replenishment.autoorder.repository.postgres.SalesRepository;
import ru.yandex.market.replenishment.autoorder.repository.postgres.SpecialOrderItemRepository;
import ru.yandex.market.replenishment.autoorder.repository.postgres.StocksWithLifetimesRepository;
import ru.yandex.market.replenishment.autoorder.repository.postgres.TmpTablesSupportRepository;
import ru.yandex.market.replenishment.autoorder.repository.postgres.YtTableWatchLogRepository;
import ru.yandex.market.replenishment.autoorder.service.TimeService;
import ru.yandex.market.replenishment.autoorder.service.environment.EnvironmentService;
import ru.yandex.market.replenishment.autoorder.service.not_grouped_recommendations_processing_service.AbstractGroupingLoader;
import ru.yandex.market.replenishment.autoorder.utils.TestUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static ru.yandex.market.replenishment.autoorder.repository.postgres.util.DbTableNames.RECOMMENDATIONS_REGION_SUPPLIER_INFO_TABLE_NAME;
import static ru.yandex.market.replenishment.autoorder.repository.postgres.util.DbTableNames.STOCKS_WITH_LIFETIMES_TABLE_NAME;
import static ru.yandex.market.replenishment.autoorder.service.environment.EnvironmentConstants.USE_DENORMALIZED_INFOS_ENABLED;
import static ru.yandex.market.replenishment.autoorder.utils.DemandTypePartitionedLoadable.NOT_GROUPED_RECOMMENDATIONS;
import static ru.yandex.market.replenishment.autoorder.utils.DemandTypePartitionedLoadable.RECOMMENDATION_COUNTRY_INFOS;
import static ru.yandex.market.replenishment.autoorder.utils.DemandTypePartitionedLoadable.RECOMMENDATION_REGION_INFOS;
import static ru.yandex.market.replenishment.autoorder.utils.DemandTypePartitionedLoadable.RECOMMENDATION_WAREHOUSE_INFOS;
import static ru.yandex.market.replenishment.autoorder.utils.DemandTypePartitionedTable.DEMANDS;
import static ru.yandex.market.replenishment.autoorder.utils.DemandTypePartitionedTable.RECOMMENDATIONS;
import static ru.yandex.market.replenishment.autoorder.utils.DemandTypePartitionedTable.RECOMMENDATIONS_COUNTRY_INFOS;
import static ru.yandex.market.replenishment.autoorder.utils.DemandTypePartitionedTable.RECOMMENDATIONS_REGION_INFOS;
import static ru.yandex.market.replenishment.autoorder.utils.DemandTypePartitionedTable.RECOMMENDATIONS_WH_INFOS;

@TestExecutionListeners(value = {
        BaseGroupingTest.TmpTablesCreation.class
})
public abstract class BaseGroupingTest extends FunctionalTest {

    public static final int ROSTOV = 147;
    public static final int TOMILINO = 171;
    public static final int SOFYINO = 172;
    public static final long XDOC = 47723;
    protected static final LocalDate NOW_DATE = LocalDate.of(2020, 10, 5);
    protected static final LocalDateTime NOW_DATE_TIME = LocalDateTime.of(NOW_DATE, LocalTime.of(0, 0));
    @Autowired
    protected YtTableWatchLogRepository ytTableWatchLogRepository;
    @Autowired
    protected RecommendationRepository recommendationRepository;
    @Autowired
    protected RecommendationWarehouseInfoRepository recommendationWarehouseInfoRepository;
    @Autowired
    protected RecommendationRegionInfoRepository recommendationRegionInfoRepository;
    @Autowired
    protected SpecialOrderItemRepository specialOrderItemRepository;
    @Autowired
    protected SqlSessionFactory sqlSessionFactory;
    @Autowired
    protected SalesRepository salesRepository;
    @Autowired
    protected TransactionTemplate transactionTemplate;
    @Autowired
    protected StocksWithLifetimesRepository stocksWithLifetimesRepository;
    @Autowired
    protected TimeService timeService;
    @Autowired
    protected EnvironmentService environmentService;

    @Before
    public void mockTimeService() {
        TestUtils.mockTimeService(timeService, NOW_DATE_TIME);
    }

    protected abstract AbstractGroupingLoader getLoader();

    protected void createNotGrouped(int shipmentQuantum, long supplier, int msku, int wh, Long whFrom, long responsible,
                                    LocalDate orderDate) {
        createNotGrouped(shipmentQuantum, supplier, msku, wh, whFrom, responsible, orderDate, null, null);
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    protected void createNotGrouped(
            int shipmentQuantum,
            long supplier,
            int msku,
            long wh,
            Long whFrom,
            long responsible,
            LocalDate orderDate,
            Long specialOrderItem,
            LocalDate weekStartDate
    ) {
        final String ssku = supplier + "." + msku;
        final long warehouseRegionId = wh == ROSTOV ?
            WarehouseRegion.Companion.getRostovId() : WarehouseRegion.Companion.getMoscowId();

        NotGroupedRecommendation notGrouped = new NotGroupedRecommendation();
        notGrouped.setMsku(msku);
        notGrouped.setSsku(ssku);
        notGrouped.setShipmentQuantum(shipmentQuantum);
        notGrouped.setWarehouseId(wh);
        notGrouped.setUserId(responsible);
        notGrouped.setPurchQty(1);
        notGrouped.setMinShipment(1);
        notGrouped.setDeliveryDate(orderDate);
        notGrouped.setOrderDate(orderDate);
        notGrouped.setSupplierId(supplier);
        notGrouped.setSpecialOrderItemId(specialOrderItem);
        notGrouped.setWarehouseIdFrom(whFrom);
        notGrouped.setSupplyRoute(whFrom == null ? "direct" : "xdoc");
        notGrouped.setDemandType(getDemandType());
        recommendationRepository.saveNotGroupedRecommendationNew(notGrouped);

        RecommendationWarehouseInfo whInfo = new RecommendationWarehouseInfo();
        whInfo.setMsku(msku);
        whInfo.setWarehouseId(wh);
        whInfo.setStock(10L);
        whInfo.setTransit(10);
        whInfo.setDemandType(getDemandType());
        recommendationWarehouseInfoRepository.savePartitionedByDemandType(whInfo,
                RECOMMENDATIONS_WH_INFOS.partitionTmp(getDemandType()));

        RecommendationRegionInfo regionInfo = new RecommendationRegionInfo();
        regionInfo.setMsku(msku);
        regionInfo.setRegionId(warehouseRegionId);
        regionInfo.setMissedOrders28d(0.0);
        regionInfo.setMissedOrders56d(0.0);
        regionInfo.setOosDays(0L);
        regionInfo.setDemandType(getDemandType());
        recommendationRegionInfoRepository.savePartitionedByDemandType(
                regionInfo,
                RECOMMENDATIONS_REGION_INFOS.partitionTmp(getDemandType())
        );

        StocksWithLifetimes stocksWithLifetimes = new StocksWithLifetimes();
        stocksWithLifetimes.setMsku((long) msku);
        stocksWithLifetimes.setWarehouseRegionId(warehouseRegionId);
        stocksWithLifetimes.setLifetimesList(new Integer[]{65535});
        stocksWithLifetimes.setStocksList(new Long[]{10L});
        stocksWithLifetimesRepository.save(stocksWithLifetimes, STOCKS_WITH_LIFETIMES_TABLE_NAME);

        if (weekStartDate != null) {
            final SpecialOrderItem orderItem = new SpecialOrderItem();
            orderItem.setId(specialOrderItem);
            orderItem.setSpecialOrderId(1L);
            orderItem.setWeekStartDate(weekStartDate);
            orderItem.setQuantity(10);
            specialOrderItemRepository.saveSpecialOrderItem(orderItem);
        }
    }

    protected void test(boolean sameDemands, boolean sameGroups) {
        createEvents();

        getLoader().load();
        RecommendationNew repl1 = find(1);
        RecommendationNew repl2 = find(2);

        assertEquals(sameDemands, repl1.getDemandId() == repl2.getDemandId());

        DemandDTO demand = getSingleDemandById(repl1.getDemandId());
        assertNotNull(demand);
        Long group1 = demand.getLinkGroup();

        demand = getSingleDemandById(repl2.getDemandId());
        assertNotNull(demand);
        Long group2 = demand.getLinkGroup();

        if (sameGroups) {
            assertNotNull(group1);
            assertNotNull(group2);
            assertEquals(group1, group2);
        } else if (group1 != null || group2 != null) {
            assertNotEquals(group1, group2);
        }
    }

    protected void createEvents() {
        createEvent(NOT_GROUPED_RECOMMENDATIONS.getOutEvents(getDemandType())[0]);
        createEvent(RECOMMENDATION_COUNTRY_INFOS.getOutEvents(getDemandType())[0]);
        createEvent(RECOMMENDATION_REGION_INFOS.getOutEvents(getDemandType())[0]);
        createEvent(RECOMMENDATION_WAREHOUSE_INFOS.getOutEvents(getDemandType())[0]);
    }

    protected DemandDTO getSingleDemandById(long id) {
        DemandDTO demand;
        try (final SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.SIMPLE)) {
            final DemandDTORepository mapper = sqlSession.getMapper(DemandDTORepository.class);
            demand = mapper.getDemandByIdFromTmp(id, getDemandType());
        }

        return demand;
    }

    protected RecommendationNew find(int shipmentQuantum) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            final RecommendationRepository recommendationRepository = session.getMapper(RecommendationRepository.class);
            final boolean useDenormalized = environmentService.getBooleanWithDefault(USE_DENORMALIZED_INFOS_ENABLED, false);
            return StreamSupport.stream(recommendationRepository.getAllFomTmp(getDemandType(), useDenormalized).spliterator(), false)
                    .filter(r -> r.getShipmentQuantum() == shipmentQuantum)
                    .findAny()
                    .orElseThrow(() -> new RuntimeException("recommendation doesn't exist, shipmentQuantum: " + shipmentQuantum));
        }
    }

    protected void createEvent(String notGroupedRecommendationsLoad) {
        final YtTableWatchLog ytTableWatchLog = new YtTableWatchLog();
        ytTableWatchLog.setImported(false);
        ytTableWatchLog.setCluster("hahn");
        ytTableWatchLog.setTablePath("//home/market/production/replenishment/order_planning/2020-05-15/outputs" +
                "/recommendations");
        ytTableWatchLog.setTimeProcessed(NOW_DATE_TIME);
        ytTableWatchLog.setEventsGroup(notGroupedRecommendationsLoad);
        ytTableWatchLogRepository.save(ytTableWatchLog);
    }

    protected abstract DemandType getDemandType();

    public static class TmpTablesCreation extends AbstractTestExecutionListener {

        @Override
        public void beforeTestClass(TestContext testContext) {
            SqlSessionFactory factory = testContext.getApplicationContext().getBean("sqlSessionFactory", SqlSessionFactory.class);
            try (SqlSession session = factory.openSession()) {
                TmpTablesSupportRepository mapper = session.getMapper(TmpTablesSupportRepository.class);
                for (DemandType demandType : DemandType.values()) {
                    mapper.createTmpTable(RECOMMENDATIONS.partition(demandType));
                    mapper.createTmpTable(DEMANDS.partition(demandType));
                    mapper.createTmpTable(RECOMMENDATIONS_WH_INFOS.partition(demandType));
                    mapper.createTmpTable(RECOMMENDATIONS_REGION_INFOS.partition(demandType));
                    mapper.createTmpTable(RECOMMENDATIONS_COUNTRY_INFOS.partition(demandType));
                    if (demandType == DemandType.TYPE_3P) {
                        mapper.createTmpTable(RECOMMENDATIONS_REGION_SUPPLIER_INFO_TABLE_NAME);
                    }
                    mapper.createTmpTable(STOCKS_WITH_LIFETIMES_TABLE_NAME);
                }
                session.commit();
            }
        }
    }

}
