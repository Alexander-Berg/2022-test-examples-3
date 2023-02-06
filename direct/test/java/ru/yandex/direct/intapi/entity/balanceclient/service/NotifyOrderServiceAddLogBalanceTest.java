package ru.yandex.direct.intapi.entity.balanceclient.service;

import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.common.log.container.LogCampaignBalanceData;
import ru.yandex.direct.common.log.service.LogCampaignBalanceService;
import ru.yandex.direct.core.entity.balanceaggrmigration.lock.AggregateMigrationRedisLockService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.xiva.XivaPushesQueueService;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.intapi.entity.balanceclient.container.CampaignDataForNotifyOrder;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

/**
 * Тесты на внутренний метод addLogBalance из NotifyOrderService
 *
 * @see NotifyOrderService
 */
public class NotifyOrderServiceAddLogBalanceTest {

    @Mock
    AggregateMigrationRedisLockService migrationRedisLockService;

    @Mock
    private LogCampaignBalanceService logCampaignBalanceService;

    @Mock
    private FeatureService featureService;

    @Mock
    private XivaPushesQueueService xivaPushesQueueService;

    @Captor
    private ArgumentCaptor<LogCampaignBalanceData> logCampaignBalanceDataArgumentCaptor;


    private NotifyOrderService notifyOrderService;
    private CampaignDataForNotifyOrder dbCampaignData;
    private Money sum;
    private Money sumDelta;
    private BigDecimal sumBalance;
    private long balanceTid;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        notifyOrderService = new NotifyOrderService(
                null,
                null,
                null,
                null,
                null,
                logCampaignBalanceService,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                migrationRedisLockService,
                featureService,
                xivaPushesQueueService);

        dbCampaignData = NotifyOrderTestHelper.generateCampaignDataForNotifyOrder();
        sum = Money.valueOf(RandomNumberUtils.nextPositiveBigDecimal(), dbCampaignData.getCurrency());
        sumDelta = Money.valueOf(RandomNumberUtils.nextPositiveBigDecimal(), dbCampaignData.getCurrency());
        sumBalance = RandomNumberUtils.nextPositiveBigDecimal();
        balanceTid = RandomNumberUtils.nextPositiveLong();
    }

    @Test
    public void checkAddLogBalance() {
        LogCampaignBalanceData expectedLogCampaignBalanceData = new LogCampaignBalanceData()
                .withCid(dbCampaignData.getCampaignId())
                .withType(dbCampaignData.getType().name().toLowerCase())
                .withCurrency(dbCampaignData.getCurrency().name())
                .withClientId(dbCampaignData.getClientId())
                .withTid(balanceTid)
                .withSum(sum.bigDecimalValue())
                .withSumBalance(sumBalance)
                .withSumDelta(sumDelta.bigDecimalValue());

        notifyOrderService.addLogBalance(dbCampaignData, sum, sumDelta, sumBalance, balanceTid);

        verify(logCampaignBalanceService).logCampaignBalance(logCampaignBalanceDataArgumentCaptor.capture());
        assertThat(logCampaignBalanceDataArgumentCaptor.getValue(), beanDiffer(expectedLogCampaignBalanceData));
    }
}
