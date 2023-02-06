package ru.yandex.market.gutgin.tms.service.datacamp.scheduling;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import Market.DataCamp.DataCampContentMarketParameterValue.MarketParameterValue;
import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferMapping;
import Market.DataCamp.DataCampOfferMapping.Mapping.MarketSkuType;
import Market.DataCamp.DataCampOfferMarketContent.MarketParameterValues;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.gutgin.tms.config.TestServiceConfig;
import ru.yandex.market.gutgin.tms.service.SskuLockService;
import ru.yandex.market.partner.content.common.DBDcpStateGenerator;
import ru.yandex.market.partner.content.common.db.dao.PipelineService;
import ru.yandex.market.partner.content.common.db.dao.SskuLockDao;
import ru.yandex.market.partner.content.common.db.dao.dcp.BusinessToLockInfoDao;
import ru.yandex.market.partner.content.common.db.dao.dcp.DatacampOfferDao;
import ru.yandex.market.partner.content.common.db.jooq.enums.DatacampOfferStatus;
import ru.yandex.market.partner.content.common.db.jooq.enums.GcSkuTicketType;
import ru.yandex.market.partner.content.common.db.jooq.enums.MrgrienPipelineStatus;
import ru.yandex.market.partner.content.common.db.jooq.enums.PipelineType;
import ru.yandex.market.partner.content.common.db.jooq.enums.SskuLockStatus;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.BusinessToLockInfo;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.DataBucket;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.DatacampOffer;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuTicket;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.Pipeline;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.SskuLock;
import ru.yandex.market.partner.content.common.engine.parameter.ProcessDataBucketData;
import ru.yandex.market.partner.content.common.partner.content.SourceController;

import static Market.DataCamp.DataCampOfferMapping.Mapping.MarketSkuType.MARKET_SKU_TYPE_FAST;
import static Market.DataCamp.DataCampOfferMapping.Mapping.MarketSkuType.MARKET_SKU_TYPE_MSKU;
import static Market.DataCamp.DataCampOfferMapping.Mapping.MarketSkuType.MARKET_SKU_TYPE_PSKU;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.gutgin.tms.service.datacamp.scheduling.OfferProcessingStrategy.Priority.DEFAULT;
import static ru.yandex.market.gutgin.tms.service.datacamp.scheduling.OfferProcessingStrategy.Priority.HIGH;
import static ru.yandex.market.gutgin.tms.service.datacamp.scheduling.OfferProcessingStrategy.Priority.LOW;

@ContextConfiguration(
        classes = {TestServiceConfig.class}
)
public class DatacampPipelineSchedulerServiceTest extends DBDcpStateGenerator {
    public static final int CATEGORY_ID1 = 914910;
    public static final int CATEGORY_ID2 = 914921;
    public static final String DCP_OFFER_ID1 = "DCP1";
    public static final String DCP_OFFER_ID2 = "DCP2";
    public static final String DCP_OFFER_ID3 = "DCP3";
    public static final String DCP_OFFER_ID4 = "DCP4";
    public static final String DCP_OFFER_ID5 = "DCP5";
    public static final String DCP_OFFER_ID6 = "DCP6";
    public static final String DCP_OFFER_ID7 = "DCP7";
    public static final String FAST_OFFER_ID1 = "FAST1";
    public static final String FAST_OFFER_ID2 = "FAST2";
    private static final int LOWER_PIPELINE_BATCH_SIZE = 5;
    private static final int LOWER_BUSINESS_LIMIT = 10;
    private static final Long SKU_ID = 1L;
    private static final Long SKU_ID2 = 2L;
    private static final Long SKU_ID3 = 3L;
    private static final Long SKU_ID4 = 4L;
    private static final Long SKU_ID5 = 5L;
    private static final int GROUP_ID = 1;

    @Autowired
    DatacampOfferDao datacampOfferDao;
    @Autowired
    PipelineService pipelineService;
    @Autowired
    SskuLockService sskuLockService;
    @Autowired
    SskuLockDao sskuLockDao;
    @Autowired
    SourceController sourceController;
    @Autowired
    BusinessToLockInfoDao businessToLockInfoDao;
    @Autowired
    OfferInfoBatchProducer offerInfoBatchProducer;
    @Autowired
    List<OfferProcessingStrategy> offerProcessingStrategies;

    List<ProcessDataBucketData> processDataBucketDataList;
    List<Long> pipelineIds;
    private int partnerShopId;
    private DatacampPipelineSchedulerService datacampPipelineSchedulerService;
    private DatacampPipelineSchedulerService datacampPipelineSchedulerServiceWithLowerLimit;

    @Before
    @Override
    public void setUp() {
        super.setUp();
        createSource(
                DatacampOfferForPipelineInformationBuilder.DEFAULT_SOURCE_ID,
                DatacampOfferForPipelineInformationBuilder.DEFAULT_BUSINESS_ID
        );
        partnerShopId = DatacampOfferForPipelineInformationBuilder.DEFAULT_BUSINESS_ID;
        PipelineService spyPipelineService = spy(pipelineService);
        datacampPipelineSchedulerService = new DatacampPipelineSchedulerService(
                datacampOfferDao,
                spyPipelineService,
                dataBucketDao,
                gcSkuTicketDao,
                sskuLockService,
                sourceController,
                businessToLockInfoDao,
                offerInfoBatchProducer
        );

        datacampPipelineSchedulerServiceWithLowerLimit = new DatacampPipelineSchedulerService(
                datacampOfferDao,
                spyPipelineService,
                dataBucketDao,
                gcSkuTicketDao,
                sskuLockService,
                sourceController,
                businessToLockInfoDao,
                new OfferInfoBatchProducer(LOWER_PIPELINE_BATCH_SIZE, LOWER_BUSINESS_LIMIT, offerProcessingStrategies)
        );

        businessToLockInfoDao.insert(new BusinessToLockInfo(
                partnerShopId, null, 0, 0
        ));
        businessToLockInfoDao.insert(new BusinessToLockInfo(
                11062549, null, 0, 0
        ));

        processDataBucketDataList = new ArrayList<>();
        pipelineIds = new ArrayList<>();
        doAnswer(invocation -> {
            processDataBucketDataList.add(invocation.getArgument(0));
            long pipelineId = pipelineService.createPipeline(
                    invocation.getArgument(0),
                    invocation.getArgument(1),
                    invocation.getArgument(2),
                    (Integer) invocation.getArgument(3),
                    invocation.getArgument(4)
            );
            pipelineIds.add(pipelineId);
            return pipelineId;
        }).when(spyPipelineService).createPipeline(
                any(ProcessDataBucketData.class),
                any(PipelineType.class),
                any(Integer.class),
                any(Integer.class),
                any(Integer.class)
        );
    }

