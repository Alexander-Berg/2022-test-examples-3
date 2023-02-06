package ru.yandex.direct.jobs.balanceaggrmigration;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.bs.export.queue.model.BsExportSpecials;
import ru.yandex.direct.core.entity.bs.export.queue.model.QueueType;
import ru.yandex.direct.core.entity.bs.export.queue.repository.BsExportQueueRepository;
import ru.yandex.direct.core.entity.bs.export.queue.repository.BsExportSpecialsRepository;
import ru.yandex.direct.core.entity.campaign.model.AggregatingSumStatus;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.ppcproperty.model.PpcPropertyEnum;
import ru.yandex.direct.core.entity.ppcproperty.model.WalletMigrationStateFlag;
import ru.yandex.direct.core.entity.wallet.model.WalletParamsModel;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.utils.DateTimeUtils;
import ru.yandex.direct.utils.JsonUtils;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.jobs.balanceaggrmigration.BalanceMigrationDirection.MIGRATE;
import static ru.yandex.direct.jobs.balanceaggrmigration.BalanceMigrationDirection.ROLLBACK;

class BalanceAggregateMigrationCheckerTest {
    private static final int SHARD = 2;
    private static final Long WALLET_ID = 314L;

    private BalanceAggregateMigrationChecker checker;

    private BsExportSpecialsRepository bsExportSpecialsRepository;
    private BsExportQueueRepository bsExportQueueRepository;
    private PpcPropertiesSupport ppcPropertiesSupport;

    @BeforeEach
    void before() {
        bsExportSpecialsRepository = mock(BsExportSpecialsRepository.class);
        bsExportQueueRepository = mock(BsExportQueueRepository.class);

        ShardHelper shardHelper = mock(ShardHelper.class);
        when(shardHelper.getShardByCampaignId(WALLET_ID)).thenReturn(SHARD);

        ppcPropertiesSupport = mock(PpcPropertiesSupport.class);

        checker = new BalanceAggregateMigrationChecker(bsExportSpecialsRepository,
                bsExportQueueRepository,
                shardHelper,
                ppcPropertiesSupport);
    }

    @Test
    void isJobEnabled_FlagEnabledMoreThan15MinutesAgo_True() {
        when(ppcPropertiesSupport.find(PpcPropertyEnum.WALLET_SUMS_MIGRATION_STATE.getName()))
                .thenReturn(Optional.of(toJsonString(true, secondsNowMinusMinutes(
                        BalanceAggregateMigrationChecker.MAX_TIME_MIGRATION_DURATION.toMinutes() + 1))));

        assertTrue(checker.isJobEnabled());
    }

    @Test
    void isJobEnabled_FlagEnabledLessThan15MinutesAgo_False() {
        when(ppcPropertiesSupport.find(PpcPropertyEnum.WALLET_SUMS_MIGRATION_STATE.getName()))
                .thenReturn(Optional.of(toJsonString(true, secondsNowMinusMinutes(
                        BalanceAggregateMigrationChecker.MAX_TIME_MIGRATION_DURATION.toMinutes() - 1))));

        assertFalse(checker.isJobEnabled());
    }

    @Test
    void isJobEnabled_FlagDisabled_False() {
        when(ppcPropertiesSupport.find(PpcPropertyEnum.WALLET_SUMS_MIGRATION_STATE.getName()))
                .thenReturn(Optional.of(toJsonString(false, secondsNowMinusMinutes(
                        BalanceAggregateMigrationChecker.MAX_TIME_MIGRATION_DURATION.toMinutes()))));

        assertFalse(checker.isJobEnabled());
    }

    @Test
    void isJobEnabled_FlagEnabledNow_False() {
        when(ppcPropertiesSupport.find(PpcPropertyEnum.WALLET_SUMS_MIGRATION_STATE.getName()))
                .thenReturn(Optional.of(toJsonString(true, DateTimeUtils.getNowEpochSeconds())));

        assertFalse(checker.isJobEnabled());
    }

    @Test
    void isJobEnabled_PropertyNotFound_False() {
        when(ppcPropertiesSupport.find(PpcPropertyEnum.WALLET_SUMS_MIGRATION_STATE.getName()))
                .thenReturn(Optional.empty());

        assertFalse(checker.isJobEnabled());
    }

