package ru.yandex.direct.intapi.entity.balanceclient.service;

import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.StatusMail;
import ru.yandex.direct.currency.Currencies;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.intapi.entity.balanceclient.container.CampaignDataForNotifyOrder;
import ru.yandex.direct.intapi.entity.balanceclient.container.NotifyOrderDbCampaignChanges;
import ru.yandex.direct.intapi.entity.balanceclient.model.NotifyOrderParameters;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.campaign.MediaCampaignUtil.calcMediaSumSpent;
import static ru.yandex.direct.intapi.entity.balanceclient.BalanceClientServiceConstants.BAYAN_SERVICE_ID;
import static ru.yandex.direct.intapi.entity.balanceclient.BalanceClientServiceConstants.DIRECT_SERVICE_ID;
import static ru.yandex.direct.intapi.entity.balanceclient.service.NotifyOrderUpdateCampaignDataService.PAID_BY_CERTIFICATE;

/**
 * Тесты на методы NotifyOrderUpdateCampaignDataService
 *
 * @see NotifyOrderUpdateCampaignDataService
 */
public class NotifyOrderUpdateCampaignDataServiceMethodTest {

    private static final int SHARD = 1;

    @Captor
    private ArgumentCaptor<NotifyOrderDbCampaignChanges> campaignChangesArgumentCaptor;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private NotifyOrderUpdateCampaignDataService service;
    private CampaignDataForNotifyOrder dbCampaignData;
    private NotifyOrderParameters updateRequest;
    private Money sum;
    private Money sumDelta;
    private BigDecimal sumBalance;
    private long sumUnits;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        service = spy(new NotifyOrderUpdateCampaignDataService(
                null,
                null,
                null,
                BAYAN_SERVICE_ID));

        updateRequest = NotifyOrderTestHelper.generateNotifyOrderParameters();
        dbCampaignData = NotifyOrderTestHelper.generateCampaignDataForNotifyOrder()
                .withCampaignId(updateRequest.getCampaignId())
                .withBalanceTid(updateRequest.getTid())
                .withWalletId(RandomNumberUtils.nextPositiveLong());
        sum = Money.valueOf(RandomNumberUtils.nextPositiveBigDecimal(), dbCampaignData.getCurrency());
        sumDelta = Money.valueOf(BigDecimal.ZERO, dbCampaignData.getCurrency());
        sumBalance = RandomNumberUtils.nextPositiveBigDecimal();
        sumUnits = service.getSumUnitsByServiceId(updateRequest.getServiceId(), updateRequest.getSumUnits());

