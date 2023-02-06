package ru.yandex.direct.intapi.entity.balanceclient.service;

import java.math.BigDecimal;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.service.CampaignService;
import ru.yandex.direct.core.entity.client.model.ClientNds;
import ru.yandex.direct.core.entity.client.service.ClientNdsService;
import ru.yandex.direct.core.entity.notification.container.NotifyOrderPayType;
import ru.yandex.direct.core.entity.user.repository.UserRepository;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Percent;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.intapi.entity.balanceclient.container.CampaignDataForNotifyOrder;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.intapi.entity.balanceclient.service.NotifyOrderNotificationService.calcSumPayedUnitsRate;
import static ru.yandex.direct.intapi.entity.balanceclient.service.NotifyOrderNotificationService.hasNds;

/**
 * Тесты на внутренние методы NotifyOrderNotificationService
 *
 * @see NotifyOrderNotificationService
 */
public class NotifyOrderNotificationServiceMethodTest {

    private NotifyOrderNotificationService notifyOrderNotificationService;
    private CampaignDataForNotifyOrder dbCampaignData;

    @Mock
    private ShardHelper shardHelper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ClientNdsService clientNdsService;

    @Mock
    private CampaignService campaignService;

    @Captor
    private ArgumentCaptor<Campaign> captor;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        notifyOrderNotificationService =
                new NotifyOrderNotificationService(shardHelper, userRepository, clientNdsService, campaignService,
                        null, null);