    @Test
    void isJobEnabled_InvalidJson_False() {
        when(ppcPropertiesSupport.find(PpcPropertyEnum.WALLET_SUMS_MIGRATION_STATE.getName()))
                .thenReturn(Optional.of("invalid_json"));

        assertFalse(checker.isJobEnabled());
    }

    @Test
    void hasAvailableInBsExportSpecials_Zero_True() {
        when(bsExportSpecialsRepository.campaignsByTypeSizeInQueue(SHARD, QueueType.CAMPS_ONLY, CampaignType.WALLET))
                .thenReturn(0);
        assertTrue(checker.hasAvailablePlaceInBsExportSpecials(SHARD));
    }

    @Test
    void hasAvailableInBsExportSpecials_LessThanMax_True() {
        when(bsExportSpecialsRepository.campaignsByTypeSizeInQueue(SHARD, QueueType.CAMPS_ONLY, CampaignType.WALLET))
                .thenReturn(1);
        assertTrue(checker.hasAvailablePlaceInBsExportSpecials(SHARD));
    }

    @Test
    void hasAvailableInBsExportSpecials_Max_False() {
        when(bsExportSpecialsRepository.campaignsByTypeSizeInQueue(SHARD, QueueType.CAMPS_ONLY, CampaignType.WALLET))
                .thenReturn(BalanceAggregateMigrationChecker.BS_QUEUE_SPECIAL_WALLETS_LIMIT);
        assertFalse(checker.hasAvailablePlaceInBsExportSpecials(SHARD));
    }

    @Test
    void hasAvailableInBsExportSpecials_MaxPlus1_False() {
        when(bsExportSpecialsRepository.campaignsByTypeSizeInQueue(SHARD, QueueType.CAMPS_ONLY, CampaignType.WALLET))
                .thenReturn(BalanceAggregateMigrationChecker.BS_QUEUE_SPECIAL_WALLETS_LIMIT + 1);
        assertFalse(checker.hasAvailablePlaceInBsExportSpecials(SHARD));
    }

    @Test
    void isWalletAndHasRealCurrency_WalletWithRubCurrency_True() {
        Campaign campaign = new Campaign().withType(CampaignType.WALLET).withCurrency(CurrencyCode.RUB);
        assertTrue(checker.isWalletAndHasRealCurrency(campaign));
    }

    @Test
    void isWalletAndHasRealCurrency_TextCampaignWithRubCurrency_False() {
        Campaign campaign = new Campaign().withType(CampaignType.TEXT).withCurrency(CurrencyCode.RUB);
        assertFalse(checker.isWalletAndHasRealCurrency(campaign));
    }

    @Test
    void isWalletAndHasRealCurrency_TextCampaignWithYndFixedCurrency_False() {
        Campaign campaign = new Campaign().withType(CampaignType.TEXT).withCurrency(CurrencyCode.YND_FIXED);
        assertFalse(checker.isWalletAndHasRealCurrency(campaign));

    }

    @Test
    void isWalletAndHasRealCurrency_WalletWithYndFixedCurrency_False() {
        Campaign campaign = new Campaign().withType(CampaignType.WALLET).withCurrency(CurrencyCode.YND_FIXED);
        assertFalse(checker.isWalletAndHasRealCurrency(campaign));
    }

    @Test
    void checkSumInCampaigns_WalletSumIsZeroEmptyCampaignsTotalSumIsZero_True() {
        Campaign wallet = campaign(BigDecimal.ZERO);
        WalletParamsModel walletParams = new WalletParamsModel().withTotalSum(BigDecimal.ZERO);
        List<Campaign> campaigns = emptyList();

        assertTrue(checker.checkSumInCampaigns(wallet, walletParams, campaigns));
    }

    @Test
    void checkSumInCampaigns_WalletSumEqualsTotalSumEmptyCampaigns_True() {
        Campaign wallet = campaign(BigDecimal.ONE);
        WalletParamsModel walletParams = new WalletParamsModel().withTotalSum(BigDecimal.ONE);
        List<Campaign> campaigns = emptyList();

        assertTrue(checker.checkSumInCampaigns(wallet, walletParams, campaigns));
    }

