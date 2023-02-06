package ru.yandex.market.mbo.billing.counter.task;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.billing.PaidAction;
import ru.yandex.market.mbo.billing.action.YangBillingAction;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.http.YangLogStorage;
import ru.yandex.market.mbo.statistic.model.RawStatistics;
import ru.yandex.market.mbo.statistic.model.TaskType;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.mbo.statistic.model.TaskType.BLUE_LOGS;
import static ru.yandex.market.mbo.statistic.model.TaskType.MAPPING_MODERATION;
import static ru.yandex.market.mbo.statistic.model.TaskType.MAPPING_MODERATION_TO_PSKU;
import static ru.yandex.market.mbo.statistic.model.TaskType.SKU_PARAMETERS_CONFLICT;
import static ru.yandex.market.mbo.statistic.model.TaskType.WHITE_LOGS;

/**
 * @author kravchenko-aa
 * @date 23/05/2019
 */
public class YangTaskBillingHelperTest {
    static final long CATEGORY_ID = 1L;
    static final long USER1 = 10L;
    static final long USER2 = 20L;
    static final long MODEL1 = 100L;
    static final long MODEL2 = 200L;
    static final long MODEL3 = 300L;
    static final long SKU1 = 100000L;
    static final long SKU2 = 100001L;
    static final long SKU3 = 100002L;
    static final long OPTION_ID1 = 500L;
    static final long OPTION_ID2 = 600L;
    static final long OPTION_ID3 = 700L;
    static final long OFFER_ID_1 = 1000L;
    static final long OFFER_ID_2 = 2000L;
    static final long OFFER_ID_3 = 3000L;
    static final long OFFER_ID_4 = 4000L;
    static final long OFFER_ID_5 = 5000L;
    static final String WHITE_OFFER_ID_1 = "abcabcac23234_1";
    static final String WHITE_OFFER_ID_2 = "abcabcac23234_2";
    static final String WHITE_OFFER_ID_3 = "abcabcac23234_3";
    static final String WHITE_OFFER_ID_4 = "abcabcac23234_4";
    static final long TASK_ID = 10000;
    static final int ALIASES_COUNT = 42;
    static final int CUT_OFF_COUNT = 15;
    static final long PARAMETER_ID = 1;

    private final Date tomorrow = tomorrow();

    @Before
    public void setUp() {
        YangTaskBillingHelper.enableLocalVendorBilling();
    }

    @Test
    public void testBillMappingsWithoutUserId() {
        List<YangBillingAction> billingActions = YangTaskBillingHelper.processRawStatistic(
            new RawStatistics(tomorrow, YangLogStorage.YangLogStoreRequest.newBuilder()
                .setHitmanId(TASK_ID)
                .setTaskType(YangLogStorage.YangTaskType.BLUE_LOGS)
                .setCategoryId(CATEGORY_ID)
                .setContractorInfo(YangLogStorage.OperatorInfo.newBuilder().setUid(USER1).build())
                .setInspectorInfo(YangLogStorage.OperatorInfo.newBuilder().setUid(USER2).build())
                .addMappingStatistic(YangLogStorage.MappingStatistic.newBuilder()
                    .setMarketSkuId(SKU2).setOfferId(OFFER_ID_1)
                    .setOfferMappingStatus(YangLogStorage.MappingStatus.MAPPED).build())
                .addMappingStatistic(YangLogStorage.MappingStatistic.newBuilder()
                    .setMarketSkuId(SKU2).setOfferId(OFFER_ID_2)
                    .setOfferMappingStatus(YangLogStorage.MappingStatus.TRASH).build())
                .addMappingStatistic(YangLogStorage.MappingStatistic.newBuilder()
                    .setMarketSkuId(SKU2).setOfferId(OFFER_ID_3)
                    .setOfferMappingStatus(YangLogStorage.MappingStatus.WRONG_CATEGORY).build())
                .build()));

        assertThat(billingActions).containsExactlyInAnyOrder(
            withLinkData(OFFER_ID_1, user1Sku2Action(PaidAction.YANG_SKU_MAPPING, BLUE_LOGS)),
            withLinkData(OFFER_ID_2, user1Sku2Action(PaidAction.YANG_TRASH_MAPPING, BLUE_LOGS)),
            withLinkData(OFFER_ID_3, user1Sku2Action(PaidAction.YANG_TRASH_MAPPING, BLUE_LOGS)),
            withLinkData(OFFER_ID_1, user2Sku2Action(PaidAction.YANG_SKU_MAPPING_VERIFICATION, BLUE_LOGS)),
            withLinkData(OFFER_ID_2, user2Sku2Action(PaidAction.YANG_TRASH_MAPPING_VERIFICATION, BLUE_LOGS)),
            withLinkData(OFFER_ID_3, user2Sku2Action(PaidAction.YANG_TRASH_MAPPING_VERIFICATION, BLUE_LOGS))
        );
    }

    @Test
    public void testBillOnlyContractorMappingsWithoutUserId() {
        List<YangBillingAction> billingActions = YangTaskBillingHelper.getOperatorChangesBillingAction(
            new RawStatistics(tomorrow, YangLogStorage.YangLogStoreRequest.newBuilder()
                .setHitmanId(TASK_ID)
                .setCategoryId(CATEGORY_ID)
                .setContractorInfo(YangLogStorage.OperatorInfo.newBuilder().setUid(USER1).build())
                .setInspectorInfo(YangLogStorage.OperatorInfo.newBuilder().setUid(USER2).build())
                .addMappingStatistic(YangLogStorage.MappingStatistic.newBuilder()
                    .setMarketSkuId(SKU1).setOfferId(OFFER_ID_1)
                    .setOfferMappingStatus(YangLogStorage.MappingStatus.MAPPED).build())
                .addMappingStatistic(YangLogStorage.MappingStatistic.newBuilder()
                    .setMarketSkuId(SKU1).setOfferId(OFFER_ID_2)
                    .setOfferMappingStatus(YangLogStorage.MappingStatus.TRASH).build())
                .addMappingStatistic(YangLogStorage.MappingStatistic.newBuilder()
                    .setMarketSkuId(SKU1).setOfferId(OFFER_ID_3)
                    .setOfferMappingStatus(YangLogStorage.MappingStatus.WRONG_CATEGORY).build())
                .build()));

        assertThat(billingActions).containsExactlyInAnyOrder(
            withLinkData(OFFER_ID_1, user1Sku1Action(PaidAction.YANG_SKU_MAPPING, BLUE_LOGS)),
            withLinkData(OFFER_ID_2, user1Sku1Action(PaidAction.YANG_TRASH_MAPPING, BLUE_LOGS)),
            withLinkData(OFFER_ID_3, user1Sku1Action(PaidAction.YANG_TRASH_MAPPING, BLUE_LOGS))
        );
    }

    @Test
    public void testBillMappingsWithCorrection() {
        List<YangBillingAction> billingActions = YangTaskBillingHelper.processRawStatistic(
            new RawStatistics(tomorrow, YangLogStorage.YangLogStoreRequest.newBuilder()
                .setHitmanId(TASK_ID)
                .setTaskType(YangLogStorage.YangTaskType.BLUE_LOGS)
                .setCategoryId(CATEGORY_ID)
                .setContractorInfo(YangLogStorage.OperatorInfo.newBuilder().setUid(USER1).build())
                .setInspectorInfo(YangLogStorage.OperatorInfo.newBuilder().setUid(USER2).build())
                .addMappingStatistic(YangLogStorage.MappingStatistic.newBuilder()
                    .setMarketSkuId(SKU2).setOfferId(OFFER_ID_1).setUid(USER1)
                    .setOfferMappingStatus(YangLogStorage.MappingStatus.MAPPED).build())
                .addMappingStatistic(YangLogStorage.MappingStatistic.newBuilder()
                    .setMarketSkuId(SKU2).setOfferId(OFFER_ID_2).setUid(USER2)
                    .setOfferMappingStatus(YangLogStorage.MappingStatus.TRASH).build())
                .addMappingStatistic(YangLogStorage.MappingStatistic.newBuilder()
                    .setMarketSkuId(SKU2).setOfferId(OFFER_ID_3).setUid(USER2)
                    .setOfferMappingStatus(YangLogStorage.MappingStatus.WRONG_CATEGORY).build())
                .addMappingStatistic(YangLogStorage.MappingStatistic.newBuilder()
                    .setMarketSkuId(SKU2).setOfferId(OFFER_ID_4).setUid(USER1)
                    .setOfferMappingStatus(YangLogStorage.MappingStatus.TRASH).build())
                .addMappingStatistic(YangLogStorage.MappingStatistic.newBuilder()
                    .setMarketSkuId(SKU2).setOfferId(OFFER_ID_4).setUid(USER2)
                    .setOfferMappingStatus(YangLogStorage.MappingStatus.WRONG_CATEGORY).build())
                .build())
        );

        assertThat(billingActions).containsExactlyInAnyOrder(
            withLinkData(OFFER_ID_1, user1Sku2Action(PaidAction.YANG_SKU_MAPPING, BLUE_LOGS)),
            withLinkData(OFFER_ID_1, user2Sku2Action(PaidAction.YANG_SKU_MAPPING_VERIFICATION, BLUE_LOGS)),
            withLinkData(OFFER_ID_2, user2Sku2Action(PaidAction.YANG_TRASH_MAPPING_CORRECTION, BLUE_LOGS)),
            withLinkData(OFFER_ID_3, user2Sku2Action(PaidAction.YANG_TRASH_MAPPING_CORRECTION, BLUE_LOGS)),
            withLinkData(OFFER_ID_4, user2Sku2Action(PaidAction.YANG_TRASH_MAPPING_CORRECTION, BLUE_LOGS))
        );
    }