        dbCampaignData = NotifyOrderTestHelper.generateCampaignDataForNotifyOrder();
    }


    @Test
    public void checkMoneyOnCampaignIsBlockedArguments() {
        notifyOrderNotificationService.getPayType(dbCampaignData);
        verify(campaignService).moneyOnCampaignIsBlocked(captor.capture(), eq(false), eq(true));

        Campaign expectedCampaignData = new Campaign()
                .withId(dbCampaignData.getCampaignId())
                .withType(dbCampaignData.getType())
                .withWalletId(dbCampaignData.getWalletId())
                .withUserId(dbCampaignData.getUid())
                .withManagerUserId(dbCampaignData.getManagerUid())
                .withAgencyUserId(dbCampaignData.getAgencyUid())
                .withStatusPostModerate(dbCampaignData.getStatusPostModerate())
                .withStatusModerate(dbCampaignData.getStatusModerate());
        assertThat("проверяем, что сервис вызвал campaignService.moneyOnCampaignIsBlocked с ожидаемым объектом",
                captor.getValue(), beanDiffer(expectedCampaignData));
    }

    @Test
    public void checkMoneyOnCampaignIsBlocked() {
        when(campaignService.moneyOnCampaignIsBlocked(any(Campaign.class), eq(false), eq(true)))
                .thenReturn(true);

        NotifyOrderPayType payType = notifyOrderNotificationService.getPayType(dbCampaignData);
        assertThat("метод вернул ожидаемый payType", payType, equalTo(NotifyOrderPayType.BLOCKED));
    }

    @Test
    public void checkMoneyOnCampaignNotBlocked() {
        when(campaignService.moneyOnCampaignIsBlocked(any(Campaign.class), eq(false), eq(true)))
                .thenReturn(false);

        NotifyOrderPayType payType = notifyOrderNotificationService.getPayType(dbCampaignData);
        assertThat("метод вернул ожидаемый payType", payType, equalTo(NotifyOrderPayType.ANY));
    }

    @Test
    public void checkGetNdsForAgency() {
        notifyOrderNotificationService.getAgencyOrClientNds(dbCampaignData.getAgencyId(), dbCampaignData.getClientId());
        verify(clientNdsService).getClientNds(eq(ClientId.fromLong(dbCampaignData.getAgencyId())));
    }

    @Test
    public void checkGetNdsForClient() {
        notifyOrderNotificationService.getAgencyOrClientNds(null, dbCampaignData.getClientId());
        verify(clientNdsService).getClientNds(eq(ClientId.fromLong(dbCampaignData.getClientId())));
    }

    @Test
    public void checkGetNdsValue() {
        ClientNds ndsFromDb = new ClientNds()
                .withNds(Percent.fromPercent(RandomNumberUtils.nextPositiveBigDecimal()));
        when(clientNdsService.getClientNds(ClientId.fromLong(dbCampaignData.getAgencyId()))).thenReturn(ndsFromDb);

        Percent nds = notifyOrderNotificationService
                .getAgencyOrClientNds(dbCampaignData.getAgencyId(), dbCampaignData.getClientId());
        Percent expectedNds = ndsFromDb.getNds();
        assertThat("метод вернул ожидаемый ндс", nds, equalTo(expectedNds));
    }

    @Test
    public void checkGetNdsValueWhenNdsNotExistInDb() {
        when(clientNdsService.getClientNds(ClientId.fromLong(dbCampaignData.getAgencyId()))).thenReturn(null);

        Percent nds = notifyOrderNotificationService
                .getAgencyOrClientNds(dbCampaignData.getAgencyId(), dbCampaignData.getClientId());
        assertThat("метод вернул ожидаемый ндс", nds, nullValue());
    }

    @Test
    public void checkGetAgencyEmail() {
        String expectedEmail = RandomStringUtils.randomAlphanumeric(10);
        int shard = 2;
        when(shardHelper.getShardByUserId(dbCampaignData.getAgencyUid())).thenReturn(shard);
        when(userRepository.getUserEmail(shard, dbCampaignData.getAgencyUid())).thenReturn(expectedEmail);

        String email = notifyOrderNotificationService.getAgencyEmail(dbCampaignData.getAgencyUid());
        assertThat("метод вернул ожидаемую почту агенста", email, equalTo(expectedEmail));
    }

    @Test
    public void checkGetAgencyEmailWhenAgencyUidIsNull() {
        String email = notifyOrderNotificationService.getAgencyEmail(null);
        assertThat("метод вернул ожидаемую почту агенста", email, nullValue());
    }

    @Test
    public void checkCalcSumPayedUnitsRate() {
        BigDecimal sumUnits = RandomNumberUtils.nextPositiveBigDecimal();
        Long productRate = RandomNumberUtils.nextPositiveLong();
        BigDecimal expectedSumPayedUnitsRate = sumUnits
                .subtract(BigDecimal.valueOf(dbCampaignData.getSumUnits()))
                .divide(BigDecimal.valueOf(productRate), 3, BigDecimal.ROUND_HALF_DOWN)
                .stripTrailingZeros();

        BigDecimal sumPayedUnitsRate = calcSumPayedUnitsRate(sumUnits, dbCampaignData.getSumUnits(), productRate);
        assertThat("метод вернул ожидаемое значение", sumPayedUnitsRate, equalTo(expectedSumPayedUnitsRate));
    }

    @Test
    public void checkHasNds() {
        Percent nds = Percent.fromPercent(RandomNumberUtils.nextPositiveBigDecimal());
        assertThat("метод вернул ожидаемую значение", hasNds(CurrencyCode.RUB, nds), is(true));
    }

    @Test
    public void checkHasNdsWhenCurrencyIsYndFixed() {
        Percent nds = Percent.fromPercent(RandomNumberUtils.nextPositiveBigDecimal());
        assertThat("метод вернул ожидаемую значение", hasNds(CurrencyCode.YND_FIXED, nds), is(false));
    }

    @Test
    public void checkHasNdsWhenNdsIsZero() {
        Percent nds = Percent.fromPercent(BigDecimal.ZERO);
        assertThat("метод вернул ожидаемую значение", hasNds(CurrencyCode.YND_FIXED, nds), is(false));
    }

    @Test
    public void checkHasNdsWhenNdsIsNull() {
        Percent nds = null;
        assertThat("метод вернул ожидаемую значение", hasNds(CurrencyCode.YND_FIXED, nds), is(false));
    }
}
