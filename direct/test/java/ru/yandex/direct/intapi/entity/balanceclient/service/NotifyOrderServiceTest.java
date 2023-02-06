package ru.yandex.direct.intapi.entity.balanceclient.service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.balanceaggrmigration.lock.AggregateMigrationRedisLockService;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignForNotifyOrder;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignService;
import ru.yandex.direct.core.entity.client.repository.ClientRepository;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.product.model.Product;
import ru.yandex.direct.core.entity.product.service.ProductService;
import ru.yandex.direct.core.entity.walletparams.container.WalletParams;
import ru.yandex.direct.core.entity.walletparams.repository.WalletParamsRepository;
import ru.yandex.direct.core.entity.walletparams.service.WalletParamsService;
import ru.yandex.direct.core.entity.xiva.XivaPushesQueueService;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.intapi.entity.balanceclient.BalanceClientServiceConstants;
import ru.yandex.direct.intapi.entity.balanceclient.container.BalanceClientResponse;
import ru.yandex.direct.intapi.entity.balanceclient.container.CampaignDataForNotifyOrder;
import ru.yandex.direct.intapi.entity.balanceclient.model.NotifyOrderParameters;
import ru.yandex.direct.intapi.entity.balanceclient.repository.NotifyOrderRepository;
import ru.yandex.direct.intapi.entity.balanceclient.service.migration.MigrationSchema;
import ru.yandex.direct.intapi.entity.balanceclient.service.validation.NotifyOrderValidationService;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.intapi.entity.balanceclient.model.NotifyOrderParameters.TOTAL_SUM_FIELD_NAME;
import static ru.yandex.direct.intapi.entity.balanceclient.service.NotifyOrderService.CAMPAIGN_DOES_NOT_EXISTS_ERROR_MESSAGE;
import static ru.yandex.direct.intapi.entity.balanceclient.service.NotifyOrderService.CAMPAIGN_NOT_EXISTS_ERROR_CODE;
import static ru.yandex.direct.intapi.entity.balanceclient.service.NotifyOrderService.ID_NOT_SET;
import static ru.yandex.direct.intapi.entity.balanceclient.service.validation.NotifyOrderValidationService.invalidFieldMessage;

/**
 * Тесты на метод notifyOrder из NotifyOrderService
 *
 * @see NotifyOrderService
 */
public class NotifyOrderServiceTest {

    private static final int SHARD = 2;
    private static final int INVALID_TOTAL_SUM_ERROR_CODE = 1015;
    private static final BigDecimal SUM_ON_CAMPAIGN_NEW_SCHEMA = BigDecimal.ZERO;
    private static BalanceClientResponse errorResponse;
    private static BalanceClientResponse successResponse;
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
    private ClientRepository clientRepository;

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

    @Captor
    private ArgumentCaptor<Money> moneyArgumentCaptor;

    private NotifyOrderService notifyOrderService;
    private NotifyOrderParameters updateRequest;
    private CampaignDataForNotifyOrder dbCampaignData;
    private Product productInfo;
    private Money sum;
    private Money dbSum;
    private Money sumDelta;
    private BigDecimal sumBalance;

    private MigrationSchema schema;

