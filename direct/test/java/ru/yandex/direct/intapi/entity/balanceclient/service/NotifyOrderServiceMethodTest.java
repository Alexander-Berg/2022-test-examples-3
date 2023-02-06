package ru.yandex.direct.intapi.entity.balanceclient.service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.core.entity.balanceaggrmigration.lock.AggregateMigrationRedisLockService;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignForNotifyOrder;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.walletparams.container.WalletParams;
import ru.yandex.direct.core.entity.walletparams.repository.WalletParamsRepository;
import ru.yandex.direct.core.entity.xiva.XivaPushesQueueService;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

/**
 * Тесты на внутренние методы NotifyOrderService: updateWalletParams и calcUncoveredSpents
 *
 * @see NotifyOrderService
 */
public class NotifyOrderServiceMethodTest {

    private static final int SHARD = 1;
    private static final CurrencyCode CURRENCY_CODE = CurrencyCode.RUB;

    @Mock
    private WalletParamsRepository walletCampaignsRepository;

    @Mock
    private AggregateMigrationRedisLockService migrationRedisLockService;

    @Mock
    private FeatureService featureService;

    @Mock
    private XivaPushesQueueService xivaPushesQueueService;

    @Captor
    private ArgumentCaptor<WalletParams> walletParamsArgumentCaptor;