    @Test
    public void whenOffersInTwoCategoryThenTwoDataBuckets() {
        DatacampOffer datacampOffer1 = newOffer().withOfferId(DCP_OFFER_ID1)
                .withGroupId(0)
                .withCategoryId(CATEGORY_ID1)
                .build();
        DatacampOffer datacampOffer2 = newOffer().withOfferId(DCP_OFFER_ID2)
                .withGroupId(1)
                .withCategoryId(CATEGORY_ID2)
                .build();

        datacampOfferDao.insert(datacampOffer1, datacampOffer2);
        datacampPipelineSchedulerService.schedulePipelines(partnerShopId, Optional.empty());

        assertThat(processDataBucketDataList).hasSize(2);
        List<DataBucket> dataBuckets = dataBucketDao.fetchById(
                processDataBucketDataList.stream()
                        .map(ProcessDataBucketData::getDataBucketId)
                        .toArray(Long[]::new)
        );

        Optional<DataBucket> firstDataBucket = dataBuckets.stream()
                .filter(dataBucket -> dataBucket.getCategoryId() == CATEGORY_ID1)
                .findFirst();

        assertThat(firstDataBucket).isPresent();

        List<GcSkuTicket> gcSkuTickets = gcSkuTicketDao.getTicketsByDataBucket(firstDataBucket.get().getId());

        assertThat(gcSkuTickets).hasSize(1);

        GcSkuTicket gcSkuTicket = gcSkuTickets.get(0);

        assertThat(gcSkuTicket.getCategoryId()).isEqualTo(CATEGORY_ID1);
        assertThat(gcSkuTicket.getPartnerShopId()).isEqualTo(partnerShopId);
        assertThat(gcSkuTicket.getShopSku()).isEqualTo(DCP_OFFER_ID1);

        List<SskuLock> sskuLocks = sskuLockDao.fetchByGcSkuTicket(gcSkuTicket.getId());

        assertThat(sskuLocks).hasSize(1);

        SskuLock sskuLock = sskuLocks.get(0);
        assertThat(sskuLock.getShopSku()).isEqualTo(DCP_OFFER_ID1);
        assertThat(sskuLock.getSupplierId()).isEqualTo(partnerShopId);
        assertThat(sskuLock.getStatus()).isEqualTo(SskuLockStatus.LOCKED);

        List<DatacampOffer> datacampOffers = datacampOfferDao.fetchById(datacampOffer1.getId(), datacampOffer2.getId());

        assertThat(datacampOffers)
                .hasSize(2)
                .extracting(DatacampOffer::getStatus)
                .containsOnly(DatacampOfferStatus.ACTIVATED);

        List<Pipeline> pipelines = getPipelines();

        assertThat(pipelines)
                .hasSize(2)
                .allMatch(pipeline -> pipeline.getStatus() == MrgrienPipelineStatus.NEW)
                .allMatch(pipeline -> pipeline.getType() == PipelineType.CSKU);
    }

    @Test
    public void whenOfferWithoutSourceIdThenPass() {
        datacampOfferDao.insert(
                newOffer()
                        .withOfferId(DCP_OFFER_ID1)
                        .withSourceId(null)
                        .withGroupId(0)
                        .build()
        );

        List<DatacampOfferDao.OfferInfo> list = datacampOfferDao.getNewOffersInfo(partnerShopId);

        assertThat(list).hasSize(0);
    }


    @Test
    @Ignore
    public void whenOlderIsDeduplicatedThenCsku() {
        Timestamp ts5 = Timestamp.from(Instant.now().minus(5, ChronoUnit.HOURS));
        datacampOfferDao.insert(
                newOffer()
                        .withOfferId(DCP_OFFER_ID1)
                        .withBusinessId(partnerShopId)
                        .withTimestamp(Timestamp.from(Instant.now()))
                        .withDeduplicated(false)
                        .build()
        );
        datacampOfferDao.insert(
                newOffer()
                        .withOfferId(DCP_OFFER_ID1)
                        .withBusinessId(partnerShopId)
                        .withTimestamp(ts5)
                        .withDeduplicated(true)
                        .withGroupId(0)
                        .build()
        );
        List<DatacampOfferDao.OfferInfo> list = datacampOfferDao.getNewOffersInfo(partnerShopId);

        assertThat(list).hasSize(1);
        assertThat(list.stream().map(DatacampOfferDao.OfferInfo::isCsku).collect(Collectors.toSet()))
                .containsExactlyInAnyOrder(true);
    }

    @Test
    public void whenOfferWithoutSourceIdThenSetSourceIdAndProceed() {
        datacampOfferDao.insert(
                newOffer()
                        .withOfferId(DCP_OFFER_ID1)
                        .withGroupId(0)
                        .withCategoryId(CATEGORY_ID1)
                        .withSourceId(null)
                        .build()
        );

        datacampPipelineSchedulerService.schedulePipelines(partnerShopId, Optional.empty());

        assertThat(processDataBucketDataList).hasSize(1);
        assertThat(pipelineIds).hasSize(1);
    }

    @Test
    public void whenOffersNoCategoryThenThrowException() {
        datacampOfferDao.insert(
                newOffer()
                        .withOfferId(DCP_OFFER_ID1)
                        .build()
        );

        DatacampPipelineSchedulerService spyDatacampPipelineSchedulerService = spy(datacampPipelineSchedulerService);
        spyDatacampPipelineSchedulerService.schedulePipelines(partnerShopId, Optional.empty());

        verify(spyDatacampPipelineSchedulerService, times(1)).processExceptedOffers(any());
    }

    @Test
    public void whenExistsLockThenNotFoundOffersForScheduling() {
        datacampOfferDao.insert(
                newOffer()
                        .withOfferId(DCP_OFFER_ID1)
                        .withGroupId(0)
                        .withCategoryId(CATEGORY_ID1)
                        .build()
        );
        datacampPipelineSchedulerService.schedulePipelines(partnerShopId, Optional.empty());
        assertThat(processDataBucketDataList).hasSize(1);

        datacampOfferDao.insert(
                newOffer()
                        .withOfferId(DCP_OFFER_ID1)
                        .withGroupId(1)
                        .withCategoryId(CATEGORY_ID1)
                        .build(),
                newOffer()
                        .withOfferId(DCP_OFFER_ID2)
                        .withGroupId(1)
                        .withCategoryId(CATEGORY_ID1)
                        .build()
        );
        datacampPipelineSchedulerService.schedulePipelines(partnerShopId, Optional.empty());
        assertThat(processDataBucketDataList).hasSize(1);

        long dataBucketId = processDataBucketDataList.get(0).getDataBucketId();
        List<GcSkuTicket> tickets = gcSkuTicketDao.getTicketsByDataBucket(dataBucketId);
        assertThat(tickets).hasSize(1);

        sskuLockService.unlock(tickets);
        datacampPipelineSchedulerService.schedulePipelines(partnerShopId, Optional.empty());
        assertThat(processDataBucketDataList).hasSize(2);

        long secondDataBucketId = processDataBucketDataList.get(1).getDataBucketId();
        List<GcSkuTicket> lastTickets = gcSkuTicketDao.getTicketsByDataBucket(secondDataBucketId);
        assertThat(lastTickets).hasSize(2);
    }

