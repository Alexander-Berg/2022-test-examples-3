package ru.yandex.direct.intapi.entity.balanceclient.service;

import java.util.Arrays;
import java.util.List;

import com.google.common.collect.Iterables;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaignpayment.repository.CampPaymentsInfoRepository;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.core.testing.steps.campaign.repository0.CampaignRepository;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.balanceclient.container.NotifyOrderDbCampaignChanges;
import ru.yandex.direct.intapi.entity.balanceclient.repository.NotifyOrderRepository;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static com.google.common.base.Preconditions.checkState;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.intapi.entity.balanceclient.BalanceClientServiceConstants.BAYAN_SERVICE_ID;

/**
 * Тесты на метод updateCampaignDataInTransaction с использованием базы из докера.
 * Проверяется, что изменения сохраняются в базу, если все ок или происходит корректный rollback
 *
 * @see NotifyOrderUpdateCampaignDataService
 */
@RunWith(SpringJUnit4ClassRunner.class)
@IntApiTest
public class NotifyOrderUpdateCampaignDataServiceCheckRollbackTransactionTest {

    @Autowired
    private Steps steps;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private NotifyOrderRepository notifyOrderRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private NotifyOrderUpdateCampaignDataService service;
    private CampPaymentsInfoRepository campPaymentsInfoRepository;
    private int shard;
    private Long campaignId;
    private Long balanceTid;
    private Long campaignBalanceTid;
    private Long campaignWalletId;
    private Money sumDelta;
    private NotifyOrderDbCampaignChanges campaignChanges;

    @Before
    public void before() {
        campPaymentsInfoRepository = mock(CampPaymentsInfoRepository.class);
        service = new NotifyOrderUpdateCampaignDataService(
                notifyOrderRepository,
                dslContextProvider,
                campPaymentsInfoRepository,
                BAYAN_SERVICE_ID);

        CampaignInfo campaign = steps.campaignSteps().createDefaultCampaign();
        campaignId = campaign.getCampaignId();
        shard = campaign.getShard();
        campaignBalanceTid = getCampaignFromDb().getBalanceInfo().getBalanceTid();
        campaignWalletId = getCampaignFromDb().getBalanceInfo().getWalletCid();
        balanceTid = RandomNumberUtils.nextPositiveLong();
        sumDelta = Money.valueOf(RandomNumberUtils.nextPositiveBigDecimal(), CurrencyCode.RUB);

        campaignChanges = new NotifyOrderDbCampaignChanges()
                .withBalanceTid(balanceTid)
                .withChanged(true);
    }

    private Campaign getCampaignFromDb() {
        List<Campaign> campaigns = campaignRepository.getCampaigns(shard, Arrays.asList(campaignId));
        Campaign campaign = Iterables.getFirst(campaigns, null);
        checkState(campaign != null);
        return campaign;
    }


    @Test
    public void checkUpdateCampaignBalanceTid() {
        service.updateCampaignDataInTransaction(shard, campaignId, balanceTid, campaignBalanceTid, campaignWalletId,
                campaignChanges, sumDelta);

        long newCampaignBalanceTid = getCampaignFromDb().getBalanceInfo().getBalanceTid();
        assertThat(newCampaignBalanceTid, equalTo(campaignChanges.getBalanceTid()));
    }

    @Test
    public void checkRollbackUpdateCampaign_WhenAddPaymentThrowException() {
        String errorMessage = "Can't add payment";
        doThrow(new RuntimeException(errorMessage)).when(campPaymentsInfoRepository)
                .addCampaignPayment(any(), eq(campaignId), eq(sumDelta.bigDecimalValue()));
        thrown.expect(RuntimeException.class);
        thrown.expectMessage(errorMessage);

        service.updateCampaignDataInTransaction(shard, campaignId, balanceTid, campaignBalanceTid, campaignWalletId,
                campaignChanges, sumDelta);

        assertThat(getCampaignFromDb().getBalanceInfo().getBalanceTid(), equalTo(campaignBalanceTid));
    }
}