        doNothing().when(service).updateCampaignDataInTransaction(eq(SHARD), any(), any(), any(), any(), any(), any());
    }


    @Test
    public void checkGetSumUnitsByServiceId_ForDirectCampaign() {
        BigDecimal campSum = BigDecimal.valueOf(123.456);

        Long sumUnitsByServiceId = service.getSumUnitsByServiceId(DIRECT_SERVICE_ID, campSum);
        assertThat(sumUnitsByServiceId, equalTo(campSum.longValue()));
    }

    @Test
    public void checkGetSumUnitsByServiceId_ForMcbCampaign() {
        long sup = RandomNumberUtils.nextPositiveLong();

        Long sumUnitsByServiceId = service.getSumUnitsByServiceId(BAYAN_SERVICE_ID, BigDecimal.valueOf(sup));
        assertThat(sumUnitsByServiceId, equalTo(sup));
    }

    @Test
    public void checkGetSumUnitsByServiceId_ForMcbCampaign_WhenRoundingNecessary() {
        thrown.expect(ArithmeticException.class);
        thrown.expectMessage("Rounding necessary");

        //Баланс присылает только целые числа для Баяновских кампаний, иначе падаем
        service.getSumUnitsByServiceId(BAYAN_SERVICE_ID, BigDecimal.valueOf(123.456));
    }

    @Test
    public void checkNotUpdateCampaign_WhenNotChanges() {
        boolean result =
                service.updateCampaignData(SHARD, false, sum, sumDelta, sumBalance, dbCampaignData, updateRequest);

        assertThat(result, is(false));
        verify(service, never()).updateCampaignDataInTransaction(eq(SHARD), any(), any(), any(), any(), any(), any());
    }

    @Test
    public void checkReturnCampaignNotChanged_WhenUpdatedOnlyTid() {
        dbCampaignData.withBalanceTid(updateRequest.getTid() - 1);

        boolean result =
                service.updateCampaignData(SHARD, false, sum, sumDelta, sumBalance, dbCampaignData, updateRequest);

        verify(service).updateCampaignDataInTransaction(eq(SHARD), eq(updateRequest.getCampaignId()),
                eq(updateRequest.getTid()), eq(dbCampaignData.getBalanceTid()), eq(dbCampaignData.getWalletId()),
                campaignChangesArgumentCaptor.capture(), eq(sumDelta));
        NotifyOrderDbCampaignChanges expectedChanges = new NotifyOrderDbCampaignChanges()
                .withBalanceTid(updateRequest.getTid())
                .withChanged(true);
        assertThat(campaignChangesArgumentCaptor.getValue(), beanDiffer(expectedChanges));
        assertThat(result, is(false));
    }

    @Test
    public void checkSetSumSumUnitsSumBalance_InNotifyOrderDbCampaignChanges() {
        boolean result =
                service.updateCampaignData(SHARD, true, sum, sumDelta, sumBalance, dbCampaignData, updateRequest);

        verify(service).updateCampaignDataInTransaction(eq(SHARD), eq(updateRequest.getCampaignId()),
                eq(updateRequest.getTid()), eq(dbCampaignData.getBalanceTid()), eq(dbCampaignData.getWalletId()),
                campaignChangesArgumentCaptor.capture(), eq(sumDelta));
        NotifyOrderDbCampaignChanges expectedChanges = new NotifyOrderDbCampaignChanges()
                .withSumUnits(sumUnits)
                .withSum(sum.bigDecimalValue())
                .withSumBalance(sumBalance)
                .withChanged(true);
        assertThat(campaignChangesArgumentCaptor.getValue(), beanDiffer(expectedChanges));
        assertThat(result, is(true));
    }

    @Test
    public void checkSetSumLastAndStatusMailAndSumToPay_InNotifyOrderDbCampaignChanges_WhenSumDeltaEqualMinPay() {
        BigDecimal minPay = Currencies.getCurrency(dbCampaignData.getCurrency()).getMinSumInterpreteAsPayment();
        sumDelta = Money.valueOf(minPay, dbCampaignData.getCurrency());

        boolean result =
                service.updateCampaignData(SHARD, true, sum, sumDelta, sumBalance, dbCampaignData, updateRequest);

        verify(service).updateCampaignDataInTransaction(eq(SHARD), eq(updateRequest.getCampaignId()),
                eq(updateRequest.getTid()), eq(dbCampaignData.getBalanceTid()), eq(dbCampaignData.getWalletId()),
                campaignChangesArgumentCaptor.capture(), eq(sumDelta));
        NotifyOrderDbCampaignChanges expectedChanges = new NotifyOrderDbCampaignChanges()
                .withSumLast(sumDelta.bigDecimalValue())
                .withStatusMail(StatusMail.NOT_SENT)
                .withSumToPay(BigDecimal.ZERO)
                .withSumUnits(sumUnits)
                .withSum(sum.bigDecimalValue())
                .withSumBalance(sumBalance)
                .withChanged(true);
        assertThat(campaignChangesArgumentCaptor.getValue(), beanDiffer(expectedChanges));
        assertThat(result, is(true));
    }

    @Test
    public void checkSetSumLastAndStatusMailAndSumToPay_InNotifyOrderDbCampaignChanges_WhenSumDeltaGreaterMinPay() {
        BigDecimal minPay = Currencies.getCurrency(dbCampaignData.getCurrency()).getMinSumInterpreteAsPayment();
        sumDelta = Money.valueOf(minPay.add(BigDecimal.ONE), dbCampaignData.getCurrency());

        service.updateCampaignData(SHARD, true, sum, sumDelta, sumBalance, dbCampaignData, updateRequest);

        verify(service).updateCampaignDataInTransaction(eq(SHARD), eq(updateRequest.getCampaignId()),
                eq(updateRequest.getTid()), eq(dbCampaignData.getBalanceTid()), eq(dbCampaignData.getWalletId()),
                campaignChangesArgumentCaptor.capture(), eq(sumDelta));
        NotifyOrderDbCampaignChanges expectedChanges = new NotifyOrderDbCampaignChanges()
                .withSumLast(sumDelta.bigDecimalValue())
                .withStatusMail(StatusMail.NOT_SENT)
                .withSumToPay(BigDecimal.ZERO)
                .withSumUnits(sumUnits)
                .withSum(sum.bigDecimalValue())
                .withSumBalance(sumBalance)
                .withChanged(true);
        assertThat(campaignChangesArgumentCaptor.getValue(), beanDiffer(expectedChanges));
    }

    @Test
    public void checkNotSetSumLastAndStatusMailAndSumToPay_InNotifyOrderDbCampaignChanges_WhenSumDeltaLessMinPay() {
        BigDecimal minPay = Currencies.getCurrency(dbCampaignData.getCurrency()).getMinSumInterpreteAsPayment();
        sumDelta = Money.valueOf(minPay.subtract(BigDecimal.ONE), dbCampaignData.getCurrency());

        service.updateCampaignData(SHARD, true, sum, sumDelta, sumBalance, dbCampaignData, updateRequest);

        verify(service).updateCampaignDataInTransaction(eq(SHARD), eq(updateRequest.getCampaignId()),
                eq(updateRequest.getTid()), eq(dbCampaignData.getBalanceTid()), eq(dbCampaignData.getWalletId()),
                campaignChangesArgumentCaptor.capture(), eq(sumDelta));
        NotifyOrderDbCampaignChanges expectedChanges = new NotifyOrderDbCampaignChanges()
                .withSumLast(null)
                .withStatusMail(null)
                .withSumToPay(null)
                .withSumUnits(sumUnits)
                .withSum(sum.bigDecimalValue())
                .withSumBalance(sumBalance)
                .withChanged(true);
        assertThat(campaignChangesArgumentCaptor.getValue(), beanDiffer(expectedChanges));
    }

    @Test
    public void checkSetSumSpent_InNotifyOrderDbCampaignChanges_WhenCampaignTypeIsMcb() {
        dbCampaignData.withType(CampaignType.MCB);

        boolean result =
                service.updateCampaignData(SHARD, true, sum, sumDelta, sumBalance, dbCampaignData, updateRequest);

        verify(service).updateCampaignDataInTransaction(eq(SHARD), eq(updateRequest.getCampaignId()),
                eq(updateRequest.getTid()), eq(dbCampaignData.getBalanceTid()), eq(dbCampaignData.getWalletId()),
                campaignChangesArgumentCaptor.capture(), eq(sumDelta));
        NotifyOrderDbCampaignChanges expectedChanges = new NotifyOrderDbCampaignChanges()
                .withSumSpent(calcMediaSumSpent(sum, dbCampaignData.getSumSpentUnits(), sumUnits))
                .withSumUnits(sumUnits)
                .withSum(sum.bigDecimalValue())
                .withSumBalance(sumBalance)
                .withChanged(true);
        assertThat(campaignChangesArgumentCaptor.getValue(), beanDiffer(expectedChanges));
        assertThat(result, is(true));
    }

    @Test
    public void checkSetPaidByCertificate_InNotifyOrderDbCampaignChanges() {
        updateRequest.withPaidByCertificate(PAID_BY_CERTIFICATE);

        boolean result =
                service.updateCampaignData(SHARD, false, sum, sumDelta, sumBalance, dbCampaignData, updateRequest);

        verify(service).updateCampaignDataInTransaction(eq(SHARD), eq(updateRequest.getCampaignId()),
                eq(updateRequest.getTid()), eq(dbCampaignData.getBalanceTid()), eq(dbCampaignData.getWalletId()),
                campaignChangesArgumentCaptor.capture(), eq(sumDelta));
        NotifyOrderDbCampaignChanges expectedChanges = new NotifyOrderDbCampaignChanges()
                .withPaidByCertificate(true)
                .withChanged(true);
        assertThat(campaignChangesArgumentCaptor.getValue(), beanDiffer(expectedChanges));
        assertThat(result, is(true));
    }

    @Test
    public void checkSetResetStatusBsSynced_InNotifyOrderDbCampaignChanges() {
        updateRequest.withPaidByCertificate(PAID_BY_CERTIFICATE);

        service.updateCampaignData(SHARD, false, sum, sumDelta, sumBalance, dbCampaignData, updateRequest);

        verify(service).updateCampaignDataInTransaction(eq(SHARD), eq(updateRequest.getCampaignId()),
                eq(updateRequest.getTid()), eq(dbCampaignData.getBalanceTid()), eq(dbCampaignData.getWalletId()),
                campaignChangesArgumentCaptor.capture(), eq(sumDelta));
        assertThat(campaignChangesArgumentCaptor.getValue().needResetStatusBsSynced(), is(true));
    }

    @Test
    public void checkNotSetResetStatusBsSynced_InNotifyOrderDbCampaignChanges_WhenCampaignNotChanged() {
        dbCampaignData.withBalanceTid(updateRequest.getTid() + 1);

        service.updateCampaignData(SHARD, false, sum, sumDelta, sumBalance, dbCampaignData, updateRequest);

        verify(service).updateCampaignDataInTransaction(eq(SHARD), eq(updateRequest.getCampaignId()),
                eq(updateRequest.getTid()), eq(dbCampaignData.getBalanceTid()), eq(dbCampaignData.getWalletId()),
                campaignChangesArgumentCaptor.capture(), eq(sumDelta));
        assertThat(campaignChangesArgumentCaptor.getValue().needResetStatusBsSynced(), is(false));
    }
}
