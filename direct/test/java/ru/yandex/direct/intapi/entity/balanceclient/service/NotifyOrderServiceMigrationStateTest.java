package ru.yandex.direct.intapi.entity.balanceclient.service;

import java.math.BigDecimal;
import java.util.Optional;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.balanceaggrmigration.lock.AggregateMigrationRedisLockService;
import ru.yandex.direct.core.entity.campaign.model.AggregatingSumStatus;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.ppcproperty.model.PpcPropertyEnum;
import ru.yandex.direct.core.entity.product.model.Product;
import ru.yandex.direct.core.entity.product.service.ProductService;
import ru.yandex.direct.core.entity.walletparams.repository.WalletParamsRepository;
import ru.yandex.direct.core.entity.walletparams.service.WalletParamsService;
import ru.yandex.direct.core.entity.xiva.XivaPushesQueueService;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.intapi.entity.balanceclient.container.BalanceClientResponse;
import ru.yandex.direct.intapi.entity.balanceclient.container.CampaignDataForNotifyOrder;
import ru.yandex.direct.intapi.entity.balanceclient.model.NotifyOrderParameters;
import ru.yandex.direct.intapi.entity.balanceclient.repository.NotifyOrderRepository;
import ru.yandex.direct.intapi.entity.balanceclient.service.migration.MigrationSchema;
import ru.yandex.direct.intapi.entity.balanceclient.service.validation.NotifyOrderValidationService;
import ru.yandex.direct.redislock.DistributedLock;
import ru.yandex.direct.redislock.DistributedLockException;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.intapi.entity.balanceclient.service.NotifyOrderService.GET_LOCK_TROUBLES_ERROR_CODE;
import static ru.yandex.direct.intapi.entity.balanceclient.service.NotifyOrderService.GET_LOCK_TROUBLES_ERROR_MESSAGE;

public class NotifyOrderServiceMigrationStateTest {

    private static final int SHARD = 2;
    private static final long WALLET_ID = 567L;

    @Mock
    private NotifyOrderValidationService validationService;

    @Mock
    private ShardHelper shardHelper;

    @Mock
    private NotifyOrderRepository notifyOrderRepository;

    @Mock
    private ProductService productService;

    @Mock
    private NotifyOrderUpdateCampaignDataService updateCampaignDataService;

    @Mock
    private NotifyOrderCampaignPostProcessingService postProcessingService;

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private CampaignService campaignService;

    @Mock
    private WalletParamsRepository walletParamsRepository;

    @Mock
    private WalletParamsService walletParamsService;

    @Mock
    private PpcPropertiesSupport ppcPropertiesSupport;

    @Mock
    private AggregateMigrationRedisLockService migrationRedisLockService;

    @Mock
    private FeatureService featureService;

    @Mock
    private XivaPushesQueueService xivaPushesQueueService;

    private NotifyOrderService notifyOrderService;
    private NotifyOrderParameters updateRequest;
    private CampaignDataForNotifyOrder dbCampaignData;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        notifyOrderService = spy(new NotifyOrderService(
                validationService,
                updateCampaignDataService,
                postProcessingService,
                productService,
                campaignService,
                null,
                notifyOrderRepository,
                shardHelper,
                campaignRepository,
                null,
                null,
                walletParamsRepository,
                walletParamsService,
                ppcPropertiesSupport,
                migrationRedisLockService,
                featureService,
                xivaPushesQueueService));

        updateRequest = NotifyOrderTestHelper.generateNotifyOrderParameters();

        BigDecimal sum = RandomNumberUtils.nextPositiveBigDecimal();
        dbCampaignData = NotifyOrderTestHelper.generateCampaignDataForNotifyOrder()
                .withCampaignId(updateRequest.getCampaignId())
                .withWalletId(WALLET_ID)
                .withType(CampaignType.TEXT)
                .withBalanceTid(updateRequest.getTid())
                .withProductId(RandomNumberUtils.nextPositiveLong())
                .withStatusEmpty(false)
                .withSum(sum)
                .withSumSpent(sum.subtract(BigDecimal.ONE))
                .withArchived(false);

        Product productInfo = new Product()
                .withPrice(RandomNumberUtils.nextPositiveBigDecimal())
                .withRate(RandomNumberUtils.nextPositiveLong())
                .withCurrencyCode(dbCampaignData.getCurrency());