    @Test
    public void testBillMatchingTrash() {
        List<YangBillingAction> billingActions = YangTaskBillingHelper.processRawStatistic(
            new RawStatistics(tomorrow, YangLogStorage.YangLogStoreRequest.newBuilder()
                .setHitmanId(TASK_ID)
                .setTaskType(YangLogStorage.YangTaskType.WHITE_LOGS)
                .setCategoryId(CATEGORY_ID)
                .setContractorInfo(YangLogStorage.OperatorInfo.newBuilder().setUid(USER1).build())
                .setInspectorInfo(YangLogStorage.OperatorInfo.newBuilder().setUid(USER2).build())
                .addMatchingStatistic(YangLogStorage.MatchingStatistic.newBuilder()
                    .setOfferId(WHITE_OFFER_ID_1).setUid(USER1)
                    .setOfferStatus(YangLogStorage.MatchingStatus.TRASH_OFFER).build())
                .addMatchingStatistic(YangLogStorage.MatchingStatistic.newBuilder()
                    .setOfferId(WHITE_OFFER_ID_2).setUid(USER2)
                    .setOfferStatus(YangLogStorage.MatchingStatus.TRASH_OFFER).build())
                .addMatchingStatistic(YangLogStorage.MatchingStatistic.newBuilder()
                    .setModelId(MODEL1).setOfferId(WHITE_OFFER_ID_3).setUid(USER1)
                    .setOfferStatus(YangLogStorage.MatchingStatus.CANNOT_BE_IMPROVED).build())
                .addMatchingStatistic(YangLogStorage.MatchingStatistic.newBuilder()
                    .setModelId(MODEL2).setOfferId(WHITE_OFFER_ID_4).setUid(USER1)
                    .setOfferStatus(YangLogStorage.MatchingStatus.TRASH_OFFER).build())
                .addMatchingStatistic(YangLogStorage.MatchingStatistic.newBuilder()
                    .setModelId(MODEL2).setOfferId(WHITE_OFFER_ID_4).setUid(USER2)
                    .setOfferStatus(YangLogStorage.MatchingStatus.CANNOT_BE_IMPROVED).build())
                .build())
        );

        assertThat(billingActions).containsExactlyInAnyOrder(
            withLinkData(WHITE_OFFER_ID_1, user1Action(PaidAction.YANG_WRONG_CATEGORY, 0L,
                null, WHITE_LOGS)),
            withLinkData(WHITE_OFFER_ID_1, user2Action(PaidAction.YANG_WRONG_CATEGORY_VERIFICATION, 0L,
                null, WHITE_LOGS)),
            withLinkData(WHITE_OFFER_ID_2, user2Action(PaidAction.YANG_WRONG_CATEGORY_CORRECTION, 0L,
                null, WHITE_LOGS)),

            withLinkData(WHITE_OFFER_ID_3, user1Action(PaidAction.YANG_CANNOT_BE_IMPROVED, MODEL1,
                AuditAction.EntityType.MODEL_GURU, WHITE_LOGS)),
            withLinkData(WHITE_OFFER_ID_3,
                user2Action(PaidAction.YANG_CANNOT_BE_IMPROVED_VERIFICATION, MODEL1,
                    AuditAction.EntityType.MODEL_GURU, WHITE_LOGS)),
            withLinkData(WHITE_OFFER_ID_4,
                user2Action(PaidAction.YANG_CANNOT_BE_IMPROVED_CORRECTION, MODEL2,
                    AuditAction.EntityType.MODEL_GURU, WHITE_LOGS))
        );
    }

    @Test
    public void testBillMatchingWithSku() {
        List<YangBillingAction> billingActions = YangTaskBillingHelper.processRawStatistic(
            new RawStatistics(tomorrow, YangLogStorage.YangLogStoreRequest.newBuilder()
                .setHitmanId(TASK_ID)
                .setTaskType(YangLogStorage.YangTaskType.WHITE_LOGS)
                .setCategoryId(CATEGORY_ID)
                .setContractorInfo(YangLogStorage.OperatorInfo.newBuilder().setUid(USER1).build())
                .setInspectorInfo(YangLogStorage.OperatorInfo.newBuilder().setUid(USER2).build())
                .addMatchingStatistic(YangLogStorage.MatchingStatistic.newBuilder()
                    .setModelId(MODEL3).setOfferId(WHITE_OFFER_ID_1).setUid(USER1)
                    .setMarketSkuId(SKU1)
                    .setOfferStatus(YangLogStorage.MatchingStatus.CANNOT_BE_IMPROVED).build())
                .addMatchingStatistic(YangLogStorage.MatchingStatistic.newBuilder()
                    .setModelId(MODEL3).setOfferId(WHITE_OFFER_ID_2).setUid(USER1)
                    .setMarketSkuId(SKU2)
                    .setOfferStatus(YangLogStorage.MatchingStatus.TRASH_OFFER).build())
                .addMatchingStatistic(YangLogStorage.MatchingStatistic.newBuilder()
                    .setModelId(MODEL3).setOfferId(WHITE_OFFER_ID_2).setUid(USER2)
                    .setMarketSkuId(SKU2)
                    .setOfferStatus(YangLogStorage.MatchingStatus.CANNOT_BE_IMPROVED).build())
                .addMatchingStatistic(YangLogStorage.MatchingStatistic.newBuilder()
                    .setOfferId(WHITE_OFFER_ID_3).setUid(USER1)
                    .setOfferStatus(YangLogStorage.MatchingStatus.SKUTCHED).build())
                .addMatchingStatistic(YangLogStorage.MatchingStatistic.newBuilder()
                    .setOfferId(WHITE_OFFER_ID_4).setUid(USER1)
                    .setOfferStatus(YangLogStorage.MatchingStatus.POSTPONE).build())
                .build())
        );

        assertThat(billingActions).containsExactlyInAnyOrder(
            withLinkData(WHITE_OFFER_ID_1, user1Action(PaidAction.YANG_WHITE_LOGS_SKU_MAPPING,
                SKU1, AuditAction.EntityType.MODEL_SKU, WHITE_LOGS)),
            withLinkData(WHITE_OFFER_ID_1,
                user2Action(PaidAction.YANG_WHITE_LOGS_SKU_MAPPING_VERIFICATION, SKU1,
                    AuditAction.EntityType.MODEL_SKU, WHITE_LOGS)),
            withLinkData(WHITE_OFFER_ID_2,
                user2Action(PaidAction.YANG_WHITE_LOGS_SKU_MAPPING_CORRECTION, SKU2,
                    AuditAction.EntityType.MODEL_SKU, WHITE_LOGS))
        );
    }

    @Test
    public void testMappingModeration() {
        List<YangBillingAction> billingActions = YangTaskBillingHelper.processRawStatistic(
            new RawStatistics(tomorrow, YangLogStorage.YangLogStoreRequest.newBuilder()
                .setHitmanId(TASK_ID)
                .setTaskType(YangLogStorage.YangTaskType.MAPPING_MODERATION)
                .setCategoryId(CATEGORY_ID)
                .setContractorInfo(YangLogStorage.OperatorInfo.newBuilder().setUid(USER1))
                .addMappingModerationStatistic(YangLogStorage.MappingModerationStatistic.newBuilder()
                    .setMarketSkuId(SKU1)
                    .setOfferId(OFFER_ID_1)
                    .setMappingModerationStatus(YangLogStorage.MappingModerationStatus.ACCEPTED)
                    .setMappingModerationType(YangLogStorage.MappingModerationType.COMMON)
                    .setUid(USER1))
                .addMappingModerationStatistic(YangLogStorage.MappingModerationStatistic.newBuilder()
                    .setMarketSkuId(SKU1)
                    .setOfferId(OFFER_ID_2)
                    .setMappingModerationStatus(YangLogStorage.MappingModerationStatus.REJECTED)
                    .setMappingModerationType(YangLogStorage.MappingModerationType.DEDUPLICATION)
                    .setUid(USER1))
                .addMappingModerationStatistic(YangLogStorage.MappingModerationStatistic.newBuilder()
                    .setMarketSkuId(SKU1)
                    .setOfferId(OFFER_ID_3)
                    .setMappingModerationStatus(YangLogStorage.MappingModerationStatus.NEED_INFO)
                    .setMappingModerationType(YangLogStorage.MappingModerationType.RECHECK)
                    .setUid(USER1))
                .build())
        );
        assertThat(billingActions).containsExactlyInAnyOrder(
            withLinkData(OFFER_ID_1,
                user1Action(PaidAction.YANG_MAPPING_MODERATION_COMMON_ACCEPTED, SKU1,
                    AuditAction.EntityType.MODEL_SKU,
                    MAPPING_MODERATION)),
            withLinkData(OFFER_ID_2,
                user1Action(PaidAction.YANG_MAPPING_MODERATION_DEDUPLICATION_REJECTED, SKU1,
                    AuditAction.EntityType.MODEL_SKU,
                    MAPPING_MODERATION)),
            withLinkData(OFFER_ID_3,
                user1Action(PaidAction.YANG_MAPPING_MODERATION_RECHECK_NEED_INFO, SKU1,
                    AuditAction.EntityType.MODEL_SKU,
                    MAPPING_MODERATION))
        );
    }

