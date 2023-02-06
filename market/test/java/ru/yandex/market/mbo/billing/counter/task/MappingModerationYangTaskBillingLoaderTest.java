package ru.yandex.market.mbo.billing.counter.task;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.mbo.billing.BillingProvider;
import ru.yandex.market.mbo.billing.PaidAction;
import ru.yandex.market.mbo.billing.action.BillingAction;
import ru.yandex.market.mbo.billing.action.YangBillingAction;
import ru.yandex.market.mbo.billing.tarif.TarifMultiplicatorService;
import ru.yandex.market.mbo.category.mappings.CategoryMappingServiceMock;
import ru.yandex.market.mbo.core.guru.GuruCategoryService;
import ru.yandex.market.mbo.core.kdepot.api.EntityStub;
import ru.yandex.market.mbo.db.ParameterLoaderServiceStub;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction;
import ru.yandex.market.mbo.http.YangLogStorage;
import ru.yandex.market.mbo.statistic.StatisticsService;
import ru.yandex.market.mbo.statistic.model.RawStatistics;
import ru.yandex.market.mbo.statistic.model.TaskType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbo.billing.counter.task.YangTaskBillingHelperTest.CATEGORY_ID;
import static ru.yandex.market.mbo.billing.counter.task.YangTaskBillingHelperTest.OFFER_ID_1;
import static ru.yandex.market.mbo.billing.counter.task.YangTaskBillingHelperTest.OFFER_ID_2;
import static ru.yandex.market.mbo.billing.counter.task.YangTaskBillingHelperTest.OFFER_ID_3;
import static ru.yandex.market.mbo.billing.counter.task.YangTaskBillingHelperTest.SKU1;
import static ru.yandex.market.mbo.billing.counter.task.YangTaskBillingHelperTest.SKU2;
import static ru.yandex.market.mbo.billing.counter.task.YangTaskBillingHelperTest.SKU3;
import static ru.yandex.market.mbo.billing.counter.task.YangTaskBillingHelperTest.TASK_ID;
import static ru.yandex.market.mbo.billing.counter.task.YangTaskBillingHelperTest.USER1;
import static ru.yandex.market.mbo.billing.counter.task.YangTaskBillingHelperTest.USER2;

public class MappingModerationYangTaskBillingLoaderTest {
    private static final BigDecimal BASE_TARIF = new BigDecimal("0.03333");
    private static final Long HID1 = 42L;
    private static final Long HID2 = 4242L;

    private AbstractYangTaskBillingLoader billingLoader;
    private StatisticsService statisticsService;
    private BillingProvider billingProvider;
    private Date now = now();
    private Date tomorrow = tomorrow();

    @Before
    public void setUp() throws Exception {
        billingProvider = mock(BillingProvider.class);
        when(billingProvider.getPrice(any(), any())).thenReturn(BASE_TARIF);
        when(billingProvider.getInterval()).thenReturn(new Pair<>(now, tomorrow));
        statisticsService = mock(StatisticsService.class);

        CategoryMappingServiceMock categoryMappingService = new CategoryMappingServiceMock();
        categoryMappingService.addMapping(HID1, HID1);
        categoryMappingService.addMapping(HID2, HID2);

        GuruCategoryService guruCategoryService = mock(GuruCategoryService.class);
        when(guruCategoryService.getGuruCategoryEntityById(HID1)).thenReturn(new EntityStub());
        when(guruCategoryService.getGuruCategoryEntityById(HID2)).thenReturn(new EntityStub());

        TarifMultiplicatorService tarifMultiplicatorService =
            new TarifMultiplicatorService(guruCategoryService, new ParameterLoaderServiceStub());

        YangBillingLoaderFactory factory = new YangBillingLoaderFactory(statisticsService, tarifMultiplicatorService);
        billingLoader = factory.createLoader(YangLogStorage.YangTaskType.MAPPING_MODERATION);
    }

    @Test
    public void testBillingEnabled() {
        when(statisticsService.loadRawStatistics(any())).thenReturn(Collections.singletonList(
            new RawStatistics(tomorrow, YangLogStorage.YangLogStoreRequest.newBuilder()
                .setHitmanId(TASK_ID)
                .setCategoryId(CATEGORY_ID)
                .setContractorInfo(YangLogStorage.OperatorInfo.newBuilder().setUid(USER1).build())
                .addMappingModerationStatistic(YangLogStorage.MappingModerationStatistic.newBuilder()
                    .setOfferId(OFFER_ID_1)
                    .setMarketSkuId(SKU1)
                    .setMappingModerationStatus(YangLogStorage.MappingModerationStatus.ACCEPTED)
                    .setUid(USER1)
                    .build())
                .build())
        ));

        billingLoader.setEnabled(false);
        assertThat(billingLoader.loadBillingActions(billingProvider)).isEmpty();

        billingLoader.setEnabled(true);
        assertThat(billingLoader.loadBillingActions(billingProvider)).isNotEmpty();
    }

