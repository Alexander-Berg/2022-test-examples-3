package ru.yandex.market.mbo.billing.counter.task;

import com.google.common.collect.ImmutableList;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.mbo.billing.BillingProvider;
import ru.yandex.market.mbo.billing.PaidAction;
import ru.yandex.market.mbo.billing.action.BillingAction;
import ru.yandex.market.mbo.billing.action.YangBillingAction;
import ru.yandex.market.mbo.category.mappings.CategoryMappingServiceMock;
import ru.yandex.market.mbo.core.kdepot.api.EntityStub;
import ru.yandex.market.mbo.db.ParameterLoaderServiceStub;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction;
import ru.yandex.market.mbo.gwt.models.params.CategoryParamBuilder;
import ru.yandex.market.mbo.http.ModelStorage;
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
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbo.billing.counter.task.YangTaskBillingHelperTest.CATEGORY_ID;
import static ru.yandex.market.mbo.billing.counter.task.YangTaskBillingHelperTest.MODEL1;
import static ru.yandex.market.mbo.billing.counter.task.YangTaskBillingHelperTest.MODEL2;
import static ru.yandex.market.mbo.billing.counter.task.YangTaskBillingHelperTest.OFFER_ID_1;
import static ru.yandex.market.mbo.billing.counter.task.YangTaskBillingHelperTest.OFFER_ID_2;
import static ru.yandex.market.mbo.billing.counter.task.YangTaskBillingHelperTest.SKU1;
import static ru.yandex.market.mbo.billing.counter.task.YangTaskBillingHelperTest.TASK_ID;
import static ru.yandex.market.mbo.billing.counter.task.YangTaskBillingHelperTest.USER1;
import static ru.yandex.market.mbo.billing.counter.task.YangTaskBillingHelperTest.USER2;
import static ru.yandex.market.mbo.billing.counter.task.YangTaskBillingHelperTest.WHITE_OFFER_ID_1;

/**
 * @author danfertev
 * @since 20.03.2019
 */
@SuppressWarnings("checkstyle:magicNumber")
public abstract class AbstractYangTaskBillingLoaderTest {
    protected static final BigDecimal BASE_TARIF = new BigDecimal("0.03333");
    protected static final Long HID1 = 42L;
    protected static final Long HID2 = 4242L;
    protected static final long PARAM1 = 100L;
    protected static final long PARAM2 = 200L;
    protected static final long PARAM3 = 300L;
    protected static final BigDecimal SEARCH_DIFFICULTY = new BigDecimal("10.0");
    protected static final BigDecimal FILL_DIFFICULTY = new BigDecimal("1.5");

    protected StatisticsService statisticsService;
    protected BillingProvider billingProvider;
    protected ParameterLoaderServiceStub parameterLoaderService;
    protected Date now = now();
    protected Date tomorrow = tomorrow();
    protected ru.yandex.market.mbo.core.guru.GuruCategoryService guruCategoryService;

    @Before
    public void setUp() throws Exception {
        billingProvider = mock(BillingProvider.class);
        when(billingProvider.getPrice(any(), any())).thenReturn(BASE_TARIF);
        when(billingProvider.getInterval()).thenReturn(new Pair<>(now, tomorrow));
        statisticsService = mock(StatisticsService.class);
        parameterLoaderService = new ParameterLoaderServiceStub();
        parameterLoaderService.addCategoryParam(CategoryParamBuilder.newBuilder(PARAM1, "test-xsl-name-1")
            .setCategoryHid(HID1)
            .setName("My-parameter in category 1")
            .setFillDifficulty(FILL_DIFFICULTY)
            .build());
        parameterLoaderService.addCategoryParam(CategoryParamBuilder.newBuilder(PARAM2, "test-xsl-name-2")
            .setCategoryHid(HID2)
            .setName("My-parameter in category 2")
            .build());
        parameterLoaderService.addCategoryParam(CategoryParamBuilder.newBuilder(PARAM3, "test-xsl-name-3")
            .setCategoryHid(HID1)
            .setName("My-parameter in category 3")
            .setFillDifficulty(FILL_DIFFICULTY)
            .setService(true)
            .build());

        CategoryMappingServiceMock categoryMappingService = new CategoryMappingServiceMock();
        categoryMappingService.addMapping(HID1, HID1);
        categoryMappingService.addMapping(HID2, HID2);

        guruCategoryService = spy(new ru.yandex.market.mbo.core.guru.GuruCategoryService(null, categoryMappingService));

        EntityStub guruCategory1 = new EntityStub();
        guruCategory1.setAttribute("search_info_difficulty", SEARCH_DIFFICULTY);

        EntityStub guruCategory2 = new EntityStub();
        Mockito.doReturn(guruCategory1).when(guruCategoryService).getGuruCategoryEntityById(HID1);
        Mockito.doReturn(guruCategory2).when(guruCategoryService).getGuruCategoryEntityById(HID2);
    }