    @Test
    void checkSumInCampaigns_WalletSumNotEqualsTotalSumEmptyCampaigns_False() {
        Campaign wallet = campaign(BigDecimal.TEN);
        WalletParamsModel walletParams = new WalletParamsModel().withTotalSum(BigDecimal.ONE);
        List<Campaign> campaigns = emptyList();

        assertFalse(checker.checkSumInCampaigns(wallet, walletParams, campaigns));
    }

    @Test
    void checkSumInCampaigns_WalletSumZeroCampaignZeroEqualsTotalSum_True() {
        Campaign wallet = campaign(BigDecimal.ZERO);
        WalletParamsModel walletParams = new WalletParamsModel().withTotalSum(BigDecimal.ZERO);
        List<Campaign> campaigns = singletonList(campaign(BigDecimal.ZERO));

        assertTrue(checker.checkSumInCampaigns(wallet, walletParams, campaigns));
    }

    @Test
    void checkSumInCampaigns_WalletSumZeroCampaignNotEmptySumEqualsTotalSum_True() {
        Campaign wallet = campaign(BigDecimal.ZERO);
        WalletParamsModel walletParams = new WalletParamsModel().withTotalSum(BigDecimal.TEN);
        List<Campaign> campaigns = singletonList(campaign(BigDecimal.TEN));

        assertTrue(checker.checkSumInCampaigns(wallet, walletParams, campaigns));
    }

    @Test
    void checkSumInCampaigns_WalletSumZeroTwoCampaignsNotEmptySumEqualsTotalSum_True() {
        Campaign wallet = campaign(BigDecimal.ZERO);
        WalletParamsModel walletParams = new WalletParamsModel().withTotalSum(BigDecimal.valueOf(5));
        List<Campaign> campaigns = asList(campaign(BigDecimal.valueOf(2)), campaign(BigDecimal.valueOf(3)));

        assertTrue(checker.checkSumInCampaigns(wallet, walletParams, campaigns));
    }

    @Test
    void checkSumInCampaigns_WalletSumZeroTwoCampaignsNotEmptySumNotEqualsTotalSum_False() {
        Campaign wallet = campaign(BigDecimal.ZERO);
        WalletParamsModel walletParams = new WalletParamsModel().withTotalSum(BigDecimal.valueOf(10));
        List<Campaign> campaigns = asList(campaign(BigDecimal.valueOf(2)), campaign(BigDecimal.valueOf(3)));

        assertFalse(checker.checkSumInCampaigns(wallet, walletParams, campaigns));
    }

    @Test
    void checkSumInCampaigns_WalletSumAndTwoCampaignsSumEqualsTotalSum_True() {
        Campaign wallet = campaign(BigDecimal.valueOf(1));
        WalletParamsModel walletParams = new WalletParamsModel().withTotalSum(BigDecimal.valueOf(6));
        List<Campaign> campaigns = asList(campaign(BigDecimal.valueOf(2)), campaign(BigDecimal.valueOf(3)));

        assertTrue(checker.checkSumInCampaigns(wallet, walletParams, campaigns));
    }

    @Test
    void checkSumInCampaigns_WalletSumAndTwoCampaignsSumNotEqualsTotalSum_False() {
        Campaign wallet = campaign(BigDecimal.valueOf(1));
        WalletParamsModel walletParams = new WalletParamsModel().withTotalSum(BigDecimal.valueOf(10));
        List<Campaign> campaigns = asList(campaign(BigDecimal.valueOf(2)), campaign(BigDecimal.valueOf(3)));

        assertFalse(checker.checkSumInCampaigns(wallet, walletParams, campaigns));
    }

    @Test
    void allSumsAreZero_emptyList_True() {
        assertTrue(checker.allSumsAreZero(emptyList()));
    }

    @Test
    void allSumsAreZero_CampaignWithZeroSum_True() {
        assertTrue(checker.allSumsAreZero(singletonList(campaign(BigDecimal.ZERO))));
    }

    @Test
    void allSumsAreZero_CampaignWithNonZeroSum_False() {
        assertFalse(checker.allSumsAreZero(singletonList(campaign(BigDecimal.ONE))));
    }

    @Test
    void allSumsAreZero_CampaignsWithZeroSum_True() {
        assertTrue(checker.allSumsAreZero(asList(campaign(BigDecimal.ZERO), campaign(BigDecimal.ZERO))));
    }