    @Test
    public void whenManySameOffersThenScheduleFreshestAndSkipOther() {
        Instant now = Instant.now();
        List<DatacampOffer> offers = IntStream.range(0, 10)
                .mapToObj(t -> now.minus(t, ChronoUnit.HOURS))
                .map(i -> newOffer()
                        .withOfferId(DCP_OFFER_ID1)
                        .withGroupId(0)
                        .withCategoryId(CATEGORY_ID1)
                        .withTimestamp(Timestamp.from(i))
                        .build()
                )
                .collect(Collectors.toList());

        DatacampOffer newest = offers.get(0);

        datacampOfferDao.insert(offers);
        datacampPipelineSchedulerService.schedulePipelines(partnerShopId, Optional.empty());

        assertThat(processDataBucketDataList).hasSize(1);

        long dataBucketId = processDataBucketDataList.get(0).getDataBucketId();
        List<GcSkuTicket> gcSkuTickets = gcSkuTicketDao.getTicketsByDataBucket(dataBucketId);

        assertThat(gcSkuTickets).hasSize(1);
        assertThat(gcSkuTickets.get(0).getDatacampOfferId()).isEqualTo(newest.getId());
        assertThat(datacampOfferDao.findAll())
                .filteredOn(o -> !o.getId().equals(newest.getId()))
                .extracting(DatacampOffer::getStatus)
                .containsOnly(DatacampOfferStatus.SKIPPED)
                .hasSize(offers.size() - 1);

    }

    @Test
    public void createdSinglePipelineForAddAndEditOffers() {
        datacampOfferDao.insert(
                newOffer()
                        .withOfferId(DCP_OFFER_ID1)
                        .withGroupId(0)
                        .withCategoryId(CATEGORY_ID1)
                        .build(),
                newOffer()
                        .withOfferId(DCP_OFFER_ID2)
                        .withGroupId(0)
                        .withCategoryId(CATEGORY_ID1)
                        .build()
        );

        Long marketSkuId = 123L;
        datacampOfferDao.insert(
                newOffer()
                        .withOfferId(DCP_OFFER_ID3)
                        .withGroupId(0)
                        .withCategoryId(CATEGORY_ID1)
                        .withMapping(marketSkuId, MarketSkuType.MARKET_SKU_TYPE_PSKU)
                        .build(),
                newOffer()
                        .withOfferId(DCP_OFFER_ID4)
                        .withGroupId(0)
                        .withCategoryId(CATEGORY_ID1)
                        .withMapping(marketSkuId, MarketSkuType.MARKET_SKU_TYPE_PSKU)
                        .build()
        );

        datacampPipelineSchedulerService.schedulePipelines(partnerShopId, Optional.empty());
        List<Pipeline> pipelines = getPipelines();

        assertThat(pipelines).hasSize(1)
                .allMatch(pipeline -> pipeline.getStatus() == MrgrienPipelineStatus.NEW)
                .allMatch(pipeline -> pipeline.getType() == PipelineType.CSKU);

        assertThat(pipelines.get(0).getPriority()).isEqualTo(DEFAULT.getValue());
        assertThat(pipelines.get(0).getTicketsCount()).isEqualTo(4);
    }

    @Test
    public void createPipelinesOnlyForCurrentBusinessId() {
        datacampOfferDao.insert(
                newOffer()
                        .withOfferId(DCP_OFFER_ID3)
                        .withGroupId(0)
                        .withCategoryId(CATEGORY_ID1)
                        .build()
        );

        Integer missBusinessId = 1051;
        datacampOfferDao.insert(
                newOffer()
                        .withOfferId(DCP_OFFER_ID2)
                        .withGroupId(0)
                        .withCategoryId(CATEGORY_ID2)
                        .withBusinessId(missBusinessId)
                        .build()
        );

        datacampPipelineSchedulerService.schedulePipelines(partnerShopId, Optional.empty());

        assertThat(getPipelines())
                .hasSize(1)
                .allMatch(pipeline -> pipeline.getStatus() == MrgrienPipelineStatus.NEW)
                .allMatch(pipeline -> pipeline.getType() == PipelineType.CSKU);

        List<DatacampOffer> offerList = datacampOfferDao.findAll();

        assertThat(offerList.stream()
                .filter(datacampOffer -> datacampOffer.getBusinessId().equals(missBusinessId)
                        && datacampOffer.getStatus().equals(DatacampOfferStatus.NEW))
                .collect(Collectors.toList())
        ).hasSize(1);

        assertThat(offerList.stream()
                .filter(datacampOffer -> datacampOffer.getBusinessId().equals(partnerShopId)
                        && !datacampOffer.getStatus().equals(DatacampOfferStatus.NEW))
                .collect(Collectors.toList())
        ).hasSize(1);

    }

    @Test
    public void checkFastOffersProcessedByFastWhenModification() {
        datacampOfferDao.insert(
                newOffer()
                        .withOfferId(FAST_OFFER_ID2)
                        .withCategoryId(CATEGORY_ID1)
                        .withMapping(7357L, MARKET_SKU_TYPE_FAST)
                        .withAllowFastSkuCreation(true)
                        .build()
        );

        datacampPipelineSchedulerService.schedulePipelines(partnerShopId, Optional.empty());
        List<Pipeline> pipelines = getPipelines();

        assertThat(pipelines).hasSize(1)
                .allMatch(pipeline -> pipeline.getType() == PipelineType.FAST_CARD);

        assertThat(pipelines).extracting(Pipeline::getPriority)
                .containsOnly(DEFAULT.getValue());
    }