    @Test
    public void testMappingModerationToPsku() {
        List<YangBillingAction> billingActions = YangTaskBillingHelper.processRawStatistic(
            new RawStatistics(tomorrow, YangLogStorage.YangLogStoreRequest.newBuilder()
                .setHitmanId(TASK_ID)
                .setTaskType(YangLogStorage.YangTaskType.MAPPING_MODERATION_TO_PSKU)
                .setCategoryId(CATEGORY_ID)
                .setContractorInfo(YangLogStorage.OperatorInfo.newBuilder().setUid(USER1))
                .addMappingModerationStatistic(YangLogStorage.MappingModerationStatistic.newBuilder()
                    .setMarketSkuId(SKU1)
                    .setOfferId(OFFER_ID_1)
                    .setMappingModerationStatus(YangLogStorage.MappingModerationStatus.ACCEPTED)
                    .setMappingModerationType(YangLogStorage.MappingModerationType.COMMON)
                    .setUid(USER1))
                .addMappingModerationStatistic(YangLogStorage.MappingModerationStatistic.newBuilder()
                    .setMarketSkuId(SKU1)
                    .setOfferId(OFFER_ID_2)
                    .setMappingModerationStatus(YangLogStorage.MappingModerationStatus.REJECTED)
                    .setMappingModerationType(YangLogStorage.MappingModerationType.COMMON)
                    .setUid(USER1))
                .addMappingModerationStatistic(YangLogStorage.MappingModerationStatistic.newBuilder()
                    .setMarketSkuId(SKU1)
                    .setOfferId(OFFER_ID_3)
                    .setMappingModerationStatus(YangLogStorage.MappingModerationStatus.NEED_INFO)
                    .setMappingModerationType(YangLogStorage.MappingModerationType.COMMON)
                    .setBadCard(false)
                    .setUid(USER1))
                .addMappingModerationStatistic(YangLogStorage.MappingModerationStatistic.newBuilder()
                    .setMarketSkuId(SKU1)
                    .setOfferId(OFFER_ID_4)
                    .setMappingModerationStatus(YangLogStorage.MappingModerationStatus.NEED_INFO)
                    .setMappingModerationType(YangLogStorage.MappingModerationType.COMMON)
                    .setBadCard(true)
                    .setUid(USER1))
                .addMappingModerationStatistic(YangLogStorage.MappingModerationStatistic.newBuilder()
                    .setMarketSkuId(SKU2)
                    .setOfferId(OFFER_ID_5)
                    .setMappingModerationStatus(YangLogStorage.MappingModerationStatus.REJECTED)
                    .setMappingModerationType(YangLogStorage.MappingModerationType.COMMON)
                    .setBadCard(true)
                    .setUid(USER1))
                .build())
        );
        assertThat(billingActions).containsExactlyInAnyOrder(
            withLinkData(OFFER_ID_1,
                user1Action(PaidAction.YANG_MAPPING_MODERATION_COMMON_ACCEPTED, SKU1, AuditAction.EntityType.MODEL_SKU,
                    MAPPING_MODERATION_TO_PSKU)),
            withLinkData(OFFER_ID_2,
                user1Action(PaidAction.YANG_MAPPING_MODERATION_COMMON_REJECTED, SKU1, AuditAction.EntityType.MODEL_SKU,
                    MAPPING_MODERATION_TO_PSKU)),
            withLinkData(OFFER_ID_3,
                user1Action(PaidAction.YANG_MAPPING_MODERATION_COMMON_NEED_INFO, SKU1,
                    AuditAction.EntityType.MODEL_SKU,
                    MAPPING_MODERATION_TO_PSKU)),
            withLinkData(OFFER_ID_4,
                user1Action(PaidAction.YANG_MAPPING_MODERATION_COMMON_NEED_INFO, SKU1,
                    AuditAction.EntityType.MODEL_SKU,
                    MAPPING_MODERATION_TO_PSKU)),
            user1Action(PaidAction.YANG_MAPPING_MODERATION_COMMON_BAD_CARD, SKU1, AuditAction.EntityType.MODEL_SKU,
                MAPPING_MODERATION_TO_PSKU),
            user1Action(PaidAction.YANG_MAPPING_MODERATION_COMMON_BAD_CARD, SKU2, AuditAction.EntityType.MODEL_SKU,
                MAPPING_MODERATION_TO_PSKU)
        );
    }


    @Test
    public void testBillMatchingNoCard() {
        List<YangBillingAction> billingActions = YangTaskBillingHelper.processRawStatistic(
            new RawStatistics(tomorrow, YangLogStorage.YangLogStoreRequest.newBuilder()
                .setHitmanId(TASK_ID)
                .setTaskType(YangLogStorage.YangTaskType.WHITE_LOGS)
                .setCategoryId(CATEGORY_ID)
                .setContractorInfo(YangLogStorage.OperatorInfo.newBuilder().setUid(USER1).build())
                .setInspectorInfo(YangLogStorage.OperatorInfo.newBuilder().setUid(USER2).build())
                .addMatchingStatistic(YangLogStorage.MatchingStatistic.newBuilder()
                    .setOfferId(WHITE_OFFER_ID_1).setUid(USER1)
                    .setOfferStatus(YangLogStorage.MatchingStatus.NO_MATCHING_CARD).build())
                .addMatchingStatistic(YangLogStorage.MatchingStatistic.newBuilder()
                    .setOfferId(WHITE_OFFER_ID_2).setUid(USER1)
                    .setOfferStatus(YangLogStorage.MatchingStatus.CANNOT_BE_IMPROVED).build())
                .addMatchingStatistic(YangLogStorage.MatchingStatistic.newBuilder()
                    .setOfferId(WHITE_OFFER_ID_2).setUid(USER2)
                    .setMarketSkuId(SKU1)
                    .setOfferStatus(YangLogStorage.MatchingStatus.NO_MATCHING_CARD).build())
                .build())
        );

        assertThat(billingActions).containsExactlyInAnyOrder(
            withLinkData(WHITE_OFFER_ID_1, user1Action(PaidAction.YANG_NO_MATCHING_CARD, 0L,
                null, WHITE_LOGS)),
            withLinkData(WHITE_OFFER_ID_1, user2Action(PaidAction.YANG_NO_MATCHING_CARD_VERIFICATION, 0L,
                null, WHITE_LOGS)),
            withLinkData(WHITE_OFFER_ID_2, user2Action(PaidAction.YANG_NO_MATCHING_CARD_CORRECTION, SKU1,
                AuditAction.EntityType.MODEL_SKU, WHITE_LOGS))
        );
    }

    @Test
    public void testBillOnlySuperOperatorMappingsWithCorrection() {
        List<YangBillingAction> billingActions = YangTaskBillingHelper.getSuperOperatorCorrectionsBillingAction(
            new RawStatistics(tomorrow, YangLogStorage.YangLogStoreRequest.newBuilder()
                .setHitmanId(TASK_ID)
                .setTaskType(YangLogStorage.YangTaskType.BLUE_LOGS)
                .setCategoryId(CATEGORY_ID)
                .setContractorInfo(YangLogStorage.OperatorInfo.newBuilder().setUid(USER1).build())
                .setInspectorInfo(YangLogStorage.OperatorInfo.newBuilder().setUid(USER2).build())
                .addMappingStatistic(YangLogStorage.MappingStatistic.newBuilder()
                    .setMarketSkuId(SKU2).setOfferId(OFFER_ID_1).setUid(USER1)
                    .setOfferMappingStatus(YangLogStorage.MappingStatus.MAPPED).build())
                .addMappingStatistic(YangLogStorage.MappingStatistic.newBuilder()
                    .setMarketSkuId(SKU2).setOfferId(OFFER_ID_2).setUid(USER2)
                    .setOfferMappingStatus(YangLogStorage.MappingStatus.TRASH).build())
                .addMappingStatistic(YangLogStorage.MappingStatistic.newBuilder()
                    .setMarketSkuId(SKU2).setOfferId(OFFER_ID_3).setUid(USER2)
                    .setOfferMappingStatus(YangLogStorage.MappingStatus.WRONG_CATEGORY).build())
                .build())
        );

        assertThat(billingActions).containsExactlyInAnyOrder(
            withLinkData(OFFER_ID_2, user2Sku2Action(PaidAction.YANG_TRASH_MAPPING_CORRECTION, BLUE_LOGS)),
            withLinkData(OFFER_ID_3, user2Sku2Action(PaidAction.YANG_TRASH_MAPPING_CORRECTION, BLUE_LOGS))
        );
    }

    @Test
    public void testGuruCreated() {
        List<YangBillingAction> billingActions = YangTaskBillingHelper.processRawStatistic(
            new RawStatistics(tomorrow, YangLogStorage.YangLogStoreRequest.newBuilder()
                .setHitmanId(TASK_ID)
                .setTaskType(YangLogStorage.YangTaskType.BLUE_LOGS)
                .setCategoryId(CATEGORY_ID)
                .setContractorInfo(YangLogStorage.OperatorInfo.newBuilder().setUid(USER1).build())
                .setInspectorInfo(YangLogStorage.OperatorInfo.newBuilder().setUid(USER2).build())
                .addModelStatistic(YangLogStorage.ModelStatistic.newBuilder()
                    .setModelId(MODEL1)
                    .setType(ModelStorage.ModelType.GURU)
                    .setCreatedInTask(true)
                    .setCreatedByUid(USER1)
                    .build())
                .addModelStatistic(YangLogStorage.ModelStatistic.newBuilder()
                    .setModelId(MODEL2)
                    .setType(ModelStorage.ModelType.GURU)
                    .setCreatedInTask(true)
                    .setCreatedByUid(USER2)
                    .build())
                .addModelStatistic(YangLogStorage.ModelStatistic.newBuilder()
                    .setModelId(MODEL3)
                    .setType(ModelStorage.ModelType.SKU)
                    .setCreatedInTask(false)
                    .build())
                .build())
        );

        assertThat(billingActions).containsExactlyInAnyOrder(
            user1Model1Action(PaidAction.YANG_CREATE_MODEL, BLUE_LOGS),
            user2Model1Action(PaidAction.YANG_CREATE_MODEL_VERIFICATION, BLUE_LOGS),
            user2Model2Action(PaidAction.YANG_CREATE_MODEL, BLUE_LOGS)
        );
    }