    protected abstract AbstractYangTaskBillingLoader getLoader();

    @Test
    public void testBillingEnabled() {
        AbstractYangTaskBillingLoader billingLoader = getLoader();

        when(statisticsService.loadRawStatistics(any())).thenReturn(Collections.singletonList(
            new RawStatistics(tomorrow, YangLogStorage.YangLogStoreRequest.newBuilder()
                .setHitmanId(TASK_ID)
                .setCategoryId(CATEGORY_ID)
                .setContractorInfo(YangLogStorage.OperatorInfo.newBuilder().setUid(USER1).build())
                .setInspectorInfo(YangLogStorage.OperatorInfo.newBuilder().setUid(USER2).build())
                .addMappingStatistic(YangLogStorage.MappingStatistic.newBuilder()
                    .setMarketSkuId(MODEL2).setOfferId(OFFER_ID_1)
                    .setOfferMappingStatus(YangLogStorage.MappingStatus.MAPPED).build())
                .build())
        ));

        billingLoader.setEnabled(false);
        assertThat(billingLoader.loadBillingActions(billingProvider)).isEmpty();

        billingLoader.setEnabled(true);
        assertThat(billingLoader.loadBillingActions(billingProvider)).isNotEmpty();
    }

    @Test
    public void testBillingForOffers() {
        AbstractYangTaskBillingLoader billingLoader = getLoader();

        when(statisticsService.loadRawStatistics(any())).thenReturn(ImmutableList.of(
            new RawStatistics(tomorrow, YangLogStorage.YangLogStoreRequest.newBuilder()
                .setHitmanId(TASK_ID)
                .setCategoryId(HID1)
                .setContractorInfo(YangLogStorage.OperatorInfo.newBuilder().setUid(USER1).build())
                .setInspectorInfo(YangLogStorage.OperatorInfo.newBuilder().setUid(USER2).build())
                .addMappingStatistic(YangLogStorage.MappingStatistic.newBuilder()
                    .setUid(USER1)
                    .setOfferId(OFFER_ID_1)
                    .setMarketSkuId(SKU1)
                    .setOfferMappingStatus(YangLogStorage.MappingStatus.MAPPED))
                .addMappingModerationStatistic(YangLogStorage.MappingModerationStatistic.newBuilder()
                    .setUid(USER1)
                    .setOfferId(OFFER_ID_2)
                    .setMarketSkuId(SKU1)
                    .setMappingModerationStatus(YangLogStorage.MappingModerationStatus.ACCEPTED)
                    .setMappingModerationType(YangLogStorage.MappingModerationType.DEDUPLICATION))
                .addMatchingStatistic(YangLogStorage.MatchingStatistic.newBuilder()
                    .setUid(USER1)
                    .setOfferId(WHITE_OFFER_ID_1)
                    .setModelId(MODEL1)
                    .setOfferStatus(YangLogStorage.MatchingStatus.TRASH_OFFER))
                .build())
        ));

        List<BillingAction> billingActions = billingLoader.loadBillingActions(billingProvider);

        Assertions.assertThat(billingActions).containsExactlyInAnyOrder(
            billingActionWithOfferID(USER1, HID1, SKU1, AuditAction.EntityType.MODEL_SKU,
                    PaidAction.YANG_SKU_MAPPING, OFFER_ID_1),
            billingActionWithOfferID(USER2, HID1, SKU1, AuditAction.EntityType.MODEL_SKU,
                    PaidAction.YANG_SKU_MAPPING_VERIFICATION, OFFER_ID_1),
            billingActionWithOfferID(USER1, HID1, SKU1, AuditAction.EntityType.MODEL_SKU,
                    PaidAction.YANG_MAPPING_MODERATION_DEDUPLICATION_ACCEPTED, OFFER_ID_2),
            billingActionWithOfferID(USER1, HID1, MODEL1, AuditAction.EntityType.MODEL_GURU,
                    PaidAction.YANG_WRONG_CATEGORY, WHITE_OFFER_ID_1),
            billingActionWithOfferID(USER2, HID1, MODEL1, AuditAction.EntityType.MODEL_GURU,
                    PaidAction.YANG_WRONG_CATEGORY_VERIFICATION, WHITE_OFFER_ID_1)
        );
    }