    @Test
    public void checkFastOfferProcessedByFastPipe() {
        datacampOfferDao.insert(
                newOffer()
                        .withOfferId(FAST_OFFER_ID1)
                        .withCategoryId(CATEGORY_ID1)
                        .build(),
                newOffer()
                        .withOfferId(FAST_OFFER_ID2)
                        .withCategoryId(CATEGORY_ID1)
                        .withMapping(7357L, MARKET_SKU_TYPE_FAST)
                        .build()
        );

        datacampPipelineSchedulerService.schedulePipelines(partnerShopId, Optional.empty());
        List<Pipeline> pipelines = getPipelines();

        assertThat(pipelines).hasSize(2)
                .allMatch(pipeline -> pipeline.getType() == PipelineType.FAST_CARD);

        assertThat(pipelines).extracting(Pipeline::getPriority)
                .containsOnly(HIGH.getValue(), DEFAULT.getValue());

    }

    @Test
    public void checkCskuCreatePipelinesCreated() {
        datacampOfferDao.insert(
                newOffer()
                        .withOfferId(DCP_OFFER_ID1)
                        .withCategoryId(CATEGORY_ID1)
                        .withGroupId(0)
                        .build(),
                newOffer()
                        .withOfferId(DCP_OFFER_ID2)
                        .withCategoryId(CATEGORY_ID1)
                        .withRandomParameters()
                        .build(),
                newOffer()
                        .withOfferId(DCP_OFFER_ID3)
                        .withCategoryId(CATEGORY_ID1)
                        .withGroupId(0)
                        .withRandomParameters()
                        .build()
        );

        datacampPipelineSchedulerService.schedulePipelines(partnerShopId, Optional.empty());
        List<Pipeline> pipelines = getPipelines();

        assertThat(pipelines).hasSize(1)
                .allMatch(pipeline -> pipeline.getType() == PipelineType.CSKU)
                .allMatch(pipeline -> pipeline.getPriority() == DEFAULT.getValue());
        assertThat(pipelines.get(0).getTicketsCount()).isEqualTo(3);
    }

    @Test
    public void checkCskuIsNotCreatedFromFCWhenItsNotAllowedByMBOC() {
        datacampOfferDao.insert(
                newOffer()
                        .withOfferId(DCP_OFFER_ID1)
                        .withCategoryId(CATEGORY_ID1)
                        .withMapping(7357L, MARKET_SKU_TYPE_FAST)
                        .withRandomParameters()
                        .withGroupId(12345)
                        .withAllowModelCreateUpdate(false)
                        .withAllowFastSkuCreation(true)
                        .build()
        );

        datacampPipelineSchedulerService.schedulePipelines(partnerShopId, Optional.empty());
        List<Pipeline> pipelines = getPipelines();

        assertThat(pipelines).hasSize(1)
                .allMatch(pipeline -> pipeline.getType() == PipelineType.FAST_CARD);

        assertThat(pipelines).extracting(Pipeline::getPriority)
                .containsOnly(DEFAULT.getValue());
    }

    @Test
    public void checkСskuIsCreatedFromFCWhenItsAllowedByMBOC() {
        datacampOfferDao.insert(
                newOffer()
                        .withOfferId(DCP_OFFER_ID1)
                        .withCategoryId(CATEGORY_ID1)
                        .withMapping(7357L, MARKET_SKU_TYPE_FAST)
                        .withRandomParameters()
                        .withGroupId(12345)
                        .withAllowModelCreateUpdate(true)
                        .build()
        );

        datacampPipelineSchedulerService.schedulePipelines(partnerShopId, Optional.empty());
        List<Pipeline> pipelines = getPipelines();

        assertThat(pipelines).hasSize(1)
                .allMatch(pipeline -> pipeline.getType() == PipelineType.CSKU);

        assertThat(pipelines).extracting(Pipeline::getPriority)
                .containsOnly(LOW.getValue());
    }

    @Test
    public void checkFastWhenPskuIsNotAllowed() {
        datacampOfferDao.insert(
                newOffer()
                        .withOfferId(DCP_OFFER_ID1)
                        .withCategoryId(CATEGORY_ID1)
                        .withMapping(7357L, MARKET_SKU_TYPE_FAST)
                        .withRandomParameters()
                        .withGroupId(12345)
                        .withAllowModelCreateUpdate(false)
                        .withAllowFastSkuCreation(true)
                        .build()
        );

        datacampPipelineSchedulerService.schedulePipelines(partnerShopId, Optional.empty());
        List<Pipeline> pipelines = getPipelines();

        assertThat(pipelines).hasSize(1)
                .allMatch(pipeline -> pipeline.getType() == PipelineType.FAST_CARD);

        assertThat(pipelines).extracting(Pipeline::getPriority)
                .containsOnly(DEFAULT.getValue());
    }

    @Test
    public void checkCskuEditPipelinesCreatedWhenOfferMappedToFastCard() {
        Long fastMarketSkuId = 7357L;
        datacampOfferDao.insert(
                newOffer()
                        .withOfferId(DCP_OFFER_ID1)
                        .withCategoryId(CATEGORY_ID1)
                        .withGroupId(0)
                        .withMapping(fastMarketSkuId, MARKET_SKU_TYPE_FAST)
                        .build(),
                newOffer()
                        .withOfferId(DCP_OFFER_ID2)
                        .withCategoryId(CATEGORY_ID1)
                        .withRandomParameters()
                        .withMapping(fastMarketSkuId, MARKET_SKU_TYPE_FAST)
                        .build(),
                newOffer()
                        .withOfferId(DCP_OFFER_ID3)
                        .withCategoryId(CATEGORY_ID1)
                        .withGroupId(0)
                        .withRandomParameters()
                        .withMapping(fastMarketSkuId, MARKET_SKU_TYPE_FAST)
                        .build()
        );

        datacampPipelineSchedulerService.schedulePipelines(partnerShopId, Optional.empty());
        List<Pipeline> pipelines = getPipelines();

        assertThat(pipelines).hasSize(1)
                .allMatch(pipeline -> pipeline.getType() == PipelineType.CSKU)
                .allMatch(pipeline -> pipeline.getPriority() == LOW.getValue());
    }

    @Test
    public void checkDatacampEditPipelinesWithLimitCreatedWhenOfferMappedToFastCard() {
        Long fastMarketSkuId = 7357L;
        for (int i = 0; i < LOWER_PIPELINE_BATCH_SIZE; i++) {
            datacampOfferDao.insert(
                    newOffer()
                            .withOfferId(DCP_OFFER_ID2 + i)
                            .withCategoryId(CATEGORY_ID1)
                            .withRandomParameters()
                            .withMapping(fastMarketSkuId + i, MARKET_SKU_TYPE_FAST)
                            .build()
            );
        }
        datacampOfferDao.insert(
                newOffer()
                        .withOfferId(DCP_OFFER_ID1)
                        .withCategoryId(CATEGORY_ID1)
                        .withGroupId(0)
                        .withMapping(fastMarketSkuId + LOWER_PIPELINE_BATCH_SIZE, MARKET_SKU_TYPE_FAST)
                        .build(),
                newOffer()
                        .withOfferId(DCP_OFFER_ID3)
                        .withCategoryId(CATEGORY_ID1)
                        .withGroupId(0)
                        .withRandomParameters()
                        .withMapping(fastMarketSkuId + LOWER_PIPELINE_BATCH_SIZE + 1, MARKET_SKU_TYPE_FAST)
                        .build()
        );

        datacampPipelineSchedulerServiceWithLowerLimit.schedulePipelines(partnerShopId, Optional.empty());
        List<Pipeline> pipelines = getPipelines();

        assertThat(pipelines).hasSize(2)
                .allMatch(pipeline -> pipeline.getType() == PipelineType.CSKU)
                .allMatch(pipeline -> pipeline.getPriority() == LOW.getValue());
    }

