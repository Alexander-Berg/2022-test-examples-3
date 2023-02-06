package ru.yandex.direct.intapi.entity.balanceclient.service;

import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaignpayment.repository.CampPaymentsInfoRepository;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.balanceclient.container.NotifyOrderDbCampaignChanges;
import ru.yandex.direct.intapi.entity.balanceclient.exception.BalanceClientException;
import ru.yandex.direct.intapi.entity.balanceclient.repository.NotifyOrderRepository;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static java.lang.String.format;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.intapi.entity.balanceclient.BalanceClientServiceConstants.BAYAN_SERVICE_ID;
import static ru.yandex.direct.intapi.entity.balanceclient.service.NotifyOrderUpdateCampaignDataService.CANT_UPDATE_CAMPAIGN_ERROR_MESSAGE;

/**
 * Тесты на метод updateCampaignDataInTransaction из NotifyOrderUpdateCampaignDataService
 *
 * @see NotifyOrderUpdateCampaignDataService
 */
@RunWith(SpringJUnit4ClassRunner.class)
@IntApiTest
public class NotifyOrderUpdateCampaignDataServiceUpdateCampaignDataInTransactionTest {

    private static final int SHARD = 1;
    private static final CurrencyCode CURRENCY = CurrencyCode.RUB;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Mock
    private NotifyOrderRepository notifyOrderRepository;

    @Mock
    private CampPaymentsInfoRepository campPaymentsInfoRepository;

    @Captor
    private ArgumentCaptor<NotifyOrderDbCampaignChanges> campaignChangesArgumentCaptor;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private NotifyOrderUpdateCampaignDataService service;
    private Long campaignId;
    private Long balanceTid;
    private Long campaignBalanceTid;
    private Long campaignWalletId;
    private Money sumDelta;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        service = spy(new NotifyOrderUpdateCampaignDataService(
                notifyOrderRepository,
                dslContextProvider,
                campPaymentsInfoRepository,
                BAYAN_SERVICE_ID));

        campaignId = RandomNumberUtils.nextPositiveLong();
        balanceTid = RandomNumberUtils.nextPositiveLong();
        campaignBalanceTid = RandomNumberUtils.nextPositiveLong();
        campaignWalletId = RandomNumberUtils.nextPositiveLong();
        sumDelta = Money.valueOf(RandomNumberUtils.nextPositiveBigDecimal(), CURRENCY);

        doReturn(true).when(notifyOrderRepository).updateCampaignData(any(), any(), any(), any(), any());
    }


    @Test
    public void checkUpdateCampaignData() {
        NotifyOrderDbCampaignChanges campaignChanges = new NotifyOrderDbCampaignChanges()
                .withBalanceTid(balanceTid)
                .withChanged(true);

        service.updateCampaignDataInTransaction(SHARD, campaignId, balanceTid, campaignBalanceTid, campaignWalletId,
                campaignChanges, sumDelta);

        verify(notifyOrderRepository).updateCampaignData(any(), any(), any(), any(),
                campaignChangesArgumentCaptor.capture());
        NotifyOrderDbCampaignChanges expectedChanges = new NotifyOrderDbCampaignChanges()
                .withBalanceTid(balanceTid)
                .withChanged(true);
        assertThat(campaignChangesArgumentCaptor.getValue(), beanDiffer(expectedChanges));
    }

    @Test
    public void checkGetException_WhenCantUpdateCampaign() {
        doReturn(false).when(notifyOrderRepository).updateCampaignData(any(), any(), any(), any(), any());
        thrown.expect(BalanceClientException.class);
        thrown.expectMessage(format(CANT_UPDATE_CAMPAIGN_ERROR_MESSAGE, campaignId, balanceTid, campaignWalletId));

        service.updateCampaignDataInTransaction(SHARD, campaignId, balanceTid, campaignBalanceTid, campaignWalletId,
                null, sumDelta);
    }

    @Test
    public void checkAddCampaignPayment() {
        service.updateCampaignDataInTransaction(SHARD, campaignId, balanceTid,
                campaignBalanceTid, campaignWalletId, null, sumDelta);

        verify(campPaymentsInfoRepository).addCampaignPayment(any(), eq(campaignId), eq(sumDelta.bigDecimalValue()));
    }

    @Test
    public void checkNotAddCampaignPayment_WhenSumDeltaIsZero() {
        sumDelta = Money.valueOf(BigDecimal.ZERO, CURRENCY);

        service.updateCampaignDataInTransaction(SHARD, campaignId, balanceTid, campaignBalanceTid, campaignWalletId,
                null, sumDelta);

        verifyZeroInteractions(campPaymentsInfoRepository);
    }

    @Test
    public void checkNotAddCampaignPayment_WhenSumDeltaLessThanZero() {
        sumDelta = Money.valueOf(-1, CURRENCY);

        service.updateCampaignDataInTransaction(SHARD, campaignId, balanceTid, campaignBalanceTid, campaignWalletId,
                null, sumDelta);

        verifyZeroInteractions(campPaymentsInfoRepository);
    }
}