    @Test
    public void testSkuCreated() {
        List<YangBillingAction> billingActions = YangTaskBillingHelper.processRawStatistic(
            new RawStatistics(tomorrow, YangLogStorage.YangLogStoreRequest.newBuilder()
                .setHitmanId(TASK_ID)
                .setTaskType(YangLogStorage.YangTaskType.BLUE_LOGS)
                .setCategoryId(CATEGORY_ID)
                .setContractorInfo(YangLogStorage.OperatorInfo.newBuilder().setUid(USER1).build())
                .setInspectorInfo(YangLogStorage.OperatorInfo.newBuilder().setUid(USER2).build())
                .addModelStatistic(YangLogStorage.ModelStatistic.newBuilder()
                    .setModelId(SKU1)
                    .setType(ModelStorage.ModelType.SKU)
                    .setCreatedInTask(true)
                    .setCreatedByUid(USER1)
                    .build())
                .addModelStatistic(YangLogStorage.ModelStatistic.newBuilder()
                    .setModelId(SKU2)
                    .setType(ModelStorage.ModelType.SKU)
                    .setCreatedInTask(true)
                    .setCreatedByUid(USER2)
                    .build())
                .addModelStatistic(YangLogStorage.ModelStatistic.newBuilder()
                    .setModelId(SKU3)
                    .setType(ModelStorage.ModelType.SKU)
                    .setCreatedInTask(false)
                    .build())
                .build())
        );

        assertThat(billingActions).containsExactlyInAnyOrder(
            user1Sku1Action(PaidAction.YANG_CREATE_SKU, BLUE_LOGS),
            user2Sku1Action(PaidAction.YANG_CREATE_SKU_VERIFICATION, BLUE_LOGS),
            user2Sku2Action(PaidAction.YANG_CREATE_SKU, BLUE_LOGS)
        );
    }

    @Test
    public void testBillContractorActions() {
        List<YangBillingAction> billingActions = YangTaskBillingHelper.processRawStatistic(
            new RawStatistics(tomorrow, YangLogStorage.YangLogStoreRequest.newBuilder()
                .setHitmanId(TASK_ID)
                .setTaskType(YangLogStorage.YangTaskType.BLUE_LOGS)
                .setCategoryId(CATEGORY_ID)
                .setContractorInfo(YangLogStorage.OperatorInfo.newBuilder().setUid(USER1).build())
                .setInspectorInfo(YangLogStorage.OperatorInfo.newBuilder().setUid(USER2).build())
                .addModelStatistic(YangLogStorage.ModelStatistic.newBuilder()
                    .setModelId(MODEL1)
                    .setType(ModelStorage.ModelType.GURU)
                    .setContractorChanges(YangLogStorage.ActionCount.newBuilder()
                        .setAliases(2)
                        .setBarCode(1)
                        .setCutOffWord(1)
                        .setIsSku(1)
                        .setParam(1)
                        .addParamIds(100L)
                        .setPickerAdded(1)
                        .setPictureUploaded(1)
                        .setPictureCopied(2)
                        .setVendorCode(1)
                        .build())
                    .build())
                .build())
        );

        assertThat(billingActions).containsExactlyInAnyOrder(
            user1Model1Action(PaidAction.YANG_ADD_ALIAS, BLUE_LOGS),
            user1Model1Action(PaidAction.YANG_ADD_ALIAS, BLUE_LOGS),
            user1Model1Action(PaidAction.YANG_ADD_VENDORCODE_OR_BARCODE, BLUE_LOGS),
            user1Model1Action(PaidAction.YANG_ADD_VENDORCODE_OR_BARCODE, BLUE_LOGS),
            user1Model1Action(PaidAction.YANG_ADD_CUT_OFF_WORD, BLUE_LOGS),
            user1Model1Action(PaidAction.YANG_MARK_MODEL_AS_SKU, BLUE_LOGS),
            user1Model1Action(PaidAction.YANG_ADD_PARAM_VALUE, BLUE_LOGS).setParamId(100L),
            user1Model1Action(PaidAction.YANG_ADD_PICKER, BLUE_LOGS),
            user1Model1Action(PaidAction.YANG_UPLOAD_PICTURE, BLUE_LOGS),
            user1Model1Action(PaidAction.YANG_COPY_PICTURE, BLUE_LOGS),
            user1Model1Action(PaidAction.YANG_COPY_PICTURE, BLUE_LOGS),
            user2Model1Action(PaidAction.YANG_ADD_ALIAS_VERIFICATION, BLUE_LOGS),
            user2Model1Action(PaidAction.YANG_ADD_ALIAS_VERIFICATION, BLUE_LOGS),
            user2Model1Action(PaidAction.YANG_ADD_VENDORCODE_OR_BARCODE_VERIFICATION, BLUE_LOGS),
            user2Model1Action(PaidAction.YANG_ADD_VENDORCODE_OR_BARCODE_VERIFICATION, BLUE_LOGS),
            user2Model1Action(PaidAction.YANG_ADD_CUT_OFF_WORD_VERIFICATION, BLUE_LOGS),
            user2Model1Action(PaidAction.YANG_MARK_MODEL_AS_SKU_VERIFICATION, BLUE_LOGS),
            user2Model1Action(PaidAction.YANG_ADD_PARAM_VALUE_VERIFICATION, BLUE_LOGS).setParamId(100L),
            user2Model1Action(PaidAction.YANG_ADD_PICKER_VERIFICATION, BLUE_LOGS),
            user2Model1Action(PaidAction.YANG_UPLOAD_PICTURE_VERIFICATION, BLUE_LOGS),
            user2Model1Action(PaidAction.YANG_COPY_PICTURE_VERIFICATION, BLUE_LOGS),
            user2Model1Action(PaidAction.YANG_COPY_PICTURE_VERIFICATION, BLUE_LOGS)
        );
    }

    @Test
    public void testBillInspectorCorrections() {
        List<YangBillingAction> billingActions = YangTaskBillingHelper.processRawStatistic(
            new RawStatistics(tomorrow, YangLogStorage.YangLogStoreRequest.newBuilder()
                .setHitmanId(TASK_ID)
                .setTaskType(YangLogStorage.YangTaskType.BLUE_LOGS)
                .setCategoryId(CATEGORY_ID)
                .setContractorInfo(YangLogStorage.OperatorInfo.newBuilder().setUid(USER2).build())
                .setInspectorInfo(YangLogStorage.OperatorInfo.newBuilder().setUid(USER1).build())
                .addModelStatistic(YangLogStorage.ModelStatistic.newBuilder()
                    .setModelId(MODEL1)
                    .setType(ModelStorage.ModelType.GURU)
                    .setInspectorCorrections(YangLogStorage.ActionCount.newBuilder()
                        .setAliases(2)
                        .setBarCode(1)
                        .setCutOffWord(1)
                        .setIsSku(1)
                        .setParam(1)
                        .addParamIds(100L)
                        .setPickerAdded(1)
                        .setPictureUploaded(1)
                        .setVendorCode(1)
                        .build())
                    .build())
                .build())
        );

        assertThat(billingActions).containsExactlyInAnyOrder(
            user1Model1Action(PaidAction.YANG_ADD_ALIAS_CORRECTION, BLUE_LOGS),
            user1Model1Action(PaidAction.YANG_ADD_ALIAS_CORRECTION, BLUE_LOGS),
            user1Model1Action(PaidAction.YANG_ADD_VENDORCODE_OR_BARCODE_CORRECTION, BLUE_LOGS),
            user1Model1Action(PaidAction.YANG_ADD_VENDORCODE_OR_BARCODE_CORRECTION, BLUE_LOGS),
            user1Model1Action(PaidAction.YANG_ADD_CUT_OFF_WORD_CORRECTION, BLUE_LOGS),
            user1Model1Action(PaidAction.YANG_MARK_MODEL_AS_SKU_CORRECTION, BLUE_LOGS),
            user1Model1Action(PaidAction.YANG_ADD_PARAM_VALUE_CORRECTION, BLUE_LOGS).setParamId(100L),
            user1Model1Action(PaidAction.YANG_ADD_PICKER_CORRECTION, BLUE_LOGS),
            user1Model1Action(PaidAction.YANG_UPLOAD_PICTURE_CORRECTION, BLUE_LOGS)
        );
    }