    @Test
    public void checkCskuEditPipelinesCreatedWhenOfferMappedToPSKU() {
        Long marketSkuId = 7357L;
        datacampOfferDao.insert(
                newOffer()
                        .withOfferId(DCP_OFFER_ID1)
                        .withCategoryId(CATEGORY_ID1)
                        .withMapping(marketSkuId, MARKET_SKU_TYPE_PSKU)
                        .build(),
                newOffer()
                        .withOfferId(DCP_OFFER_ID2)
                        .withCategoryId(CATEGORY_ID2)
                        .withGroupId(0)
                        .withMapping(SKU_ID, MARKET_SKU_TYPE_PSKU)
                        .build(),
                newOffer()
                        .withOfferId(DCP_OFFER_ID3)
                        .withCategoryId(CATEGORY_ID2)
                        .withRandomParameters()
                        .withMapping(SKU_ID2, MARKET_SKU_TYPE_PSKU)
                        .build(),
                newOffer()
                        .withOfferId(DCP_OFFER_ID4)
                        .withCategoryId(CATEGORY_ID2)
                        .withGroupId(0)
                        .withRandomParameters()
                        .withMapping(SKU_ID3, MARKET_SKU_TYPE_PSKU)
                        .build()
        );

        datacampPipelineSchedulerService.schedulePipelines(partnerShopId, Optional.empty());
        List<Pipeline> pipelines = getPipelines();

        assertThat(pipelines).hasSize(2)
                .allMatch(pipeline -> pipeline.getType() == PipelineType.CSKU)
                .allMatch(pipeline -> pipeline.getPriority() == LOW.getValue());
    }

    @Test
    public void createPipelinesWithCustomPriority() {
        datacampOfferDao.insert(
                newOffer()
                        .withOfferId(DCP_OFFER_ID3)
                        .withGroupId(0)
                        .withCategoryId(CATEGORY_ID1)
                        .build()
        );

        datacampPipelineSchedulerService.schedulePipelines(partnerShopId, Optional.of(100));
        List<Pipeline> pipelines = getPipelines();
        assertThat(pipelines).hasSize(1).allMatch(p -> p.getPriority() == 100);
    }

    @Test
    public void createPipelinesWithTicketsCount() {
        datacampOfferDao.insert(
                newOffer()
                        .withOfferId(DCP_OFFER_ID1)
                        .withGroupId(0)
                        .withCategoryId(CATEGORY_ID1)
                        .build()
        );

        datacampPipelineSchedulerService.schedulePipelines(partnerShopId, Optional.empty());
        List<Pipeline> pipelines = getPipelines();
        assertThat(pipelines).hasSize(1).allMatch(p -> p.getTicketsCount() == 1);
    }

    @SuppressWarnings("deprecated")
    @Test
    public void testWhenNewOfferAndAllowFastCardAndPskuNotAllowedThenFC() {
        datacampOfferDao.insert(
                newOffer()
                        .withOfferId(DCP_OFFER_ID1)
                        .withCategoryId(Math.toIntExact(
                                Categories.DISABLED_CATEGORIES_FOR_FAST_CARD.stream()
                                        .findFirst()
                                        .orElse(0L)
                        ))
                        .withAllowModelCreateUpdate(false)
                        .withAllowFastSkuCreation(true)
                        .build()
        );

        datacampPipelineSchedulerService.schedulePipelines(partnerShopId, Optional.empty());
        List<Pipeline> pipelines = getPipelines();

        assertThat(pipelines).hasSize(1)
                .allMatch(pipeline -> pipeline.getType() == PipelineType.FAST_CARD)
                .allMatch(pipeline -> pipeline.getPriority() == HIGH.getValue());
    }

    @Test
    public void testWhenNewOfferAndFCCategoryFallbackAndPskuNotAllowedThenFC() {
        datacampOfferDao.insert(
                newOffer()
                        .withOfferId(DCP_OFFER_ID1)
                        .withCategoryId(CATEGORY_ID1)
                        .withAllowModelCreateUpdate(false)
                        .build()
        );

        datacampPipelineSchedulerService.schedulePipelines(partnerShopId, Optional.empty());
        List<Pipeline> pipelines = getPipelines();

        assertThat(pipelines).hasSize(1)
                .allMatch(pipeline -> pipeline.getType() == PipelineType.FAST_CARD)
                .allMatch(pipeline -> pipeline.getPriority() == HIGH.getValue());
    }

    @Test
    public void testWhenNewOfferAndNotFCCategoryAndPskuNotAllowedThenExceptOffer() {
        datacampOfferDao.insert(
                newOffer()
                        .withOfferId(DCP_OFFER_ID1)
                        .withCategoryId(0)
                        .withAllowModelCreateUpdate(false)
                        .withAllowFastSkuCreation(false)
                        .build()
        );

        datacampPipelineSchedulerService.schedulePipelines(partnerShopId, Optional.empty());
        List<Pipeline> pipelines = getPipelines();

        assertThat(pipelines).isEmpty();
        assertThat(datacampOfferDao.fetchByOfferId(DCP_OFFER_ID1).get(0).getStatus())
                .isEqualTo(DatacampOfferStatus.EXCEPTED);
    }

    @Test
    public void shouldTakeOnlyBusinessLimitedOffersCount() {
        int delta = 0;
        int count = 3 * LOWER_BUSINESS_LIMIT;
        for (int i = 0; i < count; i++) {
            datacampOfferDao.insert(
                    newOffer()
                            .withOfferId(DCP_OFFER_ID1 + delta++)
                            .withCategoryId(CATEGORY_ID1)
                            .withBusinessId(partnerShopId)
                            .build()
            );
        }
        businessToLockInfoDao.insertMissedBusinessToLockInfoDao();

        datacampPipelineSchedulerServiceWithLowerLimit.schedulePipelines(partnerShopId, Optional.empty());
        List<Pipeline> pipelines = getPipelines();

        assertThat(pipelines).hasSize(LOWER_BUSINESS_LIMIT / LOWER_PIPELINE_BATCH_SIZE);
        List<DatacampOffer> offerList = datacampOfferDao.findAll();

        assertThat(offerList)
                .filteredOn(offer -> offer.getStatus() == DatacampOfferStatus.ACTIVATED)
                .hasSize(LOWER_BUSINESS_LIMIT);
    }