        doReturn(SHARD).when(shardHelper).getShardByCampaignId(updateRequest.getCampaignId());
        doReturn(dbCampaignData).when(notifyOrderRepository).fetchCampaignData(SHARD, updateRequest.getCampaignId());
        doReturn(productInfo).when(productService).getProductById(dbCampaignData.getProductId());
        doNothing().when(notifyOrderService)
                .addLogBalance(eq(dbCampaignData), any(), any(), any(), eq(updateRequest.getTid()));
    }

    @Test
    public void processSchemaState_WalletSumAggregatedYes_MigrationSchemaNew() {
        dbCampaignData.withType(CampaignType.WALLET)
                .withWalletId(0L)
                .withWalletAggregateMigrated(AggregatingSumStatus.YES);

        MigrationSchema schema = notifyOrderService.processMigrationSchema(SHARD, dbCampaignData);
        assertThat(schema.getState(), is(MigrationSchema.State.NEW));
    }

    @Test
    public void processSchemaState_CampaignUnderWallet_MigrationSchemaNew() {
        dbCampaignData.withType(CampaignType.TEXT)
                .withWalletId(1L)
                .withWalletAggregateMigrated(AggregatingSumStatus.YES);

        MigrationSchema schema = notifyOrderService.processMigrationSchema(SHARD, dbCampaignData);
        assertThat(schema.getState(), is(MigrationSchema.State.NEW));
    }

    @Test
    public void processSchemaState_SumAggregatedNull_MigrationSchemaOld() {
        dbCampaignData.withType(CampaignType.WALLET)
                .withWalletId(0L)
                .withWalletAggregateMigrated(null);

        MigrationSchema schema = notifyOrderService.processMigrationSchema(SHARD, dbCampaignData);
        assertThat(schema.getState(), is(MigrationSchema.State.OLD));
    }

    @Test
    public void processSchemaState_SumAggregatedNoAndCurrencyYndFixed_MigrationSchemaOld() {
        dbCampaignData.withType(CampaignType.WALLET)
                .withWalletId(0L)
                .withWalletAggregateMigrated(AggregatingSumStatus.NO)
                .withCurrency(CurrencyCode.YND_FIXED);

        MigrationSchema schema = notifyOrderService.processMigrationSchema(SHARD, dbCampaignData);
        assertThat(schema.getState(), is(MigrationSchema.State.OLD));
    }

    @Test
    public void processSchemaState_MigrationFlagNull_MigrationSchemaOld() {
        setPpcProperty(null);

        dbCampaignData.withType(CampaignType.WALLET)
                .withWalletId(0L)
                .withWalletAggregateMigrated(AggregatingSumStatus.NO);

        MigrationSchema schema = notifyOrderService.processMigrationSchema(SHARD, dbCampaignData);
        assertThat(schema.getState(), is(MigrationSchema.State.OLD));
    }

    @Test
    public void processSchemaState_MigrationFlagBrokenJson_MigrationSchemaOld() {
        setPpcProperty("{bad_json}");

        dbCampaignData.withType(CampaignType.WALLET)
                .withWalletId(0L)
                .withWalletAggregateMigrated(AggregatingSumStatus.NO);

        MigrationSchema schema = notifyOrderService.processMigrationSchema(SHARD, dbCampaignData);
        assertThat(schema.getState(), is(MigrationSchema.State.OLD));
    }

    @Test
    public void processSchemaState_MigrationFlagDisabled_MigrationSchemaOld() {
        setPpcProperty("{\"enabled\": false}");

        dbCampaignData.withType(CampaignType.WALLET)
                .withWalletId(0L)
                .withWalletAggregateMigrated(AggregatingSumStatus.NO);

        MigrationSchema schema = notifyOrderService.processMigrationSchema(SHARD, dbCampaignData);
        assertThat(schema.getState(), is(MigrationSchema.State.OLD));
    }

    @Test
    public void processSchemaState_MigrationFlagDisabledTimeLongerThanMaxDuration_MigrationSchemaOld() {
        long secondsUtc =
                DateTime.now().minusSeconds(NotifyOrderService.MAX_TIME_MIGRATION_DURATION_SEC + 1).getMillis() / 1000;
        setPpcProperty("{\"enabled\": false, \"time\": " + secondsUtc + "}");

        dbCampaignData.withType(CampaignType.WALLET)
                .withWalletId(0L)
                .withWalletAggregateMigrated(AggregatingSumStatus.NO);

        MigrationSchema schema = notifyOrderService.processMigrationSchema(SHARD, dbCampaignData);
        assertThat(schema.getState(), is(MigrationSchema.State.OLD));
    }

    @Test
    public void processSchemaState_FailedLock_MigrationSchemaLockTroubles() {
        enablePpcProperty();
        doReturn(failedLock()).when(migrationRedisLockService).lock(dbCampaignData.getCampaignId());

        dbCampaignData.withType(CampaignType.WALLET)
                .withWalletId(0L)
                .withWalletAggregateMigrated(AggregatingSumStatus.NO);

        MigrationSchema schema = notifyOrderService.processMigrationSchema(SHARD, dbCampaignData);
        assertThat(schema.getState(), is(MigrationSchema.State.LOCK_TROUBLES));
    }

    @Test
    public void processSchemaState_FailedUnlockOnNewSchema_MigrationSchemaLockTroubles() {
        enablePpcProperty();
        DistributedLock lock = successLock();
        doReturn(lock).when(migrationRedisLockService).lock(dbCampaignData.getCampaignId());
        doThrow(new DistributedLockException()).when(lock).unlock();

        dbCampaignData.withType(CampaignType.WALLET)
                .withWalletId(0L)
                .withWalletAggregateMigrated(AggregatingSumStatus.NO);

        CampaignDataForNotifyOrder updatedDbCampaignData = NotifyOrderTestHelper.generateCampaignDataForNotifyOrder()
                .withWalletAggregateMigrated(AggregatingSumStatus.YES);

        doReturn(updatedDbCampaignData)
                .when(notifyOrderRepository).fetchCampaignData(SHARD, updateRequest.getCampaignId());

        MigrationSchema schema = notifyOrderService.processMigrationSchema(SHARD, dbCampaignData);
        assertThat(schema.getState(), is(MigrationSchema.State.LOCK_TROUBLES));
    }

    @Test
    public void processSchemaState_SuccessLock_MigrationSchemaOldWithLock() {
        enablePpcProperty();
        DistributedLock lock = successLock();
        doReturn(lock).when(migrationRedisLockService).lock(dbCampaignData.getCampaignId());

        dbCampaignData.withType(CampaignType.WALLET)
                .withWalletId(0L)
                .withWalletAggregateMigrated(AggregatingSumStatus.NO);

        MigrationSchema schema = notifyOrderService.processMigrationSchema(SHARD, dbCampaignData);
        assertThat(schema.getState(), is(MigrationSchema.State.OLD_WITH_LOCK));
        assertNotNull(schema.getLock());
        verify(lock, never()).unlock();
    }

    @Test
    public void processSchemaState_SuccessLockCampaignMigrated_MigrationSchemaNewLockIsUnlocked() {
        enablePpcProperty();
        DistributedLock lock = successLock();
        doReturn(lock).when(migrationRedisLockService).lock(dbCampaignData.getCampaignId());
        doReturn(true).when(migrationRedisLockService).unlock(lock);

        dbCampaignData.withType(CampaignType.WALLET)
                .withWalletId(0L)
                .withWalletAggregateMigrated(AggregatingSumStatus.NO);

        CampaignDataForNotifyOrder updatedDbCampaignData = NotifyOrderTestHelper.generateCampaignDataForNotifyOrder()
                .withWalletAggregateMigrated(AggregatingSumStatus.YES);

        doReturn(updatedDbCampaignData)
                .when(notifyOrderRepository).fetchCampaignData(SHARD, updateRequest.getCampaignId());

        MigrationSchema schema = notifyOrderService.processMigrationSchema(SHARD, dbCampaignData);
        assertThat(schema.getState(), is(MigrationSchema.State.NEW));
        verify(migrationRedisLockService).unlock(lock);
    }

    @Test
    public void notifyOrder_FailedLock_GetLockTroublesErrorResponse() {
        enablePpcProperty();
        doReturn(failedLock()).when(migrationRedisLockService).lock(dbCampaignData.getCampaignId());

        dbCampaignData.withType(CampaignType.WALLET)
                .withWalletId(0L)
                .withWalletAggregateMigrated(AggregatingSumStatus.NO);

        BalanceClientResponse response = notifyOrderService.notifyOrder(updateRequest);
        assertThat(response,
                beanDiffer(BalanceClientResponse.error(GET_LOCK_TROUBLES_ERROR_CODE, GET_LOCK_TROUBLES_ERROR_MESSAGE)));
    }

    @Test
    public void notifyOrder_OldSchemaWithLock_SuccessResponseAndUnlock() {
        enablePpcProperty();
        DistributedLock lock = successLock();
        doReturn(lock).when(migrationRedisLockService).lock(dbCampaignData.getCampaignId());

        dbCampaignData.withType(CampaignType.WALLET)
                .withWalletId(0L)
                .withWalletAggregateMigrated(AggregatingSumStatus.NO);

        BalanceClientResponse response = notifyOrderService.notifyOrder(updateRequest);
        assertThat(response, beanDiffer(BalanceClientResponse.success()));
        verify(migrationRedisLockService).unlock(lock);
    }

    private DistributedLock successLock() {
        DistributedLock lock = spy(DistributedLock.class);
        try {
            doReturn(true).when(lock).lock();
            doReturn(true).when(lock).isLocked();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return lock;
    }

    private DistributedLock failedLock() {
        DistributedLock lock = spy(DistributedLock.class);
        try {
            doReturn(false).when(lock).lock();
            doReturn(false).when(lock).isLocked();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return lock;
    }

    private void enablePpcProperty() {
        setPpcProperty("{\"enabled\": true, \"time\": " + System.currentTimeMillis() / 1000 + "}");
    }

    private void setPpcProperty(String json) {
        doReturn(Optional.ofNullable(json)).when(ppcPropertiesSupport)
                .find(PpcPropertyEnum.WALLET_SUMS_MIGRATION_STATE.getName());

    }
}