    @Test
    public void testBillContractorActionsWithInspectorCorrections() {
        List<YangBillingAction> billingActions = YangTaskBillingHelper.processRawStatistic(
            new RawStatistics(tomorrow, YangLogStorage.YangLogStoreRequest.newBuilder()
                .setHitmanId(TASK_ID)
                .setTaskType(YangLogStorage.YangTaskType.BLUE_LOGS)
                .setCategoryId(CATEGORY_ID)
                .setContractorInfo(YangLogStorage.OperatorInfo.newBuilder().setUid(USER1).build())
                .setInspectorInfo(YangLogStorage.OperatorInfo.newBuilder().setUid(USER2).build())
                .addModelStatistic(YangLogStorage.ModelStatistic.newBuilder()
                    .setModelId(MODEL1)
                    .setType(ModelStorage.ModelType.GURU)
                    .setContractorChanges(YangLogStorage.ActionCount.newBuilder()
                        .setAliases(2)
                        .setBarCode(1)
                        .setCutOffWord(1)
                        .setIsSku(1)
                        .setParam(1)
                        .addParamIds(100L)
                        .setPickerAdded(1)
                        .setPictureUploaded(1)
                        .setPictureCopied(1)
                        .setVendorCode(1)
                        .build())
                    .setInspectorCorrections(YangLogStorage.ActionCount.newBuilder()
                        .setAliases(2)
                        .setBarCode(1)
                        .setCutOffWord(1)
                        .setIsSku(1)
                        .setParam(2)
                        .addAllParamIds(Arrays.asList(100L, 2L))
                        .setPickerAdded(1)
                        .setPictureUploaded(1)
                        .setPictureCopied(1)
                        .setVendorCode(1)
                        .build())
                    .build())
                .build())
        );

        assertThat(billingActions).containsExactlyInAnyOrder(
            user1Model1Action(PaidAction.YANG_ADD_ALIAS, BLUE_LOGS),
            user1Model1Action(PaidAction.YANG_ADD_ALIAS, BLUE_LOGS),
            user1Model1Action(PaidAction.YANG_ADD_VENDORCODE_OR_BARCODE, BLUE_LOGS),
            user1Model1Action(PaidAction.YANG_ADD_VENDORCODE_OR_BARCODE, BLUE_LOGS),
            user1Model1Action(PaidAction.YANG_ADD_CUT_OFF_WORD, BLUE_LOGS),
            user1Model1Action(PaidAction.YANG_MARK_MODEL_AS_SKU, BLUE_LOGS),
            user1Model1Action(PaidAction.YANG_ADD_PARAM_VALUE, BLUE_LOGS).setParamId(100L),
            user1Model1Action(PaidAction.YANG_ADD_PICKER, BLUE_LOGS),
            user1Model1Action(PaidAction.YANG_UPLOAD_PICTURE, BLUE_LOGS),
            user1Model1Action(PaidAction.YANG_COPY_PICTURE, BLUE_LOGS),
            user2Model1Action(PaidAction.YANG_ADD_ALIAS_VERIFICATION, BLUE_LOGS),
            user2Model1Action(PaidAction.YANG_ADD_ALIAS_VERIFICATION, BLUE_LOGS),
            user2Model1Action(PaidAction.YANG_ADD_VENDORCODE_OR_BARCODE_VERIFICATION, BLUE_LOGS),
            user2Model1Action(PaidAction.YANG_ADD_VENDORCODE_OR_BARCODE_VERIFICATION, BLUE_LOGS),
            user2Model1Action(PaidAction.YANG_ADD_CUT_OFF_WORD_VERIFICATION, BLUE_LOGS),
            user2Model1Action(PaidAction.YANG_MARK_MODEL_AS_SKU_VERIFICATION, BLUE_LOGS),
            user2Model1Action(PaidAction.YANG_ADD_PARAM_VALUE_VERIFICATION, BLUE_LOGS).setParamId(100L),
            user2Model1Action(PaidAction.YANG_ADD_PICKER_VERIFICATION, BLUE_LOGS),
            user2Model1Action(PaidAction.YANG_UPLOAD_PICTURE_VERIFICATION, BLUE_LOGS),
            user2Model1Action(PaidAction.YANG_COPY_PICTURE_VERIFICATION, BLUE_LOGS),
            user2Model1Action(PaidAction.YANG_ADD_ALIAS_CORRECTION, BLUE_LOGS),
            user2Model1Action(PaidAction.YANG_ADD_ALIAS_CORRECTION, BLUE_LOGS),
            user2Model1Action(PaidAction.YANG_ADD_VENDORCODE_OR_BARCODE_CORRECTION, BLUE_LOGS),
            user2Model1Action(PaidAction.YANG_ADD_VENDORCODE_OR_BARCODE_CORRECTION, BLUE_LOGS),
            user2Model1Action(PaidAction.YANG_ADD_CUT_OFF_WORD_CORRECTION, BLUE_LOGS),
            user2Model1Action(PaidAction.YANG_MARK_MODEL_AS_SKU_CORRECTION, BLUE_LOGS),
            user2Model1Action(PaidAction.YANG_ADD_PARAM_VALUE_CORRECTION, BLUE_LOGS).setParamId(100L),
            user2Model1Action(PaidAction.YANG_ADD_PARAM_VALUE_CORRECTION, BLUE_LOGS).setParamId(2L),
            user2Model1Action(PaidAction.YANG_ADD_PICKER_CORRECTION, BLUE_LOGS),
            user2Model1Action(PaidAction.YANG_UPLOAD_PICTURE_CORRECTION, BLUE_LOGS),
            user2Model1Action(PaidAction.YANG_COPY_PICTURE_CORRECTION, BLUE_LOGS)
        );
    }

    @Test
    public void testBillOnlyContractorChanges() {
        List<YangBillingAction> billingActions = YangTaskBillingHelper.getOperatorChangesBillingAction(
            new RawStatistics(tomorrow, YangLogStorage.YangLogStoreRequest.newBuilder()
                .setHitmanId(TASK_ID)
                .setCategoryId(CATEGORY_ID)
                .setContractorInfo(YangLogStorage.OperatorInfo.newBuilder().setUid(USER1).build())
                .setInspectorInfo(YangLogStorage.OperatorInfo.newBuilder().setUid(USER2).build())
                .addModelStatistic(YangLogStorage.ModelStatistic.newBuilder()
                    .setModelId(MODEL1)
                    .setType(ModelStorage.ModelType.GURU)
                    .setContractorChanges(YangLogStorage.ActionCount.newBuilder()
                        .setAliases(2)
                        .setBarCode(1)
                        .setCutOffWord(1)
                        .setIsSku(1)
                        .setParam(1)
                        .addParamIds(100L)
                        .setPickerAdded(1)
                        .setPictureUploaded(1)
                        .setVendorCode(1)
                        .build())
                    .setInspectorCorrections(YangLogStorage.ActionCount.newBuilder()
                        .setAliases(2)
                        .setBarCode(1)
                        .setCutOffWord(1)
                        .setIsSku(1)
                        .setParam(1)
                        .addParamIds(100L)
                        .setPickerAdded(1)
                        .setPictureUploaded(1)
                        .setPictureUploaded(2)
                        .setVendorCode(1)
                        .build())
                    .build())
                .build())
        );

        assertThat(billingActions).containsExactlyInAnyOrder(
            user1Model1Action(PaidAction.YANG_ADD_ALIAS, BLUE_LOGS),
            user1Model1Action(PaidAction.YANG_ADD_ALIAS, BLUE_LOGS),
            user1Model1Action(PaidAction.YANG_ADD_VENDORCODE_OR_BARCODE, BLUE_LOGS),
            user1Model1Action(PaidAction.YANG_ADD_VENDORCODE_OR_BARCODE, BLUE_LOGS),
            user1Model1Action(PaidAction.YANG_ADD_CUT_OFF_WORD, BLUE_LOGS),
            user1Model1Action(PaidAction.YANG_MARK_MODEL_AS_SKU, BLUE_LOGS),
            user1Model1Action(PaidAction.YANG_ADD_PARAM_VALUE, BLUE_LOGS).setParamId(100L),
            user1Model1Action(PaidAction.YANG_ADD_PICKER, BLUE_LOGS),
            user1Model1Action(PaidAction.YANG_UPLOAD_PICTURE, BLUE_LOGS)
        );
    }

    @Test
    public void testBillOnlyInspectorCorrection() {
        List<YangBillingAction> billingActions = YangTaskBillingHelper.getSuperOperatorCorrectionsBillingAction(
            new RawStatistics(tomorrow, YangLogStorage.YangLogStoreRequest.newBuilder()
                .setHitmanId(TASK_ID)
                .setCategoryId(CATEGORY_ID)
                .setContractorInfo(YangLogStorage.OperatorInfo.newBuilder().setUid(USER1).build())
                .setInspectorInfo(YangLogStorage.OperatorInfo.newBuilder().setUid(USER2).build())
                .addModelStatistic(YangLogStorage.ModelStatistic.newBuilder()
                    .setModelId(MODEL1)
                    .setType(ModelStorage.ModelType.GURU)
                    .setContractorChanges(YangLogStorage.ActionCount.newBuilder()
                        .setAliases(2)
                        .setBarCode(1)
                        .setCutOffWord(1)
                        .setIsSku(1)
                        .setParam(1)
                        .addParamIds(100L)
                        .setPickerAdded(1)
                        .setPictureUploaded(1)
                        .setVendorCode(1)
                        .build())
                    .setInspectorCorrections(YangLogStorage.ActionCount.newBuilder()
                        .setAliases(2)
                        .setBarCode(1)
                        .setCutOffWord(1)
                        .setIsSku(1)
                        .setParam(1)
                        .addParamIds(100L)
                        .setPickerAdded(1)
                        .setPictureUploaded(1)
                        .setVendorCode(1)
                        .build())
                    .build())
                .build())
        );

        assertThat(billingActions).containsExactlyInAnyOrder(
            user2Model1Action(PaidAction.YANG_ADD_ALIAS_CORRECTION, BLUE_LOGS),
            user2Model1Action(PaidAction.YANG_ADD_ALIAS_CORRECTION, BLUE_LOGS),
            user2Model1Action(PaidAction.YANG_ADD_VENDORCODE_OR_BARCODE_CORRECTION, BLUE_LOGS),
            user2Model1Action(PaidAction.YANG_ADD_VENDORCODE_OR_BARCODE_CORRECTION, BLUE_LOGS),
            user2Model1Action(PaidAction.YANG_ADD_CUT_OFF_WORD_CORRECTION, BLUE_LOGS),
            user2Model1Action(PaidAction.YANG_MARK_MODEL_AS_SKU_CORRECTION, BLUE_LOGS),
            user2Model1Action(PaidAction.YANG_ADD_PARAM_VALUE_CORRECTION, BLUE_LOGS).setParamId(100L),
            user2Model1Action(PaidAction.YANG_ADD_PICKER_CORRECTION, BLUE_LOGS),
            user2Model1Action(PaidAction.YANG_UPLOAD_PICTURE_CORRECTION, BLUE_LOGS)
        );
    }