    @Test
    public void shouldNotClearBatchWhenAllOffersWithGroups() {
        int delta = 0;
        int count = LOWER_PIPELINE_BATCH_SIZE + 1;
        for (int i = 0; i < count; i++) {
            datacampOfferDao.insert(
                    newOffer()
                            .withOfferId(DCP_OFFER_ID1 + delta++)
                            .withCategoryId(CATEGORY_ID1)
                            .withBusinessId(partnerShopId)
                            .withGroupId(delta)
                            .build()
            );
        }
        businessToLockInfoDao.insertMissedBusinessToLockInfoDao();

        datacampPipelineSchedulerServiceWithLowerLimit.schedulePipelines(partnerShopId, Optional.empty());
        List<Pipeline> pipelines = getPipelines();

        assertThat(pipelines).hasSize(2);
        List<DatacampOffer> offerList = datacampOfferDao.findAll();

        assertThat(offerList)
                .filteredOn(offer -> offer.getStatus() == DatacampOfferStatus.ACTIVATED)
                .hasSize(count);
    }

    @Test
    public void whenAlreadyHasOffersInWorkShouldNotSchedule() {
        int delta = 0;
        int count = 3 * LOWER_BUSINESS_LIMIT;
        for (int i = 0; i < count; i++) {
            datacampOfferDao.insert(
                    newOffer()
                            .withOfferId(DCP_OFFER_ID1 + delta++)
                            .withCategoryId(CATEGORY_ID1)
                            .withBusinessId(partnerShopId)
                            .build()
            );
        }
        businessToLockInfoDao.insertMissedBusinessToLockInfoDao();
        BusinessToLockInfo info = businessToLockInfoDao.findById(partnerShopId);
        info.setInWorkOffersCount(LOWER_BUSINESS_LIMIT);
        businessToLockInfoDao.update(info);

        datacampPipelineSchedulerServiceWithLowerLimit.schedulePipelines(partnerShopId, Optional.empty());
        List<Pipeline> pipelines = getPipelines();

        assertThat(pipelines).hasSize(0);
        List<DatacampOffer> offerList = datacampOfferDao.findAll();

        assertThat(offerList)
                .filteredOn(offer -> offer.getStatus() == DatacampOfferStatus.ACTIVATED)
                .isEmpty();
    }

    @Test
    public void whenMultipleStrategiesCheckCreateCskuPipelinesCreated() {
        int businessId = 11062549;
        datacampOfferDao.insert(
                newOffer()
                        .withOfferId(DCP_OFFER_ID1)
                        .withCategoryId(CATEGORY_ID1)
                        .withBusinessId(businessId)
                        .withGroupId(0)
                        .build(),
                newOffer()
                        .withOfferId(DCP_OFFER_ID2)
                        .withCategoryId(CATEGORY_ID1)
                        .withBusinessId(businessId)
                        .withRandomParameters()
                        .build(),
                newOffer()
                        .withOfferId(DCP_OFFER_ID3)
                        .withCategoryId(CATEGORY_ID1)
                        .withBusinessId(businessId)
                        .withGroupId(0)
                        .withRandomParameters()
                        .build()
        );

        datacampPipelineSchedulerService.schedulePipelines(businessId, Optional.empty());
        List<Pipeline> pipelines = getPipelines();

        assertThat(pipelines).hasSize(1)
                .allMatch(pipeline -> pipeline.getType() == PipelineType.CSKU)
                .allMatch(pipeline -> pipeline.getPriority() == DEFAULT.getValue());
    }

    @Test
    public void whenMultipleStrategiesWithLimitCheckMultipleCskuPipelinesCreated() {
        int businessId = 11062549;
        Long fastMarketSkuId = 7357L;
        for (int i = 0; i < LOWER_PIPELINE_BATCH_SIZE; i++) {
            datacampOfferDao.insert(
                    newOffer()
                            .withOfferId(DCP_OFFER_ID2 + i)
                            .withCategoryId(CATEGORY_ID1)
                            .withBusinessId(businessId)
                            .withRandomParameters()
                            .withMapping(fastMarketSkuId, MARKET_SKU_TYPE_FAST)
                            .build()
            );
        }
        datacampOfferDao.insert(
                newOffer()
                        .withOfferId(DCP_OFFER_ID1)
                        .withCategoryId(CATEGORY_ID1)
                        .withBusinessId(businessId)
                        .withGroupId(0)
                        .build(),
                newOffer()
                        .withOfferId(DCP_OFFER_ID3)
                        .withCategoryId(CATEGORY_ID1)
                        .withBusinessId(businessId)
                        .withGroupId(0)
                        .withRandomParameters()
                        .build()
        );

        datacampPipelineSchedulerServiceWithLowerLimit.schedulePipelines(businessId, Optional.empty());
        List<Pipeline> pipelines = getPipelines();

        assertThat(pipelines).hasSize(2)
                .allMatch(pipeline -> pipeline.getType() == PipelineType.CSKU)
                .anyMatch(pipeline -> pipeline.getPriority() == DEFAULT.getValue())
                .anyMatch(pipeline -> pipeline.getPriority() == LOW.getValue());
    }

    @Test
    public void whenTestingCheckEditCskuPipelinesCreated() {
        Long fastMarketSkuId = 7357L;
        int businessId = 11062549;
        datacampOfferDao.insert(
                newOffer()
                        .withOfferId(DCP_OFFER_ID1)
                        .withCategoryId(CATEGORY_ID1)
                        .withGroupId(0)
                        .withBusinessId(businessId)
                        .withMapping(fastMarketSkuId, MARKET_SKU_TYPE_FAST)
                        .build(),
                newOffer()
                        .withOfferId(DCP_OFFER_ID2)
                        .withCategoryId(CATEGORY_ID1)
                        .withBusinessId(businessId)
                        .withMapping(fastMarketSkuId, MARKET_SKU_TYPE_FAST)
                        .withRandomParameters()
                        .build(),
                newOffer()
                        .withOfferId(DCP_OFFER_ID3)
                        .withCategoryId(CATEGORY_ID1)
                        .withGroupId(0)
                        .withBusinessId(businessId)
                        .withMapping(fastMarketSkuId, MARKET_SKU_TYPE_FAST)
                        .withRandomParameters()
                        .build());

        datacampPipelineSchedulerService.schedulePipelines(businessId, Optional.empty());
        List<Pipeline> pipelines = getPipelines();

        assertThat(pipelines).hasSize(1)
                .allMatch(pipeline -> pipeline.getType() == PipelineType.CSKU)
                .allMatch(pipeline -> pipeline.getPriority() == LOW.getValue());
    }