    @BeforeClass
    public static void initTestData() {
        errorResponse = BalanceClientResponse.criticalError("some error");
        successResponse = BalanceClientResponse.success();
    }

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
                clientRepository,
                null,
                walletParamsRepository,
                walletParamsService,
                ppcPropertiesSupport,
                migrationRedisLockService,
                featureService,
                xivaPushesQueueService));

        schema = new MigrationSchema().withState(MigrationSchema.State.OLD);

        updateRequest = NotifyOrderTestHelper.generateNotifyOrderParameters();
        dbCampaignData = NotifyOrderTestHelper.generateCampaignDataForNotifyOrder()
                .withCampaignId(updateRequest.getCampaignId())
                .withBalanceTid(updateRequest.getTid())
                .withProductId(RandomNumberUtils.nextPositiveLong())
                .withStatusEmpty(false)
                .withSum(RandomNumberUtils.nextPositiveBigDecimal())
                .withArchived(false);
        dbCampaignData.withSumSpent(dbCampaignData.getSum().subtract(BigDecimal.ONE));

        productInfo = new Product()
                .withPrice(RandomNumberUtils.nextPositiveBigDecimal())
                .withRate(RandomNumberUtils.nextPositiveLong())
                .withCurrencyCode(dbCampaignData.getCurrency());
        sum = Money.valueOf(productInfo.getPrice(), productInfo.getCurrencyCode())
                .multiply(updateRequest.getSumUnits())
                .divide(productInfo.getRate());
        dbSum = Money.valueOf(dbCampaignData.getSum(), dbCampaignData.getCurrency());
        sumDelta = sum.subtract(dbSum);
        sumBalance = BigDecimal.ZERO;

        doReturn(schema).when(notifyOrderService).processMigrationSchema(SHARD, dbCampaignData);

        doReturn(SHARD).when(shardHelper).getShardByCampaignId(updateRequest.getCampaignId());
        doReturn(dbCampaignData).when(notifyOrderRepository).fetchCampaignData(SHARD, updateRequest.getCampaignId());
        doReturn(false).when(notifyOrderService)
                .isSumsChanged(any(), any(), eq(updateRequest.getSumUnits()), eq(dbCampaignData.getSumUnits()));
        doReturn(false).when(notifyOrderService)
                .updateMulticurrencySums(SHARD, dbCampaignData, updateRequest, schema.getState());
        doNothing().when(notifyOrderService)
                .addLogBalance(eq(dbCampaignData), any(), any(), any(), eq(updateRequest.getTid()));
        doReturn(productInfo).when(productService).getProductById(dbCampaignData.getProductId());
    }


    /**
     * Тест проверяет, что тестовые данные подобраны так, что достигается конец метода и все if-ы не выполняются
     */
    @Test
    public void checkReturnSuccessResponse_whenAchieveEndOfMethod() {
        BalanceClientResponse balanceClientResponse = notifyOrderService.notifyOrder(updateRequest);

        assertThat(balanceClientResponse, beanDiffer(successResponse));

        //Проверяем, что if-ы по умолчанию не выполняются
        verifyZeroInteractions(postProcessingService);
        verifyZeroInteractions(campaignRepository);
        verifyZeroInteractions(campaignService);
        verify(notifyOrderService, never()).updateWalletParams(anyInt(), any(), any());
    }

    @Test
    public void checkReturnValidationResponse_whenValidateRequestFailed() {
        doReturn(errorResponse).when(validationService).validateRequest(updateRequest);

        BalanceClientResponse balanceClientResponse = notifyOrderService.notifyOrder(updateRequest);

        assertThat(balanceClientResponse, equalTo(errorResponse));
    }

    @Test
    public void checkReturnErrorResponse_whenCampaignDoesNotExistInDb() {
        doReturn(null).when(notifyOrderRepository)
                .fetchCampaignData(SHARD, updateRequest.getCampaignId());

        BalanceClientResponse balanceClientResponse = notifyOrderService.notifyOrder(updateRequest);

        String message = String.format(CAMPAIGN_DOES_NOT_EXISTS_ERROR_MESSAGE, updateRequest.getCampaignId());
        assertThat(balanceClientResponse,
                beanDiffer(BalanceClientResponse.error(CAMPAIGN_NOT_EXISTS_ERROR_CODE, message)));
    }

    @Test
    public void checkReturnSuccessResponse_whenAllMoneyFieldsAreZeroForCampaignDoesNotExistInDbAnd() {
        doReturn(null).when(notifyOrderRepository)
                .fetchCampaignData(SHARD, updateRequest.getCampaignId());

        updateRequest = new NotifyOrderParameters()
                .withCampaignId(RandomNumberUtils.nextPositiveLong())
                .withServiceId(BalanceClientServiceConstants.DIRECT_SERVICE_ID)
                .withChipsCost(BigDecimal.ZERO)
                .withChipsSpent(BigDecimal.ZERO)
                .withSumRealMoney(BigDecimal.ZERO)
                .withSumUnits(BigDecimal.ZERO)
                .withTotalSum(BigDecimal.ZERO)
                .withTid(RandomNumberUtils.nextPositiveLong())
                .withPaidByCertificate(1);

        BalanceClientResponse balanceClientResponse = notifyOrderService.notifyOrder(updateRequest);

        assertThat(balanceClientResponse, beanDiffer(successResponse));
    }

    @Test
    public void checkReturnSuccessResponse_whenAllMoneyFieldsAbsentForCampaignDoesNotExistInDbAnd() {
        doReturn(null).when(notifyOrderRepository)
                .fetchCampaignData(SHARD, updateRequest.getCampaignId());

        updateRequest = new NotifyOrderParameters()
                .withCampaignId(RandomNumberUtils.nextPositiveLong())
                .withServiceId(BalanceClientServiceConstants.DIRECT_SERVICE_ID)
                .withTid(RandomNumberUtils.nextPositiveLong())
                .withPaidByCertificate(1);

        BalanceClientResponse balanceClientResponse = notifyOrderService.notifyOrder(updateRequest);

        assertThat(balanceClientResponse, beanDiffer(successResponse));
    }

    @Test
    public void checkReturnValidationResponse_whenValidateCampaignTypeFailed() {
        doReturn(errorResponse).when(validationService)
                .validateCampaignType(updateRequest.getServiceId(), dbCampaignData.getType(),
                        dbCampaignData.getCampaignId());

        BalanceClientResponse balanceClientResponse = notifyOrderService.notifyOrder(updateRequest);

        assertThat(balanceClientResponse, equalTo(errorResponse));
    }

    @Test
    public void checkReturnSuccessResponse_whenCampaignTidGreaterBalanceTid() {
        dbCampaignData.withBalanceTid(updateRequest.getTid() + 1);

        BalanceClientResponse balanceClientResponse = notifyOrderService.notifyOrder(updateRequest);

        assertThat(balanceClientResponse, beanDiffer(successResponse));
    }

    @Test
    public void checkReturnValidationResponse_whenValidateClientCurrencyConversionStateFailed() {
        doReturn(errorResponse).when(validationService)
                .validateClientCurrencyConversionState(dbCampaignData.getClientId());

        BalanceClientResponse balanceClientResponse = notifyOrderService.notifyOrder(updateRequest);

        assertThat(balanceClientResponse, equalTo(errorResponse));
    }

    @Test
    public void checkCallUpdateMulticurrencySums() {
        notifyOrderService.notifyOrder(updateRequest);

        verify(notifyOrderService).updateMulticurrencySums(SHARD, dbCampaignData, updateRequest, schema.getState());
    }

    @Test
    public void checkReturnValidationResponse_whenValidateProductCurrencyFailed() {
        doReturn(errorResponse).when(validationService)
                .validateProductCurrency(updateRequest.getServiceId(), updateRequest.getProductCurrency(),
                        productInfo.getCurrencyCode(), updateRequest.getCampaignId());

        BalanceClientResponse balanceClientResponse = notifyOrderService.notifyOrder(updateRequest);

        assertThat(balanceClientResponse, equalTo(errorResponse));
    }

    @Test
    public void checkReturnValidationResponse_whenValidateSumOnEmptyCampaignFailed() {
        doReturn(errorResponse).when(validationService)
                .validateSumOnEmptyCampaign(eq(dbCampaignData.getStatusEmpty()), any(),
                        eq(dbCampaignData.getCampaignId()));

        BalanceClientResponse balanceClientResponse = notifyOrderService.notifyOrder(updateRequest);

        assertThat(balanceClientResponse, equalTo(errorResponse));
    }

    @Test
    public void checkCalcSum_whenCampaignModifyConverted() {
        doReturn(errorResponse).when(validationService)
                .validateSumOnEmptyCampaign(anyBoolean(), moneyArgumentCaptor.capture(), any());
        doReturn(true).when(notifyOrderService)
                .isCampaignModifyConverted(dbCampaignData.getCurrency(), dbCampaignData.getCurrencyConverted());

        notifyOrderService.notifyOrder(updateRequest);

        assertThat(moneyArgumentCaptor.getValue(),
                equalTo(Money.valueOf(updateRequest.getSumRealMoney(), CurrencyCode.RUB)));
    }

    @Test
    public void checkCalcSum_whenCampaignNotModifyConverted() {
        doReturn(errorResponse).when(validationService)
                .validateSumOnEmptyCampaign(anyBoolean(), moneyArgumentCaptor.capture(), any());
        doReturn(false).when(notifyOrderService)
                .isCampaignModifyConverted(dbCampaignData.getCurrency(), dbCampaignData.getCurrencyConverted());

        notifyOrderService.notifyOrder(updateRequest);

        assertThat(moneyArgumentCaptor.getValue(), equalTo(sum));
    }

    @Test
    public void checkCallIsCampaignModifyConverted() {
        notifyOrderService.notifyOrder(updateRequest);

        verify(notifyOrderService)
                .isCampaignModifyConverted(dbCampaignData.getCurrency(), dbCampaignData.getCurrencyConverted());
    }

    @Test
    public void checkCallAddLogBalance() {
        notifyOrderService.notifyOrder(updateRequest);

        verify(notifyOrderService).addLogBalance(dbCampaignData, sum, sumDelta, sumBalance, updateRequest.getTid());
    }

    @Test
    public void checkGetCampsInWallet() {
        dbCampaignData.withType(CampaignType.WALLET);
        List<CampaignForNotifyOrder> campsInWallet = Arrays.asList(new Campaign()
                .withId(RandomNumberUtils.nextPositiveLong()));
        doReturn(campsInWallet).when(campaignRepository)
                .getCampaignsForNotifyOrder(SHARD, dbCampaignData.getUid(), updateRequest.getCampaignId());
        doReturn(Money.valueOf(BigDecimal.ONE, dbCampaignData.getCurrency())).when(notifyOrderService).
                calcUncoveredSpents(campsInWallet, dbCampaignData.getCurrency());
        doReturn(emptyList()).when(clientRepository)
                .getClientsAutoOverdraftInfo(SHARD, List.of(ClientId.fromLong(dbCampaignData.getClientId())));

        notifyOrderService.notifyOrder(updateRequest);

        verify(campaignRepository)
                .getCampaignsForNotifyOrder(SHARD, dbCampaignData.getUid(), updateRequest.getCampaignId());
        verify(notifyOrderService).calcUncoveredSpents(campsInWallet, dbCampaignData.getCurrency());
    }

    @Test
    public void checkCallUnarcCampaign() {
        dbCampaignData.withArchived(true)
                .withWalletId(ID_NOT_SET)
                .withSumSpent(sum.bigDecimalValue().subtract(BigDecimal.ONE)); //чтобы sumRestNew был > 0

        notifyOrderService.notifyOrder(updateRequest);

        verify(campaignService).unarchiveCampaign(dbCampaignData.getUid(), dbCampaignData.getCampaignId());
    }

    @Test
    public void checkCallIsSumsChanged() {
        notifyOrderService.notifyOrder(updateRequest);

        verify(notifyOrderService).isSumsChanged(sum, dbSum, updateRequest.getSumUnits(), dbCampaignData.getSumUnits());
    }

    @Test
    public void checkCallUpdateCampaignData() {
        notifyOrderService.notifyOrder(updateRequest);

        verify(updateCampaignDataService)
                .updateCampaignData(SHARD, false, sum, sumDelta, sumBalance, dbCampaignData, updateRequest);
    }

    @Test
    public void checkCallProcessUnchangedCampaign() {
        doReturn(true).when(notifyOrderService)
                .updateMulticurrencySums(SHARD, dbCampaignData, updateRequest, schema.getState());
        doReturn(false).when(updateCampaignDataService)
                .updateCampaignData(SHARD, false, sum, sumDelta, sumBalance, dbCampaignData, updateRequest);

        notifyOrderService.notifyOrder(updateRequest);

        verify(postProcessingService).processUnchangedCampaign(SHARD, updateRequest.getCampaignId());
    }

    @Test
    public void checkCallProcessMoneyRefillFromZero() {
        dbCampaignData.withSumSpent(sum.bigDecimalValue().subtract(BigDecimal.ONE)) //чтобы sumRestNew был > 0
                .withSum(dbCampaignData.getSumSpent()); //чтобы sumRestOld был < 0

        notifyOrderService.notifyOrder(updateRequest);

        verify(postProcessingService).processMoneyRefillFromZero(SHARD, dbCampaignData, emptyList());
    }

    @Test
    public void checkCallProcessMoneyRefillFromZero_WhenCampsInWalletListNotEmpty() {
        dbCampaignData.withSumSpent(sum.bigDecimalValue().subtract(BigDecimal.ONE)) //чтобы sumRestNew был > 0
                .withSum(dbCampaignData.getSumSpent()); //чтобы sumRestOld был < 0
        dbCampaignData.withType(CampaignType.WALLET);
        List<CampaignForNotifyOrder> campsInWallet = Arrays.asList(new Campaign()
                .withId(RandomNumberUtils.nextPositiveLong()));
        doReturn(campsInWallet).when(campaignRepository)
                .getCampaignsForNotifyOrder(SHARD, dbCampaignData.getUid(), updateRequest.getCampaignId());
        doReturn(Money.valueOf(BigDecimal.ZERO, dbCampaignData.getCurrency())).when(notifyOrderService).
                calcUncoveredSpents(campsInWallet, dbCampaignData.getCurrency());

        notifyOrderService.notifyOrder(updateRequest);

        verify(postProcessingService).processMoneyRefillFromZero(SHARD, dbCampaignData, campsInWallet);
    }

    @Test
    public void checkCallProcessSumOnCampChange() {
        doReturn(true).when(notifyOrderService)
                .isSumsChanged(sum, dbSum, updateRequest.getSumUnits(), dbCampaignData.getSumUnits());

        notifyOrderService.notifyOrder(updateRequest);

        verify(postProcessingService)
                .processSumOnCampChange(SHARD, updateRequest, dbCampaignData, productInfo.getRate(), sum, sumDelta,
                        MigrationSchema.State.OLD);
    }

    @Test
    public void checkCallProcessSumOnSumBalanceChangedNewSchema() {
        sum = Money.valueOf(SUM_ON_CAMPAIGN_NEW_SCHEMA, sum.getCurrencyCode());
        sumDelta = sum.subtract(dbSum);

        doReturn(false).when(notifyOrderService)
                .isSumsChanged(sum, dbSum, updateRequest.getSumUnits(), dbCampaignData.getSumUnits());

        schema.withState(MigrationSchema.State.NEW);
        dbCampaignData.withSumBalance(RandomNumberUtils.nextPositiveBigDecimal());

        notifyOrderService.notifyOrder(updateRequest);

        verify(postProcessingService)
                .processSumOnCampChange(SHARD, updateRequest, dbCampaignData, productInfo.getRate(), sum, sumDelta,
                        MigrationSchema.State.NEW);
    }

    @Test
    public void checkReturnValidationResponse_whenValidateTotalSumFailed() {
        updateRequest.withTotalSum(null);
        dbCampaignData.withType(CampaignType.WALLET)
                .withClientWorkCurrency(dbCampaignData.getCurrency())
                .withTotalBalanceTid(null);

        BalanceClientResponse balanceClientResponse = notifyOrderService.notifyOrder(updateRequest);

        String message = invalidFieldMessage(TOTAL_SUM_FIELD_NAME, updateRequest.getTotalSum());
        assertThat(balanceClientResponse,
                beanDiffer(BalanceClientResponse.error(INVALID_TOTAL_SUM_ERROR_CODE, message)));
    }

    /**
     * Тест по DIRECT-78810: NotifyOrder: не падать при отсутствии TotalConsumeQty в нотификациях без денег.
     * <p>
     * Проверяея, что нотификацию по кошельку без TotalConsumeQty (и без денег) - принимаем и не пытаемся обновлять поля
     */
    @Test
    public void checkReturnSuccessResponse_whenTotalAbsentAndAllOtherMoneyFieldsAreZero() {
        updateRequest.withTotalSum(null)
                .withChipsCost(BigDecimal.ZERO)
                .withChipsSpent(BigDecimal.ZERO)
                .withSumRealMoney(BigDecimal.ZERO)
                .withSumUnits(BigDecimal.ZERO);
        dbCampaignData.withType(CampaignType.WALLET)
                .withClientWorkCurrency(dbCampaignData.getCurrency())
                .withTotalBalanceTid(null);

        BalanceClientResponse balanceClientResponse = notifyOrderService.notifyOrder(updateRequest);

        assertThat(balanceClientResponse, beanDiffer(successResponse));
        verify(notifyOrderService, never()).updateWalletParams(anyInt(), any(), any());
    }

    @Test
    public void checkCallUpdateWalletParams() {
        dbCampaignData.withType(CampaignType.WALLET)
                .withClientWorkCurrency(dbCampaignData.getCurrency())
                .withTotalBalanceTid(updateRequest.getTid() - 1)
                .withTotalSum(updateRequest.getTotalSum().subtract(BigDecimal.ONE));
        doNothing().when(notifyOrderService)
                .updateWalletParams(eq(SHARD), eq(dbCampaignData.getTotalBalanceTid()), any());
        ArgumentCaptor<WalletParams> captor = ArgumentCaptor.forClass(WalletParams.class);

        notifyOrderService.notifyOrder(updateRequest);

        verify(notifyOrderService)
                .updateWalletParams(eq(SHARD), eq(dbCampaignData.getTotalBalanceTid()), captor.capture());
        WalletParams expectedWalletParams = new WalletParams()
                .withTotalSum(updateRequest.getTotalSum())
                .withTotalBalanceTid(updateRequest.getTid())
                .withWalletId(dbCampaignData.getCampaignId());
        assertThat(captor.getValue(), beanDiffer(expectedWalletParams));
    }

    @Test
    public void checkCallNoWalletCampaignsZeroConsumeQty() {
        updateRequest.withTotalSum(BigDecimal.ZERO);
        dbCampaignData.withType(CampaignType.WALLET)
                .withClientWorkCurrency(dbCampaignData.getCurrency())
                .withTotalBalanceTid(null)
                .withTotalSum(null);

        notifyOrderService.notifyOrder(updateRequest);

        verify(notifyOrderService, never())
                .updateWalletParams(anyInt(), any(), any());
    }

    @Test
    public void checkCallNoWalletCampaignsNonZeroConsumeQty() {
        updateRequest.withTotalSum(BigDecimal.ONE);
        dbCampaignData.withType(CampaignType.WALLET)
                .withClientWorkCurrency(dbCampaignData.getCurrency())
                .withTotalBalanceTid(null)
                .withTotalSum(null);

        doNothing().when(walletParamsRepository)
                .addWalletParams(eq(SHARD), any());
        ArgumentCaptor<WalletParams> captor = ArgumentCaptor.forClass(WalletParams.class);

        notifyOrderService.notifyOrder(updateRequest);

        verify(walletParamsRepository)
                .addWalletParams(eq(SHARD), captor.capture());
        WalletParams expectedWalletParams = new WalletParams()
                .withTotalSum(updateRequest.getTotalSum())
                .withTotalBalanceTid(updateRequest.getTid())
                .withWalletId(dbCampaignData.getCampaignId());
        assertThat(captor.getValue(), beanDiffer(expectedWalletParams));
    }

    @Test
    public void checkCallUpdateCampaignDataOnNewSchemaAndCampaignUnderWallet() {
        initNewSchemaSums(false, false);

        notifyOrderService.notifyOrder(updateRequest);

        verify(updateCampaignDataService)
                .updateCampaignData(SHARD, false, sum, sumDelta, sumBalance, dbCampaignData, updateRequest);
    }

    @Test
    public void checkCallUpdateCampaignDataOnNewSchemaAndWallet() {
        initNewSchemaSums(true, false);

        notifyOrderService.notifyOrder(updateRequest);

        verify(updateCampaignDataService)
                .updateCampaignData(SHARD, false, sum, sumDelta, sumBalance, dbCampaignData, updateRequest);
    }

    @Test
    public void checkCallUpdateCampaignDataOnNewSchemaAndEmptyWallet() {
        initNewSchemaSums(true, true);

        notifyOrderService.notifyOrder(updateRequest);

        verify(updateCampaignDataService)
                .updateCampaignData(SHARD, false, sum, sumDelta, sumBalance, dbCampaignData, updateRequest);
    }

    private void initNewSchemaSums(boolean isWallet, boolean isEmptyWallet) {
        schema.withState(MigrationSchema.State.NEW);

        sumBalance = sum.bigDecimalValue();
        dbCampaignData.withSumBalance(sumBalance);

        BigDecimal value;
        if (isWallet) {
            dbCampaignData.withType(CampaignType.WALLET);
            value = updateRequest.getTotalSum();
            if (isEmptyWallet) {
                value = sum.bigDecimalValue();
                updateRequest.withTotalSum(null);
            }
        } else {
            value = SUM_ON_CAMPAIGN_NEW_SCHEMA;
        }
        sum = Money.valueOf(value, sum.getCurrencyCode());
        sumDelta = sum.subtract(dbSum);
    }
}