    @Test
    public void testBillingWithSearchDifficulty() {
        AbstractYangTaskBillingLoader billingLoader = getLoader();

        when(statisticsService.loadRawStatistics(any())).thenReturn(ImmutableList.of(
            new RawStatistics(tomorrow, YangLogStorage.YangLogStoreRequest.newBuilder()
                .setHitmanId(TASK_ID)
                .setCategoryId(HID1)
                .setContractorInfo(YangLogStorage.OperatorInfo.newBuilder().setUid(USER1).build())
                .setInspectorInfo(YangLogStorage.OperatorInfo.newBuilder().setUid(USER2).build())
                .addModelStatistic(YangLogStorage.ModelStatistic.newBuilder()
                    .setModelId(MODEL1)
                    .setType(ModelStorage.ModelType.GURU)
                    .setContractorChanges(YangLogStorage.ActionCount.newBuilder()
                        .setAliases(1)
                        .setParam(1)
                        .addParamIds(PARAM1)
                        .build())
                    .setInspectorCorrections(YangLogStorage.ActionCount.newBuilder()
                        .setAliases(1)
                        .setParam(1)
                        .addParamIds(PARAM1)
                        .build())
                    .build())
                .addModelStatistic(YangLogStorage.ModelStatistic.newBuilder()
                    .setModelId(MODEL2)
                    .setType(ModelStorage.ModelType.SKU)
                    .setContractorChanges(YangLogStorage.ActionCount.newBuilder()
                        .setParam(1)
                        .addParamIds(PARAM1)
                        .build())
                    .setInspectorCorrections(YangLogStorage.ActionCount.newBuilder()
                        .setParam(1)
                        .addParamIds(PARAM1)
                        .build())
                    .build())
                .build()),
            new RawStatistics(tomorrow, YangLogStorage.YangLogStoreRequest.newBuilder()
                .setHitmanId(TASK_ID)
                .setCategoryId(HID2)
                .setContractorInfo(YangLogStorage.OperatorInfo.newBuilder().setUid(USER1).build())
                .setInspectorInfo(YangLogStorage.OperatorInfo.newBuilder().setUid(USER2).build())
                .addModelStatistic(YangLogStorage.ModelStatistic.newBuilder()
                    .setModelId(MODEL1)
                    .setType(ModelStorage.ModelType.GURU)
                    .setContractorChanges(YangLogStorage.ActionCount.newBuilder()
                        .setAliases(1)
                        .setParam(1)
                        .addParamIds(PARAM2)
                        .build())
                    .setInspectorCorrections(YangLogStorage.ActionCount.newBuilder()
                        .setAliases(1)
                        .setParam(1)
                        .addParamIds(PARAM2)
                        .build())
                    .build())
                .addModelStatistic(YangLogStorage.ModelStatistic.newBuilder()
                    .setModelId(MODEL2)
                    .setType(ModelStorage.ModelType.SKU)
                    .setContractorChanges(YangLogStorage.ActionCount.newBuilder()
                        .setParam(1)
                        .addParamIds(PARAM2)
                        .build())
                    .setInspectorCorrections(YangLogStorage.ActionCount.newBuilder()
                        .setParam(1)
                        .addParamIds(PARAM2)
                        .build())
                    .build())
                .build())
        ));

        List<BillingAction> billingActions = billingLoader.loadBillingActions(billingProvider);

        assertThat(billingActions).containsExactlyInAnyOrder(
                // в категории с заданной "сложностью", тарифы для параметров увеличены
                billingAction(USER1, HID1, MODEL1, AuditAction.EntityType.MODEL_GURU, PaidAction.YANG_ADD_ALIAS),
                billingAction(USER1, HID1, MODEL1, AuditAction.EntityType.MODEL_GURU, PaidAction.YANG_ADD_PARAM_VALUE)
                        .setPriceMultiplicator(SEARCH_DIFFICULTY.multiply(FILL_DIFFICULTY))
                        .setParameterName("My-parameter in category 1"),
                billingAction(USER2, HID1, MODEL1, AuditAction.EntityType.MODEL_GURU,
                        PaidAction.YANG_ADD_ALIAS_VERIFICATION),
                billingAction(USER2, HID1, MODEL1, AuditAction.EntityType.MODEL_GURU,
                        PaidAction.YANG_ADD_PARAM_VALUE_VERIFICATION)
                        .setPriceMultiplicator(SEARCH_DIFFICULTY.multiply(FILL_DIFFICULTY))
                        .setParameterName("My-parameter in category 1"),
                billingAction(USER1, HID1, MODEL2, AuditAction.EntityType.MODEL_SKU,
                        PaidAction.YANG_ADD_SKU_PARAM_VALUE)
                        .setPriceMultiplicator(SEARCH_DIFFICULTY.multiply(FILL_DIFFICULTY))
                        .setParameterName("My-parameter in category 1"),
                billingAction(USER2, HID1, MODEL2, AuditAction.EntityType.MODEL_SKU,
                        PaidAction.YANG_ADD_SKU_PARAM_VALUE_VERIFICATION)
                        .setPriceMultiplicator(SEARCH_DIFFICULTY.multiply(FILL_DIFFICULTY))
                        .setParameterName("My-parameter in category 1"),
                billingAction(USER2, HID1, MODEL1, AuditAction.EntityType.MODEL_GURU,
                        PaidAction.YANG_ADD_ALIAS_CORRECTION),
                billingAction(USER2, HID1, MODEL1, AuditAction.EntityType.MODEL_GURU,
                        PaidAction.YANG_ADD_PARAM_VALUE_CORRECTION)
                        .setPriceMultiplicator(SEARCH_DIFFICULTY.multiply(FILL_DIFFICULTY))
                        .setParameterName("My-parameter in category 1"),
                billingAction(USER2, HID1, MODEL2, AuditAction.EntityType.MODEL_SKU,
                        PaidAction.YANG_ADD_SKU_PARAM_VALUE_CORRECTION)
                        .setPriceMultiplicator(SEARCH_DIFFICULTY.multiply(FILL_DIFFICULTY))
                        .setParameterName("My-parameter in category 1"),

                // в категории без "сложности" все тарифы остались без изменений
                billingAction(USER1, HID2, MODEL1, AuditAction.EntityType.MODEL_GURU, PaidAction.YANG_ADD_ALIAS),
                billingAction(USER1, HID2, MODEL1, AuditAction.EntityType.MODEL_GURU, PaidAction.YANG_ADD_PARAM_VALUE),
                billingAction(USER2, HID2, MODEL1, AuditAction.EntityType.MODEL_GURU,
                        PaidAction.YANG_ADD_ALIAS_VERIFICATION),
                billingAction(USER2, HID2, MODEL1, AuditAction.EntityType.MODEL_GURU,
                        PaidAction.YANG_ADD_PARAM_VALUE_VERIFICATION),
                billingAction(USER1, HID2, MODEL2, AuditAction.EntityType.MODEL_SKU,
                        PaidAction.YANG_ADD_SKU_PARAM_VALUE),
                billingAction(USER2, HID2, MODEL2, AuditAction.EntityType.MODEL_SKU,
                        PaidAction.YANG_ADD_SKU_PARAM_VALUE_VERIFICATION),
                billingAction(USER2, HID2, MODEL1, AuditAction.EntityType.MODEL_GURU,
                        PaidAction.YANG_ADD_ALIAS_CORRECTION),
                billingAction(USER2, HID2, MODEL1, AuditAction.EntityType.MODEL_GURU,
                        PaidAction.YANG_ADD_PARAM_VALUE_CORRECTION),
                billingAction(USER2, HID2, MODEL2, AuditAction.EntityType.MODEL_SKU,
                        PaidAction.YANG_ADD_SKU_PARAM_VALUE_CORRECTION)
        );
    }