    @Test
    public void whenOffersWithSameSkuIdAndWithoutGroupThenPostponeProcessing() {
        //Не обрабатываем изначально, повторяет скю группы
        DatacampOffer singleOfferWithSku = newOffer().withOfferId(DCP_OFFER_ID1)
                .withCategoryId(CATEGORY_ID1)
                .withMapping(SKU_ID, MARKET_SKU_TYPE_PSKU)
                .build();
        DatacampOffer groupedOfferWithSku = newOffer().withOfferId(DCP_OFFER_ID2)
                .withGroupId(1)
                .withCategoryId(CATEGORY_ID1)
                .withMapping(SKU_ID, MARKET_SKU_TYPE_PSKU)
                .build();
        DatacampOffer groupedOfferWithSku2 = newOffer().withOfferId(DCP_OFFER_ID3)
                .withGroupId(1)
                .withCategoryId(CATEGORY_ID1)
                .withMapping(SKU_ID2, MARKET_SKU_TYPE_PSKU)
                .build();
        DatacampOffer groupedOfferWithoutSku = newOffer().withOfferId(DCP_OFFER_ID4)
                .withGroupId(1)
                .withCategoryId(CATEGORY_ID1)
                .build();
        //Не обрабатываем изначально, повторяет скю группы
        DatacampOffer singleOfferWithSku2 = newOffer().withOfferId(DCP_OFFER_ID5)
                .withCategoryId(CATEGORY_ID1)
                .withMapping(SKU_ID, MARKET_SKU_TYPE_PSKU)
                .build();
        DatacampOffer singleOfferWithNewSku = newOffer().withOfferId(DCP_OFFER_ID6)
                .withCategoryId(CATEGORY_ID1)
                .withMapping(SKU_ID3, MARKET_SKU_TYPE_PSKU)
                .build();
        DatacampOffer singleOfferWithoutSku = newOffer().withOfferId(DCP_OFFER_ID7)
                .withCategoryId(CATEGORY_ID1)
                .build();

        datacampOfferDao.insert(singleOfferWithSku, groupedOfferWithSku, groupedOfferWithSku2,
                groupedOfferWithoutSku, singleOfferWithSku2,
                singleOfferWithNewSku, singleOfferWithoutSku);
        datacampPipelineSchedulerService.schedulePipelines(partnerShopId, Optional.empty());

        assertThat(processDataBucketDataList).hasSize(3);
        List<DataBucket> dataBuckets = dataBucketDao.fetchById(
                processDataBucketDataList.stream()
                        .map(ProcessDataBucketData::getDataBucketId)
                        .toArray(Long[]::new)
        );

        List<Long> dataBucketIds = dataBuckets
                .stream()
                .map(DataBucket::getId)
                .collect(Collectors.toList());

        List<GcSkuTicket> gcSkuTickets = gcSkuTicketDao.getTicketsByDataBucket(dataBucketIds.get(0));
        gcSkuTickets.addAll(gcSkuTicketDao.getTicketsByDataBucket(dataBucketIds.get(1)));
        gcSkuTickets.addAll(gcSkuTicketDao.getTicketsByDataBucket(dataBucketIds.get(2)));

        assertThat(gcSkuTickets).hasSize(5);
        List<Long> existingPskuIds = gcSkuTickets.stream()
                .map(GcSkuTicket::getExistingMboPskuId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        assertThat(existingPskuIds).hasSize(3);

        List<Pipeline> pipelines = getPipelines();

        assertThat(pipelines)
                .hasSize(3)
                .allMatch(pipeline -> pipeline.getStatus() == MrgrienPipelineStatus.NEW);
    }

    @Test
    public void whenOfferWithMskuMappingUseSpecificTypeAndChangeGroupId() {
        //group1
        DatacampOffer offer1 = newOffer()
                .withGroupId(GROUP_ID)
                .withOfferId(DCP_OFFER_ID1)
                .withCategoryId(CATEGORY_ID1)
                .withMapping(SKU_ID, MARKET_SKU_TYPE_PSKU)
                .build();
        DatacampOffer offer2 = newOffer()
                .withOfferId(DCP_OFFER_ID2)
                .withGroupId(GROUP_ID)
                .withCategoryId(CATEGORY_ID1)
                .withMapping(SKU_ID2, MARKET_SKU_TYPE_MSKU)
                .build();
        DatacampOffer offer3 = newOffer()
                .withOfferId(DCP_OFFER_ID3)
                .withGroupId(GROUP_ID)
                .withCategoryId(CATEGORY_ID1)
                .withMapping(SKU_ID3, MARKET_SKU_TYPE_MSKU)
                .build();
        //no group
        DatacampOffer offer4 = newOffer()
                .withOfferId(DCP_OFFER_ID4)
                .withCategoryId(CATEGORY_ID1)
                .withMapping(SKU_ID4, MARKET_SKU_TYPE_MSKU)
                .build();
        DatacampOffer offer5 = newOffer()
                .withOfferId(DCP_OFFER_ID5)
                .withCategoryId(CATEGORY_ID1)
                .withMapping(SKU_ID5, MARKET_SKU_TYPE_PSKU)
                .build();

        datacampOfferDao.insert(offer1, offer2, offer3, offer4, offer5);

        datacampPipelineSchedulerService.schedulePipelines(partnerShopId, Optional.empty());

        List<Pipeline> pipelines = getPipelines();

        assertThat(pipelines).hasSize(1)
                .allMatch(pipeline -> pipeline.getType() == PipelineType.CSKU)
                .allMatch(pipeline -> pipeline.getPriority() == LOW.getValue());

        assertThat(processDataBucketDataList).hasSize(1);

        List<GcSkuTicket> gcSkuTickets = gcSkuTicketDao.getTicketsByDataBucket(processDataBucketDataList.get(0).getDataBucketId());

        assertThat(findBySsku(DCP_OFFER_ID1, gcSkuTickets)).extracting(GcSkuTicket::getType)
                .isEqualTo(GcSkuTicketType.CSKU);
        assertThat(findBySsku(DCP_OFFER_ID1, gcSkuTickets)).extracting(GcSkuTicket::getDcpGroupId)
                .isEqualTo(GROUP_ID);

        assertThat(findBySsku(DCP_OFFER_ID2, gcSkuTickets)).extracting(GcSkuTicket::getType)
                .isEqualTo(GcSkuTicketType.CSKU_MSKU);
        assertThat(findBySsku(DCP_OFFER_ID2, gcSkuTickets)).extracting(GcSkuTicket::getDcpGroupId)
                .isEqualTo(-GROUP_ID);
        assertThat(findBySsku(DCP_OFFER_ID3, gcSkuTickets)).extracting(GcSkuTicket::getType)
                .isEqualTo(GcSkuTicketType.CSKU_MSKU);
        assertThat(findBySsku(DCP_OFFER_ID3, gcSkuTickets)).extracting(GcSkuTicket::getDcpGroupId)
                .isEqualTo(-GROUP_ID);

        assertThat(findBySsku(DCP_OFFER_ID4, gcSkuTickets)).extracting(GcSkuTicket::getType)
                .isEqualTo(GcSkuTicketType.CSKU_MSKU);
        assertThat(findBySsku(DCP_OFFER_ID4, gcSkuTickets)).extracting(GcSkuTicket::getDcpGroupId)
                .isNull();

        assertThat(findBySsku(DCP_OFFER_ID5, gcSkuTickets)).extracting(GcSkuTicket::getType)
                .isEqualTo(GcSkuTicketType.CSKU);
        assertThat(findBySsku(DCP_OFFER_ID5, gcSkuTickets)).extracting(GcSkuTicket::getDcpGroupId)
                .isNull();

    }

    private GcSkuTicket findBySsku(String shopSku, List<GcSkuTicket> gcSkuTickets) {
        return gcSkuTickets.stream()
                .filter(ticket -> ticket.getShopSku().equals(shopSku))
                .findFirst()
                .orElseThrow();
    }

    @NotNull
    private List<Pipeline> getPipelines() {
        return pipelineIds.stream()
                .map(pipelineService::getPipeline)
                .collect(Collectors.toList());
    }

    private DatacampOfferForPipelineInformationBuilder newOffer() {
        return DatacampOfferForPipelineInformationBuilder.builder();
    }

    private static class DatacampOfferForPipelineInformationBuilder {

        public static Integer DEFAULT_SOURCE_ID = 7357;
        public static Integer DEFAULT_BUSINESS_ID = 7357;

        private String offerId;
        private Integer sourceId = DEFAULT_SOURCE_ID;
        private Integer businessId = DEFAULT_BUSINESS_ID;
        private Integer groupId;
        private Integer categoryId;
        private Long marketSkuId;
        private MarketSkuType mappingType;
        private Boolean createRandomParameters = false;
        private Boolean allowModelCreateUpdate = true;
        private Timestamp timestamp = Timestamp.from(Instant.now());
        private boolean deduplicated;
        private Boolean allowFastSkuCreation;

        private DatacampOfferForPipelineInformationBuilder() {
        }

        public static DatacampOfferForPipelineInformationBuilder builder() {
            return new DatacampOfferForPipelineInformationBuilder();
        }

        public DatacampOfferForPipelineInformationBuilder withOfferId(String offerId) {
            this.offerId = offerId;
            return this;
        }

        public DatacampOfferForPipelineInformationBuilder withSourceId(Integer sourceId) {
            this.sourceId = sourceId;
            return this;
        }

        public DatacampOfferForPipelineInformationBuilder withBusinessId(Integer businessId) {
            this.businessId = businessId;
            return this;
        }

        public DatacampOfferForPipelineInformationBuilder withGroupId(Integer groupId) {
            this.groupId = groupId;
            return this;
        }

        public DatacampOfferForPipelineInformationBuilder withCategoryId(Integer categoryId) {
            this.categoryId = categoryId;
            return this;
        }

        public DatacampOfferForPipelineInformationBuilder withMapping(Long marketSkuId, MarketSkuType mappingType) {
            this.mappingType = mappingType;
            this.marketSkuId = marketSkuId;
            return this;
        }

        public DatacampOfferForPipelineInformationBuilder withRandomParameters() {
            this.createRandomParameters = true;
            return this;
        }

        public DatacampOfferForPipelineInformationBuilder withTimestamp(Timestamp timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public DatacampOfferForPipelineInformationBuilder withAllowModelCreateUpdate(Boolean allow) {
            this.allowModelCreateUpdate = allow;
            return this;
        }

        public DatacampOfferForPipelineInformationBuilder withDeduplicated(Boolean isDeduplicated) {
            this.deduplicated = isDeduplicated;
            return this;
        }

        public DatacampOfferForPipelineInformationBuilder withAllowFastSkuCreation(Boolean isAllowFastSkuCreation) {
            this.allowFastSkuCreation = isAllowFastSkuCreation;
            return this;
        }

        public DatacampOffer build() {
            DatacampOffer datacampOffer = new DatacampOffer();
            datacampOffer.setCreateTime(timestamp);
            datacampOffer.setRequestTs(timestamp);
            datacampOffer.setStatus(DatacampOfferStatus.NEW);
            datacampOffer.setOfferId(this.offerId);
            datacampOffer.setBusinessId(this.businessId);
            datacampOffer.setSourceId(this.sourceId);
            datacampOffer.setGroupId(this.groupId);
            datacampOffer.setIsDeduplicated(this.deduplicated);
            datacampOffer.setIsAllowFastSkuCreation(this.allowFastSkuCreation);
            DataCampOffer.Offer.Builder offerDataBuilder = DataCampOffer.Offer.newBuilder();

            offerDataBuilder.getContentBuilder()
                    .getStatusBuilder()
                    .getContentSystemStatusBuilder()
                    .setAllowModelCreateUpdate(this.allowModelCreateUpdate);

            DataCampOfferMapping.Mapping.Builder approvedBuilder = offerDataBuilder.getContentBuilder()
                    .getBindingBuilder()
                    .getApprovedBuilder();

            if (this.categoryId != null) {
                approvedBuilder.setMarketCategoryId(this.categoryId);
            }

            if (this.marketSkuId != null) {
                approvedBuilder.setMarketSkuId(this.marketSkuId);
            }

            if (this.mappingType != null) {
                approvedBuilder.setMarketSkuType(this.mappingType);
            }

            if (this.createRandomParameters) {
                offerDataBuilder.getContentBuilder()
                        .getPartnerBuilder()
                        .getMarketSpecificContentBuilder()
                        .setParameterValues(
                                MarketParameterValues
                                        .newBuilder()
                                        .addParameterValues(
                                                MarketParameterValue
                                                        .newBuilder()
                                                        .setParamId(-1)
                                                        .build()
                                        )
                                        .build()
                        );

            }

            datacampOffer.setData(offerDataBuilder.build());
            return datacampOffer;
        }

    }

}
