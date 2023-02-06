package ru.yandex.direct.intapi.entity.balanceclient.service;

import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.core.entity.balanceaggrmigration.lock.AggregateMigrationRedisLockService;
import ru.yandex.direct.core.entity.campaign.model.CampaignMulticurrencySums;
import ru.yandex.direct.core.entity.campaign.repository.CampaignsMulticurrencySumsRepository;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.walletparams.service.WalletParamsService;
import ru.yandex.direct.core.entity.xiva.XivaPushesQueueService;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.intapi.entity.balanceclient.container.CampaignDataForNotifyOrder;
import ru.yandex.direct.intapi.entity.balanceclient.exception.BalanceClientException;
import ru.yandex.direct.intapi.entity.balanceclient.model.NotifyOrderParameters;
import ru.yandex.direct.intapi.entity.balanceclient.service.migration.MigrationSchema;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static java.lang.String.format;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.intapi.entity.balanceclient.service.NotifyOrderService.CANT_UPDATE_CAMPAIGNS_MULTICURRENCY_SUMS_ERROR_MESSAGE;

public class NotifyOrderServiceMulticurrencySumsTest {
    private static final int SHARD = 1;
    private static final long CID = 1000000;
    private static final BigDecimal SUM_REAL_MONEY = BigDecimal.valueOf(100);
    private static final BigDecimal CHIPS_COST = BigDecimal.valueOf(20);
    private static final BigDecimal CHIPS_SPENT = BigDecimal.valueOf(10);
    private static final BigDecimal DIFFERENT_CHIPS_SPENT = BigDecimal.valueOf(15);

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private CampaignsMulticurrencySumsRepository campaignsMulticurrencySumsRepository;

    @Mock
    private WalletParamsService walletParamsService;

    @Mock
    private AggregateMigrationRedisLockService migrationRedisLockService;

    @Mock
    private FeatureService featureService;

    @Mock
    private XivaPushesQueueService xivaPushesQueueService;

    @Captor
    private ArgumentCaptor<CampaignMulticurrencySums> requestCaptor;

    private NotifyOrderService notifyOrderService;

    private MigrationSchema.State state;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        notifyOrderService = new NotifyOrderService(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                campaignsMulticurrencySumsRepository,
                null,
                walletParamsService,
                null,
                migrationRedisLockService,
                featureService,
                xivaPushesQueueService);