    @Test
    public void testBillParameters() {
        List<YangBillingAction> billingActions = YangTaskBillingHelper.processRawStatistic(
            new RawStatistics(tomorrow, YangLogStorage.YangLogStoreRequest.newBuilder()
                .setTaskType(YangLogStorage.YangTaskType.BLUE_LOGS)
                .setHitmanId(TASK_ID)
                .setCategoryId(CATEGORY_ID)
                .setContractorInfo(YangLogStorage.OperatorInfo.newBuilder().setUid(USER1).build())
                .setInspectorInfo(YangLogStorage.OperatorInfo.newBuilder().setUid(USER2).build())
                .addParameterStatistic(YangLogStorage.ParameterStatistic.newBuilder()
                    .setEntityId(MODEL1)
                    .addChanges(YangLogStorage.ParameterActions.newBuilder()
                        .setUid(USER1)
                        .setChangesType(YangLogStorage.ChangesType.CONTRACTOR)
                        .setCreatedInTask(true)
                        .setAliases(ALIASES_COUNT)
                        .setCutOffWords(CUT_OFF_COUNT)))
                .addParameterStatistic(YangLogStorage.ParameterStatistic.newBuilder()
                    .setEntityId(MODEL2)
                    .addChanges(YangLogStorage.ParameterActions.newBuilder()
                        .setUid(USER1)
                        .setChangesType(YangLogStorage.ChangesType.CONTRACTOR)
                        .setCreatedInTask(true)
                        .setAliases(ALIASES_COUNT)
                        .setCutOffWords(CUT_OFF_COUNT))
                    .addChanges(YangLogStorage.ParameterActions.newBuilder()
                        .setUid(USER2)
                        .setChangesType(YangLogStorage.ChangesType.INSPECTOR)
                        .setCreatedInTask(false)
                        .setAliases(ALIASES_COUNT)
                        .setCutOffWords(CUT_OFF_COUNT)))
                .addParameterStatistic(YangLogStorage.ParameterStatistic.newBuilder()
                    .setEntityId(MODEL3)
                    .addChanges(YangLogStorage.ParameterActions.newBuilder()
                        .setUid(USER2)
                        .setChangesType(YangLogStorage.ChangesType.INSPECTOR)
                        .setCreatedInTask(true)
                        .setAliases(ALIASES_COUNT)
                        .setCutOffWords(CUT_OFF_COUNT)))
                .build())
        );

        assertThat(billingActions).containsExactlyInAnyOrder(
            user1Action(PaidAction.YANG_CREATE_LOCAL_VENDOR, MODEL1, AuditAction.EntityType.OPTION, BLUE_LOGS),
            user2Action(PaidAction.YANG_CREATE_LOCAL_VENDOR_VERIFICATION, MODEL1,
                AuditAction.EntityType.OPTION, BLUE_LOGS),
            user1Action(PaidAction.YANG_CREATE_LOCAL_VENDOR, MODEL2, AuditAction.EntityType.OPTION, BLUE_LOGS),
            user2Action(PaidAction.YANG_CREATE_LOCAL_VENDOR_VERIFICATION, MODEL2,
                AuditAction.EntityType.OPTION, BLUE_LOGS),
            user2Action(PaidAction.YANG_CREATE_LOCAL_VENDOR, MODEL3, AuditAction.EntityType.OPTION, BLUE_LOGS)
        );
    }

    @Test
    public void testBillParametersWhiteLogs() {
        for (YangLogStorage.YangTaskType type : Arrays.asList(
            YangLogStorage.YangTaskType.DEEPMATCHER_LOGS,
            YangLogStorage.YangTaskType.WHITE_LOGS
        )) {
            List<YangBillingAction> billingActions = YangTaskBillingHelper.processRawStatistic(
                new RawStatistics(tomorrow, YangLogStorage.YangLogStoreRequest.newBuilder()
                    .setTaskType(type)
                    .setHitmanId(TASK_ID)
                    .setCategoryId(CATEGORY_ID)
                    .setContractorInfo(YangLogStorage.OperatorInfo.newBuilder().setUid(USER1).build())
                    .setInspectorInfo(YangLogStorage.OperatorInfo.newBuilder().setUid(USER2).build())
                    .addParameterStatistic(YangLogStorage.ParameterStatistic.newBuilder()
                        .setEntityId(OPTION_ID1)
                        .addChanges(YangLogStorage.ParameterActions.newBuilder()
                            .setUid(USER1)
                            .setChangesType(YangLogStorage.ChangesType.CONTRACTOR)
                            .setCreatedInTask(true)
                            .setAliases(1)
                            .setCutOffWords(2)))
                    .addParameterStatistic(YangLogStorage.ParameterStatistic.newBuilder()
                        .setEntityId(OPTION_ID2)
                        .addChanges(YangLogStorage.ParameterActions.newBuilder()
                            .setUid(USER1)
                            .setChangesType(YangLogStorage.ChangesType.CONTRACTOR)
                            .setCreatedInTask(true)
                            .setCutOffWords(1))
                        .addChanges(YangLogStorage.ParameterActions.newBuilder()
                            .setUid(USER2)
                            .setChangesType(YangLogStorage.ChangesType.INSPECTOR)
                            .setCreatedInTask(false)
                            .setAliases(2))
                        .addChanges(YangLogStorage.ParameterActions.newBuilder()
                            .setUid(USER2)
                            .setChangesType(YangLogStorage.ChangesType.CORRECTIONS)
                            .setCreatedInTask(false)
                            .setAliases(1)
                            .setCutOffWords(2)))
                    .build())
            );

            TaskType taskType = TaskType.convertProto(type);
            assertThat(billingActions).containsExactlyInAnyOrder(
                user1Action(PaidAction.YANG_CREATE_LOCAL_VENDOR, OPTION_ID1, AuditAction.EntityType.OPTION, taskType),
                user2Action(PaidAction.YANG_CREATE_LOCAL_VENDOR_VERIFICATION, OPTION_ID1, AuditAction.EntityType.OPTION,
                    taskType),
                user1Action(PaidAction.YANG_ADD_ALIAS, OPTION_ID1, AuditAction.EntityType.OPTION, taskType),
                user1Action(PaidAction.YANG_ADD_CUT_OFF_WORD, OPTION_ID1, AuditAction.EntityType.OPTION, taskType),
                user1Action(PaidAction.YANG_ADD_CUT_OFF_WORD, OPTION_ID1, AuditAction.EntityType.OPTION, taskType),
                user2Action(PaidAction.YANG_ADD_ALIAS_VERIFICATION, OPTION_ID1, AuditAction.EntityType.OPTION,
                    taskType),
                user2Action(PaidAction.YANG_ADD_CUT_OFF_WORD_VERIFICATION, OPTION_ID1, AuditAction.EntityType.OPTION,
                    taskType),
                user2Action(PaidAction.YANG_ADD_CUT_OFF_WORD_VERIFICATION, OPTION_ID1, AuditAction.EntityType.OPTION,
                    taskType),

                user1Action(PaidAction.YANG_ADD_CUT_OFF_WORD, OPTION_ID2, AuditAction.EntityType.OPTION, taskType),
                user2Action(PaidAction.YANG_ADD_CUT_OFF_WORD_VERIFICATION, OPTION_ID2, AuditAction.EntityType.OPTION,
                    taskType),
                user2Action(PaidAction.YANG_ADD_ALIAS, OPTION_ID2, AuditAction.EntityType.OPTION, taskType),
                user2Action(PaidAction.YANG_ADD_ALIAS, OPTION_ID2, AuditAction.EntityType.OPTION, taskType),
                user2Action(PaidAction.YANG_ADD_ALIAS_CORRECTION, OPTION_ID2, AuditAction.EntityType.OPTION, taskType),
                user2Action(PaidAction.YANG_ADD_CUT_OFF_WORD_CORRECTION, OPTION_ID2, AuditAction.EntityType.OPTION,
                    taskType),
                user2Action(PaidAction.YANG_ADD_CUT_OFF_WORD_CORRECTION, OPTION_ID2, AuditAction.EntityType.OPTION,
                    taskType),
                user1Action(PaidAction.YANG_CREATE_LOCAL_VENDOR, OPTION_ID2, AuditAction.EntityType.OPTION, taskType),
                user2Action(PaidAction.YANG_CREATE_LOCAL_VENDOR_VERIFICATION, OPTION_ID2, AuditAction.EntityType.OPTION,
                    taskType)
            );
        }
    }

    @Test
    public void testBillCategoryStatistics() {
        List<YangBillingAction> billingActions = YangTaskBillingHelper.processRawStatistic(
            new RawStatistics(tomorrow, YangLogStorage.YangLogStoreRequest.newBuilder()
                .setTaskType(YangLogStorage.YangTaskType.WHITE_LOGS)
                .setHitmanId(TASK_ID)
                .setCategoryId(CATEGORY_ID)
                .setContractorInfo(YangLogStorage.OperatorInfo.newBuilder().setUid(USER1).build())
                .setInspectorInfo(YangLogStorage.OperatorInfo.newBuilder().setUid(USER2).build())
                .setCategoryStatistic(YangLogStorage.CategoryStatistic.newBuilder()
                    .setContractorChanges(
                        YangLogStorage.CategoryActions.newBuilder()
                            .setCutOffWords(1)
                    )
                    .setInspectorChanges(
                        YangLogStorage.CategoryActions.newBuilder()
                            .setCutOffWords(2)
                    )
                    .setInspectorCorrections(
                        YangLogStorage.CategoryActions.newBuilder()
                            .setCutOffWords(1 + 2)
                    ))
                .build())
        );

        assertThat(billingActions).containsExactlyInAnyOrder(
            user1Action(PaidAction.YANG_ADD_CUT_OFF_WORD, CATEGORY_ID, AuditAction.EntityType.CATEGORY, WHITE_LOGS),
            user2Action(PaidAction.YANG_ADD_CUT_OFF_WORD_VERIFICATION, CATEGORY_ID,
                AuditAction.EntityType.CATEGORY, WHITE_LOGS),
            user2Action(PaidAction.YANG_ADD_CUT_OFF_WORD, CATEGORY_ID,
                AuditAction.EntityType.CATEGORY, WHITE_LOGS),
            user2Action(PaidAction.YANG_ADD_CUT_OFF_WORD, CATEGORY_ID,
                AuditAction.EntityType.CATEGORY, WHITE_LOGS),
            user2Action(PaidAction.YANG_ADD_CUT_OFF_WORD_CORRECTION, CATEGORY_ID,
                AuditAction.EntityType.CATEGORY, WHITE_LOGS),
            user2Action(PaidAction.YANG_ADD_CUT_OFF_WORD_CORRECTION, CATEGORY_ID,
                AuditAction.EntityType.CATEGORY, WHITE_LOGS),
            user2Action(PaidAction.YANG_ADD_CUT_OFF_WORD_CORRECTION, CATEGORY_ID,
                AuditAction.EntityType.CATEGORY, WHITE_LOGS)
        );
    }