    @Test
    void allSumsAreZero_TwoCampaignsOneWithNoneZeroSum_False() {
        assertFalse(checker.allSumsAreZero(asList(campaign(BigDecimal.ONE), campaign(BigDecimal.ZERO))));
    }

    @Test
    void allSumsAreZero_CampaignsWithNoneZeroSum_False() {
        assertFalse(checker.allSumsAreZero(asList(campaign(BigDecimal.TEN), campaign(BigDecimal.ONE))));
    }

    @Test
    void isMigrateStatusCorrect_StatusNoRollbackFalse_True() {
        WalletParamsModel walletParams = new WalletParamsModel().withAggregateMigrateStatus(AggregatingSumStatus.NO);
        assertTrue(checker.isMigrateStatusCorrect(walletParams, MIGRATE));
    }

    @Test
    void isMigrateStatusCorrect_StatusYesRollbackTrue_True() {
        WalletParamsModel walletParams = new WalletParamsModel().withAggregateMigrateStatus(AggregatingSumStatus.YES);
        assertTrue(checker.isMigrateStatusCorrect(walletParams, ROLLBACK));
    }

    @Test
    void isMigrateStatusCorrect_StatusYesRollbackFalse_False() {
        WalletParamsModel walletParams = new WalletParamsModel().withAggregateMigrateStatus(AggregatingSumStatus.YES);
        assertFalse(checker.isMigrateStatusCorrect(walletParams, MIGRATE));
    }

    @Test
    void isMigrateStatusCorrect_StatusNoRollbackTrue_False() {
        WalletParamsModel walletParams = new WalletParamsModel().withAggregateMigrateStatus(AggregatingSumStatus.NO);
        assertFalse(checker.isMigrateStatusCorrect(walletParams, ROLLBACK));
    }

    @Test
    void isInBsQueues_WalletNotInQueuesCampaignsEmpty_False() {
        List<Long> campaignIds = singletonList(WALLET_ID);

        when(bsExportQueueRepository.getCampaignIdsInQueueExceptMasterExport(SHARD, campaignIds))
                .thenReturn(emptySet());
        when(bsExportSpecialsRepository.getByCampaignIds(SHARD, campaignIds))
                .thenReturn(emptyList());

        assertFalse(checker.isInBsQueues(WALLET_ID, emptyList(), emptyMap(), false, MIGRATE));
    }

    @Test
    void isInBsQueues_WalletInQueueCampaignsEmpty_True() {
        List<Long> campaignIds = singletonList(WALLET_ID);

        when(bsExportQueueRepository.getCampaignIdsInQueueExceptMasterExport(SHARD, campaignIds))
                .thenReturn(singleton(WALLET_ID));

        assertTrue(checker.isInBsQueues(WALLET_ID, emptyList(), emptyMap(), false, MIGRATE));
    }

    @Test
    void isInBsQueues_WalletInBuggyExportCampaignsEmpty_False() {
        List<Long> campaignIds = singletonList(WALLET_ID);

        when(bsExportSpecialsRepository.getByCampaignIds(SHARD, campaignIds))
                .thenReturn(singletonList(bsExport(WALLET_ID, QueueType.BUGGY)));

        assertFalse(checker.isInBsQueues(WALLET_ID, emptyList(), emptyMap(), false, MIGRATE));
    }

    @Test
    void isInBsQueues_WalletInHeavyExportCampaignsEmpty_False() {
        List<Long> campaignIds = singletonList(WALLET_ID);

        when(bsExportSpecialsRepository.getByCampaignIds(SHARD, campaignIds))
                .thenReturn(singletonList(bsExport(WALLET_ID, QueueType.HEAVY)));

        assertFalse(checker.isInBsQueues(WALLET_ID, emptyList(), emptyMap(), false, MIGRATE));
    }

    @Test
    void isInBsQueues_WalletInCampsOnlyExportCampaignsEmpty_False() {
        List<Long> campaignIds = singletonList(WALLET_ID);

        when(bsExportSpecialsRepository.getByCampaignIds(SHARD, campaignIds))
                .thenReturn(singletonList(bsExport(WALLET_ID, QueueType.CAMPS_ONLY)));

        assertFalse(checker.isInBsQueues(WALLET_ID, emptyList(), emptyMap(), false, MIGRATE));
    }