    private NotifyOrderService notifyOrderService;
    private WalletParams walletCampaign;
    private Long currentWalletTid;

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
                null,
                walletCampaignsRepository,
                null,
                null,
                migrationRedisLockService,
                featureService,
                xivaPushesQueueService);

        walletCampaign = new WalletParams()
                .withTotalSum(RandomNumberUtils.nextPositiveBigDecimal())
                .withTotalBalanceTid(RandomNumberUtils.nextPositiveLong())
                .withWalletId(RandomNumberUtils.nextPositiveLong());
        currentWalletTid = RandomNumberUtils.nextPositiveLong();
    }

    @Test
    public void checkUpdateWalletParams() {
        WalletParams expectedWalletCampaignParam = new WalletParams()
                .withTotalSum(walletCampaign.getTotalSum())
                .withTotalBalanceTid(walletCampaign.getTotalBalanceTid())
                .withWalletId(walletCampaign.getWalletId());
        notifyOrderService.updateWalletParams(SHARD, currentWalletTid, walletCampaign);

        verify(walletCampaignsRepository)
                .updateWalletParams(eq(SHARD), eq(currentWalletTid), walletParamsArgumentCaptor.capture());
        assertThat("проверяем, что объект walletCampaign был передан без изменений в репозиторий",
                walletParamsArgumentCaptor.getValue(), beanDiffer(expectedWalletCampaignParam));
    }

    @Test
    public void checkUpdateWalletParamsLogging_WhenNotUpdatedParams() {
        WalletParams spyWalletCampaign = spy(walletCampaign);
        doReturn(false).when(walletCampaignsRepository)
                .updateWalletParams(SHARD, currentWalletTid, spyWalletCampaign);
        notifyOrderService.updateWalletParams(SHARD, currentWalletTid, spyWalletCampaign);

        verify(spyWalletCampaign).getWalletId();
        verify(spyWalletCampaign).getTotalBalanceTid();
    }

    @Test
    public void checkUpdateWalletParamsLogging_WhenUpdatedParams() {
        WalletParams spyWalletCampaign = spy(walletCampaign);
        doReturn(true).when(walletCampaignsRepository)
                .updateWalletParams(SHARD, currentWalletTid, spyWalletCampaign);
        notifyOrderService.updateWalletParams(SHARD, currentWalletTid, spyWalletCampaign);

        verifyZeroInteractions(spyWalletCampaign);
    }

    @Test
    public void checkUpdateWalletParams_WhenCurrentWalletTidIsNull() {
        currentWalletTid = null;
        WalletParams expectedWalletCampaignParam = new WalletParams()
                .withTotalSum(walletCampaign.getTotalSum())
                .withTotalBalanceTid(walletCampaign.getTotalBalanceTid())
                .withWalletId(walletCampaign.getWalletId());
        notifyOrderService.updateWalletParams(SHARD, currentWalletTid, walletCampaign);

        verify(walletCampaignsRepository)
                .addWalletParams(eq(SHARD), walletParamsArgumentCaptor.capture());
        assertThat("проверяем, что объект walletCampaign был передан без изменений в репозиторий",
                walletParamsArgumentCaptor.getValue(), beanDiffer(expectedWalletCampaignParam));
    }

    @Test
    public void checkCalcUncoveredSpents() {
        int campaignCount = 3;
        List<CampaignForNotifyOrder> campsInWallet = Stream.generate(Campaign::new)
                .limit(campaignCount)
                .map(campaign -> campaign
                        .withSum(RandomNumberUtils.nextPositiveBigDecimal())
                        .withSumSpent(campaign.getSum().add(RandomNumberUtils.nextPositiveBigDecimal())))
                .collect(Collectors.toList());
        Money result = notifyOrderService.calcUncoveredSpents(campsInWallet, CURRENCY_CODE);

        BigDecimal expectedUncoveredSpents = campsInWallet.stream()
                .map(camp -> camp.getSumSpent().subtract(camp.getSum()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(result, equalTo(Money.valueOf(expectedUncoveredSpents, CURRENCY_CODE)));
    }

    @Test
    public void checkCalcUncoveredSpents_WhenAllSumsGreaterThenSumSpent() {
        int campaignCount = 2;
        List<CampaignForNotifyOrder> campsInWallet = Stream.generate(Campaign::new)
                .limit(campaignCount)
                .map(campaign -> campaign
                        .withSum(RandomNumberUtils.nextPositiveBigDecimal())
                        .withSumSpent(campaign.getSum().subtract(RandomNumberUtils.nextPositiveBigDecimal())))
                .collect(Collectors.toList());
        Money result = notifyOrderService.calcUncoveredSpents(campsInWallet, CURRENCY_CODE);

        assertThat(result, equalTo(Money.valueOf(BigDecimal.ZERO, CURRENCY_CODE)));
    }

    @Test
    public void checkCalcUncoveredSpents_WhenCampsInWalletListIsEmpty() {
        Money result = notifyOrderService.calcUncoveredSpents(Collections.emptyList(), CURRENCY_CODE);

        assertThat(result, equalTo(Money.valueOf(BigDecimal.ZERO, CURRENCY_CODE)));
    }

    @Test
    public void checkSumsNotChanged() {
        Money sum = Money.valueOf(RandomNumberUtils.nextPositiveBigDecimal(), CURRENCY_CODE);
        Money dbSum = Money.valueOf(sum.bigDecimalValue(), CURRENCY_CODE);
        Long dbSumUnits = RandomNumberUtils.nextPositiveLong();

        boolean result = notifyOrderService.isSumsChanged(sum, dbSum, BigDecimal.valueOf(dbSumUnits), dbSumUnits);

        assertThat(result, is(false));
    }

    @Test
    public void checkSumsChanged_WhenDbSumNotEqualBalanceSum() {
        Money sum = Money.valueOf(RandomNumberUtils.nextPositiveBigDecimal(), CURRENCY_CODE);
        Money dbSum = Money.valueOf(RandomNumberUtils.nextPositiveBigDecimal(), CURRENCY_CODE);
        Long dbSumUnits = RandomNumberUtils.nextPositiveLong();

        boolean result = notifyOrderService.isSumsChanged(sum, dbSum, BigDecimal.valueOf(dbSumUnits), dbSumUnits);

        assertThat(result, is(true));
    }

    @Test
    public void checkSumsChanged_WhenDbSumUnitsNotEqualBalanceSumUnits() {
        Money sum = Money.valueOf(RandomNumberUtils.nextPositiveBigDecimal(), CURRENCY_CODE);
        BigDecimal sumUnits = BigDecimal.valueOf(RandomNumberUtils.nextPositiveLong());
        Long dbSumUnits = RandomNumberUtils.nextPositiveLong();

        boolean result = notifyOrderService.isSumsChanged(sum, sum, sumUnits, dbSumUnits);

        assertThat(result, is(true));
    }

    @Test
    public void checkIsCampaignModifyConverted() {
        boolean result = notifyOrderService.isCampaignModifyConverted(CurrencyCode.RUB, true);

        assertThat(result, is(true));
    }

    @Test
    public void checkIsCampaignNotModifyConverted() {
        boolean result = notifyOrderService.isCampaignModifyConverted(CurrencyCode.RUB, false);

        assertThat(result, is(false));
    }

    @Test
    public void checkIsCampaignNotModifyConverted_whenCurrencyConvertedIsNull() {
        boolean result = notifyOrderService.isCampaignModifyConverted(CurrencyCode.RUB, null);

        assertThat(result, is(false));
    }

    @Test
    public void checkIsCampaignNotModifyConverted_whenCurrencyIsNotRub() {
        boolean result = notifyOrderService.isCampaignModifyConverted(CurrencyCode.EUR, true);

        assertThat(result, is(false));
    }
}