        state = MigrationSchema.State.OLD;
    }

    private NotifyOrderParameters notifyOrderParameters() {
        return new NotifyOrderParameters()
                .withCampaignId(CID)
                .withChipsCost(CHIPS_COST)
                .withChipsSpent(CHIPS_SPENT)
                .withSumRealMoney(SUM_REAL_MONEY)
                .withTid(RandomNumberUtils.nextPositiveLong());
    }

    private CampaignDataForNotifyOrder campaignDataForNotifyOrder() {
        return new CampaignDataForNotifyOrder()
                .withCampaignId(CID)
                .withCmsSum(SUM_REAL_MONEY)
                .withCmsChipsCost(CHIPS_COST)
                .withCmsChipsSpent(CHIPS_SPENT);
    }

    @Test
    public void testUpdateMulticurrencySumsTrueOnYndFixedAndDifferentSums() {
        NotifyOrderParameters updateRequest = notifyOrderParameters()
                .withChipsSpent(DIFFERENT_CHIPS_SPENT);

        CampaignDataForNotifyOrder dbCampaignData = campaignDataForNotifyOrder()
                .withCurrency(CurrencyCode.YND_FIXED);
        assertTrue("Для валюты YND_FIXED результат true, если деньги изменились",
                notifyOrderService.updateMulticurrencySums(SHARD, dbCampaignData, updateRequest, state));

        verify(campaignsMulticurrencySumsRepository)
                .insertCampaignsMulticurrencySums(eq(SHARD), requestCaptor.capture());

        assertThat("Сохраняемая запись корректна", requestCaptor.getValue(), allOf(
                hasProperty("id", equalTo(CID)),
                hasProperty("sum", equalTo(SUM_REAL_MONEY)),
                hasProperty("chipsCost", equalTo(CHIPS_COST)),
                hasProperty("chipsSpent", equalTo(DIFFERENT_CHIPS_SPENT)),
                hasProperty("avgDiscount", equalTo(BigDecimal.ZERO)),
                hasProperty("balanceTid", equalTo(updateRequest.getTid()))
        ));
    }

    @Test
    public void testUpdateMulticurrencySumsTrueOnRubConvertedAndDifferentSums() {
        NotifyOrderParameters updateRequest = notifyOrderParameters()
                .withChipsSpent(DIFFERENT_CHIPS_SPENT);

        CampaignDataForNotifyOrder dbCampaignData = campaignDataForNotifyOrder()
                .withCurrency(CurrencyCode.RUB)
                .withCurrencyConverted(true);
        assertTrue("Для валюты RUB и currencyConverted=true результат true, если деньги изменились",
                notifyOrderService.updateMulticurrencySums(SHARD, dbCampaignData, updateRequest, state));
    }

    @Test
    public void testUpdateMulticurrencySumsOnTrueUpdateAndNewSchemaUpdateTotalCost() {
        NotifyOrderParameters updateRequest = notifyOrderParameters()
                .withChipsCost(DIFFERENT_CHIPS_SPENT);

        CampaignDataForNotifyOrder dbCampaignData = campaignDataForNotifyOrder()
                .withWalletId(1L)
                .withCurrency(CurrencyCode.RUB)
                .withCurrencyConverted(true);

        notifyOrderService.updateMulticurrencySums(SHARD, dbCampaignData, updateRequest, MigrationSchema.State.NEW);

        verify(walletParamsService).updateTotalCost(dbCampaignData.getWalletId());
    }

    @Test
    public void testUpdateMulticurrencySumsFalseOnRubNotConvertedAndDifferentSums() {
        NotifyOrderParameters updateRequest = notifyOrderParameters()
                .withChipsSpent(DIFFERENT_CHIPS_SPENT);

        CampaignDataForNotifyOrder dbCampaignData = campaignDataForNotifyOrder()
                .withCurrency(CurrencyCode.RUB)
                .withCurrencyConverted(false);
        assertFalse("Для валюты RUB и currencyConverted=false результат false",
                notifyOrderService.updateMulticurrencySums(SHARD, dbCampaignData, updateRequest, state));
    }

    @Test
    public void testUpdateMulticurrencySumsTrueOnRubConvertedDifferentSumsAndBalanceTid() {
        NotifyOrderParameters updateRequest = notifyOrderParameters()
                .withChipsSpent(DIFFERENT_CHIPS_SPENT);

        long tid = RandomNumberUtils.nextPositiveLong();
        CampaignDataForNotifyOrder dbCampaignData = campaignDataForNotifyOrder()
                .withCurrency(CurrencyCode.RUB)
                .withCurrencyConverted(true)
                .withCmsBalanceTid(tid);

        when(campaignsMulticurrencySumsRepository
                .updateCampaignsMulticurrencySumsByCidAndBalanceTid(eq(SHARD), any(), eq(tid))).thenReturn(true);
        assertTrue(
                "Для валюты RUB, currencyConverted=true и наличия cmsBalanceTid результат true, если деньги изменились",
                notifyOrderService.updateMulticurrencySums(SHARD, dbCampaignData, updateRequest, state));

        verify(campaignsMulticurrencySumsRepository)
                .updateCampaignsMulticurrencySumsByCidAndBalanceTid(eq(SHARD), requestCaptor.capture(), eq(tid));

        assertThat("Сохраняемая запись корректна", requestCaptor.getValue(), allOf(
                hasProperty("id", equalTo(CID)),
                hasProperty("sum", equalTo(SUM_REAL_MONEY)),
                hasProperty("chipsCost", equalTo(CHIPS_COST)),
                hasProperty("chipsSpent", equalTo(DIFFERENT_CHIPS_SPENT)),
                hasProperty("avgDiscount", equalTo(BigDecimal.ZERO)),
                hasProperty("balanceTid", equalTo(updateRequest.getTid()))
        ));
    }

    @Test
    public void testUpdateMulticurrencySumsThrowsUpdateError() {
        NotifyOrderParameters updateRequest = notifyOrderParameters()
                .withChipsSpent(DIFFERENT_CHIPS_SPENT);

        long tid = RandomNumberUtils.nextPositiveLong();
        CampaignDataForNotifyOrder dbCampaignData = campaignDataForNotifyOrder()
                .withCurrency(CurrencyCode.RUB)
                .withCurrencyConverted(true)
                .withCmsBalanceTid(tid);

        when(campaignsMulticurrencySumsRepository
                .updateCampaignsMulticurrencySumsByCidAndBalanceTid(eq(SHARD), any(), eq(tid))).thenReturn(false);
        // Ожидаем падения, так как обновлено 0 строк
        thrown.expect(BalanceClientException.class);
        thrown.expectMessage(format(CANT_UPDATE_CAMPAIGNS_MULTICURRENCY_SUMS_ERROR_MESSAGE,
                dbCampaignData.getCampaignId(), updateRequest.getTid()));
        notifyOrderService.updateMulticurrencySums(SHARD, dbCampaignData, updateRequest, state);
    }

    @Test
    public void testUpdateMulticurrencySumsFalseOnMostCurrenciesDifferentSums() {
        NotifyOrderParameters updateRequest = notifyOrderParameters()
                .withChipsSpent(DIFFERENT_CHIPS_SPENT);
        for (CurrencyCode currencyCode : CurrencyCode.values()) {
            if (currencyCode == CurrencyCode.RUB || currencyCode == CurrencyCode.YND_FIXED) {
                continue;
            }

            CampaignDataForNotifyOrder dbCampaignData = campaignDataForNotifyOrder()
                    .withCurrency(currencyCode);
            assertFalse(format("Для валюты %s результат всегда false", currencyCode),
                    notifyOrderService.updateMulticurrencySums(SHARD, dbCampaignData, updateRequest, state));
        }
    }

    @Test
    public void testUpdateMulticurrencySumsFalseOnAllCurrenciesSameSums() {
        NotifyOrderParameters updateRequest = notifyOrderParameters();
        for (CurrencyCode currencyCode : CurrencyCode.values()) {
            CampaignDataForNotifyOrder dbCampaignData = campaignDataForNotifyOrder()
                    .withCurrency(currencyCode);
            assertFalse(format("Для валюты %s результат всегда false, если деньги не изменились", currencyCode),
                    notifyOrderService.updateMulticurrencySums(SHARD, dbCampaignData, updateRequest, state));
        }
    }
}