    @Test
    void isInBsQueues_WalletInSpecialExportCampaignsEmpty_True() {
        List<Long> campaignIds = singletonList(WALLET_ID);

        when(bsExportSpecialsRepository.getByCampaignIds(SHARD, campaignIds))
                .thenReturn(singletonList(bsExport(WALLET_ID, QueueType.PREPROD)));

        assertTrue(checker.isInBsQueues(WALLET_ID, emptyList(), emptyMap(), false, MIGRATE));
    }

    @Test
    void isInBsQueues_WalletNotInQueuesCampaignNotInQueues_False() {
        long otherCampaignId = 528L;
        List<Long> campaignIds = asList(WALLET_ID, otherCampaignId);

        when(bsExportQueueRepository.getCampaignIdsInQueueExceptMasterExport(SHARD, campaignIds))
                .thenReturn(emptySet());
        when(bsExportSpecialsRepository.getByCampaignIds(SHARD, campaignIds))
                .thenReturn(emptyList());

        assertFalse(checker.isInBsQueues(
                WALLET_ID, singletonList(campaign(otherCampaignId)), emptyMap(), false, MIGRATE
        ));
    }

    @Test
    void isInBsQueues_WalletNotInQueuesCampaignInExportQueue_True() {
        long otherCampaignId = 528L;
        List<Long> campaignIds = asList(WALLET_ID, otherCampaignId);

        when(bsExportQueueRepository.getCampaignIdsInQueueExceptMasterExport(SHARD, campaignIds))
                .thenReturn(singleton(otherCampaignId));

        assertTrue(checker.isInBsQueues(
                WALLET_ID, singletonList(campaign(otherCampaignId)), emptyMap(), false, MIGRATE
        ));
    }

    @Test
    void isInBsQueues_WalletNotInQueuesCampaignInSpecialExport_True() {
        long otherCampaignId = 528L;
        List<Long> campaignIds = asList(WALLET_ID, otherCampaignId);

        when(bsExportSpecialsRepository.getByCampaignIds(SHARD, campaignIds))
                .thenReturn(singletonList(bsExport(otherCampaignId, QueueType.DEV1)));

        assertTrue(checker.isInBsQueues(
                WALLET_ID, singletonList(campaign(otherCampaignId)), emptyMap(), false, MIGRATE
        ));
    }

    @Test
    void isInBsQueues_WalletNotInQueuesCampaignInAllowedSpecialExport_False() {
        long otherCampaignId = 528L;
        List<Long> campaignIds = asList(WALLET_ID, otherCampaignId);

        when(bsExportSpecialsRepository.getByCampaignIds(SHARD, campaignIds))
                .thenReturn(singletonList(bsExport(otherCampaignId, QueueType.CAMPS_ONLY)));

        assertFalse(checker.isInBsQueues(
                WALLET_ID, singletonList(campaign(otherCampaignId)), emptyMap(), false, MIGRATE
        ));
    }

    @Test
    public void isInBsQueues_CampaignInExportQueueIgnoreQueue_False() {
        long otherCampaignId = 528L;
        List<Long> campaignIds = asList(WALLET_ID, otherCampaignId);

        when(bsExportQueueRepository.getCampaignIdsInQueueExceptMasterExport(SHARD, campaignIds))
                .thenReturn(singleton(otherCampaignId));

        assertFalse(checker.isInBsQueues(
                WALLET_ID, singletonList(campaign(otherCampaignId)), emptyMap(), true, MIGRATE
        ));
    }

    @Test
    public void isInBsQueues_CampaignInExportQueueAndDev1SpecQueueIgnoreQueue_True() {
        long otherCampaignId = 528L;
        List<Long> campaignIds = asList(WALLET_ID, otherCampaignId);

        when(bsExportQueueRepository.getCampaignIdsInQueueExceptMasterExport(SHARD, campaignIds))
                .thenReturn(singleton(otherCampaignId));
        when(bsExportSpecialsRepository.getByCampaignIds(SHARD, campaignIds))
                .thenReturn(singletonList(bsExport(otherCampaignId, QueueType.DEV1)));

        assertTrue(checker.isInBsQueues(
                WALLET_ID, singletonList(campaign(otherCampaignId)), emptyMap(), true, MIGRATE
        ));
    }