    @Test
    public void testSkuParametersConflictStatisticConflictResolve() {
        List<YangBillingAction> billingActions = YangTaskBillingHelper.processRawStatistic(
            new RawStatistics(tomorrow, YangLogStorage.YangLogStoreRequest.newBuilder()
                .setTaskType(YangLogStorage.YangTaskType.SKU_PARAMETERS_CONFLICT)
                .setHitmanId(TASK_ID)
                .setCategoryId(CATEGORY_ID)
                .setContractorInfo(YangLogStorage.OperatorInfo.newBuilder().setUid(USER1).build())
                .addModelStatistic(YangLogStorage.ModelStatistic.newBuilder()
                    .setModelId(MODEL1)
                    .setType(ModelStorage.ModelType.GURU)
                    .setCreatedInTask(true)
                    .setCreatedByUid(USER1)
                    .setContractorActions(YangLogStorage.ModelActions.newBuilder()
                        .addParamMetaData(
                            YangLogStorage.ActionInfo.newBuilder()
                                .setAuditActionId(-1)
                                .setEntityId(1)
                                .build()
                        )
                        .addBarCode(
                            YangLogStorage.ActionInfo.newBuilder()
                                .setAuditActionId(-1)
                                .setEntityId(1)
                                .build()
                        )
                        .addBarCode(
                            YangLogStorage.ActionInfo.newBuilder()
                                .setAuditActionId(-1)
                                .setEntityId(1)
                                .build()
                        )
                    ).build())
                .addSkuParameterConflictStatistic(
                    YangLogStorage.SkuParameterConflictStatistic.newBuilder()
                        .addParameterConflicts(
                            YangLogStorage.ParameterConflict.newBuilder()
                                .setParameterId(PARAMETER_ID)
                                .setDecision(YangLogStorage.ParameterConflictDecision.CONFLICT_RESOLVED)
                                .build()
                        ).setModelId(MODEL1)
                        .setUid(USER1)
                        .build()
                ).build()
            ));

        assertThat(billingActions).containsExactlyInAnyOrder(
            user1Action(PaidAction.YANG_CONFLICTS_RESOLVE_CONFLICT, MODEL1,
                AuditAction.EntityType.MODEL_SKU, SKU_PARAMETERS_CONFLICT)
                .setParamId(PARAMETER_ID),
            user1Action(PaidAction.YANG_ADD_VENDORCODE_OR_BARCODE, MODEL1,
                AuditAction.EntityType.MODEL_GURU, SKU_PARAMETERS_CONFLICT)
                .setParamId(PARAMETER_ID),
            user1Action(PaidAction.YANG_ADD_VENDORCODE_OR_BARCODE, MODEL1,
                AuditAction.EntityType.MODEL_GURU, SKU_PARAMETERS_CONFLICT)
                .setParamId(PARAMETER_ID)
        );
    }

    @Test
    public void testSkuParametersConflictStatistic() {
        List<YangBillingAction> billingActions = YangTaskBillingHelper.processRawStatistic(
            new RawStatistics(tomorrow, YangLogStorage.YangLogStoreRequest.newBuilder()
                .setTaskType(YangLogStorage.YangTaskType.SKU_PARAMETERS_CONFLICT)
                .setHitmanId(TASK_ID)
                .setCategoryId(CATEGORY_ID)
                .setContractorInfo(YangLogStorage.OperatorInfo.newBuilder().setUid(USER1).build())
                .addModelStatistic(YangLogStorage.ModelStatistic.newBuilder()
                    .setModelId(MODEL1)
                    .setType(ModelStorage.ModelType.GURU)
                    .setCreatedInTask(true)
                    .setCreatedByUid(USER1)
                    .setContractorActions(YangLogStorage.ModelActions.newBuilder()
                        .addParamMetaData(
                            YangLogStorage.ActionInfo.newBuilder()
                                .setAuditActionId(-1)
                                .setEntityId(1)
                                .build()
                        ))
                    .build())
                .addSkuParameterConflictStatistic(
                    YangLogStorage.SkuParameterConflictStatistic.newBuilder()
                        .addParameterConflicts(
                            YangLogStorage.ParameterConflict.newBuilder()
                                .setParameterId(PARAMETER_ID)
                                .setDecision(YangLogStorage.ParameterConflictDecision.CONFLICT_NEED_INFO)
                                .build()
                        ).setModelId(MODEL1)
                        .setUid(USER1)
                        .build()
                ).build()
            ));

        assertThat(billingActions).containsExactlyInAnyOrder(
            user1Action(PaidAction.YANG_CONFLICTS_NEED_INFO, MODEL1,
                AuditAction.EntityType.MODEL_SKU, SKU_PARAMETERS_CONFLICT)
                .setParamId(PARAMETER_ID)
        );
    }

    @Test
    public void testSkuParametersConflictStatisticRemovedMultivalueParams() throws InvalidProtocolBufferException {
        String changedRequest = "{\"hitman_id\": 1234,\"task_type\": \"SKU_PARAMETERS_CONFLICT\",\"category_id\": 1," +
            "\"contractor_info\": {\"uid\": 10101,\"task_suite_created_date\": \"1970-01-01T00:00:00.000\"}," +
            "\"model_statistic\": [{\"type\": \"GURU\",\"model_id\": 1,\"created_in_task\": false," +
            "\"contractor_actions\": {\"param\": [{\"audit_action_id\": 6,\"entity_id\": 1}],\"removed_param\": " +
            "[{\"audit_action_id\": 4,\"entity_id\": 1},{\"audit_action_id\": 5,\"entity_id\": 1}]," +
            "\"param_meta_data\": [{\"audit_action_id\": 8,\"entity_id\": 1}]},\"is_sku\": false}]," +
            "\"category_statistic\": {\"contractor_changes\": {},\"inspector_changes\": {},\"inspector_corrections\":" +
            " {}},\"sku_parameter_conflict_statistic\": [{\"uid\": 10101,\"model_id\": 1,\"parameter_conflicts\": " +
            "[{\"parameter_id\": 1,\"decision\": \"CONFLICT_RESOLVED\"}]}]}";
        YangLogStorage.YangLogStoreRequest.Builder oldBuilder =
            YangLogStorage.YangLogStoreRequest.newBuilder();
        JsonFormat.parser().merge(changedRequest, oldBuilder);
        RawStatistics rawStatistics = new RawStatistics(new Date(), oldBuilder.build());

        List<YangBillingAction> yangBillingActions = YangTaskBillingHelper.processRawStatistic(rawStatistics);

        assertThat(yangBillingActions).map(YangBillingAction::getPaidAction).containsExactlyInAnyOrder(
            PaidAction.YANG_CONFLICTS_RESOLVE_CONFLICT,
            PaidAction.YANG_CONFLICTS_ADD_PARAM_VALUE,
            PaidAction.YANG_CONFLICTS_REMOVE_PARAM_VALUE,
            PaidAction.YANG_CONFLICTS_REMOVE_PARAM_VALUE
        );
    }

    @Test
    public void testSkuParametersConflictStatisticRemovedBarCodeAddAndRemove() throws InvalidProtocolBufferException {
        String changedRequest = "{\"id\":\"m3_2120533\",\"hitman_id\":102120533," +
            "\"task_type\":\"SKU_PARAMETERS_CONFLICT\",\"category_id\":7812208," +
            "\"contractor_info\":{\"uid\":878616658,\"pool_id\":\"798\"," +
            "\"task_id\":\"2fbf91e1-39bd-4cf6-a9b8-c16f9d9b132d\"," +
            "\"assignment_id\":\"00000c8c39--62bc065beb19111064595892\",\"pool_name\":\"Конфликты в параметрах sku\"," +
            "\"task_suite_created_date\":\"2022-06-02T12:37:22.218267Z\"},\"model_statistic\":[{\"type\":\"SKU\"," +
            "\"model_id\":100870823496,\"created_in_task\":false," +
            "\"contractor_actions\":{\"bar_code\":[{\"audit_action_id\":1579010004,\"entity_id\":14202862}]," +
            "\"removed_param\":[{\"audit_action_id\":1579009798,\"entity_id\":14202862}]," +
            "\"param_meta_data\":[{\"audit_action_id\":1579010005,\"entity_id\":14202862}," +
            "{\"audit_action_id\":1579009800,\"entity_id\":17838024}]},\"is_sku\":false}]," +
            "\"category_statistic\":{\"contractor_changes\":{},\"inspector_changes\":{}," +
            "\"inspector_corrections\":{}},\"sku_parameter_conflict_statistic\":[{\"uid\":878616658," +
            "\"model_id\":100870823496,\"parameter_conflicts\":[{\"parameter_id\":14202862," +
            "\"decision\":\"CONFLICT_NEED_INFO\"}]}]}";
        YangLogStorage.YangLogStoreRequest.Builder oldBuilder =
            YangLogStorage.YangLogStoreRequest.newBuilder();
        JsonFormat.parser().merge(changedRequest, oldBuilder);
        RawStatistics rawStatistics = new RawStatistics(new Date(), oldBuilder.build());

        List<YangBillingAction> yangBillingActions = YangTaskBillingHelper.processRawStatistic(rawStatistics);

        assertThat(yangBillingActions).map(YangBillingAction::getPaidAction).containsExactlyInAnyOrder(
            PaidAction.YANG_CONFLICTS_NEED_INFO,
            PaidAction.YANG_ADD_VENDORCODE_OR_BARCODE,
            PaidAction.YANG_CONFLICTS_REMOVE_PARAM_VALUE
        );
    }

    @Test
    public void testSkuParametersConflictStatisticHypothesisBilled() throws InvalidProtocolBufferException {
        String changedRequest = "{\"hitman_id\": 1234,\"task_type\": \"SKU_PARAMETERS_CONFLICT\",\"category_id\": 1," +
            "\"contractor_info\": {\"uid\": 10101,\"task_suite_created_date\": \"1970-01-01T00:00:00.000\"}," +
            "\"model_statistic\": [{\"type\": \"GURU\",\"model_id\": 1,\"created_in_task\": false," +
            "\"contractor_actions\": {\"param_hypothesis\": [{\"audit_action_id\": 3,\"entity_id\": 777}]," +
            "\"param_meta_data\": [{\"audit_action_id\": 4,\"entity_id\": 777}]},\"is_sku\": false}]," +
            "\"category_statistic\": {\"contractor_changes\": {},\"inspector_changes\": {},\"inspector_corrections\":" +
            " {}},\"sku_parameter_conflict_statistic\": [{\"uid\": 10101,\"model_id\": 1,\"parameter_conflicts\": " +
            "[{\"parameter_id\": 777,\"decision\": \"CONFLICT_RESOLVED\"}]}]}";
        YangLogStorage.YangLogStoreRequest.Builder oldBuilder =
            YangLogStorage.YangLogStoreRequest.newBuilder();
        JsonFormat.parser().merge(changedRequest, oldBuilder);
        RawStatistics rawStatistics = new RawStatistics(new Date(), oldBuilder.build());

        List<YangBillingAction> yangBillingActions = YangTaskBillingHelper.processRawStatistic(rawStatistics);

        assertThat(yangBillingActions).map(YangBillingAction::getPaidAction).containsExactlyInAnyOrder(
            PaidAction.YANG_CONFLICTS_RESOLVE_CONFLICT,
            PaidAction.YANG_CONFLICTS_ADD_PARAM_HYPOTHESIS
        );
    }