    @Test
    public void testServiceParamWithFillDifficulty() {
        AbstractYangTaskBillingLoader billingLoader = getLoader();

        when(statisticsService.loadRawStatistics(any())).thenReturn(ImmutableList.of(
            new RawStatistics(tomorrow, YangLogStorage.YangLogStoreRequest.newBuilder()
                .setHitmanId(TASK_ID)
                .setCategoryId(HID1)
                .setContractorInfo(YangLogStorage.OperatorInfo.newBuilder().setUid(USER1).build())
                .setInspectorInfo(YangLogStorage.OperatorInfo.newBuilder().setUid(USER2).build())
                .addModelStatistic(YangLogStorage.ModelStatistic.newBuilder()
                    .setModelId(MODEL1)
                    .setType(ModelStorage.ModelType.GURU)
                    .setInspectorChanges(YangLogStorage.ActionCount.newBuilder()
                        .setParam(2)
                        .addParamIds(PARAM1).addParamIds(PARAM3)
                        .build())
                    .build())
                .build()
            )
        ));

        List<BillingAction> billingActions = billingLoader.loadBillingActions(billingProvider);

        assertThat(billingActions).containsExactlyInAnyOrder(
                billingActionWithMultiplicator(USER2, HID1, MODEL1,
                        AuditAction.EntityType.MODEL_GURU, PaidAction.YANG_ADD_PARAM_VALUE,
                        SEARCH_DIFFICULTY.multiply(FILL_DIFFICULTY))
                        .setParameterName("My-parameter in category 1"),
                billingActionWithMultiplicator(USER2, HID1, MODEL1,
                        AuditAction.EntityType.MODEL_GURU, PaidAction.YANG_ADD_PARAM_VALUE,
                        SEARCH_DIFFICULTY)
        );
    }