    @Test
    public void isInBsQueues_CampaignWithoutSumInSpecialExport_False() {
        long otherCampaignId = 528L;
        List<Long> campaignIds = asList(WALLET_ID, otherCampaignId);

        when(bsExportSpecialsRepository.getByCampaignIds(SHARD, campaignIds))
                .thenReturn(singletonList(bsExport(otherCampaignId, QueueType.DEV1)));

        assertFalse(checker.isInBsQueues(
                WALLET_ID, singletonList(campaignZeroSum(otherCampaignId)), emptyMap(), false, MIGRATE
        ));
    }

    @Test
    public void isInBsQueues_CampaignWithSumBalanceInSpecialExport_True() {
        long otherCampaignId = 528L;
        List<Long> campaignIds = asList(WALLET_ID, otherCampaignId);

        when(bsExportSpecialsRepository.getByCampaignIds(SHARD, campaignIds))
                .thenReturn(singletonList(bsExport(otherCampaignId, QueueType.DEV1)));

        assertTrue(checker.isInBsQueues(
                WALLET_ID, singletonList(campaignSumBalance(otherCampaignId)), emptyMap(), false, ROLLBACK
        ));
    }

    // Если campaigns_multicurrency_sums.chips_cost равно кол-ву зачислений на кампании,
    // это значит, что у кампании нет зачислений в валюте
    // (были только фишки, и все были потрачены до конвертации в валюту)
    @Test
    public void isInBsQueues_CampaignInSpecQueueNoSumCur_False() {
        long otherCampaignId = 528L;
        List<Long> campaignIds = asList(WALLET_ID, otherCampaignId);

        when(bsExportSpecialsRepository.getByCampaignIds(SHARD, campaignIds))
                .thenReturn(singletonList(bsExport(otherCampaignId, QueueType.NOSEND)));

        var camp = campaign(otherCampaignId);
        var chipsCosts = Map.of(otherCampaignId, camp.getSum().setScale(3, RoundingMode.DOWN));
        assertFalse(checker.isInBsQueues(
                WALLET_ID, singletonList(camp), chipsCosts, false, MIGRATE
        ));
    }

    // Аналогично предыдущему тесту, но в случае откатывания в старую схему зачислений
    @Test
    public void isInBsQueues_RollbackCampaignInSpecQueueNoSumCur_False() {
        long otherCampaignId = 528L;
        List<Long> campaignIds = asList(WALLET_ID, otherCampaignId);

        when(bsExportSpecialsRepository.getByCampaignIds(SHARD, campaignIds))
                .thenReturn(singletonList(bsExport(otherCampaignId, QueueType.DEV1)));

        var camp = campaignSumBalance(otherCampaignId);
        var chipsCosts = Map.of(otherCampaignId, camp.getSumBalance().setScale(3, RoundingMode.DOWN));
        assertFalse(checker.isInBsQueues(
                WALLET_ID, singletonList(camp), chipsCosts, false, ROLLBACK
        ));
    }

    private String toJsonString(Boolean isEnabled, Long time) {
        return JsonUtils.toJson(new WalletMigrationStateFlag().withEnabled(isEnabled).withTime(time));
    }

    private Long secondsNowMinusMinutes(long minutes) {
        return DateTimeUtils.getNowEpochSeconds() - Duration.ofMinutes(minutes).toMillis() / 1000;
    }

    private Campaign campaign(BigDecimal sum) {
        return new Campaign().withSum(sum).withSumBalance(BigDecimal.ZERO);
    }

    private Campaign campaign(Long id) {
        return new Campaign().withId(id).withSum(BigDecimal.TEN).withSumBalance(BigDecimal.ZERO);
    }

    private Campaign campaignZeroSum(Long id) {
        return new Campaign().withId(id).withSum(BigDecimal.ZERO).withSumBalance(BigDecimal.ZERO);
    }

    private Campaign campaignSumBalance(Long id) {
        return new Campaign().withId(id).withSum(BigDecimal.ZERO).withSumBalance(BigDecimal.TEN);
    }

    private BsExportSpecials bsExport(long id, QueueType type) {
        return new BsExportSpecials().withCampaignId(id).withType(type);
    }
}
