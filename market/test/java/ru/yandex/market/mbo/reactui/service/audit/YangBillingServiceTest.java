package ru.yandex.market.mbo.reactui.service.audit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.market.mbo.billing.counter.task.YangBillingLoaderFactory;
import ru.yandex.market.mbo.billing.tarif.TarifManager;
import ru.yandex.market.mbo.billing.tarif.TarifMultiplicatorService;
import ru.yandex.market.mbo.billing.tarif.TarifProvider;
import ru.yandex.market.mbo.category.mappings.CategoryMappingService;
import ru.yandex.market.mbo.db.billing.dao.FullPaidEntry;
import ru.yandex.market.mbo.db.params.guru.GuruVendorsReader;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.http.YangLogStorage;
import ru.yandex.market.mbo.statistic.StatisticsService;
import ru.yandex.market.mbo.statistic.model.RawStatistics;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author dergachevfv
 * @since 12/30/19
 */
@SuppressWarnings("checkstyle:magicNumber")
@RunWith(Parameterized.class)
public class YangBillingServiceTest {

    public static final long USER_1 = 100L;
    public static final long USER_2 = 200L;

    private StatisticsService statisticsService;
    private CategoryMappingService categoryMappingService;
    private GuruVendorsReader vendorsReader;
    private TarifMultiplicatorService tarifMultiplicatorService;
    private TarifManager tarifManager;
    private YangBillingService yangBillingService;

    private YangLogStorage.YangTaskType yangTaskType;

    public YangBillingServiceTest(YangLogStorage.YangTaskType yangTaskType) {
        this.yangTaskType = yangTaskType;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> yangTaskTypes() {
        return Stream.of(YangLogStorage.YangTaskType.values())
            .map(type -> new Object[]{type})
            .collect(Collectors.toList());
    }

    @Before
    public void init() {
        statisticsService = mock(StatisticsService.class);
        categoryMappingService = mock(CategoryMappingService.class);
        vendorsReader = mock(GuruVendorsReader.class);
        tarifMultiplicatorService = mock(TarifMultiplicatorService.class);
        tarifManager = mock(TarifManager.class);

        TarifProvider tarifProvider = mock(TarifProvider.class);
        when(tarifProvider.containsTarif(anyInt(), anyLong()))
            .thenReturn(true);
        when(tarifProvider.getTarif(anyInt(), anyLong(), any()))
            .thenReturn(BigDecimal.ONE);
        when(tarifManager.loadTarifs(any()))
            .thenReturn(tarifProvider);

        yangBillingService = new YangBillingService(
            categoryMappingService,
            vendorsReader,
            tarifManager, new YangBillingLoaderFactory(statisticsService, tarifMultiplicatorService));
    }

    @Test
    public void testBillingIsCalculated() {
        Set<YangLogStorage.YangTaskType> toTest = Set.of(
                YangLogStorage.YangTaskType.BLUE_LOGS,
                YangLogStorage.YangTaskType.WHITE_LOGS,
                YangLogStorage.YangTaskType.DEEPMATCHER_LOGS,
                YangLogStorage.YangTaskType.MAPPING_MODERATION,
                YangLogStorage.YangTaskType.PARTNER_SKU_MODERATION,
                YangLogStorage.YangTaskType.CONTENT_LAB,
                YangLogStorage.YangTaskType.MSKU_FROM_PSKU_GENERATION,
                YangLogStorage.YangTaskType.FILL_SKU,
                YangLogStorage.YangTaskType.FMCG_BLUE_LOGS,
                YangLogStorage.YangTaskType.FMCG_CLASSIFICATION,
                YangLogStorage.YangTaskType.BLUE_CLASSIFICATION,
                YangLogStorage.YangTaskType.PARTNER_SKU_CLASSIFICATION
        );
        if (toTest.contains(yangTaskType)) {
            YangLogStorage.YangLogStoreRequest statistics = YangLogStorage.YangLogStoreRequest.newBuilder()
                    .setTaskType(yangTaskType)
                    .setContractorInfo(YangLogStorage.OperatorInfo.newBuilder()
                            .setUid(USER_1)
                            .build())
                    .setInspectorInfo(YangLogStorage.OperatorInfo.newBuilder()
                            .setUid(USER_2)
                            .build())
                    .addModelStatistic(YangLogStorage.ModelStatistic.newBuilder()
                            .setModelId(1L)
                            .setType(ModelStorage.ModelType.GURU)
                            .setContractorActions(YangLogStorage.ModelActions.newBuilder()
                                    .addParam(YangLogStorage.ActionInfo.newBuilder()
                                            .setAuditActionId(10L)
                                            .setEntityId(1L)))
                            .setInspectorActions(YangLogStorage.ModelActions.newBuilder()
                                    .addParam(YangLogStorage.ActionInfo.newBuilder()
                                            .setAuditActionId(20L)
                                            .setEntityId(2L))))
                    .addMappingStatistic(YangLogStorage.MappingStatistic.newBuilder()
                            .setUid(USER_1)
                            .setOfferId(1L)
                            .setMarketSkuId(1L)
                            .setOfferMappingStatus(YangLogStorage.MappingStatus.MAPPED))
                    .build();
            RawStatistics rawStatistics = new RawStatistics(new Date(), statistics);

            List<FullPaidEntry> paidEntries = yangBillingService.calculateBilling(rawStatistics);

            assertThat(paidEntries).isNotEmpty();
            assertThat(paidEntries).isNotEmpty();
        }
    }
}