    @Test
    public void testMappingModerationBilling() {
        when(statisticsService.loadRawStatistics(any())).thenReturn(ImmutableList.of(
            new RawStatistics(tomorrow, YangLogStorage.YangLogStoreRequest.newBuilder()
                .setHitmanId(TASK_ID)
                .setTaskType(YangLogStorage.YangTaskType.MAPPING_MODERATION)
                .setCategoryId(HID1)
                .setContractorInfo(YangLogStorage.OperatorInfo.newBuilder().setUid(USER1).build())
                .addMappingModerationStatistic(YangLogStorage.MappingModerationStatistic.newBuilder()
                    .setMarketSkuId(SKU1)
                    .setOfferId(OFFER_ID_1)
                    .setMappingModerationStatus(YangLogStorage.MappingModerationStatus.NEED_INFO)
                    .setMappingModerationType(YangLogStorage.MappingModerationType.COMMON)
                    .setUid(USER1)
                    .build())
                .build()),
            new RawStatistics(tomorrow, YangLogStorage.YangLogStoreRequest.newBuilder()
                .setHitmanId(TASK_ID)
                .setTaskType(YangLogStorage.YangTaskType.MAPPING_MODERATION)
                .setCategoryId(HID2)
                .setContractorInfo(YangLogStorage.OperatorInfo.newBuilder().setUid(USER2).build())
                .addMappingModerationStatistic(YangLogStorage.MappingModerationStatistic.newBuilder()
                    .setMarketSkuId(SKU2)
                    .setOfferId(OFFER_ID_2)
                    .setMappingModerationStatus(YangLogStorage.MappingModerationStatus.ACCEPTED)
                    .setMappingModerationType(YangLogStorage.MappingModerationType.DEDUPLICATION)
                    .setUid(USER2)
                    .build())
                .build()),
            new RawStatistics(tomorrow, YangLogStorage.YangLogStoreRequest.newBuilder()
                .setHitmanId(TASK_ID)
                .setTaskType(YangLogStorage.YangTaskType.MAPPING_MODERATION)
                .setCategoryId(HID2)
                .setContractorInfo(YangLogStorage.OperatorInfo.newBuilder().setUid(USER2).build())
                .addMappingModerationStatistic(YangLogStorage.MappingModerationStatistic.newBuilder()
                    .setMarketSkuId(SKU3)
                    .setOfferId(OFFER_ID_3)
                    .setMappingModerationStatus(YangLogStorage.MappingModerationStatus.REJECTED)
                    .setMappingModerationType(YangLogStorage.MappingModerationType.RECHECK)
                    .setUid(USER2)
                    .build())
                .build())
        ));

        List<BillingAction> billingActions = billingLoader.loadBillingActions(billingProvider);

        assertThat(billingActions).containsExactlyInAnyOrder(
            billingAction(USER1, HID1, SKU1, OFFER_ID_1, PaidAction.YANG_MAPPING_MODERATION_COMMON_NEED_INFO),
            billingAction(USER2, HID2, SKU2, OFFER_ID_2, PaidAction.YANG_MAPPING_MODERATION_DEDUPLICATION_ACCEPTED),
            billingAction(USER2, HID2, SKU3, OFFER_ID_3, PaidAction.YANG_MAPPING_MODERATION_RECHECK_REJECTED)
        );
    }

    private BillingAction billingAction(long userId, long categoryId, long modelId, long offerId,
                                        PaidAction paidAction) {
        return
            new BillingAction(userId, paidAction, tomorrow, categoryId, modelId,
                AuditAction.EntityType.MODEL_SKU,
                YangBillingAction.DEFAULT_AUDIT_ACTION_ID, TaskType.MAPPING_MODERATION, String.valueOf(TASK_ID)
            )
                .setLinkData(Long.toString(offerId));
    }

    private static Date now() {
        return new Date();
    }

    private static Date tomorrow() {
        return toDate(LocalDateTime.now().plusDays(1));
    }

    private static Date toDate(LocalDateTime ldt) {
        return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
    }
}