    @Test
    public void testSkuParametersConflictStatisticHypothesisRemoveBilled() throws InvalidProtocolBufferException {
        // json generated from test
        // ModelAuditStatisticsServiceImplTest::testComputeParamMetadataChangesParamRemoveHypothesis()
        String changedRequest = "{\"hitman_id\": 1234,\"task_type\": \"SKU_PARAMETERS_CONFLICT\",\"category_id\": 1," +
            "\"contractor_info\": {\"uid\": 10101,\"task_suite_created_date\": \"1970-01-01T00:00:00.000\"}," +
            "\"model_statistic\": [{\"type\": \"GURU\",\"model_id\": 1,\"created_in_task\": false," +
            "\"contractor_actions\": {\"param_meta_data\": [{\"audit_action_id\": 5,\"entity_id\": 712377}," +
            "{\"audit_action_id\": 6,\"entity_id\": 777}],\"removed_param_hypothesis\": [{\"audit_action_id\": 3," +
            "\"entity_id\": 712377},{\"audit_action_id\": 4,\"entity_id\": 777}]},\"is_sku\": false}]," +
            "\"category_statistic\": {\"contractor_changes\": {},\"inspector_changes\": {},\"inspector_corrections\":" +
            " {}},\"sku_parameter_conflict_statistic\": [{\"uid\": 10101,\"model_id\": 1,\"parameter_conflicts\": " +
            "[{\"parameter_id\": 712377,\"decision\": \"CONFLICT_RESOLVED\"},{\"parameter_id\": 777,\"decision\": " +
            "\"CONFLICT_RESOLVED\"}]}]}";
        YangLogStorage.YangLogStoreRequest.Builder oldBuilder =
            YangLogStorage.YangLogStoreRequest.newBuilder();
        JsonFormat.parser().merge(changedRequest, oldBuilder);
        RawStatistics rawStatistics = new RawStatistics(new Date(), oldBuilder.build());

        List<YangBillingAction> yangBillingActions = YangTaskBillingHelper.processRawStatistic(rawStatistics);

        assertThat(yangBillingActions).map(YangBillingAction::getPaidAction).containsExactlyInAnyOrder(
            PaidAction.YANG_CONFLICTS_RESOLVE_CONFLICT,
            PaidAction.YANG_CONFLICTS_RESOLVE_CONFLICT,
            PaidAction.YANG_CONFLICTS_REMOVE_PARAM_HYPOTHESIS,
            PaidAction.YANG_CONFLICTS_REMOVE_PARAM_HYPOTHESIS
        );
    }

    @Test
    public void testSkuParametersConflictStatisticConflictAndRecheckWithNoAuditModels() throws InvalidProtocolBufferException {
        // real case json
        String changedRequest = "{\n" +
            "    \"id\": \"m3_28045975\",\n" +
            "    \"hitman_id\": 128045975,\n" +
            "    \"task_type\": \"SKU_PARAMETERS_CONFLICT\",\n" +
            "    \"category_id\": 12473293,\n" +
            "    \"contractor_info\":\n" +
            "    {\n" +
            "        \"uid\": 56852727,\n" +
            "        \"pool_id\": \"39203\",\n" +
            "        \"task_id\": \"ff651fd5-7d7c-496e-a829-1f1a18aab724\",\n" +
            "        \"pool_name\": \"Конфликты в параметрах sku\",\n" +
            "        \"assignment_id\": \"00027c2932--62c46348a4e9c4475f2a351c\",\n" +
            "        \"task_suite_created_date\": \"2022-07-01T12:37:14.416075Z\"\n" +
            "    },\n" +
            "    \"model_statistic\":\n" +
            "    [\n" +
            "        \n" +
            "        {\n" +
            "            \"type\": \"GURU\",\n" +
            "            \"is_sku\": false,\n" +
            "            \"model_id\": 1450580018,\n" +
            "            \"created_in_task\": false,\n" +
            "            \"contractor_actions\":\n" +
            "            {\n" +
            "                \"param_meta_data\":\n" +
            "                [\n" +
            "                    {\n" +
            "                        \"entity_id\": 21194330,\n" +
            "                        \"audit_action_id\": 108058351263\n" +
            "                    }\n" +
            "                ]\n" +
            "            }\n" +
            "        }\n" +
            "    ],\n" +
            "    \"category_statistic\":\n" +
            "    {\n" +
            "        \"inspector_changes\":\n" +
            "        {},\n" +
            "        \"contractor_changes\":\n" +
            "        {},\n" +
            "        \"inspector_corrections\":\n" +
            "        {}\n" +
            "    },\n" +
            "    \"sku_parameter_conflict_statistic\":\n" +
            "    [\n" +
            "        {\n" +
            "            \"uid\": 56852727,\n" +
            "            \"model_id\": 1450580018,\n" +
            "            \"parameter_conflicts\":\n" +
            "            [\n" +
            "                {\n" +
            "                    \"decision\": \"CONFLICT_RESOLVED\",\n" +
            "                    \"parameter_id\": 12514795\n" +
            "                },\n" +
            "                {\n" +
            "                    \"decision\": \"CONFLICT_RESOLVED\",\n" +
            "                    \"parameter_id\": 21194330\n" + // not in model_statistics
            "                }\n" +
            "            ]\n" +
            "        },\n" +
            "        {\n" +
            "            \"uid\": 56852727,\n" +
            "            \"model_id\": 101464563329,\n" +
            "            \"recheck_mapping_offers\":\n" +
            "            [\n" +
            "                266327922\n" +
            "            ]\n" +
            "        }\n" +
            "    ]\n" +
            "}";
        YangLogStorage.YangLogStoreRequest.Builder oldBuilder =
            YangLogStorage.YangLogStoreRequest.newBuilder();
        JsonFormat.parser().merge(changedRequest, oldBuilder);
        RawStatistics rawStatistics = new RawStatistics(new Date(), oldBuilder.build());

        List<YangBillingAction> yangBillingActions = YangTaskBillingHelper.processRawStatistic(rawStatistics);

        assertThat(yangBillingActions).map(YangBillingAction::getPaidAction).containsExactlyInAnyOrder(
            PaidAction.YANG_CONFLICTS_RESOLVE_CONFLICT,
            PaidAction.YANG_CONFLICTS_RESOLVE_CONFLICT,
            PaidAction.YANG_CONFLICTS_MAPPING_RECHECK
        );
    }

    private YangBillingAction withLinkData(long offerId, YangBillingAction action) {
        return withLinkData(Long.toString(offerId), action);
    }

    private YangBillingAction withLinkData(String linkData, YangBillingAction action) {
        action.setLinkData(linkData);
        return action;
    }

    private YangBillingAction user1Action(PaidAction paidAction,
                                          Long entityId, AuditAction.EntityType entityType, TaskType taskType) {
        return new YangBillingAction(USER1,
            paidAction,
            tomorrow,
            CATEGORY_ID,
            entityId,
            entityType,
            taskType,
            String.valueOf(TASK_ID));
    }

    private YangBillingAction user2Action(PaidAction paidAction,
                                          Long entityId, AuditAction.EntityType entityType, TaskType taskType) {
        return new YangBillingAction(USER2,
            paidAction,
            tomorrow,
            CATEGORY_ID,
            entityId,
            entityType,
            taskType,
            String.valueOf(TASK_ID));
    }

    private YangBillingAction user1Model1Action(PaidAction paidAction, TaskType taskType) {
        return user1Action(paidAction, MODEL1, AuditAction.EntityType.MODEL_GURU, taskType);
    }

    private YangBillingAction user2Model1Action(PaidAction paidAction, TaskType taskType) {
        return user2Action(paidAction, MODEL1, AuditAction.EntityType.MODEL_GURU, taskType);
    }

    private YangBillingAction user1Model2Action(PaidAction paidAction, TaskType taskType) {
        return user1Action(paidAction, MODEL2, AuditAction.EntityType.MODEL_GURU, taskType);
    }

    private YangBillingAction user2Model2Action(PaidAction paidAction, TaskType taskType) {
        return user2Action(paidAction, MODEL2, AuditAction.EntityType.MODEL_GURU, taskType);
    }

    private YangBillingAction user1Sku1Action(PaidAction paidAction, TaskType taskType) {
        return user1Action(paidAction, SKU1, AuditAction.EntityType.MODEL_SKU, taskType);
    }

    private YangBillingAction user2Sku1Action(PaidAction paidAction, TaskType taskType) {
        return user2Action(paidAction, SKU1, AuditAction.EntityType.MODEL_SKU, taskType);
    }


    private YangBillingAction user1Sku2Action(PaidAction paidAction, TaskType taskType) {
        return user1Action(paidAction, SKU2, AuditAction.EntityType.MODEL_SKU, taskType);
    }

    private YangBillingAction user2Sku2Action(PaidAction paidAction, TaskType taskType) {
        return user2Action(paidAction, SKU2, AuditAction.EntityType.MODEL_SKU, taskType);
    }

    private static Date tomorrow() {
        return toDate(LocalDateTime.now().plusDays(1));
    }

    private static Date toDate(LocalDateTime ldt) {
        return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
    }
}