    @Test
    public void testServiceParamWithFillDifficultyAndStaticMultiplier() {
        AbstractYangTaskBillingLoader billingLoader = getLoader();
        BigDecimal staticMultiplier = new BigDecimal("1.2");
        billingLoader.setStaticMultiplicator(staticMultiplier);

        when(statisticsService.loadRawStatistics(any())).thenReturn(ImmutableList.of(
                new RawStatistics(tomorrow, YangLogStorage.YangLogStoreRequest.newBuilder()
                        .setHitmanId(TASK_ID)
                        .setCategoryId(HID1)
                        .setContractorInfo(YangLogStorage.OperatorInfo.newBuilder().setUid(USER1).build())
                        .setInspectorInfo(YangLogStorage.OperatorInfo.newBuilder().setUid(USER2).build())
                        .addModelStatistic(YangLogStorage.ModelStatistic.newBuilder()
                                .setModelId(MODEL1)
                                .setType(ModelStorage.ModelType.GURU)
                                .setInspectorChanges(YangLogStorage.ActionCount.newBuilder()
                                        .setParam(2)
                                        .addParamIds(PARAM1).addParamIds(PARAM3)
                                        .build())
                                .build())
                        .build()
                )
        ));

        List<BillingAction> billingActions = billingLoader.loadBillingActions(billingProvider);

        assertThat(billingActions).containsExactlyInAnyOrder(
                billingActionWithMultiplicator(USER2, HID1, MODEL1, AuditAction.EntityType.MODEL_GURU,
                        PaidAction.YANG_ADD_PARAM_VALUE,
                        SEARCH_DIFFICULTY.multiply(FILL_DIFFICULTY).multiply(staticMultiplier))
                        .setParameterName("My-parameter in category 1"),
                billingActionWithMultiplicator(USER2, HID1, MODEL1, AuditAction.EntityType.MODEL_GURU,
                        PaidAction.YANG_ADD_PARAM_VALUE,
                        SEARCH_DIFFICULTY.multiply(staticMultiplier))
        );
    }

    private BillingAction billingActionWithOfferID(long userId, long categoryId, long modelId,
                                                   AuditAction.EntityType entityType, PaidAction paidAction,
                                                   long offerId) {
        return billingActionWithOfferID(userId, categoryId, modelId, entityType, paidAction, Long.toString(offerId));
    }

    private BillingAction billingActionWithOfferID(long userId, long categoryId, long modelId,
                                                   AuditAction.EntityType entityType, PaidAction paidAction,
                                                   String offerId) {
        return billingAction(userId, categoryId, modelId, entityType, paidAction)
            .setLinkData(offerId);
    }

    private BillingAction billingActionWithMultiplicator(long userId, long categoryId, long modelId,
                                                         AuditAction.EntityType entityType,
                                                         PaidAction paidAction, BigDecimal priceMultiplicator) {
        return billingAction(userId, categoryId, modelId, entityType, paidAction)
            .setPriceMultiplicator(priceMultiplicator);
    }

    private BillingAction billingAction(long userId, long categoryId, long modelId,
                                        AuditAction.EntityType entityType, PaidAction paidAction) {
        return new BillingAction(userId, paidAction, tomorrow, categoryId, modelId,
                entityType,
                YangBillingAction.DEFAULT_AUDIT_ACTION_ID, TaskType.BLUE_LOGS, String.valueOf(TASK_ID));
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
